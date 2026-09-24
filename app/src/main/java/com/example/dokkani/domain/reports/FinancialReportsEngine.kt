package com.example.dokkani.domain.reports

import com.example.dokkani.data.local.entities.CostValuationMethod
import com.example.dokkani.data.local.entities.ExpenseEntity
import com.example.dokkani.data.local.entities.InvoiceEntity
import com.example.dokkani.data.local.entities.InvoiceItemEntity
import com.example.dokkani.data.local.entities.InvoiceType
import com.example.dokkani.data.local.entities.PaymentMethod
import com.example.dokkani.data.local.entities.ProductWithUnits
import com.example.dokkani.data.local.entities.StockMovementEntity
import com.example.dokkani.data.local.entities.FinancialAccountEntity
import com.example.dokkani.data.local.entities.PartyEntity
import com.example.dokkani.data.local.entities.PartyType
import com.example.dokkani.domain.assets.EquityCalculationResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * المحرك المالي الشامل للتقارير والأرباح والخسائر وحركة المخزون
 */
object FinancialReportsEngine {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun generateProfitAndLossReport(
        invoices: List<InvoiceEntity>,
        invoiceItems: List<InvoiceItemEntity>,
        expenses: List<ExpenseEntity>,
        productUnitCosts: Map<Long, Double>,
        valuationMethod: CostValuationMethod
    ): ProfitAndLossReport {
        val saleInvoices = invoices.filter { it.type == InvoiceType.SALE }

        val grossSales = saleInvoices.sumOf { it.subtotal }
        val totalDiscounts = saleInvoices.sumOf { it.discount }
        val netSalesRevenue = (grossSales - totalDiscounts).coerceAtLeast(0.0)
        val taxCollected = saleInvoices.sumOf { it.taxAmount }

        val totalCashSales = saleInvoices.filter { it.paymentMethod == PaymentMethod.CASH }.sumOf { it.total }
        val totalMadaSales = saleInvoices.filter { it.paymentMethod.isElectronic }.sumOf { it.total }
        val totalCreditSales = saleInvoices.filter { it.paymentMethod == PaymentMethod.CREDIT }.sumOf { it.total }

        // حساب تكلفة البضاعة المباعة (COGS)
        val saleInvoiceIds = saleInvoices.map { it.id }.toSet()
        val itemsSold = invoiceItems.filter { it.invoiceId in saleInvoiceIds }

        var calculatedCogs = 0.0
        for (item in itemsSold) {
            val baseCost = productUnitCosts[item.productId] ?: item.unitCostPrice
            val itemCogs = item.quantity * item.unitConversionFactor * baseCost
            calculatedCogs += itemCogs
        }

        val grossProfit = netSalesRevenue - calculatedCogs
        val grossMarginPercent = if (netSalesRevenue > 0.001) (grossProfit / netSalesRevenue) * 100 else 0.0

        // المصروفات التشغيلية الحقيقية (استبعاد شراء الأصول الثابتة والمسحوبات الشخصية)
        val operationalExpenses = expenses.filter { exp ->
            val cat = exp.category.trim()
            !cat.contains("أصل") && !cat.contains("أصول") && !cat.contains("مسحوبات") && !cat.contains("رأس المال")
        }
        val totalExpenses = operationalExpenses.sumOf { it.amount }
        val expensesByCategory = operationalExpenses.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        val netOperatingProfit = grossProfit - totalExpenses
        val netProfitMarginPercent = if (netSalesRevenue > 0.001) (netOperatingProfit / netSalesRevenue) * 100 else 0.0

        val avgInvoiceValue = if (saleInvoices.isNotEmpty()) netSalesRevenue / saleInvoices.size else 0.0

        return ProfitAndLossReport(
            valuationMethodUsed = valuationMethod,
            totalInvoicesCount = saleInvoices.size,
            grossSales = grossSales,
            totalDiscounts = totalDiscounts,
            netSalesRevenue = netSalesRevenue,
            taxCollected = taxCollected,
            totalCashSales = totalCashSales,
            totalMadaSales = totalMadaSales,
            totalCreditSales = totalCreditSales,
            cogs = calculatedCogs,
            grossProfit = grossProfit,
            grossProfitMarginPercent = grossMarginPercent,
            totalOperatingExpenses = totalExpenses,
            expensesByCategory = expensesByCategory,
            netOperatingProfit = netOperatingProfit,
            netProfitMarginPercent = netProfitMarginPercent,
            averageInvoiceValue = avgInvoiceValue
        )
    }

