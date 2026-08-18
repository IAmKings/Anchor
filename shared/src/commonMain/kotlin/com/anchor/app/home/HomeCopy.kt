package com.anchor.app.home

internal fun homeWorryBody(pendingCount: Int, sessionOpen: Boolean, nextLabel: String): String = when {
    pendingCount <= 0 -> "点这里把念头放进去"
    sessionOpen -> "${pendingCount}张卡，专场已开箱"
    else -> "${pendingCount}张卡，${nextLabel}开箱"
}

internal fun homeMicroActionTitle(running: Boolean): String =
    if (running) "微行动 · 进行中" else "微行动"

internal fun homeMicroActionBody(title: String?): String =
    title ?: "选一个低到无需说服自己的动作"

internal fun homeMicroActionActionLabel(hasTitle: Boolean, running: Boolean, completed: Boolean): String = when {
    running -> ""
    completed -> "已记下"
    hasTitle -> "开始"
    else -> "选择"
}
