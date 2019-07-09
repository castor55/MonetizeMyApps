package net.monetizemyapp.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.monetizemyapp.toolbox.CoroutineContextPool
import net.monetizemyapp.toolbox.extentions.*
import java.io.IOException
import java.net.Socket
import kotlin.coroutines.CoroutineContext


@ExperimentalStdlibApi
class SocketTcpClient(private val socket: Socket) : TcpClient, CoroutineScope {

    private val lifecycleJob = SupervisorJob()
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
        logd(TAG, "\t--- STOP ---")
        logd(TAG, "\tremoving listener")
        listenToUpdates = false
        listener = null
        logd(TAG, "\tclosing socket : ${socket.inetAddress}")
        launch {
            socket.close()
        }
        logd(TAG, "\tcanceling jobs")
        logd(TAG, "\t------------")
        lifecycleJob.cancel()
    }

    private fun startListeningUpdates() {
        //in this while the client listens for the messages sent by the server
        launch {
            try {
                while (isActive && listenToUpdates) {
                    //logd(TAG, "listening loop started")
                    val response = socket.waitForStringResponse()
                    //logd(TAG, "server response = $response")
                    if (response.isNullOrBlank()) {
                        continue
                        // listener?.onError(this@SocketTcpClient, "response is Empty")
                    } else {
                        logd(TAG, "server response = $response")
                        listener?.onNewMessage(this@SocketTcpClient, response)
                    }
                }

            } catch (e: Exception) {
                listener?.onError(this@SocketTcpClient, e.message ?: "connection error")
                loge(TAG, "Error ${e.message}")
            }
        }
    }

    override fun waitForBytesSync() = socket.waitForBytes()

    @Throws(IOException::class)
    override fun waitForMessageSync(): String? = socket.waitForStringResponse()

    override fun setTimeout(timeout: Long) {
        socket.soTimeout = timeout.toInt()
    }

    override fun getTimeout(): Long = socket.soTimeout.toLong()


    override fun setKeepAlive(keepAlive: Boolean) {
        socket.keepAlive = keepAlive
    }

    companion object {
        val TAG = SocketTcpClient::class.java.simpleName
    }

}