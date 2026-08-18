package com.anchor.app.worry

internal const val hangSheetTitle = "把这个念头放进保险箱"
internal const val hangFieldHint = "说话，或写一句…"
internal const val hangSealLabel = "封存"

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
