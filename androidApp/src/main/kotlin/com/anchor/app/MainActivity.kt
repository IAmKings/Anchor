package com.anchor.app

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.app.NotificationManager
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.anchor.app.reminder.AndroidReminderScheduler
import com.anchor.app.reminder.ReminderKind
import com.anchor.app.settings.ReminderToggle
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
    private val importBackup = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(::restoreEncryptedExport)
    }
    private var inAppBannerText by mutableStateOf<String?>(null)
    private lateinit var biometricLock: AndroidBiometricLock
    private var appLockAvailable by mutableStateOf(false)
    private var appLockEnabled by mutableStateOf(false)
    private var appUnlocked by mutableStateOf(true)
    private var authenticationInProgress = false
    private var appLockMessage by mutableStateOf<String?>(null)
    private lateinit var offlineSpeech: AndroidOfflineSpeech
    private lateinit var audioRecorder: AndroidLocalAudioRecorder
    private var speechStatus by mutableStateOf<String?>(null)
    private var speechRecording by mutableStateOf(false)
    private var audioPlaybackStatus by mutableStateOf<String?>(null)
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
        offlineSpeech = AndroidOfflineSpeech(this)
        audioRecorder = AndroidLocalAudioRecorder(this)
        val databaseKey = AndroidDatabaseKey.getOrCreate(this, DATABASE_NAME)
        anchorStore = AndroidEncryptedProbeStore(this, DATABASE_NAME, databaseKey)
        backgroundTimer = AndroidBackgroundTimer(this)
        microActionTimer = AndroidBackgroundTimer(this, BackgroundTimerKind.MicroAction)
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
            App(
                anchorStore = anchorStore,
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
                onTestOfflineSpeech = { testOfflineSpeech() },
                onCaptureWorrySpeech = { onResult -> captureOfflineSpeech(onResult) },
                onStopWorryRecording = { stopRecording() },
                audioPlaybackStatus = audioPlaybackStatus,
                onPlayWorryAudio = { playWorryAudio(it) },
                onTestRecordingFallback = {
                    if (speechRecording) stopRecording() else withMicrophonePermission {
                        startRecording("正在录音；内容只保存在 App 私有目录。")
                    }
                },
                exportPassword = exportPassword,
                onExportPasswordChange = { exportPassword = it },
                exportStatus = exportStatus,
                exportPasswordCopied = exportPasswordCopied,
                onTestEncryptedExport = { testEncryptedExport() },
                onShareExport = { shareLastExport() },
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
                onPostponeReassessment = { AndroidReminderScheduler(this).postponeReassessment() },
                nowMillis = System::currentTimeMillis,
                waveClockMillis = SystemClock::elapsedRealtime,
                onStartWaveTimer = { backgroundTimer.schedule(it) },
                waveRemainingMillis = backgroundTimer::remainingMillis,
                isWorrySessionOpen = { AndroidWorrySessionClock.isOpen() },
                nextWorrySessionMillis = { AndroidWorrySessionClock.nextSessionMillis() },
                nextWorrySessionLabel = { AndroidWorrySessionClock.nextSessionLabel() },
                onStartMicroActionTimer = { microActionTimer.schedule(it) },
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
            )
        }
    }

    override fun onResume() {
        super.onResume()
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
        offlineSpeech.destroy()
        stopAudioPlayback()
        if (speechRecording) stopRecording()
        if (appLockEnabled) appUnlocked = false
    }

    override fun onDestroy() {
        offlineSpeech.destroy()
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

    private fun testOfflineSpeech() = captureOfflineSpeech()

    private fun captureOfflineSpeech(onResult: (String) -> Unit = {}) = withMicrophonePermission {
        speechStatus = "请说一句话。识别只在设备上进行。"
        offlineSpeech.start(
            onResult = { text ->
                speechStatus = "已完成端侧识别。"
                onResult(text)
            },
            onFailure = { reason -> startRecording("$reason 已切换为本地录音，请重新说一次。") },
        )
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
    }

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
            exportStatus = it.message ?: "无法生成加密导出文件。"
        }
        password.fill('\u0000')
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
            replaceRestoredWorryAudio(this, decrypted.audio)
        }.onSuccess {
            exportPassword = ""
            exportStatus = "备份恢复成功。"
            recreate()
        }.onFailure {
            exportStatus = when (it) {
                is javax.crypto.AEADBadTagException -> "密码错误或备份已损坏。"
                else -> it.message ?: "无法恢复备份。"
            }
        }
        password.fill('\u0000')
        temporary.delete()
    }

    private fun deleteAllLocalData() {
        runCatching {
            pendingAudioAction = null
            offlineSpeech.destroy()
            stopAudioPlayback()
            if (speechRecording) stopRecording()
            backgroundTimer.cancel()
            microActionTimer.cancel()
            AndroidReminderScheduler(this).cancelAll()
            getSystemService(NotificationManager::class.java).cancelAll()
            anchorStore.clearAllData()
            check(File(cacheDir, "exports").let { !it.exists() || it.deleteRecursively() }) {
                "无法删除导出缓存。"
            }
            biometricLock.enabled = false
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
    }
}
