package com.example.dokkani.domain.produce

import com.example.dokkani.data.local.entities.MovementType
import com.example.dokkani.data.local.entities.StockMovementEntity

/**
 * مدخلات الجرد الدوري السريع لصنف خضار أو قسم فوضوي
 */
data class ProduceAuditInput(
    val productId: Long,
    val productName: String,
    val beginningInventoryQty: Double,       // مخزون أول المدة (كجم / وحدة)
    val beginningInventoryCost: Double,      // تكلفة مخزون أول المدة (ر.س)
    val purchasesQty: Double,                // المشتريات الواردة اليوم (كجم / وحدة)
    val purchasesCost: Double,               // تكلفة مشتريات اليوم (ر.س)
    val estimatedEndingInventoryQty: Double, // مخزون آخر المدة التقديري على الرفوف (كجم)
    val wasteQty: Double,                    // التوالف والهالك اليومي (كجم)
    val posRecordedSoldQty: Double = 0.0,    // الكمية المسجلة مبيعات عبر الكاشير
    val posRecordedRevenue: Double = 0.0     // إجمالي المبيعات المحققة للكاشير
)

/**
 * نتيجة الحساب المحاسبي التلقائي للجرد ومعادلة COGS
 */
data class ProduceAuditResult(
    val productId: Long,
    val productName: String,

    // البضاعة المتاحة للبيع
    val goodsAvailableQty: Double,
    val goodsAvailableCost: Double,
    val averageCostPerUnit: Double,

    // مخزون آخر المدة التقديري
    val endingInventoryQty: Double,
    val endingInventoryCost: Double,

    // التوالف والهالك
    val wasteQty: Double,
    val wasteCost: Double,
    val wastePercentageOfAvailable: Double,

    // صافي تكلفة المباع (COGS)
    // COGS = (مخزون أول المدة + المشتريات) - مخزون آخر المدة - التوالف
    val cogsCalculatedQty: Double,
    val cogsCalculatedCost: Double,

    // تحليل الكاشير والعجز/الفائض
    val posSoldQty: Double,
    val posRevenue: Double,
    val shrinkageDiscrepancyQty: Double,     // الفارق بين المحسوب بالجرد والمسجل بالكاشير
    val shrinkageDiscrepancyCost: Double,    // تكلفة العجز أو الفائض التقديري
    val grossProfit: Double,                 // مجمل الربح = المبيعات - COGS
    val grossProfitMarginPercent: Double,    // هامش الربح %

    // القيود والتحليلات التوجيهية
    val auditSummaryArabic: String,
    val generatedAdjustments: List<SilentAdjustmentEntry>
)

/**
 * قيد تسوية مخزنية صامتة جاهز للترحيل
 */
data class SilentAdjustmentEntry(
    val productId: Long,
    val movementType: MovementType,
    val quantityChange: Double,             // موجب للزيادة، سالب للنقص
    val unitCost: Double,
    val reasonArabic: String,
    val referenceNumber: String
)

/**
 * المحرك المحاسبي للجرد الدوري السريع للخضار والورقيات والأصناف الفوضوية
 */
object ProduceAuditEngine {

