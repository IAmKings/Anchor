package com.anchor.app.worry

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 松手结算：滑到左侧取消；滑到右侧仅录音；原地松手转文字（默认）。 */
enum class VoiceRelease { Cancel, Settle, VoiceOnly }

private val HoldShape = RoundedCornerShape(999.dp)
/** 手指进入按钮左/右三分之一即视为滑入对应区域。 */
private const val ZONE_DIVISOR = 3f
/** 轻点判定毫秒数：短于此且未出区域 = 无障碍切换（开始后保持录音）。 */
private const val TAP_TOGGLE_MILLIS = 280L
/** 音波柱数量：多而细，避免粗柱观感。 */
private const val WAVE_BAR_COUNT = 48

/**
 * 微信式按住说话按钮：按下即开始采集，按住期间显示实时音波，左右浮出
 * 「取消 / 转文字」药丸，滑入区域松手结算，原地松手为纯录音（由屏幕侧路由结算方式）。
 * 提示行常驻固定高度，按住/空闲不发生布局位移。
 * 无障碍：TalkBack 双击 = 开始/结束录音，自定义动作「转文字」可直接出文字卡。
 */
@Composable
fun VoiceMemoButton(
    speechRecording: Boolean,
    speechTranscribes: Boolean,
    amplitude: Float,
    secondsLeft: Int?,
    statusText: String?,
    onCapture: () -> Unit,
    onRelease: (VoiceRelease) -> Unit,
    modifier: Modifier = Modifier,
) {
    var holdActive by remember { mutableStateOf(false) }
    var fingerX by remember { mutableFloatStateOf(0f) }
    var buttonWidth by remember { mutableFloatStateOf(1f) }
    val bars = remember { mutableStateListOf<Float>() }
    // 手势监听不能用 speechRecording 当 key：按下后状态翻转会让协程重启、丢失抬起事件，
    // 导致永远收不了尾。改用 Unit key + rememberUpdatedState 读实时值。
    val currentRecording by rememberUpdatedState(speechRecording)
    val currentTranscribes by rememberUpdatedState(speechTranscribes)
    val currentOnCapture by rememberUpdatedState(onCapture)
    val currentOnRelease by rememberUpdatedState(onRelease)

    LaunchedEffect(amplitude, speechRecording) {
        if (!speechRecording) {
            bars.clear()
        } else {
            bars.add(amplitude)
            while (bars.size > WAVE_BAR_COUNT) bars.removeAt(0)
        }
    }

    val cancelZone = holdActive && fingerX < buttonWidth / ZONE_DIVISOR
    val textZone = holdActive && fingerX > buttonWidth * (ZONE_DIVISOR - 1) / ZONE_DIVISOR

    Column(modifier.fillMaxWidth()) {
        // 提示行常驻（固定高度）：按住时显示「取消 / 倒计时 / 转文字」，空闲时留白——
        // 避免按住瞬间插入一行造成按钮位移，也不预告未发生的行为。
        Row(
            Modifier.fillMaxWidth().height(44.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (holdActive) {
                SidePill(hangVoiceCancelLabel, cancelZone, Modifier.weight(1f))
                Text(
                    // 录音中倒计时优先（用户反馈：10 分钟版倒计时被状态文字顶掉了）；非录音显示状态。
                    if (holdActive || (secondsLeft ?: 0) > 0) countdownText(secondsLeft)
                    else statusText ?: countdownText(secondsLeft),
                    Modifier.weight(1.6f),
                    textAlign = TextAlign.Center,
                    color = if ((secondsLeft ?: 601) in 1..10) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )
                SidePill(hangVoiceTextLabel, textZone, Modifier.weight(1f))
            } else if (statusText != null) {
                Text(
                    statusText,
                    Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                )
            }
        }
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        down.consume()
                        val wasRecording = currentRecording
                        val downAt = down.uptimeMillis
                        holdActive = true
                        fingerX = down.position.x
                        var upAt = downAt
                        if (!wasRecording) currentOnCapture()
                        var released = false
                        while (!released) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: continue
                            fingerX = change.position.x
                            buttonWidth = size.width.toFloat()
                            event.changes.forEach { it.consume() }
                            if (!change.pressed) {
                                released = true
                                upAt = change.uptimeMillis
                            }
                        }
                        holdActive = false
                        val release = when {
                            fingerX < buttonWidth / ZONE_DIVISOR -> VoiceRelease.Cancel
                            fingerX > buttonWidth * (ZONE_DIVISOR - 1) / ZONE_DIVISOR -> VoiceRelease.VoiceOnly
                            else -> VoiceRelease.Settle
                        }
                        // 误触轻点（空闲态快速起落、未滑出中间区）：保持录音，不结算（无障碍切换态）。
                        if (release == VoiceRelease.Settle && !wasRecording &&
                            upAt - downAt < TAP_TOGGLE_MILLIS
                        ) {
                            return@awaitEachGesture
                        }
                        currentOnRelease(release)
                    }
                }
                .semantics {
                    contentDescription = when {
                        currentRecording && currentTranscribes -> hangSpeechStopTranscribeLabel
                        currentRecording -> hangSpeechStopLabel
                        else -> hangVoiceHoldLabel
                    }
                    onClick {
                        if (!currentRecording) currentOnCapture() else currentOnRelease(VoiceRelease.Settle)
                        true
                    }
                    customActions = listOf(
                        CustomAccessibilityAction(hangVoiceTextLabel) {
                            if (currentRecording) currentOnRelease(VoiceRelease.VoiceOnly)
                            true
                        },
                    )
                },
            color = MaterialTheme.colorScheme.primary,
            shape = HoldShape,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            if (currentRecording) {
                Row(
                    Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Waveform(bars, Modifier.weight(1f).height(34.dp))
                }
            } else {
                Box(
                    Modifier.fillMaxWidth().height(56.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        hangVoiceHoldLabel,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 17.sp,
                    )
                }
            }
        }
    }
}

