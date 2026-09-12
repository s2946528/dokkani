package com.example.dokkani.ui.screens.purchase

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.example.dokkani.data.local.DokkaniDatabase
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
    val costPrice: Double,         // سعر الشراء والتكلفة للوحدة المحددة
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
    val discount: Double = 0.0,
    val notes: String = "",
    val isProcessing: Boolean = false,
    val lastSavedInvoiceNumber: String? = null,
    val wacUpdates: List<WacCalculationSummary> = emptyList(),
    val showSuccessDialog: Boolean = false,
    val showAddSupplierDialog: Boolean = false,
    val feedbackMessage: String? = null,
    val isError: Boolean = false
) {
    val subtotal: Double get() = items.sumOf { it.totalCost }
    val taxableAmount: Double get() = (subtotal - discount).coerceAtLeast(0.0)
    val taxAmount: Double get() = if (isTaxApplied) taxableAmount * 0.15 else 0.0
    val finalTotal: Double get() = taxableAmount + taxAmount
    val totalQuantity: Double get() = items.sumOf { it.quantity }
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
     * تنفيذ واعتماد فاتورة الشراء وتطبيق دالة WAC لحساب التكلفة الجديدة وتحديث المخزون
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

                    val isCredit = state.paymentMethod == PaymentMethod.CREDIT
                    val paid = if (isCredit) 0.0 else state.finalTotal
                    val remaining = if (isCredit) state.finalTotal else 0.0

                    // 1. إدخال فاتورة الشراء في جدول Invoices
                    val invoiceId = invoiceDao.insertInvoice(
                        InvoiceEntity(
                            invoiceNumber = invoiceNumber,
                            type = InvoiceType.PURCHASE,
                            partyId = state.selectedSupplier?.id,
                            date = timestamp,
                            currencyId = 1,
                            exchangeRate = 1.0,
                            subtotal = state.subtotal,
                            discount = state.discount,
                            taxRate = if (state.isTaxApplied) 0.15 else 0.0,
                            taxAmount = state.taxAmount,
                            total = state.finalTotal,
                            paidAmount = paid,
                            remainingAmount = remaining,
                            paymentMethod = state.paymentMethod,
                            status = InvoiceStatus.COMPLETED,
                            notes = "فاتورة شراء مورد رقم: ${state.supplierInvoiceNumber.ifEmpty { "غير محدد" }} - ${state.notes}"
                        )
                    )

                    // 2. بنود الفاتورة وحركات المخزون وحساب WAC
                    val itemsToInsert = mutableListOf<InvoiceItemEntity>()
                    val movementsToInsert = mutableListOf<StockMovementEntity>()
                    val wacSummaries = mutableListOf<WacCalculationSummary>()

                    state.items.forEach { item ->
                        itemsToInsert.add(
                            InvoiceItemEntity(
                                invoiceId = invoiceId,
                                productId = item.productId,
                                productUnitId = item.unitId,
                                quantity = item.quantity,
                                unitConversionFactor = item.conversionFactor,
                                unitCostPrice = item.costPrice,
                                unitSellingPrice = item.newSellingPrice ?: item.sellingPrice,
                                discount = 0.0,
                                taxRate = if (state.isTaxApplied) 0.15 else 0.0,
                                totalPrice = item.totalCost
                            )
                        )

                        val baseQtyPurchased = item.quantity * item.conversionFactor
                        val unitCostBasePurchased = item.costPrice / item.conversionFactor

                        // إضافة حركة مخزون PURCHASE_IN
                        movementsToInsert.add(
                            StockMovementEntity(
                                productId = item.productId,
                                productUnitId = item.unitId,
                                movementType = MovementType.PURCHASE_IN,
                                quantityBaseUnit = baseQtyPurchased,
                                remainingQuantityForFifo = baseQtyPurchased,
                                unitCostPriceBase = unitCostBasePurchased,
                                timestamp = timestamp,
                                referenceNumber = invoiceNumber
                            )
                        )

                        // 3. تطبيق دالة المتوسط المرجح WAC:
                        // قانون WAC = ((الكمية الحالية بالمخزن * التكلفة الحالية) + (الكمية المشتراة * تكلفة الشراء الجديدة)) / (الكمية الإجمالية الجديدة)
                        val existingStockBase = stockMovementDao.getTotalStockQuantity(item.productId)
                        val oldUnitCostBase = item.oldCostPrice / item.conversionFactor

                        val newWacCostBase = if (existingStockBase > 0.001) {
                            val currentInventoryValue = existingStockBase * oldUnitCostBase
                            val incomingValue = baseQtyPurchased * unitCostBasePurchased
                            (currentInventoryValue + incomingValue) / (existingStockBase + baseQtyPurchased)
                        } else {
                            unitCostBasePurchased
                        }

                        val unitEntity = productDao.getUnitById(item.unitId)
                        if (unitEntity != null) {
                            val updatedCostForThisUnit = newWacCostBase * unitEntity.conversionFactor
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
                                    newWacCost = updatedCostForThisUnit,
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
                        // بالسالب لأن دكاني يعتمد رصيد المورد بالسالب = له عندنا / دائن
                        partyDao.updateBalance(state.selectedSupplier.id, -state.finalTotal)
                    }

                    // 5. خصم المبلغ من الصندوق إذا كان الشراء نقداً
                    if (state.paymentMethod == PaymentMethod.CASH) {
                        val openShift = shiftDao.getOpenShift()
                        if (openShift != null) {
                            shiftDao.updateShift(
                                openShift.copy(
                                    totalCashExpenses = openShift.totalCashExpenses + state.finalTotal,
                                    expectedCashInDrawer = openShift.expectedCashInDrawer - state.finalTotal
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
}
