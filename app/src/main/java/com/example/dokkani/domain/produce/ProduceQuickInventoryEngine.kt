package com.example.dokkani.domain.produce

import com.example.dokkani.data.local.entities.BatchStatus
import com.example.dokkani.data.local.entities.MixedProduceBatchEntity
import com.example.dokkani.data.local.entities.MixedProduceYieldItemEntity

/**
 * نتيجة الحساب الفوري للجرد السريع والهدر لسحارة خضار مشكل
 */
data class ProduceQuickCalcSummary(
    val grossWeightKg: Double,
    val wasteWeightKg: Double,
    val netSalableWeightKg: Double,
    val wastePercentage: Double,
    val totalCrateCost: Double,
    val additionalExpenses: Double,
    val totalEffectiveInvestedCost: Double,
    val effectiveCostPerSalableKg: Double,
    val targetProfitMarginPercent: Double,
    val suggestedSalePricePerKg: Double,
    val expectedTotalRevenue: Double,
    val expectedGrossProfit: Double,
    val accountantNotes: List<String>
)

/**
 * محرك حسابات الجرد السريع لـ "خضار المشكل" والهدر وإعادة احتساب التكلفة الفعلية للبقالات
 */
object ProduceQuickInventoryEngine {

    /**
     * احتساب التكلفة الفعلية ونسبة الهدر لسحارة خضار مشكل
     *
     * في محلات الخضار والبقالات، يتم شراء سحارة خضار مشكل بسعر مقطوع (مثلاً 100 ريال لوزن 25 كجم).
     * بعد التنزيل والفرز، يظهر جزء تالف أو غير صالح للبيع (مثلاً 3 كجم).
     * المحاسبة الصحيحة: لا يجوز قسمة 100 على 25 كجم (4 ريال)،
     * بل يجب تحميل تكلفة التالف على الوزن الصافي الصالح للبيع (22 كجم):
     * التكلفة الفعلية للكيلو الصافي = 100 ريال ÷ 22 كجم = 4.545 ريال للكيلو.
     */
    fun calculateQuickInventory(
        grossWeightKg: Double,
        purchaseCost: Double,
        additionalExpenses: Double = 0.0,
        wasteWeightKg: Double = 0.0,
        targetMarginPercent: Double = 25.0
    ): ProduceQuickCalcSummary {
        val safeGross = maxOf(0.001, grossWeightKg)
        val safeWaste = maxOf(0.0, minOf(wasteWeightKg, safeGross))
        val netSalableKg = maxOf(0.0, safeGross - safeWaste)
        val wastePercent = (safeWaste / safeGross) * 100.0

        val totalCost = purchaseCost + additionalExpenses
        val effectiveCostPerKg = if (netSalableKg > 0.0001) totalCost / netSalableKg else 0.0

        val suggestedPricePerKg = effectiveCostPerKg * (1.0 + (targetMarginPercent / 100.0))
        val expectedRevenue = netSalableKg * suggestedPricePerKg
        val expectedGrossProfit = expectedRevenue - totalCost

        val notes = mutableListOf<String>()
        notes.add("الوزن القائم للسحارة: $safeGross كجم | التالف/الهدر: $safeWaste كجم")
        notes.add("نسبة الهدر الطبيعي/التالف = %.2f%%".format(wastePercent))
        notes.add("الوزن الصافي القابل للبيع على الرفوف = %.2f كجم".format(netSalableKg))
        notes.add("إجمالي التكلفة المستثمرة (شراء + مصاريف نقل) = %.2f ر.س".format(totalCost))
        notes.add("التكلفة الفعلية المحاسبية للكيلو الصافي = %.3f ر.س/كجم (مقارنة بـ %.3f ر.س قبل استبعاد التالف)".format(
            effectiveCostPerKg,
            totalCost / safeGross
        ))
        notes.add("سعر البيع المقترح لتحقيق هامش ربح %.1f%% = %.2f ر.س/كجم".format(
            targetMarginPercent,
            suggestedPricePerKg
        ))
        notes.add("الربح الإجمالي المتوقع عند بيع كامل الصافي = %.2f ر.س".format(expectedGrossProfit))

        return ProduceQuickCalcSummary(
            grossWeightKg = safeGross,
            wasteWeightKg = safeWaste,
            netSalableWeightKg = netSalableKg,
            wastePercentage = wastePercent,
            totalCrateCost = purchaseCost,
            additionalExpenses = additionalExpenses,
            totalEffectiveInvestedCost = totalCost,
            effectiveCostPerSalableKg = effectiveCostPerKg,
            targetProfitMarginPercent = targetMarginPercent,
            suggestedSalePricePerKg = suggestedPricePerKg,
            expectedTotalRevenue = expectedRevenue,
            expectedGrossProfit = expectedGrossProfit,
            accountantNotes = notes
        )
    }

    /**
     * توزيع تكلفة السحارة المشكلة على الأصناف المفروزة
     * بناءً على أوزانها ومعامل القيمة السوقية النسبية لكل صنف
     */
    fun allocateCrateCostToSortedProduceItems(
        batchId: Long,
        totalInvestedCost: Double,
        itemsToAllocate: List<SortedProduceInput>
    ): List<MixedProduceYieldItemEntity> {
        val totalWeightedUnits = itemsToAllocate.sumOf { it.sortedWeightKg * it.costRatio }
        if (totalWeightedUnits <= 0.0001) return emptyList()

        val costPerWeightedUnit = totalInvestedCost / totalWeightedUnits

        return itemsToAllocate.map { item ->
            val costPerKg = costPerWeightedUnit * item.costRatio
            val targetSellingPrice = costPerKg * (1.0 + (item.targetMarginPercent / 100.0))
            val expectedRevenue = item.sortedWeightKg * targetSellingPrice

            MixedProduceYieldItemEntity(
                batchId = batchId,
                productId = item.productId,
                productName = item.productName,
                sortedWeightKg = item.sortedWeightKg,
                costAllocationRatio = item.costRatio,
                calculatedCostPerKg = costPerKg,
                targetSellingPricePerKg = targetSellingPrice,
                expectedRevenue = expectedRevenue
            )
        }
    }
}

/**
 * بيانات مدخلات الصنف المفروز من السحارة
 */
data class SortedProduceInput(
    val productId: Long,
    val productName: String,
    val sortedWeightKg: Double,
    val costRatio: Double = 1.0, // معامل القيمة: 1.0 للصنف المتوسط، 1.3 للصنف الأغلى كالفلفل الملون
    val targetMarginPercent: Double = 25.0
)
