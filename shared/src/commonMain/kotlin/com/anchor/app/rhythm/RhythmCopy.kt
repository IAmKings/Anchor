package com.anchor.app.rhythm

import com.anchor.app.storage.RhythmEntry

internal const val RHYTHM_ONE_HOUR = 60L * 60 * 1_000

internal fun lightIsLaterThanOneHour(wakeAt: Long?, lightAt: Long?): Boolean =
    wakeAt != null && lightAt != null && lightAt - wakeAt > RHYTHM_ONE_HOUR

internal fun lateLightCopy(): String =
    "今天见光稍晚；阴天也算见光，明天仍然可以重新记录。"

internal fun rhythmSavedCopy(): String = "已保存。暂停或漏记都很正常。"

internal const val rhythmBackLabel = "返回"
internal const val rhythmTitle = "晨间节律"
internal const val rhythmIntro = "起得好不好不重要。为今天留下两个时间点。"
internal const val rhythmWakeTitle = "起床时间"
internal const val rhythmLightTitle = "见光时间"
internal const val rhythmWakeGlyph = "起"
internal const val rhythmLightGlyph = "光"
internal const val rhythmWakeEmptyCaption = "点一下记下此刻"
internal const val rhythmWakeRecordedCaption = "今天"
internal const val rhythmLightEmptyCaption = "阴天也算见光"
internal const val rhythmLightRecordedCaption = "起床后一小时内更好"
internal const val rhythmSaveLabel = "保存今天的记录"
internal const val rhythmBedHint = "睡不好也不用补觉。躺了 20 分钟仍睡不着，可以起来做点无聊的事，困了再回床。"
internal const val rhythmRecentTitle = "近期记录"
internal const val rhythmEmpty = "还没有记录。漏记也很正常。"
internal const val rhythmStabilityTitle = "30 日稳定度"
internal const val rhythmStabilityDetail = "起床时间的温和波动，不是得分。"
internal const val rhythmTimePlaceholder = "--:--"
internal const val rhythmUnrecorded = "未记"
internal const val rhythmWakeRecordDescription = "记下起床时间"
internal const val rhythmLightRecordDescription = "记下见光时间"

internal fun rhythmWakeCaption(recorded: Boolean): String =
    if (recorded) rhythmWakeRecordedCaption else rhythmWakeEmptyCaption

internal fun rhythmLightCaption(recorded: Boolean): String =
    if (recorded) rhythmLightRecordedCaption else rhythmLightEmptyCaption

internal fun rhythmWakeLine(time: String): String = "起床 $time"

internal fun rhythmLightLine(time: String): String = "见光 $time"

internal fun wakeMinutesOldestFirst(entries: List<RhythmEntry>, localMinuteOfDay: (Long) -> Int): List<Int> =
    entries.mapNotNull { it.wakeAtMillis }.take(30).map(localMinuteOfDay).reversed()
