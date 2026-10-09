package com.anchor.app.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import com.anchor.app.relation.HomeRelationKind
import com.anchor.app.relation.HomeRelationPresentation
import com.anchor.app.relation.homeRelationPresentation
import com.anchor.app.relation.homeRelationPreviewNote
import com.anchor.app.relation.previewHomeRelation
import com.anchor.app.onboarding.AddIcon
import com.anchor.app.onboarding.AutoGraphIcon
import com.anchor.app.onboarding.ChecklistIcon
import com.anchor.app.onboarding.FactsJournalFilledIcon
import com.anchor.app.onboarding.FactsJournalIcon
import com.anchor.app.onboarding.HelpIcon
import com.anchor.app.onboarding.MedicalIcon
import com.anchor.app.onboarding.MicroActionIcon
import com.anchor.app.onboarding.PersonFilledIcon
import com.anchor.app.onboarding.PersonIcon
import com.anchor.app.onboarding.ReassessIcon
import com.anchor.app.onboarding.RestIcon
import com.anchor.app.onboarding.RhythmIcon
import com.anchor.app.onboarding.SocialEnergyIcon
import com.anchor.app.onboarding.TodayFilledIcon
import com.anchor.app.onboarding.TodayIcon
import com.anchor.app.onboarding.WorryVaultIcon
import com.anchor.app.onboarding.addAnotherAnchorLabel
import com.anchor.app.onboarding.additionalPracticeUnlocked
import com.anchor.app.onboarding.anchorsAvailableToAdd
import com.anchor.app.onboarding.firstAnchorDescription
import com.anchor.app.onboarding.firstAnchorIcon
import com.anchor.app.onboarding.firstAnchorTitle
import com.anchor.app.onboarding.practiceVisible
import com.anchor.app.onboarding.waitingReassessNotice
import com.anchor.app.onboarding.oneThingLockBody
import com.anchor.app.relation.relationEntryBody
import com.anchor.app.relation.relationEntryLabel
import com.anchor.app.settings.unlockPreviewNote
import com.anchor.app.storage.AnchorStore
import com.anchor.app.storage.FirstAnchor
import com.anchor.app.storage.RhythmEntry
import com.anchor.app.storage.WorryResolution
import com.anchor.app.ui.AnchorAppIcon
import com.anchor.app.ui.AnchorIconWell
import com.anchor.app.ui.formatCountdown
import com.anchor.app.wave.waveHomeCaption
import com.anchor.app.wave.waveHomeContentDescription
import com.anchor.app.wave.waveHomeIdleStatus
import com.anchor.app.wave.waveHomeStatus
import com.anchor.app.wave.waveHomeStatusMaxMillis
import com.anchor.app.wave.waveHomeTitle
import kotlinx.coroutines.delay

