package com.pelajstudios.trafiku.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

internal class SystemLocationSource(
    private val context: Context,
    private val locationManager: LocationManager,
) : LocationSource {
    override fun events(): Flow<LocationSourceEvent> = callbackFlow {
        if (!locationManager.hasProvider(LocationManager.GPS_PROVIDER)) {
            trySend(LocationSourceEvent.Unavailable(LocationSourceKind.SYSTEM))
            close()
            return@callbackFlow
        }
        if (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            trySend(LocationSourceEvent.LocationPermissionRequired)
            close()
            return@callbackFlow
        }
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                trySend(LocationSourceEvent.Fix(location.toFix()))
            }

            override fun onProviderDisabled(provider: String) {
                if (provider == LocationManager.GPS_PROVIDER) {
                    trySend(LocationSourceEvent.Unavailable(LocationSourceKind.SYSTEM))
                }
            }

            @Deprecated("The platform does not use provider status callbacks on current Android versions.")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
        }

        trySend(LocationSourceEvent.Acquiring(LocationSourceKind.SYSTEM))
        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                UPDATE_INTERVAL_MILLIS,
                MINIMUM_DISTANCE_METERS,
                listener,
                context.mainLooper,
            )
        } catch (_: SecurityException) {
            trySend(LocationSourceEvent.LocationPermissionRequired)
            close()
            return@callbackFlow
        } catch (_: IllegalArgumentException) {
            trySend(LocationSourceEvent.Unavailable(LocationSourceKind.SYSTEM))
            close()
            return@callbackFlow
        }

        awaitClose {
            locationManager.removeUpdates(listener)
        }
    }

    private fun Location.toFix(): LocationFix = LocationFix(
        latitude = latitude,
        longitude = longitude,
        accuracyMeters = if (hasAccuracy()) accuracy else Float.MAX_VALUE,
        speedMetersPerSecond = if (hasSpeed()) speed else null,
        elapsedRealtimeMillis = elapsedRealtimeNanos / NANOS_PER_MILLISECOND,
        source = LocationSourceKind.SYSTEM,
    )

    private companion object {
        const val UPDATE_INTERVAL_MILLIS = 1_000L
        const val MINIMUM_DISTANCE_METERS = 0.0F
        const val NANOS_PER_MILLISECOND = 1_000_000L
    }
}