    /**
     * حساب تكلفة البضاعة المباعة COGS وقيود التسوية الصامتة
     * المعادلة المعتمدة:
     * COGS = (مخزون أول المدة + المشتريات) - مخزون آخر المدة - التوالف
     */
    fun computeProduceAudit(input: ProduceAuditInput): ProduceAuditResult {
        val begQty = maxOf(0.0, input.beginningInventoryQty)
        val begCost = maxOf(0.0, input.beginningInventoryCost)

        val purQty = maxOf(0.0, input.purchasesQty)
        val purCost = maxOf(0.0, input.purchasesCost)

        // 1. إجمالي البضاعة المتاحة
        val totalAvailableQty = begQty + purQty
        val totalAvailableCost = begCost + purCost

        val avgCost = if (totalAvailableQty > 0.0001) {
            totalAvailableCost / totalAvailableQty
        } else if (purQty > 0.0001) {
            purCost / purQty
        } else {
            0.0
        }

        // 2. التوالف ومخزون آخر المدة
        val wasteQty = maxOf(0.0, input.wasteQty)
        val wasteCost = wasteQty * avgCost
        val wastePercent = if (totalAvailableQty > 0.0001) (wasteQty / totalAvailableQty) * 100.0 else 0.0

        val endingQty = maxOf(0.0, input.estimatedEndingInventoryQty)
        val endingCost = endingQty * avgCost

        // 3. تطبيق المعادلة المحاسبية لصافي تكلفة المباع (COGS)
        // COGS = (مخزون أول المدة + المشتريات) - مخزون آخر المدة - التوالف
        val cogsQty = maxOf(0.0, totalAvailableQty - endingQty - wasteQty)
        val cogsCost = maxOf(0.0, totalAvailableCost - endingCost - wasteCost)

        // 4. مقارنة المبيعات المسجلة والعجز الدفتري
        val posQty = input.posRecordedSoldQty
        val posRev = input.posRecordedRevenue
        val discrepancyQty = cogsQty - posQty
        val discrepancyCost = discrepancyQty * avgCost

        val grossProfit = if (posRev > 0.0) posRev - cogsCost else 0.0
        val profitMargin = if (posRev > 0.0) (grossProfit / posRev) * 100.0 else 0.0

        // 5. توليد قيود التسوية المخزنية الصامتة لإغلاق اليوم
        val timestamp = System.currentTimeMillis()
        val refPrefix = "AUDIT-${timestamp.toString().takeLast(6)}"
        val adjustments = mutableListOf<SilentAdjustmentEntry>()

        // قيد 1: إثبات التوالف والهالك من الخضار
        if (wasteQty > 0.0001) {
            adjustments.add(
                SilentAdjustmentEntry(
                    productId = input.productId,
                    movementType = MovementType.INVENTORY_ADJUSTMENT,
                    quantityChange = -wasteQty,
                    unitCost = avgCost,
                    reasonArabic = "إثبات توالف وهالك خضار يومي طبيعي (${input.productName})",
                    referenceNumber = "$refPrefix-WASTE"
                )
            )
        }

        // قيد 2: تسوية الرصيد المتبقي على الرفوف
        // إذا كان هناك عجز غير مسجل أو تعديل لمطابقة الرف التقديري
        val currentBookBalance = maxOf(0.0, totalAvailableQty - posQty - wasteQty)
        val shelfDiff = endingQty - currentBookBalance

        if (Math.abs(shelfDiff) > 0.001) {
            val directionDesc = if (shelfDiff > 0) "فائض رف تقديري (+)" else "عجز رف وفروقات بيع (-)"
            adjustments.add(
                SilentAdjustmentEntry(
                    productId = input.productId,
                    movementType = MovementType.INVENTORY_ADJUSTMENT,
                    quantityChange = shelfDiff,
                    unitCost = avgCost,
                    reasonArabic = "تسوية صامتة لمطابقة رصيد الرف التقديري: $directionDesc",
                    referenceNumber = "$refPrefix-SHELF"
                )
            )
        }

        val summary = buildString {
            append("تم احتساب صافي تكلفة المباع (COGS) للصنف [${input.productName}]: ")
            append("%.2f كجم بتكلفة %.2f ر.س. ".format(cogsQty, cogsCost))
            append("التوالف: %.2f كجم (%.1f%%). ".format(wasteQty, wastePercent))
            append("مخزون الرف المتبقي المعتمد: %.2f كجم (%.2f ر.س).".format(endingQty, endingCost))
        }

        return ProduceAuditResult(
            productId = input.productId,
            productName = input.productName,
            goodsAvailableQty = totalAvailableQty,
            goodsAvailableCost = totalAvailableCost,
            averageCostPerUnit = avgCost,
            endingInventoryQty = endingQty,
            endingInventoryCost = endingCost,
            wasteQty = wasteQty,
            wasteCost = wasteCost,
            wastePercentageOfAvailable = wastePercent,
            cogsCalculatedQty = cogsQty,
            cogsCalculatedCost = cogsCost,
            posSoldQty = posQty,
            posRevenue = posRev,
            shrinkageDiscrepancyQty = discrepancyQty,
            shrinkageDiscrepancyCost = discrepancyCost,
            grossProfit = grossProfit,
            grossProfitMarginPercent = profitMargin,
            auditSummaryArabic = summary,
            generatedAdjustments = adjustments
        )
    }
}
