@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.pelajstudios.trafiku.ui.route

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.pelajstudios.trafiku.R
import com.pelajstudios.trafiku.ui.app.trilingualStringResource
import com.pelajstudios.trafiku.ui.app.DirectionOption
import com.pelajstudios.trafiku.ui.app.LineOption
import com.pelajstudios.trafiku.ui.app.TrafikuUiState
import com.pelajstudios.trafiku.data.model.RouteDirection

@Composable
internal fun LineSelectionScreen(
    state: TrafikuUiState.SelectLine,
    onSelectLine: (String) -> Unit,
    onRefresh: () -> Unit,
) {
    SelectionLayout(
        title = trilingualStringResource(R.string.select_line),
        message = state.statusMessage(),
        options = state.lines,
        optionKey = LineOption::id,
        optionLabel = LineOption::name,
        onSelect = { option -> onSelectLine(option.id) },
        onRefresh = onRefresh,
    )
}

@Composable
internal fun DirectionSelectionScreen(
    state: TrafikuUiState.SelectDirection,
    onSelectDirection: (RouteDirection) -> Unit,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
) {
    BackHandler(onBack = onBack)
    SelectionLayout(
        title = trilingualStringResource(R.string.select_direction, state.lineName),
        message = state.statusMessage(),
        options = state.directions,
        optionKey = { option -> option.direction.name },
        optionLabel = DirectionOption::label,
        onSelect = { option -> onSelectDirection(option.direction) },
        onRefresh = onRefresh,
    )
}

@Composable
private fun <T> SelectionLayout(
    title: String,
    message: String?,
    options: List<T>,
    optionKey: (T) -> Any,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    onRefresh: () -> Unit,
) {
    val firstOptionFocus = remember { FocusRequester() }

    LaunchedEffect(options) {
        firstOptionFocus.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 72.dp, vertical = 52.dp)
            .widthIn(max = 920.dp),
    ) {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 24.sp,
                lineHeight = 28.sp,
            ),
        )
        StatusMessage(message)
        Spacer(modifier = Modifier.height(24.dp))

        if (options.isEmpty()) {
            Text(trilingualStringResource(R.string.no_options))
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRefresh,
                modifier = Modifier.focusRequester(firstOptionFocus),
            ) {
                Text(trilingualStringResource(R.string.download_again))
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                itemsIndexed(
                    items = options,
                    key = { _, option -> optionKey(option) },
                ) { index, option ->
                    Button(
                        onClick = { onSelect(option) },
                        modifier = Modifier
                            .then(
                                if (index == 0) {
                                    Modifier.focusRequester(firstOptionFocus)
                                } else {
                                    Modifier
                                },
                            )
                            .fillMaxWidth(),
                    ) {
                        Text(
                            text = optionLabel(option),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
