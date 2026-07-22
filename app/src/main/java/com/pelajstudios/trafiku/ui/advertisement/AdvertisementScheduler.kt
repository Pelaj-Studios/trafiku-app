package com.pelajstudios.trafiku.ui.advertisement

import com.pelajstudios.trafiku.data.advertisement.Advertisement
import com.pelajstudios.trafiku.data.advertisement.AdvertisementRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

internal class AdvertisementScheduler(
    private val repository: AdvertisementRepository,
    private val scope: CoroutineScope,
    private val routeDurationMillis: Long = ROUTE_DURATION_MILLIS,
    private val imageDurationMillis: Long = IMAGE_DURATION_MILLIS,
) {
    private val mutableState = MutableStateFlow<AdvertisementUiState>(
        AdvertisementUiState.Inactive,
    )
    private var scheduleJob: Job? = null
    private var completion = CompletableDeferred<AdvertisementResult>()
    private var lastDisplayedId: String? = null

    val state: StateFlow<AdvertisementUiState> = mutableState

    fun setEnabled(enabled: Boolean) {
        if (enabled == (scheduleJob?.isActive == true)) {
            return
        }
        scheduleJob?.cancel()
        completion.cancel()
        mutableState.value = AdvertisementUiState.Inactive
        if (enabled) {
            scheduleJob = scope.launch { runSchedule() }
        } else {
            scheduleJob = null
        }
    }

    fun dismissAdvertisement() {
        completion.complete(AdvertisementResult.Displayed)
    }

    fun reportPlaybackEnded() {
        completion.complete(AdvertisementResult.Displayed)
    }

    fun reportMediaFailure() {
        completion.complete(AdvertisementResult.Failed)
    }

    private suspend fun runSchedule() {
        var waitForRoute = true
        val failedIds = mutableSetOf<String>()
        while (true) {
            mutableState.value = AdvertisementUiState.Waiting
            if (waitForRoute) {
                delay(routeDurationMillis)
            }

            val advertisements = repository.getAdvertisements()
            val advertisement = advertisements.nextAfter(lastDisplayedId, failedIds)
            if (advertisement == null) {
                failedIds.clear()
                waitForRoute = true
                continue
            }

            completion = CompletableDeferred()
            mutableState.value = AdvertisementUiState.Showing(advertisement)
            val result = when (advertisement) {
                is Advertisement.Image -> awaitImageCompletion()
                is Advertisement.Video -> completion.await()
            }
            if (result == AdvertisementResult.Failed) {
                failedIds += advertisement.id
                waitForRoute = false
            } else {
                lastDisplayedId = advertisement.id
                failedIds.clear()
                waitForRoute = true
            }
        }
    }

    private suspend fun awaitImageCompletion(): AdvertisementResult =
        withTimeoutOrNull(imageDurationMillis) { completion.await() }
            ?: AdvertisementResult.Displayed

    private fun List<Advertisement>.nextAfter(
        lastId: String?,
        excludedIds: Set<String>,
    ): Advertisement? {
        if (isEmpty()) {
            return null
        }
        val startIndex = indexOfFirst { advertisement -> advertisement.id == lastId }
            .takeIf { index -> index >= 0 }
            ?.let { index -> (index + 1) % size }
            ?: 0
        return indices
            .asSequence()
            .map { offset -> this[(startIndex + offset) % size] }
            .firstOrNull { advertisement -> advertisement.id !in excludedIds }
    }

    private enum class AdvertisementResult {
        Displayed,
        Failed,
    }

    private companion object {
        const val ROUTE_DURATION_MILLIS = 30_000L
        const val IMAGE_DURATION_MILLIS = 20_000L
    }
}

internal sealed interface AdvertisementUiState {
    data object Inactive : AdvertisementUiState

    data object Waiting : AdvertisementUiState

    data class Showing(val advertisement: Advertisement) : AdvertisementUiState
}
