package com.example.dokkani.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * جدول جلسات الشفت ومطابقة النقدية في الدرج (Cash Drawer & Shift Reconciliation)
 * يضبط عهدة بداية اليوم والمبيعات النقدية والمصروفات ومطابقة الجرد الفعلي للنقدية
 */
@Entity(
    tableName = "cash_shifts",
    indices = [
        Index(value = ["shiftNumber"], unique = true),
        Index(value = ["startTime"]),
        Index(value = ["status"])
    ]
)
data class CashShiftEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val shiftNumber: String,                     // رقم الشفت (مثل: SHF-2026-0001)
    val cashierName: String = "كاشير 1",         // اسم الكاشير / المسؤول
    val startTime: Long = System.currentTimeMillis(), // وقت بدء الشفت
    val endTime: Long? = null,                   // وقت إغلاق الشفت
    val openingCash: Double = 200.0,             // عهدة النقدية الافتتاحية في الدرج (رصيد بداية الشفت)
    val totalCashSales: Double = 0.0,            // إجمالي المبيعات النقدية (الكاش)
    val totalCashCollections: Double = 0.0,      // إجمالي سندات القبض النقدية من عملاء الدفتر
    val totalCashExpenses: Double = 0.0,         // إجمالي المصروفات التشغيلية النقدية
    val totalSupplierPayments: Double = 0.0,     // إجمالي تسديد الموردين نقداً (سندات الصرف)
    val totalCashPurchases: Double = 0.0,        // إجمالي المشتريات النقدية المسددة من الدرج
    val totalOwnerDrawings: Double = 0.0,        // إجمالي مسحوبات صاحب البقالة النقدية
    val totalStaffAdvances: Double = 0.0,        // إجمالي سلف ومسحوبات العمال والموظفين
    val totalMadaSales: Double = 0.0,            // إجمالي مبيعات البطاقة والشبكة (POS)
    val totalWalletSales: Double = 0.0,          // إجمالي مبيعات المحافظ الإلكترونية
    val totalTransferSales: Double = 0.0,        // إجمالي التحويلات الشواخص البنكية
    val totalCreditSales: Double = 0.0,          // إجمالي المبيعات الآجلة (على الحساب)
    val expectedCashInDrawer: Double = 0.0,      // النقدية الدفترية المتوقعة = (الافتتاحية + الواردات النقدية - الصادرات النقدية)
    val actualPhysicalCash: Double = 0.0,        // النقدية الفعلية المجرودة باليد داخل الدرج
    val cashDiscrepancy: Double = 0.0,           // الفرق (الفعلية - المتوقعة): 0 مطابق، سالب = عجز، موجب = زيادة
    val status: String = "CLOSED",               // حالة الشفت: OPEN, CLOSED, SETTLED
    val notes: String = "",                      // ملاحظات تسليم الشفت
    val settlementStatus: String = "UNSETTLED",   // حالة التسوية للمدير: UNSETTLED, SETTLED_EXPENSE, WAIVED, SETTLED_STAFF, SETTLED_SURPLUS
    val settlementNotes: String = ""             // ملاحظات وبيان تسوية الفروقات من المدير
) {
    val totalInflows: Double
        get() = totalCashSales + totalCashCollections

    val totalOutflows: Double
        get() = totalCashExpenses + totalSupplierPayments + totalCashPurchases + totalOwnerDrawings + totalStaffAdvances
}
