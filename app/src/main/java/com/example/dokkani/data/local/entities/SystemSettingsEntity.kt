package com.example.dokkani.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * جدول إعدادات النظام (System_Settings)
 * يخزن إعدادات برنامج "دكاني"، وبشكل أساسي طريقة تقييم التكلفة المحاسبية المعتمدة
 * (المتوسط المرجح WAC / الوارد أولاً صادر أولاً FIFO / آخر سعر شراء Last Purchase Price)
 */
@Entity(tableName = "system_settings")
data class SystemSettingsEntity(
    @PrimaryKey
    val id: Int = 1,                                       // سجل وحيد للإعدادات العامة للنظام
    val storeName: String = "دكاني - تموينات ومخضار السعادة", // اسم البقالة أو المتجر
    val costValuationMethod: CostValuationMethod = CostValuationMethod.WAC, // طريقة التقييم المحاسبي المعتمدة
    val defaultCurrencyCode: String = "SAR",               // العملة الافتراضية للفواتير
    val defaultTaxRate: Double = 0.15,                     // نسبة الضريبة الافتراضية (15% ضريبة القيمة المضافة)
    val enableProduceShrinkageTracking: Boolean = true,    // تفعيل احتساب الهدر والتالف للخضار المشكل
    val enableNegativeStock: Boolean = false,              // السماح بالبيع بالسالب في حال تأخر إدخال الفواتير
    val invoiceFooterText: String = "شكراً لزيارتكم دكاني - تسوقكم يسعدنا!", // رسالة ذيل الفاتورة
    val lastUpdated: Long = System.currentTimeMillis()     // وقت آخر تعديل للإعدادات
)
