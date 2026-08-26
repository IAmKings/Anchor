package com.anchor.app.storage

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LocalExportTest {
    @Test
    fun exportsRealRecordsAndEscapesJsonAndCsv() {
        val store = InMemoryAnchorStore()
        store.saveUserProfile(UserProfile(crisisRegion = CrisisRegion.Japan))
        store.addCameraLog("他说 \"好\"", "第一行\n第二行", 1_000)
        store.addRhythmEntry(900, null, 1_000)

        val export = buildLocalExport(store)

        assertContains(export.json, "\"exportVersion\":4")
        assertContains(export.json, "他说 \\\"好\\\"")
        assertContains(export.json, "第一行\\n第二行")
        assertContains(export.json, "\"wakeAt\":900")
        assertContains(export.json, "\"crisisRegion\":\"Japan\"")
        assertContains(export.csv, "\"他说 \"\"好\"\"")
        assertContains(export.csv, "\"rhythm\"")
    }

    @Test
    fun worryAudioExportNameIsBasenameOnly() {
        assertEquals("voice-1.m4a", worryAudioExportName("voice-1.m4a"))
        assertEquals(null, worryAudioExportName("/data/user/0/com.anchor.app/files/voice-notes/voice-1.m4a"))
        assertEquals(null, worryAudioExportName("../secret.m4a"))
        assertEquals(null, worryAudioExportName("clip.wav"))
        assertEquals(null, worryAudioExportName("a/b.m4a"))
        val store = InMemoryAnchorStore()
        store.addWorryCard("", 1_000, 2_000, "voice-9.m4a")
        val json = buildLocalExport(store).json
        assertContains(json, "\"hasAudio\":true")
        assertContains(json, "\"audioName\":\"voice-9.m4a\"")
        assertTrue(!json.contains("/voice-notes/"))
        assertTrue(!json.contains("files/"))
    }
}
