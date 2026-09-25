package com.example.dokkani.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dokkani.data.local.DokkaniDatabase
import com.example.dokkani.data.local.entities.CostCenterEntity
import com.example.dokkani.data.local.entities.CostValuationMethod
import com.example.dokkani.data.local.entities.InvoiceEntity
import com.example.dokkani.data.local.entities.InvoiceItemEntity
import com.example.dokkani.data.local.entities.InvoiceType
import com.example.dokkani.data.local.entities.PaymentMethod
import com.example.dokkani.data.local.entities.InvoiceStatus
import com.example.dokkani.data.local.entities.ProductWithUnits
import com.example.dokkani.data.local.entities.ShortageSettlementEntity
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
    val endingActualQtyInput: String = "0.0"
)

/**
 * حالة واجهة بيع بالقيمة وإدارة المجموعات والجرد
 */
data class ValueSellingUiState(
    val activeTab: Int = 0, // 0: إدارة المجموعات، 1: الجرد الدوري و COGS، 2: تسوية العجز والبيع بالقيمة
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

    // تسوية العجز المخزني والبيع بالقيمة (Shortage Settlement & Value Sales)
    val shortageSettlements: List<ShortageSettlementEntity> = emptyList(),
    val costCenters: List<CostCenterEntity> = emptyList(),
    val selectedCostCenterIdFilter: Long? = null, // null = جميع المراكز
    val selectedStatusFilter: String? = null, // null = الكل، "PENDING"، "SETTLED"
    val selectedShortageForSettlement: ShortageSettlementEntity? = null,
    val showSettlementConfirmDialog: Boolean = false,
    val showManualShortageDialog: Boolean = false,
    val settlementAdminNotesInput: String = "",
    val isSettlingShortage: Boolean = false,

    // إحصائيات مالية للعجز والبيع بالقيمة
    val totalPendingShortageQty: Double = 0.0,
    val totalPendingShortageCost: Double = 0.0,
    val totalPendingValueSalesRevenue: Double = 0.0,
    val totalSettledValueSalesRevenue: Double = 0.0,

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
    private val costCenterDao = db.costCenterDao()
    private val shortageDao = db.shortageSettlementDao()
    private val invoiceDao = db.invoiceDao()

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

        // 4. مراقبة مراكز التكلفة
        viewModelScope.launch(Dispatchers.IO) {
            costCenterDao.getAllCostCenters().collectLatest { centers ->
                _uiState.update { it.copy(costCenters = centers) }
            }
        }

        // 5. مراقبة قيود وتسويات العجز المخزني والبيع بالقيمة
        viewModelScope.launch(Dispatchers.IO) {
            shortageDao.getAllShortages().collectLatest { list ->
                val pending = list.filter { it.status == ShortageSettlementEntity.STATUS_PENDING }
                val settled = list.filter { it.status == ShortageSettlementEntity.STATUS_SETTLED }

                _uiState.update { state ->
                    state.copy(
                        shortageSettlements = list,
                        totalPendingShortageQty = pending.sumOf { it.shortageQuantity },
                        totalPendingShortageCost = pending.sumOf { it.totalShortageCost },
                        totalPendingValueSalesRevenue = pending.sumOf { it.totalValueSalesAmount },
                        totalSettledValueSalesRevenue = settled.sumOf { it.totalValueSalesAmount }
                    )
                }
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
                var unitName = "كجم"

                if (item.productId != null && item.productId > 0) {
                    stockQty = db.stockMovementDao().getTotalStockQuantity(item.productId)
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
                    endingActualQtyInput = "0.0"
                )
            }

            val totalStock = itemDetails.sumOf { it.currentStockQty }

            // جلب إجمالي المبيعات بالقيمة آلياً (Read-Only) للأصناف التابعة للمجموعة
            val groupProductIds = details.items.mapNotNull { it.productId }.toSet()
            val saleInvoices = db.invoiceDao().getAllInvoicesSync().filter { it.type == InvoiceType.SALE }
            val saleInvoiceIds = saleInvoices.map { it.id }.toSet()
            val allInvoiceItems = db.invoiceDao().getAllInvoiceItemsSync()
            val autoFetchedSalesRevenue = allInvoiceItems
                .filter { it.invoiceId in saleInvoiceIds && it.productId in groupProductIds }
                .sumOf { it.totalPrice }

            _uiState.update { state ->
                state.copy(
                    groupItemsAuditDetails = itemDetails,
                    auditBeginningQtyInput = String.format(java.util.Locale.US, "%.1f", totalStock),
                    auditRecordedSalesRevenueInput = String.format(java.util.Locale.US, "%.1f", autoFetchedSalesRevenue)
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

    // --- إدارة تسوية العجز والبيع بالقيمة (Shortage Settlement & Value-Based Sales) ---

    fun setCostCenterFilter(costCenterId: Long?) {
        _uiState.update { it.copy(selectedCostCenterIdFilter = costCenterId) }
    }

    fun setStatusFilter(status: String?) {
        _uiState.update { it.copy(selectedStatusFilter = status) }
    }

    /**
     * احتساب ورصد العجز المخزني الناتج حصرياً عن اعتماد الجرد الدوري (الدفتري - الفعلي)
     * مع الضوابط الآتية:
     * 1. عزل التلف والهادر تماماً كـ مصروف مستقل وعدم دخوله في معادلة العجز.
     * 2. ربطه بمركز التكلفة المحدد أو مركز التكلفة العام افتراضياً.
     */
    fun calculateAndImportShortageFromAudit(targetCostCenterId: Long = 1) {
        viewModelScope.launch(Dispatchers.IO) {
            val state = _uiState.value
            val currentGroupDetails = state.selectedGroupDetails
            val itemsDetails = state.groupItemsAuditDetails

            val costCenter = costCenterDao.getCostCenterById(targetCostCenterId)
            val ccName = costCenter?.centerName ?: "مركز التكلفة العام"

            var newShortagesCreated = 0
            val shortagesToInsert = mutableListOf<ShortageSettlementEntity>()

            // رصد العجز لكل صنف داخل الجرد
            for (item in itemsDetails) {
                val bookQty = item.currentStockQty
                val actualQty = item.endingActualQtyInput.toDoubleOrNull() ?: 0.0

                // العجز = كمية الدفتري المتبقي - كمية الفعلي
                // (معزول تماماً عن autoWasteQty المخصص للتلف الهادر)
                if (bookQty > actualQty) {
                    val shortageQty = bookQty - actualQty

                    // استخراج سعر التكلفة والبيع للصنف من جدول وحدات المنتج
                    var unitCost = 10.0
                    var unitPrice = 15.0

                    if (item.productId != null && item.productId > 0) {
                        val units = productDao.getUnitsForProductSync(item.productId)
                        val baseUnit = units.firstOrNull { it.isBaseUnit } ?: units.firstOrNull()
                        if (baseUnit != null) {
                            unitCost = if (baseUnit.costPrice > 0) baseUnit.costPrice else 10.0
                            unitPrice = if (baseUnit.sellingPrice > 0) baseUnit.sellingPrice else 15.0
                        }
                    }

                    val totalCost = shortageQty * unitCost
                    val totalValueRevenue = shortageQty * unitPrice

                    shortagesToInsert.add(
                        ShortageSettlementEntity(
                            auditId = currentGroupDetails?.group?.id ?: 0,
                            productId = item.productId,
                            productName = item.productName,
                            costCenterId = targetCostCenterId,
                            costCenterName = ccName,
                            bookQuantity = bookQty,
                            actualQuantity = actualQty,
                            shortageQuantity = shortageQty,
                            unitCost = unitCost,
                            unitSellingPrice = unitPrice,
                            totalShortageCost = totalCost,
                            totalValueSalesAmount = totalValueRevenue,
                            status = ShortageSettlementEntity.STATUS_PENDING,
                            notes = "عجز مخزني ناتج عن جرد ${currentGroupDetails?.group?.name ?: "دوري"}"
                        )
                    )
                    newShortagesCreated++
                }
            }

            if (shortagesToInsert.isNotEmpty()) {
                shortageDao.insertShortages(shortagesToInsert)
                _uiState.update {
                    it.copy(
                        feedbackMessage = "تم رصد واحتساب ($newShortagesCreated) قيود عجز مخزني جديدة وتحويلها لشاشة البيع بالقيمة بنجاح",
                        isErrorFeedback = false
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        feedbackMessage = "لا يوجد عجز مخزني مرصود في هذا الجرد (الكمية الفعلية تطابق أو تفوق الدفتري).",
                        isErrorFeedback = true
                    )
                }
            }
        }
    }

    /**
     * إدخال قيد عجز/بيع بالقيمة يدوي بواسطة مدير النظام
     */
    fun addManualShortageRecord(
        productName: String,
        costCenterId: Long,
        shortageQty: Double,
        unitCost: Double,
        unitSellingPrice: Double,
        notes: String
    ) {
        if (productName.isBlank() || shortageQty <= 0) {
            _uiState.update {
                it.copy(feedbackMessage = "يرجى كتابة اسم الصنف وكمية العجز بشكل صحيح", isErrorFeedback = true)
            }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val cc = costCenterDao.getCostCenterById(costCenterId)
            val ccName = cc?.centerName ?: "مركز التكلفة العام"

            val totalCost = shortageQty * unitCost
            val totalRevenue = shortageQty * unitSellingPrice

            val entity = ShortageSettlementEntity(
                productName = productName.trim(),
                costCenterId = costCenterId,
                costCenterName = ccName,
                bookQuantity = shortageQty,
                actualQuantity = 0.0,
                shortageQuantity = shortageQty,
                unitCost = unitCost,
                unitSellingPrice = unitSellingPrice,
                totalShortageCost = totalCost,
                totalValueSalesAmount = totalRevenue,
                status = ShortageSettlementEntity.STATUS_PENDING,
                notes = notes.ifBlank { "قيد عجز يدوي للبيع بالقيمة" }
            )

            shortageDao.insertShortage(entity)
            _uiState.update {
                it.copy(
                    showManualShortageDialog = false,
                    feedbackMessage = "تم إضافة قيد العجز للصنف '$productName' بنجاح",
                    isErrorFeedback = false
                )
            }
        }
    }

    fun openSettlementConfirmDialog(shortage: ShortageSettlementEntity) {
        _uiState.update {
            it.copy(
                selectedShortageForSettlement = shortage,
                showSettlementConfirmDialog = true,
                settlementAdminNotesInput = shortage.notes
            )
        }
    }

    fun dismissSettlementConfirmDialog() {
        _uiState.update {
            it.copy(
                selectedShortageForSettlement = null,
                showSettlementConfirmDialog = false
            )
        }
    }

    fun openManualShortageDialog() {
        _uiState.update { it.copy(showManualShortageDialog = true) }
    }

    fun dismissManualShortageDialog() {
        _uiState.update { it.copy(showManualShortageDialog = false) }
    }

    fun updateSettlementNotesInput(notes: String) {
        _uiState.update { it.copy(settlementAdminNotesInput = notes) }
    }

    /**
     * تأكيد اعتماد تسوية العجز تحويله إلى مبيعات بالقيمة مقفلة ومسددة (خاص بمدير النظام فقط):
     * 1. تحديث حالة القيد إلى SETTLED (مقفلة/مسددة).
     * 2. تسجيل فاتورة مبيعات كاش بالخزينة بقيمة إيراد البيع بالقيمة.
     * 3. إقفال الدفاتر المخزنية والمحاسبية بنظافة.
     */
    fun confirmAndSettleShortage(adminUserName: String = "مدير النظام") {
        val shortage = _uiState.value.selectedShortageForSettlement ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isSettlingShortage = true) }

            val notes = _uiState.value.settlementAdminNotesInput.ifBlank {
                "تسوية عجز مخزني كـ مبيعات بالقيمة - معتمد بواسطة $adminUserName"
            }

            // 1. تحديث حالة التسوية في جدول العجز
            shortageDao.markAsSettled(
                id = shortage.id,
                settledBy = adminUserName,
                notes = notes,
                settledAt = System.currentTimeMillis()
            )

            // 2. إنشاء فاتورة مبيعات كاش لتدخل الخزينة/الصندوق كإيراد مبيعات بالقيمة
            val baseCurrency = currencyDao.getBaseCurrency() ?: currencyDao.getAllCurrenciesSync().firstOrNull()
            val currencyId = baseCurrency?.id ?: 1L
            val invoiceNumber = "INV-VAL-${System.currentTimeMillis() % 100000}"

            val invoice = InvoiceEntity(
                invoiceNumber = invoiceNumber,
                type = InvoiceType.SALE,
                partyId = null,
                currencyId = currencyId,
                subtotal = shortage.totalValueSalesAmount,
                discount = 0.0,
                total = shortage.totalValueSalesAmount,
                paidAmount = shortage.totalValueSalesAmount,
                remainingAmount = 0.0,
                paymentMethod = PaymentMethod.CASH,
                status = InvoiceStatus.COMPLETED,
                costCenterId = shortage.costCenterId,
                notes = "إيراد مبيعات بالقيمة ناتج عن تسوية عجز الصنف (${shortage.productName}) بمركز (${shortage.costCenterName})"
            )

            val invoiceId = invoiceDao.insertInvoice(invoice)

            val productUnitId = if (shortage.productId != null && shortage.productId > 0) {
                productDao.getUnitsForProductSync(shortage.productId).firstOrNull()?.id ?: 1L
            } else {
                1L
            }

            // إدراج صنف الفاتورة التفصيلي
            invoiceDao.insertInvoiceItems(
                listOf(
                    InvoiceItemEntity(
                        invoiceId = invoiceId,
                        productId = shortage.productId ?: 1L,
                        productUnitId = productUnitId,
                        quantity = shortage.shortageQuantity,
                        unitConversionFactor = 1.0,
                        unitCostPrice = shortage.unitCost,
                        unitSellingPrice = shortage.unitSellingPrice,
                        totalPrice = shortage.totalValueSalesAmount
                    )
                )
            )

            _uiState.update {
                it.copy(
                    isSettlingShortage = false,
                    showSettlementConfirmDialog = false,
                    selectedShortageForSettlement = null,
                    feedbackMessage = "تم تأكيد تسوية عجز الصنف '${shortage.productName}' بقيمة (%.2f %s) وقيده كـ مبيعات بالقيمة مقفلة دخلت الخزينة بنجاح!".format(
                        shortage.totalValueSalesAmount,
                        it.currencySymbol
                    ),
                    isErrorFeedback = false
                )
            }
        }
    }

    fun deleteShortageRecord(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            shortageDao.deleteShortageById(id)
            _uiState.update {
                it.copy(
                    feedbackMessage = "تم حذف قيد العجز بنجاح",
                    isErrorFeedback = false
                )
            }
        }
    }

    fun dismissFeedback() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }
}
