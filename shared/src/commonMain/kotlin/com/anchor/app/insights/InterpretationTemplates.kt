package com.anchor.app.insights

import com.anchor.app.storage.CameraLog

internal const val INTERPRETATION_MIN_SAMPLES = 15
internal const val INTERPRETATION_MIN_HITS = 2

internal data class InterpretationTemplate(
    val label: String,
    val factCues: List<String>,
    val inferenceCues: List<String>,
)

internal data class InterpretationHit(
    val label: String,
    val count: Int,
)

internal val interpretationTemplates = listOf(
    InterpretationTemplate(
        label = "回复慢 = 对我不满",
        factCues = listOf("回", "消息", "信息", "没回", "小时", "分钟"),
        inferenceCues = listOf("不满", "不高兴", "不想理", "针对", "讨厌", "生气"),
    ),
    InterpretationTemplate(
        label = "没有表扬 = 否定",
        factCues = listOf("没", "没有", "没说", "没夸", "没人"),
        inferenceCues = listOf("否定", "不行", "不认可", "不看好", "失败"),
    ),
    InterpretationTemplate(
        label = "批评 = 否定整个人",
        factCues = listOf("批评", "被否", "说我", "骂", "指摘"),
        inferenceCues = listOf("整个人", "不专业", "没用", "不行", "否定"),
    ),
)

internal fun CameraLog.hasInference(): Boolean = inference.isNotBlank()

internal fun InterpretationTemplate.matches(log: CameraLog): Boolean {
    if (!log.hasInference()) return false
    val factHit = factCues.isEmpty() || factCues.any { it in log.fact }
    val inferenceHit = inferenceCues.any { it in log.inference }
    return factHit && inferenceHit
}

internal fun interpretationHits(logs: List<CameraLog>): List<InterpretationHit> {
    val usable = logs.filter { it.hasInference() }
    if (usable.size < INTERPRETATION_MIN_SAMPLES) return emptyList()
    return interpretationTemplates.mapNotNull { template ->
        val count = usable.count { template.matches(it) }
        if (count >= INTERPRETATION_MIN_HITS) InterpretationHit(template.label, count) else null
    }.sortedByDescending { it.count }
}

internal fun interpretationSampleCount(logs: List<CameraLog>): Int = logs.count { it.hasInference() }
