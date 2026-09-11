package com.example.dokkani.domain.reports

import com.example.dokkani.data.local.entities.CostValuationMethod

/**
 * تقرير الأرباح والخسائر الشامل (P&L Report)
 */
data class ProfitAndLossReport(
    val valuationMethodUsed: CostValuationMethod,
    val totalInvoicesCount: Int,
    val grossSales: Double,                 // إجمالي المبيعات قبل الخصم
    val totalDiscounts: Double,             // إجمالي الخصومات الممنوحة
    val netSalesRevenue: Double,            // صافي الإيرادات بعد الخصم
    val taxCollected: Double,               // ضريبة القيمة المضافة المحصلة
    val totalCashSales: Double,             // مبيعات النقد
    val totalMadaSales: Double,             // مبيعات الشبكة / مدى
    val totalCreditSales: Double,           // مبيعات الآجل (الدفتر)
    val cogs: Double,                       // تكلفة البضاعة المباعة حسب طريقة التقييم (WAC / FIFO / Last Purchase)
    val grossProfit: Double,                // إجمالي الربح التجاري = صافي الإيرادات - تكلفة البضاعة
    val grossProfitMarginPercent: Double,   // هامش الربح الإجمالي %
    val totalOperatingExpenses: Double,     // إجمالي المصروفات التشغيلية
    val expensesByCategory: Map<String, Double>, // توزيع المصروفات حسب البنود
    val netOperatingProfit: Double,         // صافي الربح التشغيلي = إجمالي الربح - المصروفات
    val netProfitMarginPercent: Double,     // نسبة صافي الربح %
    val averageInvoiceValue: Double         // متوسط قيمة الفاتورة
)

/**
 * بند تقرير الأصناف الأكثر حركة وربحية
 */
data class TopProductItem(
    val productId: Long,
    val productName: String,
    val category: String,
    val totalQuantitySold: Double,
    val baseUnitName: String,
    val totalRevenue: Double,
    val totalCost: Double,
    val grossProfit: Double,
    val profitMarginPercent: Double
)

/**
 * تقرير تحليل الأصناف
 */
data class TopProductsReport(
    val topMovingByQuantity: List<TopProductItem>,
    val topProfitableByMargin: List<TopProductItem>
)

/**
 * بند تقرير نواقص المخزون
 */
data class InventoryShortageItem(
    val productId: Long,
    val code: String,
    val name: String,
    val category: String,
    val currentStock: Double,
    val minStockAlert: Double,
    val baseUnitName: String,
    val isOutOfStock: Boolean
)

/**
 * حالة صلاحية المنتج
 */
enum class ExpiryStatus(val labelArabic: String) {
    EXPIRED("منتهي الصلاحية"),
    CRITICAL("حرج (أقل من 7 أيام)"),
    WARNING("قريب (أقل من 30 يوماً)"),
    GOOD("ساري الصلاحية")
}

/**
 * بند تنبيهات تواريخ الصلاحية
 */
data class ExpiryAlertItem(
    val productId: Long,
    val name: String,
    val category: String,
    val expiryDate: Long,
    val expiryDateFormatted: String,
    val daysRemaining: Long,
    val status: ExpiryStatus
)

/**
 * تقرير المخزون والصلاحيات الشامل
 */
data class InventoryHealthReport(
    val lowStockItems: List<InventoryShortageItem>,
    val expiryAlerts: List<ExpiryAlertItem>,
    val totalLowStockCount: Int,
    val totalExpiredOrNearExpiryCount: Int
)
