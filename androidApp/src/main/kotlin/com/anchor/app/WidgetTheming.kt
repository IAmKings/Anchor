package com.anchor.app

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.anchor.app.settings.ThemeChoice

/**
 * 小组件与「应用内主题选择」的一致性：应用选浅色/深色时，组件强制对应色板（RemoteViews 的
 * day/night 资源只跟随系统，需要显式覆盖）；应用选「跟随系统」时返回 null——维持资源自动机制。
 *
 * 色值不重复定义：通过强制 uiMode 的上下文解析同一批 day/night 颜色资源（anchor_widget_*），
 * 资源仍是唯一事实源。
 */
data class WidgetPalette(
    val backgroundRes: Int,
    val onBackground: Int,
    val muted: Int,
    val primary: Int,
)

object WidgetTheming {

    fun paletteFor(context: Context): WidgetPalette? {
        val choice = AndroidThemePreference(context).choice
        if (choice == ThemeChoice.System) return null
        val night = choice == ThemeChoice.Dark
        val themed = context.themeContext(night)
        return WidgetPalette(
            backgroundRes = if (night) R.drawable.anchor_widget_bg_dark else R.drawable.anchor_widget_bg_light,
            onBackground = ContextCompat.getColor(themed, R.color.anchor_widget_on_background),
            muted = ContextCompat.getColor(themed, R.color.anchor_widget_muted),
            primary = ContextCompat.getColor(themed, R.color.anchor_widget_primary),
        )
    }

    private fun Context.themeContext(night: Boolean): Context {
        val conf = Configuration(resources.configuration)
        conf.uiMode = (conf.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
            (if (night) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO)
        return createConfigurationContext(conf)
    }

    fun applyText(views: RemoteViews, palette: WidgetPalette?, vararg viewIds: Int) {
        if (palette == null) return
        viewIds.forEach { views.setTextColor(it, palette.onBackground) }
    }

    fun applyMuted(views: RemoteViews, palette: WidgetPalette?, vararg viewIds: Int) {
        if (palette == null) return
        viewIds.forEach { views.setTextColor(it, palette.muted) }
    }

    fun applyPrimaryText(views: RemoteViews, palette: WidgetPalette?, vararg viewIds: Int) {
        if (palette == null) return
        viewIds.forEach { views.setTextColor(it, palette.primary) }
    }

    fun applyIconTint(views: RemoteViews, palette: WidgetPalette?, vararg viewIds: Int) {
        if (palette == null) return
        viewIds.forEach { views.setInt(it, "setColorFilter", palette.primary) }
    }

    fun applyBackground(views: RemoteViews, palette: WidgetPalette?, viewId: Int) {
        if (palette == null) return
        views.setInt(viewId, "setBackgroundResource", palette.backgroundRes)
    }
}
