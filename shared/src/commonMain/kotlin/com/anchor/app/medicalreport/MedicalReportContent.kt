package com.anchor.app.medicalreport

import com.anchor.app.insights.interpretationLabelsFor
import com.anchor.app.storage.AnchorStore

// 表格区块最多列最近 10 条，总数在注记里给出；情绪卡固定取 Top 3。
const val MEDICAL_REPORT_ROW_LIMIT = 10
private const val EMOTION_TOP_LIMIT = 3
private const val DAY_MILLIS = 86_400_000L

data class MedicalReportCameraRow(
    val createdAtMillis: Long,
    val fact: String,
    val inference: String,
    val labels: List<String>,
)

// 应用内情绪卡没有强度评分：只给频次与相对占比（相对最常见情绪，仅用于画条），不虚构强度数字。
data class MedicalReportEmotionStat(
    val emotion: String,
    val count: Int,
    val relativeShare: Float,
    val lastAtMillis: Long,
)

data class MedicalReportActionRow(
    val title: String,
    val completedAtMillis: Long,
    val predicted: Int,
    val actual: Int,
) {
    val gap: Int get() = actual - predicted
}

data class MedicalReport(
    val patientIdMasked: String,
    val generatedAtMillis: Long,
    val periodFromMillis: Long?,
    val periodToMillis: Long?,
    val coreRecordCount: Int,
    val cameraRows: List<MedicalReportCameraRow>,
    val cameraTotal: Int,
    val emotionTop: List<MedicalReportEmotionStat>,
    val emotionTotal: Int,
    val actionRows: List<MedicalReportActionRow>,
    val actionTotal: Int,
) {
    val hasContent: Boolean get() = coreRecordCount > 0
}

// 报告刻意不含：忧虑卡原文与录音（最私密）、关系监控数据（涉及第三方）、量表分数（医生自会评估）。
fun buildMedicalReport(store: AnchorStore, patientId: String, nowMillis: Long): MedicalReport {
    val cameraLogs = store.cameraLogs()
    val emotions = store.emotionCards()
    val actions = store.microActions()

    val inferential = cameraLogs.filter { it.inference.isNotBlank() }
    val cameraRows = inferential.take(MEDICAL_REPORT_ROW_LIMIT).map { log ->
        MedicalReportCameraRow(
            createdAtMillis = log.createdAtMillis,
            fact = log.fact,
            inference = log.inference,
            labels = interpretationLabelsFor(log),
        )
    }

    val grouped = emotions.groupBy { it.emotion }
        .map { (emotion, cards) -> Triple(emotion, cards.size, cards.maxOf { it.createdAtMillis }) }
        .sortedWith(compareByDescending<Triple<String, Int, Long>> { it.second }.thenByDescending { it.third })
    val topCount = grouped.firstOrNull()?.second ?: 0
    val emotionTop = grouped.take(EMOTION_TOP_LIMIT).map { (emotion, count, lastAt) ->
        MedicalReportEmotionStat(
            emotion = emotion,
            count = count,
            relativeShare = if (topCount > 0) count.toFloat() / topCount else 0f,
            lastAtMillis = lastAt,
        )
    }

    val completed = actions.filter { it.predictedDifficulty != null && it.actualDifficulty != null }
    val actionRows = completed.take(MEDICAL_REPORT_ROW_LIMIT).map { action ->
        MedicalReportActionRow(
            title = action.title,
            completedAtMillis = action.completedAtMillis ?: action.createdAtMillis,
            predicted = action.predictedDifficulty!!,
            actual = action.actualDifficulty!!,
        )
    }

    val created = buildList {
        cameraLogs.forEach { add(it.createdAtMillis) }
        emotions.forEach { add(it.createdAtMillis) }
        actions.forEach { add(it.createdAtMillis) }
    }
    return MedicalReport(
        patientIdMasked = maskPatientId(patientId),
        generatedAtMillis = nowMillis,
        periodFromMillis = created.minOrNull(),
        periodToMillis = created.maxOrNull(),
        coreRecordCount = cameraLogs.size + emotions.size + actions.size,
        cameraRows = cameraRows,
        cameraTotal = inferential.size,
        emotionTop = emotionTop,
        emotionTotal = emotions.size,
        actionRows = actionRows,
        actionTotal = completed.size,
    )
}

// 页眉只显示打码版；完整 ID 留在设备上，跨多次报告的尾 4 位保持一致即可供医生对档。
fun maskPatientId(patientId: String): String =
    if (patientId.length >= 8) "ANC-****-" + patientId.takeLast(4) else "ANC-****"

// 落差 = 实际 − 预测。负值最常见（预期高估），只报数字差，不给认知模式定名。
fun medicalReportGapCopy(gap: Int): String = when {
    gap < 0 -> "预测高 ${-gap} 分"
    gap > 0 -> "实际高 $gap 分"
    else -> "与预测一致"
}

fun medicalReportPeriodDays(fromMillis: Long, toMillis: Long): Long =
    (toMillis - fromMillis) / DAY_MILLIS + 1

fun medicalReportCountLine(total: Int): String = "共 $total 条核心记录"

fun medicalReportTruncatedNote(total: Int): String =
    if (total > MEDICAL_REPORT_ROW_LIMIT) "共 $total 条，仅列最近 $MEDICAL_REPORT_ROW_LIMIT 条。" else ""

const val medicalReportTitle = "状态记录导出报告"
const val medicalReportSubtitle = "医生审阅版"
const val medicalReportMetaPatient = "患者 ID"
const val medicalReportMetaPeriod = "报告生成期"
const val medicalReportMetaCount = "记录频次"
const val medicalReportMetaExportedAt = "导出时间"

const val medicalReportCameraTitle = "双栏日志分析（事实 vs. 推断）"
const val medicalReportCameraDetail = "红色标注为该条推断命中的解释句式，来自应用内词表，不构成归类结论。"
const val medicalReportColumnTime = "日期/时间"
const val medicalReportColumnFact = "事实（客观）"
const val medicalReportColumnInference = "推断（主观）"

const val medicalReportEmotionTitle = "核心情绪卡片分布（Top 3）"
const val medicalReportEmotionDetail = "应用内没有强度评分，条长为相对最常见情绪的出现次数。"
const val medicalReportEmotionFrequency = "出现频次"
const val medicalReportEmotionLastAt = "最近一次"

const val medicalReportActionTitle = "微行动验证试验（预测 vs. 实际）"
const val medicalReportActionDetail = "行为激活记录：行动前预测的困难度与实际体感之间的落差。"
const val medicalReportColumnAction = "微行动"
const val medicalReportColumnPredicted = "预测难度"
const val medicalReportColumnActual = "实际体感"
const val medicalReportColumnGap = "落差"

const val medicalReportEmptySection = "本期暂无相关记录。"
const val medicalReportConfidential = "CONFIDENTIAL MEDICAL RECORD"
const val medicalReportDisclaimer =
    "此报告由使用者主动授权通过锚点应用生成，仅供医师评估参考。内容为使用者本人的自评记录，不是诊断，数据已经脱敏。"
const val medicalReportGeneratedBy = "GENERATED BY ANCHOR APP"
