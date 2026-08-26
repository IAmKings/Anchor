package com.anchor.app

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.anchor.app.storage.InMemoryAnchorStore
import com.anchor.app.storage.buildLocalExport
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import javax.crypto.AEADBadTagException

@RunWith(AndroidJUnit4::class)
class AndroidEncryptedExportTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val exporter = AndroidEncryptedExport(context, AndroidEncryptedExport.INSTRUMENTED_CACHE_FOLDER)

    @After
    fun deleteInstrumentedExportCache() {
        File(context.cacheDir, AndroidEncryptedExport.INSTRUMENTED_CACHE_FOLDER).deleteRecursively()
    }

    @Test
    fun writesOnlyInstrumentedExportCache() {
        assertNotEquals(AndroidEncryptedExport.CACHE_FOLDER, AndroidEncryptedExport.INSTRUMENTED_CACHE_FOLDER)
        val production = File(context.cacheDir, AndroidEncryptedExport.CACHE_FOLDER)
        val before = production.snapshot()

        val store = InMemoryAnchorStore().apply {
            addCameraLog("10:00 收到回复", "他可能不高兴", 1_000)
        }
        val payload = buildLocalExport(store)
        val file = exporter.create("correct horse".toCharArray(), payload.json, payload.csv)

        assertEquals(AndroidEncryptedExport.INSTRUMENTED_CACHE_FOLDER, file.parentFile?.name)
        assertEquals(before, production.snapshot())
        assertTrue(file.exists())
    }

    @Test
    fun roundTripAndReadOnlyShare() {
        val store = InMemoryAnchorStore().apply {
            addCameraLog("10:00 收到回复", "他可能不高兴", 1_000)
            addRhythmEntry(900, 950, 1_000)
        }
        val payload = buildLocalExport(store)
        val json = payload.json
        val csv = payload.csv
        val file = exporter.create("correct horse".toCharArray(), json, csv)

        assertFalse(file.readBytes().containsSubsequence("他可能不高兴".toByteArray()))
        val decrypted = exporter.decrypt(file, "correct horse".toCharArray())
        assertEquals(json, decrypted.json)
        assertEquals(csv, decrypted.csv)
        assertTrue(decrypted.audio.isEmpty())
        runCatching { exporter.decrypt(file, "wrong password".toCharArray()) }
            .onSuccess { throw AssertionError("错误密码不应解密成功") }
            .onFailure { assertTrue(it is AEADBadTagException) }

        val intent = exporter.shareIntent(file)
        assertEquals(Intent.ACTION_SEND, intent.action)
        assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        assertTrue(intent.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION == 0)
        assertEquals("content", intent.clipData!!.getItemAt(0).uri.scheme)
    }

    @Test
    fun roundTripsAudioBytesWithoutDevicePaths() {
        val bytes = byteArrayOf(0x00, 0x01, 0x4D, 0x34, 0x41)
        val file = exporter.create(
            password = "correct horse".toCharArray(),
            json = """{"exportVersion":4}""",
            csv = "type,createdAt,data\n",
            audio = mapOf("voice-9.m4a" to bytes),
        )
        val decrypted = exporter.decrypt(file, "correct horse".toCharArray())
        assertTrue(decrypted.audio["voice-9.m4a"].contentEquals(bytes))
        assertTrue(!file.readBytes().containsSubsequence("voice-notes".toByteArray()))
    }

    private fun File.snapshot(): Set<Pair<String, Long>> =
        listFiles()?.map { it.name to it.length() }?.toSet().orEmpty()

    private fun ByteArray.containsSubsequence(needle: ByteArray): Boolean =
        indices.any { start -> start + needle.size <= size && needle.indices.all { this[start + it] == needle[it] } }
}
