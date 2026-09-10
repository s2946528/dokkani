package com.example.dokkani.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dokkani.data.local.DokkaniDatabase
import com.example.dokkani.data.local.entities.BatchStatus
import com.example.dokkani.data.local.entities.CostValuationMethod
import com.example.dokkani.data.local.entities.MixedProduceBatchEntity
import com.example.dokkani.data.local.entities.MixedProduceYieldItemEntity
import com.example.dokkani.data.local.entities.ProductWithUnits
import com.example.dokkani.data.local.entities.StockMovementEntity
import com.example.dokkani.data.local.entities.SystemSettingsEntity
import com.example.dokkani.data.repository.DokkaniRepository
import com.example.dokkani.domain.costing.CostCalculationResult
import com.example.dokkani.domain.produce.ProduceQuickCalcSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * حالة واجهة المستخدم لنظام دكاني
 */
data class DokkaniUiState(
    val selectedTab: Int = 0,
    val selectedProductIdForCosting: Long? = null,
    val selectedMethodForCosting: CostValuationMethod = CostValuationMethod.WAC,
    val costingResult: CostCalculationResult? = null,
    val wacComparisonResult: CostCalculationResult? = null,
    val fifoComparisonResult: CostCalculationResult? = null,
    val lastPurchaseComparisonResult: CostCalculationResult? = null,
    val activeLotsForProduct: List<StockMovementEntity> = emptyList(),
    // حاسبة جرد خضار المشكل
    val produceGrossWeightInput: String = "25.0",
    val produceCostInput: String = "90.0",
    val produceExpenseInput: String = "10.0",
    val produceWasteInput: String = "3.0",
    val produceMarginInput: String = "25.0",
    val produceCrateDescription: String = "سحارة خضار مشكل طماطم وخيار وفلفل",
    val produceCalcSummary: ProduceQuickCalcSummary? = null,
    val isSavingProduceBatch: Boolean = false,
    val userNotification: String? = null
)

class DokkaniViewModel(application: Application) : AndroidViewModel(application) {

    val repository: DokkaniRepository

    init {
        val database = DokkaniDatabase.getDatabase(application, viewModelScope)
        repository = DokkaniRepository(database)
    }

    val productsWithUnits: StateFlow<List<ProductWithUnits>> = repository.allProductsWithUnits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currencies = repository.allCurrencies
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val parties = repository.allParties
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentInvoices = repository.recentInvoices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val produceBatches = repository.produceBatchesWithYields
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val systemSettings: StateFlow<SystemSettingsEntity?> = repository.systemSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _uiState = MutableStateFlow(DokkaniUiState())
    val uiState: StateFlow<DokkaniUiState> = _uiState.asStateFlow()

    init {
        // حساب أولي لحاسبة خضار المشكل
        recalculateProduceQuickInventory()

        // مراقبة الأصناف لاختيار أول صنف افتراضياً لحاسبة التكلفة
        viewModelScope.launch {
            productsWithUnits.collect { list ->
                if (list.isNotEmpty() && _uiState.value.selectedProductIdForCosting == null) {
                    val firstProd = list.first()
                    selectProductForCosting(firstProd.product.id)
                }
            }
        }
    }

