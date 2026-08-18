package com.anchor.app.ui

fun formatCountdown(remainingMillis: Long): String {
    val totalSeconds = (remainingMillis / 1_000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return minutes.toString().padStart(2, '0') + ":" + seconds.toString().padStart(2, '0')
}
