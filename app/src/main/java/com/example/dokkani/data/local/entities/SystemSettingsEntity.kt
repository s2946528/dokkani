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
    val storeAddress: String = "صنعاء - شارع الزبيري", // عنوان البقالة أو المنشأة
    val storePhone: String = "777000111",                 // هاتف المنشأة
    val taxNumber: String = "",                           // الرقم الضريبي للمنشأة (وفقاً لنظام وضوابط الدولة)
    val showPreviousBalanceOnInvoice: Boolean = true,     // خيار تفعيل/إلغاء إظهار الرصيد السابق في طباعة الفواتير الآجلة
    val costValuationMethod: CostValuationMethod = CostValuationMethod.WAC, // طريقة التقييم المحاسبي المعتمدة
    val defaultCurrencyCode: String = "SAR",               // العملة الافتراضية للفواتير
    val isTaxEnabled: Boolean = false,                     // تفعيل / إلغاء حساب الضريبة في المبيعات (معطلة افتراضياً وفق بيئة العمل بالجمهورية اليمنية)
    val defaultTaxRate: Double = 0.0,                      // نسبة ضريبة المبيعات الافتراضية (0.0% افتراضياً)
    val isPurchaseTaxEnabled: Boolean = false,             // تفعيل / إلغاء حساب ضريبة المشتريات بشكل مستقل (معطلة افتراضياً)
    val purchaseTaxRate: Double = 0.0,                     // نسبة ضريبة المشتريات الافتراضية (0.0% افتراضياً)
    val enableProduceShrinkageTracking: Boolean = true,    // تفعيل احتساب الهدر والتالف للخضار المشكل
    val enableNegativeStock: Boolean = false,              // السماح بالبيع بالسالب في حال تأخر إدخال الفواتير
    val invoiceFooterText: String = "شكراً لزيارتكم دكاني - تسوقكم يسعدنا!", // رسالة ذيل الفاتورة
    val lastUpdated: Long = System.currentTimeMillis()     // وقت آخر تعديل للإعدادات
)
