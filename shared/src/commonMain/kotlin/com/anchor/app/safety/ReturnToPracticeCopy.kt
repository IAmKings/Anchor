package com.anchor.app.safety

internal const val returnToPracticeTitle = "平稳回落，欢迎回归练习"
internal const val returnToPracticeBody = "连续两次评估落在轻度范围。练习已重新打开。"
internal const val returnWaveTitle = "浪潮等待"
internal const val returnWaveDetail = "难受的时候可以待在这里。"
internal const val returnActionTitle = "微行动"
internal const val returnActionDetail = "从一个低到无需说服自己的动作开始。"
internal const val returnHomeLabel = "回到今天"
internal const val practiceReturnedBanner = "练习已重新打开。今天可以从一件最轻的事开始。"

fun recoveredToPractice(previous: SafetyState, next: SafetyState): Boolean =
    previous.mode == SafetyMode.MedicalWaiting && next.mode == SafetyMode.Normal
