package com.example.dokkani.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * جدول العملات وأسعار الصرف (Currencies)
 * يدعم العملة الأساسية للبقالة (مثلاً ريال سعودي SAR) والعملات الأخرى مع أسعار الصرف.
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
    val code: String,                     // كود العملة (SAR, EGP, USD, YER, AED...)
    val name: String,                     // اسم العملة بالعربية (ريال سعودي، جنيه مصري، دولار أمريكي...)
    val symbol: String,                   // رمز العملة (ر.س، ج.م، $...)
    val exchangeRateToBase: Double = 1.0, // سعر الصرف مقابل العملة الأساسية (العملة الأساسية = 1.0)
    val isBaseCurrency: Boolean = false,  // هل هي العملة الأساسية للنظام المحاسبي
    val isDefault: Boolean = false        // هل هي العملة الافتراضية للفواتير
)
