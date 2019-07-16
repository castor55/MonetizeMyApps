package net.monetizemyapp.di

import net.monetizemyapp.Properties
import net.monetizemyapp.network.SocketTcpClient
import net.monetizemyapp.network.api.GeolocationService
import net.monetizemyapp.toolbox.extentions.loge
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.Socket
import java.net.SocketException
import javax.net.SocketFactory
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
        private val ENABLED_SOCKET_PROTOCOLS = arrayOf("TLSv1.2")

        private val sslSocketFactory by lazy {
            SSLSocketFactory.getDefault()
        }

        private val socketFactory by lazy {
            SocketFactory.getDefault()
        }

        fun getEnabledProtocols() = ENABLED_SOCKET_PROTOCOLS
        fun provideSSLSocketFactory() = sslSocketFactory
        fun provideSocketFactory() = socketFactory

        fun provideSSlSocketConnection(host: String, port: Int, protocols: Array<String>): Socket? = try {
            provideSSLSocketFactory().createSocket(host, port).apply {
                protocols.takeIf { it.isNotEmpty() }?.let {
                    (this as? SSLSocket)?.enabledProtocols = it
                }
            }
        } catch (e: SocketException) {
            loge(Properties.APP_TAG, e.message)
            null
        }

        fun provideSimpleSocketConnection(host: String, port: Int): Socket =
            provideSocketFactory().createSocket(host, port)
    }

    object TcpClient {
        private const val HOST = "monetizemyapp.net"
        private const val PORT = 443

        @ExperimentalStdlibApi
        fun provideServerTcpClient(): net.monetizemyapp.network.TcpClient? = Sockets.provideSSlSocketConnection(
            HOST, PORT, Sockets.getEnabledProtocols()
        )?.let {
            SocketTcpClient(it)
        }
    }

    object Api {
        private val locationApi by lazy { Network.provideIpApiRetrofit().create(GeolocationService::class.java) }

        fun provideLocationApi(): GeolocationService = locationApi
    }
}