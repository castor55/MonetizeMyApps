package net.monetizemyapp.network.model.step0

import net.monetizemyapp.network.model.base.ClientMessage
import net.monetizemyapp.network.model.base.ClientMessageType

data class Pong(
    val type: String = ClientMessageType.PONG
) : ClientMessage()