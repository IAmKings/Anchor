# 晨间节律按钮加高并抽出文案

## Goal

晨间节律「记下此刻」和「保存今天的记录」更好点。30 日稳定度不是得分。洞察页下一刀再做。

## Background

保存钮默认高度。起床/见光圆形记下钮是 44.dp。多数中文写在 composable 里。`lateLightCopy` / `rhythmSavedCopy` 已抽出。Stitch 引言「温和的开始」；产品是「起得好不好不重要」。稿子 `设置---晨间节律提醒` 是第五类通知，与 §6.5 四类白名单冲突，不加。FR-5.2 补觉/躺 20 分钟是页内文案，不是提醒。稳定度数字复用 `stabilityCopy`（「约 N 分钟」/「记录还不够」）。

## Requirements

1. 「保存今天的记录」`heightIn(min = 52.dp)`，17sp。起床、见光都空时仍不可点。
2. 记下圆形钮最小 52.dp，并设 `contentDescription`（记下起床时间 / 记下见光时间）。近期记录行 `heightIn(min = 52.dp)`。
3. 抽出页面可见文案到 `RhythmCopy`。引言保持「起得好不好不重要」，不用「温和的开始」。
4. 不加节律提醒开关或补觉催促。漏记文案不含连续/失败。
5. `RhythmCopyTest` 覆盖新文案；保存句仍不含「连续」。

## Acceptance Criteria

- [ ] 保存和记下钮最小高度 52.dp。
- [ ] 节律页用户可见中文走 `RhythmCopy`。
- [ ] 无「温和的开始」、无 streak、无第五类提醒。
- [ ] 稳定度仍说「不是得分」。
- [ ] `./gradlew :shared:testDebugUnitTest` 通过。

## Out of Scope

- 洞察页（A9）。
- 新增晨间提醒（§6.5 白名单外）。
- 改 `addRhythmEntry` / schema。
- `SafetyPolicy`、热线。
