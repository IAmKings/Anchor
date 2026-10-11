package com.anchor.app.widget

import com.anchor.app.home.homeMicroActionActionLabel
import com.anchor.app.home.homeMicroActionTimeUpLabel
import com.anchor.app.home.homeMicroActionTimeUpTitle
import com.anchor.app.home.homeMicroActionTitle
import com.anchor.app.home.homeWorryBody
import com.anchor.app.onboarding.firstAnchorTitle
import com.anchor.app.storage.AnchorStore
import com.anchor.app.safety.SafetyMode
import com.anchor.app.storage.WorryResolution
import com.anchor.app.worry.AndroidWorrySessionClock

/** 「今天」状态小组件与宿主间的刷新广播：计时到点等后台事件触发 widget 重渲染。 */
const val ACTION_TODAY_WIDGET_REFRESH = "com.anchor.app.action.TODAY_WIDGET_REFRESH"

/**
 * 「今天」小组件的渲染模型。文案与首页同一套 HomeCopy，防 KPI 化措辞由共享层保证；
 * androidApp 侧只负责把模型映射到 RemoteViews。
 */
data class TodayWidgetModel(
    val anchorTitle: String,
    val microTitle: String,
    val microLabel: String,
    /** 倒计时的分钟级展示文本（如「约 3 分」）；null = 不显示。静态文本 + 自续链闹钟刷新，
     *  弃用 Chronometer：OEM 策略下精确闹钟可降级为最长 1 小时窗口，Chronometer 过零后无法钳位会显示负数。 */
    val microCountdownText: String?,
    val worryLine: String,
)

private const val FIVE_MINUTES = 5L * 60 * 1_000

/**
 * @param microDeadlineElapsed 微行动后台计时器存的 deadline（elapsedRealtime 时基，见
 *   AndroidBackgroundTimer 的 prefs；0 或 null 表示无计时），与 store 的 startedAt 互为印证。
 */
fun todayWidgetModel(store: AnchorStore, wallNow: Long): TodayWidgetModel {
    val profile = store.userProfile()
    val waiting = store.safetyState().mode == SafetyMode.MedicalWaiting

    val actions = store.microActions()
    val active = actions.firstOrNull { it.startedAtMillis != null && it.completedAtMillis == null }
    // 与首页 featured 同序：进行中优先 → 最新未完成 → 任意第一条
    val featured = active ?: actions.firstOrNull { it.completedAtMillis == null } ?: actions.firstOrNull()

    val microTitle: String
    val microLabel: String
    var countdownText: String? = null
    when {
        waiting -> {
            microTitle = "就医等待期"
            microLabel = "暂停练习，不是惩罚"
        }
        active != null -> {
            val remaining = (active.startedAtMillis!! + FIVE_MINUTES - wallNow).coerceAtLeast(0)
            if (remaining > 0) {
                microTitle = homeMicroActionTitle(running = true)
                microLabel = ""
                // 分钟粒度：模型永远按墙钟判定，任何迟到刷新都不会产生负数或低于真实剩余的展示。
                countdownText = "约 ${remaining / 60_000L + 1} 分"
            } else {
                microTitle = homeMicroActionTimeUpTitle
                microLabel = homeMicroActionTimeUpLabel
            }
        }
        else -> {
            microTitle = homeMicroActionTitle(running = false)
            microLabel = homeMicroActionActionLabel(
                hasTitle = featured?.title != null,
                running = false,
                completed = featured?.completedAtMillis != null,
            )
        }
    }

    val pendingWorry = store.worryCards().count { it.resolution == WorryResolution.Pending }
    val worryLine = homeWorryBody(
        pendingCount = pendingWorry,
        sessionOpen = AndroidWorrySessionClock.isOpen(wallNow),
        nextLabel = AndroidWorrySessionClock.nextSessionLabel(wallNow),
    )

    return TodayWidgetModel(
        anchorTitle = profile.firstAnchor?.let(::firstAnchorTitle) ?: "锚点",
        microTitle = microTitle,
        microLabel = microLabel,
        microCountdownText = countdownText,
        worryLine = worryLine,
    )
}
