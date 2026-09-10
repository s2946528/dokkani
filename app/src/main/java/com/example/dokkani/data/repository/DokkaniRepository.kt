package com.example.dokkani.data.repository

import com.example.dokkani.data.local.DokkaniDatabase
import com.example.dokkani.data.local.entities.BatchWithYields
import com.example.dokkani.data.local.entities.CostValuationMethod
import com.example.dokkani.data.local.entities.CurrencyEntity
import com.example.dokkani.data.local.entities.InvoiceEntity
import com.example.dokkani.data.local.entities.InvoiceItemEntity
import com.example.dokkani.data.local.entities.InvoiceWithDetails
import com.example.dokkani.data.local.entities.MixedProduceBatchEntity
import com.example.dokkani.data.local.entities.MixedProduceYieldItemEntity
import com.example.dokkani.data.local.entities.MovementType
import com.example.dokkani.data.local.entities.PartyEntity
import com.example.dokkani.data.local.entities.ProductEntity
import com.example.dokkani.data.local.entities.ProductUnitEntity
import com.example.dokkani.data.local.entities.ProductWithUnits
import com.example.dokkani.data.local.entities.StockMovementEntity
import com.example.dokkani.data.local.entities.SystemSettingsEntity
import com.example.dokkani.domain.costing.CostCalculationEngine
import com.example.dokkani.domain.costing.CostCalculationResult
import com.example.dokkani.domain.produce.ProduceQuickCalcSummary
import com.example.dokkani.domain.produce.ProduceQuickInventoryEngine
import kotlinx.coroutines.flow.Flow

/**
 * مستودع بيانات نظام دكاني (Dokkani Repository)
 * يربط بين طبقة البيانات وقواعد البيانات والمحرك المحاسبي وواجهة المستخدم
 */
class DokkaniRepository(private val database: DokkaniDatabase) {

    private val productDao = database.productDao()
    private val currencyDao = database.currencyDao()
    private val partyDao = database.partyDao()
    private val invoiceDao = database.invoiceDao()
    private val stockMovementDao = database.stockMovementDao()
    private val produceBatchDao = database.mixedProduceBatchDao()
    private val settingsDao = database.systemSettingsDao()

    val costingEngine = CostCalculationEngine(productDao, stockMovementDao, settingsDao)

    // Flow streams for reactive UI
    val allProductsWithUnits: Flow<List<ProductWithUnits>> = productDao.getProductsWithUnits()
    val allCurrencies: Flow<List<CurrencyEntity>> = currencyDao.getAllCurrencies()
    val allParties: Flow<List<PartyEntity>> = partyDao.getAllParties()
    val recentInvoices: Flow<List<InvoiceWithDetails>> = invoiceDao.getRecentInvoicesWithDetails()
    val produceBatchesWithYields: Flow<List<BatchWithYields>> = produceBatchDao.getBatchesWithYields()
    val systemSettings: Flow<SystemSettingsEntity?> = settingsDao.getSettings()

    // Costing calculations
    suspend fun calculateCost(
        productId: Long,
        method: CostValuationMethod,
        targetUnitId: Long? = null
    ): CostCalculationResult {
        return costingEngine.calculateProductCost(productId, method, targetUnitId)
    }

    suspend fun calculateCostWithCurrentSettings(
        productId: Long,
        targetUnitId: Long? = null
    ): CostCalculationResult {
        return costingEngine.calculateCostAccordingToSettings(productId, targetUnitId)
    }

    suspend fun getAvailableLotsForProduct(productId: Long): List<StockMovementEntity> {
        return stockMovementDao.getAvailableFifoLots(productId)
    }

    suspend fun getAllMovementsForProduct(productId: Long): List<StockMovementEntity> {
        return stockMovementDao.getMovementsForProductSync(productId)
    }

    // System Settings updates
    suspend fun updateCostValuationMethod(method: CostValuationMethod) {
        settingsDao.updateCostValuationMethod(method)
    }

    suspend fun updateSettings(settings: SystemSettingsEntity) {
        settingsDao.insertOrUpdateSettings(settings)
    }

    // Produce Quick Inventory Calculation & Persistence
    fun calculateProduceQuickInventory(
        grossWeightKg: Double,
        purchaseCost: Double,
        additionalExpenses: Double,
        wasteWeightKg: Double,
        targetMarginPercent: Double
    ): ProduceQuickCalcSummary {
        return ProduceQuickInventoryEngine.calculateQuickInventory(
            grossWeightKg,
            purchaseCost,
            additionalExpenses,
            wasteWeightKg,
            targetMarginPercent
        )
    }

    suspend fun saveMixedProduceBatch(
        batch: MixedProduceBatchEntity,
        yieldItems: List<MixedProduceYieldItemEntity>
    ): Long {
        val batchId = produceBatchDao.insertBatch(batch)
        if (yieldItems.isNotEmpty()) {
            val itemsWithBatchId = yieldItems.map { it.copy(batchId = batchId) }
            produceBatchDao.insertYieldItems(itemsWithBatchId)
        }
        return batchId
    }

    // Insert purchase movement (to test costing simulation with new lots)
    suspend fun addPurchaseLotMovement(
        productId: Long,
        unitId: Long,
        quantity: Double,
        unitCost: Double,
        invoiceRef: String
    ): Long {
        return stockMovementDao.insertMovement(
            StockMovementEntity(
                productId = productId,
                productUnitId = unitId,
                movementType = MovementType.PURCHASE_IN,
                quantityBaseUnit = quantity,
                remainingQuantityForFifo = quantity,
                unitCostPriceBase = unitCost,
                referenceNumber = invoiceRef,
                notes = "توريد دفعة مخزون تجريبية لحساب التكلفة"
            )
        )
    }

    // Insert Product with units
    suspend fun addProductWithUnits(
        product: ProductEntity,
        units: List<ProductUnitEntity>
    ): Long {
        val productId = productDao.insertProduct(product)
        val unitsWithId = units.map { it.copy(productId = productId) }
        productDao.insertUnits(unitsWithId)
        return productId
    }
}
