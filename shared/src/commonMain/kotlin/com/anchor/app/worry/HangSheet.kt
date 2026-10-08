package com.anchor.app.worry

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anchor.app.safety.CrisisClarificationDialog
import com.anchor.app.safety.findCrisisPhrase
import com.anchor.app.speech.SpeechFinal
import com.anchor.app.speech.VOICE_HOLD_LIMIT_SECONDS
import com.anchor.app.storage.AnchorStore
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val SheetShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
private val FieldShape = RoundedCornerShape(12.dp)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun HangSheet(
    store: AnchorStore,
    nowMillis: () -> Long,
    isSessionOpen: () -> Boolean,
    nextSessionMillis: () -> Long,
    nextSessionLabel: () -> String,
    speechStatus: String?,
    speechRecording: Boolean,
    speechPartial: String?,
    speechResult: SpeechFinal?,
    speechTranscribes: Boolean,
    speechAmplitude: Float,
    audioPlaybackStatus: String?,
    audioPlaying: Boolean,
    onStopPendingAudioPlayback: () -> Unit,
    onPlayWorryAudio: (String) -> Unit,
    onCaptureSpeech: () -> Unit,
    onFinalizeSpeech: () -> Unit,
    onDiscardSpeech: (String?) -> Unit,
    onConsumeSpeechResult: () -> Unit,
    onTranscriptEdited: () -> Unit,
    onCrisisGuidance: () -> Unit = {},
    onClose: () -> Unit,
) {
    var content by remember { mutableStateOf("") }
    var savedMessage by remember { mutableStateOf<String?>(null) }
    var pendingPhrase by remember { mutableStateOf<String?>(null) }
    var pendingAudio by remember { mutableStateOf<String?>(null) }
    var voiceOnlyPending by remember { mutableStateOf(false) }
    var awaitingTranscriptEdit by remember { mutableStateOf(false) }
    var secondsLeft by remember { mutableStateOf<Int?>(null) }

    // 转写结果一次性消费：到达即清空宿主状态（否则每次进入组合都会重新封存同一张录音卡）。
    // 默认（原地松手/60 秒到限）：文字（自动标点）+ 录音回填待封存；
    // 「仅录音」（滑右）：忽略转写文字，只封录音卡；转写为空时自动封纯录音卡（兜底）。
    LaunchedEffect(speechResult) {
        speechResult?.let { final ->
            onConsumeSpeechResult()
            if (voiceOnlyPending) {
                voiceOnlyPending = false
                content = ""
                final.audioFileName?.let { fileName ->
                    store.addWorryCard("", nowMillis(), nextSessionMillis(), fileName)
                    savedMessage = vaultSealedMessage(isSessionOpen(), nextSessionLabel())
                }
            } else {
                content = final.text ?: ""
                pendingAudio = final.audioFileName
                awaitingTranscriptEdit = content.isNotBlank()
            }
        }
    }

    // 按住说话的 10 分钟倒计时：到限按松手默认（转文字）结算。
    LaunchedEffect(speechRecording) {
        if (speechRecording) {
            for (left in VOICE_HOLD_LIMIT_SECONDS downTo 1) {
                secondsLeft = left
                delay(1_000)
            }
            secondsLeft = 0
            onFinalizeSpeech()
        } else {
            secondsLeft = null
        }
    }

    fun close() {
        // 离开弹层 = 本次语音输入彻底结束：未封存的录音删除，宿主结果状态一并清空。
        if (speechRecording || pendingAudio != null) onDiscardSpeech(pendingAudio)
        onConsumeSpeechResult()
        pendingAudio = null
        onClose()
    }

    fun seal() {
        val text = content.trim()
        if (text.isBlank() && pendingAudio == null) return
        if (text.isNotBlank()) {
            val hit = findCrisisPhrase(text)
            if (hit != null) {
                pendingPhrase = hit
                return
            }
        }
        store.addWorryCard(text, nowMillis(), nextSessionMillis(), pendingAudio)
        content = ""
        pendingAudio = null
        onConsumeSpeechResult()
        savedMessage = vaultSealedMessage(isSessionOpen(), nextSessionLabel())
    }

    fun confirmSeal(goToCrisis: Boolean) {
        val text = content.trim()
        if (text.isNotBlank() || pendingAudio != null) {
            store.addWorryCard(text, nowMillis(), nextSessionMillis(), pendingAudio)
            content = ""
            pendingAudio = null
            onConsumeSpeechResult()
            savedMessage = vaultSealedMessage(isSessionOpen(), nextSessionLabel())
        }
        pendingPhrase = null
        if (goToCrisis) onCrisisGuidance()
    }

    BackHandler(onBack = ::close)

    // 下拉关闭：拖拽把手是视觉引导，这里补上真实手势。输入框文本溢出时的滚动
    // 由子组件先消费拖拽，父级监听自然让位。
    var dragY by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val dismissThreshold = with(density) { 96.dp.toPx() }
    val hardDismiss = with(density) { 200.dp.toPx() }

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                // 吞掉垂直拖拽：否则拖拽松手会被 clickable 当成点击而关闭弹层。
                .pointerInput(Unit) {
                    detectVerticalDragGestures { change, _ -> change.consume() }
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { close() },
                )
                .semantics { contentDescription = "关闭挂卡" },
        )
        // Surface 本体铺到屏幕底：ColorOS/Gboard 会在键盘 inset 里保留一段透明带，
        // 垫 inset 只会让弹层悬空露出遮罩。让表面穿过透明带（被键盘遮住的部分不可见），
        // 内容再用 inset 垫到键盘上沿。
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .offset { IntOffset(0, dragY.roundToInt()) }
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            dragY = (dragY + dragAmount).coerceAtLeast(0f)
                            if (dragY >= hardDismiss) close()
                        },
                        onDragEnd = {
                            if (dragY >= dismissThreshold) {
                                close()
                            } else {
                                scope.launch {
                                    animate(dragY, 0f, animationSpec = spring()) { value, _ ->
                                        dragY = value
                                    }
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch {
                                animate(dragY, 0f, animationSpec = spring()) { value, _ ->
                                    dragY = value
                                }
                            }
                        },
                    )
                },
            color = MaterialTheme.colorScheme.surface,
            shape = SheetShape,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)),
            ) {
                HangComposer(
                    title = hangSheetTitle,
                    fieldLabel = hangFieldHint,
                    // 录音中显示完整转写（引擎侧 1 秒节流控制排版开销），光标钉尾跟随最新语音。
                    content = if (speechRecording) (speechPartial ?: "") else content,
                    onContent = {
                        if (!speechRecording) {
                            if (awaitingTranscriptEdit) {
                                awaitingTranscriptEdit = false
                                onTranscriptEdited()
                            }
                            content = it
                        }
                    },
                    speechRecording = speechRecording,
                    speechTranscribes = speechTranscribes,
                    speechAmplitude = speechAmplitude,
                    secondsLeft = secondsLeft,
                    pendingAudio = pendingAudio,
                    audioPlaybackStatus = audioPlaybackStatus,
                    audioPlaying = audioPlaying,
                    onStopPendingAudioPlayback = onStopPendingAudioPlayback,
                    onPlayPendingAudio = onPlayWorryAudio,
                    sealEnabled = content.isNotBlank() || pendingAudio != null,
                    speechStatus = speechStatus,
                    savedMessage = savedMessage,
                    onCaptureSpeech = onCaptureSpeech,
                    onReleaseVoice = { release ->
                        when (release) {
                            VoiceRelease.Cancel -> {
                                if (speechRecording || pendingAudio != null) onDiscardSpeech(pendingAudio)
                                pendingAudio = null
                                onConsumeSpeechResult()
                            }
                            VoiceRelease.Settle -> {
                                // 到限自动结算后手指仍按着，松手会产生幽灵结算——只在录音中有效。
                                if (speechRecording) onFinalizeSpeech()
                            }
                            VoiceRelease.VoiceOnly -> {
                                if (speechRecording) {
                                    voiceOnlyPending = true
                                    onFinalizeSpeech()
                                }
                            }
                        }
                    },
                    onHang = { seal() },
                    onDismiss = { close() },
                    showDismiss = savedMessage != null,
                )
            }
        }
        pendingPhrase?.let { phrase ->
            CrisisClarificationDialog(
                phrase = phrase,
                onNotSelf = { confirmSeal(goToCrisis = false) },
                onSelf = { confirmSeal(goToCrisis = true) },
                onUncertain = { confirmSeal(goToCrisis = true) },
            )
        }
    }
}

