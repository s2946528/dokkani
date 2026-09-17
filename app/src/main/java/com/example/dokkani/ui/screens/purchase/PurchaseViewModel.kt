package com.example.dokkani.ui.screens.purchase

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.example.dokkani.data.local.DokkaniDatabase
import com.example.dokkani.data.local.entities.CurrencyEntity
import com.example.dokkani.data.local.entities.InvoiceEntity
import com.example.dokkani.data.local.entities.InvoiceItemEntity
import com.example.dokkani.data.local.entities.InvoiceStatus
import com.example.dokkani.data.local.entities.InvoiceType
import com.example.dokkani.data.local.entities.MovementType
import com.example.dokkani.data.local.entities.PartyEntity
import com.example.dokkani.data.local.entities.PartyType
import com.example.dokkani.data.local.entities.PaymentMethod
import com.example.dokkani.data.local.entities.ProductEntity
import com.example.dokkani.data.local.entities.ProductUnitEntity
import com.example.dokkani.data.local.entities.ProductWithUnits
import com.example.dokkani.data.local.entities.StockMovementEntity
import com.example.dokkani.data.local.entities.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * إجراء يتطلب صلاحيات مدير النظام
 */
sealed class PendingAdminAction {
    data class DeleteInvoice(val invoice: InvoiceEntity) : PendingAdminAction()
    data class EditInvoice(val invoice: InvoiceEntity) : PendingAdminAction()
}

/**
 * بند شراء بضاعة في فاتورة التوريد
 */
data class PurchaseLineItem(
    val productId: Long,
    val productName: String,
    val productCode: String,
    val unitId: Long,
    val unitName: String,
    val conversionFactor: Double,
    val quantity: Double,
    val costPrice: Double,         // سعر الشراء والتكلفة للوحدة بالعملة المحددة
    val oldCostPrice: Double,      // سعر التكلفة الحالي المسجل قبل الشراء
    val sellingPrice: Double,      // سعر البيع الحالي
    val newSellingPrice: Double? = null // سعر البيع الجديد المقترح (اختياري)
) {
    val totalCost: Double get() = quantity * costPrice
}

/**
 * ملخص نتيجة احتساب WAC بعد التوريد
 */
data class WacCalculationSummary(
    val productName: String,
    val unitName: String,
    val oldCost: Double,
    val purchaseCost: Double,
    val newWacCost: Double,
    val oldStock: Double,
    val newStock: Double
)

/**
 * حالة شاشة المشتريات والتوريد (Purchase UI State)
 */
data class PurchaseUiState(
    val suppliers: List<PartyEntity> = emptyList(),
    val selectedSupplier: PartyEntity? = null,
    val supplierInvoiceNumber: String = "",
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val productsWithUnits: List<ProductWithUnits> = emptyList(),
    val searchQuery: String = "",
    val items: List<PurchaseLineItem> = emptyList(),
    val isTaxApplied: Boolean = true,
    val purchaseTaxRate: Double = 0.15,
    val discount: Double = 0.0,
    val notes: String = "",
    val isProcessing: Boolean = false,
    val lastSavedInvoiceNumber: String? = null,
    val wacUpdates: List<WacCalculationSummary> = emptyList(),
    val showSuccessDialog: Boolean = false,
    val showAddSupplierDialog: Boolean = false,
    val feedbackMessage: String? = null,
    val isError: Boolean = false,

    // العملة وسعر الصرف
    val availableCurrencies: List<CurrencyEntity> = emptyList(),
    val selectedCurrency: CurrencyEntity? = null,
    val exchangeRate: Double = 1.0,
    val currencySymbol: String = "ر.س",
    val currencyName: String = "الريال السعودي",
    val baseCurrencyId: Long = 1L,
    val baseCurrencySymbol: String = "ر.س",

    // استعراض فواتير الشراء والتعديل والحذف
    val purchaseInvoices: List<InvoiceEntity> = emptyList(),
    val invoiceSearchQuery: String = "",
    val showInvoiceDetailsDialog: Boolean = false,
    val selectedInvoiceWithDetails: List<InvoiceItemEntity> = emptyList(),
    val selectedInvoice: InvoiceEntity? = null,
    val showEditPurchaseDialog: Boolean = false,
    val editPurchaseNotes: String = "",
    val editPurchasePaymentMethod: PaymentMethod = PaymentMethod.CASH,
    val editPurchaseSupplier: PartyEntity? = null,
    val editPurchaseCurrency: CurrencyEntity? = null,
    val editPurchaseExchangeRate: Double = 1.0,
    val editPurchaseItems: List<PurchaseLineItem> = emptyList(),
    val isBottomHistoryExpanded: Boolean = true,

    // حماية وصلاحيات مدير النظام
    val showAdminPinDialog: Boolean = false,
    val pendingAdminAction: PendingAdminAction? = null,
    val adminPinError: String? = null
) {
    val subtotal: Double get() = items.sumOf { it.totalCost }
    val taxableAmount: Double get() = (subtotal - discount).coerceAtLeast(0.0)
    val taxAmount: Double get() = if (isTaxApplied) taxableAmount * purchaseTaxRate else 0.0
    val finalTotal: Double get() = taxableAmount + taxAmount
    val totalQuantity: Double get() = items.sumOf { it.quantity }

    // المعادلة بالعملة المحلية الأساسية
    val subtotalBaseCurrency: Double get() = subtotal * exchangeRate
    val taxAmountBaseCurrency: Double get() = taxAmount * exchangeRate
    val finalTotalBaseCurrency: Double get() = finalTotal * exchangeRate
}

