package net.monetizemyapp.android

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.proxyrack.R
import kotlinx.android.synthetic.main.activity_prompt.*
import net.monetizemyapp.MonetizeMyApp
import net.monetizemyapp.network.*
import net.monetizemyapp.toolbox.extentions.gone

class PromptActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_prompt)

        showParentAppIcon()

        btnAgree.setOnClickListener {
            prefs.edit().putString(PREFS_KEY_MODE, PREFS_VALUE_MODE_PROXY).apply()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                if (!(applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager)
                        .isIgnoringBatteryOptimizations(getPackageNameFromBundle())
                ) {
                    try {
                        startActivity(
                            Intent().apply {
                                @SuppressLint("BatteryLife")
                                action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                                data = Uri.parse("package:${getPackageNameFromBundle()}")
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            })
                    } catch (e: ActivityNotFoundException) {
                        Toast.makeText(
                            applicationContext,
                            getString(R.string.dissable_battery_optimization),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }

            MonetizeMyApp.scheduleServiceStart(MonetizeMyApp.StartMode.PeriodicRestart)
            finish()
        }
        btnDisagree.setOnClickListener {
            prefs.edit().putString(PREFS_KEY_MODE, PREFS_VALUE_MODE_ADS).apply()
            finish()
        }
    }

    override fun onResume() {
        prefs.getString(
            PREFS_KEY_MODE,
            PREFS_VALUE_MODE_UNSELECTED
        ).takeIf { it != PREFS_VALUE_MODE_UNSELECTED }?.let { finish() }
        super.onResume()
    }

    private fun getPackageNameFromBundle() = intent.getStringExtra(EXTRA_PACKAGE_NAME)
    private fun showParentAppIcon() {
        try {
            val icon = packageManager.getApplicationIcon(getPackageNameFromBundle())
            ivIcon.setImageDrawable(icon)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            header.gone()
        }
    }
}
