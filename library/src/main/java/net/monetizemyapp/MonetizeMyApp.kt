package net.monetizemyapp

import android.content.Context
import android.content.Intent
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import net.monetizemyapp.android.PromptActivity
import net.monetizemyapp.android.ProxyServiceStarter
import net.monetizemyapp.network.EXTRA_PACKAGE_NAME
import net.monetizemyapp.network.PREFS_PROMPT_SHOWN
import net.monetizemyapp.network.PREFS_SERVICE_ENABLED
import net.monetizemyapp.network.prefs

object MonetizeMyApp {

    @JvmStatic
    fun init(context: Context) {

        if (!context.prefs.getBoolean(PREFS_PROMPT_SHOWN, false)) {

            val intent = Intent(context, PromptActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.putExtra(EXTRA_PACKAGE_NAME, context.packageName)
            context.startActivity(intent)

        } else if (context.prefs.getBoolean(PREFS_SERVICE_ENABLED, false)) {
            scheduleServiceStart()
        }
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