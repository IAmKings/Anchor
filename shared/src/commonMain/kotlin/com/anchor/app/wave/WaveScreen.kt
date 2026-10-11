package com.anchor.app.wave

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.translate
import kotlin.math.PI
import kotlin.math.sin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anchor.app.ui.AnchorBackBar
import com.anchor.app.ui.formatCountdown
private enum class WaveStep { Recognize, Locate, Wait }

private val WaveActionMinHeight = 52.dp
private val WaveLocationMinHeight = 96.dp
internal val WaveDisc = Color(0xFFD7E6DC)
internal val WaveSage = Color(0xFF7A9B86)
internal val WaveSageDeep = Color(0xFF5A7B66)
private val WaveBreathClearance = 32.dp

@Composable
fun WaveWaitingScreen(
    nowMillis: () -> Long,
    initialRemainingMillis: Long,
    onSchedule: (Long) -> Unit,
    onClose: () -> Unit,
    onNoteEmotion: () -> Unit = {},
    parallelNote: String? = null,
) {
    var step by remember { mutableStateOf(if (initialRemainingMillis > 0) WaveStep.Wait else WaveStep.Recognize) }
    var bodyLocation by remember { mutableStateOf<String?>(null) }
    var customLocation by remember { mutableStateOf("") }
    var sessionTotalMillis by remember {
        mutableStateOf(if (initialRemainingMillis > 0) initialRemainingMillis else WAVE_TEN_MINUTES)
    }
    var deadlineMillis by remember { mutableStateOf(nowMillis() + initialRemainingMillis) }
    var remainingMillis by remember { mutableStateOf(initialRemainingMillis) }

    LaunchedEffect(step, deadlineMillis) {
        if (step == WaveStep.Wait) {
            while (remainingMillis > 0) {
                withFrameMillis {
                    remainingMillis = (deadlineMillis - nowMillis()).coerceAtLeast(0)
                }
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        AnchorBackBar(onBack = onClose, label = waveLeave()) {
            if (step == WaveStep.Wait && parallelNote != null) {
                Text(
                    parallelNote,
                    Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                )
            }
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                when (step) {
                    WaveStep.Recognize -> RecognizeStep(onSeen = { step = WaveStep.Locate })
                    WaveStep.Locate -> LocateStep(
                        selected = bodyLocation,
                        custom = customLocation,
                        onSelect = { bodyLocation = it; if (it != customLocation.trim()) customLocation = "" },
                        onCustom = {
                            customLocation = it
                            if (it.isNotBlank()) bodyLocation = it.trim()
                        },
                        onContinue = {
                            remainingMillis = WAVE_TEN_MINUTES
                            sessionTotalMillis = WAVE_TEN_MINUTES
                            deadlineMillis = nowMillis() + WAVE_TEN_MINUTES
                            onSchedule(WAVE_TEN_MINUTES)
                            step = WaveStep.Wait
                        },
                    )
                    WaveStep.Wait -> WaitStep(
                        remainingMillis = remainingMillis,
                        sessionTotalMillis = sessionTotalMillis,
                        location = bodyLocation,
                        onAddTen = {
                            val extension = remainingMillis + WAVE_TEN_MINUTES
                            remainingMillis = extension
                            sessionTotalMillis = extension
                            deadlineMillis = nowMillis() + extension
                            onSchedule(extension)
                        },
                        onNoteEmotion = onNoteEmotion,
                        onClose = onClose,
                    )
                }
            }
        }
    }
}

@Composable
private fun RecognizeStep(onSeen: () -> Unit) {
    BoxWithConstraints(
        Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        val diameter = minOf(
            maxWidth,
            (maxHeight - WaveBreathClearance - WaveActionMinHeight).coerceAtLeast(0.dp),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(diameter), contentAlignment = Alignment.Center) {
                // 承认/定位阶段：平静盘，深色步骤文案保持可读；浪在开始等待时才涌起。
                RollingWaves(Modifier.fillMaxSize(), rolling = false)
                val copyWidth = waveRecognizeCopyWidth(diameter.value).dp
                Column(
                    Modifier
                        .width(copyWidth)
                        .heightIn(max = copyWidth)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        waveRecognizeTitle(),
                        Modifier.semantics { heading() },
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        lineHeight = 32.sp,
                    )
                    Text(
                        waveRecognizeBody(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 17.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 28.sp,
                    )
                }
            }
            Spacer(Modifier.height(WaveBreathClearance))
            Button(
                onClick = onSeen,
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .heightIn(min = WaveActionMinHeight),
            ) {
                Text(waveRecognizeAction(), Modifier.padding(horizontal = 16.dp), fontSize = 17.sp)
            }
        }
    }
}

