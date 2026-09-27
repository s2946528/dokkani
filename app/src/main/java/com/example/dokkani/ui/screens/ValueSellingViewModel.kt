package com.example.dokkani.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.example.dokkani.data.local.DokkaniDatabase
import com.example.dokkani.data.local.entities.CostCenterEntity
import com.example.dokkani.data.local.entities.CostValuationMethod
import com.example.dokkani.data.local.entities.MovementType
import com.example.dokkani.data.local.entities.ProductWithUnits
import com.example.dokkani.data.local.entities.ShortageSettlementEntity
import com.example.dokkani.data.local.entities.StockGroupAuditEntity
import com.example.dokkani.data.local.entities.StockGroupEntity
import com.example.dokkani.data.local.entities.StockGroupItemEntity
import com.example.dokkani.data.local.entities.StockGroupWithDetails
import com.example.dokkani.data.local.entities.StockMovementEntity
import com.example.dokkani.domain.costing.CostCalculationEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuditItemRowState(
    val itemId: Long,
    val productId: Long?,
    val productName: String,
    val unitName: String = "كيلو",
    val currencySymbol: String = "ر.ي",
    val currentStockQty: Double = 0.0,
    val unitCostPrice: Double = 0.0,
    val endingActualQtyInput: String = "0.0"
)

data class GroupInvoiceSummary(
    val invoiceId: Long,
    val invoiceNumber: String,
    val date: Long,
    val groupItemsTotal: Double
)

data class ValueSellingUiState(
    val groupsWithDetails: List<StockGroupWithDetails> = emptyList(),
    val selectedGroupId: Long? = null,
    val selectedGroupDetails: StockGroupWithDetails? = null,
    val activeTab: Int = 0,
    val feedbackMessage: String? = null,
    val isErrorFeedback: Boolean = false,
    val currencySymbol: String = "ر.ي",
    val costCenters: List<CostCenterEntity> = emptyList(),

    // Group Dialog
    val showGroupDialog: Boolean = false,
    val groupNameInput: String = "",
    val groupCodeInput: String = "",
    val groupCategoryInput: String = "",
    val groupCostMethod: CostValuationMethod = CostValuationMethod.WAC,
    val groupDescriptionInput: String = "",
    val editingGroupId: Long? = null,

    // Delete validation dialog
    val showDeleteValidationDialog: Boolean = false,
    val pendingDeleteGroup: StockGroupEntity? = null,
    val isDeleteAllowed: Boolean = true,
    val deleteValidationMessage: String = "",

    // Manual shortage dialog
    val showManualShortageDialog: Boolean = false,

    // Settlement confirm dialog
    val showSettlementConfirmDialog: Boolean = false,
    val selectedShortageForSettlement: ShortageSettlementEntity? = null,
    val settlementAdminNotesInput: String = "",
    val isSettlingShortage: Boolean = false,

    // Products
    val productsWithUnits: List<ProductWithUnits> = emptyList(),

    // Audit tab
    val groupItemsAuditDetails: List<AuditItemRowState> = emptyList(),
    val groupInvoiceSummaries: List<GroupInvoiceSummary> = emptyList(),
    val lastAuditTimestamp: Long? = null,
    val auditCostMethod: CostValuationMethod = CostValuationMethod.WAC,
    val selectedAuditCostCenterId: Long? = null,
    val auditRecordedSalesRevenueInput: String = "0.0",
    val cogsCalculatedQty: Double = 0.0,
    val cogsCalculatedCost: Double = 0.0,
    val costCenterExpenses: Double = 0.0,
    val costCenterWastage: Double = 0.0,
    val netProfitCalculated: Double = 0.0,

    // Shortages tab
    val shortageSettlements: List<ShortageSettlementEntity> = emptyList(),
    val filterCostCenterId: Long? = null,
    val filterStatus: String? = null,
    val totalPendingShortageQty: Double = 0.0,
    val totalPendingShortageCost: Double = 0.0,
    val totalPendingValueSalesRevenue: Double = 0.0,
    val totalSettledValueSalesRevenue: Double = 0.0
)

class ValueSellingViewModel(application: Application) : AndroidViewModel(application) {

