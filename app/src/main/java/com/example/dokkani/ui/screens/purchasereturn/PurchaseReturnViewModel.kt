package com.example.dokkani.ui.screens.purchasereturn

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.example.dokkani.data.local.DokkaniDatabase
import com.example.dokkani.data.local.dao.CashShiftDao
import com.example.dokkani.data.local.entities.CashShiftEntity
import com.example.dokkani.data.local.entities.InvoiceEntity
import com.example.dokkani.data.local.entities.InvoiceItemEntity
import com.example.dokkani.data.local.entities.InvoiceStatus
import com.example.dokkani.data.local.entities.InvoiceType
import com.example.dokkani.data.local.entities.MovementType
import com.example.dokkani.data.local.entities.PartyEntity
import com.example.dokkani.data.local.entities.PaymentMethod
import com.example.dokkani.data.local.entities.StockMovementEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * بند من بنود مردود المشتريات
 * يعتمد بشكل صارم على سعر التكلفة الفعلي المسجل داخل الفاتورة الأصلية (invoice_items.unit_cost_price)
 * وليس سعر التكلفة الحالي في جدول الأصناف (products_units.cost_price).
 */
data class PurchaseReturnItem(
    val productId: Long,
    val productName: String,
    val productCode: String,
    val unitId: Long,
    val unitName: String,
    val conversionFactor: Double = 1.0,
    // القاعدة المحاسبية: سعر الشراء التاريخي المسجل داخل الفاتورة الأصلية كمرجع ثابت
    val originalUnitCostPrice: Double,
    // الكمية المشتراة بالفاتورة الأصلية (سقف الحد الأقصى للمردود)
    val originalPurchasedQuantity: Double,
    // كمية المردود الحالية القابلة للتعديل
    val returnQuantity: Double,
    // سعر الإرجاع الفعلي (افتراضياً يساوي سعر الشراء الأصلي التاريخي)
    val returnCostPrice: Double = originalUnitCostPrice,
    val isSelectedForReturn: Boolean = true,
    val notes: String = ""
) {
    val totalReturnCost: Double
        get() = if (isSelectedForReturn) (returnQuantity * returnCostPrice).coerceAtLeast(0.0) else 0.0

    val originalTotalLineCost: Double
        get() = originalPurchasedQuantity * originalUnitCostPrice
}

data class PurchaseReturnUiState(
    val purchaseInvoices: List<InvoiceEntity> = emptyList(),
    val searchQuery: String = "",
    val selectedInvoice: InvoiceEntity? = null,
    val supplier: PartyEntity? = null,
    val returnItems: List<PurchaseReturnItem> = emptyList(),
    val paymentMethod: PaymentMethod = PaymentMethod.CREDIT,
    val transactionRef: String = "",
    val receiptImagePath: String? = null,
    val returnNotes: String = "",
    val isProcessing: Boolean = false,
    val userFeedbackMessage: String? = null,
    val isError: Boolean = false,
    val currencySymbol: String = "ر.ي",
    val showSelectInvoiceDialog: Boolean = false,
    val showSuccessDialog: Boolean = false,
    val generatedReturnInvoiceNumber: String? = null,
    val showReturnQuantityWarningDialog: Boolean = false,
    val returnQuantityWarningMessage: String = ""
) {
    val totalReturnAmount: Double
        get() = returnItems.filter { it.isSelectedForReturn }.sumOf { it.totalReturnCost }

    val totalOriginalAmount: Double
        get() = returnItems.sumOf { it.originalTotalLineCost }

    val activeItemsCount: Int
        get() = returnItems.count { it.isSelectedForReturn && it.returnQuantity > 0.0 }
}

class PurchaseReturnViewModel(application: Application) : AndroidViewModel(application) {

    private val db = DokkaniDatabase.getDatabase(application, viewModelScope)
    private val invoiceDao = db.invoiceDao()
    private val productDao = db.productDao()
    private val partyDao = db.partyDao()
    private val stockMovementDao = db.stockMovementDao()
    private val shiftDao = db.cashShiftDao()