@Composable
private fun LocateStep(
    selected: String?,
    custom: String,
    onSelect: (String) -> Unit,
    onCustom: (String) -> Unit,
    onContinue: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            waveLocateTitle(),
            Modifier.semantics { heading() },
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Text(
            waveLocateBody(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 17.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(0.85f),
            lineHeight = 28.sp,
        )
        waveBodyLocations.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { location ->
                    val isSelected = selected == location.fullLabel
                    Surface(
                        onClick = { onSelect(location.fullLabel) },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = WaveLocationMinHeight)
                            .semantics { contentDescription = location.fullLabel },
                    ) {
                        Box(Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.Center) {
                            Text(
                                location.shortLabel,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                fontSize = 16.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            )
                        }
                    }
                }
            }
        }
        OutlinedTextField(
            value = custom,
            onValueChange = onCustom,
            label = { Text("其他身体感受") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
        )
        Button(
            onClick = onContinue,
            enabled = selected != null,
            shape = RoundedCornerShape(999.dp),
            modifier = Modifier
                .padding(top = 8.dp)
                .heightIn(min = WaveActionMinHeight),
        ) {
            Text(waveLocateContinue(), Modifier.padding(horizontal = 20.dp), fontSize = 17.sp)
        }
    }
}

@Composable
private fun WaitStep(
    remainingMillis: Long,
    sessionTotalMillis: Long,
    location: String?,
    onAddTen: () -> Unit,
    onNoteEmotion: () -> Unit,
    onClose: () -> Unit,
) {
    val done = remainingMillis == 0L
    BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 8.dp)) {
        val controls = if (done) {
            WaveActionMinHeight * 3 + 24.dp + 16.dp * 2
        } else {
            WaveActionMinHeight + 24.dp
        }
        val textReserve = 80.dp + 72.dp
        val viewport = maxHeight
        val diameter = minOf(
            maxWidth,
            (viewport - WaveBreathClearance * 2 - controls - textReserve).coerceAtLeast(0.dp),
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(viewport)
                .verticalScroll(rememberScrollState()),
        ) {
            Column(
                Modifier.fillMaxWidth().heightIn(min = viewport),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    waveWaitTitle(done),
                    Modifier.semantics { heading() },
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    lineHeight = 32.sp,
                )
                Spacer(Modifier.height(WaveBreathClearance))
                Box(Modifier.size(diameter), contentAlignment = Alignment.Center) {
                    RollingWaves(Modifier.fillMaxSize())
                    CountdownRing(
                        remainingMillis = remainingMillis,
                        sessionTotalMillis = sessionTotalMillis,
                        done = done,
                        diameter = diameter,
                    )
                }
                Spacer(Modifier.height(WaveBreathClearance))
                Text(
                    waveWaitBody(if (done) null else location),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 17.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(0.8f),
                    lineHeight = 28.sp,
                )
                Spacer(Modifier.height(24.dp))
                OutlinedButton(
                    onClick = onAddTen,
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier.heightIn(min = WaveActionMinHeight),
                ) {
                    Text(waveAddTenMinutes(), fontSize = 17.sp)
                }
                if (done) {
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = onNoteEmotion,
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier.heightIn(min = WaveActionMinHeight),
                    ) {
                        Text(waveNoteEmotion(), Modifier.padding(horizontal = 12.dp), fontSize = 17.sp)
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = onClose,
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier.heightIn(min = WaveActionMinHeight),
                    ) {
                        Text(waveBackToToday(), Modifier.padding(horizontal = 12.dp), fontSize = 17.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun CountdownRing(
    remainingMillis: Long,
    sessionTotalMillis: Long,
    done: Boolean,
    diameter: Dp,
) {
    val progress = if (sessionTotalMillis <= 0L) 0f else (remainingMillis.toFloat() / sessionTotalMillis).coerceIn(0f, 1f)
    val track = MaterialTheme.colorScheme.surfaceVariant
    val active = MaterialTheme.colorScheme.primary
    val ring = minOf(256.dp, diameter)
    Box(Modifier.size(ring), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 4.dp.toPx()
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
            if (done) "00:00" else formatCountdown(remainingMillis),
            fontFamily = FontFamily.Monospace,
            fontSize = 40.sp,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun RollingWaves(modifier: Modifier = Modifier, rolling: Boolean = true) {
    // 涌浪圆盘：3 层有机浪（双谐波叠加，非正圆）横向翻滚（8s 内分别平移 1/2/3 个波长——
    // 整波长平移保证 Restart 循环接缝无缝）+ 涌落沿用既有呼吸节奏（waveBreathUnit/Scale，测试覆盖的语义）。
    // 层内纵向渐变为 DESIGN.md 特许的浪潮用渐变。波形参数为初值，装机截屏后可校准。
    // rolling = false 为平静盘（承认/定位阶段）：浪未涌起，仅一层低透明度底浪，深色步骤文案保持可读。
    val transition = rememberInfiniteTransition(label = "wave-roll")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "wave-roll-t",
    )
    Canvas(modifier) {
        val unit = size.minDimension / 200f
        scale(unit, unit, pivot = Offset.Zero) {
            val disc = Path().apply { addOval(Rect(center = Offset(100f, 100f), radius = 90f)) }
            clipPath(disc) {
                drawCircle(WaveDisc, radius = 90f, center = Offset(100f, 100f))
                // 涌落：呼吸缩放的「未收缩余量」作为整组浪层的缓升位移，随 8s 周期过零衔接。
                val swell = (1f - waveBreathScale(waveBreathUnit(t, 0f))) * 12f
                translate(top = swell) {
                    if (rolling) {
                        rollingWavePath(wavelength = 130f, loops = 1, base = 108f, amp1 = 12f, amp2 = 6f, phase = 0f)
                            .let { drawPath(it, WaveSage.copy(alpha = 0.35f)) }
                        rollingWavePath(wavelength = 130f, loops = 2, base = 126f, amp1 = 14f, amp2 = 7f, phase = 2.1f)
                            .let { drawPath(it, WaveSage.copy(alpha = 0.6f)) }
                        rollingWavePath(wavelength = 130f, loops = 3, base = 144f, amp1 = 16f, amp2 = 8f, phase = 4.2f)
                            .let { drawPath(it, Brush.verticalGradient(0f to WaveSage.copy(alpha = 0.55f), 1f to WaveSageDeep.copy(alpha = 0.9f))) }
                    } else {
                        rollingWavePath(wavelength = 130f, loops = 1, base = 158f, amp1 = 10f, amp2 = 5f, phase = 0f)
                            .let { drawPath(it, WaveSage.copy(alpha = 0.28f)) }
                    }
                }
            }
        }
    }
}

/**
 * 单层浪的静态路径（一次构建，逐帧仅平移）：y = base − (a1·sin(2πx/λ) + a2·sin(4πx/λ + φ))。
 * 两条谐波周期均为 λ 或 λ/2 → 整条路径以 λ 为周期，平移 loops·λ 后与原图重合（Restart 无缝）。
 * 平移方向向左（浪向左涌）；路径横向跨度覆盖 [−loops·λ−10, 210+10]，保证最大平移下盘内不露边。
 */
private fun rollingWavePath(
    wavelength: Float,
    loops: Int,
    base: Float,
    amp1: Float,
    amp2: Float,
    phase: Float,
): Path = Path().apply {
    val maxShift = loops * wavelength
    fun yAt(x: Float): Float = base -
        (amp1 * sin(x * 2f * PI / wavelength + phase).toFloat() +
            amp2 * sin(x * 4f * PI / wavelength + phase * 1.7f).toFloat())
    var x = -maxShift - 10f
    moveTo(x, yAt(x))
    while (x < 210f + 10f) {
        x += 6f
        lineTo(x, yAt(x))
    }
    lineTo(210f + 10f, 210f)
    lineTo(-maxShift - 10f, 210f)
    close()
}
