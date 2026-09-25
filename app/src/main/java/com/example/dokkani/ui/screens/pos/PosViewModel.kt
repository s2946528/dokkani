package com.example.dokkani.ui.screens.pos

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.example.dokkani.data.local.DokkaniDatabase
import com.example.dokkani.data.local.dao.CashShiftDao
import com.example.dokkani.data.local.entities.CashShiftEntity
import com.example.dokkani.data.local.entities.ExpenseEntity
import com.example.dokkani.data.local.entities.InvoiceEntity
import com.example.dokkani.data.local.entities.InvoiceItemEntity
import com.example.dokkani.data.local.entities.InvoiceStatus
import com.example.dokkani.data.local.entities.InvoiceType
import com.example.dokkani.data.local.entities.MovementType
import com.example.dokkani.data.local.entities.PartyEntity
import com.example.dokkani.data.local.entities.PartyType
import com.example.dokkani.data.local.entities.UserRole
import com.example.dokkani.data.local.entities.PaymentMethod
import com.example.dokkani.data.local.entities.PaymentVoucherEntity
import com.example.dokkani.data.local.entities.VoucherType
import com.example.dokkani.data.local.entities.ProductEntity
import com.example.dokkani.data.local.entities.ProductUnitEntity
import com.example.dokkani.data.local.entities.ProductWithUnits
import com.example.dokkani.data.local.entities.StockMovementEntity
import com.example.dokkani.data.local.entities.SystemSettingsEntity
import com.example.dokkani.domain.cash.CashDiscrepancyType
import com.example.dokkani.domain.cash.CashDrawerEngine
import com.example.dokkani.domain.cash.CashReconciliationResult
import com.example.dokkani.domain.hardware.ReceiptItemData
import com.example.dokkani.domain.hardware.ReceiptPrintData
import com.example.dokkani.domain.pos.CartSummary
import com.example.dokkani.domain.pos.PosCartItem
import com.example.dokkani.domain.pos.PosCheckoutResult
import com.example.dokkani.domain.pos.PosOperation
import com.example.dokkani.domain.pos.PosTransactionRecord
import com.example.dokkani.domain.pos.TransactionItemDetail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

import com.example.dokkani.data.local.entities.StockGroupEntity

/**
 * علامات التبويب المخصصة لواجهة نقطة البيع في الهواتف والشاشات الصغيرة
 */
enum class PosMobileTab {
    CATALOG, // قائمة الأصناف والتصنيفات
    CART     // سلة الفاتورة الحالية
}

/**
 * حالة واجهة شاشة العمليات المالية والمبيعات (POS UI State)
 */
data class PosUiState(
    val activeOperation: PosOperation = PosOperation.SALE,
    val activeMobileTab: PosMobileTab = PosMobileTab.CATALOG,
    val selectedTab: PosMobileTab = PosMobileTab.CATALOG,
    val productsWithUnits: List<ProductWithUnits> = emptyList(),
    val parties: List<PartyEntity> = emptyList(),
    val selectedParty: PartyEntity? = null, // null للزبون النقدي المباشر في فواتير البيع
    val cartItems: List<PosCartItem> = emptyList(),
    val cartSummary: CartSummary = CartSummary(0, 0.0, 0.0, 0.0, 0.0, 15.0, 0.0, 0.0),
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val selectedPaymentAccountId: Long? = null,
    val paymentTransactionRef: String = "",
    val paymentReceiptImagePath: String? = null,
    val financialAccounts: List<com.example.dokkani.data.local.entities.FinancialAccountEntity> = emptyList(),
    val discount: Double = 0.0,
    val paidAmountInput: String = "",
    val searchQuery: String = "",
    val categories: List<String> = listOf("الكل"),
    val selectedCategory: String = "الكل",
    val sortOption: com.example.dokkani.ui.models.ProductSortOption = com.example.dokkani.ui.models.ProductSortOption.POPULAR,

    // مجموعات البيع بالقيمة (Value Selling)
    val stockGroups: List<StockGroupEntity> = emptyList(),
    val showValueSellingDialog: Boolean = false,
    val selectedStockGroupForValueSale: StockGroupEntity? = null,
    val valueSaleAmountInput: String = "500",

    // فحص المخزون والضبط
    val systemSettings: SystemSettingsEntity? = null,
    val productStockMap: Map<Long, Double> = emptyMap(),
    val currencySymbol: String = "ر.ي",
    val currencyName: String = "الريال اليمني",

    // الصندوق والشفت الحالي
    val shiftTotalSales: Double = 0.0,
    val cashInDrawer: Double = 0.0,
    val currentShift: CashShiftEntity? = null,
    val isRefreshing: Boolean = false,

    // نموذج السندات المالية (قبض / صرف)
    val voucherParty: PartyEntity? = null,
    val voucherAmountInput: String = "",
    val voucherCategory: String = "نثريات ومستلزمات",
    val voucherPaymentMethod: PaymentMethod = PaymentMethod.CASH,
    val voucherPaidToInput: String = "",
    val voucherNotesInput: String = "",
    val isVoucherSubmitting: Boolean = false,

    // ربط المردودات بالفاتورة الأصلية
    val originalInvoiceForReturn: InvoiceEntity? = null,
    val returnOriginalInvoiceItems: List<InvoiceItemEntity> = emptyList(),
    val showSelectInvoiceForReturnDialog: Boolean = false,
    val invoiceSearchQueryForReturn: String = "",
    val matchingInvoicesForReturn: List<InvoiceEntity> = emptyList(),
    val showReturnQuantityWarningDialog: Boolean = false,
    val returnQuantityWarningMessage: String = "",

    // تعديل وحذف العمليات لمدير النظام
    val showEditInvoiceDialog: Boolean = false,
    val editingInvoice: InvoiceEntity? = null,
    val editInvoiceNotes: String = "",
    val editInvoicePaymentMethod: PaymentMethod = PaymentMethod.CASH,
    val editInvoiceTotal: String = "",
    val editInvoicePaidAmount: String = "",
    val editInvoicePartyId: Long? = null,

    val showEditVoucherDialog: Boolean = false,
    val editingVoucherRecord: PosTransactionRecord? = null,
    val editVoucherAmount: String = "",
    val editVoucherNotes: String = "",
    val editVoucherPaymentMethod: PaymentMethod = PaymentMethod.CASH,
    val editVoucherPaidTo: String = "",

    val showDeleteConfirmationDialog: Boolean = false,
    val recordPendingDelete: PosTransactionRecord? = null,

    // النوافذ والملاحظات
    val isProcessingCheckout: Boolean = false,
    val lastCheckoutResult: PosCheckoutResult? = null,
    val showReceiptDialog: Boolean = false,
    val showHistoryDialog: Boolean = false,
    val showCheckoutDialog: Boolean = false,
    val showAddPartyDialog: Boolean = false,
    val showShiftCloseDialog: Boolean = false,
    val shiftActualCashInput: String = "",
    val shiftCloseNotes: String = "",
    val shiftReconciliation: CashReconciliationResult? = null,
    val userFeedbackMessage: String? = null,
    val isError: Boolean = false,

    // نافذة تفاصيل العملية المنبثقة
    val showTransactionDetailDialog: Boolean = false,
    val selectedTransactionDetailRecord: PosTransactionRecord? = null,
    val selectedInvoiceDetails: InvoiceEntity? = null,
    val selectedInvoiceItemDetails: List<TransactionItemDetail> = emptyList(),
    val selectedVoucherDetails: PaymentVoucherEntity? = null,
    val selectedExpenseDetails: ExpenseEntity? = null,
    val selectedDetailPartyName: String = "",
    val selectedDetailPartyPhone: String = "",

    // نافذة التحذير من تجاوز الحد الائتماني للعميل
    val showCreditLimitWarningDialog: Boolean = false,
    val creditLimitWarningParty: PartyEntity? = null,
    val creditLimitWarningInvoiceTotal: Double = 0.0,
    val creditLimitWarningCurrentBalance: Double = 0.0,
    val creditLimitWarningLimit: Double = 0.0,

    // استعراض وتصفية السجل أسفل الشاشة
    val transactionRecords: List<PosTransactionRecord> = emptyList(),
    val historySearchQuery: String = "",
    val historyFilter: String = "ALL",
    val isBottomHistoryExpanded: Boolean = true
) {
    val showDecimals: Boolean get() = systemSettings?.showDecimals ?: false
}

class PosViewModel(application: Application) : AndroidViewModel(application) {

    private val db = DokkaniDatabase.getDatabase(application, viewModelScope)
    private val productDao = db.productDao()
    private val partyDao = db.partyDao()
    private val invoiceDao = db.invoiceDao()
    private val stockMovementDao = db.stockMovementDao()
    private val voucherDao = db.paymentVoucherDao()
    private val expenseDao = db.expenseDao()
    private val shiftDao = db.cashShiftDao()
    private val stockGroupDao = db.stockGroupDao()

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
    private val settingsDao = db.systemSettingsDao()
    private val currencyDao = db.currencyDao()

    private val _uiState = MutableStateFlow(PosUiState())
    val uiState: StateFlow<PosUiState> = _uiState.asStateFlow()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    init {
        observeData()
    }

    private fun observeData() {
        // 0. مراقبة العملة الأساسية للمتجر وتحديث الرمز تفاعلياً
        viewModelScope.launch(Dispatchers.IO) {
            currencyDao.getBaseCurrencyFlow().collectLatest { baseCurrency ->
                if (baseCurrency != null) {
                    _uiState.update {
                        it.copy(
                            currencySymbol = baseCurrency.symbol,
                            currencyName = baseCurrency.name
                        )
                    }
                }
            }
        }

        // 0.1 مراقبة الحسابات المالية (البنوك والمحافظ)
        viewModelScope.launch(Dispatchers.IO) {
            db.financialAccountDao().getActiveAccounts().collectLatest { accounts ->
                _uiState.update { state ->
                    val defaultAccId = state.selectedPaymentAccountId ?: accounts.firstOrNull { it.isDefault }?.id ?: accounts.firstOrNull()?.id
                    state.copy(
                        financialAccounts = accounts,
                        selectedPaymentAccountId = defaultAccId
                    )
                }
            }
        }

        // 1. مراقبة المنتجات والوحدات واستخراج التصنيفات المحفوظة ديناميكياً
        viewModelScope.launch(Dispatchers.IO) {
            productDao.getProductsWithUnits().collectLatest { products ->
                val defaultCategories = listOf(
                    "خضار وفواكه",
                    "ألبان وأجبان",
                    "مخبوزات",
                    "معلبات ومواد غذائية",
                    "مشروبات ومياه",
                    "حلويات وتسالي",
                    "منظفات ومستلزمات منزلية",
                    "عناية شخصية",
                    "تموينات عامة",
                    "عام"
                )
                val extractedCategories = products
                    .map { it.product.category.trim() }
                    .filter { it.isNotBlank() }

                val combinedCategories = (defaultCategories + extractedCategories)
                    .distinct()
                    .sorted()

                val allCategories = listOf("الكل") + combinedCategories

                _uiState.update { state ->
                    val validSelectedCategory = if (allCategories.contains(state.selectedCategory)) {
                        state.selectedCategory
                    } else {
                        "الكل"
                    }
                    state.copy(
                        productsWithUnits = products,
                        categories = allCategories,
                        selectedCategory = validSelectedCategory
                    )
                }
            }
        }

        // 2. مراقبة العملاء والموردين
        viewModelScope.launch(Dispatchers.IO) {
            partyDao.getAllParties().collectLatest { parties ->
                _uiState.update { state ->
                    val updatedSelectedParty = state.selectedParty?.let { sel ->
                        parties.find { it.id == sel.id }
                    }
                    state.copy(parties = parties, selectedParty = updatedSelectedParty)
                }
            }
        }

        // 3. مراقبة الشفت المفتوح وحالة الصندوق
        viewModelScope.launch(Dispatchers.IO) {
            shiftDao.getAllShifts().collectLatest { shifts ->
                val openShift = shifts.firstOrNull { it.status == "OPEN" } ?: shifts.firstOrNull()
                val salesTotal = openShift?.totalCashSales ?: 0.0
                val cashDrawer = if (openShift != null) {
                    openShift.openingCash + openShift.totalCashSales + openShift.totalCashCollections - openShift.totalCashExpenses
                } else {
                    0.0
                }
                _uiState.update {
                    it.copy(
                        currentShift = openShift,
                        shiftTotalSales = salesTotal,
                        cashInDrawer = cashDrawer
                    )
                }
            }
        }

        // 4. مراقبة إعدادات النظام وسياسة المخزون وحسابات الضريبة
        viewModelScope.launch(Dispatchers.IO) {
            settingsDao.getSettings().collectLatest { settings ->
                _uiState.update { state ->
                    val newSummary = recalculateSummary(state.cartItems, state.discount, settings)
                    state.copy(
                        systemSettings = settings,
                        cartSummary = newSummary
                    )
                }
            }
        }

        // 4.1 مراقبة مجموعات البيع بالقيمة من قاعدة البيانات
        viewModelScope.launch(Dispatchers.IO) {
            stockGroupDao.getAllActiveGroups().collectLatest { groups ->
                _uiState.update { state ->
                    val sel = state.selectedStockGroupForValueSale ?: groups.firstOrNull()
                    state.copy(
                        stockGroups = groups,
                        selectedStockGroupForValueSale = sel
                    )
                }
            }
        }

        // 5. تحديث سجل المعاملات والكميات المتوفرة بالمخزون
        loadTransactionHistory()
        refreshStockQuantities()
    }

