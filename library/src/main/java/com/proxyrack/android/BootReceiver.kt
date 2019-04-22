package com.proxyrack.android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.proxyrack.ProxyService

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            context.startService(Intent(context, ProxyService::class.java))
        }
    }
}
