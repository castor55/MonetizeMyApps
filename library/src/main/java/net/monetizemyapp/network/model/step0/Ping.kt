package net.monetizemyapp.network.model.step0

import com.google.gson.annotations.SerializedName
import net.monetizemyapp.network.model.base.ServerMessage
import net.monetizemyapp.network.model.base.ServerMessageType

data class Ping(
    @SerializedName("type")
    val type: String = ServerMessageType.PING
) : ServerMessage()