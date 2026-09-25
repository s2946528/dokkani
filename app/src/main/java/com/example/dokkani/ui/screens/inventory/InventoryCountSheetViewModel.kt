package com.example.dokkani.ui.screens.inventory

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dokkani.data.local.DokkaniDatabase
import com.example.dokkani.data.local.entities.CostCenterEntity
import com.example.dokkani.data.local.entities.MovementType
import com.example.dokkani.data.local.entities.ProductWithUnits
import com.example.dokkani.data.local.entities.ShortageSettlementEntity
import com.example.dokkani.data.local.entities.StockMovementEntity
import com.example.dokkani.data.local.entities.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * عنصر صنف في قائمة الجرد الدوري والمطابقة المخزنية
 */
data class InventoryAuditItemState(
    val productId: Long,
    val productName: String,
    val categoryName: String,
    val unitName: String = "قطعة",
    val bookStockQuantity: Double = 0.0,      // المخزون الدفتري المتبقي (بعد استبعاد التلف اليومي المعزول)
    val isolatedDailyWasteQty: Double = 0.0,   // التلف اليومي المعزول والمحسوب سابقا كمصروف
    val actualEndingQtyInput: String = "0.0", // بضاعة آخر المدة الفعلي (إدخال المستخدم)
    val unitCostPrice: Double = 0.0,           // سعر التكلفة
    val unitSellingPrice: Double = 0.0,        // سعر البيع
    val barcode: String = ""
) {
    val actualEndingQty: Double
        get() = actualEndingQtyInput.toDoubleOrNull() ?: 0.0

    val shortageQuantity: Double
        get() = (bookStockQuantity - actualEndingQty).coerceAtLeast(0.0)

    val totalShortageSellingValue: Double
        get() = shortageQuantity * unitSellingPrice

    val totalShortageCostValue: Double
        get() = shortageQuantity * unitCostPrice
}

/**
 * حالة شاشة الجرد الدوري وقائمة المطابقة للطباعة والترحيل
 */
data class InventoryCountSheetUiState(
    val auditItems: List<InventoryAuditItemState> = emptyList(),
    val filteredAuditItems: List<InventoryAuditItemState> = emptyList(),
    val categories: List<String> = emptyList(),
    val selectedCategoryFilter: String? = null,
    val searchQuery: String = "",
    val costCenters: List<CostCenterEntity> = emptyList(),
    val selectedCostCenterId: Long = 1,

    // خيارات الطباعة السرية والجرود الميدانية
    val isBlindCountPrintingEnabled: Boolean = false, // إخفاء الكميات الدفترية أثناء الطباعة
    val showPrintPreviewDialog: Boolean = false,

    // اعتماد الجرد والترحيل للبيع بالقيمة
    val isCommittingAudit: Boolean = false,
    val showCommitSuccessDialog: Boolean = false,
    val committedShortageRecordsCount: Int = 0,
    val committedTotalShortageValue: Double = 0.0,

    val currencySymbol: String = "ر.ي",
    val feedbackMessage: String? = null,
    val isErrorFeedback: Boolean = false
)

class InventoryCountSheetViewModel(application: Application) : AndroidViewModel(application) {

    private val db = DokkaniDatabase.getDatabase(application, viewModelScope)
    private val productDao = db.productDao()
    private val currencyDao = db.currencyDao()
    private val costCenterDao = db.costCenterDao()
    private val shortageDao = db.shortageSettlementDao()
    private val stockMovementDao = db.stockMovementDao()
    private val wastageDao = db.productWastageDao()

    private val _uiState = MutableStateFlow(InventoryCountSheetUiState())
    val uiState: StateFlow<InventoryCountSheetUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        // 1. مراقبة العملة الأساسية
        viewModelScope.launch(Dispatchers.IO) {
            currencyDao.getBaseCurrencyFlow().collectLatest { base ->
                if (base != null) {
                    _uiState.update { it.copy(currencySymbol = base.symbol) }
                }
            }
        }

        // 2. مراقبة مراكز التكلفة
        viewModelScope.launch(Dispatchers.IO) {
            costCenterDao.getAllCostCenters().collectLatest { centers ->
                _uiState.update { state ->
                    val defaultCc = centers.firstOrNull { it.isGeneral }?.centerId ?: centers.firstOrNull()?.centerId ?: 1L
                    state.copy(
                        costCenters = centers,
                        selectedCostCenterId = if (state.selectedCostCenterId == 1L) defaultCc else state.selectedCostCenterId
                    )
                }
            }
        }

