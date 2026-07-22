package com.pelajstudios.trafiku.ui.advertisement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pelajstudios.trafiku.data.advertisement.AdvertisementRepository
import com.pelajstudios.trafiku.di.TrafikuAppContainer
import kotlinx.coroutines.flow.StateFlow

class AdvertisementViewModel internal constructor(
    repository: AdvertisementRepository,
) : ViewModel() {
    private val scheduler = AdvertisementScheduler(
        repository = repository,
        scope = viewModelScope,
    )

    internal val uiState: StateFlow<AdvertisementUiState> = scheduler.state

    fun setRouteScreenAvailable(available: Boolean) {
        scheduler.setEnabled(available)
    }

    fun dismissAdvertisement() {
        scheduler.dismissAdvertisement()
    }

    fun reportPlaybackEnded() {
        scheduler.reportPlaybackEnded()
    }

    fun reportMediaFailure() {
        scheduler.reportMediaFailure()
    }

    class Factory(
        private val container: TrafikuAppContainer,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(AdvertisementViewModel::class.java)) {
                "Unknown ViewModel class: ${modelClass.name}"
            }
            return requireNotNull(
                modelClass.cast(
                    AdvertisementViewModel(container.advertisementRepository),
                ),
            )
        }
    }
}
