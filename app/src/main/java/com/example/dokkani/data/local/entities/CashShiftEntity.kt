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
    val openingCash: Double = 200.0,             // عهدة النقدية الافتتاحية في الدرج (الفكة)
    val totalCashSales: Double = 0.0,            // إجمالي المبيعات النقدية (الكاش)
    val totalCashCollections: Double = 0.0,      // إجمالي سندات القبض النقدية من عملاء الدفتر
    val totalCashExpenses: Double = 0.0,         // إجمالي المصروفات التشغيلية النقدية الخارجة من الدرج
    val expectedCashInDrawer: Double = 0.0,      // النقدية الدفترية المتوقعة = الافتتاحية + المبيعات + التحصيل - المصروفات
    val actualPhysicalCash: Double = 0.0,        // النقدية الفعلية المجرودة باليد داخل الدرج
    val cashDiscrepancy: Double = 0.0,           // الفرق (الفعلية - المتوقعة): 0 مطابق، سالب = عجز، موجب = زيادة
    val status: String = "CLOSED",               // حالة الشفت: OPEN أو CLOSED
    val notes: String = ""                       // ملاحظات تسليم الشفت
)
