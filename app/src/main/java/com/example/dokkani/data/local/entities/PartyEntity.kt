package com.example.dokkani.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * جدول العملاء والموردين (Parties / Contacts)
 * لإدارة الحسابات والديون والأرصدة للبقالات (عملاء الدفتر والموردين كشركات الألبان والمشروبات والخضار)
 */
@Entity(
    tableName = "parties",
    indices = [
        Index(value = ["phone"]),
        Index(value = ["type"])
    ]
)
data class PartyEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,                     // اسم العميل أو المورد أو الشركة
    val type: PartyType,                  // عميل (CUSTOMER)، مورد (SUPPLIER)، أو كلاهما (BOTH)
    val phone: String = "",               // رقم الجوال للتواصل
    val taxNumber: String = "",           // الرقم الضريبي
    val currentBalance: Double = 0.0,     // الرصيد الحالي (موجب = لنا عنده / مدين، سالب = له عندنا / دائن)
    val creditLimit: Double = 0.0,        // سقف الائتمان المسموح به لعميل الحساب/الدفتر
    val address: String = "",             // العنوان أو الموقع
    val notes: String = ""                // ملاحظات محاسبية
)
