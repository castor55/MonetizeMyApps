package com.proxyrack.network.model

data class Message(
    val type: String,
    val deviceId: String,
    val city: String,
    val country: String
)
