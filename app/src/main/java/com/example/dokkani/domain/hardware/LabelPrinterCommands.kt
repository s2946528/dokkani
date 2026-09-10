package com.example.dokkani.domain.hardware

import java.io.ByteArrayOutputStream
import java.nio.charset.Charset

/**
 * مولد أوامر لغات طابعات ملصقات الباركود:
 * 1. لغة TSPL (TSC / Xprinter / Rongta / Zebra Label Printers)
 * 2. لغة ESC/POS Barcode للطابعات الحرارية المزدوجة
 */
object LabelPrinterCommands {

    private val WINDOWS_1256: Charset = Charset.forName("windows-1256")
    private val UTF_8: Charset = Charset.forName("UTF-8")

    /**
     * بناء حزمة أوامر TSPL القياسية لطباعة ملصق الباركود
     */
    fun buildTsplPayload(data: BarcodeLabelData): ByteArray {
        val baos = ByteArrayOutputStream()
        val sb = StringBuilder()

        val widthMm = data.size.widthMm
        val heightMm = data.size.heightMm
        val copies = data.copies.coerceIn(1, 100)

        // 1. تهيئة مقاس الملصق والفجوة والاتجاه ومسح الذاكرة المؤقتة
        sb.append("SIZE $widthMm mm, $heightMm mm\r\n")
        sb.append("GAP 2 mm, 0 mm\r\n")
        sb.append("DIRECTION 1\r\n")
        sb.append("REFERENCE 0,0\r\n")
        sb.append("CLS\r\n")

        val totalDotsW = data.size.dotsWidth
        val centerX = totalDotsW / 2

        var currentY = 15

        // 2. اسم المتجر (إذا كان مفعلاً)
        if (data.showStoreName && data.storeName.isNotBlank()) {
            val storeX = maxOf(10, centerX - (data.storeName.length * 6))
            sb.append("TEXT $storeX,$currentY,\"3\",0,1,1,\"${sanitizeText(data.storeName)}\"\r\n")
            currentY += 28
        }

        // 3. اسم الصنف والوحدة
        val itemLine = if (data.showUnitName && data.unitName.isNotBlank()) {
            "${data.productName} (${data.unitName})"
        } else {
            data.productName
        }
        val itemX = maxOf(10, centerX - (itemLine.length * 6))
        sb.append("TEXT $itemX,$currentY,\"3\",0,1,1,\"${sanitizeText(itemLine)}\"\r\n")
        currentY += 30

        // 4. رسم الباركود (EAN-13 أو 128)
        val barcodeCode = data.barcode.filter { it.isDigit() || it.isLetter() }.ifBlank { "2900000000018" }
        val isEan13 = barcodeCode.length == 13 && barcodeCode.all { it.isDigit() }
        val barcodeType = if (isEan13) "EAN13" else "128"

        val barcodeH = when (data.size) {
            LabelPaperSize.SIZE_38X25 -> 50
            LabelPaperSize.SIZE_40X30 -> 65
            LabelPaperSize.SIZE_50X30 -> 75
            LabelPaperSize.SIZE_58X40 -> 90
        }

        val barcodeX = when (data.size) {
            LabelPaperSize.SIZE_38X25 -> 25
            LabelPaperSize.SIZE_40X30 -> 30
            LabelPaperSize.SIZE_50X30 -> 40
            LabelPaperSize.SIZE_58X40 -> 50
        }

        // BARCODE x, y, "type", height, human_readable (1=yes), rotation (0), narrow, wide, "content"
        val humanReadable = if (data.showBarcodeText) 1 else 0
        sb.append("BARCODE $barcodeX,$currentY,\"$barcodeType\",$barcodeH,$humanReadable,0,2,2,\"$barcodeCode\"\r\n")
        currentY += barcodeH + (if (data.showBarcodeText) 26 else 10)

        // 5. السعر والضريبة
        if (data.showPrice) {
            val priceStr = "%.2f %s".format(data.price, data.currencySymbol)
            val priceWithTax = if (data.showTaxNote) "$priceStr شامل الضريبة" else priceStr
            val priceX = maxOf(10, centerX - (priceWithTax.length * 6))
            sb.append("TEXT $priceX,$currentY,\"3\",0,1,1,\"${sanitizeText(priceWithTax)}\"\r\n")
        }

        // 6. أمر الطباعة وعدد النسخ
        sb.append("PRINT $copies,1\r\n")

        val commandString = sb.toString()
        try {
            // محاولة التحويل أولاً بـ windows-1256 للنصوص العربية على الطابعات الحرارية
            baos.write(commandString.toByteArray(WINDOWS_1256))
        } catch (_: Exception) {
            baos.write(commandString.toByteArray(UTF_8))
        }

        return baos.toByteArray()
    }

