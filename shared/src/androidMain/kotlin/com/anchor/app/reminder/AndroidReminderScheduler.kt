package com.anchor.app.reminder

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import java.time.ZoneId

private const val PREFERENCES = "m0-reminders"
private const val CHANNEL = "reminders"
private const val ENABLED_SUFFIX = "-enabled"
private const val AT_SUFFIX = "-at"
private const val WORRY_HOUR = "worry-hour"
private const val WORRY_MINUTE = "worry-minute"
private const val SCHEDULED_ZONE = "scheduled-zone"
private const val PENDING = "pending"
private const val DAY_MILLIS = 24L * 60 * 60 * 1_000

enum class ReminderKind(
    val requestCode: Int,
    val body: String,
) {
    CRISIS_24H(7201, "这几天还好吗？需要的话，帮助入口一直在。"),
    CRISIS_72H(7202, "这几天还好吗？需要的话，帮助入口一直在。"),
    WORRY_SESSION(7203, "专场快开始了。"),
    REASSESSMENT(7204, "距离上次评估两周了，愿意的话再测一次。"),
    MEDICAL_WAITING(7205, "就医资源页一直在，也可以再做一次评估。"),
}

class AndroidReminderScheduler(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val alarmManager = appContext.getSystemService(AlarmManager::class.java)

    fun allows(kind: ReminderKind): Boolean =
        !preferences.getBoolean(kind.userOffKey, false)

    fun setAllows(kind: ReminderKind, allow: Boolean) {
        preferences.edit().putBoolean(kind.userOffKey, !allow).apply()
        if (!allow) {
            disable(kind)
        } else if (kind == ReminderKind.WORRY_SESSION) {
            scheduleWorrySession()
        }
    }

    fun scheduleWorrySession(sessionHour: Int = 20, sessionMinute: Int = 0) {
        if (!allows(ReminderKind.WORRY_SESSION)) return
        val reminderMinutes = sessionHour * 60 + sessionMinute - 5
        val normalizedMinutes = (reminderMinutes + 24 * 60) % (24 * 60)
        preferences.edit()
            .putBoolean(ReminderKind.WORRY_SESSION.enabledKey, true)
            .putInt(WORRY_HOUR, normalizedMinutes / 60)
            .putInt(WORRY_MINUTE, normalizedMinutes % 60)
            .apply()
        scheduleWorry(System.currentTimeMillis())
    }

    fun cancelWorrySession() = disable(ReminderKind.WORRY_SESSION)

    fun scheduleCrisisFollowUps(triggeredAtMillis: Long = System.currentTimeMillis()) {
        if (!allows(ReminderKind.CRISIS_24H) && !allows(ReminderKind.CRISIS_72H)) return
        scheduleOneShot(ReminderKind.CRISIS_24H, triggeredAtMillis + DAY_MILLIS)
        scheduleOneShot(ReminderKind.CRISIS_72H, triggeredAtMillis + 3 * DAY_MILLIS)
    }

    fun cancelCrisisFollowUps() {
        disable(ReminderKind.CRISIS_24H)
        disable(ReminderKind.CRISIS_72H)
    }

    fun scheduleReassessment(lastAssessmentAtMillis: Long) {
        if (!allows(ReminderKind.REASSESSMENT)) return
        val target = lastAssessmentAtMillis + 14 * DAY_MILLIS
        val existing = scheduledAt(ReminderKind.REASSESSMENT)
        val now = System.currentTimeMillis()
        if (existing > now) return
        if (target <= now) return
        scheduleOneShot(ReminderKind.REASSESSMENT, target)
    }

    fun postponeReassessment(nowMillis: Long = System.currentTimeMillis(), delayMillis: Long = DAY_MILLIS) {
        if (!allows(ReminderKind.REASSESSMENT)) return
        scheduleOneShot(ReminderKind.REASSESSMENT, nowMillis + delayMillis)
    }

    fun cancelReassessment() = disable(ReminderKind.REASSESSMENT)

    fun scheduleMedicalWaitingCare(startedAtMillis: Long = System.currentTimeMillis()) {
        if (!allows(ReminderKind.MEDICAL_WAITING)) return
        preferences.edit()
            .putBoolean(ReminderKind.MEDICAL_WAITING.enabledKey, true)
            .putLong(ReminderKind.MEDICAL_WAITING.atKey, startedAtMillis + 7 * DAY_MILLIS)
            .apply()
        schedule(ReminderKind.MEDICAL_WAITING, startedAtMillis + 7 * DAY_MILLIS)
    }

    fun cancelMedicalWaitingCare() = disable(ReminderKind.MEDICAL_WAITING)

    fun rescheduleAll(nowMillis: Long = System.currentTimeMillis()) {
        if (preferences.getBoolean(ReminderKind.WORRY_SESSION.enabledKey, false)) {
            scheduleWorry(nowMillis)
        }
        ReminderKind.entries
            .filterNot { it == ReminderKind.WORRY_SESSION }
            .filter { preferences.getBoolean(it.enabledKey, false) }
            .forEach { kind ->
                val storedAt = preferences.getLong(kind.atKey, 0L)
                val nextAt = if (kind == ReminderKind.MEDICAL_WAITING) {
                    ReminderTimes.nextWeekly(storedAt, nowMillis)
                } else {
                    storedAt
                }
                if (kind == ReminderKind.MEDICAL_WAITING && nextAt != storedAt) {
                    preferences.edit().putLong(kind.atKey, nextAt).apply()
                }
                schedule(kind, if (nextAt > nowMillis) nextAt else nowMillis + 1_000)
            }
    }

    fun consumePendingNotice(): String? {
        val pending = preferences.getStringSet(PENDING, emptySet()).orEmpty()
        val kind = ReminderKind.entries.firstOrNull { it.name in pending } ?: return null
        preferences.edit().putStringSet(PENDING, pending - kind.name).apply()
        return kind.body
    }

    fun cancelAll() {
        ReminderKind.entries.forEach { alarmManager.cancel(pendingIntent(it)) }
        preferences.edit().clear().apply()
    }

    fun scheduledAt(kind: ReminderKind): Long = preferences.getLong(kind.atKey, 0L)
    fun scheduledZone(): String? = preferences.getString(SCHEDULED_ZONE, null)

    fun onDelivered(kind: ReminderKind, nowMillis: Long = System.currentTimeMillis()) {
        when (kind) {
            ReminderKind.WORRY_SESSION -> scheduleWorry(nowMillis)
            ReminderKind.MEDICAL_WAITING -> {
                val nextAt = ReminderTimes.nextWeekly(scheduledAt(kind), nowMillis)
                preferences.edit().putLong(kind.atKey, nextAt).apply()
                schedule(kind, nextAt)
            }
            else -> disable(kind)
        }
    }

    private fun scheduleWorry(nowMillis: Long) {
        val zoneId = ZoneId.systemDefault()
        val at = ReminderTimes.nextDaily(
            nowMillis,
            preferences.getInt(WORRY_HOUR, 19),
            preferences.getInt(WORRY_MINUTE, 55),
            zoneId,
        )
        preferences.edit()
            .putLong(ReminderKind.WORRY_SESSION.atKey, at)
            .putString(SCHEDULED_ZONE, zoneId.id)
            .apply()
        schedule(ReminderKind.WORRY_SESSION, at)
    }

    private fun scheduleOneShot(kind: ReminderKind, atMillis: Long) {
        preferences.edit()
            .putBoolean(kind.enabledKey, true)
            .putLong(kind.atKey, atMillis)
            .apply()
        schedule(kind, atMillis)
    }

    private fun schedule(kind: ReminderKind, atMillis: Long) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pendingIntent(kind))
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pendingIntent(kind))
        }
    }

    private fun disable(kind: ReminderKind) {
        alarmManager.cancel(pendingIntent(kind))
        preferences.edit()
            .remove(kind.enabledKey)
            .remove(kind.atKey)
            .apply()
    }

    private fun pendingIntent(kind: ReminderKind): PendingIntent = PendingIntent.getBroadcast(
        appContext,
        kind.requestCode,
        Intent(appContext, ReminderReceiver::class.java).putExtra(ReminderReceiver.EXTRA_KIND, kind.name),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val kind = intent.getStringExtra(EXTRA_KIND)?.let(ReminderKind::valueOf) ?: return
        val scheduler = AndroidReminderScheduler(context)
        if (!deliverReminder(context, kind)) {
            val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            val pending = preferences.getStringSet(PENDING, emptySet()).orEmpty()
            preferences.edit().putStringSet(PENDING, pending + kind.name).apply()
        }
        scheduler.onDelivered(kind)
    }

    companion object {
        const val EXTRA_KIND = "kind"
    }
}

class ReminderRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        AndroidReminderScheduler(context).rescheduleAll()
    }
}

private fun deliverReminder(context: Context, kind: ReminderKind): Boolean {
    val manager = context.getSystemService(NotificationManager::class.java)
    manager.createNotificationChannel(
        NotificationChannel(CHANNEL, "温和提醒", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "仅用于用户主动开启的四类提醒"
            lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            setShowBadge(false)
        },
    )
    val permissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
    if (!permissionGranted || !manager.areNotificationsEnabled() ||
        manager.getNotificationChannel(CHANNEL).importance == NotificationManager.IMPORTANCE_NONE
    ) return false

    val publicVersion = Notification.Builder(context, CHANNEL)
        .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
        .setContentTitle("锚点")
        .build()
    val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
    val contentIntent = launchIntent?.let {
        PendingIntent.getActivity(
            context,
            kind.requestCode,
            it,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
    manager.notify(
        kind.requestCode,
        Notification.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("锚点")
            .setContentText(kind.body)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion)
            .build(),
    )
    return true
}

private val ReminderKind.enabledKey get() = "$name$ENABLED_SUFFIX"
private val ReminderKind.atKey get() = "$name$AT_SUFFIX"
private val ReminderKind.userOffKey get() = "$name-user-off"
