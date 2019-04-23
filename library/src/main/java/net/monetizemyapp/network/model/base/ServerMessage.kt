package net.monetizemyapp.network.model.base

abstract class ServerMessage

class ServerMessageType {
    companion object {
        val PING = "ping"
        val BACKCONNECT = "backconnect"
    }
}

class ServerMessageEmpty : ServerMessage()