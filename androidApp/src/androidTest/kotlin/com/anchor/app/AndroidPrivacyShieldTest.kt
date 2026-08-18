package com.anchor.app

import android.content.Context
import android.content.Intent
import android.view.WindowManager
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidPrivacyShieldTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @After
    fun clearInstrumentedLockPrefs() {
        context.getSharedPreferences(TEST_PREFERENCES, Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun secureWindowFollowsShieldFlagWithoutOpeningMainActivity() {
        assertSecureFlag(enabled = false, expected = 0)
        assertSecureFlag(enabled = true, expected = WindowManager.LayoutParams.FLAG_SECURE)
    }

    @Test
    fun biometricLockWritesOnlyIsolatedPreferences() {
        val production = context.getSharedPreferences(AndroidBiometricLock.PREFERENCES, Context.MODE_PRIVATE)
        val before = production.all.toMap()

        ActivityScenario.launch(PrivacyShieldHostActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val lock = AndroidBiometricLock(activity, TEST_PREFERENCES)
                lock.enabled = true
                assertTrue(AndroidBiometricLock(activity, TEST_PREFERENCES).enabled)
                assertEquals(before["enabled"], production.all["enabled"])
                lock.enabled = false
                assertFalse(AndroidBiometricLock(activity, TEST_PREFERENCES).enabled)
            }
        }

        assertEquals(before, production.all.toMap())
        assertFalse(
            context.getSharedPreferences(TEST_PREFERENCES, Context.MODE_PRIVATE)
                .getBoolean("enabled", false),
        )
    }

    private fun assertSecureFlag(enabled: Boolean, expected: Int) {
        val intent = Intent(context, PrivacyShieldHostActivity::class.java)
            .putExtra(PrivacyShieldHostActivity.EXTRA_ENABLED, enabled)
        ActivityScenario.launch<PrivacyShieldHostActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                assertEquals(expected, activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }

    private companion object {
        const val TEST_PREFERENCES = "app-lock-instrumented"
    }
}