private val CardShape = RoundedCornerShape(16.dp)

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
    worryDataRevision: Int = 0,
    onHang: () -> Unit = onWorryVault,
    onMicroAction: () -> Unit,
    onRhythm: () -> Unit,
    onRelation: () -> Unit = {},
    onCameraLog: () -> Unit,
    relationPreview: HomeRelationKind? = null,
    onRelationPreviewChange: (HomeRelationKind?) -> Unit = {},
    forceOneThingLock: Boolean = false,
    onDismissOneThingLock: () -> Unit = {},
    forceUnlocked: Boolean = false,
    onDismissUnlockPreview: () -> Unit = {},
    addedAnchors: List<FirstAnchor> = emptyList(),
    canAddAnchor: Boolean = false,
    onAddAnchor: () -> Unit = {},
    onEmotion: () -> Unit = {},
    waveRemainingMillis: () -> Long = { 0L },
    scrollState: ScrollState = rememberScrollState(),
) {
    var waveRemaining by remember { mutableStateOf(waveRemainingMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            val remaining = waveRemainingMillis()
            waveRemaining = remaining
            if (remaining <= 0L || remaining > waveHomeStatusMaxMillis) break
            val untilNextMinute = remaining % 60_000L
            delay(if (untilNextMinute == 0L) 60_000L else untilNextMinute)
        }
    }
    val profile = store.userProfile()
    val canAdd = canAddAnchor || forceUnlocked ||
        (!forceOneThingLock && additionalPracticeUnlocked(profile.firstAnchorAtMillis, nowMillis()))
    val selectedAnchor = profile.firstAnchor ?: if (forceOneThingLock) FirstAnchor.MicroAction else null
    fun showPractice(anchor: FirstAnchor): Boolean = practiceVisible(anchor, selectedAnchor, addedAnchors.toSet())
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { HomeTopBar(onHelp = onHelp, showHelp = !medicalWaiting) },
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
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                        shape = CircleShape,
                        modifier = Modifier.semantics { contentDescription = "此刻需要帮助" },
                    ) { Icon(HelpIcon, contentDescription = null, modifier = Modifier.size(24.dp)) }
                } else if (showPractice(FirstAnchor.WorryVault)) {
                    FloatingActionButton(
                        onClick = onHang,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = CircleShape,
                        modifier = Modifier.semantics { contentDescription = "挂卡" },
                    ) { Icon(AddIcon, contentDescription = null, modifier = Modifier.size(28.dp)) }
                }
            }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
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
                    reassessBody = waitingReassessNotice(
                        store.safetyState().firstLowAssessmentAtMillis,
                        nowMillis(),
                    ).body,
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
                    waveStatus = waveHomeStatus(waveRemaining),
                    onRhythm = onRhythm,
                    onRelation = onRelation,
                    onMicroAction = onMicroAction,
                    onWorryVault = onWorryVault,
                    worryDataRevision = worryDataRevision,
                    relationPreview = relationPreview,
                    onRelationPreviewChange = onRelationPreviewChange,
                    showWave = showPractice(FirstAnchor.WaveWaiting),
                    showMicroAction = showPractice(FirstAnchor.MicroAction),
                    showWorry = showPractice(FirstAnchor.WorryVault),
                    selectedAnchor = selectedAnchor,
                    addedAnchors = addedAnchors,
                    canAddAnchor = canAdd && anchorsAvailableToAdd(selectedAnchor, addedAnchors).isNotEmpty(),
                    onAddAnchor = onAddAnchor,
                    onEmotion = onEmotion,
                    onCameraLog = onCameraLog,
                    lockAnchor = if (!canAdd) selectedAnchor else null,
                    lockNote = if (!canAdd && selectedAnchor != null) oneThingLockBody(selectedAnchor) else null,
                    onDismissLockPreview = if (forceOneThingLock) onDismissOneThingLock else null,
                    unlockPreview = forceUnlocked,
                    onDismissUnlockPreview = onDismissUnlockPreview,
                )
            }
            Spacer(Modifier.height(72.dp))
        }
    }
}

internal enum class HomeTab { Today, Records, Insights, Mine }

@Composable
internal fun HomeTopBar(onHelp: () -> Unit, showHelp: Boolean = true) {
    Row(
        Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        AnchorAppIcon(Modifier.size(40.dp))
        Text(
            "锚点",
            Modifier.semantics { heading() },
            color = MaterialTheme.colorScheme.primary,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
        )
        if (showHelp) {
            Surface(
                onClick = onHelp,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                modifier = Modifier.size(40.dp).semantics { contentDescription = "此刻需要帮助" },
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        HelpIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxSize(0.55f),
                    )
                }
            }
        } else {
            Spacer(Modifier.size(40.dp))
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
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BottomTab("今天", TodayIcon, TodayFilledIcon, selected == HomeTab.Today, onToday)
            BottomTab("记录", FactsJournalIcon, FactsJournalFilledIcon, selected == HomeTab.Records, onRecords)
            BottomTab("洞察", AutoGraphIcon, AutoGraphIcon, selected == HomeTab.Insights, onInsights)
            BottomTab("我的", PersonIcon, PersonFilledIcon, selected == HomeTab.Mine, onMine)
        }
    }
}

