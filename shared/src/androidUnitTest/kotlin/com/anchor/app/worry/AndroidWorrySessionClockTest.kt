package com.anchor.app.worry

import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AndroidWorrySessionClockTest {
    private val shanghai = ZoneId.of("Asia/Shanghai")

    @Test
    fun sessionOpensForTwentyMinutesAndThenRollsToTomorrow() {
        val before = millis("2026-08-16T19:59:00+08:00[Asia/Shanghai]")
        val start = millis("2026-08-16T20:00:00+08:00[Asia/Shanghai]")
        val end = millis("2026-08-16T20:20:00+08:00[Asia/Shanghai]")

        assertFalse(AndroidWorrySessionClock.isOpen(before, shanghai))
        assertTrue(AndroidWorrySessionClock.isOpen(start, shanghai))
        assertFalse(AndroidWorrySessionClock.isOpen(end, shanghai))
        assertEquals(start, AndroidWorrySessionClock.nextSessionMillis(before, shanghai))
        assertEquals(
            millis("2026-08-17T20:00:00+08:00[Asia/Shanghai]"),
            AndroidWorrySessionClock.nextSessionMillis(end, shanghai),
        )
        assertEquals("明天 20:00", AndroidWorrySessionClock.nextSessionLabel(end, shanghai))
    }

    private fun millis(value: String): Long = ZonedDateTime.parse(value).toInstant().toEpochMilli()
}
