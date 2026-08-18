package com.anchor.app.relation

import com.anchor.app.storage.AltruismDraw
import com.anchor.app.storage.AltruismFeel
import com.anchor.app.storage.AltruismKind
import com.anchor.app.storage.EnergyMark
import com.anchor.app.storage.InMemoryAnchorStore
import com.anchor.app.storage.RelationContact
import com.anchor.app.storage.RelationEnergyEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RelationCopyTest {
    @Test
    fun defaultDrawPrefersNonSocialAndSkipsSocialWhenRecentlyTight() {
        val empty = nextAltruismCard(emptyList(), preferSocial = false)
        assertEquals(AltruismKind.NonSocial, empty.kind)
        val tight = List(3) { index ->
            AltruismDraw(index.toLong(), "x$index", AltruismKind.Social, 1, AltruismFeel.Tighter, 2)
        }
        assertEquals(AltruismKind.NonSocial, nextAltruismCard(tight, preferSocial = true).kind)
        assertTrue(shouldPauseAltruism(tight))
        assertFalse(shouldPauseAltruism(emptyList()))
    }

    @Test
    fun adviceIsNotABreakupAndCopyHasNoKpi() {
        assertTrue(reliefAdvice("导师").contains("不是断交"))
        assertTrue(leavingCopy.contains("离开"))
        assertFalse(energyCopy(emptyList()).contains("%"))
        assertEquals("还没有需要监控自己的关系", monitorCopy(emptyList()))
        assertEquals(
            "1 段需要监控自己",
            monitorCopy(listOf(RelationContact(1, "A", null, true, 1))),
        )
        assertEquals(
            "回血 1 · 抽干 1",
            energyCopy(
                listOf(
                    RelationEnergyEntry(1, 1, EnergyMark.Filled, 1),
                    RelationEnergyEntry(2, 1, EnergyMark.Drained, 2),
                ),
            ),
        )
    }

    @Test
    fun storePersistsInventoryEnergyAndAltruismFeel() {
        val store = InMemoryAnchorStore()
        store.addRelationContact("导师", "工作", 1_000)
        val contact = store.relationContacts().single()
        store.setRelationMonitorsSelf(contact.id, true)
        store.addRelationEnergy(contact.id, EnergyMark.Drained, 2_000)
        store.addAltruismDraw("给一盆植物浇水", AltruismKind.NonSocial, 3_000)
        val draw = store.altruismDraws().single()
        store.completeAltruismDraw(draw.id, AltruismFeel.Lighter, 4_000)
        assertEquals(true, store.relationContacts().single().monitorsSelf)
        assertEquals(EnergyMark.Drained, store.relationEnergy().single().mark)
        assertEquals(AltruismFeel.Lighter, store.altruismDraws().single().felt)
    }

    @Test
    fun homeRelationStatesPreferGuardrailThenRecentEnergy() {
        val contact = RelationContact(1, "导师", null, true, 1)
        val now = 10_000_000L
        val filled = homeRelationPresentation(
            contacts = listOf(contact),
            energy = listOf(RelationEnergyEntry(1, 1, EnergyMark.Filled, now - 1_000)),
            draws = emptyList(),
            nowMillis = now,
        )
        assertEquals(HomeRelationKind.Filled, filled.kind)
        assertEquals(homeRelationFilledTitle, filled.bannerTitle)
        assertFalse(filled.bannerBody.orEmpty().contains("%"))

        val drained = homeRelationPresentation(
            contacts = listOf(contact),
            energy = listOf(RelationEnergyEntry(1, 1, EnergyMark.Drained, now - 1_000)),
            draws = emptyList(),
            nowMillis = now,
        )
        assertEquals(HomeRelationKind.Drained, drained.kind)
        assertTrue(drained.bannerBody.orEmpty().contains("不是断交"))

        val exhaustedDraws = List(3) { index ->
            AltruismDraw(index.toLong(), "x$index", AltruismKind.Social, now, AltruismFeel.Tighter, now)
        }
        val exhausted = homeRelationPresentation(
            contacts = listOf(contact),
            energy = listOf(RelationEnergyEntry(1, 1, EnergyMark.Filled, now - 1_000)),
            draws = exhaustedDraws,
            nowMillis = now,
        )
        assertEquals(HomeRelationKind.Exhausted, exhausted.kind)
        assertEquals(homeRelationExhaustedLabel, exhausted.cardLabel)
        assertTrue(listOf(filled, drained, exhausted).none { presentation ->
            listOf(presentation.bannerTitle, presentation.bannerBody, presentation.cardBody, presentation.cardHint)
                .filterNotNull()
                .any { it.contains("电量") || it.contains("完成率") || it.contains("%") || it.contains("streak") }
        })

        val stale = homeRelationPresentation(
            contacts = emptyList(),
            energy = listOf(RelationEnergyEntry(1, 1, EnergyMark.Filled, now - homeRelationBannerWindowMillis - 1)),
            draws = emptyList(),
            nowMillis = now,
        )
        assertEquals(HomeRelationKind.Default, stale.kind)
    }
}
