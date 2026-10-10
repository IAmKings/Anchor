package com.anchor.app.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anchor.app.ui.AnchorBackBar

// 法务全文的离线快照，与 anchor-legal.125457.xyz 上的公开网页同文。
// 网页是权威版本：法务文本更新时先改网页，再随应用版本更新这里。

enum class LegalDocKind { Terms, Privacy }

data class LegalSection(
    val heading: String,
    val paragraphs: List<String>,
)

private const val legalEffectiveDate = "生效日期：2026 年 10 月 10 日。应用内的文本随应用版本发布，最新版本以网页为准。"

internal val termsSections: List<LegalSection> = listOf(
    LegalSection(
        "1. 这是什么",
        listOf(
            "锚点（Anchor）是一款微行为辅助工具。它帮你把情绪、事实与推断、忧虑、一个很小的行动、起床和见光，记在你自己的手机上。应用没有账号，也没有云端同步。",
            "安装、打开或勾选「我已阅读并同意用户协议与隐私政策」，即表示你接受本协议和同时公布的隐私政策。若你不同意，请不要继续使用，并可以卸载应用。",
        ),
    ),
    LegalSection(
        "2. 不是医疗，也不代替专业帮助",
        listOf(
            "锚点不是医疗器械，不提供诊断、治疗、处方或危机干预服务。PHQ-9 与 GAD-7 只供你自己做筛查参考，版权归 Pfizer，来源为 phqscreeners.com。分数和分档不是诊断。",
            "应用面向尚未达到精神科诊断阈值、但仍有情绪波动、内耗或反刍的日常调节。它不能判断你是否患病，也不能替代医生、心理咨询师或危机热线。",
            "若你正处于危险之中，或有伤害自己、伤害他人的想法，请立刻联系你所在地的紧急电话和心理援助热线。应用可以按你自己选择的地区显示这些号码，但不会代你拨打，也不会有人在远端看着你的记录并前来救助。",
            "量表里关于自伤的题目若达到危机提示，应用会进入危机说明，而不是继续把你送进练习。任一量表达到中重度分档时，练习入口会先关上，进入就医等待。等待期是否结束，由本机上的后续评估决定，不是由开发者远程决定。",
        ),
    ),
    LegalSection(
        "3. 谁可以使用",
        listOf(
            "产品面向 18 岁以上的人。年龄只由你在首启时自己选择，应用不会、也不能检测年龄，也不向任何机构核验身份。",
            "若你选择 14–17 岁，应用仍可在本机使用，并改用面向青少年的危机资源说明，同时建议监护人知情。这不表示开发者已经同意未成年人单独使用，也不建立监护关系。13 岁及以下的人不应使用本应用。若你是监护人，请在未成年人的设备上卸载它。",
        ),
    ),
    LegalSection(
        "4. 应用为你做什么",
        listOf(
            "功能都在本机完成，包括：",
            "· 基线评估、复评，以及据此在本机切换练习是否可见。",
            "· 一次只固定练习一件事。选定后约 14 天内不把其余练习铺开。这不是任务，也没有打卡或连续天数。",
            "· 情绪命名、双栏日志、忧虑保险箱、约 5 分钟的微行动、晨间节律、浪潮等待。",
            "· 关系与利他的本地记录，以及只根据本机记录算出的洞察。",
            "· 四类可关闭的本地提醒、可选的应用锁、加密导出和彻底删除。",
            "双栏日志里的「摄像头」是一种写法，指你亲眼看到的事实。应用不调用手机相机，也不读取相册。",
            "应用不保证某一项练习会让你变好，也不保证提醒一定准时送达。系统可以推迟闹钟，或在你关闭通知后改为下次打开时的应用内提示。",
        ),
    ),
    LegalSection(
        "5. 你写下的内容",
        listOf(
            "评估答案、情绪、日志、忧虑、行动、节律、关系备注和录音，都由你主动写入，并只为你在这台设备上回看。开发者不会阅读这些内容，也没有服务器可以接收它们。",
            "你应避免在记录里写入不必要的他人敏感信息。关系名单若你写下了别人的名字，那份名字也只留在这台设备上，直到你自己删除或导出。",
        ),
    ),
    LegalSection(
        "6. 导出、分享与删除",
        listOf(
            "换机靠你自己生成的加密备份，不靠账号。导出前你要设定至少 8 个字符的密码。这个密码不会保存在应用里。丢失密码后，开发者也无法帮你恢复。",
            "你若把备份交给系统分享，之后的传送由你选择的应用或对方负责。请用单独、可靠的方式把密码告诉需要它的人，不要把密码和文件放在同一条公开消息里。",
            "备份也可以通过系统文件夹选择器保存到你在设备上指定的位置。保存之后，那份文件在应用之外，由你自行保管；删除应用或清除应用数据都不会回收它。",
            "恢复会替换这台设备上现有的记录。更早制作的备份可能没有录音。彻底删除需二次确认，删除后无法从开发者处找回，因为开发者处本来就没有副本。",
        ),
    ),
    LegalSection(
        "7. 版本与商店包",
        listOf(
            "日常安装包和将来的商店包不申请网络权限，因此也不会在线检查更新。仅供内部测试、且单独签名的安装包，可以在启动或你点击「检查更新」时读取公开的版本列表。该请求不上传你的记录。商店包没有这项功能，也不能覆盖安装到内部测试包上。更换商店包之前，请先自己导出备份。",
        ),
    ),
    LegalSection(
        "8. 责任边界",
        listOf(
            "应用按「现状」提供。在法律允许的范围内，开发者不对下列情况承担责任：你依据量表或洞察自行作出的医疗决定；你未寻求专业帮助而延误照护；你丢失导出密码或备份文件；操作系统清除应用数据；你主动分享备份后发生的泄露。",
            "若你所在地的法律不允许排除某些责任，则那些责任不受上一句限制。本协议没有把你的法定权利全部取消。",
        ),
    ),
    LegalSection(
        "9. 协议如何变更",
        listOf(
            "协议若有实质变更，会更新网页版本顶部的日期。应用没有账号，也无法把新文本推送到旧版本里；应用内的这份文本随应用版本更新。以日期较新的文本为准。",
        ),
    ),
    LegalSection(
        "10. 联系",
        listOf(
            "本页不设表单，也不会因此收到你的记录。开发者联系方式以应用商店页面公布的为准。请不要把评估、日记或录音发送给开发者。",
        ),
    ),
)

