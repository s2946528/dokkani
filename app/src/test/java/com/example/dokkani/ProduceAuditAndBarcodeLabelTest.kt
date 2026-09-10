package com.example.dokkani

import com.example.dokkani.data.local.entities.MovementType
import com.example.dokkani.domain.barcode.BarcodeGenerator
import com.example.dokkani.domain.hardware.BarcodeLabelData
import com.example.dokkani.domain.hardware.BluetoothPrinterManager
import com.example.dokkani.domain.hardware.LabelPaperSize
import com.example.dokkani.domain.hardware.LabelPrinterCommands
import com.example.dokkani.domain.produce.ProduceAuditEngine
import com.example.dokkani.domain.produce.ProduceAuditInput
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ProduceAuditAndBarcodeLabelTest {

    @Test
    fun testProduceAuditCogsFormula_accurateAccountingCalculation() {
        // اختبار معادلة التكلفة الآلية لصافي تكلفة المباع (COGS):
        // COGS = (مخزون أول المدة + المشتريات) - مخزون آخر المدة - التوالف
        val input = ProduceAuditInput(
            productId = 1L,
            productName = "طماطم بلدي",
            beginningInventoryQty = 20.0,      // أول المدة 20 كجم
            beginningInventoryCost = 80.0,     // تكلفة 80 ر.س (4 ر.س/كجم)
            purchasesQty = 50.0,               // مشتريات 50 كجم
            purchasesCost = 200.0,             // تكلفة 200 ر.س (4 ر.س/كجم)
            estimatedEndingInventoryQty = 18.0,// التقديري على الرف آخر اليوم 18 كجم
            wasteQty = 5.0,                    // التوالف والهالك 5 كجم
            posRecordedSoldQty = 45.0,         // مبيعات الكاشير المسجلة 45 كجم
            posRecordedRevenue = 270.0         // إيراد مبيعات 270 ر.س (بيع بـ 6 ر.س/كجم)
        )

        val result = ProduceAuditEngine.computeProduceAudit(input)

        // 1. فحص إجمالي المتاح للبيع: 20 + 50 = 70 كجم
        assertEquals(70.0, result.goodsAvailableQty, 0.001)
        assertEquals(280.0, result.goodsAvailableCost, 0.001)
        assertEquals(4.0, result.averageCostPerUnit, 0.001)

        // 2. فحص مخزون آخر المدة التقديري على الرف: 18 كجم × 4.0 = 72 ر.س
        assertEquals(18.0, result.endingInventoryQty, 0.001)
        assertEquals(72.0, result.endingInventoryCost, 0.001)

        // 3. فحص التوالف والهالك: 5 كجم × 4.0 = 20 ر.س
        assertEquals(5.0, result.wasteQty, 0.001)
        assertEquals(20.0, result.wasteCost, 0.001)

        // 4. تطبيق المعادلة المحاسبية لصافي تكلفة المباع (COGS):
        // COGS Qty = (20 + 50) - 18 - 5 = 47 كجم
        // COGS Cost = (80 + 200) - 72 - 20 = 188 ر.س
        assertEquals(47.0, result.cogsCalculatedQty, 0.001)
        assertEquals(188.0, result.cogsCalculatedCost, 0.001)

        // 5. فحص مجمل الربح المحقق: 270 - 188 = 82 ر.س
        assertEquals(82.0, result.grossProfit, 0.001)

        // 6. فحص قيود التسوية المخزنية الصامتة المولدة:
        // يجب أن تتضمن قيد إثبات التوالف (-5 كجم) وقيد تسوية الرف
        assertTrue(result.generatedAdjustments.isNotEmpty())
        val wasteAdjustment = result.generatedAdjustments.firstOrNull { it.quantityChange == -5.0 }
        assertNotNull(wasteAdjustment)
        assertEquals(MovementType.INVENTORY_ADJUSTMENT, wasteAdjustment?.movementType)
        assertTrue(wasteAdjustment?.reasonArabic?.contains("توالف وهالك") == true)
    }

    @Test
    fun testBarcodeGenerator_ean13Modulo10CheckDigit() {
        // التحقق من صحة خانة التحقق المودولو 10 لباركود EAN-13
        val barcode = BarcodeGenerator.generateEan13Barcode(
            prefix = "290",
            uniqueNumber = 123456789L
        )

        assertEquals(13, barcode.length)
        assertTrue(barcode.startsWith("290"))
        assertTrue(BarcodeGenerator.isValidEan13(barcode))

        // فحص صحة حساب خانة التحقق يدوياً
        val body12 = barcode.substring(0, 12)
        val expectedCheckDigit = BarcodeGenerator.calculateEan13CheckDigit(body12)
        assertEquals(expectedCheckDigit, barcode.last().digitToInt())
    }

    @Test
    fun testBarcodeGenerator_uniqueSingleUnitBarcode() {
        // توليد باركود فريد مخصص للوحدة الفردية المستخرجة من كرتون
        val barcode = BarcodeGenerator.generateUniqueSingleUnitBarcode(productId = 4L, unitId = 12L)

        assertEquals(13, barcode.length)
        assertTrue(barcode.startsWith("290"))
        assertTrue(BarcodeGenerator.isValidEan13(barcode))

        // فحص رسم أعمدة الباركود (Canvas Bars)
        val bars = BarcodeGenerator.generateBarcodeBars(barcode)
        assertTrue(bars.isNotEmpty())
        assertTrue(bars.size >= 90)
    }

    @Test
    fun testLabelPrinterCommands_tsplGeneration() {
        val labelData = BarcodeLabelData(
            storeName = "تموينات البركة",
            productName = "خيار طازج محلي",
            unitName = "عبوة صحن",
            barcode = "2900004012019",
            price = 4.75,
            currencySymbol = "ر.س",
            size = LabelPaperSize.SIZE_38X25,
            copies = 3,
            showStoreName = true,
            showUnitName = true,
            showPrice = true,
            showBarcodeText = true,
            showTaxNote = true
        )

        // بناء أوامر TSPL
        val tsplText = LabelPrinterCommands.buildTsplPreviewText(labelData)
        val tsplBytes = LabelPrinterCommands.buildTsplPayload(labelData)

        assertTrue(tsplText.contains("SIZE 38 mm, 25 mm"))
        assertTrue(tsplText.contains("GAP 2 mm, 0 mm"))
        assertTrue(tsplText.contains("CLS"))
        assertTrue(tsplText.contains("BARCODE"))
        assertTrue(tsplText.contains("2900004012019"))
        assertTrue(tsplText.contains("PRINT 3"))
        assertTrue(tsplBytes.isNotEmpty())
    }

    @Test
    fun testBluetoothPrinterManager_printBarcodeLabelSimulated() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val printerManager = BluetoothPrinterManager(context)

        val labelData = BarcodeLabelData(
            storeName = "دكاني",
            productName = "حزمة بقدونس",
            unitName = "ربطة",
            barcode = "2900005001015",
            price = 1.50,
            size = LabelPaperSize.SIZE_40X30,
            copies = 5
        )

        val result = printerManager.printBarcodeLabel(labelData)

        assertTrue(result.success)
        assertEquals(5, result.copiesPrinted)
        assertTrue(result.tsplCommands.contains("SIZE 40 mm, 30 mm"))
        assertTrue(result.tsplCommands.contains("PRINT 5"))
    }
}
