@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.pelajstudios.trafiku.ui.route

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.pelajstudios.trafiku.R
import com.pelajstudios.trafiku.ui.app.StopProgressUiStatus
import com.pelajstudios.trafiku.ui.app.TrackedStopUi
import com.pelajstudios.trafiku.ui.app.trilingualStringResource
import com.pelajstudios.trafiku.ui.theme.TrafikuMutedText
import com.pelajstudios.trafiku.ui.theme.TrafikuNextBackground
import com.pelajstudios.trafiku.ui.theme.TrafikuPast
import com.pelajstudios.trafiku.ui.theme.TrafikuRed
import com.pelajstudios.trafiku.ui.theme.TrafikuSurface

@Composable
internal fun StopTimeline(
    stops: List<TrackedStopUi>,
    currentStopIndex: Int?,
    nextStopIndex: Int?,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val anchorIndex = currentStopIndex ?: nextStopIndex ?: 0
    LaunchedEffect(anchorIndex, stops.size) {
        if (stops.isNotEmpty()) {
            listState.animateScrollToItem((anchorIndex - 2).coerceAtLeast(0))
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 86.dp),
    ) {
        itemsIndexed(
            items = stops,
            key = { _, stop -> stop.id },
        ) { index, stop ->
            StopRow(
                stop = stop,
                isFirst = index == 0,
                isLast = index == stops.lastIndex,
                isNext = index == nextStopIndex,
            )
        }
    }
}

@Composable
private fun StopRow(
    stop: TrackedStopUi,
    isFirst: Boolean,
    isLast: Boolean,
    isNext: Boolean,
) {
    val textColor = if (stop.status == StopProgressUiStatus.PASSED) {
        TrafikuMutedText
    } else {
        MaterialTheme.colorScheme.onBackground
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp)
            .background(
                color = if (isNext) TrafikuNextBackground else Color.Transparent,
                shape = RoundedCornerShape(12.dp),
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StopMarker(
            status = stop.status,
            isFirst = isFirst,
            isLast = isLast,
        )
        Text(
            text = stop.name,
            color = textColor,
            fontWeight = if (
                stop.status == StopProgressUiStatus.CURRENT || isNext
            ) {
                FontWeight.Bold
            } else {
                FontWeight.Normal
            },
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1.0F),
        )
        StopTime(stop = stop, isNext = isNext)
        Spacer(modifier = Modifier.width(28.dp))
    }
}

@Composable
private fun StopMarker(
    status: StopProgressUiStatus,
    isFirst: Boolean,
    isLast: Boolean,
) {
    Canvas(
        modifier = Modifier
            .width(92.dp)
            .fillMaxHeight(),
    ) {
        val centerX = size.width / 2.0F
        val centerY = size.height / 2.0F
        val center = Offset(centerX, centerY)
        val aboveColor = if (status == StopProgressUiStatus.UPCOMING) {
            TrafikuRed
        } else {
            TrafikuPast
        }
        val belowColor = if (status == StopProgressUiStatus.PASSED) {
            TrafikuPast
        } else {
            TrafikuRed
        }
        if (!isFirst) {
            drawLine(
                color = aboveColor,
                start = Offset(centerX, 0.0F),
                end = center,
                strokeWidth = 6.dp.toPx(),
            )
        }
        if (!isLast) {
            drawLine(
                color = belowColor,
                start = center,
                end = Offset(centerX, size.height),
                strokeWidth = 6.dp.toPx(),
            )
        }
        when (status) {
            StopProgressUiStatus.PASSED -> {
                drawCircle(
                    color = TrafikuSurface,
                    radius = 11.dp.toPx(),
                    center = center,
                )
                drawCircle(
                    color = TrafikuPast,
                    radius = 11.dp.toPx(),
                    center = center,
                    style = Stroke(width = 5.dp.toPx()),
                )
            }

            StopProgressUiStatus.CURRENT -> {
                drawCircle(
                    color = TrafikuSurface,
                    radius = 18.dp.toPx(),
                    center = center,
                )
                drawCircle(
                    color = TrafikuRed,
                    radius = 17.dp.toPx(),
                    center = center,
                    style = Stroke(width = 4.dp.toPx()),
                )
                drawCircle(
                    color = TrafikuRed,
                    radius = 9.dp.toPx(),
                    center = center,
                )
            }

            StopProgressUiStatus.UPCOMING -> drawCircle(
                color = TrafikuRed,
                radius = 10.dp.toPx(),
                center = center,
            )
        }
    }
}

@Composable
private fun StopTime(stop: TrackedStopUi, isNext: Boolean) {
    val text = when (stop.status) {
        StopProgressUiStatus.PASSED -> stop.reachedTime?.let { time -> "$time  ✓" } ?: "✓"
        StopProgressUiStatus.CURRENT -> trilingualStringResource(R.string.now)
        StopProgressUiStatus.UPCOMING -> stop.arrivalMinutes
            ?.let { minutes -> trilingualStringResource(R.string.minutes, minutes) }
            ?: "—"
    }
    Text(
        text = text,
        color = if (stop.status == StopProgressUiStatus.CURRENT || isNext) {
            TrafikuRed
        } else {
            TrafikuMutedText
        },
        fontWeight = if (stop.status == StopProgressUiStatus.CURRENT || isNext) {
            FontWeight.Bold
        } else {
            FontWeight.Normal
        },
        style = if (stop.status == StopProgressUiStatus.CURRENT) {
            MaterialTheme.typography.bodyMedium.copy(
                fontSize = 17.sp,
                lineHeight = 20.sp,
            )
        } else {
            MaterialTheme.typography.bodyLarge
        },
        modifier = Modifier.width(152.dp),
    )
}
