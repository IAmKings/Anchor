# 补开箱书桌引导和首页声明

## Goal

忧虑专场开箱时提示在书桌前处理。首页声明补全为轻度/亚临床 + 就医边界。仪器测试隔离下一刀再做。

## Background

FR-3.2：专场「建议场景提示在书桌前处理（非床上/沙发），产品层面以文案引导」。开箱概览已有「深呼吸，我们开始整理。」，没有书桌。文案不得「必须」，不得羞耻。

FR-7.5：常驻声明是「本应用仅适用于轻度/亚临床调节。诊断与治疗请务必寻求专业医生帮助。」首页现在是「应用仅适用于轻度调节」。CONTEXT 用「亚临床」。Stitch 短句让路给 PRD。

## Requirements

1. 专场已开启（`OverviewStep` open）显示书桌引导：建议在书桌前处理，而不是床上或沙发上。不含「必须」。
2. `homeMildUseDisclaimer` 改为 PRD 全句。等待首页若共用该常量则一起变。
3. 不改专场时段、三选一语义、热线、SafetyPolicy。
4. `WorryCopyTest` / `HomeCopyTest` 覆盖新句。

## Acceptance Criteria

- [ ] 开箱开启态可见书桌引导，不含必须。
- [ ] 首页声明含「亚临床」和「专业医生」。
- [ ] `./gradlew :shared:testDebugUnitTest` 通过。

## Out of Scope

- 仪器测试隔离（B5）。
- 录音进导出（B6）。
- schema、热线、量表题目。
