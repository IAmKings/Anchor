package com.anchor.app.home

internal const val homeMildUseDisclaimer = "本应用仅适用于轻度/亚临床调节。诊断与治疗请务必寻求专业医生帮助。"
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

internal const val homeWaitingBadge = "就医等待期"
internal const val homeWaitingTitle = "休息即是当下的练习"
internal const val homeWaitingPauseLine = "暂停练习不是惩罚，是防止你在未获专业支持时过度自我要求。"
internal const val homeWaitingBody =
    "暂停练习不是惩罚，是防止你在未获专业支持时过度自我要求。请把力气放在寻求专业帮助和基础休养上。"
internal const val homeWaitingSupportTitle = "寻求专业支持"
internal const val homeWaitingSupportBody = "你的状态需要专业的评估与帮助，这很正常。"
internal const val homeWaitingGuideAction = "查看就医指南"
internal const val homeWaitingGuideTitle = "就医指南"
internal const val homeWaitingGuideDetail = "了解看诊流程与准备"
internal const val homeWaitingToolsLabel = "可用工具"
internal const val homeWaitingChecklistTitle = "就医准备清单"
internal const val homeWaitingChecklistBody = "为下一次医生问诊做准备，记录近期的身体感受。"
internal const val enterMedicalWaitingLabel = "进入就医等待期"
internal const val homeWaitingJournalTitle = "事实记录"
internal const val homeWaitingJournalBody = "记录此刻能被看见的事实，暂不进行分析。"
internal const val homeWaitingReassessTitle = "再次评估"
internal const val homeWaitingReassessBody = "距上次评估已过 48 小时，可以再测一次以纠正误测。"
