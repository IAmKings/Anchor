package com.anchor.app.worry

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import kotlinx.coroutines.delay
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import com.anchor.app.onboarding.DoneIcon
import com.anchor.app.onboarding.LockedIcon
import com.anchor.app.onboarding.ThoughtIcon
import com.anchor.app.onboarding.UnlockedIcon
import com.anchor.app.ui.AnchorIconWell
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.anchor.app.ui.AnchorBackBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anchor.app.action.microActionPresets
import com.anchor.app.storage.AnchorStore
import com.anchor.app.storage.WorryCard
import com.anchor.app.storage.WorryResolution

private val CardShape = RoundedCornerShape(16.dp)
private val VaultActionMinHeight = 52.dp

private enum class VaultStep { Overview, Process, Convert, Done }

/** 三选一退场动效：已不再重要=左滑划掉；暂时无解/转化=淡出。 */
private enum class VaultExit { Strike, Fade }

@Composable
fun WorryVaultScreen(
    store: AnchorStore,
    nowMillis: () -> Long,
    isSessionOpen: () -> Boolean,
    nextSessionMillis: () -> Long,
    nextSessionLabel: () -> String,
    externalRevision: Int,
    onHang: () -> Unit,
    audioPlaybackStatus: String?,
    audioPlaying: Boolean,
    onStopAudio: () -> Unit,
    onPlayAudio: (String) -> Unit,
    onClose: () -> Unit,
) {
    var confirmationVisible by remember { mutableStateOf(false) }
    var forcedOpen by remember { mutableStateOf(false) }
    var savedMessage by remember { mutableStateOf<String?>(null) }
    var action by remember { mutableStateOf("") }
    var processedCount by remember { mutableIntStateOf(0) }
    var step by remember { mutableStateOf(VaultStep.Overview) }
    var revision by remember { mutableIntStateOf(0) }
    var selectedCardId by remember { mutableStateOf<Long?>(null) }
    var continuous by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    // 确认开箱时整体替换滚动状态实例：即时回到顶部，不受惯性/布局时序影响。
    var scrollState by remember { mutableStateOf(ScrollState(0)) }
    val cards = remember(revision, externalRevision) { store.worryCards() }
    val pending = cards.filter { it.resolution == WorryResolution.Pending }
    // 专场开/闭按墙钟判定且组合期无订阅：每分钟对齐一次，让开始/结束在一分钟内翻转。
    var sessionTick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L + 1_000L)
            sessionTick++
        }
    }
    val open = isSessionOpen() || forcedOpen
    // 选中卡优先（点击列表直接处理该张），未选中按列表顺序。
    val current = pending.firstOrNull { it.id == selectedCardId } ?: pending.firstOrNull()
    // 顶部返回层级化：标签即目的地（概览=退出保险箱；处理=回开箱；转化=回三选一）。
    val backLabel = when (step) {
        VaultStep.Process -> "开箱"
        VaultStep.Convert -> "三选一"
        else -> "返回"
    }
    AnchorBackBar(
        onBack = when (step) {
            VaultStep.Process -> ({
                step = VaultStep.Overview
                selectedCardId = null
                continuous = false
            })
            VaultStep.Convert -> ({ step = VaultStep.Process })
            else -> onClose
        },
        label = backLabel,
        title = vaultTitle,
    ) {
        // 开箱与未开箱各自独立的滚动容器：新容器从顶部开始，状态标与主按钮始终首屏可见。
        if (open) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                when {
                    step == VaultStep.Done -> DoneStep(processedCount, onClose)
                    step == VaultStep.Convert && current != null -> ConvertStep(
                        card = current,
                        action = action,
                        nowMillis = nowMillis(),
                        onActionChange = { action = it },
                        onConfirm = {
                            store.resolveWorryAsAction(current.id, action, nowMillis())
                            action = ""
                            processedCount++
                            revision++
                            step = when {
                                continuous && pending.size > 1 -> VaultStep.Process
                                pending.size <= 1 -> VaultStep.Done
                                else -> VaultStep.Overview
                            }
                        },
                    )
                    step == VaultStep.Process && current != null -> ProcessStep(
                        card = current,
                        nowMillis = nowMillis(),
                        nextSessionLabel = nextSessionLabel(),
                        audioPlaybackStatus = audioPlaybackStatus,
                        audioPlaying = audioPlaying,
                        onStopAudio = onStopAudio,
                        onPlayAudio = onPlayAudio,
                        onAction = { step = VaultStep.Convert },
                        onUnsolvable = {
                            store.resolveWorryCard(current.id, WorryResolution.Unsolvable)
                            processedCount++
                            revision++
                            step = when {
                                continuous && pending.size > 1 -> VaultStep.Process
                                pending.size <= 1 -> VaultStep.Done
                                else -> VaultStep.Overview
                            }
                        },
                        onDismiss = {
                            store.dismissWorryCard(current.id)
                            processedCount++
                            revision++
                            step = when {
                                continuous && pending.size > 1 -> VaultStep.Process
                                pending.size <= 1 -> VaultStep.Done
                                else -> VaultStep.Overview
                            }
                        },
                    )
                    else -> OverviewStep(
                        open = true,
                        pending = pending,
                        selectedCardId = selectedCardId,
                        onCardTap = { id ->
                            continuous = false
                            selectedCardId = id
                            scope.launch {
                                delay(250)
                                step = VaultStep.Process
                            }
                        },
                        savedMessage = savedMessage,
                        confirmationVisible = false,
                        nextSessionLabel = nextSessionLabel(),
                        nowMillis = nowMillis(),
                        onHang = onHang,
                        onAskOpenNow = { confirmationVisible = true },
                        onConfirmOpen = {
                            forcedOpen = true
                            scrollState = ScrollState(0)
                            confirmationVisible = false
                        },
                        onWaitForSession = { confirmationVisible = false },
                        onStartProcess = {
                            continuous = true
                            selectedCardId = null
                            step = VaultStep.Process
                        },
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        } else {
            // 未开箱：概览（挂卡入口走全局浮层）。
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                OverviewStep(
                    open = false,
                    pending = pending,
                    selectedCardId = null,
                    onCardTap = {},
                    savedMessage = null,
                    confirmationVisible = confirmationVisible,
                    nextSessionLabel = nextSessionLabel(),
                    nowMillis = nowMillis(),
                    onHang = onHang,
                    onAskOpenNow = { confirmationVisible = true },
                    onConfirmOpen = { forcedOpen = true },
                    onWaitForSession = { confirmationVisible = false },
                    onStartProcess = { step = VaultStep.Process },
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun OverviewStep(
    open: Boolean,
    pending: List<WorryCard>,
    selectedCardId: Long?,
    onCardTap: (Long) -> Unit,
    savedMessage: String?,
    confirmationVisible: Boolean,
    nextSessionLabel: String,
    nowMillis: Long,
    onHang: () -> Unit,
    onAskOpenNow: () -> Unit,
    onConfirmOpen: () -> Unit,
    onWaitForSession: () -> Unit,
    onStartProcess: () -> Unit,
) {
    // 未开箱：状态标 + 张数卡（含挂卡入口，浮层式）+ 确认开箱卡；开箱：收起挂卡入口，专注整理。
    if (!open) {
        StatusMark(LockedIcon, vaultLockedCaption)
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = CardShape,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(vaultRuminationMessage(nextSessionLabel), fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 26.sp)
                Text(vaultPendingSummary(pending.size), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp, lineHeight = 24.sp)
                OutlinedButton(
                    onClick = onHang,
                    modifier = Modifier.fillMaxWidth().heightIn(min = VaultActionMinHeight),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(vaultHangEntryLabel, fontSize = 17.sp)
                }
                OutlinedButton(
                    onClick = onAskOpenNow,
                    modifier = Modifier.fillMaxWidth().heightIn(min = VaultActionMinHeight),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(vaultAskOpenNowLabel, fontSize = 17.sp)
                }
            }
        }
        if (confirmationVisible) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                shape = CardShape,
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(vaultConfirmOpenHint, fontSize = 15.sp, lineHeight = 24.sp)
                    Button(
                        onClick = onConfirmOpen,
                        modifier = Modifier.fillMaxWidth().heightIn(min = VaultActionMinHeight),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(vaultConfirmOpenLabel, fontSize = 17.sp)
                    }
                    TextButton(onClick = onWaitForSession) { Text(vaultWaitForSessionLabel) }
                }
            }
        }
    } else {
        StatusMark(UnlockedIcon, vaultOpenCaption)
        Text(vaultOpenBreath, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Text(vaultDeskHint, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        if (pending.isEmpty()) {
            Text(vaultEmptyPending, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            // 主操作前置：长列表（数十张卡）下按钮无需滚动即可见；列表沉底作浏览详情。
            Button(
                // 「第一项」= 连续模式从第一张开始（处理完自动进下一张）。
                onClick = onStartProcess,
                modifier = Modifier.fillMaxWidth().heightIn(min = VaultActionMinHeight),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(vaultStartFirstLabel, fontSize = 17.sp)
            }
            Text(
                vaultAcceptedQuote,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            pending.forEach { card ->
                PreviewCard(
                    card,
                    nowMillis,
                    highlighted = card.id == selectedCardId,
                    onClick = { onCardTap(card.id) },
                )
            }
        }
    }
}



@Composable
private fun PreviewCard(
    card: WorryCard,
    nowMillis: Long,
    highlighted: Boolean = false,
    onClick: () -> Unit = {},
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface,
        shape = CardShape,
        border = if (highlighted) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
        },
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(worryRecordedLabel(card.sealedAtMillis, nowMillis), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            Text(
                if (card.content.isBlank()) vaultAudioHangLabel else card.content,
                fontSize = 16.sp,
                lineHeight = 26.sp,
            )
        }
    }
}

@Composable
private fun ProcessStep(
    card: WorryCard,
    nowMillis: Long,
    nextSessionLabel: String,
    audioPlaybackStatus: String?,
    audioPlaying: Boolean,
    onStopAudio: () -> Unit,
    onPlayAudio: (String) -> Unit,
    onAction: () -> Unit,
    onUnsolvable: () -> Unit,
    onDismiss: () -> Unit,
) {
    // 退场反馈：已不再重要=左滑划掉（✓ 标记收尾）；暂时无解=淡出。动画完成后才落库切页。
    var exit by remember { mutableStateOf<VaultExit?>(null) }
    val exitProgress = remember { Animatable(1f) }
    val markProgress = remember { Animatable(0f) }
    val markShown = remember { mutableStateOf(false) }

    fun beginExit(style: VaultExit) {
        if (exit == null) exit = style
    }

    // 连续模式下复用同一组合位：卡片切换时重置，下一张完整可见。
    LaunchedEffect(card.id) {
        exit = null
        exitProgress.snapTo(1f)
        markProgress.snapTo(0f)
        markShown.value = false
    }

    LaunchedEffect(exit) {
        when (exit) {
            VaultExit.Strike -> {
                exitProgress.animateTo(0f, tween(240, easing = FastOutSlowInEasing))
                markProgress.animateTo(1f, tween(160))
                markShown.value = true
                delay(520)
                onDismiss()
            }
            VaultExit.Fade -> {
                exitProgress.animateTo(0f, tween(220, easing = FastOutSlowInEasing))
                onUnsolvable()
            }
            null -> {}
        }
    }

    Text(vaultProcessPrompt, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = CardShape,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = exitProgress.value
                    when (exit) {
                        VaultExit.Strike -> translationX = -(1f - exitProgress.value) * 1100f
                        else -> {
                            val s = 0.92f + 0.08f * exitProgress.value
                            scaleX = s
                            scaleY = s
                        }
                    }
                },
        ) {
            Column(
                Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatusMark(ThoughtIcon, null)
                Text(
                    vaultQuotedCard(card.content),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    lineHeight = 30.sp,
                )
                Text(worryRecordedLabel(card.sealedAtMillis, nowMillis), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                card.audioFileName?.let { fileName ->
                    OutlinedButton(
                        onClick = { if (audioPlaying) onStopAudio() else onPlayAudio(fileName) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = VaultActionMinHeight),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(
                            if (audioPlaying) vaultStopAudioLabel else vaultPlayAudioLabel,
                            fontSize = 17.sp,
                        )
                    }
                    audioPlaybackStatus?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp) }
                }
            }
        }
        if (markShown.value) {
            // 划掉仪式收尾：✓ 圆标 + 文案短暂驻留。
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AnchorIconWell(DoneIcon, MaterialTheme.colorScheme.primary, size = 40.dp, tint = MaterialTheme.colorScheme.onPrimary)
                Text(vaultStruckLabel, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
    if (exit == null) {
        // 明天能做的一个动作 = 前进到转化页（不是完成，无退场动画）。
        Button(
            onClick = onAction,
            modifier = Modifier.fillMaxWidth().heightIn(min = VaultActionMinHeight),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text(vaultChooseActionLabel, fontSize = 17.sp)
        }
        OutlinedButton(
            onClick = { beginExit(VaultExit.Fade) },
            modifier = Modifier.fillMaxWidth().heightIn(min = VaultActionMinHeight),
            shape = RoundedCornerShape(12.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(vaultUnsolvableLabel, fontSize = 17.sp)
                Text(
                    vaultUnsolvableHint(nextSessionLabel),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                )
            }
        }
        OutlinedButton(
            onClick = { beginExit(VaultExit.Strike) },
            modifier = Modifier.fillMaxWidth().heightIn(min = VaultActionMinHeight),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text(vaultDismissLabel, fontSize = 17.sp)
        }
    }
}

@Composable
private fun ConvertStep(
    card: WorryCard,
    action: String,
    nowMillis: Long,
    onActionChange: (String) -> Unit,
    onConfirm: () -> Unit,
) {
    var exiting by remember { mutableStateOf(false) }
    val exitAlpha = remember { Animatable(1f) }
    val exitScale = remember { Animatable(1f) }
    LaunchedEffect(exiting) {
        if (exiting) {
            exitAlpha.animateTo(0f, tween(220, easing = FastOutSlowInEasing))
            onConfirm()
        }
    }
    Text(vaultConvertTitle, Modifier.semantics { heading() }, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = CardShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        modifier = Modifier.graphicsLayer {
            alpha = exitAlpha.value
            val s = exitScale.value
            scaleX = s
            scaleY = s
        },
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                vaultQuotedCard(card.content),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Text(worryRecordedLabel(card.sealedAtMillis, nowMillis), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        }
    }
    OutlinedTextField(
        value = action,
        onValueChange = onActionChange,
        label = { Text(vaultConvertFieldLabel) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
    )
    // 预设快选（复用微行动库）：点击填入输入框，仍可自由编辑个性化动作。
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        microActionPresets.map { it.title }.forEach { preset ->
            Surface(
                onClick = { onActionChange(preset) },
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Text(
                    preset,
                    Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    maxLines = 1,
                )
            }
        }
    }
    Button(
        onClick = {
            if (!exiting) {
                exiting = true
            }
        },
        enabled = action.isNotBlank() && !exiting,
        modifier = Modifier.fillMaxWidth().heightIn(min = VaultActionMinHeight),
        shape = RoundedCornerShape(12.dp),
    ) { Text(vaultConvertConfirmLabel, fontSize = 17.sp) }
}

@Composable
private fun DoneStep(processedCount: Int, onClose: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(top = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        StatusMark(DoneIcon, null)
        Text(vaultDoneTitle, Modifier.semantics { heading() }, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        Text(vaultDoneBody, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, fontSize = 16.sp, lineHeight = 26.sp)
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = CardShape,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        ) {
            Text(vaultDoneSummary(processedCount), Modifier.padding(16.dp), fontSize = 16.sp)
        }
        Button(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth().heightIn(min = VaultActionMinHeight),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text(vaultDoneHomeLabel, fontSize = 17.sp)
        }
        Text(vaultDoneFooter, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
    }
}

@Composable
private fun StatusMark(icon: ImageVector, caption: String?) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // primary/onPrimary 配对（与主按钮一致），primaryContainer 在浅色下底和图标同为深绿系，对比不够。
        AnchorIconWell(
            icon,
            MaterialTheme.colorScheme.primary,
            size = 64.dp,
            tint = MaterialTheme.colorScheme.onPrimary,
        )
        if (caption != null) {
            Text(caption, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
