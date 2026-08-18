package com.anchor.app.wave

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WaveCopyTest {
    @Test
    fun recognizeCopyMatchesPrdAndAvoidsPepTalk() {
        assertEquals("我现在很难受，这是真的。", waveRecognizeTitle())
        assertFalse(waveRecognizeBody().contains("加油"))
        assertFalse(waveRecognizeBody().contains("但是"))
        assertEquals("我看见了", waveRecognizeAction())
    }

    @Test
    fun locateHasAtLeastEightBodySites() {
        assertTrue(waveBodyLocations.size >= 8)
        assertTrue(waveBodyLocations.map { it.fullLabel }.containsAll(
            listOf("胸口发紧", "喉咙堵", "手心出汗", "胃部收紧", "肩膀僵", "头皮发麻", "呼吸浅", "面部发烫"),
        ))
    }

    @Test
    fun waitCopyUsesPrdEndingAndNoUrgency() {
        assertEquals("我们一起等浪潮过去", waveWaitTitle(done = false))
        assertEquals("它自己退了。你没有掐掉它，它也会走。", waveWaitTitle(done = true))
        assertEquals("留意：胸口发紧", waveWaitBody("胸口发紧"))
        assertEquals("什么都不用做，只是呼吸。", waveWaitBody(null))
        assertEquals("再加 10 分钟", waveAddTenMinutes())
    }
}
