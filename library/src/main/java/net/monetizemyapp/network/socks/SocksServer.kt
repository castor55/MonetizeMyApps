package net.monetizemyapp.network.socks

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.monetizemyapp.di.InjectorUtils
import net.monetizemyapp.network.SocketTcpClient
import net.monetizemyapp.network.TcpClient
import net.monetizemyapp.network.model.step2.Backconnect
import net.monetizemyapp.network.model.step2.Connect
import net.monetizemyapp.network.model.step2.ConnectBody
import net.monetizemyapp.toolbox.CoroutineContextPool
import net.monetizemyapp.toolbox.extentions.logd
import net.monetizemyapp.toolbox.extentions.loge
import net.monetizemyapp.toolbox.extentions.toJson
import java.net.SocketException
import kotlin.coroutines.CoroutineContext

@ExperimentalUnsignedTypes
@ExperimentalStdlibApi
class SocksServer : CoroutineScope {

    private val lifecycleJob = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = CoroutineContextPool.network + lifecycleJob

    val TAG: String = SocksServer::class.java.canonicalName ?: SocksServer::class.java.name

    private val serverConnections = mutableListOf<TcpClient>()

    @ExperimentalUnsignedTypes
    private fun parseSocksConnectionRequest(bytes: ByteArray): Pair<String, Int> {
        val socksMessage = Socks5Message(bytes, Socks5Message.SocksMessageMode.ClientMessage)
        logd(TAG, "SOCKS CONNECTION REQUEST: $socksMessage")
        return Pair(socksMessage.connectionAddress, socksMessage.port)
    }

    fun startNewBackconnectSession(backconnect: Backconnect) {
        launch {
            val newServerConnectionTcpClient = InjectorUtils.TcpClient.provideServerTcpClient()
            serverConnections.add(newServerConnectionTcpClient)

            val message = Connect(ConnectBody(backconnect.body.token))
            newServerConnectionTcpClient.sendMessageSync(message.toJson())

            logd(TAG, "startNewBackconnectSession: sendMessageSync, message = ${message.toJson()}")
            val authMethodsSupported = newServerConnectionTcpClient.waitForBytesSync()
            logd(
                TAG,
                "startNewBackconnectSession: waitForBytesSync, firstBytesResponse = ${authMethodsSupported.contentToString()}"
            )

            val chosenAuthMethod = byteArrayOf(5, 0)
            newServerConnectionTcpClient.sendBytesSync(chosenAuthMethod)
            logd(
                TAG,
                "startNewBackconnectSession: send chosenAuthMethod = ${chosenAuthMethod.contentToString()}"
            )

            val connectionRequestBytes = newServerConnectionTcpClient.waitForBytesSync()
            logd(
                TAG,
                "startNewBackconnectSession: connectionRequestBytes = ${connectionRequestBytes.contentToString()}"
            )


            val (ip, port) = parseSocksConnectionRequest(connectionRequestBytes)

            logd(TAG, "startNewBackconnectSession: try to connect to backconnect ip = $ip, port = $port\n")

            val backConnectSocket = InjectorUtils.Sockets.provideSimpleSocketConnection(ip, port)
            val newBackConnectTcpClient = SocketTcpClient(backConnectSocket)
            serverConnections.add(newBackConnectTcpClient)

            // Return bytes, or error, to the client
            val status = if (backConnectSocket.isBound && !backConnectSocket.isClosed) 0 else 5

            val socksServerResponse =
                Socks5Message(5.toByte(), status.toByte(), 0, 0, 0, byteArrayOf(0), byteArrayOf(0))
            logd(
                TAG,
                "startNewBackconnectSession: sendBytesSync, statusResponseToServer = ${socksServerResponse.bytes.contentToString()}"
            )

            newServerConnectionTcpClient.sendBytesSync(socksServerResponse.bytes)
            exchangeBytes(newServerConnectionTcpClient, newBackConnectTcpClient)
        }
    }

    private fun exchangeBytes(serverConnectionClient: TcpClient, backConnectClient: TcpClient) {
        var canceled = false
        launch {
            try {
                while (isActive && !canceled) {
                    val backConnectResponse = backConnectClient.waitForBytesSync()
                    logd(TAG, "Received message from backconnect: message = $backConnectResponse")
                    logd(
                        TAG,
                        "Received message from backconnect: message (decoded) = ${backConnectResponse.decodeToString()}"
                    )
                    if (backConnectResponse.isNotEmpty()) {
                        serverConnectionClient.sendBytesSync(backConnectResponse)
                    } else {
                        canceled = true
                    }
                }
            } catch (e: SocketException) {
                e.printStackTrace()
                loge(TAG, "exchangeBytes Error: ${e.message}")
            }
        }
        launch {
            try {
                while (isActive && !canceled) {
                    val serverConnectionResponse = serverConnectionClient.waitForBytesSync()
                    logd(TAG, "Received message from server: message = $serverConnectionResponse")
                    logd(
                        TAG,
                        "Received message from server: message (decoded) = ${serverConnectionResponse.decodeToString()}"
                    )
                    if (serverConnectionResponse.isNotEmpty()) {
                        backConnectClient.sendBytesSync(serverConnectionResponse)
                    } else {
                        canceled = true
                    }
                }
            } catch (e: SocketException) {
                e.printStackTrace()
                loge(TAG, "exchangeBytes Error: ${e.message}")
            }
        }

        if (canceled) {
            listOf(serverConnectionClient, backConnectClient).forEach {
                serverConnections.remove(it.apply { stop() })
            }
        }
    }


    private fun stopAllConnections() {
        serverConnections.forEach {
            it.stop()
        }
        serverConnections.clear()
    }

    fun stopServer() {
        stopAllConnections()
        lifecycleJob.cancel()
    }
}