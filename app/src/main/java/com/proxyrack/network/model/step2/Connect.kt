package com.proxyrack.network.model.step2

import com.google.gson.annotations.SerializedName
import com.proxyrack.network.model.base.ClientMessageType
import com.proxyrack.network.model.base.ServerMessage

data class Connect(
    val type: String = ClientMessageType.CONNECT,
    @SerializedName("msg")
    val body: ConnectBody
) : ServerMessage()

data class ConnectBody(
    val token: String
)