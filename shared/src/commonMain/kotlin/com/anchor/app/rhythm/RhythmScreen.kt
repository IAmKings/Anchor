package com.anchor.app.rhythm

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anchor.app.insights.stabilityCopy
import com.anchor.app.insights.wakeStabilityMinutes
import com.anchor.app.storage.AnchorStore
import com.anchor.app.storage.RhythmEntry
private val CardShape = RoundedCornerShape(16.dp)
private val RhythmActionMinHeight = 52.dp

@Composable
fun RhythmScreen(
    store: AnchorStore,
    nowMillis: () -> Long,
    formatLocalTime: (Long) -> String,
    localMinuteOfDay: (Long) -> Int = { 0 },
    onClose: () -> Unit,
) {
    var wakeAt by remember { mutableStateOf<Long?>(null) }
    var lightAt by remember { mutableStateOf<Long?>(null) }
    var saved by remember { mutableStateOf(false) }
    var revision by remember { mutableIntStateOf(0) }
    val entries = remember(revision) { store.rhythmEntries() }
    val wakeMinutes = remember(entries) { wakeMinutesOldestFirst(entries, localMinuteOfDay) }
    val stability = remember(entries) { wakeStabilityMinutes(entries, localMinuteOfDay) }

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TextButton(onClick = onClose, modifier = Modifier.align(Alignment.End)) { Text(rhythmBackLabel) }
        Text(rhythmTitle, Modifier.semantics { heading() }, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        Text(rhythmIntro, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp)

        TimeCard(
            title = rhythmWakeTitle,
            glyph = rhythmWakeGlyph,
            time = wakeAt,
            caption = rhythmWakeCaption(wakeAt != null),
            recordDescription = rhythmWakeRecordDescription,
            formatLocalTime = formatLocalTime,
            onRecord = { wakeAt = nowMillis(); saved = false },
        )
        TimeCard(
            title = rhythmLightTitle,
            glyph = rhythmLightGlyph,
            time = lightAt,
            caption = rhythmLightCaption(lightAt != null),
            recordDescription = rhythmLightRecordDescription,
            formatLocalTime = formatLocalTime,
            onRecord = { lightAt = nowMillis(); saved = false },
        )
        if (lightIsLaterThanOneHour(wakeAt, lightAt)) {
            Text(lateLightCopy(), color = MaterialTheme.colorScheme.secondary, fontSize = 14.sp, lineHeight = 22.sp)
        }
        Button(
            onClick = {
                store.addRhythmEntry(wakeAt, lightAt, nowMillis())
                wakeAt = null
                lightAt = null
                saved = true
                revision++
            },
            enabled = wakeAt != null || lightAt != null,
            modifier = Modifier.fillMaxWidth().heightIn(min = RhythmActionMinHeight),
            shape = RoundedCornerShape(12.dp),
        ) { Text(rhythmSaveLabel, fontSize = 17.sp) }
        if (saved) Text(rhythmSavedCopy(), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)

        StabilityCard(stabilityMinutes = stability, points = wakeMinutes)

        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = CardShape) {
            Text(
                rhythmBedHint,
                Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 15.sp,
                lineHeight = 24.sp,
            )
        }

        Text(rhythmRecentTitle, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        if (entries.isEmpty()) {
            Text(rhythmEmpty, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        entries.take(7).forEach { entry -> HistoryRow(entry, formatLocalTime) }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun TimeCard(
    title: String,
    glyph: String,
    time: Long?,
    caption: String,
    recordDescription: String,
    formatLocalTime: (Long) -> String,
    onRecord: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = CardShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(glyph, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        time?.let(formatLocalTime) ?: rhythmTimePlaceholder,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 28.sp,
                    )
                    Text(caption, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                }
                Surface(
                    onClick = onRecord,
                    shape = CircleShape,
                    color = if (time != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(RhythmActionMinHeight).semantics { contentDescription = recordDescription },
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(if (time != null) "✓" else "+", color = MaterialTheme.colorScheme.primary, fontSize = 18.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun StabilityCard(stabilityMinutes: Double?, points: List<Int>) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = CardShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(rhythmStabilityTitle, fontWeight = FontWeight.SemiBold)
            Text(rhythmStabilityDetail, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            Text(
                stabilityCopy(stabilityMinutes),
                color = MaterialTheme.colorScheme.primary,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
            )
            if (points.size >= 2) {
                val line = MaterialTheme.colorScheme.primary
                Canvas(Modifier.fillMaxWidth().height(96.dp)) {
                    val min = points.min()
                    val max = points.max().coerceAtLeast(min + 1)
                    val path = Path()
                    points.forEachIndexed { index, minute ->
                        val x = size.width * index / (points.size - 1).coerceAtLeast(1)
                        val y = size.height * (1f - (minute - min).toFloat() / (max - min))
                        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(path, color = line, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                    val last = points.last()
                    val lastX = size.width
                    val lastY = size.height * (1f - (last - min).toFloat() / (max - min))
                    drawCircle(color = line, radius = 5.dp.toPx(), center = Offset(lastX, lastY))
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(entry: RhythmEntry, formatLocalTime: (Long) -> String) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = RhythmActionMinHeight).padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            rhythmWakeLine(entry.wakeAtMillis?.let(formatLocalTime) ?: rhythmUnrecorded),
            fontFamily = FontFamily.Monospace,
            fontSize = 16.sp,
        )
        Text(
            rhythmLightLine(entry.lightAtMillis?.let(formatLocalTime) ?: rhythmUnrecorded),
            fontFamily = FontFamily.Monospace,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
