package net.monetizemyapp.toolbox.extentions

import android.content.Context
import android.content.pm.PackageManager

fun Context?.getAppInfo() = this?.let {
    applicationContext
        ?.packageManager
        ?.getApplicationInfo(applicationContext.packageName, PackageManager.GET_META_DATA)
}