package com.anchor.app.onboarding

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
