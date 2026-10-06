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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anchor.app.safety.CrisisClarificationDialog
import com.anchor.app.safety.findCrisisPhrase
import com.anchor.app.speech.SpeechFinal
import com.anchor.app.storage.AnchorStore
import kotlin.math.roundToInt
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
    onCaptureSpeech: () -> Unit,
    onFinalizeSpeech: () -> Unit,
    onDiscardSpeech: (String?) -> Unit,
    onConsumeSpeechResult: () -> Unit,
    onCrisisGuidance: () -> Unit = {},
    onClose: () -> Unit,
) {
    var content by remember { mutableStateOf("") }
    var savedMessage by remember { mutableStateOf<String?>(null) }
    var pendingPhrase by remember { mutableStateOf<String?>(null) }
    var pendingAudio by remember { mutableStateOf<String?>(null) }

    // 转写结果一次性消费：到达即清空宿主状态（否则每次进入组合都会重新封存同一张录音卡）。
    // 有文字先填进输入框让用户过目（录音文件随卡片一起封存）；
    // 纯录音兜底没有文字可看，维持"停止并保存录音"的直接封存语义。
    LaunchedEffect(speechResult) {
        speechResult?.let { final ->
            onConsumeSpeechResult()
            if (final.text.isNullOrBlank()) {
                final.audioFileName?.let { fileName ->
                    store.addWorryCard("", nowMillis(), nextSessionMillis(), fileName)
                    savedMessage = vaultSealedMessage(isSessionOpen(), nextSessionLabel())
                }
            } else {
                content = final.text
                pendingAudio = final.audioFileName
            }
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
                    content = if (speechRecording) (speechPartial ?: "") else content,
                    onContent = { if (!speechRecording) content = it },
                    speechRecording = speechRecording,
                    speechTranscribes = speechTranscribes,
                    sealEnabled = content.isNotBlank() || pendingAudio != null,
                    speechStatus = speechStatus,
                    savedMessage = savedMessage,
                    onSpeech = {
                        if (speechRecording) onFinalizeSpeech() else onCaptureSpeech()
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
    sealEnabled: Boolean,
    speechStatus: String?,
    savedMessage: String?,
    onSpeech: () -> Unit,
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
        OutlinedTextField(
            value = content,
            onValueChange = onContent,
            label = { Text(fieldLabel) },
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "挂卡输入" },
            shape = FieldShape,
            minLines = 2,
            maxLines = 4,
        )
        OutlinedButton(
            onClick = onSpeech,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            shape = FieldShape,
        ) {
            Text(
                when {
                    speechRecording && speechTranscribes -> hangSpeechStopTranscribeLabel
                    speechRecording -> hangSpeechStopLabel
                    else -> hangSpeechLabel
                },
                fontSize = 17.sp,
            )
        }
        speechStatus?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp) }
        Button(
            onClick = onHang,
            enabled = sealEnabled,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            shape = FieldShape,
        ) { Text(hangSealLabel, fontSize = 17.sp) }
        savedMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 22.sp)
        }
        if (onDismiss != null) {
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(if (showDismiss) hangDismissKnown else hangDismissCancel)
            }
        }
    }
}
