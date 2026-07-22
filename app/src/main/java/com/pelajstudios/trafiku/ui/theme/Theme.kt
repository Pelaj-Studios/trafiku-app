package com.pelajstudios.trafiku.ui.theme

import androidx.compose.runtime.Composable
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.lightColorScheme

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TrafikuTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = TrafikuRed,
            onPrimary = TrafikuOnRed,
            background = TrafikuBackground,
            onBackground = TrafikuText,
            surface = TrafikuSurface,
            onSurface = TrafikuText,
            secondary = TrafikuMutedText,
            onSecondary = TrafikuSurface,
            error = TrafikuRed,
        ),
        typography = Typography,
        content = content,
    )
}
