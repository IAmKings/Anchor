# 记录中心加保险箱并加高双栏按钮

## Goal

记录 Tab 能进忧虑保险箱。双栏「写下 / 保存」更好点。评价词仍是弱提示，不阻断。晨间节律下一刀再做。

## Background

PRD §11.2 记录 Tab：情绪标签箱、双栏日志、忧虑保险箱。`RecordsHub` 现在只有情绪 + 双栏；保险箱只在首页。就医等待期仍应能写双栏、隐藏练习（情绪/保险箱）。一次一件锁用现有 `showEmotion` / `showJournal` / `showWorry`。

双栏「写下这一栏」「保存记录」默认高度。评价词提示「仍然可以直接保存」+「挪过去」。Stitch 引言「找回内在的平静」、推断栏写「情绪」、按钮「移动」——CONTEXT/PRD：摄像头事实不含「感受」，弱提示不阻断。保留「挪过去」。

## Requirements

1. 记录中心在非就医等待且 `showWorry` 时增加「忧虑保险箱」入口；点进去走现有 `WorryVaultScreen`。待处理张数用「N 张待处理」，不展示卡片正文。
2. 就医等待期继续只强调双栏；不显示情绪和保险箱。
3. 「写下这一栏」「保存记录」`heightIn(min = 52.dp)`，17sp。任一栏非空即可保存。
4. 抽出记录中心标题/三张入口文案，以及双栏页硬编码中文到 `CameraLogCopy`。
5. 评价词仍提示可挪、可直接保存；不改成阻断。不用稿子「平静 / 感受 / 移动」。
6. `CameraLogCopyTest` 覆盖保险箱文案与「已记下」；评价词测试仍通过。

## Acceptance Criteria

- [ ] 记录 Tab 有忧虑保险箱入口（等待期除外）。
- [ ] 写下、保存最小高度 52.dp。
- [ ] 双栏用户可见中文走 `CameraLogCopy`。
- [ ] `firstEvaluativeWord` 仍只出提示，保存不因评价词失败。
- [ ] `./gradlew :shared:testDebugUnitTest` 通过。

## Out of Scope

- 晨间节律（A8）。
- 把评价词改成阻断校验。
- 改 `WorryVaultScreen` 流程。
- `SafetyPolicy`、schema、热线。
