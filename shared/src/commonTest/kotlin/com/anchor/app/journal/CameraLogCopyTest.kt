package com.anchor.app.journal

import com.anchor.app.storage.cameraFactNeedsHint
import com.anchor.app.storage.firstEvaluativeWord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CameraLogCopyTest {
    @Test
    fun evaluativeWordsAreHintsNotBlocks() {
        assertEquals("故意", firstEvaluativeWord("他故意不回复我"))
        assertTrue(cameraFactNeedsHint("他故意不回复我"))
        assertNull(firstEvaluativeWord("10:00 发了消息，16:00 回了一个好"))
        assertFalse(cameraFactNeedsHint("10:00 发了消息，16:00 回了一个好"))
    }

    @Test
    fun moveTakesTheSentenceContainingTheWord() {
        val (fact, inference) = moveEvaluativeSentence(
            fact = "10:00 发了消息。他故意不回复。",
            inference = "他不满意。",
            word = "故意",
        )
        assertEquals("10:00 发了消息。", fact)
        assertEquals("他不满意。\n他故意不回复。", inference)
    }

    @Test
    fun moveFallsBackToWholeFactWhenThereIsNoSentenceBreak() {
        val (fact, inference) = moveEvaluativeSentence("他故意针对我", "", "故意")
        assertEquals("", fact)
        assertEquals("他故意针对我", inference)
    }

    @Test
    fun recordsHubCopyHasNoStreakOrPercent() {
        val intro = recordsHubIntro(medicalWaiting = false)
        val waiting = recordsHubIntro(medicalWaiting = true)
        val texts = listOf(
            intro,
            waiting,
            recordsEmotionBody(),
            recordsJournalBody(),
            recordsWorryBody(),
            recordsCountLabel("张", 2).orEmpty(),
            recordsWorryCountLabel(3).orEmpty(),
            journalWriteLabel,
            journalSaveLabel,
            journalHintTitle,
            journalMoveLabel,
            journalReflection,
        )
        assertTrue(waiting.contains("双栏"))
        assertEquals("已记下 2 张", recordsCountLabel("张", 2))
        assertEquals(null, recordsCountLabel("条", 0))
        assertEquals("忧虑保险箱", recordsWorryTitle)
        assertEquals("3 张待处理", recordsWorryCountLabel(3))
        assertEquals(null, recordsWorryCountLabel(0))
        assertFalse(recordsWorryBody().contains("汇报"))
        assertFalse(recordsWorryBody().contains("今晚"))
        assertFalse(journalInferenceCaption.contains("感受"))
        assertFalse(journalReflection.contains("平静"))
        assertEquals("挪过去", journalMoveLabel)
        assertTrue(journalHintDetail("故意").contains("仍然可以直接保存"))
        assertTrue(texts.none { it.contains("完成率") || it.contains("%") || it.contains("streak") || it.contains("连续") })
    }
}
