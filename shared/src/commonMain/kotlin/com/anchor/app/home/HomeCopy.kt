package com.anchor.app.home

internal const val homeMildUseDisclaimer = "应用仅适用于轻度调节"
internal const val homeRhythmKicker = "晨间节律"
internal const val homeRhythmEmptyTitle = "见光记录"
internal const val homeWorryLabel = "忧虑保险箱"
internal const val homeWorryCountUnit = "张卡"

internal fun homeWorryBody(pendingCount: Int, sessionOpen: Boolean, nextLabel: String): String = when {
    pendingCount <= 0 -> "点这里把念头放进去"
    sessionOpen -> "${pendingCount}张卡，专场已开箱"
    else -> "${pendingCount}张卡，${nextLabel}开箱"
}

internal fun homeWorryCountText(pendingCount: Int): String? =
    pendingCount.takeIf { it > 0 }?.toString()

internal fun homeWorryUnlockLine(sessionOpen: Boolean, nextLabel: String): String =
    if (sessionOpen) "专场已开箱" else "${nextLabel}开箱"

internal fun homeMicroActionTitle(running: Boolean): String =
    if (running) "微行动 · 进行中" else "微行动"

internal fun homeMicroActionBody(title: String?): String =
    title ?: "选一个低到无需说服自己的动作"

internal fun homeMicroActionActionLabel(hasTitle: Boolean, running: Boolean, completed: Boolean): String = when {
    running -> ""
    completed -> "已记下"
    hasTitle -> "开始 5 分钟"
    else -> "选择"
}