    fun generateTopProductsReport(
        productsWithUnits: List<ProductWithUnits>,
        invoiceItems: List<InvoiceItemEntity>,
        productUnitCosts: Map<Long, Double>
    ): TopProductsReport {
        val productMap = productsWithUnits.associateBy { it.product.id }
        val groupedByProduct = invoiceItems.groupBy { it.productId }

        val topItemList = mutableListOf<TopProductItem>()

        for ((productId, items) in groupedByProduct) {
            val pwu = productMap[productId]
            val productName = pwu?.product?.name ?: "صنف #$productId"
            val category = pwu?.product?.category ?: "عام"
            val baseUnit = pwu?.units?.firstOrNull { it.isBaseUnit }?.unitName ?: "حبة"

            val totalQtySold = items.sumOf { it.quantity * it.unitConversionFactor }
            val totalRevenue = items.sumOf { it.totalPrice }

            var totalCost = 0.0
            for (itm in items) {
                val unitCost = productUnitCosts[productId] ?: itm.unitCostPrice
                totalCost += (itm.quantity * itm.unitConversionFactor * unitCost)
            }

            val grossProfit = totalRevenue - totalCost
            val margin = if (totalRevenue > 0.001) (grossProfit / totalRevenue) * 100 else 0.0

            topItemList.add(
                TopProductItem(
                    productId = productId,
                    productName = productName,
                    category = category,
                    totalQuantitySold = totalQtySold,
                    baseUnitName = baseUnit,
                    totalRevenue = totalRevenue,
                    totalCost = totalCost,
                    grossProfit = grossProfit,
                    profitMarginPercent = margin
                )
            )
        }

        val topMoving = topItemList.sortedByDescending { it.totalQuantitySold }
        val topProfitable = topItemList.sortedByDescending { it.grossProfit }

        return TopProductsReport(
            topMovingByQuantity = topMoving,
            topProfitableByMargin = topProfitable
        )
    }

    fun generateInventoryHealthReport(
        productsWithUnits: List<ProductWithUnits>,
        stockMovements: List<StockMovementEntity>
    ): InventoryHealthReport {
        val now = System.currentTimeMillis()
        val oneDayMillis = 86400000L

        val stockByProduct = stockMovements.groupBy { it.productId }
            .mapValues { entry -> entry.value.sumOf { it.quantityBaseUnit } }

        val lowStockList = mutableListOf<InventoryShortageItem>()
        val expiryAlertList = mutableListOf<ExpiryAlertItem>()

        for (pwu in productsWithUnits) {
            val prod = pwu.product
            val currentStock = stockByProduct[prod.id] ?: 0.0
            val baseUnit = pwu.units.firstOrNull { it.isBaseUnit }?.unitName ?: "حبة"

            // فحص حد النواقص
            if (currentStock <= prod.minStockAlert) {
                lowStockList.add(
                    InventoryShortageItem(
                        productId = prod.id,
                        code = prod.code,
                        name = prod.name,
                        category = prod.category,
                        currentStock = currentStock,
                        minStockAlert = prod.minStockAlert,
                        baseUnitName = baseUnit,
                        isOutOfStock = currentStock <= 0.001
                    )
                )
            }

            // فحص تواريخ الصلاحية
            prod.expiryDate?.let { expDate ->
                val daysRemaining = (expDate - now) / oneDayMillis
                val status = when {
                    daysRemaining < 0 -> ExpiryStatus.EXPIRED
                    daysRemaining <= 7 -> ExpiryStatus.CRITICAL
                    daysRemaining <= 30 -> ExpiryStatus.WARNING
                    else -> ExpiryStatus.GOOD
                }

                if (status != ExpiryStatus.GOOD) {
                    expiryAlertList.add(
                        ExpiryAlertItem(
                            productId = prod.id,
                            name = prod.name,
                            category = prod.category,
                            expiryDate = expDate,
                            expiryDateFormatted = dateFormat.format(Date(expDate)),
                            daysRemaining = daysRemaining,
                            status = status
                        )
                    )
                }
            }
        }

        val sortedLowStock = lowStockList.sortedBy { it.currentStock }
        val sortedExpiry = expiryAlertList.sortedBy { it.daysRemaining }

        return InventoryHealthReport(
            lowStockItems = sortedLowStock,
            expiryAlerts = sortedExpiry,
            totalLowStockCount = sortedLowStock.size,
            totalExpiredOrNearExpiryCount = sortedExpiry.size
        )
    }

