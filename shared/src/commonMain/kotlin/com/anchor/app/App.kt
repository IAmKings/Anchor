package com.anchor.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anchor.app.onboarding.FirstRunAssessment
import com.anchor.app.onboarding.FirstAnchorChoice
import com.anchor.app.onboarding.addAnchorBody
import com.anchor.app.onboarding.addAnchorConfirm
import com.anchor.app.onboarding.addAnchorHeadline
import com.anchor.app.onboarding.addAnchorHint
import com.anchor.app.onboarding.additionalPracticeUnlocked
import com.anchor.app.onboarding.anchorsAvailableToAdd
import com.anchor.app.onboarding.choosableFirstAnchors
import com.anchor.app.onboarding.factLogVisible
import com.anchor.app.onboarding.oneThingLockBody
import com.anchor.app.onboarding.practiceVisible
import com.anchor.app.storage.FirstAnchor
import com.anchor.app.onboarding.ReassessmentFlow
import com.anchor.app.onboarding.isReassessmentBanner
import com.anchor.app.onboarding.medicalReassessAllowed
import com.anchor.app.onboarding.lastAssessmentAt
import com.anchor.app.onboarding.reassessmentDue
import com.anchor.app.emotion.EmotionCardsScreen
import com.anchor.app.home.HomeScreen
import com.anchor.app.relation.HomeRelationKind
import com.anchor.app.journal.CameraLogScreen
import com.anchor.app.journal.RecordsHub
import com.anchor.app.safety.CrisisClarificationDialog
import com.anchor.app.speech.SpeechFinal
import com.anchor.app.safety.HelpNowScreen
import com.anchor.app.safety.ReturnToPracticeScreen
import com.anchor.app.safety.MedicalGuideScreen
import com.anchor.app.safety.practiceReturnedBanner
import com.anchor.app.safety.SafetyMode
import com.anchor.app.safety.SafetyPlace
import com.anchor.app.safety.SomaticChecklistScreen
import com.anchor.app.safety.popSafety
import com.anchor.app.safety.pushSafety
import com.anchor.app.safety.safetyStateKey
import com.anchor.app.storage.AgeGroup
import com.anchor.app.storage.AnchorStore
import com.anchor.app.storage.InMemoryAnchorStore
import com.anchor.app.storage.WorryResolution
import com.anchor.app.worry.HangSheet
import com.anchor.app.worry.WorryVaultScreen
import com.anchor.app.action.MicroActionHistoryScreen
import com.anchor.app.action.MicroActionScreen
import com.anchor.app.relation.RelationFlow
import com.anchor.app.rhythm.RhythmScreen
import com.anchor.app.rhythm.seedRhythmDemoData
import com.anchor.app.insights.LocalInsightsScreen
import com.anchor.app.settings.ExportCompleteScreen
import com.anchor.app.settings.NotificationPermission
import com.anchor.app.settings.ReminderToggle
import com.anchor.app.settings.SettingsScreen
import com.anchor.app.settings.ThemeChoice
import com.anchor.app.settings.useDark
import com.anchor.app.settings.exportFileLabel
import com.anchor.app.settings.isExportSuccess
import com.anchor.app.update.UpdatePhase
import com.anchor.app.update.UpdatePromptDialog
import com.anchor.app.wave.WaveExactAlarmPrompt
import com.anchor.app.wave.WaveWaitingScreen

private val AnchorColors = lightColorScheme(
    primary = Color(0xFF466552),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF7A9B86),
    onPrimaryContainer = Color(0xFF133222),
    secondary = Color(0xFF8E4D34),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFEAA8B),
    onSecondaryContainer = Color(0xFF783C24),
    tertiary = Color(0xFF645D55),
    tertiaryContainer = Color(0xFFEBE1D6),
    onTertiaryContainer = Color(0xFF302B24),
    background = Color(0xFFFCF9F3),
    onBackground = Color(0xFF1C1C18),
    surface = Color.White,
    onSurface = Color(0xFF33302B),
    surfaceVariant = Color(0xFFE5E2DC),
    onSurfaceVariant = Color(0xFF424843),
    outline = Color(0xFF727973),
    outlineVariant = Color(0xFFE3DDD3),
)

private val AnchorDarkColors = darkColorScheme(
    primary = Color(0xFFACCFB8),
    onPrimary = Color(0xFF173A27),
    primaryContainer = Color(0xFF2E4D3C),
    onPrimaryContainer = Color(0xFFC8EBD3),
    secondary = Color(0xFFFFB59B),
    onSecondary = Color(0xFF552010),
    secondaryContainer = Color(0xFF71361F),
    onSecondaryContainer = Color(0xFFFFDBCF),
    tertiary = Color(0xFFCEC5BA),
    tertiaryContainer = Color(0xFF4C463E),
    onTertiaryContainer = Color(0xFFEBE1D6),
    background = Color(0xFF1C1B18),
    onBackground = Color(0xFFE8E4DC),
    surface = Color(0xFF262521),
    onSurface = Color(0xFFE8E4DC),
    surfaceVariant = Color(0xFF3A3730),
    onSurfaceVariant = Color(0xFFC2C8C0),
    outline = Color(0xFF8A837A),
    outlineVariant = Color(0xFF3A3730),
)

