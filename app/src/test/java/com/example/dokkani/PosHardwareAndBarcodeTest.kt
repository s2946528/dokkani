package com.example.dokkani

import com.example.dokkani.domain.barcode.ScaleBarcodeParser
import com.example.dokkani.domain.hardware.EscPosHelper
import com.example.dokkani.domain.hardware.PrinterPaperWidth
import com.example.dokkani.domain.hardware.ReceiptItemData
import com.example.dokkani.domain.hardware.ReceiptPrintData
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PosHardwareAndBarcodeTest {

    @Test
    fun testScaleBarcodeParser_WeightExtraction() {
        // باركود ميزان إلكتروني: 21 (بادئة) + 0004 (كود الصنف) + 01750 (الوزن: 1.750 كجم) + 8 (فحص)
        val barcode = "210004017508"

        assertTrue("Should be detected as scale barcode", ScaleBarcodeParser.isScaleBarcode(barcode))

        val result = ScaleBarcodeParser.parse(barcode, assumeWeightBased = true)

        assertTrue(result.isValidScaleBarcode)
        assertEquals("21", result.prefix)
        assertEquals("0004", result.productCode)
        assertNotNull(result.weightKg)
        assertEquals(1.750, result.weightKg!!, 0.0001)
    }

    @Test
    fun testScaleBarcodeParser_PriceExtraction() {
        // باركود ميزان بالسعر المضمن: 20 (بادئة) + 0005 (كود الصنف) + 01550 (السعر: 15.50 ر.س) + 1
        val barcode = "200005015501"

        assertTrue(ScaleBarcodeParser.isScaleBarcode(barcode))

        val result = ScaleBarcodeParser.parse(barcode, assumeWeightBased = false)

        assertTrue(result.isValidScaleBarcode)
        assertEquals("20", result.prefix)
        assertEquals("0005", result.productCode)
        assertNotNull(result.embeddedPrice)
        assertEquals(15.50, result.embeddedPrice!!, 0.0001)
    }

    @Test
    fun testScaleBarcodeParser_StandardEan13NotScaleBarcode() {
        // باركود تجاري عادي (حليب المراعي 628...) لا يبدأ بـ 20 أو 21
        val standardBarcode = "6281007010015"
        assertFalse(ScaleBarcodeParser.isScaleBarcode(standardBarcode))
    }

    @Test
    fun testEscPosCashDrawerKickCommand() {
        // اختبار أمر فتح درج النقدية المعتمد قياسياً: ESC p 0 25 250
        val drawerBytes = EscPosHelper.CMD_OPEN_CASH_DRAWER_PIN2

        val expected = byteArrayOf(
            0x1B, 0x70, 0x00, 25.toByte(), 250.toByte()
        )

        assertArrayEquals(expected, drawerBytes)
    }

    @Test
    fun testEscPosReceiptBuilderGeneratesBytes() {
        val testReceipt = ReceiptPrintData(
            storeName = "بقالة دكاني النموذجية",
            storePhone = "0501234567",
            taxNumber = "310123456700003",
            invoiceNumber = "INV-TEST-001",
            invoiceDateFormatted = "2026/09/10 12:00",
            cashierName = "كاشير 1",
            paymentMethodArabic = "كاش (نقدي)",
            customerName = null,
            customerOldBalance = null,
            customerNewBalance = null,
            items = listOf(
                ReceiptItemData(
                    name = "طماطم بلدي",
                    quantityFormatted = "1.750 كجم",
                    unitPrice = 5.50,
                    totalPrice = 9.625
                ),
                ReceiptItemData(
                    name = "خبز صامولي",
                    quantityFormatted = "2 حبة",
                    unitPrice = 1.50,
                    totalPrice = 3.00
                )
            ),
            subtotal = 12.625,
            discount = 0.0,
            taxRatePercent = 15.0,
            taxAmount = 1.89,
            total = 14.52,
            paidAmount = 20.00,
            remainingAmount = 0.0,
            currencySymbol = "ر.س",
            footerText = "شكراً لزيارتكم!"
        )

        val bytes80 = EscPosHelper.buildReceiptPayload(testReceipt, PrinterPaperWidth.WIDTH_80MM)
        assertTrue(bytes80.isNotEmpty())

        val bytes58 = EscPosHelper.buildReceiptPayload(testReceipt, PrinterPaperWidth.WIDTH_58MM)
        assertTrue(bytes58.isNotEmpty())
    }
}
