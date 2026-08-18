# M0 Compose Multiplatform 平台原型记录

> 日期：2026-08-15  
> 状态：进行中  
> 目的：在业务功能扩建前验证 Compose Multiplatform 双端基础与平台能力。

## 技术基线

| 项目 | 版本 |
| --- | --- |
| Kotlin | 2.4.10 |
| Compose Multiplatform | 1.11.1 |
| Android Gradle Plugin | 8.13.2 |
| Gradle | 8.14.2 |
| Android compile/target SDK | 36 |
| Android min SDK | 26 |
| Xcode | 26.3 (17C529) |
| iOS framework target | iosArm64 / iosSimulatorArm64 |

Google Maven 在当前网络中直连超时，因此项目将阿里云 Google Maven 镜像放在官方仓库之前；Maven Central 仍使用官方地址。

## 已通过

- 最小工程结构建立：`shared`、`androidApp`、`iosApp`。
- 共享 Compose 检查页同时编译至 Android 与 iOS framework。
- 共享 `M0Checklist` 逻辑测试在 Android/JVM 通过。
- Android Debug APK 生成成功：`androidApp/build/outputs/apk/debug/androidApp-debug.apk`。
- iOS Simulator static framework 生成成功：`shared/build/bin/iosSimulatorArm64/debugFramework/Shared.framework`。
- 最小 SwiftUI 宿主与共享 Compose `UIViewController` 接口已连接。
- SQLDelight 2.3.2 schema 与 SQLCipher Android 4.17.0 已接通，Android 主代码编译通过。
- Android 测试 APK 已生成，并包含 `arm64-v8a`、`armeabi-v7a`、`x86`、`x86_64` 四种 ABI 的 `libsqlcipher.so`。
- 加密集成测试已实现：覆盖关闭后重开读取、错误密钥拒绝、文件头非明文 SQLite、数据库及 WAL/SHM 删除。
- Android 16（API 36）真机 `PJZ110` 已执行 SQLCipher 集成测试：加密读写/删除与 v1→v2 schema 迁移共 2 项通过，耗时 3.106 秒。
- v1 加密库升级至 v2 后旧数据保留、新字段默认值正确，数据库文件仍不是明文 SQLite。
- Android 后台计时已使用 `SystemClock.elapsedRealtime()` 实现，计时状态不依赖进程内逐秒累加；无精确闹钟权限时的到期状态测试通过。
- Android 正式应用进程在获得“闹钟和提醒”特殊访问后返回 `exact=true`；2 秒计时在 10 秒验收窗口内收到结束广播，结果为 `fired=true, remaining=0`。
- Android 计时结束本地通知已在真机通过：通知开启时正常投递；锁屏公开版本仅含应用名“锚点”，不含正文。
- Android 通知关闭兜底已在真机通过：系统通知未投递，返回应用冷启动后显示中性横幅“它自己退了。你没有掐掉它，它也会走。”，并一次性消费待显示标记。
- Android 四类提醒调度已在真机通过：忧虑专场每日 1 次、危机后 24h/72h 各 1 次、复评 14 天后 1 次、就医等待期每 7 天 1 次；系统闹钟队列仅出现对应的 5 个 PendingIntent。
- Android 时区重排已在真机通过：从 `Asia/Shanghai` 切换到 `Europe/London` 后，忧虑专场提醒自动移动 7 小时，另外三类事件相对时间不变；设备随后已恢复上海时区。
- Android 生物识别应用锁已在真机通过：设备能力检查成功，系统认证后启用并持久化；切到后台再返回时重新认证，认证前仅渲染无敏感信息的锁定页。
- Android 离线语音能力在真机完成验证：系统返回 `onDevice=false`，因此不调用可能联网的默认识别服务，自动降级为 App 私有目录录音；M4A 保存、退后台自动停止及测试文件删除均通过。
- Android 加密分享已在真机通过：用户自选密码生成同时包含 JSON/CSV 的 `.anchor` 密文，错误密码拒绝解密，文件中不存在明文样本；ColorOS 系统分享面板正常显示 404 B 测试文件。
- Android 多任务隐私遮蔽已在真机通过：应用锁开启时窗口设置 `FLAG_SECURE`，关闭时清除；最近任务缩略图、系统截图与非安全投屏由系统遮蔽。
- Android 冷启动与现有 Compose 首屏性能基线已通过：10 次冷启动最大 457ms，低于 2 秒红线；持续滚动 306 帧无 deadline missed/jank。
- 浪潮等待 Compose 动画已在 Android 真机通过：5 秒正弦呼吸周期连续运行 15 秒，925 帧无 jank、无 deadline missed，P99 为 7ms。

验证命令：

```bash
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' \
  ./gradlew :shared:testDebugUnitTest \
  :shared:assembleDebugAndroidTest \
  :androidApp:assembleDebug \
  :shared:linkDebugFrameworkIosSimulatorArm64 --no-daemon
```

结果：`BUILD SUCCESSFUL`。

生成的设备测试包：`shared/build/outputs/apk/androidTest/debug/shared-debug-androidTest.apk`。

