package com.example.dokkani.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * جدول العملات وأسعار الصرف (Currencies)
 * يدعم العملة الأساسية للبقالة/المتجر (مثل: الريال اليمني YER، الريال السعودي SAR...) 
 * والعملات الأجنبية الأخرى مع مراقبة أسعار الصرف بالنسبة للعملة الأساسية.
 */
@Entity(
    tableName = "currencies",
    indices = [
        Index(value = ["code"], unique = true)
    ]
)
data class CurrencyEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val code: String,                     // كود العملة الفريد (مثلاً: YER, SAR, USD, EGP)
    val name: String,                     // اسم العملة باللغة العربية (ريال يمني، ريال سعودي، دولار أمريكي)
    val symbol: String,                   // رمز العملة للعرض في الفواتير والواجهات (ر.ي، ر.س، $)
    val exchangeRateToBase: Double = 1.0, // سعر الصرف مقابل العملة الأساسية (تكون دائماً 1.0 للعملة الأساسية)
    val isBaseCurrency: Boolean = false,  // هل هي العملة الأساسية المعتمدة للحسابات والميزانية
    val isDefault: Boolean = false        // هل هي العملة الافتراضية المحددة حالياً لتسعير الفواتير
)
