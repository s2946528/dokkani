package com.example.dokkani.domain.reports

import com.example.dokkani.data.local.entities.CostValuationMethod
import com.example.dokkani.data.local.entities.ExpenseEntity
import com.example.dokkani.data.local.entities.InvoiceEntity
import com.example.dokkani.data.local.entities.InvoiceItemEntity
import com.example.dokkani.data.local.entities.InvoiceType
import com.example.dokkani.data.local.entities.PaymentMethod
import com.example.dokkani.data.local.entities.ProductWithUnits
import com.example.dokkani.data.local.entities.StockMovementEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
        val totalMadaSales = saleInvoices.filter { it.paymentMethod == PaymentMethod.MADA }.sumOf { it.total }
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

        val totalExpenses = expenses.sumOf { it.amount }
        val expensesByCategory = expenses.groupBy { it.category }
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
}
