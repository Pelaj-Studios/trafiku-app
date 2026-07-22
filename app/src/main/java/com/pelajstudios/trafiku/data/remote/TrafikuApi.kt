package com.pelajstudios.trafiku.data.remote

import retrofit2.http.GET
import retrofit2.http.Path

interface TrafikuApi {
    @GET("api/bus-network/{operator}/snapshot")
    suspend fun getBusNetworkSnapshot(
        @Path("operator") operator: String,
    ): BusNetworkSnapshotDto
}
