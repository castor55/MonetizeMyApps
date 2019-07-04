package net.monetizemyapp.di

import android.net.SSLCertificateSocketFactory
import net.monetizemyapp.network.SocketTcpClient
import net.monetizemyapp.network.api.GeolocationService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.Socket
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
        private val sslSocketFactory by lazy {
            SSLSocketFactory.getDefault()
            SSLCertificateSocketFactory.getDefault()
        }

        private val socketFactory by lazy {
            SocketFactory.getDefault()
        }


        fun provideSSLSocketFactory() = sslSocketFactory
        fun provideSocketFactory() = socketFactory

        fun provideSSlSocketConnection(host: String, port: Int, protocols: Array<String>): Socket =
            provideSSLSocketFactory().createSocket(host, port).apply {
                protocols.takeIf { it.isNotEmpty() }?.let {
                    (this as SSLSocket).enabledProtocols = protocols
                }
            }

        fun provideSocketConnection(host: String, port: Int): Socket = provideSocketFactory().createSocket(host, port)
    }

    object TcpClient {
        private const val HOST = "monetizemyapp.net"
        private const val PORT = 443
        private val ENABLED_SOCKET_PROTOCOLS = arrayOf("TLSv1.2")

        @ExperimentalStdlibApi
        fun provideServerTcpClient(): net.monetizemyapp.network.TcpClient = provideNewSSLTcpClient(HOST, PORT)

        @ExperimentalStdlibApi
        fun provideNewSSLTcpClient(
            host: String,
            port: Int
        ): net.monetizemyapp.network.TcpClient = SocketTcpClient(Sockets.provideSSlSocketConnection(host, port, ENABLED_SOCKET_PROTOCOLS))

        @ExperimentalStdlibApi
        fun provideNewTcpClient(
            host: String,
            port: Int
        ): net.monetizemyapp.network.TcpClient = SocketTcpClient(Sockets.provideSocketConnection(host, port))
    }

    object Api {
        private val locationApi by lazy { Network.provideIpApiRetrofit().create(GeolocationService::class.java) }

        fun provideLocationApi(): GeolocationService = locationApi
    }
}