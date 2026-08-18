package com.anchor.app.storage

import com.anchor.app.safety.SafetyAction

internal fun String.toAnswers(): List<Int> = split(',').map(String::toInt)

internal fun String.toSafetyAction(): SafetyAction = when (this) {
    "Continue" -> SafetyAction.Continue
    "AskClarification" -> SafetyAction.AskClarification
    "CrisisGuidance" -> SafetyAction.CrisisGuidance
    "MedicalWaiting" -> SafetyAction.MedicalWaiting
    "ClinicalReview" -> SafetyAction.ClinicalReview
    else -> error("未知安全动作：$this")
}

internal fun SafetyAction.storageName(): String = when (this) {
    SafetyAction.Continue -> "Continue"
    SafetyAction.AskClarification -> "AskClarification"
    SafetyAction.CrisisGuidance -> "CrisisGuidance"
    SafetyAction.MedicalWaiting -> "MedicalWaiting"
    SafetyAction.ClinicalReview -> "ClinicalReview"
}

internal fun ByteArray.toSqlCipherKey(): String =
    "x'" + joinToString("") { byte ->
        byte.toUByte().toString(16).padStart(2, '0')
    } + "'"
