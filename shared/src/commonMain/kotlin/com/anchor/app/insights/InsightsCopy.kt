package com.anchor.app.insights

import com.anchor.app.relation.drainedLabel
import com.anchor.app.relation.filledLabel
import com.anchor.app.relation.lighterLabel
import com.anchor.app.relation.tighterLabel
import com.anchor.app.storage.AltruismDraw
import com.anchor.app.storage.AltruismFeel
import com.anchor.app.storage.EnergyMark
import com.anchor.app.storage.MicroAction
import com.anchor.app.storage.RelationEnergyEntry
import com.anchor.app.storage.RhythmEntry
import com.anchor.app.storage.WorryCard
import com.anchor.app.storage.WorryResolution
import kotlin.math.roundToInt

fun predictionPairs(actions: List<MicroAction>): List<Pair<Int, Int>> =
    actions.mapNotNull { action ->
        val predicted = action.predictedDifficulty
        val actual = action.actualDifficulty
        if (predicted == null || actual == null) null else predicted to actual
    }

fun movingAverage(values: List<Int>, window: Int): List<Double> {
    if (values.isEmpty() || window <= 0) return emptyList()
    return values.mapIndexed { index, _ ->
        val from = (index - window + 1).coerceAtLeast(0)
        values.subList(from, index + 1).average()
    }
}

internal fun stabilityCopy(minutes: Double?): String =
    minutes?.let { "约 ${it.roundToInt()} 分钟" } ?: "记录还不够"

internal fun biasCopy(average: Double?): String = when {
    average == null -> "记录还不够"
    average > 0 -> "平均高估 ${average.roundToInt()} 分"
    average < 0 -> "平均低估 ${(-average).roundToInt()} 分"
    else -> "预测与实际接近"
}

internal fun worryAcceptedCopy(cards: List<WorryCard>): String {
    val accepted = cards.count { it.resolution != WorryResolution.Pending }
    return if (accepted == 0) "还没有受理记录" else "已受理 $accepted 张"
}

internal fun worryUnsolvableCopy(cards: List<WorryCard>): String {
    val processed = cards.filter { it.resolution != WorryResolution.Pending }
    if (processed.isEmpty()) return "还没有处理过的卡片"
    val unsolvable = processed.count { it.resolution == WorryResolution.Unsolvable }
    return "暂时无解 $unsolvable / 已处理 ${processed.size}"
}

internal const val worryUnsolvableTitle = "暂时无解"
internal const val worryUnsolvableDetail = "只看已处理卡片里暂时无解的张数。不是完成率，也不是诊断。"

internal const val insightsBackLabel = "返回"
internal const val insightsTitle = "洞察"
internal const val insightsIntro = "长期趋势，非单日波动。这些计算只留在这台设备，不会上报。"
internal const val insightsWakeTitle = "30 日起床稳定度"
internal const val insightsAverageTitle = "三个月移动平均"
internal const val insightsAverageReady = "看长期，不看某一天"
internal const val insightsAverageDetail = "用最近起床时间做滚动平均。别看某一天的值。"
internal const val insightsBiasTitle = "大脑的困难预测"
internal const val insightsBiasDetail = "只比较同时有预测与实际体感的微行动，不评价完成多少。"
internal const val insightsWorryAcceptedTitle = "忧虑受理"
internal const val insightsWorryAcceptedDetail = "只计数已被处理的卡片，没有完成率，也没有断档提醒。"
internal const val insightsMonitorTitle = "关系监控"
internal const val insightsMonitorDetail = "只计需要监控自己的关系段数，不是得分。"
internal const val insightsEnergyTitle = "回血与抽干"
internal const val insightsEnergyDetail = "只计次数，没有净值，也没有完成率。"
internal const val insightsAltruismTitle = "利他体感"
internal const val insightsAltruismDetail = "更紧只作护栏，不评价你做得够不够。"

internal fun insightsWakeDetail(sampleCount: Int): String =
    "使用最近 $sampleCount 次起床记录的标准差。数字越小代表时间越接近，不是得分。"

internal const val interpretationTitle = "反复出现的解释"
internal const val interpretationNotEnough = "记录还不够"
internal const val interpretationEmpty = "还没有反复出现的解释"
internal const val interpretationDetail = "满 15 条带推断的双栏后才回看。只计次数，不是诊断。"

internal fun interpretationCopy(sampleCount: Int, hits: List<InterpretationHit>): String = when {
    sampleCount < INTERPRETATION_MIN_SAMPLES -> interpretationNotEnough
    hits.isEmpty() -> interpretationEmpty
    else -> "回看 ${hits.size} 种解释"
}

internal fun interpretationHitLine(hit: InterpretationHit): String = "${hit.label} · ${hit.count} 次"

internal const val insightCycleWindow = 12

internal data class InsightCountBar(val label: String, val count: Int, val warning: Boolean = false)

internal fun energyCountBars(entries: List<RelationEnergyEntry>): List<InsightCountBar> {
    if (entries.isEmpty()) return emptyList()
    return listOf(
        InsightCountBar(filledLabel, entries.count { it.mark == EnergyMark.Filled }),
        InsightCountBar(drainedLabel, entries.count { it.mark == EnergyMark.Drained }, warning = true),
    )
}

internal fun energyCycleSeries(entries: List<RelationEnergyEntry>): List<Float> =
    entries.sortedBy { it.createdAtMillis }.takeLast(insightCycleWindow).map { entry ->
        if (entry.mark == EnergyMark.Filled) 1f else 0f
    }

internal fun altruismCountBars(draws: List<AltruismDraw>): List<InsightCountBar> {
    val felt = draws.filter { it.felt != null }
    if (felt.isEmpty()) return emptyList()
    return listOf(
        InsightCountBar(lighterLabel, felt.count { it.felt == AltruismFeel.Lighter }),
        InsightCountBar(tighterLabel, felt.count { it.felt == AltruismFeel.Tighter }, warning = true),
    )
}

internal fun altruismCycleSeries(draws: List<AltruismDraw>): List<Float> =
    draws.filter { it.felt != null }
        .sortedBy { it.completedAtMillis ?: it.drawnAtMillis }
        .takeLast(insightCycleWindow)
        .map { draw -> if (draw.felt == AltruismFeel.Lighter) 1f else 0f }
