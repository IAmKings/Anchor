package com.anchor.app.widget

import com.anchor.app.storage.InMemoryAnchorStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TodayWidgetModelTest {

    private fun storeWithRunningAction(startedAtWall: Long): InMemoryAnchorStore =
        InMemoryAnchorStore().apply {
            addMicroAction("整理书桌", 1_000)
            startMicroAction(1, 5, startedAtWall)
        }

    private val fiveMinutes = 5L * 60 * 1_000

    @Test
    fun worryLineIsCountWithAcceptancePhraseOrInvitation() {
        val store = InMemoryAnchorStore().apply {
            addWorryCard("一张卡", 1_000, 2_000, null)
            addWorryCard("另一张", 3_000, 4_000, null)
        }
        val withCards = todayWidgetModel(store, wallNow = 10_000, elapsedNow = 10_000)
        assertEquals("2张卡，都在纸上", withCards.worryLine)

        val empty = InMemoryAnchorStore()
        val none = todayWidgetModel(empty, wallNow = 10_000, elapsedNow = 10_000)
        assertEquals("点这里把念头放进去", none.worryLine)
    }

    @Test
    fun countdownRenderedOnlyOutsideSafetyMargin() {
        val store = storeWithRunningAction(startedAtWall = 60_000)
        // 剩余 10 秒（> 余量 3 秒）：渲染 Chronometer，base = elapsedNow + remaining，并安排 R2 提前刷新
        val outside = todayWidgetModel(store, wallNow = 60_000 + fiveMinutes - 10_000, elapsedNow = 100_000)
        assertEquals(100_000 + 10_000, outside.microCountdownBase)
        assertEquals(60_000 + fiveMinutes - 3_000, outside.microCountdownRefreshAtWall)

        // R1 不变量：剩余进入安全余量（1 秒 ≤ 3 秒）→ 一律结束态，绝不渲染倒计时
        val inside = todayWidgetModel(store, wallNow = 60_000 + fiveMinutes - 1_000, elapsedNow = 200_000)
        assertNull(inside.microCountdownBase)
        assertNull(inside.microCountdownRefreshAtWall)

        // 过零后（墙钟已超 5 分钟）：同样结束态——「负数即结束」数据不变量
        val past = todayWidgetModel(store, wallNow = 60_000 + fiveMinutes + 5_000, elapsedNow = 300_000)
        assertNull(past.microCountdownBase)
    }

    @Test
    fun idleAndWaitingStatesNeverRenderCountdown() {
        val empty = InMemoryAnchorStore()
        val idle = todayWidgetModel(empty, wallNow = 1_000, elapsedNow = 1_000)
        assertNull(idle.microCountdownBase)

        val done = InMemoryAnchorStore().apply {
            addMicroAction("整理书桌", 1_000)
            startMicroAction(1, 5, 60_000)
            completeMicroAction(1, 3, 61_000)
        }
        val completed = todayWidgetModel(done, wallNow = 70_000, elapsedNow = 5_000)
        assertNull(completed.microCountdownBase)
    }
}
