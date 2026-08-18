package com.anchor.app.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anchor.app.insights.BiasChart
import com.anchor.app.insights.Sparkline
import com.anchor.app.insights.averagePredictionBias
import com.anchor.app.insights.biasCopy
import com.anchor.app.insights.predictionPairs
import com.anchor.app.insights.stabilityCopy
import com.anchor.app.insights.wakeStabilityMinutes
import com.anchor.app.safety.ReturnToPracticeScreen
import com.anchor.app.safety.SafetyAction
import com.anchor.app.safety.SafetyOutcome
import com.anchor.app.safety.SafetyPolicy
import com.anchor.app.safety.recoveredToPractice
import com.anchor.app.storage.AgeGroup
import com.anchor.app.storage.AnchorStore
import com.anchor.app.storage.AssessmentInput
import com.anchor.app.storage.MicroAction
import com.anchor.app.storage.StoredAssessment

private enum class ReassessStep { Invite, Phq9, Gad7, Buffer, SafetyResult, Compare, ReturnToPractice }

private val CardShape = RoundedCornerShape(16.dp)
private val PillShape = RoundedCornerShape(28.dp)

@Composable
fun ReassessmentFlow(
    store: AnchorStore,
    nowMillis: () -> Long,
    persist: Boolean = true,
    localMinuteOfDay: (Long) -> Int,
    onOpenGuide: () -> Unit = {},
    onOpenChecklist: () -> Unit = {},
    onAddMicroAction: () -> Unit = {},
    onPostpone: () -> Unit = {},
    onRecovered: () -> Unit = {},
    onClose: () -> Unit,
) {
    var step by remember { mutableStateOf(ReassessStep.Invite) }
    val phq9 = remember { mutableStateListOf<Int?>().also { list -> repeat(9) { list.add(null) } } }
    val gad7 = remember { mutableStateListOf<Int?>().also { list -> repeat(7) { list.add(null) } } }
    var outcome by remember { mutableStateOf<SafetyOutcome?>(null) }
    var justRecovered by remember { mutableStateOf(false) }
    val profile = store.userProfile()
    val due = reassessmentDue(store.assessments(), nowMillis())

    fun finishAssessment() {
        val phqAnswers = phq9.map { it ?: 0 }
        val gadAnswers = if (shouldSkipGad7(phq9.getOrNull(8))) List(7) { 0 } else gad7.map { it ?: 0 }
        val previous = store.safetyState()
        outcome = if (persist) {
            store.evaluateAndStore(AssessmentInput(phqAnswers, gadAnswers, nowMillis()))
        } else {
            SafetyPolicy.evaluate(phqAnswers, gadAnswers, nowMillis(), previous = previous)
        }
        justRecovered = recoveredToPractice(previous, outcome!!.state)
        step = ReassessStep.Buffer
    }

    when (step) {
        ReassessStep.Invite -> ReassessmentInviteScreen(
            store = store,
            nowMillis = nowMillis(),
            due = due,
            localMinuteOfDay = localMinuteOfDay,
            onStart = { step = ReassessStep.Phq9 },
            onPostpone = {
                onPostpone()
                onClose()
            },
            onClose = onClose,
        )
        ReassessStep.Phq9 -> ScaleForm(
            title = "情绪自评 (PHQ-9)",
            questions = phq9Questions,
            answers = phq9,
            onAnswer = { index, score -> phq9[index] = score },
            highlightLast = true,
            onBack = { step = ReassessStep.Invite },
            onSubmit = {
                if (shouldSkipGad7(phq9.getOrNull(8))) finishAssessment() else step = ReassessStep.Gad7
            },
        )
        ReassessStep.Gad7 -> ScaleForm(
            title = "焦虑自评 (GAD-7)",
            questions = gad7Questions,
            answers = gad7,
            onAnswer = { index, score -> gad7[index] = score },
            onBack = { step = ReassessStep.Phq9 },
            onSubmit = { finishAssessment() },
        )
        ReassessStep.Buffer -> BufferStep {
            val action = outcome!!.action
            step = when {
                justRecovered -> ReassessStep.ReturnToPractice
                action == SafetyAction.CrisisGuidance || action == SafetyAction.MedicalWaiting -> ReassessStep.SafetyResult
                else -> ReassessStep.Compare
            }
        }
        ReassessStep.SafetyResult -> ResultStep(
            outcome = outcome!!,
            youth = profile.ageGroup == AgeGroup.Youth14To17,
            region = profile.crisisRegion,
            onChooseAnchor = onClose,
            onOpenGuide = onOpenGuide,
            onOpenChecklist = onOpenChecklist,
            onFinish = onClose,
        )
        ReassessStep.Compare -> ReassessmentResultScreen(
            assessments = store.assessments(),
            actions = store.microActions(),
            onContinue = onClose,
            onAddMicroAction = onAddMicroAction,
        )
        ReassessStep.ReturnToPractice -> ReturnToPracticeScreen(
            onHome = {
                onRecovered()
                onClose()
            },
        )
    }
}

