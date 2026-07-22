@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.pelajstudios.trafiku.ui.route

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pelajstudios.trafiku.ui.app.GpsSourceUi
import com.pelajstudios.trafiku.ui.app.GpsUiStatus
import com.pelajstudios.trafiku.ui.theme.TrafikuTheme

@Preview(widthDp = 440, showBackground = true)
@Composable
private fun GpsStatusBadgePreview() {
    TrafikuTheme {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            GpsStatusBadge(
                status = GpsUiStatus.Active(
                    source = GpsSourceUi.SYSTEM,
                    accuracyMeters = 7.0F,
                ),
            )
            GpsStatusBadge(status = GpsUiStatus.NoLocationSource)
        }
    }
}
