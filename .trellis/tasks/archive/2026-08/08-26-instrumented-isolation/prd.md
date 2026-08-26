# 仪器测试隔离日常库与导出缓存

## Goal

真机 instrumented 用例不得改日常 `anchor.db`、应用锁、浪潮/微行动计时，也不得清空日常导出缓存。

## Background

SQLCipher 用例已用 `m0-encrypted-probe.db`。应用锁用例已用 `app-lock-instrumented`。计时已用 `InstrumentedA/B`。缺口：`AndroidEncryptedExport.create` 写 `cacheDir/exports` 并删除该目录全部文件；在同一 Debug 安装上跑导出会清掉人工测试刚生成的导出。计划里「会改应用锁或计时」的旧描述部分已过时，本刀把隔离写成可断言的契约。

不在本刀跑 ColorOS 上卡住的后两例，也不把录音塞进导出。

## Requirements

1. `AndroidEncryptedExport` 可指定缓存目录；默认仍是 `exports`。仪器测试用独立目录，测完删除该目录，且不改 `exports` 里已有文件。
2. 应用锁、计时仪器测试断言：跑完后生产 prefs（`app-lock`、`m0-background-timer-Wave`、`m0-background-timer-MicroAction`）内容不变。
3. SQLCipher 仪器测试继续断言库名不是 `anchor.db`。
4. 不改生产 `MainActivity.DATABASE_NAME`、不改 `SafetyPolicy`、不改导出文件格式。

## Acceptance Criteria

- [ ] 导出仪器测试使用非 `exports` 目录，并清理自己的目录。
- [ ] 锁/计时仪器测试证明不写生产 prefs。
- [ ] `./gradlew :shared:testDebugUnitTest` 通过。仪器测试本机无设备则不声称已在真机跑过。

## Out of Scope

- 录音进导出（B6）。
- ColorOS 仪器测试卡顿排查。
- 提醒调度仪器测试。
