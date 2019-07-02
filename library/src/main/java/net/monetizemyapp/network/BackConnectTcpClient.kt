package net.monetizemyapp.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.monetizemyapp.toolbox.CoroutineContextPool
import net.monetizemyapp.toolbox.extentions.logd
import net.monetizemyapp.toolbox.extentions.loge
import net.monetizemyapp.toolbox.extentions.sendBytes
import net.monetizemyapp.toolbox.extentions.waitForExternalBytes
import java.net.URLConnection
import kotlin.coroutines.CoroutineContext

@ExperimentalStdlibApi
class BackConnectTcpClient(private val connection: URLConnection) :
    TcpClient,
    CoroutineScope {

    private val lifecycleJob = Job()
    override val coroutineContext: CoroutineContext
        get() = CoroutineContextPool.network + lifecycleJob

    // sends message received notifications
    override var listener: TcpClient.OnSocketResponseListener? = null
    // while this is true, the server will continue running
    override var listenToUpdates = true
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
            connection.sendBytes(message.toByteArray())
        }
    }

    override fun sendMessageSync(message: String) {
        logd(TAG, "Sending message: $message")
        connection.sendBytes(message.toByteArray())
    }

    override fun sendBytesSync(bytes: ByteArray) {
        logd(TAG, "Sending bytes sync: ${bytes.contentToString()}")
        connection.sendBytes(bytes)
    }

    /**
     * Close the connection and release the members
     */
    override fun stop() {
        listenToUpdates = false
        logd(TAG, "canceling jobs")
        lifecycleJob.cancel()
        logd(TAG, "closing socket : $connection")
        connection.getInputStream().close()
        logd(TAG, "removing listener")
        listener = null
    }

    private fun startListeningUpdates() {
        try {
            //in this while the client listens for the messages sent by the server
            launch {
                while (isActive && listenToUpdates) {
                    //logd(TAG, "listening loop started")
                    val response = connection.waitForExternalBytes()
                    //logd(TAG, "server response = $response")
                    if (response.isNotEmpty()) {
                        listener?.onError(this@BackConnectTcpClient, "response is Empty")
                        stop()
                    } else {
                        logd(TAG, "server response = $response")
                        listener?.onNewBytes(this@BackConnectTcpClient, response)
                    }
                }
            }
        } catch (e: Exception) {
            listener?.onError(this@BackConnectTcpClient, e.message ?: "connection error")
            loge(TAG, "Error ${e.message}")
        }
    }

    override fun waitForBytesSync() = connection.waitForExternalBytes()

    companion object {

        val TAG = SocketTcpClient::class.java.simpleName
        /* val SERVER_IP = "192.168.1.8" //server IP address
         val SERVER_PORT = 1234*/
    }

}