package net.monetizemyapp.network

interface TcpClient {
    var listenToUpdates: Boolean
    var listener : OnSocketResponseListener?

    fun sendMessage(message: String)
    fun sendMessageSync(message: String)
    fun sendBytesSync(bytes: ByteArray)
    fun stop()
    fun waitForBytesSync(): ByteArray

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
