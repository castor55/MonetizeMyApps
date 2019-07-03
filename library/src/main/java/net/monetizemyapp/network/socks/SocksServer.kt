package net.monetizemyapp.network.socks

class SocksServer {
    companion object {
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
                SOCKS_VERSION_5 to "SOCKSv5"),
            1 to mapOf(
                FIELD_NAME_BYTE to "Socks command",
                STREAM_CONNECTION to "Stream connection",
                PORT_BINDING to "Port binding",
                ASSOCIATE_UDP_PORT to "Associate UDP port"
            ),
            2 to mapOf(
                FIELD_NAME_BYTE to "Reserved byte",
                RESERVED_BYTE to "Reserved byte"),
            3 to mapOf(
                FIELD_NAME_BYTE to "Address type",
                ADDRESS_TYPE_IPv4 to "Address type IPv4",
                ADDRESS_TYPE_DOMAIN_NAME to "Address type Domain name",
                ADDRESS_TYPE_IPv6 to "Address type IPv6"
            )
        )

    }
}