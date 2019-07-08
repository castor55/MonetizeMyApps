package net.monetizemyapp

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import net.monetizemyapp.android.MonetizationSettingsActivity
import net.monetizemyapp.android.PromptActivity
import net.monetizemyapp.android.ProxyServiceStarter
import net.monetizemyapp.network.*
import net.monetizemyapp.toolbox.extentions.logd
import java.util.concurrent.TimeUnit

object MonetizeMyApp {

    @JvmStatic
    fun init(context: Context) {

        val mode = context.prefs.getString(PREFS_KEY_MODE, PREFS_VALUE_MODE_UNSELECTED)

        if (mode == PREFS_VALUE_MODE_UNSELECTED) {
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
                    ).takeIf { it != PREFS_VALUE_MODE_UNSELECTED }?.let {
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

        } else if (mode == PREFS_VALUE_MODE_PROXY) {
            scheduleServiceStart()
        }
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

    fun scheduleServiceStart() {

        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .build()

        val uploadWorkRequest = OneTimeWorkRequestBuilder<ProxyServiceStarter>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance().enqueue(uploadWorkRequest)
    }
}