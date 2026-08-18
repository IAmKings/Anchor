package com.anchor.app.onboarding

import com.anchor.app.safety.AssessmentBand
import com.anchor.app.safety.SafetyAction
import com.anchor.app.storage.MicroAction
import com.anchor.app.storage.RhythmEntry
import com.anchor.app.storage.StoredAssessment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReassessmentCopyTest {
    @Test
    fun dueAfterFourteenDaysAndNotBefore() {
        val first = assessment(completedAt = 1_000)
        assertFalse(reassessmentDue(listOf(first), nowMillis = 1_000 + REASSESSMENT_INTERVAL_MILLIS - 1))
        assertTrue(reassessmentDue(listOf(first), nowMillis = 1_000 + REASSESSMENT_INTERVAL_MILLIS))
        assertFalse(reassessmentDue(emptyList(), nowMillis = REASSESSMENT_INTERVAL_MILLIS))
    }

    @Test
    fun medicalReassessUsesFortyEightHours() {
        assertTrue(medicalReassessAllowed(null, 10_000))
        assertFalse(medicalReassessAllowed(1_000, 1_000 + MEDICAL_REASSESS_MIN_MILLIS - 1))
        assertTrue(medicalReassessAllowed(1_000, 1_000 + MEDICAL_REASSESS_MIN_MILLIS))
    }

    @Test
    fun baselineIsOldestAndLatestIsNewest() {
        val older = assessment(completedAt = 1_000, phq = 12)
        val newer = assessment(completedAt = 2_000, phq = 8)
        val pair = baselineAndLatest(listOf(newer, older))
        assertEquals(12, pair!!.first.phq9Score)
        assertEquals(8, pair.second.phq9Score)
        assertNull(baselineAndLatest(listOf(older)))
    }

    @Test
    fun scoreCopyIsFactualAndNotPep() {
        assertEquals("PHQ-9 比基线低 4 分。这不是诊断。", scoreDeltaCopy(12, 8, "PHQ-9"))
        assertEquals("PHQ-9 比基线高 3 分。这不是诊断。", scoreDeltaCopy(8, 11, "PHQ-9"))
        assertEquals("GAD-7 和基线相同。这不是诊断。", scoreDeltaCopy(5, 5, "GAD-7"))
        assertFalse(scoreDeltaCopy(12, 8, "PHQ-9").contains("积极"))
        assertFalse(inviteHeadline(due = true).contains("提升"))
    }

    @Test
    fun invitationAvoidsCompletionRateAndPercentLift() {
        assertFalse(inviteBody.contains("%"))
        assertFalse(actionCardDetail.contains("完成率"))
        assertEquals("预测高于实际 2 次", overestimateCopy(2))
        assertEquals("还没有可比较的预测", overestimateCopy(0))
        assertEquals("已记下 3 次体感", recordedActionCopy(3))
    }

    @Test
    fun countsOnlyComparableAndCompletedActions() {
        val actions = listOf(
            MicroAction(1, "a", null, 1, predictedDifficulty = 8, actualDifficulty = 3, completedAtMillis = 2),
            MicroAction(2, "b", null, 1, predictedDifficulty = 4, actualDifficulty = 6, completedAtMillis = 2),
            MicroAction(3, "c", null, 1, predictedDifficulty = 5),
        )
        assertEquals(1, overestimateCount(actions))
        assertEquals(2, recordedActionCount(actions))
    }

    @Test
    fun wakeWindowKeepsOnlyRecentWakes() {
        val entries = listOf(
            RhythmEntry(1, wakeAtMillis = 100, lightAtMillis = null, createdAtMillis = 100),
            RhythmEntry(2, wakeAtMillis = 10_000, lightAtMillis = null, createdAtMillis = 10_000),
            RhythmEntry(3, wakeAtMillis = null, lightAtMillis = null, createdAtMillis = 10_000),
        )
        assertEquals(listOf(10), wakeMinutesInWindow(entries, { it.toInt() / 1_000 }, nowMillis = 10_000, windowMillis = 1_000))
    }

    @Test
    fun bannerDetectsOnlyReassessmentCopy() {
        assertTrue(isReassessmentBanner("距离上次评估两周了，愿意的话再测一次。"))
        assertTrue(isReassessmentBanner("就医资源页一直在，也可以再做一次评估。"))
        assertFalse(isReassessmentBanner("专场快开始了。"))
    }

    @Test
    fun inviteHeadlineChangesWhenNotYetDue() {
        assertEquals(dueInviteHeadline, inviteHeadline(true))
        assertEquals(earlyInviteHeadline, inviteHeadline(false))
    }

    private fun assessment(completedAt: Long, phq: Int = 0) = StoredAssessment(
        completedAtMillis = completedAt,
        phq9 = List(9) { 0 }.toMutableList().also { it[0] = phq.coerceIn(0, 3) },
        gad7 = List(7) { 0 },
        phq9Score = phq,
        phq9Band = AssessmentBand.Minimal,
        gad7Score = 0,
        gad7Band = AssessmentBand.Minimal,
        action = SafetyAction.Continue,
    )
}