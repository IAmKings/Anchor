package com.anchor.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.anchor.app.medicalreport.buildMedicalReport
import com.anchor.app.storage.InMemoryAnchorStore
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MedicalReportPdfTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @After
    fun deleteInstrumentedReportCache() {
        File(context.cacheDir, MEDICAL_REPORT_INSTRUMENTED_CACHE_FOLDER).deleteRecursively()
    }

    @Test
    fun rendersPdfWithContentIntoInstrumentedCacheOnly() {
        val production = File(context.cacheDir, MEDICAL_REPORT_CACHE_FOLDER)
        val before = production.snapshot()

        val store = InMemoryAnchorStore().apply {
            addCameraLog("发送了汇报邮件，主管 6 小时没回", "他一定对我的表现非常失望", 1_000)
            addEmotionCard("羞耻", "会上被点名", "当众承认没做完", 2_000)
            addMicroAction("回一封短信", 3_000)
            startMicroAction(1, 8, 4_000)
            completeMicroAction(1, 3, 5_000)
        }
        val report = buildMedicalReport(store, "ANC-1A2B3C4D-9F8E", 9_000)
        assertTrue(report.hasContent)

        val file = renderMedicalReportPdf(context, report, MEDICAL_REPORT_INSTRUMENTED_CACHE_FOLDER)

        assertEquals(MEDICAL_REPORT_INSTRUMENTED_CACHE_FOLDER, file.parentFile?.name)
        assertEquals(before, production.snapshot())
        assertTrue("PDF 应有实质内容", file.length() > 1_000)
        // 真身必须以 %PDF 开头；文字在压缩流里，原始字节扫描不到明文，脱敏已由 shared 单测锁定
        assertTrue(file.inputStream().use { input ->
            val head = ByteArray(5); input.read(head); String(head)
        } == "%PDF-")
    }

    @Test
    fun emptyReportStillRendersWithoutCrashing() {
        val report = buildMedicalReport(InMemoryAnchorStore(), "ANC-1A2B3C4D-9F8E", 1_000)
        assertTrue(!report.hasContent)
        val file = renderMedicalReportPdf(context, report, MEDICAL_REPORT_INSTRUMENTED_CACHE_FOLDER)
        assertTrue(file.length() > 1_000)
    }

    private fun File.snapshot(): Set<Pair<String, Long>> =
        listFiles()?.map { it.name to it.length() }?.toSet().orEmpty()
}
