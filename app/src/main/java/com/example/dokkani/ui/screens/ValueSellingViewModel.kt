package com.example.dokkani.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dokkani.data.local.DokkaniDatabase
import com.example.dokkani.data.local.entities.CostValuationMethod
import com.example.dokkani.data.local.entities.ProductWithUnits
import com.example.dokkani.data.local.entities.StockGroupAuditEntity
import com.example.dokkani.data.local.entities.StockGroupEntity
import com.example.dokkani.data.local.entities.StockGroupItemEntity
import com.example.dokkani.data.local.entities.StockGroupWithDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import com.example.dokkani.data.local.entities.UserRole

/**
 * تفاصيل الجرد المخزني للصنف التابع للمجموعة
 */
data class GroupItemAuditDetail(
    val itemId: Long,
    val productId: Long?,
    val productName: String,
    val unitName: String = "كجم",
    val currencySymbol: String = "ر.ي",
    val currentStockQty: Double = 0.0,
    val autoWasteQty: Double = 0.0,
    val endingActualQtyInput: String = "0.0"
)

/**
 * حالة واجهة بيع بالقيمة وإدارة المجموعات والجرد
 */
data class ValueSellingUiState(
    val activeTab: Int = 0, // 0: إدارة المجموعات، 1: الجرد الدوري و COGS
    val groupsWithDetails: List<StockGroupWithDetails> = emptyList(),
    val selectedGroupDetails: StockGroupWithDetails? = null,
    val selectedGroupId: Long? = null,
    val productsWithUnits: List<ProductWithUnits> = emptyList(),
    val groupItemsAuditDetails: List<GroupItemAuditDetail> = emptyList(),

    // نافذة إضافة/تعديل مجموعة
    val showGroupDialog: Boolean = false,
    val editingGroupId: Long? = null,
    val groupNameInput: String = "",
    val groupCodeInput: String = "",
    val groupCategoryInput: String = "خضار وفواكه",
    val groupCostMethod: CostValuationMethod = CostValuationMethod.WAC,
    val groupDescriptionInput: String = "",

    // إضافة/حذف أصناف داخل مجموعة
    val selectedProductForGroup: Long? = null,
    val groupItemRatioInput: String = "1.0",

    // مدخلات الجرد الدوري ومحرك COGS للمجموعة
    val auditBeginningQtyInput: String = "10.0",
    val auditBeginningCostInput: String = "100.0",
    val auditNewPurchasesQtyInput: String = "20.0",
    val auditNewPurchasesCostInput: String = "200.0",
    val auditEndingActualQtyInput: String = "5.0",
    val auditWasteQtyInput: String = "2.0",
    val auditRecordedSalesRevenueInput: String = "350.0",
    val auditCostMethod: CostValuationMethod = CostValuationMethod.WAC,
    val isSavingAudit: Boolean = false,

    // حسابات COGS المحسوبة حياً
    val cogsCalculatedQty: Double = 0.0,
    val cogsCalculatedCost: Double = 0.0,
    val netProfitCalculated: Double = 0.0,
    val averageCostPerKg: Double = 0.0,

    // رسائل الملاحظات والحماية البرمجية
    val feedbackMessage: String? = null,
    val isErrorFeedback: Boolean = false,
    val showDeleteValidationDialog: Boolean = false,
    val deleteValidationMessage: String = "",
    val pendingDeleteGroup: StockGroupEntity? = null,
    val isDeleteAllowed: Boolean = false,
    val currencySymbol: String = "ر.ي"
)

class ValueSellingViewModel(application: Application) : AndroidViewModel(application) {

    private val db = DokkaniDatabase.getDatabase(application, viewModelScope)
    private val groupDao = db.stockGroupDao()
    private val productDao = db.productDao()
    private val currencyDao = db.currencyDao()

    private val _uiState = MutableStateFlow(ValueSellingUiState())
    val uiState: StateFlow<ValueSellingUiState> = _uiState.asStateFlow()

    init {
        observeData()
        seedDefaultGroupsIfEmpty()
    }

