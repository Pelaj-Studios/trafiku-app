package com.pelajstudios.trafiku.data.advertisement

internal class LocalAdvertisementRepository(
    private val dataSource: MediaStoreAdvertisementDataSource,
) : AdvertisementRepository {
    override suspend fun getAdvertisements(): List<Advertisement> =
        dataSource.getAdvertisements()
}
