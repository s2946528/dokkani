package com.example.dokkani.domain.assets

import com.example.dokkani.data.local.entities.FixedAssetEntity
import com.example.dokkani.data.local.entities.InvoiceEntity
import com.example.dokkani.data.local.entities.InvoiceType
import com.example.dokkani.data.local.entities.LeaseholdRightEntity
import com.example.dokkani.data.local.entities.OwnerTransactionEntity
import com.example.dokkani.data.local.entities.OwnerTransactionType
import com.example.dokkani.data.local.entities.PartyEntity
import com.example.dokkani.data.local.entities.PartyType
import com.example.dokkani.data.local.entities.ProductWithUnits
import com.example.dokkani.data.local.entities.StockMovementEntity

/**
 * محرك المعالجة الآلية لرأس المال والأصول وحقوق الملكية
 */
object AssetsAndEquityEngine {

    /**
     * حساب رأس المال الافتتاحي وحقوق الملكية آلياً وفق المعادلة المحاسبية المعتمدة:
     * رأس المال الافتتاحي = (نقدية الخزينة + أرصدة البنوك + تقييم بضاعة أول المدة + خلو/نقل القدم + ديون العملاء) - (ديون الموردين والالتزامات)
     */
    fun calculateInitialCapitalAndEquity(
        cashInDrawer: Double,
        bankBalances: Double,
        productsWithUnits: List<ProductWithUnits>,
        stockMovements: List<StockMovementEntity>,
        parties: List<PartyEntity>,
        fixedAssets: List<FixedAssetEntity>,
        leaseholdRights: List<LeaseholdRightEntity> = emptyList(),
        ownerTransactions: List<OwnerTransactionEntity>,
        netOperatingProfit: Double = 0.0
    ): EquityCalculationResult {
        // 1. حساب تقييم البضاعة والمخزون الحالي بسعر التكلفة
        val stockByProduct = stockMovements.groupBy { it.productId }
            .mapValues { entry -> entry.value.sumOf { it.quantityBaseUnit } }

        var inventoryValuation = 0.0
        for (pwu in productsWithUnits) {
            val prodId = pwu.product.id
            val currentStock = stockByProduct[prodId] ?: 0.0
            val baseUnitCost = pwu.units.firstOrNull { it.isBaseUnit }?.costPrice ?: 0.0
            if (currentStock > 0) {
                inventoryValuation += (currentStock * baseUnitCost)
            }
        }

        // 2. حساب ديون العملاء (الأرصدة المدينة المستحقة للبقالة)
        val customerReceivables = parties
            .filter { it.type == PartyType.CUSTOMER && it.currentBalance > 0 }
            .sumOf { it.currentBalance }

        // 3. حساب ديون الموردين (الأرصدة الدائنة المستحقة على البقالة)
        val supplierPayables = parties
            .filter { it.type == PartyType.SUPPLIER && it.currentBalance < 0 }
            .sumOf { kotlin.math.abs(it.currentBalance) }

        // 4. إجمالي القيمة الدفترية للأصول الثابتة
        val totalFixedAssets = fixedAssets
            .filter { it.status == "ACTIVE" }
            .sumOf { it.currentValue }

        // 5. إجمالي القيمة الدفترية لنقل القدم / الخلو كأصل غير ملموس
        val totalLeaseholdGoodwill = leaseholdRights
            .filter { it.status == "ACTIVE" }
            .sumOf { it.currentBookValue }

        // 6. حساب رأس المال الافتتاحي الآلي
        // رأس المال تلقائياً = (نقدية الخزينة + أرصدة البنوك + تقييم بضاعة أول المدة + الخلو كأصل تأسيس + ديون العملاء) - (ديون الموردين والالتزامات)
        val calculatedCapital = (cashInDrawer + bankBalances + inventoryValuation + totalLeaseholdGoodwill + customerReceivables) - supplierPayables

        // 7. مسحوبات المالك وإيداعات رأس المال
        val totalDrawings = ownerTransactions
            .filter { it.type == OwnerTransactionType.CASH_DRAWING || it.type == OwnerTransactionType.GOODS_DRAWING }
            .sumOf { it.amount }

        val totalDeposits = ownerTransactions
            .filter { it.type == OwnerTransactionType.CAPITAL_DEPOSIT }
            .sumOf { it.amount }

        // 8. صافي حقوق الملكية الإجمالي = رأس المال الافتتاحي + إيداعات إضافية - مسحوبات المالك + الأصول الثابتة + الخلو + صافي الأرباح
        val netEquity = calculatedCapital + totalDeposits - totalDrawings + totalFixedAssets + netOperatingProfit

        return EquityCalculationResult(
            cashInHandAndDrawer = cashInDrawer,
            bankAndMadaBalances = bankBalances,
            inventoryValuationAtCost = inventoryValuation,
            customerReceivables = customerReceivables,
            supplierPayables = supplierPayables,
            totalFixedAssetsValue = totalFixedAssets,
            totalLeaseholdGoodwillValue = totalLeaseholdGoodwill,
            calculatedInitialCapital = calculatedCapital,
            totalOwnerDrawings = totalDrawings,
            totalAdditionalCapitalDeposits = totalDeposits,
            netOperatingProfit = netOperatingProfit,
            netTotalEquity = netEquity
        )
    }
}
