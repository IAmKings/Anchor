package com.anchor.app

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.media.MediaPlayer
import android.app.NotificationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.anchor.app.reminder.AndroidReminderScheduler
import com.anchor.app.rhythm.seedRhythmDemoData
import com.anchor.app.reminder.ReminderKind
import com.anchor.app.settings.NotificationPermission
import com.anchor.app.settings.ReminderToggle
import com.anchor.app.safety.phoneTelUri
import com.anchor.app.settings.ThemeChoice
import com.anchor.app.settings.useDark
import com.anchor.app.speech.AndroidLocalAudioRecorder
import com.anchor.app.speech.AndroidOnnxModelManager
import com.anchor.app.speech.OnnxAsrState
import com.anchor.app.speech.SherpaOnnxRuntime
import com.anchor.app.speech.punctuateTranscript
import com.anchor.app.speech.SpeechEngine
import com.anchor.app.speech.SpeechFinal
import com.anchor.app.speech.createSpeechEngine
import com.anchor.app.timer.AndroidBackgroundTimer
import com.anchor.app.timer.BackgroundTimerKind
import com.anchor.app.storage.AndroidDatabaseKey
import com.anchor.app.storage.AndroidEncryptedProbeStore
import com.anchor.app.storage.buildLocalExport
import com.anchor.app.storage.worryAudioExportName
import com.anchor.app.update.UpdatePhase
import com.anchor.app.worry.AndroidWorrySessionClock
import java.io.File

class MainActivity : FragmentActivity() {
    // SAF：把最近一次导出的加密文件另存到用户选择的文件夹（sdcard 任意位置），无需存储权限。
    private val saveExportToFolder = registerForActivityResult(
        ActivityResultContracts.CreateDocument(AndroidEncryptedExport.MIME_TYPE),
    ) { uri ->
        if (uri != null) copyLastExportTo(uri)
    }

