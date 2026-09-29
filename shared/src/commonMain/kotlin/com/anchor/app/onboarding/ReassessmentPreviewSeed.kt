package com.anchor.app.onboarding

import com.anchor.app.insights.averagePredictionBias
import com.anchor.app.insights.biasCopy
import com.anchor.app.insights.stabilityCopy
import com.anchor.app.insights.wakeStabilityMinutes
import com.anchor.app.storage.AnchorStore

data class PreviewMicroAction(
    val title: String,
    val predictedDifficulty: Int,
    val actualDifficulty: Int,
    val atMillis: Long,
)

data class ReassessmentPreviewReport(
    val appended: Boolean,
    val wakeValue: String,
    val biasValue: String,
    val biasDetail: String,
    val actionValue: String,
)

fun appendReassessmentPreviewIfEmpty(
    store: AnchorStore,
    nowMillis: Long,
    localMinuteOfDay: (Long) -> Int,
    wakesAtMillis: List<Long>,
    actions: List<PreviewMicroAction>,
): ReassessmentPreviewReport {
    val sampleAlreadyAdded = store.microActions().any { it.title.startsWith(SAMPLE_TITLE_PREFIX) }
    val wakeEntries = wakesInWindow(store, nowMillis)
    val existingActions = store.microActions()
    val cardsStillEmpty = wakeEntries.size < 2 ||
        overestimateCount(existingActions) == 0 ||
        recordedActionCount(existingActions) == 0
    val appended = !sampleAlreadyAdded && cardsStillEmpty
    if (appended) {
        wakesAtMillis.forEach { wake -> store.addRhythmEntry(wake, null, wake) }
        actions.forEach { action ->
            store.addMicroAction(action.title, action.atMillis)
            val id = store.microActions().first { it.title == action.title && it.completedAtMillis == null }.id
            store.startMicroAction(id, action.predictedDifficulty, action.atMillis)
            store.completeMicroAction(id, action.actualDifficulty, action.atMillis)
        }
    }
    return previewReport(store, nowMillis, localMinuteOfDay, appended)
}

private const val SAMPLE_TITLE_PREFIX = "示例："

private fun wakesInWindow(store: AnchorStore, nowMillis: Long) =
    store.rhythmEntries().filter { entry ->
        val wake = entry.wakeAtMillis
        wake != null && wake >= nowMillis - REASSESSMENT_INTERVAL_MILLIS
    }

private fun previewReport(
    store: AnchorStore,
    nowMillis: Long,
    localMinuteOfDay: (Long) -> Int,
    appended: Boolean,
): ReassessmentPreviewReport {
    val wakeEntries = wakesInWindow(store, nowMillis)
    val actions = store.microActions()
    return ReassessmentPreviewReport(
        appended = appended,
        wakeValue = stabilityCopy(wakeStabilityMinutes(wakeEntries, localMinuteOfDay)),
        biasValue = overestimateCopy(overestimateCount(actions)),
        biasDetail = biasCopy(averagePredictionBias(actions)),
        actionValue = recordedActionCopy(recordedActionCount(actions)),
    )
}
