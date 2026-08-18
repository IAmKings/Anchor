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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anchor.app.ui.formatCountdown
import kotlin.math.PI
import kotlin.math.sin

private enum class WaveStep { Recognize, Locate, Wait }

@Composable
fun WaveWaitingScreen(
    nowMillis: () -> Long,
    initialRemainingMillis: Long,
    onSchedule: (Long) -> Unit,
    onClose: () -> Unit,
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
        BreathingBackdrop()
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            TextButton(
                onClick = onClose,
                modifier = Modifier.align(Alignment.End).semantics { contentDescription = waveLeave() },
            ) {
                Text(waveLeave(), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontSize = 14.sp)
            }
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
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
                        onClose = onClose,
                    )
                }
            }
        }
    }
}

@Composable
private fun RecognizeStep(onSeen: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
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
            modifier = Modifier.fillMaxWidth(0.8f),
            lineHeight = 28.sp,
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onSeen,
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            modifier = Modifier.padding(horizontal = 12.dp),
        ) {
            Text(waveRecognizeAction(), Modifier.padding(horizontal = 16.dp, vertical = 6.dp), fontSize = 14.sp)
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
            .padding(bottom = 24.dp),
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
                        modifier = Modifier.weight(1f).height(88.dp),
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
            modifier = Modifier.padding(top = 8.dp),
        ) {
            Text(waveLocateContinue(), Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
        }
    }
}

@Composable
private fun WaitStep(
    remainingMillis: Long,
    sessionTotalMillis: Long,
    location: String?,
    onAddTen: () -> Unit,
    onClose: () -> Unit,
) {
    val done = remainingMillis == 0L
    val progress = if (sessionTotalMillis <= 0L) 0f else (remainingMillis.toFloat() / sessionTotalMillis).coerceIn(0f, 1f)
    val track = MaterialTheme.colorScheme.surfaceVariant
    val active = MaterialTheme.colorScheme.primary
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text(
            waveWaitTitle(done),
            Modifier.semantics { heading() },
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            lineHeight = 32.sp,
        )
        Box(Modifier.size(256.dp), contentAlignment = Alignment.Center) {
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
        Text(
            waveWaitBody(if (done) null else location),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 17.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(0.8f),
            lineHeight = 28.sp,
        )
        OutlinedButton(onClick = onAddTen, shape = RoundedCornerShape(999.dp)) {
            Text(waveAddTenMinutes())
        }
        if (done) {
            Button(onClick = onClose, shape = RoundedCornerShape(999.dp)) {
                Text(waveBackToToday(), Modifier.padding(horizontal = 12.dp))
            }
        }
    }
}

@Composable
private fun BreathingBackdrop() {
    val transition = rememberInfiniteTransition(label = "wave-breathe")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "wave-breathe-t",
    )
    val color = MaterialTheme.colorScheme.primaryContainer
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        GlowCircle(fraction = 0.88f, alpha = 0.18f, phase = 0f, t = t, color = color)
        GlowCircle(fraction = 0.66f, alpha = 0.22f, phase = 0.25f, t = t, color = color)
        GlowCircle(fraction = 0.44f, alpha = 0.28f, phase = 0.5f, t = t, color = color)
    }
}

@Composable
private fun GlowCircle(fraction: Float, alpha: Float, phase: Float, t: Float, color: androidx.compose.ui.graphics.Color) {
    val scale = 1f + 0.15f * sin(2.0 * PI * (t + phase)).toFloat()
    Box(
        Modifier
            .fillMaxWidth(fraction)
            .aspectRatio(1f)
            .graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }
            .clip(CircleShape)
            .background(color),
    )
}
