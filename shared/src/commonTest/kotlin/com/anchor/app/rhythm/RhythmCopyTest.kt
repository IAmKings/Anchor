package com.anchor.app.rhythm

import com.anchor.app.storage.RhythmEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RhythmCopyTest {
    @Test
    fun lateLightIsAHintNotAFailure() {
        assertFalse(lightIsLaterThanOneHour(1_000, 1_000 + RHYTHM_ONE_HOUR))
        assertTrue(lightIsLaterThanOneHour(1_000, 1_000 + RHYTHM_ONE_HOUR + 1))
        assertFalse(lateLightCopy().contains("失败"))
        assertFalse(rhythmSavedCopy().contains("连续"))
    }

    @Test
    fun sparklineUsesOldestWakeFirst() {
        val entries = listOf(
            RhythmEntry(3, 3 * 60_000, null, 3),
            RhythmEntry(2, 2 * 60_000, null, 2),
            RhythmEntry(1, 1 * 60_000, null, 1),
        )
        assertEquals(listOf(1, 2, 3), wakeMinutesOldestFirst(entries) { (it / 60_000).toInt() })
    }
}
