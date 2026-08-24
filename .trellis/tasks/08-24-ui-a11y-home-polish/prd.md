# 首页与无障碍打磨：顶栏助、14sp、语义色

## Goal

把已写在工作区的首页打磨落地：顶栏对齐 Stitch 首发中文首页（锚标 + 标题 + 「助」），用户可见字号不低于 14sp，危险/删除按钮改用主题 `secondary`，免责声明与首发稿一致。设置入口只走底栏「我的」，不改 `SafetyState`。

## Background

工作区已有 8 个未提交文件。对照：

- `design/02-home-daily/首页---今天-(中文版)__17100cc4.html`：顶栏无「设置」/「SOS」字样，右侧是 error-container 圆形帮助；浪潮球内「浪潮等待」+「等待中」，下方「难受的时候点这里」；免责声明「应用仅适用于轻度调节」。
- `.trellis/spec/shared/compose-ui.md`：新文案 ≥14sp；新按钮用 `MaterialTheme.colorScheme.secondary`，不再复制 terracotta hex。
- 底栏「我的」已接到 `onSettings`（`HomeScreen` / `RecordsHub`），去掉顶栏设置不会丢入口。

## Requirements

1. 首页与记录中心共用 `HomeTopBar(onHelp)`：左侧品牌标、中间「锚点」标题、右侧圆形「助」（`contentDescription = "此刻需要帮助"`）。不在顶栏放设置。
2. 浪潮入口：192.dp 圆、球内标题+「等待中」、说明文案移到球下。仍一键进入浪潮等待；语义描述保留「浪潮等待，难受的时候点这里」。
3. 免责声明常量为 `应用仅适用于轻度调节`，并有 copy 测试。
4. 本任务改动文件里，用户可见 `12.sp`/`13.sp` 提到 ≥14sp（量表选项、声明、就医等待卡片说明、历史证据标签）。
5. 危险主按钮（就医等待 FAB、危机「立即拨打」、彻底删除）使用 `secondary` / `onSecondary`，不新增 `Color(0xFFB26A4F)`。
6. 不改 `SafetyPolicy`、schema、热线号码、预览开关的持久化行为。

## Out of scope

- 全仓库 13sp 清扫（洞察、复评、就医指南、关系、导出完成页、双栏日志未列入本轮文件）。
- 把剩余 `Terracotta` 文本色全部换成 error/secondary（按钮已改；标签/分数可留到下一切片）。
- 医生 PDF、第 5 类提醒、锁屏通知素材。
- Navigation 库、图标字体替换「锚」字标。

## Acceptance Criteria

- [ ] 今天/记录顶栏无「设置」「SOS」；帮助控件语义为「此刻需要帮助」；「我的」仍打开设置。
- [ ] `homeMildUseDisclaimer == "应用仅适用于轻度调节"`，`HomeCopyTest` 覆盖。
- [ ] 本任务触及的 Kotlin 文件中，用户可见 `Text` 无 `12.sp`/`13.sp`。
- [ ] 危险主按钮不再使用硬编码 terracotta 作为 `containerColor`。
- [ ] `./gradlew :shared:testDebugUnitTest` 通过。
- [ ] 不写入 `SafetyState` / 不改评估计分。

## Key decisions

- 「等待中」保留在空闲浪潮球上：来自首发 Stitch 中文首页，不是计时态。
- 字号范围限于本轮已改文件，避免把无关页面卷进这次提交。
