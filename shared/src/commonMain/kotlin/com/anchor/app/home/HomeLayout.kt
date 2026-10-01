package com.anchor.app.home

import com.anchor.app.storage.FirstAnchor

internal enum class HomeAddedCard { Rhythm, Emotion, Facts, Relation }

internal fun commitmentCard(selected: FirstAnchor?): HomeAddedCard? = when (selected) {
    FirstAnchor.Rhythm -> HomeAddedCard.Rhythm
    FirstAnchor.EmotionLabel -> HomeAddedCard.Emotion
    FirstAnchor.FactsJournal -> HomeAddedCard.Facts
    FirstAnchor.SocialEnergy, FirstAnchor.AltruisticTask -> HomeAddedCard.Relation
    else -> null
}

internal fun layoutPracticeCards(
    selected: FirstAnchor?,
    addedInOrder: List<FirstAnchor>,
): Pair<HomeAddedCard?, List<HomeAddedCard>> {
    if (selected == null) {
        return HomeAddedCard.Rhythm to listOf(HomeAddedCard.Emotion, HomeAddedCard.Facts, HomeAddedCard.Relation)
    }
    return commitmentCard(selected) to bottomCards(selected, addedInOrder)
}

internal fun bottomCards(selected: FirstAnchor, addedInOrder: List<FirstAnchor>): List<HomeAddedCard> {
    val cards = mutableListOf<HomeAddedCard>()
    var relationShown = commitmentCard(selected) == HomeAddedCard.Relation
    for (anchor in addedInOrder) {
        if (anchor == selected) continue
        val card = when (anchor) {
            FirstAnchor.Rhythm -> HomeAddedCard.Rhythm
            FirstAnchor.EmotionLabel -> HomeAddedCard.Emotion
            FirstAnchor.FactsJournal -> HomeAddedCard.Facts
            FirstAnchor.SocialEnergy, FirstAnchor.AltruisticTask -> HomeAddedCard.Relation
            else -> null
        } ?: continue
        if (card == commitmentCard(selected)) continue
        if (card == HomeAddedCard.Relation) {
            if (relationShown) continue
            relationShown = true
        }
        if (card !in cards) cards += card
    }
    return cards
}

internal fun cardMatchesAnchor(card: HomeAddedCard, anchor: FirstAnchor?): Boolean = when (card) {
    HomeAddedCard.Rhythm -> anchor == FirstAnchor.Rhythm
    HomeAddedCard.Emotion -> anchor == FirstAnchor.EmotionLabel
    HomeAddedCard.Facts -> anchor == FirstAnchor.FactsJournal
    HomeAddedCard.Relation -> anchor == FirstAnchor.SocialEnergy || anchor == FirstAnchor.AltruisticTask
}
