package com.anchor.app.safety

internal sealed interface SafetyPlace {
    data object Help : SafetyPlace
    data class Guide(val preview: Boolean) : SafetyPlace
    data object Checklist : SafetyPlace
    data object Journal : SafetyPlace
}

internal fun pushSafety(stack: List<SafetyPlace>, place: SafetyPlace): List<SafetyPlace> = stack + place

internal fun popSafety(stack: List<SafetyPlace>): List<SafetyPlace> =
    if (stack.isEmpty()) emptyList() else stack.dropLast(1)

internal fun safetyStateKey(place: SafetyPlace): String = when (place) {
    SafetyPlace.Help -> "help"
    is SafetyPlace.Guide -> if (place.preview) "guide-preview" else "guide"
    SafetyPlace.Checklist -> "checklist"
    SafetyPlace.Journal -> "journal"
}
