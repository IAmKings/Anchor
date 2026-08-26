# 关系/利他按钮加高并抽出文案

## Goal

关系盘点、能量账本、利他抽卡更好点。默认仍先抽非社交。不是断交，离开仍在。

## Background

功能已在代码里（PRD §5.6，P1，不挡首发）。Hub / 盘点 / 账本 / 抽卡主按钮还是默认高度。Hub 利他提示、称呼栏、加进清单、账本空态、非社交/社交标签写在 composable 里。

Stitch 利他稿是「开始抽取」+ 勾选「防讨好」+ 自评「轻盈自然 / 紧绷内耗」；产品按 FR-6.3/6.4 是两档抽卡、`peoplePleasingHint`、更轻/更紧。Stitch 干预稿「系统注意到」不进产品。洞察里关系能量/利他周期大图另开。

## Requirements

1. Hub 入口卡、盘点「加进清单」、账本「回血 / 抽干」、抽卡「抽一张非社交 / 抽一张社交 / 更轻 / 更紧」、监控芯片用 `heightIn(min = 52.dp)`。主按钮 17sp。
2. 抽出 Hub/盘点/账本/抽卡可见中文到 `RelationCopy`。利他 Hub 提示仍是「默认先抽不社交的小事。」
3. 默认仍先抽非社交；连续更紧仍暂停社交抽；减负仍「不是断交」；页尾仍 `leavingCopy`。
4. 不加 Stitch「开始抽取」、勾选防讨好、「轻盈自然」、「系统注意到」、KPI/%。
5. `RelationCopyTest` 覆盖抽出文案；预设库与暂停规则不变。

## Acceptance Criteria

- [ ] 上述主按钮和监控芯片最小高度 52.dp。
- [ ] 关系/利他用户可见中文走 `RelationCopy`。
- [ ] 无开始抽取、无轻盈自然、无系统注意到、无完成率。
- [ ] 默认非社交抽卡与更紧暂停仍过现有测试。
- [ ] `./gradlew :shared:testDebugUnitTest` 通过。

## Out of Scope

- 洞察关系能量/利他周期 Stitch 大图。
- 改 `nextAltruismCard` / `shouldPauseAltruism` 规则、联系人 schema。
- 首页 `StatusCard` 布局、设置预览写记录。
- `SafetyPolicy`、热线、导出。
