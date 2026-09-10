package com.example.dokkani

import com.example.dokkani.domain.produce.ProduceQuickInventoryEngine
import com.example.dokkani.domain.produce.SortedProduceInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProduceAndCostingTest {

    @Test
    fun testQuickInventoryAndShrinkageCalculation() {
        val grossWeight = 25.0 // كجم
        val purchaseCost = 90.0 // ر.س
        val expenses = 10.0 // ر.س نقل وعتالة
        val wasteWeight = 3.0 // كجم تالف
        val targetMargin = 25.0 // %

        val result = ProduceQuickInventoryEngine.calculateQuickInventory(
            grossWeightKg = grossWeight,
            purchaseCost = purchaseCost,
            additionalExpenses = expenses,
            wasteWeightKg = wasteWeight,
            targetMarginPercent = targetMargin
        )

        // التحقق من الوزن الصافي الصالح للبيع: 25 - 3 = 22 كجم
        assertEquals(22.0, result.netSalableWeightKg, 0.001)

        // التحقق من نسبة الهدر والتالف: 3 / 25 = 12.0%
        assertEquals(12.0, result.wastePercentage, 0.001)

        // إجمالي التكلفة المستثمرة: 90 + 10 = 100 ر.س
        assertEquals(100.0, result.totalEffectiveInvestedCost, 0.001)

        // التكلفة الفعلية للكيلو الصافي: 100 / 22 = 4.5454... ر.س/كجم
        assertEquals(4.5454, result.effectiveCostPerSalableKg, 0.001)

        // سعر البيع المقترح بهامش 25%: 4.5454 * 1.25 = 5.6818... ر.س/كجم
        assertEquals(5.6818, result.suggestedSalePricePerKg, 0.001)

        // مجمل الربح المتوقع إيجابي
        assertTrue(result.expectedGrossProfit > 0)
    }

    @Test
    fun testSortedProduceYieldAllocation() {
        val totalCost = 100.0 // ر.س
        val items = listOf(
            SortedProduceInput(
                productId = 1L,
                productName = "طماطم فرز درجة أولى",
                sortedWeightKg = 12.0,
                costRatio = 1.0,
                targetMarginPercent = 25.0
            ),
            SortedProduceInput(
                productId = 2L,
                productName = "خيار فرز درجة ثانية",
                sortedWeightKg = 10.0,
                costRatio = 1.0,
                targetMarginPercent = 25.0
            )
        )

        val yields = ProduceQuickInventoryEngine.allocateCrateCostToSortedProduceItems(
            batchId = 1L,
            totalInvestedCost = totalCost,
            itemsToAllocate = items
        )

        assertEquals(2, yields.size)
        // كلا الصنفين لهما نفس معامل القيمة (1.0)، لذا تكلفة الكيلو متساوية (100 / 22 = 4.5454)
        assertEquals(4.5454, yields[0].calculatedCostPerKg, 0.001)
        assertEquals(4.5454, yields[1].calculatedCostPerKg, 0.001)
        assertEquals(12.0, yields[0].sortedWeightKg, 0.001)
        assertEquals(10.0, yields[1].sortedWeightKg, 0.001)
    }
}
