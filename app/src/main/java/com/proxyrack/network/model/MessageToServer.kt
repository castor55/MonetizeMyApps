package com.proxyrack.network.model

import com.google.gson.annotations.SerializedName

data class MessageToServer(
    val type: String,
    val deviceId: String,
    val city: String,
    @SerializedName("countryCode")
    val country: String
)
