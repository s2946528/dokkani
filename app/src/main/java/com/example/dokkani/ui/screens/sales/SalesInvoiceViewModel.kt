package com.example.dokkani.ui.screens.sales

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.example.dokkani.data.local.DokkaniDatabase
import com.example.dokkani.data.local.entities.CostCenterEntity
import com.example.dokkani.data.local.entities.CurrencyEntity
import com.example.dokkani.data.local.entities.FinancialAccountEntity
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

sealed class SalesPendingAdminAction {
    data class DeleteInvoice(val invoice: InvoiceEntity) : SalesPendingAdminAction()
    data class EditInvoice(val invoice: InvoiceEntity) : SalesPendingAdminAction()
}

data class SalesLineItem(
    val productId: Long,
    val productName: String,
    val productCode: String,
    val unitId: Long,
    val unitName: String,
    val conversionFactor: Double,
    val quantity: Double,
    val sellingPrice: Double,
    val costPrice: Double
) {
    val totalPrice: Double get() = quantity * sellingPrice
}

data class SalesInvoiceUiState(
    val customers: List<PartyEntity> = emptyList(),
    val selectedCustomer: PartyEntity? = null,
    val costCenters: List<CostCenterEntity> = emptyList(),
    val selectedCostCenterId: Long = 1L,
    val customerInvoiceNumber: String = "",
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val financialAccounts: List<FinancialAccountEntity> = emptyList(),
    val selectedPaymentAccountId: Long? = null,
    val productsWithUnits: List<ProductWithUnits> = emptyList(),
    val searchQuery: String = "",
    val categories: List<String> = listOf("الكل"),
    val selectedCategory: String = "الكل",
    val sortOption: com.example.dokkani.ui.models.ProductSortOption = com.example.dokkani.ui.models.ProductSortOption.POPULAR,
    val productStockMap: Map<Long, Double> = emptyMap(),
    val items: List<SalesLineItem> = emptyList(),
    val isTaxApplied: Boolean = false,
    val salesTaxRate: Double = 0.0,
    val discount: Double = 0.0,
    val notes: String = "",
    val isProcessing: Boolean = false,
    val lastSavedInvoiceNumber: String? = null,
    val showSuccessDialog: Boolean = false,
    val showAddCustomerDialog: Boolean = false,
    val feedbackMessage: String? = null,
    val isError: Boolean = false,

    // العملة وسعر الصرف
    val availableCurrencies: List<CurrencyEntity> = emptyList(),
    val selectedCurrency: CurrencyEntity? = null,
    val exchangeRate: Double = 1.0,
    val currencySymbol: String = "ر.ي",
    val currencyName: String = "الريال اليمني",
    val baseCurrencyId: Long = 1L,
    val baseCurrencySymbol: String = "ر.ي",

    // استعراض فواتير البيع
    val salesInvoices: List<InvoiceEntity> = emptyList(),
    val invoiceSearchQuery: String = "",
    val showInvoiceDetailsDialog: Boolean = false,
    val selectedInvoiceWithDetails: List<InvoiceItemEntity> = emptyList(),
    val selectedInvoice: InvoiceEntity? = null,
    val showEditSalesDialog: Boolean = false,
    val isBottomHistoryExpanded: Boolean = true,

    // حماية وصلاحيات مدير النظام
    val showAdminPinDialog: Boolean = false,
    val pendingAdminAction: SalesPendingAdminAction? = null,
    val adminPinError: String? = null
) {
    val subtotal: Double get() = items.sumOf { it.totalPrice }
    val taxableAmount: Double get() = (subtotal - discount).coerceAtLeast(0.0)
    val taxAmount: Double get() = if (isTaxApplied) taxableAmount * salesTaxRate else 0.0
    val finalTotal: Double get() = taxableAmount + taxAmount
    val totalQuantity: Double get() = items.sumOf { it.quantity }
}

class SalesInvoiceViewModel(application: Application) : AndroidViewModel(application) {
    private val db = DokkaniDatabase.getDatabase(application, viewModelScope)
    private val partyDao = db.partyDao()
    private val productDao = db.productDao()
    private val invoiceDao = db.invoiceDao()
    private val stockMovementDao = db.stockMovementDao()
    private val costCenterDao = db.costCenterDao()
    private val financialAccountDao = db.financialAccountDao()
    private val currencyDao = db.currencyDao()
    private val cashShiftDao = db.cashShiftDao()

