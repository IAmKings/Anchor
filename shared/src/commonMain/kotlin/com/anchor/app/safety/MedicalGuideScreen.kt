package com.anchor.app.safety

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anchor.app.onboarding.label
import com.anchor.app.storage.CrisisRegion

private val CardShape = RoundedCornerShape(16.dp)
private val ChipShape = RoundedCornerShape(20.dp)
private val DesignedPreviewRegions = listOf(
    CrisisRegion.MainlandChina,
    CrisisRegion.Taiwan,
    CrisisRegion.HongKong,
    CrisisRegion.UnitedStates,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MedicalGuideScreen(
    region: CrisisRegion,
    youth: Boolean,
    onOpenChecklist: () -> Unit,
    onOpenRecords: () -> Unit = {},
    onClose: () -> Unit,
    previewing: Boolean = false,
) {
    var shownRegion by remember(region) { mutableStateOf(region) }
    val content = medicalGuideContent(shownRegion, youth)
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TextButton(onClick = onClose, modifier = Modifier.align(Alignment.End)) { Text("返回") }
        Text(
            "就医指南",
            Modifier.fillMaxWidth().semantics { heading() },
            color = MaterialTheme.colorScheme.primary,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Text(
            shownRegion.label(),
            Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
        )
        if (previewing) {
            Text(
                "预览中，不会改设备地区。",
                Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                DesignedPreviewRegions.forEach { option ->
                    val selected = shownRegion == option
                    Surface(
                        onClick = { shownRegion = option },
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        shape = ChipShape,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Text(
                            option.label(),
                            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                        )
                    }
                }
            }
        }
        Text(
            "寻求专业帮助是保护自己最重要的一步。\n判断诊断不是你的责任，那是医生的工作。",
            Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 16.sp,
            lineHeight = 26.sp,
            textAlign = TextAlign.Center,
        )
        Text("紧急援助热线", fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = CardShape,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
            modifier = Modifier.semantics { contentDescription = "紧急援助热线 ${shownRegion.label()}" },
        ) {
            Column(Modifier.fillMaxWidth()) {
                Surface(color = MaterialTheme.colorScheme.errorContainer) {
                    Column(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            content.emergency.title,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                        )
                        EmergencyActionLine(content.emergency)
                    }
                }
                content.hotlines.forEachIndexed { index, line ->
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    HotlineRow(line, emphasize = index == 0)
                }
            }
        }
        Text("就诊准备清单", fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
        PrepStep(
            index = 1,
            title = "寻找陪伴者",
            body = "寻找一位信任的家人或朋友陪同就诊。他们在你状态不佳时能提供客观描述和情感支持。",
        )
        PrepStep(
            index = 2,
            title = "整理症状记录",
            body = "回顾并导出你在 Anchor 上的状态记录，帮助医生快速了解你的睡眠、情绪起伏和躯体化症状。",
            actionLabel = "打开记录",
            onAction = onOpenRecords,
        )
        Surface(
            onClick = onOpenChecklist,
            color = MaterialTheme.colorScheme.surface,
            shape = CardShape,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top,
            ) {
                StepBadge(3)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("基础躯体排查提醒", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text(
                        "很多心理症状可能由躯体问题引起。初诊时，可向医生询问是否需要进行以下基础排查：",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 15.sp,
                        lineHeight = 24.sp,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SomaticChip("甲状腺功能 (T3/T4/TSH)")
                        SomaticChip("贫血检查")
                        SomaticChip("维生素 D 水平")
                    }
                }
            }
        }
        Text(
            "暂停练习不是惩罚，是防止你在未获专业支持时过度自我要求。",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 15.sp,
            lineHeight = 24.sp,
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun EmergencyActionLine(emergency: MedicalGuideEmergency) {
    if (emergency.numbers.isEmpty()) {
        Text(
            emergency.actionLine(),
            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f),
            fontSize = 14.sp,
        )
        return
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "请立即拨打 ",
            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f),
            fontSize = 14.sp,
        )
        emergency.numbers.forEachIndexed { index, number ->
            if (index > 0) {
                Text(
                    " 或 ",
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f),
                    fontSize = 14.sp,
                )
            }
            Text(
                number,
                color = MaterialTheme.colorScheme.error,
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun HotlineRow(line: GuideHotline, emphasize: Boolean) {
    Row(
        Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(line.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            if (line.detail.isNotEmpty()) {
                Text(line.detail, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            }
        }
        Text(
            line.number,
            Modifier.widthIn(max = 168.dp),
            color = if (emphasize) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun PrepStep(
    index: Int,
    title: String,
    body: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = CardShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            StepBadge(index)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                    if (actionLabel != null && onAction != null) {
                        TextButton(onClick = onAction) { Text(actionLabel) }
                    }
                }
                Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp, lineHeight = 24.sp)
            }
        }
    }
}

@Composable
private fun StepBadge(index: Int) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = CircleShape,
        modifier = Modifier.size(32.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text("$index", fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun SomaticChip(label: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = ChipShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
    ) {
        Text(
            label,
            Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
        )
    }
}