internal val privacySections: List<LegalSection> = listOf(
    LegalSection(
        "1. 先说结论",
        listOf(
            "锚点（Anchor）把你的记录留在这台设备上。没有账号，没有云端副本，也没有分析或广告组件。商店版和日常调试版不申请网络权限，因此应用不会把评估、日记、忧虑或录音上传到开发者或任何服务器。",
            "请同时阅读用户协议（在应用内可打开）。应用不是医疗器械，量表结果不是诊断。",
        ),
    ),
    LegalSection(
        "2. 我们不收集的东西",
        listOf(
            "· 不收集姓名、手机号、电子邮箱、身份证件或通讯录。你若在关系记录里自己写下别人的名字，那只存在你的设备上。",
            "· 不采集位置，不读取相册，不调用相机。",
            "· 不集成崩溃上报、行为统计、广告或推送服务商。",
            "· 不使用系统的云备份保存应用数据。应用声明不允许系统自动备份它的数据。",
            "· 不读取你的指纹或面容图像。应用锁只向系统询问「是否通过」，通过与否由系统完成。",
        ),
    ),
    LegalSection(
        "3. 数据存在哪里",
        listOf(
            "记录保存在应用的私有目录中，使用 SQLCipher 加密。打开数据库的密钥是随机生成的，并由 Android 密钥库包裹。它不是你设置的导出密码，你也看不到这把密钥。",
            "开发者没有存放这些数据库的服务器，所以也不存在由开发者恢复、调取或按政府以外的渠道提供云端副本。若法律要求向设备持有人以外的人提供数据，能够提供的只有这台设备上实际存在、且能被依法访问的内容。开发者自己打不开你的手机。",
        ),
    ),
    LegalSection(
        "4. 设备上可能有哪些记录",
        listOf(
            "只有你使用相应功能时，本机才会写入这些内容：",
            "· 你自报的年龄段（18 岁以上，或 14–17 岁）、你选择的危机资源地区、你选定的第一件练习及其开始时间。",
            "· PHQ-9 与 GAD-7 的逐题答案、分数、分档，以及本机据此保存的安全状态。安全状态用来决定练习是否可见、是否处于就医等待。",
            "· 情绪卡片：情绪词、发生了什么、最难的部分，以及你标记它已经过去的时间。",
            "· 双栏日志：你写下的事实和推断。",
            "· 忧虑卡片：文字、封存时间、计划处理的时间、处理结果。若你使用录音，还会有一个仅指向本机文件的录音名。",
            "· 微行动：标题、预计难度、实际难度和起止时间。它可能和某一张忧虑卡片关联。",
            "· 晨间节律：起床时间、见光时间。",
            "· 关系与利他：你写下的称呼和备注、是否觉得相处时要监控自己、回血或抽干的标记、抽到的小事和你的体感。",
            "· 提醒开关、进行中的本地计时、应用锁是否开启。这些是设备上的偏好或闹钟，不是一份上传的档案。",
            "· 外观是跟随系统、浅色还是深色。这只影响显示，彻底删除记录时不会把它当作临床数据清掉。",
            "洞察页上的稳定度、预测偏差等数字，是应用在本机用上述记录现算的。样本不够时，页面会写「记录还不够」。这些数字不会发送出去。",
        ),
    ),
    LegalSection(
        "5. 系统权限",
        listOf(
            "· 麦克风。仅在你点下语音速记或保存本地录音时使用。不用时不会在后台录音。",
            "· 生物识别或设备凭据。仅在你打开应用锁之后，用于确认是你在打开应用。关闭应用锁后不再要求验证。",
            "· 通知。仅用于你允许的本地提醒。没有远程推送。锁屏上只显示「锚点」，不显示正文。四类提醒都可以单独关掉。系统若不允许通知，相关提示改为你下次打开应用时的横幅。",
            "应用不申请网络权限（见下方唯一例外）、位置、相机、通讯录或存储管理权限。导出文件通过系统提供的临时只读地址交给你选择的分享目标，也可以经系统文件夹选择器写入你指定的位置。应用自始不持有存储权限，也不会读取你选定位置里的其他内容。",
        ),
    ),
    LegalSection(
        "6. 语音与录音",
        listOf(
            "语音识别只使用系统提供的端侧识别。设备没有可用的端侧中文模型时，应用不会改走在线识别，而会告诉你当前不可用。识别失败时，你可以改为一句文字，或只在本机保存一段 m4a 录音。也可以在「我的 → 语音识别」按需下载离线的高精度模型，下载后识别依旧在本机完成。",
            "录音文件放在应用私有目录。离开应用或识别结束时，识别器会被关掉。录音不会被上传。播放也只在本机进行。",
        ),
    ),
    LegalSection(
        "7. 通知里有什么",
        listOf(
            "本地提醒最多四类：忧虑专场开始前、危机后的两次关怀、距上次评估约两周的复评邀请、就医等待期间大约每 7 天一次的资源提醒。没有「好久没来」这类催促，也没有连续打卡提醒。",
            "计时结束时，通知同样只显示应用名。具体句子在你打开应用后才能看到。提醒由这台设备的闹钟触发，不经过开发者的服务器。",
        ),
    ),
    LegalSection(
        "8. 应用锁",
        listOf(
            "应用锁开启后，每次回到应用都要先通过系统的生物识别或设备凭据。应用不保存你的生物特征。开启期间，应用还会要求系统在多任务预览里遮住画面，减少旁边的人直接看到记录。",
        ),
    ),
    LegalSection(
        "9. 导出、恢复与你主动分享",
        listOf(
            "导出生成一个加密的 .anchor 文件，内有 JSON 与 CSV，新备份还可以带上本地录音。加密使用你当场输入的密码（至少 8 个字符），算法为 PBKDF2 与 AES-GCM。密码不写入应用，也不写入开发者能读取的任何地方。",
            "你点击分享后，文件会交给 Android 的分享界面。从那一刻起，接收方是谁、对方是否再上传，由你选择的应用和你自己决定。这是你主动把备份送出设备，不是锚点在后台同步。",
            "你也可以不经过分享界面，通过系统文件夹选择器把备份直接保存到你在设备上指定的任意位置（例如下载或文档目录）。写出的仍然只是这一份加密文件；保存位置由你选择和掌控，应用不会读取或上传该位置的其他内容。删除应用数据不会清除你保存在外部的备份副本。",
            "恢复前会再次确认。恢复会替换本机现有记录。更早的备份可能没有录音，恢复后那些录音仍然缺失。密码错误或文件损坏时，恢复不会进行。",
        ),
    ),
    LegalSection(
        "10. 删除",
        listOf(
            "在「我的」里可以彻底删除。确认之后，本机会清掉评估、记录、录音、提醒、计时、导出缓存和应用锁。此操作不可恢复。",
            "卸载应用通常也会由系统删掉应用的私有数据。已经分享出去的备份不在手机上，卸载不会销毁那些副本。请自行删除你保存或发出的文件。",
        ),
    ),
    LegalSection(
        "11. 年龄",
        listOf(
            "年龄只有你自己点选的那一项，不会用证件、账号或设备特征去推断。产品面向 18 岁以上。选择 14–17 岁的人会看到不同的危机资源说明和监护人建议。应用没有单独的儿童账号，也不应被 13 岁及以下的人使用。",
            "因为没有账号，开发者无法远程关闭某一台儿童设备上的应用。监护人应在那台设备上卸载。",
        ),
    ),
    LegalSection(
        "12. 内部测试包的唯一网络请求",
        listOf(
            "商店版和日常调试版没有这项功能。",
            "仅供内部测试的安装包可以在进程启动时，以及你点击「检查更新」时，向公开仓库的 GitHub Releases 地址发送一次读取请求，用来比较版本号。请求没有正文，不包含你的记录、量表、录音、密码或设备标识，应用里也不放置访问令牌。失败时不会挡住使用，也不会弹出要求你登记身份的窗口。",
        ),
    ),
    LegalSection(
        "13. 危机信息",
        listOf(
            "热线号码和紧急电话写在应用里，按你选择的地区显示。应用不会根据你的位置自动切换，也不会把你正在阅读帮助页这件事报告给任何人。号码是否仍然有效，以发布前的核对为准。应用不会联网改写这些号码。",
        ),
    ),
    LegalSection(
        "14. 这两份文件",
        listOf(
            "用户协议和本政策在应用内随版本提供，公开网页版本是最新的权威文本。网页不要求登录，不放置统计脚本。请不要把个人记录发到托管这些网页的服务上。",
            "网页由 Cloudflare Pages 托管。传输页面时，托管方可能按其自身规则记录连接所需的技术信息，例如 IP 地址和时间。那不是锚点收集的使用数据，锚点也由此拿不到你的日记或评估。应用本身并不请求这两份网页。",
        ),
    ),
    LegalSection(
        "15. 第三方",
        listOf(
            "量表文本的权利归 Pfizer。应用只在本机展示，用于筛查参考。",
            "端侧语音识别由你的设备系统提供。应用只在系统确认存在端侧识别时才调用它。",
            "除此之外，应用不把记录交给分析、广告、云存储或账号服务。你自己分享备份时涉及的应用，不在此列。",
        ),
    ),
    LegalSection(
        "16. 政策如何变更",
        listOf(
            "实质变更会更新网页版本顶部的日期。应用没有渠道把新政策推送到已经安装的旧版本；应用内的这份文本随应用版本更新。以日期较新的文本为准。",
        ),
    ),
    LegalSection(
        "17. 联系",
        listOf(
            "本页不收集来信。开发者联系方式以应用商店页面公布的为准。请不要通过任何渠道把评估答案、日记或录音发给开发者。那些内容开发者无法代为保管，也不应离开你的设备。",
        ),
    ),
)

@Composable
fun LegalDocumentScreen(
    kind: LegalDocKind,
    onBack: () -> Unit,
) {
    val title = if (kind == LegalDocKind.Terms) termsTitle else privacyTitle
    val sections = if (kind == LegalDocKind.Terms) termsSections else privacySections
    AnchorBackBar(onBack = onBack, title = title) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                legalEffectiveDate,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                lineHeight = 21.sp,
            )
            sections.forEach { section ->
                Text(
                    section.heading,
                    Modifier.semantics { heading() },
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                )
                section.paragraphs.forEach { paragraph ->
                    Text(paragraph, fontSize = 15.sp, lineHeight = 24.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "最新版本以 anchor-legal.125457.xyz 上的公开网页为准。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}
