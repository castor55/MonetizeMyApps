package com.proxyrack.android

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.widget.Toast
import com.google.gson.Gson
import com.proxyrack.network.*
import com.proxyrack.network.model.base.ClientMessage
import com.proxyrack.network.model.base.ServerMessage
import com.proxyrack.network.model.base.ServerMessageEmpty
import com.proxyrack.network.model.base.ServerMessageType
import com.proxyrack.network.model.step0.Pong
import com.proxyrack.network.model.step1.Geolocation
import com.proxyrack.network.model.step1.Hello
import com.proxyrack.network.model.step1.HelloBody
import com.proxyrack.network.model.step2.Backconnect
import com.proxyrack.network.model.step2.Connect
import com.proxyrack.network.model.step2.ConnectBody
import com.proxyrack.network.service.GeolocationService
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers.mainThread
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers.io
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

// TODO: If no message is received for last 10 minutes, client should assume connection is dead and reconnect.
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
                    val message = Hello(HelloBody(getDeviceId(), it.city, it.countryCode, getSystemInfo()))
                    sendToServer(message)
                    waitForResponse() as Backconnect
                }
                .map {
                    val message = Connect(ConnectBody(it.body.token))
                    sendToServer(message)
                    waitForResponse()
                }
                .map {
                    // TODO: Start socks5 proxy and funnel traffic to server
                }
                .subscribeOn(io())
                .observeOn(mainThread())
                .subscribe({}, { t ->
                    Toast.makeText(baseContext, t.localizedMessage, Toast.LENGTH_LONG).show()
                    t.printStackTrace()
                })
        )
    }

    /**
     * Connects to backconnect server via SSL
     */
    private fun sendToServer(message: ClientMessage) {

        val host = "monetizemyapp.net"
        val port = 443

        // (every exchange happens in a fresh socket)
        val isConnectionActive = socket != null && socket!!.isConnected
        if (isConnectionActive) closeConnection()

        // Start a connection
        socket = createSocket(host, port)
        writerStream = DataOutputStream(socket!!.outputStream)
        readerStream = DataInputStream(socket!!.inputStream)

        // Prepare message body, indicating end of string with a special char
        val body = Gson().toJson(message) + endOfString()

        // Send message to server immediately
        writerStream!!.write(body.toByteArray())
        writerStream!!.flush()
    }

    @Throws(IOException::class)
    fun createSocket(host: String, port: Int): SSLSocket {

        val socket = SSLSocketFactory.getDefault()
            .createSocket(host, port) as SSLSocket
        socket.enabledProtocols = arrayOf("TLSv1.2")
        return socket
    }

    private fun waitForResponse(): ServerMessage {
        val response = readerStream!!.getString()

        if (response.contains(ServerMessageType.PING)) {
            if (socket!!.isConnected) {
                sendToServer(Pong())
            }
            return waitForResponse()
        }
        // Convert server response to corresponding object type
        return when {
            response.contains(ServerMessageType.BACKCONNECT) -> response.toObject<Backconnect>()
            else -> ServerMessageEmpty()
        }
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
        closeConnection()
        disposables.clear()
    }

    private fun closeConnection() {
        readerStream?.close()
        writerStream?.close()
        socket?.close()
    }
}