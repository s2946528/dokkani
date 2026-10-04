package com.example.dokkani.ui.screens.assembly

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.example.dokkani.data.local.DokkaniDatabase
import com.example.dokkani.data.local.entities.AssemblyStatus
import com.example.dokkani.data.local.entities.ItemAssemblyComponentEntity
import com.example.dokkani.data.local.entities.ItemAssemblyEntity
import com.example.dokkani.data.local.entities.ItemAssemblyWithComponents
import com.example.dokkani.data.local.entities.MovementType
import com.example.dokkani.data.local.entities.ProductEntity
import com.example.dokkani.data.local.entities.ProductUnitEntity
import com.example.dokkani.data.local.entities.StockMovementEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * تمثيل الصنف مع أحدث رصيد متوفر ووحدته الأساسية
 */
data class AssemblyProductItem(
    val product: ProductEntity,
    val baseUnit: ProductUnitEntity?,
    val currentStock: Double = 0.0
) {
    val id: Long get() = product.id
    val name: String get() = product.name
    val code: String get() = product.code
    val category: String get() = product.category
    val unitName: String get() = baseUnit?.unitName ?: "حبة"
    val costPrice: Double get() = baseUnit?.costPrice ?: 0.0
    val sellPrice: Double get() = baseUnit?.sellingPrice ?: 0.0
}

/**
 * حالة المكون الفرعي المسجل في شاشة التركيب
 */
data class AssemblyComponentItemState(
    val productItem: AssemblyProductItem,
    val unitName: String = productItem.unitName,
    val quantityPerUnit: Double = 1.0,
    val unitCostPrice: Double = productItem.costPrice
) {
    val product: AssemblyProductItem get() = productItem
    fun getTotalQuantityUsed(producedQuantity: Double): Double = quantityPerUnit * producedQuantity
    fun getTotalCost(producedQuantity: Double): Double = getTotalQuantityUsed(producedQuantity) * unitCostPrice
}

/**
 * حالة واجهة تركيب الأصناف (Item Assembly UI State)
 */
data class ItemAssemblyUiState(
    val products: List<AssemblyProductItem> = emptyList(),
    val assembliesHistory: List<ItemAssemblyWithComponents> = emptyList(),
    val selectedFinishedProduct: AssemblyProductItem? = null,
    val producedQuantityInput: String = "1",
    val selectedComponents: List<AssemblyComponentItemState> = emptyList(),
    val notesInput: String = "",
    val componentSearchQuery: String = "",
    val editingAssemblyId: Long? = null,
    val showComponentPickerModal: Boolean = false,
    val showDeleteConfirmDialog: Boolean = false,
    val assemblyToDelete: ItemAssemblyWithComponents? = null,
    val isProcessing: Boolean = false,
    val feedbackMessage: String? = null,
    val isError: Boolean = false,
    val baseCurrencySymbol: String = "ر.ي"
)

class ItemAssemblyViewModel(application: Application) : AndroidViewModel(application) {
    private val db = DokkaniDatabase.getDatabase(application, viewModelScope)
    private val itemAssemblyDao = db.itemAssemblyDao()
    private val productDao = db.productDao()
    private val currencyDao = db.currencyDao()
    private val stockMovementDao = db.stockMovementDao()

    private val _uiState = MutableStateFlow(ItemAssemblyUiState())
    val uiState: StateFlow<ItemAssemblyUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        // 1. جلب العملة الأساسية
        viewModelScope.launch(Dispatchers.IO) {
            currencyDao.getAllCurrencies().collectLatest { currList ->
                val baseCurr = currList.firstOrNull { it.isBaseCurrency }?.symbol ?: "ر.ي"
                _uiState.update { it.copy(baseCurrencySymbol = baseCurr) }
            }
        }

