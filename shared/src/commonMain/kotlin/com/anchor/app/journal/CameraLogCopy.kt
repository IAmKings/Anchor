package com.anchor.app.journal

internal fun recordsHubIntro(medicalWaiting: Boolean): String =
    if (medicalWaiting) "就医等待期仍可写双栏。只写一栏也可以。"
    else "只写一栏也可以。没有任务，也不需要补记。"

internal fun recordsEmotionBody(): String = "给情绪起一个准确的名字。不是为了分析它。"

internal fun recordsJournalBody(): String = "把摄像头拍到的事实和大脑自动补全的推断分开。"

internal fun recordsCountLabel(kind: String, count: Int): String? =
    if (count <= 0) null else "已记下 $count $kind"

internal const val recordsTitle = "记录"
internal const val recordsEmotionTitle = "情绪标签箱"
internal const val recordsJournalTitle = "双栏日志"
internal const val recordsWorryTitle = "忧虑保险箱"
internal const val recordsEmotionGlyph = "情"
internal const val recordsJournalGlyph = "栏"
internal const val recordsWorryGlyph = "箱"

internal fun recordsWorryBody(): String = "念头来了就挂一张。专场再统一开箱。"

internal fun recordsWorryCountLabel(pending: Int): String? =
    if (pending <= 0) null else "$pending 张待处理"

internal const val journalBackLabel = "返回"
internal const val journalBadge = "隔离事实与大脑推断"
internal const val journalWriteLabel = "写下这一栏"
internal const val journalEmpty = "还没有记录。只写一栏也可以。"
internal const val journalCancelLabel = "取消"
internal const val journalFactTitle = "摄像头拍到的事实"
internal const val journalFactCaption = "仅记录动作、时间、地点、话语。"
internal const val journalFactPlaceholder = "例如：早上 9 点，他发来一条信息说「我现在很忙」。"
internal const val journalInferenceTitle = "我的大脑推断"
internal const val journalInferenceCaption = "记录你的猜测、评价和联想。"
internal const val journalInferencePlaceholder = "例如：我觉得他是在针对我。"
internal const val journalOneColumnHint = "低精力时只写一栏也可以。"
internal const val journalSaveLabel = "保存记录"
internal const val journalReflection = "右栏是你的大脑推断，并非已发生的事实。"
internal const val journalReflectionDismiss = "知道了"
internal const val journalHintTitle = "这条可能属于右栏"
internal const val journalMoveLabel = "挪过去"
internal const val journalSavedHint = "这条可能包含推断，需要的话可以放到右栏。"

internal fun journalHintDetail(word: String): String = "检测到「$word」。仍然可以直接保存。"

internal fun moveEvaluativeSentence(fact: String, inference: String, word: String): Pair<String, String> {
    val parts = fact.split(Regex("(?<=[。！？；.!?])"))
    val moving = StringBuilder()
    val staying = StringBuilder()
    parts.forEach { part ->
        if (word in part) moving.append(part) else staying.append(part)
    }
    var stay = staying.toString().trim()
    var move = moving.toString().trim()
    if (move.isEmpty()) {
        move = fact.trim()
        stay = ""
    }
    val newInference = listOf(inference.trim(), move).filter { it.isNotBlank() }.joinToString("\n")
    return stay to newInference
}
