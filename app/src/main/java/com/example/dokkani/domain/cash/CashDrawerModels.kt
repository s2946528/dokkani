package com.example.dokkani.domain.cash

/**
 * حالة مطابقة النقدية في الدرج
 */
enum class CashDiscrepancyType(val labelArabic: String) {
    MATCHED("مطابق تماماً"),
    SHORTAGE("عجز نقدي في الصندوق"),
    SURPLUS("زيادة نقدية في الصندوق")
}

/**
 * نتيجة مطابقة الدرج وإغلاق الشفت
 */
data class CashReconciliationResult(
    val openingCash: Double,
    val cashSalesCount: Int,
    val totalCashSales: Double,
    val cashCollectionsCount: Int,
    val totalCashCollections: Double,
    val cashExpensesCount: Int,
    val totalCashExpenses: Double,
    val expectedCashInDrawer: Double,
    val actualPhysicalCash: Double,
    val discrepancy: Double, // actual - expected
    val discrepancyType: CashDiscrepancyType
)

/**
 * تصنيفات المصروفات التشغيلية الشائعة في البقالات
 */
object ExpenseCategories {
    val ALL = listOf(
        "كهرباء ومياه",
        "إيجار المحل",
        "نظافة ومستلزمات وأكياس",
        "بوفية وضيافة",
        "رواتب وعمالة",
        "صيانة وثلاجات",
        "مسحوبات المالك",
        "نثريات متنوعة"
    )
}

/**
 * محرك مطابقة حركة الصندوق وإغلاق الشفت
 */
object CashDrawerEngine {

    fun calculateReconciliation(
        openingCash: Double,
        cashSales: List<Double>,
        cashCollections: List<Double>,
        cashExpenses: List<Double>,
        actualPhysicalCash: Double
    ): CashReconciliationResult {
        val totalSales = cashSales.sum()
        val totalCollections = cashCollections.sum()
        val totalExpenses = cashExpenses.sum()

        val expected = (openingCash + totalSales + totalCollections - totalExpenses).coerceAtLeast(0.0)
        val diff = actualPhysicalCash - expected

        val type = when {
            kotlin.math.abs(diff) < 0.01 -> CashDiscrepancyType.MATCHED
            diff < 0 -> CashDiscrepancyType.SHORTAGE
            else -> CashDiscrepancyType.SURPLUS
        }

        return CashReconciliationResult(
            openingCash = openingCash,
            cashSalesCount = cashSales.size,
            totalCashSales = totalSales,
            cashCollectionsCount = cashCollections.size,
            totalCashCollections = totalCollections,
            cashExpensesCount = cashExpenses.size,
            totalCashExpenses = totalExpenses,
            expectedCashInDrawer = expected,
            actualPhysicalCash = actualPhysicalCash,
            discrepancy = diff,
            discrepancyType = type
        )
    }
}
