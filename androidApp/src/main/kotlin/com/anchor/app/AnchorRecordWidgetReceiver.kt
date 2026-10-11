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

    private companion object {
        const val REQUEST_CODE_BASE = 8500
    }
}
