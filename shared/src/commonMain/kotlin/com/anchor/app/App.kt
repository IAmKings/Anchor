package com.anchor.app

import androidx.compose.foundation.background
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anchor.app.onboarding.FirstRunAssessment
import com.anchor.app.onboarding.FirstAnchorChoice
import com.anchor.app.onboarding.additionalPracticeUnlocked
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
import com.anchor.app.safety.HelpNowScreen
import com.anchor.app.safety.ReturnToPracticeScreen
import com.anchor.app.safety.MedicalGuideScreen
import com.anchor.app.safety.practiceReturnedBanner
import com.anchor.app.safety.SafetyMode
import com.anchor.app.safety.SomaticChecklistScreen
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
import com.anchor.app.insights.LocalInsightsScreen
import com.anchor.app.settings.ExportCompleteScreen
import com.anchor.app.settings.ReminderToggle
import com.anchor.app.settings.SettingsScreen
import com.anchor.app.settings.exportFileLabel
import com.anchor.app.settings.isExportSuccess
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
    onTestOfflineSpeech: () -> Unit = {},
    onTestRecordingFallback: () -> Unit = {},
    onCaptureWorrySpeech: ((String) -> Unit) -> Unit = {},
    onStopWorryRecording: () -> String? = { null },
    audioPlaybackStatus: String? = null,
    onPlayWorryAudio: (String) -> Unit = {},
    exportPassword: String = "",
    onExportPasswordChange: (String) -> Unit = {},
    exportStatus: String? = null,
    exportPasswordCopied: Boolean = false,
    onTestEncryptedExport: () -> Unit = {},
    onShareExport: () -> Unit = {},
    onDismissExportComplete: () -> Unit = {},
    onImportEncryptedExport: () -> Unit = {},
    deleteStatus: String? = null,
    onDeleteAllData: () -> Unit = {},
    reminderAllows: Map<ReminderToggle, Boolean> = ReminderToggle.entries.associateWith { true },
    onToggleReminder: (ReminderToggle) -> Unit = {},
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
) {
    val store = anchorStore ?: remember { InMemoryAnchorStore() }
    var waveVisible by remember { mutableStateOf(false) }
    var emotionCardsVisible by remember { mutableStateOf(false) }
    var recordsHubVisible by remember { mutableStateOf(false) }
    var cameraLogVisible by remember { mutableStateOf(false) }
    var worryVaultVisible by remember { mutableStateOf(false) }
    var hangSheetVisible by remember { mutableStateOf(false) }
    var relationVisible by remember { mutableStateOf(false) }
    var returnToPracticePreviewVisible by remember { mutableStateOf(false) }
    var practiceReturnedVisible by remember { mutableStateOf(false) }
    var microActionVisible by remember { mutableStateOf(false) }
    var microActionHistoryVisible by remember { mutableStateOf(false) }
    var rhythmVisible by remember { mutableStateOf(false) }
    var insightsVisible by remember { mutableStateOf(false) }
    var settingsVisible by remember { mutableStateOf(false) }
    var helpVisible by remember { mutableStateOf(false) }
    var medicalGuideVisible by remember { mutableStateOf(false) }
    var medicalGuidePreviewing by remember { mutableStateOf(false) }
    var homeRelationPreview by remember { mutableStateOf<HomeRelationKind?>(null) }
    var crisisClarificationPreviewVisible by remember { mutableStateOf(false) }
    var oneThingLockPreview by remember { mutableStateOf(false) }
    val profile = store.userProfile()
    val practiceUnlocked = !oneThingLockPreview && additionalPracticeUnlocked(profile.firstAnchorAtMillis, nowMillis())
    val lockedAnchor = profile.firstAnchor ?: if (oneThingLockPreview) FirstAnchor.MicroAction else null
    fun canPractice(anchor: FirstAnchor): Boolean = practiceVisible(anchor, lockedAnchor, practiceUnlocked)
    val oneThingNote = lockedAnchor?.takeIf { !practiceUnlocked }?.let(::oneThingLockBody)
    fun openCrisisFromText(persistWaiting: Boolean) {
        if (persistWaiting) store.enterCrisisWaiting()
        hangSheetVisible = false
        worryVaultVisible = false
        emotionCardsVisible = false
        cameraLogVisible = false
        recordsHubVisible = false
        settingsVisible = false
        crisisClarificationPreviewVisible = false
        helpVisible = true
    }
    var somaticChecklistVisible by remember { mutableStateOf(false) }
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
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) AnchorDarkColors else AnchorColors) {
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
                    onOpenGuide = { onboardingVisible = false; medicalGuideVisible = true },
                    onOpenChecklist = { onboardingVisible = false; somaticChecklistVisible = true },
                    onClose = { onboardingVisible = false },
                )
                return@Surface
            }
            if (assessmentPreviewVisible) {
                FirstRunAssessment(
                    store = store,
                    nowMillis = nowMillis,
                    persist = false,
                    onOpenGuide = { assessmentPreviewVisible = false; medicalGuideVisible = true },
                    onOpenChecklist = { assessmentPreviewVisible = false; somaticChecklistVisible = true },
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
                    onOpenGuide = { reassessmentVisible = false; medicalGuideVisible = true },
                    onOpenChecklist = { reassessmentVisible = false; somaticChecklistVisible = true },
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
                WaveWaitingScreen(
                    nowMillis = waveClockMillis,
                    initialRemainingMillis = waveRemainingMillis(),
                    onSchedule = onStartWaveTimer,
                    onClose = { waveVisible = false },
                )
                return@Surface
            }
            if (worryVaultVisible) {
                WorryVaultScreen(
                    store = store,
                    nowMillis = nowMillis,
                    isSessionOpen = isWorrySessionOpen,
                    nextSessionMillis = nextWorrySessionMillis,
                    nextSessionLabel = nextWorrySessionLabel,
                    speechStatus = speechStatus,
                    speechRecording = speechRecording,
                    onCaptureSpeech = onCaptureWorrySpeech,
                    onStopRecording = onStopWorryRecording,
                    audioPlaybackStatus = audioPlaybackStatus,
                    onPlayAudio = onPlayWorryAudio,
                    onCrisisGuidance = { openCrisisFromText(persistWaiting = true) },
                    onClose = {
                        if (speechRecording) onStopWorryRecording()
                        worryVaultVisible = false
                    },
                )
                return@Surface
            }
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
                )
                return@Surface
            }
            if (helpVisible) {
                HelpNowScreen(
                    region = store.userProfile().crisisRegion,
                    youth = store.userProfile().ageGroup == AgeGroup.Youth14To17,
                    onOpenGuide = { helpVisible = false; medicalGuideVisible = true },
                    onOpenChecklist = { helpVisible = false; somaticChecklistVisible = true },
                    onClose = { helpVisible = false },
                )
                return@Surface
            }
            if (medicalGuideVisible) {
                MedicalGuideScreen(
                    region = store.userProfile().crisisRegion,
                    youth = store.userProfile().ageGroup == AgeGroup.Youth14To17,
                    onOpenChecklist = {
                        medicalGuideVisible = false
                        medicalGuidePreviewing = false
                        somaticChecklistVisible = true
                    },
                    onOpenRecords = {
                        medicalGuideVisible = false
                        medicalGuidePreviewing = false
                        cameraLogVisible = true
                    },
                    onClose = {
                        medicalGuideVisible = false
                        medicalGuidePreviewing = false
                    },
                    previewing = medicalGuidePreviewing,
                )
                return@Surface
            }
            if (somaticChecklistVisible) {
                SomaticChecklistScreen(onClose = { somaticChecklistVisible = false })
                return@Surface
            }
            if (settingsVisible) {
                if (isExportSuccess(exportStatus)) {
                    ExportCompleteScreen(
                        fileName = exportFileLabel(exportStatus),
                        passwordCopied = exportPasswordCopied,
                        onShare = onShareExport,
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
                        onOpenHelp = { settingsVisible = false; helpVisible = true },
                        onPreviewOnboarding = { settingsVisible = false; assessmentPreviewVisible = true },
                        onPreviewReturnToPractice = { settingsVisible = false; returnToPracticePreviewVisible = true },
                        onPreviewMedicalGuide = {
                            settingsVisible = false
                            medicalGuidePreviewing = true
                            medicalGuideVisible = true
                        },
                        onPreviewHomeRelation = {
                            settingsVisible = false
                            homeRelationPreview = HomeRelationKind.Filled
                        },
                        onPreviewCrisisClarification = { crisisClarificationPreviewVisible = true },
                        onPreviewOneThingLock = {
                            settingsVisible = false
                            oneThingLockPreview = true
                        },
                        onOpenReassessment = { settingsVisible = false; reassessmentVisible = true },
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
                    onHelp = { recordsHubVisible = false; helpVisible = true },
                    onClose = { recordsHubVisible = false },
                    showEmotion = canPractice(FirstAnchor.EmotionLabel),
                    showJournal = canPractice(FirstAnchor.FactsJournal),
                    showWorry = canPractice(FirstAnchor.WorryVault),
                    lockNote = oneThingNote,
                )
                return@Surface
            }
            Box(Modifier.fillMaxSize()) {
                HomeScreen(
                    store = store,
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
                    onWave = { if (canPractice(FirstAnchor.WaveWaiting)) waveVisible = true },
                    onRecords = { recordsHubVisible = true },
                    onInsights = { insightsVisible = true },
                    onSettings = { settingsVisible = true },
                    onHelp = { helpVisible = true },
                    onMedicalGuide = { medicalGuideVisible = true },
                    onSomaticChecklist = { somaticChecklistVisible = true },
                    onWorryVault = { if (canPractice(FirstAnchor.WorryVault)) worryVaultVisible = true },
                    hangSheetOpen = hangSheetVisible,
                    onHang = { if (canPractice(FirstAnchor.WorryVault)) hangSheetVisible = true },
                    onMicroAction = { if (canPractice(FirstAnchor.MicroAction)) microActionVisible = true },
                    onRhythm = { if (canPractice(FirstAnchor.Rhythm)) rhythmVisible = true },
                    onRelation = {
                        if (canPractice(FirstAnchor.SocialEnergy) || canPractice(FirstAnchor.AltruisticTask)) {
                            relationVisible = true
                        }
                    },
                    onCameraLog = { if (canPractice(FirstAnchor.FactsJournal)) cameraLogVisible = true },
                    relationPreview = homeRelationPreview,
                    onRelationPreviewChange = { homeRelationPreview = it },
                    forceOneThingLock = oneThingLockPreview,
                    onDismissOneThingLock = { oneThingLockPreview = false },
                )
                if (hangSheetVisible) {
                    HangSheet(
                        store = store,
                        nowMillis = nowMillis,
                        isSessionOpen = isWorrySessionOpen,
                        nextSessionMillis = nextWorrySessionMillis,
                        nextSessionLabel = nextWorrySessionLabel,
                        speechStatus = speechStatus,
                        speechRecording = speechRecording,
                        onCaptureSpeech = onCaptureWorrySpeech,
                        onStopRecording = onStopWorryRecording,
                        onCrisisGuidance = { openCrisisFromText(persistWaiting = true) },
                        onClose = {
                            if (speechRecording) onStopWorryRecording()
                            hangSheetVisible = false
                        },
                    )
                }
            }
        }
    }
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
