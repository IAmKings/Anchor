package com.anchor.app.onboarding

import com.anchor.app.safety.AssessmentBand
import com.anchor.app.safety.SafetyAction
import com.anchor.app.storage.CrisisRegion
import com.anchor.app.storage.FirstAnchor

internal data class FirstAnchorOption(
    val anchor: FirstAnchor,
    val title: String,
    val description: String,
)

internal val p1FirstAnchors = listOf(
    FirstAnchorOption(FirstAnchor.SocialEnergy, "无损消耗", "留意需要全程自我监控的关系。"),
    FirstAnchorOption(FirstAnchor.AltruisticTask, "利他微任务", "用一件小事把注意力转向外部。"),
)

internal val p0FirstAnchors = listOf(
    FirstAnchorOption(FirstAnchor.EmotionLabel, "给情绪起名", "把模糊的难受变成具体的词。"),
    FirstAnchorOption(FirstAnchor.MicroAction, "5 分钟微行动", "不等动机，先做一个荒唐小的动作。"),
    FirstAnchorOption(FirstAnchor.FactsJournal, "只记事实", "分开摄像头拍到的事实和大脑推断。"),
    FirstAnchorOption(FirstAnchor.WorryVault, "把担心挂起来", "给念头安排一个确定的处理时间。"),
    FirstAnchorOption(FirstAnchor.Rhythm, "固定节律", "固定起床，起床后见光。"),
    FirstAnchorOption(FirstAnchor.WaveWaiting, "等它过去", "承认、定位身体感受，然后等待回落。"),
)

internal const val welcomeTagline = "一款不讲鸡汤，只靠行为与事实的微行为辅助工具。"
internal const val ageConfirmTitle = "年龄确认"
internal const val ageDisclaimer = "本产品面向 18 岁以上人群设计。年龄只由你自报，不会尝试检测。"
internal const val adultAgeLabel = "18 岁以上"
internal const val youthAgeLabel = "14–17 岁"
internal const val crisisRegionLabel = "危机资源地区"
internal const val agreementPrefix = "我已阅读并同意"
internal const val agreementAnd = "与"
internal const val termsTitle = "用户协议"
internal const val termsBody = "锚点是本地微行为辅助工具，不是医疗器械，也不提供诊断或治疗。量表仅供筛查参考。"
internal const val privacyTitle = "隐私政策"
internal const val privacyBody = "数据只留在这台设备。没有账号，也不会上传。换机只能通过你自己导出的加密备份。"
internal val agreementLabel = "$agreementPrefix$termsTitle$agreementAnd$privacyTitle"
internal const val startBaselineLabel = "开始基线评估"
internal const val startBaselineHint = "终结自我审判的第一步"
internal const val scalePrompt = "过去两周，你有多少时间被以下问题困扰？请根据真实感受选择。"
internal const val scaleDisclaimer = "评估结果仅供参考，不作为医疗诊断依据。"
internal const val scaleSource = "PHQ-9 / GAD-7 版权归 Pfizer，来源 phqscreeners.com。免费用于筛查参考。"
internal const val submitAssessmentLabel = "提交评估"
internal const val bufferTitle = "评估已提交"
internal const val bufferBody = "深呼吸，我们正在为你整理反馈…"
internal const val mildInsight = "这是一次自我觉察，而非最终判决。"
internal const val chooseFirstAnchorLabel = "选择第一个锚点"
internal const val oneThingKicker = "一次一件"
internal const val medicalQuote = "判断这个不是你的工作，是医生的工作。去做一次评估，是为了终结自我审判。"
internal const val medicalWaitingTitle = "就医等待期模式"
internal const val medicalWaitingBody = "日常练习已暂时隐去，请优先关注专业介入。暂停练习不是惩罚，是防止你在未获专业支持时过度自我要求。"
internal const val firstAnchorHeadline = "感谢你的诚实。目前你可以通过行为来进行日常调节。"
internal const val firstAnchorBody = "建议从最轻微的动作开始，请在下方列表里只选一个。"
internal const val firstAnchorCommitHint = "选定后我们将固定练习 14 天"
internal const val confirmFirstAnchorLabel = "确认选择"
internal const val oneThingLockNote = "这 14 天只练这一件。不是任务。"

internal fun firstAnchorGlyph(anchor: FirstAnchor): String = when (anchor) {
    FirstAnchor.EmotionLabel -> "情"
    FirstAnchor.MicroAction -> "步"
    FirstAnchor.FactsJournal -> "记"
    FirstAnchor.WorryVault -> "箱"
    FirstAnchor.Rhythm -> "光"
    FirstAnchor.WaveWaiting -> "浪"
    FirstAnchor.SocialEnergy -> "他"
    FirstAnchor.AltruisticTask -> "植"
}

internal fun additionalPracticeUnlocked(firstAnchorAtMillis: Long?, nowMillis: Long): Boolean =
    firstAnchorAtMillis == null || nowMillis - firstAnchorAtMillis >= REASSESSMENT_INTERVAL_MILLIS

internal fun practiceVisible(anchor: FirstAnchor, selected: FirstAnchor?, unlocked: Boolean): Boolean =
    unlocked || selected == null || selected == anchor

internal fun firstAnchorTitle(anchor: FirstAnchor): String =
    (p0FirstAnchors + p1FirstAnchors).first { it.anchor == anchor }.title

internal fun oneThingLockBody(anchor: FirstAnchor): String =
    "这 14 天只练「${firstAnchorTitle(anchor)}」。不是任务。"
internal const val item9CrisisNote = "如果你现在处于立即危险中，请直接拨打当地急救或报警电话。"

internal const val bufferDelayMillis = 3_000L

internal fun canStartBaseline(ageSelected: Boolean, agreed: Boolean): Boolean = ageSelected && agreed

internal fun shouldSkipGad7(phq9Item9: Int?): Boolean = (phq9Item9 ?: 0) > 0

internal fun allAnswered(answers: List<Int?>): Boolean = answers.isNotEmpty() && answers.all { it != null }

internal fun CrisisRegion.label(): String = when (this) {
    CrisisRegion.MainlandChina -> "中国大陆"
    CrisisRegion.HongKong -> "香港"
    CrisisRegion.Macau -> "澳门"
    CrisisRegion.Taiwan -> "台湾"
    CrisisRegion.UnitedStates -> "美国"
    CrisisRegion.UnitedKingdom -> "英国"
    CrisisRegion.Japan -> "日本"
    CrisisRegion.Other -> "其他地区"
}

internal fun phqBandLabel(band: AssessmentBand): String = when (band) {
    AssessmentBand.Minimal -> "很低"
    AssessmentBand.Mild -> "轻度状态"
    AssessmentBand.Moderate -> "中度"
    AssessmentBand.ModeratelySevere -> "中重度"
    AssessmentBand.Severe -> "重度"
}

internal fun resultBadge(action: SafetyAction): String = when (action) {
    SafetyAction.CrisisGuidance -> "需要立即支持"
    SafetyAction.MedicalWaiting -> "中度至重度"
    else -> "可以开始练习"
}