    fun selectTab(index: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = index)
    }

    fun clearNotification() {
        _uiState.value = _uiState.value.copy(userNotification = null)
    }

    /**
     * اختيار صنف واحتساب تكاليفه بالطرق الثلاث (WAC / FIFO / Last Purchase) للمقارنة المباشرة
     */
    fun selectProductForCosting(productId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(selectedProductIdForCosting = productId)
            recalculateCostingForSelectedProduct()
        }
    }

    fun selectCostingMethod(method: CostValuationMethod) {
        _uiState.value = _uiState.value.copy(selectedMethodForCosting = method)
        viewModelScope.launch {
            recalculateCostingForSelectedProduct()
        }
    }

    private suspend fun recalculateCostingForSelectedProduct() {
        val prodId = _uiState.value.selectedProductIdForCosting ?: return
        try {
            val lots = repository.getAllMovementsForProduct(prodId)

            val wac = repository.calculateCost(prodId, CostValuationMethod.WAC)
            val fifo = repository.calculateCost(prodId, CostValuationMethod.FIFO)
            val lpp = repository.calculateCost(prodId, CostValuationMethod.LAST_PURCHASE_PRICE)

            val currentMethod = _uiState.value.selectedMethodForCosting
            val selectedResult = when (currentMethod) {
                CostValuationMethod.WAC -> wac
                CostValuationMethod.FIFO -> fifo
                CostValuationMethod.LAST_PURCHASE_PRICE -> lpp
            }

            _uiState.value = _uiState.value.copy(
                activeLotsForProduct = lots,
                costingResult = selectedResult,
                wacComparisonResult = wac,
                fifoComparisonResult = fifo,
                lastPurchaseComparisonResult = lpp
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(userNotification = "خطأ في حساب التكلفة: ${e.message}")
        }
    }

    /**
     * تحديث طريقة التقييم المحاسبي المعتمدة في إعدادات نظام دكاني
     */
    fun updateSystemCostingMethod(method: CostValuationMethod) {
        viewModelScope.launch {
            repository.updateCostValuationMethod(method)
            _uiState.value = _uiState.value.copy(
                selectedMethodForCosting = method,
                userNotification = "تم حفظ طريقة التقييم المحاسبي [${method.labelArabic}] كإعداد معتمد لبرنامج دكاني"
            )
            recalculateCostingForSelectedProduct()
        }
    }

    /**
     * إضافة دفعة شراء تجريبية جديدة لإظهار التأثير الفوري على WAC و FIFO و Last Purchase Price
     */
    fun addSimulatedPurchaseBatch(quantity: Double, unitCost: Double) {
        val prodId = _uiState.value.selectedProductIdForCosting ?: return
        viewModelScope.launch {
            val prodWithUnits = productsWithUnits.value.firstOrNull { it.product.id == prodId }
            val baseUnitId = prodWithUnits?.units?.firstOrNull { it.isBaseUnit }?.id
                ?: prodWithUnits?.units?.firstOrNull()?.id ?: 1L

            val ref = "PUR-TEST-${System.currentTimeMillis().toString().takeLast(4)}"
            repository.addPurchaseLotMovement(prodId, baseUnitId, quantity, unitCost, ref)
            _uiState.value = _uiState.value.copy(
                userNotification = "تمت إضافة دفعة توريد جديدة: $quantity كجم بسعر $unitCost ر.س"
            )
            recalculateCostingForSelectedProduct()
        }
    }

    // إدارة مدخلات حاسبة خضار المشكل
    fun updateProduceInputs(
        gross: String? = null,
        cost: String? = null,
        expense: String? = null,
        waste: String? = null,
        margin: String? = null,
        description: String? = null
    ) {
        _uiState.value = _uiState.value.copy(
            produceGrossWeightInput = gross ?: _uiState.value.produceGrossWeightInput,
            produceCostInput = cost ?: _uiState.value.produceCostInput,
            produceExpenseInput = expense ?: _uiState.value.produceExpenseInput,
            produceWasteInput = waste ?: _uiState.value.produceWasteInput,
            produceMarginInput = margin ?: _uiState.value.produceMarginInput,
            produceCrateDescription = description ?: _uiState.value.produceCrateDescription
        )
        recalculateProduceQuickInventory()
    }

    private fun recalculateProduceQuickInventory() {
        val state = _uiState.value
        val gross = state.produceGrossWeightInput.toDoubleOrNull() ?: 25.0
        val cost = state.produceCostInput.toDoubleOrNull() ?: 90.0
        val expense = state.produceExpenseInput.toDoubleOrNull() ?: 0.0
        val waste = state.produceWasteInput.toDoubleOrNull() ?: 0.0
        val margin = state.produceMarginInput.toDoubleOrNull() ?: 25.0

        val summary = repository.calculateProduceQuickInventory(gross, cost, expense, waste, margin)
        _uiState.value = _uiState.value.copy(produceCalcSummary = summary)
    }

    /**
     * حفظ دفعة الجرد السريع للخضار المشكل في قاعدة البيانات
     */
    fun saveProduceBatchToDatabase() {
        val summary = _uiState.value.produceCalcSummary ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSavingProduceBatch = true)
            val batchNumber = "MIX-${System.currentTimeMillis().toString().takeLast(6)}"

            val batchEntity = MixedProduceBatchEntity(
                batchNumber = batchNumber,
                sourceDescription = _uiState.value.produceCrateDescription,
                totalGrossWeightKg = summary.grossWeightKg,
                totalPurchaseCost = summary.totalCrateCost,
                additionalExpense = summary.additionalExpenses,
                wasteWeightKg = summary.wasteWeightKg,
                netSalableWeightKg = summary.netSalableWeightKg,
                wastePercentage = summary.wastePercentage,
                effectiveCostPerKg = summary.effectiveCostPerSalableKg,
                targetProfitMarginPercent = summary.targetProfitMarginPercent,
                suggestedSalePricePerKg = summary.suggestedSalePricePerKg,
                status = BatchStatus.SORTED,
                notes = "تم إجراء الجرد السريع بنجاح واحتساب تكلفة الصافي بعد استبعاد التالف"
            )

            // إنشاء أصناف فرز أولية مرتبطة بالخضار المتوفر في النظام
            val products = productsWithUnits.value.filter { it.product.isWeighted }
            val yieldItems = mutableListOf<MixedProduceYieldItemEntity>()

            if (products.size >= 2) {
                val p1 = products[0]
                val p2 = products[1]
                val halfNet = summary.netSalableWeightKg / 2.0
                yieldItems.add(
                    MixedProduceYieldItemEntity(
                        batchId = 0,
                        productId = p1.product.id,
                        productName = "${p1.product.name} (فرز سحارة)",
                        sortedWeightKg = halfNet,
                        costAllocationRatio = 1.0,
                        calculatedCostPerKg = summary.effectiveCostPerSalableKg,
                        targetSellingPricePerKg = summary.suggestedSalePricePerKg,
                        expectedRevenue = halfNet * summary.suggestedSalePricePerKg
                    )
                )
                yieldItems.add(
                    MixedProduceYieldItemEntity(
                        batchId = 0,
                        productId = p2.product.id,
                        productName = "${p2.product.name} (فرز سحارة)",
                        sortedWeightKg = halfNet,
                        costAllocationRatio = 1.0,
                        calculatedCostPerKg = summary.effectiveCostPerSalableKg,
                        targetSellingPricePerKg = summary.suggestedSalePricePerKg,
                        expectedRevenue = halfNet * summary.suggestedSalePricePerKg
                    )
                )
            }

            repository.saveMixedProduceBatch(batchEntity, yieldItems)

            _uiState.value = _uiState.value.copy(
                isSavingProduceBatch = false,
                userNotification = "تم حفظ دفعة الخضار المشكل رقم ($batchNumber) وتوثيق التكلفة الصافية في قاعدة البيانات بنجاح!"
            )
        }
    }
}
