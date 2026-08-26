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
internal const val settingsBackLabel = "返回"
internal const val settingsTitle = "我的"
internal const val settingsIntro = "管理应用锁、四类提醒和本地数据。"
internal const val settingsPrivacyTitle = "隐私与安全"
internal const val settingsLockTitle = "应用锁"
internal const val settingsLockAvailable = "开启后，每次打开都需要验证。"
internal const val settingsLockUnavailable = "当前设备暂不支持生物识别或设备凭据。"
internal const val settingsLockscreenNote = "锁屏通知只显示「锚点」，不显示正文。"
internal const val settingsRemindersTitle = "提醒"
internal const val settingsRemindersIntro = "只有这四类。关掉后不追问、不挽留。"
internal const val settingsDataTitle = "数据管理"
internal const val settingsExportTitle = "加密导出"
internal const val settingsExportBody = "导出 JSON 与 CSV。密码不会保存，遗失后无法恢复。"
internal const val settingsExportPasswordLabel = "导出密码（至少 8 个字符）"
internal const val settingsExportAction = "生成加密导出文件"
internal const val settingsImportConfirm = "恢复会替换本机现有记录，确定继续吗？"
internal const val settingsImportConfirmAction = "确认并选择备份"
internal const val settingsCancel = "取消"
internal const val settingsImportAction = "选择加密备份并恢复"
internal const val settingsAudioNotRestored = "新备份会带上本地录音。更早的备份恢复后，录音仍会缺失。"
internal const val settingsDeleteTitle = "彻底删除所有数据"
internal const val settingsDeleteBody = "此操作不可逆。评估、记录、录音、提醒、计时、导出缓存和应用锁都会被清掉。"
internal const val settingsDeleteConfirm = "确定要彻底删除吗？删除后无法恢复。"
internal const val settingsDeleteConfirmAction = "确认彻底删除"
internal const val settingsDeleteAction = "删除全部本地数据"
internal const val settingsAboutTitle = "关于"
internal const val settingsAboutBody = "锚点只把数据留在这台设备。没有账号，也不会上传。"
internal const val settingsHelpLink = "就医与帮助入口"
internal const val settingsReassessmentLink = "复评量表"
internal const val settingsPreviewOnboarding = "预览首启评估（不保存）"
internal const val settingsPreviewReturn = "预览回归练习（不改状态）"
internal const val settingsPreviewMedical = "预览就医指南（不改地区）"
internal const val settingsPreviewRelation = "预览首页回血 / 抽干 / 耗竭（不改记录）"
internal const val settingsPreviewCrisis = "预览危机澄清（不改状态）"
internal const val settingsPreviewLock = "预览一次一件锁定（不改选择）"

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
