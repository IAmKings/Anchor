package com.anchor.app

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.anchor.app.storage.IOS_DATABASE_NAME
import com.anchor.app.storage.IosDatabaseKey
import com.anchor.app.storage.IosEncryptedProbeStore
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitHour
import platform.Foundation.NSCalendarUnitMinute
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSProcessInfo
import platform.Foundation.dateWithTimeIntervalSince1970

@OptIn(ExperimentalForeignApi::class)
fun MainViewController() = ComposeUIViewController {
    val store = remember {
        val key = IosDatabaseKey.getOrCreate(IOS_DATABASE_NAME)
        IosEncryptedProbeStore(IOS_DATABASE_NAME, key).also { key.fill(0) }
    }
    DisposableEffect(store) {
        onDispose { store.close() }
    }
    App(
        anchorStore = store,
        nowMillis = { platform.posix.time(null) * 1_000 },
        waveClockMillis = { (NSProcessInfo.processInfo.systemUptime * 1_000).toLong() },
        formatLocalTime = ::formatIosStamp,
        formatLocalStamp = ::formatIosStamp,
        localMinuteOfDay = ::iosLocalMinuteOfDay,
    )
}

private fun formatIosStamp(millis: Long): String {
    val formatter = NSDateFormatter().apply { dateFormat = "HH:mm" }
    return formatter.stringFromDate(NSDate.dateWithTimeIntervalSince1970(millis / 1_000.0))
}

private fun iosLocalMinuteOfDay(millis: Long): Int {
    val date = NSDate.dateWithTimeIntervalSince1970(millis / 1_000.0)
    val calendar = NSCalendar.currentCalendar
    val hour = calendar.component(NSCalendarUnitHour, date)
    val minute = calendar.component(NSCalendarUnitMinute, date)
    return (hour * 60 + minute).toInt()
}

