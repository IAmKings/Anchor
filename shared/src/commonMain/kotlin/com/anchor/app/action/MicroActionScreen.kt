package com.anchor.app.action

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anchor.app.storage.AnchorStore
import com.anchor.app.storage.MicroAction
import com.anchor.app.ui.formatCountdown

private val CardShape = RoundedCornerShape(16.dp)
private val ActionMinHeight = 52.dp

private enum class ActionStep { Pick, Predict, Run, Rate, Result }

@Composable
fun MicroActionScreen(
    store: AnchorStore,
    nowMillis: () -> Long,
    onStartTimer: (Long) -> Unit,
    onCancelTimer: () -> Unit,
    onOpenHistory: () -> Unit = {},
    onClose: () -> Unit,
) {
    var revision by remember { mutableIntStateOf(0) }
    var predicted by remember { mutableIntStateOf(5) }
    var actual by remember { mutableIntStateOf(5) }
    var custom by remember { mutableStateOf("") }
    var selectedId by remember { mutableStateOf<Long?>(null) }
    var justCompletedId by remember { mutableStateOf<Long?>(null) }
    var tick by remember { mutableStateOf(0L) }
    val actions = remember(revision) { store.microActions() }
    val active = actions.firstOrNull { it.startedAtMillis != null && it.completedAtMillis == null }
    val ready = actions.filter { it.startedAtMillis == null }
    val completed = actions.filter { it.completedAtMillis != null }
    val selected = actions.firstOrNull { it.id == selectedId } ?: ready.firstOrNull()
    val finished = actions.firstOrNull { it.id == justCompletedId }
    var step by remember {
        mutableStateOf(
            when {
                active != null -> ActionStep.Run
                else -> ActionStep.Pick
            },
        )
    }

    LaunchedEffect(active?.id) {
        while (active != null) {
            withFrameMillis { frame -> if (frame - tick >= 1_000) tick = frame }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TextButton(onClick = onClose, modifier = Modifier.align(Alignment.End)) { Text("返回") }
        when (step) {
            ActionStep.Pick -> PickStep(
                ready = ready,
                hasHistory = completed.isNotEmpty(),
                custom = custom,
                onCustom = { custom = it },
                onPickExisting = { selectedId = it; step = ActionStep.Predict },
                onPickPreset = { title ->
                    store.addMicroAction(title, nowMillis())
                    revision++
                    selectedId = store.microActions().firstOrNull { it.title == title && it.startedAtMillis == null }?.id
                    custom = ""
                    step = ActionStep.Predict
                },
                onOpenHistory = onOpenHistory,
            )
            ActionStep.Predict -> selected?.let { action ->
                PredictStep(
                    title = action.title,
                    predicted = predicted,
                    onPredicted = { predicted = it },
                    onStart = {
                        store.startMicroAction(action.id, predicted, nowMillis())
                        onStartTimer(MICRO_ACTION_MILLIS)
                        revision++
                        step = ActionStep.Run
                    },
                    onBack = { step = ActionStep.Pick },
                )
            } ?: Text(microActionNeedPick)
            ActionStep.Run -> active?.let { action ->
                val remaining = (action.startedAtMillis!! + MICRO_ACTION_MILLIS - nowMillis()).coerceAtLeast(0)
                RunStep(
                    title = action.title,
                    predicted = action.predictedDifficulty,
                    remainingMillis = remaining,
                    onFinish = { step = ActionStep.Rate },
                    onLeave = onClose,
                )
            } ?: Text(microActionTimerEnded)
            ActionStep.Rate -> (active ?: finished)?.let { action ->
                RateStep(
                    title = action.title,
                    actual = actual,
                    onActual = { actual = it },
                    onSave = {
                        if (action.completedAtMillis == null) {
                            store.completeMicroAction(action.id, actual, nowMillis())
                            onCancelTimer()
                        }
                        justCompletedId = action.id
                        revision++
                        step = ActionStep.Result
                    },
                )
            }
            ActionStep.Result -> finished?.let { action ->
                ResultStep(
                    action = action,
                    hasHistory = completed.isNotEmpty(),
                    onClose = onClose,
                    onAnother = { selectedId = null; step = ActionStep.Pick },
                    onOpenHistory = onOpenHistory,
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PickStep(
    ready: List<MicroAction>,
    hasHistory: Boolean,
    custom: String,
    onCustom: (String) -> Unit,
    onPickExisting: (Long) -> Unit,
    onPickPreset: (String) -> Unit,
    onOpenHistory: () -> Unit,
) {
    Text(microActionTitle, Modifier.semantics { heading() }, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
    Text(microActionIntro, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp)
    if (hasHistory) {
        TextButton(onClick = onOpenHistory) { Text(historyOpenLabel) }
    }
    if (ready.isNotEmpty()) {
        Text(microActionReadyGroup, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        ready.forEach { action ->
            ActionPickCard(action.title) { onPickExisting(action.id) }
        }
    }
    microActionPresets.groupBy { it.group }.forEach { (group, items) ->
        Text(group, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        items.forEach { preset ->
            ActionPickCard(preset.title) { onPickPreset(preset.title) }
        }
    }
    Text(microActionCustomGroup, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
    Text(microActionCustomHint(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
    OutlinedTextField(
        value = custom,
        onValueChange = onCustom,
        label = { Text(microActionCustomFieldLabel) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
    )
    Button(
        onClick = { onPickPreset(custom.trim()) },
        enabled = custom.isNotBlank(),
        modifier = Modifier.fillMaxWidth().heightIn(min = ActionMinHeight),
        shape = RoundedCornerShape(12.dp),
    ) { Text(microActionCustomConfirm, fontSize = 17.sp) }
}

@Composable
private fun PredictStep(
    title: String,
    predicted: Int,
    onPredicted: (Int) -> Unit,
    onStart: () -> Unit,
    onBack: () -> Unit,
) {
    TextButton(onClick = onBack) { Text(microActionSwapLabel) }
    Text(title, Modifier.semantics { heading() }, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
    Text(microActionPredictPrompt, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp)
    ScoreRow(predicted, onPredicted)
    Button(
        onClick = onStart,
        modifier = Modifier.fillMaxWidth().heightIn(min = ActionMinHeight),
        shape = RoundedCornerShape(12.dp),
    ) {
        Text(microActionStartLabel, fontSize = 17.sp)
    }
}

@Composable
private fun RunStep(
    title: String,
    predicted: Int?,
    remainingMillis: Long,
    onFinish: () -> Unit,
    onLeave: () -> Unit,
) {
    predicted?.let {
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(999.dp)) {
            Text(microActionPredictedBadge(it), Modifier.padding(horizontal = 12.dp, vertical = 4.dp), fontSize = 14.sp)
        }
    }
    Text(title, Modifier.fillMaxWidth().semantics { heading() }, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
    val progress = (remainingMillis.toFloat() / MICRO_ACTION_MILLIS).coerceIn(0f, 1f)
    val track = MaterialTheme.colorScheme.surfaceVariant
    val active = MaterialTheme.colorScheme.primary
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(Modifier.size(256.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 8.dp.toPx()
                drawCircle(color = track, style = Stroke(width = stroke))
                if (progress > 0f) {
                    drawArc(
                        color = active,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                }
            }
            Text(
                formatCountdown(remainingMillis),
                fontFamily = FontFamily.Monospace,
                fontSize = 40.sp,
            )
        }
    }
    Text(microActionWaitBody, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
    Button(
        onClick = onFinish,
        modifier = Modifier.fillMaxWidth().heightIn(min = ActionMinHeight),
        shape = RoundedCornerShape(12.dp),
    ) {
        Text(microActionFinishTimerLabel, fontSize = 17.sp)
    }
    TextButton(onClick = onLeave, modifier = Modifier.fillMaxWidth()) { Text(microActionPauseLabel) }
}

@Composable
private fun RateStep(
    title: String,
    actual: Int,
    onActual: (Int) -> Unit,
    onSave: () -> Unit,
) {
    Text(title, Modifier.semantics { heading() }, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
    Text(microActionRatePrompt, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp)
    ScoreRow(actual, onActual)
    Button(
        onClick = onSave,
        modifier = Modifier.fillMaxWidth().heightIn(min = ActionMinHeight),
        shape = RoundedCornerShape(12.dp),
    ) {
        Text(microActionSaveFeltLabel, fontSize = 17.sp)
    }
}

@Composable
private fun ResultStep(
    action: MicroAction,
    hasHistory: Boolean,
    onClose: () -> Unit,
    onAnother: () -> Unit,
    onOpenHistory: () -> Unit,
) {
    Text(microActionResultTitle, Modifier.semantics { heading() }, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
    Text(microActionDidCopy(action.title), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = CardShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(microActionPredictLine(action.predictedDifficulty))
            Text(microActionActualLine(action.actualDifficulty))
            Text(microActionBiasCopy(action.predictedDifficulty, action.actualDifficulty), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        }
    }
    if (hasHistory) {
        OutlinedButton(
            onClick = onOpenHistory,
            modifier = Modifier.fillMaxWidth().heightIn(min = ActionMinHeight),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text(historyOpenLabel, fontSize = 17.sp)
        }
    }
    Button(
        onClick = onClose,
        modifier = Modifier.fillMaxWidth().heightIn(min = ActionMinHeight),
        shape = RoundedCornerShape(12.dp),
    ) { Text(microActionHomeLabel, fontSize = 17.sp) }
    TextButton(onClick = onAnother, modifier = Modifier.fillMaxWidth()) { Text(microActionAnotherLabel) }
}

@Composable
private fun ScoreRow(value: Int, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(1..5, 6..10).forEach { range ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                range.forEach { score ->
                    val selected = score == value
                    Surface(
                        onClick = { onSelect(score) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.weight(1f).heightIn(min = ActionMinHeight),
                    ) {
                        Box(Modifier.fillMaxWidth().heightIn(min = ActionMinHeight), contentAlignment = Alignment.Center) {
                            Text(
                                "$score",
                                textAlign = TextAlign.Center,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                fontSize = 16.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionPickCard(title: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface,
        shape = CardShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        modifier = Modifier.fillMaxWidth().heightIn(min = ActionMinHeight),
    ) {
        Box(
            Modifier.fillMaxWidth().heightIn(min = ActionMinHeight).padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(title, fontSize = 17.sp)
        }
    }
}