    private val importBackup = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(::restoreEncryptedExport)
    }
    private val requestPostNotifications = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (!granted &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            )
        ) {
            notificationPrefs().edit().putBoolean(NOTIFICATION_ASKED, true).apply()
        }
        refreshNotificationPermission()
    }
    private var notificationPermission by mutableStateOf(NotificationPermission.Requestable)
    private var exactAlarmGranted by mutableStateOf(true)
    private var exactAlarmPromptAsked by mutableStateOf(true)
    private val deadlineHandler = Handler(Looper.getMainLooper())
    private val deadlineTick: Runnable = Runnable { onDeadlineTick() }

    private fun onDeadlineTick() {
        if (!::backgroundTimer.isInitialized) return
        backgroundTimer.deliverIfDue()
        microActionTimer.deliverIfDue()
        val waveLeft = backgroundTimer.remainingMillis()
        val microLeft = microActionTimer.remainingMillis()
        val wait = minOf(
            if (waveLeft > 0L) waveLeft else Long.MAX_VALUE,
            if (microLeft > 0L) microLeft else Long.MAX_VALUE,
        )
        val delayMillis = if (wait == Long.MAX_VALUE) 30_000L else wait.coerceIn(500L, 12L * 60 * 60 * 1000)
        deadlineHandler.postDelayed(deadlineTick, delayMillis)
    }
    private var inAppBannerText by mutableStateOf<String?>(null)
    private lateinit var biometricLock: AndroidBiometricLock
    private lateinit var themePreference: AndroidThemePreference
    private var themeChoice by mutableStateOf(ThemeChoice.System)
    private var appLockAvailable by mutableStateOf(false)
    private var appLockEnabled by mutableStateOf(false)
    private var appUnlocked by mutableStateOf(true)
    private var authenticationInProgress = false
    private var appLockMessage by mutableStateOf<String?>(null)
    private lateinit var audioRecorder: AndroidLocalAudioRecorder
    private var speechEngine: SpeechEngine? = null
    private var speechStatus by mutableStateOf<String?>(null)
    private var speechRecording by mutableStateOf(false)
    private var speechPartial by mutableStateOf<String?>(null)
    private var speechResult by mutableStateOf<SpeechFinal?>(null)
    private var speechTranscribes by mutableStateOf(false)
    private var speechAmplitude by mutableStateOf(0f)
    private var speechFinalDelivered = false
    private var audioPlaybackStatus by mutableStateOf<String?>(null)
    private var audioPlaying by mutableStateOf(false)
    private lateinit var onnxModelManager: AndroidOnnxModelManager
    private var onnxAsrState by mutableStateOf(OnnxAsrState())
    private var mediaPlayer: MediaPlayer? = null
    private var exportPassword by mutableStateOf("")
    private var exportStatus by mutableStateOf<String?>(null)
    private var exportPasswordCopied by mutableStateOf(false)
    private var lastExportFile: File? = null
    private var deleteStatus by mutableStateOf<String?>(null)
    private var reminderAllows by mutableStateOf(ReminderToggle.entries.associateWith { true })
    private var updatePhase by mutableStateOf<UpdatePhase>(UpdatePhase.Idle)
    private var showUpdatePrompt by mutableStateOf(false)
    private var pendingAudioAction: (() -> Unit)? = null
    private lateinit var anchorStore: AndroidEncryptedProbeStore
    private lateinit var backgroundTimer: AndroidBackgroundTimer
    private lateinit var microActionTimer: AndroidBackgroundTimer
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != MICROPHONE_REQUEST_CODE) return
        val action = pendingAudioAction
        pendingAudioAction = null
        if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            action?.invoke()
        } else {
            speechStatus = "未获得麦克风权限。"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        biometricLock = AndroidBiometricLock(this)
        themePreference = AndroidThemePreference(this)
        themeChoice = themePreference.choice
        syncAnchorNightMode(this, themeChoice)
        // 每次启动补挂提醒：忧虑专场默认开启（未主动关即装填），其余按既有计划重挂。
        AndroidReminderScheduler(this).rescheduleAll()
        refreshNotificationPermission()
        audioRecorder = AndroidLocalAudioRecorder(this)
        onnxModelManager = AndroidOnnxModelManager(this)
        onnxAsrState = onnxModelManager.currentState()
        onnxModelManager.onStateChanged = { state ->
            onnxAsrState = state
            if (state.enabled && state.modelReady && !SherpaOnnxRuntime.isWarm()) {
                warmUpOnnxEngine()
            }
        }
        val databaseKey = AndroidDatabaseKey.getOrCreate(this, DATABASE_NAME)
        anchorStore = AndroidEncryptedProbeStore(this, DATABASE_NAME, databaseKey)
        backgroundTimer = AndroidBackgroundTimer(this)
        microActionTimer = AndroidBackgroundTimer(this, BackgroundTimerKind.MicroAction)
        refreshExactAlarm()
        exactAlarmPromptAsked = exactAlarmPrefs().getBoolean(EXACT_ALARM_ASKED, false)
        armDeadlineWatch()
        databaseKey.fill(0)
        appLockAvailable = biometricLock.isAvailable()
        appLockEnabled = biometricLock.enabled
        appUnlocked = !appLockEnabled
        reminderAllows = loadReminderAllows()
        updatePrivacyShield()
        if (BuildConfig.UPDATE_ENABLED) {
            AnchorUpdateService.ensureStarted(
                localVersionCode = BuildConfig.VERSION_CODE,
                onPhase = { phase ->
                    updatePhase = phase
                    showUpdatePrompt = AnchorUpdateService.shouldPrompt()
                },
                openUrl = ::openUpdateUrl,
            )
        }
        setContent {
            val view = LocalView.current
            val darkBars = themeChoice.useDark(isNightMode())
            SideEffect { applyAnchorSystemBars(window, view, darkBars) }
            App(
                anchorStore = anchorStore,
                debugTools = BuildConfig.DEBUG,
                onSeedRhythmDemo = {
                    val before = anchorStore.rhythmEntries().map { it.id }.toSet()
                    seedRhythmDemoData(anchorStore, System.currentTimeMillis()) { AndroidWorrySessionClock.localMinuteOfDay(it) }
                    val after = anchorStore.rhythmEntries().map { it.id }.toSet()
                    saveDemoRhythmIds(after - before)
                },
                onClearRhythmDemo = {
                    anchorStore.deleteRhythmEntries(loadDemoRhythmIds())
                    clearDemoRhythmIds()
                },
                inAppBannerText = inAppBannerText,
                onDismissInAppBanner = { inAppBannerText = null },
                appLocked = appLockEnabled && !appUnlocked,
                appLockAvailable = appLockAvailable,
                appLockEnabled = appLockEnabled,
                appLockMessage = appLockMessage,
                onUnlock = { authenticate() },
                onToggleAppLock = {
                    if (appLockEnabled) {
                        biometricLock.enabled = false
                        appLockEnabled = false
                        appUnlocked = true
                        appLockMessage = null
                        updatePrivacyShield()
                    } else {
                        authenticate(enableAfterSuccess = true)
                    }
                },
                speechStatus = speechStatus,
                speechRecording = speechRecording,
                speechPartial = speechPartial,
                speechResult = speechResult,
                speechTranscribes = speechTranscribes,
                speechAmplitude = speechAmplitude,
                onCaptureWorrySpeech = { beginWorrySpeech() },
                onFinalizeWorrySpeech = { finalizeWorrySpeech() },
                onDiscardWorrySpeech = { name -> discardWorrySpeech(name) },
                onConsumeWorrySpeechResult = {
                    speechResult = null
                    speechStatus = null
                },
                audioPlaybackStatus = audioPlaybackStatus,
                audioPlaying = audioPlaying,
                onStopWorryAudioPlayback = { stopAudioPlayback() },
                onTranscriptEdited = { recordTranscriptEditSession() },
                speechDownloading = onnxAsrState.downloading,
                speechDownloadProgress = onnxAsrState.downloadProgress,
                speechReady = onnxAsrState.modelReady,
                speechEnabled = onnxAsrState.enabled,
                speechError = onnxAsrState.error,
                onSpeechDownload = { onnxModelManager.startDownload() },
                onSpeechCancelDownload = { onnxModelManager.cancelDownload() },
                onSpeechToggle = { enabled ->
                    onnxModelManager.setEnabled(enabled)
                    onnxAsrState = onnxModelManager.currentState()
                    if (enabled) warmUpOnnxEngine() else SherpaOnnxRuntime.release()
                },
                onSpeechDelete = {
                    onnxModelManager.deleteModel()
                    SherpaOnnxRuntime.release()
                    onnxAsrState = onnxModelManager.currentState()
                },
                onPlayWorryAudio = { playWorryAudio(it) },
                exportPassword = exportPassword,
                onExportPasswordChange = { exportPassword = it },
                exportStatus = exportStatus,
                exportPasswordCopied = exportPasswordCopied,
                onTestEncryptedExport = { testEncryptedExport() },
                onShareExport = { shareLastExport() },
                onSaveExportToFolder = {
                    saveExportToFolder.launch(lastExportFile?.name ?: "anchor-export.anchor")
                },
                onDismissExportComplete = {
                    exportStatus = null
                    exportPasswordCopied = false
                },
                onImportEncryptedExport = {
                    exportStatus = null
                    importBackup.launch(arrayOf("application/vnd.anchor.encrypted-export", "application/octet-stream"))
                },
                deleteStatus = deleteStatus,
                onDeleteAllData = { deleteAllLocalData() },
                reminderAllows = reminderAllows,
                onToggleReminder = { toggleReminder(it) },
                notificationPermission = notificationPermission,
                onRequestNotificationPermission = { requestNotificationPermission() },
                exactAlarmGranted = exactAlarmGranted,
                onRequestExactAlarm = { requestExactAlarm() },
                showExactAlarmPrompt = !exactAlarmGranted && !exactAlarmPromptAsked,
                onExactAlarmPromptShown = {
                    exactAlarmPromptAsked = true
                    exactAlarmPrefs().edit().putBoolean(EXACT_ALARM_ASKED, true).apply()
                },
                onPostponeReassessment = { AndroidReminderScheduler(this).postponeReassessment() },
                nowMillis = System::currentTimeMillis,
                waveClockMillis = SystemClock::elapsedRealtime,
                onStartWaveTimer = {
                    backgroundTimer.schedule(it)
                    armDeadlineWatch()
                },
                waveRemainingMillis = backgroundTimer::remainingMillis,
                isWorrySessionOpen = { AndroidWorrySessionClock.isOpen() },
                nextWorrySessionMillis = { AndroidWorrySessionClock.nextSessionMillis() },
                nextWorrySessionLabel = { AndroidWorrySessionClock.nextSessionLabel() },
                onStartMicroActionTimer = {
                    microActionTimer.schedule(it)
                    armDeadlineWatch()
                },
                onCancelMicroActionTimer = { microActionTimer.cancel() },
                formatLocalTime = { AndroidWorrySessionClock.formatLocalTime(it) },
                formatLocalStamp = { AndroidWorrySessionClock.formatLocalStamp(it) },
                localMinuteOfDay = { AndroidWorrySessionClock.localMinuteOfDay(it) },
                installedVersionName = BuildConfig.VERSION_NAME,
                updateCheckAvailable = BuildConfig.UPDATE_ENABLED,
                updatePhase = updatePhase,
                showUpdatePrompt = showUpdatePrompt,
                onCheckUpdate = {
                    if (BuildConfig.UPDATE_ENABLED) AnchorUpdateService.check()
                },
                onDismissUpdate = {
                    AnchorUpdateService.dismiss()
                    showUpdatePrompt = false
                },
                onOpenUpdate = {
                    AnchorUpdateService.openDownload()
                    AnchorUpdateService.dismiss()
                    showUpdatePrompt = false
                },
                themeChoice = themeChoice,
                onThemeChoice = { choice ->
                    themeChoice = choice
                    themePreference.choice = choice
                    syncAnchorNightMode(this, choice)
                },
                onDial = ::dialNumber,
            )
        }
    }

    private fun dialNumber(number: String) {
        val tel = phoneTelUri(number) ?: return
        try {
            startActivity(Intent(Intent.ACTION_DIAL, Uri.parse(tel)))
        } catch (_: ActivityNotFoundException) {
            // The number stays on the page.
        }
    }

    private fun isNightMode(): Boolean =
        (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    private var onnxStartupWarmupArmed = false

    override fun onResume() {
        super.onResume()
        if (!onnxStartupWarmupArmed && onnxModelManager.enabled) {
            onnxStartupWarmupArmed = true
            Handler(Looper.getMainLooper()).postDelayed({ warmUpOnnxEngine() }, 5_000)
        }
        refreshNotificationPermission()
        refreshExactAlarm()
        backgroundTimer.promoteToExactAlarm()
        microActionTimer.promoteToExactAlarm()
        backgroundTimer.deliverIfDue()
        microActionTimer.deliverIfDue()
        armDeadlineWatch()
        inAppBannerText = when {
            backgroundTimer.consumePendingInAppNotice() ->
                "它自己退了。你没有掐掉它，它也会走。"
            microActionTimer.consumePendingInAppNotice() ->
                "五分钟到了。停在这里，也算完成了一次尝试。"
            else -> AndroidReminderScheduler(this).consumePendingNotice()
        }
        if (appLockEnabled && !appUnlocked) authenticate()
        anchorStore.assessments().maxOfOrNull { it.completedAtMillis }?.let { lastAt ->
            AndroidReminderScheduler(this).scheduleReassessment(lastAt)
        }
    }

    override fun onStop() {
        super.onStop()
        destroySpeechEngine()
        stopAudioPlayback()
        if (speechRecording) {
            stopRecording()
            speechRecording = false
        }
        speechPartial = null
        speechAmplitude = 0f
        speechFinalDelivered = true
        if (appLockEnabled) appUnlocked = false
    }

    override fun onDestroy() {
        deadlineHandler.removeCallbacks(deadlineTick)
        destroySpeechEngine()
        stopAudioPlayback()
        anchorStore.close()
        super.onDestroy()
    }

    private fun openUpdateUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: ActivityNotFoundException) {
        }
    }

    private fun authenticate(enableAfterSuccess: Boolean = false) {
        if (authenticationInProgress) return
        authenticationInProgress = true
        appLockMessage = null
        biometricLock.authenticate(
            onSuccess = {
                authenticationInProgress = false
                if (enableAfterSuccess) {
                    biometricLock.enabled = true
                    appLockEnabled = true
                    updatePrivacyShield()
                }
                appUnlocked = true
                appLockMessage = null
            },
            onError = { message ->
                authenticationInProgress = false
                appLockMessage = message
            },
        )
    }

    private fun beginWorrySpeech() = withMicrophonePermission {
        if (speechRecording) return@withMicrophonePermission
        speechPartial = null
        speechAmplitude = 0f
        speechResult = null
        speechFinalDelivered = false
        val engine = createSpeechEngine(this)
        if (engine == null) {
            speechTranscribes = false
            startRecording("当前设备没有可用的转写引擎，已改为纯录音。")
            return@withMicrophonePermission
        }
        speechEngine = engine
        speechTranscribes = true
        speechRecording = true
        speechStatus = "请说一句话。识别只在设备上进行。"
        engine.start(
            onPartial = { text -> speechPartial = text },
            onAmplitude = { value -> speechAmplitude = value },
            onFinal = onFinal@{ final ->
                // 空文本也必须交付：否则 speechRecording 不复位，状态机卡死在「录音中」。
                fun deliver(text: String?) {
                    deliverSpeechFinal(
                        SpeechFinal(text = text, audioFileName = final.audioFile?.name),
                        decodeLoad = final.decodeLoad,
                    )
                }
                if (onnxModelManager.punctReady() && !final.text.isNullOrBlank()) {
                    // L1：CT-transformer 文本级标点（3000 字亚秒~秒级），推理在后台执行器排队。
                    speechStatus = "正在整理标点…"
                    SherpaOnnxRuntime.addPunctuation(this, final.text) { punctuated ->
                        deliver((punctuated ?: final.text).takeIf { it.isNotBlank() })
                    }
                } else if (final.segments.isNotEmpty()) {
                    // L0 标点：按端点分段连接（段间「，」末段「。」）。
                    deliver(
                        punctuateTranscript(final.segments).takeIf { it.isNotBlank() }
                            ?: final.text?.takeIf { it.isNotBlank() },
                    )
                } else {
                    deliver(final.text?.takeIf { it.isNotBlank() })
                }
            },
            onError = { reason ->
                // 转写引擎失败（模型加载、麦克风被占等）：落到纯录音兜底，文案说明原因。
                if (speechRecording) {
                    destroySpeechEngine()
                    startRecording("$reason 已改为纯录音。")
                }
            },
        )
    }

    private fun finalizeWorrySpeech() {
        val engine = speechEngine
        if (engine != null) {
            engine.stop()
            return
        }
        if (speechRecording) {
            // 纯录音兜底：停止即产出（保留既有"停止并保存录音"语义，由 UI 直接封存）。
            deliverSpeechFinal(SpeechFinal(text = null, audioFileName = stopRecording()))
        }
    }

    private fun deliverSpeechFinal(final: SpeechFinal, decodeLoad: Double? = null) {
        if (speechFinalDelivered) return
        speechFinalDelivered = true
        speechRecording = false
        speechPartial = null
        speechAmplitude = 0f
        speechResult = final
        val loadNote = decodeLoad?.let { " 解码负载 %.2f。".format(it) } ?: ""
        speechStatus = when {
            !final.text.isNullOrBlank() -> "已完成端侧识别。$loadNote"
            final.audioFileName != null -> "本地录音已保存。"
            else -> "没有识别到内容。"
        }
        destroySpeechEngine()
    }

    /** 取消语音输入/放弃未封存结果：[pendingAudioName] 是 UI 侧尚未封存的录音文件。 */
    private fun discardWorrySpeech(pendingAudioName: String?) {
        destroySpeechEngine()
        stopAudioPlayback()
        if (speechRecording) {
            speechRecording = false
            stopRecording()?.let { name -> File(filesDir, "voice-notes/$name").delete() }
        }
        pendingAudioName?.let { name ->
            File(filesDir, "voice-notes/$name").takeIf { it.isFile }?.delete()
        }
        speechResult = null
        speechPartial = null
        speechAmplitude = 0f
        speechFinalDelivered = true
    }

    private fun warmUpOnnxEngine() {
        if (!onnxModelManager.enabled || !onnxModelManager.modelReady()) return
        SherpaOnnxRuntime.warmUp(this) { }
    }

    /** 挂卡/保险箱里的修正会话计数：转写回填后用户编辑过文本即 +1。 */
    fun recordTranscriptEditSession() {
        val prefs = getSharedPreferences("anchor-transcript-edits", MODE_PRIVATE)
        val count = prefs.getInt("count", 0) + 1
        prefs.edit().putInt("count", count).apply()
        if (count == 3 || count == 10) {
            inAppBannerText = "识别准确率不满意？可在 我的 → 语音识别 获取高精度语音包（202MB，含标点，仅本机运行）"
        }
    }

    private fun destroySpeechEngine() {
        speechEngine?.destroy()
        speechEngine = null
    }

    private fun startRecording(message: String) {
        runCatching { audioRecorder.start() }
            .onSuccess {
                speechRecording = true
                speechStatus = message
            }
            .onFailure {
                speechStatus = "无法开始本地录音：${it::class.simpleName} ${it.message.orEmpty()}"
            }
    }

    private fun stopRecording(): String? {
        val file = audioRecorder.stop()
        speechRecording = false
        speechStatus = if (file == null) {
            "录音时间太短，未保留文件。"
        } else {
            "本地录音已保存：${file.name}（${file.length()} 字节）"
        }
        return file?.name
    }

    private fun playWorryAudio(fileName: String) {
        if (fileName != File(fileName).name) {
            audioPlaybackStatus = "录音文件名无效。"
            return
        }
        val file = File(filesDir, "voice-notes/$fileName")
        if (!file.isFile) {
            audioPlaybackStatus = "本地录音已不存在。"
            return
        }
        stopAudioPlayback()
        runCatching {
            MediaPlayer().also { player ->
                mediaPlayer = player
                player.setDataSource(file.absolutePath)
                player.setOnCompletionListener {
                    stopAudioPlayback()
                    audioPlaybackStatus = "播放完成。"
                }
                player.prepare()
                player.start()
                audioPlaying = true
            }
        }.onSuccess {
            audioPlaybackStatus = "正在播放本地录音。"
        }.onFailure {
            stopAudioPlayback()
            audioPlaybackStatus = "无法播放本地录音。"
        }
    }

    private fun stopAudioPlayback() {
        mediaPlayer?.release()
        mediaPlayer = null
        audioPlaying = false
    }

    private fun refreshNotificationPermission() {
        notificationPermission = currentNotificationPermission()
    }

    private fun refreshExactAlarm() {
        exactAlarmGranted = backgroundTimer.canScheduleExactAlarms()
    }

    private fun armDeadlineWatch() {
        deadlineHandler.removeCallbacks(deadlineTick)
        deadlineHandler.post(deadlineTick)
    }

    private fun requestExactAlarm() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || backgroundTimer.canScheduleExactAlarms()) {
            exactAlarmGranted = true
            return
        }
        val request = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            .setData(Uri.parse("package:$packageName"))
        try {
            startActivity(request)
        } catch (_: ActivityNotFoundException) {
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)),
            )
        }
    }

    private fun currentNotificationPermission(): NotificationPermission {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return NotificationPermission.Granted
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) return NotificationPermission.Granted
        val asked = notificationPrefs().getBoolean(NOTIFICATION_ASKED, false)
        val canAskAgain = ActivityCompat.shouldShowRequestPermissionRationale(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        )
        return if (asked && !canAskAgain) {
            NotificationPermission.Blocked
        } else {
            NotificationPermission.Requestable
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            notificationPermission = NotificationPermission.Granted
            return
        }
        if (currentNotificationPermission() == NotificationPermission.Blocked) {
            openNotificationSettings()
            return
        }
        requestPostNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun openNotificationSettings() {
        val settings = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        try {
            startActivity(settings)
        } catch (_: ActivityNotFoundException) {
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)),
            )
        }
    }

    private fun notificationPrefs() =
        getSharedPreferences(NOTIFICATION_PREFERENCES, MODE_PRIVATE)

    private fun exactAlarmPrefs() =
        getSharedPreferences(EXACT_ALARM_PREFERENCES, MODE_PRIVATE)

    private fun withMicrophonePermission(action: () -> Unit) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            action()
        } else {
            pendingAudioAction = action
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                MICROPHONE_REQUEST_CODE,
            )
        }
    }

    private fun testEncryptedExport() {
        val password = exportPassword.toCharArray()
        runCatching {
            val exporter = AndroidEncryptedExport(this)
            val localExport = buildLocalExport(anchorStore)
            val file = exporter.create(
                password = password,
                json = localExport.json,
                csv = localExport.csv,
                audio = collectWorryAudio(this, anchorStore),
            )
            exportPasswordCopied = copyExportPassword(password)
            lastExportFile = file
            exportPassword = ""
            exportStatus = "已生成 ${file.name}；密码未保存，请另行保管。"
        }.onFailure {
            lastExportFile = null
            exportPasswordCopied = false
            exportPassword = ""
            exportStatus = it.message ?: "无法生成加密导出文件。"
        }
        password.fill('\u0000')
    }

    private fun saveDemoRhythmIds(ids: Set<Long>) {
        getSharedPreferences("m0-debug", MODE_PRIVATE)
            .edit()
            .putStringSet("rhythm-demo-ids", ids.map(Long::toString).toSet())
            .apply()
    }

    private fun loadDemoRhythmIds(): Set<Long> =
        getSharedPreferences("m0-debug", MODE_PRIVATE)
            .getStringSet("rhythm-demo-ids", emptySet())
            .orEmpty()
            .mapNotNull { it.toLongOrNull() }
            .toSet()

    private fun clearDemoRhythmIds() {
        getSharedPreferences("m0-debug", MODE_PRIVATE).edit().remove("rhythm-demo-ids").apply()
    }

    private fun copyLastExportTo(uri: Uri) {
        val file = lastExportFile ?: return
        runCatching {
            contentResolver.openOutputStream(uri)?.use { out ->
                file.inputStream().use { input -> input.copyTo(out) }
            } ?: error("无法写入所选位置")
            exportStatus = "已生成 ${file.name}；已保存到所选位置。密码未保存，请另行保管。"
        }.onFailure {
            exportStatus = it.message ?: "无法保存到所选位置。"
        }
    }

    private fun shareLastExport() {
        val file = lastExportFile ?: return
        startActivity(Intent.createChooser(AndroidEncryptedExport(this).shareIntent(file), "分享加密导出"))
    }

    private fun copyExportPassword(password: CharArray): Boolean = runCatching {
        val clipboard = getSystemService(android.content.ClipboardManager::class.java)
        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("锚点导出密码", String(password)))
    }.isSuccess

    private fun restoreEncryptedExport(uri: android.net.Uri) {
        val password = exportPassword.toCharArray()
        val temporary = File.createTempFile("anchor-import-", ".anchor", cacheDir)
        runCatching {
            contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input) { "无法读取所选备份。" }
                temporary.outputStream().use(input::copyTo)
            }
            val decrypted = AndroidEncryptedExport(this).decrypt(temporary, password)
            val restoredNames = decrypted.audio.keys.mapNotNull(::worryAudioExportName).toSet()
            anchorStore.restoreFromJson(decrypted.json, restoredNames)
            // DB 已替换后才写录音；写盘失败时记录已恢复但音频可能不全，必须让用户知道。
            runCatching { replaceRestoredWorryAudio(this, decrypted.audio) }
                .onFailure { throw IllegalStateException("录音写入未完成", it) }
        }.onSuccess {
            exportPassword = ""
            exportStatus = "备份恢复成功。"
            recreate()
        }.onFailure {
            exportPassword = ""
            exportStatus = when (it) {
                is javax.crypto.AEADBadTagException -> "密码错误或备份已损坏。"
                is IllegalStateException -> if (it.message.orEmpty().contains("录音")) "记录已恢复，但部分录音可能未写入，请检查。" else it.message ?: "无法恢复备份。"
                else -> it.message ?: "无法恢复备份。"
            }
        }
        password.fill('\u0000')
        temporary.delete()
    }

    private fun deleteAllLocalData() {
        runCatching {
            pendingAudioAction = null
            destroySpeechEngine()
            stopAudioPlayback()
            if (speechRecording) stopRecording()
            speechRecording = false
            speechPartial = null
        speechAmplitude = 0f
            speechResult = null
            speechFinalDelivered = true
            backgroundTimer.cancel()
            microActionTimer.cancel()
            AndroidReminderScheduler(this).cancelAll()
            getSystemService(NotificationManager::class.java).cancelAll()
            // 次要清理尽力而为：失败不中断（缓存残留无碍），绝不能出现"报错但已清空"的状态。
            runCatching {
                check(File(cacheDir, "exports").let { !it.exists() || it.deleteRecursively() }) {
                    "无法删除导出缓存。"
                }
            }
            biometricLock.enabled = false
            // 不可逆的清库放最后：只有真正清空成功才会走到重建界面。
            anchorStore.clearAllData()
        }.onSuccess {
            appLockEnabled = false
            appUnlocked = true
            exportPassword = ""
            exportStatus = null
            exportPasswordCopied = false
            lastExportFile = null
            deleteStatus = null
            inAppBannerText = null
            updatePrivacyShield()
            recreate()
        }.onFailure {
            deleteStatus = it.message ?: "无法彻底删除本地数据，请重试。"
        }
    }

    private fun loadReminderAllows(): Map<ReminderToggle, Boolean> {
        val scheduler = AndroidReminderScheduler(this)
        return mapOf(
            ReminderToggle.WorrySession to scheduler.allows(ReminderKind.WORRY_SESSION),
            ReminderToggle.CrisisCare to (
                scheduler.allows(ReminderKind.CRISIS_24H) || scheduler.allows(ReminderKind.CRISIS_72H)
                ),
            ReminderToggle.Reassessment to scheduler.allows(ReminderKind.REASSESSMENT),
            ReminderToggle.MedicalWaiting to scheduler.allows(ReminderKind.MEDICAL_WAITING),
        )
    }

    private fun toggleReminder(toggle: ReminderToggle) {
        val scheduler = AndroidReminderScheduler(this)
        val next = !(reminderAllows[toggle] ?: true)
        when (toggle) {
            ReminderToggle.WorrySession -> scheduler.setAllows(ReminderKind.WORRY_SESSION, next)
            ReminderToggle.CrisisCare -> {
                scheduler.setAllows(ReminderKind.CRISIS_24H, next)
                scheduler.setAllows(ReminderKind.CRISIS_72H, next)
            }
            ReminderToggle.Reassessment -> scheduler.setAllows(ReminderKind.REASSESSMENT, next)
            ReminderToggle.MedicalWaiting -> scheduler.setAllows(ReminderKind.MEDICAL_WAITING, next)
        }
        reminderAllows = reminderAllows + (toggle to next)
    }

    private fun updatePrivacyShield() {
        applyPrivacyShield(window, appLockEnabled)
    }

    private companion object {
        const val MICROPHONE_REQUEST_CODE = 7
        const val DATABASE_NAME = "anchor.db"
        const val NOTIFICATION_PREFERENCES = "anchor-notification"
        const val NOTIFICATION_ASKED = "asked"
        const val EXACT_ALARM_PREFERENCES = "anchor-exact-alarm"
        const val EXACT_ALARM_ASKED = "asked"
    }
}
