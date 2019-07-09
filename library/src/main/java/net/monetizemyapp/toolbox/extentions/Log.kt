package net.monetizemyapp.toolbox.extentions

import android.util.Log
import com.proxyrack.BuildConfig

fun Any.logd(tag: String = this.javaClass.canonicalName ?: this.javaClass.name, text: String?) {
    if (!BuildConfig.DEBUG) return
    Log.d(tag, text ?: "")
}

fun Any.loge(tag: String = this.javaClass.canonicalName ?: this.javaClass.name, text: String?) {
    if (!BuildConfig.DEBUG) return
    Log.e(tag, text ?: "")
}

