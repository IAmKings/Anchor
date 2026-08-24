# 其余页面 14sp 与 terracotta 改主题色

## Goal

把上一轮刻意留着的用户可见 13sp 提到 14sp，并把剩余 `Color(0xFFB26A4F)` / `Terracotta` 换成主题 `secondary`，让暗色模式和对比度跟首页一致。

## Background

`08-24-ui-a11y-home-polish` 只改了当时工作区里的文件。仓库里仍有：

- 13sp：洞察图例、双栏日志 caption、导出完成说明、复评、就医指南、关系页
- `private val Terracotta`：设置警告文案、帮助页步骤/危机句、首启第 9 题边框与就医结果分数

`.trellis/spec/shared/compose-ui.md`：caption ≥14sp；help / delete / crisis 用 `MaterialTheme.colorScheme.secondary`，不要再复制 terracotta hex。

## Requirements

1. 下列文件中用户可见 `Text` 的 `13.sp` 改为 `14.sp`：`LocalInsights.kt`、`CameraLogScreen.kt`、`ExportCompleteScreen.kt`、`Reassessment.kt`、`MedicalGuideScreen.kt`、`RelationScreens.kt`。
2. 删除 `Terracotta` 常量；原用法改为 `MaterialTheme.colorScheme.secondary`（含 `.copy(alpha = …)`）。涉及 `SettingsScreen.kt`、`HelpNowScreen.kt`、`Onboarding.kt`。
3. 不改文案内容、热线、`SafetyPolicy`、schema、预览是否持久化。
4. 不引入新 hex，不把危机句改成 `error` 红（产品危机色是 secondary 陶土，不是 Material error）。

## Out of scope

- 真机 TalkBack / 200% 字体回归（本轮无设备验证承诺）
- 图标字体替换「锚」字标
- 未出现 13sp 的其他页面布局重排

## Acceptance Criteria

- [ ] `rg 'fontSize = 1[23]\.sp' shared/` 无匹配
- [ ] `rg 'Terracotta|0xFFB26A4F' shared/` 无匹配
- [ ] `./gradlew :shared:testDebugUnitTest` 通过
- [ ] 设置预览、就医等待、危机判定行为不变

## Key decisions

- 危机/删除强调色继续用 `secondary`（与已改按钮一致），不用 `error`。
- 就医指南步骤数字徽章、躯体 chip 也提到 14sp，避免同一页混用两档 caption。
