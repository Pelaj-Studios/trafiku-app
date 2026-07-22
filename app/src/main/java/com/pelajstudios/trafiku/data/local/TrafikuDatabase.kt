package com.pelajstudios.trafiku.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        BusNetworkSnapshotEntity::class,
        BusLineEntity::class,
        BusRouteEntity::class,
        BusStopEntity::class,
        RoutePointEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class TrafikuDatabase : RoomDatabase() {
    abstract fun busNetworkSnapshotDao(): BusNetworkSnapshotDao
}
