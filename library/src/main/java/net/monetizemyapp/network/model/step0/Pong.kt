package net.monetizemyapp.network.model.step0

import com.google.gson.annotations.SerializedName
import net.monetizemyapp.network.model.base.ClientMessage
import net.monetizemyapp.network.model.base.ClientMessageType

data class Pong(
    @SerializedName("type")
    val type: String = ClientMessageType.PONG
) : ClientMessage()