    private suspend fun getOrCreateOpenShift(dao: CashShiftDao): CashShiftEntity {
        var openShift = dao.getOpenShift()
        if (openShift == null) {
            val lastShift = dao.getLastShift()
            if (lastShift != null && lastShift.status == "OPEN") {
                openShift = lastShift
            } else {
                val now = System.currentTimeMillis()
                val opening = lastShift?.actualPhysicalCash ?: lastShift?.expectedCashInDrawer ?: 200.0
                val count = dao.countShifts() + 1
                val newShift = CashShiftEntity(
                    shiftNumber = "SHF-%04d".format(count),
                    cashierName = "كاشير 1",
                    startTime = now,
                    openingCash = opening,
                    totalCashSales = 0.0,
                    totalCashCollections = 0.0,
                    totalCashExpenses = 0.0,
                    expectedCashInDrawer = opening,
                    actualPhysicalCash = opening,
                    status = "OPEN",
                    notes = "فتح شفت تلقائي للنظام"
                )
                val id = dao.insertShift(newShift)
                openShift = newShift.copy(id = id)
            }
        }
        return openShift
    }
    private val currencyDao = db.currencyDao()

    private val _uiState = MutableStateFlow(PurchaseReturnUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadPurchaseInvoices()
        loadCurrencySymbol()
    }

    private fun loadCurrencySymbol() {
        viewModelScope.launch(Dispatchers.IO) {
            val baseCurrency = currencyDao.getBaseCurrency() ?: currencyDao.getDefaultCurrency()
            if (baseCurrency != null) {
                _uiState.update { it.copy(currencySymbol = baseCurrency.symbol) }
            }
        }
    }

    fun loadPurchaseInvoices() {
        viewModelScope.launch(Dispatchers.IO) {
            invoiceDao.getInvoicesByType(InvoiceType.PURCHASE).collect { list ->
                val allReturnInvoices = invoiceDao.getAllInvoicesSync().filter { it.type == InvoiceType.PURCHASE_RETURN }
                val allReturnItems = invoiceDao.getAllInvoiceItemsSync()
                val returnItemsByInvoiceId = allReturnItems.groupBy { it.invoiceId }

                // القاعدة 1: إخفاء تماماً أي فاتورة تم رد كافة بنودها وكمياتها بنسبة 100%
                val filteredList = list.filter { candidate ->
                    val originalItems = invoiceDao.getInvoiceItems(candidate.id)
                    if (originalItems.isEmpty()) return@filter false

                    val linkedReturns = allReturnInvoices.filter { ret ->
                        ret.transactionRef == candidate.invoiceNumber || ret.notes.contains(candidate.invoiceNumber)
                    }

                    val returnedQtyMap = mutableMapOf<Pair<Long, Long>, Double>()
                    for (retInv in linkedReturns) {
                        val retItems = returnItemsByInvoiceId[retInv.id] ?: emptyList()
                        for (retItem in retItems) {
                            val key = Pair(retItem.productId, retItem.productUnitId)
                            returnedQtyMap[key] = (returnedQtyMap[key] ?: 0.0) + retItem.quantity
                        }
                    }

                    originalItems.any { origItem ->
                        val alreadyReturned = returnedQtyMap[Pair(origItem.productId, origItem.productUnitId)] ?: 0.0
                        (origItem.quantity - alreadyReturned) > 0.0001
                    }
                }

                _uiState.update { it.copy(purchaseInvoices = filteredList) }
            }
        }
    }

