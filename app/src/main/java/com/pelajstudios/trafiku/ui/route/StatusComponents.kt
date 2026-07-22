@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.pelajstudios.trafiku.ui.route

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.pelajstudios.trafiku.R
import com.pelajstudios.trafiku.ui.app.GpsSourceUi
import com.pelajstudios.trafiku.ui.app.GpsUiStatus
import com.pelajstudios.trafiku.ui.app.TrafikuUiState
import com.pelajstudios.trafiku.ui.app.localizedStringArgument
import com.pelajstudios.trafiku.ui.app.trilingualStringResource
import com.pelajstudios.trafiku.ui.theme.TrafikuGreen
import com.pelajstudios.trafiku.ui.theme.TrafikuGreenBadgeBackground
import com.pelajstudios.trafiku.ui.theme.TrafikuGreenBadgeBorder
import com.pelajstudios.trafiku.ui.theme.TrafikuRed
import com.pelajstudios.trafiku.ui.theme.TrafikuRedBadgeBackground
import com.pelajstudios.trafiku.ui.theme.TrafikuRedBadgeBorder
import com.pelajstudios.trafiku.ui.theme.TrafikuSurface

@Composable
internal fun gpsStatusText(status: GpsUiStatus): String = when (status) {
    is GpsUiStatus.Acquiring -> gpsSourceResource(status.source).let { sourceResource ->
        trilingualStringResource(
            R.string.gps_acquiring,
            localizedStringArgument(sourceResource),
        )
    }
    is GpsUiStatus.Active -> gpsSourceResource(status.source).let { sourceResource ->
        trilingualStringResource(
            R.string.gps_active,
            localizedStringArgument(sourceResource),
            status.accuracyMeters,
        )
    }
    GpsUiStatus.LocationPermissionRequired ->
        trilingualStringResource(R.string.gps_permission_required)
    is GpsUiStatus.UsbPermissionRequired -> trilingualStringResource(
        R.string.gps_usb_permission_required,
        status.deviceName,
    )
    GpsUiStatus.NoLocationSource -> trilingualStringResource(R.string.gps_no_source)
    GpsUiStatus.OffRoute -> trilingualStringResource(R.string.off_route)
    is GpsUiStatus.Error -> gpsSourceResource(status.source).let { sourceResource ->
        trilingualStringResource(
            R.string.gps_error,
            localizedStringArgument(sourceResource),
        )
    }
}

private fun gpsSourceResource(source: GpsSourceUi): Int =
    when (source) {
        GpsSourceUi.SYSTEM -> R.string.gps_system
        GpsSourceUi.USB -> R.string.gps_usb
        GpsSourceUi.SIMULATOR -> R.string.gps_simulator
    }

@Composable
internal fun GpsStatusBadge(
    status: GpsUiStatus,
    modifier: Modifier = Modifier,
) {
    val activeSource = (status as? GpsUiStatus.Active)?.source
    val labelResource = when (activeSource) {
        GpsSourceUi.SYSTEM -> R.string.gps_badge_device
        GpsSourceUi.USB -> R.string.gps_badge_usb
        GpsSourceUi.SIMULATOR -> R.string.gps_badge_simulator
        null -> R.string.gps_badge_unavailable
    }
    val shape = RoundedCornerShape(6.dp)
    val foregroundColor = if (activeSource != null) TrafikuGreen else TrafikuRed
    val backgroundColor = if (activeSource != null) {
        TrafikuGreenBadgeBackground
    } else {
        TrafikuRedBadgeBackground
    }
    val borderColor = if (activeSource != null) {
        TrafikuGreenBadgeBorder
    } else {
        TrafikuRedBadgeBorder
    }
    Text(
        text = trilingualStringResource(labelResource),
        color = foregroundColor,
        fontWeight = FontWeight.SemiBold,
        style = MaterialTheme.typography.bodySmall,
        modifier = modifier
            .wrapContentWidth()
            .background(
                color = backgroundColor,
                shape = shape,
            )
            .border(width = 1.dp, color = borderColor, shape = shape)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

@Composable
internal fun ErrorScreen(message: String, onRetry: () -> Unit) {
    val retryFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        retryFocus.requestFocus()
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = message, style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onRetry,
            modifier = Modifier.focusRequester(retryFocus),
        ) {
            Text(trilingualStringResource(R.string.try_again))
        }
    }
}

@Composable
internal fun MessageScreen(message: String) {
    Text(text = message, style = MaterialTheme.typography.titleLarge)
}

@Composable
internal fun StatusMessage(message: String?) {
    if (message != null) {
        Text(
            text = message,
            color = TrafikuRed,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .fillMaxWidth()
                .background(TrafikuSurface)
                .padding(horizontal = 64.dp, vertical = 8.dp),
        )
    }
}

@Composable
internal fun TrafikuUiState.SelectLine.statusMessage(): String? =
    messageResource?.let { resourceId -> trilingualStringResource(resourceId) }
        ?: if (isRefreshing) trilingualStringResource(R.string.downloading_network) else null

@Composable
internal fun TrafikuUiState.SelectDirection.statusMessage(): String? =
    messageResource?.let { resourceId -> trilingualStringResource(resourceId) }
        ?: if (isRefreshing) trilingualStringResource(R.string.downloading_network) else null

@Composable
internal fun TrafikuUiState.RouteSelected.statusMessage(): String? =
    messageResource?.let { resourceId -> trilingualStringResource(resourceId) }
        ?: if (isRefreshing) trilingualStringResource(R.string.downloading_network) else null
