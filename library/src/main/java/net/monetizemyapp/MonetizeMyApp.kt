package net.monetizemyapp

import android.content.Context
import android.content.Intent
import android.os.Handler
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import net.monetizemyapp.android.PromptActivity
import net.monetizemyapp.android.ProxyServiceStarter
import net.monetizemyapp.network.*

object MonetizeMyApp {

    @JvmStatic
    fun init(context: Context) {

        val mode = context.prefs.getString(PREFS_KEY_MODE, PREFS_VALUE_MODE_UNSELECTED)

        if (mode == PREFS_VALUE_MODE_UNSELECTED) {

            Handler().postDelayed({
                val intent = Intent(context, PromptActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.putExtra(EXTRA_PACKAGE_NAME, context.packageName)
                context.startActivity(intent)
            }, 3000)

        } else if (mode == PREFS_VALUE_MODE_PROXY) {
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