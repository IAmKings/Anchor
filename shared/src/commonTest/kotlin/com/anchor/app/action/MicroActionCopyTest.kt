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
        assertEquals("微行动", microActionTitle)
        assertEquals("确认自定义行动", microActionCustomConfirm)
        assertEquals("开始 5 分钟", microActionStartLabel)
        assertEquals("我已完成", microActionFinishTimerLabel)
        assertEquals("记下这次体感", microActionSaveFeltLabel)
        assertEquals("回到今天", microActionHomeLabel)
        assertEquals("不评估想不想。启动 5 分钟。", microActionWaitBody)
        assertEquals("预测困难度 8 / 10", microActionPredictedBadge(8))
        assertEquals("你做到了「穿好鞋走到楼下」", microActionDidCopy("穿好鞋走到楼下"))
        assertTrue(microActionPresets.any { it.title == "做 5 个深蹲" })
        assertTrue(listOf(microActionIntro, microActionStartLabel, microActionResultTitle).none { it.contains("完成率") || it.contains("打卡") })
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
        assertEquals("“$historyEvidenceQuote”", historyQuotedEvidence())
        assertEquals("返回", historyBackLabel)
        assertEquals("8/10", historyScore(8))
        assertEquals("2 次", historyOverestimateValue(2))
        assertEquals("还没有", historyOverestimateValue(0))
        assertTrue(!historyEvidenceQuote.contains("这张表"))
        assertTrue(!historyEvidenceIntro.contains("启动通常是最难"))
        assertTrue(!historyBiasLabel.contains("%"))
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
