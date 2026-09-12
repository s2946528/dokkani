package com.example.dokkani.ui.screens.pos

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.example.dokkani.data.local.DokkaniDatabase
import com.example.dokkani.data.local.entities.CashShiftEntity
import com.example.dokkani.data.local.entities.ExpenseEntity
import com.example.dokkani.data.local.entities.InvoiceEntity
import com.example.dokkani.data.local.entities.InvoiceItemEntity
import com.example.dokkani.data.local.entities.InvoiceStatus
import com.example.dokkani.data.local.entities.InvoiceType
import com.example.dokkani.data.local.entities.MovementType
import com.example.dokkani.data.local.entities.PartyEntity
import com.example.dokkani.data.local.entities.PartyType
import com.example.dokkani.data.local.entities.PaymentMethod
import com.example.dokkani.data.local.entities.PaymentVoucherEntity
import com.example.dokkani.data.local.entities.ProductEntity
import com.example.dokkani.data.local.entities.ProductUnitEntity
import com.example.dokkani.data.local.entities.ProductWithUnits
import com.example.dokkani.data.local.entities.StockMovementEntity
import com.example.dokkani.domain.hardware.ReceiptItemData
import com.example.dokkani.domain.hardware.ReceiptPrintData
import com.example.dokkani.domain.pos.CartSummary
import com.example.dokkani.domain.pos.PosCartItem
import com.example.dokkani.domain.pos.PosCheckoutResult
import com.example.dokkani.domain.pos.PosOperation
import com.example.dokkani.domain.pos.PosTransactionRecord
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
import java.util.UUID

/**
 * حالة واجهة شاشة العمليات المالية والمبيعات (POS UI State)
 */
data class PosUiState(
    val activeOperation: PosOperation = PosOperation.SALE,
    val productsWithUnits: List<ProductWithUnits> = emptyList(),
    val parties: List<PartyEntity> = emptyList(),
    val selectedParty: PartyEntity? = null, // null للزبون النقدي المباشر في فواتير البيع
    val cartItems: List<PosCartItem> = emptyList(),
    val cartSummary: CartSummary = CartSummary(0, 0.0, 0.0, 0.0, 0.0, 15.0, 0.0, 0.0),
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val discount: Double = 0.0,
    val paidAmountInput: String = "",
    val searchQuery: String = "",
    val selectedCategory: String = "الكل",

    // الصندوق والشفت الحالي
    val shiftTotalSales: Double = 0.0,
    val cashInDrawer: Double = 0.0,
    val currentShift: CashShiftEntity? = null,

    // نموذج السندات المالية (قبض / صرف)
    val voucherParty: PartyEntity? = null,
    val voucherAmountInput: String = "",
    val voucherCategory: String = "نثريات ومستلزمات",
    val voucherPaymentMethod: PaymentMethod = PaymentMethod.CASH,
    val voucherPaidToInput: String = "",
    val voucherNotesInput: String = "",
    val isVoucherSubmitting: Boolean = false,

    // النوافذ والملاحظات
    val isProcessingCheckout: Boolean = false,
    val lastCheckoutResult: PosCheckoutResult? = null,
    val showReceiptDialog: Boolean = false,
    val showHistoryDialog: Boolean = false,
    val showCheckoutDialog: Boolean = false,
    val showAddPartyDialog: Boolean = false,
    val userFeedbackMessage: String? = null,
    val isError: Boolean = false,

    // سجل العمليات
    val transactionRecords: List<PosTransactionRecord> = emptyList()
)

class PosViewModel(application: Application) : AndroidViewModel(application) {

    private val db = DokkaniDatabase.getDatabase(application, viewModelScope)
    private val productDao = db.productDao()
    private val partyDao = db.partyDao()
    private val invoiceDao = db.invoiceDao()
    private val stockMovementDao = db.stockMovementDao()
    private val voucherDao = db.paymentVoucherDao()
    private val expenseDao = db.expenseDao()
    private val shiftDao = db.cashShiftDao()
    private val settingsDao = db.systemSettingsDao()

