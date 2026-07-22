package com.pelajstudios.trafiku.data.repository

import com.pelajstudios.trafiku.data.local.BusNetworkSnapshotDao
import com.pelajstudios.trafiku.data.local.BusNetworkSnapshotEntity
import com.pelajstudios.trafiku.data.local.BusRouteDetails
import com.pelajstudios.trafiku.data.model.BusLineSummary
import com.pelajstudios.trafiku.data.model.BusNetworkMetadata
import com.pelajstudios.trafiku.data.model.BusRoute
import com.pelajstudios.trafiku.data.model.BusRouteSummary
import com.pelajstudios.trafiku.data.model.BusSelection
import com.pelajstudios.trafiku.data.model.BusStop
import com.pelajstudios.trafiku.data.model.GeoPoint
import com.pelajstudios.trafiku.data.model.RouteDirection
import com.pelajstudios.trafiku.data.remote.TrafikuApi
import java.util.concurrent.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class OfflineFirstBusNetworkSnapshotRepository(
    private val snapshotDao: BusNetworkSnapshotDao,
    private val trafikuApi: TrafikuApi,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
    private val workerDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val cacheTimeToLiveMillis: Long = CACHE_TIME_TO_LIVE_MILLIS,
) : BusNetworkSnapshotRepository {

    override val metadata: Flow<BusNetworkMetadata?> = snapshotDao
        .observeMetadata(TRAFIKU_URBAN_OPERATOR)
        .map { entity -> entity?.toMetadata() }
        .distinctUntilChanged()
        .flowOn(workerDispatcher)

    override val lines: Flow<List<BusLineSummary>> = snapshotDao
        .observeLines(TRAFIKU_URBAN_OPERATOR)
        .map { entities ->
            entities.map { entity -> BusLineSummary(id = entity.id, name = entity.name) }
        }
        .distinctUntilChanged()

    override fun observeRoutes(lineId: String): Flow<List<BusRouteSummary>> = snapshotDao
        .observeRoutes(lineId)
        .map { entities ->
            entities.map { entity ->
                val direction = RouteDirection.valueOf(entity.direction)
                BusRouteSummary(
                    id = entity.id,
                    direction = direction,
                    directionLabel = if (
                        entity.firstStopName != null && entity.lastStopName != null
                    ) {
                        "${entity.firstStopName} → ${entity.lastStopName}"
                    } else {
                        direction.displayName
                    },
                )
            }
        }
        .distinctUntilChanged()

    override fun observeRoute(selection: BusSelection): Flow<BusRoute?> = snapshotDao
        .observeRoute(
            lineId = selection.lineId,
            direction = selection.direction.name,
        )
        .map { details -> details?.toDomain() }
        .distinctUntilChanged()
        .flowOn(workerDispatcher)

    override suspend fun refresh(force: Boolean): SnapshotRefreshResult {
        val storedSnapshot = snapshotDao.getMetadata(TRAFIKU_URBAN_OPERATOR)
        val downloadedAt = storedSnapshot?.downloadedAtEpochMillis
        val cacheIsFresh = downloadedAt != null &&
            currentTimeMillis() - downloadedAt < cacheTimeToLiveMillis

        if (!force && cacheIsFresh) {
            return SnapshotRefreshResult.SkippedFresh
        }

        return try {
            val remoteSnapshot = trafikuApi.getBusNetworkSnapshot(TRAFIKU_URBAN_OPERATOR)
            val downloadedAtEpochMillis = currentTimeMillis()
            val normalizedSnapshot = withContext(workerDispatcher) {
                remoteSnapshot.normalize(
                    operator = TRAFIKU_URBAN_OPERATOR,
                    downloadedAtEpochMillis = downloadedAtEpochMillis,
                )
            }
            snapshotDao.replaceSnapshot(normalizedSnapshot)
            SnapshotRefreshResult.Updated
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            SnapshotRefreshResult.Failed(exception)
        }
    }

    companion object {
        const val TRAFIKU_URBAN_OPERATOR = "trafiku_urban"
        const val CACHE_TIME_TO_LIVE_MILLIS = 24L * 60L * 60L * 1_000L
    }
}

private fun BusNetworkSnapshotEntity.toMetadata(): BusNetworkMetadata = BusNetworkMetadata(
    version = version,
    updatedAt = serverUpdatedAt,
    downloadedAtEpochMillis = downloadedAtEpochMillis,
)

private fun BusRouteDetails.toDomain(): BusRoute = BusRoute(
    id = route.id,
    direction = RouteDirection.valueOf(route.direction),
    stops = stops
        .sortedBy { stop -> stop.sequence }
        .map { stop ->
            BusStop(
                id = stop.id,
                name = stop.name,
                point = GeoPoint(latitude = stop.latitude, longitude = stop.longitude),
            )
        },
    geometry = routePoints
        .sortedBy { point -> point.sequence }
        .map { point -> GeoPoint(latitude = point.latitude, longitude = point.longitude) },
)
