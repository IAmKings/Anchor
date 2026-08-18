package com.anchor.app.safety

internal const val crisisClarificationTitle = "这句话说的是你自己吗？"
internal const val crisisClarificationBody = "刚才写下的内容里出现了需要先确认的说法。判断诊断不是你的工作。"
internal const val crisisClarificationNotSelf = "不是我"
internal const val crisisClarificationSelf = "是我"
internal const val crisisClarificationUncertain = "不确定"
internal const val crisisClarificationPreviewNote = "预览中，不会改安全状态。"

internal val crisisPhrases = listOf(
    "不想活",
    "不想活了",
    "想死",
    "去死",
    "自杀",
    "自我了断",
    "了结自己",
    "结束自己",
    "伤害自己",
    "自残",
    "割腕",
    "活不下去",
    "不如死",
    "不想活下去",
    "离开这个世界",
    "kill myself",
    "end my life",
    "want to die",
    "suicide",
    "self-harm",
    "cut myself",
)

fun findCrisisPhrase(text: String): String? {
    val raw = text.lowercase()
    if (raw.isBlank()) return null
    val compact = raw.replace(Regex("[\\s\\p{Punct}]+"), "")
    return crisisPhrases.firstOrNull { phrase ->
        val needle = phrase.lowercase()
        raw.contains(needle) || compact.contains(needle.replace(" ", ""))
    }
}

fun findCrisisPhrase(texts: Iterable<String>): String? =
    texts.firstNotNullOfOrNull(::findCrisisPhrase)

fun onFreeTextCrisis(clarification: Clarification?): SafetyAction = when (clarification) {
    null -> SafetyAction.AskClarification
    Clarification.NotSelf -> SafetyAction.Continue
    Clarification.Self, Clarification.Uncertain -> SafetyAction.CrisisGuidance
}
