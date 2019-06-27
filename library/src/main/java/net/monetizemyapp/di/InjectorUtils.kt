package net.monetizemyapp.di

import net.monetizemyapp.network.api.GeolocationService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.Socket
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

object InjectorUtils {
    private object Network {
        private val ipApiRetrofit: Retrofit by lazy {
            Retrofit.Builder()
                .baseUrl("http://ip-api.com")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }

        fun provideIpApiRetrofit() = ipApiRetrofit
    }

    object Sockets {
        private val webSocketFactory by lazy {
            SSLSocketFactory.getDefault()
        }


        fun provideSocketFactory() = webSocketFactory
        fun provideSSlWebSocketConnection(url: String, port: Int, protocols: Array<String>): Socket =
            provideSocketFactory().createSocket(url, port).apply {
                (this as SSLSocket).enabledProtocols = protocols
            }

    }

    object Api {
        private val locationApi by lazy { Network.provideIpApiRetrofit().create(GeolocationService::class.java) }

        fun provideLocationApi(): GeolocationService = locationApi
    }
}