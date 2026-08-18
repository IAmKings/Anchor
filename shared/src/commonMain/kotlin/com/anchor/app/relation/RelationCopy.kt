package com.anchor.app.relation

import com.anchor.app.storage.AltruismDraw
import com.anchor.app.storage.AltruismFeel
import com.anchor.app.storage.AltruismKind
import com.anchor.app.storage.EnergyMark
import com.anchor.app.storage.RelationContact
import com.anchor.app.storage.RelationEnergyEntry

internal data class AltruismPreset(val title: String, val kind: AltruismKind)

internal val nonSocialAltruism = listOf(
    AltruismPreset("给一盆植物浇水", AltruismKind.NonSocial),
    AltruismPreset("把桌面收拾出一小块空位", AltruismKind.NonSocial),
    AltruismPreset("把一份资料放回原处", AltruismKind.NonSocial),
    AltruismPreset("回答一个简单问题后就停下", AltruismKind.NonSocial),
    AltruismPreset("叠好一件衣服", AltruismKind.NonSocial),
)

internal val socialAltruism = listOf(
    AltruismPreset("发一句完整的谢谢", AltruismKind.Social),
    AltruismPreset("顺手给旁边的人倒半杯水", AltruismKind.Social),
    AltruismPreset("把门替后面的人扶一下", AltruismKind.Social),
)

internal const val relationHubTitle = "关系与利他"
internal const val relationHubBody = "盘点需要演戏的关系，或抽一件对别人有用的小事。默认先做不社交的。"
internal const val inventoryTitle = "关系耗竭自测"
internal const val inventoryPrompt = "跟这个人相处时，我需要监控自己吗？"
internal const val inventoryHint = "需要全程注意措辞、表情或接话，就是表演耗竭。"
internal const val monitorYes = "需要监控"
internal const val monitorNo = "不需要"
internal const val ledgerTitle = "回血 / 抽干"
internal const val ledgerHint = "见面后记一下，被充满了还是被抽干了。"
internal const val filledLabel = "回血"
internal const val drainedLabel = "抽干"
internal const val altruismTitle = "微小利他"
internal const val drawNonSocialLabel = "抽一张非社交"
internal const val drawSocialLabel = "抽一张社交"
internal const val peoplePleasingHint = "做之前先想：我期待对方怎么回应？若期待回应，这就是讨好。"
internal const val feelPrompt = "做完之后，你是更轻还是更紧？"
internal const val lighterLabel = "更轻"
internal const val tighterLabel = "更紧"
internal const val leavingCopy = "如果你的痛苦来自榨干你的工作、贬低你的关系、持续伤害的家庭，最有效的心理干预有时是离开。产品不鼓吹调节好就能忍下去。"

internal fun reliefAdvice(name: String): String =
    "可以把和「$name」的见面从每周一次降到每月一次。这不是断交，是减负。"

internal fun notBreakingOffCopy(): String = "减负不是断交。"

internal fun nextAltruismCard(recent: List<AltruismDraw>, preferSocial: Boolean = false): AltruismPreset {
    val recentTighter = recent.mapNotNull { it.felt }.take(3).count { it == AltruismFeel.Tighter }
    val useSocial = preferSocial && recentTighter < 2
    val pool = if (useSocial) socialAltruism else nonSocialAltruism
    val used = recent.map { it.title }.toSet()
    return pool.firstOrNull { it.title !in used } ?: pool.first()
}

internal fun shouldPauseAltruism(recent: List<AltruismDraw>): Boolean {
    val felt = recent.mapNotNull { it.felt }.take(3)
    return felt.size >= 3 && felt.all { it == AltruismFeel.Tighter }
}

internal fun pauseAltruismCopy(): String =
    "连续几次更紧。小剂量、无压力才有效，变成讨好就会反噬。可以先停用，或只抽非社交。"

internal fun monitorCopy(contacts: List<RelationContact>): String {
    val count = contacts.count { it.monitorsSelf == true }
    return if (count == 0) "还没有需要监控自己的关系" else "$count 段需要监控自己"
}

internal fun energyCopy(entries: List<RelationEnergyEntry>): String {
    if (entries.isEmpty()) return "还没有回血或抽干记录"
    val filled = entries.count { it.mark == EnergyMark.Filled }
    val drained = entries.count { it.mark == EnergyMark.Drained }
    return "回血 $filled · 抽干 $drained"
}

internal fun altruismFeelCopy(draws: List<AltruismDraw>): String {
    val felt = draws.filter { it.felt != null }
    if (felt.isEmpty()) return "还没有记下的体感"
    val tighter = felt.count { it.felt == AltruismFeel.Tighter }
    return "已记下 ${felt.size} 次体感，其中更紧 $tighter 次"
}

