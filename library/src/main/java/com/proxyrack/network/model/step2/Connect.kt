package com.proxyrack.network.model.step2

import com.google.gson.annotations.SerializedName
import com.proxyrack.network.model.base.ClientMessage
import com.proxyrack.network.model.base.ClientMessageType

data class Connect(
    @SerializedName("msg")
    val body: ConnectBody,
    val type: String = ClientMessageType.CONNECT
) : ClientMessage()

data class ConnectBody(
    val token: String
)