package net.monetizemyapp.toolbox.extentions

import android.content.Context
import android.util.Log
import com.proxyrack.BuildConfig
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException


fun Any.logd(tag: String = this.javaClass.canonicalName ?: this.javaClass.name, text: String?) {
    if (!BuildConfig.DEBUG) return
    Log.d(tag, text ?: "")
}

fun Any.loge(tag: String = this.javaClass.canonicalName ?: this.javaClass.name, text: String?) {
    if (!BuildConfig.DEBUG) return
    Log.e(tag, text ?: "")
}

fun appendLogToFile(context: Context, text: String) {
    if (!BuildConfig.DEBUG) return
    val appDirectory = File(context.getExternalFilesDir(null)?.path + "/MonetizeMyApp")
    val logFileName = "Proxy_logs"
    val logFile = File(appDirectory, "/logcat$logFileName" /*+ ".txt"*/)

    if (!appDirectory.exists()) {
        try {
            appDirectory.mkdir()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
    if (!logFile.exists()) {
        try {
            logFile.createNewFile()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
    try {
        //BufferedWriter for performance, true to set append to file flag
        val buf = BufferedWriter(FileWriter(logFile, true))
        buf.append(text)
        buf.newLine()
        buf.close()
    } catch (e: IOException) {
        e.printStackTrace()
    }
}
