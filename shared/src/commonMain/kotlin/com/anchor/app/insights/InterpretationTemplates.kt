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
    InterpretationTemplate(
        label = "出错 = 完蛋",
        factCues = listOf("出错", "失误", "搞砸", "迟到", "忘了", "搞错", "考砸", "被拒"),
        inferenceCues = listOf("完蛋", "全完了", "灾难", "毁了", "没救", "不可收拾"),
    ),
    InterpretationTemplate(
        label = "被夸 = 不算数",
        factCues = listOf("夸", "表扬", "好评", "称赞"),
        inferenceCues = listOf("客气", "碰巧", "侥幸", "不算", "随口说说"),
    ),
    // 过度概括、个人化、非黑即白、应该句式、读心的语言特征几乎都在推断栏，
    // 事实栏不做限制（factCues 为空 = 仅看推断栏）。
    InterpretationTemplate(
        label = "一次 = 总是",
        factCues = emptyList(),
        inferenceCues = listOf("总是", "从来", "每次", "永远", "一向", "谁都", "人人都"),
    ),
    InterpretationTemplate(
        label = "别人的反应 = 我的错",
        factCues = emptyList(),
        inferenceCues = listOf("我的错", "怪我", "都怪我", "因为我才", "我害"),
    ),
    InterpretationTemplate(
        label = "不完美 = 一无是处",
        factCues = emptyList(),
        inferenceCues = listOf("一无是处", "全废", "全错", "不完美就是"),
    ),
    InterpretationTemplate(
        label = "本可以 = 自责",
        factCues = emptyList(),
        inferenceCues = listOf("本可以", "本应该", "早知道", "我应该早"),
    ),
    InterpretationTemplate(
        label = "别人 = 在笑话我",
        factCues = emptyList(),
        inferenceCues = listOf("笑话我", "议论我", "看不起我", "嘲笑", "背后说"),
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
