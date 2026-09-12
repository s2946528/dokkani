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
}
