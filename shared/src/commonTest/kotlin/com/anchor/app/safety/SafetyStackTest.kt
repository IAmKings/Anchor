package com.anchor.app.safety

import kotlin.test.Test
import kotlin.test.assertEquals

class SafetyStackTest {
    @Test
    fun crisisChildrenReturnToCrisisThenHome() {
        var stack = pushSafety(emptyList(), SafetyPlace.Help)
        stack = pushSafety(stack, SafetyPlace.Guide(preview = false))
        stack = pushSafety(stack, SafetyPlace.Checklist)
        assertEquals(SafetyPlace.Checklist, stack.last())
        stack = popSafety(stack)
        assertEquals(SafetyPlace.Guide(preview = false), stack.last())
        stack = popSafety(stack)
        assertEquals(SafetyPlace.Help, stack.last())
        stack = popSafety(stack)
        assertEquals(emptyList(), stack)
    }

    @Test
    fun guideOpenedFromHomeReturnsHomeNotCrisis() {
        var stack = pushSafety(emptyList(), SafetyPlace.Guide(preview = false))
        stack = pushSafety(stack, SafetyPlace.Checklist)
        stack = popSafety(stack)
        assertEquals(listOf(SafetyPlace.Guide(preview = false)), stack)
        assertEquals(emptyList(), popSafety(stack))
    }

    @Test
    fun journalPopsBackToTheGuide() {
        val stack = pushSafety(
            pushSafety(emptyList(), SafetyPlace.Help),
            SafetyPlace.Guide(preview = true),
        )
        val withJournal = pushSafety(stack, SafetyPlace.Journal)
        assertEquals(stack, popSafety(withJournal))
    }

    @Test
    fun popOnEmptyStackStaysEmpty() {
        assertEquals(emptyList(), popSafety(emptyList()))
    }

    @Test
    fun scrollKeyStaysForPlacesStillUnderTheChild() {
        val stack = listOf(
            SafetyPlace.Help,
            SafetyPlace.Guide(preview = false),
            SafetyPlace.Checklist,
        )
        assertEquals(listOf("help", "guide", "checklist"), stack.map(::safetyStateKey))
        assertEquals(listOf("help", "guide"), popSafety(stack).map(::safetyStateKey))
        assertEquals("guide-preview", safetyStateKey(SafetyPlace.Guide(preview = true)))
    }
}
