package net.monetizemyapp.android

import android.app.Service
import android.content.Intent
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.monetizemyapp.di.InjectorUtils
import net.monetizemyapp.network.*
import net.monetizemyapp.network.model.base.ServerMessage
import net.monetizemyapp.network.model.base.ServerMessageEmpty
import net.monetizemyapp.network.model.base.ServerMessageType
import net.monetizemyapp.network.model.response.IpApiResponse
import net.monetizemyapp.network.model.step0.Ping
import net.monetizemyapp.network.model.step0.Pong
import net.monetizemyapp.network.model.step1.Hello
import net.monetizemyapp.network.model.step1.HelloBody
import net.monetizemyapp.network.model.step2.Backconnect
import net.monetizemyapp.toolbox.CoroutineContextPool
import net.monetizemyapp.toolbox.extentions.*
import retrofit2.HttpException
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.net.URLConnection
import kotlin.coroutines.CoroutineContext

// TODO: If no message is received for last 10 minutes, client should assume connection is dead and reconnect.
class ProxyService : Service(), CoroutineScope {
    private val lifecycleJob = Job()
    override val coroutineContext: CoroutineContext
        get() = CoroutineContextPool.network + lifecycleJob

    companion object {
        private val TAG: String = ProxyService::class.java.canonicalName ?: ProxyService::class.java.name
        private const val HOST = "monetizemyapp.net"
        private const val PORT = 443
        private val ENABLED_SOCKET_PROTOCOLS = arrayOf("TLSv1.2")
    }

    private val sockets = mutableListOf<Socket>()
    private val socketsExternal = mutableListOf<URLConnection>()
    private val serverSocketConnection by lazy {
        InjectorUtils.Sockets.provideSSlWebSocketConnection(
            HOST,
            PORT,
            ENABLED_SOCKET_PROTOCOLS
        )
    }

    private val locationApi by lazy { InjectorUtils.Api.provideLocationApi() }

    private val testSocketConnection by lazy { TcpClient(serverSocketConnection, tcpClientCallback) }

    val tcpClientCallback = object : TcpClient.OnSocketResponseListener {

        override fun onError(error: String) {
            loge(TAG, "onError : $error")
        }

        override fun onNewMessage(client: TcpClient, message: String) {
            logd(TAG, "onNewMessage, message : $message")
            val response = message.toObject()
            logd(TAG, "onNewMessage, response : $response")
            when (response) {
                is ServerMessageEmpty -> {
                    logd(TAG, "response message is empty")
                }
                is Ping -> {
                    logd(TAG, "response message is Ping")
                    client.sendMessage(Pong().toJson())
                }
                is Backconnect -> {
                    var serverSocket: TcpClient?
                    var backSocket: TcpClient? = null

                    val serverListener = object : TcpClient.OnSocketResponseListener {
                        override fun onNewMessage(client: TcpClient, message: String) {
                            backSocket?.sendMessage(message)
                        }

                        override fun onError(error: String) {}
                    }
                    val socket =
                        InjectorUtils.Sockets.provideSSlWebSocketConnection(HOST, PORT, ENABLED_SOCKET_PROTOCOLS)
                    serverSocket = TcpClient(socket, serverListener)

                    // todo implement logic for message exchange
                    val backListener = object : TcpClient.OnSocketResponseListener {
                        override fun onNewMessage(client: TcpClient, message: String) {
                            serverSocket.sendMessage(message)
                        }

                        override fun onError(error: String) {}
                    }
                    /*val message = Connect(ConnectBody(response.body.token))
                    client.sendMessage(message.toJson())*/
                    logd(TAG, "response message is Backconnect")
                }
            }
        }
    }

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

        launch {

            val location: Response<IpApiResponse>? = try {
                locationApi.getLocation()
            } catch (ex: HttpException) {
                loge(TAG, "Location request error : ${ex.message}")
                null
            } catch (ex: IOException) {
                loge(TAG, "Location request error : ${ex.message}")
                null
            }

            location?.takeIf { it.isSuccessful }?.body()?.let {
                val message = Hello(
                    HelloBody(
                        clientKey,
                        it.query,
                        deviceId,
                        it.city,
                        it.countryCode,
                        getSystemInfo()
                    )
                )

                testSocketConnection.sendMessage(message.toJson())

                // Prepare message body, indicating end of string with a special char

                /*  serverSocketConnection.sendJson(message)

                  var result: ServerMessage
                  while (true) {
                      if (!serverSocketConnection.isConnected) {
                          logd(TAG, "breaking socket loop : ${serverSocketConnection.localAddress}")
                          break
                      }

                      result = serverSocketConnection.waitForJson()

                      if (result !is ServerMessageEmpty) {
                          logd(TAG, "hello response : $result")
                      }

                      when (result) {
                          is Backconnect -> {
                              startNewSession(result)
                          }
                          is Ping -> {
                              // Exchange ping-pong messages, on first socket only
                              serverSocketConnection.sendJson(Pong())
                          }
                          else -> {

                          }

                      }
                  }*/

            }
        }
    }

    private fun startNewBackconnectSession(backconnect: Backconnect) {


    }

    private fun startNewSession(backconnect: Backconnect) = launch {

        /*val socket = InjectorUtils.Sockets.provideSSlWebSocketConnection(HOST, PORT, ENABLED_SOCKET_PROTOCOLS)
        sockets.add(socket)

        // Verify client token
        val message = Connect(ConnectBody(backconnect.body.token))
        socket.sendJson(message)
        val firstBackconnectResponse = socket.waitForBytes()
        logd(TAG, "firstBackconnectResponse = $firstBackconnectResponse\n")

        // Start socks5 proxy and funnel traffic to server
        socket.sendBytes(byteArrayOf(5, 0))
        val requestBytes = socket.waitForBytes()
        logd(TAG, "requestBytes = $requestBytes")

        // Parse IP that backconnect is asking us to connect to
        val ipBytes = requestBytes.copyOfRange(4, 8)
        val ip = ByteBuffer.wrap(ipBytes).int.toIP()
        val portByte = requestBytes[9]
        val port = portByte.toInt()

        logd(TAG, "new socket connection credentials: ip = $ip, port = $port\n")

        // Get bytes from external source
        val address = InetSocketAddress(ip, port)

        val socketExternal = createSocketExternal(address)
        socketsExternal.add(socketExternal)

        exchangeBytes(serverSocketConnection, socketExternal)*/

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

    private fun Socket.sendBytes(bytes: ByteArray) {

        getOutputStream().write(bytes)
        getOutputStream().flush()
    }

    private fun URLConnection.sendBytes(bytes: ByteArray) {
        getOutputStream().write(bytes)
        getOutputStream().flush()
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
        val response = try {
            DataInputStream(getInputStream()).getString()
        } catch (ex: IOException) {
            loge(TAG, ex.message)
            null
        }

        // Convert server response to corresponding object type
        return when {
            response?.contains(ServerMessageType.BACKCONNECT) == true -> response.fromJson<Backconnect>()
            response?.contains(ServerMessageType.PING) == true -> response.fromJson<Ping>()
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
        //sockets.forEach { it.close() }
        testSocketConnection.stopClient()
        lifecycleJob.cancel()
    }
}