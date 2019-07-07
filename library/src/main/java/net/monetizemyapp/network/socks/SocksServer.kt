package net.monetizemyapp.network.socks

import net.monetizemyapp.network.shl
import net.monetizemyapp.network.toIP
import net.monetizemyapp.toolbox.extentions.logd
import java.nio.ByteBuffer
import kotlin.experimental.and
import kotlin.experimental.or
import kotlin.math.abs

object SocksServer {
    val TAG: String = SocksServer::class.java.canonicalName ?: SocksServer::class.java.name

    //socks version
    const val SOCKS_VERSION_5 = 5.toByte()

    //socks command
    const val STREAM_CONNECTION = 1.toByte()
    const val PORT_BINDING = 2.toByte()
    const val ASSOCIATE_UDP_PORT = 3.toByte()

    //reserved byte
    const val RESERVED_BYTE = 0.toByte()

    //address type byte
    const val ADDRESS_TYPE_IPv4 = 1.toByte()
    const val ADDRESS_TYPE_DOMAIN_NAME = 3.toByte()
    const val ADDRESS_TYPE_IPv6 = 4.toByte()

    const val CONNECTION_FIELD_SOCKS_VERSION = 0
    const val CONNECTION_FIELD_COMMAND = 1
    const val CONNECTION_FIELD_RESERVED = 2
    const val CONNECTION_FIELD_ADDRESS_TYPE = 3


    val FIELD_NAME_BYTE = Byte.MAX_VALUE
    val CONNECTION_FIELDS = mapOf(
        0 to mapOf(
            FIELD_NAME_BYTE to "Socks version",
            SOCKS_VERSION_5 to "SOCKSv5"
        ),
        1 to mapOf(
            FIELD_NAME_BYTE to "Socks command",
            STREAM_CONNECTION to "Stream connection",
            PORT_BINDING to "Port binding",
            ASSOCIATE_UDP_PORT to "Associate UDP port"
        ),
        2 to mapOf(
            FIELD_NAME_BYTE to "Reserved byte",
            RESERVED_BYTE to "Reserved byte"
        ),
        3 to mapOf(
            FIELD_NAME_BYTE to "Address type",
            ADDRESS_TYPE_IPv4 to "Address type IPv4",
            ADDRESS_TYPE_DOMAIN_NAME to "Address type Domain name",
            ADDRESS_TYPE_IPv6 to "Address type IPv6"
        )
    )

    fun parseSocksConnectionRequest(bytes: ByteArray): Pair<String, Int> {
        logd(TAG, "parseSocksConnectionRequest\n")
        bytes.forEachIndexed { index, byte ->
            logd(
                TAG,
                "Field${index + 1} (${
                SocksServer.CONNECTION_FIELDS[index]?.get(SocksServer.FIELD_NAME_BYTE)
                }) is ${
                SocksServer.CONNECTION_FIELDS[index]?.get(byte)}\n"
            )
        }

        val ipAddress: String = when {
            bytes[SocksServer.CONNECTION_FIELD_ADDRESS_TYPE] == SocksServer.ADDRESS_TYPE_IPv4 ->
                ByteBuffer.wrap(bytes.copyOfRange(4, 8)).int.toIP()
            bytes[SocksServer.CONNECTION_FIELD_ADDRESS_TYPE] == SocksServer.ADDRESS_TYPE_IPv6 ->
                bytes.copyOfRange(4, 20).contentToString()
            else -> {
                val length = bytes.get(SocksServer.CONNECTION_FIELD_ADDRESS_TYPE + 1)
                bytes.copyOfRange(4, length.toInt()).contentToString()
            }
        }
        logd(TAG, "parseSocksConnectionRequest: Request IP Address = $ipAddress\n")
        val portBytes = bytes.takeLast(2).toByteArray()
        logd(TAG, "parseSocksConnectionRequest: Port Bytes = ${portBytes.contentToString()}")
        logd(TAG, "parseSocksConnectionRequest: Port UBytes = ${portBytes.toUByteArray().contentToString()}")
        val portByte = ((portBytes[0] and 0xff.toByte()) shl 8 or (portBytes[1] and 0xff.toByte()))
        val port = portByte.toInt()
        val uPort = portByte.toUByte().toInt()
        logd(TAG, "parseSocksConnectionRequest: Request portByte = $portByte\n")
        logd(TAG, "parseSocksConnectionRequest: Request port = $port\n")
        logd(TAG, "parseSocksConnectionRequest: Request uPort = $uPort\n")
        return Pair(ipAddress, port.takeIf { it > 0 } ?: (uPort * 2) + abs(port))
    }

}