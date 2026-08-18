package com.anchor.app.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BackgroundTimerProbeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val kind = intent.getStringExtra(EXTRA_KIND)
            ?.let { runCatching { BackgroundTimerKind.valueOf(it) }.getOrNull() }
            ?: BackgroundTimerKind.Wave
        val timer = AndroidBackgroundTimer(context, kind)
        resultData = when (intent.action) {
            ACTION_SCHEDULE -> "exact=${timer.schedule(intent.getLongExtra(EXTRA_DURATION, 2_000L))}"
            ACTION_STATUS -> "fired=${timer.hasFired()},remaining=${timer.remainingMillis()}," +
                "pendingInAppNotice=${timer.hasPendingInAppNotice()}"
            ACTION_CANCEL -> {
                timer.cancel()
                "cancelled"
            }
            ACTION_FIRE -> {
                BackgroundTimerReceiver().onReceive(
                    context,
                    Intent(context, BackgroundTimerReceiver::class.java).putExtra(TIMER_KIND, kind.name),
                )
                "fired"
            }
            else -> "unknown-action"
        }
    }

    companion object {
        const val ACTION_SCHEDULE = "com.anchor.app.timer.SCHEDULE_PROBE"
        const val ACTION_STATUS = "com.anchor.app.timer.STATUS_PROBE"
        const val ACTION_CANCEL = "com.anchor.app.timer.CANCEL_PROBE"
        const val ACTION_FIRE = "com.anchor.app.timer.FIRE_PROBE"
        const val EXTRA_DURATION = "durationMillis"
        const val EXTRA_KIND = "timerKind"
    }
}
