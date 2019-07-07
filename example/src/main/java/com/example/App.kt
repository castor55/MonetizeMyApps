package com.example

import android.app.Application
import android.os.StrictMode
import android.os.StrictMode.setThreadPolicy
import net.monetizemyapp.MonetizeMyApp


class App : Application() {

    override fun onCreate() {
        super.onCreate()
        MonetizeMyApp.init(applicationContext)
    }
}