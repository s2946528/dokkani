package com.example.dokkani.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * جدول المصروفات التشغيلية والنثريات (Operational Expenses)
 * لإدارة مصاريف البقالة والمتجر اليومية والشهرية (كهرباء، إيجار، عمالة، بوفية، نظافة، نثريات...)
 */
@Entity(
    tableName = "expenses",
    indices = [
        Index(value = ["expenseNumber"], unique = true),
        Index(value = ["category"]),
        Index(value = ["date"]),
        Index(value = ["paymentMethod"])
    ]
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val expenseNumber: String,                  // رقم السند (مثل: EXP-2026-0001)
    val category: String,                       // تصنيف المصروف: كهرباء ومياه، إيجار، نظافة ومستلزمات، بوفية وضيافة، رواتب، صيانة، نثريات
    val amount: Double,                         // قيمة المصروف
    val paymentMethod: PaymentMethod = PaymentMethod.CASH, // نقداً من الدرج / شبكة / تحويل بنكي
    val date: Long = System.currentTimeMillis(),// تاريخ ووقت المصروف
    val paidTo: String = "",                    // المدفوع له (الجهة أو الشخص)
    val notes: String = "",                     // البيان والملاحظات
    val recordedBy: String = "كاشير 1"          // المستخدم / الكاشير
)
