package com.anchor.app.insights

import com.anchor.app.storage.MicroAction
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
