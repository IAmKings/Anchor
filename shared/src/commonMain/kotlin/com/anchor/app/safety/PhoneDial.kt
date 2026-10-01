package com.anchor.app.safety

private val PhoneSeparators = charArrayOf('/', '、', '；', ';', '或')

fun phoneTelUri(display: String): String? {
    if (display.any { it.isLetter() }) return null
    val body = display.filter { it.isDigit() || it == '+' }
    val normalized = if (body.startsWith("+")) {
        "+" + body.drop(1).filter { it.isDigit() }
    } else {
        body.filter { it.isDigit() }
    }
    val digits = normalized.count { it.isDigit() }
    if (digits !in 3..15) return null
    return "tel:$normalized"
}

fun dialableNumbers(text: String): List<String> =
    text.split(*PhoneSeparators).map { it.trim() }.filter { it.isNotEmpty() && phoneTelUri(it) != null }

fun isEntirelyDialable(text: String): Boolean {
    val parts = text.split(*PhoneSeparators).map { it.trim() }.filter { it.isNotEmpty() }
    return parts.isNotEmpty() && parts.all { phoneTelUri(it) != null }
}

fun GuideHotline.phoneNumbers(): List<String> =
    (dialableNumbers(number) + dialableNumbers(detail)).distinct()

fun GuideHotline.note(): String = if (dialableNumbers(detail).isEmpty()) detail else ""
