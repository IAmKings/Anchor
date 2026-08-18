package com.anchor.app.journal

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anchor.app.home.HomeBottomBar
import com.anchor.app.home.HomeTab
import com.anchor.app.home.HomeTopBar

private val CardShape = RoundedCornerShape(16.dp)

@Composable
fun RecordsHub(
    medicalWaiting: Boolean,
    emotionCount: Int,
    journalCount: Int,
    onEmotionCards: () -> Unit,
    onCameraLog: () -> Unit,
    onInsights: () -> Unit,
    onSettings: () -> Unit,
    onHelp: () -> Unit,
    onClose: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { HomeTopBar(onHelp = onHelp, onSettings = onSettings) },
        bottomBar = {
            HomeBottomBar(
                selected = HomeTab.Records,
                onToday = onClose,
                onRecords = {},
                onInsights = onInsights,
                onMine = onSettings,
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("记录", Modifier.semantics { heading() }, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
            Text(
                recordsHubIntro(medicalWaiting),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 15.sp,
                lineHeight = 24.sp,
            )
            if (!medicalWaiting) {
                RecordEntryCard(
                    glyph = "情",
                    wellColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                    title = "情绪标签箱",
                    body = recordsEmotionBody(),
                    countLabel = recordsCountLabel("张", emotionCount),
                    contentDescription = "情绪标签箱",
                    onClick = onEmotionCards,
                )
            }
            RecordEntryCard(
                glyph = "栏",
                wellColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                title = "双栏日志",
                body = recordsJournalBody(),
                countLabel = recordsCountLabel("条", journalCount),
                contentDescription = "双栏日志",
                onClick = onCameraLog,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RecordEntryCard(
    glyph: String,
    wellColor: androidx.compose.ui.graphics.Color,
    title: String,
    body: String,
    countLabel: String?,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface,
        shape = CardShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        modifier = Modifier.fillMaxWidth().semantics { this.contentDescription = contentDescription },
    ) {
        Row(
            Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(wellColor),
                contentAlignment = Alignment.Center,
            ) {
                Text(glyph, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp, lineHeight = 24.sp)
                if (countLabel != null) {
                    Text(countLabel, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                }
            }
        }
    }
}