        // 2. جلب جميع الأصناف والمنتجات المسجلة بالمخزن مع وحداتها وأرصدتها الحقيقية
        viewModelScope.launch(Dispatchers.IO) {
            productDao.getProductsWithUnits().collectLatest { productWithUnitsList ->
                val itemsList = productWithUnitsList.map { pwu ->
                    val baseUnit = pwu.units.firstOrNull { it.isBaseUnit } ?: pwu.units.firstOrNull()
                    val stock = stockMovementDao.getTotalStockQuantity(pwu.product.id)
                    AssemblyProductItem(
                        product = pwu.product,
                        baseUnit = baseUnit,
                        currentStock = stock
                    )
                }
                _uiState.update { state ->
                    val updatedFinished = itemsList.find { it.id == state.selectedFinishedProduct?.id } ?: state.selectedFinishedProduct
                    val updatedComps = state.selectedComponents.map { compState ->
                        val latestProd = itemsList.find { it.id == compState.product.id } ?: compState.product
                        compState.copy(productItem = latestProd)
                    }
                    state.copy(
                        products = itemsList,
                        selectedFinishedProduct = updatedFinished,
                        selectedComponents = updatedComps
                    )
                }
            }
        }

        // 3. جلب سجل العمليات المجمعة السابقة
        viewModelScope.launch(Dispatchers.IO) {
            itemAssemblyDao.getAllAssembliesWithComponents().collectLatest { list ->
                _uiState.update { it.copy(assembliesHistory = list) }
            }
        }
    }

    fun selectFinishedProduct(product: AssemblyProductItem?) {
        _uiState.update { it.copy(selectedFinishedProduct = product) }
    }

    fun setProducedQuantity(quantityStr: String) {
        _uiState.update { it.copy(producedQuantityInput = quantityStr) }
    }

    fun setNotesInput(notes: String) {
        _uiState.update { it.copy(notesInput = notes) }
    }

    fun setComponentSearchQuery(query: String) {
        _uiState.update { it.copy(componentSearchQuery = query) }
    }

    fun toggleComponentPickerModal(show: Boolean) {
        _uiState.update { it.copy(showComponentPickerModal = show) }
    }

    fun addComponent(product: AssemblyProductItem) {
        val currentList = _uiState.value.selectedComponents.toMutableList()
        val existingIndex = currentList.indexOfFirst { it.product.id == product.id }
        if (existingIndex != -1) {
            val existing = currentList[existingIndex]
            currentList[existingIndex] = existing.copy(quantityPerUnit = existing.quantityPerUnit + 1.0)
        } else {
            currentList.add(
                AssemblyComponentItemState(
                    productItem = product,
                    quantityPerUnit = 1.0,
                    unitCostPrice = product.costPrice
                )
            )
        }
        _uiState.update { it.copy(selectedComponents = currentList, showComponentPickerModal = false) }
    }

    fun updateComponentQuantity(productId: Long, newQtyPerUnit: Double) {
        val currentList = _uiState.value.selectedComponents.toMutableList()
        val index = currentList.indexOfFirst { it.product.id == productId }
        if (index != -1 && newQtyPerUnit > 0) {
            currentList[index] = currentList[index].copy(quantityPerUnit = newQtyPerUnit)
            _uiState.update { it.copy(selectedComponents = currentList) }
        }
    }

    fun updateComponentCost(productId: Long, newUnitCost: Double) {
        val currentList = _uiState.value.selectedComponents.toMutableList()
        val index = currentList.indexOfFirst { it.product.id == productId }
        if (index != -1 && newUnitCost >= 0) {
            currentList[index] = currentList[index].copy(unitCostPrice = newUnitCost)
            _uiState.update { it.copy(selectedComponents = currentList) }
        }
    }

    fun removeComponent(productId: Long) {
        val currentList = _uiState.value.selectedComponents.filter { it.product.id != productId }
        _uiState.update { it.copy(selectedComponents = currentList) }
    }

    fun resetForm() {
        _uiState.update {
            it.copy(
                selectedFinishedProduct = null,
                producedQuantityInput = "1",
                selectedComponents = emptyList(),
                notesInput = "",
                editingAssemblyId = null,
                isProcessing = false
            )
        }
    }

    fun editAssembly(target: ItemAssemblyWithComponents) {
        val finished = _uiState.value.products.find { it.id == target.assembly.finishedProductId }
        val compStates = target.components.mapNotNull { compEntity ->
            val prodItem = _uiState.value.products.find { it.id == compEntity.componentProductId } ?: return@mapNotNull null
            AssemblyComponentItemState(
                productItem = prodItem,
                unitName = compEntity.unitName,
                quantityPerUnit = compEntity.quantityPerAssembly,
                unitCostPrice = compEntity.unitCostPrice
            )
        }
        _uiState.update {
            it.copy(
                selectedFinishedProduct = finished,
                producedQuantityInput = if (target.assembly.producedQuantity % 1.0 == 0.0) target.assembly.producedQuantity.toInt().toString() else target.assembly.producedQuantity.toString(),
                selectedComponents = compStates,
                notesInput = target.assembly.notes,
                editingAssemblyId = target.assembly.id
            )
        }
    }

    /**
     * اعتماد أو حفظ مسودة عملية تركيب الأصناف
     */
    fun executeSaveAssembly(isDraft: Boolean) {
        val state = _uiState.value
        val finishedProd = state.selectedFinishedProduct
        if (finishedProd == null) {
            _uiState.update { it.copy(feedbackMessage = "يرجى اختيار الصنف النهائي المراد إنتاجه وتجميعه.", isError = true) }
            return
        }

        val producedQty = state.producedQuantityInput.toDoubleOrNull() ?: 0.0
        if (producedQty <= 0) {
            _uiState.update { it.copy(feedbackMessage = "يرجى تحديد كمية إنتاج صحيحة أكبر من الصفر.", isError = true) }
            return
        }

        if (state.selectedComponents.isEmpty()) {
            _uiState.update { it.copy(feedbackMessage = "يرجى إضافة مكون أو صنف فرعي واحد على الأقل داخل التركيب.", isError = true) }
            return
        }

        // فحص والتحقق الصارم من كفاية رصيد المكونات والخامات بالكامل قبل الاعتماد المباشر
        if (!isDraft) {
            for (comp in state.selectedComponents) {
                val requiredTotal = comp.getTotalQuantityUsed(producedQty)
                val availableStock = comp.product.currentStock
                if (availableStock < requiredTotal) {
                    _uiState.update {
                        it.copy(
                            feedbackMessage = "عذراً! الكمية المتاحة بالمخزن من المادة الخام (${comp.product.name}) البالغة (%.2f) غير كافية للكمية المطلوبة للتركيب (%.2f). يرجى توريد الخامة أولاً."
                                .format(Locale.US, availableStock, requiredTotal),
                            isError = true
                        )
                    }
                    return
                }
            }
        }

        _uiState.update { it.copy(isProcessing = true) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                db.withTransaction {
                    val timestamp = System.currentTimeMillis()
                    val assemblyStatus = if (isDraft) AssemblyStatus.DRAFT else AssemblyStatus.APPROVED

                    var assemblyNum = "ASM-2026-%04d".format(itemAssemblyDao.getAssemblyCount() + 1)

                    // 1. إذا كانت هذه العملية تعديلاً لعملية سابقة، نعكس حركات المخزون القديمة أولاً
                    if (state.editingAssemblyId != null) {
                        val prevAssembly = itemAssemblyDao.getAssemblyByIdWithComponents(state.editingAssemblyId)
                        if (prevAssembly != null) {
                            assemblyNum = prevAssembly.assembly.assemblyNumber
                            stockMovementDao.deleteMovementsByReferenceNumber(assemblyNum)
                        }
                    }

                    // 2. حساب التكاليف الإجمالية والفردية
                    val totalCostPerUnit = state.selectedComponents.sumOf { it.getTotalCost(1.0) }
                    val totalAssemblyCost = totalCostPerUnit * producedQty

                    val masterEntity = ItemAssemblyEntity(
                        id = state.editingAssemblyId ?: 0L,
                        assemblyNumber = assemblyNum,
                        finishedProductId = finishedProd.id,
                        finishedProductName = finishedProd.name,
                        finishedProductCode = finishedProd.code,
                        producedQuantity = producedQty,
                        totalAssemblyCost = totalAssemblyCost,
                        costPerUnit = totalCostPerUnit,
                        sellingPricePerUnit = finishedProd.sellPrice,
                        status = assemblyStatus,
                        notes = state.notesInput.ifBlank { "عملية تركيب وتجميع لـ $producedQty ${finishedProd.name}" },
                        date = timestamp
                    )

                    val newAssemblyId = itemAssemblyDao.insertAssembly(masterEntity)

                    // إذا كان تعديلاً، ننظف المكونات القديمة
                    if (state.editingAssemblyId != null) {
                        itemAssemblyDao.deleteComponentsByAssemblyId(state.editingAssemblyId)
                    }

                    val targetAssemblyId = if (state.editingAssemblyId != null) state.editingAssemblyId else newAssemblyId

                    val componentEntities = state.selectedComponents.map { comp ->
                        ItemAssemblyComponentEntity(
                            assemblyId = targetAssemblyId,
                            componentProductId = comp.product.id,
                            componentProductName = comp.product.name,
                            componentProductCode = comp.product.code,
                            unitName = comp.unitName,
                            quantityPerAssembly = comp.quantityPerUnit,
                            totalQuantityUsed = comp.getTotalQuantityUsed(producedQty),
                            unitCostPrice = comp.unitCostPrice,
                            totalCostPrice = comp.getTotalCost(producedQty)
                        )
                    }

                    itemAssemblyDao.insertComponents(componentEntities)

                    // 3. عند الاعتماد النهائي: تسجيل حركات المخزون لتحديث الأرصدة والطبقات
                    if (assemblyStatus == AssemblyStatus.APPROVED) {
                        // أ) توريد الكمية المنتجة للصنف النهائي
                        stockMovementDao.insertMovement(
                            StockMovementEntity(
                                productId = finishedProd.id,
                                productUnitId = finishedProd.baseUnit?.id,
                                movementType = MovementType.INVENTORY_ADJUSTMENT,
                                quantityBaseUnit = +producedQty,
                                remainingQuantityForFifo = +producedQty,
                                unitCostPriceBase = totalCostPerUnit,
                                referenceNumber = assemblyNum,
                                notes = "توريد صنف نهائي مجمع: ${finishedProd.name}"
                            )
                        )

                        // ب) خصم الكميات المستهلكة من الأصناف الفرعية والمواد الخام
                        for (comp in state.selectedComponents) {
                            val totalUsed = comp.getTotalQuantityUsed(producedQty)
                            stockMovementDao.insertMovement(
                                StockMovementEntity(
                                    productId = comp.product.id,
                                    productUnitId = comp.product.baseUnit?.id,
                                    movementType = MovementType.INVENTORY_ADJUSTMENT,
                                    quantityBaseUnit = -totalUsed,
                                    remainingQuantityForFifo = 0.0,
                                    unitCostPriceBase = comp.unitCostPrice,
                                    referenceNumber = assemblyNum,
                                    notes = "خصم مادة خام لإنتاج: ${finishedProd.name}"
                                )
                            )
                        }
                    }
                }

                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        feedbackMessage = if (isDraft) "تم حفظ عملية التركيب كمسودة بنجاح." else "تم اعتماد سند التركيب وتحديث أرصدة المخزون بالكامل بنجاح!",
                        isError = false
                    )
                }
                resetForm()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        feedbackMessage = "خطأ أثناء حفظ عملية التركيب: ${e.localizedMessage}",
                        isError = true
                    )
                }
            }
        }
    }

    fun requestDeleteAssembly(assembly: ItemAssemblyWithComponents) {
        _uiState.update { it.copy(assemblyToDelete = assembly, showDeleteConfirmDialog = true) }
    }

    fun dismissDeleteDialog() {
        _uiState.update { it.copy(assemblyToDelete = null, showDeleteConfirmDialog = false) }
    }

    fun confirmDeleteAssembly() {
        val target = _uiState.value.assemblyToDelete ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                db.withTransaction {
                    // عكس حركات المخزون بحذف سجل الحركة المرجعي
                    stockMovementDao.deleteMovementsByReferenceNumber(target.assembly.assemblyNumber)
                    // حذف الكيانات والتفاصيل
                    itemAssemblyDao.deleteComponentsByAssemblyId(target.assembly.id)
                    itemAssemblyDao.deleteAssemblyById(target.assembly.id)
                }
                _uiState.update {
                    it.copy(
                        showDeleteConfirmDialog = false,
                        assemblyToDelete = null,
                        feedbackMessage = "تم حذف سند التركيب وعكس أثر المخزون بنجاح."
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        showDeleteConfirmDialog = false,
                        feedbackMessage = "خطأ أثناء الحذف: ${e.localizedMessage}",
                        isError = true
                    )
                }
            }
        }
    }

    fun dismissFeedback() {
        _uiState.update { it.copy(feedbackMessage = null, isError = false) }
    }
}