    private val db = DokkaniDatabase.getDatabase(application, viewModelScope)
    private val costCalculationEngine = CostCalculationEngine(db.productDao(), db.stockMovementDao(), db.systemSettingsDao())
    private val _uiState = MutableStateFlow(ValueSellingUiState())
    val uiState: StateFlow<ValueSellingUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch(Dispatchers.IO) {
            db.costCenterDao().getAllActiveCostCenters().collectLatest { centers ->
                _uiState.update { it.copy(costCenters = centers) }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            db.stockGroupDao().getAllGroupsWithDetails().collectLatest { list ->
                _uiState.update { state ->
                    val selectedId = state.selectedGroupId ?: list.firstOrNull()?.group?.id
                    val selectedDetails = list.firstOrNull { it.group.id == selectedId }
                    state.copy(
                        groupsWithDetails = list,
                        selectedGroupId = selectedId,
                        selectedGroupDetails = selectedDetails
                    )
                }
                loadAuditRowsForSelectedGroup()
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            db.productDao().getProductsWithUnits().collectLatest { prods ->
                _uiState.update { it.copy(productsWithUnits = prods) }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            db.shortageSettlementDao().getAllShortages().collectLatest { settlements ->
                _uiState.update { state ->
                    val pendingQty = settlements.filter { it.status == ShortageSettlementEntity.STATUS_PENDING }.sumOf { it.shortageQuantity }
                    val pendingCost = settlements.filter { it.status == ShortageSettlementEntity.STATUS_PENDING }.sumOf { it.totalShortageCost }
                    val pendingRevenue = settlements.filter { it.status == ShortageSettlementEntity.STATUS_PENDING }.sumOf { it.totalValueSalesAmount }
                    val settledRevenue = settlements.filter { it.status == ShortageSettlementEntity.STATUS_SETTLED }.sumOf { it.totalValueSalesAmount }
                    state.copy(
                        shortageSettlements = settlements,
                        totalPendingShortageQty = pendingQty,
                        totalPendingShortageCost = pendingCost,
                        totalPendingValueSalesRevenue = pendingRevenue,
                        totalSettledValueSalesRevenue = settledRevenue
                    )
                }
            }
        }
    }

    private fun loadAuditRowsForSelectedGroup() {
        viewModelScope.launch(Dispatchers.IO) {
            val groupDetails = uiState.value.selectedGroupDetails ?: return@launch
            val costCenterId = uiState.value.selectedAuditCostCenterId
            val valuationMethod = uiState.value.auditCostMethod

            val groupProductIds = groupDetails.items.mapNotNull { it.productId }
            val groupProductsMap = if (groupProductIds.isNotEmpty()) {
                db.productDao().getProductsByIds(groupProductIds).associateBy { it.id }
            } else emptyMap()

            // 1. استثناء وإخفاء المجموعة الرئيسية نفسها من قائمة حقول الجرد وعرض الأصناف الفرعية الحقيقية فقط
            val subItemsOnly = groupDetails.items.filter { item ->
                val prod = groupProductsMap[item.productId]
                val isMainGroupProduct = item.notes == "صنف رئيسي ممثل للمجموعة" ||
                        item.productName == groupDetails.group.name ||
                        prod?.category == "مجموعات مخزنية" ||
                        prod?.code == groupDetails.group.code
                !isMainGroupProduct
            }
            val targetItemsForAudit = if (subItemsOnly.isNotEmpty()) subItemsOnly else groupDetails.items

            val rows = targetItemsForAudit.map { item ->
                val stockQty = if (item.productId != null) {
                    db.stockMovementDao().getTotalStockQuantity(item.productId, costCenterId)
                } else 0.0

                val calculatedUnitCost = if (item.productId != null) {
                    try {
                        val result = costCalculationEngine.calculateProductCost(
                            productId = item.productId,
                            valuationMethod = valuationMethod,
                            costCenterId = costCenterId
                        )
                        result.unitCostTargetUnit
                    } catch (e: Exception) {
                        item.unitCost
                    }
                } else item.unitCost

                val costPrice = if (calculatedUnitCost > 0.0) calculatedUnitCost else item.unitCost
                AuditItemRowState(
                    itemId = item.id,
                    productId = item.productId,
                    productName = item.productName,
                    unitName = "كيلو",
                    currencySymbol = uiState.value.currencySymbol,
                    currentStockQty = stockQty,
                    unitCostPrice = costPrice,
                    endingActualQtyInput = "0.0"
                )
            }

            // تصفية أصناف المجموعة المبيعة بالقيمة فقط (isWeighted = false)
            val valueProductIdsInGroup = groupProductsMap.values
                .filter { !it.isWeighted }
                .map { it.id }

            val audits = db.stockGroupDao().getAuditsForGroupSync(groupDetails.group.id)
            val lastAudit = audits.firstOrNull { it.status == "COMPLETED" || it.status == "مكتمل" }
            val lastAuditTime = lastAudit?.auditDate ?: 0L
            val currentTime = System.currentTimeMillis()

            val invoiceSummaries = mutableListOf<GroupInvoiceSummary>()
            var totalGroupRevenue = 0.0

            if (valueProductIdsInGroup.isNotEmpty()) {
                val invoices = db.invoiceDao().getValueSaleInvoicesForGroupInDateRange(
                    productIds = valueProductIdsInGroup,
                    startDate = lastAuditTime,
                    endDate = currentTime,
                    costCenterId = costCenterId
                )
                for (invDetails in invoices) {
                    // تفحص بنود الفاتورة الداخلية وتجاهل أي أصناف بالوزن أو تنتمي لمجموعات أخرى
                    val groupItemsValue = invDetails.items
                        .filter { item ->
                            val prod = groupProductsMap[item.productId]
                            item.productId in valueProductIdsInGroup && prod != null && !prod.isWeighted
                        }
                        .sumOf { it.totalPrice }

                    if (groupItemsValue > 0.0) {
                        invoiceSummaries.add(
                            GroupInvoiceSummary(
                                invoiceId = invDetails.invoice.id,
                                invoiceNumber = invDetails.invoice.invoiceNumber,
                                date = invDetails.invoice.date,
                                groupItemsTotal = groupItemsValue
                            )
                        )
                        totalGroupRevenue += groupItemsValue
                    }
                }
            }

            _uiState.update {
                it.copy(
                    groupItemsAuditDetails = rows,
                    groupInvoiceSummaries = invoiceSummaries,
                    lastAuditTimestamp = if (lastAuditTime > 0L) lastAuditTime else null,
                    auditRecordedSalesRevenueInput = String.format(java.util.Locale.US, "%.2f", totalGroupRevenue)
                )
            }
            recalculateAuditCogs()
        }
    }

    fun selectGroup(groupId: Long) {
        val details = _uiState.value.groupsWithDetails.firstOrNull { it.group.id == groupId }
        _uiState.update {
            it.copy(selectedGroupId = groupId, selectedGroupDetails = details)
        }
        loadAuditRowsForSelectedGroup()
    }

    fun setActiveTab(index: Int) {
        _uiState.update { it.copy(activeTab = index) }
    }

    fun dismissFeedback() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }

    fun openAddGroupDialog() {
        _uiState.update {
            it.copy(
                showGroupDialog = true,
                groupNameInput = "",
                groupCodeInput = "",
                groupCategoryInput = "",
                groupCostMethod = CostValuationMethod.WAC,
                groupDescriptionInput = "",
                editingGroupId = null
            )
        }
    }

    fun openEditGroupDialog(group: StockGroupEntity) {
        _uiState.update {
            it.copy(
                showGroupDialog = true,
                groupNameInput = group.name,
                groupCodeInput = group.code,
                groupCategoryInput = "",
                groupCostMethod = CostValuationMethod.WAC,
                groupDescriptionInput = group.description,
                editingGroupId = group.id
            )
        }
    }

    fun dismissGroupDialog() {
        _uiState.update { it.copy(showGroupDialog = false) }
    }

    fun saveGroup(name: String, code: String, category: String, method: CostValuationMethod, desc: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val editId = _uiState.value.editingGroupId
            if (editId != null && editId > 0) {
                val existing = db.stockGroupDao().getGroupById(editId)
                if (existing != null) {
                    db.stockGroupDao().updateGroup(
                        existing.copy(name = name, code = code, description = desc)
                    )
                }
            } else {
                val newGroup = StockGroupEntity(
                    name = name,
                    code = code.ifBlank { "GRP-%03d".format((1..999).random()) },
                    description = desc
                )
                db.stockGroupDao().insertGroup(newGroup)
            }
            _uiState.update {
                it.copy(
                    showGroupDialog = false,
                    feedbackMessage = "تم حفظ المجموعة بنجاح",
                    isErrorFeedback = false
                )
            }
        }
    }

