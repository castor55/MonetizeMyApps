package net.monetizemyapp.android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BatteryLevelReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BATTERY_OKAY) {
            context.startService(Intent(context, ProxyService::class.java))
        } else {
            context.stopService(Intent(context, ProxyService::class.java))
        }
    }
}