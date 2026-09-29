package com.anchor.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.anchor.app.onboarding.PreviewMicroAction
import com.anchor.app.onboarding.appendReassessmentPreviewIfEmpty
import com.anchor.app.storage.AndroidDatabaseKey
import com.anchor.app.storage.AndroidEncryptedProbeStore
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/** Internal walkthrough only. Appends invite-card samples; does not touch assessments or safety state. */
class ReassessmentPreviewSeedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION) return
        val key = AndroidDatabaseKey.getOrCreate(context, DATABASE_NAME)
        try {
            AndroidEncryptedProbeStore(context, DATABASE_NAME, key).use { store ->
                val zone = ZoneId.systemDefault()
                val today = LocalDate.now(zone)
                fun at(daysAgo: Long, hour: Int, minute: Int): Long =
                    ZonedDateTime.of(today.minusDays(daysAgo), LocalTime.of(hour, minute), zone)
                        .toInstant()
                        .toEpochMilli()
                val report = appendReassessmentPreviewIfEmpty(
                    store = store,
                    nowMillis = System.currentTimeMillis(),
                    localMinuteOfDay = { millis ->
                        val time = java.time.Instant.ofEpochMilli(millis).atZone(zone)
                        time.hour * 60 + time.minute
                    },
                    wakesAtMillis = listOf(at(4, 7, 0), at(3, 7, 10), at(2, 7, 20), at(1, 7, 30)),
                    actions = listOf(
                        PreviewMicroAction("示例：走到楼下", 8, 3, at(4, 8, 0)),
                        PreviewMicroAction("示例：打开文档写一句", 6, 4, at(3, 8, 0)),
                        PreviewMicroAction("示例：倒一杯水", 5, 5, at(2, 8, 0)),
                        PreviewMicroAction("示例：把杯子放下", 4, 7, at(1, 8, 0)),
                    ),
                )
                resultData = if (report.appended) {
                    "appended wake=${report.wakeValue} bias=${report.biasValue} detail=${report.biasDetail} action=${report.actionValue}"
                } else {
                    "skipped wake=${report.wakeValue} bias=${report.biasValue} detail=${report.biasDetail} action=${report.actionValue}"
                }
            }
        } catch (error: RuntimeException) {
            resultData = "failed:${error.message}"
        } finally {
            key.fill(0)
        }
    }

    private companion object {
        const val ACTION = "com.anchor.app.action.SEED_REASSESSMENT_PREVIEW"
        const val DATABASE_NAME = "anchor.db"
    }
}
