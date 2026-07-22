package com.pelajstudios.trafiku.ui.app

import androidx.annotation.StringRes
import com.pelajstudios.trafiku.data.model.RouteDirection

sealed interface TrafikuUiState {
    data object Loading : TrafikuUiState

    data class LoadFailed(@param:StringRes val messageResource: Int) : TrafikuUiState

    data class SelectLine(
        val lines: List<LineOption>,
        val isRefreshing: Boolean,
        @param:StringRes val messageResource: Int?,
    ) : TrafikuUiState

    data class SelectDirection(
        val lineName: String,
        val directions: List<DirectionOption>,
        val isRefreshing: Boolean,
        @param:StringRes val messageResource: Int?,
    ) : TrafikuUiState

    data class RouteSelected(
        val lineName: String,
        val destinationName: String,
        val stops: List<TrackedStopUi>,
        val currentStopIndex: Int?,
        val nextStopIndex: Int?,
        val gpsStatus: GpsUiStatus,
        val simulationSupported: Boolean,
        val simulationEnabled: Boolean,
        val isRefreshing: Boolean,
        @param:StringRes val messageResource: Int?,
    ) : TrafikuUiState
}

data class LineOption(
    val id: String,
    val name: String,
)

data class DirectionOption(
    val direction: RouteDirection,
    val label: String,
)

data class TrackedStopUi(
    val id: String,
    val name: String,
    val status: StopProgressUiStatus,
    val reachedTime: String?,
    val arrivalMinutes: Int?,
)

enum class StopProgressUiStatus {
    PASSED,
    CURRENT,
    UPCOMING,
}

enum class GpsSourceUi {
    SYSTEM,
    USB,
    SIMULATOR,
}

sealed interface GpsUiStatus {
    data class Acquiring(val source: GpsSourceUi) : GpsUiStatus

    data class Active(
        val source: GpsSourceUi,
        val accuracyMeters: Float,
    ) : GpsUiStatus

    data object LocationPermissionRequired : GpsUiStatus

    data class UsbPermissionRequired(val deviceName: String) : GpsUiStatus

    data object NoLocationSource : GpsUiStatus

    data object OffRoute : GpsUiStatus

    data class Error(val source: GpsSourceUi) : GpsUiStatus
}
