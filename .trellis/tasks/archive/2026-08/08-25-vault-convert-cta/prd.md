# 忧虑转化按钮加高并抽出文案

## Goal

忧虑专场 `ConvertStep`：「确认并同步到首页」更好点。空输入仍不可点。完成页下一刀再做。

## Background

确认钮是默认高度 Button。标题、返回、输入标签写在 composable 里。Stitch 转化页是 `py-4` 主按钮「确认并同步到首页」，输入「明天要做的微行动」。卡片展示与三选一同一张引号包装。`store.resolveWorryAsAction` 事务不改。

已抽出且本刀不改：Overview、Process、`vaultQuotedCard`、`vaultUnsolvableHint`。

## Requirements

1. 「确认并同步到首页」用 `heightIn(min = 52.dp)`，字号 17sp。空输入仍 `enabled = false`。
2. 抽出 Convert 可见文案到 `WorryCopy`（回到三选一、转化动作、输入标签、确认按钮）。卡片正文复用 `vaultQuotedCard`。
3. 不改 `resolveWorryAsAction`、不改微行动 schema、不把确认做成可空提交。
4. `WorryCopyTest` 覆盖新文案；确认句不含「打卡」。

## Acceptance Criteria

- [ ] 确认按钮最小高度 52.dp；空输入不可点。
- [ ] Convert 用户可见中文走 `WorryCopy`。
- [ ] 不改 Done / Process / Overview / store 事务。
- [ ] `./gradlew :shared:testDebugUnitTest` 通过。

## Out of Scope

- DoneStep。
- 改忧虑转微行动事务或首页同步逻辑。
- `SafetyPolicy`、schema、热线。
