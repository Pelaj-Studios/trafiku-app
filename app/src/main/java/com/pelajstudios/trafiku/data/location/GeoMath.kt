package com.pelajstudios.trafiku.data.location

import com.pelajstudios.trafiku.data.model.GeoPoint
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

internal fun distanceMeters(first: GeoPoint, second: GeoPoint): Double {
    val firstLatitude = first.latitude.toRadians()
    val secondLatitude = second.latitude.toRadians()
    val latitudeDelta = (second.latitude - first.latitude).toRadians()
    val longitudeDelta = (second.longitude - first.longitude).toRadians()
    val haversine = sin(latitudeDelta / 2.0) * sin(latitudeDelta / 2.0) +
        cos(firstLatitude) * cos(secondLatitude) *
        sin(longitudeDelta / 2.0) * sin(longitudeDelta / 2.0)
    return EARTH_RADIUS_METERS * 2.0 * atan2(sqrt(haversine), sqrt(1.0 - haversine))
}

internal fun interpolate(first: GeoPoint, second: GeoPoint, fraction: Double): GeoPoint = GeoPoint(
    latitude = first.latitude + (second.latitude - first.latitude) * fraction,
    longitude = first.longitude + (second.longitude - first.longitude) * fraction,
)

internal fun Double.toRadians(): Double = this * PI / 180.0

internal const val EARTH_RADIUS_METERS = 6_371_000.0
