package com.anchor.app.timer

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidBackgroundTimerTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val timer = AndroidBackgroundTimer(context, BackgroundTimerKind.InstrumentedA)
    private val otherTimer = AndroidBackgroundTimer(context, BackgroundTimerKind.InstrumentedB)

    @After
    fun cleanUp() {
        timer.cancel()
        otherTimer.cancel()
    }

    @Test
    fun instrumentedKindsDoNotWriteProductionTimerPreferences() {
        assertNotEquals(BackgroundTimerKind.Wave, BackgroundTimerKind.InstrumentedA)
        assertNotEquals(BackgroundTimerKind.MicroAction, BackgroundTimerKind.InstrumentedB)
        val waveBefore = productionTimerPrefs("Wave")
        val microBefore = productionTimerPrefs("MicroAction")
        timer.schedule(5_000L)
        otherTimer.schedule(8_000L)
        assertEquals(waveBefore, productionTimerPrefs("Wave"))
        assertEquals(microBefore, productionTimerPrefs("MicroAction"))
    }

    @Test
    fun remainingTimeExpiresEvenWhenAlarmDeliveryIsLate() {
        timer.schedule(50L)
        Thread.sleep(100L)
        assertEquals(0L, timer.remainingMillis())
    }

    @Test
    fun timerKindsDoNotOverwriteEachOther() {
        timer.schedule(5_000L)
        otherTimer.schedule(10_000L)
        timer.cancel()

        assertEquals(true, otherTimer.remainingMillis() > 0)
    }

    private fun productionTimerPrefs(kind: String): Map<String, *> =
        context.getSharedPreferences("m0-background-timer-$kind", Context.MODE_PRIVATE).all.toMap()
}
