package com.pelajstudios.trafiku.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class BusNetworkSnapshotDto(
    val version: Long,
    val updatedAt: String,
    val busLines: Map<String, BusLineDto>,
)

@Serializable
data class BusLineDto(
    val id: String,
    val name: String,
    val routes: Map<String, BusRouteDto>,
)

@Serializable
data class BusRouteDto(
    val id: String,
    val geometry: RouteGeometryDto,
    val stops: List<BusStopDto>,
    val direction: RouteDirectionDto,
)

@Serializable
data class RouteGeometryDto(
    val geoLine: GeoPolylineDto,
)

@Serializable
data class GeoPolylineDto(
    val points: List<GeoPointDto>,
)

@Serializable
data class BusStopDto(
    val id: String,
    val name: String,
    val point: GeoPointDto,
)

@Serializable
data class GeoPointDto(
    val latitude: Double,
    val longitude: Double,
)

@Serializable
enum class RouteDirectionDto {
    OUTBOUND,
    INBOUND,
}
