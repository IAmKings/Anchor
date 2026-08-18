package com.anchor.app.reminder

import java.time.Instant
import java.time.ZoneId

internal object ReminderTimes {
    fun nextDaily(
        nowMillis: Long,
        hour: Int,
        minute: Int,
        zoneId: ZoneId,
    ): Long {
        require(hour in 0..23 && minute in 0..59)
        val now = Instant.ofEpochMilli(nowMillis).atZone(zoneId)
        var next = now.toLocalDate().atTime(hour, minute).atZone(zoneId)
        if (!next.toInstant().isAfter(now.toInstant())) next = next.plusDays(1)
        return next.toInstant().toEpochMilli()
    }

    fun nextWeekly(anchorMillis: Long, nowMillis: Long): Long {
        val weekMillis = 7L * 24 * 60 * 60 * 1_000
        if (anchorMillis > nowMillis) return anchorMillis
        return anchorMillis + ((nowMillis - anchorMillis) / weekMillis + 1) * weekMillis
    }
}