@Composable
fun App(
    anchorStore: AnchorStore? = null,
    checklist: M0Checklist = M0Checklist(),
    inAppBannerText: String? = null,
    onDismissInAppBanner: () -> Unit = {},
    appLocked: Boolean = false,
    appLockAvailable: Boolean = false,
    appLockEnabled: Boolean = false,
    appLockMessage: String? = null,
    onUnlock: () -> Unit = {},
    onToggleAppLock: () -> Unit = {},
    speechStatus: String? = null,
    speechRecording: Boolean = false,
    speechPartial: String? = null,
    speechResult: SpeechFinal? = null,
    speechTranscribes: Boolean = false,
    speechAmplitude: Float = 0f,
    onCaptureWorrySpeech: () -> Unit = {},
    onFinalizeWorrySpeech: () -> Unit = {},
    onDiscardWorrySpeech: (String?) -> Unit = {},
    onConsumeWorrySpeechResult: () -> Unit = {},
    audioPlaybackStatus: String? = null,
    onPlayWorryAudio: (String) -> Unit = {},
    audioPlaying: Boolean = false,
    onStopWorryAudioPlayback: () -> Unit = {},
    onTranscriptEdited: () -> Unit = {},
    speechDownloading: Boolean = false,
    speechDownloadProgress: Int = 0,
    speechReady: Boolean = false,
    speechEnabled: Boolean = false,
    speechError: String? = null,
    onSpeechDownload: () -> Unit = {},
    onSpeechCancelDownload: () -> Unit = {},
    onSpeechToggle: (Boolean) -> Unit = {},
    onSpeechDelete: () -> Unit = {},
    exportPassword: String = "",
    onExportPasswordChange: (String) -> Unit = {},
    exportStatus: String? = null,
    exportPasswordCopied: Boolean = false,
    onTestEncryptedExport: () -> Unit = {},
    onShareExport: () -> Unit = {},
    onSaveExportToFolder: () -> Unit = {},
    onDismissExportComplete: () -> Unit = {},
    onImportEncryptedExport: () -> Unit = {},
    deleteStatus: String? = null,
    onDeleteAllData: () -> Unit = {},
    reminderAllows: Map<ReminderToggle, Boolean> = ReminderToggle.entries.associateWith { true },
    onToggleReminder: (ReminderToggle) -> Unit = {},
    notificationPermission: NotificationPermission? = null,
    onRequestNotificationPermission: () -> Unit = {},
    exactAlarmGranted: Boolean? = null,
    onRequestExactAlarm: () -> Unit = {},
    showExactAlarmPrompt: Boolean = false,
    onExactAlarmPromptShown: () -> Unit = {},
    onPostponeReassessment: () -> Unit = {},
    nowMillis: () -> Long = { 0L },
    waveClockMillis: () -> Long = nowMillis,
    onStartWaveTimer: (Long) -> Unit = {},
    waveRemainingMillis: () -> Long = { 0L },
    isWorrySessionOpen: () -> Boolean = { false },
    nextWorrySessionMillis: () -> Long = nowMillis,
    nextWorrySessionLabel: () -> String = { "下一次专场" },
    onStartMicroActionTimer: (Long) -> Unit = {},
    onCancelMicroActionTimer: () -> Unit = {},
    formatLocalTime: (Long) -> String = { "--:--" },
    formatLocalStamp: (Long) -> String = { "--" },
    localMinuteOfDay: (Long) -> Int = { 0 },
    debugTools: Boolean = false,
    installedVersionName: String = "",
    updateCheckAvailable: Boolean = false,
    updatePhase: UpdatePhase = UpdatePhase.Idle,
    showUpdatePrompt: Boolean = false,
    onCheckUpdate: () -> Unit = {},
    onDismissUpdate: () -> Unit = {},
    onOpenUpdate: () -> Unit = {},
    themeChoice: ThemeChoice = ThemeChoice.System,
    onThemeChoice: (ThemeChoice) -> Unit = {},
    onDial: (String) -> Unit = {},
) {
    val store = anchorStore ?: remember { InMemoryAnchorStore() }
    var waveVisible by remember { mutableStateOf(false) }
    var waveEmotionNote by remember { mutableStateOf(false) }
    var waveExactAlarmPromptVisible by remember { mutableStateOf(false) }
    var emotionCardsVisible by remember { mutableStateOf(false) }
    var recordsHubVisible by remember { mutableStateOf(false) }
    var cameraLogVisible by remember { mutableStateOf(false) }
    var worryVaultVisible by remember { mutableStateOf(false) }
    var hangSheetVisible by remember { mutableStateOf(false) }
    // 挂卡弹层是首页之上的浮层，关掉时首页不一定重读仓库；用版本号驱动保险箱卡数刷新。
    var worryDataRevision by remember { mutableIntStateOf(0) }
    var relationVisible by remember { mutableStateOf(false) }
    var returnToPracticePreviewVisible by remember { mutableStateOf(false) }
    var practiceReturnedVisible by remember { mutableStateOf(false) }
    var microActionVisible by remember { mutableStateOf(false) }
    var microActionHistoryVisible by remember { mutableStateOf(false) }
    var rhythmVisible by remember { mutableStateOf(false) }
    var insightsVisible by remember { mutableStateOf(false) }
    var settingsVisible by remember { mutableStateOf(false) }
    var safetyStack by remember { mutableStateOf(emptyList<SafetyPlace>()) }
    val homeScroll = rememberScrollState()
    // 洞察页滚动状态提升：进微行动历史再返回时不丢进度（页面会短暂离开组合）。
    val insightsScroll = rememberScrollState()
    var homeRelationPreview by remember { mutableStateOf<HomeRelationKind?>(null) }
    var crisisClarificationPreviewVisible by remember { mutableStateOf(false) }
    var oneThingLockPreview by remember { mutableStateOf(false) }
    var unlockPreview by remember { mutableStateOf(false) }
    var previewAdded by remember { mutableStateOf(emptyList<FirstAnchor>()) }
    var addAnchorVisible by remember { mutableStateOf(false) }
    val profile = store.userProfile()
    val canAddAnchor = !oneThingLockPreview &&
        (unlockPreview || additionalPracticeUnlocked(profile.firstAnchorAtMillis, nowMillis()))
    val addedAnchors = profile.addedAnchors + if (unlockPreview) previewAdded else emptyList()
    val lockedAnchor = profile.firstAnchor ?: if (oneThingLockPreview) FirstAnchor.MicroAction else null
    fun canPractice(anchor: FirstAnchor): Boolean = practiceVisible(anchor, lockedAnchor, addedAnchors.toSet())
    val oneThingNote = lockedAnchor?.takeIf { !canAddAnchor }?.let(::oneThingLockBody)
    fun openCrisisFromText(persistWaiting: Boolean) {
        if (persistWaiting) store.enterCrisisWaiting()
        hangSheetVisible = false
        worryVaultVisible = false
        emotionCardsVisible = false
        cameraLogVisible = false
        recordsHubVisible = false
        settingsVisible = false
        crisisClarificationPreviewVisible = false
        safetyStack = listOf(SafetyPlace.Help)
    }
    var assessmentPreviewVisible by remember { mutableStateOf(false) }
    var reassessmentVisible by remember(store) {
        mutableStateOf(
            store.userProfile().onboardingComplete &&
                store.safetyState().mode != SafetyMode.MedicalWaiting &&
                reminderAllows[ReminderToggle.Reassessment] != false &&
                reassessmentDue(store.assessments(), nowMillis()),
        )
    }
    var onboardingVisible by remember(store) {
        mutableStateOf(!store.userProfile().onboardingComplete)
    }
    var anchorChoiceVisible by remember(store) {
        val profile = store.userProfile()
        mutableStateOf(
            profile.onboardingComplete && profile.firstAnchor == null &&
                store.safetyState().mode != SafetyMode.MedicalWaiting,
        )
    }
    val darkTheme = themeChoice.useDark(isSystemInDarkTheme())
    MaterialTheme(colorScheme = if (darkTheme) AnchorDarkColors else AnchorColors) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            if (appLocked) {
                LockedApp(appLockMessage, onUnlock)
                return@Surface
            }
            if (onboardingVisible) {
                FirstRunAssessment(
                    store = store,
                    nowMillis = nowMillis,
                    persist = true,
                    onOpenGuide = { onboardingVisible = false; safetyStack = listOf(SafetyPlace.Guide(preview = false)) },
                    onOpenChecklist = { onboardingVisible = false; safetyStack = listOf(SafetyPlace.Checklist) },
                    onDial = onDial,
                    onClose = { onboardingVisible = false },
                )
                return@Surface
            }
            if (assessmentPreviewVisible) {
                FirstRunAssessment(
                    store = store,
                    nowMillis = nowMillis,
                    persist = false,
                    onOpenGuide = { assessmentPreviewVisible = false; safetyStack = listOf(SafetyPlace.Guide(preview = false)) },
                    onOpenChecklist = { assessmentPreviewVisible = false; safetyStack = listOf(SafetyPlace.Checklist) },
                    onDial = onDial,
                    onClose = { assessmentPreviewVisible = false },
                )
                return@Surface
            }
            if (reassessmentVisible) {
                ReassessmentFlow(
                    store = store,
                    nowMillis = nowMillis,
                    persist = true,
                    localMinuteOfDay = localMinuteOfDay,
                    onOpenGuide = { reassessmentVisible = false; safetyStack = listOf(SafetyPlace.Guide(preview = false)) },
                    onOpenChecklist = { reassessmentVisible = false; safetyStack = listOf(SafetyPlace.Checklist) },
                    onDial = onDial,
                    onAddMicroAction = { reassessmentVisible = false; microActionVisible = true },
                    onPostpone = onPostponeReassessment,
                    onRecovered = { practiceReturnedVisible = true },
                    onClose = { reassessmentVisible = false },
                )
                return@Surface
            }
            if (returnToPracticePreviewVisible) {
                ReturnToPracticeScreen(onHome = { returnToPracticePreviewVisible = false })
                return@Surface
            }
            if (addAnchorVisible) {
                FirstAnchorChoice(
                    options = choosableFirstAnchors.filter { it.anchor in anchorsAvailableToAdd(lockedAnchor, addedAnchors) },
                    headline = addAnchorHeadline,
                    body = addAnchorBody,
                    commitHint = addAnchorHint,
                    confirmLabel = addAnchorConfirm,
                    onBack = { addAnchorVisible = false },
                    onConfirm = { anchor ->
                        if (unlockPreview) {
                            previewAdded = previewAdded + anchor
                        } else {
                            val current = store.userProfile()
                            store.saveUserProfile(current.copy(addedAnchors = current.addedAnchors + anchor))
                        }
                        addAnchorVisible = false
                    },
                )
                return@Surface
            }
            if (relationVisible) {
                RelationFlow(
                    store = store,
                    nowMillis = nowMillis,
                    showInventory = canPractice(FirstAnchor.SocialEnergy),
                    showAltruism = canPractice(FirstAnchor.AltruisticTask),
                    onClose = { relationVisible = false },
                )
                return@Surface
            }
            if (anchorChoiceVisible) {
                FirstAnchorChoice(
                    onConfirm = { firstAnchor ->
                        val current = store.userProfile()
                        store.saveUserProfile(
                            current.copy(
                                firstAnchor = firstAnchor,
                                firstAnchorAtMillis = current.firstAnchorAtMillis ?: nowMillis(),
                            ),
                        )
                        anchorChoiceVisible = false
                    },
                )
                return@Surface
            }
            if (waveVisible) {
                Box(Modifier.fillMaxSize()) {
                    WaveWaitingScreen(
                        nowMillis = waveClockMillis,
                        initialRemainingMillis = waveRemainingMillis(),
                        onSchedule = { millis ->
                            onStartWaveTimer(millis)
                            if (showExactAlarmPrompt) waveExactAlarmPromptVisible = true
                        },
                        onNoteEmotion = { waveEmotionNote = true },
                        onClose = {
                            waveEmotionNote = false
                            waveVisible = false
                        },
                    )
                    if (waveEmotionNote) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {},
                                ),
                        ) {
                            EmotionCardsScreen(
                                store = store,
                                nowMillis = nowMillis,
                                writeOnly = true,
                                onCrisisGuidance = {
                                    waveEmotionNote = false
                                    waveVisible = false
                                    openCrisisFromText(persistWaiting = true)
                                },
                                onClose = { waveEmotionNote = false },
                            )
                        }
                    }
                    if (waveExactAlarmPromptVisible && showExactAlarmPrompt) {
                        WaveExactAlarmPrompt(
                            onAllow = {
                                waveExactAlarmPromptVisible = false
                                onExactAlarmPromptShown()
                                onRequestExactAlarm()
                            },
                            onSkip = {
                                waveExactAlarmPromptVisible = false
                                onExactAlarmPromptShown()
                            },
                        )
                    }
                }
                return@Surface
            }
            if (worryVaultVisible) {
                Box(Modifier.fillMaxSize()) {
                    WorryVaultScreen(
                        store = store,
                        nowMillis = nowMillis,
                        isSessionOpen = isWorrySessionOpen,
                        nextSessionMillis = nextWorrySessionMillis,
                        nextSessionLabel = nextWorrySessionLabel,
                        externalRevision = worryDataRevision,
                        onHang = { hangSheetVisible = true },
                        audioPlaybackStatus = audioPlaybackStatus,
                        audioPlaying = audioPlaying,
                        onStopAudio = onStopWorryAudioPlayback,
                        onPlayAudio = onPlayWorryAudio,
                        onClose = {
                            worryVaultVisible = false
                        },
                    )
                    if (hangSheetVisible) {
                        AppHangSheet(
                            store = store,
                            nowMillis = nowMillis,
                            isSessionOpen = isWorrySessionOpen,
                            nextSessionMillis = nextWorrySessionMillis,
                            nextSessionLabel = nextWorrySessionLabel,
                            speechStatus = speechStatus,
                            speechRecording = speechRecording,
                            speechPartial = speechPartial,
                            speechResult = speechResult,
                            speechTranscribes = speechTranscribes,
                            speechAmplitude = speechAmplitude,
                            onCaptureWorrySpeech = onCaptureWorrySpeech,
                            onFinalizeWorrySpeech = onFinalizeWorrySpeech,
                            onDiscardWorrySpeech = onDiscardWorrySpeech,
                            onConsumeWorrySpeechResult = onConsumeWorrySpeechResult,
                            onTranscriptEdited = onTranscriptEdited,
                            audioPlaybackStatus = audioPlaybackStatus,
                            audioPlaying = audioPlaying,
                            onStopWorryAudioPlayback = onStopWorryAudioPlayback,
                            onPlayWorryAudio = onPlayWorryAudio,
                            onCrisisGuidance = { openCrisisFromText(persistWaiting = true) },
                            onClose = {
                                hangSheetVisible = false
                                worryDataRevision++
                            },
                        )
                    }
                }
                return@Surface
            }
            // 历史分支须在微行动分支之前：从微行动页内打开历史时 microActionVisible 仍为 true，
            // 若被微行动分支先匹配，历史页永远渲染不出来。
            if (microActionHistoryVisible) {
                MicroActionHistoryScreen(
                    store = store,
                    formatLocalStamp = formatLocalStamp,
                    onClose = { microActionHistoryVisible = false },
                )
                return@Surface
            }
            if (microActionVisible) {
                MicroActionScreen(
                    store = store,
                    nowMillis = nowMillis,
                    onStartTimer = onStartMicroActionTimer,
                    onCancelTimer = onCancelMicroActionTimer,
                    onOpenHistory = { microActionHistoryVisible = true },
                    onClose = { microActionVisible = false },
                )
                return@Surface
            }
            if (rhythmVisible) {
                RhythmScreen(store, nowMillis, formatLocalTime, localMinuteOfDay) { rhythmVisible = false }
                return@Surface
            }
            if (insightsVisible) {
                LocalInsightsScreen(
                    store = store,
                    localMinuteOfDay = localMinuteOfDay,
                    onOpenHistory = { microActionHistoryVisible = true },
                    onClose = { insightsVisible = false },
                    scrollState = insightsScroll,
                )
                return@Surface
            }
            if (safetyStack.isNotEmpty()) {
                val region = store.userProfile().crisisRegion
                val youth = store.userProfile().ageGroup == AgeGroup.Youth14To17
                val safetyStates = rememberSaveableStateHolder()
                val keptSafetyKeys = remember { mutableSetOf<String>() }
                val place = safetyStack.last()
                // Places still underneath keep their scroll. A place that left the stack does not.
                safetyStates.SaveableStateProvider(place.let(::safetyStateKey)) {
                    when (place) {
                    SafetyPlace.Help -> HelpNowScreen(
                        region = region,
                        youth = youth,
                        onOpenGuide = { safetyStack = pushSafety(safetyStack, SafetyPlace.Guide(preview = false)) },
                        onOpenChecklist = { safetyStack = pushSafety(safetyStack, SafetyPlace.Checklist) },
                        onClose = { safetyStack = popSafety(safetyStack) },
                        onDial = onDial,
                    )
                    is SafetyPlace.Guide -> MedicalGuideScreen(
                        region = region,
                        youth = youth,
                        onOpenChecklist = { safetyStack = pushSafety(safetyStack, SafetyPlace.Checklist) },
                        onOpenRecords = { safetyStack = pushSafety(safetyStack, SafetyPlace.Journal) },
                        onClose = { safetyStack = popSafety(safetyStack) },
                        previewing = place.preview,
                        onDial = onDial,
                    )
                    SafetyPlace.Checklist -> SomaticChecklistScreen(
                        onClose = { safetyStack = popSafety(safetyStack) },
                    )
                    SafetyPlace.Journal -> CameraLogScreen(
                        store = store,
                        nowMillis = nowMillis,
                        onCrisisGuidance = { openCrisisFromText(persistWaiting = true) },
                        onClose = { safetyStack = popSafety(safetyStack) },
                    )
                    }
                }
                SideEffect {
                    val current = safetyStack.map(::safetyStateKey).toSet()
                    (keptSafetyKeys - current).forEach(safetyStates::removeState)
                    keptSafetyKeys.clear()
                    keptSafetyKeys.addAll(current)
                }
                return@Surface
            }
            if (settingsVisible) {
                if (isExportSuccess(exportStatus)) {
                    ExportCompleteScreen(
                        fileName = exportFileLabel(exportStatus),
                        passwordCopied = exportPasswordCopied,
                        onShare = onShareExport,
                        onSaveToFolder = onSaveExportToFolder,
                        onHome = {
                            settingsVisible = false
                            onDismissExportComplete()
                        },
                        onBack = onDismissExportComplete,
                    )
                } else {
                    SettingsScreen(
                        appLockAvailable = appLockAvailable,
                        appLockEnabled = appLockEnabled,
                        onToggleAppLock = onToggleAppLock,
                        exportPassword = exportPassword,
                        onExportPasswordChange = onExportPasswordChange,
                        exportStatus = exportStatus,
                        onExport = onTestEncryptedExport,
                        onImport = onImportEncryptedExport,
                        deleteStatus = deleteStatus,
                        onDeleteAllData = onDeleteAllData,
                        reminderAllows = reminderAllows,
                        onToggleReminder = onToggleReminder,
                        notificationPermission = notificationPermission,
                        onRequestNotificationPermission = onRequestNotificationPermission,
                        exactAlarmGranted = exactAlarmGranted,
                        onRequestExactAlarm = onRequestExactAlarm,
                        themeChoice = themeChoice,
                        onThemeChoice = onThemeChoice,
                        speechDownloading = speechDownloading,
                        speechDownloadProgress = speechDownloadProgress,
                        speechReady = speechReady,
                        speechEnabled = speechEnabled,
                        speechError = speechError,
                        onSpeechDownload = onSpeechDownload,
                        onSpeechCancelDownload = onSpeechCancelDownload,
                        onSpeechToggle = onSpeechToggle,
                        onSpeechDelete = onSpeechDelete,
                        onOpenHelp = { safetyStack = pushSafety(safetyStack, SafetyPlace.Help) },
                        onPreviewOnboarding = { settingsVisible = false; assessmentPreviewVisible = true },
                        onPreviewReturnToPractice = { settingsVisible = false; returnToPracticePreviewVisible = true },
                        onPreviewMedicalGuide = {
                            safetyStack = pushSafety(safetyStack, SafetyPlace.Guide(preview = true))
                        },
                        onPreviewHomeRelation = {
                            settingsVisible = false
                            homeRelationPreview = HomeRelationKind.Filled
                        },
                        onPreviewCrisisClarification = { crisisClarificationPreviewVisible = true },
                        onPreviewOneThingLock = {
                            settingsVisible = false
                            unlockPreview = false
                            previewAdded = emptyList()
                            addAnchorVisible = false
                            oneThingLockPreview = true
                        },
                        onPreviewUnlock = {
                            settingsVisible = false
                            oneThingLockPreview = false
                            previewAdded = emptyList()
                            unlockPreview = true
                        },
                        debugSeedVisible = debugTools,
                        onSeedRhythmDemo = { seedRhythmDemoData(store, nowMillis(), localMinuteOfDay) },
                        onOpenReassessment = { settingsVisible = false; reassessmentVisible = true },
                        installedVersionName = installedVersionName,
                        updateCheckAvailable = updateCheckAvailable,
                        updatePhase = updatePhase,
                        onCheckUpdate = onCheckUpdate,
                        onOpenUpdate = onOpenUpdate,
                        onClose = { settingsVisible = false },
                    )
                }
                if (crisisClarificationPreviewVisible) {
                    CrisisClarificationDialog(
                        phrase = "活不下去",
                        previewing = true,
                        onNotSelf = { crisisClarificationPreviewVisible = false },
                        onSelf = { openCrisisFromText(persistWaiting = false) },
                        onUncertain = { openCrisisFromText(persistWaiting = false) },
                    )
                } else {
                    MaybeUpdatePrompt(showUpdatePrompt, updatePhase, installedVersionName, onOpenUpdate, onDismissUpdate)
                }
                return@Surface
            }
            if (emotionCardsVisible) {
                EmotionCardsScreen(
                    store = store,
                    nowMillis = nowMillis,
                    onCrisisGuidance = { openCrisisFromText(persistWaiting = true) },
                    onClose = { emotionCardsVisible = false },
                )
                return@Surface
            }
            if (cameraLogVisible) {
                CameraLogScreen(
                    store = store,
                    nowMillis = nowMillis,
                    onCrisisGuidance = { openCrisisFromText(persistWaiting = true) },
                    onClose = { cameraLogVisible = false },
                )
                return@Surface
            }
            if (recordsHubVisible) {
                RecordsHub(
                    medicalWaiting = store.safetyState().mode == SafetyMode.MedicalWaiting,
                    emotionCount = store.emotionCards().size,
                    journalCount = store.cameraLogs().size,
                    worryCount = store.worryCards().count { it.resolution == WorryResolution.Pending },
                    onEmotionCards = { emotionCardsVisible = true },
                    onCameraLog = { cameraLogVisible = true },
                    onWorryVault = { if (canPractice(FirstAnchor.WorryVault)) worryVaultVisible = true },
                    onInsights = { recordsHubVisible = false; insightsVisible = true },
                    onSettings = { recordsHubVisible = false; settingsVisible = true },
                    onHelp = { safetyStack = pushSafety(safetyStack, SafetyPlace.Help) },
                    onClose = { recordsHubVisible = false },
                    showEmotion = canPractice(FirstAnchor.EmotionLabel) || store.emotionCards().isNotEmpty(),
                    showWorry = canPractice(FirstAnchor.WorryVault),
                    lockNote = oneThingNote,
                )
                return@Surface
            }
            Box(Modifier.fillMaxSize()) {
                HomeScreen(
                    store = store,
                    scrollState = homeScroll,
                    medicalWaiting = store.safetyState().mode == SafetyMode.MedicalWaiting,
                    bannerText = inAppBannerText ?: if (practiceReturnedVisible) practiceReturnedBanner else null,
                    onDismissBanner = {
                        if (inAppBannerText != null) onDismissInAppBanner()
                        practiceReturnedVisible = false
                    },
                    onBannerReassessment = if (inAppBannerText != null && isReassessmentBanner(inAppBannerText)) {
                        { onDismissInAppBanner(); reassessmentVisible = true }
                    } else {
                        null
                    },
                    canReassessNow = medicalReassessAllowed(lastAssessmentAt(store.assessments()), nowMillis()),
                    onReassess = { reassessmentVisible = true },
                    nowMillis = nowMillis,
                    formatLocalTime = formatLocalTime,
                    isWorrySessionOpen = isWorrySessionOpen,
                    nextWorrySessionLabel = nextWorrySessionLabel,
                    waveRemainingMillis = waveRemainingMillis,
                    onWave = { if (canPractice(FirstAnchor.WaveWaiting)) waveVisible = true },
                    onRecords = { recordsHubVisible = true },
                    onInsights = { insightsVisible = true },
                    onSettings = { settingsVisible = true },
                    onHelp = { safetyStack = pushSafety(safetyStack, SafetyPlace.Help) },
                    onMedicalGuide = { safetyStack = pushSafety(safetyStack, SafetyPlace.Guide(preview = false)) },
                    onSomaticChecklist = { safetyStack = pushSafety(safetyStack, SafetyPlace.Checklist) },
                    onWorryVault = { if (canPractice(FirstAnchor.WorryVault)) worryVaultVisible = true },
                    hangSheetOpen = hangSheetVisible,
                    worryDataRevision = worryDataRevision,
                    onHang = { if (canPractice(FirstAnchor.WorryVault)) hangSheetVisible = true },
                    onMicroAction = { if (canPractice(FirstAnchor.MicroAction)) microActionVisible = true },
                    onRhythm = { if (canPractice(FirstAnchor.Rhythm)) rhythmVisible = true },
                    onRelation = {
                        if (canPractice(FirstAnchor.SocialEnergy) || canPractice(FirstAnchor.AltruisticTask)) {
                            relationVisible = true
                        }
                    },
                    onCameraLog = {
                        if (factLogVisible(
                                store.safetyState().mode == SafetyMode.MedicalWaiting,
                                canPractice(FirstAnchor.FactsJournal),
                            )
                        ) {
                            cameraLogVisible = true
                        }
                    },
                    relationPreview = homeRelationPreview,
                    onRelationPreviewChange = { homeRelationPreview = it },
                    forceOneThingLock = oneThingLockPreview,
                    onDismissOneThingLock = { oneThingLockPreview = false },
                    forceUnlocked = unlockPreview,
                    onDismissUnlockPreview = {
                        unlockPreview = false
                        previewAdded = emptyList()
                        addAnchorVisible = false
                    },
                    addedAnchors = addedAnchors,
                    canAddAnchor = canAddAnchor,
                    onAddAnchor = { addAnchorVisible = true },
                    onEmotion = { if (canPractice(FirstAnchor.EmotionLabel)) emotionCardsVisible = true },
                )
                if (hangSheetVisible) {
                    AppHangSheet(
                        store = store,
                        nowMillis = nowMillis,
                        isSessionOpen = isWorrySessionOpen,
                        nextSessionMillis = nextWorrySessionMillis,
                        nextSessionLabel = nextWorrySessionLabel,
                        speechStatus = speechStatus,
                        speechRecording = speechRecording,
                        speechPartial = speechPartial,
                        speechResult = speechResult,
                        speechTranscribes = speechTranscribes,
                        speechAmplitude = speechAmplitude,
                        onCaptureWorrySpeech = onCaptureWorrySpeech,
                        onFinalizeWorrySpeech = onFinalizeWorrySpeech,
                        onDiscardWorrySpeech = onDiscardWorrySpeech,
                        onConsumeWorrySpeechResult = onConsumeWorrySpeechResult,
                        onTranscriptEdited = onTranscriptEdited,
                        audioPlaybackStatus = audioPlaybackStatus,
                        audioPlaying = audioPlaying,
                        onStopWorryAudioPlayback = onStopWorryAudioPlayback,
                        onPlayWorryAudio = onPlayWorryAudio,
                        onCrisisGuidance = { openCrisisFromText(persistWaiting = true) },
                        onClose = {
                            hangSheetVisible = false
                            worryDataRevision++
                        },
                    )
                }
                if (!hangSheetVisible) {
                    MaybeUpdatePrompt(showUpdatePrompt, updatePhase, installedVersionName, onOpenUpdate, onDismissUpdate)
                }
            }
        }
    }
}

