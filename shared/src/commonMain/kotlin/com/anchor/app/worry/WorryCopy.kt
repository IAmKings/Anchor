package com.anchor.app.worry

internal const val hangSheetTitle = "把这个念头放进保险箱"
internal const val hangFieldHint = "说话，或写一句…"
internal const val hangSealLabel = "封存"
internal const val hangSpeechLabel = "语音速记"
internal const val hangSpeechStopLabel = "停止并保存录音"
internal const val hangDismissKnown = "知道了"
internal const val hangDismissCancel = "取消"

internal const val vaultTitle = "忧虑保险箱"
internal const val vaultLockedCaption = "念头已经在纸上了"
internal const val vaultAskOpenNowLabel = "现在就想处理"
internal const val vaultConfirmOpenHint = "现在开箱后，请尽量为每张卡做一个选择，优先找“明天能做的一个动作”。"
internal const val vaultConfirmOpenLabel = "确认开箱"
internal const val vaultWaitForSessionLabel = "等到专场"
internal const val vaultOpenCaption = "专场已开启"
internal const val vaultOpenBreath = "深呼吸，我们开始整理。"
internal const val vaultEmptyPending = "今天没有待处理卡片。"
internal const val vaultAcceptedQuote = "在这里，忧虑是被受理的，而非被压抑。"
internal const val vaultStartFirstLabel = "开始处理第一项"
internal const val vaultAudioHangLabel = "语音挂卡"
internal const val vaultBackToOverview = "回到开箱"
internal const val vaultProcessPrompt = "请选择处理方式"
internal const val vaultChooseActionLabel = "明天能做的一个动作"
internal const val vaultUnsolvableLabel = "暂时无解"
internal const val vaultDismissLabel = "已不再重要"
internal const val vaultPlayAudioLabel = "播放本地录音"

internal fun vaultQuotedCard(content: String): String =
    if (content.isBlank()) "“$vaultAudioHangLabel”" else "“$content”"

internal fun vaultUnsolvableHint(nextLabel: String): String = "${nextLabel} 前无需再想。"

internal fun vaultSealedMessage(sessionOpen: Boolean, nextLabel: String): String =
    if (sessionOpen) "已存入保险箱，可以在当前专场处理。"
    else "已存入保险箱，${nextLabel} 统一开箱。"

internal fun vaultRuminationMessage(nextLabel: String): String =
    "这个已经在纸上了，${nextLabel} 再说。"

internal fun vaultPendingSummary(count: Int): String =
    if (count <= 0) "保险箱是空的。念头来了再挂一张。"
    else "保险箱里有 ${count} 张待处理卡片。不会显示内容，也不需要现在解决。"

internal fun worryRecordedLabel(sealedAtMillis: Long, nowMillis: Long): String {
    val minutes = ((nowMillis - sealedAtMillis).coerceAtLeast(0) / 60_000)
    return when {
        minutes < 1L -> "刚刚挂上"
        minutes < 60L -> "记录于 ${minutes} 分钟前"
        minutes < 24L * 60L -> "记录于 ${minutes / 60} 小时前"
        else -> "记录于 ${minutes / (24 * 60)} 天前"
    }
}

internal fun vaultDoneSummary(count: Int): String =
    if (count <= 0) "这一刻没有需要处理的卡片。"
    else "今日受理了 ${count} 项忧虑"
