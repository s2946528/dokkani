package com.example.dokkani

import com.example.dokkani.data.local.entities.FixedAssetEntity
import com.example.dokkani.data.local.entities.LeaseholdRightEntity
import com.example.dokkani.data.local.entities.MovementType
import com.example.dokkani.data.local.entities.OwnerTransactionEntity
import com.example.dokkani.data.local.entities.OwnerTransactionType
import com.example.dokkani.data.local.entities.PartyEntity
import com.example.dokkani.data.local.entities.PartyType
import com.example.dokkani.data.local.entities.PaymentMethod
import com.example.dokkani.data.local.entities.ProductEntity
import com.example.dokkani.data.local.entities.ProductUnitEntity
import com.example.dokkani.data.local.entities.ProductWithUnits
import com.example.dokkani.data.local.entities.StockMovementEntity
import com.example.dokkani.domain.assets.AssetsAndEquityEngine
import org.junit.Assert.assertEquals
import org.junit.Test

class AssetsAndEquityAccountingTest {

    @Test
    fun testInitialCapitalIncludesFixedAssetsAndExcludesLiabilities() {
        // نقدية الصندوق والدرج: 50,000 ر.ي
        val cashInDrawer = 50000.0
        // أرصدة البنوك ومقبوضات مدى: 20,000 ر.ي
        val bankBalances = 20000.0

        // بضاعة أول المدة بالتكلفة: 100 قطعة بسعر تكلفة 1000 = 100,000 ر.ي
        val prod = ProductEntity(id = 1, code = "P1", name = "سكر", category = "معلبات")
        val unit = ProductUnitEntity(
            id = 10,
            productId = 1,
            unitName = "كيس",
            conversionFactor = 1.0,
            barcode = "123456",
            costPrice = 1000.0,
            sellingPrice = 1200.0,
            isBaseUnit = true
        )
        val productsWithUnits = listOf(ProductWithUnits(prod, listOf(unit)))
        val stockMovements = listOf(
            StockMovementEntity(
                id = 1,
                productId = 1,
                productUnitId = 10,
                movementType = MovementType.PURCHASE_IN,
                quantityBaseUnit = 100.0,
                remainingQuantityForFifo = 100.0,
                unitCostPriceBase = 1000.0,
                timestamp = System.currentTimeMillis()
            )
        )

        // الأصول الثابتة: ثلاجات (50,000) + أرفف وتجهيزات (30,000) = 80,000 ر.ي
        val fixedAssets = listOf(
            FixedAssetEntity(id = 1, assetCode = "AST-1", name = "ثلاجات عرض ألبان", category = "ثلاجات وتبريد", purchaseCost = 50000.0, currentValue = 50000.0, purchaseDate = 1000L),
            FixedAssetEntity(id = 2, assetCode = "AST-2", name = "أرفف جدارية", category = "أرفف وتجهيزات عرض", purchaseCost = 30000.0, currentValue = 30000.0, purchaseDate = 1000L)
        )

        // نقل القدم / خلو المحل كأصل غير ملموس: 30,000 ر.ي
        val leaseholdRights = listOf(
            LeaseholdRightEntity(id = 1, code = "L1", name = "خلو المحل", initialCost = 30000.0, currentBookValue = 30000.0, contractStartDate = 1000L)
        )

        // ديون العملاء (أرصدة مدينة مستحقة للبقالة): 15,000 ر.ي
        // ديون الموردين (أرصدة دائنة مستحقة على البقالة): 25,000 ر.ي
        val parties = listOf(
            PartyEntity(id = 1, name = "عميل آجل", type = PartyType.CUSTOMER, currentBalance = 15000.0),
            PartyEntity(id = 2, name = "شركة الألبان", type = PartyType.SUPPLIER, currentBalance = -25000.0)
        )

        val ownerTransactions = emptyList<OwnerTransactionEntity>()

        val result = AssetsAndEquityEngine.calculateInitialCapitalAndEquity(
            cashInDrawer = cashInDrawer,
            bankBalances = bankBalances,
            productsWithUnits = productsWithUnits,
            stockMovements = stockMovements,
            parties = parties,
            fixedAssets = fixedAssets,
            leaseholdRights = leaseholdRights,
            ownerTransactions = ownerTransactions,
            netOperatingProfit = 0.0
        )

        // إجمالي الأصول التأسيسية:
        // 50,000 (نقدية) + 20,000 (بنوك) + 100,000 (بضاعة) + 80,000 (أصول ثابتة) + 30,000 (خلو) + 15,000 (ديون عملاء)
        // = 295,000 ر.ي
        // مطروحاً منها التزامات الموردين (25,000):
        // رأس المال الافتتاحي = 295,000 - 25,000 = 270,000 ر.ي
        assertEquals(80000.0, result.totalFixedAssetsValue, 0.001)
        assertEquals(270000.0, result.calculatedInitialCapital, 0.001)
    }

    @Test
    fun testNetTotalEquityPreventsDoubleCountingOfFixedAssets() {
        val cashInDrawer = 10000.0
        val bankBalances = 5000.0
        val fixedAssets = listOf(
            FixedAssetEntity(id = 1, assetCode = "AST-1", name = "ثلاجة", category = "ثلاجات وتبريد", purchaseCost = 40000.0, currentValue = 40000.0, purchaseDate = 1000L)
        )

        // إيداع رأس مال إضافي: 20,000 ر.ي
        // مسحوبات شخصية: 5,000 ر.ي
        val ownerTransactions = listOf(
            OwnerTransactionEntity(id = 1, transactionNumber = "T1", type = OwnerTransactionType.CAPITAL_DEPOSIT, amount = 20000.0, date = 1000L),
            OwnerTransactionEntity(id = 2, transactionNumber = "T2", type = OwnerTransactionType.CASH_DRAWING, amount = 5000.0, date = 2000L)
        )

        // أرباح تشغيلية مبقاة: 15,000 ر.ي
        val netOperatingProfit = 15000.0

        val result = AssetsAndEquityEngine.calculateInitialCapitalAndEquity(
            cashInDrawer = cashInDrawer,
            bankBalances = bankBalances,
            productsWithUnits = emptyList(),
            stockMovements = emptyList(),
            parties = emptyList(),
            fixedAssets = fixedAssets,
            leaseholdRights = emptyList(),
            ownerTransactions = ownerTransactions,
            netOperatingProfit = netOperatingProfit
        )

        // رأس المال الافتتاحي = 10,000 (نقدية) + 5,000 (بنوك) + 40,000 (أصول ثابتة) = 55,000 ر.ي
        assertEquals(55000.0, result.calculatedInitialCapital, 0.001)

        // صافي حقوق الملكية الإجمالي =
        // رأس المال الافتتاحي (55,000) + إيداعات إضافية (20,000) - مسحوبات (5,000) + أرباح تشغيلية (15,000)
        // = 85,000 ر.ي
        // (وليس 85,000 + 40,000 = 125,000 بتضاعف الأصول الثابتة)
        assertEquals(85000.0, result.netTotalEquity, 0.001)
    }
}