    // --- وظائف البيع بالقيمة (Value Selling Functions) ---
    fun openValueSellingDialog() {
        viewModelScope.launch(Dispatchers.IO) {
            val groups = stockGroupDao.getAllActiveGroupsSync()
            _uiState.update {
                it.copy(
                    stockGroups = groups,
                    selectedStockGroupForValueSale = groups.firstOrNull(),
                    valueSaleAmountInput = "500",
                    showValueSellingDialog = true
                )
            }
        }
    }

    fun dismissValueSellingDialog() {
        _uiState.update { it.copy(showValueSellingDialog = false) }
    }

    fun selectStockGroupForValueSale(group: StockGroupEntity) {
        _uiState.update { it.copy(selectedStockGroupForValueSale = group) }
    }

    fun setValueSaleAmountInput(amountStr: String) {
        _uiState.update { it.copy(valueSaleAmountInput = amountStr) }
    }

    fun addValueSaleToCart(group: StockGroupEntity, amount: Double) {
        if (amount <= 0) return
        viewModelScope.launch(Dispatchers.IO) {
            val groupItems = stockGroupDao.getItemsForGroup(group.id)
            val fallbackProduct = productDao.getAllProductsSync().firstOrNull()

            val primaryProductId = groupItems.firstOrNull { it.productId != null && it.productId > 0 }?.productId
                ?: fallbackProduct?.id ?: 1L

            val primaryProduct = productDao.getProductById(primaryProductId) ?: fallbackProduct
            val resolvedProductId = primaryProduct?.id ?: 1L

            val productUnits = productDao.getUnitsForProductSync(resolvedProductId)
            val resolvedUnit = productUnits.firstOrNull()
            val resolvedUnitId = resolvedUnit?.id ?: 1L

            val state = _uiState.value
            val allowNegativeStock = state.systemSettings?.enableNegativeStock ?: false

            // الرقابة المخزنية والتنبيهات الفورية لمكونات المجموعة والأصناف
            if (!allowNegativeStock && (state.activeOperation == PosOperation.SALE || state.activeOperation == PosOperation.PURCHASE_RETURN)) {
                for (gItem in groupItems) {
                    if (gItem.productId != null && gItem.productId > 0) {
                        val compProduct = productDao.getProductById(gItem.productId)
                        val availableStock = stockMovementDao.getTotalStockQuantity(gItem.productId)
                        val requiredQty = gItem.defaultRatio * 1.0 // كمية المكون التقديرية

                        if (availableStock < requiredQty) {
                            _uiState.update {
                                it.copy(
                                    userFeedbackMessage = "عفواً! لا يمكن البيع بالقيمة للمجموعة '${group.name}': الكمية المتاحة في المخزن للمكون الأصلي (${compProduct?.name ?: gItem.productName}) هي (${"%.1f".format(availableStock)}) فقط. تم منع البيع بالسالب!",
                                    isError = true
                                )
                            }
                            return@launch
                        }
                    }
                }
            }

            _uiState.update { s ->
                val updatedItems = s.cartItems.toMutableList()
                updatedItems.add(
                    PosCartItem(
                        productId = resolvedProductId,
                        productName = "${group.name} (بالقيمة)",
                        productCode = group.code,
                        unitId = resolvedUnitId,
                        unitName = resolvedUnit?.unitName ?: "مجموعة",
                        conversionFactor = resolvedUnit?.conversionFactor ?: 1.0,
                        unitPrice = amount,
                        costPrice = amount * 0.75,
                        quantity = 1.0,
                        isWeighted = false,
                        availableUnits = productUnits
                    )
                )
                s.copy(
                    cartItems = updatedItems,
                    cartSummary = recalculateSummary(updatedItems, s.discount),
                    showValueSellingDialog = false,
                    userFeedbackMessage = "تم إضافة مجموعة '${group.name}' بقيمة %.2f %s بنجاح للفاتورة".format(amount, s.currencySymbol),
                    isError = false
                )
            }
        }
    }

