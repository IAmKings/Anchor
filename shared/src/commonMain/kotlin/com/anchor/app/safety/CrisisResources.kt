package com.anchor.app.safety

import com.anchor.app.storage.CrisisRegion

data class CrisisResource(
    val emergency: String,
    val hotline: String,
)

fun crisisResource(region: CrisisRegion, youth: Boolean): CrisisResource = when (region) {
    CrisisRegion.MainlandChina -> CrisisResource("110 / 120 / 119", if (youth) "12355" else "12356")
    CrisisRegion.HongKong -> CrisisResource("999", if (youth) "2382 0777" else "18111 / 2896 0000")
    CrisisRegion.Macau -> CrisisResource("999", "请前往就近精神科或急诊")
    CrisisRegion.Taiwan -> CrisisResource("110 / 119", "1925 / 1995 / 1980")
    CrisisRegion.UnitedStates -> CrisisResource("911", "988")
    CrisisRegion.UnitedKingdom -> CrisisResource("999 / 112", "NHS 111 转 2；116 123")
    CrisisRegion.Japan -> CrisisResource("110 / 119", "0570-064-556")
    CrisisRegion.Other -> CrisisResource("当地紧急电话", "请查询当地心理援助资源")
}

data class GuideHotline(
    val name: String,
    val number: String,
    val detail: String = "",
)

data class MedicalGuideEmergency(
    val title: String = "如遇生命危险或需紧急医疗干预",
    val numbers: List<String>,
)

data class MedicalGuideContent(
    val emergency: MedicalGuideEmergency,
    val hotlines: List<GuideHotline>,
)

fun MedicalGuideEmergency.actionLine(): String = when (numbers.size) {
    0 -> "请立即拨打当地急救或报警电话。"
    1 -> "请立即拨打 ${numbers.single()}"
    2 -> "请立即拨打 ${numbers[0]} 或 ${numbers[1]}"
    else -> "请立即拨打 ${numbers.dropLast(1).joinToString("、")} 或 ${numbers.last()}"
}

fun medicalGuideContent(region: CrisisRegion, youth: Boolean): MedicalGuideContent = when (region) {
    CrisisRegion.MainlandChina -> MedicalGuideContent(
        emergency = MedicalGuideEmergency(numbers = listOf("120", "110")),
        hotlines = buildList {
            add(GuideHotline("全国统一心理援助热线", "12356", "国家卫健委设立"))
            if (youth) add(GuideHotline("青少年法律与心理咨询热线", "12355", "共青团中央设立"))
            add(GuideHotline("北京心理危机研究与干预中心", "010-82951332", "800-810-1117"))
        },
    )
    CrisisRegion.Taiwan -> MedicalGuideContent(
        emergency = MedicalGuideEmergency(numbers = listOf("110", "119")),
        hotlines = listOf(
            GuideHotline("安心专线（卫福部）", "1925", "24小时心理咨询服务"),
            GuideHotline("生命线", "1995", "自杀防治专线"),
            GuideHotline("张老师专线", "1980", "青少年与大众心理咨商"),
        ),
    )
    CrisisRegion.HongKong -> MedicalGuideContent(
        emergency = MedicalGuideEmergency(numbers = listOf("999")),
        hotlines = buildList {
            add(GuideHotline("情绪通精神健康支援热线", "18111", "24小时支援"))
            add(GuideHotline("生命热线", "2382 0000", "24小时热线"))
            add(GuideHotline("医院管理局精神健康专线", "2466 7350"))
            add(GuideHotline("香港撒玛利亚防止自杀会", "2389 2222"))
            add(GuideHotline("香港撒玛利亚会防止自杀热线（多语言）", "2896 0000"))
            if (youth) add(GuideHotline("生命热线青少年专线", "2382 0777"))
        },
    )
    CrisisRegion.UnitedStates -> MedicalGuideContent(
        emergency = MedicalGuideEmergency(numbers = listOf("911")),
        hotlines = listOf(
            GuideHotline("988 Suicide & Crisis Lifeline", "988", "24小时免费保密支持"),
        ),
    )
    CrisisRegion.Macau,
    CrisisRegion.UnitedKingdom,
    CrisisRegion.Japan,
    CrisisRegion.Other,
    -> {
        val primary = crisisResource(region, youth)
        val numbers = primary.emergency
            .split(" / ", "；", ";")
            .map { it.trim() }
            .filter { it.isNotEmpty() && it != "当地紧急电话" }
        MedicalGuideContent(
            emergency = MedicalGuideEmergency(numbers = numbers),
            hotlines = listOf(
                GuideHotline(if (youth) "青少年热线" else "心理援助热线", primary.hotline),
            ),
        )
    }
}

fun guideHotlines(region: CrisisRegion, youth: Boolean): List<GuideHotline> =
    medicalGuideContent(region, youth).hotlines

data class SomaticPrepItem(val title: String, val note: String)

val somaticPrepItems = listOf(
    SomaticPrepItem("甲状腺功能", "甲亢或甲减也可能表现为焦虑或低落。"),
    SomaticPrepItem("血糖", "低血糖或血糖波动有时会像心慌和烦躁。"),
    SomaticPrepItem("维生素 D / B12", "缺乏时也可能出现疲惫和情绪变化。"),
    SomaticPrepItem("贫血", "供氧不足常被体会成无力或心情沉。"),
    SomaticPrepItem("睡眠呼吸暂停", "夜间反复憋醒会让白天更难撑住。"),
    SomaticPrepItem("慢性疼痛或药物副作用", "身体持续不适或药物影响也可能改变化心情。"),
    SomaticPrepItem("围产期相关变化（如适用）", "孕期、产后或经前期激素变化可能放大波动。"),
)
