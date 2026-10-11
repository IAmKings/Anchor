package com.anchor.app.wave

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** One-time ask before the first waiting round when exact alarms are not granted. */
@Composable
fun WaveExactAlarmPrompt(
    onAllow: () -> Unit,
    onSkip: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onSkip,
        title = {
            Text(
                waveExactAlarmPromptTitle(),
                Modifier.semantics { heading() },
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Text(waveExactAlarmPromptBody(), fontSize = 15.sp, lineHeight = 24.sp)
        },
        confirmButton = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                TextButton(onClick = onAllow, modifier = Modifier.fillMaxWidth()) {
                    Text(waveExactAlarmPromptAllow)
                }
                TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
                    Text(waveExactAlarmPromptSkip)
                }
            }
        },
        shape = RoundedCornerShape(16.dp),
    )
}