/** 剩余时间的自然中文显示：>60 秒用分，整分钟不带零秒。 */
internal fun countdownText(secondsLeft: Int?): String {
    if (secondsLeft == null || secondsLeft <= 0) return hangVoiceReleaseHint
    return if (secondsLeft >= 60) {
        val minutes = secondsLeft / 60
        val seconds = secondsLeft % 60
        if (seconds == 0) "还能说 $minutes 分钟" else "还能说 $minutes 分 $seconds 秒"
    } else {
        "还能说 $secondsLeft 秒"
    }
}

@Composable
private fun SidePill(label: String, highlighted: Boolean, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.heightIn(min = 44.dp),
        shape = RoundedCornerShape(999.dp),
        color = if (highlighted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(
            label,
            Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            textAlign = TextAlign.Center,
            color = if (highlighted) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 15.sp,
        )
    }
}

@Composable
private fun Waveform(bars: List<Float>, modifier: Modifier = Modifier) {
    val barColor = MaterialTheme.colorScheme.onPrimary
    Canvas(modifier) {
        val gap = 2.5.dp.toPx()
        val barWidth = (size.width - gap * (WAVE_BAR_COUNT - 1)) / WAVE_BAR_COUNT
        val midY = size.height / 2
        for (i in 0 until WAVE_BAR_COUNT) {
            // 右侧是最新振幅；空位画最小高度。
            val source = bars.getOrNull(bars.size - WAVE_BAR_COUNT + i)
            val level = (source ?: 0.08f).coerceIn(0.08f, 1f)
            val barHeight = size.height * (0.18f + 0.82f * level)
            drawRoundRect(
                color = barColor,
                topLeft = Offset(i * (barWidth + gap), midY - barHeight / 2),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2),
            )
        }
    }
}
