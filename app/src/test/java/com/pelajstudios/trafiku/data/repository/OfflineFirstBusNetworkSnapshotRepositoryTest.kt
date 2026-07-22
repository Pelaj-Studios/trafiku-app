package com.pelajstudios.trafiku.data.repository

import com.pelajstudios.trafiku.data.local.BusLineEntity
import com.pelajstudios.trafiku.data.local.BusNetworkSnapshotDao
import com.pelajstudios.trafiku.data.local.BusNetworkSnapshotEntity
import com.pelajstudios.trafiku.data.local.BusRouteDetails
import com.pelajstudios.trafiku.data.local.BusRouteEntity
import com.pelajstudios.trafiku.data.local.BusRouteOptionRow
import com.pelajstudios.trafiku.data.local.BusStopEntity
import com.pelajstudios.trafiku.data.local.NormalizedBusNetworkSnapshot
import com.pelajstudios.trafiku.data.local.RoutePointEntity
import com.pelajstudios.trafiku.data.remote.BusNetworkSnapshotDto
import com.pelajstudios.trafiku.data.remote.TrafikuApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineFirstBusNetworkSnapshotRepositoryTest {
    private val remoteSnapshot = BusNetworkSnapshotDto(
        version = 2L,
        updatedAt = "2026-07-20T12:00:00Z",
        busLines = emptyMap(),
    )

    @Test
    fun freshSnapshotDoesNotDownload() = runBlocking {
        val snapshotDao = FakeBusNetworkSnapshotDao(
            initialSnapshot = storedSnapshot(downloadedAt = NOW - 1_000L),
        )
        val trafikuApi = FakeTrafikuApi(remoteSnapshot)
        val repository = repository(snapshotDao, trafikuApi)

        val result = repository.refresh()

        assertSame(SnapshotRefreshResult.SkippedFresh, result)
        assertEquals(0, trafikuApi.requestCount)
    }

    @Test
    fun staleSnapshotDownloadsAndReplacesStoredSnapshot() = runBlocking {
        val snapshotDao = FakeBusNetworkSnapshotDao(
            initialSnapshot = storedSnapshot(
                version = 1L,
                downloadedAt = NOW - CACHE_TIME_TO_LIVE - 1L,
            ),
        )
        val trafikuApi = FakeTrafikuApi(remoteSnapshot)
        val repository = repository(snapshotDao, trafikuApi)

        val result = repository.refresh()

        assertSame(SnapshotRefreshResult.Updated, result)
        assertEquals(1, trafikuApi.requestCount)
        assertEquals(remoteSnapshot.version, snapshotDao.getMetadata(OPERATOR)?.version)
        assertEquals(NOW, snapshotDao.getMetadata(OPERATOR)?.downloadedAtEpochMillis)
    }

    @Test
    fun forcedRefreshDownloadsWhenStoredSnapshotIsFresh() = runBlocking {
        val snapshotDao = FakeBusNetworkSnapshotDao(
            initialSnapshot = storedSnapshot(downloadedAt = NOW - 1_000L),
        )
        val trafikuApi = FakeTrafikuApi(remoteSnapshot)
        val repository = repository(snapshotDao, trafikuApi)

        val result = repository.refresh(force = true)

        assertSame(SnapshotRefreshResult.Updated, result)
        assertEquals(1, trafikuApi.requestCount)
    }

    @Test
    fun failedRefreshKeepsStoredSnapshot() = runBlocking {
        val originalSnapshot = storedSnapshot(
            version = 1L,
            downloadedAt = NOW - CACHE_TIME_TO_LIVE - 1L,
        )
        val snapshotDao = FakeBusNetworkSnapshotDao(initialSnapshot = originalSnapshot)
        val failure = IllegalStateException("Server is unavailable.")
        val trafikuApi = FakeTrafikuApi(remoteSnapshot, failure)
        val repository = repository(snapshotDao, trafikuApi)

        val result = repository.refresh()

        assertTrue(result is SnapshotRefreshResult.Failed)
        assertSame(failure, (result as SnapshotRefreshResult.Failed).cause)
        assertEquals(originalSnapshot, snapshotDao.getMetadata(OPERATOR))
    }

    private fun repository(
        snapshotDao: BusNetworkSnapshotDao,
        trafikuApi: TrafikuApi,
    ) = OfflineFirstBusNetworkSnapshotRepository(
        snapshotDao = snapshotDao,
        trafikuApi = trafikuApi,
        currentTimeMillis = { NOW },
        workerDispatcher = Dispatchers.Unconfined,
        cacheTimeToLiveMillis = CACHE_TIME_TO_LIVE,
    )

    private fun storedSnapshot(
        version: Long = remoteSnapshot.version,
        downloadedAt: Long,
    ): BusNetworkSnapshotEntity = BusNetworkSnapshotEntity(
        operator = OPERATOR,
        version = version,
        serverUpdatedAt = remoteSnapshot.updatedAt,
        downloadedAtEpochMillis = downloadedAt,
    )

    private class FakeBusNetworkSnapshotDao(
        initialSnapshot: BusNetworkSnapshotEntity?,
    ) : BusNetworkSnapshotDao() {
        private val storedSnapshot = MutableStateFlow(initialSnapshot)

        override fun observeMetadata(operator: String): Flow<BusNetworkSnapshotEntity?> =
            storedSnapshot

        override suspend fun getMetadata(operator: String): BusNetworkSnapshotEntity? =
            storedSnapshot.value

        override fun observeLines(operator: String): Flow<List<BusLineEntity>> =
            flowOf(emptyList())

        override fun observeRoutes(lineId: String): Flow<List<BusRouteOptionRow>> =
            flowOf(emptyList())

        override fun observeRoute(
            lineId: String,
            direction: String,
        ): Flow<BusRouteDetails?> = flowOf(null)

        override suspend fun replaceSnapshot(snapshot: NormalizedBusNetworkSnapshot) {
            storedSnapshot.value = snapshot.metadata
        }

        override suspend fun deleteSnapshot(operator: String) {
            storedSnapshot.value = null
        }

        override suspend fun insertMetadata(metadata: BusNetworkSnapshotEntity) = Unit

        override suspend fun insertLines(lines: List<BusLineEntity>) = Unit

        override suspend fun insertRoutes(routes: List<BusRouteEntity>) = Unit

        override suspend fun insertStops(stops: List<BusStopEntity>) = Unit

        override suspend fun insertRoutePoints(points: List<RoutePointEntity>) = Unit
    }

    private class FakeTrafikuApi(
        private val snapshot: BusNetworkSnapshotDto,
        private val failure: Exception? = null,
    ) : TrafikuApi {
        var requestCount = 0
            private set

        override suspend fun getBusNetworkSnapshot(operator: String): BusNetworkSnapshotDto {
            requestCount += 1
            failure?.let { throw it }
            return snapshot
        }
    }

    private companion object {
        const val NOW = 1_000_000_000L
        const val CACHE_TIME_TO_LIVE = 24L * 60L * 60L * 1_000L
        const val OPERATOR = "trafiku_urban"
    }
}
