package com.pelajstudios.trafiku.data.repository

import com.pelajstudios.trafiku.data.local.BusLineEntity
import com.pelajstudios.trafiku.data.local.BusNetworkSnapshotEntity
import com.pelajstudios.trafiku.data.local.BusRouteEntity
import com.pelajstudios.trafiku.data.local.BusStopEntity
import com.pelajstudios.trafiku.data.local.NormalizedBusNetworkSnapshot
import com.pelajstudios.trafiku.data.local.RoutePointEntity
import com.pelajstudios.trafiku.data.remote.BusNetworkSnapshotDto

fun BusNetworkSnapshotDto.normalize(
    operator: String,
    downloadedAtEpochMillis: Long,
): NormalizedBusNetworkSnapshot {
    val lines = ArrayList<BusLineEntity>(busLines.size)
    val routes = ArrayList<BusRouteEntity>()
    val stops = ArrayList<BusStopEntity>()
    val routePoints = ArrayList<RoutePointEntity>()

    busLines.values.forEach { line ->
        lines += BusLineEntity(
            id = line.id,
            operator = operator,
            name = line.name,
        )
        line.routes.values.forEach { route ->
            routes += BusRouteEntity(
                id = route.id,
                lineId = line.id,
                direction = route.direction.name,
            )
            route.stops.forEachIndexed { index, stop ->
                stops += BusStopEntity(
                    routeId = route.id,
                    sequence = index,
                    id = stop.id,
                    name = stop.name,
                    latitude = stop.point.latitude,
                    longitude = stop.point.longitude,
                )
            }
            route.geometry.geoLine.points.forEachIndexed { index, point ->
                routePoints += RoutePointEntity(
                    routeId = route.id,
                    sequence = index,
                    latitude = point.latitude,
                    longitude = point.longitude,
                )
            }
        }
    }

    return NormalizedBusNetworkSnapshot(
        metadata = BusNetworkSnapshotEntity(
            operator = operator,
            version = version,
            serverUpdatedAt = updatedAt,
            downloadedAtEpochMillis = downloadedAtEpochMillis,
        ),
        lines = lines,
        routes = routes,
        stops = stops,
        routePoints = routePoints,
    )
}
