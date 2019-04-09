package com.proxyrack.network.model.step1

import com.google.gson.annotations.SerializedName
import com.proxyrack.network.model.base.ServerMessage
import com.proxyrack.network.model.base.ServerMessageType

data class Token(
    val type: String = ServerMessageType.TOKEN,
    @SerializedName("msg")
    val body: TokenBody
) : ServerMessage()

data class TokenBody(
    val token: String
)