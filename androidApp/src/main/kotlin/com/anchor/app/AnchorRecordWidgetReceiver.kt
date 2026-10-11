package com.anchor.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/**
 * 「锚点 · 记录」桌面小组件：双栏日志与情绪标签箱两个记录入口，样式与「锚点 · 快捷」一致。
 *
 * 深链复用 EXTRA_OPEN 通道（facts/emotion），守卫与记录页同语义：
 * facts 无条件可达（记录工具，等待期仍可写）；emotion 非等待期且可练习/有历史卡。
 */
class AnchorRecordWidgetReceiver : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val views = RemoteViews(context.packageName, R.layout.anchor_widget_record)
        val palette = WidgetTheming.paletteFor(context)
        WidgetTheming.applyBackground(views, palette, R.id.record_root)
        WidgetTheming.applyIconTint(views, palette, R.id.record_anchor_icon, R.id.record_facts_icon, R.id.record_emotion_icon)
        WidgetTheming.applyText(views, palette, R.id.record_title, R.id.record_facts_label, R.id.record_emotion_label)
        val launch = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val targets = listOf(
            R.id.record_facts to "facts",
            R.id.record_emotion to "emotion",
        )
        targets.forEachIndexed { index, (viewId, open) ->
            val intent = Intent(launch).putExtra(MainActivity.EXTRA_OPEN, open)
            val pending = PendingIntent.getActivity(
                context,
                REQUEST_CODE_BASE + index,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(viewId, pending)
        }
        appWidgetIds.forEach { id -> appWidgetManager.updateAppWidget(id, views) }
    }

    companion object {
    /** 宿主统一刷新入口（主题切换等外观事件）。 */
    fun updateAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, AnchorRecordWidgetReceiver::class.java))
        if (ids.isEmpty()) return
        AnchorRecordWidgetReceiver().onUpdate(context, manager, ids)
    }
        private const val REQUEST_CODE_BASE = 8500
    }
}
