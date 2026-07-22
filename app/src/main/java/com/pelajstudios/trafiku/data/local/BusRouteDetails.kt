package com.pelajstudios.trafiku.data.local

import androidx.room.Embedded
import androidx.room.Relation

data class BusRouteDetails(
    @Embedded val route: BusRouteEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "routeId",
    )
    val stops: List<BusStopEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "routeId",
    )
    val routePoints: List<RoutePointEntity>,
)

data class BusRouteOptionRow(
    val id: String,
    val direction: String,
    val firstStopName: String?,
    val lastStopName: String?,
)
