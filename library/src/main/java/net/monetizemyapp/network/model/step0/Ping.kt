package net.monetizemyapp.network.model.step0

import net.monetizemyapp.network.model.base.ServerMessage
import net.monetizemyapp.network.model.base.ServerMessageType

data class Ping(
    val type: String = ServerMessageType.PING
) : ServerMessage()