@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.pelajstudios.trafiku.ui.app

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pelajstudios.trafiku.R
import com.pelajstudios.trafiku.ui.advertisement.AdvertisementViewModel
import com.pelajstudios.trafiku.ui.route.DirectionSelectionScreen
import com.pelajstudios.trafiku.ui.route.ErrorScreen
import com.pelajstudios.trafiku.ui.route.LineSelectionScreen
import com.pelajstudios.trafiku.ui.route.MessageScreen
import com.pelajstudios.trafiku.ui.route.RouteTrackingScreen

@Composable
fun TrafikuScreen(
    viewModel: TrafikuViewModel,
    advertisementViewModel: AdvertisementViewModel,
    modifier: Modifier = Modifier,
    onExit: () -> Unit = {},
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    val advertisementState = advertisementViewModel.uiState
        .collectAsStateWithLifecycle()
        .value
    var automaticPermissionsRequestDone by rememberSaveable { mutableStateOf(false) }
    var automaticUsbRequestDevice by rememberSaveable { mutableStateOf<String?>(null) }
    var permissionsRequestInProgress by remember { mutableStateOf(false) }
    val permissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        permissionsRequestInProgress = false
        viewModel.notifyLocationPermissionChanged()
    }
    val requestLocationPermission = {
        permissionsRequestInProgress = true
        permissionsLauncher.launch(REQUIRED_RUNTIME_PERMISSIONS)
    }
    val gpsStatus = (uiState as? TrafikuUiState.RouteSelected)?.gpsStatus

    LaunchedEffect(gpsStatus) {
        if (
            uiState is TrafikuUiState.RouteSelected &&
            !automaticPermissionsRequestDone
        ) {
            automaticPermissionsRequestDone = true
            permissionsRequestInProgress = true
            permissionsLauncher.launch(REQUIRED_RUNTIME_PERMISSIONS)
        }
        val usbPermission = gpsStatus as? GpsUiStatus.UsbPermissionRequired
        if (
            usbPermission != null &&
            automaticUsbRequestDevice != usbPermission.deviceName
        ) {
            automaticUsbRequestDevice = usbPermission.deviceName
            viewModel.requestUsbPermission()
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        when (uiState) {
            TrafikuUiState.Loading -> MessageScreen(
                trilingualStringResource(R.string.loading_network),
            )
            is TrafikuUiState.LoadFailed -> ErrorScreen(
                message = trilingualStringResource(uiState.messageResource),
                onRetry = viewModel::retry,
            )

            is TrafikuUiState.SelectLine -> LineSelectionScreen(
                state = uiState,
                onSelectLine = viewModel::selectLine,
                onRefresh = viewModel::forceRefresh,
            )

            is TrafikuUiState.SelectDirection -> DirectionSelectionScreen(
                state = uiState,
                onSelectDirection = viewModel::selectDirection,
                onBack = viewModel::showDirectionSelection,
                onRefresh = viewModel::forceRefresh,
            )

            is TrafikuUiState.RouteSelected -> RouteTrackingScreen(
                state = uiState,
                advertisementState = advertisementState,
                advertisementSchedulingAllowed = !permissionsRequestInProgress,
                onChangeLine = viewModel::showLineSelection,
                onChangeDirection = viewModel::changeDirection,
                onRefresh = viewModel::forceRefresh,
                onRequestLocationPermission = requestLocationPermission,
                onRequestUsbPermission = viewModel::requestUsbPermission,
                onToggleSimulation = viewModel::toggleSimulation,
                onAdvertisementAvailabilityChanged =
                    advertisementViewModel::setRouteScreenAvailable,
                onDismissAdvertisement = advertisementViewModel::dismissAdvertisement,
                onAdvertisementEnded = advertisementViewModel::reportPlaybackEnded,
                onAdvertisementFailed = advertisementViewModel::reportMediaFailure,
                onExit = onExit,
            )
        }
    }
}

private val REQUIRED_RUNTIME_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.READ_MEDIA_IMAGES,
    Manifest.permission.READ_MEDIA_VIDEO,
    Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
)
