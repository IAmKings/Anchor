package com.anchor.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/**
 * 「锚点 · 快捷」桌面小组件：三个直达入口（挂卡 / 浪潮等待 / 微行动）。
 *
 * 无数据读取、无刷新调度（updatePeriodMillis=0）、零用户内容——桌面常驻但不制造待办压力。
 * 深链通过 EXTRA_OPEN 交给 MainActivity（singleTop），能否进入由应用内 canPractice 守卫决定：
 * 就医等待期或未添加对应锚点时，点击只会打开应用首页，不进入练习。
 */
class AnchorQuickWidgetReceiver : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val views = RemoteViews(context.packageName, R.layout.anchor_widget_quick)
        val launch = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val targets = listOf(
            R.id.widget_hang to "hang",
            R.id.widget_wave to "wave",
            R.id.widget_micro to "micro",
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
        const val REQUEST_CODE_BASE = 8300
    }
}
