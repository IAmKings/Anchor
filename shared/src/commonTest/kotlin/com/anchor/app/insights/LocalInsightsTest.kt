package com.anchor.app.insights

import com.anchor.app.relation.altruismFeelCopy
import com.anchor.app.relation.energyCopy
import com.anchor.app.relation.pauseAltruismCopy
import com.anchor.app.relation.shouldPauseAltruism
import com.anchor.app.storage.AltruismDraw
import com.anchor.app.storage.AltruismFeel
import com.anchor.app.storage.AltruismKind
import com.anchor.app.storage.CameraLog
import com.anchor.app.storage.EnergyMark
import com.anchor.app.storage.MicroAction
import com.anchor.app.storage.RelationEnergyEntry
import com.anchor.app.storage.RhythmEntry
import com.anchor.app.storage.WorryCard
import com.anchor.app.storage.WorryResolution
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LocalInsightsTest {
    @Test
    fun calculatesAcrossMidnightAndIgnoresIncompleteActions() {
        val rhythm = listOf(
            RhythmEntry(1, 1_430, null, 1_500),
            RhythmEntry(2, 10, null, 1_500),
        )
        assertEquals(10.0, wakeStabilityMinutes(rhythm) { it.toInt() })

        val actions = listOf(
            MicroAction(1, "一步", null, 1, predictedDifficulty = 8, actualDifficulty = 4),
            MicroAction(2, "另一步", null, 2, predictedDifficulty = 5),
        )
        assertEquals(4.0, averagePredictionBias(actions))
        assertEquals(listOf(8 to 4), predictionPairs(actions))
        assertEquals(listOf(2.0, 2.5, 3.5), movingAverage(listOf(2, 3, 4), window = 2))
        assertEquals("平均高估 4 分", biasCopy(4.0))
        assertEquals("记录还不够", stabilityCopy(null))
    }

    @Test
    fun unsolvableCopyIsACountNotAPercent() {
        assertEquals("还没有处理过的卡片", worryUnsolvableCopy(emptyList()))
        val cards = listOf(
            WorryCard(1, "a", 1, 2, WorryResolution.Unsolvable),
            WorryCard(2, "b", 1, 2, WorryResolution.Action, "写一句"),
            WorryCard(3, "c", 1, 2, WorryResolution.Pending),
        )
        assertEquals("暂时无解 1 / 已处理 2", worryUnsolvableCopy(cards))
        assertTrue(!worryUnsolvableCopy(cards).contains("%"))
        assertTrue(worryUnsolvableDetail.contains("不是完成率"))
    }

    @Test
    fun interpretationWaitsForFifteenInferencesAndDoesNotDiagnose() {
        val few = List(14) { index ->
            CameraLog(index.toLong(), "6 小时后回了一个好", "他肯定对我不满", false, 1)
        }
        assertEquals(emptyList(), interpretationHits(few))
        assertEquals(interpretationNotEnough, interpretationCopy(14, emptyList()))

        val enough = few + CameraLog(15, "他回得很慢", "他不满意", false, 2)
        val hits = interpretationHits(enough)
        assertTrue(hits.any { it.label == "回复慢 = 对我不满" && it.count >= 2 })
        assertEquals("回看 ${hits.size} 种解释", interpretationCopy(15, hits))
        assertTrue(interpretationDetail.contains("不是诊断"))
        assertTrue(hits.none { it.label.contains("认知扭曲") })
    }

    @Test
    fun screenCopyAvoidsPercentGoalsAndDiagnosis() {
        assertEquals("洞察", insightsTitle)
        assertEquals("看长期，不看某一天", insightsAverageReady)
        assertEquals("记录还不够", interpretationNotEnough)
        assertEquals("使用最近 3 次起床记录的标准差。数字越小代表时间越接近，不是得分。", insightsWakeDetail(3))
        val texts = listOf(
            insightsIntro,
            insightsWakeTitle,
            insightsAverageTitle,
            insightsAverageReady,
            insightsAverageDetail,
            insightsBiasTitle,
            insightsBiasDetail,
            insightsWorryAcceptedTitle,
            insightsMonitorTitle,
            insightsEnergyTitle,
            insightsAltruismTitle,
        )
        assertTrue(texts.none { it.contains("%") || it.contains("15%") || it.contains("提升") || it.contains("诊断") })
        assertTrue(insightsWorryAcceptedDetail.contains("没有完成率"))
        assertTrue(insightsEnergyDetail.contains("没有完成率"))
        assertTrue(insightsEnergyDetail.contains("没有净值"))
        assertFalse(insightsMonitorDetail.contains("耗竭率"))
        assertTrue(!insightsAltruismDetail.contains("够不够好"))
    }

    @Test
    fun relationAndAltruismCycleUseCountsNotRates() {
        assertEquals(emptyList(), energyCountBars(emptyList()))
        assertEquals(emptyList(), energyCycleSeries(emptyList()))
        val energy = listOf(
            RelationEnergyEntry(1, 1, EnergyMark.Drained, 30),
            RelationEnergyEntry(2, 1, EnergyMark.Filled, 10),
            RelationEnergyEntry(3, 1, EnergyMark.Filled, 20),
        )
        assertEquals(
            listOf("回血" to 2, "抽干" to 1),
            energyCountBars(energy).map { it.label to it.count },
        )
        assertEquals(listOf(1f, 1f, 0f), energyCycleSeries(energy))
        val many = (1..14).map { index ->
            RelationEnergyEntry(
                index.toLong(),
                1,
                if (index % 2 == 0) EnergyMark.Filled else EnergyMark.Drained,
                index.toLong(),
            )
        }
        assertEquals(insightCycleWindow, energyCycleSeries(many).size)

        val open = AltruismDraw(1, "浇水", AltruismKind.NonSocial, 1)
        assertEquals(emptyList(), altruismCountBars(listOf(open)))
        val draws = listOf(
            AltruismDraw(1, "a", AltruismKind.NonSocial, 1, AltruismFeel.Tighter, 11),
            AltruismDraw(2, "b", AltruismKind.NonSocial, 2, AltruismFeel.Lighter, 12),
            AltruismDraw(3, "c", AltruismKind.Social, 3),
        )
        assertEquals(
            listOf("更轻" to 1, "更紧" to 1),
            altruismCountBars(draws).map { it.label to it.count },
        )
        assertEquals(listOf(0f, 1f), altruismCycleSeries(draws))
        val tight = List(3) { index ->
            AltruismDraw(index.toLong(), "x", AltruismKind.Social, index.toLong(), AltruismFeel.Tighter, index.toLong())
        }
        assertTrue(shouldPauseAltruism(tight))
        assertTrue(pauseAltruismCopy().contains("讨好就会反噬"))
        val forbidden = listOf(
            energyCountBars(energy).joinToString { it.label },
            altruismCountBars(draws).joinToString { it.label },
            energyCopy(energy),
            altruismFeelCopy(draws),
            pauseAltruismCopy(),
            insightsEnergyDetail,
            insightsAltruismDetail,
            insightsMonitorDetail,
        )
        assertTrue(forbidden.none { it.contains("%") || it.contains("耗竭率") || it.contains("75%") || it.contains("HRV") })
    }
}
