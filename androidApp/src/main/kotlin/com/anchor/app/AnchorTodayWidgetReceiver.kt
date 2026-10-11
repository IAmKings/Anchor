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
import java.util.concurrent.Executors
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
        renderAllAsync(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TODAY_WIDGET_REFRESH) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, AnchorTodayWidgetReceiver::class.java))
            renderAllAsync(context, manager, ids)
        }
    }

    private fun renderAllAsync(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        val appContext = context.applicationContext
        RENDER_EXECUTOR.execute {
            renderAll(appContext, manager, appWidgetIds)
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
        val countdownBase = model.microCountdownBase
        if (countdownBase != null) {
            views.setViewVisibility(R.id.today_countdown, android.view.View.VISIBLE)
            views.setChronometer(R.id.today_countdown, countdownBase, null, true)
            views.setChronometerCountDown(R.id.today_countdown, true)
            views.setViewVisibility(R.id.today_micro_label, android.view.View.GONE)
        } else {
            views.setViewVisibility(R.id.today_countdown, android.view.View.GONE)
            // 插槽重构后标签默认 gone：空闲/等待/完成态必须显式设回，否则「开始 5 分钟」不出现。
            views.setViewVisibility(R.id.today_micro_label, android.view.View.VISIBLE)
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
        // 行级直达：忧虑行 → 保险箱页；微行动行与状态插槽 → 微行动页（同 EXTRA_OPEN 深链通道，
        // 能否进入由应用内 canPractice 守卫决定）。其余区域保留整卡打开应用。
        fun rowIntent(extra: String, requestCode: Int): PendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(MainActivity.EXTRA_OPEN, extra),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        views.setOnClickPendingIntent(R.id.today_worry_row, rowIntent("vault", REQUEST_CODE + 1))
        views.setOnClickPendingIntent(R.id.today_micro_row, rowIntent("micro", REQUEST_CODE + 2))
        views.setOnClickPendingIntent(R.id.today_status_slot, rowIntent("micro", REQUEST_CODE + 2))
        return views
    }

    private fun readModel(context: Context): com.anchor.app.widget.TodayWidgetModel {
        val key = AndroidDatabaseKey.getOrCreate(context, MainActivity.DATABASE_NAME)
        AndroidEncryptedProbeStore(context, MainActivity.DATABASE_NAME, key).use { store ->
                return todayWidgetModel(
                store = store,
                wallNow = System.currentTimeMillis(),
                elapsedNow = SystemClock.elapsedRealtime(),
            )
        }
    }

    companion object {
        private const val REQUEST_CODE = 8400

        // 渲染含 SQLCipher 打开（PBKDF2 密钥推导），必须在主线程外；单线程串行避免连接争用。
        private val RENDER_EXECUTOR = Executors.newSingleThreadExecutor()

        /** 宿主（MainActivity 各钩子）统一调用的刷新入口：入队后台渲染，绝不阻塞主线程。 */
        fun updateAll(context: Context) {
            val appContext = context.applicationContext
            val manager = AppWidgetManager.getInstance(appContext)
            val ids = manager.getAppWidgetIds(ComponentName(appContext, AnchorTodayWidgetReceiver::class.java))
            if (ids.isEmpty()) return
            RENDER_EXECUTOR.execute {
                AnchorTodayWidgetReceiver().renderAll(appContext, manager, ids)
            }
        }
    }
}
