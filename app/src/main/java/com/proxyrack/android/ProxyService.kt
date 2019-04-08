package com.proxyrack.android

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import com.google.gson.Gson
import com.proxyrack.network.*
import com.proxyrack.network.model.*
import com.proxyrack.network.service.GeolocationService
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

class ProxyService : Service() {

    private var socket: SSLSocket? = null
    private var readerStream: DataInputStream? = null
    private var writerStream: DataOutputStream? = null
    private val disposables = CompositeDisposable()

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {

        disposables.add(
            getLocation()
                .map {
                    val message = HelloMessage(
                        body = HelloMessageBody(
                            getDeviceId(), it.city, it.countryCode, getSystemInfo()
                        )
                    )
                    connectToServer(message)
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({}, { t ->
                    Toast.makeText(baseContext, t.localizedMessage, Toast.LENGTH_LONG).show()
                })
        )
    }

    /**
     * Connects to backconnect server via SSL
     */
    private fun connectToServer(helloMessage: HelloMessage) {

        val host = "monetizemyapp.net"
        val port = 443

        // Create and open a connection
        socket = createSocket(host, port)
        writerStream = DataOutputStream(socket!!.outputStream)
        readerStream = DataInputStream(socket!!.inputStream)

        sendToServer(helloMessage)

        // Server won't respond to hello message,
        // but we need to start listening for user-initiated connection requests
        disposables.add(
            Flowable.interval(10, TimeUnit.SECONDS)
                .map {
                    val response = readerStream!!.getResponse()
                    when {
                        response.contains("backconnect") -> {
                            val helloResponse = Gson().fromJson<HelloResponse>(response, HelloResponse::class.java)
                            val token = helloResponse.body.token

                            sendToServer(ConnectMessage(body = ConnectMessageBody(token)))
                        }
                        response.contains("ping") -> {
                            sendToServer(PongMessage())
                            // TODO: If no message is received for last 10 minutes, client should assume connection is dead and reconnect.
                        }
                        else -> Log.d(ProxyService::class.java.simpleName, response)
                    }
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ }, { e -> throw e })
        )
    }

    @Throws(IOException::class)
    fun createSocket(host: String, port: Int): SSLSocket {

        val socket = SSLSocketFactory.getDefault()
            .createSocket(host, port) as SSLSocket
        socket.enabledProtocols = arrayOf("TLSv1.2")
        return socket
    }

    /**
     * Sends specified message object to server in JSON format
     */
    private fun sendToServer(message: Any) {
        // Prepare message body, indicating end of string with a special char
        val body = Gson().toJson(message) + endOfString()

        // Send message to server immediately
        writerStream!!.write(body.toByteArray())
        writerStream!!.flush()
    }

    /**
     * Retrieves user location, which is used for registration on backconnect server
     */
    private fun getLocation(): Single<Geolocation> {
        val geoClient = createRetrofitClient("http://ip-api.com/")
        val geoService = geoClient.create(GeolocationService::class.java)
        return geoService.location
    }

    override fun onDestroy() {
        // Cleanup
        readerStream?.close()
        writerStream?.close()

        if (socket != null) {
            socket!!.close()
        }
        disposables.clear()
    }
}