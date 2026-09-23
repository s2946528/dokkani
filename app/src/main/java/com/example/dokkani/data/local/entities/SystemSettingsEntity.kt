package com.example.dokkani.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * خيارات نوع وطبيعة كلمة المرور والرمز السري
 */
enum class PasswordType(val labelArabic: String, val description: String) {
    NUMERIC_PIN("أرقام فقط (PIN)", "رمز مكون من أرقام فقط (4 أو 6 أرقام) مع إمكانية الدخول التلقائي بدون زر موافقة"),
    ALPHANUMERIC("أرقام وحروف ورموز (Alphanumeric)", "كلمة مرور مركبة تتكون من أرقام وحروف ورموز مع تحديد الحد الأدنى والقصوى للطول وزر دخول")
}

/**
 * خيارات حساب سعر التكلفة/البيع للعملة الأجنبية
 */
enum class ForeignCurrencyPricingMode(val labelArabic: String, val description: String) {
    SALE_DATE("تاريخ البيع (الافتراضي)", "احتساب سعر الصنف بالعملة المحلية وفقاً بسعر الصرف اليومي السائد عند البيع"),
    PURCHASE_DATE("تاريخ الشراء", "احتساب سعر الصنف بالعملة المحلية وفقاً بسعر الصرف التاريخي المسجل بالفاتورة أثناء الشراء")
}

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
    val defaultCurrencyCode: String = "YER",               // العملة الافتراضية للفواتير
    val foreignCurrencyPricingMode: ForeignCurrencyPricingMode = ForeignCurrencyPricingMode.SALE_DATE, // خيار حساب سعر الصنف الأجنبي (الافتراضي: تاريخ البيع)
    val enableDailyExchangeRatePrompt: Boolean = true,     // جعل شاشة/تحديث الصرف اليومي هو الخيار الافتراضي
    val enableAutoLock: Boolean = true,                    // تفعيل قفل الشاشة التلقائي بعد فترة عدم نشاط
    val autoLockSeconds: Int = 120,                        // المهلة الزمنية قبل قفل الشاشة التلقائي بالثواني (افتراضي 120 ثانية)
    val passwordType: PasswordType = PasswordType.NUMERIC_PIN, // نوع كلمة المرور: أرقام فقط (PIN) أو أرقام وحروف ورموز
    val pinLength: Int = 4,                                // طول الـ PIN عند اختيار أرقام فقط (4 أو 6 أرقام)
    val enableAutoSubmitPin: Boolean = true,               // الدخول التلقائي فور إدخال الرقم السري بدون الحاجة لضغط زر "دخول"
    val minPasswordLength: Int = 8,                        // الحد الأدنى لطول كلمة المرور عند اختيار Alphanumeric (8 خانات)
    val maxPasswordLength: Int = 16,                       // الحد الأقصى لطول كلمة المرور عند اختيار Alphanumeric (16 خانة)
    val initialCapital: Double = 0.0,                      // رأس المال الافتتاحي المعتمد في معالج التهيئة
    val openingCashDrawer: Double = 0.0,                   // نقدية الصندوق والدرج الافتتاحية المعتمدة في معالج التهيئة
    val initialBankBalance: Double = 0.0,                  // الرصيد البنكي الافتتاحي في معالج التهيئة
    val isTaxEnabled: Boolean = false,                     // تفعيل / إلغاء حساب الضريبة في المبيعات (معطلة افتراضياً وفق بيئة العمل بالجمهورية اليمنية)
    val defaultTaxRate: Double = 0.0,                      // نسبة ضريبة المبيعات الافتراضية (0.0% افتراضياً)
    val isPurchaseTaxEnabled: Boolean = false,             // تفعيل / إلغاء حساب ضريبة المشتريات بشكل مستقل (معطلة افتراضياً)
    val purchaseTaxRate: Double = 0.0,                     // نسبة ضريبة المشتريات الافتراضية (0.0% افتراضياً)
    val enableProduceShrinkageTracking: Boolean = true,    // تفعيل احتساب الهدر والتالف للخضار المشكل
    val enableNegativeStock: Boolean = false,              // السماح بالبيع بالسالب في حال تأخر إدخال الفواتير
    val showDecimals: Boolean = false,                     // التحكم المركزي في إظهار الكسور العشرية (معطلة افتراضياً false)
    val invoiceFooterText: String = "شكراً لزيارتكم دكاني - تسوقكم يسعدنا!", // رسالة ذيل الفاتورة
    val lastUpdated: Long = System.currentTimeMillis()     // وقت آخر تعديل للإعدادات
)
