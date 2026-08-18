package com.anchor.app.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.time.ZonedDateTime

class ReminderSchedulerProbeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val scheduler = AndroidReminderScheduler(context)
        resultData = when (intent.action) {
            ACTION_SEED -> {
                scheduler.cancelAll()
                val now = System.currentTimeMillis()
                val session = ZonedDateTime.now().plusMinutes(10)
                scheduler.scheduleWorrySession(session.hour, session.minute)
                scheduler.scheduleCrisisFollowUps(now)
                scheduler.scheduleReassessment(now)
                scheduler.scheduleMedicalWaitingCare(now)
                status(scheduler)
            }
            ACTION_RESCHEDULE -> {
                scheduler.rescheduleAll()
                status(scheduler)
            }
            ACTION_STATUS -> status(scheduler)
            ACTION_FIRE -> {
                val kind = intent.getStringExtra(EXTRA_KIND)?.let(ReminderKind::valueOf)
                    ?: ReminderKind.WORRY_SESSION
                ReminderReceiver().onReceive(
                    context,
                    Intent().putExtra(ReminderReceiver.EXTRA_KIND, kind.name),
                )
                "fired=${kind.name}"
            }
            ACTION_CANCEL -> {
                scheduler.cancelAll()
                "cancelled"
            }
            else -> "unknown-action"
        }
    }

    private fun status(scheduler: AndroidReminderScheduler): String = buildString {
        append("zone=${scheduler.scheduledZone()}")
        ReminderKind.entries.forEach { kind ->
            append(",${kind.name}=${scheduler.scheduledAt(kind)}")
        }
    }

    companion object {
        const val ACTION_SEED = "com.anchor.app.reminder.SEED_PROBE"
        const val ACTION_RESCHEDULE = "com.anchor.app.reminder.RESCHEDULE_PROBE"
        const val ACTION_STATUS = "com.anchor.app.reminder.STATUS_PROBE"
        const val ACTION_FIRE = "com.anchor.app.reminder.FIRE_PROBE"
        const val ACTION_CANCEL = "com.anchor.app.reminder.CANCEL_PROBE"
        const val EXTRA_KIND = "kind"
    }
}
