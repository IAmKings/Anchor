package com.anchor.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import com.anchor.app.medicalreport.MedicalReport
import com.anchor.app.medicalreport.medicalReportActionDetail
import com.anchor.app.medicalreport.medicalReportActionTitle
import com.anchor.app.medicalreport.medicalReportCameraDetail
import com.anchor.app.medicalreport.medicalReportCameraTitle
import com.anchor.app.medicalreport.medicalReportColumnAction
import com.anchor.app.medicalreport.medicalReportColumnActual
import com.anchor.app.medicalreport.medicalReportColumnFact
import com.anchor.app.medicalreport.medicalReportColumnGap
import com.anchor.app.medicalreport.medicalReportColumnInference
import com.anchor.app.medicalreport.medicalReportColumnPredicted
import com.anchor.app.medicalreport.medicalReportColumnTime
import com.anchor.app.medicalreport.medicalReportConfidential
import com.anchor.app.medicalreport.medicalReportCountLine
import com.anchor.app.medicalreport.medicalReportDisclaimer
import com.anchor.app.medicalreport.medicalReportEmotionCountLine
import com.anchor.app.medicalreport.medicalReportEmotionDetail
import com.anchor.app.medicalreport.medicalReportEmotionFrequency
import com.anchor.app.medicalreport.medicalReportEmotionLastAt
import com.anchor.app.medicalreport.medicalReportEmotionTitle
import com.anchor.app.medicalreport.medicalReportEmptySection
import com.anchor.app.medicalreport.medicalReportGapCopy
import com.anchor.app.medicalreport.medicalReportGeneratedBy
import com.anchor.app.medicalreport.medicalReportMetaCount
import com.anchor.app.medicalreport.medicalReportMetaExportedAt
import com.anchor.app.medicalreport.medicalReportMetaPatient
import com.anchor.app.medicalreport.medicalReportMetaPeriod
import com.anchor.app.medicalreport.medicalReportPeriodDays
import com.anchor.app.medicalreport.medicalReportSubtitle
import com.anchor.app.medicalreport.medicalReportTitle
import com.anchor.app.medicalreport.medicalReportTruncatedNote
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// 设计稿 design/09-settings-export 的打印色板；PDF 永远是白纸，不随应用主题。
private val ReportPrimary = 0xFF466552.toInt()
private val ReportSage = 0xFF7A9B86.toInt()
private val ReportText = 0xFF33302B.toInt()
private val ReportMuted = 0xFF8A837A.toInt()
private val ReportBorder = 0xFFE3DDD3.toInt()
private val ReportLabelRed = 0xFFBA1A1A.toInt()
private val ReportLabelRedBg = 0x26BA1A1A.toInt()
private val ReportHeaderBg = 0xFFF6F3ED.toInt()
private val ReportBarTrack = 0xFFE5E2DC.toInt()
private val ReportWarn = 0xFF8A4D34.toInt()

private const val PAGE_WIDTH = 595
private const val PAGE_HEIGHT = 842
private const val MARGIN_H = 48f
private const val MARGIN_TOP = 44f
private const val FOOTER_ZONE = 66f

private val dateTimeFormat = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")
private val shortDateFormat = DateTimeFormatter.ofPattern("MM.dd HH:mm")
private val fileStampFormat = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")

const val MEDICAL_REPORT_CACHE_FOLDER = "medical-reports"
const val MEDICAL_REPORT_INSTRUMENTED_CACHE_FOLDER = "medical-reports-instrumented"

private fun localTime(millis: Long): LocalDateTime =
    LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())

private fun textPaint(color: Int, size: Float, bold: Boolean = false, mono: Boolean = false): TextPaint =
    TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        this.textSize = size
        typeface = when {
            mono -> Typeface.MONOSPACE
            bold -> Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            else -> Typeface.DEFAULT
        }
    }

private fun layoutOf(text: String, paint: TextPaint, width: Int, center: Boolean = false): StaticLayout =
    StaticLayout.Builder.obtain(text, 0, text.length, paint, width.coerceAtLeast(1))
        .setAlignment(if (center) Layout.Alignment.ALIGN_CENTER else Layout.Alignment.ALIGN_NORMAL)
        .setIncludePad(false)
        .build()

