@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.pelajstudios.trafiku.ui.advertisement

import android.graphics.ImageDecoder
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.ContentFrame
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.pelajstudios.trafiku.R
import com.pelajstudios.trafiku.data.advertisement.Advertisement
import com.pelajstudios.trafiku.ui.app.TrafikuUiState
import com.pelajstudios.trafiku.ui.app.trilingualStringResource
import com.pelajstudios.trafiku.ui.theme.TrafikuMutedText
import com.pelajstudios.trafiku.ui.theme.TrafikuRed
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
internal fun AdvertisementScreen(
    route: TrafikuUiState.RouteSelected,
    advertisement: Advertisement,
    onPlaybackEnded: () -> Unit,
    onMediaFailure: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        AdvertisementHeader(route)
        Box(
            modifier = Modifier
                .weight(1.0F)
                .fillMaxWidth()
                .padding(start = 28.dp, end = 28.dp, bottom = 28.dp)
                .background(Color.Black, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            when (advertisement) {
                is Advertisement.Image -> AdvertisementImage(
                    advertisement = advertisement,
                    onMediaFailure = onMediaFailure,
                )
                is Advertisement.Video -> AdvertisementVideo(
                    advertisement = advertisement,
                    onPlaybackEnded = onPlaybackEnded,
                    onMediaFailure = onMediaFailure,
                )
            }
        }
    }
}

@Composable
private fun AdvertisementHeader(route: TrafikuUiState.RouteSelected) {
    val currentStopName = route.currentStopIndex
        ?.let { index -> route.stops.getOrNull(index)?.name }
    val nextStopName = route.nextStopIndex
        ?.let { index -> route.stops.getOrNull(index)?.name }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(148.dp)
            .padding(horizontal = 38.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(width = 96.dp, height = 82.dp)
                .background(TrafikuRed, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = route.lineName,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineLarge,
                maxLines = 1,
            )
        }
        Spacer(modifier = Modifier.width(24.dp))
        Column(modifier = Modifier.width(220.dp)) {
            Text(
                text = trilingualStringResource(R.string.direction),
                color = TrafikuMutedText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    lineHeight = 17.sp,
                ),
            )
            Text(
                text = route.destinationName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.width(30.dp))
        Column(modifier = Modifier.weight(1.0F)) {
            Text(
                text = if (currentStopName != null) {
                    trilingualStringResource(R.string.current_stop)
                } else {
                    trilingualStringResource(R.string.next_stop_label)
                },
                color = TrafikuMutedText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    lineHeight = 17.sp,
                ),
            )
            Text(
                text = currentStopName ?: nextStopName
                ?: trilingualStringResource(R.string.route_complete),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        CurrentTime()
    }
}

@Composable
private fun CurrentTime() {
    val currentTime by produceState(initialValue = formatCurrentTime()) {
        while (true) {
            value = formatCurrentTime()
            delay(MINUTE_MILLIS - System.currentTimeMillis() % MINUTE_MILLIS)
        }
    }
    Text(
        text = currentTime,
        style = MaterialTheme.typography.titleLarge,
    )
}

@Composable
private fun AdvertisementImage(
    advertisement: Advertisement.Image,
    onMediaFailure: () -> Unit,
) {
    val context = LocalContext.current
    val imageState by produceState<DecodedImageState>(
        initialValue = DecodedImageState.Loading,
        key1 = advertisement.contentUri,
    ) {
        value = withContext(Dispatchers.IO) {
            try {
                val source = ImageDecoder.createSource(
                    context.contentResolver,
                    advertisement.contentUri.toUri(),
                )
                val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    val scale = minOf(
                        1.0F,
                        MAX_IMAGE_WIDTH.toFloat() / info.size.width,
                        MAX_IMAGE_HEIGHT.toFloat() / info.size.height,
                    )
                    if (scale < 1.0F) {
                        decoder.setTargetSize(
                            (info.size.width * scale).roundToInt(),
                            (info.size.height * scale).roundToInt(),
                        )
                    }
                }
                DecodedImageState.Ready(bitmap.asImageBitmap())
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                DecodedImageState.Failed
            }
        }
    }
    LaunchedEffect(imageState) {
        if (imageState == DecodedImageState.Failed) {
            onMediaFailure()
        }
    }
    val image = (imageState as? DecodedImageState.Ready)?.image ?: return
    Image(
        bitmap = image,
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Fit,
    )
}

@Composable
private fun AdvertisementVideo(
    advertisement: Advertisement.Video,
    onPlaybackEnded: () -> Unit,
    onMediaFailure: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentPlaybackEnded by rememberUpdatedState(onPlaybackEnded)
    val currentMediaFailure by rememberUpdatedState(onMediaFailure)
    val player = remember(context) {
        ExoPlayer.Builder(context).build().apply {
            volume = 0.0F
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    DisposableEffect(player, lifecycleOwner) {
        val playerListener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    currentPlaybackEnded()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                currentMediaFailure()
            }
        }
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> player.play()
                Lifecycle.Event.ON_STOP -> player.pause()
                else -> Unit
            }
        }
        player.addListener(playerListener)
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            player.removeListener(playerListener)
            player.release()
        }
    }

    LaunchedEffect(player, advertisement.contentUri) {
        try {
            player.setMediaItem(MediaItem.fromUri(advertisement.contentUri))
            player.prepare()
            player.playWhenReady = true
        } catch (exception: RuntimeException) {
            currentMediaFailure()
        }
    }

    ContentFrame(
        player = player,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Fit,
    )
}

private sealed interface DecodedImageState {
    data object Loading : DecodedImageState

    data class Ready(val image: androidx.compose.ui.graphics.ImageBitmap) : DecodedImageState

    data object Failed : DecodedImageState
}

private fun formatCurrentTime(): String = TIME_FORMATTER.format(Instant.now())

private const val MAX_IMAGE_WIDTH = 1920
private const val MAX_IMAGE_HEIGHT = 1080
private const val MINUTE_MILLIS = 60_000L

private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter
    .ofPattern("HH:mm")
    .withZone(ZoneId.systemDefault())
