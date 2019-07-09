package net.monetizemyapp.network.socks

import net.monetizemyapp.network.shl
import net.monetizemyapp.network.toIP
import net.monetizemyapp.toolbox.extentions.logd
import java.nio.ByteBuffer
import kotlin.experimental.and
import kotlin.experimental.or
import kotlin.math.abs

@ExperimentalStdlibApi
@ExperimentalUnsignedTypes
data class Socks5Message(private val messageBytes: ByteArray, private val mode: SocksMessageMode) {

    constructor(
        version: Byte,
        status: Byte,
        reserved: Byte,
        addressType: Byte,
        boundAddressType: Byte,
        boundAddress: ByteArray,
        boundPort: ByteArray
    ) : this(
        byteArrayOf(version, status, reserved, addressType, boundAddressType).plus(boundAddress).plus(boundPort),
        SocksMessageMode.ServerMessage
    )

    companion object {
        val TAG: String = Socks5Message::class.java.canonicalName ?: Socks5Message::class.java.name
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
            CONNECTION_FIELD_SOCKS_VERSION to mapOf(
                FIELD_NAME_BYTE to "Socks version",
                SOCKS_VERSION_5 to "SOCKSv5"
            ),
            CONNECTION_FIELD_COMMAND to mapOf(
                FIELD_NAME_BYTE to "Socks command",
                STREAM_CONNECTION to "Stream connection",
                PORT_BINDING to "Port binding",
                ASSOCIATE_UDP_PORT to "Associate UDP port"
            ),
            CONNECTION_FIELD_RESERVED to mapOf(
                FIELD_NAME_BYTE to "Reserved byte",
                RESERVED_BYTE to "Reserved byte"
            ),
            CONNECTION_FIELD_ADDRESS_TYPE to mapOf(
                FIELD_NAME_BYTE to "Address type",
                ADDRESS_TYPE_IPv4 to "Address type IPv4",
                ADDRESS_TYPE_DOMAIN_NAME to "Address type Domain name",
                ADDRESS_TYPE_IPv6 to "Address type IPv6"
            )
        )
    }

    val version: Byte
        get() {
            return bytes[CONNECTION_FIELD_SOCKS_VERSION]
        }


    val socksCommand: Byte
        get() {
            return bytes[CONNECTION_FIELD_COMMAND]
        }

    val reservedByte: Byte
        get() {
            return bytes[CONNECTION_FIELD_RESERVED]
        }

    val addressType: Byte
        get() {
            return bytes[CONNECTION_FIELD_ADDRESS_TYPE]
        }

    val connectionAddress: String
        get() {
            return when (addressType) {
                ADDRESS_TYPE_IPv4 ->
                    ByteBuffer.wrap(bytes.copyOfRange(4, 8)).int.toIP()
                ADDRESS_TYPE_IPv6 ->
                    bytes.copyOfRange(4, 20).contentToString()
                else -> {
                    bytes.copyOfRange(5, bytes.size - 2).decodeToString()
                }
            }
        }

    val bytes: ByteArray
        get() {
            return messageBytes.copyOf()
        }

    val port: Int
        get() {
            return bytes.takeLast(2).toByteArray().let { portBytes ->
                logd(TAG, "PORT BYTES = ${portBytes.contentToString()}")
                val portByte = (portBytes[0] and 0xff.toByte()) shl 8 or (portBytes[1] and 0xff.toByte())
                val port = portByte.toInt()
                val uPort = portByte.toUByte().toInt()
                logd(TAG, "Request portByte = $portByte\n")
                logd(TAG, "Request port = $port\n")
                logd(TAG, "Request uPort = $uPort\n")

                port.takeIf { it > 0 } ?: (uPort * 2) + abs(port)
            }
        }

    override fun toString(): String {
        return """${"\n"}   --- SOCKS5 MESSAGE ---   
            Version = ${CONNECTION_FIELDS[CONNECTION_FIELD_SOCKS_VERSION]?.get(version)}
            Socks Command = ${CONNECTION_FIELDS[CONNECTION_FIELD_COMMAND]?.get(socksCommand)}
            Reserved Byte = ${CONNECTION_FIELDS[CONNECTION_FIELD_RESERVED]?.get(reservedByte)}
            Address Type = ${CONNECTION_FIELDS[CONNECTION_FIELD_ADDRESS_TYPE]?.get(addressType)}
            Address = $connectionAddress
            Port = $port
            """.trimMargin()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Socks5Message

        if (!bytes.contentEquals(other.bytes)) return false
        if (mode != other.mode) return false

        return true
    }

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + mode.hashCode()
        return result
    }

    sealed class SocksMessageMode {
        object ServerMessage : SocksMessageMode()
        object ClientMessage : SocksMessageMode()
    }
}