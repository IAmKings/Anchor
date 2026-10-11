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
    /** RemoteViews Chronometer 的 base（elapsedRealtime 时基，倒计时模式）；null = 不显示。 */
    val microCountdownBase: Long?,
    /** R2：倒计时安全余量边界的墙钟时刻（deadline − M），宿主据此调度提前刷新闹钟；null = 无需调度。 */
    val microCountdownRefreshAtWall: Long?,
    val worryLine: String,
)

private const val FIVE_MINUTES = 5L * 60 * 1_000

/**
 * 倒计时安全余量：剩余 ≤ 此值时一律渲染「时间到」，绝不渲染 Chronometer。
 * 到点刷新链路（闹钟唤醒→广播→开库→桌面应用）实测存在 0.5–3 秒延迟，
 * Chronometer 过零后无法钳位会显示负数——本余量就是给这条链路的缓冲。
 */
const val WIDGET_COUNTDOWN_MARGIN_MILLIS = 3_000L

/**
 * @param microDeadlineElapsed 微行动后台计时器存的 deadline（elapsedRealtime 时基，见
 *   AndroidBackgroundTimer 的 prefs；0 或 null 表示无计时），与 store 的 startedAt 互为印证。
 */
fun todayWidgetModel(store: AnchorStore, wallNow: Long, elapsedNow: Long): TodayWidgetModel {
    val profile = store.userProfile()
    val waiting = store.safetyState().mode == SafetyMode.MedicalWaiting

    val actions = store.microActions()
    val active = actions.firstOrNull { it.startedAtMillis != null && it.completedAtMillis == null }
    // 与首页 featured 同序：进行中优先 → 最新未完成 → 任意第一条
    val featured = active ?: actions.firstOrNull { it.completedAtMillis == null } ?: actions.firstOrNull()

    val microTitle: String
    val microLabel: String
    var countdownBase: Long? = null
    var countdownRefreshAtWall: Long? = null
    when {
        waiting -> {
            microTitle = "就医等待期"
            microLabel = "暂停练习，不是惩罚"
        }
        active != null -> {
            val remaining = (active.startedAtMillis!! + FIVE_MINUTES - wallNow).coerceAtLeast(0)
            if (remaining > WIDGET_COUNTDOWN_MARGIN_MILLIS) {
                microTitle = homeMicroActionTitle(running = true)
                microLabel = ""
                countdownBase = elapsedNow + remaining
                // R2：在余量边界（deadline − M）安排一次刷新，让 R1 在过零前把 Chronometer 换成结束态。
                countdownRefreshAtWall = wallNow + (remaining - WIDGET_COUNTDOWN_MARGIN_MILLIS)
            } else {
                // R1 不变量：剩余进入安全余量（含 ≤0）后，任何渲染都是结束态——数据层不产生负数倒计时。
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
        microCountdownBase = countdownBase,
        microCountdownRefreshAtWall = countdownRefreshAtWall,
        worryLine = worryLine,
    )
}