class PurchaseViewModel(application: Application) : AndroidViewModel(application) {

    private val db = DokkaniDatabase.getDatabase(application, viewModelScope)
    private val productDao = db.productDao()
    private val partyDao = db.partyDao()
    private val invoiceDao = db.invoiceDao()
    private val stockMovementDao = db.stockMovementDao()
    private val shiftDao = db.cashShiftDao()

    private val _uiState = MutableStateFlow(PurchaseUiState())
    val uiState: StateFlow<PurchaseUiState> = _uiState.asStateFlow()

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch(Dispatchers.IO) {
            partyDao.getPartiesByType(PartyType.SUPPLIER).collectLatest { suppliers ->
                _uiState.update { state ->
                    val updatedSel = state.selectedSupplier?.let { s -> suppliers.find { it.id == s.id } }
                        ?: suppliers.firstOrNull()
                    state.copy(suppliers = suppliers, selectedSupplier = updatedSel)
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            productDao.getProductsWithUnits().collectLatest { products ->
                _uiState.update { it.copy(productsWithUnits = products) }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            invoiceDao.getAllInvoices().collectLatest { invoices ->
                val purchases = invoices.filter { it.type == InvoiceType.PURCHASE || it.type == InvoiceType.PURCHASE_RETURN }
                _uiState.update { it.copy(purchaseInvoices = purchases) }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            db.currencyDao().getAllCurrencies().collectLatest { currencies ->
                val base = currencies.find { it.isBaseCurrency } ?: currencies.firstOrNull()
                _uiState.update { state ->
                    val selCurr = state.selectedCurrency ?: base
                    state.copy(
                        availableCurrencies = currencies,
                        selectedCurrency = selCurr,
                        currencySymbol = selCurr?.symbol ?: "ر.س",
                        currencyName = selCurr?.name ?: "الريال السعودي",
                        exchangeRate = selCurr?.exchangeRateToBase ?: 1.0,
                        baseCurrencyId = base?.id ?: 1L,
                        baseCurrencySymbol = base?.symbol ?: "ر.س"
                    )
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            db.systemSettingsDao().getSettings().collectLatest { settings ->
                if (settings != null) {
                    _uiState.update { state ->
                        state.copy(
                            isTaxApplied = settings.isPurchaseTaxEnabled,
                            purchaseTaxRate = settings.purchaseTaxRate
                        )
                    }
                }
            }
        }
    }

    fun selectCurrency(currency: CurrencyEntity) {
        _uiState.update { state ->
            state.copy(
                selectedCurrency = currency,
                currencySymbol = currency.symbol,
                currencyName = currency.name,
                exchangeRate = currency.exchangeRateToBase
            )
        }
    }

    fun setExchangeRate(rate: Double) {
        if (rate <= 0.0) return
        _uiState.update { it.copy(exchangeRate = rate) }
    }

    fun selectSupplier(supplier: PartyEntity?) {
        _uiState.update { it.copy(selectedSupplier = supplier) }
    }

    fun setSupplierInvoiceNumber(number: String) {
        _uiState.update { it.copy(supplierInvoiceNumber = number) }
    }

    fun setPaymentMethod(method: PaymentMethod) {
        _uiState.update { it.copy(paymentMethod = method) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setTaxApplied(applied: Boolean) {
        _uiState.update { it.copy(isTaxApplied = applied) }
    }

    fun setDiscount(discount: Double) {
        _uiState.update { it.copy(discount = discount) }
    }

    fun setNotes(notes: String) {
        _uiState.update { it.copy(notes = notes) }
    }

    fun addProductItem(product: ProductEntity, unit: ProductUnitEntity, quantity: Double = 1.0, costPrice: Double = unit.costPrice) {
        _uiState.update { state ->
            val existingIndex = state.items.indexOfFirst { it.productId == product.id && it.unitId == unit.id }
            val updated = state.items.toMutableList()

            if (existingIndex >= 0) {
                val existing = updated[existingIndex]
                updated[existingIndex] = existing.copy(
                    quantity = existing.quantity + quantity,
                    costPrice = costPrice
                )
            } else {
                updated.add(
                    PurchaseLineItem(
                        productId = product.id,
                        productName = product.name,
                        productCode = product.code,
                        unitId = unit.id,
                        unitName = unit.unitName,
                        conversionFactor = unit.conversionFactor,
                        quantity = quantity,
                        costPrice = costPrice,
                        oldCostPrice = unit.costPrice,
                        sellingPrice = unit.sellingPrice
                    )
                )
            }
            state.copy(items = updated)
        }
    }

    fun updateItemQuantity(productId: Long, unitId: Long, newQty: Double) {
        _uiState.update { state ->
            val updated = if (newQty <= 0.001) {
                state.items.filterNot { it.productId == productId && it.unitId == unitId }
            } else {
                state.items.map {
                    if (it.productId == productId && it.unitId == unitId) it.copy(quantity = newQty) else it
                }
            }
            state.copy(items = updated)
        }
    }

    fun updateItemCostPrice(productId: Long, unitId: Long, newCost: Double) {
        _uiState.update { state ->
            val updated = state.items.map {
                if (it.productId == productId && it.unitId == unitId) it.copy(costPrice = newCost) else it
            }
            state.copy(items = updated)
        }
    }

    fun updateItemSellingPrice(productId: Long, unitId: Long, newSellingPrice: Double) {
        _uiState.update { state ->
            val updated = state.items.map {
                if (it.productId == productId && it.unitId == unitId) it.copy(newSellingPrice = newSellingPrice) else it
            }
            state.copy(items = updated)
        }
    }

    fun removeItem(productId: Long, unitId: Long) {
        _uiState.update { state ->
            state.copy(items = state.items.filterNot { it.productId == productId && it.unitId == unitId })
        }
    }

    fun clearInvoice() {
        _uiState.update {
            it.copy(
                items = emptyList(),
                supplierInvoiceNumber = "",
                discount = 0.0,
                notes = "",
                wacUpdates = emptyList(),
                lastSavedInvoiceNumber = null,
                showSuccessDialog = false,
                feedbackMessage = null
            )
        }
    }

    fun dismissSuccessDialog() {
        _uiState.update { it.copy(showSuccessDialog = false) }
    }

    fun dismissFeedback() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }

    /**
     * تنفيذ واعتماد فاتورة الشراء وتطبيق دالة WAC لحساب التكلفة الجديدة بالعملة المحلية وتحويلات الصرف
     */
    fun executePurchaseTransaction() {
        val state = _uiState.value

        if (state.items.isEmpty()) {
            _uiState.update { it.copy(feedbackMessage = "لا يمكن حفظ فاتورة شراء فارغة! الرجاء إضافة منتجات.", isError = true) }
            return
        }

        if (state.selectedSupplier == null && state.paymentMethod == PaymentMethod.CREDIT) {
            _uiState.update { it.copy(feedbackMessage = "الشراء بالآجل يتطلب تحديد المورد لتسجيل المستحقات في حسابه.", isError = true) }
            return
        }

        _uiState.update { it.copy(isProcessing = true) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                db.withTransaction {
                    val timestamp = System.currentTimeMillis()
                    val totalCount = invoiceDao.countInvoices() + 1
                    val invoiceNumber = "PUR-2026-%04d".format(totalCount)

                    val rate = state.exchangeRate
                    val selectedCurrencyId = state.selectedCurrency?.id ?: state.baseCurrencyId

                    val isCredit = state.paymentMethod == PaymentMethod.CREDIT
                    val finalTotalLocal = state.finalTotalBaseCurrency
                    val paidLocal = if (isCredit) 0.0 else finalTotalLocal
                    val remainingLocal = if (isCredit) finalTotalLocal else 0.0

                    // 1. إدخال فاتورة الشراء بالقيم المترجمة للعملة الأساسية
                    val invoiceId = invoiceDao.insertInvoice(
                        InvoiceEntity(
                            invoiceNumber = invoiceNumber,
                            type = InvoiceType.PURCHASE,
                            partyId = state.selectedSupplier?.id,
                            date = timestamp,
                            currencyId = selectedCurrencyId,
                            exchangeRate = rate,
                            subtotal = state.subtotalBaseCurrency,
                            discount = state.discount * rate,
                            taxRate = if (state.isTaxApplied) 0.15 else 0.0,
                            taxAmount = state.taxAmountBaseCurrency,
                            total = finalTotalLocal,
                            paidAmount = paidLocal,
                            remainingAmount = remainingLocal,
                            paymentMethod = state.paymentMethod,
                            status = InvoiceStatus.COMPLETED,
                            notes = "فاتورة شراء مورد رقم: ${state.supplierInvoiceNumber.ifEmpty { "غير محدد" }} - ${state.notes}"
                        )
                    )

                    // 2. بنود الفاتورة وحركات المخزون وحساب WAC بالعملة المحلية
                    val itemsToInsert = mutableListOf<InvoiceItemEntity>()
                    val movementsToInsert = mutableListOf<StockMovementEntity>()
                    val wacSummaries = mutableListOf<WacCalculationSummary>()

                    state.items.forEach { item ->
                        val itemCostPriceLocal = item.costPrice * rate
                        val itemTotalCostLocal = item.totalCost * rate

                        itemsToInsert.add(
                            InvoiceItemEntity(
                                invoiceId = invoiceId,
                                productId = item.productId,
                                productUnitId = item.unitId,
                                quantity = item.quantity,
                                unitConversionFactor = item.conversionFactor,
                                unitCostPrice = itemCostPriceLocal,
                                unitSellingPrice = item.newSellingPrice ?: item.sellingPrice,
                                discount = 0.0,
                                taxRate = if (state.isTaxApplied) 0.15 else 0.0,
                                totalPrice = itemTotalCostLocal
                            )
                        )

                        val baseQtyPurchased = item.quantity * item.conversionFactor
                        val unitCostBasePurchasedLocal = itemCostPriceLocal / item.conversionFactor

                        // إضافة حركة مخزون PURCHASE_IN
                        movementsToInsert.add(
                            StockMovementEntity(
                                productId = item.productId,
                                productUnitId = item.unitId,
                                movementType = MovementType.PURCHASE_IN,
                                quantityBaseUnit = baseQtyPurchased,
                                remainingQuantityForFifo = baseQtyPurchased,
                                unitCostPriceBase = unitCostBasePurchasedLocal,
                                timestamp = timestamp,
                                referenceNumber = invoiceNumber
                            )
                        )

                        // 3. تطبيق دالة المتوسط المرجح WAC:
                        val existingStockBase = stockMovementDao.getTotalStockQuantity(item.productId)
                        val oldUnitCostBaseLocal = item.oldCostPrice / item.conversionFactor

                        val newWacCostBaseLocal = if (existingStockBase > 0.001) {
                            val currentInventoryValue = existingStockBase * oldUnitCostBaseLocal
                            val incomingValue = baseQtyPurchased * unitCostBasePurchasedLocal
                            (currentInventoryValue + incomingValue) / (existingStockBase + baseQtyPurchased)
                        } else {
                            unitCostBasePurchasedLocal
                        }

                        val unitEntity = productDao.getUnitById(item.unitId)
                        if (unitEntity != null) {
                            val updatedCostForThisUnit = newWacCostBaseLocal * unitEntity.conversionFactor
                            val updatedSellingPrice = item.newSellingPrice ?: unitEntity.sellingPrice
                            productDao.updateUnit(
                                unitEntity.copy(
                                    costPrice = updatedCostForThisUnit,
                                    sellingPrice = updatedSellingPrice
                                )
                            )

                            wacSummaries.add(
                                WacCalculationSummary(
                                    productName = item.productName,
                                    unitName = item.unitName,
                                    oldCost = item.oldCostPrice,
                                    purchaseCost = item.costPrice,
                                    newWacCost = updatedCostForThisUnit / rate,
                                    oldStock = existingStockBase,
                                    newStock = existingStockBase + baseQtyPurchased
                                )
                            )
                        }
                    }

                    invoiceDao.insertInvoiceItems(itemsToInsert)
                    stockMovementDao.insertMovements(movementsToInsert)

                    // 4. تحديث رصيد المورد إذا كان الدفع آجلاً
                    if (isCredit && state.selectedSupplier != null) {
                        partyDao.updateBalance(state.selectedSupplier.id, -finalTotalLocal)
                    }

                    // 5. خصم المبلغ من الصندوق إذا كان الشراء نقداً
                    if (state.paymentMethod == PaymentMethod.CASH) {
                        val openShift = shiftDao.getOpenShift()
                        if (openShift != null) {
                            shiftDao.updateShift(
                                openShift.copy(
                                    totalCashExpenses = openShift.totalCashExpenses + finalTotalLocal,
                                    expectedCashInDrawer = openShift.expectedCashInDrawer - finalTotalLocal
                                )
                            )
                        }
                    }

                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            items = emptyList(),
                            supplierInvoiceNumber = "",
                            lastSavedInvoiceNumber = invoiceNumber,
                            wacUpdates = wacSummaries,
                            showSuccessDialog = true,
                            feedbackMessage = "تم حفظ واعتماد فاتورة التوريد والشراء بنجاح برقم: $invoiceNumber",
                            isError = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        feedbackMessage = "خطأ أثناء حفظ فاتورة الشراء: ${e.localizedMessage}",
                        isError = true
                    )
                }
            }
        }
    }

    fun addNewSupplier(name: String, phone: String, taxNumber: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            val id = partyDao.insertParty(
                PartyEntity(
                    name = name,
                    phone = phone,
                    taxNumber = taxNumber,
                    type = PartyType.SUPPLIER,
                    currentBalance = 0.0
                )
            )
            val created = partyDao.getPartyById(id)
            _uiState.update {
                it.copy(
                    selectedSupplier = created,
                    feedbackMessage = "تمت إضافة المورد $name بنجاح",
                    isError = false
                )
            }
        }
    }

    // --- حماية وصلاحيات مدير النظام Admin Security PIN ---
    fun openAdminPinDialog(action: PendingAdminAction) {
        _uiState.update {
            it.copy(
                showAdminPinDialog = true,
                pendingAdminAction = action,
                adminPinError = null
            )
        }
    }

    fun dismissAdminPinDialog() {
        _uiState.update {
            it.copy(
                showAdminPinDialog = false,
                pendingAdminAction = null,
                adminPinError = null
            )
        }
    }

    fun verifyAdminPin(pin: String, onVerified: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val userDao = db.userDao()
            val user = userDao.getUserByPin(pin)
            if (user != null && user.role == UserRole.ADMIN) {
                _uiState.update { it.copy(showAdminPinDialog = false, pendingAdminAction = null, adminPinError = null) }
                launch(Dispatchers.Main) { onVerified() }
            } else {
                _uiState.update {
                    it.copy(adminPinError = "رمز مدير النظام غير صحيح أو لا يملك صلاحية مدير")
                }
            }
        }
    }

    // --- استعراض فواتير الشراء والتعديل والحذف مع إعادة احتساب WAC ورصيد المورد والمخزون ---
    fun deletePurchaseInvoice(invoiceId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                db.withTransaction {
                    val inv = invoiceDao.getInvoiceById(invoiceId) ?: return@withTransaction
                    val items = invoiceDao.getInvoiceItems(invoiceId)

                    // 1. حذف حركات المخزون الخاصة بالفاتورة
                    stockMovementDao.deleteMovementsByReferenceNumber(inv.invoiceNumber)

                    // 2. إعادة احتساب WAC وتكاليف الوحدات لكل الأقسام والصنوف المتأثرة
                    val affectedProductIds = items.map { it.productId }.distinct()
                    for (productId in affectedProductIds) {
                        recalculateProductWacAndStock(productId)
                    }

                    // 3. عكس رصيد المورد إذا كانت العملية آجلة
                    if (inv.partyId != null && inv.paymentMethod == PaymentMethod.CREDIT) {
                        val party = partyDao.getPartyById(inv.partyId)
                        if (party != null) {
                            partyDao.updateParty(party.copy(currentBalance = party.currentBalance + inv.total))
                        }
                    }

                    // 4. عكس نقدية الصندوق للشفت المفتوح إذا كان الدفع نقداً
                    if (inv.paymentMethod == PaymentMethod.CASH) {
                        val openShift = shiftDao.getOpenShift()
                        if (openShift != null) {
                            val newExp = (openShift.totalCashExpenses - inv.total).coerceAtLeast(0.0)
                            shiftDao.updateShift(openShift.copy(totalCashExpenses = newExp))
                        }
                    }

                    // 5. حذف بنود الفاتورة والفاتورة نفسها
                    invoiceDao.deleteInvoiceItemsByInvoiceId(invoiceId)
                    invoiceDao.deleteInvoice(inv)
                }
                _uiState.update {
                    it.copy(
                        feedbackMessage = "تم حذف فاتورة الشراء وعكس حركات المخزون واحتساب WAC بنجاح.",
                        isError = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        feedbackMessage = "فشل حذف فاتورة الشراء: ${e.localizedMessage}",
                        isError = true
                    )
                }
            }
        }
    }

