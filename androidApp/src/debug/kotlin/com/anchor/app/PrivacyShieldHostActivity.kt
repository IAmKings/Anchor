package com.anchor.app

import android.os.Bundle
import androidx.fragment.app.FragmentActivity

class PrivacyShieldHostActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyPrivacyShield(window, intent.getBooleanExtra(EXTRA_ENABLED, false))
    }

    companion object {
        const val EXTRA_ENABLED = "privacy-shield-enabled"
    }
}
