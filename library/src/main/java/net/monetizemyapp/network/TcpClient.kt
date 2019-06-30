package net.monetizemyapp.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.monetizemyapp.toolbox.CoroutineContextPool
import net.monetizemyapp.toolbox.extentions.logd
import net.monetizemyapp.toolbox.extentions.loge
import net.monetizemyapp.toolbox.extentions.sendJson
import net.monetizemyapp.toolbox.extentions.waitForJson
import java.net.Socket
import kotlin.coroutines.CoroutineContext


class TcpClient(private val socket: Socket, listener: OnSocketResponseListener) : CoroutineScope {

    private val lifecycleJob = Job()
    override val coroutineContext: CoroutineContext
        get() = CoroutineContextPool.network + lifecycleJob

    // sends message received notifications
    private var listener: OnSocketResponseListener? = null
    // while this is true, the server will continue running
    private var run = false

    init {
        this.listener = listener
        run()
    }

    /**
     * Sends the message entered by client to the server
     *
     * @param message text entered by client
     */
    fun sendMessage(message: String) {
        launch {
            logd(TAG, "Sending: $message")
            socket.sendJson(message)
        }
    }

    /**
     * Close the connection and release the members
     */
    fun stopClient() {
        run = false
        lifecycleJob.cancel()
        socket.close()
        listener = null
    }

    private fun run() {

        run = true

        try {
            //in this while the client listens for the messages sent by the server
            launch {
                while (run) {
                    logd(TAG, "listening loop started")
                    val response = socket.waitForJson()
                    logd(TAG, "server response = $response")
                    if (response.isNullOrBlank()) {
                        listener?.onError("response is Empty")
                    } else {
                        listener?.onNewMessage(this@TcpClient, response)
                    }
                }
            }
        } catch (e: Exception) {
            listener?.onError(e.message ?: "connection error")
            loge(TAG, "Error ${e.message}")
        }
    }

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