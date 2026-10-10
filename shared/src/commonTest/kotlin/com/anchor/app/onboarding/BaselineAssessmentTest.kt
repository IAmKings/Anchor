package com.anchor.app.onboarding

import com.anchor.app.safety.AssessmentBand
import com.anchor.app.safety.SafetyAction
import com.anchor.app.storage.FirstAnchor
import com.anchor.app.storage.InMemoryAnchorStore
import com.anchor.app.storage.UserProfile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
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
    fun relationFamilyHintExplainsSharedCardWhenSiblingAdded() {
        assertEquals(null, relationFamilyHint(FirstAnchor.AltruisticTask, emptySet()))
        assertEquals(relationAltruismHint, relationFamilyHint(FirstAnchor.AltruisticTask, setOf(FirstAnchor.SocialEnergy)))
        assertEquals(relationInventoryHint, relationFamilyHint(FirstAnchor.SocialEnergy, setOf(FirstAnchor.AltruisticTask)))
        assertEquals(null, relationFamilyHint(FirstAnchor.Rhythm, setOf(FirstAnchor.SocialEnergy, FirstAnchor.AltruisticTask)))
    }

    @Test
    fun freshProfileMustNotUnlockAdditionalAnchor() {
        // 删除全部数据后的空档案：firstAnchorAtMillis == null 不等于已解锁，首启完成是前提。
        val store = InMemoryAnchorStore()
        store.clearAllData()
        store.saveUserProfile(UserProfile())
        val profile = store.userProfile()
        assertTrue(profile.firstAnchorAtMillis == null)
        assertTrue(additionalPracticeUnlocked(profile.firstAnchorAtMillis, 10_000L * 24 * 60 * 60 * 1_000))

        store.saveUserProfile(
            profile.copy(onboardingComplete = true, firstAnchor = FirstAnchor.Rhythm, firstAnchorAtMillis = 10_000L),
        )
        val locked = store.userProfile()
        assertFalse(additionalPracticeUnlocked(locked.firstAnchorAtMillis, 10_000L + 60_000))
        assertTrue(additionalPracticeUnlocked(locked.firstAnchorAtMillis, 10_000L + 15L * 24 * 60 * 60 * 1_000))
    }

    @Test
    fun legalOfflineDocumentsAreComplete() {
        // 应用内全文快照：章节数与网页一致，每章非空
        assertEquals(10, termsSections.size)
        assertEquals(17, privacySections.size)
        assertTrue(termsSections.all { it.heading.isNotBlank() && it.paragraphs.isNotEmpty() && it.paragraphs.all(String::isNotBlank) })
        assertTrue(privacySections.all { it.heading.isNotBlank() && it.paragraphs.isNotEmpty() && it.paragraphs.all(String::isNotBlank) })
        // 短摘要保留（首启勾选行的对话场景仍在文案里）
        assertTrue(termsBody.contains("不是医疗器械"))
        assertTrue(privacyBody.contains("不会上传"))
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
