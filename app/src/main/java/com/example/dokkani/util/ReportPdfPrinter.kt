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
