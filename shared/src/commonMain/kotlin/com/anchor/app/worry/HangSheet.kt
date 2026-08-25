package com.anchor.app.worry

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anchor.app.safety.CrisisClarificationDialog
import com.anchor.app.safety.findCrisisPhrase
import com.anchor.app.storage.AnchorStore

private val SheetShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
private val FieldShape = RoundedCornerShape(12.dp)

@Composable
fun HangSheet(
    store: AnchorStore,
    nowMillis: () -> Long,
    isSessionOpen: () -> Boolean,
    nextSessionMillis: () -> Long,
    nextSessionLabel: () -> String,
    speechStatus: String?,
    speechRecording: Boolean,
    onCaptureSpeech: ((String) -> Unit) -> Unit,
    onStopRecording: () -> String?,
    onCrisisGuidance: () -> Unit = {},
    onClose: () -> Unit,
) {
    var content by remember { mutableStateOf("") }
    var savedMessage by remember { mutableStateOf<String?>(null) }
    var pendingPhrase by remember { mutableStateOf<String?>(null) }

    fun close() {
        if (speechRecording) onStopRecording()
        onClose()
    }

    fun sealText() {
        val text = content.trim()
        if (text.isBlank()) return
        val hit = findCrisisPhrase(text)
        if (hit != null) {
            pendingPhrase = hit
            return
        }
        store.addWorryCard(text, nowMillis(), nextSessionMillis())
        content = ""
        savedMessage = vaultSealedMessage(isSessionOpen(), nextSessionLabel())
    }

    fun confirmSeal(goToCrisis: Boolean) {
        val text = content.trim()
        if (text.isNotBlank()) {
            store.addWorryCard(text, nowMillis(), nextSessionMillis())
            content = ""
            savedMessage = vaultSealedMessage(isSessionOpen(), nextSessionLabel())
        }
        pendingPhrase = null
        if (goToCrisis) onCrisisGuidance()
    }

    fun sealSpeech() {
        if (speechRecording) {
            onStopRecording()?.let { audioFileName ->
                store.addWorryCard("", nowMillis(), nextSessionMillis(), audioFileName)
                savedMessage = vaultSealedMessage(isSessionOpen(), nextSessionLabel())
            }
        } else {
            onCaptureSpeech { content = it }
        }
    }

    Box(Modifier.fillMaxSize().imePadding()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { close() },
                )
                .semantics { contentDescription = "关闭挂卡" },
        )
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars),
            color = MaterialTheme.colorScheme.surface,
            shape = SheetShape,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            HangComposer(
                title = hangSheetTitle,
                fieldLabel = hangFieldHint,
                content = content,
                onContent = { content = it },
                speechRecording = speechRecording,
                speechStatus = speechStatus,
                savedMessage = savedMessage,
                onSpeech = { sealSpeech() },
                onHang = { sealText() },
                onDismiss = { close() },
                showDismiss = savedMessage != null,
            )
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
            Text(if (speechRecording) hangSpeechStopLabel else hangSpeechLabel, fontSize = 17.sp)
        }
        speechStatus?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp) }
        Button(
            onClick = onHang,
            enabled = content.isNotBlank(),
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
