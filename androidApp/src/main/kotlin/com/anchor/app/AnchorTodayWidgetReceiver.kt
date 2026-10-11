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
import com.anchor.app.widget.EXTRA_REFRESH_TIMER_KIND
import com.anchor.app.widget.todayWidgetModel
import com.anchor.app.widget.timeUpWidgetModel

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
            // 微行动到点：零库读取快翻结束态（抢在 Chronometer 过零前），全量渲染随后校正缓存陈旧；
            // 浪潮到点与「今天」组件无关，走全量渲染按现值重算，绝不错误快翻微行动状态。
            if (intent.getStringExtra(EXTRA_REFRESH_TIMER_KIND) == "MicroAction") {
                fastFlipTimeUp(context, manager, ids)
            } else {
                renderAllAsync(context, manager, ids)
            }
        }
    }

    /** 零库读取的结束态快翻：缓存锚点名/忧虑行 + 时间到常量，毫秒级消除 Chronometer 负数窗口。 */
    private fun fastFlipTimeUp(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        if (appWidgetIds.isEmpty()) return
        val cache = context.getSharedPreferences(WIDGET_CACHE_PREFERENCES, Context.MODE_PRIVATE)
        val model = timeUpWidgetModel(
            anchorTitle = cache.getString(CACHE_ANCHOR, "锚点") ?: "锚点",
            worryLine = cache.getString(CACHE_WORRY, "") ?: "",
        )
        val views = buildViews(context, model)
        appWidgetIds.forEach { id -> manager.updateAppWidget(id, views) }
    }

    private fun renderAllAsync(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        val appContext = context.applicationContext
        RENDER_EXECUTOR.execute {
            renderAll(appContext, manager, appWidgetIds)
        }
    }

    private fun renderAll(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        if (appWidgetIds.isEmpty()) return
        val model = readModel(context)
        val views = buildViews(context, model)
        appWidgetIds.forEach { id -> manager.updateAppWidget(id, views) }
        // 全量渲染后写显示缓存：供到点快翻零库读取（缓存陈旧由快翻后的本次全量渲染顺序保证最新）。
        context.getSharedPreferences(WIDGET_CACHE_PREFERENCES, Context.MODE_PRIVATE)
            .edit()
            .putString(CACHE_ANCHOR, model.anchorTitle)
            .putString(CACHE_WORRY, model.worryLine)
            .apply()
    }

    private fun buildViews(context: Context): RemoteViews {
        val model = readModel(context)
        return buildViews(context, model)
    }

    private fun buildViews(context: Context, model: com.anchor.app.widget.TodayWidgetModel): RemoteViews {
        val palette = WidgetTheming.paletteFor(context)
        val views = RemoteViews(context.packageName, R.layout.anchor_widget_today)
        WidgetTheming.applyBackground(views, palette, R.id.today_root)
        WidgetTheming.applyIconTint(views, palette, R.id.today_anchor_icon, R.id.today_micro_icon, R.id.today_worry_icon)
        WidgetTheming.applyText(views, palette, R.id.today_anchor, R.id.today_micro_title)
        WidgetTheming.applyMuted(views, palette, R.id.today_micro_label, R.id.today_worry)
        WidgetTheming.applyPrimaryText(views, palette, R.id.today_countdown)
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
        // R2：倒计时存续期间排「余量边界」刷新（deadline − 3s），到点触发 R1 把 Chronometer 换成结束态；
        // 即使此闹钟被 OEM 拖过零点，到点准点闹钟（R3）也会再次刷新收敛——负数只可能闪现亚秒级。
        val refreshPi = PendingIntent.getBroadcast(
            context,
            REFRESH_REQUEST_CODE,
            Intent(context, AnchorTodayWidgetReceiver::class.java).setAction(ACTION_TODAY_WIDGET_REFRESH),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val alarm = context.getSystemService(AlarmManager::class.java)
        val refreshAtWall = model.microCountdownRefreshAtWall
        if (refreshAtWall != null) {
            alarm.cancel(refreshPi)
            alarm.setAlarmClock(
                AlarmManager.AlarmClockInfo(refreshAtWall, showIntent(context)),
                refreshPi,
            )
        } else {
            alarm.cancel(refreshPi)
        }
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

    private fun showIntent(context: Context): PendingIntent {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent().setPackage(context.packageName)
        return PendingIntent.getActivity(
            context,
            0,
            launch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
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
        private const val REFRESH_REQUEST_CODE = 8420
        private const val WIDGET_CACHE_PREFERENCES = "m0-widget-cache"
        private const val CACHE_ANCHOR = "anchor_title"
        private const val CACHE_WORRY = "worry_line"

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
