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

internal const val EXTRA_PACKAGE_NAME = "package_name"
internal const val PREFS_KEY_MODE = "service_enabled"
internal const val PREFS_VALUE_MODE_UNSELECTED = "mode_unselected"
internal const val PREFS_VALUE_MODE_PROXY = "mode_proxy"
internal const val PREFS_VALUE_MODE_ADS = "mode_ads"
internal const val PREFS_VALUE_MODE_SUBSCRIPTION = "mode_subscription"