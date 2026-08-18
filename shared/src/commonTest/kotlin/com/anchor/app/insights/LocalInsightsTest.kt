package com.anchor.app.insights

import com.anchor.app.storage.MicroAction
import com.anchor.app.storage.RhythmEntry
import kotlin.test.Test
import kotlin.test.assertEquals

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
}
