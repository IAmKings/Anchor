package com.anchor.app.safety

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CrisisPhrasesTest {
    @Test
    fun explicitPhrasesHitIncludingQuotedSpeech() {
        assertEquals("自我了断", findCrisisPhrase("领导说要自我了断"))
        assertEquals("活不下去", findCrisisPhrase("歌词写着我活不下去"))
        assertEquals("kill myself", findCrisisPhrase("I want to kill myself"))
        assertEquals("不想活", findCrisisPhrase("有时会觉得不想活了"))
    }

    @Test
    fun everydayAndImplicitPhrasesDoNotHit() {
        assertNull(findCrisisPhrase("今天开会没人接话"))
        assertNull(findCrisisPhrase("好累，想睡过去"))
        assertNull(findCrisisPhrase("让一切停下来休息一下"))
        assertNull(findCrisisPhrase("他回复了一个好"))
        assertNull(findCrisisPhrase("I can't do this"))
        assertNull(findCrisisPhrase("不想上班"))
        assertNull(findCrisisPhrase("想辞职休息几天"))
        assertNull(findCrisisPhrase("好累"))
        assertNull(findCrisisPhrase("想睡过去"))
    }

    @Test
    fun explicitSelfHarmPhrasesStillHit() {
        assertEquals("自杀", findCrisisPhrase("我在想自杀"))
        assertEquals("自残", findCrisisPhrase("有时会自残"))
        assertEquals("割腕", findCrisisPhrase("他写了割腕"))
        assertEquals("suicide", findCrisisPhrase("thinking about suicide"))
        assertEquals("self-harm", findCrisisPhrase("notes about self-harm"))
        assertEquals("不如死", findCrisisPhrase("觉得不如死掉"))
        assertEquals("不想活", findCrisisPhrase("我真的不想活下去"))
    }

    @Test
    fun punctuationDoesNotHideEnglishPhrases() {
        assertEquals("want to die", findCrisisPhrase("I want-to die tonight"))
    }

    @Test
    fun clarificationKeepsNotSelfAsContinue() {
        assertEquals(SafetyAction.AskClarification, onFreeTextCrisis(null))
        assertEquals(SafetyAction.Continue, onFreeTextCrisis(Clarification.NotSelf))
        assertEquals(SafetyAction.CrisisGuidance, onFreeTextCrisis(Clarification.Self))
        assertEquals(SafetyAction.CrisisGuidance, onFreeTextCrisis(Clarification.Uncertain))
    }

    @Test
    fun copyAvoidsPepTalk() {
        val texts = listOf(
            crisisClarificationTitle,
            crisisClarificationBody,
            crisisClarificationNotSelf,
            crisisClarificationSelf,
            crisisClarificationUncertain,
        )
        assertTrue(crisisClarificationTitle.contains("你自己"))
        assertTrue(texts.none { it.contains("加油") || it.contains("没事") || it.contains("坚强") })
    }
}
