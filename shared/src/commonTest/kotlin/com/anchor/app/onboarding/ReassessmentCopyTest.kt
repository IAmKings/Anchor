package com.anchor.app.onboarding

import com.anchor.app.safety.AssessmentBand
import com.anchor.app.safety.SafetyAction
import com.anchor.app.safety.SafetyMode
import com.anchor.app.safety.SafetyPolicy
import com.anchor.app.safety.SafetyState
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

    @Test
    fun inviteOutsideWaitingKeepsTheFourteenDayReview() {
        val due = reassessInviteCopy(waiting = false, due = true, firstLowAtMillis = null, nowMillis = 0)
        val early = reassessInviteCopy(waiting = false, due = false, firstLowAtMillis = 1, nowMillis = 2)
        assertEquals(reassessmentKicker, due.kicker)
        assertEquals(dueInviteHeadline, due.headline)
        assertEquals(inviteBody, due.body)
        assertEquals(earlyInviteHeadline, early.headline)
    }

    @Test
    fun waitingInviteMatchesTheStepStillRequired() {
        val day = 24L * 60 * 60 * 1_000
        val first = waitingReassessNotice(null, nowMillis = 0)
        assertEquals("这次记下分数。等待期不一定因此结束。", first.headline)
        assertTrue(first.body.contains("第一次回到轻度"))
        assertTrue(first.body.contains("纠正误测"))
        assertFalse(first.body.contains("必须"))

        val waiting = waitingReassessNotice(firstLowAtMillis = 0, nowMillis = 3 * day)
        assertEquals("上一次已经回到轻度。这次还不能结束等待期。", waiting.headline)
        assertTrue(waiting.body.contains("大约还要 4 天"))

        val almost = waitingReassessNotice(firstLowAtMillis = 0, nowMillis = 6 * day + 60 * 60 * 1_000)
        assertTrue(almost.body.contains("大约还要 1 天"))

        val ready = waitingReassessNotice(firstLowAtMillis = 0, nowMillis = 7 * day)
        assertEquals("上一次低分已经隔满 7 天。", ready.headline)
        assertTrue(ready.body.contains("练习会重新打开"))
        assertFalse(ready.body.contains("中度至重度"))

        val shown = reassessInviteCopy(waiting = true, due = true, firstLowAtMillis = null, nowMillis = 0)
        assertEquals("就医等待期", shown.kicker)
        assertEquals(first.headline, shown.headline)
        assertEquals(first.body, shown.body)
        assertFalse(shown.headline.contains("14 天到了"))
    }

    @Test
    fun zeroWhileAlreadyWaitingIsTheFirstLowNotModerateToSevere() {
        val before = SafetyState(SafetyMode.MedicalWaiting)
        val at = 5_000L
        val outcome = SafetyPolicy.evaluate(phq(0), gad(0), at, previous = before)
        val copy = stillWaitingResult(before, outcome, at)!!
        assertEquals("PHQ-9 0 · 很低", copy.phqLine)
        assertEquals("GAD-7 0 · 很低", copy.gadLine)
        assertEquals(
            "这是第一次两张表都回到 9 分或以下。再过至少 7 天，若下一次仍是这样，练习会重新打开。现在还在等待期。",
            copy.body,
        )
        assertEquals(waitingResultHomeLabel, copy.button)
        assertFalse(copy.body.contains("中度至重度"))
        assertFalse(copy.body.contains("必须"))
    }

    @Test
    fun lowInsideSevenDaysSaysHowLongIsLeft() {
        val before = SafetyState(SafetyMode.MedicalWaiting, firstLowAssessmentAtMillis = 0)
        val at = 3L * 24 * 60 * 60 * 1_000
        val copy = stillWaitingResult(before, SafetyPolicy.evaluate(phq(0), gad(0), at, previous = before), at)!!
        assertTrue(copy.body.contains("大约还要 4 天"))
        assertTrue(copy.body.contains("等待期还在") || copy.body.contains("还不到 7 天"))
        assertFalse(copy.body.contains("中度至重度"))
        assertEquals(waitingResultHomeLabel, copy.button)
    }

    @Test
    fun moderateWhileWaitingIsNotCalledModeratelySevere() {
        val before = SafetyState(SafetyMode.MedicalWaiting)
        val copy = stillWaitingResult(before, SafetyPolicy.evaluate(phq(12), gad(0), 1_000, previous = before), 1_000)!!
        assertEquals("PHQ-9 12 · 中度", copy.phqLine)
        assertEquals("GAD-7 0 · 很低", copy.gadLine)
        assertEquals("这次还没回到 9 分或以下，等待期还在，这次也不计入那两次。", copy.body)
        assertFalse(copy.phqLine.contains("中重度"))
        assertFalse(copy.body.contains("中重度"))
    }

    @Test
    fun stillHighWhileWaitingDoesNotUseTheEntryButton() {
        val before = SafetyState(SafetyMode.MedicalWaiting, firstLowAssessmentAtMillis = 1_000)
        val copy = stillWaitingResult(before, SafetyPolicy.evaluate(phq(16), gad(0), 9_000, previous = before), 9_000)!!
        assertEquals("PHQ-9 16 · 中重度", copy.phqLine)
        assertEquals(
            "等待期还在。离开要连续两次都是两张表都在 9 分或以下，而且两次至少隔 7 天。这次没有计入。",
            copy.body,
        )
        assertEquals(waitingResultHomeLabel, copy.button)
    }

    @Test
    fun gadHighDoesNotRenameAZeroPhq() {
        val before = SafetyState(SafetyMode.MedicalWaiting)
        val copy = stillWaitingResult(before, SafetyPolicy.evaluate(phq(0), gad(15), 1_000, previous = before), 1_000)!!
        assertEquals("PHQ-9 0 · 很低", copy.phqLine)
        assertEquals("GAD-7 15 · 重度", copy.gadLine)
        assertTrue(copy.body.contains("这次没有计入"))
        assertFalse(copy.phqLine.contains("中度至重度"))
    }

    @Test
    fun firstEntryAndRecoveryDoNotUseTheStillWaitingCopy() {
        val entered = SafetyPolicy.evaluate(phq(15), gad(0), 1_000, previous = SafetyState())
        assertNull(stillWaitingResult(SafetyState(), entered, 1_000))
        val before = SafetyState(SafetyMode.MedicalWaiting, firstLowAssessmentAtMillis = 0)
        val at = 7L * 24 * 60 * 60 * 1_000
        val recovered = SafetyPolicy.evaluate(phq(0), gad(0), at, previous = before)
        assertNull(stillWaitingResult(before, recovered, at))
    }

    private fun phq(total: Int): List<Int> {
        var remaining = total
        return List(9) { index ->
            if (index == 8) 0 else minOf(3, remaining).also { remaining -= it }
        }
    }

    private fun gad(total: Int): List<Int> {
        var remaining = total
        return List(7) { minOf(3, remaining).also { remaining -= it } }
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