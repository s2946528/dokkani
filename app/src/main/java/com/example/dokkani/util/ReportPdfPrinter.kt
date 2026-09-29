package com.example.dokkani.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * محرك الطباعة وتصدير التقارير الماليّة إلى ملفات PDF احترافية لبرنامج "دكاني"
 */
object ReportPdfPrinter {

    data class TableRowData(
        val col1: String,
        val col2: String,
        val col3: String,
        val col4: String = "",
        val isHeader: Boolean = false,
        val isTotal: Boolean = false
    )

    enum class PrintAction {
        PRINT, SHARE
    }

    fun generateAndPrintReport(
        context: Context,
        storeName: String,
        reportTitle: String,
        subTitle: String,
        kpiSummary: List<Pair<String, String>>,
        tableRows: List<TableRowData>,
        action: PrintAction = PrintAction.PRINT
    ) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // قياس A4 القياسي 595x842 pt
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint()
        val titlePaint = Paint()

        // إعداد الألوان والخطوط الأساسية
        paint.textSize = 10f
        paint.color = Color.BLACK
        paint.isAntiAlias = true

        titlePaint.textSize = 16f
        titlePaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        titlePaint.color = Color.rgb(15, 81, 50) // الأخضر المالي الاحترافي
        titlePaint.isAntiAlias = true

        var y = 40f

        // 1. ترويسة التقرير: اسم المنشأة والعنوان
        canvas.drawText(storeName, 40f, y, titlePaint)
        y += 22f

        titlePaint.textSize = 13f
        titlePaint.color = Color.rgb(30, 58, 138) // الأزرق الكحلي
        canvas.drawText(reportTitle, 40f, y, titlePaint)
        y += 16f

        paint.textSize = 9f
        paint.color = Color.rgb(100, 116, 139)
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        canvas.drawText("تاريخ الإصدار: $dateStr | $subTitle", 40f, y, paint)
        y += 16f

        // خط فاصل علوي
        paint.color = Color.rgb(203, 213, 225)
        canvas.drawLine(40f, y, 555f, y, paint)
        y += 16f

        // 2. صندوق الإحصائيات والمؤشرات السريعة (KPI Summary)
        if (kpiSummary.isNotEmpty()) {
            paint.color = Color.rgb(241, 245, 249)
            val boxHeight = (kpiSummary.size * 18f) + 12f
            canvas.drawRect(40f, y, 555f, y + boxHeight, paint)

            paint.color = Color.rgb(15, 23, 42)
            paint.textSize = 10f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

            var kpiY = y + 18f
            for ((key, value) in kpiSummary) {
                canvas.drawText("$key: $value", 52f, kpiY, paint)
                kpiY += 18f
            }
            y = kpiY + 14f
        }

        // 3. جدول البيانات التفصيلي
        for (row in tableRows) {
            if (y > 790f) {
                // حد الصفحة الواحدة للتصميم المبسط
                break
            }

            if (row.isHeader) {
                paint.color = Color.rgb(226, 232, 240)
                canvas.drawRect(40f, y - 12f, 555f, y + 6f, paint)
                paint.color = Color.rgb(15, 23, 42)
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = 10f
            } else if (row.isTotal) {
                paint.color = Color.rgb(220, 252, 231)
                canvas.drawRect(40f, y - 12f, 555f, y + 6f, paint)
                paint.color = Color.rgb(21, 128, 61)
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = 10f
            } else {
                paint.color = Color.rgb(51, 65, 85)
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.textSize = 9f
            }

            canvas.drawText(row.col1, 45f, y, paint)
            canvas.drawText(row.col2, 220f, y, paint)
            canvas.drawText(row.col3, 360f, y, paint)
            if (row.col4.isNotBlank()) {
                canvas.drawText(row.col4, 470f, y, paint)
            }

            y += 20f
        }

        // 4. ذيل الصفحة (Footer)
        paint.color = Color.rgb(148, 163, 184)
        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        canvas.drawText("تم تصدير هذا التقرير آلياً عبر نظام دكاني المحاسبي لنقاط البيع Dokkani POS", 40f, 825f, paint)

        pdfDocument.finishPage(page)

        // حفظ ملف الـ PDF في ذاكرة التخزين المؤقتة للجهاز
        val fileName = "Report_${reportTitle.replace(" ", "_")}_${System.currentTimeMillis()}.pdf"
        val pdfFile = File(context.cacheDir, fileName)
        val outputStream = FileOutputStream(pdfFile)
        pdfDocument.writeTo(outputStream)
        pdfDocument.close()
        outputStream.close()

