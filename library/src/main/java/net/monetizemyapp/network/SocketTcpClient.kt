package net.monetizemyapp.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.monetizemyapp.toolbox.CoroutineContextPool
import net.monetizemyapp.toolbox.extentions.*
import java.net.Socket
import kotlin.coroutines.CoroutineContext


@ExperimentalStdlibApi
class SocketTcpClient(private val socket: Socket) : TcpClient, CoroutineScope {
    override fun waitForMessageSync(): String? =
        socket.waitForStringResponse()


    private val lifecycleJob = Job()
    override val coroutineContext: CoroutineContext
        get() = CoroutineContextPool.network + lifecycleJob

    // sends message received notifications
    override var listener: TcpClient.OnSocketResponseListener? = null
        set(value) {
            field = value
            listenToUpdates = value != null

        }
    // while this is true, the server will continue running
    override var listenToUpdates = false
        set(value) {
            field = value
            if (value) {
                startListeningUpdates()
            }
        }

    init {
        startListeningUpdates()
    }

    /**
     * Sends the message entered by client to the server
     *
     * @param message text entered by client
     */
    override fun sendMessage(message: String) {
        launch {
            logd(TAG, "Sending message: $message")
            socket.sendJson(message)
        }
    }

    override fun sendMessageSync(message: String) {
        logd(TAG, "Sending message: $message")
        socket.sendJson(message)
    }

    override fun sendBytesSync(bytes: ByteArray) {
        logd(TAG, "Sending bytes sync: ${bytes.contentToString()}")
        socket.sendBytes(bytes)
    }

    /**
     * Close the connection and release the members
     */
    override fun stop() {
        listenToUpdates = false
        logd(TAG, "canceling jobs")
        lifecycleJob.cancel()
        logd(TAG, "closing socket : ${socket.inetAddress}")
        socket.close()
        logd(TAG, "removing listener")
        listener = null
    }

    private fun startListeningUpdates() {
        try {
            //in this while the client listens for the messages sent by the server
            launch {
                while (isActive && listenToUpdates) {
                    //logd(TAG, "listening loop started")
                    val response = socket.waitForStringResponse()
                    //logd(TAG, "server response = $response")
                    if (response.isNullOrBlank()) {
                        continue
                        listener?.onError(this@SocketTcpClient, "response is Empty")
                    } else {
                        logd(TAG, "server response = $response")
                        listener?.onNewMessage(this@SocketTcpClient, response)
                    }
                }
            }
        } catch (e: Exception) {
            listener?.onError(this@SocketTcpClient, e.message ?: "connection error")
            loge(TAG, "Error ${e.message}")
        }
    }

    override fun waitForBytesSync() = socket.waitForBytes()

    companion object {

        val TAG = SocketTcpClient::class.java.simpleName
        /* val SERVER_IP = "192.168.1.8" //server IP address
         val SERVER_PORT = 1234*/
    }

}