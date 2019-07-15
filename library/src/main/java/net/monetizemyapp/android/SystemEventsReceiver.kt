package net.monetizemyapp.android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import net.monetizemyapp.MonetizeMyApp

class SystemEventsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            MonetizeMyApp.init(context.applicationContext)
        }
    }
}
