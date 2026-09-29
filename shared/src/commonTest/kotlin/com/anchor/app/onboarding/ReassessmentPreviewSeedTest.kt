package com.anchor.app.onboarding

import com.anchor.app.storage.InMemoryAnchorStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ReassessmentPreviewSeedTest {
    @Test
    fun emptyLibraryShowsTheFilledInviteCards() {
        val store = InMemoryAnchorStore()
        val now = 20L * DAY
        val report = appendReassessmentPreviewIfEmpty(
            store,
            now,
            ::minuteOf,
            wakes,
            actions,
        )
        assertEquals(true, report.appended)
        assertEquals("约 11 分钟", report.wakeValue)
        assertEquals("预测高于实际 2 次", report.biasValue)
        assertEquals("平均高估 1 分", report.biasDetail)
        assertEquals("已记下 4 次体感", report.actionValue)
        assertEquals(4, store.rhythmEntries().size)
        assertEquals(4, store.microActions().size)
    }

    @Test
    fun recordsOutsideTheInviteWindowDoNotBlockTheSample() {
        val store = InMemoryAnchorStore()
        val now = 20L * DAY
        store.addRhythmEntry(now - 16 * DAY, null, now - 16 * DAY)
        val report = appendReassessmentPreviewIfEmpty(store, now, ::minuteOf, wakes, actions)
        assertEquals(true, report.appended)
        assertEquals("约 11 分钟", report.wakeValue)
        assertEquals(5, store.rhythmEntries().size)
    }

    @Test
    fun sampleIsNotAddedTwice() {
        val store = InMemoryAnchorStore()
        val now = 20L * DAY
        appendReassessmentPreviewIfEmpty(store, now, ::minuteOf, wakes, actions)
        val again = appendReassessmentPreviewIfEmpty(store, now, ::minuteOf, wakes, actions)
        assertFalse(again.appended)
        assertEquals(4, store.rhythmEntries().size)
        assertEquals(4, store.microActions().size)
        assertEquals("约 11 分钟", again.wakeValue)
    }

    private fun minuteOf(millis: Long): Int = when (millis) {
        nowBase - 4 * DAY -> 7 * 60
        nowBase - 3 * DAY -> 7 * 60 + 10
        nowBase - 2 * DAY -> 7 * 60 + 20
        nowBase - DAY -> 7 * 60 + 30
        else -> 0
    }

    private companion object {
        const val DAY = 24L * 60 * 60 * 1_000
        const val nowBase = 20L * DAY
        val wakes = listOf(nowBase - 4 * DAY, nowBase - 3 * DAY, nowBase - 2 * DAY, nowBase - DAY)
        val actions = listOf(
            PreviewMicroAction("示例：走到楼下", 8, 3, nowBase - 4 * DAY),
            PreviewMicroAction("示例：打开文档写一句", 6, 4, nowBase - 3 * DAY),
            PreviewMicroAction("示例：倒一杯水", 5, 5, nowBase - 2 * DAY),
            PreviewMicroAction("示例：把杯子放下", 4, 7, nowBase - DAY),
        )
    }
}
