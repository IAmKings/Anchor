package com.anchor.app.rhythm

import com.anchor.app.storage.RhythmEntry

internal const val RHYTHM_ONE_HOUR = 60L * 60 * 1_000

internal fun lightIsLaterThanOneHour(wakeAt: Long?, lightAt: Long?): Boolean =
    wakeAt != null && lightAt != null && lightAt - wakeAt > RHYTHM_ONE_HOUR

internal fun lateLightCopy(): String =
    "今天见光稍晚；阴天也算见光，明天仍然可以重新记录。"

internal fun rhythmSavedCopy(): String = "已保存。暂停或漏记都很正常。"

internal fun wakeMinutesOldestFirst(entries: List<RhythmEntry>, localMinuteOfDay: (Long) -> Int): List<Int> =
    entries.mapNotNull { it.wakeAtMillis }.take(30).map(localMinuteOfDay).reversed()
