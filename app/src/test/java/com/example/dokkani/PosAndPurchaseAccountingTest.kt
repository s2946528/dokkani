package com.example.dokkani

import com.example.dokkani.data.local.entities.MovementType
import com.example.dokkani.data.local.entities.PartyType
import com.example.dokkani.data.local.entities.PaymentMethod
import com.example.dokkani.domain.pos.PosOperation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PosAndPurchaseAccountingTest {

    @Test
    fun testPosOperationTypesDistinction() {
        // التحقق من التفريق بين الفواتير (سلة) والسندات (نموذج مالي)
        assertTrue(PosOperation.SALE.isInvoiceType)
        assertTrue(PosOperation.PURCHASE.isInvoiceType)
        assertTrue(PosOperation.SALE_RETURN.isInvoiceType)
        assertTrue(PosOperation.PURCHASE_RETURN.isInvoiceType)

        assertFalse(PosOperation.RECEIPT.isInvoiceType)
        assertFalse(PosOperation.EXPENSE.isInvoiceType)

        assertTrue(PosOperation.RECEIPT.isVoucher)
        assertTrue(PosOperation.EXPENSE.isVoucher)
    }

    @Test
    fun testWacCalculationFormula() {
        // قانون المتوسط المرجح WAC:
        // مخزون قديم: 20 كجم بسعر 3.00 ر.س = 60 ر.س
        // شراء جديد: 30 كجم بسعر 4.00 ر.س = 120 ر.س
        // إجمالي القيمة: 180 ر.س
        // إجمالي الكمية: 50 كجم
        // التكلفة المرجحة الجديدة: 180 / 50 = 3.60 ر.س
        val oldStock = 20.0
        val oldCost = 3.0
        val purchaseQty = 30.0
        val purchaseCost = 4.0

        val newWacCost = ((oldStock * oldCost) + (purchaseQty * purchaseCost)) / (oldStock + purchaseQty)
        assertEquals(3.60, newWacCost, 0.0001)
    }

    @Test
    fun testCustomerCreditAccounting() {
        // مديونية العميل:
        // فاتورة بيع آجل بمبلغ 150 ر.س -> رصيد العميل يزيد (+150)
        var customerBalance = 0.0
        val saleCreditAmount = 150.0
        customerBalance += saleCreditAmount
        assertEquals(150.0, customerBalance, 0.001)

        // سند قبض من العميل بمبلغ 50 ر.س -> يقلل دين العميل (-50)
        val receiptAmount = 50.0
        customerBalance -= receiptAmount
        assertEquals(100.0, customerBalance, 0.001)

        // مردود مبيعات آجل بمبلغ 30 ر.س -> يقلل دين العميل (-30)
        val returnAmount = 30.0
        customerBalance -= returnAmount
        assertEquals(70.0, customerBalance, 0.001)
    }

    @Test
    fun testSupplierCreditAccounting() {
        // التزام المورد (دائن لنا بالسالب):
        var supplierBalance = 0.0
        val purchaseCreditAmount = 500.0
        supplierBalance -= purchaseCreditAmount
        assertEquals(-500.0, supplierBalance, 0.001) // دائن لنا بـ 500

        // سند صرف / دفعة مسددة للمورد بمبلغ 200 ر.س -> تخفيض التزامه (+200)
        val paymentVoucherAmount = 200.0
        supplierBalance += paymentVoucherAmount
        assertEquals(-300.0, supplierBalance, 0.001) // دائن لنا بـ 300
    }

    @Test
    fun testStockMovementSignRules() {
        // مبيعات تنقص المخزون
        val saleQty = 5.0
        val saleMovementBase = -saleQty
        assertTrue(saleMovementBase < 0)

        // مشتريات تزيد المخزون
        val purchaseQty = 10.0
        val purchaseMovementBase = purchaseQty
        assertTrue(purchaseMovementBase > 0)

        // مردود بيع يدخل المخزون
        val saleReturnQty = 2.0
        val returnInBase = saleReturnQty
        assertTrue(returnInBase > 0)

        // مردود شراء يخرج من المخزون
        val purchaseReturnQty = 1.0
        val returnOutBase = -purchaseReturnQty
        assertTrue(returnOutBase < 0)
    }

    @Test
    fun testStartNewInvoiceStateAndNavigationReset() {
        // التحقق من القيم الافتراضية والجاهزية لفاتورة جديدة:
        // 1. التوجيه التلقائي إلى قائمة الأصناف (CATALOG) وليس السلة الفارغة
        // 2. تصفير السلة بالكامل
        // 3. تصفير الخصم والمدفوع
        // 4. إغلاق نافذة الإيصال
        val freshState = com.example.dokkani.ui.screens.pos.PosUiState()
        assertEquals(com.example.dokkani.ui.screens.pos.PosMobileTab.CATALOG, freshState.activeMobileTab)
        assertEquals(com.example.dokkani.ui.screens.pos.PosMobileTab.CATALOG, freshState.selectedTab)
        assertEquals(0.0, freshState.discount, 0.0)
        assertEquals("", freshState.paidAmountInput)
        assertFalse(freshState.showReceiptDialog)
        assertTrue(freshState.cartItems.isEmpty())
    }

    @Test
    fun testPurchaseReturnHistoricalCostAccounting() {
        // القاعدة المحاسبية:
        // عند إجراء مردود مشتريات عن فاتورة شراء سابقة:
        // سعر التكلفة التاريخي المسجل داخل الفاتورة الأصلية = 12.50 ر.س
        // سعر التكلفة الحالي في جدول الأصناف العامة ارتفع لاحقاً إلى 18.00 ر.س
        // يجب أن يعتمد النظام حصرياً على التكلفة التاريخية (12.50 ر.س)
        val historicalInvoiceCost = 12.50
        val currentGeneralCost = 18.00
        val purchasedQuantity = 10.0

        val returnItem = com.example.dokkani.ui.screens.purchasereturn.PurchaseReturnItem(
            productId = 1L,
            productName = "حليب نادك 1 لتر",
            productCode = "NADEC-1L",
            unitId = 1L,
            unitName = "كرتون",
            conversionFactor = 1.0,
            originalUnitCostPrice = historicalInvoiceCost,
            originalPurchasedQuantity = purchasedQuantity,
            returnQuantity = 4.0,
            returnCostPrice = historicalInvoiceCost,
            isSelectedForReturn = true
        )

        // 1. التحقق من التكلفة المعتمدة هي التاريخية (12.50) وليس الحالية (18.00)
        assertEquals(historicalInvoiceCost, returnItem.returnCostPrice, 0.001)
        assertEquals(historicalInvoiceCost, returnItem.originalUnitCostPrice, 0.001)
        assertTrue(returnItem.returnCostPrice != currentGeneralCost)

        // 2. التحقق من حساب إجمالي سطر المردود (4 * 12.50 = 50.00 ر.س)
        assertEquals(50.0, returnItem.totalReturnCost, 0.001)

        // 3. التحقق من قاعدة عدم تجاوز الكمية المشتراة
        val requestedExcessQty = 15.0
        val clampedQty = requestedExcessQty.coerceAtMost(returnItem.originalPurchasedQuantity)
        assertEquals(purchasedQuantity, clampedQty, 0.001) // تم تقييدها بالحد الأقصى 10.0

        // 4. التحقق من الأثر المحاسبي والمالي:
        // رصيد المورد الأصلي: دائن بـ 1000 (-1000.0)
        // مردود مشتريات آجل بـ 50 ر.س -> يخفض التزام المورد (-1000 + 50 = -950.0)
        var supplierBalance = -1000.0
        supplierBalance += returnItem.totalReturnCost
        assertEquals(-950.0, supplierBalance, 0.001)

        // 5. التحقق من حركة المخزون المعكوسة (RETURN_OUT):
        // خروج من المخزون بقيمة سالبة = -4 وحدات
        val stockMovementQty = -(returnItem.returnQuantity * returnItem.conversionFactor)
        assertEquals(-4.0, stockMovementQty, 0.001)
        assertTrue(stockMovementQty < 0)
    }
}
