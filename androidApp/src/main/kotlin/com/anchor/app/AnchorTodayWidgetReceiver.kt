package com.anchor.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.widget.RemoteViews
import com.anchor.app.storage.AndroidDatabaseKey
import com.anchor.app.storage.AndroidEncryptedProbeStore
import com.anchor.app.widget.ACTION_TODAY_WIDGET_REFRESH
import com.anchor.app.widget.todayWidgetModel

/**
 * 「锚点 · 今天」状态小组件：当前锚点、微行动状态（进行中显示 Chronometer 倒计时）、
 * 忧虑保险箱张数与下一场专场。
 *
 * 刷新钩子：onUpdate（系统）、WIDGET_REFRESH 广播（后台计时到点）、MainActivity.onResume /
 * 计时启停 / 数据变更回调。文案由 shared 的 todayWidgetModel 产出，与首页同一套 HomeCopy；
 * 无 streak、无完成率、陶土红禁用。
 */
class AnchorTodayWidgetReceiver : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        renderAll(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TODAY_WIDGET_REFRESH || intent.action == ACTION_TODAY_WIDGET_TICK) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, AnchorTodayWidgetReceiver::class.java))
            renderAll(context, manager, ids)
        }
    }

    private fun renderAll(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        if (appWidgetIds.isEmpty()) return
        val views = buildViews(context)
        appWidgetIds.forEach { id -> manager.updateAppWidget(id, views) }
    }

    private fun buildViews(context: Context): RemoteViews {
        val model = readModel(context)
        val views = RemoteViews(context.packageName, R.layout.anchor_widget_today)
        views.setTextViewText(R.id.today_anchor, model.anchorTitle)
        views.setTextViewText(R.id.today_micro_title, model.microTitle)
        views.setTextViewText(R.id.today_micro_label, model.microLabel)
        val countdownText = model.microCountdownText
        if (countdownText != null) {
            views.setViewVisibility(R.id.today_countdown, android.view.View.VISIBLE)
            views.setTextViewText(R.id.today_countdown, countdownText)
            // 自续链：每次渲染倒计时态都排一个下一分钟边界的刷新；到点后模型按墙钟切「时间到」，链自然终止。
            val alarm = context.getSystemService(AlarmManager::class.java)
            val tick = PendingIntent.getBroadcast(
                context,
                TICK_REQUEST_CODE,
                Intent(context, AnchorTodayWidgetReceiver::class.java).setAction(ACTION_TODAY_WIDGET_TICK),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            alarm.cancel(tick)
            alarm.setWindow(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + 60_000L,
                60_000L,
                tick,
            )
        } else {
            views.setViewVisibility(R.id.today_countdown, android.view.View.GONE)
            context.getSystemService(AlarmManager::class.java)
                .cancel(
                    PendingIntent.getBroadcast(
                        context,
                        TICK_REQUEST_CODE,
                        Intent(context, AnchorTodayWidgetReceiver::class.java).setAction(ACTION_TODAY_WIDGET_TICK),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    ),
                )
        }
        views.setTextViewText(R.id.today_worry, model.worryLine)
        // 整卡点按 = 打开应用（无 extra），进入哪页由应用自己决定
        val pending = PendingIntent.getActivity(
            context,
            REQUEST_CODE,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        views.setOnClickPendingIntent(R.id.today_root, pending)
        return views
    }

    private fun readModel(context: Context): com.anchor.app.widget.TodayWidgetModel {
        val key = AndroidDatabaseKey.getOrCreate(context, MainActivity.DATABASE_NAME)
        AndroidEncryptedProbeStore(context, MainActivity.DATABASE_NAME, key).use { store ->
                return todayWidgetModel(store = store, wallNow = System.currentTimeMillis())
        }
    }

    companion object {
        private const val REQUEST_CODE = 8400
        private const val TICK_REQUEST_CODE = 8410
        private const val ACTION_TODAY_WIDGET_TICK = "com.anchor.app.action.TODAY_WIDGET_TICK"

        /** 宿主（MainActivity 各钩子）统一调用的刷新入口。 */
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, AnchorTodayWidgetReceiver::class.java))
            if (ids.isEmpty()) return
            AnchorTodayWidgetReceiver().onUpdate(context, manager, ids)
        }
    }
}
