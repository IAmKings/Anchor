package com.anchor.app.medicalreport

import com.anchor.app.insights.interpretationLabelsFor
import com.anchor.app.storage.CameraLog
import com.anchor.app.storage.InMemoryAnchorStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MedicalReportContentTest {
    @Test
    fun labelsComeFromSharedVocabularyWithoutSampleThreshold() {
        // 事实线索 + 推断线索齐备才命中（与洞察页同一词表）
        assertEquals(
            listOf("出错 = 完蛋"),
            interpretationLabelsFor(CameraLog(1, "方案出错被退回", "这回全完了", false, 1)),
        )
        // 一条推断可同时命中多组句式
        val both = interpretationLabelsFor(CameraLog(2, "汇报搞砸了", "总是全完了", false, 2))
        assertTrue(both.containsAll(listOf("出错 = 完蛋", "一次 = 总是")))
        // 事实线索不命中则不打标；无推断的记录永不打标
        assertEquals(emptyList(), interpretationLabelsFor(CameraLog(3, "今天开了会", "全完了", false, 3)))
        assertEquals(emptyList(), interpretationLabelsFor(CameraLog(4, "只是写事实", "", false, 4)))
    }

    @Test
    fun buildsNewestRowsTruncatesAtTenAndCountsTotals() {
        val store = InMemoryAnchorStore()
        repeat(12) { index ->
            store.addCameraLog("第${index}次方案出错被退回", "这回全完了", (index + 1) * 1_000L)
        }
        store.addCameraLog("只写事实不推断", "", 20_000L)
        val report = buildMedicalReport(store, "ANC-1A2B3C4D-9F8E", 99_000L)

        assertEquals(10, report.cameraRows.size)
        assertEquals(12, report.cameraTotal)
        // store 返回最新在前：第一行是最后写入的一条
        assertEquals("第11次方案出错被退回", report.cameraRows.first().fact)
        assertEquals(listOf("出错 = 完蛋"), report.cameraRows.first().labels)
        assertEquals("共 12 条，仅列最近 10 条。", medicalReportTruncatedNote(report.cameraTotal))
        assertEquals("ANC-****-9F8E", report.patientIdMasked)
        // 报告窗口 = 全部历史跨度
        assertEquals(1_000L, report.periodFromMillis)
        assertEquals(20_000L, report.periodToMillis)
        assertTrue(medicalReportTruncatedNote(10).isEmpty())
    }

    @Test
    fun emotionTopRanksByCountThenRecencyAndShareIsRelativeToTop() {
        val store = InMemoryAnchorStore()
        fun card(emotion: String, at: Long) = store.addEmotionCard(emotion, "出了件事", "最难的是这里", at)
        repeat(3) { card("羞耻", it * 10L) } // 3 次，最早
        repeat(3) { card("失望", 100L + it) } // 3 次，更新 → 并列时排前
        card("空虚", 200L)
        val report = buildMedicalReport(store, "ANC-AAAAAAAA-BBBB", 300L)

        assertEquals(listOf("失望", "羞耻", "空虚"), report.emotionTop.map { it.emotion })
        assertEquals(7, report.emotionTotal)
        assertEquals(1f, report.emotionTop[0].relativeShare)
        assertEquals(1f, report.emotionTop[1].relativeShare)
        assertEquals(1f / 3f, report.emotionTop[2].relativeShare, 0.001f)
        assertEquals(102L, report.emotionTop[0].lastAtMillis)
    }

    @Test
    fun actionsNeedBothDifficultiesAndGapIsActualMinusPredicted() {
        val store = InMemoryAnchorStore()
        store.addMicroAction("回一封短信", 1_000L)
        store.startMicroAction(1, 8, 2_000L)
        store.completeMicroAction(1, 3, 3_000L)
        store.addMicroAction("只预测了没做成", 4_000L)
        store.startMicroAction(2, 6, 5_000L)
        store.addMicroAction("浇一次花", 6_000L)
        val report = buildMedicalReport(store, "ANC-AAAAAAAA-BBBB", 9_000L)

        assertEquals(1, report.actionTotal)
        assertEquals(1, report.actionRows.size)
        val row = report.actionRows.single()
        assertEquals("回一封短信", row.title)
        assertEquals(8, row.predicted)
        assertEquals(3, row.actual)
        assertEquals(-5, row.gap)
        assertEquals("预测高 5 分", medicalReportGapCopy(row.gap))
        assertEquals("实际高 2 分", medicalReportGapCopy(2))
        assertEquals("与预测一致", medicalReportGapCopy(0))
    }

    @Test
    fun emptyStoreYieldsEmptyReportAndShortIdStaysMasked() {
        val report = buildMedicalReport(InMemoryAnchorStore(), "短", 1L)
        assertEquals(0, report.coreRecordCount)
        assertEquals(emptyList(), report.cameraRows)
        assertEquals(emptyList(), report.emotionTop)
        assertEquals(emptyList(), report.actionRows)
        assertEquals(null, report.periodFromMillis)
        assertTrue(!report.hasContent)
        assertEquals("ANC-****", report.patientIdMasked)
    }

    @Test
    fun coreCountCoversAllThreeRecordTypesAndPeriodSpansThem() {
        val store = InMemoryAnchorStore()
        store.addCameraLog("事实", "推断", 1_000L)
        store.addEmotionCard("羞耻", "出了件事", "最难的是这里", 2_000L)
        store.addMicroAction("浇一次花", 3_000L)
        val report = buildMedicalReport(store, "ANC-AAAAAAAA-BBBB", 4_000L)

        assertEquals(3, report.coreRecordCount)
        assertEquals(1_000L, report.periodFromMillis)
        assertEquals(3_000L, report.periodToMillis)
        assertEquals(3L, medicalReportPeriodDays(1_000L, 3_000L + 2L * 86_400_000L))
        assertEquals("共 24 条核心记录", medicalReportCountLine(24))
    }

    @Test
    fun copyStaysSelfReportOnlyAndDisclaimsDiagnosis() {
        assertTrue(medicalReportDisclaimer.contains("不是诊断"))
        assertTrue(medicalReportDisclaimer.contains("脱敏"))
        assertTrue(medicalReportCameraDetail.contains("不构成"))
        assertTrue(medicalReportEmotionDetail.contains("没有强度评分"))
        assertTrue(!medicalReportTitle.contains("诊断"))
        assertEquals("CONFIDENTIAL MEDICAL RECORD", medicalReportConfidential)
    }
}
