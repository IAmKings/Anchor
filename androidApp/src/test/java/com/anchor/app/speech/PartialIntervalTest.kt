package com.anchor.app.speech

import org.junit.Assert.assertEquals
import org.junit.Test

class PartialIntervalTest {
    @Test
    fun streamingBaselineUpToThreeThousandChars() {
        assertEquals(200L, partialIntervalMillis(0))
        assertEquals(200L, partialIntervalMillis(2_999))
        assertEquals(200L, partialIntervalMillis(3_000 - 1))
    }

    @Test
    fun protectionBandBeyondTypicalInput() {
        assertEquals(500L, partialIntervalMillis(3_000))
        assertEquals(500L, partialIntervalMillis(9_999))
    }

    @Test
    fun extremeProtectionBeyondTenThousandChars() {
        assertEquals(1_000L, partialIntervalMillis(10_000))
        assertEquals(1_000L, partialIntervalMillis(30_000))
    }
}
