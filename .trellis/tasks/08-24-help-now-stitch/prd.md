# 危机帮助页对照 Stitch 版式

## Goal

`HelpNowScreen` 的步骤层次和立即危险按钮更接近 Stitch 危机稿，热线和急救号继续走 `crisisResource` / PRD §10.4。

## Background

对照稿：`design/08-medical-safety/危机干预页__40f8ae92.html`。

稿子不能用的部分：

- 「拨打 12356 (希望24热线)」——号码必须按地区/年龄，见 `CrisisResources.kt`
- 「寻找附近急诊」——要联网/地图，违反 ADR-0001
- 「从通讯录选择」——新权限，本任务不做
- 「深呼吸，事情可能会好起来的」——鸡汤，CONTEXT 禁止

现页：标题「你现在不是一个人」、三步卡、躯体提示、就医指南、底部「立即危险请拨 {emergency}」。急救号已按地区。

## Requirements

1. 标题保持「你现在不是一个人」。不要稿子的鸡汤副文。
2. 三步标题保持：拨打心理援助热线 / 前往精神科或急诊 / 告诉一位身边可信的人。第一步正文是 `resource.hotline`，不是写死 12356。
3. 步骤卡左侧圆标写 1/2/3，不用 Material 图标。
4. 「立即危险」说明和按钮钉在底部；文案用 `resource.emergency`，不写死 120/110。按钮 ≥52.dp。
5. 保留躯体清单、就医指南、PRD 就医引导句「判断这个不是你的工作…」。
6. 文案进 `HelpNowCopy.kt`（或 safety copy 文件）。测试：不含「希望24」「好起来」；热线/急救来自 `crisisResource`。
7. 不改 `SafetyPolicy`、`crisisResource` 号码表。

## Out of scope

- `tel:` 拨号、地图、通讯录
- 评估结果里的 `CrisisResult` 全页重做（可共用 copy 常量）
- 改热线数字

## Acceptance Criteria

- [ ] 帮助页看不到希望24、好起来、寻找附近急诊、通讯录。
- [ ] 大陆成人热线仍是 12356，青少年 12355；急救随地区。
- [ ] 立即危险按钮在底部固定，≥52.dp。
- [ ] `./gradlew :shared:testDebugUnitTest` 通过。
- [ ] 不写 `SafetyState`。

## Key decisions

- 版式跟稿（编号井、底部危险条）；号码和话术跟 PRD/CONTEXT。