    private val _uiState = MutableStateFlow(PosUiState())
    val uiState: StateFlow<PosUiState> = _uiState.asStateFlow()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    init {
        observeData()
    }

    private fun observeData() {
        // 1. مراقبة المنتجات والوحدات
        viewModelScope.launch(Dispatchers.IO) {
            productDao.getProductsWithUnits().collectLatest { products ->
                _uiState.update { it.copy(productsWithUnits = products) }
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

        // 4. تحديث سجل المعاملات الحديثة
        loadTransactionHistory()
    }

    fun loadTransactionHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            val invoices = invoiceDao.getAllInvoicesSync()
            val vouchers = voucherDao.getAllVouchers().let {
                // استرجاع السندات
                val list = mutableListOf<PaymentVoucherEntity>()
                val job = viewModelScope.launch {
                    it.collectLatest { vList -> list.clear(); list.addAll(vList) }
                }
                job.cancel()
                list
            }
            val expenses = expenseDao.getAllExpenses().let {
                val list = mutableListOf<ExpenseEntity>()
                val job = viewModelScope.launch {
                    it.collectLatest { eList -> list.clear(); list.addAll(eList) }
                }
                job.cancel()
                list
            }

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
                        notes = inv.notes
                    )
                )
            }

            vouchers.forEach { v ->
                val party = partyDao.getPartyById(v.partyId)
                records.add(
                    PosTransactionRecord(
                        id = v.voucherNumber,
                        partyName = party?.name ?: "حساب عميل",
                        date = dateFormat.format(Date(v.date)),
                        amount = v.amount,
                        status = "تم السداد",
                        operation = PosOperation.RECEIPT,
                        paymentMethod = v.paymentMethod,
                        notes = v.notes
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
                        notes = exp.notes
                    )
                )
            }

            _uiState.update { it.copy(transactionRecords = records) }
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
        _uiState.update { it.copy(selectedParty = party) }
    }

    fun selectVoucherParty(party: PartyEntity?) {
        _uiState.update { it.copy(voucherParty = party) }
    }

    // --- إدارة السلة (للعمليات الأربع: بيع، شراء، مردود بيع، مردود شراء) ---
    fun addToCart(product: ProductEntity, unit: ProductUnitEntity, quantity: Double = 1.0) {
        _uiState.update { state ->
            val existingIndex = state.cartItems.indexOfFirst {
                it.productId == product.id && it.unitId == unit.id
            }

            val updatedItems = state.cartItems.toMutableList()
            if (existingIndex >= 0) {
                val existing = updatedItems[existingIndex]
                updatedItems[existingIndex] = existing.copy(quantity = existing.quantity + quantity)
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
                        isWeighted = product.isWeighted
                    )
                )
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

    fun updateCartQuantity(cartItemId: String, newQty: Double) {
        _uiState.update { state ->
            val updatedItems = if (newQty <= 0.001) {
                state.cartItems.filterNot { it.cartItemId == cartItemId }
            } else {
                state.cartItems.map {
                    if (it.cartItemId == cartItemId) it.copy(quantity = newQty) else it
                }
            }
            state.copy(
                cartItems = updatedItems,
                cartSummary = recalculateSummary(updatedItems, state.discount)
            )
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
                discount = 0.0
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

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setSelectedCategory(category: String) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    private fun recalculateSummary(items: List<PosCartItem>, discount: Double): CartSummary {
        val totalQty = items.sumOf { it.quantity }
        val subtotal = items.sumOf { it.quantity * it.unitPrice }
        val itemsDiscount = items.sumOf { it.discount }
        val totalDiscount = itemsDiscount + discount
        val taxable = (subtotal - totalDiscount).coerceAtLeast(0.0)
        val taxRate = 15.0
        val taxAmount = taxable * (taxRate / 100.0)
        val finalTotal = taxable + taxAmount

        return CartSummary(
            itemsCount = items.size,
            totalQuantity = totalQty,
            subtotal = subtotal,
            discount = totalDiscount,
            taxableAmount = taxable,
            taxRatePercent = taxRate,
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
                            taxRate = 0.15,
                            taxAmount = state.cartSummary.taxAmount,
                            total = state.cartSummary.finalTotal,
                            paidAmount = paid,
                            remainingAmount = remaining,
                            paymentMethod = state.paymentMethod,
                            status = InvoiceStatus.COMPLETED,
                            notes = "عملية نقطة البيع (${state.activeOperation.titleArabic})"
                        )
                    )

                    // 2. حفظ بنود الفاتورة وحركات المخزون وحساب التكلفة
                    val itemsToInsert = mutableListOf<InvoiceItemEntity>()
                    val movementsToInsert = mutableListOf<StockMovementEntity>()

                    state.cartItems.forEach { item ->
                        itemsToInsert.add(
                            InvoiceItemEntity(
                                invoiceId = invoiceId,
                                productId = item.productId,
                                productUnitId = item.unitId,
                                quantity = item.quantity,
                                unitConversionFactor = item.conversionFactor,
                                unitCostPrice = item.costPrice,
                                unitSellingPrice = item.unitPrice,
                                discount = item.discount,
                                taxRate = 0.15,
                                totalPrice = item.totalPrice
                            )
                        )

                        // حركة المخزون المحاسبية
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
                                productId = item.productId,
                                productUnitId = item.unitId,
                                movementType = movementType,
                                quantityBaseUnit = qtyBase,
                                remainingQuantityForFifo = if (movementType == MovementType.PURCHASE_IN) item.quantity * item.conversionFactor else 0.0,
                                unitCostPriceBase = item.costPrice / item.conversionFactor,
                                timestamp = timestamp,
                                referenceNumber = invoiceNumber
                            )
                        )

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

                    // 3. تحديث مديونية العميل أو المورد إذا كانت العملية آجلة
                    if (isCredit && state.selectedParty != null) {
                        val partyId = state.selectedParty.id
                        val amountDiff = when (state.activeOperation) {
                            PosOperation.SALE -> remaining // زيادة دين العميل (مدين لنا)
                            PosOperation.SALE_RETURN -> -state.cartSummary.finalTotal // إنقاص دين العميل
                            PosOperation.PURCHASE -> -remaining // زيادة التزامنا للمورد (دائن لنا بالسالب)
                            PosOperation.PURCHASE_RETURN -> state.cartSummary.finalTotal // إنقاص التزام المورد
                            else -> 0.0
                        }
                        partyDao.updateBalance(partyId, amountDiff)
                    }

                    // 4. تحديث صندوق الكاشير إذا كانت نقداً
                    if (state.paymentMethod == PaymentMethod.CASH) {
                        val openShift = shiftDao.getOpenShift()
                        if (openShift != null) {
                            when (state.activeOperation) {
                                PosOperation.SALE -> {
                                    shiftDao.updateShift(
                                        openShift.copy(
                                            totalCashSales = openShift.totalCashSales + state.cartSummary.finalTotal,
                                            expectedCashInDrawer = openShift.expectedCashInDrawer + state.cartSummary.finalTotal
                                        )
                                    )
                                }
                                PosOperation.SALE_RETURN -> {
                                    shiftDao.updateShift(
                                        openShift.copy(
                                            totalCashSales = openShift.totalCashSales - state.cartSummary.finalTotal,
                                            expectedCashInDrawer = openShift.expectedCashInDrawer - state.cartSummary.finalTotal
                                        )
                                    )
                                }
                                PosOperation.PURCHASE -> {
                                    shiftDao.updateShift(
                                        openShift.copy(
                                            totalCashExpenses = openShift.totalCashExpenses + state.cartSummary.finalTotal,
                                            expectedCashInDrawer = openShift.expectedCashInDrawer - state.cartSummary.finalTotal
                                        )
                                    )
                                }
                                PosOperation.PURCHASE_RETURN -> {
                                    shiftDao.updateShift(
                                        openShift.copy(
                                            totalCashCollections = openShift.totalCashCollections + state.cartSummary.finalTotal,
                                            expectedCashInDrawer = openShift.expectedCashInDrawer + state.cartSummary.finalTotal
                                        )
                                    )
                                }
                                else -> {}
                            }
                        }
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

                    val partyOldBal = state.selectedParty?.currentBalance
                    val partyNewBal = if (isCredit && state.selectedParty != null) {
                        val diff = when (state.activeOperation) {
                            PosOperation.SALE -> remaining
                            PosOperation.SALE_RETURN -> -state.cartSummary.finalTotal
                            PosOperation.PURCHASE -> -remaining
                            PosOperation.PURCHASE_RETURN -> state.cartSummary.finalTotal
                            else -> 0.0
                        }
                        (partyOldBal ?: 0.0) + diff
                    } else null

                    val receiptData = ReceiptPrintData(
                        storeName = "دكاني - تموينات ومخضار السعادة",
                        storePhone = "0500000000",
                        taxNumber = "300123456700003",
                        invoiceNumber = invoiceNumber,
                        invoiceDateFormatted = dateFormat.format(Date(timestamp)),
                        cashierName = "كاشير 1",
                        customerName = state.selectedParty?.name ?: "عميل نقدي",
                        paymentMethodArabic = state.paymentMethod.labelArabic,
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
                        currencySymbol = "ر.س"
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
                            cartItems = emptyList(),
                            cartSummary = recalculateSummary(emptyList(), 0.0),
                            lastCheckoutResult = result,
                            showReceiptDialog = true,
                            userFeedbackMessage = "تم حفظ الفاتورة بنجاح: $invoiceNumber",
                            isError = false
                        )
                    }

                    loadTransactionHistory()
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
                                paymentMethod = state.voucherPaymentMethod,
                                date = timestamp,
                                receivedBy = "كاشير 1",
                                notes = state.voucherNotesInput.ifEmpty { "سداد دين من العميل ${party.name}" }
                            )
                        )

                        // تقليل رصيد دين العميل
                        partyDao.updateBalance(party.id, -amount)

                        // تحديث حركة الصندوق إذا كان نقداً
                        if (state.voucherPaymentMethod == PaymentMethod.CASH) {
                            val openShift = shiftDao.getOpenShift()
                            if (openShift != null) {
                                shiftDao.updateShift(
                                    openShift.copy(
                                        totalCashCollections = openShift.totalCashCollections + amount,
                                        expectedCashInDrawer = openShift.expectedCashInDrawer + amount
                                    )
                                )
                            }
                        }

                        _uiState.update {
                            it.copy(
                                isVoucherSubmitting = false,
                                voucherAmountInput = "",
                                voucherNotesInput = "",
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
                                    paymentMethod = state.voucherPaymentMethod,
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

                        // إنقاص رصيد النقدية في الدرج إذا كان نقداً
                        if (state.voucherPaymentMethod == PaymentMethod.CASH) {
                            val openShift = shiftDao.getOpenShift()
                            if (openShift != null) {
                                shiftDao.updateShift(
                                    openShift.copy(
                                        totalCashExpenses = openShift.totalCashExpenses + amount,
                                        expectedCashInDrawer = openShift.expectedCashInDrawer - amount
                                    )
                                )
                            }
                        }

                        _uiState.update {
                            it.copy(
                                isVoucherSubmitting = false,
                                voucherAmountInput = "",
                                voucherPaidToInput = "",
                                voucherNotesInput = "",
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

    fun dismissReceiptDialog() {
        _uiState.update { it.copy(showReceiptDialog = false) }
    }

    fun dismissFeedback() {
        _uiState.update { it.copy(userFeedbackMessage = null) }
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
}
