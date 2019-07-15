package net.monetizemyapp.network

interface TcpClient {

    fun sendMessageSync(message: String)
    fun sendBytesSync(bytes: ByteArray)
    fun stop()
    fun waitForBytesSync(): ByteArray
    fun waitForMessageSync(): String?

    fun setTimeout(timeout: Long)
    fun getTimeout(): Long

    fun setKeepAlive(keepAlive: Boolean)

    interface OnSocketResponseListener {
        fun onNewMessage(client: TcpClient, message: String)
        fun onNewBytes(client: TcpClient, bytes: ByteArray)
        fun onError(client: TcpClient, error: String)
    }

    abstract class OnSocketResponseSimpleListener : OnSocketResponseListener {
        override fun onNewMessage(client: TcpClient, message: String) {}
        override fun onNewBytes(client: TcpClient, bytes: ByteArray) {}
        override fun onError(client: TcpClient, error: String) {}
    }
}
