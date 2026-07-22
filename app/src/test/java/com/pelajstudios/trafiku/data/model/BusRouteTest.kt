package com.pelajstudios.trafiku.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class BusRouteTest {
    @Test
    fun directionLabelUsesFirstAndLastStopInSequence() {
        val route = BusRoute(
            id = "route-id",
            geometry = emptyList(),
            stops = listOf(
                stop(id = "first", name = "Central Station"),
                stop(id = "middle", name = "City Hall"),
                stop(id = "last", name = "University"),
            ),
            direction = RouteDirection.OUTBOUND,
        )

        assertEquals("Central Station → University", route.directionLabel)
    }

    @Test
    fun directionLabelFallsBackToDirectionWhenStopsAreMissing() {
        val route = BusRoute(
            id = "route-id",
            geometry = emptyList(),
            stops = emptyList(),
            direction = RouteDirection.INBOUND,
        )

        assertEquals("Kthim", route.directionLabel)
    }

    private fun stop(id: String, name: String) = BusStop(
        id = id,
        name = name,
        point = GeoPoint(latitude = 0.0, longitude = 0.0),
    )
}
