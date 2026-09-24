# 内部测试包可以向 GitHub Releases 核对版本号

灰度安装走 GitHub Release 的 APK，不走商店。测试者需要知道有没有新的测试包。我们决定：只有 Android 的 `internal` 构建类型，在进程启动时以及用户点击「检查更新」时，请求公开仓库 `IAmKings/Anchor` 的 Releases 列表，用其中的 `versionCode` 和本机比较。

**Status**: accepted

**Considered Options**:
- 所有安装包都联网检查：被拒。这会把 `INTERNET` 放进日常 Debug 和将来的商店包，直接撞上 ADR-0001 的零网络表面。
- 自建更新服务器或在请求里带设备号：被拒。没有账号体系，也不采集任何使用数据（ADR-0003）。
- 仅 `internal` 包、固定 GET、无正文、无标识：选中。

**Consequences**:
- `debug` 与 `release` 仍然没有网络权限。`scripts/verify_privacy.py` 只放行 `androidApp/src/internal/AndroidManifest.xml` 里的 `INTERNET`。
- 请求不上传记录、量表、录音或密码。失败时不弹窗，也不挡住危机页。
- 覆盖安装要求每一发内部包使用同一把签名钥，且 `versionCode` 递增。这把钥匙不是 Play 的安装证书。商店包上架后不能覆盖灰度包，测试者需先加密导出再卸载。
- 不在 APK 里放置 GitHub token。仓库保持公开，否则这个请求无法在不嵌入凭证的前提下工作。
- 签名文件只留在本机密码管理器和 GitHub Actions secrets：`ANDROID_KEYSTORE_BASE64`、`ANDROID_KEYSTORE_PASSWORD`、`ANDROID_KEY_ALIAS`、`ANDROID_KEY_PASSWORD`。本地打内部包时对应环境变量是 `ANCHOR_KEYSTORE`、`ANCHOR_KEYSTORE_PASSWORD`、`ANCHOR_KEY_ALIAS`、`ANCHOR_KEY_PASSWORD`。
