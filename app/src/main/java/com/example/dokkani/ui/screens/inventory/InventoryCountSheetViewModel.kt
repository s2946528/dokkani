package com.example.dokkani.ui.screens.inventory

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dokkani.data.local.DokkaniDatabase
import com.example.dokkani.data.local.entities.CostCenterEntity
import com.example.dokkani.data.local.entities.InventoryAuditSheetEntity
import com.example.dokkani.data.local.entities.MovementType
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
import org.json.JSONArray
import org.json.JSONObject

import com.example.dokkani.ui.components.isItemMatchQuery

enum class AuditItemStatus(val labelArabic: String) {
    MATCHING("مطابق ✓"),
    SHORTAGE("عجز ⚠"),
    SURPLUS("زيادة ▲")
}

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

    val varianceQuantity: Double
        get() = actualEndingQty - bookStockQuantity

    val shortageQuantity: Double
        get() = (bookStockQuantity - actualEndingQty).coerceAtLeast(0.0)

    val surplusQuantity: Double
        get() = (actualEndingQty - bookStockQuantity).coerceAtLeast(0.0)

    val totalShortageSellingValue: Double
        get() = shortageQuantity * unitSellingPrice

    val totalShortageCostValue: Double
        get() = shortageQuantity * unitCostPrice

    val totalSurplusSellingValue: Double
        get() = surplusQuantity * unitSellingPrice

    val totalSurplusCostValue: Double
        get() = surplusQuantity * unitCostPrice

    val status: AuditItemStatus
        get() = when {
            kotlin.math.abs(varianceQuantity) < 0.001 -> AuditItemStatus.MATCHING
            varianceQuantity < 0 -> AuditItemStatus.SHORTAGE
            else -> AuditItemStatus.SURPLUS
        }
}

/**
 * حالة شاشة الجرد الدوري وقائمة المطابقة والتبويبات
 */
data class InventoryCountSheetUiState(
    val selectedTabIndex: Int = 0, // 0 = الجرد الحالي, 1 = أرشيف السندات السابقة
    val currentSheetId: Long? = null,
    val voucherNumber: String = "AUDIT-${(System.currentTimeMillis() % 1000000).toString().padStart(6, '0')}",
    val voucherDate: Long = System.currentTimeMillis(),
    val targetStoreName: String = "المخزن الرئيسي - الفرع 1",
    val voucherStatus: String = InventoryAuditSheetEntity.STATUS_DRAFT, // "DRAFT" أو "POSTED"
    val isApproved: Boolean = false,

    val auditItems: List<InventoryAuditItemState> = emptyList(),
    val filteredAuditItems: List<InventoryAuditItemState> = emptyList(),
    val categories: List<String> = emptyList(),
    val selectedCategoryFilter: String? = null,
    val searchQuery: String = "",
    val costCenters: List<CostCenterEntity> = emptyList(),
    val selectedCostCenterId: Long = 1,

    // بيانات أرشيف السندات السابقة
    val archivedSheets: List<InventoryAuditSheetEntity> = emptyList(),
    val filteredArchivedSheets: List<InventoryAuditSheetEntity> = emptyList(),
    val archiveSearchQuery: String = "",
    val archiveStatusFilter: String? = null, // null = الكل, "DRAFT", "POSTED"
    val previewingArchivedSheet: InventoryAuditSheetEntity? = null,
    val showDeleteConfirmDialog: Boolean = false,
    val sheetToDelete: InventoryAuditSheetEntity? = null,

    // خيارات الطباعة السرية والجرود الميدانية والماسح الضوئي
    val isBlindCountPrintingEnabled: Boolean = false, // إخفاء الكميات الدفترية أثناء الطباعة
    val showPrintPreviewDialog: Boolean = false,
    val showCameraScannerDialog: Boolean = false,

    // اعتماد الجرد والترحيل للبيع بالقيمة
    val isCommittingAudit: Boolean = false,
    val showCommitSuccessDialog: Boolean = false,
    val committedShortageRecordsCount: Int = 0,
    val committedTotalShortageValue: Double = 0.0,

    val currencySymbol: String = "ر.ي",
    val isRefreshing: Boolean = false,
    val feedbackMessage: String? = null,
    val isErrorFeedback: Boolean = false
) {
    val isCurrentSheetDraft: Boolean get() = voucherStatus == InventoryAuditSheetEntity.STATUS_DRAFT
    val isCurrentSheetPosted: Boolean get() = voucherStatus == InventoryAuditSheetEntity.STATUS_POSTED
}

