# 设置导出删除按钮加高并抽出文案

## Goal

「我的」页导出、恢复、删除更好点。仍只有四类提醒。录音不进备份。

## Background

生成导出、选择备份、删除确认是默认高度。导出完成页「点击分享」已是 56.dp，「返回主页」还是 TextButton。多数设置中文写在 composable 里。提醒四种标题已抽出。Stitch 有独立「锁屏隐藏内容」开关；产品锁屏脱敏是全局规则，页内说明即可，不加第五类开关。Stitch 医生 PDF 样例不做。备份不含录音正文，文案保留「当前无法恢复」。

轨 A 到此结束。录音进导出是轨 B6，另开任务。

## Requirements

1. 「生成加密导出文件」「选择加密备份并恢复」「确认并选择备份」「删除全部本地数据」「确认彻底删除」用 `heightIn(min = 52.dp)`，17sp。导出密码仍至少 8 位才可点导出/恢复。
2. 导出完成「返回主页」`heightIn(min = 52.dp)`。开关行最小高度 52.dp。
3. 抽出设置页与导出完成「返回」等到 `SettingsCopy`。锁屏说明仍是只显示「锚点」。
4. 不加第五类提醒、不加录音进备份、不加 PDF。预览入口仍不得写 `SafetyState`。
5. `SettingsCopyTest` 覆盖新文案；仍只有四类提醒；导出完成不含 PDF。

## Acceptance Criteria

- [ ] 导出/恢复/删除主按钮最小高度 52.dp。
- [ ] 设置页用户可见中文走 `SettingsCopy`。
- [ ] 录音无法恢复的说明仍在；无 PDF、无第五类提醒。
- [ ] `./gradlew :shared:testDebugUnitTest` 通过。

## Out of Scope

- 录音进入加密导出（B6）。
- 改删除事务、导出格式、提醒调度。
- `SafetyPolicy`、schema、热线。
