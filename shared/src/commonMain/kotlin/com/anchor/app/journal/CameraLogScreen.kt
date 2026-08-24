package com.anchor.app.journal

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anchor.app.safety.CrisisClarificationDialog
import com.anchor.app.safety.findCrisisPhrase
import com.anchor.app.storage.AnchorStore
import com.anchor.app.storage.CameraLog
import com.anchor.app.storage.firstEvaluativeWord

private val CardShape = RoundedCornerShape(16.dp)

@Composable
fun CameraLogScreen(
    store: AnchorStore,
    nowMillis: () -> Long,
    onCrisisGuidance: () -> Unit = {},
    onClose: () -> Unit,
) {
    var composing by remember { mutableStateOf(false) }
    var reflectionVisible by remember { mutableStateOf(false) }
    var revision by remember { mutableIntStateOf(0) }
    var pendingPhrase by remember { mutableStateOf<String?>(null) }
    var pendingSave by remember { mutableStateOf<(() -> Unit)?>(null) }
    if (composing) {
        NewCameraLog(
            onCancel = { composing = false },
            onSave = { fact, inference ->
                val save = {
                    store.addCameraLog(fact, inference, nowMillis())
                    revision++
                    composing = false
                    reflectionVisible = true
                }
                val hit = findCrisisPhrase(listOf(fact, inference))
                if (hit == null) save()
                else {
                    pendingSave = save
                    pendingPhrase = hit
                }
            },
        )
        pendingPhrase?.let { phrase ->
            CrisisClarificationDialog(
                phrase = phrase,
                onNotSelf = {
                    pendingSave?.invoke()
                    pendingSave = null
                    pendingPhrase = null
                },
                onSelf = {
                    pendingSave?.invoke()
                    pendingSave = null
                    pendingPhrase = null
                    onCrisisGuidance()
                },
                onUncertain = {
                    pendingSave?.invoke()
                    pendingSave = null
                    pendingPhrase = null
                    onCrisisGuidance()
                },
            )
        }
        return
    }
    val logs = remember(revision) { store.cameraLogs() }
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TextButton(onClick = onClose, modifier = Modifier.align(Alignment.End)) { Text("返回") }
        JournalHeader()
        if (reflectionVisible) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f),
                shape = CardShape,
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("右栏是你的大脑推断，并非已发生的事实。", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    TextButton(onClick = { reflectionVisible = false }) { Text("知道了") }
                }
            }
        }
        Button(onClick = { composing = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Text("写下这一栏")
        }
        if (logs.isEmpty()) {
            Text("还没有记录。只写一栏也可以。", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp)
        }
        logs.forEach { SavedCameraLog(it) }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun JournalHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(999.dp)) {
            Text(
                "隔离事实与大脑推断",
                Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
            )
        }
        Text("双栏日志", Modifier.semantics { heading() }, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        Text(
            "把摄像头拍到的事实和大脑自动补全的推断分开。",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 15.sp,
            lineHeight = 24.sp,
        )
    }
}

@Composable
private fun SavedCameraLog(log: CameraLog) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = CardShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (log.fact.isNotEmpty()) {
                Text("摄像头拍到的事实", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(log.fact, fontSize = 16.sp, lineHeight = 26.sp)
            }
            if (log.inference.isNotEmpty()) {
                Text("我的大脑推断", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(log.inference, fontSize = 16.sp, lineHeight = 26.sp)
            }
            if (log.factNeedsHint) {
                Text("这条可能包含推断，需要的话可以放到右栏。", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun NewCameraLog(onCancel: () -> Unit, onSave: (String, String) -> Unit) {
    var fact by remember { mutableStateOf("") }
    var inference by remember { mutableStateOf("") }
    val detected = firstEvaluativeWord(fact)
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TextButton(onClick = onCancel, modifier = Modifier.align(Alignment.End)) { Text("取消") }
        JournalHeader()
        ColumnCard(
            glyph = "摄",
            title = "摄像头拍到的事实",
            caption = "仅记录动作、时间、地点、话语。",
            wellColor = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            OutlinedTextField(
                value = fact,
                onValueChange = { fact = it },
                placeholder = { Text("例如：早上 9 点，他发来一条信息说「我现在很忙」。") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 5,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
            )
            if (detected != null) {
                Surface(
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("这条可能属于右栏", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Text("检测到「$detected」。仍然可以直接保存。", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                        }
                        TextButton(onClick = {
                            val moved = moveEvaluativeSentence(fact, inference, detected)
                            fact = moved.first
                            inference = moved.second
                        }) { Text("挪过去") }
                    }
                }
            }
        }
        ColumnCard(
            glyph = "脑",
            title = "我的大脑推断",
            caption = "记录你的猜测、评价和联想。",
            wellColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
        ) {
            OutlinedTextField(
                value = inference,
                onValueChange = { inference = it },
                placeholder = { Text("例如：我觉得他是在针对我。") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 5,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.secondary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
            )
        }
        Text("低精力时只写一栏也可以。", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        Button(
            onClick = { onSave(fact, inference) },
            enabled = fact.isNotBlank() || inference.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
        ) { Text("保存记录") }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ColumnCard(
    glyph: String,
    title: String,
    caption: String,
    wellColor: androidx.compose.ui.graphics.Color,
    content: @Composable () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = CardShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(color = wellColor, shape = CircleShape, modifier = Modifier.size(40.dp)) {
                    androidx.compose.foundation.layout.Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(glyph, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    }
                }
                Column {
                    Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text(caption, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                }
            }
            content()
        }
    }
}
