package net.monetizemyapp

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import androidx.work.*
import net.monetizemyapp.android.MonetizationSettingsActivity
import net.monetizemyapp.android.PromptActivity
import net.monetizemyapp.android.ProxyServiceWorker
import net.monetizemyapp.network.*
import net.monetizemyapp.toolbox.extentions.logd

object MonetizeMyApp {

    @JvmStatic
    fun init(context: Context) {

        val callback = object : Application.ActivityLifecycleCallbacks {
            override fun onActivityPaused(activity: Activity?) {}
            override fun onActivityResumed(activity: Activity?) {}
            override fun onActivityStarted(activity: Activity?) {}
            override fun onActivityDestroyed(activity: Activity?) {}
            override fun onActivitySaveInstanceState(activity: Activity?, outState: Bundle?) {}
            override fun onActivityStopped(activity: Activity?) {}

            override fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?) {
                logd("ActivityLifecycleCallbacks", "onActivityCreated : ${activity?.localClassName}")

                context.prefs.getString(
                    PREFS_KEY_MODE,
                    PREFS_VALUE_MODE_UNSELECTED
                ).takeIf { it != PREFS_VALUE_MODE_UNSELECTED }?.let { mode ->
                    if (mode == PREFS_VALUE_MODE_PROXY) {
                        scheduleServiceStart(context.applicationContext)
                    }
                    removeActivityListener(context, this)
                    return
                } ?: activity?.intent?.action?.equals(Intent.ACTION_MAIN)?.takeIf { it }?.let {
                    startPromptActivity(context, 500L)
                }
            }
        }

        (context.applicationContext as? Application)?.registerActivityLifecycleCallbacks(callback)
            ?: (context as? Activity)?.application?.registerActivityLifecycleCallbacks(callback)
            ?: startPromptActivity(context, 3_000L)
    }

    private fun removeActivityListener(context: Context, callback: Application.ActivityLifecycleCallbacks) {
        (context.applicationContext as? Application)?.unregisterActivityLifecycleCallbacks(callback)
            ?: (context as? Activity)?.application?.unregisterActivityLifecycleCallbacks(callback)
    }

    /**
     * Launches Prompt Activity after delay.
     * @param delay time in millis before activity will be launched.
     **/
    private fun startPromptActivity(context: Context, delay: Long) {
        Handler().postDelayed({
            val intent = Intent(context, PromptActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.putExtra(EXTRA_PACKAGE_NAME, context.packageName)
            context.startActivity(intent)
        }, delay)
    }

    /**
     * Launches monetization settings Activity.
     **/
    fun openSettings(context: Context) {
        val intent = Intent(context, MonetizationSettingsActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.putExtra(EXTRA_PACKAGE_NAME, context.packageName)
        context.startActivity(intent)
    }

    fun scheduleServiceStart(context: Context) {
        logd(Properties.APP_TAG, "schedule Proxy Worker Start")

        val batteryStatusIntent = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            context.registerReceiver(null, ifilter)
        }
        val batteryLevel: Float = batteryStatusIntent?.let { intent ->
            val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            level / scale.toFloat()
        } ?: Properties.Worker.REQURED_BATTERY_LEVEL

        val status = batteryStatusIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING
                || status == BatteryManager.BATTERY_STATUS_FULL


        logd(Properties.APP_TAG, "BATTERY LEVEL = $batteryLevel")
        logd(Properties.APP_TAG, "BATTERY CHARGING = $isCharging")

        if (batteryLevel >= Properties.Worker.REQURED_BATTERY_LEVEL || isCharging) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .build()

            val proxyWorkRequest = OneTimeWorkRequestBuilder<ProxyServiceWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance()
                .enqueueUniqueWork(Properties.Worker.PROXY_WORK_ID, ExistingWorkPolicy.REPLACE, proxyWorkRequest)
        }
    }

}