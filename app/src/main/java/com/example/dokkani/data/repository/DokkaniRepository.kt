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

    /**
     * إتمام عملية البيع لنقطة البيع (POS Checkout) وحفظ الفاتورة والبنود
     * وتحديث رصيد العميل في حال البيع الآجل (الشكك)
     * وخصم المخزون واستهلاك طبقات FIFO
     */
    suspend fun processPosSale(
        cartItems: List<com.example.dokkani.domain.pos.PosCartItem>,
        paymentMethod: com.example.dokkani.data.local.entities.PaymentMethod,
        partyId: Long?,
        paidAmount: Double,
        discount: Double = 0.0,
        notes: String = ""
    ): com.example.dokkani.domain.pos.PosCheckoutResult {
        if (cartItems.isEmpty()) {
            throw IllegalArgumentException("لا يمكن إتمام فاتورة بسلة فارغة!")
        }

        val settings = settingsDao.getSettingsSync() ?: SystemSettingsEntity()
        val currency = currencyDao.getDefaultCurrency()
            ?: currencyDao.getBaseCurrency()
            ?: CurrencyEntity(code = "SAR", name = "ريال سعودي", symbol = "ر.س", exchangeRateToBase = 1.0)

        val subtotal = cartItems.sumOf { it.totalPrice }
        val netBeforeTax = (subtotal - discount).coerceAtLeast(0.0)
        val taxAmount = netBeforeTax * settings.defaultTaxRate
        val finalTotal = netBeforeTax + taxAmount

        val actualPaid: Double
        val changeAmount: Double
        val remainingCreditAmount: Double

        when (paymentMethod) {
            com.example.dokkani.data.local.entities.PaymentMethod.CASH -> {
                actualPaid = paidAmount.coerceAtLeast(0.0)
                changeAmount = (actualPaid - finalTotal).coerceAtLeast(0.0)
                remainingCreditAmount = (finalTotal - actualPaid).coerceAtLeast(0.0)
            }
            com.example.dokkani.data.local.entities.PaymentMethod.MADA -> {
                actualPaid = finalTotal
                changeAmount = 0.0
                remainingCreditAmount = 0.0
            }
            com.example.dokkani.data.local.entities.PaymentMethod.CREDIT -> {
                actualPaid = paidAmount.coerceAtLeast(0.0)
                changeAmount = 0.0
                remainingCreditAmount = (finalTotal - actualPaid).coerceAtLeast(0.0)
            }
            else -> {
                actualPaid = paidAmount
                changeAmount = (actualPaid - finalTotal).coerceAtLeast(0.0)
                remainingCreditAmount = (finalTotal - actualPaid).coerceAtLeast(0.0)
            }
        }

        // قراءة بيانات العميل قبل التحديث
        var customerOldBalance: Double? = null
        var customerNewBalance: Double? = null
        var customerName: String? = null

        if (partyId != null) {
            val party = partyDao.getPartyById(partyId)
            if (party != null) {
                customerName = party.name
                customerOldBalance = party.currentBalance
                if (paymentMethod == com.example.dokkani.data.local.entities.PaymentMethod.CREDIT && remainingCreditAmount > 0.001) {
                    partyDao.updateBalance(partyId, remainingCreditAmount)
                    customerNewBalance = party.currentBalance + remainingCreditAmount
                } else {
                    customerNewBalance = party.currentBalance
                }
            }
        }

        // 1. إنشاء الفاتورة
        val count = invoiceDao.countInvoicesByType(com.example.dokkani.data.local.entities.InvoiceType.SALE)
        val invoiceNumber = "INV-2026-%04d".format(count + 1)
        val now = System.currentTimeMillis()

        val invoiceEntity = InvoiceEntity(
            invoiceNumber = invoiceNumber,
            type = com.example.dokkani.data.local.entities.InvoiceType.SALE,
            partyId = partyId,
            date = now,
            currencyId = currency.id,
            exchangeRate = currency.exchangeRateToBase,
            subtotal = subtotal,
            discount = discount,
            taxRate = settings.defaultTaxRate,
            taxAmount = taxAmount,
            total = finalTotal,
            paidAmount = actualPaid,
            remainingAmount = remainingCreditAmount,
            paymentMethod = paymentMethod,
            status = com.example.dokkani.data.local.entities.InvoiceStatus.COMPLETED,
            notes = notes.ifBlank { "فاتورة نقطة بيع POS" }
        )

        val invoiceId = invoiceDao.insertInvoice(invoiceEntity)

        // 2. إدراج بنود الفاتورة مع تثبيت التكلفة وسعر البيع
        val invoiceItems = cartItems.map { item ->
            InvoiceItemEntity(
                invoiceId = invoiceId,
                productId = item.productId,
                productUnitId = item.unitId,
                quantity = item.quantity,
                unitConversionFactor = item.conversionFactor,
                unitCostPrice = item.costPrice,
                unitSellingPrice = item.unitPrice,
                discount = item.discount,
                taxRate = settings.defaultTaxRate,
                totalPrice = item.totalPrice
            )
        }
        invoiceDao.insertInvoiceItems(invoiceItems)

        // 3. خصم المخزون وتسجيل حركات خروج المخزون (SALE_OUT) واستهلاك طبقات FIFO
        val isFifo = settings.costValuationMethod == CostValuationMethod.FIFO

        for (item in cartItems) {
            val baseQtySold = item.quantity * item.conversionFactor
            if (baseQtySold > 0.0001 && item.productId > 0) {
                // تسجيل حركة الخروج
                stockMovementDao.insertMovement(
                    StockMovementEntity(
                        productId = item.productId,
                        productUnitId = item.unitId,
                        invoiceId = invoiceId,
                        movementType = MovementType.SALE_OUT,
                        quantityBaseUnit = -baseQtySold,
                        remainingQuantityForFifo = 0.0,
                        unitCostPriceBase = item.costPrice,
                        timestamp = now,
                        referenceNumber = invoiceNumber,
                        notes = "صرف مبيعات كاشير (${item.unitName})"
                    )
                )

                // إذا كان تقييم المخزون المعتمد FIFO، استهلاك الطبقات تدريجياً من الأقدم
                if (isFifo) {
                    var neededToDeduct = baseQtySold
                    val activeFifoLots = stockMovementDao.getAvailableFifoLots(item.productId)
                    for (lot in activeFifoLots) {
                        if (neededToDeduct <= 0.0001) break
                        if (lot.remainingQuantityForFifo > 0.0001) {
                            val deductFromThisLot = minOf(lot.remainingQuantityForFifo, neededToDeduct)
                            val updatedRemaining = lot.remainingQuantityForFifo - deductFromThisLot
                            stockMovementDao.updateMovement(lot.copy(remainingQuantityForFifo = updatedRemaining))
                            neededToDeduct -= deductFromThisLot
                        }
                    }
                }
            }
        }

        // 4. تجهيز بيانات الإيصال الحراري
        val receiptItems = cartItems.map { itm ->
            com.example.dokkani.domain.hardware.ReceiptItemData(
                name = itm.productName,
                quantityFormatted = itm.quantityFormatted,
                unitPrice = itm.unitPrice,
                totalPrice = itm.totalPrice,
                isWeighted = itm.isWeighted
            )
        }

        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
        val dateFormatted = dateFormat.format(java.util.Date(now))

        val receiptData = com.example.dokkani.domain.hardware.ReceiptPrintData(
            storeName = settings.storeName,
            storePhone = "0501234567",
            taxNumber = "300123456700003",
            invoiceNumber = invoiceNumber,
            invoiceDateFormatted = dateFormatted,
            cashierName = "كاشير 1",
            customerName = customerName,
            paymentMethodArabic = paymentMethod.labelArabic,
            items = receiptItems,
            subtotal = subtotal,
            discount = discount,
            taxRatePercent = settings.defaultTaxRate * 100,
            taxAmount = taxAmount,
            total = finalTotal,
            paidAmount = actualPaid,
            remainingAmount = remainingCreditAmount,
            customerOldBalance = customerOldBalance,
            customerNewBalance = customerNewBalance,
            currencySymbol = currency.symbol,
            footerText = settings.invoiceFooterText,
            qrCodePayload = "DOKKANI|INV:$invoiceNumber|TOTAL:%.2f|VAT:%.2f|DATE:$dateFormatted".format(finalTotal, taxAmount)
        )

        return com.example.dokkani.domain.pos.PosCheckoutResult(
            success = true,
            invoiceId = invoiceId,
            invoiceNumber = invoiceNumber,
            total = finalTotal,
            paymentMethod = paymentMethod,
            paidAmount = actualPaid,
            changeAmount = changeAmount,
            remainingCreditAmount = remainingCreditAmount,
            customerName = customerName,
            customerNewBalance = customerNewBalance,
            drawerKickTriggered = paymentMethod == com.example.dokkani.data.local.entities.PaymentMethod.CASH,
            receiptData = receiptData,
            message = "تم إصدار الفاتورة ($invoiceNumber) بنجاح!"
        )
    }
}

