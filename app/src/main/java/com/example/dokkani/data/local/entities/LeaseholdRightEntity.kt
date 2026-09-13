package com.example.dokkani.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * كيان "نقل القدم / الخلو" وحقوق الإيجار كأصل غير ملموس (Intangible Asset / Goodwill)
 * يدعم:
 * 1- احتساب مبلغ الخلو ضمن أصول التأسيس لرأس المال الافتتاحي.
 * 2- الإطفاء الدوري (Amortization) المحمل على الأرباح التشغيلية عبر سنوات العقد.
 * 3- إعادة البيع والتنازل وتحديد الأرباح/الخسائر الرأسمالية.
 */
@Entity(
    tableName = "leasehold_rights",
    indices = [
        Index(value = ["code"], unique = true),
        Index(value = ["status"])
    ]
)
data class LeaseholdRightEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val code: String,                            // كود السند (مثال: GW-2026-0001)
    val name: String,                            // اسم/وصف نقل القدم (مثال: خلو موقع بقالة شارع العليا)
    val initialCost: Double,                     // مبلغ الخلو الأصلي المسدد عند التأسيس
    val currentBookValue: Double,                // القيمة الدفترية المتبقية بعد خصم الإطفاء المتراكم
    val accumulatedAmortization: Double = 0.0,   // مجمع الإطفاء المتراكم حتى تاريخه
    val contractStartDate: Long = System.currentTimeMillis(), // تاريخ بداية العقد/التأسيس
    val contractDurationYears: Int = 5,          // عدد سنوات عقد الإيجار/الخلو لإحتساب الإطفاء
    val status: String = "ACTIVE",              // حالة الأصل: ACTIVE, FULLY_AMORTIZED, SOLD_TRANSFERRED
    val notes: String = ""                       // ملاحظات تفصيلية وعنوان موقع البقالة
)
