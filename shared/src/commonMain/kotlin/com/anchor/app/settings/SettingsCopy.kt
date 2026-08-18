package com.anchor.app.settings

enum class ReminderToggle {
    WorrySession,
    CrisisCare,
    Reassessment,
    MedicalWaiting,
}

internal fun reminderToggleTitle(toggle: ReminderToggle): String = when (toggle) {
    ReminderToggle.WorrySession -> "忧虑专场"
    ReminderToggle.CrisisCare -> "危机后关怀"
    ReminderToggle.Reassessment -> "复评邀请"
    ReminderToggle.MedicalWaiting -> "就医等待期关怀"
}

internal const val exportCompleteTitle = "文件已加密生成"
internal const val exportCompleteBody = "你可以通过系统分享功能将其发送给医生，或保存到本地。"
internal const val exportPasswordCopiedTitle = "提取密码已复制"
internal const val exportPasswordCopiedBody = "密码已存入剪贴板。为保护数据隐私，建议在分享文件时，将密码通过其他安全渠道单独发送。"
internal const val exportPasswordRememberTitle = "请自行记下密码"
internal const val exportPasswordRememberBody = "密码没有保存在应用里。分享文件时，请通过其他安全渠道单独发送密码。"
internal const val exportShareLabel = "点击分享"
internal const val exportHomeLabel = "返回主页"

internal fun isExportSuccess(status: String?): Boolean =
    status?.startsWith("已生成") == true

internal fun exportFileLabel(status: String?): String? {
    val prefix = "已生成 "
    if (status == null || !status.startsWith(prefix)) return null
    return status.removePrefix(prefix).substringBefore("；").ifBlank { null }
}

internal fun reminderToggleDetail(toggle: ReminderToggle): String = when (toggle) {
    ReminderToggle.WorrySession -> "专场开始前 5 分钟。锁屏只显示「锚点」。"
    ReminderToggle.CrisisCare -> "拦截后 24 小时和 72 小时各一条，不连续追问。"
    ReminderToggle.Reassessment -> "距上次评估两周时邀请一次，可以关掉。"
    ReminderToggle.MedicalWaiting -> "屏蔽状态下每 7 天一条，提醒资源还在。"
}
