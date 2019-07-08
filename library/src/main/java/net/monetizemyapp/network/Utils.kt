package net.monetizemyapp.network

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.preference.PreferenceManager
import com.google.gson.Gson
import net.monetizemyapp.network.model.step1.SystemInfo
import java.util.*


var uniqueId: String? = null

val deviceId: String
    get() {
        if (uniqueId == null) {
            uniqueId = UUID.randomUUID().toString()
                .replace("-", "")
                .take(16)
        }
        return "$uniqueId"
    }

@SuppressLint("HardwareIds")
fun getSystemInfo(): SystemInfo {
    return SystemInfo(
        sdkVersion = Build.VERSION.RELEASE,
        architecture = System.getProperty("os.arch")!!
    )
}


inline fun <reified T> String.fromJson(): T {
    return Gson().fromJson<T>(this, T::class.java)
}

fun Int.toIP(): String = """
        ${this shr 24 and 0xFF}.${this shr 16 and 0xFF}.${this shr 8 and 0xFF}.${this and 0xFF}
        """.trimIndent()

infix fun Byte.shl(shift: Int): Byte = (this.toInt() shl shift).toByte()

val Context.prefs: SharedPreferences
    get() = PreferenceManager.getDefaultSharedPreferences(this)

const val EXTRA_PACKAGE_NAME = "package_name"
const val PREFS_KEY_MODE = "service_enabled"
const val PREFS_VALUE_MODE_UNSELECTED = "mode_unselected"
const val PREFS_VALUE_MODE_PROXY = "mode_proxy"
const val PREFS_VALUE_MODE_ADS = "mode_ads"
const val PREFS_VALUE_MODE_SUBSCRIPTION = "mode_subscription"