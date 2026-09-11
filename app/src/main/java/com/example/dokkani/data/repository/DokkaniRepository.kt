package com.example.dokkani.data.repository

import com.example.dokkani.data.local.DokkaniDatabase
import com.example.dokkani.data.local.entities.BatchWithYields
import com.example.dokkani.data.local.entities.CashShiftEntity
import com.example.dokkani.data.local.entities.CostValuationMethod
import com.example.dokkani.data.local.entities.CurrencyEntity
import com.example.dokkani.data.local.entities.ExpenseEntity
import com.example.dokkani.data.local.entities.InvoiceEntity
import com.example.dokkani.data.local.entities.InvoiceItemEntity
import com.example.dokkani.data.local.entities.InvoiceWithDetails
import com.example.dokkani.data.local.entities.MixedProduceBatchEntity
import com.example.dokkani.data.local.entities.MixedProduceYieldItemEntity
import com.example.dokkani.data.local.entities.MovementType
import com.example.dokkani.data.local.entities.PartyEntity
import com.example.dokkani.data.local.entities.PaymentMethod
import com.example.dokkani.data.local.entities.PaymentVoucherEntity
import com.example.dokkani.data.local.entities.ProductEntity
import com.example.dokkani.data.local.entities.ProductUnitEntity
import com.example.dokkani.data.local.entities.ProductWithUnits
import com.example.dokkani.data.local.entities.StockMovementEntity
import com.example.dokkani.data.local.entities.SystemSettingsEntity
import com.example.dokkani.domain.cash.CashDrawerEngine
import com.example.dokkani.domain.cash.CashReconciliationResult
import com.example.dokkani.domain.costing.CostCalculationEngine
import com.example.dokkani.domain.costing.CostCalculationResult
import com.example.dokkani.domain.credit.CreditNotebookEngine
import com.example.dokkani.domain.credit.CustomerStatementSummary
import com.example.dokkani.domain.produce.ProduceQuickCalcSummary
import com.example.dokkani.domain.produce.ProduceQuickInventoryEngine
import com.example.dokkani.domain.reports.FinancialReportsEngine
import com.example.dokkani.domain.reports.InventoryHealthReport
import com.example.dokkani.domain.reports.ProfitAndLossReport
import com.example.dokkani.domain.reports.TopProductsReport
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
    private val paymentVoucherDao = database.paymentVoucherDao()
    private val expenseDao = database.expenseDao()
    private val cashShiftDao = database.cashShiftDao()
    private val licenseDao = database.licenseDao()

    val costingEngine = CostCalculationEngine(productDao, stockMovementDao, settingsDao)

    // Flow streams for reactive UI
    val allProductsWithUnits: Flow<List<ProductWithUnits>> = productDao.getProductsWithUnits()
    val allCurrencies: Flow<List<CurrencyEntity>> = currencyDao.getAllCurrencies()
    val allParties: Flow<List<PartyEntity>> = partyDao.getAllParties()
    val recentInvoices: Flow<List<InvoiceWithDetails>> = invoiceDao.getRecentInvoicesWithDetails()
    val produceBatchesWithYields: Flow<List<BatchWithYields>> = produceBatchDao.getBatchesWithYields()
    val systemSettings: Flow<SystemSettingsEntity?> = settingsDao.getSettings()
    val allPaymentVouchers: Flow<List<PaymentVoucherEntity>> = paymentVoucherDao.getAllVouchers()
    val allExpenses: Flow<List<ExpenseEntity>> = expenseDao.getAllExpenses()
    val allCashShifts: Flow<List<CashShiftEntity>> = cashShiftDao.getAllShifts()
    val licenseFlow: Flow<com.example.dokkani.data.local.entities.LicenseEntity?> = licenseDao.getLicenseFlow()
    val totalInvoicesCountFlow: Flow<Int> = invoiceDao.getTotalInvoicesCountFlow()

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

    /**
     * تطبيق وتسجيل قيود التسوية المخزنية الصامتة لإنهاء حركات يوم الخضار
     */
    suspend fun applySilentInventoryAdjustments(
        adjustments: List<com.example.dokkani.domain.produce.SilentAdjustmentEntry>
    ): Int {
        val now = System.currentTimeMillis()
        var appliedCount = 0
        for (adj in adjustments) {
            val movement = StockMovementEntity(
                productId = adj.productId,
                productUnitId = null,
                invoiceId = null,
                movementType = adj.movementType,
                quantityBaseUnit = adj.quantityChange,
                remainingQuantityForFifo = if (adj.quantityChange > 0) adj.quantityChange else 0.0,
                unitCostPriceBase = adj.unitCost,
                timestamp = now,
                referenceNumber = adj.referenceNumber,
                notes = adj.reasonArabic
            )
            stockMovementDao.insertMovement(movement)
            appliedCount++
        }
        return appliedCount
    }

    /**
     * توليد باركود فريد للوحدات الفردية (كالحبات/العبوات المستخرجة من الكراتين)
     * والتحقق من عدم تكراره وتخزينه مباشرة في جدول product_units
     */
    suspend fun generateAndSaveUniqueBarcodeForUnit(productId: Long, unitId: Long): String {
        val existingUnit = productDao.getUnitById(unitId)
            ?: throw IllegalArgumentException("الوحدة رقم ($unitId) غير موجودة في قاعدة البيانات!")

        // توليد باركود فريد والتأكد من عدم تكراره
        var generatedBarcode = com.example.dokkani.domain.barcode.BarcodeGenerator.generateUniqueSingleUnitBarcode(productId, unitId)
        var attempts = 0
        while (productDao.findUnitByBarcode(generatedBarcode) != null && attempts < 10) {
            attempts++
            generatedBarcode = com.example.dokkani.domain.barcode.BarcodeGenerator.generateEan13Barcode(
                uniqueNumber = System.currentTimeMillis() + attempts
            )
        }

        // تحديث الوحدة بالباركود الجديد
        val updatedUnit = existingUnit.copy(barcode = generatedBarcode)
        productDao.updateUnit(updatedUnit)
        return generatedBarcode
    }

    /**
     * تحديث باركود وحدة محددة يدوياً
     */
    suspend fun updateUnitBarcode(unitId: Long, newBarcode: String) {
        val existing = productDao.getUnitById(unitId) ?: return
        productDao.updateUnit(existing.copy(barcode = newBarcode.trim()))
    }

    // =========================================================================
    // موديول الديون ودفتر الشكك وسندات القبض (Credit & Customer Notebook)
    // =========================================================================

    /**
     * تسجيل سند قبض وتسديد دفعة لعميل أو مورد مع تحديث الرصيد آلياً
     */
    suspend fun recordPaymentVoucher(
        partyId: Long,
        amount: Double,
        paymentMethod: PaymentMethod = PaymentMethod.CASH,
        notes: String = "",
        receivedBy: String = "كاشير 1"
    ): PaymentVoucherEntity {
        if (amount <= 0.0) {
            throw IllegalArgumentException("مبلغ السداد يجب أن يكون أكبر من الصفر!")
        }

        val party = partyDao.getPartyById(partyId)
            ?: throw IllegalArgumentException("العميل غير موجود!")

        val count = paymentVoucherDao.countVouchers()
        val voucherNumber = "RCV-2026-%04d".format(count + 1)
        val now = System.currentTimeMillis()

        val voucher = PaymentVoucherEntity(
            voucherNumber = voucherNumber,
            partyId = partyId,
            amount = amount,
            paymentMethod = paymentMethod,
            date = now,
            receivedBy = receivedBy,
            notes = notes.ifBlank { "سند قبض وسداد دفعة حساب" }
        )

        paymentVoucherDao.insertVoucher(voucher)

        // تحديث رصيد العميل آلياً في جدول parties (تقليل الدين بمقدار المبلغ المسدد)
        partyDao.updateBalance(partyId, -amount)

        return voucher
    }

    /**
     * جلب كشف حساب زمني تفصيلي لعميل محدد
     */
    suspend fun getCustomerStatement(partyId: Long): CustomerStatementSummary? {
        val party = partyDao.getPartyById(partyId) ?: return null
        val invoices = invoiceDao.getInvoicesForPartySync(partyId)
        val vouchers = paymentVoucherDao.getVouchersForPartySync(partyId)
        val settings = settingsDao.getSettingsSync() ?: SystemSettingsEntity()

        return CreditNotebookEngine.buildCustomerStatement(
            party = party,
            invoices = invoices,
            vouchers = vouchers,
            storeName = settings.storeName
        )
    }

    // =========================================================================
    // موديول المصروفات وحركة الصندوق وإغلاق الشفت (Cash Drawer & Expenses)
    // =========================================================================

    /**
     * تسجيل مصروف تشغيلي ونثريات
     */
    suspend fun recordExpense(
        category: String,
        amount: Double,
        paymentMethod: PaymentMethod = PaymentMethod.CASH,
        paidTo: String = "",
        notes: String = "",
        recordedBy: String = "كاشير 1"
    ): ExpenseEntity {
        if (amount <= 0.0) {
            throw IllegalArgumentException("قيمة المصروف يجب أن تكون أكبر من الصفر!")
        }

        val count = expenseDao.countExpenses()
        val expenseNumber = "EXP-2026-%04d".format(count + 1)
        val now = System.currentTimeMillis()

        val expense = ExpenseEntity(
            expenseNumber = expenseNumber,
            category = category,
            amount = amount,
            paymentMethod = paymentMethod,
            date = now,
            paidTo = paidTo,
            notes = notes,
            recordedBy = recordedBy
        )

        expenseDao.insertExpense(expense)
        return expense
    }

    /**
     * حساب مطابقة النقدية اللحظية في الدرج
     */
    suspend fun calculateCashReconciliation(
        openingCash: Double,
        actualPhysicalCash: Double,
        startTime: Long = 0L,
        endTime: Long = System.currentTimeMillis()
    ): CashReconciliationResult {
        val invoices = invoiceDao.getInvoicesByDateRangeSync(startTime, endTime)
        val vouchers = paymentVoucherDao.getVouchersByDateRangeSync(startTime, endTime)
        val expenses = expenseDao.getExpensesByDateRangeSync(startTime, endTime)

        val cashSales = invoices
            .filter { it.type == com.example.dokkani.data.local.entities.InvoiceType.SALE && it.paymentMethod == PaymentMethod.CASH }
            .map { it.paidAmount }

        val cashCollections = vouchers
            .filter { it.paymentMethod == PaymentMethod.CASH }
            .map { it.amount }

        val cashExpenses = expenses
            .filter { it.paymentMethod == PaymentMethod.CASH }
            .map { it.amount }

        return CashDrawerEngine.calculateReconciliation(
            openingCash = openingCash,
            cashSales = cashSales,
            cashCollections = cashCollections,
            cashExpenses = cashExpenses,
            actualPhysicalCash = actualPhysicalCash
        )
    }

    /**
     * إغلاق الشفت وحفظ سجل المطابقة رسمياً في جدول cash_shifts
     */
    suspend fun closeShiftAndSave(
        openingCash: Double,
        actualPhysicalCash: Double,
        cashierName: String = "كاشير 1",
        notes: String = ""
    ): CashShiftEntity {
        val now = System.currentTimeMillis()
        val startOfToday = now - (12 * 3600000L) // فترة الشفت الحالي
        val recon = calculateCashReconciliation(openingCash, actualPhysicalCash, startOfToday, now)

        val count = cashShiftDao.countShifts()
        val shiftNumber = "SHF-2026-%04d".format(count + 1)

        val shift = CashShiftEntity(
            shiftNumber = shiftNumber,
            cashierName = cashierName,
            startTime = startOfToday,
            endTime = now,
            openingCash = recon.openingCash,
            totalCashSales = recon.totalCashSales,
            totalCashCollections = recon.totalCashCollections,
            totalCashExpenses = recon.totalCashExpenses,
            expectedCashInDrawer = recon.expectedCashInDrawer,
            actualPhysicalCash = recon.actualPhysicalCash,
            cashDiscrepancy = recon.discrepancy,
            status = "CLOSED",
            notes = notes.ifBlank { "إغلاق شفت ومطابقة النقدية: ${recon.discrepancyType.labelArabic}" }
        )

        cashShiftDao.insertShift(shift)
        return shift
    }

    // =========================================================================
    // موديول لوحة التحكم والتقارير المالية والمخزنية (Dashboard & Reports)
    // =========================================================================

    /**
     * توليد تقرير الأرباح والخسائر الشامل (P&L) المتوافق مع طريقة تقييم المخزون المحددة
     */
    suspend fun generateProfitAndLossReport(
        methodOverride: CostValuationMethod? = null
    ): ProfitAndLossReport {
        val settings = settingsDao.getSettingsSync() ?: SystemSettingsEntity()
        val method = methodOverride ?: settings.costValuationMethod

        val invoices = invoiceDao.getAllInvoicesWithDetailsSync().map { it.invoice }
        val invoiceItems = invoiceDao.getAllInvoiceItemsSync()
        val expenses = expenseDao.getExpensesByDateRangeSync(0, Long.MAX_VALUE)

        // حساب تكاليف الأصناف وفقاً لطريقة التقييم المحددة (WAC / FIFO / Last Purchase)
        val products = productDao.getProductsSync()
        val productUnitCosts = mutableMapOf<Long, Double>()
        for (prod in products) {
            val costResult = costingEngine.calculateProductCost(prod.id, method)
            productUnitCosts[prod.id] = costResult.unitCostBase
        }

        return FinancialReportsEngine.generateProfitAndLossReport(
            invoices = invoices,
            invoiceItems = invoiceItems,
            expenses = expenses,
            productUnitCosts = productUnitCosts,
            valuationMethod = method
        )
    }

    /**
     * توليد تقرير الأصناف الأكثر حركة وربحية
     */
    suspend fun generateTopProductsReport(
        methodOverride: CostValuationMethod? = null
    ): TopProductsReport {
        val settings = settingsDao.getSettingsSync() ?: SystemSettingsEntity()
        val method = methodOverride ?: settings.costValuationMethod

        val productsWithUnits = productDao.getProductsWithUnitsSync()
        val invoiceItems = invoiceDao.getAllInvoiceItemsSync()

        val productUnitCosts = mutableMapOf<Long, Double>()
        for (pwu in productsWithUnits) {
            val costResult = costingEngine.calculateProductCost(pwu.product.id, method)
            productUnitCosts[pwu.product.id] = costResult.unitCostBase
        }

        return FinancialReportsEngine.generateTopProductsReport(
            productsWithUnits = productsWithUnits,
            invoiceItems = invoiceItems,
            productUnitCosts = productUnitCosts
        )
    }

    /**
     * توليد تقرير نواقص المخزون وتواريخ الصلاحية
     */
    suspend fun generateInventoryHealthReport(): InventoryHealthReport {
        val productsWithUnits = productDao.getProductsWithUnitsSync()
        val stockMovements = stockMovementDao.getAllMovementsSync()

        return FinancialReportsEngine.generateInventoryHealthReport(
            productsWithUnits = productsWithUnits,
            stockMovements = stockMovements
        )
    }

    // ==========================================
    // إدارة الترخيص والحماية بدون إنترنت
    // ==========================================

    suspend fun getLicenseSync(): com.example.dokkani.data.local.entities.LicenseEntity? {
        return licenseDao.getLicenseSync()
    }

    suspend fun saveLicense(license: com.example.dokkani.data.local.entities.LicenseEntity) {
        licenseDao.insertOrUpdate(license)
    }

    suspend fun updateLastKnownTime(time: Long) {
        licenseDao.updateLastKnownTime(time)
    }

    suspend fun updateTamperState(isTampered: Boolean, reason: String?) {
        licenseDao.updateTamperState(isTampered, reason)
    }

    suspend fun getTotalInvoicesCountSync(): Int {
        return invoiceDao.getTotalInvoicesCountSync()
    }

    suspend fun getLatestInvoiceTimestampSync(): Long? {
        return invoiceDao.getLatestInvoiceTimestampSync()
    }
}


