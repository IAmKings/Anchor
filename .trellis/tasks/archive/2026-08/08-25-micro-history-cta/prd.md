# 微行动历史证据行加高并守住文案

## Goal

`MicroActionHistoryScreen` 近期记录更好扫。偏差仍用「分 / 次数」，不用稿子 60%。情绪标签箱下一刀再做。

## Background

标题、引言、空态、引语、预测/实际标签已在 `MicroActionCopy`。记录行是默认内边距卡片。「返回」和箭头写在 composable 里。Stitch 中文稿用 60% 平均偏差、「已记录次数」、引语「这张表」、页内再放「查看历史记录」。产品 `biasCopy` 是「平均高估 N 分」；`historyOverestimateValue` 是「N 次」；引语是源文章「那张表」。本页就是历史证据，不再套一层查看按钮。不把洞察页的图搬过来，沿用页内 `PredictionLineChart`。

## Requirements

1. 近期记录行 `heightIn(min = 52.dp)`。
2. 抽出「返回」和引号包装；箭头抽出。`historyEvidenceQuote` 仍是「那张表」。
3. 偏差展示继续走 `biasCopy` / `historyOverestimateValue`，不加 %、完成率、已记录总次数 KPI。
4. 不加稿子页底「查看历史记录」，不改预测折线算法。
5. `MicroActionCopyTest` 覆盖：那张表、无 %、引言不含「启动通常是最难」。

## Acceptance Criteria

- [ ] 证据行最小高度 52.dp。
- [ ] 历史页用户可见中文走 `MicroActionCopy`（含返回与引语包装）。
- [ ] 引语是「那张表」；文案不含 % / 完成率。
- [ ] 没有页内「查看历史记录」按钮。
- [ ] `./gradlew :shared:testDebugUnitTest` 通过。

## Out of Scope

- 情绪标签箱（A6）。
- 把 `LocalInsights` 图表搬进本页。
- 改 `averagePredictionBias` 公式。
- `SafetyPolicy`、schema、热线。
