# 微行动流程按钮加高并抽出文案

## Goal

微行动 `Pick` / `Predict` / `Run` / `Rate` / `Result` 主按钮更好点。预设库和 5 分钟计时不改。历史证据页下一刀再做。

## Background

确认自定义、开始 5 分钟、我已完成、记下这次体感、回到今天都是默认高度。1–10 分芯片只有约 10.dp 竖向内边距。多数中文写在 composable 里。`microActionCustomHint`、`microActionBiasCopy`、`historyOpenLabel` 已抽出。Stitch 挑选页有个性化推荐稿，预设有「做 5 个深呼吸」；产品库是「做 5 个深蹲」，PRD 示例也不含深呼吸推荐。计时页「我已完成」保留现句，不改成打卡。

## Requirements

1. 「确认自定义行动」「开始 5 分钟」「我已完成」「记下这次体感」「回到今天」用 `heightIn(min = 52.dp)`，字号 17sp。空自定义仍不可点。
2. 预测/实际 1–10 分芯片 `heightIn(min = 52.dp)`。预设卡和已挂上的动作卡同样最小 52.dp。
3. 抽出 Pick–Result 可见文案到 `MicroActionCopy`。预设分组与标题不改。
4. 不加个性化推荐，不改成 Stitch「做 5 个深呼吸」，不加完成率。
5. `MicroActionCopyTest` 覆盖新文案；仍不含完成率 / 打卡。

## Acceptance Criteria

- [ ] 五个主按钮和分数芯片最小高度 52.dp。
- [ ] Pick–Result 用户可见中文走 `MicroActionCopy`。
- [ ] 预设仍含「穿好鞋走到楼下」和「做 5 个深蹲」，不含「必须」。
- [ ] 不改 `MICRO_ACTION_MILLIS`、历史证据页、store。
- [ ] `./gradlew :shared:testDebugUnitTest` 通过。

## Out of Scope

- `MicroActionHistoryScreen`（A5）。
- 个性化推荐、计时时长、后台 Alarm。
- `SafetyPolicy`、schema、热线。
