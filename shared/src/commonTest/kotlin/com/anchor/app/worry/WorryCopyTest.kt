package com.anchor.app.worry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class WorryCopyTest {
    @Test
    fun hangSheetCopyIsShortAndUsesDynamicSealMessage() {
        assertEquals("把这个念头放进保险箱", hangSheetTitle)
        assertEquals("说话，或写一句…", hangFieldHint)
        assertEquals("封存", hangSealLabel)
        assertFalse(hangSheetTitle.contains("今晚"))
        assertFalse(vaultSealedMessage(false, "明天 20:00").contains("今晚"))
    }

    @Test
    fun sealedMessageUsesNextSessionAndNeverShowsCardContent() {
        assertEquals("已存入保险箱，明天 20:00 统一开箱。", vaultSealedMessage(false, "明天 20:00"))
        assertEquals("已存入保险箱，可以在当前专场处理。", vaultSealedMessage(true, "今天 20:00"))
        assertEquals("这个已经在纸上了，明天 20:00 再说。", vaultRuminationMessage("明天 20:00"))
        assertFalse(vaultPendingSummary(3).contains("汇报"))
    }

    @Test
    fun recordedLabelIsRelativeAndShameFree() {
        assertEquals("刚刚挂上", worryRecordedLabel(1_000, 1_000))
        assertEquals("记录于 2 分钟前", worryRecordedLabel(0, 2 * 60_000))
        assertEquals("记录于 3 小时前", worryRecordedLabel(0, 3 * 60 * 60_000))
        assertEquals("记录于 2 天前", worryRecordedLabel(0, 2 * 24 * 60 * 60_000))
    }

    @Test
    fun doneSummaryCountsAcceptedCardsNotStreaks() {
        assertEquals("今日受理了 3 项忧虑", vaultDoneSummary(3))
        assertEquals("这一刻没有需要处理的卡片。", vaultDoneSummary(0))
    }
}
