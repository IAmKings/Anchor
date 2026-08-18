package com.anchor.app.reminder

import java.time.Instant
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals

class ReminderTimesTest {
    @Test
    fun dailyReminderFollowsTheNewLocalTimeZone() {
        val now = Instant.parse("2026-08-15T12:00:00Z").toEpochMilli()

        val shanghai = ReminderTimes.nextDaily(now, 19, 55, ZoneId.of("Asia/Shanghai"))
        val london = ReminderTimes.nextDaily(now, 19, 55, ZoneId.of("Europe/London"))

        assertEquals("2026-08-16T11:55:00Z", Instant.ofEpochMilli(shanghai).toString())
        assertEquals("2026-08-15T18:55:00Z", Instant.ofEpochMilli(london).toString())
    }

    @Test
    fun weeklyReminderKeepsItsSevenDayCadence() {
        val day = 24L * 60 * 60 * 1_000
        assertEquals(21 * day, ReminderTimes.nextWeekly(0, 14 * day))
    }
}
