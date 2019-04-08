package com.proxyrack.network.model

import com.google.gson.annotations.SerializedName

data class ConnectMessage(
    val type: String = "reverseCon",
    @SerializedName("msg")
    val body: ConnectMessageBody
)

data class ConnectMessageBody(
    val token: String
)