class InventoryCountSheetViewModel(application: Application) : AndroidViewModel(application) {

    private val db = DokkaniDatabase.getDatabase(application, viewModelScope)
    private val productDao = db.productDao()
    private val currencyDao = db.currencyDao()
    private val costCenterDao = db.costCenterDao()
    private val shortageDao = db.shortageSettlementDao()
    private val stockMovementDao = db.stockMovementDao()
    private val wastageDao = db.productWastageDao()
    private val auditSheetDao = db.inventoryAuditSheetDao()

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

        // 3. مراقبة السندات والأرشيف من قاعدة البيانات
        viewModelScope.launch(Dispatchers.IO) {
            auditSheetDao.getAllAuditSheets().collectLatest { sheets ->
                _uiState.update { state ->
                    val filtered = filterArchivedSheets(sheets, state.archiveSearchQuery, state.archiveStatusFilter)
                    state.copy(
                        archivedSheets = sheets,
                        filteredArchivedSheets = filtered
                    )
                }
            }
        }

        // 4. تحميل جميع المنتجات وحساب رصيدها الدفتري والتلف المعزول
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

    /**
     * تحديث وجلب قائمة الأصناف والرصيد الدفتري من قاعدة البيانات عند السحب للأسفل (Swipe-To-Refresh)
     */
    fun refreshInventoryItems() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isRefreshing = true) }
            try {
                val productsWithUnits = productDao.getProductsWithUnitsSync()
                val items = productsWithUnits.map { pw ->
                    val prod = pw.product
                    val baseUnit = pw.units.firstOrNull { it.isBaseUnit } ?: pw.units.firstOrNull()
                    val unitName = baseUnit?.unitName ?: "قطعة"
                    val costPrice = baseUnit?.costPrice ?: 0.0
                    val sellingPrice = baseUnit?.sellingPrice ?: 0.0

                    val rawStock = stockMovementDao.getTotalStockQuantity(prod.id)
                    val wasteQty = wastageDao.getTotalWasteQuantityForProduct(prod.id) ?: 0.0
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
                        categories = categories,
                        isRefreshing = false,
                        feedbackMessage = "تم تحديث الأصناف ورصيد المخزون بنجاح",
                        isErrorFeedback = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        feedbackMessage = "حدث خطأ أثناء تحديث الأصناف: ${e.localizedMessage}",
                        isErrorFeedback = true
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
            val matchesSearch = (query.isBlank() || isItemMatchQuery(
                itemName = item.productName,
                itemCode = "",
                barcode = item.barcode,
                category = item.categoryName,
                searchQuery = query
            ))
            matchesCategory && matchesSearch
        }
    }

    private fun filterArchivedSheets(
        sheets: List<InventoryAuditSheetEntity>,
        query: String,
        statusFilter: String?
    ): List<InventoryAuditSheetEntity> {
        return sheets.filter { sheet ->
            val matchesStatus = (statusFilter == null || sheet.status == statusFilter)
            val matchesSearch = (query.isBlank() ||
                    sheet.voucherNumber.contains(query, ignoreCase = true) ||
                    sheet.targetStoreName.contains(query, ignoreCase = true) ||
                    sheet.costCenterName.contains(query, ignoreCase = true) ||
                    sheet.notes.contains(query, ignoreCase = true))
            matchesStatus && matchesSearch
        }
    }

    fun selectTab(tabIndex: Int) {
        _uiState.update { it.copy(selectedTabIndex = tabIndex) }
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
        val state = _uiState.value
        if (state.isCurrentSheetPosted) {
            _uiState.update {
                it.copy(
                    feedbackMessage = "عذراً! هذا السند معتمد ومقفل محاسبياً ولا يمكن تعديل كمياته مباشرة.",
                    isErrorFeedback = true
                )
            }
            return
        }

        _uiState.update { st ->
            val updatedItems = st.auditItems.map { item ->
                if (item.productId == productId) {
                    item.copy(actualEndingQtyInput = newQtyInput)
                } else {
                    item
                }
            }
            val filtered = filterItems(updatedItems, st.searchQuery, st.selectedCategoryFilter)
            st.copy(auditItems = updatedItems, filteredAuditItems = filtered)
        }
    }

    fun setArchiveSearchQuery(query: String) {
        _uiState.update { state ->
            val filtered = filterArchivedSheets(state.archivedSheets, query, state.archiveStatusFilter)
            state.copy(archiveSearchQuery = query, filteredArchivedSheets = filtered)
        }
    }

    fun setArchiveStatusFilter(statusFilter: String?) {
        _uiState.update { state ->
            val filtered = filterArchivedSheets(state.archivedSheets, state.archiveSearchQuery, statusFilter)
            state.copy(archiveStatusFilter = statusFilter, filteredArchivedSheets = filtered)
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

    fun setPreviewingArchivedSheet(sheet: InventoryAuditSheetEntity?) {
        _uiState.update { it.copy(previewingArchivedSheet = sheet) }
    }

    fun dismissCommitSuccessDialog() {
        _uiState.update { it.copy(showCommitSuccessDialog = false) }
    }

    fun setShowCameraScannerDialog(show: Boolean) {
        _uiState.update { it.copy(showCameraScannerDialog = show) }
    }

    fun dismissFeedback() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }

    /**
     * حفظ سند الجرد الحالي كمسودة (Draft) في قاعدة البيانات
     */
    fun saveDraftSheet() {
        viewModelScope.launch(Dispatchers.IO) {
            val state = _uiState.value
            val costCenter = costCenterDao.getCostCenterById(state.selectedCostCenterId)
            val ccName = costCenter?.centerName ?: "مركز التكلفة العام"

            val itemsCount = state.auditItems.size
            val matchingCount = state.auditItems.count { it.status == AuditItemStatus.MATCHING }
            val shortageCount = state.auditItems.count { it.status == AuditItemStatus.SHORTAGE }
            val surplusCount = state.auditItems.count { it.status == AuditItemStatus.SURPLUS }

            val totalShortageSelling = state.auditItems.sumOf { it.totalShortageSellingValue }
            val totalShortageCost = state.auditItems.sumOf { it.totalShortageCostValue }
            val totalSurplusSelling = state.auditItems.sumOf { it.totalSurplusSellingValue }
            val totalSurplusCost = state.auditItems.sumOf { it.totalSurplusCostValue }

            val itemsJson = encodeAuditItemsToJson(state.auditItems)

            val sheetEntity = InventoryAuditSheetEntity(
                id = state.currentSheetId ?: 0L,
                voucherNumber = state.voucherNumber,
                date = state.voucherDate,
                targetStoreName = state.targetStoreName,
                costCenterId = state.selectedCostCenterId,
                costCenterName = ccName,
                status = InventoryAuditSheetEntity.STATUS_DRAFT,
                itemsCount = itemsCount,
                matchingCount = matchingCount,
                shortageCount = shortageCount,
                surplusCount = surplusCount,
                totalShortageSellingValue = totalShortageSelling,
                totalShortageCostValue = totalShortageCost,
                totalSurplusSellingValue = totalSurplusSelling,
                totalSurplusCostValue = totalSurplusCost,
                itemsDataJson = itemsJson,
                notes = "مسودة جرد تحفظ مؤقتاً قبل الاعتماد النهائي",
                updatedAt = System.currentTimeMillis()
            )

            val newId = auditSheetDao.insertAuditSheet(sheetEntity)

            _uiState.update {
                it.copy(
                    currentSheetId = if (state.currentSheetId == null || state.currentSheetId == 0L) newId else state.currentSheetId,
                    voucherStatus = InventoryAuditSheetEntity.STATUS_DRAFT,
                    feedbackMessage = "تم حفظ مسودة الجرد رقم #${state.voucherNumber} بنجاح في قاعدة البيانات",
                    isErrorFeedback = false
                )
            }
        }
    }

    /**
     * تحميل أو استعراض سند جرد سابق من الأرشيف وإتاحة التعديل إذا كان مسودة
     */
    fun loadSheetToActiveAudit(sheet: InventoryAuditSheetEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val decodedItems = decodeAuditItemsFromJson(sheet.itemsDataJson)

            _uiState.update { state ->
                val activeItems = if (decodedItems.isNotEmpty()) {
                    decodedItems
                } else {
                    state.auditItems
                }
                val filtered = filterItems(activeItems, state.searchQuery, state.selectedCategoryFilter)

                state.copy(
                    currentSheetId = sheet.id,
                    voucherNumber = sheet.voucherNumber,
                    voucherDate = sheet.date,
                    voucherStatus = sheet.status,
                    isApproved = sheet.isPosted,
                    selectedCostCenterId = sheet.costCenterId,
                    auditItems = activeItems,
                    filteredAuditItems = filtered,
                    selectedTabIndex = 0, // الانقال لتبويب الجرد الحالي
                    feedbackMessage = if (sheet.isPosted)
                        "تم عرض السند المعتمد رقم #${sheet.voucherNumber} (ملاحظة: السند معتمد ومقفل ضد التعديل)"
                    else
                        "تم تحميل مسودة الجرد رقم #${sheet.voucherNumber} لاستكمال العد الفعلي والتعديل",
                    isErrorFeedback = false
                )
            }
        }
    }

    /**
     * بدء سند جرد جديد بالكامل
     */
    fun startNewStocktakingSheet() {
        val newVoucherNumber = "AUDIT-${(System.currentTimeMillis() % 1000000).toString().padStart(6, '0')}"
        _uiState.update { state ->
            val resetItems = state.auditItems.map { item ->
                item.copy(actualEndingQtyInput = String.format(java.util.Locale.US, "%.1f", item.bookStockQuantity))
            }
            val filtered = filterItems(resetItems, state.searchQuery, state.selectedCategoryFilter)

            state.copy(
                currentSheetId = null,
                voucherNumber = newVoucherNumber,
                voucherDate = System.currentTimeMillis(),
                voucherStatus = InventoryAuditSheetEntity.STATUS_DRAFT,
                isApproved = false,
                auditItems = resetItems,
                filteredAuditItems = filtered,
                selectedTabIndex = 0,
                feedbackMessage = "تم فتح سند جرد جديد برقم #$newVoucherNumber",
                isErrorFeedback = false
            )
        }
    }

    /**
     * طلب حذف سند جرد (مع التحقق الصارم من الصلاحيات والنزاهة)
     */
    fun requestDeleteSheet(sheet: InventoryAuditSheetEntity) {
        if (sheet.isPosted) {
            _uiState.update {
                it.copy(
                    feedbackMessage = "عذراً! لا يمكن حذف سند الجرد المعتمد (#${sheet.voucherNumber}) حمايةً للنزاهة المالية والمحاسبية. يمكنك إنشاء سند تسوية جديد عند الحاجة.",
                    isErrorFeedback = true
                )
            }
            return
        }

        // إذا كان مسودة، يُسمح بالحذف بعد التأكيد
        _uiState.update {
            it.copy(
                showDeleteConfirmDialog = true,
                sheetToDelete = sheet
            )
        }
    }

    fun dismissDeleteConfirmDialog() {
        _uiState.update {
            it.copy(
                showDeleteConfirmDialog = false,
                sheetToDelete = null
            )
        }
    }

    /**
     * تأكيد حذف مسودة الجرد
     */
    fun confirmDeleteSheet() {
        val sheet = _uiState.value.sheetToDelete ?: return
        if (sheet.isPosted) return

        viewModelScope.launch(Dispatchers.IO) {
            auditSheetDao.deleteDraftSheetById(sheet.id)

            _uiState.update { state ->
                val isCurrentDeleted = (state.currentSheetId == sheet.id)
                state.copy(
                    showDeleteConfirmDialog = false,
                    sheetToDelete = null,
                    currentSheetId = if (isCurrentDeleted) null else state.currentSheetId,
                    voucherStatus = if (isCurrentDeleted) InventoryAuditSheetEntity.STATUS_DRAFT else state.voucherStatus,
                    feedbackMessage = "تم حذف مسودة الجرد رقم #${sheet.voucherNumber} بنجاح من قاعدة البيانات",
                    isErrorFeedback = false
                )
            }

            if (_uiState.value.currentSheetId == null) {
                startNewStocktakingSheet()
            }
        }
    }

    /**
     * اعتماد الجرد الدوري وترحيل العجز المخزني النظيف للبيع بالقيمة (صلاحية مدير النظام فقط)
     * - يقوم بتحديث الأرصدة الفعلية في النظام عبر تسجيل حركات تسوية مخزنية.
     * - يحسب العجز الناتج حصرياً (الدفتري - الفعلي) لكل صنف بدون التلف المعزول سابقاً.
     * - يرحل السجلات إلى جدول `shortage_settlements` لتظهر في شاشة البيع بالقيمة كإيراد مستحق.
     * - يحفظ السند في حالة "POSTED" ويقفله ضد التعديل أو الحذف المباشر.
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

        val state = _uiState.value
        if (state.isCurrentSheetPosted) {
            _uiState.update {
                it.copy(
                    feedbackMessage = "هذا السند معتمد ومقفل سابقاً من قبل مدير النظام ولا يمكن إعادة ترحيله مرة أخرى.",
                    isErrorFeedback = true
                )
            }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isCommittingAudit = true) }

            val currentState = _uiState.value
            val costCenter = costCenterDao.getCostCenterById(currentState.selectedCostCenterId)
            val ccName = costCenter?.centerName ?: "مركز التكلفة العام"

            val shortageItemsToInsert = mutableListOf<ShortageSettlementEntity>()
            val stockAdjustmentsToInsert = mutableListOf<StockMovementEntity>()
            val auditTimestamp = System.currentTimeMillis()

            var totalShortageValue = 0.0

            for (item in currentState.auditItems) {
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
                            referenceNumber = "ADJ-${currentState.voucherNumber}",
                            notes = "تسوية جرد دوري معتمد (${currentState.voucherNumber}) - تحديث الرصيد الفعلي إلى ($actualQty ${item.unitName})"
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
                            costCenterId = currentState.selectedCostCenterId,
                            costCenterName = ccName,
                            bookQuantity = bookQty,
                            actualQuantity = actualQty,
                            shortageQuantity = shortageQty,
                            unitCost = item.unitCostPrice,
                            unitSellingPrice = item.unitSellingPrice,
                            totalShortageCost = totalCost,
                            totalValueSalesAmount = totalRevenue,
                            status = ShortageSettlementEntity.STATUS_PENDING,
                            notes = "عجز مخزني ناتج عن اعتماد الجرد الدوري #${currentState.voucherNumber}",
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

            // حفظ وإغلاق سند الجرد كـ POSTED معتمد ومقفل
            val itemsJson = encodeAuditItemsToJson(currentState.auditItems)
            val sheetEntity = InventoryAuditSheetEntity(
                id = currentState.currentSheetId ?: 0L,
                voucherNumber = currentState.voucherNumber,
                date = currentState.voucherDate,
                targetStoreName = currentState.targetStoreName,
                costCenterId = currentState.selectedCostCenterId,
                costCenterName = ccName,
                status = InventoryAuditSheetEntity.STATUS_POSTED,
                itemsCount = currentState.auditItems.size,
                matchingCount = currentState.auditItems.count { it.status == AuditItemStatus.MATCHING },
                shortageCount = currentState.auditItems.count { it.status == AuditItemStatus.SHORTAGE },
                surplusCount = currentState.auditItems.count { it.status == AuditItemStatus.SURPLUS },
                totalShortageSellingValue = currentState.auditItems.sumOf { it.totalShortageSellingValue },
                totalShortageCostValue = currentState.auditItems.sumOf { it.totalShortageCostValue },
                totalSurplusSellingValue = currentState.auditItems.sumOf { it.totalSurplusSellingValue },
                totalSurplusCostValue = currentState.auditItems.sumOf { it.totalSurplusCostValue },
                itemsDataJson = itemsJson,
                notes = "سند جرد معتمد ومقفل محاسبياً",
                updatedAt = System.currentTimeMillis()
            )

            val savedId = auditSheetDao.insertAuditSheet(sheetEntity)

            _uiState.update {
                it.copy(
                    currentSheetId = savedId,
                    voucherStatus = InventoryAuditSheetEntity.STATUS_POSTED,
                    isApproved = true,
                    isCommittingAudit = false,
                    showCommitSuccessDialog = true,
                    committedShortageRecordsCount = shortageItemsToInsert.size,
                    committedTotalShortageValue = totalShortageValue,
                    feedbackMessage = "تم اعتماد وترحيل الجرد الدوري بنجاح. السند الان معتمد ومقفل محاسبياً وضد التعديل أو الحذف المباشر.",
                    isErrorFeedback = false
                )
            }
        }
    }

    private fun encodeAuditItemsToJson(items: List<InventoryAuditItemState>): String {
        val array = JSONArray()
        for (item in items) {
            val obj = JSONObject()
            obj.put("productId", item.productId)
            obj.put("productName", item.productName)
            obj.put("categoryName", item.categoryName)
            obj.put("unitName", item.unitName)
            obj.put("bookStockQuantity", item.bookStockQuantity)
            obj.put("isolatedDailyWasteQty", item.isolatedDailyWasteQty)
            obj.put("actualEndingQtyInput", item.actualEndingQtyInput)
            obj.put("unitCostPrice", item.unitCostPrice)
            obj.put("unitSellingPrice", item.unitSellingPrice)
            obj.put("barcode", item.barcode)
            array.put(obj)
        }
        return array.toString()
    }

    private fun decodeAuditItemsFromJson(jsonStr: String): List<InventoryAuditItemState> {
        if (jsonStr.isBlank()) return emptyList()
        val list = mutableListOf<InventoryAuditItemState>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    InventoryAuditItemState(
                        productId = obj.optLong("productId"),
                        productName = obj.optString("productName"),
                        categoryName = obj.optString("categoryName"),
                        unitName = obj.optString("unitName", "قطعة"),
                        bookStockQuantity = obj.optDouble("bookStockQuantity", 0.0),
                        isolatedDailyWasteQty = obj.optDouble("isolatedDailyWasteQty", 0.0),
                        actualEndingQtyInput = obj.optString("actualEndingQtyInput", "0.0"),
                        unitCostPrice = obj.optDouble("unitCostPrice", 0.0),
                        unitSellingPrice = obj.optDouble("unitSellingPrice", 0.0),
                        barcode = obj.optString("barcode", "")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}
