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
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.io.*
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

class ProxyService : Service() {

    private var socket: SSLSocket? = null
    private var readerStream: InputStream? = null
    private var writerStream: OutputStream? = null
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

    /**
     * Connects to backconnect server via SSL
     */
    private fun connectToServer(message: MessageToServer) {

        val host = "monetizemyapp.net"
        val port = 8080 // default secure port

        // Create and open a connection
        socket = createSocket(host, port)

        // Prepare json body, indicating end of string with a special char
        val json = Gson().toJson(message) + "\\0"

        writerStream = BufferedOutputStream(socket!!.outputStream)
        readerStream = BufferedInputStream(socket!!.inputStream)

        // Send hello message to server
        writerStream!!.write(json.toByteArray())
        writerStream!!.flush()

        // Server won't respond to hello message,
        // but we need to start listening for user-initiated connection requests
        disposables.add(
            Flowable.interval(1, TimeUnit.SECONDS)
                .map {
                    // Check for response from server
                    val data = ByteArray(2048)
                    val len = readerStream!!.read(data)
                    if (len > 0) {
                        val response = String(data, 0, len)
                        Log.d(ProxyService::class.java.simpleName, response)
                    } else {
                        Log.d(ProxyService::class.java.simpleName, "No data received")
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
     * Retrieves user location, used for registration on backconnect server later
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
