package com.pelajstudios.trafiku.ui.app

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pelajstudios.trafiku.R
import com.pelajstudios.trafiku.data.location.LocationRepository
import com.pelajstudios.trafiku.data.location.LocationSourceKind
import com.pelajstudios.trafiku.data.location.LocationStatus
import com.pelajstudios.trafiku.data.location.StopProgressStatus
import com.pelajstudios.trafiku.data.location.TrackedRouteState
import com.pelajstudios.trafiku.data.location.trackRouteProgress
import com.pelajstudios.trafiku.data.model.BusLineSummary
import com.pelajstudios.trafiku.data.model.BusNetworkMetadata
import com.pelajstudios.trafiku.data.model.BusRoute
import com.pelajstudios.trafiku.data.model.BusRouteSummary
import com.pelajstudios.trafiku.data.model.BusSelection
import com.pelajstudios.trafiku.data.model.RouteDirection
import com.pelajstudios.trafiku.data.preferences.BusSelectionStore
import com.pelajstudios.trafiku.data.repository.BusNetworkSnapshotRepository
import com.pelajstudios.trafiku.data.repository.SnapshotRefreshResult
import com.pelajstudios.trafiku.di.TrafikuAppContainer
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class TrafikuViewModel internal constructor(
    private val busNetworkSnapshots: BusNetworkSnapshotRepository,
    private val busSelectionStore: BusSelectionStore,
    private val locationRepository: LocationRepository,
) : ViewModel() {
    private val metadata = busNetworkSnapshots.metadata.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null,
    )
    private val lines = busNetworkSnapshots.lines.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList(),
    )
    private val storedSelection = busSelectionStore.selection
        .map<BusSelection?, StoredSelectionState>(StoredSelectionState::Loaded)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = StoredSelectionState.Loading,
        )
    private val pendingLineId = MutableStateFlow<String?>(null)
    private val pendingLine = pendingLineId
        .flatMapLatest { lineId ->
            if (lineId == null) {
                flowOf(null)
            } else {
                busNetworkSnapshots.observeRoutes(lineId)
                    .map { routes -> PendingLine(lineId = lineId, routes = routes) }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null,
        )
    private val selectedRoute = storedSelection
        .flatMapLatest { selectionState -> observeSelectedRoute(selectionState) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = SelectedRouteState.Loading,
        )
    private val trackedRoute = selectedRoute
        .flatMapLatest { routeState -> observeTrackedRoute(routeState) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = RouteTrackingState.Loading,
        )
    private val refreshStatus = MutableStateFlow(RefreshStatus())
    private var refreshJob: Job? = null

    private val contentInputs = combine(
        metadata,
        lines,
        storedSelection,
        pendingLine,
        selectedRoute,
    ) { metadata, lines, selectionState, pendingLine, selectedRoute ->
        ContentInputs(
            metadata = metadata,
            lines = lines,
            selectionState = selectionState,
            pendingLine = pendingLine,
            selectedRoute = selectedRoute,
        )
    }

    val uiState: StateFlow<TrafikuUiState> = contentInputs
        .combine(trackedRoute, ::createContentState)
        .combine(refreshStatus, ::applyRefreshStatus)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = TrafikuUiState.Loading,
        )

    init {
        clearInvalidStoredSelection()
        refresh(force = false)
    }

    fun selectLine(lineId: String) {
        if (lines.value.any { line -> line.id == lineId }) {
            pendingLineId.value = lineId
        }
    }

    fun selectDirection(direction: RouteDirection) {
        val pendingLine = pendingLine.value ?: return
        if (pendingLine.routes.none { route -> route.direction == direction }) {
            return
        }

        viewModelScope.launch {
            busSelectionStore.save(
                BusSelection(
                    lineId = pendingLine.lineId,
                    direction = direction,
                ),
            )
            pendingLineId.value = null
        }
    }

    fun showLineSelection() {
        locationRepository.setSimulationEnabled(false)
        viewModelScope.launch {
            busSelectionStore.clear()
            pendingLineId.value = null
        }
    }

    fun showDirectionSelection() {
        pendingLineId.value = null
    }

    fun changeDirection() {
        val selection = (storedSelection.value as? StoredSelectionState.Loaded)?.selection ?: return
        locationRepository.setSimulationEnabled(false)
        viewModelScope.launch {
            busSelectionStore.clear()
            pendingLineId.value = selection.lineId
        }
    }

    fun retry() {
        refresh(force = false)
    }

    fun forceRefresh() {
        refresh(force = true)
    }

    fun notifyLocationPermissionChanged() {
        locationRepository.notifyLocationPermissionChanged()
    }

    fun requestUsbPermission() {
        locationRepository.requestUsbPermission()
    }

    fun toggleSimulation() {
        locationRepository.setSimulationEnabled(!locationRepository.simulationEnabled)
    }

    private fun observeSelectedRoute(
        selectionState: StoredSelectionState,
    ): Flow<SelectedRouteState> = when (selectionState) {
        StoredSelectionState.Loading -> flowOf(SelectedRouteState.Loading)
        is StoredSelectionState.Loaded -> {
            val selection = selectionState.selection
            if (selection == null) {
                flowOf(SelectedRouteState.Loaded(route = null))
            } else {
                busNetworkSnapshots.observeRoute(selection)
                    .map<BusRoute?, SelectedRouteState>(SelectedRouteState::Loaded)
                    .onStart { emit(SelectedRouteState.Loading) }
            }
        }
    }

    private fun observeTrackedRoute(routeState: SelectedRouteState): Flow<RouteTrackingState> {
        val route = (routeState as? SelectedRouteState.Loaded)?.route
            ?: return flowOf(RouteTrackingState.Loading)
        return trackRouteProgress(
            route = route,
            locations = locationRepository.observeLocation(route),
        ).map<TrackedRouteState, RouteTrackingState> { state ->
            RouteTrackingState.Loaded(
                routeId = route.id,
                state = state,
            )
        }.onStart {
            emit(RouteTrackingState.Loading)
        }
    }

    private fun refresh(force: Boolean) {
        if (refreshJob?.isActive == true) {
            return
        }

        refreshJob = viewModelScope.launch {
            refreshStatus.value = RefreshStatus(isRefreshing = true)
            refreshStatus.value = when (busNetworkSnapshots.refresh(force)) {
                SnapshotRefreshResult.Updated,
                SnapshotRefreshResult.SkippedFresh,
                -> RefreshStatus()

                is SnapshotRefreshResult.Failed -> RefreshStatus(
                    errorMessageResource = if (metadata.value == null) {
                        R.string.network_download_failed
                    } else {
                        R.string.network_update_failed
                    },
                )
            }
        }
    }

    private fun clearInvalidStoredSelection() {
        viewModelScope.launch {
            combine(
                metadata.filterNotNull(),
                storedSelection,
                selectedRoute,
            ) { _, selectionState, routeState ->
                selectionState is StoredSelectionState.Loaded &&
                    selectionState.selection != null &&
                    routeState is SelectedRouteState.Loaded &&
                    routeState.route == null
            }.distinctUntilChanged().collect { selectionIsInvalid ->
                if (selectionIsInvalid) {
                    busSelectionStore.clear()
                }
            }
        }
    }

    private fun createContentState(
        inputs: ContentInputs,
        trackingState: RouteTrackingState,
    ): TrafikuUiState {
        if (
            inputs.metadata == null ||
            inputs.selectionState == StoredSelectionState.Loading
        ) {
            return TrafikuUiState.Loading
        }

        val selection = (inputs.selectionState as StoredSelectionState.Loaded).selection
        if (selection != null) {
            val route = (inputs.selectedRoute as? SelectedRouteState.Loaded)?.route
                ?: return TrafikuUiState.Loading
            val tracking = (trackingState as? RouteTrackingState.Loaded)
                ?.takeIf { state -> state.routeId == route.id }
                ?.state
                ?: return TrafikuUiState.Loading
            val lineName = inputs.lines.firstOrNull { line -> line.id == selection.lineId }?.name
                ?: return TrafikuUiState.Loading
            return tracking.toUiState(
                lineName = lineName,
                route = route,
                simulationSupported = locationRepository.simulationSupported,
                simulationEnabled = locationRepository.simulationEnabled,
            )
        }

        val pendingLine = inputs.pendingLine
        if (pendingLine != null) {
            val lineName = inputs.lines.firstOrNull { line -> line.id == pendingLine.lineId }?.name
            if (lineName != null) {
                return TrafikuUiState.SelectDirection(
                    lineName = lineName,
                    directions = pendingLine.routes.map { route ->
                        DirectionOption(
                            direction = route.direction,
                            label = route.directionLabel,
                        )
                    },
                    isRefreshing = false,
                    messageResource = null,
                )
            }
        }

        return TrafikuUiState.SelectLine(
            lines = inputs.lines.map { line -> LineOption(id = line.id, name = line.name) },
            isRefreshing = false,
            messageResource = null,
        )
    }

    private fun TrackedRouteState.toUiState(
        lineName: String,
        route: BusRoute,
        simulationSupported: Boolean,
        simulationEnabled: Boolean,
    ): TrafikuUiState.RouteSelected {
        val gpsStatus = if (isOffRoute) {
            GpsUiStatus.OffRoute
        } else {
            locationStatus.toUiStatus()
        }
        return TrafikuUiState.RouteSelected(
            lineName = lineName,
            destinationName = route.stops.lastOrNull()?.name ?: route.direction.displayName,
            stops = stops.map { trackedStop ->
                TrackedStopUi(
                    id = trackedStop.stop.id,
                    name = trackedStop.stop.name,
                    status = when (trackedStop.status) {
                        StopProgressStatus.PASSED -> StopProgressUiStatus.PASSED
                        StopProgressStatus.CURRENT -> StopProgressUiStatus.CURRENT
                        StopProgressStatus.UPCOMING -> StopProgressUiStatus.UPCOMING
                    },
                    reachedTime = trackedStop.reachedAtEpochMillis?.let { timestamp ->
                        REACHED_TIME_FORMATTER.format(Instant.ofEpochMilli(timestamp))
                    },
                    arrivalMinutes = trackedStop.arrivalMinutes,
                )
            },
            currentStopIndex = currentStopIndex,
            nextStopIndex = nextStopIndex,
            gpsStatus = gpsStatus,
            simulationSupported = simulationSupported,
            simulationEnabled = simulationEnabled,
            isRefreshing = false,
            messageResource = null,
        )
    }

    private fun LocationStatus.toUiStatus(): GpsUiStatus = when (this) {
        is LocationStatus.Acquiring -> GpsUiStatus.Acquiring(source.toUiSource())
        is LocationStatus.Active -> GpsUiStatus.Active(
            source = source.toUiSource(),
            accuracyMeters = accuracyMeters,
        )
        LocationStatus.LocationPermissionRequired -> GpsUiStatus.LocationPermissionRequired
        is LocationStatus.UsbPermissionRequired -> GpsUiStatus.UsbPermissionRequired(deviceName)
        LocationStatus.NoLocationSource -> GpsUiStatus.NoLocationSource
        is LocationStatus.Error -> GpsUiStatus.Error(source.toUiSource())
    }

    private fun LocationSourceKind.toUiSource(): GpsSourceUi = when (this) {
        LocationSourceKind.SYSTEM -> GpsSourceUi.SYSTEM
        LocationSourceKind.USB -> GpsSourceUi.USB
        LocationSourceKind.SIMULATOR -> GpsSourceUi.SIMULATOR
    }

    private fun applyRefreshStatus(
        contentState: TrafikuUiState,
        refreshStatus: RefreshStatus,
    ): TrafikuUiState = when (contentState) {
        TrafikuUiState.Loading -> refreshStatus.errorMessageResource
            ?.let(TrafikuUiState::LoadFailed)
            ?: TrafikuUiState.Loading

        is TrafikuUiState.LoadFailed -> contentState
        is TrafikuUiState.SelectLine -> contentState.copy(
            isRefreshing = refreshStatus.isRefreshing,
            messageResource = refreshStatus.errorMessageResource,
        )
        is TrafikuUiState.SelectDirection -> contentState.copy(
            isRefreshing = refreshStatus.isRefreshing,
            messageResource = refreshStatus.errorMessageResource,
        )
        is TrafikuUiState.RouteSelected -> contentState.copy(
            isRefreshing = refreshStatus.isRefreshing,
            messageResource = refreshStatus.errorMessageResource,
        )
    }

    private sealed interface StoredSelectionState {
        data object Loading : StoredSelectionState

        data class Loaded(val selection: BusSelection?) : StoredSelectionState
    }

    private sealed interface SelectedRouteState {
        data object Loading : SelectedRouteState

        data class Loaded(val route: BusRoute?) : SelectedRouteState
    }

    private sealed interface RouteTrackingState {
        data object Loading : RouteTrackingState

        data class Loaded(
            val routeId: String,
            val state: TrackedRouteState,
        ) : RouteTrackingState
    }

    private data class PendingLine(
        val lineId: String,
        val routes: List<BusRouteSummary>,
    )

    private data class ContentInputs(
        val metadata: BusNetworkMetadata?,
        val lines: List<BusLineSummary>,
        val selectionState: StoredSelectionState,
        val pendingLine: PendingLine?,
        val selectedRoute: SelectedRouteState,
    )

    private data class RefreshStatus(
        val isRefreshing: Boolean = false,
        @param:StringRes val errorMessageResource: Int? = null,
    )

    class Factory(
        private val container: TrafikuAppContainer,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(TrafikuViewModel::class.java)) {
                "Unknown ViewModel class: ${modelClass.name}"
            }
            return requireNotNull(
                modelClass.cast(
                    TrafikuViewModel(
                        busNetworkSnapshots = container.busNetworkSnapshots,
                        busSelectionStore = container.busSelectionStore,
                        locationRepository = container.locationRepository,
                    ),
                ),
            )
        }
    }

    private companion object {
        val REACHED_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter
            .ofPattern("HH:mm")
            .withZone(ZoneId.systemDefault())
    }
}
