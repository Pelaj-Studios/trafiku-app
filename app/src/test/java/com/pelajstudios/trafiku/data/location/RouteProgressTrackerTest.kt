package com.pelajstudios.trafiku.data.location

import com.pelajstudios.trafiku.data.model.BusRoute
import com.pelajstudios.trafiku.data.model.BusStop
import com.pelajstudios.trafiku.data.model.GeoPoint
import com.pelajstudios.trafiku.data.model.RouteDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteProgressTrackerTest {
    private val route = BusRoute(
        id = "route",
        geometry = listOf(
            point(0.0000, 0.0),
            point(0.0010, 0.0),
            point(0.0020, 0.0),
            point(0.0030, 0.0),
        ),
        stops = listOf(
            stop("A", 0.0000),
            stop("B", 0.0010),
            stop("C", 0.0020),
            stop("D", 0.0030),
        ),
        direction = RouteDirection.OUTBOUND,
    )

    @Test
    fun stopIsCurrentInsideTwentyMeters() {
        val tracker = RouteProgressTracker(route)

        val outside = tracker.update(fix(0.0, 0.00023, elapsed = 1_000L), activeStatus())
        val inside = tracker.update(fix(0.0, 0.00017, elapsed = 2_000L), activeStatus())

        assertNull(outside.currentStopIndex)
        assertEquals(0, inside.currentStopIndex)
        assertEquals(1, inside.nextStopIndex)
    }

    @Test
    fun progressDoesNotMoveBackwardAfterGpsNoise() {
        val tracker = RouteProgressTracker(route)

        val atThirdStop = tracker.update(fix(0.0020, 0.0, elapsed = 1_000L), activeStatus())
        val noisyOlderPosition = tracker.update(fix(0.0010, 0.0, elapsed = 2_000L), activeStatus())

        assertEquals(2, atThirdStop.currentStopIndex)
        assertEquals(2, noisyOlderPosition.currentStopIndex)
        assertEquals(StopProgressStatus.PASSED, noisyOlderPosition.stops[1].status)
    }

    @Test
    fun threeFixesControlOffRouteAndRecoveryHysteresis() {
        val tracker = RouteProgressTracker(route)
        tracker.update(fix(0.0010, 0.0, elapsed = 1_000L), activeStatus())

        tracker.update(fix(0.0010, 0.0010, elapsed = 2_000L), activeStatus())
        tracker.update(fix(0.0010, 0.0010, elapsed = 3_000L), activeStatus())
        val offRoute = tracker.update(fix(0.0010, 0.0010, elapsed = 4_000L), activeStatus())
        assertTrue(offRoute.isOffRoute)
        assertTrue(offRoute.stops.all { stop -> stop.arrivalMinutes == null })

        tracker.update(fix(0.0010, 0.0, elapsed = 5_000L), activeStatus())
        tracker.update(fix(0.0010, 0.0, elapsed = 6_000L), activeStatus())
        val recovered = tracker.update(fix(0.0010, 0.0, elapsed = 7_000L), activeStatus())
        assertFalse(recovered.isOffRoute)
    }

    @Test
    fun threeSpeedSamplesProduceArrivalEstimate() {
        val tracker = RouteProgressTracker(route)

        tracker.update(fix(0.0, 0.0, elapsed = 1_000L, speed = 8.0F), activeStatus())
        tracker.update(fix(0.0, 0.0, elapsed = 2_000L, speed = 8.0F), activeStatus())
        val state = tracker.update(
            fix(0.0, 0.0, elapsed = 3_000L, speed = 8.0F),
            activeStatus(),
        )

        assertEquals(1, state.stops[1].arrivalMinutes)
        assertEquals(1, state.stops[3].arrivalMinutes)
    }

    private fun activeStatus() = LocationStatus.Active(
        source = LocationSourceKind.SIMULATOR,
        accuracyMeters = 5.0F,
    )

    private fun fix(
        latitude: Double,
        longitude: Double,
        elapsed: Long,
        speed: Float = 8.0F,
    ) = LocationFix(
        latitude = latitude,
        longitude = longitude,
        accuracyMeters = 5.0F,
        speedMetersPerSecond = speed,
        elapsedRealtimeMillis = elapsed,
        source = LocationSourceKind.SIMULATOR,
    )

    private fun stop(name: String, latitude: Double) = BusStop(
        id = name,
        name = name,
        point = point(latitude, 0.0),
    )

    private fun point(latitude: Double, longitude: Double) = GeoPoint(
        latitude = latitude,
        longitude = longitude,
    )
}
