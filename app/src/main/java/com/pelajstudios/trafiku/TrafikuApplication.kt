package com.pelajstudios.trafiku

import android.app.Application
import com.pelajstudios.trafiku.di.TrafikuAppContainer

class TrafikuApplication : Application() {
    val container: TrafikuAppContainer by lazy {
        TrafikuAppContainer(applicationContext)
    }
}
