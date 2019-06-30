package net.monetizemyapp.toolbox.extentions

import com.google.gson.Gson
import net.monetizemyapp.network.fromJson
import net.monetizemyapp.network.model.base.ServerMessage
import net.monetizemyapp.network.model.base.ServerMessageEmpty
import net.monetizemyapp.network.model.base.ServerMessageType
import net.monetizemyapp.network.model.step0.Ping
import net.monetizemyapp.network.model.step2.Backconnect

fun Any?.toJson() = Gson().toJson(this) + endOfString()

fun String?.toObject(): ServerMessage {
    return when {
        this?.contains(ServerMessageType.BACKCONNECT) == true -> this.fromJson<Backconnect>()
        this?.contains(ServerMessageType.PING) == true -> this.fromJson<Ping>()
        else -> ServerMessageEmpty()
    }
}

fun endOfString(): String {
    return String(Character.toChars(Integer.parseInt("0000", 16)))
}

