package com.anchor.app.safety

enum class AssessmentBand { Minimal, Mild, Moderate, ModeratelySevere, Severe }

data class AssessmentResult(
    val phq9Score: Int,
    val phq9Band: AssessmentBand,
    val gad7Score: Int,
    val gad7Band: AssessmentBand,
)

enum class SafetyMode { Normal, MedicalWaiting }
enum class Clarification { NotSelf, Self, Uncertain }

data class SafetyState(
    val mode: SafetyMode = SafetyMode.Normal,
    val firstLowAssessmentAtMillis: Long? = null,
)

sealed interface SafetyAction {
    data object Continue : SafetyAction
    data object AskClarification : SafetyAction
    data object CrisisGuidance : SafetyAction
    data object MedicalWaiting : SafetyAction
    data object ClinicalReview : SafetyAction
}

data class SafetyOutcome(
    val assessment: AssessmentResult,
    val action: SafetyAction,
    val state: SafetyState,
)

object SafetyPolicy {
    private const val SEVEN_DAYS_MILLIS = 7L * 24 * 60 * 60 * 1_000

    fun evaluate(
        phq9: List<Int>,
        gad7: List<Int>,
        completedAtMillis: Long,
        previous: SafetyState = SafetyState(),
        keywordHit: Boolean = false,
        clarification: Clarification? = null,
        repeatedLowSelfEvaluation: Boolean = false,
    ): SafetyOutcome {
        require(completedAtMillis >= 0) { "评估时间不能为负数" }
        validateAnswers("PHQ-9", phq9, 9)
        validateAnswers("GAD-7", gad7, 7)
        require(keywordHit || clarification == null) { "没有关键词命中时不应提供澄清结果" }

        val assessment = AssessmentResult(
            phq9Score = phq9.sum(),
            phq9Band = phqBand(phq9.sum()),
            gad7Score = gad7.sum(),
            gad7Band = gadBand(gad7.sum()),
        )
        val waiting = SafetyState(SafetyMode.MedicalWaiting)

        if (phq9[8] > 0) return SafetyOutcome(assessment, SafetyAction.CrisisGuidance, waiting)
        if (keywordHit && clarification == null) {
            return SafetyOutcome(assessment, SafetyAction.AskClarification, previous)
        }
        if (keywordHit && clarification != Clarification.NotSelf) {
            return SafetyOutcome(assessment, SafetyAction.CrisisGuidance, waiting)
        }
        if (assessment.phq9Score >= 15 || assessment.gad7Score >= 15) {
            return SafetyOutcome(assessment, SafetyAction.MedicalWaiting, waiting)
        }

        val nextState = recoverIfEligible(previous, assessment, completedAtMillis)
        val action = when {
            nextState.mode == SafetyMode.MedicalWaiting -> SafetyAction.MedicalWaiting
            repeatedLowSelfEvaluation -> SafetyAction.ClinicalReview
            else -> SafetyAction.Continue
        }
        return SafetyOutcome(assessment, action, nextState)
    }

    fun phqBand(score: Int): AssessmentBand = when (score) {
        in 0..4 -> AssessmentBand.Minimal
        in 5..9 -> AssessmentBand.Mild
        in 10..14 -> AssessmentBand.Moderate
        in 15..19 -> AssessmentBand.ModeratelySevere
        in 20..27 -> AssessmentBand.Severe
        else -> error("PHQ-9 分数必须在 0..27")
    }

    fun gadBand(score: Int): AssessmentBand = when (score) {
        in 0..4 -> AssessmentBand.Minimal
        in 5..9 -> AssessmentBand.Mild
        in 10..14 -> AssessmentBand.Moderate
        in 15..21 -> AssessmentBand.Severe
        else -> error("GAD-7 分数必须在 0..21")
    }

    private fun recoverIfEligible(
        previous: SafetyState,
        assessment: AssessmentResult,
        completedAtMillis: Long,
    ): SafetyState {
        if (previous.mode == SafetyMode.Normal) return previous
        val isLow = assessment.phq9Score <= 9 && assessment.gad7Score <= 9
        if (!isLow) return SafetyState(SafetyMode.MedicalWaiting)
        val firstLow = previous.firstLowAssessmentAtMillis
            ?: return SafetyState(SafetyMode.MedicalWaiting, completedAtMillis)
        return if (completedAtMillis - firstLow >= SEVEN_DAYS_MILLIS) {
            SafetyState()
        } else {
            previous
        }
    }

    private fun validateAnswers(name: String, answers: List<Int>, size: Int) {
        require(answers.size == size) { "$name 必须有 $size 个答案" }
        require(answers.all { it in 0..3 }) { "$name 每题分数必须在 0..3" }
    }
}
