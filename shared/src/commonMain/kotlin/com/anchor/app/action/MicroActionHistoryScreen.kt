package com.anchor.app.action

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anchor.app.insights.averagePredictionBias
import com.anchor.app.insights.biasCopy
import com.anchor.app.insights.predictionPairs
import com.anchor.app.onboarding.overestimateCount
import com.anchor.app.storage.AnchorStore

private val CardShape = RoundedCornerShape(16.dp)

@Composable
fun MicroActionHistoryScreen(
    store: AnchorStore,
    formatLocalStamp: (Long) -> String,
    onClose: () -> Unit,
) {
    val evidence = evidenceActions(store.microActions())
    val pairsOldestFirst = predictionPairs(evidence.sortedBy { it.completedAtMillis ?: 0L })
    val bias = averagePredictionBias(evidence)
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TextButton(onClick = onClose, modifier = Modifier.align(Alignment.End)) { Text("返回") }
        Text(
            historyEvidenceTitle,
            Modifier.fillMaxWidth().semantics { heading() },
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            historyEvidenceIntro,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 15.sp,
            lineHeight = 24.sp,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryCard(
                modifier = Modifier.weight(1f),
                label = historyBiasLabel,
                value = biasCopy(bias),
            )
            SummaryCard(
                modifier = Modifier.weight(1f),
                label = historyOverestimateLabel,
                value = historyOverestimateValue(overestimateCount(evidence)),
            )
        }
        if (pairsOldestFirst.isNotEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = CardShape,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(historyChartTitle, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    PredictionLineChart(pairsOldestFirst)
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                    ) {
                        LegendDot(MaterialTheme.colorScheme.outline, historyPredictedLabel)
                        LegendDot(MaterialTheme.colorScheme.primary, historyActualLabel)
                    }
                }
            }
            Text(historyListTitle, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
            evidence.forEach { action ->
                EvidenceRow(
                    title = action.title,
                    stamp = action.completedAtMillis?.let(formatLocalStamp).orEmpty(),
                    predicted = action.predictedDifficulty,
                    actual = action.actualDifficulty,
                )
            }
        } else {
            Text(historyEmpty, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp, lineHeight = 24.sp)
        }
        Text(
            "“$historyEvidenceQuote”",
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 16.sp,
            lineHeight = 26.sp,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SummaryCard(modifier: Modifier, label: String, value: String) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = CardShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            Text(value, color = MaterialTheme.colorScheme.primary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, lineHeight = 26.sp)
        }
    }
}

@Composable
private fun EvidenceRow(title: String, stamp: String, predicted: Int?, actual: Int?) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = CardShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                if (stamp.isNotEmpty()) {
                    Text(stamp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(historyPredictedLabel, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                Text(historyScore(predicted), fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.outline, fontSize = 16.sp)
            }
            Text("→", color = MaterialTheme.colorScheme.outlineVariant)
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(historyActualLabel, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                Text(
                    historyScore(actual),
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Surface(color = color, shape = CircleShape, modifier = Modifier.size(8.dp)) {}
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
    }
}

@Composable
private fun PredictionLineChart(pairs: List<Pair<Int, Int>>) {
    val predictedColor = MaterialTheme.colorScheme.outline
    val actualColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
    Canvas(Modifier.fillMaxWidth().height(220.dp)) {
        val left = 8.dp.toPx()
        val right = size.width - 8.dp.toPx()
        val top = 12.dp.toPx()
        val bottom = size.height - 12.dp.toPx()
        val width = (right - left).coerceAtLeast(1f)
        val height = (bottom - top).coerceAtLeast(1f)
        fun xAt(index: Int): Float =
            if (pairs.size <= 1) left + width / 2f else left + width * index / (pairs.size - 1)
        fun yAt(score: Int): Float = bottom - height * (score.coerceIn(0, 10) / 10f)
        listOf(0, 5, 10).forEach { score ->
            val y = yAt(score)
            drawLine(gridColor, Offset(left, y), Offset(right, y), strokeWidth = 1.dp.toPx())
        }
        val predictedPath = Path()
        val actualPath = Path()
        pairs.forEachIndexed { index, (predicted, actual) ->
            val x = xAt(index)
            if (index == 0) {
                predictedPath.moveTo(x, yAt(predicted))
                actualPath.moveTo(x, yAt(actual))
            } else {
                predictedPath.lineTo(x, yAt(predicted))
                actualPath.lineTo(x, yAt(actual))
            }
        }
        val predictedStroke = Stroke(
            width = 2.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)),
        )
        val actualStroke = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        if (pairs.size >= 2) {
            drawPath(predictedPath, predictedColor, style = predictedStroke)
            drawPath(actualPath, actualColor, style = actualStroke)
        }
        pairs.forEachIndexed { index, (predicted, actual) ->
            val x = xAt(index)
            drawCircle(predictedColor, radius = 4.dp.toPx(), center = Offset(x, yAt(predicted)))
            drawCircle(actualColor, radius = 4.dp.toPx(), center = Offset(x, yAt(actual)))
        }
    }
}