@Composable
internal fun ReassessmentInviteScreen(
    store: AnchorStore,
    nowMillis: Long,
    due: Boolean,
    localMinuteOfDay: (Long) -> Int,
    onStart: () -> Unit,
    onPostpone: () -> Unit,
    onClose: () -> Unit,
) {
    val wakeMinutes = wakeMinutesInWindow(store.rhythmEntries(), localMinuteOfDay, nowMillis)
    val wakeEntries = store.rhythmEntries().filter { entry ->
        val wake = entry.wakeAtMillis
        wake != null && wake >= nowMillis - REASSESSMENT_INTERVAL_MILLIS
    }
    val stability = wakeStabilityMinutes(wakeEntries, localMinuteOfDay)
    val overestimates = overestimateCount(store.microActions())
    val recorded = recordedActionCount(store.microActions())
    val bias = averagePredictionBias(store.microActions())

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TextButton(onClick = onClose, modifier = Modifier.align(Alignment.End)) { Text("关闭") }
        Text(reassessmentKicker, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Text(inviteHeadline(due), Modifier.semantics { heading() }, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, lineHeight = 34.sp)
        Text(inviteBody, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp, lineHeight = 24.sp)

        InviteCard(wakeCardTitle, stabilityCopy(stability), wakeCardDetail) {
            if (wakeMinutes.size >= 2) Sparkline(wakeMinutes.map { it.toFloat() }, MaterialTheme.colorScheme.primary)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.weight(1f)) {
                InviteCard(biasCardTitle, overestimateCopy(overestimates), biasCopy(bias))
            }
            Box(Modifier.weight(1f)) {
                InviteCard(actionCardTitle, recordedActionCopy(recorded), actionCardDetail)
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(inviteDurationHint, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth().height(56.dp), shape = PillShape) {
            Text(startReassessmentLabel, fontSize = 17.sp)
        }
        TextButton(onClick = onPostpone, modifier = Modifier.fillMaxWidth()) { Text(postponeReassessmentLabel) }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
internal fun ReassessmentResultScreen(
    assessments: List<StoredAssessment>,
    actions: List<MicroAction>,
    onContinue: () -> Unit,
    onAddMicroAction: () -> Unit,
) {
    val pair = baselineAndLatest(assessments)
    val latest = assessments.maxByOrNull { it.completedAtMillis }
    val pairs = predictionPairs(actions).takeLast(4)

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(resultEyebrow, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        Text(resultHeadline, Modifier.semantics { heading() }, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, lineHeight = 32.sp)
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = CardShape,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        ) {
            Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(resultInsight, textAlign = TextAlign.Center, fontSize = 16.sp, lineHeight = 24.sp)
            }
        }
        if (pair != null) {
            val (baseline, current) = pair
            CompareBlock(phqCompareTitle, baseline.phq9Score, current.phq9Score, 27)
            Text(scoreDeltaCopy(baseline.phq9Score, current.phq9Score, "PHQ-9"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, textAlign = TextAlign.Center)
            CompareBlock(gadCompareTitle, baseline.gad7Score, current.gad7Score, 21)
            Text(scoreDeltaCopy(baseline.gad7Score, current.gad7Score, "GAD-7"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, textAlign = TextAlign.Center)
        } else if (latest != null) {
            Text("PHQ-9 ${latest.phq9Score} · GAD-7 ${latest.gad7Score}", fontSize = 16.sp)
            Text("这是目前唯一一次评估，还没有可对比的基线。", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, textAlign = TextAlign.Center)
        }
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = CardShape,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(predictionTrendTitle, fontWeight = FontWeight.SemiBold)
                Text(predictionTrendDetail, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, lineHeight = 22.sp)
                if (pairs.isNotEmpty()) BiasChart(pairs) else Text("记录还不够", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Button(onClick = onContinue, modifier = Modifier.fillMaxWidth().height(52.dp), shape = CardShape) {
            Text(continuePracticeLabel)
        }
        OutlinedButton(onClick = onAddMicroAction, modifier = Modifier.fillMaxWidth().height(52.dp), shape = CardShape) {
            Text(addMicroActionLabel)
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun InviteCard(title: String, value: String, detail: String, extra: (@Composable () -> Unit)? = null) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = CardShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(value, color = MaterialTheme.colorScheme.primary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            extra?.invoke()
            Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, lineHeight = 20.sp)
        }
    }
}

@Composable
private fun CompareBlock(title: String, baseline: Int, current: Int, max: Int) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ScoreTile(baselineLabel, baseline, max, accent = false, modifier = Modifier.weight(1f))
            ScoreTile(currentLabel, current, max, accent = true, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ScoreTile(label: String, score: Int, max: Int, accent: Boolean, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = if (accent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface,
        shape = CardShape,
        border = BorderStroke(1.dp, if (accent) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, color = if (accent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            Text("$score", fontFamily = FontFamily.Monospace, fontSize = 32.sp, color = if (accent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
            ScoreBar(score, max, if (accent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary)
        }
    }
}

@Composable
private fun ScoreBar(score: Int, max: Int, color: Color) {
    Box(Modifier.fillMaxWidth().height(6.dp)) {
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(999.dp), modifier = Modifier.fillMaxSize()) {}
        Surface(
            color = color,
            shape = RoundedCornerShape(999.dp),
            modifier = Modifier.fillMaxWidth((score.toFloat() / max).coerceIn(0.04f, 1f)).height(6.dp).align(Alignment.CenterStart),
        ) {}
    }
}
