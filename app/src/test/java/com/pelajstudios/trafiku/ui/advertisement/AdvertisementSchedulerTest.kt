package com.pelajstudios.trafiku.ui.advertisement

import com.pelajstudios.trafiku.data.advertisement.Advertisement
import com.pelajstudios.trafiku.data.advertisement.AdvertisementRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AdvertisementSchedulerTest {
    @Test
    fun imageUsesRouteAndImageDurationsBeforeShowingNextFile() = runTest {
        val first = image("01-first.jpg")
        val second = image("02-second.jpg")
        val scheduler = scheduler(first, second)

        scheduler.setEnabled(true)
        runCurrent()
        assertEquals(AdvertisementUiState.Waiting, scheduler.state.value)

        advanceTimeBy(30_000L)
        runCurrent()
        assertEquals(AdvertisementUiState.Showing(first), scheduler.state.value)

        advanceTimeBy(20_000L)
        runCurrent()
        assertEquals(AdvertisementUiState.Waiting, scheduler.state.value)

        advanceTimeBy(30_000L)
        runCurrent()
        assertEquals(AdvertisementUiState.Showing(second), scheduler.state.value)
        scheduler.setEnabled(false)
    }

    @Test
    fun videoRemainsVisibleUntilPlaybackEnds() = runTest {
        val video = video("01-video.mp4")
        val scheduler = scheduler(video)

        scheduler.setEnabled(true)
        advanceTimeBy(30_000L)
        runCurrent()
        assertEquals(AdvertisementUiState.Showing(video), scheduler.state.value)

        advanceTimeBy(120_000L)
        runCurrent()
        assertEquals(AdvertisementUiState.Showing(video), scheduler.state.value)

        scheduler.reportPlaybackEnded()
        runCurrent()
        assertEquals(AdvertisementUiState.Waiting, scheduler.state.value)
        scheduler.setEnabled(false)
    }

    @Test
    fun failedFileIsSkippedWithoutAnotherRouteInterval() = runTest {
        val failed = image("01-failed.jpg")
        val valid = image("02-valid.jpg")
        val scheduler = scheduler(failed, valid)

        scheduler.setEnabled(true)
        advanceTimeBy(30_000L)
        runCurrent()
        assertEquals(AdvertisementUiState.Showing(failed), scheduler.state.value)

        scheduler.reportMediaFailure()
        runCurrent()
        assertEquals(AdvertisementUiState.Showing(valid), scheduler.state.value)
        scheduler.setEnabled(false)
    }

    private fun kotlinx.coroutines.test.TestScope.scheduler(
        vararg advertisements: Advertisement,
    ): AdvertisementScheduler = AdvertisementScheduler(
        repository = FakeAdvertisementRepository(advertisements.toList()),
        scope = this,
    )

    private fun image(name: String) = Advertisement.Image(
        id = name,
        displayName = name,
        contentUri = "content://images/$name",
    )

    private fun video(name: String) = Advertisement.Video(
        id = name,
        displayName = name,
        contentUri = "content://videos/$name",
    )

    private class FakeAdvertisementRepository(
        private val advertisements: List<Advertisement>,
    ) : AdvertisementRepository {
        override suspend fun getAdvertisements(): List<Advertisement> = advertisements
    }
}