@Composable
private fun RowScope.BottomTab(
    label: String,
    icon: ImageVector,
    selectedIcon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        Modifier
            .weight(1f)
            .heightIn(min = 48.dp)
            .selectable(selected = selected, onClick = onClick, role = Role.Tab)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent,
                    shape = RoundedCornerShape(999.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (selected) selectedIcon else icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(28.dp),
            )
        }
        Text(
            label,
            color = color,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            textAlign = TextAlign.Center,
        )
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
    waveStatus: String = waveHomeIdleStatus,
    worryDataRevision: Int = 0,
    onRhythm: () -> Unit,
    onRelation: () -> Unit,
    onMicroAction: () -> Unit,
    onWorryVault: () -> Unit,
    relationPreview: HomeRelationKind? = null,
    onRelationPreviewChange: (HomeRelationKind?) -> Unit = {},
    showWave: Boolean = true,
    showMicroAction: Boolean = true,
    showWorry: Boolean = true,
    selectedAnchor: FirstAnchor? = null,
    addedAnchors: List<FirstAnchor> = emptyList(),
    canAddAnchor: Boolean = false,
    onAddAnchor: () -> Unit = {},
    onEmotion: () -> Unit = {},
    onCameraLog: () -> Unit = {},
    lockAnchor: FirstAnchor? = null,
    lockNote: String? = null,
    onDismissLockPreview: (() -> Unit)? = null,
    unlockPreview: Boolean = false,
    onDismissUnlockPreview: () -> Unit = {},
) {
    val addedSet = addedAnchors.toSet()
    val (commitment, bottom) = layoutPracticeCards(selectedAnchor, addedAnchors)
    val socialOn = practiceVisible(FirstAnchor.SocialEnergy, selectedAnchor, addedSet)
    val altruismOn = practiceVisible(FirstAnchor.AltruisticTask, selectedAnchor, addedSet)
    val rhythm = store.rhythmEntries().firstOrNull()
    val actions = store.microActions()
    val active = actions.firstOrNull { it.startedAtMillis != null && it.completedAtMillis == null }
    val featured = active ?: actions.firstOrNull { it.completedAtMillis == null } ?: actions.firstOrNull()
    // 挂卡浮层关闭时本页可能不重组，revision 变化保证卡数重读。
    val pendingWorry = remember(worryDataRevision) {
        store.worryCards().count { it.resolution == WorryResolution.Pending }
    }
    var now by remember { mutableStateOf(nowMillis()) }
    LaunchedEffect(active?.id) {
        // 只在剩余时间里逐帧走钟；时间到后停更，避免 00:00 死状态无限重组。
        if (active != null) {
            val started = active.startedAtMillis ?: return@LaunchedEffect
            while (now - started < FIVE_MINUTES) {
                withFrameMillis { now = nowMillis() }
            }
            now = nowMillis()
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
        Text(
            homeMildUseDisclaimer,
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
            fontSize = 14.sp,
            lineHeight = 22.sp,
        )
        if (lockNote != null && commitment == null && lockAnchor !in HomeLockCards) {
            OneThingLockLine(lockNote, onDismissLockPreview)
        }
        if (showWave) {
            WaveEntry(
                onClick = onWave,
                status = waveStatus,
                lockNote = lockNote.takeIf { lockAnchor == FirstAnchor.WaveWaiting },
                onDismissLockPreview = onDismissLockPreview.takeIf { lockAnchor == FirstAnchor.WaveWaiting },
            )
        }
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
                fontSize = 14.sp,
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
            if (commitment != null) {
                PracticeEntry(
                    card = commitment,
                    lockNote = lockNote.takeIf { cardMatchesAnchor(commitment, lockAnchor) },
                    onDismissLockPreview = onDismissLockPreview,
                    rhythm = rhythm,
                    formatLocalTime = formatLocalTime,
                    relationState = relationState,
                    socialOn = socialOn,
                    altruismOn = altruismOn,
                    onRhythm = onRhythm,
                    onRelation = onRelation,
                    onEmotion = onEmotion,
                    onFacts = onCameraLog,
                )
            }
            if (showMicroAction || showWorry) {
                val toolLock = lockAnchor == FirstAnchor.MicroAction || lockAnchor == FirstAnchor.WorryVault
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (toolLock && lockNote != null) {
                        OneThingLockLine(lockNote, onDismissLockPreview)
                    }
                    // 各自贴合内容高度、顶部对齐：微行动卡因倒计时变长时，忧虑保险箱卡不该被拉高。
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                    if (showMicroAction) {
                        val timeUp = remaining != null && remaining <= 0L
                        MicroActionBentoCard(
                            modifier = Modifier.weight(1f),
                            body = homeMicroActionBody(featured?.title),
                            running = remaining != null && !timeUp,
                            timeUp = timeUp,
                            countdown = if (timeUp) null else remaining?.let(::formatCountdown),
                            actionLabel = if (timeUp) {
                                homeMicroActionTimeUpLabel
                            } else {
                                homeMicroActionActionLabel(
                                    hasTitle = featured?.title != null,
                                    running = remaining != null,
                                    completed = featured?.completedAtMillis != null && remaining == null,
                                )
                            },
                            onClick = onMicroAction,
                        )
                    }
                    if (showWorry) {
                        WorryBentoCard(
                            modifier = Modifier.weight(1f),
                            pendingCount = pendingWorry,
                            sessionOpen = isWorrySessionOpen(),
                            nextLabel = nextWorrySessionLabel(),
                            onClick = onWorryVault,
                        )
                    }
                }
                }
            }
            if (unlockPreview) {
                OneThingLockLine(unlockPreviewNote, onDismissUnlockPreview)
            }
            val relationAlreadyShown = commitment == HomeAddedCard.Relation || HomeAddedCard.Relation in bottom
            bottom.forEach { card ->
                PracticeEntry(
                    card = card,
                    lockNote = null,
                    onDismissLockPreview = null,
                    rhythm = rhythm,
                    formatLocalTime = formatLocalTime,
                    relationState = relationState,
                    socialOn = socialOn,
                    altruismOn = altruismOn,
                    onRhythm = onRhythm,
                    onRelation = onRelation,
                    onEmotion = onEmotion,
                    onFacts = onCameraLog,
                )
            }
            if (relationPreview != null && !relationAlreadyShown) {
                PracticeEntry(
                    card = HomeAddedCard.Relation,
                    lockNote = null,
                    onDismissLockPreview = null,
                    rhythm = rhythm,
                    formatLocalTime = formatLocalTime,
                    relationState = relationState,
                    socialOn = true,
                    altruismOn = true,
                    onRhythm = onRhythm,
                    onRelation = onRelation,
                    onEmotion = onEmotion,
                    onFacts = onCameraLog,
                )
            }
            if (canAddAnchor) {
                TextButton(
                    onClick = onAddAnchor,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                ) { Text(addAnotherAnchorLabel, fontSize = 17.sp) }
            }
        }
    }
}

