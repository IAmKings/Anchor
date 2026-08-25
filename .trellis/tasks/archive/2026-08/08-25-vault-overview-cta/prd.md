# 忧虑专场开箱按钮加高并抽出文案

## Goal

忧虑专场 `OverviewStep`（锁态二次确认 + 专场已开启）主按钮更好点。已抽出的封存句、反刍句、待处理摘要不改。三选一 / 转化 / 完成页下一刀再做。

## Background

`WorryVaultScreen.OverviewStep` 锁态「现在就想处理」是默认高度 TextButton；「确认开箱」「开始处理第一项」是默认高度 Button。开箱标题、确认提示、空箱、开场句仍写在 composable 里。Stitch 开箱概览是专场已开启布局参考：底部 `py-4` 主按钮「开始处理第一项」。稿子卡片 hover 上的「不再重要 / 暂时无解」属于三选一，不进本刀。CONTEXT 用「忧虑专场」，不用「处理时段 / 开箱时间」。PRD FR-3.3 二次确认通道保留，不锁死。

已抽出且本刀不改：`vaultSealedMessage`、`vaultRuminationMessage`、`vaultPendingSummary`、`HangComposer` 语音/封存 52.dp。

## Requirements

1. 「确认开箱」「开始处理第一项」用 `heightIn(min = 52.dp)`，字号 17sp。
2. 「现在就想处理」同样 `heightIn(min = 52.dp)`（FR-3.3 开箱豁免，不能是瘦 TextButton）。「等到专场」保持次要 TextButton。
3. 抽出 Overview 可见文案到 `WorryCopy`（标题、锁/开 caption、现在就想处理、确认开箱提示与按钮、等到专场、深呼吸、空箱、受理句、开始处理第一项、语音挂卡）。确认提示保持现有「请尽量…优先找“明天能做的一个动作”」，不改成「务必」。
4. 锁态仍先显示反刍句 + 待处理张数（不展示卡片正文）；开态才列出 `PreviewCard`。危机关键词澄清、`HangComposer` 行为、三选一语义不改。
5. `WorryCopyTest` 覆盖新文案；封存句仍不含「今晚」；待处理摘要仍不含「汇报」。

## Acceptance Criteria

- [ ] 确认开箱、开始处理第一项、现在就想处理最小高度 52.dp。
- [ ] Overview 用户可见中文走 `WorryCopy`，屏幕里不再硬编码这些句子。
- [ ] 专场外仍可「现在就想处理」→ 二次确认 → 开箱；取消走「等到专场」。
- [ ] 不引入稿子卡片 hover 三选一，不改 Process / Convert / Done。
- [ ] `./gradlew :shared:testDebugUnitTest` 通过。

## Out of Scope

- ProcessStep / ConvertStep / DoneStep 按钮高度与文案抽出（下一刀）。
- `SafetyPolicy`、schema、热线、专场时段逻辑。
- Stitch 底栏、lock_open 图标、卡片划掉动效。
