package com.proxyrack.android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.proxyrack.BuildConfig

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        if (!BuildConfig.DEBUG) {
            context.startService(Intent(context, ProxyService::class.java))
        }
    }
}
