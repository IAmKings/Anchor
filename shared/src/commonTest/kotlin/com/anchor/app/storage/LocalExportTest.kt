package com.anchor.app.storage

import kotlin.test.Test
import kotlin.test.assertContains

class LocalExportTest {
    @Test
    fun exportsRealRecordsAndEscapesJsonAndCsv() {
        val store = InMemoryAnchorStore()
        store.saveUserProfile(UserProfile(crisisRegion = CrisisRegion.Japan))
        store.addCameraLog("他说 \"好\"", "第一行\n第二行", 1_000)
        store.addRhythmEntry(900, null, 1_000)

        val export = buildLocalExport(store)

        assertContains(export.json, "\"exportVersion\":3")
        assertContains(export.json, "他说 \\\"好\\\"")
        assertContains(export.json, "第一行\\n第二行")
        assertContains(export.json, "\"wakeAt\":900")
        assertContains(export.json, "\"crisisRegion\":\"Japan\"")
        assertContains(export.csv, "\"他说 \"\"好\"\"")
        assertContains(export.csv, "\"rhythm\"")
    }
}
