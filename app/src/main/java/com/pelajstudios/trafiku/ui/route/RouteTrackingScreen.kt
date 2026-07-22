@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.pelajstudios.trafiku.ui.route

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.pelajstudios.trafiku.R
import com.pelajstudios.trafiku.ui.advertisement.AdvertisementScreen
import com.pelajstudios.trafiku.ui.advertisement.AdvertisementUiState
import com.pelajstudios.trafiku.ui.app.TrafikuUiState
import com.pelajstudios.trafiku.ui.app.trilingualStringResource
import com.pelajstudios.trafiku.ui.theme.TrafikuMutedText
import com.pelajstudios.trafiku.ui.theme.TrafikuRed
import com.pelajstudios.trafiku.ui.theme.TrafikuSurface

@Composable
internal fun RouteTrackingScreen(
    state: TrafikuUiState.RouteSelected,
    advertisementState: AdvertisementUiState,
    advertisementSchedulingAllowed: Boolean,
    onChangeLine: () -> Unit,
    onChangeDirection: () -> Unit,
    onRefresh: () -> Unit,
    onRequestLocationPermission: () -> Unit,
    onRequestUsbPermission: () -> Unit,
    onToggleSimulation: () -> Unit,
    onAdvertisementAvailabilityChanged: (Boolean) -> Unit,
    onDismissAdvertisement: () -> Unit,
    onAdvertisementEnded: () -> Unit,
    onAdvertisementFailed: () -> Unit,
    onExit: () -> Unit,
) {
    var showActions by rememberSaveable { mutableStateOf(false) }
    var showGpsDetails by rememberSaveable { mutableStateOf(false) }
    var showExitConfirmation by rememberSaveable { mutableStateOf(false) }
    val screenFocus = remember { FocusRequester() }

    LaunchedEffect(
        advertisementSchedulingAllowed,
        showActions,
        showGpsDetails,
        showExitConfirmation,
    ) {
        onAdvertisementAvailabilityChanged(
            advertisementSchedulingAllowed &&
                !showActions &&
                !showGpsDetails &&
                !showExitConfirmation,
        )
    }
    DisposableEffect(Unit) {
        onDispose { onAdvertisementAvailabilityChanged(false) }
    }

    val advertisement = (advertisementState as? AdvertisementUiState.Showing)?.advertisement
    if (advertisement != null) {
        BackHandler(onBack = onDismissAdvertisement)
        AdvertisementScreen(
            route = state,
            advertisement = advertisement,
            onPlaybackEnded = onAdvertisementEnded,
            onMediaFailure = onAdvertisementFailed,
        )
        return
    }

    BackHandler {
        when {
            showExitConfirmation -> showExitConfirmation = false
            showGpsDetails -> showGpsDetails = false
            showActions -> showActions = false
            else -> showExitConfirmation = true
        }
    }
    LaunchedEffect(showActions, showGpsDetails, showExitConfirmation) {
        if (!showActions && !showGpsDetails && !showExitConfirmation) {
            screenFocus.requestFocus()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event ->
                if (
                    event.type == KeyEventType.KeyUp &&
                    (event.key == Key.DirectionCenter || event.key == Key.Enter) &&
                    !showActions &&
                    !showGpsDetails &&
                    !showExitConfirmation
                ) {
                    showActions = true
                    true
                } else {
                    false
                }
            }
            .focusRequester(screenFocus)
            .focusable(),
    ) {
        RouteHeader(state)
        StatusMessage(state.statusMessage())
        StopTimeline(
            stops = state.stops,
            currentStopIndex = state.currentStopIndex,
            nextStopIndex = state.nextStopIndex,
            modifier = Modifier.weight(1.0F),
        )
        RouteFooter(state)
    }

    if (showActions) {
        ActionPanel(
            state = state,
            onChangeLine = onChangeLine,
            onChangeDirection = onChangeDirection,
            onRefresh = onRefresh,
            onShowGpsDetails = {
                showActions = false
                showGpsDetails = true
            },
            onClose = { showActions = false },
        )
    }
    if (showGpsDetails) {
        GpsPanel(
            state = state,
            onRequestLocationPermission = onRequestLocationPermission,
            onRequestUsbPermission = onRequestUsbPermission,
            onToggleSimulation = onToggleSimulation,
            onClose = { showGpsDetails = false },
        )
    }
    if (showExitConfirmation) {
        ExitConfirmation(
            onExit = onExit,
            onCancel = { showExitConfirmation = false },
        )
    }
}

@Composable
private fun RouteHeader(state: TrafikuUiState.RouteSelected) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 52.dp, end = 64.dp, top = 28.dp, bottom = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(width = 158.dp, height = 104.dp)
                .background(TrafikuRed, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = state.lineName,
                color = Color.White,
                style = MaterialTheme.typography.headlineLarge,
                maxLines = 1,
            )
        }
        Spacer(modifier = Modifier.width(38.dp))
        Column(modifier = Modifier.weight(1.0F)) {
            Text(
                text = trilingualStringResource(R.string.direction),
                color = TrafikuMutedText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 15.sp,
                    lineHeight = 18.sp,
                ),
            )
            Text(
                text = state.destinationName,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun RouteFooter(state: TrafikuUiState.RouteSelected) {
    val nextStopName = state.nextStopIndex?.let { index -> state.stops.getOrNull(index)?.name }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp)
            .background(TrafikuSurface)
            .padding(horizontal = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "›",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.width(28.dp))
        Column(modifier = Modifier.weight(1.0F)) {
            if (nextStopName != null) {
                Text(
                    text = trilingualStringResource(R.string.next_stop_label),
                    color = TrafikuMutedText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        lineHeight = 17.sp,
                    ),
                )
                Text(
                    text = nextStopName,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                Text(
                    text = trilingualStringResource(R.string.route_complete),
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 21.sp),
                )
            }
        }
    }
}