internal fun homeRelationBody(contacts: List<RelationContact>, draws: List<AltruismDraw>): String {
    val monitors = contacts.count { it.monitorsSelf == true }
    val openDraw = draws.firstOrNull { it.felt == null }
    return when {
        openDraw != null -> openDraw.title
        monitors > 0 -> "$monitors 段需要监控自己"
        else -> "盘点关系，或抽一件小事"
    }
}

enum class HomeRelationKind { Default, OpenDraw, Filled, Drained, Exhausted }

internal data class HomeRelationPresentation(
    val kind: HomeRelationKind,
    val bannerTitle: String? = null,
    val bannerBody: String? = null,
    val cardLabel: String = relationHubTitle,
    val cardBody: String,
    val cardHint: String? = null,
)

internal const val homeRelationBannerWindowMillis = 24L * 60 * 60 * 1_000
internal const val homeRelationFilledTitle = "刚才记下了回血"
internal const val homeRelationFilledBody = "这段相处是被充满的。不是得分。"
internal const val homeRelationDrainedTitle = "刚才记下了抽干"
internal const val homeRelationDrainedBody = "给自己留一段无需表演的空白。暂缓非必要的社交。不是断交。"
internal const val homeRelationExhaustedTitle = "连续几次更紧"
internal const val homeRelationExhaustedBody = "先只抽不社交的小事。感到不得不做，就先跳过。"
internal const val homeRelationExhaustedLabel = "非社交微任务"
internal const val homeRelationExhaustedHint = "无需期待他人回应。"
internal const val homeRelationPreviewNote = "预览中，不会改记录。"

internal fun homeRelationPresentation(
    contacts: List<RelationContact>,
    energy: List<RelationEnergyEntry>,
    draws: List<AltruismDraw>,
    nowMillis: Long,
): HomeRelationPresentation {
    val openDraw = draws.firstOrNull { it.felt == null }
    if (shouldPauseAltruism(draws)) {
        return HomeRelationPresentation(
            kind = HomeRelationKind.Exhausted,
            bannerTitle = homeRelationExhaustedTitle,
            bannerBody = homeRelationExhaustedBody,
            cardLabel = homeRelationExhaustedLabel,
            cardBody = openDraw?.title ?: nextAltruismCard(draws, preferSocial = false).title,
            cardHint = homeRelationExhaustedHint,
        )
    }
    val recentEnergy = energy.maxByOrNull { it.createdAtMillis }
        ?.takeIf { nowMillis - it.createdAtMillis in 0..homeRelationBannerWindowMillis }
    if (recentEnergy?.mark == EnergyMark.Drained) {
        return HomeRelationPresentation(
            kind = HomeRelationKind.Drained,
            bannerTitle = homeRelationDrainedTitle,
            bannerBody = homeRelationDrainedBody,
            cardBody = openDraw?.title ?: homeRelationBody(contacts, draws),
        )
    }
    if (recentEnergy?.mark == EnergyMark.Filled) {
        return HomeRelationPresentation(
            kind = HomeRelationKind.Filled,
            bannerTitle = homeRelationFilledTitle,
            bannerBody = homeRelationFilledBody,
            cardBody = openDraw?.title ?: homeRelationBody(contacts, draws),
        )
    }
    return HomeRelationPresentation(
        kind = if (openDraw != null) HomeRelationKind.OpenDraw else HomeRelationKind.Default,
        cardBody = homeRelationBody(contacts, draws),
    )
}

internal fun previewHomeRelation(kind: HomeRelationKind): HomeRelationPresentation = when (kind) {
    HomeRelationKind.Filled -> HomeRelationPresentation(
        kind = kind,
        bannerTitle = homeRelationFilledTitle,
        bannerBody = homeRelationFilledBody,
        cardBody = "盘点关系，或抽一件小事",
    )
    HomeRelationKind.Drained -> HomeRelationPresentation(
        kind = kind,
        bannerTitle = homeRelationDrainedTitle,
        bannerBody = homeRelationDrainedBody,
        cardBody = "盘点关系，或抽一件小事",
    )
    HomeRelationKind.Exhausted -> HomeRelationPresentation(
        kind = kind,
        bannerTitle = homeRelationExhaustedTitle,
        bannerBody = homeRelationExhaustedBody,
        cardLabel = homeRelationExhaustedLabel,
        cardBody = "给一盆植物浇水",
        cardHint = homeRelationExhaustedHint,
    )
    HomeRelationKind.OpenDraw -> HomeRelationPresentation(kind = kind, cardBody = "给一盆植物浇水")
    HomeRelationKind.Default -> HomeRelationPresentation(kind = kind, cardBody = "盘点关系，或抽一件小事")
}