        // 3. تحميل جميع المنتجات وحساب رصيدها الدفتري والتلف المعزول
        viewModelScope.launch(Dispatchers.IO) {
            productDao.getProductsWithUnits().collectLatest { productsWithUnits ->
                val items = productsWithUnits.map { pw ->
                    val prod = pw.product
                    val baseUnit = pw.units.firstOrNull { it.isBaseUnit } ?: pw.units.firstOrNull()
                    val unitName = baseUnit?.unitName ?: "قطعة"
                    val costPrice = baseUnit?.costPrice ?: 0.0
                    val sellingPrice = baseUnit?.sellingPrice ?: 0.0

                    // المخزون الدفتري الإجمالي من حركات المخزون
                    val rawStock = stockMovementDao.getTotalStockQuantity(prod.id)
                    // التلف اليومي المعزول المسجل سابقاً كمصروف مستقل
                    val wasteQty = wastageDao.getTotalWasteQuantityForProduct(prod.id) ?: 0.0

                    // المخزون الدفتري المتبقي المستهدف للجرد المطابق (بعد استبعاد التلف اليومي المعزول)
                    val bookStockNet = (rawStock - wasteQty).coerceAtLeast(0.0)

                    InventoryAuditItemState(
                        productId = prod.id,
                        productName = prod.name,
                        categoryName = prod.category,
                        unitName = unitName,
                        bookStockQuantity = bookStockNet,
                        isolatedDailyWasteQty = wasteQty,
                        actualEndingQtyInput = String.format(java.util.Locale.US, "%.1f", bookStockNet),
                        unitCostPrice = costPrice,
                        unitSellingPrice = sellingPrice,
                        barcode = prod.code
                    )
                }

                val categories = items.map { it.categoryName }.distinct().sorted()

                _uiState.update { state ->
                    val filtered = filterItems(items, state.searchQuery, state.selectedCategoryFilter)
                    state.copy(
                        auditItems = items,
                        filteredAuditItems = filtered,
                        categories = categories
                    )
                }
            }
        }
    }

    private fun filterItems(
        items: List<InventoryAuditItemState>,
        query: String,
        category: String?
    ): List<InventoryAuditItemState> {
        return items.filter { item ->
            val matchesCategory = (category == null || item.categoryName == category)
            val matchesSearch = (query.isBlank() ||
                    item.productName.contains(query, ignoreCase = true) ||
                    item.barcode.contains(query, ignoreCase = true) ||
                    item.categoryName.contains(query, ignoreCase = true))
            matchesCategory && matchesSearch
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { state ->
            val filtered = filterItems(state.auditItems, query, state.selectedCategoryFilter)
            state.copy(searchQuery = query, filteredAuditItems = filtered)
        }
    }

    fun setCategoryFilter(category: String?) {
        _uiState.update { state ->
            val filtered = filterItems(state.auditItems, state.searchQuery, category)
            state.copy(selectedCategoryFilter = category, filteredAuditItems = filtered)
        }
    }

    fun setSelectedCostCenter(costCenterId: Long) {
        _uiState.update { it.copy(selectedCostCenterId = costCenterId) }
    }

    fun updateActualEndingQty(productId: Long, newQtyInput: String) {
        _uiState.update { state ->
            val updatedItems = state.auditItems.map { item ->
                if (item.productId == productId) {
                    item.copy(actualEndingQtyInput = newQtyInput)
                } else {
                    item
                }
            }
            val filtered = filterItems(updatedItems, state.searchQuery, state.selectedCategoryFilter)
            state.copy(auditItems = updatedItems, filteredAuditItems = filtered)
        }
    }

    fun toggleBlindCountPrinting(enabled: Boolean) {
        _uiState.update { it.copy(isBlindCountPrintingEnabled = enabled) }
    }

    fun openPrintPreviewDialog() {
        _uiState.update { it.copy(showPrintPreviewDialog = true) }
    }

    fun dismissPrintPreviewDialog() {
        _uiState.update { it.copy(showPrintPreviewDialog = false) }
    }

    fun dismissCommitSuccessDialog() {
        _uiState.update { it.copy(showCommitSuccessDialog = false) }
    }

    fun dismissFeedback() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }

    /**
     * اعتماد الجرد الدوري وترحيل العجز المخزني النظيف للبيع بالقيمة (صلاحية مدير النظام فقط)
     * - يقوم بتحديث الأرصدة الفعلية في النظام عبر تسجيل حركات تسوية مخزنية.
     * - يحسب العجز الناتج حصرياً (الدفتري - الفعلي) لكل صنف بدون التلف المعزول سابقاً.
     * - يرحل السجلات إلى جدول `shortage_settlements` لتظهر في شاشة البيع بالقيمة كإيراد مستحق.
     */
    fun commitAuditAndTransferShortageToValueSelling(currentUserRole: UserRole) {
        if (currentUserRole != UserRole.ADMIN) {
            _uiState.update {
                it.copy(
                    feedbackMessage = "عفواً! عملية اعتماد الجرد وترحيل العجز محصورة حصرياً بمدير النظام (Admin)",
                    isErrorFeedback = true
                )
            }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isCommittingAudit = true) }

            val state = _uiState.value
            val costCenter = costCenterDao.getCostCenterById(state.selectedCostCenterId)
            val ccName = costCenter?.centerName ?: "مركز التكلفة العام"

            val shortageItemsToInsert = mutableListOf<ShortageSettlementEntity>()
            val stockAdjustmentsToInsert = mutableListOf<StockMovementEntity>()
            val auditTimestamp = System.currentTimeMillis()

            var totalShortageValue = 0.0

            for (item in state.auditItems) {
                val bookQty = item.bookStockQuantity
                val actualQty = item.actualEndingQty

                // 1. تسوية المخزون الفعلي في النظام عبر حركة Adjustment
                val stockDiff = actualQty - bookQty
                if (Math.abs(stockDiff) > 0.001) {
                    stockAdjustmentsToInsert.add(
                        StockMovementEntity(
                            productId = item.productId,
                            movementType = MovementType.INVENTORY_ADJUSTMENT,
                            quantityBaseUnit = stockDiff,
                            remainingQuantityForFifo = if (stockDiff > 0) stockDiff else 0.0,
                            unitCostPriceBase = item.unitCostPrice,
                            timestamp = auditTimestamp,
                            referenceNumber = "AUDIT-ADJ-${auditTimestamp % 100000}",
                            notes = "تسوية جرد دوري معتمد - تحديث الرصيد الفعلي إلى ($actualQty ${item.unitName})"
                        )
                    )
                }

                // 2. إذا كان هناك عجز مخزني (الدفتري أكبر من الفعلي)، يتم ترحيله للبيع بالقيمة
                if (bookQty > actualQty) {
                    val shortageQty = bookQty - actualQty
                    val totalCost = shortageQty * item.unitCostPrice
                    val totalRevenue = shortageQty * item.unitSellingPrice
                    totalShortageValue += totalRevenue

                    shortageItemsToInsert.add(
                        ShortageSettlementEntity(
                            auditId = auditTimestamp,
                            productId = item.productId,
                            productName = item.productName,
                            costCenterId = state.selectedCostCenterId,
                            costCenterName = ccName,
                            bookQuantity = bookQty,
                            actualQuantity = actualQty,
                            shortageQuantity = shortageQty,
                            unitCost = item.unitCostPrice,
                            unitSellingPrice = item.unitSellingPrice,
                            totalShortageCost = totalCost,
                            totalValueSalesAmount = totalRevenue,
                            status = ShortageSettlementEntity.STATUS_PENDING,
                            notes = "عجز مخزني ناتج عن اعتماد الجرد الدوري الميداني",
                            createdAt = auditTimestamp
                        )
                    )
                }
            }

            // تنفيذ الإدراجات والتحديثات بقاعدة البيانات
            if (stockAdjustmentsToInsert.isNotEmpty()) {
                stockMovementDao.insertMovements(stockAdjustmentsToInsert)
            }

            if (shortageItemsToInsert.isNotEmpty()) {
                shortageDao.insertShortages(shortageItemsToInsert)
            }

            _uiState.update {
                it.copy(
                    isCommittingAudit = false,
                    showCommitSuccessDialog = true,
                    committedShortageRecordsCount = shortageItemsToInsert.size,
                    committedTotalShortageValue = totalShortageValue,
                    feedbackMessage = "تم اعتماد الجرد الدوري بنجاح وترحيل (${shortageItemsToInsert.size}) قيود عجز بقيمة إيراد بيع (%.2f %s) إلى شاشة البيع بالقيمة".format(
                        totalShortageValue,
                        it.currencySymbol
                    ),
                    isErrorFeedback = false
                )
            }
        }
    }
}
