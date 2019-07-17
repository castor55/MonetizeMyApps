package net.monetizemyapp.android

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.paris.extensions.style
import com.proxyrack.R
import kotlinx.android.synthetic.main.activity_settings_monetization.*
import net.monetizemyapp.MonetizeMyApp
import net.monetizemyapp.network.*

class MonetizationSettingsActivity : AppCompatActivity() {

    companion object {
        internal const val EXTRA_ENABLED_MONETIZATION_OPTIONS = "enabled_monetization_options"
    }

    private val enabledMonetizationOptions by lazy {
        intent.extras?.get(EXTRA_ENABLED_MONETIZATION_OPTIONS) as? Array<MonetizeMyApp.MonetizationOptions>
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings_monetization)

        title = getString(R.string.settings)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)

        setupButtons()
    }

    private fun setupButtons() {
        enabledMonetizationOptions?.let { options ->
            if (options.find { it is MonetizeMyApp.MonetizationOptions.Subscription } == null) {
                btnSubscription.visibility = View.GONE
            }
        }

        resetButtonStyles()
        setSelectedButtonStyle()
        setClickListeners()
    }

    private fun setClickListeners() {
        btnFree.setOnClickListener {
            prefs.edit().putString(PREFS_KEY_MODE, PREFS_VALUE_MODE_PROXY).apply()
            finish()
        }
        btnAds.setOnClickListener {
            prefs.edit().putString(PREFS_KEY_MODE, PREFS_VALUE_MODE_ADS).apply()
            finish()
        }
        btnSubscription.setOnClickListener {
            prefs.edit().putString(PREFS_KEY_MODE, PREFS_VALUE_MODE_SUBSCRIPTION).apply()
            finish()
        }
    }

    private fun resetButtonStyles() {
        btnFree.style { add(R.style.ButtonUnselected) }
        btnAds.style { add(R.style.ButtonUnselected) }
        btnSubscription.style { add(R.style.ButtonUnselected) }

        hintFree.style { add(R.style.HintUnselected) }
        hintAds.style { add(R.style.HintUnselected) }
        hintSubscription.style { add(R.style.HintUnselected) }
    }

    private fun setSelectedButtonStyle() {
        when (prefs.getString(PREFS_KEY_MODE, PREFS_VALUE_MODE_UNSELECTED)) {
            PREFS_VALUE_MODE_PROXY -> {
                btnFree.style {
                    add(R.style.ButtonSelected)
                }
                hintFree.style {
                    add(R.style.HintSelected)
                }
            }
            PREFS_VALUE_MODE_ADS -> {
                btnAds.style {
                    add(R.style.ButtonSelected)
                }
                hintAds.style {
                    add(R.style.HintSelected)
                }
            }
            PREFS_VALUE_MODE_SUBSCRIPTION -> {
                btnSubscription.style {
                    add(R.style.ButtonSelected)
                }
                hintSubscription.style {
                    add(R.style.HintSelected)
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
