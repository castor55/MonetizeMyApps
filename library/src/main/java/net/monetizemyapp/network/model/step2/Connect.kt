package net.monetizemyapp.network.model.step2

import com.google.gson.annotations.SerializedName
import net.monetizemyapp.network.model.base.ClientMessage
import net.monetizemyapp.network.model.base.ClientMessageType

data class Connect(
    @SerializedName("msg")
    val body: ConnectBody,
    @SerializedName("type")
    val type: String = ClientMessageType.CONNECT
) : ClientMessage()

data class ConnectBody(
    @SerializedName("token")
    val token: String
)