package com.example.dokkani.domain.costing

import com.example.dokkani.data.local.dao.ProductDao
import com.example.dokkani.data.local.dao.StockMovementDao
import com.example.dokkani.data.local.dao.SystemSettingsDao
import com.example.dokkani.data.local.entities.CostValuationMethod
import com.example.dokkani.data.local.entities.ProductEntity
import com.example.dokkani.data.local.entities.ProductUnitEntity
import com.example.dokkani.data.local.entities.StockMovementEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * نتيجة احتساب التكلفة مع تفاصيل الخطوات المحاسبية
 */
data class CostCalculationResult(
    val productId: Long,
    val productName: String,
    val methodUsed: CostValuationMethod,
    val unitCostBase: Double,
    val unitCostTargetUnit: Double,
    val targetUnitName: String,
    val conversionFactor: Double,
    val totalStockAvailable: Double,
    val calculationSteps: List<String>,
    val formulaExplanation: String,
    val isFallbackToStandardPrice: Boolean = false
)

/**
 * نتيجة احتساب تكلفة البضاعة المباعة (COGS) لعملية بيع محددة بطريقة FIFO
 */
data class FifoCogsResult(
    val requestedQuantity: Double,
    val totalCost: Double,
    val averageCostPerUnit: Double,
    val lotsConsumed: List<ConsumedLotDetail>,
    val remainingUncoveredQuantity: Double = 0.0
)

data class ConsumedLotDetail(
    val lotId: Long,
    val dateString: String,
    val quantityTaken: Double,
    val unitCost: Double,
    val subtotalCost: Double
)

/**
 * المحرك المحاسبي لحساب تكلفة المخزون والأصناف لنظام "دكاني"
 * يدعم بدقة متناهية:
 * 1. المتوسط المرجح (WAC - Weighted Average Cost)
 * 2. الوارد أولاً صادر أولاً (FIFO - First-In, First-Out)
 * 3. آخر سعر شراء (Last Purchase Price)
 * مع مراعاة التحويل التلقائي بين الوحدات المتعددة (حبة، كرتون، كيلو، صندوق...)
 */
