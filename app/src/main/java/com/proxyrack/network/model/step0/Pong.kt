package com.proxyrack.network.model.step0

import com.proxyrack.network.model.base.ClientMessageType

data class Pong(
    val type: String = ClientMessageType.PONG
)