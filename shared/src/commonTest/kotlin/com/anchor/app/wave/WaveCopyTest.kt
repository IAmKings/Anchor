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
        assertEquals("顺手记一张情绪卡", waveNoteEmotion())
        assertFalse(waveNoteEmotion().contains("必须"))
    }

    @Test
    fun exactAlarmPromptExplainsCostWithoutPressure() {
        assertEquals("想让到点提醒准时吗", waveExactAlarmPromptTitle())
        assertTrue(waveExactAlarmPromptBody().contains("闹钟和提醒"))
        assertTrue(waveExactAlarmPromptBody().contains("推迟几分钟"))
        assertTrue(waveExactAlarmPromptBody().contains("「我的」"))
        assertFalse(waveExactAlarmPromptBody().contains("必须"))
        assertFalse(waveExactAlarmPromptBody().contains("建议"))
        assertEquals("去开启", waveExactAlarmPromptAllow)
        assertEquals("先不用", waveExactAlarmPromptSkip)
    }

    @Test
    fun homeOrbUsesApproximateMinutesOnlyWhileARoundIsRunning() {
        assertEquals("等待中", waveHomeStatus(0))
        assertEquals("等待中", waveHomeStatus(-1))
        assertEquals("等待中", waveHomeStatus(waveHomeStatusMaxMillis + 1))
        assertEquals("还剩 1 分钟", waveHomeStatus(1))
        assertEquals("还剩 1 分钟", waveHomeStatus(60_000))
        assertEquals("还剩 2 分钟", waveHomeStatus(60_001))
        assertEquals("还剩 6 分钟", waveHomeStatus(6L * 60_000))
        assertEquals("还剩 6 分钟", waveHomeStatus(5L * 60_000 + 1))
        assertEquals("浪潮等待，还剩 6 分钟。难受的时候点这里", waveHomeContentDescription("还剩 6 分钟"))
        assertFalse(waveHomeStatus(6L * 60_000).contains("秒"))
    }

    @Test
    fun breathCircleStaysInsideItsSlotAndStillCoversTheSentence() {
        assertEquals(waveBreathMinScale, waveBreathScale(0f))
        assertEquals(1f, waveBreathScale(1f))
        assertTrue(waveBreathScale(0.4f) in waveBreathMinScale..1f)
        assertEquals(0f, waveBreathUnit(0.75f, 0f), 0.001f)
        assertEquals(1f, waveBreathUnit(0.25f, 0f), 0.001f)
        val width = waveRecognizeCopyWidth(360f)
        val smallestDiameter = 360f * waveBreathMinScale
        assertTrue(width < smallestDiameter * 0.7072f)
        assertTrue(width > smallestDiameter * 0.5f)
    }
}