/** 在 cacheDir 生成医生审阅版 PDF（明文、脱敏），返回文件供 SAF 保存或分享。 */
fun renderMedicalReportPdf(
    context: Context,
    report: MedicalReport,
    cacheFolder: String = MEDICAL_REPORT_CACHE_FOLDER,
): File {
    val directory = File(context.cacheDir, cacheFolder).apply { mkdirs() }
    directory.listFiles()?.forEach(File::delete)
    val document = MedicalReportDocument(report).render()
    val file = File(directory, "anchor-report-${LocalDateTime.now().format(fileStampFormat)}.pdf")
    FileOutputStream(file).use(document::writeTo)
    document.close()
    return file
}

private class MedicalReportDocument(private val report: MedicalReport) {
    private val doc = PdfDocument()
    private val contentWidth = (PAGE_WIDTH - 2 * MARGIN_H).toInt()
    private var pageNumber = 0
    private lateinit var page: PdfDocument.Page
    private lateinit var canvas: Canvas
    private var y = 0f

    private val titlePaint = textPaint(ReportPrimary, 20f, bold = true)
    private val subtitlePaint = textPaint(ReportMuted, 11f)
    private val brandPaint = textPaint(ReportSage, 10f, bold = true)
    private val sectionPaint = textPaint(ReportText, 13.5f, bold = true)
    private val detailPaint = textPaint(ReportMuted, 8.5f)
    private val headerLabelPaint = textPaint(ReportMuted, 8.5f)
    private val metaLabelPaint = textPaint(ReportMuted, 8f)
    private val metaValuePaint = textPaint(ReportText, 9.5f, mono = true)
    private val bodyPaint = textPaint(ReportText, 9.5f)
    private val bodyMutedPaint = textPaint(ReportMuted, 9f)
    private val monoPaint = textPaint(ReportMuted, 9f, mono = true)
    private val monoDarkPaint = textPaint(ReportText, 9.5f, mono = true)
    private val chipPaint = textPaint(ReportLabelRed, 8f)
    private val numberPaint = textPaint(ReportText, 10f, mono = true)
    private val gapGoodPaint = textPaint(ReportPrimary, 9f, bold = true)
    private val gapWarnPaint = textPaint(ReportWarn, 9f, bold = true)
    private val emptyPaint = textPaint(ReportMuted, 9.5f)
    private val footerMarkPaint = textPaint(ReportPrimary, 8f, bold = true)
    private val footerPaint = textPaint(ReportMuted, 7.5f)
    private val footerBrandPaint = textPaint(ReportPrimary, 8.5f, bold = true)
    private val pagePaint = textPaint(ReportMuted, 8f, mono = true)

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 0.9f }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun render(): PdfDocument {
        startPage()
        drawHeader()
        section(medicalReportCameraTitle, medicalReportCameraDetail, ::drawCameraTable)
        section(medicalReportEmotionTitle, medicalReportEmotionDetail, ::drawEmotionCards)
        section(medicalReportActionTitle, medicalReportActionDetail, ::drawActionTable)
        finishPage()
        return doc
    }

    private fun startPage() {
        pageNumber += 1
        page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        canvas = page.canvas
        y = MARGIN_TOP
    }

    private fun finishPage() {
        drawFooter()
        doc.finishPage(page)
    }

    private fun ensureSpace(needed: Float): Boolean {
        if (y + needed > PAGE_HEIGHT - FOOTER_ZONE) {
            finishPage()
            startPage()
            return true
        }
        return false
    }

    private fun drawText(text: String, paint: TextPaint, x: Float, width: Int, center: Boolean = false): Float {
        val layout = layoutOf(text, paint, width, center)
        canvas.save()
        canvas.translate(x, y)
        layout.draw(canvas)
        canvas.restore()
        y += layout.height
        return layout.height.toFloat()
    }

    private fun moveDown(space: Float) {
        y += space
    }

    private fun horizontalLine(color: Int, x0: Float = MARGIN_H, x1: Float = PAGE_WIDTH - MARGIN_H, stroke: Float = 0.9f) {
        linePaint.color = color
        linePaint.strokeWidth = stroke
        canvas.drawLine(x0, y, x1, y, linePaint)
    }

    private fun drawHeader() {
        val titleLayout = layoutOf(medicalReportTitle, titlePaint, contentWidth)
        canvas.save()
        canvas.translate(MARGIN_H, y)
        titleLayout.draw(canvas)
        canvas.restore()
        val brandLayout = layoutOf("Anchor · 锚点", brandPaint, 140)
        canvas.save()
        canvas.translate(PAGE_WIDTH - MARGIN_H - brandLayout.width, y + 6f)
        brandLayout.draw(canvas)
        canvas.restore()
        y += titleLayout.height + 4f
        drawText("（$medicalReportSubtitle）", subtitlePaint, MARGIN_H, contentWidth)
        moveDown(16f)

        // 元数据 2×2：标签一行 + 值一行，固定基线步进
        val half = contentWidth / 2
        fun metaCell(x: Float, baseline: Float, label: String, value: String) {
            canvas.drawText(label, x, baseline, metaLabelPaint)
            canvas.drawText(value, x, baseline + 13f, metaValuePaint)
        }
        val row1 = y + 9f
        metaCell(MARGIN_H, row1, medicalReportMetaPatient, report.patientIdMasked)
        metaCell(MARGIN_H + half, row1, medicalReportMetaPeriod, periodLine())
        val row2 = row1 + 30f
        metaCell(MARGIN_H, row2, medicalReportMetaCount, medicalReportCountLine(report.coreRecordCount))
        metaCell(MARGIN_H + half, row2, medicalReportMetaExportedAt, localTime(report.generatedAtMillis).format(dateTimeFormat))
        // 值行基线在 row2+13，字形底部约到 row2+16；线再往下让出约 10pt 呼吸空间
        y = row2 + 26f

        horizontalLine(ReportPrimary, stroke = 2f)
        moveDown(24f)
    }

    private fun periodLine(): String {
        val from = report.periodFromMillis ?: return "—"
        val to = report.periodToMillis ?: return "—"
        val days = medicalReportPeriodDays(from, to)
        return "${localTime(from).format(dateTimeFormat)} - ${localTime(to).format(dateTimeFormat)}（${days}天）"
    }

    private fun section(title: String, detail: String, drawContent: () -> Unit) {
        ensureSpace(90f)
        // 区块标题前的小色块
        fillPaint.color = ReportPrimary
        val block = sectionPaint.textSize - 4f
        canvas.drawRect(MARGIN_H, y + 3f, MARGIN_H + block, y + 3f + block, fillPaint)
        drawText(title, sectionPaint, MARGIN_H + block + 8f, (contentWidth - block - 8f).toInt())
        drawText(detail, detailPaint, MARGIN_H, contentWidth)
        moveDown(12f)
        drawContent()
        moveDown(26f)
    }

    // ---------- 双栏日志表 ----------

    private val cameraTimeWidth = 78f
    private val cameraColumnWidth = (PAGE_WIDTH - 2 * MARGIN_H - cameraTimeWidth) / 2f

    // 表头横带 + 底线：两张表共用的骨架；返回是否翻到了新页，调用方据此补绘表头
    private fun headerBand(drawLabels: (base: Float) -> Unit): Boolean {
        val rowHeight = 24f
        val brokePage = ensureSpace(rowHeight)
        fillPaint.color = ReportHeaderBg
        canvas.drawRect(MARGIN_H, y, PAGE_WIDTH - MARGIN_H, y + rowHeight, fillPaint)
        drawLabels(y + 15.5f)
        y += rowHeight
        horizontalLine(ReportBorder)
        return brokePage
    }

    private fun cameraHeaderRow() {
        headerBand { base ->
            canvas.drawText(medicalReportColumnTime, MARGIN_H + 8f, base, headerLabelPaint)
            val columnX = MARGIN_H + 8f + cameraTimeWidth
            canvas.drawText(medicalReportColumnFact, columnX + 6f, base, headerLabelPaint)
            canvas.drawText(medicalReportColumnInference, columnX + cameraColumnWidth + 6f, base, headerLabelPaint)
        }
    }

    // 表尾「共 N 条，仅列最近 10 条」注记：口径写在正文里（含推断 / 已完成），读者才知道为什么比总数少
    private fun truncatedTail(total: Int, qualifier: String) {
        val note = medicalReportTruncatedNote(total, qualifier)
        if (note.isNotEmpty()) {
            moveDown(6f)
            drawText(note, detailPaint, MARGIN_H, contentWidth)
        }
    }

    private fun drawCameraTable() {
        if (report.cameraRows.isEmpty()) {
            drawText(medicalReportEmptySection, emptyPaint, MARGIN_H, contentWidth)
            return
        }
        cameraHeaderRow()
        val factX = MARGIN_H + cameraTimeWidth + 6f
        val inferenceX = factX + cameraColumnWidth + 6f
        val cellWidth = (cameraColumnWidth - 12f).toInt()
        report.cameraRows.forEachIndexed { index, row ->
            val factLayout = layoutOf(row.fact, bodyPaint, cellWidth)
            val inferenceLayout = layoutOf(row.inference, bodyPaint, cellWidth)
            val chipHeight = 15f
            val labelsHeight = row.labels.size * (chipHeight + 4f)
            val rowHeight = maxOf(factLayout.height.toFloat(), inferenceLayout.height + labelsHeight) + 14f
            // 翻页后新页先重绘表头；只有同一页内的相邻行才画分隔线
            if (ensureSpace(rowHeight + 1f)) cameraHeaderRow() else if (index > 0) horizontalLine(ReportBorder)

            val rowTop = y
            canvas.save()
            canvas.translate(MARGIN_H + 8f, rowTop + 7f)
            layoutOf(localTime(row.createdAtMillis).format(shortDateFormat), monoPaint, cameraTimeWidth.toInt()).draw(canvas)
            canvas.restore()
            canvas.save()
            canvas.translate(factX, rowTop + 7f)
            factLayout.draw(canvas)
            canvas.restore()
            canvas.save()
            canvas.translate(inferenceX, rowTop + 7f)
            inferenceLayout.draw(canvas)
            canvas.restore()
            // 命中的解释句式：红底圆角标签，逐个叠放
            var chipY = rowTop + 7f + inferenceLayout.height + 4f
            row.labels.forEach { label ->
                val shown = TextUtils.ellipsize(label, chipPaint, cameraColumnWidth - 16f, TextUtils.TruncateAt.END).toString()
                val chipWidth = chipPaint.measureText(shown) + 16f
                fillPaint.color = ReportLabelRedBg
                canvas.drawRoundRect(RectF(inferenceX, chipY, inferenceX + chipWidth, chipY + chipHeight), 4f, 4f, fillPaint)
                canvas.drawText(shown, inferenceX + 8f, chipY + 10.5f, chipPaint)
                chipY += chipHeight + 4f
            }
            y = rowTop + rowHeight
        }
        horizontalLine(ReportBorder)
        truncatedTail(report.cameraTotal, "含推断")
    }

    // ---------- 情绪 Top3 ----------

    private fun drawEmotionCards() {
        if (report.emotionTop.isEmpty()) {
            drawText(medicalReportEmptySection, emptyPaint, MARGIN_H, contentWidth)
            return
        }
        val gap = 12f
        val cardWidth = (PAGE_WIDTH - 2 * MARGIN_H - gap * (report.emotionTop.size - 1)) / report.emotionTop.size
        val cardHeight = 92f
        ensureSpace(cardHeight + 8f)
        var x = MARGIN_H
        report.emotionTop.forEach { stat ->
            fillPaint.style = Paint.Style.STROKE
            fillPaint.strokeWidth = 0.9f
            fillPaint.color = ReportBorder
            canvas.drawRoundRect(RectF(x, y, x + cardWidth, y + cardHeight), 8f, 8f, fillPaint)
            fillPaint.style = Paint.Style.FILL

            var cursor = y + 16f
            val innerX = x + 10f
            val innerWidth = (cardWidth - 20f).toInt()
            canvas.save()
            canvas.translate(innerX, cursor)
            layoutOf(stat.emotion, textPaint(ReportText, 11.5f, bold = true), innerWidth).draw(canvas)
            canvas.restore()
            cursor += 22f
            canvas.drawText(medicalReportEmotionFrequency, innerX, cursor, metaLabelPaint)
            canvas.drawText(" ${stat.count} 次", innerX + metaLabelPaint.measureText(medicalReportEmotionFrequency), cursor, numberPaint)
            cursor += 14f
            // 占比条：相对最常见情绪的出现次数
            fillPaint.color = ReportBarTrack
            canvas.drawRoundRect(RectF(innerX, cursor, x + cardWidth - 10f, cursor + 5f), 2.5f, 2.5f, fillPaint)
            val fill = (cardWidth - 20f) * stat.relativeShare
            if (fill > 1f) {
                fillPaint.color = ReportSage
                canvas.drawRoundRect(RectF(innerX, cursor, innerX + fill, cursor + 5f), 2.5f, 2.5f, fillPaint)
            }
            cursor += 16f
            canvas.drawText(medicalReportEmotionLastAt, innerX, cursor, metaLabelPaint)
            canvas.drawText(" " + localTime(stat.lastAtMillis).format(shortDateFormat), innerX + metaLabelPaint.measureText(medicalReportEmotionLastAt), cursor, monoPaint)
            x += cardWidth + gap
        }
        moveDown(cardHeight)
        // Top 3 之外的上下文：区块只展示前三种，医生需要知道总量
        drawText(medicalReportEmotionCountLine(report.emotionTotal), detailPaint, MARGIN_H, contentWidth)
    }

    // ---------- 微行动表 ----------

    private val actionGapWidth = 92f
    private val actionColumnWidth = (PAGE_WIDTH - 2 * MARGIN_H - actionGapWidth * 3)

    private fun actionHeaderRow() {
        headerBand { base ->
            canvas.drawText(medicalReportColumnAction, MARGIN_H + 8f, base, headerLabelPaint)
            fun centerAt(text: String, columnX: Float) {
                val w = headerLabelPaint.measureText(text)
                canvas.drawText(text, columnX + (actionGapWidth - w) / 2f, base, headerLabelPaint)
            }
            centerAt(medicalReportColumnPredicted, MARGIN_H + actionColumnWidth)
            centerAt(medicalReportColumnActual, MARGIN_H + actionColumnWidth + actionGapWidth)
            centerAt(medicalReportColumnGap, MARGIN_H + actionColumnWidth + actionGapWidth * 2)
        }
    }

    private fun drawActionTable() {
        if (report.actionRows.isEmpty()) {
            drawText(medicalReportEmptySection, emptyPaint, MARGIN_H, contentWidth)
            return
        }
        actionHeaderRow()
        val cellWidth = (actionColumnWidth - 12f).toInt()
        report.actionRows.forEachIndexed { index, row ->
            val titleLayout = layoutOf(row.title, bodyPaint, cellWidth)
            val rowHeight = maxOf(titleLayout.height.toFloat(), 16f) + 16f
            // 与双栏表同规则：翻页续绘表头，同页相邻行才画分隔线
            if (ensureSpace(rowHeight + 1f)) actionHeaderRow() else if (index > 0) horizontalLine(ReportBorder)
            val rowTop = y
            val base = rowTop + 11f
            canvas.save()
            canvas.translate(MARGIN_H + 8f, rowTop + 8f)
            titleLayout.draw(canvas)
            canvas.restore()
            fun centerText(text: String, columnX: Float, paint: TextPaint) {
                val w = paint.measureText(text)
                canvas.drawText(text, columnX + (actionGapWidth - w) / 2f, base, paint)
            }
            centerText(row.predicted.toString(), MARGIN_H + actionColumnWidth, numberPaint)
            centerText(row.actual.toString(), MARGIN_H + actionColumnWidth + actionGapWidth, numberPaint)
            centerText(medicalReportGapCopy(row.gap), MARGIN_H + actionColumnWidth + actionGapWidth * 2, if (row.gap < 0) gapGoodPaint else gapWarnPaint)
            y = rowTop + rowHeight
        }
        horizontalLine(ReportBorder)
        truncatedTail(report.actionTotal, "已完成")
    }

    // ---------- 页脚（每页） ----------

    private fun drawFooter() {
        val top = PAGE_HEIGHT - FOOTER_ZONE + 8f
        linePaint.color = ReportBorder
        linePaint.strokeWidth = 0.9f
        canvas.drawLine(MARGIN_H, top, PAGE_WIDTH - MARGIN_H, top, linePaint)

        val footerLayout = layoutOf(medicalReportDisclaimer, footerPaint, contentWidth, center = true)
        canvas.save()
        canvas.translate(MARGIN_H, top + 10f)
        footerLayout.draw(canvas)
        canvas.restore()

        val markWidth = footerMarkPaint.measureText(medicalReportConfidential)
        canvas.drawText(medicalReportConfidential, (PAGE_WIDTH - markWidth) / 2f, top + 28f + footerLayout.height, footerMarkPaint)
        val brandWidth = footerBrandPaint.measureText(medicalReportGeneratedBy)
        canvas.drawText(medicalReportGeneratedBy, (PAGE_WIDTH - brandWidth) / 2f, top + 42f + footerLayout.height, footerBrandPaint)
        val pageLabel = "第 $pageNumber 页"
        canvas.drawText(pageLabel, PAGE_WIDTH - MARGIN_H - pagePaint.measureText(pageLabel), PAGE_HEIGHT - 12f, pagePaint)
    }
}
