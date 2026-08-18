package com.anchor.app.home

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import com.anchor.app.relation.HomeRelationKind
import com.anchor.app.relation.HomeRelationPresentation
import com.anchor.app.relation.homeRelationPresentation
import com.anchor.app.relation.homeRelationPreviewNote
import com.anchor.app.relation.previewHomeRelation
import com.anchor.app.storage.AnchorStore
import com.anchor.app.storage.WorryResolution
import com.anchor.app.ui.formatCountdown
import kotlin.math.PI
import kotlin.math.cos

private val CardShape = RoundedCornerShape(16.dp)
private val Terracotta = Color(0xFFB26A4F)

@Composable
fun HomeScreen(
    store: AnchorStore,
    medicalWaiting: Boolean,
    bannerText: String?,
    onDismissBanner: () -> Unit,
    onBannerReassessment: (() -> Unit)? = null,
    canReassessNow: Boolean = false,
    onReassess: () -> Unit = {},
    nowMillis: () -> Long,
    formatLocalTime: (Long) -> String,
    isWorrySessionOpen: () -> Boolean,
    nextWorrySessionLabel: () -> String,
    onWave: () -> Unit,
    onRecords: () -> Unit,
    onInsights: () -> Unit,
    onSettings: () -> Unit,
    onHelp: () -> Unit,
    onMedicalGuide: () -> Unit = onHelp,
    onSomaticChecklist: () -> Unit = onHelp,
    onWorryVault: () -> Unit,
    hangSheetOpen: Boolean = false,
    onHang: () -> Unit = onWorryVault,
    onMicroAction: () -> Unit,
    onRhythm: () -> Unit,
    onRelation: () -> Unit = {},
    onCameraLog: () -> Unit,
    relationPreview: HomeRelationKind? = null,
    onRelationPreviewChange: (HomeRelationKind?) -> Unit = {},
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { HomeTopBar(onHelp = onHelp, onSettings = onSettings) },
        bottomBar = {
            HomeBottomBar(
                selected = HomeTab.Today,
                onToday = {},
                onRecords = onRecords,
                onInsights = onInsights,
                onMine = onSettings,
            )
        },
        floatingActionButton = {
            if (!hangSheetOpen) {
                if (medicalWaiting) {
                    FloatingActionButton(
                        onClick = onHelp,
                        containerColor = Terracotta,
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.semantics { contentDescription = "此刻需要帮助" },
                    ) { Text("助", fontWeight = FontWeight.SemiBold, fontSize = 18.sp) }
                } else {
                    FloatingActionButton(
                        onClick = onHang,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = CircleShape,
                        modifier = Modifier.semantics { contentDescription = "挂卡" },
                    ) { Text("+", fontSize = 28.sp, fontWeight = FontWeight.Medium) }
                }
            }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (bannerText != null) {
                Surface(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f), shape = CardShape) {
                    Row(
                        Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(bannerText, Modifier.weight(1f), fontSize = 15.sp)
                        if (onBannerReassessment != null) {
                            TextButton(onClick = onBannerReassessment) { Text("去复评") }
                        }
                        TextButton(onClick = onDismissBanner) { Text("知道了") }
                    }
                }
            }
            if (medicalWaiting) {
                MedicalWaitingHome(
                    onHelp = onHelp,
                    onGuide = onMedicalGuide,
                    onChecklist = onSomaticChecklist,
                    onCameraLog = onCameraLog,
                    canReassessNow = canReassessNow,
                    onReassess = onReassess,
                )
            } else {
                PracticeHome(
                    store = store,
                    nowMillis = nowMillis,
                    formatLocalTime = formatLocalTime,
                    isWorrySessionOpen = isWorrySessionOpen,
                    nextWorrySessionLabel = nextWorrySessionLabel,
                    onWave = onWave,
                    onRhythm = onRhythm,
                    onRelation = onRelation,
                    onMicroAction = onMicroAction,
                    onWorryVault = onWorryVault,
                    relationPreview = relationPreview,
                    onRelationPreviewChange = onRelationPreviewChange,
                )
            }
            Spacer(Modifier.height(72.dp))
        }
    }
}

internal enum class HomeTab { Today, Records, Insights, Mine }

@Composable
internal fun HomeTopBar(onHelp: () -> Unit, onSettings: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        TextButton(
            onClick = onHelp,
            modifier = Modifier.semantics { contentDescription = "此刻需要帮助" },
        ) {
            Text("SOS", color = Terracotta, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
        Text(
            "锚点 Anchor",
            Modifier.semantics { heading() },
            color = MaterialTheme.colorScheme.primary,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
        )
        TextButton(onClick = onSettings, modifier = Modifier.semantics { contentDescription = "设置" }) {
            Text("设置", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        }
    }
}

@Composable
internal fun HomeBottomBar(
    selected: HomeTab,
    onToday: () -> Unit,
    onRecords: () -> Unit,
    onInsights: () -> Unit,
    onMine: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BottomTab("今天", selected = selected == HomeTab.Today, onClick = onToday)
            BottomTab("记录", selected = selected == HomeTab.Records, onClick = onRecords)
            BottomTab("洞察", selected = selected == HomeTab.Insights, onClick = onInsights)
            BottomTab("我的", selected = selected == HomeTab.Mine, onClick = onMine)
        }
    }
}