@Composable
private fun PracticeEntry(
    card: HomeAddedCard,
    lockNote: String?,
    onDismissLockPreview: (() -> Unit)?,
    rhythm: RhythmEntry?,
    formatLocalTime: (Long) -> String,
    relationState: HomeRelationPresentation,
    socialOn: Boolean,
    altruismOn: Boolean,
    onRhythm: () -> Unit,
    onRelation: () -> Unit,
    onEmotion: () -> Unit,
    onFacts: () -> Unit,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (lockNote != null) OneThingLockLine(lockNote, onDismissLockPreview)
        when (card) {
            HomeAddedCard.Rhythm -> RhythmBentoCard(
                wakeLabel = rhythm?.wakeAtMillis?.let(formatLocalTime) ?: "--:--",
                lightLabel = rhythm?.lightAtMillis?.let(formatLocalTime) ?: "--:--",
                recorded = rhythm != null,
                onClick = onRhythm,
            )
            HomeAddedCard.Emotion -> AnchorEntryCard(FirstAnchor.EmotionLabel, onEmotion)
            HomeAddedCard.Facts -> AnchorEntryCard(FirstAnchor.FactsJournal, onFacts)
            HomeAddedCard.Relation -> StatusCard(
                icon = if (relationState.kind == HomeRelationKind.Exhausted) RestIcon else SocialEnergyIcon,
                wellColor = when (relationState.kind) {
                    HomeRelationKind.Filled -> MaterialTheme.colorScheme.primaryContainer
                    HomeRelationKind.Drained -> MaterialTheme.colorScheme.secondaryContainer
                    HomeRelationKind.Exhausted -> MaterialTheme.colorScheme.secondaryContainer
                    else -> MaterialTheme.colorScheme.primaryContainer
                },
                iconTint = when (relationState.kind) {
                    HomeRelationKind.Filled -> MaterialTheme.colorScheme.onPrimaryContainer
                    else -> MaterialTheme.colorScheme.onSecondaryContainer
                },
                label = relationEntryLabel(socialOn, altruismOn, relationState),
                title = relationEntryBody(socialOn, altruismOn, relationState),
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
private fun AnchorEntryCard(anchor: FirstAnchor, onClick: () -> Unit) {
    StatusCard(
        icon = firstAnchorIcon(anchor),
        wellColor = MaterialTheme.colorScheme.primaryContainer,
        iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
        label = firstAnchorTitle(anchor),
        title = firstAnchorDescription(anchor),
        onClick = onClick,
    )
}

@Composable
private fun MedicalWaitingHome(
    onHelp: () -> Unit,
    onGuide: () -> Unit,
    onChecklist: () -> Unit,
    onCameraLog: () -> Unit,
    canReassessNow: Boolean,
    reassessBody: String,
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
                        homeWaitingBadge,
                        Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Text(homeWaitingTitle, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    homeWaitingBody,
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
                AnchorIconWell(
                    MedicalIcon,
                    MaterialTheme.colorScheme.secondaryContainer,
                    size = 56.dp,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(homeWaitingSupportTitle, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    homeWaitingSupportBody,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 15.sp,
                )
                Button(onClick = onGuide, modifier = Modifier.fillMaxWidth()) { Text(homeWaitingGuideAction) }
            }
        }
        Text(homeWaitingToolsLabel, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        ToolRow(ChecklistIcon, homeWaitingChecklistTitle, homeWaitingChecklistBody, onChecklist)
        ToolRow(FactsJournalIcon, homeWaitingJournalTitle, homeWaitingJournalBody, onCameraLog)
        if (canReassessNow) {
            ToolRow(ReassessIcon, homeWaitingReassessTitle, reassessBody, onReassess)
        }
    }
}

private val HomeLockCards = setOf(
    FirstAnchor.WaveWaiting,
    FirstAnchor.Rhythm,
    FirstAnchor.MicroAction,
    FirstAnchor.WorryVault,
    FirstAnchor.SocialEnergy,
    FirstAnchor.AltruisticTask,
)

@Composable
private fun OneThingLockLine(note: String, onDismiss: (() -> Unit)?) {
    Column(Modifier.fillMaxWidth()) {
        Text(note, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp, lineHeight = 22.sp)
        onDismiss?.let { dismiss ->
            TextButton(onClick = dismiss) { Text("关闭预览") }
        }
    }
}

private val WaveParchment = Color(0xFFFCF9F3)
private val WaveDisc = Color(0xFFD7E6DC)
private val WaveSage = Color(0xFF7A9B86)
private val WaveSageDeep = Color(0xFF5A7B66)
private val WaveInk = Color(0xFF133222)

@Composable
private fun WaveEntry(
    onClick: () -> Unit,
    status: String,
    lockNote: String? = null,
    onDismissLockPreview: (() -> Unit)? = null,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (lockNote != null) {
            OneThingLockLine(lockNote, onDismissLockPreview)
            Spacer(Modifier.height(4.dp))
        }
        Box(
            Modifier
                .size(192.dp)
                .semantics { contentDescription = waveHomeContentDescription(status) }
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            HomeWaveMark(Modifier.fillMaxSize())
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    waveHomeTitle,
                    color = WaveInk,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    status,
                    color = WaveInk.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    maxLines = 1,
                )
            }
        }
        Text(
            waveHomeCaption,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 17.sp,
            modifier = Modifier.padding(top = 24.dp),
        )
    }
}

