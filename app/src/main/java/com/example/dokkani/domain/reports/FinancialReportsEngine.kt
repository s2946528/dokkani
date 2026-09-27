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
import com.example.dokkani.data.local.entities.FinancialAccountType
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
        valuationMethod: CostValuationMethod,
        costCenterId: Long? = null
    ): ProfitAndLossReport {
        // تصفية الفواتير والمصروفات حسب مركز التكلفة المختار (إن وجد)
        val filteredInvoices = if (costCenterId != null) {
            invoices.filter { it.costCenterId == costCenterId }
        } else invoices

        val filteredExpenses = if (costCenterId != null) {
            expenses.filter { it.costCenterId == costCenterId }
        } else expenses

        val saleInvoices = filteredInvoices.filter { it.type == InvoiceType.SALE }

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
        val operationalExpenses = filteredExpenses.filter { exp ->
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
     * توليد تقرير ميزان المراجعة المحاسبي (Trial Balance Report) مع فحص التدقيق الآلي
     */
    fun generateTrialBalanceReport(
        financialAccounts: List<FinancialAccountEntity>,
        parties: List<PartyEntity>,
        pnlReport: ProfitAndLossReport?,
        equityResult: EquityCalculationResult?,
        costCenterId: Long? = null
    ): TrialBalanceReport {
        val rawItems = mutableListOf<TrialBalanceItem>()

        val eq = equityResult ?: EquityCalculationResult(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)

        // 1. الحسابات المالية (البنوك، الصناديق، المحافظ، وحسابات حقوق الملكية بالدليل)
        financialAccounts.forEach { acc ->
            // استثناء حساب 30100 ليتم إدراجه موحداً مع رأس المال الافتتاحي لمنع التكرار والإدخالات المزدوجة
            if (acc.code == "30100") return@forEach

            val bal = acc.currentBalance
            if (abs(bal) > 0.001) {
                val isCreditNatural = acc.code.startsWith("2") || acc.code.startsWith("3") || acc.code.startsWith("4") || acc.accountType == FinancialAccountType.LIABILITY
                if (isCreditNatural) {
                    if (bal >= 0) {
                        rawItems.add(TrialBalanceItem(acc.code, acc.name, acc.accountType.labelArabic, debit = 0.0, credit = bal))
                    } else {
                        rawItems.add(TrialBalanceItem(acc.code, acc.name, acc.accountType.labelArabic, debit = abs(bal), credit = 0.0))
                    }
                } else {
                    if (bal >= 0) {
                        rawItems.add(TrialBalanceItem(acc.code, acc.name, acc.accountType.labelArabic, debit = bal, credit = 0.0))
                    } else {
                        rawItems.add(TrialBalanceItem(acc.code, acc.name, acc.accountType.labelArabic, debit = 0.0, credit = abs(bal)))
                    }
                }
            }
        }

        // 2. العملاء (أرصدة مدينة - أصل متداول)
        val customerReceivables = parties.filter { it.type == PartyType.CUSTOMER || it.type == PartyType.BOTH }.sumOf { if (it.currentBalance > 0) it.currentBalance else 0.0 }
        if (customerReceivables > 0.001) {
            rawItems.add(TrialBalanceItem("10300", "10300 - حسابات العملاء (ديون الشكك)", "أصل متداول", debit = customerReceivables, credit = 0.0))
        }

        // 3. الموردين (أرصدة دائنة - التزام متداول)
        val supplierPayables = parties.filter { it.type == PartyType.SUPPLIER || it.type == PartyType.BOTH }.sumOf { if (it.currentBalance < 0) abs(it.currentBalance) else 0.0 }
        if (supplierPayables > 0.001) {
            rawItems.add(TrialBalanceItem("20100", "20100 - حسابات الموردين والالتزامات", "التزام متداول", debit = 0.0, credit = supplierPayables))
        }

        // 4. تقييم المخزون (بضاعة آخر المدة - أصل متداول)
        if (eq.inventoryValuationAtCost > 0.001) {
            rawItems.add(TrialBalanceItem("10400", "10400 - تقييم مخزون البضاعة بسعر التكلفة", "أصل متداول", debit = eq.inventoryValuationAtCost, credit = 0.0))
        }

        // 5. الأصول الثابتة ونقل القدم (أصول غير متداولة)
        if (eq.totalFixedAssetsValue > 0.001) {
            rawItems.add(TrialBalanceItem("10500", "10500 - إجمالي قيمة الأصول الثابتة", "أصل غير متداول", debit = eq.totalFixedAssetsValue, credit = 0.0))
        }
        if (eq.totalLeaseholdGoodwillValue > 0.001) {
            rawItems.add(TrialBalanceItem("10600", "10600 - حقوق الخلو ونقل القدم (أصل غير ملموس)", "أصل غير متداول", debit = eq.totalLeaseholdGoodwillValue, credit = 0.0))
        }

        // 6. المبيعات والتكلفة والمصروفات
        if (pnlReport != null) {
            if (pnlReport.netSalesRevenue > 0.001) {
                rawItems.add(TrialBalanceItem("40100", "40100 - إيرادات المبيعات المحققة", "إيرادات", debit = 0.0, credit = pnlReport.netSalesRevenue))
            }
            if (pnlReport.cogs > 0.001) {
                rawItems.add(TrialBalanceItem("50100", "50100 - تكلفة البضاعة المباعة (COGS)", "تكاليف", debit = pnlReport.cogs, credit = 0.0))
            }
            if (pnlReport.totalOperatingExpenses > 0.001) {
                rawItems.add(TrialBalanceItem("50200", "50200 - إجمالي المصروفات والنثريات التشغيلية", "مصروفات", debit = pnlReport.totalOperatingExpenses, credit = 0.0))
            }
        }

        // 7. حقوق الملكية ورأس المال الافتتاحي (دائنة حصرياً بحسب طبيعتها المحاسبية)
        if (eq.fixedOpeningCapital > 0.001) {
            rawItems.add(TrialBalanceItem("30100", "30100 - رأس المال الافتتاحي الثابت", "حقوق ملكية", debit = 0.0, credit = eq.fixedOpeningCapital))
        }
        if (eq.totalAdditionalCapitalDeposits > 0.001) {
            rawItems.add(TrialBalanceItem("30200", "30200 - إيداعات رأس المال الإضافية", "حقوق ملكية", debit = 0.0, credit = eq.totalAdditionalCapitalDeposits))
        }
        if (eq.totalOwnerDrawings > 0.001) {
            rawItems.add(TrialBalanceItem("30300", "30300 - مسحوبات المالك الشخصية", "حقوق ملكية", debit = eq.totalOwnerDrawings, credit = 0.0))
        }

        // --- التجميع الآلي بالحساب الواحد (GROUP BY account_code) لمنع التكرار أو ظهور الحساب في المدين والدائن معاً ---
        val aggregatedMap = LinkedHashMap<String, TrialBalanceItem>()
        rawItems.forEach { item ->
            val existing = aggregatedMap[item.accountCode]
            if (existing == null) {
                aggregatedMap[item.accountCode] = item
            } else {
                aggregatedMap[item.accountCode] = existing.copy(
                    debit = existing.debit + item.debit,
                    credit = existing.credit + item.credit
                )
            }
        }

        // تسوية كل حساب ليكون إما دائن فقط أو مدين فقط بالجانب الطبيعي حصرياً
        val finalItems = aggregatedMap.values.map { item ->
            val code = item.accountCode
            val isCreditNatural = code.startsWith("2") || code.startsWith("3") || code.startsWith("4") || item.categoryLabel.contains("حقوق") || item.categoryLabel.contains("التزام") || item.categoryLabel.contains("إيراد")

            if (isCreditNatural) {
                val netCredit = item.credit - item.debit
                if (netCredit >= 0) {
                    item.copy(debit = 0.0, credit = netCredit)
                } else {
                    item.copy(debit = abs(netCredit), credit = 0.0)
                }
            } else {
                val netDebit = item.debit - item.credit
                if (netDebit >= 0) {
                    item.copy(debit = netDebit, credit = 0.0)
                } else {
                    item.copy(debit = 0.0, credit = abs(netDebit))
                }
            }
        }.sortedBy { it.accountCode }

        val totalDebit = finalItems.sumOf { it.debit }
        val totalCredit = finalItems.sumOf { it.credit }
        val diff = abs(totalDebit - totalCredit)
        val isBalanced = diff < 1.0

        val unbalanced = if (!isBalanced) {
            finalItems.filter { abs(it.debit - it.credit) > 0.01 }
        } else emptyList()

        val auditMessage = if (isBalanced) {
            "نظام التدقيق المحاسبي الآلي: ميزان المراجعة متوازن ومطابق تماماً للمواصفات المعيارية (إجمالي المدين = إجمالي الدائن) ✓"
        } else {
            "تنبيه عدم توازن في ميزان المراجعة! الفرق الحسابي قدره %.2f ر.ي بين إجمالي المدين (%.2f) وإجمالي الدائن (%.2f)".format(diff, totalDebit, totalCredit)
        }

        return TrialBalanceReport(
            items = finalItems,
            totalDebit = totalDebit,
            totalCredit = totalCredit,
            isBalanced = isBalanced,
            differenceAmount = diff,
            unbalancedAccounts = unbalanced,
            auditCheckMessage = auditMessage
        )
    }

    /**
     * توليد تقرير حركة المخزون الشامل (Stock Movement Report)
     */
    fun generateStockMovementReport(
        productsWithUnits: List<ProductWithUnits>,
        stockMovements: List<StockMovementEntity>,
        valuationMethod: CostValuationMethod,
        costCenterId: Long? = null,
        costCenterName: String = "جميع مراكز التكلفة"
    ): StockMovementReport {
        val filteredMovements = if (costCenterId != null) {
            stockMovements.filter { it.costCenterId == costCenterId }
        } else {
            stockMovements
        }

        val movementsByProduct = filteredMovements.groupBy { it.productId }
        val itemsList = mutableListOf<ProductStockMovementItem>()

        for (pwu in productsWithUnits) {
            val prod = pwu.product
            val baseUnit = pwu.units.firstOrNull { it.isBaseUnit } ?: pwu.units.firstOrNull()
            val baseUnitName = baseUnit?.unitName ?: "حبة"
            val standardUnitCost = baseUnit?.costPrice ?: 0.0

            val pMovements = movementsByProduct[prod.id] ?: emptyList()

            // Purchases & Sorted Produce
            val purchasesQty = pMovements
                .filter { it.movementType == com.example.dokkani.data.local.entities.MovementType.PURCHASE_IN || it.movementType == com.example.dokkani.data.local.entities.MovementType.PRODUCE_SORTING }
                .sumOf { it.quantityBaseUnit }

            // Purchase Returns
            val purchaseReturnsQty = pMovements
                .filter { it.movementType == com.example.dokkani.data.local.entities.MovementType.RETURN_OUT }
                .sumOf { abs(it.quantityBaseUnit) }

            // Sales Out
            val salesQty = pMovements
                .filter { it.movementType == com.example.dokkani.data.local.entities.MovementType.SALE_OUT }
                .sumOf { abs(it.quantityBaseUnit) }

            // Sale Returns In
            val saleReturnsQty = pMovements
                .filter { it.movementType == com.example.dokkani.data.local.entities.MovementType.RETURN_IN }
                .sumOf { it.quantityBaseUnit }

            // Wastage & Shortage
            val wastageAndShortageQty = pMovements
                .filter { it.movementType == com.example.dokkani.data.local.entities.MovementType.WASTAGE_OUT || (it.movementType == com.example.dokkani.data.local.entities.MovementType.INVENTORY_ADJUSTMENT && it.quantityBaseUnit < 0) }
                .sumOf { abs(it.quantityBaseUnit) }

            // Closing stock net qty
            val closingStockQty = pMovements.sumOf { it.quantityBaseUnit }

            // Opening stock qty
            val netActivity = purchasesQty - purchaseReturnsQty - salesQty + saleReturnsQty - wastageAndShortageQty
            val openingStockQty = (closingStockQty - netActivity).coerceAtLeast(0.0)

            // Effective cost per unit according to valuation method
            val effectiveUnitCost = when (valuationMethod) {
                CostValuationMethod.WAC -> {
                    val purchaseLots = pMovements.filter { it.movementType == com.example.dokkani.data.local.entities.MovementType.PURCHASE_IN || it.movementType == com.example.dokkani.data.local.entities.MovementType.PRODUCE_SORTING }
                    val totalPurchaseCost = purchaseLots.sumOf { it.quantityBaseUnit * it.unitCostPriceBase }
                    val totalPurchaseQty = purchaseLots.sumOf { it.quantityBaseUnit }
                    if (totalPurchaseQty > 0.0001) totalPurchaseCost / totalPurchaseQty else standardUnitCost
                }
                CostValuationMethod.FIFO -> {
                    val activeFifoLot = pMovements.firstOrNull { it.remainingQuantityForFifo > 0.0001 }
                    activeFifoLot?.unitCostPriceBase ?: standardUnitCost
                }
                CostValuationMethod.LAST_PURCHASE_PRICE -> {
                    val lastPurchase = pMovements.lastOrNull { it.movementType == com.example.dokkani.data.local.entities.MovementType.PURCHASE_IN || it.movementType == com.example.dokkani.data.local.entities.MovementType.PRODUCE_SORTING }
                    lastPurchase?.unitCostPriceBase ?: standardUnitCost
                }
                CostValuationMethod.LIFO -> {
                    val lastPurchase = pMovements.lastOrNull { it.movementType == com.example.dokkani.data.local.entities.MovementType.PURCHASE_IN || it.movementType == com.example.dokkani.data.local.entities.MovementType.PRODUCE_SORTING }
                    lastPurchase?.unitCostPriceBase ?: standardUnitCost
                }
            }

            val finalUnitCost = if (effectiveUnitCost > 0.0) effectiveUnitCost else standardUnitCost
            val closingStockValue = closingStockQty * finalUnitCost
            val cogs = (salesQty - saleReturnsQty).coerceAtLeast(0.0) * finalUnitCost

            itemsList.add(
                ProductStockMovementItem(
                    productId = prod.id,
                    productCode = prod.code,
                    productName = prod.name,
                    category = prod.category,
                    baseUnitName = baseUnitName,
                    openingStockQty = openingStockQty,
                    purchasesQty = purchasesQty,
                    purchaseReturnsQty = purchaseReturnsQty,
                    salesQty = salesQty,
                    saleReturnsQty = saleReturnsQty,
                    wastageAndShortageQty = wastageAndShortageQty,
                    closingStockQty = closingStockQty,
                    unitCostPrice = finalUnitCost,
                    closingStockValue = closingStockValue,
                    calculatedCogs = cogs
                )
            )
        }

        return StockMovementReport(
            valuationMethodUsed = valuationMethod,
            items = itemsList,
            totalOpeningStockQty = itemsList.sumOf { it.openingStockQty },
            totalPurchasesQty = itemsList.sumOf { it.purchasesQty },
            totalSalesQty = itemsList.sumOf { it.salesQty },
            totalWastageAndShortageQty = itemsList.sumOf { it.wastageAndShortageQty },
            totalClosingStockQty = itemsList.sumOf { it.closingStockQty },
            totalClosingStockValue = itemsList.sumOf { it.closingStockValue },
            totalCogsValue = itemsList.sumOf { it.calculatedCogs },
            selectedCostCenterName = costCenterName
        )
    }
}