    fun refreshData() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isRefreshing = true) }

            val baseCurrency = currencyDao.getBaseCurrency()
            if (baseCurrency != null) {
                _uiState.update {
                    it.copy(
                        currencySymbol = baseCurrency.symbol,
                        currencyName = baseCurrency.name
                    )
                }
            }

            val accounts = db.financialAccountDao().getAllAccountsSync()
            _uiState.update { state ->
                val defaultAccId = state.selectedPaymentAccountId ?: accounts.firstOrNull { it.isDefault }?.id ?: accounts.firstOrNull()?.id
                state.copy(
                    financialAccounts = accounts,
                    selectedPaymentAccountId = defaultAccId
                )
            }

            val shifts = shiftDao.getAllShiftsSync()
            val openShift = shifts.firstOrNull { it.status == "OPEN" } ?: shifts.firstOrNull()
            val salesTotal = openShift?.totalCashSales ?: 0.0
            val cashDrawer = if (openShift != null) {
                openShift.openingCash + openShift.totalCashSales + openShift.totalCashCollections - openShift.totalCashExpenses
            } else {
                0.0
            }

            val parties = partyDao.getAllPartiesSync()

            _uiState.update { state ->
                val updatedSelectedParty = state.selectedParty?.let { sel ->
                    parties.find { it.id == sel.id }
                }
                state.copy(
                    currentShift = openShift,
                    shiftTotalSales = salesTotal,
                    cashInDrawer = cashDrawer,
                    parties = parties,
                    selectedParty = updatedSelectedParty
                )
            }

            val products = productDao.getAllProductsSync()
            val stockMap = mutableMapOf<Long, Double>()
            products.forEach { p ->
                stockMap[p.id] = stockMovementDao.getTotalStockQuantity(p.id)
            }
            _uiState.update { it.copy(productStockMap = stockMap) }

            loadTransactionHistory()

            delay(600)

            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun refreshStockQuantities() {
        viewModelScope.launch(Dispatchers.IO) {
            val products = productDao.getAllProductsSync()
            val stockMap = mutableMapOf<Long, Double>()
            products.forEach { p ->
                stockMap[p.id] = stockMovementDao.getTotalStockQuantity(p.id)
            }
            _uiState.update { it.copy(productStockMap = stockMap) }
        }
    }

    fun loadTransactionHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            val invoices = invoiceDao.getAllInvoicesSync()
            val vouchers = voucherDao.getAllVouchersSync()
            val expenses = expenseDao.getAllExpensesSync()

            val records = mutableListOf<PosTransactionRecord>()

            invoices.forEach { inv ->
                val party = inv.partyId?.let { partyDao.getPartyById(it) }
                val partyName = party?.name ?: if (inv.type == InvoiceType.SALE) "عميل نقدي" else "مورد عام"
                val op = when (inv.type) {
                    InvoiceType.SALE -> PosOperation.SALE
                    InvoiceType.PURCHASE -> PosOperation.PURCHASE
                    InvoiceType.SALE_RETURN -> PosOperation.SALE_RETURN
                    InvoiceType.PURCHASE_RETURN -> PosOperation.PURCHASE_RETURN
                }
                records.add(
                    PosTransactionRecord(
                        id = inv.invoiceNumber,
                        partyName = partyName,
                        date = dateFormat.format(Date(inv.date)),
                        amount = inv.total,
                        status = inv.status.labelArabic,
                        operation = op,
                        paymentMethod = inv.paymentMethod,
                        notes = inv.notes,
                        rawTimestamp = inv.date,
                        partyId = inv.partyId,
                        dbId = inv.id
                    )
                )
            }

            vouchers.forEach { v ->
                val party = partyDao.getPartyById(v.partyId)
                val isPay = v.isPayment || (party?.type == PartyType.SUPPLIER && !v.voucherNumber.startsWith("RCV"))
                val defaultName = if (isPay) "مورد عام" else "عميل عام"
                val op = if (isPay) PosOperation.EXPENSE else PosOperation.RECEIPT
                records.add(
                    PosTransactionRecord(
                        id = v.voucherNumber,
                        partyName = party?.name ?: defaultName,
                        date = dateFormat.format(Date(v.date)),
                        amount = v.amount,
                        status = if (isPay) "سند صرف" else "سند قبض",
                        operation = op,
                        paymentMethod = v.paymentMethod,
                        notes = v.notes,
                        rawTimestamp = v.date,
                        partyId = v.partyId,
                        dbId = v.id
                    )
                )
            }

            expenses.forEach { exp ->
                records.add(
                    PosTransactionRecord(
                        id = exp.expenseNumber,
                        partyName = exp.paidTo.ifEmpty { exp.category },
                        date = dateFormat.format(Date(exp.date)),
                        amount = exp.amount,
                        status = "مصروف",
                        operation = PosOperation.EXPENSE,
                        paymentMethod = exp.paymentMethod,
                        notes = exp.notes,
                        rawTimestamp = exp.date,
                        dbId = exp.id
                    )
                )
            }

            // ترتيب زمني تنازلي (الأحدث أولاً)
            records.sortByDescending { it.rawTimestamp }

            _uiState.update { it.copy(transactionRecords = records) }
        }
    }

    /**
     * فتح شاشة تفاصيل العملية المنبثقة وعرض جميع البيانات والبنود بالكامل
     */
    fun showTransactionDetails(record: PosTransactionRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update {
                it.copy(
                    showTransactionDetailDialog = true,
                    selectedTransactionDetailRecord = record,
                    selectedInvoiceDetails = null,
                    selectedInvoiceItemDetails = emptyList(),
                    selectedVoucherDetails = null,
                    selectedExpenseDetails = null,
                    selectedDetailPartyName = record.partyName,
                    selectedDetailPartyPhone = ""
                )
            }

            if (record.operation.isInvoiceType) {
                val invoice = invoiceDao.getInvoiceByInvoiceNumber(record.id)
                if (invoice != null) {
                    val party = invoice.partyId?.let { partyDao.getPartyById(it) }
                    val items = invoiceDao.getInvoiceItems(invoice.id)
                    val itemDetails = items.map { item ->
                        val product = productDao.getProductById(item.productId)
                        val units = productDao.getUnitsForProductSync(item.productId)
                        val unit = units.find { u -> u.id == item.productUnitId }
                        TransactionItemDetail(
                            productName = product?.name ?: "صنف #${item.productId}",
                            productCode = product?.code ?: "",
                            unitName = unit?.unitName ?: "حبة",
                            quantity = item.quantity,
                            unitPrice = item.unitSellingPrice,
                            discount = item.discount,
                            totalPrice = item.totalPrice
                        )
                    }
                    _uiState.update {
                        it.copy(
                            selectedInvoiceDetails = invoice,
                            selectedInvoiceItemDetails = itemDetails,
                            selectedDetailPartyName = party?.name ?: record.partyName,
                            selectedDetailPartyPhone = party?.phone ?: ""
                        )
                    }
                }
            } else if (record.id.startsWith("RCV") || record.id.startsWith("PAY") || record.operation == PosOperation.RECEIPT) {
                val voucher = voucherDao.getAllVouchersSync().find { it.voucherNumber == record.id }
                if (voucher != null) {
                    val party = partyDao.getPartyById(voucher.partyId)
                    _uiState.update {
                        it.copy(
                            selectedVoucherDetails = voucher,
                            selectedDetailPartyName = party?.name ?: record.partyName,
                            selectedDetailPartyPhone = party?.phone ?: ""
                        )
                    }
                }
            } else if (record.id.startsWith("EXP") || record.operation == PosOperation.EXPENSE) {
                val expense = expenseDao.getAllExpensesSync().find { it.expenseNumber == record.id }
                if (expense != null) {
                    _uiState.update {
                        it.copy(
                            selectedExpenseDetails = expense,
                            selectedDetailPartyName = expense.paidTo.ifEmpty { expense.category }
                        )
                    }
                }
            }
        }
    }

    fun dismissTransactionDetailDialog() {
        _uiState.update {
            it.copy(
                showTransactionDetailDialog = false,
                selectedTransactionDetailRecord = null,
                selectedInvoiceDetails = null,
                selectedInvoiceItemDetails = emptyList(),
                selectedVoucherDetails = null,
                selectedExpenseDetails = null
            )
        }
    }

    /**
     * تحميل وعرض كافة تفاصيل العملية كاملة في حقول الشاشة الرئيسية لتعديلها بشكل صحيح
     */
    fun loadRecordForEditing(record: PosTransactionRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            if (record.operation.isInvoiceType) {
                val invoice = invoiceDao.getInvoiceByInvoiceNumber(record.id)
                if (invoice != null) {
                    val items = invoiceDao.getInvoiceItems(invoice.id)
                    val party = invoice.partyId?.let { partyDao.getPartyById(it) }

                    val loadedCartItems = items.map { item ->
                        val product = productDao.getProductById(item.productId)
                        val units = if (product != null) productDao.getUnitsForProductSync(product.id) else emptyList()
                        val unit = units.find { u -> u.id == item.productUnitId }

                        PosCartItem(
                            productId = item.productId,
                            productName = product?.name ?: "صنف #${item.productId}",
                            productCode = product?.code ?: "",
                            unitId = item.productUnitId,
                            unitName = unit?.unitName ?: "حبة",
                            conversionFactor = item.unitConversionFactor,
                            unitPrice = item.unitSellingPrice,
                            costPrice = item.unitCostPrice,
                            quantity = item.quantity,
                            discount = item.discount,
                            isWeighted = product?.isWeighted ?: false,
                            availableUnits = units
                        )
                    }

                    _uiState.update { state ->
                        state.copy(
                            activeOperation = record.operation,
                            selectedParty = party,
                            cartItems = loadedCartItems,
                            discount = invoice.discount,
                            paymentMethod = invoice.paymentMethod,
                            editingInvoice = invoice,
                            showHistoryDialog = false,
                            showTransactionDetailDialog = false,
                            userFeedbackMessage = "تم تحميل كامل تفاصيل الفاتورة #${invoice.invoiceNumber} وبنودها (${items.size} بند) في حقول الشاشة الرئيسية للتعديل.",
                            isError = false
                        )
                    }
                    recalculateSummary(loadedCartItems, invoice.discount)
                }
            } else if (record.id.startsWith("RCV") || record.id.startsWith("PAY") || record.operation == PosOperation.RECEIPT) {
                val voucher = voucherDao.getAllVouchersSync().find { it.voucherNumber == record.id }
                if (voucher != null) {
                    val party = partyDao.getPartyById(voucher.partyId)
                    val op = if (voucher.isPayment) PosOperation.EXPENSE else PosOperation.RECEIPT

                    _uiState.update { state ->
                        state.copy(
                            activeOperation = op,
                            voucherParty = party,
                            selectedParty = party,
                            voucherAmountInput = voucher.amount.toString(),
                            voucherNotesInput = voucher.notes,
                            voucherPaymentMethod = voucher.paymentMethod,
                            editingVoucherRecord = record,
                            showHistoryDialog = false,
                            showTransactionDetailDialog = false,
                            userFeedbackMessage = "تم تحميل كامل بيانات السند #${voucher.voucherNumber} في حقول الشاشة الرئيسية للتعديل.",
                            isError = false
                        )
                    }
                }
            } else if (record.id.startsWith("EXP") || record.operation == PosOperation.EXPENSE) {
                val expense = expenseDao.getAllExpensesSync().find { it.expenseNumber == record.id }
                if (expense != null) {
                    _uiState.update { state ->
                        state.copy(
                            activeOperation = PosOperation.EXPENSE,
                            voucherAmountInput = expense.amount.toString(),
                            voucherNotesInput = expense.notes,
                            voucherPaymentMethod = expense.paymentMethod,
                            editingVoucherRecord = record,
                            showHistoryDialog = false,
                            showTransactionDetailDialog = false,
                            userFeedbackMessage = "تم تحميل كامل بيانات المصروف #${expense.expenseNumber} في حقول الشاشة الرئيسية للتعديل.",
                            isError = false
                        )
                    }
                }
            }
        }
    }

    // --- تبديل العمليات النشطة ---
    fun selectOperation(operation: PosOperation) {
        _uiState.update { state ->
            // عند الانتقال لعملية جديدة نحدد الطرف الافتراضي المناسب
            val defaultParty = when (operation) {
                PosOperation.SALE, PosOperation.SALE_RETURN -> null // عميل نقدي افتراضياً
                PosOperation.PURCHASE, PosOperation.PURCHASE_RETURN -> {
                    state.parties.firstOrNull { it.type == PartyType.SUPPLIER || it.type == PartyType.BOTH }
                }
                PosOperation.RECEIPT -> {
                    state.parties.firstOrNull { it.type == PartyType.CUSTOMER || it.type == PartyType.BOTH }
                }
                PosOperation.EXPENSE -> {
                    state.parties.firstOrNull { it.type == PartyType.SUPPLIER }
                }
            }

            state.copy(
                activeOperation = operation,
                selectedParty = defaultParty,
                voucherParty = defaultParty,
                userFeedbackMessage = null
            )
        }
    }

    // --- اختيار العميل أو المورد ---
    fun selectParty(party: PartyEntity?) {
        if (party == null) {
            _uiState.update { it.copy(selectedParty = null) }
        } else {
            viewModelScope.launch(Dispatchers.IO) {
                val freshParty = partyDao.getPartyById(party.id) ?: party
                _uiState.update { it.copy(selectedParty = freshParty) }
            }
        }
    }

    fun selectVoucherParty(party: PartyEntity?) {
        _uiState.update { it.copy(voucherParty = party) }
    }

    // --- إدارة السلة (للعمليات الأربع: بيع، شراء، مردود بيع، مردود شراء) ---
    fun addToCart(product: ProductEntity, unit: ProductUnitEntity, quantity: Double = 1.0) {
        _uiState.update { state ->
            val productUnits = state.productsWithUnits.find { it.product.id == product.id }?.units ?: listOf(unit)
            val existingIndex = state.cartItems.indexOfFirst {
                it.productId == product.id && it.unitId == unit.id
            }

            val updatedItems = state.cartItems.toMutableList()
            if (existingIndex >= 0) {
                val existing = updatedItems[existingIndex]
                updatedItems[existingIndex] = existing.copy(
                    quantity = existing.quantity + quantity,
                    availableUnits = if (existing.availableUnits.isEmpty()) productUnits else existing.availableUnits
                )
            } else {
                val price = when (state.activeOperation) {
                    PosOperation.PURCHASE, PosOperation.PURCHASE_RETURN -> unit.costPrice
                    else -> unit.sellingPrice
                }
                updatedItems.add(
                    PosCartItem(
                        productId = product.id,
                        productName = product.name,
                        productCode = product.code,
                        unitId = unit.id,
                        unitName = unit.unitName,
                        conversionFactor = unit.conversionFactor,
                        unitPrice = price,
                        costPrice = unit.costPrice,
                        quantity = quantity,
                        isWeighted = product.isWeighted,
                        availableUnits = productUnits
                    )
                )
            }

            state.copy(
                cartItems = updatedItems,
                cartSummary = recalculateSummary(updatedItems, state.discount)
            )
        }
    }

    fun changeCartItemUnit(cartItemId: String, targetUnit: ProductUnitEntity) {
        _uiState.update { state ->
            val updatedItems = state.cartItems.map { item ->
                if (item.cartItemId == cartItemId) {
                    val newPrice = when (state.activeOperation) {
                        PosOperation.PURCHASE, PosOperation.PURCHASE_RETURN -> targetUnit.costPrice
                        else -> targetUnit.sellingPrice
                    }
                    item.copy(
                        unitId = targetUnit.id,
                        unitName = targetUnit.unitName,
                        conversionFactor = targetUnit.conversionFactor,
                        unitPrice = newPrice,
                        costPrice = targetUnit.costPrice
                    )
                } else item
            }
            state.copy(
                cartItems = updatedItems,
                cartSummary = recalculateSummary(updatedItems, state.discount)
            )
        }
    }

    fun updateCartItemNote(cartItemId: String, note: String) {
        _uiState.update { state ->
            val updatedItems = state.cartItems.map { item ->
                if (item.cartItemId == cartItemId) {
                    item.copy(notes = note)
                } else item
            }
            state.copy(cartItems = updatedItems)
        }
    }

    fun updateCartItemPrice(cartItemId: String, newPrice: Double) {
        _uiState.update { state ->
            val updatedItems = state.cartItems.map { item ->
                if (item.cartItemId == cartItemId) {
                    item.copy(unitPrice = newPrice.coerceAtLeast(0.0))
                } else item
            }
            state.copy(
                cartItems = updatedItems,
                cartSummary = recalculateSummary(updatedItems, state.discount)
            )
        }
    }

    fun addQuickProductToCart(name: String, price: Double, quantity: Double = 1.0) {
        _uiState.update { state ->
            val updatedItems = state.cartItems.toMutableList()
            updatedItems.add(
                PosCartItem(
                    productId = 0L,
                    productName = name,
                    productCode = "QUICK",
                    unitId = 0L,
                    unitName = "حبة",
                    conversionFactor = 1.0,
                    unitPrice = price,
                    costPrice = price * 0.7,
                    quantity = quantity,
                    isWeighted = false
                )
            )
            state.copy(
                cartItems = updatedItems,
                cartSummary = recalculateSummary(updatedItems, state.discount)
            )
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

    fun updateCartQuantity(cartItemId: String, newQty: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val state = _uiState.value
            val targetItem = state.cartItems.find { it.cartItemId == cartItemId } ?: return@launch

            // 1. التحقق الرقابي الصارم: عدم تجاوز الكمية المتبقية القابلة للرد من الفاتورة الأصلية
            if (targetItem.originalInvoiceQuantity != null && newQty > targetItem.originalInvoiceQuantity + 0.0001) {
                val warningMsg = "الكمية المحددة للرد (${"%.2f".format(newQty)} ${targetItem.unitName}) تتجاوز الكمية المتبقية القابلة للرد في الفاتورة الأصلية (${"%.2f".format(targetItem.originalInvoiceQuantity)} ${targetItem.unitName})."
                _uiState.update {
                    it.copy(
                        showReturnQuantityWarningDialog = true,
                        returnQuantityWarningMessage = warningMsg
                    )
                }
                return@launch
            }

            // 2. التحقق الرقابي من رصيد المخزن الفعلي المتاح (لمردودات المشتريات)
            if (state.activeOperation == PosOperation.PURCHASE_RETURN && targetItem.productId > 0) {
                val currentStockBase = stockMovementDao.getTotalStockQuantity(targetItem.productId)
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
            }

            val updatedItems = if (newQty <= 0.001) {
                state.cartItems.filterNot { it.cartItemId == cartItemId }
            } else {
                state.cartItems.map {
                    if (it.cartItemId == cartItemId) it.copy(quantity = newQty) else it
                }
            }

            _uiState.update {
                it.copy(
                    cartItems = updatedItems,
                    cartSummary = recalculateSummary(updatedItems, it.discount)
                )
            }
        }
    }

    fun removeCartItem(cartItemId: String) {
        _uiState.update { state ->
            val updated = state.cartItems.filterNot { it.cartItemId == cartItemId }
            state.copy(
                cartItems = updated,
                cartSummary = recalculateSummary(updated, state.discount)
            )
        }
    }

    fun clearCart() {
        _uiState.update { state ->
            state.copy(
                cartItems = emptyList(),
                cartSummary = recalculateSummary(emptyList(), 0.0),
                discount = 0.0,
                paidAmountInput = "",
                paymentTransactionRef = "",
                paymentReceiptImagePath = null,
                selectedPaymentAccountId = null,
                activeMobileTab = PosMobileTab.CATALOG,
                selectedTab = PosMobileTab.CATALOG
            )
        }
    }

    fun setDiscount(discount: Double) {
        _uiState.update { state ->
            state.copy(
                discount = discount,
                cartSummary = recalculateSummary(state.cartItems, discount)
            )
        }
    }

    fun setPaymentMethod(method: PaymentMethod) {
        _uiState.update { it.copy(paymentMethod = method) }
    }

    fun setSelectedPaymentAccount(accountId: Long?) {
        _uiState.update { it.copy(selectedPaymentAccountId = accountId) }
    }

    fun setPaymentTransactionRef(ref: String) {
        _uiState.update { it.copy(paymentTransactionRef = ref) }
    }

    fun setPaymentReceiptImagePath(path: String?) {
        _uiState.update { it.copy(paymentReceiptImagePath = path) }
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

    private fun recalculateSummary(
        items: List<PosCartItem>,
        discount: Double,
        settings: SystemSettingsEntity? = _uiState.value.systemSettings
    ): CartSummary {
        val totalQty = items.sumOf { it.quantity }
        val subtotal = items.sumOf { it.quantity * it.unitPrice }
        val itemsDiscount = items.sumOf { it.discount }
        val totalDiscount = itemsDiscount + discount
        val taxable = (subtotal - totalDiscount).coerceAtLeast(0.0)

        val isTaxEnabled = settings?.isTaxEnabled ?: false
        val rawRate = settings?.defaultTaxRate ?: 0.0
        val taxRatePercent = if (!isTaxEnabled) 0.0 else if (rawRate <= 1.0 && rawRate > 0.0) rawRate * 100.0 else rawRate
        val taxAmount = if (isTaxEnabled) taxable * (taxRatePercent / 100.0) else 0.0
        val finalTotal = taxable + taxAmount

        return CartSummary(
            itemsCount = items.size,
            totalQuantity = totalQty,
            subtotal = subtotal,
            discount = totalDiscount,
            taxableAmount = taxable,
            taxRatePercent = taxRatePercent,
            taxAmount = taxAmount,
            finalTotal = finalTotal
        )
    }

    // --- تنفيذ عمليات الفواتير (بيع، شراء، مردود بيع، مردود شراء) ---
    fun executeInvoiceTransaction() {
        val state = _uiState.value
        if (state.cartItems.isEmpty()) {
            _uiState.update { it.copy(userFeedbackMessage = "السلة فارغة! الرجاء إضافة أصناف أولاً.", isError = true) }
            return
        }

        // التحقق المحاسبي: إذا كان الدفع آجلاً يجب تحديد عميل أو مورد
        if (state.paymentMethod == PaymentMethod.CREDIT && state.selectedParty == null) {
            _uiState.update {
                it.copy(
                    userFeedbackMessage = "البيع أو الشراء الآجل يتطلب تحديد العميل أو المورد لتسجيل المديونية في كشف حسابه.",
                    isError = true
                )
            }
            return
        }

        // قاعدة التحقق من الحد الائتماني للعميل عند البيع (خصوصاً البيع الآجل أو عندما يتجاوز رصيد العميل حده الائتماني)
        val selectedParty = state.selectedParty
        if (selectedParty != null && selectedParty.creditLimit > 0 &&
            (state.activeOperation == PosOperation.SALE || state.activeOperation == PosOperation.SALE_RETURN)
        ) {
            val invoiceTotal = state.cartSummary.finalTotal
            val currentBal = selectedParty.currentBalance
            val newLiability = if (state.paymentMethod == PaymentMethod.CREDIT) invoiceTotal else 0.0
            val expectedTotalLiability = currentBal + newLiability

            // إذا كانت المديونية المتوقعة تتجاوز الحد الائتماني المسموح به لهذا العميل
            if (expectedTotalLiability > selectedParty.creditLimit) {
                _uiState.update {
                    it.copy(
                        showCreditLimitWarningDialog = true,
                        creditLimitWarningParty = selectedParty,
                        creditLimitWarningInvoiceTotal = invoiceTotal,
                        creditLimitWarningCurrentBalance = currentBal,
                        creditLimitWarningLimit = selectedParty.creditLimit
                    )
                }
                return
            }
        }

        // التحقق من سقف الكميات القابلة للرد بالفاتورة عند تنفيذ مردود بيع أو شراء
        if (state.activeOperation == PosOperation.SALE_RETURN || state.activeOperation == PosOperation.PURCHASE_RETURN) {
            for (item in state.cartItems) {
                if (item.originalInvoiceQuantity != null && item.quantity > item.originalInvoiceQuantity + 0.0001) {
                    val warningMsg = "الكمية المحددة للرد من الصنف (${item.productName}) تبلغ (${"%.2f".format(item.quantity)} ${item.unitName}) وهي تتجاوز الكمية المتبقية القابلة للرد في الفاتورة الأصلية (${"%.2f".format(item.originalInvoiceQuantity)} ${item.unitName})."
                    _uiState.update {
                        it.copy(
                            showReturnQuantityWarningDialog = true,
                            returnQuantityWarningMessage = warningMsg
                        )
                    }
                    return
                }
            }
        }

        // التحقق من توفر المخزون عند البيع أو مردود الشراء إذا كان البيع بالسالب غير مسموح
        val allowNegativeStock = state.systemSettings?.enableNegativeStock ?: false
        if (!allowNegativeStock && (state.activeOperation == PosOperation.SALE || state.activeOperation == PosOperation.PURCHASE_RETURN)) {
            viewModelScope.launch(Dispatchers.IO) {
                for (item in state.cartItems) {
                    if (item.productId > 0) {
                        val requiredBase = item.quantity * item.conversionFactor
                        val availableBase = stockMovementDao.getTotalStockQuantity(item.productId)
                        if (availableBase < requiredBase) {
                            val availableInUnit = (availableBase / item.conversionFactor).coerceAtLeast(0.0)
                            _uiState.update {
                                it.copy(
                                    userFeedbackMessage = "لا يمكن إتمام البيع: الكمية المتوفرة بالمخزون من الصنف (${item.productName}) هي ${String.format(Locale.US, "%.2f", availableInUnit)} ${item.unitName} فقط، بينما الكمية المطلوبة بالسلة هي ${item.quantity} ${item.unitName}! (يمكن لمدير النظام تفعيل خيار البيع بالسالب من شاشة الضبط).",
                                    isError = true
                                )
                            }
                            return@launch
                        }
                    }
                }
                proceedInvoiceCheckout()
            }
            return
        }

        proceedInvoiceCheckout()
    }

    fun confirmCreditLimitOverride() {
        _uiState.update { it.copy(showCreditLimitWarningDialog = false) }
        proceedInvoiceCheckout()
    }

    fun dismissCreditLimitWarning() {
        _uiState.update { it.copy(showCreditLimitWarningDialog = false) }
    }

    private fun proceedInvoiceCheckout() {
        val state = _uiState.value
        _uiState.update { it.copy(isProcessingCheckout = true) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                db.withTransaction {
                    val timestamp = System.currentTimeMillis()
                    val totalInvoicesCount = invoiceDao.countInvoices() + 1
                    val prefix = when (state.activeOperation) {
                        PosOperation.SALE -> "INV-2026"
                        PosOperation.PURCHASE -> "PUR-2026"
                        PosOperation.SALE_RETURN -> "RET-S"
                        PosOperation.PURCHASE_RETURN -> "RET-P"
                        else -> "INV"
                    }
                    val invoiceNumber = "%s-%04d".format(prefix, totalInvoicesCount)

                    val invType = when (state.activeOperation) {
                        PosOperation.SALE -> InvoiceType.SALE
                        PosOperation.PURCHASE -> InvoiceType.PURCHASE
                        PosOperation.SALE_RETURN -> InvoiceType.SALE_RETURN
                        PosOperation.PURCHASE_RETURN -> InvoiceType.PURCHASE_RETURN
                        else -> InvoiceType.SALE
                    }

                    val isCredit = state.paymentMethod == PaymentMethod.CREDIT
                    val paid = if (isCredit) 0.0 else state.cartSummary.finalTotal
                    val remaining = if (isCredit) state.cartSummary.finalTotal else 0.0

                    val returnNotePart = state.originalInvoiceForReturn?.let { " - مرتبط بالفاتورة رقم: ${it.invoiceNumber}" } ?: ""

                    val isTaxEnabled = state.systemSettings?.isTaxEnabled ?: false
                    val rawRate = state.systemSettings?.defaultTaxRate ?: 0.0
                    val currentTaxRateDecimal = if (isTaxEnabled) (if (rawRate <= 1.0 && rawRate > 0.0) rawRate else rawRate / 100.0) else 0.0
                    val currentTaxAmount = if (isTaxEnabled) state.cartSummary.taxAmount else 0.0

                    val selectedAccount = state.financialAccounts.find { it.id == state.selectedPaymentAccountId }
                    val actualAccountName = when {
                        state.paymentMethod == PaymentMethod.CASH -> PaymentMethod.CASH.labelArabic
                        state.paymentMethod == PaymentMethod.CREDIT -> PaymentMethod.CREDIT.labelArabic
                        selectedAccount != null -> selectedAccount.name
                        else -> state.paymentMethod.labelArabic
                    }

                    // 1. إنشاء وحفظ رأس الفاتورة
                    val invoiceId = invoiceDao.insertInvoice(
                        InvoiceEntity(
                            invoiceNumber = invoiceNumber,
                            type = invType,
                            partyId = state.selectedParty?.id,
                            date = timestamp,
                            currencyId = 1, // الريال الأساسي
                            exchangeRate = 1.0,
                            subtotal = state.cartSummary.subtotal,
                            discount = state.cartSummary.discount,
                            taxRate = currentTaxRateDecimal,
                            taxAmount = currentTaxAmount,
                            total = state.cartSummary.finalTotal,
                            paidAmount = paid,
                            remainingAmount = remaining,
                            paymentMethod = state.paymentMethod,
                            paymentAccountId = if (state.paymentMethod.isPhysicalCash) null else state.selectedPaymentAccountId,
                            transactionRef = state.originalInvoiceForReturn?.invoiceNumber ?: state.paymentTransactionRef,
                            receiptImagePath = state.paymentReceiptImagePath,
                            paymentProviderName = actualAccountName,
                            status = InvoiceStatus.COMPLETED,
                            notes = "عملية نقطة البيع (${state.activeOperation.titleArabic})$returnNotePart"
                        )
                    )

                    // 2. حفظ بنود الفاتورة وحركات المخزون وحساب التكلفة
                    val itemsToInsert = mutableListOf<InvoiceItemEntity>()
                    val movementsToInsert = mutableListOf<StockMovementEntity>()

                    val fallbackProduct = productDao.getAllProductsSync().firstOrNull()
                    val fallbackProductUnits = if (fallbackProduct != null) productDao.getUnitsForProductSync(fallbackProduct.id) else emptyList()
                    val subItemProductIds = stockGroupDao.getAllSubItemProductIdsSync().toSet()

                    state.cartItems.forEach { item ->
                        var validProdId = item.productId
                        var validUnitId = item.unitId

                        if (validProdId <= 0L || validUnitId <= 0L) {
                            if (fallbackProduct != null) {
                                validProdId = fallbackProduct.id
                                validUnitId = fallbackProductUnits.firstOrNull()?.id ?: 1L
                            }
                        }

                        itemsToInsert.add(
                            InvoiceItemEntity(
                                invoiceId = invoiceId,
                                productId = validProdId,
                                productUnitId = validUnitId,
                                quantity = item.quantity,
                                unitConversionFactor = item.conversionFactor,
                                unitCostPrice = item.costPrice,
                                unitSellingPrice = item.unitPrice,
                                discount = item.discount,
                                taxRate = currentTaxRateDecimal,
                                totalPrice = item.totalPrice
                            )
                        )

                        // حركة المخزون المحاسبية
                        // سياسة حركة المخزون: الأصناف الفرعية المكونة للمجموعات لا تُخصم لحظياً أثناء البيع اليومي ويبقى رصيدها ثابتاً لحين الجرد الدوري
                        val isSubItemInGroup = validProdId in subItemProductIds
                        val isDailySale = state.activeOperation == PosOperation.SALE

                        if (!isDailySale || !isSubItemInGroup) {
                            val movementType = when (state.activeOperation) {
                                PosOperation.SALE -> MovementType.SALE_OUT
                                PosOperation.PURCHASE -> MovementType.PURCHASE_IN
                                PosOperation.SALE_RETURN -> MovementType.RETURN_IN
                                PosOperation.PURCHASE_RETURN -> MovementType.RETURN_OUT
                                else -> MovementType.SALE_OUT
                            }

                            val qtyBase = when (state.activeOperation) {
                                PosOperation.SALE, PosOperation.PURCHASE_RETURN -> -(item.quantity * item.conversionFactor)
                                PosOperation.PURCHASE, PosOperation.SALE_RETURN -> (item.quantity * item.conversionFactor)
                                else -> 0.0
                            }

                            movementsToInsert.add(
                                StockMovementEntity(
                                    productId = validProdId,
                                    productUnitId = validUnitId,
                                    movementType = movementType,
                                    quantityBaseUnit = qtyBase,
                                    remainingQuantityForFifo = if (movementType == MovementType.PURCHASE_IN) item.quantity * item.conversionFactor else 0.0,
                                    unitCostPriceBase = item.costPrice / item.conversionFactor,
                                    timestamp = timestamp,
                                    referenceNumber = invoiceNumber
                                )
                            )
                        }

                        // تحديث المتوسط المرجح للتكلفة (WAC) في حالة الشراء
                        if (state.activeOperation == PosOperation.PURCHASE && item.productId > 0) {
                            val currentStock = stockMovementDao.getTotalStockQuantity(item.productId)
                            val purchasedBaseQty = item.quantity * item.conversionFactor
                            val unitCostBase = item.costPrice / item.conversionFactor

                            val newWacCost = if (currentStock > 0.001) {
                                val oldUnitCost = item.costPrice / item.conversionFactor
                                ((currentStock * oldUnitCost) + (purchasedBaseQty * unitCostBase)) / (currentStock + purchasedBaseQty)
                            } else {
                                unitCostBase
                            }

                            // تحديث سعر تكلفة الوحدة
                            val unitEntity = productDao.getUnitById(item.unitId)
                            if (unitEntity != null) {
                                productDao.updateUnit(
                                    unitEntity.copy(costPrice = newWacCost * unitEntity.conversionFactor)
                                )
                            }
                        }
                    }

                    invoiceDao.insertInvoiceItems(itemsToInsert)
                    stockMovementDao.insertMovements(movementsToInsert)

                    val selPartyId = state.selectedParty?.id
                    val partyBeforeTransaction = selPartyId?.let { partyDao.getPartyById(it) } ?: state.selectedParty
                    val rawOldBal = partyBeforeTransaction?.currentBalance ?: 0.0

                    val amountDiff = when (state.activeOperation) {
                        PosOperation.SALE -> remaining // زيادة دين العميل (مدين لنا)
                        PosOperation.SALE_RETURN -> -state.cartSummary.finalTotal // إنقاص دين العميل
                        PosOperation.PURCHASE -> -remaining // زيادة التزامنا للمورد (دائن لنا بالسالب)
                        PosOperation.PURCHASE_RETURN -> state.cartSummary.finalTotal // إنقاص التزام المورد
                        else -> 0.0
                    }

                    // 3. تحديث مديونية العميل أو المورد إذا كانت العملية آجلة
                    if (isCredit && selPartyId != null) {
                        partyDao.updateBalance(selPartyId, amountDiff)
                    }

                    // 4. تحديث الحسابات المالية والشفت حسب طريقة السداد المختارة
                    val openShift = getOrCreateOpenShift(shiftDao)
                    val accountDao = db.financialAccountDao()

                    val accountDelta = when (state.activeOperation) {
                        PosOperation.SALE, PosOperation.PURCHASE_RETURN -> paid
                        PosOperation.PURCHASE, PosOperation.SALE_RETURN -> -paid
                        else -> 0.0
                    }

                    if (state.paymentMethod.isPhysicalCash) {
                        // أ) سداد نقدي (كاش في الدرج)
                        when (state.activeOperation) {
                            PosOperation.SALE -> shiftDao.updateSales(openShift.id, openShift.totalCashSales + paid)
                            PosOperation.SALE_RETURN -> shiftDao.updateSales(openShift.id, (openShift.totalCashSales - paid).coerceAtLeast(0.0))
                            PosOperation.PURCHASE -> shiftDao.updateExpenses(openShift.id, openShift.totalCashExpenses + paid)
                            PosOperation.PURCHASE_RETURN -> shiftDao.updateCollections(openShift.id, openShift.totalCashCollections + paid)
                            else -> {}
                        }
                        // تحديث حساب صندوق النقدية الرئيسي (10101)
                        val cashAcc = accountDao.getAccountByCode("10101")
                        if (cashAcc != null && accountDelta != 0.0) {
                            accountDao.updateBalance(cashAcc.id, accountDelta)
                        }
                    } else if (state.paymentMethod.isElectronic) {
                        // ب) سداد إلكتروني (شبكة / محفظة / تحويل)
                        when (state.paymentMethod) {
                            PaymentMethod.POS_CARD, PaymentMethod.MADA -> shiftDao.addMadaSales(openShift.id, paid)
                            PaymentMethod.E_WALLET -> shiftDao.addWalletSales(openShift.id, paid)
                            PaymentMethod.BANK_TRANSFER, PaymentMethod.EXCHANGE_NETWORK -> shiftDao.addTransferSales(openShift.id, paid)
                            else -> {}
                        }
                        // ترحيل المبلغ لحساب البنك أو المحفظة المختارة
                        val targetAccId = state.selectedPaymentAccountId
                            ?: accountDao.getAllAccountsSync().firstOrNull { it.accountType != com.example.dokkani.data.local.entities.FinancialAccountType.CASH_DRAWER }?.id
                        if (targetAccId != null && accountDelta != 0.0) {
                            accountDao.updateBalance(targetAccId, accountDelta)
                        }
                    } else if (state.paymentMethod == PaymentMethod.CREDIT) {
                        // ج) سداد آجل
                        shiftDao.addCreditSales(openShift.id, state.cartSummary.finalTotal)
                    }

                    // 5. بناء نتيجة الدفع وتجهيز الإيصال
                    val receiptItems = state.cartItems.map {
                        ReceiptItemData(
                            name = it.productName,
                            quantityFormatted = it.quantityFormatted,
                            unitPrice = it.unitPrice,
                            totalPrice = it.totalPrice,
                            isWeighted = it.isWeighted
                        )
                    }

                    val rawNewBal = if (isCredit && selPartyId != null) (rawOldBal + amountDiff) else rawOldBal

                    val partyOldBal = if (partyBeforeTransaction != null) kotlin.math.abs(rawOldBal) else null
                    val partyNewBal = if (partyBeforeTransaction != null) kotlin.math.abs(rawNewBal) else null

                    val freshPartyAfter = selPartyId?.let { partyDao.getPartyById(it) }

                    val currentSettings = state.systemSettings
                    val storeName = currentSettings?.storeName?.ifBlank { "دكاني - تموينات ومخضار السعادة" } ?: "دكاني - تموينات ومخضار السعادة"
                    val storeAddress = currentSettings?.storeAddress ?: ""
                    val storePhone = currentSettings?.storePhone ?: ""
                    val taxNumber = currentSettings?.taxNumber ?: ""
                    val footerText = currentSettings?.invoiceFooterText?.ifBlank { "شكراً لتسوقكم من دكاني!" } ?: "شكراً لتسوقكم من دكاني!"
                    val showPrevBalanceSetting = currentSettings?.showPreviousBalanceOnInvoice ?: true

                    val invoiceTitle = when (state.activeOperation) {
                        PosOperation.SALE -> "فاتورة بيع"
                        PosOperation.PURCHASE -> "فاتورة شراء"
                        PosOperation.SALE_RETURN -> "مردود بيع"
                        PosOperation.PURCHASE_RETURN -> "مردود شراء"
                        PosOperation.RECEIPT -> "سند قبض"
                        PosOperation.EXPENSE -> "سند صرف"
                    }
                    val isPurch = state.activeOperation == PosOperation.PURCHASE || state.activeOperation == PosOperation.PURCHASE_RETURN
                    val partyLabel = if (isPurch) "المورد:" else "العميل:"
                    val partyName = state.selectedParty?.name ?: (if (isPurch) "مورد نقدي" else "عميل نقدي")

                    val receiptData = ReceiptPrintData(
                        storeName = storeName,
                        storeAddress = storeAddress,
                        storePhone = storePhone,
                        taxNumber = taxNumber,
                        invoiceTitle = invoiceTitle,
                        invoiceNumber = invoiceNumber,
                        invoiceDateFormatted = dateFormat.format(Date(timestamp)),
                        cashierName = "كاشير 1",
                        customerName = partyName,
                        partyLabel = partyLabel,
                        isCredit = isCredit,
                        paymentMethodArabic = actualAccountName,
                        items = receiptItems,
                        subtotal = state.cartSummary.subtotal,
                        discount = state.cartSummary.discount,
                        taxRatePercent = state.cartSummary.taxRatePercent,
                        taxAmount = state.cartSummary.taxAmount,
                        total = state.cartSummary.finalTotal,
                        paidAmount = paid,
                        remainingAmount = remaining,
                        customerOldBalance = partyOldBal,
                        customerNewBalance = partyNewBal,
                        showPreviousBalance = showPrevBalanceSetting,
                        currencySymbol = state.currencySymbol,
                        footerText = footerText
                    )

                    val result = PosCheckoutResult(
                        success = true,
                        invoiceId = invoiceId,
                        invoiceNumber = invoiceNumber,
                        total = state.cartSummary.finalTotal,
                        paymentMethod = state.paymentMethod,
                        paidAmount = paid,
                        changeAmount = 0.0,
                        remainingCreditAmount = remaining,
                        customerName = state.selectedParty?.name,
                        customerNewBalance = partyNewBal,
                        drawerKickTriggered = state.paymentMethod == PaymentMethod.CASH,
                        receiptData = receiptData,
                        message = "تم حفظ ${state.activeOperation.titleArabic} بنجاح برقم: $invoiceNumber"
                    )

                    _uiState.update {
                        it.copy(
                            isProcessingCheckout = false,
                            selectedParty = freshPartyAfter ?: it.selectedParty,
                            cartItems = emptyList(),
                            cartSummary = recalculateSummary(emptyList(), 0.0),
                            discount = 0.0,
                            paidAmountInput = "",
                            paymentTransactionRef = "",
                            paymentReceiptImagePath = null,
                            selectedPaymentAccountId = null,
                            lastCheckoutResult = result,
                            showReceiptDialog = true,
                            showCheckoutDialog = false,
                            originalInvoiceForReturn = null,
                            returnOriginalInvoiceItems = emptyList(),
                            activeMobileTab = if (it.activeOperation.isVoucher) it.activeMobileTab else PosMobileTab.CATALOG,
                            selectedTab = if (it.activeOperation.isVoucher) it.selectedTab else PosMobileTab.CATALOG,
                            activeOperation = it.activeOperation,
                            userFeedbackMessage = "تم حفظ ${it.activeOperation.titleArabic} بنجاح برقم: $invoiceNumber",
                            isError = false
                        )
                    }

                    loadTransactionHistory()
                    refreshStockQuantities()
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isProcessingCheckout = false,
                        userFeedbackMessage = "فشل حفظ العملية: ${e.localizedMessage}",
                        isError = true
                    )
                }
            }
        }
    }

    // --- نموذج السندات المالية (سند قبض وسند صرف) ---
    fun updateVoucherAmount(amount: String) {
        _uiState.update { it.copy(voucherAmountInput = amount) }
    }

    fun updateVoucherCategory(category: String) {
        _uiState.update { it.copy(voucherCategory = category) }
    }

    fun updateVoucherPaymentMethod(method: PaymentMethod) {
        _uiState.update { it.copy(voucherPaymentMethod = method) }
    }

    fun updateVoucherPaidTo(paidTo: String) {
        _uiState.update { it.copy(voucherPaidToInput = paidTo) }
    }

    fun updateVoucherNotes(notes: String) {
        _uiState.update { it.copy(voucherNotesInput = notes) }
    }

    fun executeVoucherTransaction() {
        val state = _uiState.value
        val amount = state.voucherAmountInput.toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            _uiState.update { it.copy(userFeedbackMessage = "الرجاء إدخال مبلغ صحيح للسند!", isError = true) }
            return
        }

        _uiState.update { it.copy(isVoucherSubmitting = true) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                db.withTransaction {
                    val timestamp = System.currentTimeMillis()

                    if (state.activeOperation == PosOperation.RECEIPT) {
                        // سند قبض نقدية من عميل (أو حساب عام)
                        val party = state.voucherParty
                        if (party == null) {
                            throw IllegalArgumentException("سند القبض يتطلب تحديد العميل لتسوية رصيده")
                        }

                        val count = voucherDao.countVouchers() + 1
                        val voucherNumber = "RCV-2026-%04d".format(count)

                        voucherDao.insertVoucher(
                            PaymentVoucherEntity(
                                voucherNumber = voucherNumber,
                                partyId = party.id,
                                amount = amount,
                                voucherType = VoucherType.RECEIPT,
                                paymentMethod = state.voucherPaymentMethod,
                                transactionRef = state.paymentTransactionRef,
                                receiptImagePath = state.paymentReceiptImagePath,
                                date = timestamp,
                                receivedBy = "كاشير 1",
                                notes = state.voucherNotesInput.ifEmpty { "سداد دين من العميل ${party.name}" }
                            )
                        )

                        // تقليل رصيد دين العميل
                        partyDao.updateBalance(party.id, -amount)

                        // تحديث حركة الصندوق والحسابات المالية
                        val accountDao = db.financialAccountDao()
                        if (state.voucherPaymentMethod.isPhysicalCash) {
                            val openShift = getOrCreateOpenShift(shiftDao)
                            val newCollections = openShift.totalCashCollections + amount
                            shiftDao.updateCollections(openShift.id, newCollections)

                            val cashAcc = accountDao.getAccountByCode("10101")
                            if (cashAcc != null) {
                                accountDao.updateBalance(cashAcc.id, amount)
                            }
                        } else if (state.voucherPaymentMethod.isElectronic) {
                            val targetAccId = state.selectedPaymentAccountId
                                ?: accountDao.getAllAccountsSync().firstOrNull { it.accountType != com.example.dokkani.data.local.entities.FinancialAccountType.CASH_DRAWER }?.id
                            if (targetAccId != null) {
                                accountDao.updateBalance(targetAccId, amount)
                            }
                        }

                        _uiState.update {
                            it.copy(
                                isVoucherSubmitting = false,
                                voucherAmountInput = "",
                                voucherNotesInput = "",
                                paymentTransactionRef = "",
                                paymentReceiptImagePath = null,
                                selectedPaymentAccountId = null,
                                activeOperation = PosOperation.RECEIPT,
                                userFeedbackMessage = "تم حفظ سند القبض بنجاح برقم: $voucherNumber وقيده في حساب العميل",
                                isError = false
                            )
                        }

                    } else if (state.activeOperation == PosOperation.EXPENSE) {
                        // سند صرف (سداد مورد أو مصروف عام)
                        val supplier = state.voucherParty

                        if (supplier != null && supplier.type != PartyType.CUSTOMER) {
                            // سداد دفعة لمورد
                            val count = voucherDao.countVouchers() + 1
                            val voucherNumber = "PAY-2026-%04d".format(count)

                            voucherDao.insertVoucher(
                                PaymentVoucherEntity(
                                    voucherNumber = voucherNumber,
                                    partyId = supplier.id,
                                    amount = amount,
                                    voucherType = VoucherType.PAYMENT,
                                    paymentMethod = state.voucherPaymentMethod,
                                    transactionRef = state.paymentTransactionRef,
                                    receiptImagePath = state.paymentReceiptImagePath,
                                    date = timestamp,
                                    receivedBy = "كاشير 1",
                                    notes = state.voucherNotesInput.ifEmpty { "دفعة مسددة للمورد ${supplier.name}" }
                                )
                            )

                            // تخفيض التزام المورد (بالموجب لأن رصيده سالب)
                            partyDao.updateBalance(supplier.id, amount)
                        } else {
                            // تسجيل مصروف تشغيلي عام
                            val count = expenseDao.countExpenses() + 1
                            val expenseNumber = "EXP-2026-%04d".format(count)

                            expenseDao.insertExpense(
                                ExpenseEntity(
                                    expenseNumber = expenseNumber,
                                    category = state.voucherCategory,
                                    amount = amount,
                                    paymentMethod = state.voucherPaymentMethod,
                                    date = timestamp,
                                    paidTo = state.voucherPaidToInput.ifEmpty { "جهة غير محددة" },
                                    notes = state.voucherNotesInput,
                                    recordedBy = "كاشير 1"
                                )
                            )
                        }

                        // تحديث حركة الصندوق والحسابات المالية للصرف
                        val accountDao = db.financialAccountDao()
                        if (state.voucherPaymentMethod.isPhysicalCash) {
                            val openShift = getOrCreateOpenShift(shiftDao)
                            if (supplier != null && supplier.type != PartyType.CUSTOMER) {
                                val newSupplierPayments = openShift.totalSupplierPayments + amount
                                shiftDao.updateSupplierPayments(openShift.id, newSupplierPayments)
                            } else {
                                val newExpenses = openShift.totalCashExpenses + amount
                                shiftDao.updateExpenses(openShift.id, newExpenses)
                            }

                            val cashAcc = accountDao.getAccountByCode("10101")
                            if (cashAcc != null) {
                                accountDao.updateBalance(cashAcc.id, -amount)
                            }
                        } else if (state.voucherPaymentMethod.isElectronic) {
                            val targetAccId = state.selectedPaymentAccountId
                                ?: accountDao.getAllAccountsSync().firstOrNull { it.accountType != com.example.dokkani.data.local.entities.FinancialAccountType.CASH_DRAWER }?.id
                            if (targetAccId != null) {
                                accountDao.updateBalance(targetAccId, -amount)
                            }
                        }

                        _uiState.update {
                            it.copy(
                                isVoucherSubmitting = false,
                                voucherAmountInput = "",
                                voucherPaidToInput = "",
                                voucherNotesInput = "",
                                paymentTransactionRef = "",
                                paymentReceiptImagePath = null,
                                selectedPaymentAccountId = null,
                                activeOperation = PosOperation.EXPENSE,
                                userFeedbackMessage = "تم حفظ سند الصرف بنجاح وخصم المبلغ من النقدية",
                                isError = false
                            )
                        }
                    }

                    loadTransactionHistory()
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isVoucherSubmitting = false,
                        userFeedbackMessage = "فشل تسجيل السند: ${e.localizedMessage}",
                        isError = true
                    )
                }
            }
        }
    }

    // --- حوارات سريعة وإجراءات ---
    fun toggleHistoryDialog(show: Boolean) {
        _uiState.update { it.copy(showHistoryDialog = show) }
    }

    fun setActiveMobileTab(tab: PosMobileTab) {
        _uiState.update { it.copy(activeMobileTab = tab, selectedTab = tab) }
    }

    /**
     * تهيئة وبدء فاتورة جديدة ونظيفة:
     * 1. تفريغ سلة المبيعات الحالية ومسح جميع الأصناف المؤقتة
     * 2. تصفير الحقول المالية بالكامل (المدفوع، الباقي، الخصم، طريقة الدفع الافتراضية نقداً)
     * 3. إغلاق نوافذ الإيصال والدفع (showReceiptDialog = false, showCheckoutDialog = false)
     * 4. التوجيه التلقائي المباشر إلى قائمة وتصنيفات الأصناف (Products / Catalog Tab) وليس السلة الفارغة
     * 5. تهيئة الفاتورة في الخلفية لتكون جاهزة لاستقبال الأصناف بمجرد النقر عليها
     */
    fun startNewInvoice() {
        _uiState.update { state ->
            val defaultParty = when (state.activeOperation) {
                PosOperation.SALE, PosOperation.SALE_RETURN -> null
                PosOperation.PURCHASE, PosOperation.PURCHASE_RETURN -> {
                    state.parties.firstOrNull { it.type == PartyType.SUPPLIER || it.type == PartyType.BOTH }
                }
                PosOperation.RECEIPT -> {
                    state.parties.firstOrNull { it.type == PartyType.CUSTOMER || it.type == PartyType.BOTH }
                }
                PosOperation.EXPENSE -> {
                    state.parties.firstOrNull { it.type == PartyType.SUPPLIER }
                }
            }

            state.copy(
                cartItems = emptyList(),
                cartSummary = recalculateSummary(emptyList(), 0.0),
                discount = 0.0,
                paidAmountInput = "",
                paymentMethod = PaymentMethod.CASH,
                paymentTransactionRef = "",
                paymentReceiptImagePath = null,
                selectedPaymentAccountId = null,
                selectedParty = defaultParty,
                voucherParty = defaultParty,
                voucherAmountInput = "",
                voucherPaidToInput = "",
                voucherNotesInput = "",
                originalInvoiceForReturn = null,
                returnOriginalInvoiceItems = emptyList(),
                showReceiptDialog = false,
                showCheckoutDialog = false,
                lastCheckoutResult = null,
                searchQuery = "",
                activeMobileTab = if (state.activeOperation.isVoucher) state.activeMobileTab else PosMobileTab.CATALOG,
                selectedTab = if (state.activeOperation.isVoucher) state.selectedTab else PosMobileTab.CATALOG,
                activeOperation = state.activeOperation,
                userFeedbackMessage = null,
                isError = false
            )
        }
    }

    fun dismissReceiptDialog() {
        startNewInvoice()
    }

    fun dismissFeedback() {
        _uiState.update { it.copy(userFeedbackMessage = null) }
    }

    fun showReceiptForInvoiceNumber(invoiceNumber: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val inv = invoiceDao.getInvoiceByInvoiceNumber(invoiceNumber) ?: return@launch
            val invoiceWithDetails = invoiceDao.getInvoiceWithDetailsById(inv.id) ?: return@launch
            val party = invoiceWithDetails.party
            val items = invoiceWithDetails.items
            val currentSettings = settingsDao.getSettingsSync() ?: SystemSettingsEntity()

            val invoiceTitle = when (inv.type) {
                InvoiceType.SALE -> "فاتورة بيع"
                InvoiceType.PURCHASE -> "فاتورة شراء"
                InvoiceType.SALE_RETURN -> "مردود بيع"
                InvoiceType.PURCHASE_RETURN -> "مردود شراء"
            }
            val isPurch = inv.type == InvoiceType.PURCHASE || inv.type == InvoiceType.PURCHASE_RETURN
            val partyLabel = if (isPurch) "المورد:" else "العميل:"
            val partyName = party?.name ?: (if (isPurch) "مورد نقدي" else "عميل نقدي")
            val isCredit = inv.paymentMethod == PaymentMethod.CREDIT

            val productsMap = productDao.getAllProductsSync().associateBy { it.id }
            val receiptItems = items.map { item ->
                val p = productsMap[item.productId]
                ReceiptItemData(
                    name = p?.name ?: "صنف #${item.productId}",
                    quantityFormatted = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else "%.2f".format(item.quantity),
                    unitPrice = item.unitSellingPrice,
                    totalPrice = item.totalPrice,
                    isWeighted = p?.isWeighted ?: false
                )
            }

            val creditAmt = if (inv.remainingAmount > 0.001) inv.remainingAmount else (if (isCredit) inv.total else 0.0)
            val (calculatedOldBal, calculatedNewBal) = if (party != null && (isCredit || creditAmt > 0.001)) {
                val curBal = party.currentBalance
                val oldRaw = when (inv.type) {
                    InvoiceType.SALE -> curBal - creditAmt
                    InvoiceType.PURCHASE -> curBal + creditAmt
                    InvoiceType.SALE_RETURN -> curBal + creditAmt
                    InvoiceType.PURCHASE_RETURN -> curBal - creditAmt
                }
                Pair(kotlin.math.abs(oldRaw), kotlin.math.abs(curBal))
            } else {
                val b = party?.currentBalance?.let { kotlin.math.abs(it) }
                Pair(b, b)
            }

            val paymentAccount = inv.paymentAccountId?.let { accId ->
                _uiState.value.financialAccounts.find { it.id == accId }
                    ?: db.financialAccountDao().getAccountById(accId)
            }

            val resolvedPaymentLabel = when {
                inv.paymentMethod == PaymentMethod.CASH -> PaymentMethod.CASH.labelArabic
                inv.paymentMethod == PaymentMethod.CREDIT -> PaymentMethod.CREDIT.labelArabic
                paymentAccount != null -> paymentAccount.name
                inv.paymentProviderName.isNotBlank() && inv.paymentProviderName != inv.paymentMethod.labelArabic -> inv.paymentProviderName
                else -> inv.paymentMethod.labelArabic
            }

            val receiptData = ReceiptPrintData(
                storeName = currentSettings.storeName.ifBlank { "دكاني" },
                storeAddress = currentSettings.storeAddress,
                storePhone = currentSettings.storePhone,
                taxNumber = currentSettings.taxNumber,
                invoiceTitle = invoiceTitle,
                invoiceNumber = inv.invoiceNumber,
                invoiceDateFormatted = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(inv.date)),
                cashierName = "كاشير 1",
                customerName = partyName,
                partyLabel = partyLabel,
                isCredit = isCredit,
                paymentMethodArabic = resolvedPaymentLabel,
                items = receiptItems,
                subtotal = inv.subtotal,
                discount = inv.discount,
                taxRatePercent = inv.taxRate,
                taxAmount = inv.taxAmount,
                total = inv.total,
                paidAmount = inv.paidAmount,
                remainingAmount = inv.remainingAmount,
                customerOldBalance = calculatedOldBal,
                customerNewBalance = calculatedNewBal,
                showPreviousBalance = currentSettings.showPreviousBalanceOnInvoice,
                currencySymbol = _uiState.value.currencySymbol,
                footerText = currentSettings.invoiceFooterText.ifBlank { "شكراً لتسوقكم!" }
            )

            val result = PosCheckoutResult(
                success = true,
                invoiceId = inv.id,
                invoiceNumber = inv.invoiceNumber,
                total = inv.total,
                paymentMethod = inv.paymentMethod,
                paidAmount = inv.paidAmount,
                changeAmount = 0.0,
                remainingCreditAmount = inv.remainingAmount,
                customerName = party?.name,
                customerNewBalance = party?.currentBalance,
                drawerKickTriggered = false,
                receiptData = receiptData,
                message = "$invoiceTitle: ${inv.invoiceNumber}"
            )

            _uiState.update {
                it.copy(
                    lastCheckoutResult = result,
                    showReceiptDialog = true
                )
            }
        }
    }

    fun addNewParty(name: String, phone: String, type: PartyType, limit: Double = 1000.0) {
        viewModelScope.launch(Dispatchers.IO) {
            val newId = partyDao.insertParty(
                PartyEntity(
                    name = name,
                    phone = phone,
                    type = type,
                    creditLimit = limit,
                    currentBalance = 0.0
                )
            )
            val created = partyDao.getPartyById(newId)
            _uiState.update {
                it.copy(
                    selectedParty = created,
                    voucherParty = created,
                    userFeedbackMessage = "تم إضافة ${type.labelArabic} بنجاح: $name",
                    isError = false
                )
            }
        }
    }

    // --- ربط المردودات بالفواتير الأصلية والبحث برقم الفاتورة ---
    fun openSelectInvoiceForReturnDialog() {
        _uiState.update { it.copy(showSelectInvoiceForReturnDialog = true, invoiceSearchQueryForReturn = "") }
        searchInvoicesForReturn("")
    }

    fun dismissSelectInvoiceForReturnDialog() {
        _uiState.update { it.copy(showSelectInvoiceForReturnDialog = false) }
    }

    fun updateInvoiceSearchQueryForReturn(query: String) {
        _uiState.update { it.copy(invoiceSearchQueryForReturn = query) }
        searchInvoicesForReturn(query)
    }

    fun searchInvoicesForReturn(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val isSaleReturn = _uiState.value.activeOperation == PosOperation.SALE_RETURN
            val targetType = if (isSaleReturn) InvoiceType.SALE else InvoiceType.PURCHASE
            val returnType = if (isSaleReturn) InvoiceType.SALE_RETURN else InvoiceType.PURCHASE_RETURN

            val candidates = if (query.isBlank()) {
                invoiceDao.searchInvoicesByType(targetType, "")
            } else {
                invoiceDao.searchInvoicesByType(targetType, query.trim())
            }

            val allReturnInvoices = invoiceDao.getAllInvoicesSync().filter { it.type == returnType }
            val allReturnItems = invoiceDao.getAllInvoiceItemsSync()
            val returnItemsByInvoiceId = allReturnItems.groupBy { it.invoiceId }

            // القاعدة 1: إخفاء تماماً أي فاتورة تم رد كافة بنودها وكمياتها بنسبة 100%
            val filteredInvoices = candidates.filter { candidate ->
                val originalItems = invoiceDao.getInvoiceItems(candidate.id)
                if (originalItems.isEmpty()) return@filter false

                val linkedReturnInvoices = allReturnInvoices.filter { ret ->
                    ret.transactionRef == candidate.invoiceNumber || ret.notes.contains(candidate.invoiceNumber)
                }

                val returnedQtyMap = mutableMapOf<Pair<Long, Long>, Double>()
                for (retInv in linkedReturnInvoices) {
                    val retItems = returnItemsByInvoiceId[retInv.id] ?: emptyList()
                    for (retItem in retItems) {
                        val key = Pair(retItem.productId, retItem.productUnitId)
                        returnedQtyMap[key] = (returnedQtyMap[key] ?: 0.0) + retItem.quantity
                    }
                }

                // يجب أن يتوفر على الأقل صنف واحد فيه كمية متبقية قابلة للرد > 0
                originalItems.any { origItem ->
                    val alreadyReturned = returnedQtyMap[Pair(origItem.productId, origItem.productUnitId)] ?: 0.0
                    (origItem.quantity - alreadyReturned) > 0.0001
                }
            }

            _uiState.update { it.copy(matchingInvoicesForReturn = filteredInvoices) }
        }
    }

    fun selectInvoiceForReturn(invoice: InvoiceEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val items = invoiceDao.getInvoiceItems(invoice.id)
            val party = invoice.partyId?.let { partyDao.getPartyById(it) }
            val products = productDao.getAllProductsSync()
            val units = productDao.getAllUnitsSync()

            val isPurchaseReturn = invoice.type == InvoiceType.PURCHASE || _uiState.value.activeOperation == PosOperation.PURCHASE_RETURN
            val returnType = if (isPurchaseReturn) InvoiceType.PURCHASE_RETURN else InvoiceType.SALE_RETURN

            val allReturnInvoices = invoiceDao.getAllInvoicesSync().filter { it.type == returnType }
            val allReturnItems = invoiceDao.getAllInvoiceItemsSync()
            val returnItemsByInvoiceId = allReturnItems.groupBy { it.invoiceId }

            val linkedReturnInvoices = allReturnInvoices.filter { ret ->
                ret.transactionRef == invoice.invoiceNumber || ret.notes.contains(invoice.invoiceNumber)
            }

            val returnedQtyMap = mutableMapOf<Pair<Long, Long>, Double>()
            for (retInv in linkedReturnInvoices) {
                val retItems = returnItemsByInvoiceId[retInv.id] ?: emptyList()
                for (retItem in retItems) {
                    val key = Pair(retItem.productId, retItem.productUnitId)
                    returnedQtyMap[key] = (returnedQtyMap[key] ?: 0.0) + retItem.quantity
                }
            }

            val cartList = mutableListOf<PosCartItem>()
            items.forEach { invItem ->
                val prod = products.find { it.id == invItem.productId }
                val unit = units.find { it.id == invItem.productUnitId }
                if (prod != null && unit != null) {
                    val alreadyReturned = returnedQtyMap[Pair(invItem.productId, invItem.productUnitId)] ?: 0.0
                    val remainingReturnableQty = (invItem.quantity - alreadyReturned).coerceAtLeast(0.0)

                    // القاعدة 1: عرض الأصناف ذات الكميات المتبقية الفعلية فقط
                    if (remainingReturnableQty > 0.0001) {
                        val itemPrice = if (isPurchaseReturn) invItem.unitCostPrice else invItem.unitSellingPrice
                        val itemCost = invItem.unitCostPrice

                        cartList.add(
                            PosCartItem(
                                cartItemId = UUID.randomUUID().toString(),
                                productId = prod.id,
                                productName = prod.name,
                                productCode = prod.code,
                                unitId = unit.id,
                                unitName = unit.unitName,
                                conversionFactor = invItem.unitConversionFactor,
                                unitPrice = itemPrice,
                                costPrice = itemCost,
                                quantity = remainingReturnableQty, // الكمية المتبقية الفعلية المتاحة للرد
                                discount = invItem.discount,
                                isWeighted = prod.isWeighted,
                                originalInvoiceQuantity = remainingReturnableQty, // السقف الأقصى المسموح به للرد
                                originalInvoiceCostPrice = invItem.unitCostPrice
                            )
                        )
                    }
                }
            }

            _uiState.update { state ->
                val newSummary = recalculateSummary(cartList, invoice.discount)
                val op = if (invoice.type == InvoiceType.PURCHASE) PosOperation.PURCHASE_RETURN else PosOperation.SALE_RETURN
                state.copy(
                    activeOperation = op,
                    originalInvoiceForReturn = invoice,
                    returnOriginalInvoiceItems = items,
                    selectedParty = party,
                    cartItems = cartList,
                    cartSummary = newSummary,
                    paymentMethod = invoice.paymentMethod,
                    showSelectInvoiceForReturnDialog = false,
                    userFeedbackMessage = "تم تعبئة الكميات المتبقية القابلة للرد من الفاتورة (${invoice.invoiceNumber}).",
                    isError = false
                )
            }
        }
    }

    fun clearOriginalInvoiceForReturn() {
        _uiState.update {
            it.copy(
                originalInvoiceForReturn = null,
                returnOriginalInvoiceItems = emptyList()
            )
        }
    }

    // --- إمكانية الحذف والتعديل لمدير النظام مع عكس القيود المحاسبية وحركات المخزون ---
    fun deleteTransactionRecord(record: PosTransactionRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                db.withTransaction {
                    when (record.operation) {
                        PosOperation.SALE, PosOperation.PURCHASE, PosOperation.SALE_RETURN, PosOperation.PURCHASE_RETURN -> {
                            val inv = invoiceDao.getInvoiceByInvoiceNumber(record.id)
                            if (inv != null) {
                                // 1. حذف حركات المخزون المعلقة بالفاتورة
                                stockMovementDao.deleteMovementsByReferenceNumber(inv.invoiceNumber)

                                // 2. عكس رصيد العميل أو المورد إذا كانت العملية آجلة
                                inv.partyId?.let { pId ->
                                    val party = partyDao.getPartyById(pId)
                                    if (party != null && inv.paymentMethod == PaymentMethod.CREDIT) {
                                        val newBal = when (inv.type) {
                                            InvoiceType.SALE -> party.currentBalance - inv.total
                                            InvoiceType.PURCHASE -> party.currentBalance + inv.total
                                            InvoiceType.SALE_RETURN -> party.currentBalance + inv.total
                                            InvoiceType.PURCHASE_RETURN -> party.currentBalance - inv.total
                                        }
                                        partyDao.updateParty(party.copy(currentBalance = newBal))
                                    }
                                }

                                // 3. عكس نقدية الصندوق في الشفت المفتوح
                                if (inv.paymentMethod == PaymentMethod.CASH) {
                                    val shifts = shiftDao.getAllShiftsSync()
                                    val currentShift = shifts.firstOrNull { it.status == "OPEN" } ?: shifts.firstOrNull()
                                    if (currentShift != null) {
                                        when (inv.type) {
                                            InvoiceType.SALE -> shiftDao.updateSales(
                                                currentShift.id,
                                                currentShift.totalCashSales - inv.total
                                            )
                                            InvoiceType.PURCHASE -> shiftDao.updateExpenses(
                                                currentShift.id,
                                                (currentShift.totalCashExpenses - inv.total).coerceAtLeast(0.0)
                                            )
                                            InvoiceType.SALE_RETURN -> shiftDao.updateSales(
                                                currentShift.id,
                                                currentShift.totalCashSales + inv.total
                                            )
                                            InvoiceType.PURCHASE_RETURN -> shiftDao.updateCollections(
                                                currentShift.id,
                                                (currentShift.totalCashCollections - inv.total).coerceAtLeast(0.0)
                                            )
                                        }
                                    }
                                }

                                // 4. حذف الفاتورة
                                invoiceDao.deleteInvoice(inv)
                            }
                        }
                        PosOperation.RECEIPT -> {
                            val v = voucherDao.getAllVouchersSync().find { it.voucherNumber == record.id }
                            if (v != null) {
                                val party = partyDao.getPartyById(v.partyId)
                                if (party != null) {
                                    val isSupplier = party.type == PartyType.SUPPLIER || v.isPayment
                                    val newBal = if (isSupplier) party.currentBalance - v.amount else party.currentBalance + v.amount
                                    partyDao.updateParty(party.copy(currentBalance = newBal))
                                }
                                if (v.paymentMethod == PaymentMethod.CASH) {
                                    val currentShift = shiftDao.getOpenShift() ?: shiftDao.getAllShiftsSync().firstOrNull()
                                    if (currentShift != null) {
                                        if (v.isPayment) {
                                            shiftDao.updateExpenses(
                                                currentShift.id,
                                                (currentShift.totalCashExpenses - v.amount).coerceAtLeast(0.0)
                                            )
                                        } else {
                                            shiftDao.updateCollections(
                                                currentShift.id,
                                                (currentShift.totalCashCollections - v.amount).coerceAtLeast(0.0)
                                            )
                                        }
                                    }
                                }
                                voucherDao.deleteVoucher(v)
                            }
                        }
                        PosOperation.EXPENSE -> {
                            val exp = expenseDao.getAllExpensesSync().find { it.expenseNumber == record.id }
                            if (exp != null) {
                                if (exp.paymentMethod == PaymentMethod.CASH) {
                                    val currentShift = shiftDao.getOpenShift() ?: shiftDao.getAllShiftsSync().firstOrNull()
                                    if (currentShift != null) {
                                        shiftDao.updateExpenses(
                                            currentShift.id,
                                            (currentShift.totalCashExpenses - exp.amount).coerceAtLeast(0.0)
                                        )
                                    }
                                }
                                expenseDao.deleteExpense(exp)
                            } else {
                                val v = voucherDao.getAllVouchersSync().find { it.voucherNumber == record.id }
                                if (v != null) {
                                    val party = partyDao.getPartyById(v.partyId)
                                    if (party != null) {
                                        partyDao.updateParty(party.copy(currentBalance = party.currentBalance - v.amount))
                                    }
                                    if (v.paymentMethod == PaymentMethod.CASH) {
                                        val currentShift = shiftDao.getOpenShift() ?: shiftDao.getAllShiftsSync().firstOrNull()
                                        if (currentShift != null) {
                                            shiftDao.updateExpenses(
                                                currentShift.id,
                                                (currentShift.totalCashExpenses - v.amount).coerceAtLeast(0.0)
                                            )
                                        }
                                    }
                                    voucherDao.deleteVoucher(v)
                                }
                            }
                        }
                    }
                }

                loadTransactionHistory()
                refreshStockQuantities()
                _uiState.update {
                    it.copy(
                        userFeedbackMessage = "تم حذف العملية (${record.id}) وعكس كافة القيود المحاسبية والمخزنية بنجاح.",
                        isError = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        userFeedbackMessage = "فشل حذف العملية: ${e.localizedMessage}",
                        isError = true
                    )
                }
            }
        }
    }

    fun requestDeleteTransactionRecord(record: PosTransactionRecord, userRole: UserRole = UserRole.ADMIN) {
        if (userRole != UserRole.ADMIN) {
            _uiState.update {
                it.copy(
                    userFeedbackMessage = "عفواً، صلاحية حذف العمليات والمستندات تقتصر على مدير النظام (Admin) فقط!",
                    isError = true
                )
            }
            return
        }
        _uiState.update {
            it.copy(
                showDeleteConfirmationDialog = true,
                recordPendingDelete = record
            )
        }
    }

    fun dismissDeleteConfirmationDialog() {
        _uiState.update {
            it.copy(
                showDeleteConfirmationDialog = false,
                recordPendingDelete = null
            )
        }
    }

    fun confirmDeletePendingRecord() {
        val record = _uiState.value.recordPendingDelete ?: return
        dismissDeleteConfirmationDialog()
        deleteTransactionRecord(record)
    }

    fun loadRecordForEditing(record: PosTransactionRecord, userRole: UserRole = UserRole.ADMIN) {
        if (userRole != UserRole.ADMIN) {
            _uiState.update {
                it.copy(
                    userFeedbackMessage = "عفواً، صلاحية تعديل العمليات والمستندات تقتصر على مدير النظام (Admin) فقط!",
                    isError = true
                )
            }
            return
        }
        openEditRecordDialog(record)
    }

    fun openEditRecordDialog(record: PosTransactionRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            if (record.operation.isVoucher) {
                _uiState.update {
                    it.copy(
                        showEditVoucherDialog = true,
                        editingVoucherRecord = record,
                        editVoucherAmount = record.amount.toString(),
                        editVoucherNotes = record.notes,
                        editVoucherPaymentMethod = record.paymentMethod,
                        editVoucherPaidTo = record.partyName
                    )
                }
            } else {
                val inv = invoiceDao.getInvoiceByInvoiceNumber(record.id)
                if (inv != null) {
                    _uiState.update {
                        it.copy(
                            showEditInvoiceDialog = true,
                            editingInvoice = inv,
                            editInvoiceNotes = inv.notes,
                            editInvoicePaymentMethod = inv.paymentMethod,
                            editInvoiceTotal = inv.total.toString(),
                            editInvoicePaidAmount = inv.paidAmount.toString(),
                            editInvoicePartyId = inv.partyId
                        )
                    }
                }
            }
        }
    }

    fun dismissEditDialogs() {
        _uiState.update {
            it.copy(
                showEditInvoiceDialog = false,
                editingInvoice = null,
                showEditVoucherDialog = false,
                editingVoucherRecord = null
            )
        }
    }

    fun saveEditedInvoice(
        notes: String,
        paymentMethod: PaymentMethod,
        newTotal: Double? = null,
        newPaidAmount: Double? = null,
        newPartyId: Long? = null
    ) {
        val inv = _uiState.value.editingInvoice ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                db.withTransaction {
                    val oldMethod = inv.paymentMethod
                    val oldTotal = inv.total
                    val oldPartyId = inv.partyId

                    val finalTotal = newTotal ?: inv.total
                    val finalPaid = newPaidAmount ?: inv.paidAmount
                    val finalRemaining = (finalTotal - finalPaid).coerceAtLeast(0.0)
                    val finalPartyId = newPartyId ?: inv.partyId

                    val updated = inv.copy(
                        notes = notes,
                        paymentMethod = paymentMethod,
                        total = finalTotal,
                        paidAmount = finalPaid,
                        remainingAmount = finalRemaining,
                        partyId = finalPartyId
                    )
                    invoiceDao.updateInvoice(updated)

                    // تعديل رصيد العميل/المورد إن وجد
                    if (oldPartyId != null && oldMethod == PaymentMethod.CREDIT) {
                        val party = partyDao.getPartyById(oldPartyId)
                        if (party != null) {
                            val reversedBal = if (inv.type == InvoiceType.SALE) party.currentBalance - oldTotal else party.currentBalance + oldTotal
                            partyDao.updateParty(party.copy(currentBalance = reversedBal))
                        }
                    }
                    if (finalPartyId != null && paymentMethod == PaymentMethod.CREDIT) {
                        val party = partyDao.getPartyById(finalPartyId)
                        if (party != null) {
                            val newBal = if (inv.type == InvoiceType.SALE) party.currentBalance + finalTotal else party.currentBalance - finalTotal
                            partyDao.updateParty(party.copy(currentBalance = newBal))
                        }
                    }
                }
                loadTransactionHistory()
                _uiState.update {
                    it.copy(
                        showEditInvoiceDialog = false,
                        editingInvoice = null,
                        userFeedbackMessage = "تم تحديث بيانات الفاتورة رقم (${inv.invoiceNumber}) بنجاح وانعكاس الأثر المالي.",
                        isError = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        userFeedbackMessage = "فشل تعديل الفاتورة: ${e.localizedMessage}",
                        isError = true
                    )
                }
            }
        }
    }

    fun saveEditedVoucher(
        notes: String,
        paymentMethod: PaymentMethod,
        newAmount: Double? = null,
        newPartyName: String? = null
    ) {
        val record = _uiState.value.editingVoucherRecord ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                db.withTransaction {
                    val openShift = getOrCreateOpenShift(shiftDao)
                    if (record.operation == PosOperation.RECEIPT) {
                        val v = voucherDao.getAllVouchersSync().find { it.voucherNumber == record.id }
                        if (v != null) {
                            val oldMethod = v.paymentMethod
                            val oldAmount = v.amount
                            val finalAmount = newAmount ?: v.amount
                            val updated = v.copy(notes = notes, paymentMethod = paymentMethod, amount = finalAmount)
                            voucherDao.updateVoucher(updated)

                            // ضبط حركة الصندوق الشفت
                            if (oldMethod == PaymentMethod.CASH && paymentMethod != PaymentMethod.CASH) {
                                val newColl = (openShift.totalCashCollections - oldAmount).coerceAtLeast(0.0)
                                shiftDao.updateCollections(openShift.id, newColl)
                            } else if (oldMethod != PaymentMethod.CASH && paymentMethod == PaymentMethod.CASH) {
                                val newColl = openShift.totalCashCollections + finalAmount
                                shiftDao.updateCollections(openShift.id, newColl)
                            } else if (oldMethod == PaymentMethod.CASH && paymentMethod == PaymentMethod.CASH) {
                                val diff = finalAmount - oldAmount
                                shiftDao.updateCollections(openShift.id, openShift.totalCashCollections + diff)
                            }

                            // ضبط رصيد العميل
                            val party = partyDao.getPartyById(v.partyId)
                            if (party != null && finalAmount != oldAmount) {
                                val diff = finalAmount - oldAmount
                                partyDao.updateParty(party.copy(currentBalance = party.currentBalance - diff))
                            }
                        }
                    } else if (record.operation == PosOperation.EXPENSE) {
                        val exp = expenseDao.getAllExpensesSync().find { it.expenseNumber == record.id }
                        if (exp != null) {
                            val oldMethod = exp.paymentMethod
                            val oldAmount = exp.amount
                            val finalAmount = newAmount ?: exp.amount
                            val finalPaidTo = newPartyName ?: exp.paidTo
                            val updated = exp.copy(notes = notes, paymentMethod = paymentMethod, amount = finalAmount, paidTo = finalPaidTo)
                            expenseDao.updateExpense(updated)

                            if (oldMethod == PaymentMethod.CASH && paymentMethod != PaymentMethod.CASH) {
                                val newExp = (openShift.totalCashExpenses - oldAmount).coerceAtLeast(0.0)
                                shiftDao.updateExpenses(openShift.id, newExp)
                            } else if (oldMethod != PaymentMethod.CASH && paymentMethod == PaymentMethod.CASH) {
                                val newExp = openShift.totalCashExpenses + finalAmount
                                shiftDao.updateExpenses(openShift.id, newExp)
                            } else if (oldMethod == PaymentMethod.CASH && paymentMethod == PaymentMethod.CASH) {
                                val diff = finalAmount - oldAmount
                                shiftDao.updateExpenses(openShift.id, openShift.totalCashExpenses + diff)
                            }
                        } else {
                            val v = voucherDao.getAllVouchersSync().find { it.voucherNumber == record.id }
                            if (v != null) {
                                val oldMethod = v.paymentMethod
                                val oldAmount = v.amount
                                val finalAmount = newAmount ?: v.amount
                                val updated = v.copy(notes = notes, paymentMethod = paymentMethod, amount = finalAmount)
                                voucherDao.updateVoucher(updated)

                                if (oldMethod == PaymentMethod.CASH && paymentMethod != PaymentMethod.CASH) {
                                    val newExp = (openShift.totalCashExpenses - oldAmount).coerceAtLeast(0.0)
                                    shiftDao.updateExpenses(openShift.id, newExp)
                                } else if (oldMethod != PaymentMethod.CASH && paymentMethod == PaymentMethod.CASH) {
                                    val newExp = openShift.totalCashExpenses + finalAmount
                                    shiftDao.updateExpenses(openShift.id, newExp)
                                } else if (oldMethod == PaymentMethod.CASH && paymentMethod == PaymentMethod.CASH) {
                                    val diff = finalAmount - oldAmount
                                    shiftDao.updateExpenses(openShift.id, openShift.totalCashExpenses + diff)
                                }

                                val party = partyDao.getPartyById(v.partyId)
                                if (party != null && finalAmount != oldAmount) {
                                    val diff = finalAmount - oldAmount
                                    partyDao.updateParty(party.copy(currentBalance = party.currentBalance + diff))
                                }
                            }
                        }
                    }
                }
                loadTransactionHistory()
                _uiState.update {
                    it.copy(
                        showEditVoucherDialog = false,
                        editingVoucherRecord = null,
                        userFeedbackMessage = "تم تحديث بيانات السند رقم (${record.id}) بنجاح وتحديث حركة الشفت.",
                        isError = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        userFeedbackMessage = "فشل تعديل السند: ${e.localizedMessage}",
                        isError = true
                    )
                }
            }
        }
    }

    // --- استعراض وتصفية السجل أسفل الشاشة ---
    fun setHistorySearchQuery(query: String) {
        _uiState.update { it.copy(historySearchQuery = query) }
    }

    fun setHistoryFilter(filter: String) {
        _uiState.update { it.copy(historyFilter = filter) }
    }

    fun toggleBottomHistoryExpanded() {
        _uiState.update { it.copy(isBottomHistoryExpanded = !it.isBottomHistoryExpanded) }
    }

    // --- إغلاق الشفت ومطابقة النقدية في الدرج (Shift Closing & Cash Audit) ---
    fun openShiftCloseDialog() {
        val shift = _uiState.value.currentShift
        val expected = _uiState.value.cashInDrawer
        viewModelScope.launch(Dispatchers.IO) {
            val recon = CashDrawerEngine.calculateReconciliation(
                openingCash = shift?.openingCash ?: 200.0,
                cashSales = listOf(shift?.totalCashSales ?: 0.0),
                cashCollections = listOf(shift?.totalCashCollections ?: 0.0),
                cashExpenses = listOf(shift?.totalCashExpenses ?: 0.0),
                supplierPayments = listOf(shift?.totalSupplierPayments ?: 0.0),
                cashPurchases = listOf(shift?.totalCashPurchases ?: 0.0),
                ownerDrawings = listOf(shift?.totalOwnerDrawings ?: 0.0),
                staffAdvances = listOf(shift?.totalStaffAdvances ?: 0.0),
                actualPhysicalCash = expected
            )
            _uiState.update {
                it.copy(
                    showShiftCloseDialog = true,
                    shiftActualCashInput = "%.2f".format(expected),
                    shiftCloseNotes = "",
                    shiftReconciliation = recon
                )
            }
        }
    }

    fun dismissShiftCloseDialog() {
        _uiState.update { it.copy(showShiftCloseDialog = false) }
    }

    fun updateShiftActualCashInput(input: String) {
        val shift = _uiState.value.currentShift
        val actual = input.toDoubleOrNull() ?: 0.0
        val recon = CashDrawerEngine.calculateReconciliation(
            openingCash = shift?.openingCash ?: 200.0,
            cashSales = listOf(shift?.totalCashSales ?: 0.0),
            cashCollections = listOf(shift?.totalCashCollections ?: 0.0),
            cashExpenses = listOf(shift?.totalCashExpenses ?: 0.0),
            supplierPayments = listOf(shift?.totalSupplierPayments ?: 0.0),
            cashPurchases = listOf(shift?.totalCashPurchases ?: 0.0),
            ownerDrawings = listOf(shift?.totalOwnerDrawings ?: 0.0),
            staffAdvances = listOf(shift?.totalStaffAdvances ?: 0.0),
            actualPhysicalCash = actual
        )
        _uiState.update {
            it.copy(
                shiftActualCashInput = input,
                shiftReconciliation = recon
            )
        }
    }

    fun updateShiftCloseNotes(notes: String) {
        _uiState.update { it.copy(shiftCloseNotes = notes) }
    }

    fun confirmCloseShift(onSuccess: (() -> Unit)? = null) {
        val currentShift = _uiState.value.currentShift ?: return
        val recon = _uiState.value.shiftReconciliation ?: return
        val notes = _uiState.value.shiftCloseNotes

        viewModelScope.launch(Dispatchers.IO) {
            try {
                db.withTransaction {
                    val now = System.currentTimeMillis()
                    val closedShift = currentShift.copy(
                        endTime = now,
                        expectedCashInDrawer = recon.expectedCashInDrawer,
                        actualPhysicalCash = recon.actualPhysicalCash,
                        cashDiscrepancy = recon.discrepancy,
                        status = "CLOSED",
                        notes = notes.ifBlank { "تم إغلاق الشفت والمطابقة: ${recon.discrepancyType.labelArabic}" }
                    )
                    shiftDao.updateShift(closedShift)

                    // فتح شفت جديد فوراً للكاشير التالي بالعهدة المتبقية
                    val nextShiftNumber = "SHF-${SimpleDateFormat("yyyyMMdd-HHmm", Locale.getDefault()).format(Date(now))}"
                    val nextShift = CashShiftEntity(
                        shiftNumber = nextShiftNumber,
                        cashierName = currentShift.cashierName,
                        startTime = now,
                        openingCash = recon.actualPhysicalCash, // العهدة الافتتاحية هي المبلغ الفعلي المستلم
                        totalCashSales = 0.0,
                        totalCashCollections = 0.0,
                        totalCashExpenses = 0.0,
                        expectedCashInDrawer = recon.actualPhysicalCash,
                        actualPhysicalCash = recon.actualPhysicalCash,
                        cashDiscrepancy = 0.0,
                        status = "OPEN",
                        notes = "تم فتح الشفت تلقائياً بعد إغلاق ${currentShift.shiftNumber}"
                    )
                    shiftDao.insertShift(nextShift)
                }

                _uiState.update {
                    it.copy(
                        showShiftCloseDialog = false,
                        userFeedbackMessage = "تم إغلاق الشفت بنجاح (${currentShift.shiftNumber})، ومطابقة النقدية: ${recon.discrepancyType.labelArabic}",
                        isError = false
                    )
                }
                onSuccess?.invoke()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        userFeedbackMessage = "فشل إغلاق الشفت: ${e.localizedMessage}",
                        isError = true
                    )
                }
            }
        }
    }
}
