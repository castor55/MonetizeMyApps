package com.example

import android.app.Application
import net.monetizemyapp.MonetizeMyApp

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        MonetizeMyApp.init(applicationContext)
    }
}