@Composable
private fun BottomTab(label: String, selected: Boolean, onClick: () -> Unit) {
    val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    TextButton(onClick = onClick, modifier = Modifier.semantics { role = Role.Tab }) {
        Text(label, color = color, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, fontSize = 13.sp)
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun PracticeHome(
    store: AnchorStore,
    nowMillis: () -> Long,
    formatLocalTime: (Long) -> String,
    isWorrySessionOpen: () -> Boolean,
    nextWorrySessionLabel: () -> String,
    onWave: () -> Unit,
    onRhythm: () -> Unit,
    onRelation: () -> Unit,
    onMicroAction: () -> Unit,
    onWorryVault: () -> Unit,
    relationPreview: HomeRelationKind? = null,
    onRelationPreviewChange: (HomeRelationKind?) -> Unit = {},
) {
    val rhythm = store.rhythmEntries().firstOrNull()
    val actions = store.microActions()
    val active = actions.firstOrNull { it.startedAtMillis != null && it.completedAtMillis == null }
    val featured = active ?: actions.firstOrNull { it.completedAtMillis == null } ?: actions.firstOrNull()
    val pendingWorry = store.worryCards().count { it.resolution == WorryResolution.Pending }
    var now by remember { mutableStateOf(nowMillis()) }
    LaunchedEffect(active?.id) {
        if (active != null) {
            while (true) {
                withFrameMillis { now = nowMillis() }
            }
        }
    }
    val remaining = active?.startedAtMillis?.let { started ->
        (started + FIVE_MINUTES - now).coerceAtLeast(0)
    }

    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("应用仅适用轻度调节", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f), fontSize = 14.sp)
        }
        WaveEntry(onClick = onWave)
        val relationState = if (relationPreview != null) {
            previewHomeRelation(relationPreview)
        } else {
            homeRelationPresentation(
                contacts = store.relationContacts(),
                energy = store.relationEnergy(),
                draws = store.altruismDraws(),
                nowMillis = nowMillis(),
            )
        }
        if (relationPreview != null) {
            Text(
                homeRelationPreviewNote,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                listOf(
                    HomeRelationKind.Filled to "回血",
                    HomeRelationKind.Drained to "抽干",
                    HomeRelationKind.Exhausted to "耗竭",
                ).forEach { (kind, label) ->
                    val selected = relationPreview == kind
                    Surface(
                        onClick = { onRelationPreviewChange(kind) },
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Text(
                            label,
                            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                        )
                    }
                }
                Surface(
                    onClick = { onRelationPreviewChange(null) },
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Text("关闭预览", Modifier.padding(horizontal = 12.dp, vertical = 8.dp), fontSize = 14.sp)
                }
            }
        }
        relationState.bannerTitle?.let { title ->
            RelationBanner(relationState)
        }
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            StatusCard(
                glyph = "光",
                wellColor = MaterialTheme.colorScheme.surfaceVariant,
                label = "今日起床见光打卡",
                title = if (rhythm == null) "尚未记录" else null,
                trailing = if (rhythm != null) "✓" else null,
                onClick = onRhythm,
            ) {
                if (rhythm != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        MonoTime("起床", rhythm.wakeAtMillis?.let(formatLocalTime) ?: "--:--")
                        MonoTime("见光", rhythm.lightAtMillis?.let(formatLocalTime) ?: "--:--")
                    }
                }
            }
            StatusCard(
                glyph = "步",
                wellColor = MaterialTheme.colorScheme.secondaryContainer,
                label = homeMicroActionTitle(running = remaining != null),
                title = homeMicroActionBody(featured?.title),
                trailing = remaining?.let(::formatCountdown),
                actionLabel = homeMicroActionActionLabel(
                    hasTitle = featured?.title != null,
                    running = remaining != null,
                    completed = featured?.completedAtMillis != null && remaining == null,
                ),
                onClick = onMicroAction,
            ) {
                if (remaining != null) {
                    Text(
                        "不评估想不想。启动 5 分钟。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                    )
                }
            }
            StatusCard(
                glyph = "箱",
                wellColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f),
                label = "忧虑保险箱",
                title = homeWorryBody(pendingWorry, isWorrySessionOpen(), nextWorrySessionLabel()),
                trailing = if (isWorrySessionOpen()) "开" else "锁",
                onClick = onWorryVault,
            )
            StatusCard(
                glyph = if (relationState.kind == HomeRelationKind.Exhausted) "植" else "他",
                wellColor = when (relationState.kind) {
                    HomeRelationKind.Filled -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                    HomeRelationKind.Drained -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                    HomeRelationKind.Exhausted -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
                    else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                },
                label = relationState.cardLabel,
                title = relationState.cardBody,
                onClick = onRelation,
            ) {
                relationState.cardHint?.let { hint ->
                    Text(hint, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, lineHeight = 20.sp)
                }
            }
        }
    }
}

