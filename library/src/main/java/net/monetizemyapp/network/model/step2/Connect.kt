package net.monetizemyapp.network.model.step2

import com.google.gson.annotations.SerializedName
import net.monetizemyapp.network.model.base.ClientMessage
import net.monetizemyapp.network.model.base.ClientMessageType

data class Connect(
    @SerializedName("msg")
    val body: ConnectBody,
    val type: String = ClientMessageType.CONNECT
) : ClientMessage()

data class ConnectBody(
    val token: String
)