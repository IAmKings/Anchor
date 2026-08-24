# 首启欢迎页对照 Stitch

## Goal

首启 `Welcome` 的协议行和年龄选项点按更接近 Stitch 欢迎稿，同时保留 PRD 要求的年龄自报、14–17 档和危机地区。

## Background

对照稿：`design/01-onboarding-assessment/首启欢迎---年龄确认与基线评估入口__aa12ee4b.html`。

稿子有：Logo、锚点 Anchor、tagline、年龄说明卡、勾选协议（文内链到用户协议/隐私政策）、开始基线评估 +「终结自我审判的第一步」。

稿子没有、产品必须留：18+ / 14–17 两档（FR-7.9 青少年热线）、危机资源地区、说明「年龄只由你自报，不会尝试检测」。

稿子年龄卡副文「致力于提供专业、理性的辅助支持」偏鸡汤，不用。`ageDisclaimer` 保持现句。

`canStartBaseline` 仍要求选了年龄且勾选协议。

## Requirements

1. 协议行做成「我已阅读并同意」+ 可点「用户协议」+「与」+ 可点「隐私政策」，勾选框仍控制 `agreed`。弹窗仍用现有 `termsTitle` / `privacyTitle`。
2. 「18 岁以上」「14–17 岁」芯片 `heightIn(min = 52.dp)`，字号 ≥14sp。
3. 危机地区选择保留。
4. 主按钮已是 64.dp / 17sp，不改文案。
5. 欢迎页保持可纵向滚动（大字号）。
6. 不改量表、`SafetyPolicy`、热线映射。

## Out of scope

- 用远程 Logo 图替换「锚」字标
- 删掉年龄档或地区
- 改 `ageDisclaimer` 去自报说明
- 欢迎页淡入动画

## Acceptance Criteria

- [ ] 协议行可分别打开用户协议和隐私政策。
- [ ] 年龄芯片最小高度 52.dp。
- [ ] 仍要选年龄 + 勾选才能开始；`canStartBaseline` 测试不过时。
- [ ] `ageDisclaimer` 仍含「自报」且不含「检测」。
- [ ] `./gradlew :shared:testDebugUnitTest` 通过。

## Key decisions

- 年龄档和地区跟 PRD，不跟稿子省略。
- 自报文案跟 FR-7.9，不跟稿子「专业、理性」。