@Composable
private fun MedicalWaitingHome(
    onHelp: () -> Unit,
    onGuide: () -> Unit,
    onChecklist: () -> Unit,
    onCameraLog: () -> Unit,
    canReassessNow: Boolean,
    onReassess: () -> Unit,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = CardShape,
            border = cardBorder(),
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f), shape = RoundedCornerShape(999.dp)) {
                    Text(
                        "医疗等待期",
                        Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Text("休息即是当下的练习", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "暂停练习不是惩罚，是防止你在未获专业支持时过度自我要求。就医指南、躯体检查清单和仅记录的双栏日志仍然可用。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 15.sp,
                    lineHeight = 24.sp,
                )
            }
        }
        Surface(
            onClick = onHelp,
            color = MaterialTheme.colorScheme.surface,
            shape = CardShape,
            border = cardBorder(),
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "寻求专业支持，查看就医指南" },
        ) {
            Column(
                Modifier.fillMaxWidth().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                IconWell("医", MaterialTheme.colorScheme.secondaryContainer, 56.dp)
                Text("寻求专业支持", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "你的状态需要专业的评估与帮助，这很正常。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 15.sp,
                )
                Button(onClick = onGuide, modifier = Modifier.fillMaxWidth()) { Text("查看就医指南") }
            }
        }
        Text("可用工具", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        ToolRow("单", "就医准备清单", "为下一次医生问诊做准备，记下近期的身体感受。", onChecklist)
        ToolRow("记", "事实记录", "记录此刻能被看见的事实，暂不进行分析。", onCameraLog)
        if (canReassessNow) {
            ToolRow("评", "再次评估", "距上次评估已过 48 小时，可以再测一次以纠正误测。", onReassess)
        }
    }
}

@Composable
private fun WaveEntry(onClick: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "home-wave")
    val sineEase = Easing { fraction -> (0.5 - cos(PI * fraction) / 2).toFloat() }
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5_000, easing = sineEase),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "home-wave-scale",
    )
    Box(
        Modifier
            .sizeIn(maxWidth = 280.dp, maxHeight = 280.dp)
            .fillMaxWidth(0.72f)
            .aspectRatio(1f)
            .graphicsLayer { scaleX = scale; scaleY = scale },
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier
                .fillMaxSize()
                .semantics { contentDescription = "浪潮等待，难受的时候点这里" },
        ) {
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    "浪潮等待",
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "难受的时候点这里",
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                    fontSize = 17.sp,
                )
            }
        }
    }
}

@Composable
private fun RelationBanner(state: HomeRelationPresentation) {
    val filled = state.kind == HomeRelationKind.Filled
    Surface(
        color = if (filled) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
        } else {
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.28f)
        },
        shape = CardShape,
        border = BorderStroke(
            1.dp,
            if (filled) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
            else MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f),
        ),
        modifier = Modifier.fillMaxWidth().semantics {
            contentDescription = state.bannerTitle.orEmpty()
        },
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                state.bannerTitle.orEmpty(),
                color = if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp,
            )
            state.bannerBody?.let { body ->
                Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp, lineHeight = 24.sp)
            }
        }
    }
}

@Composable
private fun StatusCard(
    glyph: String,
    wellColor: Color,
    label: String,
    title: String? = null,
    trailing: String? = null,
    actionLabel: String? = null,
    onClick: () -> Unit,
    extra: (@Composable () -> Unit)? = null,
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface,
        shape = CardShape,
        border = cardBorder(),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            IconWell(glyph, wellColor)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                if (title != null) {
                    Text(title, fontSize = 17.sp, fontWeight = FontWeight.Medium, lineHeight = 24.sp)
                }
                extra?.invoke()
            }
            when {
                !actionLabel.isNullOrBlank() -> {
                    Surface(
                        color = Color.Transparent,
                        shape = RoundedCornerShape(999.dp),
                        border = cardBorder(),
                    ) {
                        Text(
                            actionLabel,
                            Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 14.sp,
                        )
                    }
                }
                trailing != null -> Text(
                    trailing,
                    color = if (trailing.contains(':')) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    fontFamily = if (trailing.contains(':')) FontFamily.Monospace else FontFamily.Default,
                    fontSize = if (trailing.contains(':')) 20.sp else 16.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun ToolRow(glyph: String, title: String, body: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface,
        shape = CardShape,
        border = cardBorder(),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            IconWell(glyph, MaterialTheme.colorScheme.surfaceVariant)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, lineHeight = 22.sp)
            }
        }
    }
}

@Composable
private fun IconWell(glyph: String, color: Color, size: androidx.compose.ui.unit.Dp = 40.dp) {
    Box(
        Modifier.size(size).clip(CircleShape).background(color),
        contentAlignment = Alignment.Center,
    ) {
        Text(glyph, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
}

@Composable
private fun MonoTime(label: String, value: String) {
    Text(
        "$label $value",
        fontFamily = FontFamily.Monospace,
        fontSize = 16.sp,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun cardBorder(): BorderStroke =
    BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))

private const val FIVE_MINUTES = 5L * 60 * 1_000