    fun dismissReturnQuantityWarningDialog() {
        _uiState.update {
            it.copy(
                showReturnQuantityWarningDialog = false,
                returnQuantityWarningMessage = ""
            )
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun openSelectInvoiceDialog() {
        _uiState.update { it.copy(showSelectInvoiceDialog = true) }
    }

    fun dismissSelectInvoiceDialog() {
        _uiState.update { it.copy(showSelectInvoiceDialog = false) }
    }

    fun dismissFeedback() {
        _uiState.update { it.copy(userFeedbackMessage = null, isError = false) }
    }

    fun dismissSuccessDialog() {
        _uiState.update {
            it.copy(
                showSuccessDialog = false,
                generatedReturnInvoiceNumber = null,
                selectedInvoice = null,
                supplier = null,
                returnItems = emptyList(),
                returnNotes = ""
            )
        }
    }

    /**
     * اختيار فاتورة الشراء لغرض الإرجاع وتطبيق القواعد المحاسبية الصارمة:
     * - استبعاد أي صنف تم رده بنسبة 100%
     * - عرض الكمية المتبقية الفعلية فقط المتاحة للرد
     * - الاعتماد الحصري على التكلفة التاريخية من الفاتورة الأصلية
     */
    fun selectInvoiceForReturn(invoice: InvoiceEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isProcessing = true) }
            try {
                val items = invoiceDao.getInvoiceItems(invoice.id)
                val supplier = invoice.partyId?.let { partyDao.getPartyById(it) }
                val products = productDao.getAllProductsSync()
                val units = productDao.getAllUnitsSync()

                val allReturnInvoices = invoiceDao.getAllInvoicesSync().filter { it.type == InvoiceType.PURCHASE_RETURN }
                val allReturnItems = invoiceDao.getAllInvoiceItemsSync()
                val returnItemsByInvoiceId = allReturnItems.groupBy { it.invoiceId }

                val linkedReturns = allReturnInvoices.filter { ret ->
                    ret.transactionRef == invoice.invoiceNumber || ret.notes.contains(invoice.invoiceNumber)
                }

                val returnedQtyMap = mutableMapOf<Pair<Long, Long>, Double>()
                for (retInv in linkedReturns) {
                    val retItems = returnItemsByInvoiceId[retInv.id] ?: emptyList()
                    for (retItem in retItems) {
                        val key = Pair(retItem.productId, retItem.productUnitId)
                        returnedQtyMap[key] = (returnedQtyMap[key] ?: 0.0) + retItem.quantity
                    }
                }

                val returnList = mutableListOf<PurchaseReturnItem>()
                for (invItem in items) {
                    val prod = products.find { it.id == invItem.productId }
                    val unit = units.find { it.id == invItem.productUnitId }
                    if (prod != null && unit != null) {
                        val alreadyReturned = returnedQtyMap[Pair(invItem.productId, invItem.productUnitId)] ?: 0.0
                        val remainingReturnableQty = (invItem.quantity - alreadyReturned).coerceAtLeast(0.0)

                        if (remainingReturnableQty > 0.0001) {
                            val historicalCost = invItem.unitCostPrice

                            returnList.add(
                                PurchaseReturnItem(
                                    productId = prod.id,
                                    productName = prod.name,
                                    productCode = prod.code,
                                    unitId = unit.id,
                                    unitName = unit.unitName,
                                    conversionFactor = invItem.unitConversionFactor,
                                    originalUnitCostPrice = historicalCost,
                                    originalPurchasedQuantity = remainingReturnableQty, // الكمية المتبقية الفعلية القابلة للرد
                                    returnQuantity = remainingReturnableQty,
                                    returnCostPrice = historicalCost,
                                    isSelectedForReturn = true
                                )
                            )
                        }
                    }
                }

                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        selectedInvoice = invoice,
                        supplier = supplier,
                        returnItems = returnList,
                        paymentMethod = invoice.paymentMethod,
                        returnNotes = "مردود عن فاتورة شراء رقم: ${invoice.invoiceNumber}",
                        showSelectInvoiceDialog = false,
                        userFeedbackMessage = "تم تحميل الكميات المتبقية القابلة للرد من الفاتورة (${invoice.invoiceNumber}) مع أسعار التكلفة التاريخية.",
                        isError = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        userFeedbackMessage = "خطأ في تحميل الفاتورة: ${e.localizedMessage}",
                        isError = true
                    )
                }
            }
        }
    }

    /**
     * تعديل كمية المردود مع التحقق الصارم:
     * - يمنع التجاوز عن الكمية المتبقية القابلة للرد بالفاتورة
     * - يمنع التجاوز عن رصيد المخزن الفعلي المتاح
     */
    fun updateReturnQuantity(productId: Long, unitId: Long, newQty: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val state = _uiState.value
            val targetItem = state.returnItems.find { it.productId == productId && it.unitId == unitId } ?: return@launch

            // 1. التحقق الصارم من سقف الفاتورة الأصلي المتبقي
            if (newQty > targetItem.originalPurchasedQuantity + 0.0001) {
                val warningMsg = "الكمية المحددة للرد (${"%.2f".format(newQty)} ${targetItem.unitName}) تتجاوز الكمية المتبقية القابلة للرد في الفاتورة الأصلية (${"%.2f".format(targetItem.originalPurchasedQuantity)} ${targetItem.unitName})."
                _uiState.update {
                    it.copy(
                        showReturnQuantityWarningDialog = true,
                        returnQuantityWarningMessage = warningMsg
                    )
                }
                return@launch
            }

            // 2. التحقق الصارم من رصيد المخزن الفعلي المتاح
            val currentStockBase = stockMovementDao.getTotalStockQuantity(productId)
            val requestedStockBase = newQty * targetItem.conversionFactor
            if (requestedStockBase > currentStockBase + 0.0001) {
                val maxAllowedUnit = (currentStockBase / targetItem.conversionFactor).coerceAtLeast(0.0)
                val warningMsg = "الكمية المحددة لمردود المشتريات (${"%.2f".format(newQty)} ${targetItem.unitName}) تتجاوز رصيد المخزن الفعلي المتاح حالياً (${"%.2f".format(maxAllowedUnit)} ${targetItem.unitName})."
                _uiState.update {
                    it.copy(
                        showReturnQuantityWarningDialog = true,
                        returnQuantityWarningMessage = warningMsg
                    )
                }
                return@launch
            }

            val updatedItems = state.returnItems.map { item ->
                if (item.productId == productId && item.unitId == unitId) {
                    item.copy(returnQuantity = newQty.coerceAtLeast(0.0))
                } else item
            }

            _uiState.update {
                it.copy(returnItems = updatedItems)
            }
        }
    }

    /**
     * إمكانية تعديل سعر المردود إذا تم الاتفاق مع المورد على سعر خاص،
     * مع بقاء سعر الشراء الأصلي (originalUnitCostPrice) معروضاً كمرجع دائم.
     */
    fun updateReturnCostPrice(productId: Long, unitId: Long, newPrice: Double) {
        _uiState.update { state ->
            val updatedItems = state.returnItems.map { item ->
                if (item.productId == productId && item.unitId == unitId) {
                    item.copy(returnCostPrice = newPrice.coerceAtLeast(0.0))
                } else item
            }
            state.copy(returnItems = updatedItems)
        }
    }

    fun toggleItemSelection(productId: Long, unitId: Long, isSelected: Boolean) {
        _uiState.update { state ->
            val updated = state.returnItems.map { item ->
                if (item.productId == productId && item.unitId == unitId) {
                    item.copy(isSelectedForReturn = isSelected)
                } else item
            }
            state.copy(returnItems = updated)
        }
    }

    fun setAllQuantities(full: Boolean) {
        _uiState.update { state ->
            val updated = state.returnItems.map { item ->
                item.copy(
                    returnQuantity = if (full) item.originalPurchasedQuantity else 0.0,
                    isSelectedForReturn = full
                )
            }
            state.copy(returnItems = updated)
        }
    }

    fun setPaymentMethod(method: PaymentMethod) {
        _uiState.update { it.copy(paymentMethod = method) }
    }

    fun setTransactionRef(ref: String) {
        _uiState.update { it.copy(transactionRef = ref) }
    }

    fun setReceiptImagePath(path: String?) {
        _uiState.update { it.copy(receiptImagePath = path) }
    }

    fun setReturnNotes(notes: String) {
        _uiState.update { it.copy(returnNotes = notes) }
    }

    /**
     * دقة القيود المحاسبية (عكس التكلفة بدقة):
     * بناءً على الأسعار المسترجعة من الفاتورة الأصلية:
     * 1. إنشاء فاتورة مردود مشتريات (PURCHASE_RETURN).
     * 2. قيد إرجاع مخزني (تخفيض المخزون RETURN_OUT) بناءً على التكلفة التاريخية.
     * 3. قيد محاسبي: تخفيض مديونية المورد (إذا كانت الفاتورة آجلة) أو استرداد النقدية في صندوق الشفت.
     */
    fun confirmPurchaseReturn() {
        val state = _uiState.value
        val originalInvoice = state.selectedInvoice ?: run {
            _uiState.update { it.copy(userFeedbackMessage = "يرجى اختيار فاتورة الشراء الأصلية أولاً", isError = true) }
            return
        }

        val selectedItems = state.returnItems.filter { it.isSelectedForReturn && it.returnQuantity > 0.001 }
        if (selectedItems.isEmpty()) {
            _uiState.update { it.copy(userFeedbackMessage = "يرجى تحديد كمية موجبة لصنف واحد على الأقل للمردود", isError = true) }
            return
        }

        val totalReturnAmount = state.totalReturnAmount
        val timeNow = System.currentTimeMillis()
        val dateFormatted = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ENGLISH).format(Date(timeNow))
        val returnInvoiceNumber = "PRTN-$dateFormatted"

        viewModelScope.launch(Dispatchers.IO) {
            // التحقق الرقابي من الكميات وتوفر المخزون قبل التأكيد
            for (item in selectedItems) {
                if (item.returnQuantity > item.originalPurchasedQuantity + 0.0001) {
                    val warningMsg = "الكمية المحددة للرد من الصنف (${item.productName}) تبلغ (${"%.2f".format(item.returnQuantity)} ${item.unitName}) وهي تتجاوز الكمية المتبقية القابلة للرد في الفاتورة الأصلية (${"%.2f".format(item.originalPurchasedQuantity)} ${item.unitName})."
                    _uiState.update {
                        it.copy(
                            showReturnQuantityWarningDialog = true,
                            returnQuantityWarningMessage = warningMsg
                        )
                    }
                    return@launch
                }

                val currentStockBase = stockMovementDao.getTotalStockQuantity(item.productId)
                val requestedStockBase = item.returnQuantity * item.conversionFactor
                if (requestedStockBase > currentStockBase + 0.0001) {
                    val maxAllowedUnit = (currentStockBase / item.conversionFactor).coerceAtLeast(0.0)
                    val warningMsg = "الكمية المحددة لمردود المشتريات من الصنف (${item.productName}) تبلغ (${"%.2f".format(item.returnQuantity)} ${item.unitName}) وهي تتجاوز رصيد المخزن الفعلي المتاح حالياً (${"%.2f".format(maxAllowedUnit)} ${item.unitName})."
                    _uiState.update {
                        it.copy(
                            showReturnQuantityWarningDialog = true,
                            returnQuantityWarningMessage = warningMsg
                        )
                    }
                    return@launch
                }
            }

            _uiState.update { it.copy(isProcessing = true) }
            try {
                db.withTransaction {
                    // 1. إدراج فاتورة مردود المشتريات مع ربط مرجع الفاتورة الأصلية
                    val returnInvoice = InvoiceEntity(
                        invoiceNumber = returnInvoiceNumber,
                        type = InvoiceType.PURCHASE_RETURN,
                        status = InvoiceStatus.COMPLETED,
                        partyId = originalInvoice.partyId,
                        currencyId = originalInvoice.currencyId,
                        exchangeRate = originalInvoice.exchangeRate,
                        subtotal = totalReturnAmount,
                        discount = 0.0,
                        taxRate = 0.0,
                        taxAmount = 0.0,
                        total = totalReturnAmount,
                        paymentMethod = state.paymentMethod,
                        paidAmount = if (state.paymentMethod == PaymentMethod.CASH) totalReturnAmount else 0.0,
                        remainingAmount = if (state.paymentMethod == PaymentMethod.CREDIT) totalReturnAmount else 0.0,
                        transactionRef = state.transactionRef.ifBlank { originalInvoice.invoiceNumber },
                        receiptImagePath = state.receiptImagePath,
                        notes = state.returnNotes.ifBlank { "مردود مشتريات عن الفاتورة الأصلية: ${originalInvoice.invoiceNumber}" },
                        date = timeNow
                    )
                    val insertedReturnId = invoiceDao.insertInvoice(returnInvoice)

                    // 2. إدراج بنود الفاتورة وحركات المخزون المعكوسة بدقة
                    val invoiceItemsToInsert = mutableListOf<InvoiceItemEntity>()
                    val stockMovementsToInsert = mutableListOf<StockMovementEntity>()

                    for (item in selectedItems) {
                        invoiceItemsToInsert.add(
                            InvoiceItemEntity(
                                invoiceId = insertedReturnId,
                                productId = item.productId,
                                productUnitId = item.unitId,
                                quantity = item.returnQuantity,
                                unitSellingPrice = item.returnCostPrice,
                                // حفظ التكلفة التاريخية المسجلة بالفاتورة الأصلية
                                unitCostPrice = item.originalUnitCostPrice,
                                unitConversionFactor = item.conversionFactor,
                                totalPrice = item.totalReturnCost
                            )
                        )

                        // حركة مخزنية: تخفيض المخزون (RETURN_OUT) بالتكلفة التاريخية الدقيقة
                        stockMovementsToInsert.add(
                            StockMovementEntity(
                                productId = item.productId,
                                productUnitId = item.unitId,
                                invoiceId = insertedReturnId,
                                movementType = MovementType.RETURN_OUT,
                                // تخفيض المخزون بالوحدة الأساسية
                                quantityBaseUnit = -(item.returnQuantity * item.conversionFactor),
                                remainingQuantityForFifo = 0.0,
                                unitCostPriceBase = item.returnCostPrice / item.conversionFactor,
                                timestamp = timeNow,
                                referenceNumber = returnInvoiceNumber,
                                notes = "مردود مشتريات عن فاتورة: ${originalInvoice.invoiceNumber}"
                            )
                        )
                    }

                    invoiceDao.insertInvoiceItems(invoiceItemsToInsert)
                    stockMovementDao.insertMovements(stockMovementsToInsert)

                    // 3. القيود المحاسبية والمالية:
                    if (state.paymentMethod == PaymentMethod.CREDIT && originalInvoice.partyId != null) {
                        // تخفيض مديونية المورد (في نظام دكاني: رصيد المورد دائن بالسالب، لذا زيادة الرصيد بموجب يخفض المديونية)
                        partyDao.updateBalance(originalInvoice.partyId, totalReturnAmount)
                    } else if (state.paymentMethod == PaymentMethod.CASH) {
                        // استرداد النقدية من المورد وإيداعها في الصندوق / شفت الكاشير المفتوح
                        val openShift = getOrCreateOpenShift(shiftDao)
                        val newColl = openShift.totalCashCollections + totalReturnAmount
                        shiftDao.updateCollections(openShift.id, newColl)
                    }
                }

                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        showSuccessDialog = true,
                        transactionRef = "",
                        receiptImagePath = null,
                        returnNotes = "",
                        generatedReturnInvoiceNumber = returnInvoiceNumber,
                        userFeedbackMessage = "تم اعتماد مردود المشتريات وتوليد القيود المحاسبية والمخزنية بنجاح برقم: $returnInvoiceNumber",
                        isError = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        userFeedbackMessage = "خطأ أثناء حفظ مردود المشتريات: ${e.localizedMessage}",
                        isError = true
                    )
                }
            }
        }
    }
}