    fun checkAndDeleteGroup(group: StockGroupEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val linkedSales = db.stockGroupDao().getLinkedSalesCountForGroup(group.id)
            val isAllowed = linkedSales == 0
            val msg = if (isAllowed) {
                "هل أنت أصل ومؤكد من حذف مجموعة '${group.name}'؟ سيتم حذف المجموعة وعناصرها."
            } else {
                "لا يمكن حذف المجموعة '${group.name}' لوجود ($linkedSales) عمليات بيع مسجلة مرتبطة بها. السلامة المحاسبية تقتضي الحفاظ على البيانات."
            }
            _uiState.update {
                it.copy(
                    showDeleteValidationDialog = true,
                    pendingDeleteGroup = group,
                    isDeleteAllowed = isAllowed,
                    deleteValidationMessage = msg
                )
            }
        }
    }

    fun confirmDeletePendingGroup() {
        val group = _uiState.value.pendingDeleteGroup ?: return
        viewModelScope.launch(Dispatchers.IO) {
            db.stockGroupDao().deleteGroup(group)
            _uiState.update {
                it.copy(
                    showDeleteValidationDialog = false,
                    pendingDeleteGroup = null,
                    feedbackMessage = "تم حذف المجموعة بنجاح",
                    isErrorFeedback = false
                )
            }
        }
    }

    fun dismissDeleteValidationDialog() {
        _uiState.update {
            it.copy(showDeleteValidationDialog = false, pendingDeleteGroup = null)
        }
    }

    fun addItemToGroup(groupId: Long, productId: Long, unitPrice: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val product = db.productDao().getProductById(productId) ?: return@launch
            val item = StockGroupItemEntity(
                groupId = groupId,
                productId = productId,
                productName = product.name,
                unitSellingPrice = unitPrice
            )
            db.stockGroupDao().insertGroupItems(listOf(item))
            _uiState.update {
                it.copy(feedbackMessage = "تمت إضافة الصنف '${product.name}' للمجموعة", isErrorFeedback = false)
            }
            loadAuditRowsForSelectedGroup()
        }
    }

    fun deleteGroupItem(itemId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            db.stockGroupDao().deleteGroupItemById(itemId)
            _uiState.update {
                it.copy(feedbackMessage = "تم حذف الصنف الفرعي من المجموعة", isErrorFeedback = false)
            }
            loadAuditRowsForSelectedGroup()
        }
    }

    fun selectAuditCostCenter(costCenterId: Long?) {
        _uiState.update { it.copy(selectedAuditCostCenterId = costCenterId) }
        loadAuditRowsForSelectedGroup()
    }

    fun updateAuditInputs(recordedSalesRevenue: String? = null, costMethod: CostValuationMethod? = null) {
        _uiState.update { state ->
            state.copy(
                auditRecordedSalesRevenueInput = recordedSalesRevenue ?: state.auditRecordedSalesRevenueInput,
                auditCostMethod = costMethod ?: state.auditCostMethod
            )
        }
        recalculateAuditCogs()
    }

    fun updateItemEndingQty(itemId: Long, newQtyInput: String) {
        _uiState.update { state ->
            val updatedRows = state.groupItemsAuditDetails.map { row ->
                if (row.itemId == itemId) row.copy(endingActualQtyInput = newQtyInput) else row
            }
            state.copy(groupItemsAuditDetails = updatedRows)
        }
        recalculateAuditCogs()
    }

    private fun recalculateAuditCogs() {
        val state = _uiState.value
        val step3TotalInvoices = state.groupInvoiceSummaries.sumOf { it.groupItemsTotal }
        val recordedSales = if (state.groupInvoiceSummaries.isNotEmpty()) step3TotalInvoices else (state.auditRecordedSalesRevenueInput.toDoubleOrNull() ?: 0.0)
        val costCenterId = state.selectedAuditCostCenterId

        viewModelScope.launch(Dispatchers.IO) {
            var totalStockAll = 0.0
            var totalActualEndingAll = 0.0
            var totalCogsCostAll = 0.0

            for (row in state.groupItemsAuditDetails) {
                val actual = row.endingActualQtyInput.toDoubleOrNull() ?: 0.0
                val cogsItemQty = (row.currentStockQty - actual).coerceAtLeast(0.0)
                totalStockAll += row.currentStockQty
                totalActualEndingAll += actual
                totalCogsCostAll += cogsItemQty * row.unitCostPrice
            }

            val totalCogsQtyAll = (totalStockAll - totalActualEndingAll).coerceAtLeast(0.0)

            // جلب المصاريف والتالف المرتبطة بمركز التكلفة
            val centerExpenses = if (costCenterId != null) db.expenseDao().getTotalExpensesForCostCenter(costCenterId) else 0.0
            val centerWastage = if (costCenterId != null) db.productWastageDao().getTotalWastageCostForCostCenter(costCenterId) else 0.0

            // معادلة صافي الربح = المبيعات بالقيمة - (تكلفة المباع COGS + المصاريف والتالف لمركز التكلفة)
            val netProfit = recordedSales - (totalCogsCostAll + centerExpenses + centerWastage)

            _uiState.update {
                it.copy(
                    cogsCalculatedQty = totalCogsQtyAll,
                    cogsCalculatedCost = totalCogsCostAll,
                    costCenterExpenses = centerExpenses,
                    costCenterWastage = centerWastage,
                    netProfitCalculated = netProfit
                )
            }
        }
    }

    fun saveAudit() {
        val groupId = _uiState.value.selectedGroupId ?: return
        val state = _uiState.value
        val step3TotalInvoices = state.groupInvoiceSummaries.sumOf { it.groupItemsTotal }
        val recordedSales = if (state.groupInvoiceSummaries.isNotEmpty()) step3TotalInvoices else (state.auditRecordedSalesRevenueInput.toDoubleOrNull() ?: 0.0)
        val costCenterId = state.selectedGroupDetails?.group?.costCenterId ?: 1L

        viewModelScope.launch(Dispatchers.IO) {
            val nowTimestamp = System.currentTimeMillis()
            var totalStockAll = 0.0
            var totalActualEndingAll = 0.0
            var totalCogsCostAll = 0.0

            val movementsToInsert = mutableListOf<StockMovementEntity>()

            db.withTransaction {
                for (row in state.groupItemsAuditDetails) {
                    val actual = row.endingActualQtyInput.toDoubleOrNull() ?: 0.0
                    val cogsItemQty = (row.currentStockQty - actual).coerceAtLeast(0.0)
                    totalStockAll += row.currentStockQty
                    totalActualEndingAll += actual
                    totalCogsCostAll += cogsItemQty * row.unitCostPrice

                    // 1. تنفيذ تسوية المخزون آلياً: خصم كميات العجز (كمية المباع = المخزون السابق - الجرد الفعلي) من رصيد النظام الحالي لكل صنف فرعي (مثل: طماطم، بطاطس، كوسة)
                    if (row.productId != null && row.productId > 0 && cogsItemQty > 0.0001) {
                        movementsToInsert.add(
                            StockMovementEntity(
                                productId = row.productId,
                                movementType = MovementType.INVENTORY_ADJUSTMENT,
                                quantityBaseUnit = -cogsItemQty,
                                remainingQuantityForFifo = 0.0,
                                unitCostPriceBase = row.unitCostPrice,
                                timestamp = nowTimestamp,
                                referenceNumber = "AUDIT-GRP-$groupId",
                                notes = "تسوية جردية آليّة للمجموعة: خصم كمية المباع (${"%.2f".format(cogsItemQty)} كجم/وحدة)",
                                costCenterId = costCenterId
                            )
                        )

                        // استهلاك طبقات FIFO إن وجدت للصنف الفرعي
                        var remainingToDeduct = cogsItemQty
                        val availableLots = db.stockMovementDao().getAvailableFifoLots(row.productId)
                        for (lot in availableLots) {
                            if (remainingToDeduct <= 0.0001) break
                            val lotQty = lot.remainingQuantityForFifo
                            val deductFromLot = minOf(remainingToDeduct, lotQty)
                            val updatedLot = lot.copy(remainingQuantityForFifo = lotQty - deductFromLot)
                            db.stockMovementDao().updateMovement(updatedLot)
                            remainingToDeduct -= deductFromLot
                        }
                    }
                }

                if (movementsToInsert.isNotEmpty()) {
                    db.stockMovementDao().insertMovements(movementsToInsert)
                }

                val totalCogsQtyAll = (totalStockAll - totalActualEndingAll).coerceAtLeast(0.0)
                val approxPricePerKg = if (totalCogsQtyAll > 0) recordedSales / totalCogsQtyAll else 0.0

                val centerExpenses = db.expenseDao().getTotalExpensesForCostCenter(costCenterId)
                val centerWastage = db.productWastageDao().getTotalWastageCostForCostCenter(costCenterId)
                val netProfit = recordedSales - (totalCogsCostAll + centerExpenses + centerWastage)

                // 2. حفظ سجل الجرد الدوري ونتائجه (COGS، المبيعات، صافي الربح، وتاريخ ووقت الجرد) في جدول السجلات التاريخية للمجموعة
                val audit = StockGroupAuditEntity(
                    groupId = groupId,
                    auditDate = nowTimestamp,
                    beginningStockQty = totalStockAll,
                    purchasesQty = 0.0,
                    actualEndingQty = totalActualEndingAll,
                    totalCogsQty = totalCogsQtyAll,
                    totalValueSalesAmount = recordedSales,
                    approxPricePerKg = approxPricePerKg,
                    totalCogsCost = totalCogsCostAll,
                    netProfit = netProfit,
                    status = "COMPLETED"
                )

                db.stockGroupDao().insertAudit(audit)
            }

            _uiState.update {
                it.copy(
                    feedbackMessage = "تم حفظ وترحيل اعتماد الجرد الدوري وتسوية مخزون الأصناف الفرعية آلياً بنجاح",
                    isErrorFeedback = false
                )
            }

            // تحديث وإعادة تحميل صفوف الجرد ليعكس النظام الرصيد الجديد وتاريخ الجرد الأحدث
            loadAuditRowsForSelectedGroup()
        }
    }

    fun updateAuditRecord(
        audit: StockGroupAuditEntity,
        endingActualQtyKg: Double,
        wasteQtyKg: Double,
        totalSalesRevenue: Double,
        cogsQtyKg: Double,
        cogsCost: Double
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val netProfit = totalSalesRevenue - cogsCost
            val approxPrice = if (cogsQtyKg > 0) totalSalesRevenue / cogsQtyKg else 0.0
            val updated = audit.copy(
                actualEndingQty = endingActualQtyKg,
                totalCogsQty = cogsQtyKg,
                totalValueSalesAmount = totalSalesRevenue,
                approxPricePerKg = approxPrice,
                totalCogsCost = cogsCost,
                netProfit = netProfit
            )
            db.stockGroupDao().updateAudit(updated)
            _uiState.update {
                it.copy(feedbackMessage = "تم تحديث سجل الجرد بنجاح", isErrorFeedback = false)
            }
        }
    }

    fun deleteAudit(auditId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            db.stockGroupDao().deleteAuditById(auditId)
            _uiState.update {
                it.copy(feedbackMessage = "تم حذف سجل الجرد الدوري", isErrorFeedback = false)
            }
        }
    }

    fun openManualShortageDialog() {
        _uiState.update { it.copy(showManualShortageDialog = true) }
    }

    fun dismissManualShortageDialog() {
        _uiState.update { it.copy(showManualShortageDialog = false) }
    }

    fun addManualShortageRecord(
        productName: String,
        costCenterId: Long,
        shortageQty: Double,
        unitCost: Double,
        unitSellingPrice: Double,
        notes: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val center = db.costCenterDao().getCostCenterById(costCenterId)
            val centerName = center?.centerName ?: "مركز التكلفة العام"
            val totalCost = shortageQty * unitCost
            val totalRevenue = shortageQty * unitSellingPrice

            val entity = ShortageSettlementEntity(
                productName = productName,
                costCenterId = costCenterId,
                costCenterName = centerName,
                bookQuantity = shortageQty,
                actualQuantity = 0.0,
                shortageQuantity = shortageQty,
                unitCost = unitCost,
                unitSellingPrice = unitSellingPrice,
                totalShortageCost = totalCost,
                totalValueSalesAmount = totalRevenue,
                status = ShortageSettlementEntity.STATUS_PENDING,
                notes = notes
            )
            db.shortageSettlementDao().insertShortage(entity)
            _uiState.update {
                it.copy(
                    showManualShortageDialog = false,
                    feedbackMessage = "تم رصد قيد العجز بنجاح",
                    isErrorFeedback = false
                )
            }
        }
    }

    fun openSettlementConfirmDialog(shortage: ShortageSettlementEntity) {
        _uiState.update {
            it.copy(
                showSettlementConfirmDialog = true,
                selectedShortageForSettlement = shortage,
                settlementAdminNotesInput = shortage.notes
            )
        }
    }

    fun dismissSettlementConfirmDialog() {
        _uiState.update {
            it.copy(showSettlementConfirmDialog = false, selectedShortageForSettlement = null)
        }
    }

    fun updateSettlementNotesInput(notes: String) {
        _uiState.update { it.copy(settlementAdminNotesInput = notes) }
    }

    fun confirmAndSettleShortage(adminUser: String) {
        val shortage = _uiState.value.selectedShortageForSettlement ?: return
        val notes = _uiState.value.settlementAdminNotesInput
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isSettlingShortage = true) }
            db.shortageSettlementDao().markAsSettled(
                id = shortage.id,
                settledBy = adminUser,
                notes = notes
            )
            _uiState.update {
                it.copy(
                    isSettlingShortage = false,
                    showSettlementConfirmDialog = false,
                    selectedShortageForSettlement = null,
                    feedbackMessage = "تم اعتماد وإقفال تسوية العجز وتحويلها لإيراد مبيعات بالقيمة",
                    isErrorFeedback = false
                )
            }
        }
    }

    fun calculateAndImportShortageFromAudit() {
        viewModelScope.launch(Dispatchers.IO) {
            val groupDetails = uiState.value.selectedGroupDetails ?: return@launch
            val audit = groupDetails.audits.firstOrNull() ?: return@launch
            if (audit.actualEndingQty < audit.beginningStockQty) {
                val shortageQty = audit.beginningStockQty - audit.actualEndingQty
                val entity = ShortageSettlementEntity(
                    auditId = audit.id,
                    productName = "عجز مجموعة ${groupDetails.group.name}",
                    costCenterId = groupDetails.group.costCenterId,
                    costCenterName = "مركز التكلفة العام",
                    bookQuantity = audit.beginningStockQty,
                    actualQuantity = audit.actualEndingQty,
                    shortageQuantity = shortageQty,
                    unitCost = if (shortageQty > 0) audit.totalCogsCost / shortageQty else 0.0,
                    unitSellingPrice = audit.approxPricePerKg,
                    totalShortageCost = audit.totalCogsCost,
                    totalValueSalesAmount = audit.totalValueSalesAmount,
                    status = ShortageSettlementEntity.STATUS_PENDING,
                    notes = "استيراد تلقائي لعجز الجرد الدوري للمجموعة ${groupDetails.group.name}"
                )
                db.shortageSettlementDao().insertShortage(entity)
                _uiState.update {
                    it.copy(feedbackMessage = "تم استيراد قيد العجز من الجرد الدوري بنجاح", isErrorFeedback = false)
                }
            } else {
                _uiState.update {
                    it.copy(feedbackMessage = "لا يوجد عجز جردي لاستيراده في الجرد الحالي للمجموعة", isErrorFeedback = true)
                }
            }
        }
    }

    fun setCostCenterFilter(centerId: Long?) {
        _uiState.update { it.copy(filterCostCenterId = centerId) }
    }

    fun setStatusFilter(status: String?) {
        _uiState.update { it.copy(filterStatus = status) }
    }

    fun deleteShortageRecord(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            db.shortageSettlementDao().deleteShortageById(id)
            _uiState.update {
                it.copy(feedbackMessage = "تم حذف قيد العجز بنجاح", isErrorFeedback = false)
            }
        }
    }
}
