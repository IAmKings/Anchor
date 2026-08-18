package com.anchor.app.worry

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
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anchor.app.storage.AnchorStore
import com.anchor.app.storage.WorryCard
import com.anchor.app.storage.WorryResolution

private val CardShape = RoundedCornerShape(16.dp)

private enum class VaultStep { Overview, Process, Convert, Done }

@Composable
fun WorryVaultScreen(
    store: AnchorStore,
    nowMillis: () -> Long,
    isSessionOpen: () -> Boolean,
    nextSessionMillis: () -> Long,
    nextSessionLabel: () -> String,
    speechStatus: String?,
    speechRecording: Boolean,
    onCaptureSpeech: ((String) -> Unit) -> Unit,
    onStopRecording: () -> String?,
    audioPlaybackStatus: String?,
    onPlayAudio: (String) -> Unit,
    onClose: () -> Unit,
) {
    var content by remember { mutableStateOf("") }
    var confirmationVisible by remember { mutableStateOf(false) }
    var forcedOpen by remember { mutableStateOf(false) }
    var savedMessage by remember { mutableStateOf<String?>(null) }
    var action by remember { mutableStateOf("") }
    var processedCount by remember { mutableIntStateOf(0) }
    var step by remember { mutableStateOf(VaultStep.Overview) }
    var revision by remember { mutableIntStateOf(0) }
    val cards = remember(revision) { store.worryCards() }
    val pending = cards.filter { it.resolution == WorryResolution.Pending }
    val open = isSessionOpen() || forcedOpen
    val current = pending.firstOrNull()

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
                    step = if (pending.size <= 1) VaultStep.Done else VaultStep.Process
                },
                onBack = { step = VaultStep.Process },
            )
            step == VaultStep.Process && current != null && open -> ProcessStep(
                card = current,
                nowMillis = nowMillis(),
                audioPlaybackStatus = audioPlaybackStatus,
                onPlayAudio = onPlayAudio,
                onAction = { step = VaultStep.Convert },
                onUnsolvable = {
                    store.resolveWorryCard(current.id, WorryResolution.Unsolvable)
                    processedCount++
                    revision++
                    step = if (pending.size <= 1) VaultStep.Done else VaultStep.Process
                },
                onDismiss = {
                    store.dismissWorryCard(current.id)
                    processedCount++
                    revision++
                    step = if (pending.size <= 1) VaultStep.Done else VaultStep.Process
                },
                onBack = { step = VaultStep.Overview },
            )
            else -> OverviewStep(
                open = open,
                pending = pending,
                content = content,
                onContent = { content = it },
                savedMessage = savedMessage,
                speechStatus = speechStatus,
                speechRecording = speechRecording,
                confirmationVisible = confirmationVisible,
                nextSessionLabel = nextSessionLabel(),
                nowMillis = nowMillis(),
                onHang = {
                    store.addWorryCard(content, nowMillis(), nextSessionMillis())
                    content = ""
                    savedMessage = vaultSealedMessage(open, nextSessionLabel())
                    revision++
                },
                onSpeech = {
                    if (speechRecording) {
                        onStopRecording()?.let { audioFileName ->
                            store.addWorryCard("", nowMillis(), nextSessionMillis(), audioFileName)
                            savedMessage = vaultSealedMessage(open, nextSessionLabel())
                            revision++
                        }
                    } else {
                        onCaptureSpeech { content = it }
                    }
                },
                onAskOpenNow = { confirmationVisible = true },
                onConfirmOpen = { forcedOpen = true; confirmationVisible = false },
                onWaitForSession = { confirmationVisible = false },
                onStartProcess = { step = VaultStep.Process },
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun OverviewStep(
    open: Boolean,
    pending: List<WorryCard>,
    content: String,
    onContent: (String) -> Unit,
    savedMessage: String?,
    speechStatus: String?,
    speechRecording: Boolean,
    confirmationVisible: Boolean,
    nextSessionLabel: String,
    nowMillis: Long,
    onHang: () -> Unit,
    onSpeech: () -> Unit,
    onAskOpenNow: () -> Unit,
    onConfirmOpen: () -> Unit,
    onWaitForSession: () -> Unit,
    onStartProcess: () -> Unit,
) {
    HangComposer(
        title = "忧虑保险箱",
        fieldLabel = hangFieldHint,
        content = content,
        onContent = onContent,
        speechRecording = speechRecording,
        speechStatus = speechStatus,
        savedMessage = savedMessage,
        onSpeech = onSpeech,
        onHang = onHang,
    )
    if (!open) {
        StatusMark("锁", "念头已经在纸上了")
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = CardShape,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(vaultRuminationMessage(nextSessionLabel), fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 26.sp)
                Text(vaultPendingSummary(pending.size), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp, lineHeight = 24.sp)
                TextButton(onClick = onAskOpenNow) { Text("现在就想处理") }
            }
        }
        if (confirmationVisible) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                shape = CardShape,
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("现在开箱后，请尽量为每张卡做一个选择，优先找“明天能做的一个动作”。", fontSize = 15.sp, lineHeight = 24.sp)
                    Button(onClick = onConfirmOpen, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Text("确认开箱")
                    }
                    TextButton(onClick = onWaitForSession) { Text("等到专场") }
                }
            }
        }
    } else {
        StatusMark("开", "专场已开启")
        Text("深呼吸，我们开始整理。", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        if (pending.isEmpty()) {
            Text("今天没有待处理卡片。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            pending.forEach { card -> PreviewCard(card, nowMillis) }
            Text(
                "在这里，忧虑是被受理的，而非被压抑。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(onClick = onStartProcess, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Text("开始处理第一项")
            }
        }
    }
}



@Composable
private fun PreviewCard(card: WorryCard, nowMillis: Long) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = CardShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(worryRecordedLabel(card.sealedAtMillis, nowMillis), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            Text(
                if (card.content.isBlank()) "语音挂卡" else card.content,
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
    audioPlaybackStatus: String?,
    onPlayAudio: (String) -> Unit,
    onAction: () -> Unit,
    onUnsolvable: () -> Unit,
    onDismiss: () -> Unit,
    onBack: () -> Unit,
) {
    TextButton(onClick = onBack) { Text("回到开箱") }
    Text("请选择处理方式", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = CardShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatusMark("念", null)
            Text(
                if (card.content.isBlank()) "“语音挂卡”" else "“${card.content}”",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                lineHeight = 30.sp,
            )
            Text(worryRecordedLabel(card.sealedAtMillis, nowMillis), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            card.audioFileName?.let { fileName ->
                OutlinedButton(onClick = { onPlayAudio(fileName) }) { Text("播放本地录音") }
                audioPlaybackStatus?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp) }
            }
        }
    }
    Button(onClick = onAction, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Text("明天能做的一个动作")
    }
    OutlinedButton(onClick = onUnsolvable, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Text("暂时无解")
    }
    TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("已不再重要") }
}

