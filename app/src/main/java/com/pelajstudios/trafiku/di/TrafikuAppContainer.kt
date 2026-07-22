package com.pelajstudios.trafiku.di

import android.content.Context
import android.hardware.usb.UsbManager
import android.location.LocationManager
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.pelajstudios.trafiku.BuildConfig
import com.pelajstudios.trafiku.data.advertisement.AdvertisementRepository
import com.pelajstudios.trafiku.data.advertisement.LocalAdvertisementRepository
import com.pelajstudios.trafiku.data.advertisement.MediaStoreAdvertisementDataSource
import com.pelajstudios.trafiku.data.location.DefaultLocationRepository
import com.pelajstudios.trafiku.data.location.LocationRepository
import com.pelajstudios.trafiku.data.location.SimulatedLocationSource
import com.pelajstudios.trafiku.data.location.SystemLocationSource
import com.pelajstudios.trafiku.data.location.UsbNmeaLocationSource
import com.pelajstudios.trafiku.data.local.TrafikuDatabase
import com.pelajstudios.trafiku.data.preferences.BusSelectionStore
import com.pelajstudios.trafiku.data.remote.TrafikuApi
import com.pelajstudios.trafiku.data.repository.BusNetworkSnapshotRepository
import com.pelajstudios.trafiku.data.repository.OfflineFirstBusNetworkSnapshotRepository
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

private val Context.busSelectionDataStore by preferencesDataStore(name = "bus_selection")

class TrafikuAppContainer(context: Context) {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    private val database = Room.databaseBuilder(
        context = context,
        klass = TrafikuDatabase::class.java,
        name = "trafiku-network.db",
    ).build()

    private val trafikuApi = Retrofit.Builder()
        .baseUrl(BuildConfig.TRAFIKU_SERVER_BASE_URL)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(TrafikuApi::class.java)

    val busNetworkSnapshots: BusNetworkSnapshotRepository =
        OfflineFirstBusNetworkSnapshotRepository(
            snapshotDao = database.busNetworkSnapshotDao(),
            trafikuApi = trafikuApi,
        )

    val busSelectionStore = BusSelectionStore(context.busSelectionDataStore)

    internal val advertisementRepository: AdvertisementRepository = LocalAdvertisementRepository(
        dataSource = MediaStoreAdvertisementDataSource(context.contentResolver),
    )

    internal val locationRepository: LocationRepository = DefaultLocationRepository(
        systemLocationSource = SystemLocationSource(
            context = context,
            locationManager = context.getSystemService(LocationManager::class.java),
        ),
        usbLocationSource = UsbNmeaLocationSource(
            context = context,
            usbManager = context.getSystemService(UsbManager::class.java),
        ),
        simulatedLocationSource = SimulatedLocationSource(),
        simulationSupported = BuildConfig.DEBUG,
    )
}
