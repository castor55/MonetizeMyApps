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
class TcpClient(private val socket: Socket, listener: OnSocketResponseListener) : CoroutineScope {

    private val lifecycleJob = Job()
    override val coroutineContext: CoroutineContext
        get() = CoroutineContextPool.network + lifecycleJob

    // sends message received notifications
    private var listener: OnSocketResponseListener? = null
    // while this is true, the server will continue running
    var listenToUpdates = true
        set(value) {
            field = value
            if (value) {
                startListeningUpdates()
            }

        }

    init {
        this.listener = listener
        startListeningUpdates()
    }

    /**
     * Sends the message entered by client to the server
     *
     * @param message text entered by client
     */
    fun sendMessage(message: String) {
        launch {
            logd(TAG, "Sending message: $message")
            socket.sendJson(message)
        }
    }

    fun sendMessageSync(message: String) {
        logd(TAG, "Sending message: $message")
        socket.sendJson(message)
    }

    fun sendBytesSync(bytes: ByteArray) {
        logd(TAG, "Sending bytes sync: ${bytes.contentToString()}")
        socket.sendBytes(bytes)
    }

    /**
     * Close the connection and release the members
     */
    fun stop() {
        listenToUpdates = false
        lifecycleJob.cancel()
        socket.close()
        listener = null
    }

    private fun startListeningUpdates() {
        try {
            //in this while the client listens for the messages sent by the server
            launch {
                while (isActive && listenToUpdates) {
                    //logd(TAG, "listening loop started")
                    val response = socket.waitForJson()
                    //logd(TAG, "server response = $response")
                    if (response.isNullOrBlank()) {
                        listener?.onError("response is Empty")
                    } else {
                        logd(TAG, "server response = $response")
                        listener?.onNewMessage(this@TcpClient, response)
                    }
                }
            }
        } catch (e: Exception) {
            listener?.onError(e.message ?: "connection error")
            loge(TAG, "Error ${e.message}")
        }
    }

    fun waitForBytesSync() = socket.waitForBytes()


    //Declare the interface. The method onNewMessage(String message) will must be implemented in the Activity
    //class at on AsyncTask doInBackground
    interface OnSocketResponseListener {
        fun onNewMessage(client: TcpClient, message: String)
        fun onError(error: String)
    }

    companion object {

        val TAG = TcpClient::class.java.simpleName
        /* val SERVER_IP = "192.168.1.8" //server IP address
         val SERVER_PORT = 1234*/
    }

}