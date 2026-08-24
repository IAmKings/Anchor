# 首启就医等待结果页对齐等待首页

## Goal

量表走到 `MedicalWaiting` 时，结果页的说明、工具卡和危机三步与等待首页/帮助页同一套话术，底部固定「进入就医等待期」。

## Background

`MedicalResult` 仍用手写工具副文案和 `CrisisSteps`。等待首页已有 `homeWaitingBody`（含 PRD 惩罚句），帮助页已有编号三步。

Stitch 中重度稿的「识别失控信号」三步不用；危机步骤跟帮助页（热线 / 精神科或急诊 / 可信的人）。

## Requirements

1. 说明用 `homeWaitingBody`（含「暂停练习不是惩罚…」）。标题可用 `medicalWaitingTitle`（就医等待期模式）。
2. 工具卡标题/副文案用等待首页常量（就医指南、就医准备清单），不要「感受」当事实记录。
3. 危机 1-2-3 用 `HelpNowStepCard` + `HelpNowCopy`，热线 `crisisResource.hotline`。
4. 引用句用 `helpNowMedicalQuote`。
5. 「进入就医等待期」钉在底部，≥52.dp。
6. 保留分数环/徽章、`scaleSource`。不写死希望24、120/110。

## Acceptance Criteria

- [ ] 结果页说明包含 PRD 惩罚句。
- [ ] 三步标题与帮助页相同。
- [ ] 进入等待期按钮在底部。
- [ ] `./gradlew :shared:testDebugUnitTest` 通过。
