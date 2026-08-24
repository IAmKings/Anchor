# 首启危机结果页对齐帮助页

## Goal

首启量表走到 `CrisisGuidance` 时，结果页和 `HelpNowScreen` 同一套步骤、急救条和文案，避免帮助入口和评估分流两套危机 UI。

## Background

`HelpNowScreen` 已有编号步骤、底部固定急救按钮、`HelpNowCopy`。`CrisisResult` 仍是旧的 `CrisisSteps` 列表 + 页内急救按钮，立即危险句手写。

保留：PHQ/GAD 分数、`scaleSource`、`先进入就医等待期`（`onFinish` 进等待期）。

## Requirements

1. `CrisisResult` 使用 `helpNowTitle` / 三步 copy / `helpNowImmediateDanger` / `helpNowCallEmergency` / `helpNowMedicalQuote`。
2. 步骤卡复用帮助页编号圆标（抽 `internal` composable），热线正文仍是 `crisisResource(...).hotline`。
3. 立即危险说明和急救按钮钉在底部，≥52.dp；号码用 `resource.emergency`。
4. 保留分数行、版权行、进入就医等待期。
5. 不含希望24、好起来、地图、通讯录。
6. 不改 `SafetyPolicy`、热线表、`MildResult` 上加微行动单选。

## Acceptance Criteria

- [ ] 危机结果页标题和三步标题与帮助页相同。
- [ ] 急救按钮在底部，文案含地区急救号。
- [ ] 仍可「先进入就医等待期」。
- [ ] `./gradlew :shared:testDebugUnitTest` 通过。

## Key decisions

- 评估危机结果跟帮助页，不跟 Stitch 轻度结果里的微行动单选。
