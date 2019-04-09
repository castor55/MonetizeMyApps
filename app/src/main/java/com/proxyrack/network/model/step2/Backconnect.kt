package com.proxyrack.network.model.step2

import com.google.gson.annotations.SerializedName
import com.proxyrack.network.model.base.ServerMessage
import com.proxyrack.network.model.base.ServerMessageType

data class Backconnect(
    val type: String = ServerMessageType.BACKCONNECT,
    @SerializedName("msg")
    val body: BackconnectBody
) : ServerMessage()

data class BackconnectBody(
    val token: String
)