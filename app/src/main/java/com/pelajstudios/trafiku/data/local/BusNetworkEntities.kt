package com.pelajstudios.trafiku.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "bus_lines",
    foreignKeys = [
        ForeignKey(
            entity = BusNetworkSnapshotEntity::class,
            parentColumns = ["operator"],
            childColumns = ["operator"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("operator")],
)
data class BusLineEntity(
    @PrimaryKey val id: String,
    val operator: String,
    val name: String,
)

@Entity(
    tableName = "bus_routes",
    foreignKeys = [
        ForeignKey(
            entity = BusLineEntity::class,
            parentColumns = ["id"],
            childColumns = ["lineId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("lineId")],
)
data class BusRouteEntity(
    @PrimaryKey val id: String,
    val lineId: String,
    val direction: String,
)

@Entity(
    tableName = "bus_stops",
    primaryKeys = ["routeId", "sequence"],
    foreignKeys = [
        ForeignKey(
            entity = BusRouteEntity::class,
            parentColumns = ["id"],
            childColumns = ["routeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("routeId")],
)
data class BusStopEntity(
    val routeId: String,
    val sequence: Int,
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
)

@Entity(
    tableName = "route_points",
    primaryKeys = ["routeId", "sequence"],
    foreignKeys = [
        ForeignKey(
            entity = BusRouteEntity::class,
            parentColumns = ["id"],
            childColumns = ["routeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("routeId")],
)
data class RoutePointEntity(
    val routeId: String,
    val sequence: Int,
    val latitude: Double,
    val longitude: Double,
)

data class NormalizedBusNetworkSnapshot(
    val metadata: BusNetworkSnapshotEntity,
    val lines: List<BusLineEntity>,
    val routes: List<BusRouteEntity>,
    val stops: List<BusStopEntity>,
    val routePoints: List<RoutePointEntity>,
)
