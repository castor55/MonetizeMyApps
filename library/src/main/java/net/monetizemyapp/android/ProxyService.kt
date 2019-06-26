package net.monetizemyapp.android

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.google.gson.Gson
import net.monetizemyapp.di.InjectorUtils
import net.monetizemyapp.network.endOfString
import net.monetizemyapp.network.getBytes
import net.monetizemyapp.network.getString
import net.monetizemyapp.network.model.base.ClientMessage
import net.monetizemyapp.network.model.base.ServerMessage
import net.monetizemyapp.network.model.base.ServerMessageEmpty
import net.monetizemyapp.network.model.base.ServerMessageType
import net.monetizemyapp.network.model.response.IpApiResponse
import net.monetizemyapp.network.model.step0.Ping
import net.monetizemyapp.network.model.step2.Backconnect
import net.monetizemyapp.network.toObject
import net.monetizemyapp.toolbox.extentions.getAppInfo
import net.monetizemyapp.toolbox.extentions.logd
import net.monetizemyapp.toolbox.extentions.loge
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.*
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.net.URLConnection
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

// TODO: If no message is received for last 10 minutes, client should assume connection is dead and reconnect.
class ProxyService : Service() {
    companion object {
        private val TAG: String = ProxyService::class.java.canonicalName ?: ProxyService::class.java.name
    }

    private val sockets = mutableListOf<Socket>()
    private val socketsExternal = mutableListOf<URLConnection>()

    private val locationApi by lazy { InjectorUtils.Api.provideLocationApi() }

    private var locationCall: Call<IpApiResponse>? = null

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        startProxy()

        return START_STICKY
    }

    private fun startProxy() {

        logd(TAG, "StartProxy")

        val clientKey = getAppInfo()?.metaData?.getString("monetize_app_key")

        if (clientKey.isNullOrBlank()) {
            throw IllegalArgumentException("Error: \"monetize_app_key\" is null. Provide \"monetize_app_key\" in Manifest to enable SDK")
        }

        val locationCall = locationApi.location

        val callback = object : Callback<IpApiResponse> {
            override fun onFailure(call: Call<IpApiResponse>, t: Throwable) {
                loge(TAG, t.message)
            }

            override fun onResponse(call: Call<IpApiResponse>, response: Response<IpApiResponse>) {
                response.body()?.let {
                    logd(TAG, "Country code : ${response.body()?.countryCode}")
                }
            }

        }
        locationCall.enqueue(callback)

        /* disposables.add(getLocation()
             .map {
                 val message = Hello(
                     HelloBody(
                         clientKey,
                         getDeviceId(it.ip),
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
         )*/
    }

    private fun startNewSession(backconnect: Backconnect) {

        /* disposables.add(Observable.fromCallable {

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

             val socketExternal = createSocketExternal(address)
             socketsExternal.add(socketExternal)

             exchangeBytes(socket, socketExternal)
         }
             .subscribeOn(newThread())
             .observeOn(mainThread())
             .subscribe())*/
    }

    private fun exchangeBytes(socket: Socket, socketExternal: URLConnection) {
        while (true) {
            val externalResponseBytes = try {
                socketExternal.waitForExternalBytes()
            } catch (e: FileNotFoundException) {
                ByteArray(0)
            }
            // Return bytes, or error, to the client
            val status = if (externalResponseBytes.isEmpty()) 5 else 0

            val response = byteArrayOf(5, status.toByte(), 0, 0, 0, 0, 0, 0, 0)
            socket.sendBytes(response)

            if (externalResponseBytes.isNotEmpty()) {
                socket.sendBytes(externalResponseBytes)
                val socketBytes = socket.waitForBytes()
                socketExternal.sendBytes(socketBytes)
            } else {
                return
            }
        }
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

    private fun Socket.sendBytes(bytes: ByteArray) {

        getOutputStream().write(bytes)
        getOutputStream().flush()
    }

    private fun URLConnection.sendBytes(bytes: ByteArray) {

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

    /**
     * Connects to external server.
     * These bytes will be funneled to backconnect server
     */
    private fun createSocketExternal(address: InetSocketAddress): URLConnection {

        return URL("http://${address.hostName}:${address.port}").openConnection()
            .apply { connect() }
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

    private fun URLConnection.waitForExternalBytes(): ByteArray {

        val outputStream = ByteArrayOutputStream()
        val data = ByteArray(4096)

        var length = getInputStream().read(data)
        while (length > 0) {
            outputStream.write(data, 0, length)
            length = getInputStream().read(data)
        }
        return outputStream.toByteArray()
    }


    override fun onDestroy() {
        if (locationCall?.isCanceled == false) {
            locationCall?.cancel()
        }
        sockets.forEach { it.close() }
    }
}