# 洞察关系能量与利他周期按次数展示

## Goal

洞察里回血/抽干和利他体感能看见近期起伏。数字仍是次数，不是耗竭率。

## Background

三张次数卡已在洞察页：`monitorCopy` / `energyCopy` / `altruismFeelCopy`。Stitch「关系能量统计」「利他任务周期反馈」用 68% 表演耗竭率、32%/68% 关系分型、75% 回血效果、HRV、高回血率。产品按 ADR-0003 和现有卡片细则只计次数，没有净值，也没有完成率。连续更紧已有 `pauseAltruismCopy`，不另写 Stitch「反讨好警示」。

## Requirements

1. 回血/抽干卡保留 `energyCopy`。有记录时加按次数的双条（标签是「回血」「抽干」和整数），以及最近最多 12 条的折线（回血=高、抽干=低）。空记录不加图。
2. 利他体感卡保留 `altruismFeelCopy`。已记下体感时加「更轻」「更紧」次数条和折线。未完成抽卡不计入。连续更紧时在卡内显示 `pauseAltruismCopy`。
3. 关系监控卡仍只显示段数，不加表演耗竭率。
4. 条宽只作相对长短，界面不写 `%`、完成率、15% 提升、诊断、心率、高回血率、易耗竭。不把次数卡改成 Stitch 分型百分比。
5. `LocalInsightsTest` 覆盖条与折线的次数/顺序，以及文案不含 `%`。

## Acceptance Criteria

- [ ] 有记录时能量/利他卡有次数条；≥2 条有折线。
- [ ] 值仍是「回血 N · 抽干 N」和体感次数，不是百分比。
- [ ] 连续更紧显示既有暂停文案。
- [ ] 无表演耗竭率、无 HRV、无 75%。
- [ ] `./gradlew :shared:testDebugUnitTest` 通过。

## Out of Scope

- 改 `energyCopy` / `altruismFeelCopy` 计数规则，或改成 30 日窗口百分比。
- 独立「关系洞察」页、按联系人分型、减负建议重做。
- `SafetyPolicy`、schema、热线。
- iOS 复测、浪潮呼吸参数。
