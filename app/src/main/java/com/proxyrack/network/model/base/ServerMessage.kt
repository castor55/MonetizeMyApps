package com.proxyrack.network.model.base

abstract class ServerMessage

class ServerMessageType {
    companion object {
        val PING = "ping"
        val TOKEN = "backconnect"
    }
}
