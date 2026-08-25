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

internal const val microActionTitle = "微行动"
internal const val microActionIntro = "门槛低到不需要说服自己。"
internal const val microActionReadyGroup = "已挂上的动作"
internal const val microActionCustomGroup = "自定义微小行动"
internal const val microActionCustomFieldLabel = "写一个再小一点的动作"
internal const val microActionCustomConfirm = "确认自定义行动"
internal const val microActionNeedPick = "请先选一个动作。"
internal const val microActionTimerEnded = "计时已结束。"
internal const val microActionSwapLabel = "换一个"
internal const val microActionPredictPrompt = "开始前，你预测它有多难？1 很轻，10 很难。"
internal const val microActionStartLabel = "开始 5 分钟"
internal const val microActionWaitBody = "不评估想不想。启动 5 分钟。"
internal const val microActionFinishTimerLabel = "我已完成"
internal const val microActionPauseLabel = "暂停 / 退出"
internal const val microActionRatePrompt = "实际做起来有多难？"
internal const val microActionSaveFeltLabel = "记下这次体感"
internal const val microActionResultTitle = "动作完成"
internal const val microActionHomeLabel = "回到今天"
internal const val microActionAnotherLabel = "再选一个"

internal fun microActionPredictedBadge(score: Int): String = "预测困难度 $score / 10"

internal fun microActionDidCopy(title: String): String = "你做到了「$title」"

internal fun microActionPredictLine(score: Int?): String = "行动前 · 预测难度  ${score ?: "—"}"

internal fun microActionActualLine(score: Int?): String = "行动后 · 实际体感  ${score ?: "—"}"

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
