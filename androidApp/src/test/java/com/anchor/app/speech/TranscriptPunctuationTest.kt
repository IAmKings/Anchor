package com.anchor.app.speech

import org.junit.Assert.assertEquals
import org.junit.Test

class TranscriptPunctuationTest {
    @Test
    fun multiSegmentJoinsWithCommaAndEndsWithPeriod() {
        assertEquals(
            "明天要交报告，不知道怎么开口，有点烦。",
            punctuateTranscript(listOf("明天要交报告", "不知道怎么开口", "有点烦")),
        )
    }

    @Test
    fun singleSegmentStillEndsWithPeriod() {
        assertEquals("主要是我们。", punctuateTranscript(listOf("主要是我们")))
    }

    @Test
    fun blankSegmentsAreDroppedAndTrimmed() {
        assertEquals("第一段，第二段。", punctuateTranscript(listOf(" 第一段 ", "", "  ", "第二段")))
    }

    @Test
    fun emptyInputReturnsEmpty() {
        assertEquals("", punctuateTranscript(emptyList()))
        assertEquals("", punctuateTranscript(listOf("", "  ")))
    }
}
