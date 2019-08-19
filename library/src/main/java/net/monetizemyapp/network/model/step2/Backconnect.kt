package net.monetizemyapp.network.model.step2

import com.google.gson.annotations.SerializedName
import net.monetizemyapp.network.model.base.ServerMessage
import net.monetizemyapp.network.model.base.ServerMessageType

data class Backconnect(
    @SerializedName("type")
    val type: String = ServerMessageType.BACKCONNECT,
    @SerializedName("msg")
    val body: BackconnectBody
) : ServerMessage()

data class BackconnectBody(
    @SerializedName("token")
    val token: String
)