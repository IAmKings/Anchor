package com.anchor.app.timer

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock

private const val PREFERENCES = "m0-background-timer"
private const val DEADLINE = "deadline"
private const val FIRED = "fired"
private const val IN_APP_NOTICE_PENDING = "in-app-notice-pending"
const val TIMER_KIND = "timer-kind"
private const val NOTIFICATION_CHANNEL = "timer-completion"
enum class BackgroundTimerKind { Wave, MicroAction, InstrumentedA, InstrumentedB }

class AndroidBackgroundTimer(context: Context, private val kind: BackgroundTimerKind = BackgroundTimerKind.Wave) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences("$PREFERENCES-${kind.name}", Context.MODE_PRIVATE)
    private val alarmManager = appContext.getSystemService(AlarmManager::class.java)

    fun schedule(durationMillis: Long): Boolean {
        require(durationMillis > 0) { "durationMillis must be positive" }
        val deadline = SystemClock.elapsedRealtime() + durationMillis
        preferences.edit()
            .putLong(DEADLINE, deadline)
            .putBoolean(FIRED, false)
            .apply()
        val exact = canScheduleExactAlarms()
        if (exact) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                deadline,
                pendingIntent(),
            )
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                deadline,
                pendingIntent(),
            )
        }
        return exact
    }

    fun canScheduleExactAlarms(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    fun remainingMillis(nowMillis: Long = SystemClock.elapsedRealtime()): Long =
        (preferences.getLong(DEADLINE, nowMillis) - nowMillis).coerceAtLeast(0)

    fun hasFired(): Boolean = preferences.getBoolean(FIRED, false)

    fun hasPendingInAppNotice(): Boolean =
        preferences.getBoolean(IN_APP_NOTICE_PENDING, false)

    fun consumePendingInAppNotice(): Boolean {
        val pending = hasPendingInAppNotice()
        if (pending) preferences.edit().putBoolean(IN_APP_NOTICE_PENDING, false).apply()
        return pending
    }

    fun cancel() {
        alarmManager.cancel(pendingIntent())
        preferences.edit().clear().apply()
    }

    private fun pendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        appContext,
        kind.requestCode,
        Intent(appContext, BackgroundTimerReceiver::class.java).putExtra(TIMER_KIND, kind.name),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}

class BackgroundTimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val kind = intent.getStringExtra(TIMER_KIND)
            ?.let { runCatching { BackgroundTimerKind.valueOf(it) }.getOrNull() }
            ?: BackgroundTimerKind.Wave
        val notificationDelivered = deliverTimerCompletionNotification(context, kind)
        context.getSharedPreferences("$PREFERENCES-${kind.name}", Context.MODE_PRIVATE)
            .edit()
            .putBoolean(FIRED, true)
            .putBoolean(IN_APP_NOTICE_PENDING, !notificationDelivered)
            .apply()
    }
}

private fun deliverTimerCompletionNotification(context: Context, kind: BackgroundTimerKind): Boolean {
    if (!kind.shouldNotify) return false

    val manager = context.getSystemService(NotificationManager::class.java)
    manager.createNotificationChannel(
        NotificationChannel(
            NOTIFICATION_CHANNEL,
            "计时结束",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "仅用于正在进行的练习结束提醒"
            lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            setShowBadge(false)
        },
    )

    val permissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
    val channelEnabled = manager.areNotificationsEnabled() &&
        manager.getNotificationChannel(NOTIFICATION_CHANNEL).importance !=
        NotificationManager.IMPORTANCE_NONE
    if (!permissionGranted || !channelEnabled) return false

    val publicVersion = Notification.Builder(context, NOTIFICATION_CHANNEL)
        .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
        .setContentTitle("锚点")
        .build()
    val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
    val contentIntent = launchIntent?.let {
        PendingIntent.getActivity(
            context,
            0,
            it,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
    val notification = Notification.Builder(context, NOTIFICATION_CHANNEL)
        .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
        .setContentTitle("锚点")
        .setContentText(kind.completionText)
        .setContentIntent(contentIntent)
        .setAutoCancel(true)
        .setVisibility(Notification.VISIBILITY_PRIVATE)
        .setPublicVersion(publicVersion)
        .build()
    manager.notify(kind.notificationId, notification)
    return true
}

private val BackgroundTimerKind.requestCode: Int
    get() = when (this) {
        BackgroundTimerKind.Wave -> 7001
        BackgroundTimerKind.MicroAction -> 7011
        BackgroundTimerKind.InstrumentedA -> 7901
        BackgroundTimerKind.InstrumentedB -> 7911
    }

private val BackgroundTimerKind.notificationId: Int
    get() = when (this) {
        BackgroundTimerKind.Wave -> 7002
        BackgroundTimerKind.MicroAction -> 7012
        BackgroundTimerKind.InstrumentedA -> 7902
        BackgroundTimerKind.InstrumentedB -> 7912
    }

private val BackgroundTimerKind.shouldNotify: Boolean
    get() = this == BackgroundTimerKind.Wave || this == BackgroundTimerKind.MicroAction

private val BackgroundTimerKind.completionText: String
    get() = when (this) {
        BackgroundTimerKind.Wave -> "它自己退了。你没有掐掉它，它也会走。"
        BackgroundTimerKind.MicroAction -> "五分钟到了。停在这里，也算完成了一次尝试。"
        BackgroundTimerKind.InstrumentedA, BackgroundTimerKind.InstrumentedB -> ""
    }
