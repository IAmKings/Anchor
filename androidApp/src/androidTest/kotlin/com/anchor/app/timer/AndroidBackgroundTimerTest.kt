package com.anchor.app.timer

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidBackgroundTimerTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val timer = AndroidBackgroundTimer(context)
    private val microActionTimer = AndroidBackgroundTimer(context, BackgroundTimerKind.MicroAction)

    @After
    fun cleanUp() {
        timer.cancel()
        microActionTimer.cancel()
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
        microActionTimer.schedule(10_000L)
        timer.cancel()

        assertEquals(true, microActionTimer.remainingMillis() > 0)
    }
}
