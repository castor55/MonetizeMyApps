package net.monetizemyapp

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.work.*
import net.monetizemyapp.android.MonetizationSettingsActivity
import net.monetizemyapp.android.PromptActivity
import net.monetizemyapp.android.ProxyServerWorker
import net.monetizemyapp.network.*
import net.monetizemyapp.toolbox.extentions.logd
import java.io.Serializable
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit

object MonetizeMyApp {

    private var contextRef: WeakReference<Context>? = null

    private val optionsMap by lazy {
        mapOf(
            PREFS_VALUE_MODE_UNSELECTED to MonetizationOptions.Unselected,
            PREFS_VALUE_MODE_PROXY to MonetizationOptions.Free,
            PREFS_VALUE_MODE_ADS to MonetizationOptions.Ads,
            PREFS_VALUE_MODE_SUBSCRIPTION to MonetizationOptions.Subscription
        )
    }

    val currentOption: MonetizationOptions
        get() = optionsMap.getOrElse(
            contextRef?.get()?.prefs?.getString(
                PREFS_KEY_MODE,
                PREFS_VALUE_MODE_UNSELECTED
            ) ?: PREFS_VALUE_MODE_UNSELECTED
        ) { MonetizeMyApp.MonetizationOptions.Unselected }


    @JvmStatic
    fun init(context: Context) {
        contextRef = WeakReference(context)

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
                        scheduleServiceStart(StartMode.PeriodicRestart)
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
    @JvmOverloads
    fun openSettings(
        context: Context, enabledOptions: Array<MonetizationOptions> = arrayOf(
            MonetizationOptions.Subscription
        )
    ) {
        val intent = Intent(context, MonetizationSettingsActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.putExtra(MonetizationSettingsActivity.EXTRA_ENABLED_MONETIZATION_OPTIONS, enabledOptions)
        context.startActivity(intent)
    }

    internal fun scheduleServiceStart(startMode: StartMode) {
        logd(Properties.APP_TAG, "schedule Proxy Worker Start")

        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .build()

        if (startMode is StartMode.SingeLaunch) {
            val proxyStartWorkRequest = OneTimeWorkRequestBuilder<ProxyServerWorker>()
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance()
                .enqueueUniqueWork(
                    Properties.Worker.PROXY_WORK_ID,
                    ExistingWorkPolicy.REPLACE,
                    proxyStartWorkRequest
                )
        } else {
            val proxyStartWorkRequest = PeriodicWorkRequestBuilder<ProxyServerWorker>(
                PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS,
                TimeUnit.MILLISECONDS
            ).setConstraints(constraints)
                .build()
            WorkManager.getInstance().enqueueUniquePeriodicWork(
                Properties.Worker.PROXY_RESTART_WORK_ID,
                ExistingPeriodicWorkPolicy.REPLACE,
                proxyStartWorkRequest
            )
        }
    }

    internal sealed class StartMode {
        object SingeLaunch : StartMode()
        object PeriodicRestart : StartMode()
    }

    sealed class MonetizationOptions : Serializable {
        object Subscription : MonetizationOptions()
        object Ads : MonetizationOptions()
        object Free : MonetizationOptions()
        object Unselected : MonetizationOptions()
    }

}