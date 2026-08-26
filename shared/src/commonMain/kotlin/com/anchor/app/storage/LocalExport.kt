package com.anchor.app.storage

import com.anchor.app.safety.SafetyAction

data class LocalExport(val json: String, val csv: String)

internal const val CURRENT_EXPORT_VERSION = 4
internal const val MIN_EXPORT_VERSION = 2

fun worryAudioExportName(fileName: String?): String? {
    val base = fileName?.trim().orEmpty()
    if (base.isEmpty() || ".." in base || '/' in base || '\\' in base) return null
    if (!base.matches(Regex("[A-Za-z0-9._-]+\\.m4a"))) return null
    return base
}

fun buildLocalExport(store: AnchorStore): LocalExport {
    val profile = store.userProfile()
    val safety = store.safetyState()
    val assessments = store.assessments()
    val emotions = store.emotionCards()
    val cameraLogs = store.cameraLogs()
    val worries = store.worryCards()
    val actions = store.microActions()
    val rhythm = store.rhythmEntries()
    val contacts = store.relationContacts()
    val energy = store.relationEnergy()
    val altruism = store.altruismDraws()

    val json = """{"exportVersion":$CURRENT_EXPORT_VERSION,"userProfile":{"ageGroup":${profile.ageGroup?.name.jsonOrNull()},"onboardingComplete":${profile.onboardingComplete},"firstAnchor":${profile.firstAnchor?.name.jsonOrNull()},"firstAnchorAt":${profile.firstAnchorAtMillis.numberOrNull()}},"safetyState":{"mode":${safety.mode.name.json()},"firstLowAssessmentAt":${safety.firstLowAssessmentAtMillis.numberOrNull()}},"assessments":[${assessments.joinToString { a -> """{"completedAt":${a.completedAtMillis},"phq9":[${a.phq9.joinToString()}],"gad7":[${a.gad7.joinToString()}],"phq9Score":${a.phq9Score},"phq9Band":${a.phq9Band.name.json()},"gad7Score":${a.gad7Score},"gad7Band":${a.gad7Band.name.json()},"action":${a.action.exportName().json()}}""" }}],"emotionCards":[${emotions.joinToString { e -> """{"id":${e.id},"emotion":${e.emotion.json()},"event":${e.event.json()},"hardestPart":${e.hardestPart.json()},"createdAt":${e.createdAtMillis},"passedAt":${e.passedAtMillis.numberOrNull()}}""" }}],"cameraLogs":[${cameraLogs.joinToString { c -> """{"id":${c.id},"fact":${c.fact.json()},"inference":${c.inference.json()},"factNeedsHint":${c.factNeedsHint},"createdAt":${c.createdAtMillis}}""" }}],"worries":[${worries.joinToString { w -> """{"id":${w.id},"content":${w.content.json()},"sealedAt":${w.sealedAtMillis},"nextSessionAt":${w.nextSessionAtMillis},"resolution":${w.resolution.name.json()},"action":${w.action.jsonOrNull()},"hasAudio":${w.audioFileName != null},"audioName":${worryAudioExportName(w.audioFileName).jsonOrNull()}}""" }}],"microActions":[${actions.joinToString { a -> """{"id":${a.id},"title":${a.title.json()},"sourceWorryId":${a.sourceWorryId.numberOrNull()},"createdAt":${a.createdAtMillis},"predictedDifficulty":${a.predictedDifficulty.numberOrNull()},"startedAt":${a.startedAtMillis.numberOrNull()},"actualDifficulty":${a.actualDifficulty.numberOrNull()},"completedAt":${a.completedAtMillis.numberOrNull()}}""" }}],"rhythmEntries":[${rhythm.joinToString { r -> """{"id":${r.id},"wakeAt":${r.wakeAtMillis.numberOrNull()},"lightAt":${r.lightAtMillis.numberOrNull()},"createdAt":${r.createdAtMillis}}""" }}],"relationContacts":[${contacts.joinToString { c -> """{"id":${c.id},"name":${c.name.json()},"note":${c.note.jsonOrNull()},"monitorsSelf":${c.monitorsSelf},"createdAt":${c.createdAtMillis}}""" }}],"relationEnergy":[${energy.joinToString { e -> """{"id":${e.id},"contactId":${e.contactId},"mark":${e.mark.name.json()},"createdAt":${e.createdAtMillis}}""" }}],"altruismDraws":[${altruism.joinToString { d -> """{"id":${d.id},"title":${d.title.json()},"kind":${d.kind.name.json()},"drawnAt":${d.drawnAtMillis},"felt":${d.felt?.name.jsonOrNull()},"completedAt":${d.completedAtMillis.numberOrNull()}}""" }}]}"""

    val rows = buildList {
        assessments.forEach { add(listOf("assessment", it.completedAtMillis, "PHQ=${it.phq9Score};GAD=${it.gad7Score}")) }
        emotions.forEach { add(listOf("emotion", it.createdAtMillis, "${it.emotion} | ${it.event} | ${it.hardestPart}")) }
        cameraLogs.forEach { add(listOf("camera_log", it.createdAtMillis, "${it.fact} | ${it.inference}")) }
        worries.forEach { add(listOf("worry", it.sealedAtMillis, it.content.ifBlank { "[本地录音]" })) }
        actions.forEach { add(listOf("micro_action", it.createdAtMillis, it.title)) }
        rhythm.forEach { add(listOf("rhythm", it.createdAtMillis, "wake=${it.wakeAtMillis};light=${it.lightAtMillis}")) }
        contacts.forEach { add(listOf("relation", it.createdAtMillis, it.name)) }
        energy.forEach { add(listOf("energy", it.createdAtMillis, it.mark.name)) }
        altruism.forEach { add(listOf("altruism", it.drawnAtMillis, it.title)) }
    }
    val csv = buildString {
        append("type,createdAt,data\n")
        rows.forEach { row -> append(row.joinToString(",") { it.toString().csv() }).append('\n') }
    }
    val exportedJson = json.replace(
        "\"safetyState\"",
        "\"crisisRegion\":${profile.crisisRegion.name.json()},\"safetyState\"",
    )
    return LocalExport(exportedJson, csv)
}

private fun String.json(): String = buildString {
    append('"')
    for (character in this@json) when (character) {
        '"' -> append("\\\"")
        '\\' -> append("\\\\")
        '\b' -> append("\\b")
        '\u000C' -> append("\\f")
        '\n' -> append("\\n")
        '\r' -> append("\\r")
        '\t' -> append("\\t")
        else -> if (character.code < 0x20) append("\\u${character.code.toString(16).padStart(4, '0')}") else append(character)
    }
    append('"')
}

private fun String?.jsonOrNull(): String = this?.json() ?: "null"
private fun Number?.numberOrNull(): String = this?.toString() ?: "null"
private fun String.csv(): String = "\"${replace("\"", "\"\"")}\""

private fun SafetyAction.exportName(): String = when (this) {
    SafetyAction.Continue -> "Continue"
    SafetyAction.AskClarification -> "AskClarification"
    SafetyAction.CrisisGuidance -> "CrisisGuidance"
    SafetyAction.MedicalWaiting -> "MedicalWaiting"
    SafetyAction.ClinicalReview -> "ClinicalReview"
}
