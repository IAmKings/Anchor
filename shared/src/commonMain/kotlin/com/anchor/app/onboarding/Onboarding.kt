package com.anchor.app.onboarding

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anchor.app.safety.SafetyAction
import com.anchor.app.safety.SafetyOutcome
import com.anchor.app.safety.SafetyPolicy
import com.anchor.app.safety.SafetyState
import com.anchor.app.safety.crisisResource
import com.anchor.app.storage.AgeGroup
import com.anchor.app.storage.AnchorStore
import com.anchor.app.storage.AssessmentInput
import com.anchor.app.storage.CrisisRegion
import com.anchor.app.storage.FirstAnchor
import com.anchor.app.storage.UserProfile
import kotlinx.coroutines.delay

private enum class Step { Welcome, Phq9, Gad7, Buffer, Result, Anchor }

private val CardShape = RoundedCornerShape(16.dp)
private val PillShape = RoundedCornerShape(28.dp)
private val Terracotta = Color(0xFFB26A4F)

@Composable
fun FirstRunAssessment(
    store: AnchorStore,
    nowMillis: () -> Long,
    persist: Boolean = true,
    onOpenGuide: () -> Unit = {},
    onOpenChecklist: () -> Unit = {},
    onClose: () -> Unit,
) {
    var step by remember { mutableStateOf(Step.Welcome) }
    var ageGroup by remember { mutableStateOf<AgeGroup?>(null) }
    var crisisRegion by remember { mutableStateOf(store.userProfile().crisisRegion) }
    var agreed by remember { mutableStateOf(false) }
    val phq9 = remember { mutableStateListOf<Int?>().also { list -> repeat(9) { list.add(null) } } }
    val gad7 = remember { mutableStateListOf<Int?>().also { list -> repeat(7) { list.add(null) } } }
    var outcome by remember { mutableStateOf<SafetyOutcome?>(null) }

    fun persistProfile(complete: Boolean, firstAnchor: FirstAnchor? = null) {
        if (!persist) return
        val current = store.userProfile()
        store.saveUserProfile(
            UserProfile(
                ageGroup = ageGroup,
                onboardingComplete = complete,
                firstAnchor = firstAnchor ?: current.firstAnchor,
                firstAnchorAtMillis = if (firstAnchor != null && current.firstAnchorAtMillis == null) {
                    nowMillis()
                } else {
                    current.firstAnchorAtMillis
                },
                crisisRegion = crisisRegion,
            ),
        )
    }

    fun finishAssessment() {
        val phqAnswers = phq9.map { it ?: 0 }
        val gadAnswers = if (shouldSkipGad7(phq9.getOrNull(8))) List(7) { 0 } else gad7.map { it ?: 0 }
        outcome = if (persist) {
            store.evaluateAndStore(AssessmentInput(phqAnswers, gadAnswers, nowMillis()))
        } else {
            SafetyPolicy.evaluate(phqAnswers, gadAnswers, nowMillis(), previous = SafetyState())
        }
        persistProfile(complete = false)
        step = Step.Buffer
    }

    fun completeWithoutAnchor() {
        persistProfile(complete = true)
        onClose()
    }

    when (step) {
        Step.Welcome -> Welcome(
            ageGroup = ageGroup,
            onAgeGroup = { ageGroup = it },
            crisisRegion = crisisRegion,
            onCrisisRegion = { crisisRegion = it },
            agreed = agreed,
            onAgreed = { agreed = it },
            allowClose = !persist,
            onClose = onClose,
            onStart = { step = Step.Phq9 },
        )
        Step.Phq9 -> ScaleForm(
            title = "情绪自评 (PHQ-9)",
            questions = phq9Questions,
            answers = phq9,
            onAnswer = { index, score -> phq9[index] = score },
            highlightLast = true,
            onSubmit = {
                if (shouldSkipGad7(phq9.getOrNull(8))) finishAssessment() else step = Step.Gad7
            },
        )
        Step.Gad7 -> ScaleForm(
            title = "焦虑自评 (GAD-7)",
            questions = gad7Questions,
            answers = gad7,
            onAnswer = { index, score -> gad7[index] = score },
            onSubmit = { finishAssessment() },
        )
        Step.Buffer -> BufferStep { step = Step.Result }
        Step.Result -> ResultStep(
            outcome = outcome!!,
            youth = ageGroup == AgeGroup.Youth14To17,
            region = crisisRegion,
            onChooseAnchor = { step = Step.Anchor },
            onOpenGuide = {
                persistProfile(complete = true)
                onOpenGuide()
            },
            onOpenChecklist = {
                persistProfile(complete = true)
                onOpenChecklist()
            },
            onFinish = { completeWithoutAnchor() },
        )
        Step.Anchor -> FirstAnchorChoice(
            onConfirm = { firstAnchor ->
                persistProfile(complete = true, firstAnchor = firstAnchor)
                onClose()
            },
            onBack = { step = Step.Result },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Welcome(
    ageGroup: AgeGroup?,
    onAgeGroup: (AgeGroup) -> Unit,
    crisisRegion: CrisisRegion,
    onCrisisRegion: (CrisisRegion) -> Unit,
    agreed: Boolean,
    onAgreed: (Boolean) -> Unit,
    allowClose: Boolean,
    onClose: () -> Unit,
    onStart: () -> Unit,
) {
    var legal by remember { mutableStateOf<Pair<String, String>?>(null) }
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (allowClose) {
            TextButton(onClick = onClose, modifier = Modifier.align(Alignment.End)) { Text("关闭") }
        } else {
            Spacer(Modifier.height(36.dp))
        }
        Spacer(Modifier.height(24.dp))
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        ) {
            Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.size(56.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape, modifier = Modifier.fillMaxSize()) {}
                    Text("锚", color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 22.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Text("锚点 Anchor", Modifier.semantics { heading() }, color = MaterialTheme.colorScheme.primary, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(
            welcomeTagline,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 15.sp,
            lineHeight = 24.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 28.dp),
        )
        Spacer(Modifier.height(48.dp))
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = CardShape,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        ) {
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape) {
                    Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                        Text("i", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("年龄确认", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text(ageDisclaimer, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, lineHeight = 20.sp)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AgeChip("18 岁以上", ageGroup == AgeGroup.Adult18Plus, Modifier.weight(1f)) { onAgeGroup(AgeGroup.Adult18Plus) }
            AgeChip("14–17 岁", ageGroup == AgeGroup.Youth14To17, Modifier.weight(1f)) { onAgeGroup(AgeGroup.Youth14To17) }
        }
        Spacer(Modifier.height(16.dp))
        Text("危机资源地区", Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            CrisisRegion.entries.forEach { region ->
                val selected = crisisRegion == region
                Surface(
                    onClick = { onCrisisRegion(region) },
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
                ) {
                    Text(region.label(), Modifier.padding(horizontal = 12.dp, vertical = 8.dp), fontSize = 14.sp)
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .toggleable(value = agreed, role = Role.Checkbox, onValueChange = onAgreed)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Checkbox(checked = agreed, onCheckedChange = onAgreed)
            Text(
                agreementLabel,
                modifier = Modifier.weight(1f),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(Modifier.fillMaxWidth().padding(start = 40.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { legal = termsTitle to termsBody }) { Text("用户协议", fontSize = 14.sp) }
            TextButton(onClick = { legal = privacyTitle to privacyBody }) { Text("隐私政策", fontSize = 14.sp) }
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onStart,
            enabled = canStartBaseline(ageGroup != null, agreed),
            modifier = Modifier.fillMaxWidth().height(64.dp),
            shape = CardShape,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(startBaselineLabel, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text(startBaselineHint, fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
            }
        }
        Spacer(Modifier.height(24.dp))
    }
    legal?.let { (title, body) ->
        AlertDialog(
            onDismissRequest = { legal = null },
            confirmButton = { TextButton(onClick = { legal = null }) { Text("知道了") } },
            title = { Text(title) },
            text = { Text(body, lineHeight = 22.sp) },
        )
    }
}

@Composable
private fun AgeChip(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surface,
        shape = CardShape,
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
    ) {
        Text(text, Modifier.fillMaxWidth().padding(vertical = 14.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Medium)
    }
}

@Composable
internal fun ScaleForm(
    title: String,
    questions: List<String>,
    answers: List<Int?>,
    onAnswer: (Int, Int) -> Unit,
    highlightLast: Boolean = false,
    onBack: (() -> Unit)? = null,
    onSubmit: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (onBack != null) {
            TextButton(onClick = onBack, modifier = Modifier.align(Alignment.Start)) { Text("返回") }
        }
        Text("锚点 Anchor", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        Text(title, Modifier.semantics { heading() }, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Text(scalePrompt, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp, lineHeight = 24.sp, textAlign = TextAlign.Center)
        questions.forEachIndexed { index, question ->
            val crisis = highlightLast && index == questions.lastIndex
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = CardShape,
                border = BorderStroke(
                    1.dp,
                    if (crisis) Terracotta.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
                ),
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("${index + 1}. $question", fontSize = 16.sp, lineHeight = 24.sp)
                    if (crisis) {
                        Text(item9CrisisNote, color = Terracotta, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        assessmentChoices.chunked(2).forEachIndexed { rowIndex, row ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEachIndexed { colIndex, choice ->
                                    val score = rowIndex * 2 + colIndex
                                    val selected = answers[index] == score
                                    Surface(
                                        onClick = { onAnswer(index, score) },
                                        modifier = Modifier.weight(1f),
                                        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                                    ) {
                                        Text(
                                            choice,
                                            Modifier.fillMaxWidth().padding(vertical = 10.dp),
                                            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 14.sp,
                                            textAlign = TextAlign.Center,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Button(
            onClick = onSubmit,
            enabled = allAnswered(answers),
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = PillShape,
        ) { Text(submitAssessmentLabel, fontSize = 17.sp) }
        Text(scaleDisclaimer, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, textAlign = TextAlign.Center)
        Text(scaleSource, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
internal fun BufferStep(onDone: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(bufferDelayMillis)
        onDone()
    }
    val pulse = rememberInfiniteTransition(label = "buffer")
    val outer by pulse.animateFloat(0.92f, 1.08f, infiniteRepeatable(tween(2200), RepeatMode.Reverse), label = "outer")
    val inner by pulse.animateFloat(0.96f, 1.06f, infiniteRepeatable(tween(1800), RepeatMode.Reverse), label = "inner")
    Column(
        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars).padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(192.dp)) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                shape = CircleShape,
                modifier = Modifier.size(128.dp).graphicsLayer { scaleX = outer; scaleY = outer },
            ) {}
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                shape = CircleShape,
                modifier = Modifier.size(96.dp).graphicsLayer { scaleX = inner; scaleY = inner },
            ) {}
            Surface(color = MaterialTheme.colorScheme.surface, shape = CircleShape, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))) {
                Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                    Text("锚", color = MaterialTheme.colorScheme.primary, fontSize = 20.sp)
                }
            }
        }
        Spacer(Modifier.height(28.dp))
        Text(bufferTitle, Modifier.semantics { heading() }, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(bufferBody, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, lineHeight = 24.sp)
    }
}

@Composable
internal fun ResultStep(
    outcome: SafetyOutcome,
    youth: Boolean,
    region: CrisisRegion,
    onChooseAnchor: () -> Unit,
    onOpenGuide: () -> Unit,
    onOpenChecklist: () -> Unit,
    onFinish: () -> Unit,
) {
    when (outcome.action) {
        SafetyAction.CrisisGuidance -> CrisisResult(outcome, youth, region, onOpenGuide, onOpenChecklist, onFinish)
        SafetyAction.MedicalWaiting -> MedicalResult(outcome, youth, region, onOpenGuide, onOpenChecklist, onFinish)
        else -> MildResult(outcome, onChooseAnchor)
    }
}

@Composable
private fun MildResult(outcome: SafetyOutcome, onChooseAnchor: () -> Unit) {
    val score = outcome.assessment.phq9Score
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text("锚点 Anchor", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        ScoreRing(score = score, max = 27, caption = phqBandLabel(outcome.assessment.phq9Band))
        Text(mildInsight, fontSize = 17.sp, textAlign = TextAlign.Center, lineHeight = 26.sp)
        Text(
            "PHQ-9 ${outcome.assessment.phq9Score} · GAD-7 ${outcome.assessment.gad7Score}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
        )
        Text(scaleDisclaimer, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Button(onClick = onChooseAnchor, modifier = Modifier.fillMaxWidth().height(56.dp), shape = PillShape) {
            Text(chooseFirstAnchorLabel, fontSize = 17.sp)
        }
        Text(scaleSource, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun MedicalResult(
    outcome: SafetyOutcome,
    youth: Boolean,
    region: CrisisRegion,
    onOpenGuide: () -> Unit,
    onOpenChecklist: () -> Unit,
    onFinish: () -> Unit,
) {
    val resource = crisisResource(region, youth)
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("锚点 Anchor", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        Surface(color = MaterialTheme.colorScheme.surface, shape = CircleShape, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))) {
            Box(Modifier.size(168.dp), contentAlignment = Alignment.Center) {
                Text("${outcome.assessment.phq9Score}", color = Terracotta, fontFamily = FontFamily.Monospace, fontSize = 64.sp)
            }
        }
        Surface(color = MaterialTheme.colorScheme.secondary, shape = RoundedCornerShape(999.dp)) {
            Text(resultBadge(outcome.action), Modifier.padding(horizontal = 16.dp, vertical = 6.dp), color = MaterialTheme.colorScheme.onSecondary, fontSize = 14.sp)
        }
        QuoteCard()
        WaitingActions(onOpenGuide, onOpenChecklist)
        CrisisSteps(resource.hotline, resource.emergency)
        Button(onClick = onFinish, modifier = Modifier.fillMaxWidth().height(52.dp), shape = CardShape) { Text("进入就医等待期") }
        Text(scaleSource, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun CrisisResult(
    outcome: SafetyOutcome,
    youth: Boolean,
    region: CrisisRegion,
    onOpenGuide: () -> Unit,
    onOpenChecklist: () -> Unit,
    onFinish: () -> Unit,
) {
    val resource = crisisResource(region, youth)
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("你现在不是一个人", Modifier.semantics { heading() }, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
        Text(
            "如果你现在处于立即危险中，请直接拨打 ${resource.emergency}。",
            color = Terracotta,
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp,
            lineHeight = 26.sp,
        )
        CrisisSteps(resource.hotline, resource.emergency)
        WaitingActions(onOpenGuide, onOpenChecklist)
        Text(
            "PHQ-9 ${outcome.assessment.phq9Score} · GAD-7 ${outcome.assessment.gad7Score} · ${resultBadge(outcome.action)}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
        )
        Button(
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = CardShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
            ),
        ) { Text("立即危险请拨 ${resource.emergency}") }
        TextButton(onClick = onFinish, modifier = Modifier.fillMaxWidth()) { Text("先进入就医等待期") }
        Text(scaleSource, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun QuoteCard() {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = CardShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
    ) {
        Row(Modifier.fillMaxWidth()) {
            Box(Modifier.size(width = 4.dp, height = 88.dp)) {
                Surface(color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxSize()) {}
            }
            Text(medicalQuote, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp, lineHeight = 24.sp)
        }
    }
}

@Composable
private fun WaitingActions(onOpenGuide: () -> Unit, onOpenChecklist: () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(color = Terracotta, shape = CircleShape, modifier = Modifier.size(8.dp)) {}
            Text(medicalWaitingTitle, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        }
        Text(medicalWaitingBody, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, lineHeight = 22.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(
                onClick = onOpenGuide,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.surface,
                shape = CardShape,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("就医指南", fontWeight = FontWeight.SemiBold)
                    Text("了解看诊流程与准备", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, lineHeight = 18.sp)
                }
            }
            Surface(
                onClick = onOpenChecklist,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.surface,
                shape = CardShape,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("躯体检查清单", fontWeight = FontWeight.SemiBold)
                    Text("排除生理因素干扰", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, lineHeight = 18.sp)
                }
            }
        }
    }
}

@Composable
private fun CrisisSteps(hotline: String, emergency: String) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = CardShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("危机干预 1-2-3", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            StepRow("1", "拨打心理援助热线", hotline)
            StepRow("2", "前往精神科或急诊", "去最近的医院急诊，或直接预约精神科。")
            StepRow("3", "告诉一位身边可信的人", "找一个你信得过的朋友或家人。立即危险请拨 $emergency。")
        }
    }
}

@Composable
private fun StepRow(number: String, title: String, body: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape) {
            Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                Text(number, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, lineHeight = 20.sp)
        }
    }
}

@Composable
private fun ScoreRing(score: Int, max: Int, caption: String) {
    val progress = (score.toFloat() / max).coerceIn(0f, 1f)
    val track = MaterialTheme.colorScheme.surfaceVariant
    val active = MaterialTheme.colorScheme.primaryContainer
    Box(Modifier.size(192.dp), contentAlignment = Alignment.Center) {
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
        Surface(color = MaterialTheme.colorScheme.surface, shape = CircleShape, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))) {
            Column(Modifier.size(160.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("$score", color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace, fontSize = 36.sp)
                    Text("/$max", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp, modifier = Modifier.padding(bottom = 6.dp, start = 2.dp))
                }
                Text(caption, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun FirstAnchorChoice(onConfirm: (FirstAnchor) -> Unit, onBack: (() -> Unit)? = null) {
    var selected by remember { mutableStateOf<FirstAnchor?>(null) }
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (onBack != null) {
            TextButton(onClick = onBack) { Text("返回") }
        }
        Text(firstAnchorHeadline, Modifier.semantics { heading() }, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, lineHeight = 32.sp)
        Text(firstAnchorBody, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp, lineHeight = 24.sp)
        (p0FirstAnchors + p1FirstAnchors).forEach { option ->
            val checked = selected == option.anchor
            Surface(
                onClick = { selected = option.anchor },
                color = if (checked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surface,
                shape = CardShape,
                border = BorderStroke(1.dp, if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
                modifier = Modifier.selectable(selected = checked, role = Role.RadioButton, onClick = { selected = option.anchor }),
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(if (checked) "✓  ${option.title}" else option.title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text(option.description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, lineHeight = 20.sp)
                }
            }
        }
        Text(firstAnchorCommitHint, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Button(
            onClick = { onConfirm(selected!!) },
            enabled = selected != null,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = PillShape,
        ) { Text("确认选择") }
        Spacer(Modifier.height(16.dp))
    }
}
