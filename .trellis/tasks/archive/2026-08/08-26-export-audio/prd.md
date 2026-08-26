# 加密导出带上本地录音

## Goal

新的加密备份带上忧虑挂卡的本地 M4A。JSON 仍不写绝对路径。旧备份恢复不伪造录音。

## Background

现在 `exportVersion` 3 只标 `hasAudio`。恢复把 `audio_file_name` 写成 NULL。录音在 `filesDir/voice-notes/voice-*.m4a`。`.anchor` 已是 zip + AES-GCM，适合加 `audio/<basename>.m4a` 条目。

## Requirements

1. 导出版本 4。忧虑 JSON 增加 `audioName`（仅合法 basename，`.m4a`），仍有 `hasAudio`。禁止路径分隔符和 `..`。
2. `AndroidEncryptedExport.create` 把录音字节打进加密 zip 的 `audio/`。默认仍加密 JSON+CSV。
3. 恢复：版本 2–4。有 zip 音频则写入 `voice-notes` 并恢复文件名；v2/v3 或缺文件则 `audio_file_name` 仍为 null，不写 `missing-audio`。
4. 设置页说明改为：新备份含录音，更早的备份恢复后录音仍会缺。
5. 不改热线、SafetyPolicy、schema 表结构（只改 restore 查询绑定）。

## Acceptance Criteria

- [ ] v4 JSON 含 `audioName` 且不含 `/`。
- [ ] 导出 zip 可解出与源相同的 m4a 字节。
- [ ] 无音频条目时恢复后 `audioFileName == null`。
- [ ] `./gradlew :shared:testDebugUnitTest` 与 `:androidApp:compileDebugAndroidTestKotlin` 通过。

## Out of Scope

- 轨 C 发布阻塞。
- 把录音放进 CSV 或未加密 JSON。
