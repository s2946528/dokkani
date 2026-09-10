package com.example.dokkani.domain.hardware

import java.io.ByteArrayOutputStream
import java.nio.charset.Charset

/**
 * مقاس ورق الطابعة الحرارية
 */
enum class PrinterPaperWidth(val columns: Int, val labelArabic: String, val widthMm: Int) {
    WIDTH_80MM(48, "عرض 80 مم (48 عمود)", 80),
    WIDTH_58MM(32, "عرض 58 مم (32 عمود)", 58)
}

/**
 * نموذج بيانات طباعة الفاتورة عبر البلوتوث
 */
data class ReceiptPrintData(
    val storeName: String,
    val storePhone: String,
    val taxNumber: String,
    val invoiceNumber: String,
    val invoiceDateFormatted: String,
    val cashierName: String = "كاشير 1",
    val customerName: String? = null,
    val paymentMethodArabic: String,
    val items: List<ReceiptItemData>,
    val subtotal: Double,
    val discount: Double,
    val taxRatePercent: Double,
    val taxAmount: Double,
    val total: Double,
    val paidAmount: Double,
    val remainingAmount: Double,
    val customerOldBalance: Double? = null,
    val customerNewBalance: Double? = null,
    val currencySymbol: String = "ر.س",
    val footerText: String = "شكراً لتسوقكم من دكاني!",
    val qrCodePayload: String? = null
)

data class ReceiptItemData(
    val name: String,
    val quantityFormatted: String,
    val unitPrice: Double,
    val totalPrice: Double,
    val isWeighted: Boolean = false
)

/**
 * فئة مساعدة لبناء أوامر ESC/POS القياسية للطابعات الحرارية عبر البلوتوث
 * تشمل فتح درج النقدية المباشر، تنسيق الأعمدة لمقاسات 80mm و 58mm، وقص الورق.
 */
object EscPosHelper {

    // أوامر التحكم الأساسية
    val CMD_INIT = byteArrayOf(0x1B, 0x40) // ESC @ تهيئة الطابعة
    val CMD_ALIGN_LEFT = byteArrayOf(0x1B, 0x61, 0x00) // محاذاة يسار
    val CMD_ALIGN_CENTER = byteArrayOf(0x1B, 0x61, 0x01) // محاذاة وسط
    val CMD_ALIGN_RIGHT = byteArrayOf(0x1B, 0x61, 0x02) // محاذاة يمين

    val CMD_BOLD_ON = byteArrayOf(0x1B, 0x45, 0x01) // تفعيل الخط العريض
    val CMD_BOLD_OFF = byteArrayOf(0x1B, 0x45, 0x00) // إيقاف الخط العريض

    val CMD_FONT_NORMAL = byteArrayOf(0x1D, 0x21, 0x00) // مقاس خط عادي
    val CMD_FONT_DOUBLE_HEIGHT = byteArrayOf(0x1D, 0x21, 0x01) // مضاعف الارتفاع
    val CMD_FONT_DOUBLE_WIDTH = byteArrayOf(0x1D, 0x21, 0x10) // مضاعف العرض
    val CMD_FONT_DOUBLE_BOTH = byteArrayOf(0x1D, 0x21, 0x11) // مضاعف الحجم كاملاً

    val CMD_FEED_3_LINES = byteArrayOf(0x1B, 0x64, 0x03) // تغذية 3 أسطر
    val CMD_FEED_5_LINES = byteArrayOf(0x1B, 0x64, 0x05) // تغذية 5 أسطر

    val CMD_CUT_PAPER_PARTIAL = byteArrayOf(0x1D, 0x56, 0x42, 0x00) // قص جزئي
    val CMD_CUT_PAPER_FULL = byteArrayOf(0x1D, 0x56, 0x00) // قص كامل

    /**
     * أمر فتح درج النقدية القياسي المباشر عبر منفذ الطابعة RJ11
     * الصيغة: ESC p m t1 t2
     * ESC p 0 25 250 -> نبضة مدتها 50ms على السلك 2 (القناة الأولى)
     */
    val CMD_OPEN_CASH_DRAWER_PIN2 = byteArrayOf(0x1B, 0x70, 0x00, 25.toByte(), 250.toByte())
    val CMD_OPEN_CASH_DRAWER_PIN5 = byteArrayOf(0x1B, 0x70, 0x01, 25.toByte(), 250.toByte())

    /**
     * إنشاء مصفوفة البايتات الكاملة لأمر فتح درج النقدية
     */
    fun buildOpenCashDrawerCommand(): ByteArray {
        val stream = ByteArrayOutputStream()
        stream.write(CMD_OPEN_CASH_DRAWER_PIN2)
        stream.write(CMD_OPEN_CASH_DRAWER_PIN5) // إرسال كلا القناتين لضمان عمل كافة أنواع أدراج النقدية
        return stream.toByteArray()
    }

