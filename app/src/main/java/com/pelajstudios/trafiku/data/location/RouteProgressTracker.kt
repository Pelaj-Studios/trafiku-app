package com.pelajstudios.trafiku.data.location

import com.pelajstudios.trafiku.data.model.BusRoute
import com.pelajstudios.trafiku.data.model.BusStop
import com.pelajstudios.trafiku.data.model.GeoPoint
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

internal enum class StopProgressStatus {
    PASSED,
    CURRENT,
    UPCOMING,
}

internal data class TrackedStop(
    val stop: BusStop,
    val status: StopProgressStatus,
    val reachedAtEpochMillis: Long?,
    val arrivalMinutes: Int?,
)

internal data class TrackedRouteState(
    val locationStatus: LocationStatus,
    val stops: List<TrackedStop>,
    val currentStopIndex: Int?,
    val nextStopIndex: Int?,
    val isOffRoute: Boolean,
)

internal fun trackRouteProgress(
    route: BusRoute,
    locations: Flow<LocationState>,
): Flow<TrackedRouteState> = flow {
    val tracker = RouteProgressTracker(route)
    var state = tracker.waitingState(
        locationStatus = LocationStatus.Acquiring(LocationSourceKind.SYSTEM),
    )
    emit(state)
    locations.collect { locationState ->
        state = locationState.fix?.let { fix ->
            tracker.update(
                fix = fix,
                locationStatus = locationState.status,
            )
        } ?: state.copy(locationStatus = locationState.status)
        emit(state)
    }
}.flowOn(Dispatchers.Default)

