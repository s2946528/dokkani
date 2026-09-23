package com.example.dokkani.data.local.entities

/**
 * نوع التوظيف والأجر
 */
enum class EmploymentType(val labelArabic: String) {
    MONTHLY_SALARY("راتب شهري ثابت"),
    DAILY_WAGE("أجر يومي")
}

/**
 * حالة الحضور اليومي
 */
enum class AttendanceStatus(val labelArabic: String) {
    PRESENT("حاضر"),
    ABSENT("غائب"),
    HALF_DAY("نصف يوم"),
    OVERTIME("حاضر + إضافي"),
    OFF("إجازة / راحة")
}

/**
 * نوع الحركة المالية للموظف (سلفة، مكافأة، جزاء، إلخ)
 */
enum class EmployeeTransactionType(val labelArabic: String) {
    ADVANCE("سلفة / مسحوبات"),
    BONUS("مكافأة / حافز"),
    PENALTY("جزاء / خصم إداري"),
    DAILY_WAGE_PAYOUT("صرف أجر يومي"),
    SALARY_PAYMENT("صرف صافي راتب")
}

/**
 * حالة مسير الراتب الشهري
 */
enum class PayrollStatus(val labelArabic: String) {
    UNPAID("غير مسدد"),
    PAID("مسدد بالكامل"),
    PARTIALLY_PAID("مسدد جزئياً")
}