        if (action == PrintAction.PRINT) {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            val printAdapter = PdfPrintAdapter(pdfFile.absolutePath)
            printManager?.print(reportTitle, printAdapter, PrintAttributes.Builder().build())
        } else {
            // مشاركة الملف عبر تطبيقات التواصل أو الحفظ
            val fileUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "مشاركة تقرير: $reportTitle"))
        }
    }

    /**
     * طباعة وتصدير تقرير الحركة التفصيلية للصنف مع عملة الصنف بشكل احترافي
     */
    fun generateAndPrintItemLedgerReport(
        context: Context,
        storeName: String,
        report: com.example.dokkani.domain.reports.ProductItemLedgerReport,
        action: PrintAction = PrintAction.PRINT
    ) {
        val pdfDocument = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        var currentPageNumber = 1

        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas: Canvas = page.canvas

        val paint = Paint().apply { isAntiAlias = true; textSize = 9f; color = Color.BLACK }
        val titlePaint = Paint().apply {
            isAntiAlias = true
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(15, 81, 50)
        }

        var y = 35f

        fun drawHeaderAndItemInfo() {
            titlePaint.textSize = 15f
            titlePaint.color = Color.rgb(15, 81, 50)
            canvas.drawText(storeName, 35f, y, titlePaint)
            y += 20f

            titlePaint.textSize = 13f
            titlePaint.color = Color.rgb(30, 58, 138)
            canvas.drawText("تقرير الحركة التفصيلية للصنف", 35f, y, titlePaint)
            y += 15f

            paint.textSize = 8.5f
            paint.color = Color.rgb(100, 116, 139)
            canvas.drawText(
                "مركز التكلفة: ${report.costCenterName} | تاريخ الإصدار: ${report.issueDateFormatted} | عملة الصنف والتقرير: ${report.currencySymbol}",
                35f,
                y,
                paint
            )
            y += 12f

            paint.color = Color.rgb(203, 213, 225)
            canvas.drawLine(35f, y, 560f, y, paint)
            y += 12f

            // Item Basic Info Box
            paint.color = Color.rgb(241, 245, 249)
            canvas.drawRect(35f, y, 560f, y + 42f, paint)

            paint.color = Color.rgb(15, 23, 42)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 9.5f
            canvas.drawText("اسم الصنف: ${report.productName} (${report.category})", 42f, y + 16f, paint)
            canvas.drawText("كود الصنف: #${report.productCode} | الوحدة: ${report.unitName}", 42f, y + 32f, paint)

            val costStr = "%.2f %s".format(report.currentUnitCost, report.currencySymbol)
            canvas.drawText("عملة الصنف: ${report.currencySymbol} | تكلفة الوحدة: $costStr", 300f, y + 16f, paint)
            val balStr = "%.1f %s".format(report.closingBalance, report.unitName)
            canvas.drawText("الرصيد المتبقي: $balStr", 300f, y + 32f, paint)

            y += 52f
        }

        fun drawTableHeader() {
            paint.color = Color.rgb(30, 41, 59)
            canvas.drawRect(35f, y, 560f, y + 18f, paint)

            paint.color = Color.WHITE
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 8.5f

            canvas.drawText("التاريخ والوقت", 40f, y + 12f, paint)
            canvas.drawText("نوع الحركة", 135f, y + 12f, paint)
            canvas.drawText("رقم السند/الفاتورة", 235f, y + 12f, paint)
            canvas.drawText("الوارد", 320f, y + 12f, paint)
            canvas.drawText("الصادر", 365f, y + 12f, paint)
            canvas.drawText("الرصيد", 410f, y + 12f, paint)
            canvas.drawText("التكلفة (${report.currencySymbol})", 455f, y + 12f, paint)
            canvas.drawText("القيمة (${report.currencySymbol})", 510f, y + 12f, paint)

            y += 22f
        }

        drawHeaderAndItemInfo()
        drawTableHeader()

        for (entry in report.entries) {
            if (y > 760f) {
                pdfDocument.finishPage(page)
                currentPageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                y = 35f
                drawTableHeader()
            }

            paint.color = Color.rgb(248, 250, 252)
            canvas.drawRect(35f, y - 10f, 560f, y + 6f, paint)

            paint.color = Color.rgb(15, 23, 42)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 8f

            canvas.drawText(entry.dateFormatted, 40f, y, paint)
            canvas.drawText(entry.movementTypeLabel, 135f, y, paint)
            canvas.drawText(entry.referenceNumber, 235f, y, paint)

            if (entry.quantityIn > 0) {
                paint.color = Color.rgb(21, 128, 61)
                canvas.drawText("+%.1f".format(entry.quantityIn), 320f, y, paint)
            } else {
                paint.color = Color.rgb(15, 23, 42)
                canvas.drawText("-", 320f, y, paint)
            }

            if (entry.quantityOut > 0) {
                paint.color = Color.rgb(220, 38, 38)
                canvas.drawText("-%.1f".format(entry.quantityOut), 365f, y, paint)
            } else {
                paint.color = Color.rgb(15, 23, 42)
                canvas.drawText("-", 365f, y, paint)
            }

            paint.color = Color.rgb(15, 23, 42)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("%.1f".format(entry.runningBalance), 410f, y, paint)

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("%.2f".format(entry.unitCost), 455f, y, paint)
            canvas.drawText("%.2f %s".format(entry.totalValue, entry.currencySymbol), 510f, y, paint)

            y += 18f
        }

        // Summary Row
        if (y > 740f) {
            pdfDocument.finishPage(page)
            currentPageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            y = 35f
        }

        paint.color = Color.rgb(220, 252, 231)
        canvas.drawRect(35f, y - 10f, 560f, y + 8f, paint)

        paint.color = Color.rgb(21, 128, 61)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 8.5f

        canvas.drawText("الإجمالي والختام", 40f, y, paint)
        canvas.drawText("إجمالي الوارد: +%.1f".format(report.totalQtyIn), 220f, y, paint)
        canvas.drawText("إجمالي الصادر: -%.1f".format(report.totalQtyOut), 320f, y, paint)
        canvas.drawText("الرصيد: %.1f".format(report.closingBalance), 410f, y, paint)
        canvas.drawText("القيمة: %.2f %s".format(report.totalValue, report.currencySymbol), 485f, y, paint)

        y += 30f

        // Footer & Signature
        paint.color = Color.rgb(100, 116, 139)
        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        canvas.drawText("معلومات التوثيق: تم إصدار هذا التقرير آلياً عبر نظام دكاني المحاسبي لنقاط البيع | عملة الصنف: ${report.currencySymbol}", 35f, y, paint)
        y += 20f

        paint.color = Color.rgb(15, 23, 42)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("توقيع المسؤول / المعتمد: .....................................................", 35f, y, paint)

        pdfDocument.finishPage(page)

        // Save PDF file
        val fileName = "ItemLedger_${report.productCode}_${System.currentTimeMillis()}.pdf"
        val pdfFile = File(context.cacheDir, fileName)
        val outputStream = FileOutputStream(pdfFile)
        pdfDocument.writeTo(outputStream)
        pdfDocument.close()
        outputStream.close()

        if (action == PrintAction.PRINT) {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            val printAdapter = PdfPrintAdapter(pdfFile.absolutePath)
            printManager?.print("الحركة التفصيلية للصنف - ${report.productName}", printAdapter, PrintAttributes.Builder().build())
        } else {
            val fileUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "مشاركة الحركة التفصيلية للصنف: ${report.productName}"))
        }
    }
}

/**
 * محول طباعة ملف الـ PDF المحفوظ لنظام الطباعة في أندرويد PrintManager
 */
class PdfPrintAdapter(private val pdfFilePath: String) : PrintDocumentAdapter() {

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes?,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback?,
        extras: Bundle?
    ) {
        if (cancellationSignal?.isCanceled == true) {
            callback?.onLayoutCancelled()
            return
        }

        val info = PrintDocumentInfo.Builder(File(pdfFilePath).name)
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(1)
            .build()

        callback?.onLayoutFinished(info, true)
    }

    override fun onWrite(
        pages: Array<out PageRange>?,
        destination: ParcelFileDescriptor?,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback?
    ) {
        try {
            val input = FileInputStream(pdfFilePath)
            val output = FileOutputStream(destination?.fileDescriptor)

            input.copyTo(output)
            callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))

            input.close()
            output.close()
        } catch (e: Exception) {
            callback?.onWriteFailed(e.message)
        }
    }
}
