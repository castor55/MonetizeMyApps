package net.monetizemyapp.network

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.preference.PreferenceManager
import com.google.gson.Gson
import net.monetizemyapp.network.model.step1.SystemInfo
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.io.InputStream
import java.util.*


var uniqueId: String? = null

@SuppressLint("HardwareIds")
fun getDeviceId(ip: String): String {
    if (uniqueId == null) {
        uniqueId = UUID.randomUUID().toString()
            .replace("-", "")
            .take(16)
    }
    return "${uniqueId}_$ip"
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

fun InputStream.getString(): String {

    val data = ByteArray(2048)
    val length = read(data)

    val result = String(data, 0, length)
    return result.removeSuffix(endOfString())
}

fun InputStream.getBytes(): ByteArray {

    val data = ByteArray(512)
    val length = read(data)

    return data.copyOfRange(0, length)
}

fun endOfString(): String {
    return String(Character.toChars(Integer.parseInt("0000", 16)))
}

inline fun <reified T> String.toObject(): T {
    return Gson().fromJson<T>(this, T::class.java)
}

fun Int.toIP(): String {
    return (this shr 24 and 0xFF).toString() + "." +
            (this shr 16 and 0xFF) + "." +
            (this shr 8 and 0xFF) + "." +
            (this and 0xFF)
}

val Context.prefs: SharedPreferences
    get() = PreferenceManager.getDefaultSharedPreferences(this)

const val EXTRA_PACKAGE_NAME = "package_name"
const val PREFS_KEY_MODE = "service_enabled"
const val PREFS_VALUE_MODE_UNSELECTED = "mode_unselected"
const val PREFS_VALUE_MODE_PROXY = "mode_proxy"
const val PREFS_VALUE_MODE_ADS = "mode_ads"
const val PREFS_VALUE_MODE_SUBSCRIPTION = "mode_subscription"