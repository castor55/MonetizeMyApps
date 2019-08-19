package net.monetizemyapp.network.model.step1

import com.google.gson.annotations.SerializedName
import net.monetizemyapp.network.model.base.ClientMessage
import net.monetizemyapp.network.model.base.ClientMessageType

data class Hello(
    @SerializedName("msg")
    val body: HelloBody,
    @SerializedName("type")
    val type: String = ClientMessageType.HELLO
) : ClientMessage()

data class HelloBody(
    @SerializedName("clientKey")
    val clientKey: String,
    @SerializedName("ip")
    val ip: String,
    @SerializedName("id")
    val deviceId: String,
    @SerializedName("city")
    val city: String,
    @SerializedName("country")
    val countryCode: String,
    @SerializedName("os")
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