package com.example

import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import net.monetizemyapp.MonetizeMyApp

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bntSettings?.setOnClickListener {
            MonetizeMyApp.openSettings(this)
        }

        Handler().postDelayed({ MonetizeMyApp.openSettings(this) }, 1000)
    }
}