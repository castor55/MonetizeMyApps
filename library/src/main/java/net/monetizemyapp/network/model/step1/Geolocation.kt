package net.monetizemyapp.network.model.step1

import com.google.gson.annotations.SerializedName

data class Geolocation(
    val city: String,
    val countryCode: String,
    @SerializedName("query") val ip: String
)