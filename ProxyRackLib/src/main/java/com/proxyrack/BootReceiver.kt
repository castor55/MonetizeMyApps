package com.proxyrack

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        if (!BuildConfig.DEBUG) {
            context.startService(Intent(context, ProxyService::class.java))
        }
    }
}
