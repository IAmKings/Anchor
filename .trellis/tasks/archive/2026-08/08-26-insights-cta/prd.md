# 洞察页按钮加高并抽出文案

## Goal

洞察页「查看历史记录」更好点。数字仍是分/次数/标准差，不是完成率或诊断。设置页下一刀再做。

## Background

标题、引言、多数卡片标题写在 composable 里。`stabilityCopy` / `biasCopy` / 忧虑与解释模板文案已抽出。Stitch 三个月图用「15% 提升」；产品空态是「记录还不够」，有数据时是「看长期，不看某一天」。关系/利他周期稿后置（轨 D）。「查看历史记录」复用 `historyOpenLabel`。

## Requirements

1. 「查看历史记录」`heightIn(min = 52.dp)`，17sp。
2. 抽出洞察页可见中文到 `InsightsCopy`。三个月有数据时仍是「看长期，不看某一天」，不够时「记录还不够」。
3. 偏差、受理、暂时无解、解释模板仍走现有函数；不加 %、完成率、诊断、15% 提升。
4. 不把关系能量/利他周期 Stitch 大图替换现有次数卡。
5. `LocalInsightsTest` 覆盖新文案不含 % / 诊断；解释模板仍满 15 条才出。

## Acceptance Criteria

- [ ] 查看历史记录最小高度 52.dp。
- [ ] 洞察页用户可见中文走 `InsightsCopy`。
- [ ] 无 15% 提升、无完成率作为指标、无诊断口吻。
- [ ] 解释模板阈值仍是 15。
- [ ] `./gradlew :shared:testDebugUnitTest` 通过。

## Out of Scope

- 设置 / 导出 / 删除（A10）。
- 改 `wakeStabilityMinutes` / `averagePredictionBias` / 聚类阈值。
- 关系/利他 Stitch 大图（轨 D）。
- `SafetyPolicy`、schema、热线。
