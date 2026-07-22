@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.pelajstudios.trafiku.ui.route

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.pelajstudios.trafiku.ui.advertisement.AdvertisementUiState
import com.pelajstudios.trafiku.ui.app.GpsSourceUi
import com.pelajstudios.trafiku.ui.app.GpsUiStatus
import com.pelajstudios.trafiku.ui.app.StopProgressUiStatus
import com.pelajstudios.trafiku.ui.app.TrackedStopUi
import com.pelajstudios.trafiku.ui.app.TrafikuUiState
import com.pelajstudios.trafiku.ui.theme.TrafikuTheme

@Preview(widthDp = 1280, heightDp = 720, showBackground = true)
@Composable
private fun RouteTrackingPreview() {
    TrafikuTheme {
        RouteTrackingScreen(
            state = TrafikuUiState.RouteSelected(
                lineName = "3",
                destinationName = "Ulpianë",
                stops = listOf(
                    TrackedStopUi("1", "Bregu i Diellit", StopProgressUiStatus.PASSED, "10:12", null),
                    TrackedStopUi("2", "Biblioteka", StopProgressUiStatus.PASSED, "10:15", null),
                    TrackedStopUi(
                        "3",
                        "Stacioni i Autobusëve",
                        StopProgressUiStatus.CURRENT,
                        null,
                        null,
                    ),
                    TrackedStopUi("4", "Qendra", StopProgressUiStatus.UPCOMING, null, 2),
                    TrackedStopUi("5", "Dardani", StopProgressUiStatus.UPCOMING, null, 5),
                    TrackedStopUi("6", "Santea", StopProgressUiStatus.UPCOMING, null, 8),
                    TrackedStopUi("7", "Ulpianë", StopProgressUiStatus.UPCOMING, null, 12),
                ),
                currentStopIndex = 2,
                nextStopIndex = 3,
                gpsStatus = GpsUiStatus.Active(GpsSourceUi.SYSTEM, 7.0F),
                simulationSupported = true,
                simulationEnabled = false,
                isRefreshing = false,
                messageResource = null,
            ),
            advertisementState = AdvertisementUiState.Waiting,
            advertisementSchedulingAllowed = true,
            onChangeLine = {},
            onChangeDirection = {},
            onRefresh = {},
            onRequestLocationPermission = {},
            onRequestUsbPermission = {},
            onToggleSimulation = {},
            onAdvertisementAvailabilityChanged = {},
            onDismissAdvertisement = {},
            onAdvertisementEnded = {},
            onAdvertisementFailed = {},
            onExit = {},
        )
    }
}
