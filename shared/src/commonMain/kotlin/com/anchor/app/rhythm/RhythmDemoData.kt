package com.anchor.app.rhythm

import com.anchor.app.storage.AnchorStore
import kotlin.random.Random

// 仅测试包使用（debugTools 门控）：注入 30 天带抖动的起床/见光数据，
// 让洞察页「30 日起床稳定度」与「三个月移动平均」两张图有形状可观察。
// 固定随机种子保证每次注入的曲线一致；已有 30 条以上时不重复注入。
fun seedRhythmDemoData(store: AnchorStore, nowMillis: Long, localMinuteOfDay: (Long) -> Int): Int {
    if (store.rhythmEntries().size >= 30) return 0
    val random = Random(20261010)
    val minutesSinceSeven = (localMinuteOfDay(nowMillis) - 7 * 60 + 1_440).mod(1_440)
    val todaySeven = nowMillis - minutesSinceSeven * 60_000L
    var seeded = 0
    for (daysAgo in 30 downTo 1) {
        val sevenOclock = todaySeven - daysAgo * 86_400_000L
        val wake = sevenOclock + random.nextInt(-35, 36) * 60_000L
        val light = wake + random.nextInt(25, 91) * 60_000L
        store.addRhythmEntry(wake, light, light + 5 * 60_000L)
        seeded++
    }
    return seeded
}
