package com.pelajstudios.trafiku.data.repository

import com.pelajstudios.trafiku.data.model.BusLineSummary
import com.pelajstudios.trafiku.data.model.BusNetworkMetadata
import com.pelajstudios.trafiku.data.model.BusRoute
import com.pelajstudios.trafiku.data.model.BusRouteSummary
import com.pelajstudios.trafiku.data.model.BusSelection
import kotlinx.coroutines.flow.Flow

interface BusNetworkSnapshotRepository {
    val metadata: Flow<BusNetworkMetadata?>

    val lines: Flow<List<BusLineSummary>>

    fun observeRoutes(lineId: String): Flow<List<BusRouteSummary>>

    fun observeRoute(selection: BusSelection): Flow<BusRoute?>

    suspend fun refresh(force: Boolean = false): SnapshotRefreshResult
}

sealed interface SnapshotRefreshResult {
    data object Updated : SnapshotRefreshResult

    data object SkippedFresh : SnapshotRefreshResult

    data class Failed(val cause: Throwable) : SnapshotRefreshResult
}
