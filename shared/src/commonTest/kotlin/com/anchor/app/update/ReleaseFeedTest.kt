package com.anchor.app.update

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReleaseFeedTest {
    @Test
    fun picksTheHighestVersionCodeAndIgnoresDraftsAndForeignUrls() {
        val releases = parseReleaseFeed(sampleFeed)
        assertEquals(listOf(2), releases.map { it.versionCode })
        val offer = interpretFeed(1, releases) as UpdatePhase.UpdateAvailable
        assertEquals("0.1.1", offer.versionName)
        assertEquals(2, offer.versionCode)
        assertTrue(offer.apkUrl.endsWith("anchor-internal-0.1.1.apk"))
    }

    @Test
    fun equalOrLowerCodeIsCurrent() {
        val releases = parseReleaseFeed(sampleFeed)
        assertEquals(UpdatePhase.UpToDate, interpretFeed(2, releases))
        assertEquals(UpdatePhase.UpToDate, interpretFeed(4, releases))
    }

    @Test
    fun emptyOrUnreadableFeedFailsClosed() {
        assertEquals(UpdatePhase.Failed, interpretFeed(1, emptyList()))
        assertFails { parseReleaseFeed("not-json") }
        assertEquals(UpdatePhase.Failed, interpretFeed(1, parseReleaseFeed("[]")))
    }

    @Test
    fun versionNameDoesNotOutrankVersionCode() {
        val feed = """
            [
              {"draft": false, "body": "anchor-version-code: 2\nanchor-version-name: 0.9.0\n",
               "assets": [{"name": "anchor-internal-0.9.0.apk", "browser_download_url": "https://github.com/IAmKings/Anchor/releases/download/v0.9.0/anchor-internal-0.9.0.apk"}]},
              {"draft": false, "body": "anchor-version-code: 3\nanchor-version-name: 0.1.2\n",
               "assets": [{"name": "anchor-internal-0.1.2.apk", "browser_download_url": "https://github.com/IAmKings/Anchor/releases/download/v0.1.2/anchor-internal-0.1.2.apk"}]}
            ]
        """.trimIndent()
        val offer = interpretFeed(1, parseReleaseFeed(feed)) as UpdatePhase.UpdateAvailable
        assertEquals(3, offer.versionCode)
        assertEquals("0.1.2", offer.versionName)
    }

    @Test
    fun promptCopyDoesNotUrge() {
        val body = updatePromptBody("0.1.1", "0.1.0")
        assertEquals("测试版 0.1.1 已发布。现在是 0.1.0。", body)
        assertFalse(body.contains("必须"))
        assertEquals("这次没对上，可以再试。", updateStatusLine(UpdatePhase.Failed))
        assertEquals("已是当前测试版。", updateStatusLine(UpdatePhase.UpToDate))
        assertFalse(settingsUpdateNote.contains("上传记录").not())
    }
}

private const val sampleFeed = """
[
  {
    "draft": true,
    "body": "anchor-version-code: 9\nanchor-version-name: 9.0.0\n",
    "assets": [{"name": "anchor-internal-9.apk", "browser_download_url": "https://github.com/IAmKings/Anchor/releases/download/v9/anchor-internal-9.apk"}]
  },
  {
    "draft": false,
    "body": "说明在前\nanchor-version-code: 2\nanchor-version-name: 0.1.1\n",
    "assets": [
      {"name": "note.txt", "browser_download_url": "https://github.com/IAmKings/Anchor/releases/download/v0.1.1/note.txt"},
      {"name": "anchor-internal-0.1.1.apk", "browser_download_url": "https://github.com/IAmKings/Anchor/releases/download/v0.1.1/anchor-internal-0.1.1.apk"}
    ]
  },
  {
    "draft": false,
    "body": "anchor-version-code: 3\nanchor-version-name: 0.2.0\n",
    "assets": [{"name": "anchor-internal-0.2.0.apk", "browser_download_url": "http://evil.example/a.apk"}]
  }
]
"""