    /**
     * بناء البايتات الكاملة لطباعة إيصال الفاتورة بدعم 80mm و 58mm
     */
    fun buildReceiptPayload(
        data: ReceiptPrintData,
        paperWidth: PrinterPaperWidth,
        openDrawerOnCash: Boolean = false,
        charset: Charset = Charset.forName("UTF-8")
    ): ByteArray {
        val stream = ByteArrayOutputStream()
        val cols = paperWidth.columns

        // 1. تهيئة الطابعة
        stream.write(CMD_INIT)

        // 2. إذا كان الدفع نقداً وطُلب فتح الدرج، إرسال نبضة الدرج فوراً
        if (openDrawerOnCash) {
            stream.write(CMD_OPEN_CASH_DRAWER_PIN2)
        }

        // 3. الترويسة واسم المحل (وسط + خط مضاعف وعريض)
        stream.write(CMD_ALIGN_CENTER)
        stream.write(CMD_BOLD_ON)
        stream.write(CMD_FONT_DOUBLE_BOTH)
        stream.write("${data.storeName}\n".toByteArray(charset))

        stream.write(CMD_FONT_NORMAL)
        stream.write(CMD_BOLD_OFF)
        if (data.storePhone.isNotBlank()) {
            stream.write("هاتف: ${data.storePhone}\n".toByteArray(charset))
        }
        if (data.taxNumber.isNotBlank()) {
            stream.write("الرقم الضريبي: ${data.taxNumber}\n".toByteArray(charset))
        }

        stream.write(createSeparator(cols, '=').toByteArray(charset))

        // 4. نوع الفاتورة والبيانات الأساسية
        stream.write(CMD_BOLD_ON)
        stream.write("فاتورة مبيعات ضريبية مبسطة\n".toByteArray(charset))
        stream.write(CMD_BOLD_OFF)

        stream.write(CMD_ALIGN_LEFT)
        stream.write(formatTwoColumns("رقم الفاتورة:", data.invoiceNumber, cols).toByteArray(charset))
        stream.write(formatTwoColumns("التاريخ والوقت:", data.invoiceDateFormatted, cols).toByteArray(charset))
        stream.write(formatTwoColumns("الكاشير:", data.cashierName, cols).toByteArray(charset))
        stream.write(formatTwoColumns("طريقة الدفع:", data.paymentMethodArabic, cols).toByteArray(charset))

        if (!data.customerName.isNullOrBlank()) {
            stream.write(formatTwoColumns("العميل (الحساب):", data.customerName, cols).toByteArray(charset))
        }

        stream.write(createSeparator(cols, '-').toByteArray(charset))

        // 5. جدول البنود
        if (paperWidth == PrinterPaperWidth.WIDTH_80MM) {
            // ترويسة 80 مم: الصنف (20) | الكمية (8) | السعر (10) | الإجمالي (10)
            val header = formatItemRow80("الصنف", "الكمية", "السعر", "الإجمالي")
            stream.write(CMD_BOLD_ON)
            stream.write(header.toByteArray(charset))
            stream.write(CMD_BOLD_OFF)
            stream.write(createSeparator(cols, '-').toByteArray(charset))

            data.items.forEach { item ->
                val line = formatItemRow80(
                    item.name.take(20),
                    item.quantityFormatted,
                    "%.2f".format(item.unitPrice),
                    "%.2f".format(item.totalPrice)
                )
                stream.write(line.toByteArray(charset))
            }
        } else {
            // ترويسة 58 مم: الصنف (14) | الكمية (6) | الإجمالي (12)
            val header = formatItemRow58("الصنف", "الكمية", "الإجمالي")
            stream.write(CMD_BOLD_ON)
            stream.write(header.toByteArray(charset))
            stream.write(CMD_BOLD_OFF)
            stream.write(createSeparator(cols, '-').toByteArray(charset))

            data.items.forEach { item ->
                val line = formatItemRow58(
                    item.name.take(14),
                    item.quantityFormatted,
                    "%.2f".format(item.totalPrice)
                )
                stream.write(line.toByteArray(charset))
            }
        }

        stream.write(createSeparator(cols, '=').toByteArray(charset))

        // 6. الإجماليات والضرائب
        stream.write(CMD_ALIGN_LEFT)
        stream.write(formatTwoColumns("المجموع الفرعي:", "%.2f %s".format(data.subtotal, data.currencySymbol), cols).toByteArray(charset))
        if (data.discount > 0.0) {
            stream.write(formatTwoColumns("الخصم:", "-%.2f %s".format(data.discount, data.currencySymbol), cols).toByteArray(charset))
        }
        val taxLabel = "ضريبة القيمة المضافة (%.0f%%):".format(data.taxRatePercent)
        stream.write(formatTwoColumns(taxLabel, "%.2f %s".format(data.taxAmount, data.currencySymbol), cols).toByteArray(charset))

        stream.write(createSeparator(cols, '-').toByteArray(charset))

        // الإجمالي الصافي بخط عريض ومزدوج
        stream.write(CMD_BOLD_ON)
        stream.write(CMD_FONT_DOUBLE_HEIGHT)
        stream.write(formatTwoColumns("الإجمالي المستحق:", "%.2f %s".format(data.total, data.currencySymbol), cols).toByteArray(charset))
        stream.write(CMD_FONT_NORMAL)
        stream.write(CMD_BOLD_OFF)

        // تفاصيل المدفوع والمتبقي
        stream.write(formatTwoColumns("المبلغ المدفوع:", "%.2f %s".format(data.paidAmount, data.currencySymbol), cols).toByteArray(charset))
        if (data.remainingAmount > 0.001) {
            stream.write(formatTwoColumns("المتبقي (آجل/شكك):", "%.2f %s".format(data.remainingAmount, data.currencySymbol), cols).toByteArray(charset))
        } else {
            val change = (data.paidAmount - data.total).coerceAtLeast(0.0)
            if (change > 0.0) {
                stream.write(formatTwoColumns("الباقي للعميل:", "%.2f %s".format(change, data.currencySymbol), cols).toByteArray(charset))
            }
        }

        // كشف حساب العميل في حال البيع الآجل
        if (data.customerOldBalance != null && data.customerNewBalance != null) {
            stream.write(createSeparator(cols, '.').toByteArray(charset))
            stream.write(formatTwoColumns("الرصيد السابق للعميل:", "%.2f %s".format(data.customerOldBalance, data.currencySymbol), cols).toByteArray(charset))
            stream.write(formatTwoColumns("الرصيد الحالي الجديد:", "%.2f %s".format(data.customerNewBalance, data.currencySymbol), cols).toByteArray(charset))
        }

        // 7. رمز الاستجابة السريعة (QR Code) أو نص الفاتورة الإلكترونية
        stream.write(createSeparator(cols, '-').toByteArray(charset))
        stream.write(CMD_ALIGN_CENTER)
        val qrText = data.qrCodePayload ?: "INV:${data.invoiceNumber}|TOTAL:${data.total}|VAT:${data.taxAmount}|STORE:${data.storeName}"
        stream.write(buildEscPosQrCode(qrText))

        // 8. الذيل وشكراً لزيارتكم
        stream.write("\n${data.footerText}\n".toByteArray(charset))
        stream.write("دكاني - نظام المحاسبة ونقاط البيع السريعة\n".toByteArray(charset))

        // 9. تغذية الورق وقص الورق
        stream.write(CMD_FEED_5_LINES)
        stream.write(CMD_CUT_PAPER_PARTIAL)

        return stream.toByteArray()
    }