@Composable
private fun ConvertStep(
    card: WorryCard,
    action: String,
    nowMillis: Long,
    onActionChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
) {
    TextButton(onClick = onBack) { Text("回到三选一") }
    Text("转化动作", Modifier.semantics { heading() }, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = CardShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                if (card.content.isBlank()) "语音挂卡" else "“${card.content}”",
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
        label = { Text("明天要做的微行动") },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
    )
    Button(
        onClick = onConfirm,
        enabled = action.isNotBlank(),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
    ) { Text("确认并同步到首页") }
}

@Composable
private fun DoneStep(processedCount: Int, onClose: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(top = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        StatusMark("完", null)
        Text("整理完毕", Modifier.semantics { heading() }, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        Text("这一刻，念头已在纸上。\n你可以放心地把它们留在这里。", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, fontSize = 16.sp, lineHeight = 26.sp)
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = CardShape,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        ) {
            Text(vaultDoneSummary(processedCount), Modifier.padding(16.dp), fontSize = 16.sp)
        }
        Button(onClick = onClose, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Text("回到今天")
        }
        Text("受理，而非压抑。", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
    }
}

@Composable
private fun StatusMark(glyph: String, caption: String?) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape, modifier = Modifier.size(64.dp)) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(glyph, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
            }
        }
        if (caption != null) {
            Text(caption, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
