package com.anchor.app.storage

import com.anchor.app.safety.SafetyAction
import com.anchor.app.safety.SafetyMode
import com.anchor.app.safety.SafetyState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class InMemoryAnchorStoreTest {
    @Test
    fun followsTheSamePersistRecoverAndClearContract() {
        val store: AnchorStore = InMemoryAnchorStore()
        val highAnswers = mutableListOf(3, 3, 3, 3, 3, 0, 0, 0, 0)
        val blocked = store.evaluateAndStore(AssessmentInput(highAnswers, zeros(7), 1_000))
        highAnswers[0] = 0

        assertEquals(SafetyAction.MedicalWaiting, blocked.action)
        assertEquals(SafetyMode.MedicalWaiting, store.safetyState().mode)
        assertEquals(15, store.assessments().single().phq9Score)
        assertEquals(3, store.assessments().single().phq9.first())
        assertNotSame(highAnswers, store.assessments().single().phq9)
        store.saveUserProfile(UserProfile(AgeGroup.Adult18Plus, onboardingComplete = true, firstAnchor = FirstAnchor.MicroAction))
        assertEquals(UserProfile(AgeGroup.Adult18Plus, true, FirstAnchor.MicroAction), store.userProfile())
        assertFailsWith<IllegalArgumentException> {
            store.addEmotionCard("难过", "今天开会", "没人回应", 3_000)
        }
        store.addEmotionCard("被轻视", "  今天开会  ", "  讲完后没人回应  ", 3_000)
        val emotionCard = store.emotionCards().single()
        assertEquals("今天开会", emotionCard.event)
        assertEquals(null, emotionCard.passedAtMillis)
        store.markEmotionCardPassed(emotionCard.id, 4_000)
        assertEquals(4_000, store.emotionCards().single().passedAtMillis)
        assertFailsWith<IllegalArgumentException> { store.addCameraLog(" ", "", 5_000) }
        store.addCameraLog("他故意不回复我", "他讨厌我", 5_000)
        assertTrue(store.cameraLogs().single().factNeedsHint)
        store.addCameraLog("10:00 发了消息，16:00 回复一个‘好’", "", 6_000)
        assertEquals(false, store.cameraLogs().first().factNeedsHint)
        assertEquals("", store.cameraLogs().first().inference)
        assertFailsWith<IllegalArgumentException> { store.addWorryCard("", 7_000, 8_000) }
        store.addWorryCard("担心明天的汇报", 7_000, 8_000)
        val worry = store.worryCards().single()
        assertFailsWith<IllegalArgumentException> { store.resolveWorryCard(worry.id, WorryResolution.Action) }
        store.addMicroAction("穿好鞋走到楼下", 7_500)
        assertEquals("穿好鞋走到楼下", store.microActions().single().title)
        assertEquals(null, store.microActions().single().sourceWorryId)
        store.resolveWorryAsAction(worry.id, "打开文档写一句话", 8_000)
        assertEquals("打开文档写一句话", store.worryCards().single().action)
        val converted = store.microActions().first { it.sourceWorryId == worry.id }
        assertEquals(MicroAction(converted.id, "打开文档写一句话", worry.id, 8_000), converted)
        assertFailsWith<IllegalArgumentException> { store.startMicroAction(converted.id, 11, 9_000) }
        store.startMicroAction(converted.id, 8, 9_000)
        assertEquals(8, store.microActions().first { it.id == converted.id }.predictedDifficulty)
        assertFailsWith<IllegalArgumentException> { store.completeMicroAction(converted.id, 4, 8_999) }
        store.completeMicroAction(converted.id, 4, 10_000)
        assertEquals(4, store.microActions().first { it.id == converted.id }.actualDifficulty)
        assertFailsWith<IllegalArgumentException> { store.addRhythmEntry(null, null, 10_000) }
        store.addRhythmEntry(9_000, null, 10_000)
        assertEquals(9_000, store.rhythmEntries().single().wakeAtMillis)
        store.addWorryCard("", 7_000, 8_000, "voice.m4a")
        assertEquals("voice.m4a", store.worryCards().first().audioFileName)
        store.dismissWorryCard(store.worryCards().first().id)
        assertEquals(1, store.worryCards().size)

        store.evaluateAndStore(AssessmentInput(zeros(9), zeros(7), 2_000))
        val recovered = store.evaluateAndStore(
            AssessmentInput(zeros(9), zeros(7), 2_000 + SEVEN_DAYS),
        )
        assertEquals(SafetyAction.Continue, recovered.action)
        assertEquals(SafetyState(), store.safetyState())
        store.enterCrisisWaiting()
        assertEquals(SafetyState(SafetyMode.MedicalWaiting), store.safetyState())

        store.clearAllData()
        assertTrue(store.assessments().isEmpty())
        assertEquals(SafetyState(), store.safetyState())
        assertEquals(UserProfile(), store.userProfile())
        assertTrue(store.emotionCards().isEmpty())
        assertTrue(store.cameraLogs().isEmpty())
        assertTrue(store.worryCards().isEmpty())
        assertTrue(store.microActions().isEmpty())
        assertTrue(store.rhythmEntries().isEmpty())
    }

    private fun zeros(size: Int) = List(size) { 0 }

    private companion object {
        const val SEVEN_DAYS = 7L * 24 * 60 * 60 * 1_000
    }
}
