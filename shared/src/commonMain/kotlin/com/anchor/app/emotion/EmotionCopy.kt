package com.anchor.app.emotion

import com.anchor.app.storage.emotionVocabulary

data class EmotionGroup(
    val title: String,
    val words: List<String>,
)

data class EmotionSearch(
    val groups: List<EmotionGroup>,
    val vague: Boolean,
    val unmatched: Boolean = false,
)

internal val emotionGroups = listOf(
    EmotionGroup(
        "被怎样对待",
        listOf("被轻视", "被排除在外", "没有被认真对待", "被误解", "被拒绝", "被控制", "被背叛", "孤立无援"),
    ),
    EmotionGroup(
        "对自己",
        listOf("羞耻", "内疚", "不服气", "委屈", "失望", "嫉妒", "愤怒", "怨恨"),
    ),
    EmotionGroup(
        "身体里的紧",
        listOf("心慌", "不安", "担心被发现", "害怕失败", "害怕失去", "失控感", "无助", "绝望"),
    ),
    EmotionGroup(
        "空掉了",
        listOf("空虚", "虚无", "麻木", "孤独", "疲惫", "厌倦", "局促", "困惑"),
    ),
)

internal val vagueEmotionQueries = listOf(
    "烦", "很烦", "难过", "很难过", "不知道怎么了", "不知道", "郁闷", "心情不好", "不舒服", "难受",
)

internal fun isVagueEmotionQuery(query: String): Boolean {
    val q = query.trim()
    if (q.isEmpty()) return false
    return vagueEmotionQueries.any { phrase -> q == phrase || q.contains(phrase) }
}

internal fun searchEmotions(query: String): EmotionSearch {
    val q = query.trim()
    if (q.isEmpty()) return EmotionSearch(emotionGroups, vague = false)
    if (isVagueEmotionQuery(q) && emotionVocabulary.none { it == q }) {
        return EmotionSearch(emotionGroups, vague = true)
    }
    val hits = emotionVocabulary.filter { it.contains(q) }
    if (hits.isEmpty()) return EmotionSearch(emotionGroups, vague = false, unmatched = true)
    return EmotionSearch(listOf(EmotionGroup("接近的词", hits)), vague = false)
}

internal fun emotionCardSentence(emotion: String, event: String, hardestPart: String): String =
    "在「${event.trim()}」中，我感到$emotion；最让我难受的是「${hardestPart.trim()}」。"

internal fun groupedWordsCoverVocabulary(): Boolean =
    emotionGroups.flatMap { it.words }.toSet() == emotionVocabulary.toSet()
