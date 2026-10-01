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
        assertEquals(
            listOf(
                FirstAnchor.EmotionLabel,
                FirstAnchor.FactsJournal,
                FirstAnchor.Rhythm,
                FirstAnchor.SocialEnergy,
                FirstAnchor.AltruisticTask,
            ),
            choosableFirstAnchors.map { it.anchor },
        )
        assertEquals(
            listOf(FirstAnchor.EmotionLabel, FirstAnchor.FactsJournal, FirstAnchor.SocialEnergy, FirstAnchor.AltruisticTask),
            anchorsAvailableToAdd(FirstAnchor.Rhythm, emptyList()),
        )
        assertEquals("确认选择", confirmFirstAnchorLabel)
        assertEquals("选定后我们将固定练习 14 天", firstAnchorCommitHint)
        assertFalse(p0FirstAnchors.any { it.title.startsWith("✓") })
        val icons = FirstAnchor.entries.map { firstAnchorIcon(it) }
        assertEquals(FirstAnchor.entries.size, icons.toSet().size)
    }

    @Test
    fun welcomeRequiresAgeAndAgreement() {
        assertFalse(canStartBaseline(ageSelected = false, agreed = false))
        assertFalse(canStartBaseline(ageSelected = true, agreed = false))
        assertFalse(canStartBaseline(ageSelected = false, agreed = true))
        assertTrue(canStartBaseline(ageSelected = true, agreed = true))
    }

    @Test
    fun welcomeKeepsSelfReportAgeAndInlineLegalLinks() {
        assertTrue(ageDisclaimer.contains("自报"))
        assertTrue(ageDisclaimer.contains("不会尝试检测"))
        assertEquals("18 岁以上", adultAgeLabel)
        assertEquals("14–17 岁", youthAgeLabel)
        assertEquals("我已阅读并同意用户协议与隐私政策", agreementLabel)
        assertEquals("用户协议", termsTitle)
        assertEquals("https://anchor-legal.125457.xyz/terms", termsUrl)
        assertEquals("隐私政策", privacyTitle)
        assertEquals("https://anchor-legal.125457.xyz/privacy", privacyUrl)
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
        assertEquals("提交评估", submitAssessmentLabel)
    }

    @Test
    fun scaleAttributionNamesPfizerSourceAndDisclaimsDiagnosis() {
        assertTrue(scaleSource.contains("Pfizer"))
        assertTrue(scaleSource.contains("phqscreeners.com"))
        assertTrue(scaleSource.contains("版权"))
        assertTrue(scaleDisclaimer.contains("不作为医疗诊断"))
        assertFalse(scaleSource.contains("已获法务确认"))
        assertFalse(scaleDisclaimer.contains("确诊"))
    }

    @Test
    fun resultCopyMatchesBandsWithoutPepTalk() {
        assertEquals("轻度状态", phqBandLabel(AssessmentBand.Mild))
        assertEquals("中重度", phqBandLabel(AssessmentBand.ModeratelySevere))
        assertEquals("需要立即支持", resultBadge(SafetyAction.CrisisGuidance))
        assertEquals("中度至重度", resultBadge(SafetyAction.MedicalWaiting))
        assertEquals("可以开始练习", resultBadge(SafetyAction.Continue))
        assertEquals("一次一件", oneThingKicker)
        assertEquals("选择第一个锚点", chooseFirstAnchorLabel)
        assertFalse(oneThingKicker.contains("穿好鞋"))
    }

    @Test
    fun oneThingLockUsesFourteenDaysAndLeavesLegacyProfilesOpen() {
        val start = 1_000L
        assertTrue(additionalPracticeUnlocked(null, start))
        assertFalse(additionalPracticeUnlocked(start, start + REASSESSMENT_INTERVAL_MILLIS - 1))
        assertTrue(additionalPracticeUnlocked(start, start + REASSESSMENT_INTERVAL_MILLIS))
        assertTrue(availableDuringOneThingLock(FirstAnchor.WaveWaiting))
        assertTrue(availableDuringOneThingLock(FirstAnchor.MicroAction))
        assertTrue(availableDuringOneThingLock(FirstAnchor.WorryVault))
        assertFalse(availableDuringOneThingLock(FirstAnchor.Rhythm))
        assertFalse(availableDuringOneThingLock(FirstAnchor.SocialEnergy))
        assertTrue(practiceVisible(FirstAnchor.WaveWaiting, FirstAnchor.Rhythm))
        assertTrue(practiceVisible(FirstAnchor.MicroAction, FirstAnchor.Rhythm))
        assertTrue(practiceVisible(FirstAnchor.WorryVault, FirstAnchor.Rhythm))
        assertTrue(practiceVisible(FirstAnchor.Rhythm, FirstAnchor.Rhythm))
        assertFalse(practiceVisible(FirstAnchor.Rhythm, FirstAnchor.MicroAction))
        assertFalse(practiceVisible(FirstAnchor.SocialEnergy, FirstAnchor.Rhythm))
        assertFalse(practiceVisible(FirstAnchor.AltruisticTask, FirstAnchor.Rhythm))
        assertFalse(practiceVisible(FirstAnchor.EmotionLabel, FirstAnchor.Rhythm))
        assertFalse(practiceVisible(FirstAnchor.FactsJournal, FirstAnchor.Rhythm))
        assertTrue(practiceVisible(FirstAnchor.SocialEnergy, FirstAnchor.SocialEnergy))
        assertTrue(practiceVisible(FirstAnchor.Rhythm, null))
        assertFalse(practiceVisible(FirstAnchor.Rhythm, FirstAnchor.MicroAction, emptySet()))
        assertTrue(practiceVisible(FirstAnchor.Rhythm, FirstAnchor.MicroAction, setOf(FirstAnchor.Rhythm)))
        assertTrue(factLogVisible(medicalWaiting = true, practiceAllowed = false))
        assertFalse(factLogVisible(medicalWaiting = false, practiceAllowed = false))
        assertTrue(factLogVisible(medicalWaiting = false, practiceAllowed = true))
        assertTrue(oneThingLockBody(FirstAnchor.MicroAction).contains("5 分钟微行动"))
        assertFalse(oneThingLockBody(FirstAnchor.MicroAction).contains("必须"))
    }
}