internal class RouteProgressTracker(
    private val route: BusRoute,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) {
    private val projector = RouteProjector(route.geometry)
    private val stopRouteDistances = route.stops.map { stop ->
        projector.project(stop.point, searchAllSegments = true).routeDistanceMeters
    }
    private val reachedTimes = mutableMapOf<Int, Long>()
    private val speedSamples = ArrayDeque<SpeedSample>()
    private var lastSegmentIndex: Int? = null
    private var maximumRouteDistance = 0.0
    private var lastPassedStopIndex = -1
    private var activeCurrentStopIndex: Int? = null
    private var offRouteFixCount = 0
    private var recoveryFixCount = 0
    private var isOffRoute = false
    private var smoothedSpeedMetersPerSecond: Double? = null
    private var lastAcceptedSpeed: SpeedSample? = null

    fun waitingState(locationStatus: LocationStatus): TrackedRouteState = TrackedRouteState(
        locationStatus = locationStatus,
        stops = route.stops.map { stop ->
            TrackedStop(
                stop = stop,
                status = StopProgressStatus.UPCOMING,
                reachedAtEpochMillis = null,
                arrivalMinutes = null,
            )
        },
        currentStopIndex = null,
        nextStopIndex = route.stops.indices.firstOrNull(),
        isOffRoute = false,
    )

    fun update(
        fix: LocationFix,
        locationStatus: LocationStatus,
    ): TrackedRouteState {
        val point = GeoPoint(latitude = fix.latitude, longitude = fix.longitude)
        var projection = projector.project(
            point = point,
            preferredSegmentIndex = lastSegmentIndex,
            searchAllSegments = lastSegmentIndex == null,
        )
        if (projection.distanceFromRouteMeters > OFF_ROUTE_ENTER_DISTANCE_METERS) {
            projection = projector.project(point, searchAllSegments = true)
        }
        lastSegmentIndex = projection.segmentIndex

        updateOffRouteState(projection.distanceFromRouteMeters)
        if (!isOffRoute) {
            maximumRouteDistance = max(maximumRouteDistance, projection.routeDistanceMeters)
            updateSpeed(fix)
        }

        val detectedCurrentStopIndex = if (isOffRoute) {
            null
        } else {
            findCurrentStop(point)
        }
        if (detectedCurrentStopIndex != null) {
            activeCurrentStopIndex = detectedCurrentStopIndex
        }
        updatePassedStops()
        val currentStopIndex = if (isOffRoute) null else activeCurrentStopIndex
        val nextStopIndex = when {
            currentStopIndex != null -> route.stops.indices.firstOrNull {
                it > currentStopIndex
            }
            else -> route.stops.indices.firstOrNull { it > lastPassedStopIndex }
        }
        val now = currentTimeMillis()
        if (currentStopIndex != null) {
            reachedTimes.putIfAbsent(currentStopIndex, now)
        }

        return TrackedRouteState(
            locationStatus = locationStatus,
            stops = route.stops.mapIndexed { index, stop ->
                val status = when {
                    index == currentStopIndex -> StopProgressStatus.CURRENT
                    index <= lastPassedStopIndex -> StopProgressStatus.PASSED
                    else -> StopProgressStatus.UPCOMING
                }
                TrackedStop(
                    stop = stop,
                    status = status,
                    reachedAtEpochMillis = reachedTimes[index],
                    arrivalMinutes = arrivalMinutes(index, status),
                )
            },
            currentStopIndex = currentStopIndex,
            nextStopIndex = nextStopIndex,
            isOffRoute = isOffRoute,
        )
    }

    private fun updateOffRouteState(distanceFromRouteMeters: Double) {
        if (isOffRoute) {
            recoveryFixCount = if (distanceFromRouteMeters < OFF_ROUTE_EXIT_DISTANCE_METERS) {
                recoveryFixCount + 1
            } else {
                0
            }
            if (recoveryFixCount >= REQUIRED_ROUTE_FIX_COUNT) {
                isOffRoute = false
                recoveryFixCount = 0
                offRouteFixCount = 0
            }
        } else {
            offRouteFixCount = if (distanceFromRouteMeters > OFF_ROUTE_ENTER_DISTANCE_METERS) {
                offRouteFixCount + 1
            } else {
                0
            }
            if (offRouteFixCount >= REQUIRED_ROUTE_FIX_COUNT) {
                isOffRoute = true
                offRouteFixCount = 0
                recoveryFixCount = 0
            }
        }
    }

    private fun findCurrentStop(point: GeoPoint): Int? = route.stops.indices
        .asSequence()
        .filter { index -> index > lastPassedStopIndex }
        .map { index -> index to distanceMeters(point, route.stops[index].point) }
        .filter { (_, distance) -> distance <= STOP_ARRIVAL_DISTANCE_METERS }
        .minByOrNull { (_, distance) -> distance }
        ?.first

    private fun updatePassedStops() {
        val currentStopIndex = activeCurrentStopIndex
        if (currentStopIndex != null) {
            lastPassedStopIndex = max(lastPassedStopIndex, currentStopIndex - 1)
            if (
                maximumRouteDistance >
                stopRouteDistances[currentStopIndex] + STOP_ARRIVAL_DISTANCE_METERS
            ) {
                lastPassedStopIndex = max(lastPassedStopIndex, currentStopIndex)
                reachedTimes.putIfAbsent(currentStopIndex, currentTimeMillis())
                activeCurrentStopIndex = null
            }
        }
        stopRouteDistances.forEachIndexed { index, stopRouteDistance ->
            if (
                index > lastPassedStopIndex &&
                index != activeCurrentStopIndex &&
                maximumRouteDistance > stopRouteDistance + STOP_ARRIVAL_DISTANCE_METERS
            ) {
                lastPassedStopIndex = index
                reachedTimes.putIfAbsent(index, currentTimeMillis())
            }
        }
    }

    private fun updateSpeed(fix: LocationFix) {
        val speed = fix.speedMetersPerSecond?.toDouble() ?: return
        if (speed !in MINIMUM_SPEED_METERS_PER_SECOND..MAXIMUM_SPEED_METERS_PER_SECOND) {
            return
        }
        val sample = SpeedSample(
            elapsedRealtimeMillis = fix.elapsedRealtimeMillis,
            metersPerSecond = speed,
        )
        val previous = lastAcceptedSpeed
        if (previous != null) {
            val elapsedSeconds = (sample.elapsedRealtimeMillis - previous.elapsedRealtimeMillis) /
                MILLIS_PER_SECOND
            if (elapsedSeconds > 0.0) {
                val acceleration = kotlin.math.abs(sample.metersPerSecond - previous.metersPerSecond) /
                    elapsedSeconds
                if (acceleration > MAXIMUM_ACCELERATION_METERS_PER_SECOND_SQUARED) {
                    return
                }
            }
        }

        lastAcceptedSpeed = sample
        speedSamples.addLast(sample)
        while (
            speedSamples.isNotEmpty() &&
            sample.elapsedRealtimeMillis - speedSamples.first().elapsedRealtimeMillis >
            SPEED_WINDOW_MILLIS
        ) {
            speedSamples.removeFirst()
        }
        if (speedSamples.size >= MINIMUM_SPEED_SAMPLE_COUNT) {
            smoothedSpeedMetersPerSecond = speedSamples.map(SpeedSample::metersPerSecond).average()
        }
    }

    private fun arrivalMinutes(index: Int, status: StopProgressStatus): Int? {
        if (isOffRoute || status != StopProgressStatus.UPCOMING) {
            return null
        }
        val speed = smoothedSpeedMetersPerSecond ?: return null
        val remainingDistance = stopRouteDistances[index] - maximumRouteDistance
        if (remainingDistance <= 0.0) {
            return null
        }
        return (remainingDistance / speed / SECONDS_PER_MINUTE)
            .roundToInt()
            .coerceAtLeast(1)
    }

    private data class SpeedSample(
        val elapsedRealtimeMillis: Long,
        val metersPerSecond: Double,
    )

    private companion object {
        const val STOP_ARRIVAL_DISTANCE_METERS = 20.0
        const val OFF_ROUTE_ENTER_DISTANCE_METERS = 60.0
        const val OFF_ROUTE_EXIT_DISTANCE_METERS = 40.0
        const val REQUIRED_ROUTE_FIX_COUNT = 3
        const val SPEED_WINDOW_MILLIS = 60_000L
        const val MINIMUM_SPEED_SAMPLE_COUNT = 3
        const val MINIMUM_SPEED_METERS_PER_SECOND = 1.0
        const val MAXIMUM_SPEED_METERS_PER_SECOND = 50.0
        const val MAXIMUM_ACCELERATION_METERS_PER_SECOND_SQUARED = 10.0
        const val MILLIS_PER_SECOND = 1_000.0
        const val SECONDS_PER_MINUTE = 60.0
    }
}

