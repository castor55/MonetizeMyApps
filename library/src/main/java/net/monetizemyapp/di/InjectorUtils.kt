package net.monetizemyapp.di

import com.neovisionaries.ws.client.WebSocketFactory
import net.monetizemyapp.network.api.GeolocationService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.net.ssl.SSLContext

object InjectorUtils {
    private object Network {
        private val ipApiRetrofit: Retrofit by lazy {
            Retrofit.Builder()
                .baseUrl("http://ip-api.com")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }

        private val webSocketFactory by lazy {  WebSocketFactory().apply {
            sslContext = SSLContext.getInstance("TLSv1.2")
        }}


        fun provideIpApiRetrofit() = ipApiRetrofit


    }

    object Api {
        private val locationApi by lazy { Network.provideIpApiRetrofit().create(GeolocationService::class.java) }

        fun provideLocationApi(): GeolocationService = locationApi
    }
}