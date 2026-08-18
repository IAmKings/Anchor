package com.anchor.app.safety

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SafetyPolicyTest {
    @Test
    fun scoreBandsCoverEveryBoundary() {
        mapOf(
            0 to AssessmentBand.Minimal,
            4 to AssessmentBand.Minimal,
            5 to AssessmentBand.Mild,
            9 to AssessmentBand.Mild,
            10 to AssessmentBand.Moderate,
            14 to AssessmentBand.Moderate,
            15 to AssessmentBand.ModeratelySevere,
            19 to AssessmentBand.ModeratelySevere,
            20 to AssessmentBand.Severe,
            27 to AssessmentBand.Severe,
        ).forEach { (score, band) -> assertEquals(band, SafetyPolicy.phqBand(score)) }
        mapOf(
            0 to AssessmentBand.Minimal,
            4 to AssessmentBand.Minimal,
            5 to AssessmentBand.Mild,
            9 to AssessmentBand.Mild,
            10 to AssessmentBand.Moderate,
            14 to AssessmentBand.Moderate,
            15 to AssessmentBand.Severe,
            21 to AssessmentBand.Severe,
        ).forEach { (score, band) -> assertEquals(band, SafetyPolicy.gadBand(score)) }
    }

    @Test
    fun phqItemNineAlwaysTriggersCrisis() {
        val outcome = evaluate(phq9 = answers(9, total = 1, last = 1), keywordHit = true, clarification = Clarification.NotSelf)
        assertEquals(SafetyAction.CrisisGuidance, outcome.action)
        assertEquals(SafetyMode.MedicalWaiting, outcome.state.mode)
    }

    @Test
    fun keywordHitRequiresClarificationAndTreatsUncertaintyAsCrisis() {
        assertEquals(SafetyAction.AskClarification, evaluate(keywordHit = true).action)
        assertEquals(
            SafetyAction.Continue,
            evaluate(keywordHit = true, clarification = Clarification.NotSelf).action,
        )
        assertEquals(
            SafetyAction.CrisisGuidance,
            evaluate(keywordHit = true, clarification = Clarification.Self).action,
        )
        assertEquals(
            SafetyAction.CrisisGuidance,
            evaluate(keywordHit = true, clarification = Clarification.Uncertain).action,
        )
    }

    @Test
    fun eitherScaleAtFifteenEntersMedicalWaiting() {
        assertEquals(SafetyAction.MedicalWaiting, evaluate(phq9 = answers(9, 15)).action)
        assertEquals(SafetyAction.MedicalWaiting, evaluate(gad7 = answers(7, 15)).action)
    }

    @Test
    fun waitingRequiresTwoLowAssessmentsAtLeastSevenDaysApart() {
        val waiting = SafetyState(SafetyMode.MedicalWaiting)
        val first = evaluate(previous = waiting, completedAtMillis = 1_000)
        assertEquals(SafetyMode.MedicalWaiting, first.state.mode)
        assertEquals(1_000, first.state.firstLowAssessmentAtMillis)

        val tooEarly = evaluate(previous = first.state, completedAtMillis = 1_000 + SIX_DAYS)
        assertEquals(first.state, tooEarly.state)

        val recovered = evaluate(previous = first.state, completedAtMillis = 1_000 + SEVEN_DAYS)
        assertEquals(SafetyAction.Continue, recovered.action)
        assertEquals(SafetyState(), recovered.state)
        assertTrue(recoveredToPractice(first.state, recovered.state))
        assertFalse(recoveredToPractice(SafetyState(), recovered.state))
    }

    @Test
    fun moderateOrHighAssessmentResetsRecoveryStreak() {
        val streak = SafetyState(SafetyMode.MedicalWaiting, firstLowAssessmentAtMillis = 1_000)
        val moderate = evaluate(phq9 = answers(9, 10), previous = streak, completedAtMillis = 1_000 + SEVEN_DAYS)
        assertEquals(SafetyState(SafetyMode.MedicalWaiting), moderate.state)
        val high = evaluate(phq9 = answers(9, 15), previous = streak, completedAtMillis = 1_000 + SEVEN_DAYS)
        assertEquals(SafetyState(SafetyMode.MedicalWaiting), high.state)
    }

    @Test
    fun repeatedLowSelfEvaluationRequestsReviewWithoutBlocking() {
        assertEquals(SafetyAction.ClinicalReview, evaluate(repeatedLowSelfEvaluation = true).action)
    }

    @Test
    fun invalidInputIsRejected() {
        assertFailsWith<IllegalArgumentException> { evaluate(phq9 = emptyList()) }
        assertFailsWith<IllegalArgumentException> { evaluate(gad7 = listOf(4, 0, 0, 0, 0, 0, 0)) }
        assertFailsWith<IllegalArgumentException> { evaluate(completedAtMillis = -1) }
        assertFailsWith<IllegalArgumentException> { evaluate(clarification = Clarification.Self) }
    }

    private fun evaluate(
        phq9: List<Int> = answers(9, 0),
        gad7: List<Int> = answers(7, 0),
        completedAtMillis: Long = 0,
        previous: SafetyState = SafetyState(),
        keywordHit: Boolean = false,
        clarification: Clarification? = null,
        repeatedLowSelfEvaluation: Boolean = false,
    ) = SafetyPolicy.evaluate(
        phq9,
        gad7,
        completedAtMillis,
        previous,
        keywordHit,
        clarification,
        repeatedLowSelfEvaluation,
    )

    private fun answers(size: Int, total: Int, last: Int = 0): List<Int> {
        require(total >= last)
        var remaining = total - last
        return List(size) { index ->
            if (index == size - 1) last else minOf(3, remaining).also { remaining -= it }
        }
    }

    private companion object {
        const val SIX_DAYS = 6L * 24 * 60 * 60 * 1_000
        const val SEVEN_DAYS = 7L * 24 * 60 * 60 * 1_000
    }
}
