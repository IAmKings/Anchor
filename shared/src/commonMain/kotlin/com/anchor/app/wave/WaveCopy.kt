package com.anchor.app.wave

import kotlin.math.PI
import kotlin.math.sin

internal const val WAVE_TEN_MINUTES = 10L * 60 * 1_000

data class BodyLocation(
    val shortLabel: String,
    val fullLabel: String,
)

val waveBodyLocations = listOf(
    BodyLocation("胸口", "胸口发紧"),
    BodyLocation("喉咙", "喉咙堵"),
    BodyLocation("手心", "手心出汗"),
    BodyLocation("胃部", "胃部收紧"),
    BodyLocation("肩膀", "肩膀僵"),
    BodyLocation("头皮", "头皮发麻"),
    BodyLocation("呼吸", "呼吸浅"),
    BodyLocation("面部", "面部发烫"),
)

internal fun waveRecognizeTitle(): String = "我现在很难受，这是真的。"
internal fun waveRecognizeBody(): String = "承认这种感觉。不需要对抗，只需要看见它。"
internal fun waveRecognizeAction(): String = "我看见了"

internal fun waveLocateTitle(): String = "这种感觉，停在哪里？"
internal fun waveLocateBody(): String = "深呼吸，感受身体的哪个部位最沉重。"
internal fun waveLocateContinue(): String = "继续"

internal fun waveWaitTitle(done: Boolean): String =
    if (done) "它自己退了。你没有掐掉它，它也会走。" else "我们一起等浪潮过去"

internal fun waveWaitBody(location: String?): String = when {
    !location.isNullOrBlank() -> "留意：$location"
    else -> "什么都不用做，只是呼吸。"
}

internal fun waveAddTenMinutes(): String = "再加 10 分钟"
internal fun waveLeave(): String = "离开"
internal fun waveBackToToday(): String = "回到今天"
internal fun waveNoteEmotion(): String = "顺手记一张情绪卡"

/** Shown once before the first round when exact alarms are not granted. */
internal fun waveExactAlarmPromptTitle(): String = "想让到点提醒准时吗"

internal fun waveExactAlarmPromptBody(): String =
    "还没开系统的「闹钟和提醒」。开着的话，练习一到点，提醒就准时进通知栏；不开，手机可能把它推迟几分钟。之后也可以在「我的」里再开。"

internal const val waveExactAlarmPromptAllow = "去开启"
internal const val waveExactAlarmPromptSkip = "先不用"

internal const val waveHomeTitle = "浪潮等待"
internal const val waveHomeIdleStatus = "等待中"
internal const val waveHomeCaption = "难受的时候点这里"

/** Above this, the stored deadline is not a live round (for example after reboot). */
internal const val waveHomeStatusMaxMillis = 12L * 60 * 60 * 1_000

/** Minute label while a round is running. Rounds up, and never says 0 while time remains. */
internal fun waveHomeStatus(remainingMillis: Long): String {
    if (remainingMillis <= 0L || remainingMillis > waveHomeStatusMaxMillis) return waveHomeIdleStatus
    val minutes = (remainingMillis + 60_000L - 1L) / 60_000L
    return "还剩 $minutes 分钟"
}

internal fun waveHomeContentDescription(status: String): String =
    "$waveHomeTitle，$status。$waveHomeCaption"

/** Smallest breathing scale. The layout slot is the largest size, so this never grows past it. */
internal const val waveBreathMinScale = 0.82f

/** Share of the smallest circle used by the recognize copy. Below the inscribed square (0.707). */
internal const val waveRecognizeCopyFraction = 0.68f

internal fun waveBreathUnit(t: Float, phase: Float): Float {
    val wave = sin(2.0 * PI * (t + phase))
    return ((wave + 1.0) / 2.0).toFloat().coerceIn(0f, 1f)
}

internal fun waveBreathScale(unit: Float): Float {
    val clamped = unit.coerceIn(0f, 1f)
    return waveBreathMinScale + (1f - waveBreathMinScale) * clamped
}

internal fun waveRecognizeCopyWidth(maxDiameter: Float): Float =
    maxDiameter * waveBreathMinScale * waveRecognizeCopyFraction
