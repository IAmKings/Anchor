package com.anchor.app.widget

import com.anchor.app.home.homeMicroActionActionLabel
import com.anchor.app.home.homeMicroActionTimeUpLabel
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
    /** RemoteViews Chronometer 的 base（elapsedRealtime 时基，倒计时模式）；null = 不显示。
     *  到点切换依赖 setAlarmClock 的秒级准时间钟（豁免 OEM 降级），负数窗口已消除。 */
    val microCountdownBase: Long?,
    val worryLine: String,
)

private const val FIVE_MINUTES = 5L * 60 * 1_000

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

    // 标题用组件短文案（行首图标已表达「微行动」语义，带「微行动 · 」前缀在 2×2 宽度下会换行）。
    val microTitle: String
    val microLabel: String
    var countdownBase: Long? = null
    when {
        waiting -> {
            microTitle = "就医等待期"
            microLabel = "暂停练习，不是惩罚"
        }
        active != null -> {
            val remaining = (active.startedAtMillis!! + FIVE_MINUTES - wallNow).coerceAtLeast(0)
            if (remaining > 0) {
                microTitle = "进行中"
                microLabel = ""
                countdownBase = elapsedNow + remaining
            } else {
                microTitle = "时间到"
                microLabel = homeMicroActionTimeUpLabel
            }
        }
        else -> {
            microTitle = "微行动"
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
        worryLine = worryLine,
    )
}