    /**
     * إرجاع النص الخام لأوامر TSPL للمعاينة والتوثيق
     */
    fun buildTsplPreviewText(data: BarcodeLabelData): String {
        val widthMm = data.size.widthMm
        val heightMm = data.size.heightMm
        val copies = data.copies.coerceIn(1, 100)

        val sb = StringBuilder()
        sb.appendLine("; --- TSPL LABEL COMMAND STREAM ---")
        sb.appendLine("SIZE $widthMm mm, $heightMm mm")
        sb.appendLine("GAP 2 mm, 0 mm")
        sb.appendLine("DIRECTION 1")
        sb.appendLine("CLS")
        if (data.showStoreName) sb.appendLine("TEXT 20,15,\"3\",0,1,1,\"${data.storeName}\"")
        sb.appendLine("TEXT 20,45,\"3\",0,1,1,\"${data.productName} (${data.unitName})\"")
        val barcodeType = if (data.barcode.length == 13) "EAN13" else "128"
        sb.appendLine("BARCODE 25,80,\"$barcodeType\",55,1,0,2,2,\"${data.barcode}\"")
        if (data.showPrice) {
            val taxText = if (data.showTaxNote) "شامل الضريبة" else ""
            sb.appendLine("TEXT 20,165,\"3\",0,1,1,\"السعر: %.2f %s %s\"".format(data.price, data.currencySymbol, taxText))
        }
        sb.appendLine("PRINT $copies, 1")
        return sb.toString()
    }

    /**
     * بناء أوامر ESC/POS Barcode للطابعات الحرارية العادية
     */
    fun buildEscPosBarcodePayload(data: BarcodeLabelData): ByteArray {
        val baos = ByteArrayOutputStream()

        // تهيئة
        baos.write(byteArrayOf(0x1B, 0x40)) // ESC @
        baos.write(byteArrayOf(0x1B, 0x61, 0x01)) // Center alignment

        // اسم المتجر
        if (data.showStoreName && data.storeName.isNotBlank()) {
            baos.write(byteArrayOf(0x1B, 0x45, 0x01)) // Bold ON
            baos.write("${data.storeName}\n".toByteArray(WINDOWS_1256))
            baos.write(byteArrayOf(0x1B, 0x45, 0x00)) // Bold OFF
        }

        // اسم الصنف والوحدة
        baos.write("${data.productName} - ${data.unitName}\n".toByteArray(WINDOWS_1256))

        // إعدادات الباركود
        baos.write(byteArrayOf(0x1D, 0x77, 0x02)) // GS w 2 (Module width)
        baos.write(byteArrayOf(0x1D, 0x68, 0x40)) // GS h 64 (Height)
        baos.write(byteArrayOf(0x1D, 0x48, 0x02)) // GS H 2 (HRI characters below)

        val code = data.barcode.filter { it.isDigit() || it.isLetter() }.ifBlank { "290000000001" }
        // طباعة Barcode Code 128: GS k 73 [len] [data]
        val codeBytes = code.toByteArray(UTF_8)
        baos.write(byteArrayOf(0x1D, 0x6B, 0x49, codeBytes.size.toByte()))
        baos.write(codeBytes)
        baos.write("\n".toByteArray())

        // السعر
        if (data.showPrice) {
            val taxText = if (data.showTaxNote) "(شامل الضريبة)" else ""
            baos.write(byteArrayOf(0x1B, 0x45, 0x01)) // Bold ON
            baos.write("%.2f %s %s\n".format(data.price, data.currencySymbol, taxText).toByteArray(WINDOWS_1256))
            baos.write(byteArrayOf(0x1B, 0x45, 0x00)) // Bold OFF
        }

        // قص وتغذية
        baos.write(byteArrayOf(0x1D, 0x56, 0x42, 0x00)) // GS V 66 0
        return baos.toByteArray()
    }

    private fun sanitizeText(input: String): String {
        return input.replace("\"", " ").replace("\\", "/").trim()
    }
}
