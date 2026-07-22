package com.pelajstudios.trafiku

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import com.pelajstudios.trafiku.ui.advertisement.AdvertisementViewModel
import com.pelajstudios.trafiku.ui.app.TrafikuScreen
import com.pelajstudios.trafiku.ui.app.TrafikuViewModel
import com.pelajstudios.trafiku.ui.app.TrilingualStrings
import com.pelajstudios.trafiku.ui.theme.TrafikuTheme

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val application = application as TrafikuApplication
            val viewModel: TrafikuViewModel = viewModel(
                factory = TrafikuViewModel.Factory(application.container),
            )
            val advertisementViewModel: AdvertisementViewModel = viewModel(
                factory = AdvertisementViewModel.Factory(application.container),
            )
            TrafikuTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RectangleShape,
                ) {
                    TrilingualStrings {
                        TrafikuScreen(
                            viewModel = viewModel,
                            advertisementViewModel = advertisementViewModel,
                            onExit = ::finish,
                        )
                    }
                }
            }
        }
    }
}
