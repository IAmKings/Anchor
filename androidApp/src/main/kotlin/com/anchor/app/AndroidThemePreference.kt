package com.anchor.app

import android.app.UiModeManager
import android.content.Context
import android.graphics.Color
import android.os.Build
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

private const val APPLIED_NIGHT_MODE = "night-mode-applied"

/**
 * Persists the app night mode so the next cold start's system splash uses the saved theme.
 * 跟随系统 is left alone until an explicit light or dark choice has been applied once.
 * [UiModeManager.MODE_NIGHT_AUTO] clears that override (the platform stores "unspecified").
 */
internal fun syncAnchorNightMode(context: Context, choice: ThemeChoice) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    val preferences = context.getSharedPreferences(
        AndroidThemePreference.PREFERENCES,
        Context.MODE_PRIVATE,
    )
    val applied = preferences.getString(APPLIED_NIGHT_MODE, null)
    if (applied == choice.name) return
    if (choice == ThemeChoice.System && applied == null) {
        preferences.edit().putString(APPLIED_NIGHT_MODE, choice.name).apply()
        return
    }
    preferences.edit().putString(APPLIED_NIGHT_MODE, choice.name).commit()
    val mode = when (choice) {
        ThemeChoice.Light -> UiModeManager.MODE_NIGHT_NO
        ThemeChoice.Dark -> UiModeManager.MODE_NIGHT_YES
        ThemeChoice.System -> UiModeManager.MODE_NIGHT_AUTO
    }
    val uiMode = context.getSystemService(UiModeManager::class.java) ?: return
    runCatching { uiMode.setApplicationNightMode(mode) }
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
