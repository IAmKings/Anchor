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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
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
import com.anchor.app.relation.altruismFeelCopy
import com.anchor.app.relation.energyCopy
import com.anchor.app.relation.monitorCopy
import com.anchor.app.rhythm.wakeMinutesOldestFirst
import com.anchor.app.storage.AnchorStore
import com.anchor.app.storage.MicroAction
import com.anchor.app.storage.RhythmEntry
import kotlin.math.sqrt

private val CardShape = RoundedCornerShape(16.dp)

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
        TextButton(onClick = onClose, modifier = Modifier.align(Alignment.End)) { Text("返回") }
        Text("洞察", Modifier.semantics { heading() }, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        Text(
            "长期趋势，非单日波动。这些计算只留在这台设备，不会上报。",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 15.sp,
            lineHeight = 24.sp,
        )
        InsightCard(
            title = "30 日起床稳定度",
            value = stabilityCopy(stability),
            detail = "使用最近 ${rhythmEntries.count { it.wakeAtMillis != null }.coerceAtMost(30)} 次起床记录的标准差。数字越小代表时间越接近，不是得分。",
        ) {
            if (wakeMinutes.size >= 2) Sparkline(wakeMinutes.map { it.toFloat() }, MaterialTheme.colorScheme.primary)
        }
        InsightCard(
            title = "三个月移动平均",
            value = if (averages.size >= 2) "看长期，不看某一天" else "记录还不够",
            detail = "用最近起床时间做滚动平均。别看某一天的值。",
        ) {
            if (averages.size >= 2) Sparkline(averages.map { it.toFloat() }, MaterialTheme.colorScheme.primary)
        }
        InsightCard(
            title = "大脑的困难预测",
            value = biasCopy(predictionBias),
            detail = "只比较同时有预测与实际体感的微行动，不评价完成多少。",
        ) {
            if (pairs.isNotEmpty()) BiasChart(pairs.take(6).reversed())
            TextButton(onClick = onOpenHistory) { Text("查看历史记录") }
        }
        InsightCard(
            title = "忧虑受理",
            value = worryAcceptedCopy(store.worryCards()),
            detail = "只计数已被处理的卡片，没有完成率，也没有断档提醒。",
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
            title = "关系监控",
            value = monitorCopy(store.relationContacts()),
            detail = "只计需要监控自己的关系段数，不是得分。",
        )
        InsightCard(
            title = "回血与抽干",
            value = energyCopy(store.relationEnergy()),
            detail = "只计次数，没有净值，也没有完成率。",
        )
        InsightCard(
            title = "利他体感",
            value = altruismFeelCopy(store.altruismDraws()),
            detail = "更紧只作护栏，不评价你做得够不够。",
        )
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
            Text("预测", color = predictedColor, fontSize = 14.sp)
            Text("实际", color = actualColor, fontSize = 14.sp)
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
