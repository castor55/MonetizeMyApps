package com.proxyrack.network.model

import com.google.gson.annotations.SerializedName

data class HelloResponse(
    val type: String = "backconnect",
    @SerializedName("msg")
    val body: HelloResponseBody
)

data class HelloResponseBody(
    val token: String
)