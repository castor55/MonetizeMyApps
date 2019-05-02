package net.monetizemyapp

import android.content.Context
import android.content.Intent
import net.monetizemyapp.android.ProxyService

object MonetizeMyApp {

    @JvmStatic
    fun init(context: Context) {

        context.startService(Intent(context, ProxyService::class.java))
    }
}