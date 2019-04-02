package com.proxyrack.android

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import com.google.gson.Gson
import com.proxyrack.network.createRetrofitClient
import com.proxyrack.network.getDeviceId
import com.proxyrack.network.model.Geolocation
import com.proxyrack.network.model.MessageToServer
import com.proxyrack.network.service.GeolocationService
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.android.schedulers.AndroidSchedulers.mainThread
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.schedulers.Schedulers.io
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.io.PrintWriter
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory


class ProxyService : Service() {

    private var socket: SSLSocket? = null
    private var readerStream: BufferedReader? = null
    private var writerStream: DataOutputStream? = null
    private val disposables = CompositeDisposable()

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {

        disposables.add(
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
                Toast.makeText(baseContext, t.localizedMessage, Toast.LENGTH_LONG).show()
            })
        )
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

        // Create and open a connection
        val socketFactory = SSLSocketFactory.getDefault() as SSLSocketFactory
        socket = socketFactory.createSocket(host, port) as SSLSocket

        // Prepare json body, indicating end of string with a special char
        val json = Gson().toJson(message) + "\\0"

        writerStream = DataOutputStream(socket!!.outputStream)
        readerStream = BufferedReader(InputStreamReader(socket!!.inputStream))

        // Send json message to server
        val writer = PrintWriter(writerStream!!)
        writer.println(json)
        writer.flush()

        // Server won't respond to hello message,
        // but we need to start listening for user-initiated connection requests
        disposables.add(
            Flowable.interval(1, TimeUnit.SECONDS)
                .subscribeOn(io())
                .observeOn(mainThread())
                .subscribe({

                    var line: String
                    while (readerStream!!.ready()) {
                        line = readerStream!!.readLine()
                        Log.d(ProxyService::class.java.simpleName, "Backconnect message: $line")
                    }

                }, { e -> throw e })
        )
    }
}
