package com.anchor.app.home

import com.anchor.app.ui.formatCountdown
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeCopyTest {
    @Test
    fun worryBodyUsesPendingCountAndNextSession() {
        assertEquals("点这里把念头放进去", homeWorryBody(0, false, "今天 20:00"))
        assertEquals("3张卡，今天 20:00开箱", homeWorryBody(3, false, "今天 20:00"))
        assertEquals("2张卡，专场已开箱", homeWorryBody(2, true, "今天 20:00"))
    }

    @Test
    fun microActionCopyAvoidsStreakLanguage() {
        assertEquals("微行动", homeMicroActionTitle(running = false))
        assertEquals("微行动 · 进行中", homeMicroActionTitle(running = true))
        assertEquals("选一个低到无需说服自己的动作", homeMicroActionBody(null))
        assertEquals("穿好鞋走到楼下", homeMicroActionBody("穿好鞋走到楼下"))
        assertEquals("选择", homeMicroActionActionLabel(hasTitle = false, running = false, completed = false))
        assertEquals("开始 5 分钟", homeMicroActionActionLabel(hasTitle = true, running = false, completed = false))
        assertEquals("已记下", homeMicroActionActionLabel(hasTitle = true, running = false, completed = true))
        assertEquals("", homeMicroActionActionLabel(hasTitle = true, running = true, completed = false))
    }

    @Test
    fun disclaimerMatchesStitchFirstShipCopy() {
        assertEquals("应用仅适用于轻度调节", homeMildUseDisclaimer)
        assertEquals("晨间节律", homeRhythmKicker)
        assertEquals("见光记录", homeRhythmEmptyTitle)
        assertEquals("忧虑保险箱", homeWorryLabel)
        assertEquals("张卡", homeWorryCountUnit)
    }

    @Test
    fun worryBentoSplitsCountAndUnlockLine() {
        assertEquals(null, homeWorryCountText(0))
        assertEquals("3", homeWorryCountText(3))
        assertEquals("专场已开箱", homeWorryUnlockLine(sessionOpen = true, nextLabel = "今天 20:00"))
        assertEquals("今天 20:00开箱", homeWorryUnlockLine(sessionOpen = false, nextLabel = "今天 20:00"))
    }

    @Test
    fun waitingHomeUsesDomainBadgeAndPrdPauseLine() {
        assertEquals("就医等待期", homeWaitingBadge)
        assertEquals("休息即是当下的练习", homeWaitingTitle)
        assertTrue(homeWaitingBody.contains(homeWaitingPauseLine))
        assertFalse(homeWaitingBody.contains("感受"))
        assertEquals("记录此刻能被看见的事实，暂不进行分析。", homeWaitingJournalBody)
        assertFalse(homeWaitingJournalBody.contains("感受"))
        assertTrue(homeWaitingChecklistBody.contains("身体感受"))
    }

    @Test
    fun countdownPadsMinutesAndSeconds() {
        assertEquals("05:00", formatCountdown(5 * 60 * 1_000))
        assertEquals("04:58", formatCountdown(4 * 60 * 1_000 + 58 * 1_000))
        assertEquals("00:00", formatCountdown(-10))
    }
}
