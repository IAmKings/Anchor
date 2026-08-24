# 挑选第一个锚点页对照 Stitch

## Goal

`FirstAnchorChoice` 的卡片层次和底部确认条更接近 Stitch 挑选稿，但不换成稿子里的正念呼吸/数字睡前等方法，也不改 14 天一次一件。

## Background

对照稿：`design/01-onboarding-assessment/首启引导---挑选第一个锚点动作__bb047f26.html`。

稿子版式：返回、标题、说明（只选一个加粗）、左侧圆标的单选卡、底部固定「选定后我们将固定练习 14 天」+「确认选择」。

稿子方法列表是另一套练习（起床见光 5 分钟、正念呼吸、感官寻锚…），和产品锚点库冲突。选项必须继续是 `p0FirstAnchors + p1FirstAnchors`（`BaselineAssessmentTest.firstAnchorPickerKeepsP0AndAddsP1RelationOptions`）。

现实现：竖卡、选中标题前加「✓」、确认钮跟列表一起滚走。标题 `firstAnchorHeadline` 用「行为」不是稿子的「微行为」——跟现 copy，不改。

## Requirements

1. 选项仍是 6 个 P0 + 2 个 P1，文案用现有 `title` / `description`。
2. 卡片：左侧圆标（汉字 glyph，不要 Material 图标）+ 标题 + 说明。选中用 `primaryContainer` 边框，**不要**在标题前加「✓」。
3. 「选定后我们将固定练习 14 天」和「确认选择」放在底部固定条，列表可滚、不被挡住。确认钮 ≥52.dp，未选中时不可点。
4. 「确认选择」抽到 `OnboardingCopy`。
5. 返回仍是文字「返回」。页面可纵向滚动。
6. 不改 `additionalPracticeUnlocked` / `practiceVisible` / 14 天常量。

## Out of scope

- 换成稿子的 8 个生活习惯项
- 改 headline 为「微行为」
- 图标字体
- 量表或 `SafetyPolicy`

## Acceptance Criteria

- [ ] 仍能选到情绪标签、微行动、事实、保险箱、节律、浪潮、无损消耗、利他。
- [ ] 选中态没有「✓ 」前缀。
- [ ] 确认条在底部固定；未选不能确认。
- [ ] `firstAnchorCommitHint` 仍是「选定后我们将固定练习 14 天」。
- [ ] `./gradlew :shared:testDebugUnitTest` 通过。

## Key decisions

- 方法列表跟产品锚点，不跟 Stitch 示意库。
- 版式跟稿：圆标卡 + 底部固定确认。
