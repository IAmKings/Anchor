# 情绪标签箱按钮加高并抽出文案

## Goal

情绪标签箱列表、选词、填空、保存更好点。32 个具体词和模糊词入口不改。记录中心下一刀再做。

## Background

「写下这一张」「保存卡片」是默认高度。「它已经过去了」是瘦 TextButton。词芯片竖向内边距约 14.dp。列表/选词/填空中文写在 composable 里。`emotionGroups` 正好覆盖 `emotionVocabulary`（32 词）。模糊词只能当搜索入口，不能当最终标签。危机关键词仍走澄清对话框。design/07 只有双栏日志稿，没有标签箱 Stitch。

## Requirements

1. 「写下这一张」「保存卡片」「它已经过去了」用 `heightIn(min = 52.dp)`，主文字 17sp。事件和难受部分都空时保存仍不可点。
2. 选词芯片 `heightIn(min = 52.dp)`。
3. 抽出列表/选词/填空可见文案到 `EmotionCopy`。词库分组、模糊查询、`emotionCardSentence` 模板不改。
4. 不把「难过 / 很烦」写进词库。危机澄清流程不改。
5. `EmotionCopyTest` 覆盖新文案，并继续断言词库不含「难过」。

## Acceptance Criteria

- [ ] 写下、保存、已过去、词芯片最小高度 52.dp。
- [ ] 屏幕用户可见中文走 `EmotionCopy`。
- [ ] 32 词覆盖不变；「难过」仍不能作为最终标签。
- [ ] 不改 `CrisisClarificationDialog` / `SafetyPolicy`。
- [ ] `./gradlew :shared:testDebugUnitTest` 通过。

## Out of Scope

- 记录中心 + 双栏日志（A7）。
- 改 32 词或模糊词列表。
- schema、热线。
