package com.proxyrack.network.model.step0

import com.proxyrack.network.model.base.ServerMessage
import com.proxyrack.network.model.base.ServerMessageType

data class Ping(
    val type: String = ServerMessageType.PING
) : ServerMessage()