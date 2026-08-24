package com.anchor.app.settings

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val CardShape = RoundedCornerShape(16.dp)
private val PillShape = RoundedCornerShape(28.dp)

@Composable
fun ExportCompleteScreen(
    fileName: String?,
    passwordCopied: Boolean,
    onShare: () -> Unit,
    onHome: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.End)) { Text("返回") }
        Spacer(Modifier.height(24.dp))
        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape) {
            Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) {
                Text("✓", color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 36.sp, fontWeight = FontWeight.Medium)
            }
        }
        Text(exportCompleteTitle, Modifier.semantics { heading() }, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Text(
            exportCompleteBody,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 15.sp,
            lineHeight = 24.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        fileName?.let {
            Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, textAlign = TextAlign.Center)
        }
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = CardShape,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        ) {
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f), shape = CircleShape) {
                    Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                        Text("钥", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        if (passwordCopied) exportPasswordCopiedTitle else exportPasswordRememberTitle,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                    )
                    Text(
                        if (passwordCopied) exportPasswordCopiedBody else exportPasswordRememberBody,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = onShare, modifier = Modifier.fillMaxWidth().height(56.dp), shape = PillShape) {
            Text(exportShareLabel, fontSize = 17.sp)
        }
        TextButton(onClick = onHome, modifier = Modifier.fillMaxWidth()) { Text(exportHomeLabel) }
        Spacer(Modifier.height(16.dp))
    }
}
