package com.pelajstudios.trafiku.data.location

import com.pelajstudios.trafiku.data.model.BusRoute
import kotlinx.coroutines.flow.Flow

internal data class LocationFix(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val speedMetersPerSecond: Float?,
    val elapsedRealtimeMillis: Long,
    val source: LocationSourceKind,
)

internal enum class LocationSourceKind {
    SYSTEM,
    USB,
    SIMULATOR,
}

internal sealed interface LocationStatus {
    data class Acquiring(val source: LocationSourceKind) : LocationStatus

    data class Active(
        val source: LocationSourceKind,
        val accuracyMeters: Float,
    ) : LocationStatus

    data object LocationPermissionRequired : LocationStatus

    data class UsbPermissionRequired(val deviceName: String) : LocationStatus

    data object NoLocationSource : LocationStatus

    data class Error(val source: LocationSourceKind) : LocationStatus
}

internal data class LocationState(
    val status: LocationStatus,
    val fix: LocationFix? = null,
)

internal interface LocationRepository {
    val simulationSupported: Boolean

    val simulationEnabled: Boolean

    fun observeLocation(route: BusRoute): Flow<LocationState>

    fun notifyLocationPermissionChanged()

    fun requestUsbPermission()

    fun setSimulationEnabled(enabled: Boolean)
}

internal sealed interface LocationSourceEvent {
    data class Fix(val value: LocationFix) : LocationSourceEvent

    data class Acquiring(val source: LocationSourceKind) : LocationSourceEvent

    data object LocationPermissionRequired : LocationSourceEvent

    data class UsbPermissionRequired(val deviceName: String) : LocationSourceEvent

    data class Unavailable(val source: LocationSourceKind) : LocationSourceEvent

    data class Error(val source: LocationSourceKind) : LocationSourceEvent
}

internal fun interface LocationSource {
    fun events(): Flow<LocationSourceEvent>
}