    private fun seedDefaultGroupsIfEmpty() {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = groupDao.getAllGroupsWithDetailsSync()
            if (existing.isEmpty()) {
                val g1 = StockGroupEntity(name = "خضار مشكل (بالقيمة)", code = "GRP-VEG-01", category = "خضار وفواكه", description = "مجموعة خضار مشكل للبيع بالقيمة المباشرة")
                val g1Id = groupDao.insertGroup(g1)
                groupDao.insertGroupItems(
                    listOf(
                        StockGroupItemEntity(groupId = g1Id, productName = "طماطم بلدي", defaultRatio = 1.0),
                        StockGroupItemEntity(groupId = g1Id, productName = "خيار بلدي", defaultRatio = 1.0),
                        StockGroupItemEntity(groupId = g1Id, productName = "كوسا", defaultRatio = 1.0)
                    )
                )

                val g2 = StockGroupEntity(name = "مكسرات مشكلة (بالقيمة)", code = "GRP-NUT-01", category = "حلويات وتسالي", description = "مجموعة مكسرات فاخرة مشكلة")
                val g2Id = groupDao.insertGroup(g2)
                groupDao.insertGroupItems(
                    listOf(
                        StockGroupItemEntity(groupId = g2Id, productName = "فستق حلبي", defaultRatio = 1.2),
                        StockGroupItemEntity(groupId = g2Id, productName = "كاجو محمص", defaultRatio = 1.1),
                        StockGroupItemEntity(groupId = g2Id, productName = "لوز أمريكي", defaultRatio = 1.0)
                    )
                )

                val g3 = StockGroupEntity(name = "أجبان ومقبلات مشكلة", code = "GRP-CHS-01", category = "ألبان وأجبان", description = "تشكيلة أجبان ومخللات بالقيمة")
                val g3Id = groupDao.insertGroup(g3)
                groupDao.insertGroupItems(
                    listOf(
                        StockGroupItemEntity(groupId = g3Id, productName = "جبن فيتا", defaultRatio = 1.0),
                        StockGroupItemEntity(groupId = g3Id, productName = "زيتون أخضر محشي", defaultRatio = 1.0)
                    )
                )
            }
        }
    }

    private fun observeData() {
        // 1. مراقبة العملة الأساسية
        viewModelScope.launch(Dispatchers.IO) {
            currencyDao.getBaseCurrencyFlow().collectLatest { curr ->
                if (curr != null) {
                    _uiState.update { it.copy(currencySymbol = curr.symbol) }
                }
            }
        }

        // 2. مراقبة المنتجات
        viewModelScope.launch(Dispatchers.IO) {
            productDao.getProductsWithUnits().collectLatest { prods ->
                _uiState.update { it.copy(productsWithUnits = prods) }
            }
        }

        // 3. مراقبة المجموعات بتفاصيلها
        viewModelScope.launch(Dispatchers.IO) {
            groupDao.getAllGroupsWithDetails().collectLatest { groups ->
                _uiState.update { state ->
                    val selId = state.selectedGroupId ?: groups.firstOrNull()?.group?.id
                    val selDetails = groups.find { it.group.id == selId } ?: groups.firstOrNull()
                    state.copy(
                        groupsWithDetails = groups,
                        selectedGroupId = selDetails?.group?.id,
                        selectedGroupDetails = selDetails
                    )
                }
                recalculateCogsEngine()
            }
        }
    }

    fun setActiveTab(tab: Int) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun selectGroup(groupId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val details = groupDao.getGroupWithDetailsById(groupId)
            _uiState.update {
                it.copy(
                    selectedGroupId = groupId,
                    selectedGroupDetails = details
                )
            }
            recalculateCogsEngine()
            loadGroupItemsStockDetails(groupId)
        }
    }

    fun loadGroupItemsStockDetails(groupId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val details = groupDao.getGroupWithDetailsById(groupId) ?: return@launch
            val symbol = _uiState.value.currencySymbol

            val itemDetails = details.items.map { item ->
                var stockQty = 0.0
                var wasteQty = 0.0
                var unitName = "كجم"

                if (item.productId != null && item.productId > 0) {
                    stockQty = db.stockMovementDao().getTotalStockQuantity(item.productId)
                    wasteQty = db.productWastageDao().getTotalWasteQuantityForProduct(item.productId) ?: 0.0
                    val baseUnit = db.productDao().getUnitsForProductSync(item.productId).firstOrNull { it.isBaseUnit }
                    if (baseUnit != null) {
                        unitName = baseUnit.unitName
                    }
                }

                GroupItemAuditDetail(
                    itemId = item.id,
                    productId = item.productId,
                    productName = item.productName,
                    unitName = unitName,
                    currencySymbol = symbol,
                    currentStockQty = if (stockQty > 0) stockQty else 10.0,
                    autoWasteQty = wasteQty,
                    endingActualQtyInput = "0.0"
                )
            }

            val totalStock = itemDetails.sumOf { it.currentStockQty }
            val totalWaste = itemDetails.sumOf { it.autoWasteQty }

            _uiState.update { state ->
                state.copy(
                    groupItemsAuditDetails = itemDetails,
                    auditBeginningQtyInput = String.format(java.util.Locale.US, "%.1f", totalStock),
                    auditWasteQtyInput = String.format(java.util.Locale.US, "%.1f", totalWaste)
                )
            }
            recalculateCogsEngine()
        }
    }

    fun updateItemEndingQty(itemId: Long, endingQty: String) {
        _uiState.update { state ->
            val updatedDetails = state.groupItemsAuditDetails.map { item ->
                if (item.itemId == itemId) item.copy(endingActualQtyInput = endingQty) else item
            }
            val totalEnding = updatedDetails.sumOf { it.endingActualQtyInput.toDoubleOrNull() ?: 0.0 }
            state.copy(
                groupItemsAuditDetails = updatedDetails,
                auditEndingActualQtyInput = String.format(java.util.Locale.US, "%.1f", totalEnding)
            )
        }
        recalculateCogsEngine()
    }

    fun deleteAudit(auditId: Long) {
        val gId = _uiState.value.selectedGroupId ?: return
        viewModelScope.launch(Dispatchers.IO) {
            groupDao.deleteAuditById(auditId)
            selectGroup(gId)
            _uiState.update {
                it.copy(
                    feedbackMessage = "تم حذف سجل الجرد الدوري بنجاح",
                    isErrorFeedback = false
                )
            }
        }
    }

    fun updateAuditRecord(audit: StockGroupAuditEntity) {
        val gId = _uiState.value.selectedGroupId ?: return
        viewModelScope.launch(Dispatchers.IO) {
            groupDao.updateAudit(audit)
            selectGroup(gId)
            _uiState.update {
                it.copy(
                    feedbackMessage = "تم تحديث سجل الجرد الدوري بنجاح",
                    isErrorFeedback = false
                )
            }
        }
    }

    // --- إدارة المجموعات (إضافة / تعديل / حذف آمن) ---
    fun openAddGroupDialog() {
        val nextCode = "GRP-%03d".format(_uiState.value.groupsWithDetails.size + 1)
        _uiState.update {
            it.copy(
                showGroupDialog = true,
                editingGroupId = null,
                groupNameInput = "",
                groupCodeInput = nextCode,
                groupCategoryInput = "خضار وفواكه",
                groupCostMethod = CostValuationMethod.WAC,
                groupDescriptionInput = ""
            )
        }
    }

    fun openEditGroupDialog(group: StockGroupEntity) {
        _uiState.update {
            it.copy(
                showGroupDialog = true,
                editingGroupId = group.id,
                groupNameInput = group.name,
                groupCodeInput = group.code,
                groupCategoryInput = group.category,
                groupCostMethod = group.costMethod,
                groupDescriptionInput = group.description
            )
        }
    }

    fun dismissGroupDialog() {
        _uiState.update { it.copy(showGroupDialog = false) }
    }

    fun saveGroup(
        name: String,
        code: String,
        category: String,
        costMethod: CostValuationMethod,
        description: String
    ) {
        if (name.isBlank()) {
            _uiState.update { it.copy(feedbackMessage = "الرجاء إدخال اسم المجموعة المخزنية", isErrorFeedback = true) }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val editId = _uiState.value.editingGroupId
            if (editId == null) {
                val newGroup = StockGroupEntity(
                    name = name.trim(),
                    code = code.ifBlank { "GRP-${System.currentTimeMillis() % 1000}" },
                    category = category.ifBlank { "عام" },
                    costMethod = costMethod,
                    description = description.trim()
                )
                val newId = groupDao.insertGroup(newGroup)
                selectGroup(newId)
                _uiState.update {
                    it.copy(
                        showGroupDialog = false,
                        feedbackMessage = "تم إنشاء المجموعة المخزنية '${newGroup.name}' بنجاح",
                        isErrorFeedback = false
                    )
                }
            } else {
                val existing = groupDao.getGroupById(editId)
                if (existing != null) {
                    val updated = existing.copy(
                        name = name.trim(),
                        code = code.trim(),
                        category = category.trim(),
                        costMethod = costMethod,
                        description = description.trim()
                    )
                    groupDao.updateGroup(updated)
                    selectGroup(editId)
                    _uiState.update {
                        it.copy(
                            showGroupDialog = false,
                            feedbackMessage = "تم تحديث بيانات المجموعة المخزنية '${updated.name}'",
                            isErrorFeedback = false
                        )
                    }
                }
            }
        }
    }

    /**
     * الحذف الآمن للمجموعة مع التحقق البرمجي لمنع الحذف إذا كانت المجموعة مرتبطة بفواتير سابقة
     */
    fun checkAndDeleteGroup(group: StockGroupEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val linkedSalesCount = groupDao.getLinkedSalesCountForGroup(group.id)
            if (linkedSalesCount > 0) {
                // منع الحذف لحماية السلامة المحاسبية
                _uiState.update {
                    it.copy(
                        showDeleteValidationDialog = true,
                        pendingDeleteGroup = group,
                        isDeleteAllowed = false,
                        deleteValidationMessage = "عفواً! لا يمكن حذف المجموعة '${group.name}' لأنها مرتبطة برقم ($linkedSalesCount) عملية بيع أو فواتير سابقة في النظام. تم حظر الحذف لحماية السلامة المحاسبية والسجلات المترابطة."
                    )
                }
            } else {
                // يسمح بالحذف الآمن
                _uiState.update {
                    it.copy(
                        showDeleteValidationDialog = true,
                        pendingDeleteGroup = group,
                        isDeleteAllowed = true,
                        deleteValidationMessage = "هل أنت تأكد من حذف المجموعة المخزنية '${group.name}'؟ لا توجد فواتير مرتبطة بها حالياً وسيكون الحذف آمن تماماً."
                    )
                }
            }
        }
    }

    fun confirmDeletePendingGroup() {
        val group = _uiState.value.pendingDeleteGroup ?: return
        viewModelScope.launch(Dispatchers.IO) {
            groupDao.deleteGroup(group)
            _uiState.update {
                it.copy(
                    showDeleteValidationDialog = false,
                    pendingDeleteGroup = null,
                    feedbackMessage = "تم حذف المجموعة المخزنية '${group.name}' بنجاح",
                    isErrorFeedback = false
                )
            }
        }
    }

    fun dismissDeleteValidationDialog() {
        _uiState.update {
            it.copy(
                showDeleteValidationDialog = false,
                pendingDeleteGroup = null
            )
        }
    }

    // --- إضافة وتحديث وحذف أصناف المجموعة من قاعدة البيانات الفعليّة ---
    fun addItemToGroup(groupId: Long, productId: Long?, productName: String, ratio: Double) {
        if (productName.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val matchedProduct = if (productId != null && productId > 0) {
                productDao.getProductById(productId)
            } else {
                productDao.getAllProductsSync().find { it.name.trim().equals(productName.trim(), ignoreCase = true) }
            }

            val finalProductId = matchedProduct?.id ?: productId
            val finalName = matchedProduct?.name ?: productName.trim()

            val newItem = StockGroupItemEntity(
                groupId = groupId,
                productId = finalProductId,
                productName = finalName,
                defaultRatio = ratio
            )
            groupDao.insertGroupItems(listOf(newItem))
            selectGroup(groupId)
            _uiState.update {
                it.copy(
                    feedbackMessage = "تم إضافة الصنف '$finalName' للمجموعة بنجاح",
                    isErrorFeedback = false
                )
            }
        }
    }

    fun deleteGroupItem(itemId: Long) {
        val gId = _uiState.value.selectedGroupId ?: return
        viewModelScope.launch(Dispatchers.IO) {
            groupDao.deleteGroupItemById(itemId)
            selectGroup(gId)
            _uiState.update {
                it.copy(
                    feedbackMessage = "تم حذف الصنف من المجموعة بنجاح",
                    isErrorFeedback = false
                )
            }
        }
    }

    // --- حسابات محرك الجرد الدوري و COGS ---
    fun updateAuditInputs(
        begQty: String? = null,
        begCost: String? = null,
        newPurchasesQty: String? = null,
        newPurchasesCost: String? = null,
        endingActualQty: String? = null,
        wasteQty: String? = null,
        recordedSalesRevenue: String? = null,
        costMethod: CostValuationMethod? = null
    ) {
        _uiState.update { state ->
            state.copy(
                auditBeginningQtyInput = begQty ?: state.auditBeginningQtyInput,
                auditBeginningCostInput = begCost ?: state.auditBeginningCostInput,
                auditNewPurchasesQtyInput = newPurchasesQty ?: state.auditNewPurchasesQtyInput,
                auditNewPurchasesCostInput = newPurchasesCost ?: state.auditNewPurchasesCostInput,
                auditEndingActualQtyInput = endingActualQty ?: state.auditEndingActualQtyInput,
                auditWasteQtyInput = wasteQty ?: state.auditWasteQtyInput,
                auditRecordedSalesRevenueInput = recordedSalesRevenue ?: state.auditRecordedSalesRevenueInput,
                auditCostMethod = costMethod ?: state.auditCostMethod
            )
        }
        recalculateCogsEngine()
    }

    /**
     * محرك حساب التكلفة المحاسبية المعياري:
     * (بضاعة أول المدة + المشتريات الجديدة - بضاعة آخر المدة بالجرد الفعلي - التالف = تكلفة البضاعة المباعة COGS)
     * ومن ثم حساب صافي الربح = المبيعات المسجلة - COGS
     */
    private fun recalculateCogsEngine() {
        val state = _uiState.value
        val begQty = state.auditBeginningQtyInput.toDoubleOrNull() ?: 0.0
        val begCost = state.auditBeginningCostInput.toDoubleOrNull() ?: 0.0
        val purQty = state.auditNewPurchasesQtyInput.toDoubleOrNull() ?: 0.0
        val purCost = state.auditNewPurchasesCostInput.toDoubleOrNull() ?: 0.0
        val endingQty = state.auditEndingActualQtyInput.toDoubleOrNull() ?: 0.0
        val wasteQty = state.auditWasteQtyInput.toDoubleOrNull() ?: 0.0
        val totalRevenue = state.auditRecordedSalesRevenueInput.toDoubleOrNull() ?: 0.0

        val totalAvailableQty = begQty + purQty
        val totalAvailableCost = begCost + purCost

        val avgCost = if (totalAvailableQty > 0) totalAvailableCost / totalAvailableQty else 0.0

        // اختيار طريقة تقييم التكلفة (WAC / FIFO / LIFO / LAST_PURCHASE_PRICE)
        val unitCostForValuation = when (state.auditCostMethod) {
            CostValuationMethod.WAC -> avgCost
            CostValuationMethod.FIFO -> if (purQty > 0) purCost / purQty else avgCost
            CostValuationMethod.LIFO -> if (begQty > 0) begCost / begQty else avgCost
            CostValuationMethod.LAST_PURCHASE_PRICE -> if (purQty > 0) purCost / purQty else avgCost
        }

        // المعادلة المحاسبية المعيارية:
        // كمية المباع COGS = المتاح للبيع (أول + مشتريات) - آخر المدة للجرد الفعلي - التالف
        val cogsQty = (totalAvailableQty - endingQty - wasteQty).coerceAtLeast(0.0)
        val cogsCost = cogsQty * unitCostForValuation
        val netProfit = totalRevenue - cogsCost

        _uiState.update {
            it.copy(
                cogsCalculatedQty = cogsQty,
                cogsCalculatedCost = cogsCost,
                netProfitCalculated = netProfit,
                averageCostPerKg = unitCostForValuation
            )
        }
    }

    fun saveAudit() {
        val gId = _uiState.value.selectedGroupId ?: return
        val state = _uiState.value

        val begQty = state.auditBeginningQtyInput.toDoubleOrNull() ?: 0.0
        val begCost = state.auditBeginningCostInput.toDoubleOrNull() ?: 0.0
        val purQty = state.auditNewPurchasesQtyInput.toDoubleOrNull() ?: 0.0
        val purCost = state.auditNewPurchasesCostInput.toDoubleOrNull() ?: 0.0
        val endingQty = state.auditEndingActualQtyInput.toDoubleOrNull() ?: 0.0
        val wasteQty = state.auditWasteQtyInput.toDoubleOrNull() ?: 0.0
        val revenue = state.auditRecordedSalesRevenueInput.toDoubleOrNull() ?: 0.0

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isSavingAudit = true) }
            val audit = StockGroupAuditEntity(
                groupId = gId,
                beginningQtyKg = begQty,
                beginningCost = begCost,
                newPurchasesQtyKg = purQty,
                newPurchasesCost = purCost,
                endingActualQtyKg = endingQty,
                wasteQtyKg = wasteQty,
                cogsQtyKg = state.cogsCalculatedQty,
                cogsCost = state.cogsCalculatedCost,
                totalSalesRevenue = revenue,
                netProfit = state.netProfitCalculated,
                costValuationMethod = state.auditCostMethod
            )
            groupDao.insertAudit(audit)
            selectGroup(gId)
            _uiState.update {
                it.copy(
                    isSavingAudit = false,
                    feedbackMessage = "تم حفظ الجرد الدوري وحساب COGS بنجاح (صافي الربح: %.2f %s)".format(state.netProfitCalculated, it.currencySymbol),
                    isErrorFeedback = false
                )
            }
        }
    }

    fun dismissFeedback() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }
}
