package com.anchor.app.home

import com.anchor.app.storage.FirstAnchor
import kotlin.test.Test
import kotlin.test.assertEquals

class HomeLayoutTest {
    @Test
    fun chosenCardStaysAboveToolsAndLaterCardsStackBelow() {
        assertEquals(HomeAddedCard.Rhythm, commitmentCard(FirstAnchor.Rhythm))
        assertEquals(emptyList(), bottomCards(FirstAnchor.Rhythm, emptyList()))
        assertEquals(
            listOf(HomeAddedCard.Emotion, HomeAddedCard.Facts, HomeAddedCard.Relation),
            bottomCards(
                FirstAnchor.Rhythm,
                listOf(FirstAnchor.EmotionLabel, FirstAnchor.FactsJournal, FirstAnchor.SocialEnergy, FirstAnchor.AltruisticTask),
            ),
        )
    }

    @Test
    fun firstRelationCardStaysInTheCommitmentSlot() {
        val (commitment, bottom) = layoutPracticeCards(
            FirstAnchor.SocialEnergy,
            listOf(FirstAnchor.AltruisticTask, FirstAnchor.Rhythm),
        )
        assertEquals(HomeAddedCard.Relation, commitment)
        assertEquals(listOf(HomeAddedCard.Rhythm), bottom)
    }

    @Test
    fun emotionAndFactsUseTheCommitmentSlotWhenChosenFirst() {
        assertEquals(HomeAddedCard.Emotion, commitmentCard(FirstAnchor.EmotionLabel))
        assertEquals(HomeAddedCard.Facts, commitmentCard(FirstAnchor.FactsJournal))
        assertEquals(null, commitmentCard(FirstAnchor.MicroAction))
    }
}
