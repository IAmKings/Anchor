package com.anchor.app.emotion

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.anchor.app.safety.CrisisClarificationDialog
import com.anchor.app.safety.findCrisisPhrase
import com.anchor.app.storage.AnchorStore
import com.anchor.app.storage.EmotionCard

private val CardShape = RoundedCornerShape(16.dp)
private val EmotionActionMinHeight = 52.dp

@Composable
fun EmotionCardsScreen(
    store: AnchorStore,
    nowMillis: () -> Long,
    onCrisisGuidance: () -> Unit = {},
    onClose: () -> Unit,
) {
    var composing by remember { mutableStateOf(false) }
    var revision by remember { mutableIntStateOf(0) }
    var pendingPhrase by remember { mutableStateOf<String?>(null) }
    var pendingSave by remember { mutableStateOf<(() -> Unit)?>(null) }
    if (composing) {
        NewEmotionCard(
            onCancel = { composing = false },
            onSave = { emotion, event, hardestPart ->
                val save = {
                    store.addEmotionCard(emotion, event, hardestPart, nowMillis())
                    revision++
                    composing = false
                }
                val hit = findCrisisPhrase(listOf(emotion, event, hardestPart))
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

    val cards = remember(revision) { store.emotionCards() }
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TextButton(onClick = onClose, modifier = Modifier.align(Alignment.End)) { Text(emotionBackLabel) }
        Text(emotionListTitle, Modifier.semantics { heading() }, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        Text(
            emotionListIntro,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 15.sp,
            lineHeight = 24.sp,
        )
        Button(
            onClick = { composing = true },
            modifier = Modifier.fillMaxWidth().heightIn(min = EmotionActionMinHeight),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text(emotionWriteLabel, fontSize = 17.sp)
        }
        if (cards.isEmpty()) {
            Text(emotionEmpty, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp)
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
                OutlinedButton(
                    onClick = onPassed,
                    modifier = Modifier.fillMaxWidth().heightIn(min = EmotionActionMinHeight),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(emotionPassedAction, fontSize = 17.sp)
                }
            } else {
                Text(emotionPassedMark, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
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
        TextButton(onClick = onCancel, modifier = Modifier.align(Alignment.End)) { Text(emotionCancelLabel) }
        if (selected == null) {
            Text(emotionPickTitle, Modifier.semantics { heading() }, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
            Text(
                emotionPickIntro,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 15.sp,
                lineHeight = 24.sp,
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text(emotionSearchLabel) },
                modifier = Modifier.fillMaxWidth(),
                shape = CardShape,
            )
            if (search.vague) {
                HintBanner(emotionVagueHint)
            }
            if (search.unmatched) {
                HintBanner(emotionUnmatchedHint)
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
                                modifier = Modifier.weight(1f).heightIn(min = EmotionActionMinHeight),
                            ) {
                                Box(
                                    Modifier.fillMaxWidth().heightIn(min = EmotionActionMinHeight).padding(horizontal = 12.dp),
                                    contentAlignment = Alignment.CenterStart,
                                ) {
                                    Text(word, fontSize = 16.sp)
                                }
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
            TextButton(onClick = { selected = null }) { Text(emotionSwapWord) }
            OutlinedTextField(
                value = event,
                onValueChange = { event = it },
                label = { Text(emotionEventLabel) },
                modifier = Modifier.fillMaxWidth(),
                shape = CardShape,
                minLines = 2,
            )
            OutlinedTextField(
                value = hardestPart,
                onValueChange = { hardestPart = it },
                label = { Text(emotionHardestLabel) },
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
                modifier = Modifier.fillMaxWidth().heightIn(min = EmotionActionMinHeight),
                shape = RoundedCornerShape(12.dp),
            ) { Text(emotionSaveLabel, fontSize = 17.sp) }
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
