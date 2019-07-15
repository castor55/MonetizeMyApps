package net.monetizemyapp.android

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_prompt.*
import net.monetizemyapp.MonetizeMyApp
import net.monetizemyapp.network.*
import net.monetizemyapp.toolbox.extentions.gone

class PromptActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(com.proxyrack.R.layout.activity_prompt)

        showParentAppIcon()

        btnAgree.setOnClickListener {
            prefs.edit().putString(PREFS_KEY_MODE, PREFS_VALUE_MODE_PROXY).apply()

            MonetizeMyApp.scheduleServiceStart(this)
            finish()
        }
        btnDisagree.setOnClickListener {
            prefs.edit().putString(PREFS_KEY_MODE, PREFS_VALUE_MODE_ADS).apply()

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
            header.gone()
        }
    }
}
