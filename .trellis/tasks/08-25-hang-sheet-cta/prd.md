# 挂卡半屏语音与封存按钮加高

## Goal

首页挂卡和保险箱里共用的 `HangComposer`：语音、封存更好点。封存成功句和危机澄清不改。

## Background

封存钮已是 52.dp / 17sp。语音钮还是默认高度，「语音速记」「停止并保存录音」写在 composable 里。`HangSheet` 与 `WorryVaultScreen` 共用 composer。

## Requirements

1. 语音、封存都用 `heightIn(min = 52.dp)`，字号 17sp。空输入时封存仍不可点。
2. 抽出 `hangSpeechLabel` / `hangSpeechStopLabel`（及取消/知道了若仍硬编码）。
3. `vaultSealedMessage`、关键词澄清流程不改。
4. `WorryCopyTest` 覆盖新文案，且封存句仍不含「今晚」。

## Acceptance Criteria

- [ ] 语音与封存最小高度 52.dp。
- [ ] 录音中按钮文案是「停止并保存录音」。
- [ ] 封存动态句仍用下次专场，不写死今晚。
- [ ] `./gradlew :shared:testDebugUnitTest` 通过。
