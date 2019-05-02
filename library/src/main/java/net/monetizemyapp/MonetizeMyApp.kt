package net.monetizemyapp

import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import net.monetizemyapp.android.ProxyServiceStarter

object MonetizeMyApp {

    @JvmStatic
    fun init() {

        scheduleServiceStart()
    }

    private fun scheduleServiceStart() {
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