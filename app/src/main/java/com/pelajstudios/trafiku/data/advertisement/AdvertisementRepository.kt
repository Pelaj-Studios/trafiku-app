package com.pelajstudios.trafiku.data.advertisement

interface AdvertisementRepository {
    suspend fun getAdvertisements(): List<Advertisement>
}
