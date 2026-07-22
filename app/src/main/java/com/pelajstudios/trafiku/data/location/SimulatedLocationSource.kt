package com.pelajstudios.trafiku.data.location

import android.os.SystemClock
import com.pelajstudios.trafiku.data.model.BusRoute
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

internal class SimulatedLocationSource(
    private val elapsedRealtimeMillis: () -> Long = SystemClock::elapsedRealtime,
    private val wait: suspend (Long) -> Unit = { durationMillis -> delay(durationMillis) },
) {
    fun events(route: BusRoute): Flow<LocationState> = flow {
        if (route.stops.isEmpty()) {
            emit(LocationState(LocationStatus.NoLocationSource))
            return@flow
        }

        emit(LocationState(LocationStatus.Acquiring(LocationSourceKind.SIMULATOR)))
        route.stops.forEach { stop ->
            repeat(FIXES_PER_STOP) {
                val fix = LocationFix(
                    latitude = stop.point.latitude,
                    longitude = stop.point.longitude,
                    accuracyMeters = SIMULATED_ACCURACY_METERS,
                    speedMetersPerSecond = SIMULATED_SPEED_METERS_PER_SECOND,
                    elapsedRealtimeMillis = elapsedRealtimeMillis(),
                    source = LocationSourceKind.SIMULATOR,
                )
                emit(
                    LocationState(
                        status = LocationStatus.Active(
                            source = LocationSourceKind.SIMULATOR,
                            accuracyMeters = fix.accuracyMeters,
                        ),
                        fix = fix,
                    ),
                )
                wait(UPDATE_INTERVAL_MILLIS)
            }
        }
    }

    private companion object {
        const val FIXES_PER_STOP = 3
        const val SIMULATED_SPEED_METERS_PER_SECOND = 8.0F
        const val SIMULATED_ACCURACY_METERS = 5.0F
        const val UPDATE_INTERVAL_MILLIS = 1_000L
    }
}