    private val _uiState = MutableStateFlow(SalesInvoiceUiState())
    val uiState: StateFlow<SalesInvoiceUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch(Dispatchers.IO) {
            partyDao.getPartiesByType(PartyType.CUSTOMER).collectLatest { customerList ->
                _uiState.update { it.copy(customers = customerList) }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            costCenterDao.getAllCostCenters().collectLatest { centers ->
                _uiState.update { it.copy(costCenters = centers) }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            financialAccountDao.getAllAccounts().collectLatest { accounts ->
                _uiState.update { state ->
                    val defaultAccount = accounts.firstOrNull { it.isDefault } ?: accounts.firstOrNull()
                    state.copy(
                        financialAccounts = accounts,
                        selectedPaymentAccountId = state.selectedPaymentAccountId ?: defaultAccount?.id
                    )
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            currencyDao.getAllCurrencies().collectLatest { currencies ->
                val base = currencies.firstOrNull { it.isBaseCurrency } ?: currencies.firstOrNull()
                val baseSym = base?.symbol ?: "ر.ي"
                val baseId = base?.id ?: 1L

                _uiState.update { state ->
                    val selected = state.selectedCurrency ?: base
                    state.copy(
                        availableCurrencies = currencies,
                        selectedCurrency = selected,
                        exchangeRate = selected?.exchangeRateToBase ?: 1.0,
                        currencySymbol = selected?.symbol ?: baseSym,
                        currencyName = selected?.name ?: "الريال اليمني",
                        baseCurrencyId = baseId,
                        baseCurrencySymbol = baseSym
                    )
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            productDao.getProductsWithUnits().collectLatest { products ->
                val categorySet = mutableSetOf("الكل")
                products.forEach { p ->
                    if (p.product.category.isNotBlank()) {
                        categorySet.add(p.product.category.trim())
                    }
                }

                _uiState.update { state ->
                    state.copy(
                        productsWithUnits = products,
                        categories = categorySet.toList()
                    )
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            stockMovementDao.getAllMovements().collectLatest { movements ->
                val stockMap = mutableMapOf<Long, Double>()
                movements.forEach { m ->
                    val current = stockMap.getOrDefault(m.productId, 0.0)
                    stockMap[m.productId] = current + m.quantityBaseUnit
                }
                _uiState.update { it.copy(productStockMap = stockMap) }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            invoiceDao.getInvoicesByType(InvoiceType.SALE).collectLatest { salesInvoices ->
                _uiState.update { it.copy(salesInvoices = salesInvoices) }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            db.systemSettingsDao().getSettings().collectLatest { settings ->
                if (settings != null) {
                    _uiState.update { state ->
                        state.copy(
                            isTaxApplied = settings.isTaxEnabled,
                            salesTaxRate = settings.defaultTaxRate
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

    fun selectCustomer(customer: PartyEntity?) {
        if (customer == null) {
            _uiState.update { it.copy(selectedCustomer = null) }
        } else {
            viewModelScope.launch(Dispatchers.IO) {
                val fresh = partyDao.getPartyById(customer.id) ?: customer
                _uiState.update { it.copy(selectedCustomer = fresh) }
            }
        }
    }

    fun selectCostCenter(costCenterId: Long) {
        _uiState.update { it.copy(selectedCostCenterId = costCenterId) }
    }

    fun setCustomerInvoiceNumber(number: String) {
        _uiState.update { it.copy(customerInvoiceNumber = number) }
    }

    fun setPaymentMethod(method: PaymentMethod) {
        _uiState.update { it.copy(paymentMethod = method) }
    }

    fun setSelectedPaymentAccount(accountId: Long?) {
        _uiState.update { it.copy(selectedPaymentAccountId = accountId) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setSelectedCategory(category: String) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun setSortOption(option: com.example.dokkani.ui.models.ProductSortOption) {
        _uiState.update { it.copy(sortOption = option) }
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

    fun addProductItem(product: ProductEntity, unit: ProductUnitEntity, quantity: Double = 1.0, price: Double = unit.sellingPrice) {
        _uiState.update { state ->
            val existingIndex = state.items.indexOfFirst { it.productId == product.id && it.unitId == unit.id }
            val updated = state.items.toMutableList()

            if (existingIndex >= 0) {
                val existing = updated[existingIndex]
                updated[existingIndex] = existing.copy(
                    quantity = existing.quantity + quantity,
                    sellingPrice = price
                )
            } else {
                updated.add(
                    SalesLineItem(
                        productId = product.id,
                        productName = product.name,
                        productCode = product.code,
                        unitId = unit.id,
                        unitName = unit.unitName,
                        conversionFactor = unit.conversionFactor,
                        quantity = quantity,
                        sellingPrice = price,
                        costPrice = unit.costPrice
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

    fun updateItemSellingPrice(productId: Long, unitId: Long, newPrice: Double) {
        _uiState.update { state ->
            val updated = state.items.map {
                if (it.productId == productId && it.unitId == unitId) it.copy(sellingPrice = newPrice) else it
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
                customerInvoiceNumber = "",
                discount = 0.0,
                notes = "",
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

    fun executeSalesTransaction() {
        val state = _uiState.value

        if (state.items.isEmpty()) {
            _uiState.update { it.copy(feedbackMessage = "لا يمكن حفظ فاتورة بيع فارغة! الرجاء إضافة منتجات.", isError = true) }
            return
        }

        if (state.selectedCustomer == null && state.paymentMethod == PaymentMethod.CREDIT) {
            _uiState.update { it.copy(feedbackMessage = "البيع بالآجل يتطلب تحديد العميل لتسجيل المستحقات في حسابه.", isError = true) }
            return
        }

        _uiState.update { it.copy(isProcessing = true) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())
                val invoiceNumber = "INV_SALE_$timeStamp"

                db.withTransaction {
                    val invoiceEntity = InvoiceEntity(
                        invoiceNumber = invoiceNumber,
                        type = InvoiceType.SALE,
                        partyId = state.selectedCustomer?.id,
                        date = System.currentTimeMillis(),
                        currencyId = state.selectedCurrency?.id ?: state.baseCurrencyId,
                        exchangeRate = state.exchangeRate,
                        subtotal = state.subtotal,
                        discount = state.discount,
                        taxRate = if (state.isTaxApplied) state.salesTaxRate else 0.0,
                        taxAmount = state.taxAmount,
                        total = state.finalTotal,
                        paidAmount = if (state.paymentMethod == PaymentMethod.CREDIT) 0.0 else state.finalTotal,
                        remainingAmount = if (state.paymentMethod == PaymentMethod.CREDIT) state.finalTotal else 0.0,
                        paymentMethod = state.paymentMethod,
                        paymentAccountId = state.selectedPaymentAccountId,
                        status = InvoiceStatus.COMPLETED,
                        notes = if (state.customerInvoiceNumber.isNotBlank()) "مرجع العميل: ${state.customerInvoiceNumber} | ${state.notes}" else state.notes,
                        costCenterId = state.selectedCostCenterId
                    )

                    val savedInvoiceId = invoiceDao.insertInvoice(invoiceEntity)

                    val itemEntities = state.items.map { line ->
                        InvoiceItemEntity(
                            invoiceId = savedInvoiceId,
                            productId = line.productId,
                            productUnitId = line.unitId,
                            quantity = line.quantity,
                            unitConversionFactor = line.conversionFactor,
                            unitCostPrice = line.costPrice,
                            unitSellingPrice = line.sellingPrice,
                            discount = 0.0,
                            taxRate = 0.0,
                            totalPrice = line.totalPrice
                        )
                    }
                    invoiceDao.insertInvoiceItems(itemEntities)

                    state.items.forEach { line ->
                        val stockMove = StockMovementEntity(
                            productId = line.productId,
                            productUnitId = line.unitId,
                            invoiceId = savedInvoiceId,
                            movementType = MovementType.SALE_OUT,
                            quantityBaseUnit = -(line.quantity * line.conversionFactor),
                            remainingQuantityForFifo = 0.0,
                            unitCostPriceBase = line.costPrice,
                            timestamp = System.currentTimeMillis(),
                            referenceNumber = invoiceNumber,
                            notes = "مبيعات بموجب فاتورة $invoiceNumber",
                            costCenterId = state.selectedCostCenterId
                        )
                        stockMovementDao.insertMovement(stockMove)
                    }

                    // تحديث حساب العميل إذا كان بيع بالآجل
                    if (state.paymentMethod == PaymentMethod.CREDIT && state.selectedCustomer != null) {
                        val party = partyDao.getPartyById(state.selectedCustomer.id)
                        if (party != null) {
                            val updatedBalance = party.currentBalance + state.finalTotal
                            partyDao.updateParty(party.copy(currentBalance = updatedBalance))
                        }
                    }

                    // تحديث الحساب المالي النقدي/البنكي
                    if (state.paymentMethod != PaymentMethod.CREDIT && state.selectedPaymentAccountId != null) {
                        val acc = financialAccountDao.getAccountById(state.selectedPaymentAccountId)
                        if (acc != null) {
                            financialAccountDao.updateAccount(acc.copy(currentBalance = acc.currentBalance + state.finalTotal))
                        }
                    }

                    // تسجيل في الشفت المفتوح إن وجد
                    val openShift = cashShiftDao.getOpenShift()
                    if (openShift != null) {
                        when (state.paymentMethod) {
                            PaymentMethod.CASH -> cashShiftDao.updateSales(openShift.id, openShift.totalCashSales + state.finalTotal)
                            PaymentMethod.POS_CARD -> cashShiftDao.addMadaSales(openShift.id, state.finalTotal)
                            PaymentMethod.E_WALLET -> cashShiftDao.addWalletSales(openShift.id, state.finalTotal)
                            PaymentMethod.BANK_TRANSFER -> cashShiftDao.addTransferSales(openShift.id, state.finalTotal)
                            PaymentMethod.CREDIT -> cashShiftDao.addCreditSales(openShift.id, state.finalTotal)
                            else -> cashShiftDao.updateSales(openShift.id, openShift.totalCashSales + state.finalTotal)
                        }
                    }
                }

                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        items = emptyList(),
                        customerInvoiceNumber = "",
                        discount = 0.0,
                        notes = "",
                        lastSavedInvoiceNumber = invoiceNumber,
                        showSuccessDialog = true,
                        feedbackMessage = "تم اعتماد وحفظ فاتورة البيع برقم: $invoiceNumber بنجاح!",
                        isError = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        feedbackMessage = "حدث خطأ أثناء حفظ الفاتورة: ${e.localizedMessage}",
                        isError = true
                    )
                }
            }
        }
    }

    fun addQuickCustomer(name: String, phone: String, taxNumber: String) {
        if (name.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val customer = PartyEntity(
                name = name.trim(),
                type = PartyType.CUSTOMER,
                phone = phone.trim(),
                taxNumber = taxNumber.trim(),
                currentBalance = 0.0,
                creditLimit = 10000.0,
                notes = "تمت الإضافة السريعة من شاشة فواتير البيع"
            )
            val newId = partyDao.insertParty(customer)
            val inserted = customer.copy(id = newId)
            _uiState.update { it.copy(selectedCustomer = inserted) }
        }
    }

    fun setInvoiceSearchQuery(query: String) {
        _uiState.update { it.copy(invoiceSearchQuery = query) }
    }

    fun toggleBottomHistoryExpanded() {
        _uiState.update { it.copy(isBottomHistoryExpanded = !it.isBottomHistoryExpanded) }
    }

    fun viewInvoiceDetails(invoice: InvoiceEntity) {
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

    fun dismissInvoiceDetailsDialog() {
        _uiState.update { it.copy(showInvoiceDetailsDialog = false, selectedInvoice = null) }
    }

    fun requestDeleteInvoice(invoice: InvoiceEntity, isAdmin: Boolean) {
        if (!isAdmin) {
            _uiState.update {
                it.copy(
                    showAdminPinDialog = true,
                    pendingAdminAction = SalesPendingAdminAction.DeleteInvoice(invoice),
                    adminPinError = null
                )
            }
        } else {
            deleteInvoiceDirectly(invoice)
        }
    }

    fun confirmAdminPin(pin: String) {
        if (pin == "1234" || pin == "0000") {
            val action = _uiState.value.pendingAdminAction
            _uiState.update { it.copy(showAdminPinDialog = false, pendingAdminAction = null, adminPinError = null) }

            when (action) {
                is SalesPendingAdminAction.DeleteInvoice -> deleteInvoiceDirectly(action.invoice)
                else -> {}
            }
        } else {
            _uiState.update { it.copy(adminPinError = "رمز PIN غير صحيح!") }
        }
    }

    fun dismissAdminPinDialog() {
        _uiState.update { it.copy(showAdminPinDialog = false, pendingAdminAction = null, adminPinError = null) }
    }

    private fun deleteInvoiceDirectly(invoice: InvoiceEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                db.withTransaction {
                    invoiceDao.deleteInvoice(invoice)
                }
                _uiState.update {
                    it.copy(
                        feedbackMessage = "تم حذف الفاتورة رقم ${invoice.invoiceNumber} بنجاح",
                        isError = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        feedbackMessage = "فشل حذف الفاتورة: ${e.localizedMessage}",
                        isError = true
                    )
                }
            }
        }
    }
}
