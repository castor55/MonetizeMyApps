package com.proxyrack.android

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.google.gson.Gson
import com.proxyrack.network.model.Geolocation
import com.proxyrack.network.model.MessageToServer
import com.proxyrack.network.service.GeolocationService
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.PrintWriter
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

class ProxyService : Service() {

    private var socket: SSLSocket? = null

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    @SuppressLint("CheckResult")
    override fun onCreate() {

        getLocation()
            .map {
                val message = MessageToServer("hello", getDeviceId(), it.city, it.country)
                connectToServer(message)
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                Log.d(ProxyService::class.java.simpleName, "Finished")
            }, { t ->
                throw t
            })
    }

    override fun onDestroy() {
        // Cleanup
        if (socket != null) {
            socket!!.close()
        }
    }

    /**
     * Retrieves user location, used for registration on backconnect server later
     */
    private fun getLocation(): Single<Geolocation> {
        val geoClient = createRetrofitClient("http://ip-api.com/")
        val geoService = geoClient.create(GeolocationService::class.java)
        return geoService.location
    }

    /**
     * Connects to backconnect server via SSL
     */
    private fun connectToServer(message: MessageToServer) {

        val host = "68.183.109.83"
        val port = 443 // default secure port

        // Create and open a socket
        val socketFactory = SSLSocketFactory.getDefault() as SSLSocketFactory
        socket = socketFactory.createSocket(host, port) as SSLSocket

        // Prepare json body, indicating end of string with a special char
        val json = Gson().toJson(message) + "\\0"

        val readerStream = DataInputStream(socket!!.inputStream)
        val writerStream = DataOutputStream(socket!!.outputStream)

        // Send json message to server
        val writer = PrintWriter(writerStream)
        writer.println(json)
        writer.flush()

        // Cleanup writer
        writerStream.close()

        // TODO: Listen for server response and cleanup readerStream
    }
}
