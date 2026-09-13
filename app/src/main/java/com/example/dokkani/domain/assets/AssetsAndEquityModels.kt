package com.example.dokkani.domain.assets

import com.example.dokkani.data.local.entities.FixedAssetEntity
import com.example.dokkani.data.local.entities.OwnerTransactionEntity

/**
 * نتيجة الحساب الآلي لرأس المال وحقوق الملكية
 */
data class EquityCalculationResult(
    val cashInHandAndDrawer: Double,             // نقدية الصندوق والدرج
    val bankAndMadaBalances: Double,             // أرصدة الحسابات البنكية ومقبوضات مدى
    val inventoryValuationAtCost: Double,        // تقييم بضاعة أول المدة والمخزون الحالي بسعر التكلفة
    val customerReceivables: Double,             // ديون العملاء (الأرصدة المدينة المستحقة للبقالة)
    val supplierPayables: Double,                // ديون الموردين والالتزامات (الأرصدة الدائنة المستحقة على البقالة)
    val totalFixedAssetsValue: Double,           // إجمالي قيمة الأصول الثابتة (ثلاجات، أرفف، موازين، سيارات)
    val totalLeaseholdGoodwillValue: Double,     // إجمالي القيمة الدفترية لنقل القدم / الخلو كأصل غير ملموس
    val calculatedInitialCapital: Double,        // رأس المال الافتتاحي الآلي المحسوب = (نقدية + بنوك + تقييم بضاعة + خلو + ديون عملاء) - ديون موردين
    val totalOwnerDrawings: Double,              // إجمالي مسحوبات المالك الشخصية (نقدية + بضاعة بالتكلفة)
    val totalAdditionalCapitalDeposits: Double,  // إجمالي الإيداعات الإضافية لرأس المال
    val netOperatingProfit: Double,              // صافي الأرباح التشغيلية المبقاة
    val netTotalEquity: Double                   // صافي حقوق الملكية الإجمالي = رأس المال الافتتاحي + إيداعات إضافية - مسحوبات المالك + الأصول + الخلو + الأرباح المبقاة
)

/**
 * تصنيف الأصول الثابتة للبقالة
 */
object AssetCategories {
    val REFRIGERATION = "ثلاجات وتبريد"
    val SHELVING = "أرفف وتجهيزات عرض"
    val SCALES_AND_POS = "أجهزة وموازين باركود"
    val VEHICLES = "وسائل نقل وتوصيل"
    val AIR_CONDITIONING = "تكييف وإضاءة"
    val OTHER = "أصول أخرى"

    val ALL = listOf(REFRIGERATION, SHELVING, SCALES_AND_POS, VEHICLES, AIR_CONDITIONING, OTHER)
}
