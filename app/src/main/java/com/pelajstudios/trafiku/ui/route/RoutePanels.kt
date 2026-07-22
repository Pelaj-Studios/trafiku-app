@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.pelajstudios.trafiku.ui.route

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.pelajstudios.trafiku.R
import com.pelajstudios.trafiku.ui.app.GpsUiStatus
import com.pelajstudios.trafiku.ui.app.trilingualStringResource
import com.pelajstudios.trafiku.ui.app.TrafikuUiState
import com.pelajstudios.trafiku.ui.theme.TrafikuSurface

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
        Button(
            onClick = onChangeLine,
            modifier = Modifier
                .focusRequester(firstFocus)
                .fillMaxWidth(),
        ) {
            PanelButtonLabel(trilingualStringResource(R.string.change_line))
        }
        Button(onClick = onChangeDirection, modifier = Modifier.fillMaxWidth()) {
            PanelButtonLabel(trilingualStringResource(R.string.change_direction))
        }
        Button(
            onClick = onRefresh,
            enabled = !state.isRefreshing,
            modifier = Modifier.fillMaxWidth(),
        ) {
            PanelButtonLabel(trilingualStringResource(R.string.refresh_data))
        }
        Button(onClick = onShowGpsDetails, modifier = Modifier.fillMaxWidth()) {
            PanelButtonLabel(trilingualStringResource(R.string.gps_status))
        }
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
        Button(
            onClick = onClose,
            modifier = Modifier
                .focusRequester(firstFocus)
                .fillMaxWidth(),
        ) {
            PanelButtonLabel(trilingualStringResource(R.string.close))
        }
        if (state.gpsStatus == GpsUiStatus.LocationPermissionRequired) {
            Button(
                onClick = onRequestLocationPermission,
                modifier = Modifier.fillMaxWidth(),
            ) {
                PanelButtonLabel(trilingualStringResource(R.string.allow_location))
            }
        }
        if (state.gpsStatus is GpsUiStatus.UsbPermissionRequired) {
            Button(onClick = onRequestUsbPermission, modifier = Modifier.fillMaxWidth()) {
                PanelButtonLabel(trilingualStringResource(R.string.allow_usb))
            }
        }
        if (state.simulationSupported) {
            Button(onClick = onToggleSimulation, modifier = Modifier.fillMaxWidth()) {
                PanelButtonLabel(
                    if (state.simulationEnabled) {
                        trilingualStringResource(R.string.stop_simulator)
                    } else {
                        trilingualStringResource(R.string.start_simulator)
                    },
                )
            }
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
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 20.sp,
                        lineHeight = 23.sp,
                    ),
                )
                Spacer(modifier = Modifier.height(2.dp))
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
private fun PanelButtonLabel(text: String) {
    Text(
        text = text,
        maxLines = 3,
        style = MaterialTheme.typography.labelLarge.copy(
            fontSize = 16.sp,
            lineHeight = 19.sp,
        ),
    )
}