    private fun createSeparator(cols: Int, char: Char): String {
        return char.toString().repeat(cols) + "\n"
    }

    private fun formatTwoColumns(left: String, right: String, totalCols: Int): String {
        val spacesCount = (totalCols - left.length - right.length).coerceAtLeast(1)
        return left + " ".repeat(spacesCount) + right + "\n"
    }

    private fun formatItemRow80(name: String, qty: String, price: String, total: String): String {
        val nameCol = name.padEnd(20)
        val qtyCol = qty.padStart(8)
        val priceCol = price.padStart(10)
        val totalCol = total.padStart(10)
        return "$nameCol$qtyCol$priceCol$totalCol\n"
    }

    private fun formatItemRow58(name: String, qty: String, total: String): String {
        val nameCol = name.padEnd(14)
        val qtyCol = qty.padStart(6)
        val totalCol = total.padStart(12)
        return "$nameCol$qtyCol$totalCol\n"
    }

    /**
     * أوامر ESC/POS المباشرة لطباعة QR Code عبر الطابعة الحرارية
     * الأوامر القياسية لطابعات EPSON / Xprinter / Rongta:
     * GS ( k pL pH cn fn n1 n2
     */
    fun buildEscPosQrCode(text: String): ByteArray {
        val stream = ByteArrayOutputStream()
        val textBytes = text.toByteArray(Charset.forName("UTF-8"))
        val length = textBytes.size + 3
        val pL = (length % 256).toByte()
        val pH = (length / 256).toByte()

        // 1. تحديد حجم الموديل: GS ( k 4 0 49 65 50 0
        stream.write(byteArrayOf(0x1D, 0x28, 0x6B, 0x04, 0x00, 0x31, 0x41, 0x32, 0x00))
        // 2. حجم المربع (Module size = 6): GS ( k 3 0 49 67 6
        stream.write(byteArrayOf(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x43, 0x06))
        // 3. تصحيح الخطأ (Error correction level M): GS ( k 3 0 49 69 49
        stream.write(byteArrayOf(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x45, 0x31))
        // 4. تخزين البيانات في الذاكرة
        stream.write(byteArrayOf(0x1D, 0x28, 0x6B, pL, pH, 0x31, 0x50, 0x30))
        stream.write(textBytes)
        // 5. طباعة الرمز المخزن: GS ( k 3 0 49 81 48
        stream.write(byteArrayOf(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x51, 0x30))

        return stream.toByteArray()
    }

    /**
     * تحويل مصفوفة البايتات إلى تمثيل Hex مقروء لأغراض التتبع والمعاينة
     */
    fun bytesToHex(bytes: ByteArray, limit: Int = 120): String {
        val hex = bytes.take(limit).joinToString(" ") { "%02X".format(it) }
        return if (bytes.size > limit) "$hex ... (${bytes.size} بايت)" else hex
    }
}