/**
 * 全应用唯一的挂卡浮层接线：首页 FAB 与忧虑保险箱共用，挂卡逻辑单点维护。
 */
@Composable
private fun AppHangSheet(
    store: AnchorStore,
    nowMillis: () -> Long,
    isSessionOpen: () -> Boolean,
    nextSessionMillis: () -> Long,
    nextSessionLabel: () -> String,
    speechStatus: String?,
    speechRecording: Boolean,
    speechPartial: String?,
    speechResult: SpeechFinal?,
    speechTranscribes: Boolean,
    speechAmplitude: Float,
    onCaptureWorrySpeech: () -> Unit,
    onFinalizeWorrySpeech: () -> Unit,
    onDiscardWorrySpeech: (String?) -> Unit,
    onConsumeWorrySpeechResult: () -> Unit,
    onTranscriptEdited: () -> Unit,
    audioPlaybackStatus: String?,
    audioPlaying: Boolean,
    onStopWorryAudioPlayback: () -> Unit,
    onPlayWorryAudio: (String) -> Unit,
    onCrisisGuidance: () -> Unit,
    onClose: () -> Unit,
) {
    HangSheet(
        store = store,
        nowMillis = nowMillis,
        isSessionOpen = isSessionOpen,
        nextSessionMillis = nextSessionMillis,
        nextSessionLabel = nextSessionLabel,
        speechStatus = speechStatus,
        speechRecording = speechRecording,
        speechPartial = speechPartial,
        speechResult = speechResult,
        speechTranscribes = speechTranscribes,
        speechAmplitude = speechAmplitude,
        onCaptureSpeech = onCaptureWorrySpeech,
        onFinalizeSpeech = onFinalizeWorrySpeech,
        onDiscardSpeech = onDiscardWorrySpeech,
        onConsumeSpeechResult = onConsumeWorrySpeechResult,
        onTranscriptEdited = onTranscriptEdited,
        audioPlaybackStatus = audioPlaybackStatus,
        audioPlaying = audioPlaying,
        onStopPendingAudioPlayback = onStopWorryAudioPlayback,
        onPlayWorryAudio = onPlayWorryAudio,
        onCrisisGuidance = onCrisisGuidance,
        onClose = onClose,
    )
}

@Composable
private fun MaybeUpdatePrompt(
    showUpdatePrompt: Boolean,
    updatePhase: UpdatePhase,
    installedVersionName: String,
    onOpenUpdate: () -> Unit,
    onDismissUpdate: () -> Unit,
) {
    val offer = updatePhase as? UpdatePhase.UpdateAvailable ?: return
    if (!showUpdatePrompt) return
    UpdatePromptDialog(
        remoteVersion = offer.versionName,
        localVersion = installedVersionName,
        onDownload = onOpenUpdate,
        onLater = onDismissUpdate,
    )
}

@Composable
private fun LockedApp(message: String?, onUnlock: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("锚点已锁定", fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        Text(
            message ?: "验证身份后才能查看本地内容。",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 15.sp,
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onUnlock) {
            Text("解锁")
        }
    }
}

@Composable
private fun CheckRow(label: String, passed: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .background(
                    if (passed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(999.dp),
                )
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(
                text = if (passed) "通过" else "待验证",
                color = if (passed) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
            )
        }
        Text(label, fontSize = 17.sp)
    }
}
