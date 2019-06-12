package net.monetizemyapp.android

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_prompt.*
import net.monetizemyapp.MonetizeMyApp
import net.monetizemyapp.network.EXTRA_PACKAGE_NAME
import net.monetizemyapp.network.PREFS_PROMPT_SHOWN
import net.monetizemyapp.network.PREFS_SERVICE_ENABLED
import net.monetizemyapp.network.prefs

class PromptActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(com.proxyrack.R.layout.activity_prompt)

        showParentAppIcon()

        btnAgree.setOnClickListener {
            prefs.edit().putBoolean(PREFS_SERVICE_ENABLED, true).apply()
            prefs.edit().putBoolean(PREFS_PROMPT_SHOWN, true).apply()

            MonetizeMyApp.scheduleServiceStart()
            finish()
        }
        btnDisagree.setOnClickListener {
            prefs.edit().putBoolean(PREFS_SERVICE_ENABLED, false).apply()
            prefs.edit().putBoolean(PREFS_PROMPT_SHOWN, true).apply()

            finish()
        }
    }

    private fun showParentAppIcon() {
        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME)

        try {
            val icon = packageManager.getApplicationIcon(packageName)
            ivIcon.setImageDrawable(icon)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            header.visibility = View.GONE
        }
    }
}
