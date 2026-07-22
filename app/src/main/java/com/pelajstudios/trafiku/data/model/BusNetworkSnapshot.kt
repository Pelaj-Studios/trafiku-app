package com.pelajstudios.trafiku.data.model

data class BusNetworkMetadata(
    val version: Long,
    val updatedAt: String,
    val downloadedAtEpochMillis: Long,
)

data class BusLineSummary(
    val id: String,
    val name: String,
)

data class BusRouteSummary(
    val id: String,
    val direction: RouteDirection,
    val directionLabel: String,
)

data class BusRoute(
    val id: String,
    val geometry: List<GeoPoint>,
    val stops: List<BusStop>,
    val direction: RouteDirection,
) {
    val directionLabel: String
        get() {
            val firstStop = stops.firstOrNull()?.name ?: return direction.displayName
            val lastStop = stops.lastOrNull()?.name ?: return direction.displayName
            return "$firstStop → $lastStop"
        }
}

data class BusStop(
    val id: String,
    val name: String,
    val point: GeoPoint,
)

data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
)

enum class RouteDirection(val displayName: String) {
    OUTBOUND("Dalje"),
    INBOUND("Kthim"),
}

data class BusSelection(
    val lineId: String,
    val direction: RouteDirection,
)