## Android 真机验证

Android 真机验证命令：

```bash
adb install -r -t -g \
  shared/build/outputs/apk/androidTest/debug/shared-debug-androidTest.apk
adb shell am instrument -w -r \
  -e class com.anchor.app.storage.AndroidEncryptedProbeStoreTest \
  com.anchor.app.shared.test/androidx.test.runner.AndroidJUnitRunner
```

结果：

```text
Time: 3.106
OK (2 tests)
```

Gradle 的 `connectedDebugAndroidTest` 会临时解析 AGP UTP 依赖，当前 Google Maven 网络超时；本次使用直接 ADB 执行测试包，测试本身不受影响。

## 当前阻塞

### iOS 运行环境

Xcode 命令行可发现 iPhoneOS 26.2 SDK 路径，但没有可用 iOS destination，并明确报告：

```text
iOS 26.2 is not installed. Please download and install the platform
from Xcode > Settings > Components.
```

影响：

- `xcodebuild` 暂时无法编译/启动 `iosApp` 宿主。
- Kotlin `iosSimulatorArm64Test` 无法执行。
- Kotlin/Native framework 编译本身不受影响，已通过。

解除条件：在 Xcode → Settings → Components 安装与当前 Xcode 匹配的 iOS 26.2 platform/runtime，然后执行：

```bash
xcodebuild -project iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -destination 'generic/platform=iOS Simulator' \
  CODE_SIGNING_ALLOWED=NO build
```

按当前开发顺序，iOS runtime 下载已延后，先完成 Android 平台验证。

### Android 后台结束事件结论

Android 16 真机无精确闹钟权限时，ColorOS 将一个 2 秒后的 `setAndAllowWhileIdle()` 闹钟调整到约 3 天后：

```text
battery_saver adjustment=+2d23h57m47s680ms
```

因此后台计时采用两层保证：单调时钟负责正确的到期状态；获得“闹钟和提醒”特殊访问后使用 `setExactAndAllowWhileIdle()` 负责后台结束事件。无权限时状态仍正确，但系统通知可能延迟。

Android 16 / ColorOS 真机验证结果：

```text
schedule: exact=true
status after 10s: fired=true,remaining=0
```

结论：Android 后台计时原型通过。产品 UI 必须在开始需要准时结束提醒的练习前解释并请求特殊访问；拒绝授权时允许继续练习，但使用“回到应用后自动结算”的降级方式。

### Android 计时结束通知结论

Android 16 / ColorOS 真机分别验证通知允许与关闭分支：

```text
通知允许：notification id=7002, visibility=PRIVATE
公开版本：title=锚点, body=<none>
通知关闭：system notification=<none>, pendingInAppNotice=true
返回应用：显示中性横幅，pendingInAppNotice=false
```

结论：计时结束广播、本地通知、锁屏脱敏及通知关闭时的应用内兜底链路通过。当前兜底状态保存在应用私有本地存储中，展示后立即消费，不形成提醒历史或行为指标。

### Android 四类提醒与时区结论

Android 16 / ColorOS 真机一次性建立四类提醒，共 5 个系统闹钟：

```text
WORRY_SESSION   每日本地专场开始前 5 分钟
CRISIS_24H      危机拦截后 24 小时
CRISIS_72H      危机拦截后 72 小时
REASSESSMENT    上次评估后 14 天
MEDICAL_WAITING 进入就医等待期后每 7 天
```

时区切换结果：

```text
Asia/Shanghai: WORRY_SESSION=1786809780000
Europe/London: WORRY_SESSION=1786834980000  (+7h)
其他四个闹钟时间保持不变
恢复后: zone=Asia/Shanghai, WORRY_SESSION=1786809780000
```

提醒通知允许分支显示白名单正文，公开锁屏版本只有“锚点”且无正文、无角标。通知关闭分支未生成系统通知；冷启动后显示对应应用内横幅并从待显示集合消费。系统重启、手动改时和时区变化均由 manifest receiver 触发重排。

### Android 生物识别应用锁结论

Android 16 / ColorOS 真机验证结果：

```text
canAuthenticate(BIOMETRIC_WEAK | DEVICE_CREDENTIAL)=SUCCESS
首次系统认证：成功，应用锁开关持久化为 enabled=true
切到桌面后返回：重新触发系统认证，成功后恢复应用内容
关闭应用锁：enabled=false
```

应用锁使用 AndroidX Biometric 稳定版兼容项目的 API 26 下限，并允许设备 PIN、图案或密码作为生物识别兜底。锁定状态不渲染 M0 内容和应用内提醒，只显示“锚点已锁定”及解锁按钮；系统取消或认证错误时保持锁定。真机测试完成后应用锁已恢复关闭。

### Android 离线语音与录音兜底结论

Android 16 / ColorOS 真机端侧能力检查结果：

```text
SpeechRecognizer.isOnDeviceRecognitionAvailable(context)=false
```

因此实现只在系统明确提供端侧服务时调用 `createOnDeviceSpeechRecognizer()`；不使用可能把音频发送至远端的默认识别器。不可用或识别报错时提示用户重新说一次并切换本地录音。

