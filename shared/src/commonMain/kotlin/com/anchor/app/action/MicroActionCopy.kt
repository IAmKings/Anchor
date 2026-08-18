package com.anchor.app.action

import com.anchor.app.storage.MicroAction

data class MicroActionPreset(
    val group: String,
    val title: String,
)

val microActionPresets = listOf(
    MicroActionPreset("生活滋养", "穿好鞋走到楼下"),
    MicroActionPreset("生活滋养", "给植物浇水"),
    MicroActionPreset("生活滋养", "整理书桌 5 分钟"),
    MicroActionPreset("工作学习", "打开文档写 1 句话"),
    MicroActionPreset("工作学习", "打开 PDF 看第一段"),
    MicroActionPreset("工作学习", "清理 5 封邮件"),
    MicroActionPreset("身体活动", "下楼走 10 分钟"),
    MicroActionPreset("身体活动", "做 5 个深蹲"),
    MicroActionPreset("身体活动", "站起来伸个懒腰"),
)

internal const val MICRO_ACTION_MINUTES = 5L
internal const val MICRO_ACTION_MILLIS = MICRO_ACTION_MINUTES * 60 * 1_000

internal fun microActionBiasCopy(predicted: Int?, actual: Int?): String {
    if (predicted == null || actual == null) return "这次先如实记下，不需要得出结论。"
    val difference = predicted - actual
    return if (difference > 0) "实际比大脑预测轻 $difference 分。" else "这次先如实记下，不需要得出结论。"
}

internal fun microActionCustomHint(): String =
    "这个动作需要你做心理建设吗？需要就再降。"

internal const val historyEvidenceTitle = "历史证据"
internal const val historyEvidenceIntro = "关于大脑预测偏差的客观记录。只比较同时记下预测和实际体感的次数。"
internal const val historyEvidenceQuote = "我到现在还留着那张表，翻出来看一眼就能出门了。"
internal const val historyOpenLabel = "查看历史记录"
internal const val historyBiasLabel = "平均预测偏差"
internal const val historyOverestimateLabel = "预测高于实际"
internal const val historyChartTitle = "预测 vs 实际"
internal const val historyListTitle = "近期记录"
internal const val historyEmpty = "还没有可比较的记录。做完一次并记下体感后会出现在这里。"
internal const val historyPredictedLabel = "预测"
internal const val historyActualLabel = "实际"

internal fun evidenceActions(actions: List<MicroAction>): List<MicroAction> =
    actions.filter { it.predictedDifficulty != null && it.actualDifficulty != null && it.completedAtMillis != null }
        .sortedByDescending { it.completedAtMillis ?: 0L }

internal fun historyScore(score: Int?): String = "${score ?: "—"}/10"

internal fun historyOverestimateValue(count: Int): String =
    if (count <= 0) "还没有" else "$count 次"
