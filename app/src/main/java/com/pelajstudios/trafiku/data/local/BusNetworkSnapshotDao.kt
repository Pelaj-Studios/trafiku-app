package com.pelajstudios.trafiku.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class BusNetworkSnapshotDao {
    @Query("SELECT * FROM bus_network_snapshots WHERE operator = :operator")
    abstract fun observeMetadata(operator: String): Flow<BusNetworkSnapshotEntity?>

    @Query("SELECT * FROM bus_network_snapshots WHERE operator = :operator")
    abstract suspend fun getMetadata(operator: String): BusNetworkSnapshotEntity?

    @Query("SELECT * FROM bus_lines WHERE operator = :operator ORDER BY name COLLATE NOCASE")
    abstract fun observeLines(operator: String): Flow<List<BusLineEntity>>

    @Query(
        "SELECT routes.id, routes.direction, " +
            "(SELECT name FROM bus_stops " +
            "WHERE routeId = routes.id ORDER BY sequence ASC LIMIT 1) AS firstStopName, " +
            "(SELECT name FROM bus_stops " +
            "WHERE routeId = routes.id ORDER BY sequence DESC LIMIT 1) AS lastStopName " +
            "FROM bus_routes AS routes WHERE routes.lineId = :lineId " +
            "ORDER BY routes.direction DESC",
    )
    abstract fun observeRoutes(lineId: String): Flow<List<BusRouteOptionRow>>

    @Transaction
    @Query(
        "SELECT * FROM bus_routes " +
            "WHERE lineId = :lineId AND direction = :direction LIMIT 1",
    )
    abstract fun observeRoute(
        lineId: String,
        direction: String,
    ): Flow<BusRouteDetails?>

    @Query("DELETE FROM bus_network_snapshots WHERE operator = :operator")
    protected abstract suspend fun deleteSnapshot(operator: String)

    @Insert
    protected abstract suspend fun insertMetadata(metadata: BusNetworkSnapshotEntity)

    @Insert
    protected abstract suspend fun insertLines(lines: List<BusLineEntity>)

    @Insert
    protected abstract suspend fun insertRoutes(routes: List<BusRouteEntity>)

    @Insert
    protected abstract suspend fun insertStops(stops: List<BusStopEntity>)

    @Insert
    protected abstract suspend fun insertRoutePoints(points: List<RoutePointEntity>)

    @Transaction
    open suspend fun replaceSnapshot(snapshot: NormalizedBusNetworkSnapshot) {
        deleteSnapshot(snapshot.metadata.operator)
        insertMetadata(snapshot.metadata)
        if (snapshot.lines.isNotEmpty()) {
            insertLines(snapshot.lines)
        }
        if (snapshot.routes.isNotEmpty()) {
            insertRoutes(snapshot.routes)
        }
        if (snapshot.stops.isNotEmpty()) {
            insertStops(snapshot.stops)
        }
        if (snapshot.routePoints.isNotEmpty()) {
            insertRoutePoints(snapshot.routePoints)
        }
    }
}
