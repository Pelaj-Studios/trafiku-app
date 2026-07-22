package com.pelajstudios.trafiku.data.location

import com.pelajstudios.trafiku.data.model.BusRoute
import com.pelajstudios.trafiku.data.model.BusStop
import com.pelajstudios.trafiku.data.model.GeoPoint
import com.pelajstudios.trafiku.data.model.RouteDirection
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SimulatedLocationSourceTest {
    @Test
    fun sourceVisitsEachStopInSequence() = runBlocking {
        var elapsedRealtime = 0L
        val source = SimulatedLocationSource(
            elapsedRealtimeMillis = {
                elapsedRealtime += 1_000L
                elapsedRealtime
            },
            wait = {},
        )
        val firstStop = stop(id = "first", latitude = 42.1, longitude = 21.1)
        val secondStop = stop(id = "second", latitude = 42.2, longitude = 21.2)
        val route = BusRoute(
            id = "route",
            geometry = emptyList(),
            stops = listOf(firstStop, secondStop),
            direction = RouteDirection.OUTBOUND,
        )

        val states = source.events(route).toList()
        val fixes = states.mapNotNull(LocationState::fix)

        assertTrue(states.first().status is LocationStatus.Acquiring)
        assertEquals(6, fixes.size)
        assertEquals(List(3) { firstStop.point.latitude }, fixes.take(3).map { it.latitude })
        assertEquals(List(3) { secondStop.point.latitude }, fixes.takeLast(3).map { it.latitude })
        assertTrue(fixes.all { fix -> fix.source == LocationSourceKind.SIMULATOR })
    }

    private fun stop(id: String, latitude: Double, longitude: Double) = BusStop(
        id = id,
        name = id,
        point = GeoPoint(latitude = latitude, longitude = longitude),
    )
}
