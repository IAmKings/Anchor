package com.anchor.app.onboarding

import com.anchor.app.safety.AssessmentBand
import com.anchor.app.safety.SafetyAction
import com.anchor.app.storage.FirstAnchor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BaselineAssessmentTest {
    @Test
    fun containsTheExpectedStandardItemsAndChoices() {
        assertEquals(9, phq9Questions.size)
        assertEquals("有不如死掉或伤害自己的念头", phq9Questions.last())
        assertEquals(7, gad7Questions.size)
        assertEquals(listOf("完全没有", "有几天", "一半以上", "几乎每天"), assessmentChoices)
    }

    @Test
    fun firstAnchorPickerKeepsP0AndAddsP1RelationOptions() {
        assertEquals(6, p0FirstAnchors.size)
        assertEquals(setOf(FirstAnchor.SocialEnergy, FirstAnchor.AltruisticTask), p1FirstAnchors.map { it.anchor }.toSet())
        assertTrue(FirstAnchor.EmotionLabel in p0FirstAnchors.map { it.anchor })
        assertTrue(FirstAnchor.WaveWaiting in p0FirstAnchors.map { it.anchor })
    }

    @Test
    fun welcomeRequiresAgeAndAgreement() {
        assertFalse(canStartBaseline(ageSelected = false, agreed = false))
        assertFalse(canStartBaseline(ageSelected = true, agreed = false))
        assertFalse(canStartBaseline(ageSelected = false, agreed = true))
        assertTrue(canStartBaseline(ageSelected = true, agreed = true))
    }

    @Test
    fun phq9Item9SkipsGad7() {
        assertFalse(shouldSkipGad7(0))
        assertFalse(shouldSkipGad7(null))
        assertTrue(shouldSkipGad7(1))
        assertTrue(shouldSkipGad7(3))
    }

    @Test
    fun scaleSubmitWaitsUntilEveryItemIsAnswered() {
        assertFalse(allAnswered(List(9) { null }))
        assertFalse(allAnswered(List(9) { index -> if (index == 8) null else 0 }))
        assertTrue(allAnswered(List(9) { 0 }))
    }

    @Test
    fun resultCopyMatchesBandsWithoutPepTalk() {
        assertEquals("轻度状态", phqBandLabel(AssessmentBand.Mild))
        assertEquals("中重度", phqBandLabel(AssessmentBand.ModeratelySevere))
        assertEquals("需要立即支持", resultBadge(SafetyAction.CrisisGuidance))
        assertEquals("中度至重度", resultBadge(SafetyAction.MedicalWaiting))
        assertEquals("可以开始练习", resultBadge(SafetyAction.Continue))
    }
}
