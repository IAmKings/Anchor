package com.anchor.app.action

import com.anchor.app.storage.MicroAction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MicroActionCopyTest {
    @Test
    fun presetsStayBelowThePersuasionThreshold() {
        val titles = microActionPresets.map { it.title }
        assertTrue(titles.containsAll(listOf("穿好鞋走到楼下", "打开文档写 1 句话", "打开 PDF 看第一段", "下楼走 10 分钟")))
        assertTrue(microActionPresets.none { it.title.contains("必须") || it.title.contains("坚持") })
        assertTrue(microActionCustomHint().contains("心理建设"))
    }

    @Test
    fun biasCopyDoesNotShameAHighActual() {
        assertEquals("实际比大脑预测轻 5 分。", microActionBiasCopy(8, 3))
        assertEquals("这次先如实记下，不需要得出结论。", microActionBiasCopy(3, 8))
        assertEquals("这次先如实记下，不需要得出结论。", microActionBiasCopy(null, 4))
    }

    @Test
    fun historyCopyAvoidsPercentAndKeepsTheSourceQuote() {
        val texts = listOf(
            historyEvidenceTitle,
            historyEvidenceIntro,
            historyEvidenceQuote,
            historyOpenLabel,
            historyEmpty,
            historyScore(8),
        )
        assertTrue(historyEvidenceQuote.contains("那张表"))
        assertEquals("8/10", historyScore(8))
        assertEquals("2 次", historyOverestimateValue(2))
        assertEquals("还没有", historyOverestimateValue(0))
        assertTrue(texts.none { it.contains("%") || it.contains("完成率") || it.contains("streak") || it.contains("连续") })
        val scored = evidenceActions(
            listOf(
                MicroAction(1, "a", null, 1, predictedDifficulty = 8, actualDifficulty = 3, completedAtMillis = 20),
                MicroAction(2, "b", null, 1, predictedDifficulty = 7, completedAtMillis = 30),
                MicroAction(3, "c", null, 1, predictedDifficulty = 9, actualDifficulty = 2, completedAtMillis = 10),
            ),
        )
        assertEquals(listOf(1L, 3L), scored.map { it.id })
    }
}
