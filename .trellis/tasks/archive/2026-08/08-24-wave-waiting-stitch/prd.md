# 浪潮等待三步页对照 Stitch

## Goal

浪潮等待（认 → 定位 → 等）的点按区域和定位卡层次更接近 Stitch `浪潮等待模式`，但不改 10 分钟计时、后台闹钟，也不砍 PRD 要求的 8 个部位和自定义。

## Background

对照稿：`design/08-medical-safety/浪潮等待模式__d9c14e9b.html`。

已经对齐、本任务不动：

- 三步文案与 `WaveCopyTest`（认句带句号，跟 PRD FR-5.3，不跟稿子去句号）
- 8 个部位 +「其他身体感受」（稿子只画了 4 个；PRD ≥8）
- 等：256.dp 环、等宽倒计时、结束句「它自己退了…」
- `WAVE_TEN_MINUTES`、`onSchedule`、切后台不中断
- 顶部「离开」（稿子没画关闭，产品需要）

还差：

- 认/继续/再加 10 分钟/回到今天：主按钮偏矮，规范是 52–56.dp、约 17sp（`.trellis/spec/shared/compose-ui.md`）
- 定位卡只有短标签、无 `contentDescription` 全称；稿子是图标+短词。无图标字体，用短词 + 全称语义，不引入 Material Symbols

等步在有定位时显示「留意：$location」，空/结束时「什么都不用做，只是呼吸。」这比稿子默认句更有用，保留。

## Requirements

1. 「我看见了」「继续」「再加 10 分钟」「回到今天」高度 ≥52.dp，字号 17sp。未选部位时「继续」仍不可点。
2. 定位卡：展示 `shortLabel`；`contentDescription` 用 `fullLabel`。仍是两列、8 项 + 自定义输入。不要减成稿子的 4 项。
3. 不改 `WaveCopy.kt` 里测试钉死的句子、部位列表、10 分钟常量。
4. 不改 `onSchedule` / deadline / 再加 10 分钟的计时语义。
5. 呼吸背景可微调透明度，不做新动效库、不加庆祝。
6. `./gradlew :shared:testDebugUnitTest` 必须仍过 `WaveCopyTest`。

## Out of scope

- Material 图标字体
- 把 8 个部位减到 4 个，或去掉自定义
- 改 AlarmManager / 锁屏通知
- 就医等待期或练习首页
- 认句去句号

## Acceptance Criteria

- [ ] 四个主操作达到 52.dp 高、17sp。
- [ ] 定位卡 TalkBack 读全称（胸口发紧等），屏幕上仍是短词。
- [ ] 仍有 ≥8 个预置部位和自定义框。
- [ ] `WaveCopyTest` 断言原文不变。
- [ ] `shared:testDebugUnitTest` 通过。
- [ ] 不写 `SafetyState`，不改热线。

## Key decisions

- 计时和 PRD 文案不动；只改点按和定位卡可访问性。
- 部位数量跟 PRD，不跟稿子的 4 格示意。
