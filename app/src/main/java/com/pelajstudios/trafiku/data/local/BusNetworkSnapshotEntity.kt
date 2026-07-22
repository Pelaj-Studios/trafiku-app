package com.pelajstudios.trafiku.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bus_network_snapshots")
data class BusNetworkSnapshotEntity(
    @PrimaryKey val operator: String,
    val version: Long,
    val serverUpdatedAt: String,
    val downloadedAtEpochMillis: Long,
)
