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
import java.io.*
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.nio.ByteBuffer
import javax.net.SocketFactory
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

// TODO: If no message is received for last 10 minutes, client should assume connection is dead and reconnect.
class ProxyService : Service() {

    private var socket: SSLSocket? = null
    private var readerStream: DataInputStream? = null
    private var writerStream: DataOutputStream? = null

    private var socketExternal: Socket? = null
    private var readerStreamExternal: InputStream? = null

    private val disposables = CompositeDisposable()

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {

        disposables.add(
            getLocation()
                .map {
                    val message = Hello(HelloBody(getDeviceId(), it.city, it.countryCode, getSystemInfo()))
                    sendJson(message)
                    waitForJson() as Backconnect
                }
                // Verify client token
                .map {
                    val message = Connect(ConnectBody(it.body.token))
                    sendJson(message)
                    waitForBytes()
                }
                // Start socks5 proxy and funnel traffic to server
                .map {
                    sendBytes(byteArrayOf(5, 0))
                    waitForBytes()
                }
                // Parse IP that backconnect is asking us to connect to
                .map {
                    val ipBytes = it.copyOfRange(4, 8)
                    val ip = ByteBuffer.wrap(ipBytes).int.toIP()

                    val portByte = it[9]
                    val port = portByte.toInt()

                    InetSocketAddress(ip, port)
                }
                // Get bytes from external source
                .map {
                    connectExternal(it)
                    waitForExternalBytes()
                }
                // Return bytes to client
                .doOnSuccess {
                    val requestType = it[1]
                    sendBytes(byteArrayOf(5, 0, 0, 3) + ByteArray(6))
                }
                // Or return error to client
                .doOnError {
                    sendBytes(byteArrayOf(5, 4, 0, 3) + ByteArray(6))
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
    private fun sendJson(message: ClientMessage) {

        val host = "monetizemyapp.net"
        val port = 443

        // (every exchange happens in a fresh socket)
        val isConnectionActive = socket != null && socket!!.isConnected
        if (isConnectionActive) closeConnectionBackconnect()

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

    /**
     * Connects to external server via SSL.
     * These bytes will be funneled to backconnect server
     */
    private fun connectExternal(address: InetSocketAddress) {

        readerStreamExternal = URL("http://${address.hostName}").openStream()
    }

    private fun sendBytes(bytes: ByteArray) {

        writerStream!!.write(bytes)
        writerStream!!.flush()
    }

    @Throws(IOException::class)
    fun createSocket(host: String, port: Int): SSLSocket {

        val socket = SSLSocketFactory.getDefault()
            .createSocket(host, port) as SSLSocket
        socket.enabledProtocols = arrayOf("TLSv1.2")
        return socket
    }

    @Throws(IOException::class)
    fun createExternalSocket(host: String, port: Int): Socket {

        return SocketFactory.getDefault()
            .createSocket(host, port) as Socket
    }

    private fun waitForJson(): ServerMessage {
        val response = readerStream!!.getString()

        if (response.contains(ServerMessageType.PING)) {
            if (socket!!.isConnected) {
                sendJson(Pong())
            }
            return waitForJson()
        }
        // Convert server response to corresponding object type
        return when {
            response.contains(ServerMessageType.BACKCONNECT) -> response.toObject<Backconnect>()
            else -> ServerMessageEmpty()
        }
    }

    private fun waitForBytes(): ByteArray {
        return readerStream!!.getBytes()
    }

    private fun waitForExternalBytes(): ByteArray {
        val outputStream = ByteArrayOutputStream()
        val data = ByteArray(4096)

        var length = readerStreamExternal!!.read(data)
        while (length > 0) {
            outputStream.write(data, 0, length)
            length = readerStreamExternal!!.read(data)
        }
        return outputStream.toByteArray()
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
        closeConnectionBackconnect()
        closeConnectionExternal()
        disposables.clear()
    }

    private fun closeConnectionBackconnect() {
        readerStream?.close()
        writerStream?.close()
        socket?.close()
    }

    private fun closeConnectionExternal() {
        readerStreamExternal?.close()
        socketExternal?.close()
    }
}