    /**
     * توليد تقرير الميزانية العمومية الشامل (Balance Sheet Report)
     */
    fun generateBalanceSheetReport(
        equityResult: EquityCalculationResult?
    ): BalanceSheetReport {
        val eq = equityResult ?: EquityCalculationResult(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)

        val currentAssets = eq.cashInHandAndDrawer + eq.bankAndMadaBalances + eq.inventoryValuationAtCost + eq.customerReceivables
        val nonCurrentAssets = eq.totalFixedAssetsValue + eq.totalLeaseholdGoodwillValue
        val totalAssets = currentAssets + nonCurrentAssets

        val totalLiabilities = eq.supplierPayables
        val totalEquity = eq.currentAffectedCapital
        val totalLiabilitiesAndEquity = totalLiabilities + totalEquity

        return BalanceSheetReport(
            currentAssetsCashInDrawer = eq.cashInHandAndDrawer,
            currentAssetsBankBalances = eq.bankAndMadaBalances,
            currentAssetsInventoryValuation = eq.inventoryValuationAtCost,
            currentAssetsReceivables = eq.customerReceivables,
            totalCurrentAssets = currentAssets,
            fixedAssetsTotal = eq.totalFixedAssetsValue,
            leaseholdGoodwillTotal = eq.totalLeaseholdGoodwillValue,
            totalNonCurrentAssets = nonCurrentAssets,
            totalAssets = totalAssets,
            supplierPayables = eq.supplierPayables,
            totalLiabilities = totalLiabilities,
            fixedOpeningCapital = eq.fixedOpeningCapital,
            additionalCapitalDeposits = eq.totalAdditionalCapitalDeposits,
            ownerDrawings = eq.totalOwnerDrawings,
            netRetainedOperatingProfit = eq.netOperatingProfit,
            currentAffectedCapital = eq.currentAffectedCapital,
            totalEquity = totalEquity,
            totalLiabilitiesAndEquity = totalLiabilitiesAndEquity
        )
    }

