# 跨平台技术方向：Compose Multiplatform（对比 Flutter）

本产品需要 iOS + Android 双端、小团队、深度原生能力（本地通知、语音识别、生物识别、加密存储）且无后端。我们决定以 **Compose Multiplatform** 为推荐方向，理由：Kotlin 单语言、expect/actual 直接互操作系统 API、可混合 SwiftUI 精修 iOS 关键页（如危机干预页）。Flutter 为备选——当团队无 Kotlin 背景或要求 iOS"开箱即稳"时采用。

**Status**: proposed（最终选型须经 PRD §12.7 原型验证清单后由技术负责人拍板）

**Considered Options**:
- Flutter：插件生态更成熟、iOS 端生产稳定多年，但原生能力需走 Platform Channel，且要求 Dart 技术栈。
- CMP：iOS 端成熟度是主要风险点（Beta→Stable 过渡期），已列入原型验证清单。

**Consequences**:
- 无论最终选型如何，业务逻辑与数据层必须与 UI 解耦；危机判定引擎、量表分档、阈值滞后状态机做成平台无关纯 Kotlin 模块并 100% 单测——未来切换 CMP/Flutter 时核心逻辑无需重写。