    private suspend fun recalculateProductWacAndStock(productId: Long) {
        val remainingLots = stockMovementDao.getActiveStockLotsForWac(productId)
        val currentStockQty = stockMovementDao.getTotalStockQuantity(productId)

        val newWacCostBase = if (remainingLots.isNotEmpty() && currentStockQty > 0.0001) {
            val totalVal = remainingLots.sumOf { it.remainingQuantityForFifo * it.unitCostPriceBase }
            totalVal / remainingLots.sumOf { it.remainingQuantityForFifo }
        } else {
            val lastMvt = stockMovementDao.getLastPurchaseMovement(productId)
            lastMvt?.unitCostPriceBase ?: 0.0
        }

        val units = productDao.getUnitsForProductSync(productId)
        for (unit in units) {
            productDao.updateUnit(
                unit.copy(
                    costPrice = newWacCostBase * unit.conversionFactor
                )
            )
        }
    }

    fun openInvoiceDetails(invoice: InvoiceEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val items = invoiceDao.getInvoiceItems(invoice.id)
            _uiState.update {
                it.copy(
                    selectedInvoice = invoice,
                    selectedInvoiceWithDetails = items,
                    showInvoiceDetailsDialog = true
                )
            }
        }
    }

    fun dismissInvoiceDetails() {
        _uiState.update {
            it.copy(
                selectedInvoice = null,
                selectedInvoiceWithDetails = emptyList(),
                showInvoiceDetailsDialog = false
            )
        }
    }

    fun openEditPurchaseDialog(invoice: InvoiceEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val items = invoiceDao.getInvoiceItems(invoice.id)
            val currencies = db.currencyDao().getAllCurrenciesSync()
            val curr = currencies.find { it.id == invoice.currencyId } ?: currencies.firstOrNull()
            val party = invoice.partyId?.let { partyDao.getPartyById(it) }
            val rate = if (invoice.exchangeRate > 0) invoice.exchangeRate else (curr?.exchangeRateToBase ?: 1.0)

            val editableItems = items.mapNotNull { item ->
                val prod = productDao.getProductById(item.productId) ?: return@mapNotNull null
                val unit = productDao.getUnitById(item.productUnitId) ?: return@mapNotNull null
                val costInInvoiceCurr = if (rate > 0) item.unitCostPrice / rate else item.unitCostPrice
                PurchaseLineItem(
                    productId = item.productId,
                    productName = prod.name,
                    productCode = prod.code,
                    unitId = unit.id,
                    unitName = unit.unitName,
                    conversionFactor = item.unitConversionFactor,
                    quantity = item.quantity,
                    costPrice = costInInvoiceCurr,
                    oldCostPrice = unit.costPrice,
                    sellingPrice = item.unitSellingPrice
                )
            }

            _uiState.update {
                it.copy(
                    selectedInvoice = invoice,
                    editPurchaseNotes = invoice.notes,
                    editPurchasePaymentMethod = invoice.paymentMethod,
                    editPurchaseSupplier = party,
                    editPurchaseCurrency = curr,
                    editPurchaseExchangeRate = rate,
                    editPurchaseItems = editableItems,
                    showEditPurchaseDialog = true
                )
            }
        }
    }

    fun updateEditPurchaseItemQty(productId: Long, unitId: Long, newQty: Double) {
        _uiState.update { state ->
            val updated = if (newQty <= 0.001) {
                state.editPurchaseItems.filterNot { it.productId == productId && it.unitId == unitId }
            } else {
                state.editPurchaseItems.map {
                    if (it.productId == productId && it.unitId == unitId) it.copy(quantity = newQty) else it
                }
            }
            state.copy(editPurchaseItems = updated)
        }
    }

    fun updateEditPurchaseItemCost(productId: Long, unitId: Long, newCost: Double) {
        _uiState.update { state ->
            val updated = state.editPurchaseItems.map {
                if (it.productId == productId && it.unitId == unitId) it.copy(costPrice = newCost) else it
            }
            state.copy(editPurchaseItems = updated)
        }
    }

    fun dismissEditPurchaseDialog() {
        _uiState.update {
            it.copy(
                selectedInvoice = null,
                showEditPurchaseDialog = false
            )
        }
    }

    fun saveEditedPurchaseInvoice(
        notes: String,
        method: PaymentMethod,
        supplier: PartyEntity?,
        currency: CurrencyEntity?,
        rate: Double
    ) {
        val inv = _uiState.value.selectedInvoice ?: return
        val editedItems = _uiState.value.editPurchaseItems

        viewModelScope.launch(Dispatchers.IO) {
            try {
                db.withTransaction {
                    val timestamp = inv.date
                    val oldInvoiceItems = invoiceDao.getInvoiceItems(inv.id)

                    // 1. إلغاء حركات المخزون للنسخة القديمة من الفاتورة
                    stockMovementDao.deleteMovementsByReferenceNumber(inv.invoiceNumber)

                    // 2. إعادة احتساب التكلفة للنسخة القديمة لتصحيح WAC
                    val oldProductIds = oldInvoiceItems.map { it.productId }.distinct()
                    for (pId in oldProductIds) {
                        recalculateProductWacAndStock(pId)
                    }

                    // 3. عكس رصيد المورد القديم
                    if (inv.partyId != null && inv.paymentMethod == PaymentMethod.CREDIT) {
                        val party = partyDao.getPartyById(inv.partyId)
                        if (party != null) {
                            partyDao.updateParty(party.copy(currentBalance = party.currentBalance + inv.total))
                        }
                    }

                    // 4. عكس منصرفات الصندوق القديمة
                    if (inv.paymentMethod == PaymentMethod.CASH) {
                        val openShift = shiftDao.getOpenShift()
                        if (openShift != null) {
                            val newExp = (openShift.totalCashExpenses - inv.total).coerceAtLeast(0.0)
                            shiftDao.updateShift(openShift.copy(totalCashExpenses = newExp))
                        }
                    }

                    // 5. بناء التعديلات الجديدة وتطبيق سعر الصرف
                    val activeRate = if (rate > 0) rate else 1.0
                    val subtotalCurr = editedItems.sumOf { it.totalCost }
                    val taxCurr = subtotalCurr * 0.15
                    val totalCurr = subtotalCurr + taxCurr

                    val totalLocal = totalCurr * activeRate
                    val taxLocal = taxCurr * activeRate
                    val subtotalLocal = subtotalCurr * activeRate

                    val isCredit = method == PaymentMethod.CREDIT
                    val paidLocal = if (isCredit) 0.0 else totalLocal
                    val remainingLocal = if (isCredit) totalLocal else 0.0

                    val updatedInv = inv.copy(
                        partyId = supplier?.id,
                        currencyId = currency?.id ?: inv.currencyId,
                        exchangeRate = activeRate,
                        subtotal = subtotalLocal,
                        taxAmount = taxLocal,
                        total = totalLocal,
                        paidAmount = paidLocal,
                        remainingAmount = remainingLocal,
                        paymentMethod = method,
                        notes = notes
                    )

                    invoiceDao.updateInvoice(updatedInv)
                    invoiceDao.deleteInvoiceItemsByInvoiceId(inv.id)

                    val newItemsToInsert = mutableListOf<InvoiceItemEntity>()
                    val newMovementsToInsert = mutableListOf<StockMovementEntity>()

                    editedItems.forEach { item ->
                        val itemCostPriceLocal = item.costPrice * activeRate
                        val itemTotalCostLocal = item.totalCost * activeRate

                        newItemsToInsert.add(
                            InvoiceItemEntity(
                                invoiceId = inv.id,
                                productId = item.productId,
                                productUnitId = item.unitId,
                                quantity = item.quantity,
                                unitConversionFactor = item.conversionFactor,
                                unitCostPrice = itemCostPriceLocal,
                                unitSellingPrice = item.sellingPrice,
                                discount = 0.0,
                                taxRate = 0.15,
                                totalPrice = itemTotalCostLocal
                            )
                        )

                        val baseQtyPurchased = item.quantity * item.conversionFactor
                        val unitCostBasePurchasedLocal = itemCostPriceLocal / item.conversionFactor

                        newMovementsToInsert.add(
                            StockMovementEntity(
                                productId = item.productId,
                                productUnitId = item.unitId,
                                movementType = MovementType.PURCHASE_IN,
                                quantityBaseUnit = baseQtyPurchased,
                                remainingQuantityForFifo = baseQtyPurchased,
                                unitCostPriceBase = unitCostBasePurchasedLocal,
                                timestamp = timestamp,
                                referenceNumber = inv.invoiceNumber
                            )
                        )
                    }

                    invoiceDao.insertInvoiceItems(newItemsToInsert)
                    stockMovementDao.insertMovements(newMovementsToInsert)

                    // 6. إعادة احتساب WAC للصنوف المعدلة
                    val newProductIds = editedItems.map { it.productId }.distinct()
                    val allAffectedIds = (oldProductIds + newProductIds).distinct()
                    for (pId in allAffectedIds) {
                        recalculateProductWacAndStock(pId)
                    }

                    // 7. تطبيق رصيد المورد الجديد
                    if (isCredit && supplier != null) {
                        partyDao.updateBalance(supplier.id, -totalLocal)
                    }

                    // 8. تطبيق منصرفات الصندوق الجديدة
                    if (method == PaymentMethod.CASH) {
                        val openShift = shiftDao.getOpenShift()
                        if (openShift != null) {
                            shiftDao.updateShift(
                                openShift.copy(
                                    totalCashExpenses = openShift.totalCashExpenses + totalLocal,
                                    expectedCashInDrawer = openShift.expectedCashInDrawer - totalLocal
                                )
                            )
                        }
                    }
                }

                _uiState.update {
                    it.copy(
                        showEditPurchaseDialog = false,
                        selectedInvoice = null,
                        feedbackMessage = "تم حفظ تعديلات فاتورة الشراء واحتساب WAC بنجاح.",
                        isError = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        feedbackMessage = "فشل تعديل الفاتورة: ${e.localizedMessage}",
                        isError = true
                    )
                }
            }
        }
    }

    fun setInvoiceSearchQuery(query: String) {
        _uiState.update { it.copy(invoiceSearchQuery = query) }
    }

    fun toggleBottomHistoryExpanded() {
        _uiState.update { it.copy(isBottomHistoryExpanded = !it.isBottomHistoryExpanded) }
    }
}

