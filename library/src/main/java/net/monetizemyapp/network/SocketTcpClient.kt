package net.monetizemyapp.network

import net.monetizemyapp.toolbox.extentions.*
import java.io.IOException
import java.net.Socket


@ExperimentalStdlibApi
class SocketTcpClient(private val socket: Socket) : TcpClient {

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
        logd(TAG, "\tclosing socket : ${socket.inetAddress}")
        socket.close()
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