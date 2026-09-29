package com.anchor.app.onboarding

import com.anchor.app.safety.AssessmentBand
import com.anchor.app.safety.SafetyOutcome
import com.anchor.app.safety.SafetyPolicy
import com.anchor.app.safety.SafetyState
import com.anchor.app.safety.StillWaitingKind
import com.anchor.app.storage.MicroAction
import com.anchor.app.storage.RhythmEntry
import com.anchor.app.storage.StoredAssessment

internal const val REASSESSMENT_INTERVAL_MILLIS = 14L * 24 * 60 * 60 * 1_000
internal const val MEDICAL_REASSESS_MIN_MILLIS = 48L * 60 * 60 * 1_000
internal const val REASSESSMENT_POSTPONE_MILLIS = 24L * 60 * 60 * 1_000

internal const val reassessmentKicker = "复评邀请"
internal const val dueInviteHeadline = "14 天到了。准备好的话，做一次简短回顾。"
internal const val earlyInviteHeadline = "复评随时可以做。不是任务。"
internal const val inviteBody = "不需要任何压力。起伏是常态。我们只看长期趋势，不看某一天。"
internal const val startReassessmentLabel = "开始复评"
internal const val postponeReassessmentLabel = "稍后提醒我"
internal const val inviteDurationHint = "大约三分钟。中途可以返回。"
internal const val resultEyebrow = "复评结果"
internal const val resultHeadline = "花一点时间看看你走过的路。"
internal const val resultInsight = "看这一次和基线的差别，不是诊断。"
internal const val continuePracticeLabel = "继续当前练习"
internal const val addMicroActionLabel = "添加新的微小行动"
internal const val wakeCardTitle = "晨间稳定性"
internal const val wakeCardDetail = "过去 14 天的起床时间。数字越小代表越接近，不是得分。"
internal const val biasCardTitle = "预期 vs 现实"
internal const val actionCardTitle = "微行动"
internal const val actionCardDetail = "只计已记下体感的次数。"
internal const val phqCompareTitle = "PHQ-9 评估对比"
internal const val gadCompareTitle = "GAD-7 评估对比"
internal const val baselineLabel = "初始基线"
internal const val currentLabel = "当前评估"
internal const val predictionTrendTitle = "困难预测"
internal const val predictionTrendDetail = "只比较同时有预测与实际体感的微行动。"

internal fun lastAssessmentAt(assessments: List<StoredAssessment>): Long? =
    assessments.maxOfOrNull { it.completedAtMillis }

internal fun reassessmentDue(
    assessments: List<StoredAssessment>,
    nowMillis: Long,
    intervalMillis: Long = REASSESSMENT_INTERVAL_MILLIS,
): Boolean {
    val last = lastAssessmentAt(assessments) ?: return false
    return nowMillis - last >= intervalMillis
}

internal fun medicalReassessAllowed(
    lastAssessmentAtMillis: Long?,
    nowMillis: Long,
    minIntervalMillis: Long = MEDICAL_REASSESS_MIN_MILLIS,
): Boolean = lastAssessmentAtMillis == null || nowMillis - lastAssessmentAtMillis >= minIntervalMillis

internal fun baselineAndLatest(assessments: List<StoredAssessment>): Pair<StoredAssessment, StoredAssessment>? {
    if (assessments.size < 2) return null
    val ordered = assessments.sortedBy { it.completedAtMillis }
    return ordered.first() to ordered.last()
}

internal fun inviteHeadline(due: Boolean): String = if (due) dueInviteHeadline else earlyInviteHeadline

internal data class ReassessInviteCopy(
    val kicker: String,
    val headline: String,
    val body: String,
)

internal fun reassessInviteCopy(
    waiting: Boolean,
    due: Boolean,
    firstLowAtMillis: Long?,
    nowMillis: Long,
): ReassessInviteCopy {
    if (!waiting) {
        return ReassessInviteCopy(reassessmentKicker, inviteHeadline(due), inviteBody)
    }
    val notice = waitingReassessNotice(firstLowAtMillis, nowMillis)
    return ReassessInviteCopy(kicker = "就医等待期", headline = notice.headline, body = notice.body)
}

internal data class WaitingReassessNotice(
    val headline: String,
    val body: String,
)

internal fun waitingReassessNotice(firstLowAtMillis: Long?, nowMillis: Long): WaitingReassessNotice {
    if (firstLowAtMillis == null || nowMillis < firstLowAtMillis) {
        return WaitingReassessNotice(
            headline = "这次记下分数。等待期不一定因此结束。",
            body = "两张表都在 9 分或以下，才算第一次回到轻度。再过至少 7 天，下一次仍是这样，练习才会重新打开。高于 9 分不计入。这次也可以纠正误测。",
        )
    }
    val elapsed = nowMillis - firstLowAtMillis
    if (elapsed >= SEVEN_DAYS_MILLIS) {
        return WaitingReassessNotice(
            headline = "上一次低分已经隔满 7 天。",
            body = "这次若两张表仍在 9 分或以下，练习会重新打开。若有一张更高，等待期还在，那两次要重新算。",
        )
    }
    val days = daysUntil(firstLowAtMillis + SEVEN_DAYS_MILLIS, nowMillis)
    return WaitingReassessNotice(
        headline = "上一次已经回到轻度。这次还不能结束等待期。",
        body = "距上一次低分还不到 7 天。大约还要 $days 天，下一次两张表仍在 9 分或以下，练习才会重新打开。这次可以先记下。",
    )
}

