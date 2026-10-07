package com.anchor.app.speech

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class ComputeAmplitudeTest {
    @Test
    fun silenceIsZero() {
        assertEquals(0f, computeAmplitude(ShortArray(1_024)))
    }

    @Test
    fun fullScaleSquareWaveIsNearOne() {
        val chunk = ShortArray(1_024) { if (it % 2 == 0) 32767 else -32767 }
        assertTrue(computeAmplitude(chunk) in 0.95f..1f)
    }

    @Test
    fun sineWaveAmplitudeMatchesRms() {
        val chunk = ShortArray(1_600) { (sin(2.0 * PI * it / 1600.0 * 8) * 16384).toInt().toShort() }
        // 正弦 RMS = 峰值 / √2，16384/32768 = 0.5 → 期望约 0.354
        assertTrue(computeAmplitude(chunk) in 0.33f..0.38f)
    }

    @Test
    fun emptyChunkIsZeroAndNeverCrashes() {
        assertEquals(0f, computeAmplitude(ShortArray(0)))
        assertEquals(0f, computeAmplitude(ShortArray(10), count = 0))
    }
}