    /**
     * توليد تقرير ميزان المراجعة المحاسبي (Trial Balance Report)
     */
    fun generateTrialBalanceReport(
        financialAccounts: List<FinancialAccountEntity>,
        parties: List<PartyEntity>,
        pnlReport: ProfitAndLossReport?,
        equityResult: EquityCalculationResult?
    ): TrialBalanceReport {
        val items = mutableListOf<TrialBalanceItem>()

        val eq = equityResult ?: EquityCalculationResult(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)

        // 1. الحسابات المالية (البنوك، الصناديق، المحافظ)
        financialAccounts.forEach { acc ->
            val bal = acc.currentBalance
            if (abs(bal) > 0.001) {
                if (bal >= 0) {
                    items.add(TrialBalanceItem(acc.code, acc.name, acc.accountType.labelArabic, debit = bal, credit = 0.0))
                } else {
                    items.add(TrialBalanceItem(acc.code, acc.name, acc.accountType.labelArabic, debit = 0.0, credit = abs(bal)))
                }
            }
        }

        // 2. العملاء (أرصدة مدينة)
        val customerReceivables = parties.filter { it.type == PartyType.CUSTOMER || it.type == PartyType.BOTH }.sumOf { if (it.currentBalance > 0) it.currentBalance else 0.0 }
        if (customerReceivables > 0.001) {
            items.add(TrialBalanceItem("10300", "حسابات العملاء (ديون الشكك)", "أصل متداول", debit = customerReceivables, credit = 0.0))
        }

        // 3. الموردين (أرصدة دائنة)
        val supplierPayables = parties.filter { it.type == PartyType.SUPPLIER || it.type == PartyType.BOTH }.sumOf { if (it.currentBalance < 0) abs(it.currentBalance) else 0.0 }
        if (supplierPayables > 0.001) {
            items.add(TrialBalanceItem("20100", "حسابات الموردين والالتزامات", "التزام متداول", debit = 0.0, credit = supplierPayables))
        }

        // 4. تقييم المخزون (بضاعة آخر المدة)
        if (eq.inventoryValuationAtCost > 0.001) {
            items.add(TrialBalanceItem("10400", "تقييم مخزون البضاعة بسعر التكلفة", "أصل متداول", debit = eq.inventoryValuationAtCost, credit = 0.0))
        }

        // 5. الأصول الثابتة ونقل القدم
        if (eq.totalFixedAssetsValue > 0.001) {
            items.add(TrialBalanceItem("10500", "إجمالي قيمة الأصول الثابتة", "أصل غير متداول", debit = eq.totalFixedAssetsValue, credit = 0.0))
        }
        if (eq.totalLeaseholdGoodwillValue > 0.001) {
            items.add(TrialBalanceItem("10600", "حقوق الخلو ونقل القدم (أصل غير ملموس)", "أصل غير متداول", debit = eq.totalLeaseholdGoodwillValue, credit = 0.0))
        }

        // 6. المبيعات والتكلفة والمصروفات
        if (pnlReport != null) {
            if (pnlReport.netSalesRevenue > 0.001) {
                items.add(TrialBalanceItem("40100", "إيرادات المبيعات المحققة", "إيرادات", debit = 0.0, credit = pnlReport.netSalesRevenue))
            }
            if (pnlReport.cogs > 0.001) {
                items.add(TrialBalanceItem("50100", "تكلفة البضاعة المباعة (COGS)", "تكاليف", debit = pnlReport.cogs, credit = 0.0))
            }
            if (pnlReport.totalOperatingExpenses > 0.001) {
                items.add(TrialBalanceItem("50200", "إجمالي المصروفات والنثريات التشغيلية", "مصروفات", debit = pnlReport.totalOperatingExpenses, credit = 0.0))
            }
        }

        // 7. رأس المال الافتتاحي وحركات المالك
        if (eq.fixedOpeningCapital > 0.001) {
            items.add(TrialBalanceItem("30100", "رأس المال الافتتاحي الثابت", "حقوق ملكية", debit = 0.0, credit = eq.fixedOpeningCapital))
        }
        if (eq.totalAdditionalCapitalDeposits > 0.001) {
            items.add(TrialBalanceItem("30200", "إيداعات رأس المال الإضافية", "حقوق ملكية", debit = 0.0, credit = eq.totalAdditionalCapitalDeposits))
        }
        if (eq.totalOwnerDrawings > 0.001) {
            items.add(TrialBalanceItem("30300", "مسحوبات المالك الشخصية", "حقوق ملكية", debit = eq.totalOwnerDrawings, credit = 0.0))
        }

        val totalDebit = items.sumOf { it.debit }
        val totalCredit = items.sumOf { it.credit }
        val isBalanced = abs(totalDebit - totalCredit) < 1.0

        return TrialBalanceReport(
            items = items,
            totalDebit = totalDebit,
            totalCredit = totalCredit,
            isBalanced = isBalanced
        )
    }
}
