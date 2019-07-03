package net.monetizemyapp.di

import net.monetizemyapp.network.SocketTcpClient
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
                protocols.takeIf { it.isNotEmpty() }?.let {
                    (this as SSLSocket).enabledProtocols = protocols
                }
            }
    }

    object TcpClient {
        private const val HOST = "monetizemyapp.net"
        private const val PORT = 443
        private val ENABLED_SOCKET_PROTOCOLS = arrayOf("TLSv1.2")

        @ExperimentalStdlibApi
        fun provideServerTcpClient(): net.monetizemyapp.network.TcpClient = Sockets.provideSSlWebSocketConnection(
            HOST, PORT,
            ENABLED_SOCKET_PROTOCOLS
        ).let {
            SocketTcpClient(it)
        }
    }

    object Api {
        private val locationApi by lazy { Network.provideIpApiRetrofit().create(GeolocationService::class.java) }

        fun provideLocationApi(): GeolocationService = locationApi
    }
}