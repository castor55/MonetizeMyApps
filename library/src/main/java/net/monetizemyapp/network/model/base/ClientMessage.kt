package net.monetizemyapp.network.model.base

abstract class ClientMessage

class ClientMessageType {
    companion object {
        val HELLO = "hello"
        val PONG = "pong"
        val CONNECT = "reverseCon"
    }
}