@Composable
private fun HomeWaveMark(modifier: Modifier = Modifier) {
    val shift = rememberInfiniteTransition(label = "home-wave-mark")
    val back by shift.animateFloat(
        0f,
        200f,
        infiniteRepeatable(tween(4_000, easing = LinearEasing)),
        label = "wave-back",
    )
    val middle by shift.animateFloat(
        0f,
        200f,
        infiniteRepeatable(tween(3_000, easing = LinearEasing)),
        label = "wave-middle",
    )
    val front by shift.animateFloat(
        0f,
        200f,
        infiniteRepeatable(tween(2_500, easing = LinearEasing)),
        label = "wave-front",
    )
    val backPath = remember { homeWavePath(baseline = 120f, crest = 100f) }
    val middlePath = remember { homeWavePath(baseline = 130f, crest = 110f) }
    val frontPath = remember { homeWavePath(baseline = 145f, crest = 125f) }
    val anchor = remember { homeWaveAnchorPath() }
    Canvas(modifier) {
        val unit = size.minDimension / 200f
        scale(unit, unit, pivot = Offset.Zero) {
            drawCircle(
                color = WaveSage.copy(alpha = 0.3f),
                radius = 98f,
                center = Offset(100f, 100f),
                style = Stroke(width = 1f),
            )
            val clip = Path().apply {
                addOval(Rect(center = Offset(100f, 100f), radius = 90f))
            }
            clipPath(clip) {
                drawCircle(WaveDisc, radius = 90f, center = Offset(100f, 100f))
                translate(left = back) { drawPath(backPath, WaveSage.copy(alpha = 0.4f)) }
                translate(left = middle) { drawPath(middlePath, WaveSage.copy(alpha = 0.6f)) }
                translate(left = front) {
                    drawPath(
                        frontPath,
                        Brush.verticalGradient(
                            colors = listOf(WaveSage, WaveSageDeep),
                            startY = 125f,
                            endY = 200f,
                        ),
                    )
                }
                drawPath(anchor, WaveParchment.copy(alpha = 0.8f))
            }
        }
    }
}

