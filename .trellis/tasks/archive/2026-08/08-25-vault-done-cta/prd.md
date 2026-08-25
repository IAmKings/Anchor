# 忧虑完成页按钮加高并抽出文案

## Goal

忧虑专场 `DoneStep`：「回到今天」更好点。摘要仍只计受理张数，不加完成率。微行动页下一刀再做。

## Background

「回到今天」是默认高度 Button。标题、安抚句、页脚写在 composable 里。`vaultDoneSummary` 已抽出。Stitch 完成页是 `h-14`「回到今天」，另有「查看计划」。产品没有计划页，「查看计划」不进本刀。

已抽出且本刀不改：Overview / Process / Convert、`vaultDoneSummary`。

## Requirements

1. 「回到今天」用 `heightIn(min = 52.dp)`，字号 17sp。
2. 抽出 Done 可见文案到 `WorryCopy`（整理完毕、安抚句、回到今天、受理而非压抑）。摘要继续用 `vaultDoneSummary`。
3. 不加稿子「查看计划」，不加完成率 / 打卡 / streak。
4. `WorryCopyTest` 覆盖新文案；摘要仍不含完成率。

## Acceptance Criteria

- [ ] 回到今天最小高度 52.dp。
- [ ] Done 用户可见中文走 `WorryCopy`。
- [ ] 没有「查看计划」；`vaultDoneSummary` 仍是「今日受理了 N 项忧虑」。
- [ ] 不改 Convert / Process / Overview。
- [ ] `./gradlew :shared:testDebugUnitTest` 通过。

## Out of Scope

- 微行动挑选 / 计时（A4）。
- Stitch 水滴插画、查看计划。
- `SafetyPolicy`、schema、热线。
