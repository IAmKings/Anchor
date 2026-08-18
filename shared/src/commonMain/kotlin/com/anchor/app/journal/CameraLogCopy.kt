package com.anchor.app.journal

internal fun recordsHubIntro(medicalWaiting: Boolean): String =
    if (medicalWaiting) "就医等待期仍可写双栏。只写一栏也可以。"
    else "只写一栏也可以。没有任务，也不需要补记。"

internal fun recordsEmotionBody(): String = "给情绪起一个准确的名字。不是为了分析它。"

internal fun recordsJournalBody(): String = "把摄像头拍到的事实和大脑自动补全的推断分开。"

internal fun recordsCountLabel(kind: String, count: Int): String? =
    if (count <= 0) null else "已记下 $count $kind"

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
