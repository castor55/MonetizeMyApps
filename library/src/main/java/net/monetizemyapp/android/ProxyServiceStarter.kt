package net.monetizemyapp.android

import android.content.Context
import android.content.Intent
import androidx.work.Worker
import androidx.work.WorkerParameters

class ProxyServiceStarter(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {

    @ExperimentalUnsignedTypes
    @ExperimentalStdlibApi
    override fun doWork(): Result {
        return try {
            applicationContext.startService(Intent(applicationContext, ProxyService::class.java))
            Result.success()
        } catch (e: IllegalStateException) {
            Result.failure()
        }
    }
}