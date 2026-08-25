# 忧虑三选一按钮加高并抽出文案

## Goal

忧虑专场 `ProcessStep` 三个选项更好点。「暂时无解」带动态「下次专场前无需再想」。转化页和完成页下一刀再做。

## Background

`ProcessStep` 三个选项是默认高度 Button / OutlinedButton / TextButton。Stitch 三选一是大触控条；「暂时无解」副文案写死「明天 20:00 前无需再想」。PRD FR-3.2 是鼓励三选一、未处理静默顺延；提示时间走下次专场，不写死钟点。CONTEXT 用「忧虑专场」，不用「处理时段」。稿子划掉/归档动效不进本刀。

已抽出且本刀不改：Overview 开箱文案、`HangComposer`、`vaultSealedMessage`。

## Requirements

1. 「明天能做的一个动作」「暂时无解」「已不再重要」用 `heightIn(min = 52.dp)`，主文字 17sp。有录音时「播放本地录音」同样 52.dp。
2. 抽出 Process 可见文案到 `WorryCopy`（回到开箱、请选择处理方式、三个选项、播放录音、卡片引号包装）。
3. 「暂时无解」副文案用 `vaultUnsolvableHint(nextSessionLabel)`：`"${nextLabel} 前无需再想。"`，不写死今晚/明晚 20:00。
4. 三选一仍是鼓励：点动作进转化；点无解/不再重要后进下一张或完成。不改 `WorryResolution`、不改顺延、不加强制。
5. `WorryCopyTest` 覆盖新文案；无解提示不含「今晚」。

## Acceptance Criteria

- [ ] 三个选项和播放录音最小高度 52.dp。
- [ ] Process 用户可见中文走 `WorryCopy`。
- [ ] 无解副文案随下次专场变化，测试用「明天 20:00 前无需再想。」
- [ ] 不引入划掉动效，不改 Convert / Done / Overview。
- [ ] `./gradlew :shared:testDebugUnitTest` 通过。

## Out of Scope

- ConvertStep / DoneStep。
- 书桌前提示（轨 B2，开箱页）。
- `SafetyPolicy`、schema、热线、专场时段。
