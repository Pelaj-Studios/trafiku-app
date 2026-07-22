@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.pelajstudios.trafiku.ui.route

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.pelajstudios.trafiku.R
import com.pelajstudios.trafiku.ui.app.GpsUiStatus
import com.pelajstudios.trafiku.ui.app.TrafikuUiState
import com.pelajstudios.trafiku.ui.app.trilingualStringResource
import com.pelajstudios.trafiku.ui.theme.TrafikuMutedText
import com.pelajstudios.trafiku.ui.theme.TrafikuPanelActionBackground
import com.pelajstudios.trafiku.ui.theme.TrafikuRed
import com.pelajstudios.trafiku.ui.theme.TrafikuSurface
import com.pelajstudios.trafiku.ui.theme.TrafikuText

@Composable
internal fun ActionPanel(
    state: TrafikuUiState.RouteSelected,
    onChangeLine: () -> Unit,
    onChangeDirection: () -> Unit,
    onRefresh: () -> Unit,
    onShowGpsDetails: () -> Unit,
    onClose: () -> Unit,
) {
    OverlayPanel(title = trilingualStringResource(R.string.actions), onClose = onClose) { firstFocus ->
        GpsStatusBadge(status = state.gpsStatus)
        PanelActionButton(
            text = trilingualStringResource(R.string.change_line),
            onClick = onChangeLine,
            modifier = Modifier
                .focusRequester(firstFocus)
                .fillMaxWidth(),
        )
        PanelActionButton(
            text = trilingualStringResource(R.string.change_direction),
            onClick = onChangeDirection,
            modifier = Modifier.fillMaxWidth(),
        )
        PanelActionButton(
            text = trilingualStringResource(R.string.refresh_data),
            onClick = onRefresh,
            enabled = !state.isRefreshing,
            modifier = Modifier.fillMaxWidth(),
        )
        PanelActionButton(
            text = trilingualStringResource(R.string.gps_status),
            onClick = onShowGpsDetails,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
internal fun GpsPanel(
    state: TrafikuUiState.RouteSelected,
    onRequestLocationPermission: () -> Unit,
    onRequestUsbPermission: () -> Unit,
    onToggleSimulation: () -> Unit,
    onClose: () -> Unit,
) {
    OverlayPanel(
        title = trilingualStringResource(R.string.gps_status),
        onClose = onClose,
    ) { firstFocus ->
        PanelActionButton(
            text = trilingualStringResource(R.string.close),
            onClick = onClose,
            modifier = Modifier
                .focusRequester(firstFocus)
                .fillMaxWidth(),
        )
        if (state.gpsStatus == GpsUiStatus.LocationPermissionRequired) {
            PanelActionButton(
                text = trilingualStringResource(R.string.allow_location),
                onClick = onRequestLocationPermission,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (state.gpsStatus is GpsUiStatus.UsbPermissionRequired) {
            PanelActionButton(
                text = trilingualStringResource(R.string.allow_usb),
                onClick = onRequestUsbPermission,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (state.simulationSupported) {
            PanelActionButton(
                text = if (state.simulationEnabled) {
                    trilingualStringResource(R.string.stop_simulator)
                } else {
                    trilingualStringResource(R.string.start_simulator)
                },
                onClick = onToggleSimulation,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun OverlayPanel(
    title: String,
    onClose: () -> Unit,
    content: @Composable ColumnScope.(FocusRequester) -> Unit,
) {
    val firstFocus = remember { FocusRequester() }
    BackHandler(onBack = onClose)
    LaunchedEffect(Unit) {
        firstFocus.requestFocus()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.28F)),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(440.dp)
                .background(TrafikuSurface),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PanelTitle(title)
                Spacer(modifier = Modifier.height(4.dp))
                content(firstFocus)
            }
        }
    }
}

@Composable
internal fun ExitConfirmation(onExit: () -> Unit, onCancel: () -> Unit) {
    val cancelFocus = remember { FocusRequester() }
    BackHandler(onBack = onCancel)
    LaunchedEffect(Unit) {
        cancelFocus.requestFocus()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.28F)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(520.dp)
                .background(TrafikuSurface, RoundedCornerShape(18.dp)),
        ) {
            Column(
                modifier = Modifier.padding(38.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Text(
                    text = trilingualStringResource(R.string.exit_title),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = onCancel,
                        modifier = Modifier.focusRequester(cancelFocus),
                    ) {
                        PanelButtonLabel(trilingualStringResource(R.string.cancel))
                    }
                    Button(onClick = onExit) {
                        PanelButtonLabel(trilingualStringResource(R.string.exit))
                    }
                }
            }
        }
    }
}

@Composable
private fun PanelActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(10.dp)
    Button(
        onClick = onClick,
        modifier = modifier.height(76.dp),
        enabled = enabled,
        scale = ButtonDefaults.scale(
            focusedScale = 1.0F,
            pressedScale = 0.98F,
        ),
        shape = ButtonDefaults.shape(
            shape = shape,
            focusedShape = shape,
            pressedShape = shape,
            disabledShape = shape,
        ),
        colors = ButtonDefaults.colors(
            containerColor = TrafikuPanelActionBackground,
            contentColor = TrafikuText,
            focusedContainerColor = TrafikuRed,
            focusedContentColor = Color.White,
            pressedContainerColor = TrafikuRed,
            pressedContentColor = Color.White,
            disabledContainerColor = TrafikuPanelActionBackground.copy(alpha = 0.45F),
            disabledContentColor = TrafikuMutedText.copy(alpha = 0.55F),
        ),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
    ) {
        PanelButtonLabel(
            text = text,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PanelTitle(text: String) {
    val lines = remember(text) { text.lines().filter(String::isNotBlank) }
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        lines.forEachIndexed { index, line ->
            Text(
                text = line,
                color = if (index == 0) TrafikuText else TrafikuMutedText,
                fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = if (index == 0) 23.sp else 16.sp,
                    lineHeight = if (index == 0) 27.sp else 19.sp,
                ),
            )
        }
    }
}

@Composable
private fun PanelButtonLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    val lines = remember(text) { text.lines().filter(String::isNotBlank) }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        lines.forEachIndexed { index, line ->
            Text(
                text = line,
                fontWeight = if (index == 0) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = if (index == 0) 16.sp else 14.sp,
                    lineHeight = if (index == 0) 20.sp else 17.sp,
                ),
            )
        }
    }
}
