package net.monetizemyapp.android

import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import android.widget.Toast
import com.google.gson.Gson
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers.mainThread
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers.newThread
import net.monetizemyapp.network.*
import net.monetizemyapp.network.api.GeolocationService
import net.monetizemyapp.network.model.base.ClientMessage
import net.monetizemyapp.network.model.base.ServerMessage
import net.monetizemyapp.network.model.base.ServerMessageEmpty
import net.monetizemyapp.network.model.base.ServerMessageType
import net.monetizemyapp.network.model.step0.Ping
import net.monetizemyapp.network.model.step0.Pong
import net.monetizemyapp.network.model.step1.Geolocation
import net.monetizemyapp.network.model.step1.Hello
import net.monetizemyapp.network.model.step1.HelloBody
import net.monetizemyapp.network.model.step2.Backconnect
import net.monetizemyapp.network.model.step2.Connect
import net.monetizemyapp.network.model.step2.ConnectBody
import java.io.*
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.nio.ByteBuffer
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory


// TODO: If no message is received for last 10 minutes, client should assume connection is dead and reconnect.
class ProxyService : Service() {

    private val sockets: MutableList<Socket> = ArrayList()

    private val disposables = CompositeDisposable()

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if (disposables.size() == 0) {
            startProxy()
        }
        return START_STICKY
    }

    private fun startProxy() {

        val info = applicationContext.packageManager.getApplicationInfo(
            applicationContext.packageName,
            PackageManager.GET_META_DATA
        )
        val clientKey = info.metaData.getString("monetize_app_key")

        if (clientKey.isNullOrBlank()) {
            Toast.makeText(
                applicationContext,
                "Error: supply monetize_app_key in Manifest to enable SDK",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        disposables.add(getLocation()
            .map {
                val message = Hello(
                    HelloBody(
                        clientKey,
                        getDeviceId(),
                        it.city,
                        it.countryCode,
                        getSystemInfo()
                    )
                )
                val socket = createSocket()
                sockets.add(socket)
                socket.sendJson(message)

                var result: ServerMessage
                while (true) {
                    result = socket.waitForJson()
                    if (result is Backconnect) {
                        startNewSession(result)

                    } else if (result is Ping) {
                        // Exchange ping-pong messages, on first socket only
                        socket.sendJson(Pong())
                    }
                }
            }
            .subscribeOn(newThread())
            .observeOn(mainThread())
            .subscribe({},
                { t ->
                    Toast.makeText(baseContext, t.localizedMessage, Toast.LENGTH_LONG).show()
                    t.printStackTrace()
                })
        )
    }

    private fun startNewSession(backconnect: Backconnect) {

        disposables.add(Observable.fromCallable {

            // Multiple backconnect connections are supported, so creating sockets in different threads
            val socket = createSocket()
            sockets.add(socket)

            // Verify client token
            val message = Connect(ConnectBody(backconnect.body.token))
            socket.sendJson(message)
            socket.waitForBytes()

            // Start socks5 proxy and funnel traffic to server
            socket.sendBytes(byteArrayOf(5, 0))
            val requestBytes = socket.waitForBytes()

            // Parse IP that backconnect is asking us to connect to
            val ipBytes = requestBytes.copyOfRange(4, 8)
            val ip = ByteBuffer.wrap(ipBytes).int.toIP()
            val portByte = requestBytes[9]
            val port = portByte.toInt()

            // Get bytes from external source
            val address = InetSocketAddress(ip, port)

            var externalResponseBytes: ByteArray
            try {
                val streamExternal = createStreamExternal(address)
                externalResponseBytes = streamExternal.waitForExternalBytes()
                streamExternal.close()
            } catch (e: FileNotFoundException) {
                externalResponseBytes = ByteArray(0)
            }

            // Return bytes, or error, to the client
            val status = if (externalResponseBytes.isEmpty()) 5 else 0

            val response = byteArrayOf(5, status.toByte(), 0, 0, 0, 0, 0, 0, 0)

            socket.sendBytes(response)
            socket.sendBytes(externalResponseBytes)
            socket.close()
        }
            .subscribeOn(newThread())
            .observeOn(mainThread())
            .subscribe())
    }

    /**
     * Connects to backconnect server via SSL
     */
    private fun Socket.sendJson(message: ClientMessage) {

        // Start a connection
        val writerStream = DataOutputStream(outputStream)

        // Prepare message body, indicating end of string with a special char
        val body = Gson().toJson(message) + endOfString()

        // Send message to server immediately
        writerStream.write(body.toByteArray())
        writerStream.flush()
    }

    /**
     * Connects to external server.
     * These bytes will be funneled to backconnect server
     */
    private fun createStreamExternal(address: InetSocketAddress): InputStream {

        // Start a connection
        return URL("http://${address.hostName}").openStream()
    }

    private fun Socket.sendBytes(bytes: ByteArray) {

        getOutputStream().write(bytes)
        getOutputStream().flush()
    }

    @Throws(IOException::class)
    fun createSocket(): SSLSocket {

        val host = "monetizemyapp.net"
        val port = 443

        val socket = SSLSocketFactory.getDefault()
            .createSocket(host, port) as SSLSocket
        socket.enabledProtocols = arrayOf("TLSv1.2")
        return socket
    }

    private fun Socket.waitForJson(): ServerMessage {
        val response = DataInputStream(getInputStream()).getString()

        // Convert server response to corresponding object type
        return when {
            response.contains(ServerMessageType.BACKCONNECT) -> response.toObject<Backconnect>()
            response.contains(ServerMessageType.PING) -> response.toObject<Ping>()
            else -> ServerMessageEmpty()
        }
    }

    private fun Socket.waitForBytes(): ByteArray {
        return getInputStream().getBytes()
    }

    private fun InputStream.waitForExternalBytes(): ByteArray {
        val outputStream = ByteArrayOutputStream()
        val data = ByteArray(4096)

        var length = read(data)
        while (length > 0) {
            outputStream.write(data, 0, length)
            length = read(data)
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
        sockets.forEach { it.close() }
        disposables.clear()
    }
}