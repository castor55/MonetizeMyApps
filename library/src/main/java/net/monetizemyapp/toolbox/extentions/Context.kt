package net.monetizemyapp.toolbox.extentions

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import net.monetizemyapp.Properties

fun Context?.getAppInfo() = this?.let {
    applicationContext
        ?.packageManager
        ?.getApplicationInfo(applicationContext.packageName, PackageManager.GET_META_DATA)
}

data class BatteryInfo(val level: Float, val isCharging: Boolean){
    override fun toString(): String {
        return "\n\t------\n\tBattery Level = $level\n\tIs Charging =$isCharging"
    }
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

fun Context?.getApplicationName(): String? = this?.let {
    it.applicationInfo.let { appInfo ->
        val stringId = appInfo.labelRes
        stringId.takeIf { it == 0 }?.let { appInfo.nonLocalizedLabel.toString() } ?: it.getString(stringId)
    }
}

