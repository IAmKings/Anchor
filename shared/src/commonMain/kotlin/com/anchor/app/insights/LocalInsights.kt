package com.anchor.app.insights

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anchor.app.action.historyActualLabel
import com.anchor.app.action.historyOpenLabel
import com.anchor.app.action.historyPredictedLabel
import com.anchor.app.relation.altruismFeelCopy
import com.anchor.app.relation.energyCopy
import com.anchor.app.relation.monitorCopy
import com.anchor.app.relation.pauseAltruismCopy
import com.anchor.app.relation.shouldPauseAltruism
import com.anchor.app.rhythm.wakeMinutesOldestFirst
import com.anchor.app.storage.AnchorStore
import com.anchor.app.storage.MicroAction
import com.anchor.app.storage.RhythmEntry
import kotlin.math.sqrt

private val CardShape = RoundedCornerShape(16.dp)
private val InsightsActionMinHeight = 52.dp

fun wakeStabilityMinutes(entries: List<RhythmEntry>, localMinuteOfDay: (Long) -> Int): Double? {
    val minutes = entries.asSequence().mapNotNull { it.wakeAtMillis }.take(30).map(localMinuteOfDay).toList()
    if (minutes.size < 2) return null
    val first = minutes.first()
    val offsets = minutes.map { (it - first + 720).mod(1_440) - 720 }
    val mean = offsets.average()
    return sqrt(offsets.sumOf { (it - mean) * (it - mean) } / offsets.size)
}

fun averagePredictionBias(actions: List<MicroAction>): Double? {
    val differences = predictionPairs(actions).map { it.first - it.second }
    return differences.takeIf { it.isNotEmpty() }?.average()
}

@Composable
fun LocalInsightsScreen(
    store: AnchorStore,
    localMinuteOfDay: (Long) -> Int,
    onOpenHistory: () -> Unit = {},
    onClose: () -> Unit,
) {
    val rhythmEntries = store.rhythmEntries()
    val actions = store.microActions()
    val pairs = predictionPairs(actions)
    val wakeMinutes = wakeMinutesOldestFirst(rhythmEntries, localMinuteOfDay)
    val stability = wakeStabilityMinutes(rhythmEntries, localMinuteOfDay)
    val predictionBias = averagePredictionBias(actions)
    val averages = movingAverage(wakeMinutes, window = 7)

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TextButton(onClick = onClose, modifier = Modifier.align(Alignment.End)) { Text(insightsBackLabel) }
        Text(insightsTitle, Modifier.semantics { heading() }, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        Text(
            insightsIntro,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 15.sp,
            lineHeight = 24.sp,
        )
        InsightCard(
            title = insightsWakeTitle,
            value = stabilityCopy(stability),
            detail = insightsWakeDetail(rhythmEntries.count { it.wakeAtMillis != null }.coerceAtMost(30)),
        ) {
            if (wakeMinutes.size >= 2) Sparkline(wakeMinutes.map { it.toFloat() }, MaterialTheme.colorScheme.primary)
        }
        InsightCard(
            title = insightsAverageTitle,
            value = if (averages.size >= 2) insightsAverageReady else interpretationNotEnough,
            detail = insightsAverageDetail,
        ) {
            if (averages.size >= 2) Sparkline(averages.map { it.toFloat() }, MaterialTheme.colorScheme.primary)
        }
        InsightCard(
            title = insightsBiasTitle,
            value = biasCopy(predictionBias),
            detail = insightsBiasDetail,
        ) {
            if (pairs.isNotEmpty()) BiasChart(pairs.take(6).reversed())
            OutlinedButton(
                onClick = onOpenHistory,
                modifier = Modifier.fillMaxWidth().heightIn(min = InsightsActionMinHeight),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(historyOpenLabel, fontSize = 17.sp)
            }
        }
        InsightCard(
            title = insightsWorryAcceptedTitle,
            value = worryAcceptedCopy(store.worryCards()),
            detail = insightsWorryAcceptedDetail,
        )
        InsightCard(
            title = worryUnsolvableTitle,
            value = worryUnsolvableCopy(store.worryCards()),
            detail = worryUnsolvableDetail,
        )
        run {
            val logs = store.cameraLogs()
            val hits = interpretationHits(logs)
            InsightCard(
                title = interpretationTitle,
                value = interpretationCopy(interpretationSampleCount(logs), hits),
                detail = interpretationDetail,
            ) {
                hits.forEach { hit ->
                    Text(interpretationHitLine(hit), fontSize = 15.sp, lineHeight = 22.sp)
                }
            }
        }
        InsightCard(
            title = insightsMonitorTitle,
            value = monitorCopy(store.relationContacts()),
            detail = insightsMonitorDetail,
        )
        run {
            val energy = store.relationEnergy()
            val bars = energyCountBars(energy)
            val series = energyCycleSeries(energy)
            InsightCard(
                title = insightsEnergyTitle,
                value = energyCopy(energy),
                detail = insightsEnergyDetail,
            ) {
                if (series.size >= 2) Sparkline(series, MaterialTheme.colorScheme.primary)
                CountBars(bars)
            }
        }
        run {
            val draws = store.altruismDraws()
            val bars = altruismCountBars(draws)
            val series = altruismCycleSeries(draws)
            InsightCard(
                title = insightsAltruismTitle,
                value = altruismFeelCopy(draws),
                detail = insightsAltruismDetail,
            ) {
                if (shouldPauseAltruism(draws)) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                        shape = CardShape,
                    ) {
                        Text(pauseAltruismCopy(), Modifier.padding(12.dp), fontSize = 14.sp, lineHeight = 22.sp)
                    }
                }
                if (series.size >= 2) Sparkline(series, MaterialTheme.colorScheme.primary)
                CountBars(bars)
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun InsightCard(
    title: String,
    value: String,
    detail: String,
    extra: (@Composable () -> Unit)? = null,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = CardShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(value, fontSize = 22.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            extra?.invoke()
            Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, lineHeight = 22.sp)
        }
    }
}