private fun homeWavePath(baseline: Float, crest: Float): Path {
    val path = Path()
    path.moveTo(-200f, baseline)
    path.quadraticTo(-150f, crest, -100f, baseline)
    var x = -100f
    var controlX = -150f
    var controlY = crest
    for (endX in floatArrayOf(0f, 100f, 200f, 300f, 400f)) {
        val nextControlX = 2f * x - controlX
        val nextControlY = 2f * baseline - controlY
        path.quadraticTo(nextControlX, nextControlY, endX, baseline)
        controlX = nextControlX
        controlY = nextControlY
        x = endX
    }
    path.lineTo(400f, 200f)
    path.lineTo(-200f, 200f)
    path.close()
    return path
}

private fun homeWaveAnchorPath(): Path = Path().apply {
    moveTo(100f, 160f)
    cubicTo(95f, 160f, 92f, 155f, 92f, 150f)
    lineTo(108f, 150f)
    cubicTo(108f, 155f, 105f, 160f, 100f, 160f)
    close()
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
private fun RhythmBentoCard(
    wakeLabel: String,
    lightLabel: String,
    recorded: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface,
        shape = CardShape,
        border = cardBorder(),
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = homeRhythmKicker },
    ) {
        Row(
            Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(homeRhythmKicker, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                if (recorded) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        MonoTime("起床", wakeLabel)
                        MonoTime("见光", lightLabel)
                    }
                } else {
                    Text(homeRhythmEmptyTitle, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                }
            }
            AnchorIconWell(
                RhythmIcon,
                MaterialTheme.colorScheme.primaryContainer,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun MicroActionBentoCard(
    modifier: Modifier,
    body: String,
    running: Boolean,
    timeUp: Boolean,
    countdown: String?,
    actionLabel: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface,
        shape = CardShape,
        border = cardBorder(),
        modifier = modifier.semantics {
            contentDescription = when {
                running -> homeMicroActionTitle(true)
                timeUp -> homeMicroActionTimeUpTitle
                else -> body
            }
        },
    ) {
        Column(
            Modifier.padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AnchorIconWell(
                    MicroActionIcon,
                    MaterialTheme.colorScheme.tertiaryContainer,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                if (running || timeUp) {
                    Text(
                        if (running) homeMicroActionTitle(true) else homeMicroActionTimeUpTitle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                    )
                }
                Text(body, fontSize = 17.sp, fontWeight = FontWeight.Medium, lineHeight = 24.sp)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                countdown?.let {
                    Text(
                        it,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        "不评估想不想。启动 5 分钟。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                    )
                }
                if (actionLabel.isNotEmpty()) {
                    Text(
                        actionLabel,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun WorryBentoCard(
    modifier: Modifier,
    pendingCount: Int,
    sessionOpen: Boolean,
    nextLabel: String,
    onClick: () -> Unit,
) {
    val count = homeWorryCountText(pendingCount)
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface,
        shape = CardShape,
        border = cardBorder(),
        modifier = modifier.semantics { contentDescription = homeWorryLabel },
    ) {
        Column(
            Modifier.padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AnchorIconWell(
                    WorryVaultIcon,
                    MaterialTheme.colorScheme.tertiaryContainer,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                Text(homeWorryLabel, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                if (count == null) {
                    Text(
                        homeWorryBody(0, sessionOpen, nextLabel),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 24.sp,
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            count,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(homeWorryCountUnit, fontSize = 17.sp, modifier = Modifier.padding(bottom = 2.dp))
                    }
                }
            }
            if (count != null) {
                Text(
                    homeWorryUnlockLine(sessionOpen, nextLabel),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                )
            }
        }
    }
}

@Composable
private fun StatusCard(
    icon: ImageVector,
    wellColor: Color,
    iconTint: Color,
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
            AnchorIconWell(icon, wellColor, tint = iconTint)
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
private fun ToolRow(icon: ImageVector, title: String, body: String, onClick: () -> Unit) {
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
            AnchorIconWell(
                icon,
                MaterialTheme.colorScheme.surfaceVariant,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, lineHeight = 22.sp)
            }
        }
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
