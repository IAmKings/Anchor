package com.anchor.app

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.anchor.app.storage.InMemoryAnchorStore
import com.anchor.app.storage.buildLocalExport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import javax.crypto.AEADBadTagException

@RunWith(AndroidJUnit4::class)
class AndroidEncryptedExportTest {
    @Test
    fun roundTripAndReadOnlyShare() {
        val exporter = AndroidEncryptedExport(ApplicationProvider.getApplicationContext())
        val store = InMemoryAnchorStore().apply {
            addCameraLog("10:00 收到回复", "他可能不高兴", 1_000)
            addRhythmEntry(900, 950, 1_000)
        }
        val payload = buildLocalExport(store)
        val json = payload.json
        val csv = payload.csv
        val file = exporter.create("correct horse".toCharArray(), json, csv)

        assertFalse(file.readBytes().containsSubsequence("他可能不高兴".toByteArray()))
        assertEquals(mapOf("anchor.json" to json, "anchor.csv" to csv), exporter.decrypt(file, "correct horse".toCharArray()))
        runCatching { exporter.decrypt(file, "wrong password".toCharArray()) }
            .onSuccess { throw AssertionError("错误密码不应解密成功") }
            .onFailure { assertTrue(it is AEADBadTagException) }

        val intent = exporter.shareIntent(file)
        assertEquals(Intent.ACTION_SEND, intent.action)
        assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        assertTrue(intent.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION == 0)
        assertEquals("content", intent.clipData!!.getItemAt(0).uri.scheme)
    }

    private fun ByteArray.containsSubsequence(needle: ByteArray): Boolean =
        indices.any { start -> start + needle.size <= size && needle.indices.all { this[start + it] == needle[it] } }
}