本地录音真机结果：

```text
路径：/data/user/0/com.anchor.app/files/voice-notes/voice-*.m4a
权限：-rw-------（应用私有）
样本大小：55497 bytes
文件头：00 00 00 18 66 74 79 70 6d 70 34 32（ftyp/mp42）
退后台：自动停止并生成有效文件
清理后：voice-notes 目录为空
```

结论：目标设备无法提供离线转写，但 PRD 指定的“不联网 + 识别失败保留本地录音”降级链路通过。运行时麦克风权限使用标准 Activity Result API；ColorOS 本轮未展示标准授权弹窗，真机测试通过 Debug 安装时授权完成。

### Android 加密分享结论

导出原型将 `anchor.json` 与 `anchor.csv` 先在内存中打包，再使用用户输入的密码派生密钥并通过 AES-256-GCM 加密。磁盘只写入 `.anchor` 密文；密码在发起分享后从界面状态清除，不写入文件或偏好存储。文件头保存格式版本、随机盐、随机 nonce 和 PBKDF2 迭代次数，以便后续迁移。

Android 16 / ColorOS 真机测试结果：

```text
加密算法：PBKDF2-HMAC-SHA256（120000 次）+ AES-256-GCM
往返：正确密码恢复 anchor.json / anchor.csv
错误密码：AEADBadTagException，拒绝解密
明文扫描：密文中未出现测试敏感文本
分享 URI：content://，仅 FLAG_GRANT_READ_URI_PERMISSION
系统分享面板：正常显示 anchor-20260816-004536.anchor（404 B）
测试耗时：1.343 秒
```

FileProvider 只暴露应用缓存内的 `exports/` 子目录，未开放整个缓存或外部存储；每次创建新导出时删除上一份临时导出。M0 使用空数据结构验证格式和平台链路，接入真实 `AnchorStore` 查询留到 M1。

### Android 多任务隐私遮蔽结论

隐私遮蔽直接复用应用锁的持久状态：锁开启时为 Activity 窗口添加 `FLAG_SECURE`，关闭时立即清除。该系统能力兼容项目 API 26 下限，可阻止窗口内容进入最近任务截图、普通系统截图及非安全显示或投屏，不需要维护额外的 Compose 模糊遮罩。

Android 16 / ColorOS 真机仪器测试结果：

```text
应用锁关闭：window flags & FLAG_SECURE = 0
应用锁开启：window flags & FLAG_SECURE = FLAG_SECURE
测试结果：OK (1 test)，11.732 秒
测试结束：应用锁偏好自动恢复关闭
```

### Android 启动与首屏性能基线

Android 16 / `PJZ110` 真机使用系统 `am start -W -S` 连续执行 10 次完整冷启动：

```text
TotalTime (ms): 425, 401, 401, 405, 405, 457, 417, 419, 420, 424
中位数：418ms
P90：425ms
最大值：457ms
验收红线：≤2000ms（通过）
```

现有 M0 Compose 长页面执行 4 次完整上下滚动后的系统帧统计：

```text
Total frames rendered: 306
Janky frames: 0 (0.00%)
50th / 90th / 95th / 99th: 7 / 9 / 9 / 10ms
Frame deadline missed: 0
```

冷启动红线已通过。上述滚动数据仅作为现有首屏基线；危机状态到全屏页 `<300ms` 必须等安全页面具备真实实现后验证。

### Android 浪潮等待动画结论

原型从 M0 首屏一键进入全屏页面，使用 Compose 原生无限过渡驱动三层圆形的 GPU 缩放。完整呼吸周期为 5 秒，插值采用正弦缓入缓出；无弹跳、闪烁或庆祝动效，文案与 Stitch `浪潮等待模式` 及 PRD §5.5 保持一致。

Android 16 / `PJZ110` 真机连续运行 15 秒结果：

```text
Total frames rendered: 925
Janky frames: 0 (0.00%)
50th / 90th / 95th / 99th: 5 / 6 / 6 / 7ms
Missed Vsync: 0
Slow UI thread: 0
Frame deadline missed: 0
```

当前目标真机流畅度通过。`PJZ110` 并非低端性能档，发布前仍需补一台首发最低性能等级 Android 设备复测，不能用本结果替代低端覆盖。

## 尚未验证的平台能力

- iOS SQLCipher 原生链接和数据验证。
- 浪潮动画的低端设备复测与危机页渲染延迟。

以上能力在真机验证前均保持“待验证”，不得据此接受 ADR-0002。

## 下一步

1. M1 首启年龄确认与基线评估 UI 原型已接入，下一步持久化年龄/首启完成状态，并把 UI 从内存适配切换到 Android 加密 `AnchorStore`。
2. 接入首启评估和真实危机页后验证触发到全屏渲染 `<300ms`。
3. 发布前补首发最低性能档 Android 设备上的浪潮动画复测。
4. 后续安装 iOS platform/runtime，补齐 iOS 宿主与 SQLCipher 验证。
5. M0 全部完成后更新 ADR-0002 为 `accepted` 或切换 Flutter。
