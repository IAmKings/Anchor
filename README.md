# 锚点 Anchor

一款不讲鸡汤、不强求「积极想开」的微行为辅助应用。面向亚临床情绪波动与内耗：用极简动作和事实记录改变输入，而不是说服自己。

当前版本 `0.1.0`。数据只留在本机，没有账号、后端、云同步、埋点或广告 SDK。

> 本应用**不是**医疗诊断或治疗工具。出现中重度症状、自伤或轻生念头时，应寻求专业医疗帮助。

## 适用与不适用

**适用**：未达精神科诊断阈值、但有情绪波动、反刍、轻度压力焦虑的人。

**不适用**：中重度抑郁/焦虑、自伤或轻生念头。首启 PHQ-9 / GAD-7 达中重度，或 PHQ-9 第 9 题得分大于 0，会进入危机引导或就医等待期，并暂停练习类功能。

## 原则

- **纯本地**：SQLCipher 加密存储；换机靠用户主动的加密导出 / 导入。
- **无羞耻机制**：没有 streak、完成率、补卡或催促。断档不警告。
- **锁屏脱敏**：系统通知锁屏只显示「锚点」，不显示正文。
- **就医等待期不是惩罚**：练习类入口关闭，就医指南、躯体清单和仅记录的双栏日志仍可用。解除需连续两次（间隔 ≥7 天）回落轻度。

详见 [ADR-0001](docs/adr/0001-local-only-no-account.md)、[ADR-0003](docs/adr/0003-anti-kpi-metrics.md)。

## 功能

| 模块 | 做什么 |
| --- | --- |
| 浪潮等待 | 难受时：承认 → 定位身体受压处 → 等 10 分钟，不对抗 |
| 情绪标签箱 | 从具体词库起名，写成一句可看见的对象 |
| 双栏日志 | 左栏摄像头事实，右栏大脑推断；只写一栏也可以 |
| 忧虑保险箱 / 挂卡 | 白天速记封存，默认 20:00–20:20 专场开箱处理 |
| 微行动 | 5 分钟低门槛动作，记下预测 vs 实际；可翻「历史证据」 |
| 起床 · 见光 | 两个独立打卡点，洞察里看稳定度，不是得分 |
| 关系与利他 | 表演耗竭盘点、回血/抽干账本、默认非社交的微小利他 |
| 就医与帮助 | 地区化热线与就诊准备；大陆 / 港 / 澳 / 台 / 美 / 英 / 日 |

底部四个入口：今天、记录、洞察、我的。

## 技术栈

- Kotlin · Compose Multiplatform（共享 UI 在 `shared/`）
- Android 应用：`androidApp/`，`applicationId` `com.anchor.app`，minSdk 26
- iOS 壳：`iosApp/`，通过共享 framework 承载同一套 Compose；真机验证仍以 Android 为主
- SQLDelight + SQLCipher（Android）
- 提醒与后台计时：AlarmManager；通知不可用时写入应用内兜底

架构决策见 [ADR-0002](docs/adr/0002-compose-multiplatform.md)。

## 仓库结构

```
shared/          共享领域、存储、Compose 页面与测试
androidApp/      Android 宿主、权限、计时、导出分享
iosApp/          SwiftUI 壳
design/          Stitch 设计稿（实现对照）
docs/            可行性、ADR、调研
source/          源文章与方法笔记
PRD.md           产品需求
CONTEXT.md       领域用语（给实现对照，避免鸡汤词）
```

## 环境

- JDK 11+
- Android SDK（compileSdk / targetSdk 36）
- 可选：Xcode（编 iOS 壳）

依赖仓库在 `settings.gradle.kts` 中配置了阿里云 Google 镜像与 Maven Central。

## 常用命令

```bash
# PRD 结构校验（章节 / FR 编号 / 交叉引用）
make verify

# 共享模块单元测试
./gradlew :shared:testDebugUnitTest

# 安装 Debug 包到已连接设备
./gradlew :androidApp:installDebug
```

真机调试包名：`com.anchor.app`。

## 文档

| 文件 | 内容 |
| --- | --- |
| [PRD.md](PRD.md) | 需求、安全边界、地区热线表 |
| [CONTEXT.md](CONTEXT.md) | 产品用语与禁用说法 |
| [IMPLEMENTATION_PLAN.md](IMPLEMENTATION_PLAN.md) | 实施基线与切片记录 |
| [DESIGN.md](DESIGN.md) / [design/](design/) | 视觉与页面稿 |
| [docs/FEASIBILITY_CHECK.md](docs/FEASIBILITY_CHECK.md) | 可行性检查 |

## 当前范围外

- 医生视角 PDF 报告（导出为加密 JSON + CSV）
- 第 5 类晨间节律系统提醒（提醒白名单只有忧虑专场、危机后关怀、复评、就医等待期）
- 锁屏通知插画素材
- iOS 真机作为首发阻塞验证

## 许可与医疗声明

本仓库尚未声明开源许可证。应用内量表（PHQ-9 / GAD-7）与危机热线号码以 PRD 及发布前复核为准；号码可能变更，产品不做在线校验。