private const val SEVEN_DAYS_MILLIS = 7L * 24 * 60 * 60 * 1_000
private const val DAY_MILLIS = 24L * 60 * 60 * 1_000

internal const val waitingResultHomeLabel = "回到首页"

internal data class StillWaitingResult(
    val phqLine: String,
    val gadLine: String,
    val body: String,
    val button: String,
)

internal fun stillWaitingResult(
    previous: SafetyState,
    outcome: SafetyOutcome,
    nowMillis: Long,
): StillWaitingResult? {
    val kind = SafetyPolicy.stillWaitingKind(previous, outcome) ?: return null
    val assessment = outcome.assessment
    return StillWaitingResult(
        phqLine = scaleScoreLine("PHQ-9", assessment.phq9Score, assessment.phq9Band),
        gadLine = scaleScoreLine("GAD-7", assessment.gad7Score, assessment.gad7Band),
        body = stillWaitingBody(kind, previous, nowMillis),
        button = waitingResultHomeLabel,
    )
}

private fun scaleScoreLine(name: String, score: Int, band: AssessmentBand): String =
    "$name $score · ${phqBandLabel(band)}"

private fun stillWaitingBody(kind: StillWaitingKind, previous: SafetyState, nowMillis: Long): String = when (kind) {
    StillWaitingKind.FirstLow ->
        "这是第一次两张表都回到 9 分或以下。再过至少 7 天，若下一次仍是这样，练习会重新打开。现在还在等待期。"
    StillWaitingKind.LowTooSoon -> {
        val firstLow = checkNotNull(previous.firstLowAssessmentAtMillis)
        val days = daysUntil(firstLow + SEVEN_DAYS_MILLIS, nowMillis)
        if (days <= 0) {
            "这次两张表仍在 9 分或以下。距上一次低分还不到 7 天，所以等待期还在。"
        } else {
            "这次两张表仍在 9 分或以下。距上一次低分还不到 7 天。大约还要 $days 天，下一次仍是这样，才可能是那第二次。"
        }
    }
    StillWaitingKind.NotLowEnough ->
        "这次还没回到 9 分或以下，等待期还在，这次也不计入那两次。"
    StillWaitingKind.StillHigh ->
        "等待期还在。离开要连续两次都是两张表都在 9 分或以下，而且两次至少隔 7 天。这次没有计入。"
}

internal fun daysUntil(targetMillis: Long, nowMillis: Long): Int {
    val remaining = (targetMillis - nowMillis).coerceAtLeast(0)
    if (remaining == 0L) return 0
    return ((remaining + DAY_MILLIS - 1) / DAY_MILLIS).toInt()
}

internal fun scoreDeltaCopy(baseline: Int, current: Int, scale: String): String = when {
    current < baseline -> "$scale 比基线低 ${baseline - current} 分。这不是诊断。"
    current > baseline -> "$scale 比基线高 ${current - baseline} 分。这不是诊断。"
    else -> "$scale 和基线相同。这不是诊断。"
}

internal fun overestimateCount(actions: List<MicroAction>): Int =
    actions.count { predicted ->
        val predictedScore = predicted.predictedDifficulty
        val actualScore = predicted.actualDifficulty
        predictedScore != null && actualScore != null && predictedScore > actualScore
    }

internal fun recordedActionCount(actions: List<MicroAction>): Int =
    actions.count { it.actualDifficulty != null && it.completedAtMillis != null }

internal fun overestimateCopy(count: Int): String =
    if (count == 0) "还没有可比较的预测" else "预测高于实际 $count 次"

internal fun recordedActionCopy(count: Int): String =
    if (count == 0) "还没有记下的体感" else "已记下 $count 次体感"

internal fun wakeMinutesInWindow(
    entries: List<RhythmEntry>,
    localMinuteOfDay: (Long) -> Int,
    nowMillis: Long,
    windowMillis: Long = REASSESSMENT_INTERVAL_MILLIS,
): List<Int> =
    entries
        .mapNotNull { entry -> entry.wakeAtMillis?.takeIf { it >= nowMillis - windowMillis } }
        .sorted()
        .map(localMinuteOfDay)

internal fun isReassessmentBanner(text: String): Boolean =
    text.contains("再测一次") || text.contains("再做一次评估")
