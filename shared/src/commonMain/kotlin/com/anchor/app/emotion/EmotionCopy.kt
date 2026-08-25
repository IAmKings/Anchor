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

internal const val emotionBackLabel = "返回"
internal const val emotionListTitle = "给情绪起一个准确的名字"
internal const val emotionListIntro = "不是为了分析它，只是把模糊的难受变成一个可以看见的对象。"
internal const val emotionWriteLabel = "写下这一张"
internal const val emotionEmpty = "这里还没有卡片。暂停很正常。"
internal const val emotionPassedAction = "它已经过去了"
internal const val emotionPassedMark = "已过去"
internal const val emotionCancelLabel = "取消"
internal const val emotionPickTitle = "哪一个词更接近？"
internal const val emotionPickIntro = "“很烦”“很难过”可以是入口，但请再具体一点。"
internal const val emotionSearchLabel = "搜一个更接近的词"
internal const val emotionVagueHint = "这个还太宽。请从下面挑一个更具体的词。"
internal const val emotionUnmatchedHint = "词库里没有这个。请从下面挑一个具体的词。"
internal const val emotionSwapWord = "换一个词"
internal const val emotionEventLabel = "在什么事情中"
internal const val emotionHardestLabel = "最让我难受的具体部分"
internal const val emotionSaveLabel = "保存卡片"
