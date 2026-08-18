package com.anchor.app.emotion

import com.anchor.app.storage.emotionVocabulary
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EmotionCopyTest {
    @Test
    fun groupsCoverTheStoredVocabularyExactly() {
        assertTrue(groupedWordsCoverVocabulary())
        assertEquals(emotionVocabulary.size, emotionGroups.sumOf { it.words.size })
        assertTrue(emotionGroups.size >= 2)
    }

    @Test
    fun vagueWordsAreSearchEntriesNotFinalLabels() {
        assertTrue(isVagueEmotionQuery("很难过"))
        assertTrue(isVagueEmotionQuery("烦"))
        assertTrue(searchEmotions("难过").vague)
        assertFalse(emotionVocabulary.contains("难过"))
        assertFalse(searchEmotions("被轻视").vague)
        assertEquals(listOf("被轻视"), searchEmotions("被轻视").groups.single().words)
        assertFalse(isVagueEmotionQuery("心"))
        assertTrue(searchEmotions("心").groups.single().words.contains("心慌"))
    }

    @Test
    fun sentenceKeepsTheStructuredTemplate() {
        assertEquals(
            "在「今天开会」中，我感到被轻视；最让我难受的是「讲完没人接话」。",
            emotionCardSentence("被轻视", "  今天开会  ", "讲完没人接话"),
        )
    }
}