@Composable
internal fun HangComposer(
    title: String,
    fieldLabel: String,
    content: String,
    onContent: (String) -> Unit,
    speechRecording: Boolean,
    speechTranscribes: Boolean,
    speechAmplitude: Float,
    secondsLeft: Int?,
    pendingAudio: String?,
    audioPlaybackStatus: String?,
    audioPlaying: Boolean,
    onStopPendingAudioPlayback: () -> Unit,
    onPlayPendingAudio: (String) -> Unit,
    sealEnabled: Boolean,
    speechStatus: String?,
    savedMessage: String?,
    onCaptureSpeech: () -> Unit,
    onReleaseVoice: (VoiceRelease) -> Unit,
    onHang: () -> Unit,
    onDismiss: (() -> Unit)? = null,
    showDismiss: Boolean = false,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 4.dp)
                .fillMaxWidth(0.12f)
                .height(4.dp)
                .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(999.dp)),
        )
        Text(title, Modifier.semantics { heading() }, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        // 录音中预览每 0.1 秒更新：光标钉在末尾让视口跟随最新语音；转写回填后光标同样落到末尾；
        // 用户手动编辑（非录音）时不干扰光标位置。
        var fieldValue by remember { mutableStateOf(TextFieldValue(content)) }
        LaunchedEffect(content, speechRecording) {
            if (speechRecording || fieldValue.text != content) {
                fieldValue = TextFieldValue(content, TextRange(content.length))
            }
        }
        OutlinedTextField(
            value = fieldValue,
            onValueChange = { newValue ->
                fieldValue = newValue
                onContent(newValue.text)
            },
            label = { Text(fieldLabel) },
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "挂卡输入" },
            shape = FieldShape,
            minLines = 2,
            maxLines = 4,
        )
        if (content.isNotEmpty()) {
            Text(
                "${content.length} 字",
                Modifier.fillMaxWidth(),
                textAlign = TextAlign.End,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
            )
        }
        VoiceMemoButton(
            speechRecording = speechRecording,
            speechTranscribes = speechTranscribes,
            amplitude = speechAmplitude,
            secondsLeft = secondsLeft,
            statusText = speechStatus,
            onCapture = onCaptureSpeech,
            onRelease = onReleaseVoice,
        )
        pendingAudio?.let { fileName ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = FieldShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        hangVoiceReadyLabel,
                        Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                    )
                    TextButton(onClick = {
                        if (audioPlaying) onStopPendingAudioPlayback() else onPlayPendingAudio(fileName)
                    }) {
                        Text(
                            if (audioPlaying) hangVoiceChipStopLabel else hangVoiceChipPlayLabel,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 15.sp,
                        )
                    }
                }
                audioPlaybackStatus?.let {
                    Text(
                        it,
                        Modifier.fillMaxWidth().padding(start = 16.dp, bottom = 8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                    )
                }
            }
        }
        Button(
            onClick = onHang,
            enabled = sealEnabled,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            shape = FieldShape,
        ) { Text(hangSealLabel, fontSize = 17.sp) }
        savedMessage?.let {
            Text(
                it,
                Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center,
            )
        }
        if (onDismiss != null) {
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(if (showDismiss) hangDismissKnown else hangDismissCancel)
            }
        }
    }
}
