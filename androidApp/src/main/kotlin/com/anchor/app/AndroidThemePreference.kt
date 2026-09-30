package com.anchor.app

import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.Window
import androidx.core.view.WindowCompat
import com.anchor.app.settings.ThemeChoice

internal class AndroidThemePreference(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    var choice: ThemeChoice
        get() {
            val stored = preferences.getString(KEY, null) ?: return ThemeChoice.System
            return ThemeChoice.entries.firstOrNull { it.name == stored } ?: ThemeChoice.System
        }
        set(value) {
            preferences.edit().putString(KEY, value.name).apply()
        }

    companion object {
        const val PREFERENCES = "anchor-theme"
        private const val KEY = "choice"
    }
}

@Suppress("DEPRECATION")
internal fun applyAnchorSystemBars(window: Window, view: View, dark: Boolean) {
    val color = if (dark) Color.parseColor("#1C1B18") else Color.parseColor("#FCF9F3")
    window.statusBarColor = color
    window.navigationBarColor = color
    WindowCompat.getInsetsController(window, view).apply {
        isAppearanceLightStatusBars = !dark
        isAppearanceLightNavigationBars = !dark
    }
}
