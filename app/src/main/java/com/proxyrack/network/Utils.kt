package com.proxyrack.network

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings.Secure
import com.google.gson.Gson
import com.proxyrack.network.model.step1.SystemInfo
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.io.InputStream

@SuppressLint("HardwareIds")
fun Context.getDeviceId(): String {
    return Secure.getString(
        contentResolver,
        Secure.ANDROID_ID
    )
}

@SuppressLint("HardwareIds")
fun getSystemInfo(): SystemInfo {
    return SystemInfo(
        sdkVersion = Build.VERSION.RELEASE,
        architecture = System.getProperty("os.arch")!!
    )
}

fun createRetrofitClient(url: String): Retrofit {
    return Retrofit.Builder()
        .baseUrl(url)
        .addConverterFactory(GsonConverterFactory.create())
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .build()
}

/**
 * Checking for response from server
 */
fun InputStream.getString(): String {

    val data = ByteArray(2048)
    val length = read(data)

    val result = String(data, 0, length)

    return result.removeSuffix(endOfString())
}

fun endOfString(): String {
    return String(Character.toChars(Integer.parseInt("0000", 16)))
}

inline fun <reified T> String.toObject(): T {
    return Gson().fromJson<T>(this, T::class.java)
}