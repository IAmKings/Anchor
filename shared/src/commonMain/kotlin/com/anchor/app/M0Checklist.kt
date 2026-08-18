package com.anchor.app

enum class M0Check(val label: String) {
    EncryptedStorage("加密存储"),
    LocalNotification("本地通知"),
    BackgroundTimer("后台计时"),
    OfflineSpeech("离线语音"),
    Biometrics("生物识别"),
    SecureSharing("加密分享"),
    PrivacyShield("隐私遮蔽"),
}

data class M0Checklist(private val passed: Set<M0Check> = emptySet()) {
    val completed: Int get() = passed.size
    val total: Int get() = M0Check.entries.size

    fun isPassed(check: M0Check): Boolean = check in passed

    fun markPassed(check: M0Check): M0Checklist = copy(passed = passed + check)
}
