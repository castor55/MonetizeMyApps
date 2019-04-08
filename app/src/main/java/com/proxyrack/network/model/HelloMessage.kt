package com.proxyrack.network.model

import com.google.gson.annotations.SerializedName

data class HelloMessage(
    val type: String = "hello",
    @SerializedName("msg")
    val body: HelloMessageBody
)

data class HelloMessageBody(
    @SerializedName("id")
    val deviceId: String,
    val city: String,
    @SerializedName("country")
    val countryCode: String,
    val os: SystemInfo
)

data class SystemInfo(
    @SerializedName("type")
    val os: String = "Android",
    @SerializedName("release")
    val sdkVersion: String,
    @SerializedName("arch")
    val architecture: String
)