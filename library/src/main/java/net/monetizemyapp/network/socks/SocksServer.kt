package net.monetizemyapp.network.socks

import net.monetizemyapp.toolbox.extentions.logd

object SocksServer {
    val TAG: String = SocksServer::class.java.canonicalName ?: SocksServer::class.java.name

    fun parseSocksConnectionRequest(bytes: ByteArray): Pair<String, Int> {
        val socksMessage = Socks5Message(bytes, Socks5Message.SocksMessageMode.ClientMessage)
        logd(TAG, "SOCKS CONNECTION REQUEST: $socksMessage")
        return Pair(socksMessage.connectionAddress, socksMessage.port)
    }
}