package com.anchor.app

import android.content.Context
import android.view.WindowManager
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidPrivacyShieldTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @After
    fun resetAppLock() {
        context.getSharedPreferences("app-lock", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun secureWindowFollowsPersistedAppLock() {
        assertSecureFlag(enabled = false, expected = 0)
        assertSecureFlag(enabled = true, expected = WindowManager.LayoutParams.FLAG_SECURE)
    }

    private fun assertSecureFlag(enabled: Boolean, expected: Int) {
        context.getSharedPreferences("app-lock", Context.MODE_PRIVATE)
            .edit().putBoolean("enabled", enabled).commit()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertEquals(expected, activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }
}