@Composable
internal fun Sparkline(points: List<Float>, color: Color) {
    if (points.size < 2) return
    Canvas(Modifier.fillMaxWidth().height(96.dp)) {
        val min = points.min()
        val max = points.max().coerceAtLeast(min + 1f)
        val path = Path()
        points.forEachIndexed { index, value ->
            val x = size.width * index / (points.size - 1)
            val y = size.height * (1f - (value - min) / (max - min))
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color = color, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
internal fun BiasChart(pairs: List<Pair<Int, Int>>) {
    val predictedColor = MaterialTheme.colorScheme.outline
    val actualColor = MaterialTheme.colorScheme.primary
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(historyPredictedLabel, color = predictedColor, fontSize = 14.sp)
            Text(historyActualLabel, color = actualColor, fontSize = 14.sp)
        }
        pairs.forEach { (predicted, actual) ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Bar(predicted, predictedColor)
                Bar(actual, actualColor)
            }
        }
    }
}

@Composable
private fun CountBars(items: List<InsightCountBar>) {
    if (items.isEmpty()) return
    val max = items.maxOf { it.count }.coerceAtLeast(1)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEach { item ->
            val color = if (item.warning) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(item.label, fontSize = 14.sp)
                    Text(item.count.toString(), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
                CountBar(fraction = item.count / max.toFloat(), color = color)
            }
        }
    }
}

@Composable
private fun CountBar(fraction: Float, color: Color) {
    Canvas(Modifier.fillMaxWidth().height(10.dp)) {
        val width = size.width * fraction.coerceIn(0f, 1f)
        if (width <= 0f) return@Canvas
        drawLine(
            color = color,
            start = Offset(0f, size.height / 2),
            end = Offset(width, size.height / 2),
            strokeWidth = size.height,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun Bar(score: Int, color: Color) {
    Canvas(Modifier.fillMaxWidth().height(10.dp)) {
        val width = size.width * (score.coerceIn(1, 10) / 10f)
        drawLine(
            color = color,
            start = Offset(0f, size.height / 2),
            end = Offset(width, size.height / 2),
            strokeWidth = size.height,
            cap = StrokeCap.Round,
        )
    }
}
