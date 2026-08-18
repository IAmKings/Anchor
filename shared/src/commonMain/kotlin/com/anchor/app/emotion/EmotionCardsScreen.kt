package com.anchor.app.emotion

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anchor.app.storage.AnchorStore
import com.anchor.app.storage.EmotionCard

private val CardShape = RoundedCornerShape(16.dp)

@Composable
fun EmotionCardsScreen(store: AnchorStore, nowMillis: () -> Long, onClose: () -> Unit) {
    var composing by remember { mutableStateOf(false) }
    var revision by remember { mutableIntStateOf(0) }
    if (composing) {
        NewEmotionCard(
            onCancel = { composing = false },
            onSave = { emotion, event, hardestPart ->
                store.addEmotionCard(emotion, event, hardestPart, nowMillis())
                revision++
                composing = false
            },
        )
        return
    }

    val cards = remember(revision) { store.emotionCards() }
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TextButton(onClick = onClose, modifier = Modifier.align(Alignment.End)) { Text("返回") }
        Text("给情绪起一个准确的名字", Modifier.semantics { heading() }, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        Text(
            "不是为了分析它，只是把模糊的难受变成一个可以看见的对象。",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 15.sp,
            lineHeight = 24.sp,
        )
        Button(onClick = { composing = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Text("写下这一张")
        }
        if (cards.isEmpty()) {
            Text("这里还没有卡片。暂停很正常。", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp)
        }
        cards.forEach { card ->
            SavedEmotionCard(card, onPassed = {
                store.markEmotionCardPassed(card.id, nowMillis())
                revision++
            })
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SavedEmotionCard(card: EmotionCard, onPassed: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = CardShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                shape = RoundedCornerShape(999.dp),
            ) {
                Text(
                    card.emotion,
                    Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                )
            }
            Text(emotionCardSentence(card.emotion, card.event, card.hardestPart), fontSize = 16.sp, lineHeight = 26.sp)
            if (card.passedAtMillis == null) {
                TextButton(onClick = onPassed) { Text("它已经过去了") }
            } else {
                Text("已过去", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun NewEmotionCard(onCancel: () -> Unit, onSave: (String, String, String) -> Unit) {
    var selected by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var event by remember { mutableStateOf("") }
    var hardestPart by remember { mutableStateOf("") }
    val search = remember(query) { searchEmotions(query) }

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TextButton(onClick = onCancel, modifier = Modifier.align(Alignment.End)) { Text("取消") }
        if (selected == null) {
            Text("哪一个词更接近？", Modifier.semantics { heading() }, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "“很烦”“很难过”可以是入口，但请再具体一点。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 15.sp,
                lineHeight = 24.sp,
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("搜一个更接近的词") },
                modifier = Modifier.fillMaxWidth(),
                shape = CardShape,
            )
            if (search.vague) {
                HintBanner("这个还太宽。请从下面挑一个更具体的词。")
            }
            if (search.unmatched) {
                HintBanner("词库里没有这个。请从下面挑一个具体的词。")
            }
            search.groups.forEach { group ->
                Text(group.title, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                group.words.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { word ->
                            Surface(
                                onClick = { selected = word },
                                shape = CardShape,
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    word,
                                    Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                                    fontSize = 16.sp,
                                )
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        } else {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                shape = RoundedCornerShape(999.dp),
            ) {
                Text(
                    selected!!,
                    Modifier.padding(horizontal = 14.dp, vertical = 6.dp).semantics { heading() },
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            TextButton(onClick = { selected = null }) { Text("换一个词") }
            OutlinedTextField(
                value = event,
                onValueChange = { event = it },
                label = { Text("在什么事情中") },
                modifier = Modifier.fillMaxWidth(),
                shape = CardShape,
                minLines = 2,
            )
            OutlinedTextField(
                value = hardestPart,
                onValueChange = { hardestPart = it },
                label = { Text("最让我难受的具体部分") },
                modifier = Modifier.fillMaxWidth(),
                shape = CardShape,
                minLines = 2,
            )
            if (event.isNotBlank() && hardestPart.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = CardShape,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
                ) {
                    Text(
                        emotionCardSentence(selected!!, event, hardestPart),
                        Modifier.padding(16.dp),
                        fontSize = 16.sp,
                        lineHeight = 26.sp,
                    )
                }
            }
            Button(
                onClick = { onSave(selected!!, event, hardestPart) },
                enabled = event.isNotBlank() && hardestPart.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) { Text("保存卡片") }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun HintBanner(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f),
        shape = CardShape,
    ) {
        Text(text, Modifier.padding(14.dp), fontSize = 15.sp, lineHeight = 24.sp)
    }
}
