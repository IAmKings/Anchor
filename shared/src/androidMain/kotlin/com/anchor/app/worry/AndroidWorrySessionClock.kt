package com.anchor.app.worry

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object AndroidWorrySessionClock {
    fun formatLocalTime(millis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String =
        Instant.ofEpochMilli(millis).atZone(zoneId).format(DateTimeFormatter.ofPattern("HH:mm"))

    fun formatLocalStamp(millis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String =
        Instant.ofEpochMilli(millis).atZone(zoneId).format(DateTimeFormatter.ofPattern("M月d日 HH:mm"))

    fun localMinuteOfDay(millis: Long, zoneId: ZoneId = ZoneId.systemDefault()): Int {
        val time = Instant.ofEpochMilli(millis).atZone(zoneId).toLocalTime()
        return time.hour * 60 + time.minute
    }

    fun isOpen(nowMillis: Long = System.currentTimeMillis(), zoneId: ZoneId = ZoneId.systemDefault()): Boolean {
        val time = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalTime()
        return !time.isBefore(java.time.LocalTime.of(20, 0)) && time.isBefore(java.time.LocalTime.of(20, 20))
    }

    fun nextSessionMillis(nowMillis: Long = System.currentTimeMillis(), zoneId: ZoneId = ZoneId.systemDefault()): Long {
        val now = Instant.ofEpochMilli(nowMillis).atZone(zoneId)
        var session = now.toLocalDate().atTime(20, 0).atZone(now.zone)
        if (!now.toLocalTime().isBefore(java.time.LocalTime.of(20, 20))) session = session.plusDays(1)
        return session.toInstant().toEpochMilli().coerceAtLeast(nowMillis)
    }

    fun nextSessionLabel(nowMillis: Long = System.currentTimeMillis(), zoneId: ZoneId = ZoneId.systemDefault()): String {
        val now = Instant.ofEpochMilli(nowMillis).atZone(zoneId)
        val next = Instant.ofEpochMilli(nextSessionMillis(nowMillis, zoneId)).atZone(now.zone)
        val day = when (next.toLocalDate()) {
            now.toLocalDate() -> "今天"
            now.toLocalDate().plusDays(1) -> "明天"
            else -> next.format(DateTimeFormatter.ofPattern("M月d日"))
        }
        return "$day ${next.format(DateTimeFormatter.ofPattern("HH:mm"))}"
    }
}
