package com.pelajstudios.trafiku.data.location

import android.os.SystemClock
import com.pelajstudios.trafiku.data.model.BusRoute
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

@OptIn(ExperimentalCoroutinesApi::class)
internal class DefaultLocationRepository(
    private val systemLocationSource: LocationSource,
    private val usbLocationSource: UsbNmeaLocationSource,
    private val simulatedLocationSource: SimulatedLocationSource,
    override val simulationSupported: Boolean,
    private val elapsedRealtimeMillis: () -> Long = SystemClock::elapsedRealtime,
) : LocationRepository {
    private val permissionRevision = MutableStateFlow(0)
    private val simulationEnabledState = MutableStateFlow(false)

    override val simulationEnabled: Boolean
        get() = simulationEnabledState.value

    override fun observeLocation(route: BusRoute): Flow<LocationState> = simulationEnabledState
        .flatMapLatest { simulationEnabled ->
            if (simulationSupported && simulationEnabled) {
                simulatedLocationSource.events(route)
            } else {
                permissionRevision.flatMapLatest { observeProductionSources() }
            }
        }

    override fun notifyLocationPermissionChanged() {
        permissionRevision.value += 1
    }

    override fun requestUsbPermission() {
        usbLocationSource.requestPermission()
    }

    override fun setSimulationEnabled(enabled: Boolean) {
        if (simulationSupported) {
            simulationEnabledState.value = enabled
        }
    }

    private fun observeProductionSources(): Flow<LocationState> = channelFlow {
        val lock = Mutex()
        val startTime = elapsedRealtimeMillis()
        val startUsbFallback = CompletableDeferred<Unit>()
        var activeSource: LocationSourceKind? = null
        var lastSystemFix: LocationFix? = null
        var lastSystemFixReceivedAt = 0L
        var lastUsbFix: LocationFix? = null
        var lastUsbFixReceivedAt = 0L
        var systemUnavailable = false
        var usbUnavailable = false

        suspend fun sendFix(fix: LocationFix) {
            send(
                LocationState(
                    status = LocationStatus.Active(
                        source = fix.source,
                        accuracyMeters = fix.accuracyMeters,
                    ),
                    fix = fix,
                ),
            )
        }

        fun startUsbNow() {
            startUsbFallback.complete(Unit)
        }

        suspend fun processSystemEvent(event: LocationSourceEvent) {
            lock.withLock {
                when (event) {
                    is LocationSourceEvent.Fix -> {
                        val fix = event.value
                        if (fix.accuracyMeters <= MAXIMUM_ACCURACY_METERS) {
                            lastSystemFix = fix
                            lastSystemFixReceivedAt = elapsedRealtimeMillis()
                            if (activeSource == null) {
                                activeSource = LocationSourceKind.SYSTEM
                            }
                            if (activeSource == LocationSourceKind.SYSTEM) {
                                sendFix(fix)
                            }
                        }
                    }

                    is LocationSourceEvent.Acquiring -> {
                        if (activeSource == null) {
                            send(LocationState(LocationStatus.Acquiring(event.source)))
                        }
                    }

                    LocationSourceEvent.LocationPermissionRequired -> {
                        if (activeSource == null) {
                            send(LocationState(LocationStatus.LocationPermissionRequired))
                        }
                    }

                    is LocationSourceEvent.Unavailable -> {
                        systemUnavailable = true
                        startUsbNow()
                    }

                    is LocationSourceEvent.Error -> {
                        startUsbNow()
                        if (activeSource == null) {
                            send(LocationState(LocationStatus.Error(event.source)))
                        }
                    }

                    is LocationSourceEvent.UsbPermissionRequired -> Unit
                }
            }
        }

        suspend fun processUsbEvent(event: LocationSourceEvent) {
            lock.withLock {
                when (event) {
                    is LocationSourceEvent.Fix -> {
                        val fix = event.value
                        if (fix.accuracyMeters <= MAXIMUM_ACCURACY_METERS) {
                            lastUsbFix = fix
                            lastUsbFixReceivedAt = elapsedRealtimeMillis()
                            if (activeSource == null) {
                                activeSource = LocationSourceKind.USB
                            }
                            if (activeSource == LocationSourceKind.USB) {
                                sendFix(fix)
                            }
                        }
                    }

                    is LocationSourceEvent.Acquiring -> {
                        if (activeSource == null) {
                            send(LocationState(LocationStatus.Acquiring(event.source)))
                        }
                    }

                    is LocationSourceEvent.UsbPermissionRequired -> {
                        if (activeSource == null) {
                            send(
                                LocationState(
                                    LocationStatus.UsbPermissionRequired(event.deviceName),
                                ),
                            )
                        }
                    }

                    is LocationSourceEvent.Unavailable -> {
                        usbUnavailable = true
                        if (activeSource == null && systemUnavailable) {
                            send(LocationState(LocationStatus.NoLocationSource))
                        }
                    }

                    is LocationSourceEvent.Error -> {
                        if (activeSource == null) {
                            send(LocationState(LocationStatus.Error(event.source)))
                        }
                    }

                    LocationSourceEvent.LocationPermissionRequired -> Unit
                }
            }
        }

        launch {
            systemLocationSource.events().collect(::processSystemEvent)
        }
        launch {
            val fallbackRequested = withTimeoutOrNull(FALLBACK_TIMEOUT_MILLIS) {
                startUsbFallback.await()
                true
            } ?: false
            if (!fallbackRequested) {
                val systemIsActive = lock.withLock {
                    activeSource == LocationSourceKind.SYSTEM
                }
                if (systemIsActive) {
                    startUsbFallback.await()
                }
            }
            usbLocationSource.events().collect(::processUsbEvent)
        }
        launch {
            while (true) {
                delay(WATCHDOG_INTERVAL_MILLIS)
                lock.withLock {
                    val now = elapsedRealtimeMillis()
                    when (activeSource) {
                        LocationSourceKind.SYSTEM -> {
                            if (now - lastSystemFixReceivedAt > FALLBACK_TIMEOUT_MILLIS) {
                                activeSource = null
                                startUsbNow()
                                if (
                                    lastUsbFix != null &&
                                    now - lastUsbFixReceivedAt <= FALLBACK_TIMEOUT_MILLIS
                                ) {
                                    activeSource = LocationSourceKind.USB
                                    sendFix(checkNotNull(lastUsbFix))
                                }
                            }
                        }

                        LocationSourceKind.USB -> {
                            if (now - lastUsbFixReceivedAt > FALLBACK_TIMEOUT_MILLIS) {
                                activeSource = null
                                if (
                                    lastSystemFix != null &&
                                    now - lastSystemFixReceivedAt <= FALLBACK_TIMEOUT_MILLIS
                                ) {
                                    activeSource = LocationSourceKind.SYSTEM
                                    sendFix(checkNotNull(lastSystemFix))
                                } else {
                                    send(LocationState(LocationStatus.Error(LocationSourceKind.USB)))
                                }
                            }
                        }

                        LocationSourceKind.SIMULATOR -> Unit
                        null -> {
                            if (now - startTime >= FALLBACK_TIMEOUT_MILLIS) {
                                startUsbNow()
                                if (systemUnavailable && usbUnavailable) {
                                    send(LocationState(LocationStatus.NoLocationSource))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private companion object {
        const val MAXIMUM_ACCURACY_METERS = 25.0F
        const val FALLBACK_TIMEOUT_MILLIS = 15_000L
        const val WATCHDOG_INTERVAL_MILLIS = 1_000L
    }
}
