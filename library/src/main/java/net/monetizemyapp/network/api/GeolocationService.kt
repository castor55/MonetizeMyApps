package net.monetizemyapp.network.api

import net.monetizemyapp.network.model.response.IpApiResponse
import retrofit2.Response
import retrofit2.http.GET

interface GeolocationService {

    @GET("/json")
    suspend fun getLocation(): Response<IpApiResponse>
}