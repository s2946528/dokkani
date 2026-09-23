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
 * حالة تسوية الفروقات والجرد من مدير النظام
 */
enum class ShiftSettlementStatus(val labelArabic: String) {
    UNSETTLED("في انتظار التسوية"),
    SETTLED_EXPENSE("تمت التسوية (تحويل العجز لمصروف تشغيلي)"),
    WAIVED("تمت التسوية (إلغاء العجز/اكتشاف سند غير مقيد)"),
    SETTLED_STAFF("تمت التسوية (خصم العجز كعهدة على الكاشير)"),
    SETTLED_SURPLUS("تمت التسوية (تقييد الزيادة كإيراد صندوق متنوع)")
}

/**
 * نتيجة مطابقة الدرج وإغلاق الشفت - تقرير Z التفصيلي المحاسبي
 */
data class CashReconciliationResult(
    val openingCash: Double,                     // مبلغ العهدة الافتتاحية (رصيد بداية الشفت)
    
    // تفاصيل بنود الواردات النقدية (Cash Inflows)
    val cashSalesCount: Int = 0,
    val totalCashSales: Double = 0.0,            // مبلغ المبيعات النقدية
    val cashCollectionsCount: Int = 0,
    val totalCashCollections: Double = 0.0,      // مبلغ سندات القبض النقدية من العملاء
    val totalInflows: Double = totalCashSales + totalCashCollections, // إجمالي الدخل والوارد النقدي
    
    // تفاصيل بنود الصادرات النقدية (Cash Outflows)
    val cashExpensesCount: Int = 0,
    val totalCashExpenses: Double = 0.0,         // مبلغ المصروفات النقدية التشغيلية
    val supplierPaymentsCount: Int = 0,
    val totalSupplierPayments: Double = 0.0,     // مبلغ تسديد الموردين نقداً (سندات الصرف)
    val cashPurchasesCount: Int = 0,
    val totalCashPurchases: Double = 0.0,        // مبلغ المشتريات النقدية المسددة كاش
    val ownerDrawingsCount: Int = 0,
    val totalOwnerDrawings: Double = 0.0,        // مبلغ مسحوبات صاحب البقالة النقدية
    val staffAdvancesCount: Int = 0,
    val totalStaffAdvances: Double = 0.0,        // مبلغ سلف ومسحوبات العمال والموظفين
    val totalOutflows: Double = totalCashExpenses + totalSupplierPayments + totalCashPurchases + totalOwnerDrawings + totalStaffAdvances, // إجمالي الخرج الصادر

    // الخلاصة والنتيجة النهائية للدرج
    val expectedCashInDrawer: Double,           // صافي النقدية المتوقعة = (العهدة الافتتاحية + إجمالي الدخل - إجمالي الخرج)
    val actualPhysicalCash: Double,              // النقدية الفعلية المجرودة باليد داخل الدرج
    val discrepancy: Double,                     // فارق الجرد (الفعلية - المتوقعة)
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
        "سلف ومسحوبات عمال",
        "مسحوبات المالك",
        "صيانة وثلاجات",
        "نثريات وفروقات درج",
        "نثريات متنوعة"
    )
    const val OWNER_DRAWINGS = "مسحوبات المالك"
    const val STAFF_ADVANCES = "سلف ومسحوبات عمال"
    const val SALARIES = "رواتب وعمالة"
    const val CASH_SHORTAGE = "نثريات وفروقات درج"
}

/**
 * محرك مطابقة حركة الصندوق وإغلاق الشفت (Z-Report Engine)
 */
object CashDrawerEngine {

    fun calculateReconciliation(
        openingCash: Double,
        cashSales: List<Double>,
        cashCollections: List<Double>,
        cashExpenses: List<Double>,
        supplierPayments: List<Double> = emptyList(),
        cashPurchases: List<Double> = emptyList(),
        ownerDrawings: List<Double> = emptyList(),
        staffAdvances: List<Double> = emptyList(),
        actualPhysicalCash: Double
    ): CashReconciliationResult {
        val totalSales = cashSales.sum()
        val totalCollections = cashCollections.sum()
        val totalExpenses = cashExpenses.sum()
        val totalSuppliers = supplierPayments.sum()
        val totalPurchases = cashPurchases.sum()
        val totalOwner = ownerDrawings.sum()
        val totalStaff = staffAdvances.sum()

        val inflows = totalSales + totalCollections
        val outflows = totalExpenses + totalSuppliers + totalPurchases + totalOwner + totalStaff

        val expected = (openingCash + inflows - outflows).coerceAtLeast(0.0)
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
            totalInflows = inflows,
            cashExpensesCount = cashExpenses.size,
            totalCashExpenses = totalExpenses,
            supplierPaymentsCount = supplierPayments.size,
            totalSupplierPayments = totalSuppliers,
            cashPurchasesCount = cashPurchases.size,
            totalCashPurchases = totalPurchases,
            ownerDrawingsCount = ownerDrawings.size,
            totalOwnerDrawings = totalOwner,
            staffAdvancesCount = staffAdvances.size,
            totalStaffAdvances = totalStaff,
            totalOutflows = outflows,
            expectedCashInDrawer = expected,
            actualPhysicalCash = actualPhysicalCash,
            discrepancy = diff,
            discrepancyType = type
        )
    }

    fun calculateAvailableCash(
        openingCash: Double,
        cashSales: List<Double>,
        cashCollections: List<Double>,
        cashExpenses: List<Double>,
        supplierPayments: List<Double> = emptyList(),
        cashPurchases: List<Double> = emptyList(),
        ownerDrawings: List<Double> = emptyList(),
        staffAdvances: List<Double> = emptyList()
    ): Double {
        val inflows = cashSales.sum() + cashCollections.sum()
        val outflows = cashExpenses.sum() + supplierPayments.sum() + cashPurchases.sum() + ownerDrawings.sum() + staffAdvances.sum()
        return openingCash + inflows - outflows
    }
}