class CostCalculationEngine(
    private val productDao: ProductDao,
    private val stockMovementDao: StockMovementDao,
    private val systemSettingsDao: SystemSettingsDao
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    /**
     * الدالة الرئيسية لاحتساب تكلفة الصنف بالاعتماد على إعدادات النظام المحاسبي
     */
    suspend fun calculateCostAccordingToSettings(
        productId: Long,
        targetUnitId: Long? = null
    ): CostCalculationResult {
        val settings = systemSettingsDao.getSettingsSync()
        val valuationMethod = settings?.costValuationMethod ?: CostValuationMethod.WAC
        return calculateProductCost(productId, valuationMethod, targetUnitId)
    }

    /**
     * دالة احتساب التكلفة بناءً على طريقة تقييم محددة
     *
     * @param productId معرّف الصنف
     * @param valuationMethod طريقة التقييم: WAC أو FIFO أو LAST_PURCHASE_PRICE
     * @param targetUnitId الوحدة المطلوبة لاحتساب التكلفة لها (إذا كانت null يتم احتساب الوحدة الأساسية)
     */
    suspend fun calculateProductCost(
        productId: Long,
        valuationMethod: CostValuationMethod,
        targetUnitId: Long? = null
    ): CostCalculationResult {
        val product = productDao.getProductById(productId)
            ?: throw IllegalArgumentException("الصنف غير موجود بالرقم: $productId")

        val units = productDao.getUnitsForProductSync(productId)
        val baseUnit = units.firstOrNull { it.isBaseUnit } ?: units.firstOrNull()
            ?: ProductUnitEntity(
                productId = productId,
                unitName = if (product.isWeighted) "كيلو" else "حبة",
                conversionFactor = 1.0,
                barcode = product.code,
                costPrice = 0.0,
                sellingPrice = 0.0,
                isBaseUnit = true
            )

        val targetUnit = if (targetUnitId != null) {
            units.firstOrNull { it.id == targetUnitId } ?: baseUnit
        } else {
            baseUnit
        }

        val conversionFactor = targetUnit.conversionFactor
        val steps = mutableListOf<String>()

        return when (valuationMethod) {
            CostValuationMethod.WAC -> {
                calculateWacCost(product, baseUnit, targetUnit, steps)
            }
            CostValuationMethod.FIFO -> {
                calculateFifoCurrentUnitCost(product, baseUnit, targetUnit, steps)
            }
            CostValuationMethod.LAST_PURCHASE_PRICE -> {
                calculateLastPurchaseCost(product, baseUnit, targetUnit, steps)
            }
        }
    }

    /**
     * 1. احتساب التكلفة بطريقة المتوسط المرجح (WAC - Weighted Average Cost)
     * القانون: إجمالي تكلفة الطبقات المتاحة بالمخزن / إجمالي الكميات المتاحة
     * WAC = Σ (الكمية المتبقية * سعر تكلفة الشراء) / Σ (الكميات المتبقية)
     */
    private suspend fun calculateWacCost(
        product: ProductEntity,
        baseUnit: ProductUnitEntity,
        targetUnit: ProductUnitEntity,
        steps: MutableList<String>
    ): CostCalculationResult {
        steps.add("بدء احتساب التكلفة بطريقة [المتوسط المرجح - WAC] للصنف: ${product.name}")
        val activeLots = stockMovementDao.getActiveStockLotsForWac(product.id)

        var totalQty = 0.0
        var totalCostValue = 0.0
        val isFallback: Boolean
        val unitCostBase: Double

        if (activeLots.isNotEmpty()) {
            steps.add("تم العثور على ${activeLots.size} طبقة شراء وتوريد نشطة في المخزن:")
            for ((index, lot) in activeLots.withIndex()) {
                val lotQty = lot.remainingQuantityForFifo
                val lotCost = lot.unitCostPriceBase
                val lotTotal = lotQty * lotCost
                totalQty += lotQty
                totalCostValue += lotTotal
                val dateStr = dateFormat.format(Date(lot.timestamp))
                steps.add(" - طبقة #${index + 1} ($dateStr): كمية = $lotQty ${baseUnit.unitName} × تكلفة = $lotCost ر.س = $lotTotal ر.س")
            }

            if (totalQty > 0.0001) {
                unitCostBase = totalCostValue / totalQty
                steps.add("إجمالي التكلفة = $totalCostValue ر.س ÷ إجمالي الكميات = $totalQty ${baseUnit.unitName}")
                steps.add("الناتج الأساسي للمتوسط المرجح (WAC) للوحدة الأساسية [${baseUnit.unitName}] = %.4f ر.س".format(unitCostBase))
                isFallback = false
            } else {
                unitCostBase = baseUnit.costPrice
                steps.add("تنبيه: مجموع الكميات المتاحة صفر، تم الاعتماد على سعر الشراء القياسي للصنف: $unitCostBase ر.س")
                isFallback = true
            }
        } else {
            unitCostBase = baseUnit.costPrice
            steps.add("لم توجد حركات توريد مسجلة في المخزن، تم استخدام التكلفة المعيارية للوحدة الأساسية: $unitCostBase ر.س")
            isFallback = true
        }

        val targetCost = unitCostBase * targetUnit.conversionFactor
        if (targetUnit.id != baseUnit.id) {
            steps.add("التحويل إلى الوحدة المطلوبة [${targetUnit.unitName}] بمعامل تحويل (${targetUnit.conversionFactor}):")
            steps.add("%.4f × ${targetUnit.conversionFactor} = %.4f ر.س".format(unitCostBase, targetCost))
        }

        return CostCalculationResult(
            productId = product.id,
            productName = product.name,
            methodUsed = CostValuationMethod.WAC,
            unitCostBase = unitCostBase,
            unitCostTargetUnit = targetCost,
            targetUnitName = targetUnit.unitName,
            conversionFactor = targetUnit.conversionFactor,
            totalStockAvailable = totalQty,
            calculationSteps = steps,
            formulaExplanation = "WAC = مجموع (كميات الطبقات المتاحة × تكلفة كل طبقة) ÷ مجموع الكميات المتاحة بالمخزن",
            isFallbackToStandardPrice = isFallback
        )
    }

    /**
     * 2. احتساب التكلفة بطريقة الوارد أولاً صادر أولاً (FIFO - First In, First Out)
     * تعتمد تكلفة الطبقة الأقدم التي لا تزال تحتوي على رصيد متبقٍ لم يتم استهلاكه بعد.
     */
    private suspend fun calculateFifoCurrentUnitCost(
        product: ProductEntity,
        baseUnit: ProductUnitEntity,
        targetUnit: ProductUnitEntity,
        steps: MutableList<String>
    ): CostCalculationResult {
        steps.add("بدء احتساب التكلفة بطريقة [الوارد أولاً صادر أولاً - FIFO] للصنف: ${product.name}")
        val fifoLots = stockMovementDao.getAvailableFifoLots(product.id)

        var totalQty = 0.0
        val isFallback: Boolean
        val unitCostBase: Double

        if (fifoLots.isNotEmpty()) {
            val oldestLot = fifoLots.first()
            totalQty = fifoLots.sumOf { it.remainingQuantityForFifo }
            unitCostBase = oldestLot.unitCostPriceBase
            val oldestDateStr = dateFormat.format(Date(oldestLot.timestamp))

            steps.add("عدد الطبقات المتاحة بالمخزن: ${fifoLots.size} طبقة بإجمالي رصيد $totalQty ${baseUnit.unitName}")
            steps.add("أقدم طبقة شراء نشطة تم توريدها بتاريخ ($oldestDateStr) برصيد متبقٍ قدره (${oldestLot.remainingQuantityForFifo} ${baseUnit.unitName})")
            steps.add("سعر تكلفة هذه الطبقة الأقدم هو المعتمد وفق معيار FIFO للوحدة الأساسية = %.4f ر.س".format(unitCostBase))
            isFallback = false
        } else {
            unitCostBase = baseUnit.costPrice
            steps.add("لا توجد طبقات شراء حالية مسجلة برصيد متبقٍ، تم الرجوع للتكلفة القياسية المسجلة: $unitCostBase ر.س")
            isFallback = true
        }

        val targetCost = unitCostBase * targetUnit.conversionFactor
        if (targetUnit.id != baseUnit.id) {
            steps.add("تحويل تكلفة FIFO للوحدة [${targetUnit.unitName}] بمعامل (${targetUnit.conversionFactor}):")
            steps.add("%.4f × ${targetUnit.conversionFactor} = %.4f ر.س".format(unitCostBase, targetCost))
        }

        return CostCalculationResult(
            productId = product.id,
            productName = product.name,
            methodUsed = CostValuationMethod.FIFO,
            unitCostBase = unitCostBase,
            unitCostTargetUnit = targetCost,
            targetUnitName = targetUnit.unitName,
            conversionFactor = targetUnit.conversionFactor,
            totalStockAvailable = totalQty,
            calculationSteps = steps,
            formulaExplanation = "FIFO = تكلفة أقدم طبقة شراء/توريد متوفرة في المخزن (الوارد أولاً يصرف أولاً)",
            isFallbackToStandardPrice = isFallback
        )
    }

    /**
     * دالة متقدمة: احتساب تكلفة البضاعة المباعة (COGS) لعملية بيع كمية معينة وفق FIFO
     * واستهلاك الطبقات المتتالية
     */
    suspend fun calculateFifoCogsForSaleQuantity(
        productId: Long,
        saleQuantityBaseUnit: Double
    ): FifoCogsResult {
        val availableLots = stockMovementDao.getAvailableFifoLots(productId)
        var remainingNeeded = saleQuantityBaseUnit
        var totalCost = 0.0
        val consumedLots = mutableListOf<ConsumedLotDetail>()

        for (lot in availableLots) {
            if (remainingNeeded <= 0.00001) break
            val lotQty = lot.remainingQuantityForFifo
            val qtyToTake = minOf(remainingNeeded, lotQty)
            val subtotal = qtyToTake * lot.unitCostPriceBase

            totalCost += subtotal
            remainingNeeded -= qtyToTake

            consumedLots.add(
                ConsumedLotDetail(
                    lotId = lot.id,
                    dateString = dateFormat.format(Date(lot.timestamp)),
                    quantityTaken = qtyToTake,
                    unitCost = lot.unitCostPriceBase,
                    subtotalCost = subtotal
                )
            )
        }

        val avgCost = if (saleQuantityBaseUnit > 0) totalCost / (saleQuantityBaseUnit - remainingNeeded) else 0.0

        return FifoCogsResult(
            requestedQuantity = saleQuantityBaseUnit,
            totalCost = totalCost,
            averageCostPerUnit = avgCost,
            lotsConsumed = consumedLots,
            remainingUncoveredQuantity = remainingNeeded
        )
    }

    /**
     * 3. احتساب التكلفة بطريقة آخر سعر شراء (Last Purchase Price)
     * تأخذ سعر آخر فاتورة شراء أو توريد معتمدة للصنف
     */
    private suspend fun calculateLastPurchaseCost(
        product: ProductEntity,
        baseUnit: ProductUnitEntity,
        targetUnit: ProductUnitEntity,
        steps: MutableList<String>
    ): CostCalculationResult {
        steps.add("بدء احتساب التكلفة بطريقة [آخر سعر شراء - Last Purchase Price] للصنف: ${product.name}")
        val lastMovement = stockMovementDao.getLastPurchaseMovement(product.id)
        val totalStock = stockMovementDao.getTotalStockQuantity(product.id)

        val isFallback: Boolean
        val unitCostBase: Double

        if (lastMovement != null) {
            unitCostBase = lastMovement.unitCostPriceBase
            val lastDateStr = dateFormat.format(Date(lastMovement.timestamp))
            val refStr = lastMovement.referenceNumber ?: "توريد مخزني"
            steps.add("آخر حركة شراء مسجلة كانت بتاريخ ($lastDateStr) برقم مرجعي: $refStr")
            steps.add("كمية التوريد في تلك الفاتورة: ${lastMovement.quantityBaseUnit} ${baseUnit.unitName}")
            steps.add("سعر الشراء للوحدة الأساسية في آخر حركة = %.4f ر.س".format(unitCostBase))
            isFallback = false
        } else {
            unitCostBase = baseUnit.costPrice
            steps.add("لم يتم العثور على حركات شراء سابقة، تم استخدام سعر الشراء المعياري المسجل: $unitCostBase ر.س")
            isFallback = true
        }

        val targetCost = unitCostBase * targetUnit.conversionFactor
        if (targetUnit.id != baseUnit.id) {
            steps.add("تحويل آخر سعر شراء للوحدة [${targetUnit.unitName}] بمعامل (${targetUnit.conversionFactor}):")
            steps.add("%.4f × ${targetUnit.conversionFactor} = %.4f ر.س".format(unitCostBase, targetCost))
        }

        return CostCalculationResult(
            productId = product.id,
            productName = product.name,
            methodUsed = CostValuationMethod.LAST_PURCHASE_PRICE,
            unitCostBase = unitCostBase,
            unitCostTargetUnit = targetCost,
            targetUnitName = targetUnit.unitName,
            conversionFactor = targetUnit.conversionFactor,
            totalStockAvailable = totalStock,
            calculationSteps = steps,
            formulaExplanation = "Last Purchase Price = سعر الشراء للوحدة الأساسية المسجل في آخر فاتورة مشتريات واردة",
            isFallbackToStandardPrice = isFallback
        )
    }
}
