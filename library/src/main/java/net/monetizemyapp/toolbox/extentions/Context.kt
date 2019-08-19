package net.monetizemyapp.toolbox.extentions

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import net.monetizemyapp.Properties
import net.monetizemyapp.android.data.BatteryInfo
import net.monetizemyapp.network.prefs
import java.util.*

fun Context?.getAppInfo() = this?.let {
    applicationContext
        ?.packageManager
        ?.getApplicationInfo(applicationContext.packageName, PackageManager.GET_META_DATA)
}

fun Context?.getBatteryInfo(): BatteryInfo =
    this?.let {
        val batteryStatusIntent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val batteryLevel: Float = batteryStatusIntent?.let { intent ->
            val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            level / scale.toFloat()
        } ?: Properties.Worker.REQUIRED_BATTERY_LEVEL

        val status = batteryStatusIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING
                || status == BatteryManager.BATTERY_STATUS_FULL


        logd(Properties.APP_TAG, "BATTERY LEVEL = $batteryLevel")
        logd(Properties.APP_TAG, "BATTERY CHARGING = $isCharging")
        BatteryInfo(batteryLevel, isCharging)
    } ?: BatteryInfo(0f, false)

fun Context?.getApplicationName(): String? = this?.let { context ->
    context.applicationInfo.let { appInfo ->
        val stringId = appInfo.labelRes
        stringId.takeIf { it == 0 }?.let { appInfo.nonLocalizedLabel.toString() } ?: context.getString(stringId)
    }
}

val Context.deviceId: String
    get() = prefs.getString("unique_device_id", null)
        ?: run {
            UUID.randomUUID().toString()
                .replace("-", "")
                .take(16)
                .also { randomDeviceId ->
                    prefs.edit().putString("unique_device_id", randomDeviceId).apply()
                }
        }