private class RouteProjector(
    private val points: List<GeoPoint>,
) {
    private val segmentLengths = points.zipWithNext(::distanceMeters)
    private val cumulativeDistances = DoubleArray(points.size)

    init {
        segmentLengths.forEachIndexed { index, length ->
            cumulativeDistances[index + 1] = cumulativeDistances[index] + length
        }
    }

    fun project(
        point: GeoPoint,
        preferredSegmentIndex: Int? = null,
        searchAllSegments: Boolean,
    ): RouteProjection {
        if (points.size < 2) {
            return RouteProjection(
                routeDistanceMeters = 0.0,
                distanceFromRouteMeters = Double.MAX_VALUE,
                segmentIndex = 0,
            )
        }

        val indices = if (searchAllSegments || preferredSegmentIndex == null) {
            0 until points.lastIndex
        } else {
            max(0, preferredSegmentIndex - SEGMENT_SEARCH_RADIUS)..
                minOf(points.lastIndex - 1, preferredSegmentIndex + SEGMENT_SEARCH_RADIUS)
        }
        return indices
            .map { segmentIndex -> projectOnSegment(point, segmentIndex) }
            .minBy(RouteProjection::distanceFromRouteMeters)
    }

    private fun projectOnSegment(point: GeoPoint, segmentIndex: Int): RouteProjection {
        val first = points[segmentIndex]
        val second = points[segmentIndex + 1]
        val latitudeRadians = point.latitude.toRadians()
        val firstX = (first.longitude - point.longitude).toRadians() *
            cos(latitudeRadians) * EARTH_RADIUS_METERS
        val firstY = (first.latitude - point.latitude).toRadians() * EARTH_RADIUS_METERS
        val secondX = (second.longitude - point.longitude).toRadians() *
            cos(latitudeRadians) * EARTH_RADIUS_METERS
        val secondY = (second.latitude - point.latitude).toRadians() * EARTH_RADIUS_METERS
        val segmentX = secondX - firstX
        val segmentY = secondY - firstY
        val segmentLengthSquared = segmentX * segmentX + segmentY * segmentY
        val fraction = if (segmentLengthSquared == 0.0) {
            0.0
        } else {
            (-(firstX * segmentX + firstY * segmentY) / segmentLengthSquared)
                .coerceIn(0.0, 1.0)
        }
        val projectedX = firstX + segmentX * fraction
        val projectedY = firstY + segmentY * fraction
        return RouteProjection(
            routeDistanceMeters = cumulativeDistances[segmentIndex] +
                segmentLengths[segmentIndex] * fraction,
            distanceFromRouteMeters = sqrt(projectedX * projectedX + projectedY * projectedY),
            segmentIndex = segmentIndex,
        )
    }

    private companion object {
        const val SEGMENT_SEARCH_RADIUS = 200
    }
}

private data class RouteProjection(
    val routeDistanceMeters: Double,
    val distanceFromRouteMeters: Double,
    val segmentIndex: Int,
)
