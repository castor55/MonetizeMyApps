package com.proxyrack.android

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings.Secure
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

@SuppressLint("HardwareIds")
fun Context.getDeviceId(): String {
    return Secure.getString(
        contentResolver,
        Secure.ANDROID_ID
    )
}

fun createRetrofitClient(url: String): Retrofit {
    return Retrofit.Builder()
        .baseUrl(url)
        .addConverterFactory(GsonConverterFactory.create())
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .build()
}