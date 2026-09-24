package com.anchor.app.update

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

class UpdateCoordinatorTest {
    @Test
    fun secondCheckCancelsTheFirstAndKeepsOnlyTheLaterResult() = runBlocking {
        val firstEntered = CompletableDeferred<Unit>()
        val holdFirst = CompletableDeferred<Unit>()
        var calls = 0
        val coordinator = UpdateCoordinator(
            localVersionCode = 1,
            fetchFeed = {
                calls += 1
                if (calls == 1) {
                    firstEntered.complete(Unit)
                    holdFirst.await()
                    feed(code = 9, name = "9.0.0")
                } else {
                    feed(code = 2, name = "0.1.1")
                }
            },
            scope = this,
        )
        coordinator.check()
        withTimeout(2_000) { firstEntered.await() }
        coordinator.check()
        coordinator.joinLatest()
        val offer = coordinator.phase.value as UpdatePhase.UpdateAvailable
        assertEquals(2, offer.versionCode)
        assertEquals("0.1.1", offer.versionName)
        assertEquals(2, calls)
        assertTrue(coordinator.shouldPrompt())
        coordinator.dismiss()
        assertFalse(coordinator.shouldPrompt())
    }

    @Test
    fun failedFetchDoesNotBecomeAnOffer() = runBlocking {
        val coordinator = UpdateCoordinator(
            localVersionCode = 1,
            fetchFeed = { error("offline") },
            scope = this,
        )
        coordinator.check()
        coordinator.joinLatest()
        assertEquals(UpdatePhase.Failed, coordinator.phase.value)
        assertFalse(coordinator.shouldPrompt())
    }
}

private fun feed(code: Int, name: String): String = """
    [{"draft": false, "body": "anchor-version-code: $code\nanchor-version-name: $name\n",
      "assets": [{"name": "anchor-internal-$name.apk", "browser_download_url": "https://github.com/IAmKings/Anchor/releases/download/v$name/anchor-internal-$name.apk"}]}]
""".trimIndent()
