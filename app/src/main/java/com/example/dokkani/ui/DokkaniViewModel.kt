package com.example.dokkani.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dokkani.data.local.DokkaniDatabase
import com.example.dokkani.data.local.entities.BatchStatus
import com.example.dokkani.data.local.entities.CostValuationMethod
import com.example.dokkani.data.local.entities.MixedProduceBatchEntity
import com.example.dokkani.data.local.entities.MixedProduceYieldItemEntity
import com.example.dokkani.data.local.entities.PaymentMethod
import com.example.dokkani.data.local.entities.ProductUnitEntity
import com.example.dokkani.data.local.entities.ProductWithUnits
import com.example.dokkani.data.local.entities.StockMovementEntity
import com.example.dokkani.data.local.entities.SystemSettingsEntity
import com.example.dokkani.data.repository.DokkaniRepository
import com.example.dokkani.domain.barcode.BarcodeGenerator
import com.example.dokkani.domain.barcode.ScaleBarcodeMode
import com.example.dokkani.domain.barcode.ScaleBarcodeParser
import com.example.dokkani.domain.barcode.ScaleBarcodeResult
import com.example.dokkani.domain.costing.CostCalculationResult
import com.example.dokkani.domain.hardware.BarcodeLabelData
import com.example.dokkani.domain.hardware.BluetoothPrinterManager
import com.example.dokkani.domain.hardware.LabelPaperSize
import com.example.dokkani.domain.hardware.LabelPrintResult
import com.example.dokkani.domain.hardware.PrinterPaperWidth
import com.example.dokkani.domain.hardware.ReceiptPrintData
import com.example.dokkani.data.local.entities.CashShiftEntity
import com.example.dokkani.data.local.entities.ExpenseEntity
import com.example.dokkani.data.local.entities.PaymentVoucherEntity
import com.example.dokkani.domain.cash.CashReconciliationResult
import com.example.dokkani.domain.cash.ExpenseCategories
import com.example.dokkani.domain.credit.CreditNotebookEngine
import com.example.dokkani.domain.credit.CustomerStatementSummary
import com.example.dokkani.domain.reports.FinancialReportsEngine
import com.example.dokkani.domain.reports.InventoryHealthReport
import com.example.dokkani.domain.reports.ProfitAndLossReport
import com.example.dokkani.domain.reports.TopProductsReport
import com.example.dokkani.data.local.entities.LicenseEntity
import com.example.dokkani.domain.security.ActivationPlan
import com.example.dokkani.domain.security.ActivationVerificationResult
import com.example.dokkani.domain.security.AntiTamperGuard
import com.example.dokkani.domain.security.DeviceFingerprintManager
import com.example.dokkani.domain.security.DokkaniKeyGenerator
import com.example.dokkani.domain.security.KeyGeneratorResult
import com.example.dokkani.domain.security.LicenseEvaluationResult
import com.example.dokkani.domain.security.LicenseStatus
import com.example.dokkani.domain.security.OfflineLicenseManager
import com.example.dokkani.domain.pos.CartSummary
import com.example.dokkani.domain.pos.PosCartItem
import com.example.dokkani.domain.pos.PosCheckoutResult
import com.example.dokkani.domain.pos.QuickTileIconType
import com.example.dokkani.domain.pos.QuickTileItem
import com.example.dokkani.domain.produce.ProduceAuditEngine
import com.example.dokkani.domain.produce.ProduceAuditInput
import com.example.dokkani.domain.produce.ProduceAuditResult
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

    // شاشة الجرد الدوري السريع للخضار والفوضويات (Produce Audit & COGS)
    val produceAuditSubTab: Int = 0, // 0 = الجرد الدوري السريع و COGS، 1 = حاسبة سحارة الخضار المشكل
    val selectedProduceProductIdForAudit: Long? = null,
    val produceAuditBeginningQty: String = "15.0",
    val produceAuditBeginningCost: String = "60.0",
    val produceAuditPurchasesQty: String = "40.0",
    val produceAuditPurchasesCost: String = "160.0",
    val produceAuditEndingQty: String = "12.0",
    val produceAuditWasteQty: String = "4.0",
    val produceAuditPosSoldQty: String = "38.0",
    val produceAuditPosRevenue: String = "228.0",
    val produceAuditResult: ProduceAuditResult? = null,
    val isSubmittingProduceAudit: Boolean = false,

    // موديول تصميم وطباعة ملصقات الباركود للوحدات المتعددة
    val labelSelectedProductId: Long? = null,
    val labelSelectedUnitId: Long? = null,
    val labelPaperSize: LabelPaperSize = LabelPaperSize.SIZE_38X25,
    val labelCopies: Int = 1,
    val labelShowStoreName: Boolean = true,
    val labelShowUnitName: Boolean = true,
    val labelShowPrice: Boolean = true,
    val labelShowBarcodeText: Boolean = true,
    val labelShowTaxNote: Boolean = true,
    val labelCustomBarcode: String = "",
    val lastLabelPrintResult: LabelPrintResult? = null,
    val isPrintingLabel: Boolean = false,
    val isGeneratingBarcode: Boolean = false,
    val showLabelConfigDialog: Boolean = false,

    // حالة شاشة نقطة البيع السريعة (POS)
    val cartItems: List<PosCartItem> = emptyList(),
    val posSearchQuery: String = "",
    val selectedPaymentMethod: PaymentMethod = PaymentMethod.CASH,
    val selectedCustomerPartyId: Long? = null,
    val paidAmountInput: String = "",
    val discountInput: String = "0.0",
    val isProcessingCheckout: Boolean = false,
    val lastCheckoutResult: PosCheckoutResult? = null,

    // نوافذ الحوار والملحقات
    val showCheckoutDialog: Boolean = false,
    val showReceiptDialog: Boolean = false,
    val showScaleBarcodeDialog: Boolean = false,
    val detectedScaleBarcode: ScaleBarcodeResult? = null,
    val showOpenPriceDialog: Boolean = false,
    val showHardwareDialog: Boolean = false,
    val showCameraScannerDialog: Boolean = false,
    val openDrawerAutomaticallyOnCash: Boolean = true,

    // =========================================================================
    // حالة شاشة دفتر الشكك والديون والعملاء (Customer & Credit Ledger)
    // =========================================================================
    val creditSearchQuery: String = "",
    val selectedPartyForStatement: Long? = null,
    val customerStatementSummary: CustomerStatementSummary? = null,
    val isLoadingStatement: Boolean = false,
    val showPaymentVoucherDialog: Boolean = false,
    val voucherPartyId: Long? = null,
    val voucherAmountInput: String = "",
    val voucherNotesInput: String = "",
    val voucherPaymentMethod: PaymentMethod = PaymentMethod.CASH,
    val isSubmittingVoucher: Boolean = false,

    // =========================================================================
    // حالة شاشة حركة الخزينة والمصروفات ومطابقة الدرج (Cash & Expenses)
    // =========================================================================
    val cashSubTab: Int = 0, // 0 = المصروفات والنثريات، 1 = مطابقة الصندوق وإغلاق الشفت
    val showAddExpenseDialog: Boolean = false,
    val expenseCategoryInput: String = "كهرباء ومياه",
    val expenseAmountInput: String = "",
    val expensePaidToInput: String = "",
    val expenseNotesInput: String = "",
    val expensePaymentMethod: PaymentMethod = PaymentMethod.CASH,
    val isSubmittingExpense: Boolean = false,
    val drawerOpeningCashInput: String = "200.0",
    val drawerPhysicalCashInput: String = "",
    val drawerShiftNotesInput: String = "",
    val reconciliationResult: CashReconciliationResult? = null,
    val isClosingShift: Boolean = false,
    val lastClosedShift: CashShiftEntity? = null,

    // =========================================================================
    // حالة شاشة لوحة التحكم والتقارير المالية والمخزنية (Dashboard & Reports)
    // =========================================================================
    val reportSubTab: Int = 0, // 0 = الأرباح والخسائر P&L، 1 = حركة الأصناف والربحية، 2 = النواقص وتواريخ الصلاحية
    val selectedReportValuationMethod: CostValuationMethod = CostValuationMethod.WAC,
    val pnlReport: ProfitAndLossReport? = null,
    val topProductsReport: TopProductsReport? = null,
    val inventoryHealthReport: InventoryHealthReport? = null,
    val isLoadingReports: Boolean = false,

    // =========================================================================
    // نظام الحماية، الترخيص والتفعيل بدون إنترنت (Security, Licensing & Anti-Tampering)
    // =========================================================================
    val deviceFingerprint: String = "",
    val licenseEvaluation: LicenseEvaluationResult? = null,
    val selectedPlanForRequest: ActivationPlan = ActivationPlan.MONTHLY_1,
    val generatedChallengeCode: String = "",
    val activationCodeInput: String = "",
    val activationFeedbackMessage: String? = null,
    val isActivating: Boolean = false,
    val showLicenseLockDialog: Boolean = false,
    val licenseLockDialogMessage: String = "",
    val isDeveloperKeyGenExpanded: Boolean = false,
    val keyGenRequestCodeInput: String = "",
    val keyGenSelectedPlan: ActivationPlan = ActivationPlan.LIFETIME,
    val keyGenCustomDaysInput: String = "30",
    val keyGenGeneratedResult: KeyGeneratorResult? = null,

    val userNotification: String? = null
) {
    val cartSummary: CartSummary
        get() {
            val totalQty = cartItems.sumOf { it.quantity }
            val subtotal = cartItems.sumOf { it.totalPrice }
            val discount = discountInput.toDoubleOrNull() ?: 0.0
            val taxable = (subtotal - discount).coerceAtLeast(0.0)
            val taxRate = 0.15 // 15%
            val taxAmount = taxable * taxRate
            val finalTotal = taxable + taxAmount

            return CartSummary(
                itemsCount = cartItems.size,
                totalQuantity = totalQty,
                subtotal = subtotal,
                discount = discount,
                taxableAmount = taxable,
                taxRatePercent = taxRate * 100,
                taxAmount = taxAmount,
                finalTotal = finalTotal
            )
        }
}

class DokkaniViewModel(application: Application) : AndroidViewModel(application) {

    val repository: DokkaniRepository
    val printerManager: BluetoothPrinterManager = BluetoothPrinterManager(application)

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

    val paymentVouchers: StateFlow<List<PaymentVoucherEntity>> = repository.allPaymentVouchers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expenses: StateFlow<List<ExpenseEntity>> = repository.allExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cashShifts: StateFlow<List<CashShiftEntity>> = repository.allCashShifts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(DokkaniUiState())
    val uiState: StateFlow<DokkaniUiState> = _uiState.asStateFlow()

    init {
        val fp = DeviceFingerprintManager.getDeviceFingerprint(application)
        val initialChallenge = OfflineLicenseManager.generateChallengeCode(fp, ActivationPlan.MONTHLY_1)
        _uiState.value = _uiState.value.copy(
            deviceFingerprint = fp,
            generatedChallengeCode = initialChallenge,
            keyGenRequestCodeInput = initialChallenge
        )
        observeAndEvaluateLicense()
    }

    // قائمة الأصناف السريعة بدون باركود (Quick Tiles)
    val defaultQuickTiles = listOf(
        QuickTileItem(
            id = "tile_bread",
            titleArabic = "خبز صامولي",
            subtitle = "كيس 5 حبات",
            price = 1.50,
            category = "مخبوزات",
            iconType = QuickTileIconType.BREAD,
            linkedProductCode = "0005"
        ),
        QuickTileItem(
            id = "tile_tamees",
            titleArabic = "تميس طازج",
            subtitle = "قرص ساخن",
            price = 1.00,
            category = "مخبوزات",
            iconType = QuickTileIconType.TAMEES,
            linkedProductCode = "0006"
        ),
        QuickTileItem(
            id = "tile_mixed_veg",
            titleArabic = "خضار مشكل",
            subtitle = "سحارة مفرزة بالوزن",
            price = 5.50,
            category = "خضار وفواكه",
            iconType = QuickTileIconType.PRODUCE,
            isWeighted = true,
            defaultWeightKg = 1.0,
            linkedProductCode = "PROD-TOMATO"
        ),
        QuickTileItem(
            id = "tile_tomato",
            titleArabic = "طماطم بلدي",
            subtitle = "سعر الكيلو",
            price = 5.50,
            category = "خضار وفواكه",
            iconType = QuickTileIconType.TOMATO,
            isWeighted = true,
            defaultWeightKg = 1.0,
            linkedProductCode = "PROD-TOMATO"
        ),
        QuickTileItem(
            id = "tile_cucumber",
            titleArabic = "خيار محلي",
            subtitle = "سعر الكيلو",
            price = 4.50,
            category = "خضار وفواكه",
            iconType = QuickTileIconType.CUCUMBER,
            isWeighted = true,
            defaultWeightKg = 1.0,
            linkedProductCode = "PROD-CUCUMB"
        ),
        QuickTileItem(
            id = "tile_herbs",
            titleArabic = "حزمة ورقيات",
            subtitle = "بقدونس / كزبرة",
            price = 1.00,
            category = "خضار وفواكه",
            iconType = QuickTileIconType.HERBS,
            linkedProductCode = "0007"
        ),
        QuickTileItem(
            id = "tile_water",
            titleArabic = "ماء ميني",
            subtitle = "330 مل بارد",
            price = 1.00,
            category = "مشروبات",
            iconType = QuickTileIconType.WATER,
            linkedProductCode = "0008"
        ),
        QuickTileItem(
            id = "tile_ice",
            titleArabic = "كيس ثلج",
            subtitle = "مكعبات كبير",
            price = 5.00,
            category = "مثلجات",
            iconType = QuickTileIconType.ICE
        ),
        QuickTileItem(
            id = "tile_open_price",
            titleArabic = "سعر مفتوح",
            subtitle = "تحديد حر من الكاشير",
            price = 0.0,
            category = "متنوع",
            iconType = QuickTileIconType.OPEN_PRICE,
            isOpenPrice = true
        )
    )

    init {
        // حساب أولي لحاسبة خضار المشكل والجرد اليومي
        recalculateProduceQuickInventory()
        recalculateProduceAudit()

        // مراقبة الأصناف لاختيار أول صنف افتراضياً لحاسبة التكلفة والجرد والملصقات
        viewModelScope.launch {
            productsWithUnits.collect { list ->
                if (list.isNotEmpty()) {
                    if (_uiState.value.selectedProductIdForCosting == null) {
                        val firstProd = list.first()
                        selectProductForCosting(firstProd.product.id)
                    }
                    if (_uiState.value.selectedProduceProductIdForAudit == null) {
                        val weighted = list.firstOrNull { it.product.isWeighted } ?: list.first()
                        selectProduceProductForAudit(weighted.product.id)
                    }
                    if (_uiState.value.labelSelectedProductId == null) {
                        val firstProd = list.first()
                        selectProductForLabel(firstProd.product.id)
                    }
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

    // ==========================================
    // إدارة سلة المبيعات لنقطة البيع (POS Cart)
    // ==========================================

    fun setPosSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(posSearchQuery = query)
    }

    fun addProductToCart(
        productWithUnits: ProductWithUnits,
        selectedUnit: ProductUnitEntity? = null,
        quantity: Double = 1.0,
        overridePrice: Double? = null,
        scaleBarcode: String? = null
    ) {
        val unit = selectedUnit
            ?: productWithUnits.units.firstOrNull { it.isBaseUnit }
            ?: productWithUnits.units.firstOrNull()
            ?: return

        val price = overridePrice ?: unit.sellingPrice
        val currentCart = _uiState.value.cartItems.toMutableList()

        // إذا كان الصنف غير موزون وليس باركود ميزان مخصص وموجود مسبقاً، نزيده بمقدار الكمية
        val existingIndex = currentCart.indexOfFirst {
            it.productId == productWithUnits.product.id &&
                    it.unitId == unit.id &&
                    !productWithUnits.product.isWeighted &&
                    it.scaleBarcodeRaw == null
        }

        if (existingIndex >= 0 && !productWithUnits.product.isWeighted) {
            val existing = currentCart[existingIndex]
            val updated = existing.copy(quantity = existing.quantity + quantity)
            currentCart[existingIndex] = updated
        } else {
            currentCart.add(
                PosCartItem(
                    productId = productWithUnits.product.id,
                    productName = productWithUnits.product.name,
                    productCode = productWithUnits.product.code,
                    unitId = unit.id,
                    unitName = unit.unitName,
                    conversionFactor = unit.conversionFactor,
                    unitPrice = price,
                    costPrice = unit.costPrice,
                    quantity = quantity,
                    isWeighted = productWithUnits.product.isWeighted,
                    scaleBarcodeRaw = scaleBarcode
                )
            )
        }

        _uiState.value = _uiState.value.copy(
            cartItems = currentCart,
            posSearchQuery = "",
            userNotification = "تمت إضافة (${productWithUnits.product.name}) إلى السلة"
        )
    }

    fun addQuickTileToCart(tile: QuickTileItem) {
        if (tile.isOpenPrice) {
            _uiState.value = _uiState.value.copy(showOpenPriceDialog = true)
            return
        }

        // محاولة ربط التايل بصنف موجود في قاعدة البيانات
        val matchedProduct = if (tile.linkedProductCode != null) {
            productsWithUnits.value.firstOrNull {
                it.product.code.equals(tile.linkedProductCode, ignoreCase = true) ||
                        it.product.code.endsWith(tile.linkedProductCode)
            }
        } else {
            productsWithUnits.value.firstOrNull {
                it.product.name.contains(tile.titleArabic, ignoreCase = true)
            }
        }

        if (matchedProduct != null) {
            addProductToCart(
                productWithUnits = matchedProduct,
                quantity = if (tile.isWeighted) tile.defaultWeightKg else 1.0,
                overridePrice = if (tile.price > 0.0) tile.price else null
            )
        } else {
            // إضافة بند مباشر
            val currentCart = _uiState.value.cartItems.toMutableList()
            currentCart.add(
                PosCartItem(
                    productId = 0L,
                    productName = tile.titleArabic,
                    productCode = tile.linkedProductCode ?: "QUICK-TILE",
                    unitId = 0L,
                    unitName = if (tile.isWeighted) "كجم" else "حبة",
                    conversionFactor = 1.0,
                    unitPrice = tile.price,
                    costPrice = tile.price * 0.7,
                    quantity = if (tile.isWeighted) tile.defaultWeightKg else 1.0,
                    isWeighted = tile.isWeighted
                )
            )
            _uiState.value = _uiState.value.copy(
                cartItems = currentCart,
                userNotification = "تمت إضافة (${tile.titleArabic}) سريعا إلى السلة"
            )
        }
    }

    fun addCustomOpenPriceItem(name: String, price: Double, quantity: Double = 1.0, isWeighted: Boolean = false) {
        val cleanName = name.ifBlank { "صنف بسعر حر" }
        val currentCart = _uiState.value.cartItems.toMutableList()
        currentCart.add(
            PosCartItem(
                productId = 0L,
                productName = cleanName,
                productCode = "OPEN-PRICE",
                unitId = 0L,
                unitName = if (isWeighted) "كجم" else "طلب",
                conversionFactor = 1.0,
                unitPrice = price,
                costPrice = price * 0.75,
                quantity = quantity,
                isWeighted = isWeighted,
                isCustomOpenPrice = true
            )
        )
        _uiState.value = _uiState.value.copy(
            cartItems = currentCart,
            showOpenPriceDialog = false,
            userNotification = "تمت إضافة ($cleanName) بمبلغ (%.2f ر.س) إلى السلة".format(price)
        )
    }

    fun updateCartItemQuantity(cartItemId: String, newQty: Double) {
        if (newQty <= 0.0001) {
            removeCartItem(cartItemId)
            return
        }
        val currentCart = _uiState.value.cartItems.map { item ->
            if (item.cartItemId == cartItemId) item.copy(quantity = newQty) else item
        }
        _uiState.value = _uiState.value.copy(cartItems = currentCart)
    }

    fun removeCartItem(cartItemId: String) {
        val currentCart = _uiState.value.cartItems.filterNot { it.cartItemId == cartItemId }
        _uiState.value = _uiState.value.copy(cartItems = currentCart)
    }

    fun clearCart() {
        _uiState.value = _uiState.value.copy(
            cartItems = emptyList(),
            paidAmountInput = "",
            discountInput = "0.0",
            selectedCustomerPartyId = null,
            userNotification = "تم تفريغ سلة المبيعات"
        )
    }

    // ==========================================
    // معالجة الباركود وميزان الباركود الإلكتروني (Scale Barcode)
    // ==========================================

    fun handleBarcodeScannedOrEntered(input: String) {
        val clean = input.trim()
        if (clean.isBlank()) return

        // 1. فحص هل هو باركود ميزان إلكتروني (يبدأ بـ 21 أو 20 وطوله 12-13 رقم)
        if (ScaleBarcodeParser.isScaleBarcode(clean)) {
            val scaleResult = ScaleBarcodeParser.parse(clean, assumeWeightBased = true)
            if (scaleResult.isValidScaleBarcode) {
                // البحث عن الصنف المقابل لكود الميزان (مثلاً: 0001 طماطم، 0002 خيار)
                val code = scaleResult.productCode
                val matched = findProductByScaleCode(code)

                if (matched != null) {
                    val weight = scaleResult.weightKg ?: 1.0
                    addProductToCart(
                        productWithUnits = matched,
                        quantity = weight,
                        scaleBarcode = clean
                    )
                    _uiState.value = _uiState.value.copy(
                        posSearchQuery = "",
                        userNotification = "تم مسح باركود الميزان ($clean): ${matched.product.name} بوزن ($weight كجم) بنجاح!"
                    )
                } else {
                    // لم يتم العثور على الصنف مباشرة بكوده، عرض نافذة فك التشفير والتأكيد للكاشير
                    _uiState.value = _uiState.value.copy(
                        detectedScaleBarcode = scaleResult,
                        showScaleBarcodeDialog = true
                    )
                }
                return
            }
        }

        // 2. فحص هل هو باركود وحدة عادي في قاعدة البيانات
        val allProducts = productsWithUnits.value
        val matchedUnitProduct = allProducts.firstOrNull { prod ->
            prod.units.any { it.barcode.equals(clean, ignoreCase = true) }
        }

        if (matchedUnitProduct != null) {
            val unit = matchedUnitProduct.units.first { it.barcode.equals(clean, ignoreCase = true) }
            addProductToCart(matchedUnitProduct, selectedUnit = unit, quantity = 1.0)
            return
        }

        // 3. فحص هل هو كود صنف (SKU)
        val matchedByCode = allProducts.firstOrNull {
            it.product.code.equals(clean, ignoreCase = true) || it.product.code.endsWith(clean)
        }

        if (matchedByCode != null) {
            addProductToCart(matchedByCode, quantity = 1.0)
            return
        }

        // لم نجد مطابقة دقيقة بالباركود، نجعل البحث نصي في القائمة
        _uiState.value = _uiState.value.copy(
            posSearchQuery = clean,
            userNotification = "لا يوجد باركود مطابق لـ ($clean)، جاري تصفية البحث بالاسم"
        )
    }

    private fun findProductByScaleCode(scaleCode: String): ProductWithUnits? {
        val prods = productsWithUnits.value
        val num = scaleCode.toIntOrNull()

        return prods.firstOrNull { p ->
            p.product.code.equals(scaleCode, ignoreCase = true) ||
                    p.product.code.endsWith(scaleCode) ||
                    (num != null && p.product.id == num.toLong()) ||
                    (num == 1 && p.product.code.contains("TOMATO", ignoreCase = true)) ||
                    (num == 2 && p.product.code.contains("CUCUMB", ignoreCase = true)) ||
                    (num == 4 && p.product.name.contains("طماطم", ignoreCase = true)) ||
                    (num == 5 && p.product.code.contains("0005"))
        }
    }

    fun applyDetectedScaleBarcodeToProduct(productWithUnits: ProductWithUnits) {
        val detected = _uiState.value.detectedScaleBarcode ?: return
        val weight = detected.weightKg ?: 1.0
        val price = detected.embeddedPrice

        addProductToCart(
            productWithUnits = productWithUnits,
            quantity = weight,
            overridePrice = price,
            scaleBarcode = detected.rawBarcode
        )

        _uiState.value = _uiState.value.copy(
            showScaleBarcodeDialog = false,
            detectedScaleBarcode = null,
            posSearchQuery = ""
        )
    }

    fun dismissScaleBarcodeDialog() {
        _uiState.value = _uiState.value.copy(
            showScaleBarcodeDialog = false,
            detectedScaleBarcode = null
        )
    }

    // ==========================================
    // إتمام الفاتورة (Checkout) وطرق الدفع
    // ==========================================

    fun openCheckoutDialog() {
        if (_uiState.value.cartItems.isEmpty()) {
            _uiState.value = _uiState.value.copy(userNotification = "السلة فارغة! أضف أصنافاً أولاً.")
            return
        }

        // فحص حالة الترخيص وقفل النظام
        val evaluation = _uiState.value.licenseEvaluation
        if (evaluation?.canCreateInvoice == false) {
            _uiState.value = _uiState.value.copy(
                showLicenseLockDialog = true,
                licenseLockDialogMessage = evaluation.warningMessageArabic ?: "النظام مقفل حالياً! يرجى تفعيل أو تجديد الترخيص لمتابعة البيع."
            )
            return
        }

        val total = _uiState.value.cartSummary.finalTotal
        _uiState.value = _uiState.value.copy(
            showCheckoutDialog = true,
            paidAmountInput = "%.2f".format(total)
        )
    }

    fun dismissCheckoutDialog() {
        _uiState.value = _uiState.value.copy(showCheckoutDialog = false)
    }

    fun selectPaymentMethod(method: PaymentMethod) {
        val total = _uiState.value.cartSummary.finalTotal
        val defaultPaid = when (method) {
            PaymentMethod.CASH -> "%.2f".format(total)
            PaymentMethod.MADA -> "%.2f".format(total)
            PaymentMethod.CREDIT -> "0.00"
            else -> "%.2f".format(total)
        }
        _uiState.value = _uiState.value.copy(
            selectedPaymentMethod = method,
            paidAmountInput = defaultPaid
        )
    }

    fun selectCustomerParty(partyId: Long?) {
        _uiState.value = _uiState.value.copy(selectedCustomerPartyId = partyId)
    }

    fun setPaidAmountInput(amount: String) {
        _uiState.value = _uiState.value.copy(paidAmountInput = amount)
    }

    fun setDiscountInput(discount: String) {
        _uiState.value = _uiState.value.copy(discountInput = discount)
    }

    fun setOpenDrawerAutomatically(enable: Boolean) {
        _uiState.value = _uiState.value.copy(openDrawerAutomaticallyOnCash = enable)
    }

    fun processCheckout() {
        val state = _uiState.value
        val items = state.cartItems
        if (items.isEmpty()) return

        // فحص حالة الترخيص وقفل النظام
        val evaluation = state.licenseEvaluation
        if (evaluation?.canCreateInvoice == false) {
            _uiState.value = _uiState.value.copy(
                showCheckoutDialog = false,
                showLicenseLockDialog = true,
                licenseLockDialogMessage = evaluation.warningMessageArabic ?: "النظام مقفل حالياً! يرجى تفعيل أو تجديد الترخيص."
            )
            return
        }

        val method = state.selectedPaymentMethod
        val partyId = state.selectedCustomerPartyId
        val discount = state.discountInput.toDoubleOrNull() ?: 0.0
        val paid = state.paidAmountInput.toDoubleOrNull() ?: state.cartSummary.finalTotal

        if (method == PaymentMethod.CREDIT && partyId == null) {
            _uiState.value = _uiState.value.copy(
                userNotification = "تنبيه: يجب اختيار العميل (صاحب دفتر الحساب) لإتمام البيع الآجل (الشكك)!"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessingCheckout = true)
            try {
                val checkoutResult = repository.processPosSale(
                    cartItems = items,
                    paymentMethod = method,
                    partyId = partyId,
                    paidAmount = paid,
                    discount = discount,
                    notes = "فاتورة بيع نقطة بيع POS"
                )

                // تحديث وقت النظام المعتمد لمكافحة التلاعب بالساعة
                repository.updateLastKnownTime(System.currentTimeMillis())

                // في حال الدفع نقداً وطُلب فتح الدرج تلقائياً
                if (method == PaymentMethod.CASH && state.openDrawerAutomaticallyOnCash) {
                    printerManager.openCashDrawer("دفع نقدي لفاتورة ${checkoutResult.invoiceNumber}")
                }

                // تجهيز الفاتورة للطباعة الحرارية
                printerManager.printReceipt(
                    receiptData = checkoutResult.receiptData,
                    openDrawerIfCash = false // تم فتحه مسبقاً إذا لزم
                )

                _uiState.value = _uiState.value.copy(
                    isProcessingCheckout = false,
                    showCheckoutDialog = false,
                    showReceiptDialog = true,
                    lastCheckoutResult = checkoutResult,
                    cartItems = emptyList(),
                    posSearchQuery = "",
                    paidAmountInput = "",
                    discountInput = "0.0",
                    selectedCustomerPartyId = null,
                    userNotification = "تم إتمام الفاتورة (${checkoutResult.invoiceNumber}) بنجاح!"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isProcessingCheckout = false,
                    userNotification = "خطأ في إتمام الفاتورة: ${e.message}"
                )
            }
        }
    }

    fun dismissReceiptDialog() {
        _uiState.value = _uiState.value.copy(
            showReceiptDialog = false,
            lastCheckoutResult = null
        )
    }

    // ==========================================
    // التكامل مع الأجهزة المرفقة (Hardware / Printer / Cash Drawer)
    // ==========================================

    fun openHardwareDialog() {
        _uiState.value = _uiState.value.copy(showHardwareDialog = true)
    }

    fun dismissHardwareDialog() {
        _uiState.value = _uiState.value.copy(showHardwareDialog = false)
    }

    fun openCashDrawerManual() {
        viewModelScope.launch {
            val res = printerManager.openCashDrawer("فتح يدوي من لوحة الكاشير")
            _uiState.value = _uiState.value.copy(
                userNotification = res.message
            )
        }
    }

    fun printCurrentReceiptAgain() {
        val result = _uiState.value.lastCheckoutResult ?: return
        viewModelScope.launch {
            val res = printerManager.printReceipt(result.receiptData, openDrawerIfCash = false)
            _uiState.value = _uiState.value.copy(userNotification = res.message)
        }
    }

    fun setPaperWidth(width: PrinterPaperWidth) {
        printerManager.setPaperWidth(width)
    }

    fun connectPrinter(address: String) {
        viewModelScope.launch {
            val ok = printerManager.connectToPrinter(address)
            _uiState.value = _uiState.value.copy(
                userNotification = if (ok) "تم الاتصال بالطابعة بنجاح!" else "تعذر الاتصال بالطابعة"
            )
        }
    }

    fun disconnectPrinter() {
        printerManager.disconnect()
    }

    fun setShowCameraScannerDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showCameraScannerDialog = show)
    }

    fun setShowOpenPriceDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showOpenPriceDialog = show)
    }

    // ==========================================
    // محرك احتساب التكلفة (Costing Engine)
    // ==========================================

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
            _uiState.value = _uiState.value.copy(
                userNotification = "خطأ في احتساب التكلفة: ${e.message}"
            )
        }
    }

    fun simulateNewPurchaseLot(
        unitId: Long,
        quantity: Double,
        unitCost: Double,
        invoiceRef: String
    ) {
        val prodId = _uiState.value.selectedProductIdForCosting ?: return
        viewModelScope.launch {
            repository.addPurchaseLotMovement(prodId, unitId, quantity, unitCost, invoiceRef)
            recalculateCostingForSelectedProduct()
            _uiState.value = _uiState.value.copy(
                userNotification = "تمت إضافة دفعة شراء تجريبية (كمية: $quantity بسعر: $unitCost) وتحديث تكلفة المخزون فورياً!"
            )
        }
    }

    // ==========================================
    // حاسبة جرد خضار المشكل (Produce Quick Inventory)
    // ==========================================

    fun updateProduceGrossWeight(weight: String) {
        _uiState.value = _uiState.value.copy(produceGrossWeightInput = weight)
        recalculateProduceQuickInventory()
    }

    fun updateProduceCost(cost: String) {
        _uiState.value = _uiState.value.copy(produceCostInput = cost)
        recalculateProduceQuickInventory()
    }

    fun updateProduceExpense(expense: String) {
        _uiState.value = _uiState.value.copy(produceExpenseInput = expense)
        recalculateProduceQuickInventory()
    }

    fun updateProduceWaste(waste: String) {
        _uiState.value = _uiState.value.copy(produceWasteInput = waste)
        recalculateProduceQuickInventory()
    }

    fun updateProduceMargin(margin: String) {
        _uiState.value = _uiState.value.copy(produceMarginInput = margin)
        recalculateProduceQuickInventory()
    }

    fun updateProduceCrateDescription(desc: String) {
        _uiState.value = _uiState.value.copy(produceCrateDescription = desc)
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

    fun updateSystemCostingMethod(method: CostValuationMethod) {
        viewModelScope.launch {
            repository.updateCostValuationMethod(method)
            selectCostingMethod(method)
            _uiState.value = _uiState.value.copy(
                userNotification = "تم تحديث معيار تقييم المخزون في النظام إلى: ${method.name}"
            )
        }
    }

    fun addSimulatedPurchaseBatch(quantity: Double, unitCost: Double) {
        val prodId = _uiState.value.selectedProductIdForCosting ?: return
        val currentProd = productsWithUnits.value.firstOrNull { it.product.id == prodId }
        val unitId = currentProd?.units?.firstOrNull()?.id ?: 1L
        simulateNewPurchaseLot(
            unitId = unitId,
            quantity = quantity,
            unitCost = unitCost,
            invoiceRef = "SIM-${System.currentTimeMillis().toString().takeLast(4)}"
        )
    }

    fun updateProduceInputs(
        gross: String?,
        cost: String?,
        expense: String?,
        waste: String?,
        margin: String?,
        desc: String?
    ) {
        _uiState.value = _uiState.value.copy(
            produceGrossWeightInput = gross ?: _uiState.value.produceGrossWeightInput,
            produceCostInput = cost ?: _uiState.value.produceCostInput,
            produceExpenseInput = expense ?: _uiState.value.produceExpenseInput,
            produceWasteInput = waste ?: _uiState.value.produceWasteInput,
            produceMarginInput = margin ?: _uiState.value.produceMarginInput,
            produceCrateDescription = desc ?: _uiState.value.produceCrateDescription
        )
        recalculateProduceQuickInventory()
    }

    // ==========================================
    // الجرد الدوري السريع للخضار وحساب COGS والتسوية الصامتة
    // ==========================================

    fun selectProduceAuditSubTab(index: Int) {
        _uiState.value = _uiState.value.copy(produceAuditSubTab = index)
    }

    fun selectProduceProductForAudit(productId: Long) {
        _uiState.value = _uiState.value.copy(selectedProduceProductIdForAudit = productId)
        recalculateProduceAudit()
    }

    fun updateProduceAuditInputs(
        begQty: String? = null,
        begCost: String? = null,
        purQty: String? = null,
        purCost: String? = null,
        endingQty: String? = null,
        wasteQty: String? = null,
        posSoldQty: String? = null,
        posRev: String? = null
    ) {
        _uiState.value = _uiState.value.copy(
            produceAuditBeginningQty = begQty ?: _uiState.value.produceAuditBeginningQty,
            produceAuditBeginningCost = begCost ?: _uiState.value.produceAuditBeginningCost,
            produceAuditPurchasesQty = purQty ?: _uiState.value.produceAuditPurchasesQty,
            produceAuditPurchasesCost = purCost ?: _uiState.value.produceAuditPurchasesCost,
            produceAuditEndingQty = endingQty ?: _uiState.value.produceAuditEndingQty,
            produceAuditWasteQty = wasteQty ?: _uiState.value.produceAuditWasteQty,
            produceAuditPosSoldQty = posSoldQty ?: _uiState.value.produceAuditPosSoldQty,
            produceAuditPosRevenue = posRev ?: _uiState.value.produceAuditPosRevenue
        )
        recalculateProduceAudit()
    }

    fun recalculateProduceAudit() {
        val state = _uiState.value
        val prodId = state.selectedProduceProductIdForAudit ?: 1L
        val prod = productsWithUnits.value.firstOrNull { it.product.id == prodId }
        val prodName = prod?.product?.name ?: "طماطم بلدي"

        val input = ProduceAuditInput(
            productId = prodId,
            productName = prodName,
            beginningInventoryQty = state.produceAuditBeginningQty.toDoubleOrNull() ?: 15.0,
            beginningInventoryCost = state.produceAuditBeginningCost.toDoubleOrNull() ?: 60.0,
            purchasesQty = state.produceAuditPurchasesQty.toDoubleOrNull() ?: 40.0,
            purchasesCost = state.produceAuditPurchasesCost.toDoubleOrNull() ?: 160.0,
            estimatedEndingInventoryQty = state.produceAuditEndingQty.toDoubleOrNull() ?: 12.0,
            wasteQty = state.produceAuditWasteQty.toDoubleOrNull() ?: 4.0,
            posRecordedSoldQty = state.produceAuditPosSoldQty.toDoubleOrNull() ?: 38.0,
            posRecordedRevenue = state.produceAuditPosRevenue.toDoubleOrNull() ?: 228.0
        )

        val result = ProduceAuditEngine.computeProduceAudit(input)
        _uiState.value = _uiState.value.copy(produceAuditResult = result)
    }

    /**
     * ترحيل وتسجيل قيود التسوية المخزنية الصامتة لإغلاق اليوم في قاعدة البيانات
     */
    fun commitProduceAuditSilentAdjustments() {
        val result = _uiState.value.produceAuditResult ?: return
        if (result.generatedAdjustments.isEmpty()) {
            _uiState.value = _uiState.value.copy(userNotification = "لا توجد فروقات أو توالف تتطلب قيود تسوية صامتة.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmittingProduceAudit = true)
            try {
                val count = repository.applySilentInventoryAdjustments(result.generatedAdjustments)
                _uiState.value = _uiState.value.copy(
                    isSubmittingProduceAudit = false,
                    userNotification = "تم ترحيل وتسجيل ($count) قيود تسوية صامتة للصنف [${result.productName}] وتحديث رصيد الرف بنجاح!"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSubmittingProduceAudit = false,
                    userNotification = "تعذر تسجيل التسوية الصامتة: ${e.message}"
                )
            }
        }
    }

    // ==========================================
    // موديول تصميم وطباعة ملصقات الباركود (Barcode Label Printer)
    // ==========================================

    fun selectProductForLabel(productId: Long) {
        val prod = productsWithUnits.value.firstOrNull { it.product.id == productId }
        val firstUnit = prod?.units?.firstOrNull()
        _uiState.value = _uiState.value.copy(
            labelSelectedProductId = productId,
            labelSelectedUnitId = firstUnit?.id,
            labelCustomBarcode = firstUnit?.barcode ?: ""
        )
    }

    fun selectUnitForLabel(unitId: Long) {
        val prodId = _uiState.value.labelSelectedProductId ?: return
        val prod = productsWithUnits.value.firstOrNull { it.product.id == prodId }
        val unit = prod?.units?.firstOrNull { it.id == unitId }
        _uiState.value = _uiState.value.copy(
            labelSelectedUnitId = unitId,
            labelCustomBarcode = unit?.barcode ?: ""
        )
    }

    fun updateLabelPaperSize(size: LabelPaperSize) {
        _uiState.value = _uiState.value.copy(labelPaperSize = size)
    }

    fun updateLabelCopies(copies: Int) {
        _uiState.value = _uiState.value.copy(labelCopies = copies.coerceIn(1, 100))
    }

    fun updateCustomBarcode(barcode: String) {
        _uiState.value = _uiState.value.copy(labelCustomBarcode = barcode)
    }

    fun toggleLabelOption(
        showStoreName: Boolean? = null,
        showUnitName: Boolean? = null,
        showPrice: Boolean? = null,
        showBarcodeText: Boolean? = null,
        showTaxNote: Boolean? = null
    ) {
        _uiState.value = _uiState.value.copy(
            labelShowStoreName = showStoreName ?: _uiState.value.labelShowStoreName,
            labelShowUnitName = showUnitName ?: _uiState.value.labelShowUnitName,
            labelShowPrice = showPrice ?: _uiState.value.labelShowPrice,
            labelShowBarcodeText = showBarcodeText ?: _uiState.value.labelShowBarcodeText,
            labelShowTaxNote = showTaxNote ?: _uiState.value.labelShowTaxNote
        )
    }

    /**
     * توليد باركود فريد للوحدة المحددة وتخزينه في جدول product_units
     */
    fun generateUniqueBarcodeForCurrentUnit() {
        val prodId = _uiState.value.labelSelectedProductId ?: return
        val unitId = _uiState.value.labelSelectedUnitId ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGeneratingBarcode = true)
            try {
                val newBarcode = repository.generateAndSaveUniqueBarcodeForUnit(prodId, unitId)
                _uiState.value = _uiState.value.copy(
                    isGeneratingBarcode = false,
                    labelCustomBarcode = newBarcode,
                    userNotification = "تم توليد وتخزين باركود فريد قياسي ($newBarcode) في جدول الوحدات بنجاح!"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isGeneratingBarcode = false,
                    userNotification = "خطأ في توليد الباركود: ${e.message}"
                )
            }
        }
    }

    /**
     * إرسال أمر طباعة الملصق لطابعة البلوتوث عبر لغة TSPL
     */
    fun printBarcodeLabel() {
        val prodId = _uiState.value.labelSelectedProductId ?: return
        val unitId = _uiState.value.labelSelectedUnitId ?: return
        val prod = productsWithUnits.value.firstOrNull { it.product.id == prodId } ?: return
        val unit = prod.units.firstOrNull { it.id == unitId } ?: return
        val settings = systemSettings.value ?: SystemSettingsEntity()

        val barcodeToPrint = _uiState.value.labelCustomBarcode.ifBlank {
            unit.barcode.ifBlank {
                BarcodeGenerator.generateUniqueSingleUnitBarcode(prodId, unitId)
            }
        }

        val labelData = BarcodeLabelData(
            storeName = settings.storeName,
            productName = prod.product.name,
            unitName = unit.unitName,
            barcode = barcodeToPrint,
            price = unit.sellingPrice,
            currencySymbol = "ر.س",
            isPriceInclusiveTax = true,
            taxRatePercent = settings.defaultTaxRate * 100.0,
            size = _uiState.value.labelPaperSize,
            copies = _uiState.value.labelCopies,
            showStoreName = _uiState.value.labelShowStoreName,
            showUnitName = _uiState.value.labelShowUnitName,
            showPrice = _uiState.value.labelShowPrice,
            showBarcodeText = _uiState.value.labelShowBarcodeText,
            showTaxNote = _uiState.value.labelShowTaxNote
        )

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPrintingLabel = true)
            val result = printerManager.printBarcodeLabel(labelData)
            _uiState.value = _uiState.value.copy(
                isPrintingLabel = false,
                lastLabelPrintResult = result,
                userNotification = result.message
            )
        }
    }

    /**
     * فتح شاشة طباعة الملصق مباشرة لصنف ووحدة معينة من أي شاشة
     */
    fun openLabelPrinterForProduct(productId: Long, unitId: Long? = null) {
        selectProductForLabel(productId)
        if (unitId != null) {
            selectUnitForLabel(unitId)
        }
        selectTab(6) // التبويب المخصص للملصقات والباركود
    }

    // =========================================================================
    // إدارة الديون ودفتر الشكك وسندات القبض (Credit & Customer Ledger)
    // =========================================================================

    fun updateCreditSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(creditSearchQuery = query)
    }

    fun selectPartyForStatement(partyId: Long?) {
        _uiState.value = _uiState.value.copy(
            selectedPartyForStatement = partyId,
            customerStatementSummary = null
        )
        if (partyId != null) {
            loadCustomerStatement(partyId)
        }
    }

    fun loadCustomerStatement(partyId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingStatement = true)
            try {
                val statement = repository.getCustomerStatement(partyId)
                _uiState.value = _uiState.value.copy(
                    customerStatementSummary = statement,
                    isLoadingStatement = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingStatement = false,
                    userNotification = "خطأ أثناء تحميل كشف الحساب: ${e.message}"
                )
            }
        }
    }

    fun openPaymentVoucherDialog(partyId: Long) {
        val party = parties.value.firstOrNull { it.id == partyId }
        val remaining = party?.currentBalance ?: 0.0
        _uiState.value = _uiState.value.copy(
            showPaymentVoucherDialog = true,
            voucherPartyId = partyId,
            voucherAmountInput = if (remaining > 0) remaining.toString() else "",
            voucherNotesInput = "سداد دفعة على الحساب من ${party?.name ?: "العميل"}",
            voucherPaymentMethod = PaymentMethod.CASH
        )
    }

    fun dismissPaymentVoucherDialog() {
        _uiState.value = _uiState.value.copy(
            showPaymentVoucherDialog = false,
            voucherPartyId = null,
            voucherAmountInput = "",
            voucherNotesInput = ""
        )
    }

    fun updateVoucherInputs(amount: String, notes: String, method: PaymentMethod) {
        _uiState.value = _uiState.value.copy(
            voucherAmountInput = amount,
            voucherNotesInput = notes,
            voucherPaymentMethod = method
        )
    }

    fun submitPaymentVoucher() {
        val partyId = _uiState.value.voucherPartyId ?: return
        val amount = _uiState.value.voucherAmountInput.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            _uiState.value = _uiState.value.copy(userNotification = "يرجى إدخال مبلغ سداد صحيح أكبر من 0")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmittingVoucher = true)
            try {
                val voucher = repository.recordPaymentVoucher(
                    partyId = partyId,
                    amount = amount,
                    paymentMethod = _uiState.value.voucherPaymentMethod,
                    notes = _uiState.value.voucherNotesInput,
                    receivedBy = "كاشير 1"
                )

                _uiState.value = _uiState.value.copy(
                    isSubmittingVoucher = false,
                    showPaymentVoucherDialog = false,
                    userNotification = "تم تسجيل سند القبض ${voucher.voucherNumber} بمبلغ ${"%.2f".format(amount)} ر.س وتحديث رصيد العميل بنجاح"
                )

                // تحديث كشف الحساب إذا كان معروضاً حالياً
                if (_uiState.value.selectedPartyForStatement == partyId) {
                    loadCustomerStatement(partyId)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSubmittingVoucher = false,
                    userNotification = "فشل تسجيل سند القبض: ${e.message}"
                )
            }
        }
    }

    fun sendWhatsAppDebtReminder(context: Context, phone: String, messageText: String) {
        try {
            // تنظيف رقم الهاتف وإضافة مفتاح الدولة 966 إن لزم
            var cleanPhone = phone.trim().replace("+", "").replace(" ", "")
            if (cleanPhone.startsWith("05")) {
                cleanPhone = "966" + cleanPhone.substring(1)
            } else if (!cleanPhone.startsWith("966") && cleanPhone.startsWith("5")) {
                cleanPhone = "966$cleanPhone"
            }

            val url = "https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(messageText)}"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // إذا لم يكن واتساب مثبتاً نفتح مشاركة عامة للنص
            try {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, messageText)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(Intent.createChooser(shareIntent, "إرسال تذكير السداد"))
            } catch (ex: Exception) {
                _uiState.value = _uiState.value.copy(userNotification = "تعذر إرسال التذكير: ${ex.message}")
            }
        }
    }

    // =========================================================================
    // إدارة حركة الخزينة والمصروفات ومطابقة الشفت (Cash & Expenses)
    // =========================================================================

    fun selectCashSubTab(tab: Int) {
        _uiState.value = _uiState.value.copy(cashSubTab = tab)
        if (tab == 1) {
            // تحديث مطابقة النقدية فورياً عند فتح تبويب مطابقة الشفت
            calculateDrawerReconciliation()
        }
    }

    fun openAddExpenseDialog() {
        _uiState.value = _uiState.value.copy(
            showAddExpenseDialog = true,
            expenseCategoryInput = "نظافة ومستلزمات وأكياس",
            expenseAmountInput = "",
            expensePaidToInput = "",
            expenseNotesInput = "",
            expensePaymentMethod = PaymentMethod.CASH
        )
    }

    fun dismissAddExpenseDialog() {
        _uiState.value = _uiState.value.copy(showAddExpenseDialog = false)
    }

    fun updateExpenseInputs(
        category: String,
        amount: String,
        paidTo: String,
        notes: String,
        method: PaymentMethod
    ) {
        _uiState.value = _uiState.value.copy(
            expenseCategoryInput = category,
            expenseAmountInput = amount,
            expensePaidToInput = paidTo,
            expenseNotesInput = notes,
            expensePaymentMethod = method
        )
    }

    fun submitExpense() {
        val amount = _uiState.value.expenseAmountInput.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            _uiState.value = _uiState.value.copy(userNotification = "يرجى إدخال مبلغ مصروف صحيح أكبر من 0")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmittingExpense = true)
            try {
                val exp = repository.recordExpense(
                    category = _uiState.value.expenseCategoryInput,
                    amount = amount,
                    paymentMethod = _uiState.value.expensePaymentMethod,
                    paidTo = _uiState.value.expensePaidToInput,
                    notes = _uiState.value.expenseNotesInput,
                    recordedBy = "كاشير 1"
                )

                _uiState.value = _uiState.value.copy(
                    isSubmittingExpense = false,
                    showAddExpenseDialog = false,
                    userNotification = "تم تسجيل المصروف ${exp.expenseNumber} بقيمة ${"%.2f".format(amount)} ر.س بنجاح"
                )

                // تحديث المطابقة إذا كانت شاشة المطابقة مفتوحة
                calculateDrawerReconciliation()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSubmittingExpense = false,
                    userNotification = "فشل تسجيل المصروف: ${e.message}"
                )
            }
        }
    }

    fun updateDrawerInputs(openingCash: String, physicalCash: String, notes: String) {
        _uiState.value = _uiState.value.copy(
            drawerOpeningCashInput = openingCash,
            drawerPhysicalCashInput = physicalCash,
            drawerShiftNotesInput = notes
        )
    }

    fun calculateDrawerReconciliation() {
        viewModelScope.launch {
            val opening = _uiState.value.drawerOpeningCashInput.toDoubleOrNull() ?: 200.0
            val physical = _uiState.value.drawerPhysicalCashInput.toDoubleOrNull() ?: 0.0
            val result = repository.calculateCashReconciliation(
                openingCash = opening,
                actualPhysicalCash = physical
            )
            _uiState.value = _uiState.value.copy(reconciliationResult = result)
        }
    }

    fun closeShiftAndSave() {
        val opening = _uiState.value.drawerOpeningCashInput.toDoubleOrNull() ?: 200.0
        val physical = _uiState.value.drawerPhysicalCashInput.toDoubleOrNull()
        if (physical == null) {
            _uiState.value = _uiState.value.copy(userNotification = "يرجى إدخال النقدية الفعلية المجرودة في الدرج أولاً!")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isClosingShift = true)
            try {
                val shift = repository.closeShiftAndSave(
                    openingCash = opening,
                    actualPhysicalCash = physical,
                    cashierName = "كاشير 1",
                    notes = _uiState.value.drawerShiftNotesInput
                )
                _uiState.value = _uiState.value.copy(
                    isClosingShift = false,
                    lastClosedShift = shift,
                    userNotification = "تم إغلاق الشفت ${shift.shiftNumber} وحفظ مطابقة النقدية بنجاح"
                )
                calculateDrawerReconciliation()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isClosingShift = false,
                    userNotification = "فشل إغلاق الشفت: ${e.message}"
                )
            }
        }
    }

    // =========================================================================
    // إدارة لوحة التحكم والتقارير المالية والمخزنية (Dashboard & Reports)
    // =========================================================================

    fun selectReportSubTab(tab: Int) {
        _uiState.value = _uiState.value.copy(reportSubTab = tab)
        loadAllReports()
    }

    fun selectReportValuationMethod(method: CostValuationMethod) {
        _uiState.value = _uiState.value.copy(selectedReportValuationMethod = method)
        loadAllReports(method)
    }

    fun loadAllReports(methodOverride: CostValuationMethod? = null) {
        val method = methodOverride ?: _uiState.value.selectedReportValuationMethod
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingReports = true)
            try {
                val pnl = repository.generateProfitAndLossReport(method)
                val topProds = repository.generateTopProductsReport(method)
                val invHealth = repository.generateInventoryHealthReport()

                _uiState.value = _uiState.value.copy(
                    isLoadingReports = false,
                    pnlReport = pnl,
                    topProductsReport = topProds,
                    inventoryHealthReport = invHealth
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingReports = false,
                    userNotification = "خطأ أثناء توليد التقارير: ${e.message}"
                )
            }
        }
    }

    // =========================================================================
    // إدارة الترخيص والحماية بدون إنترنت (Security, Licensing & Anti-Tampering)
    // =========================================================================

    private fun observeAndEvaluateLicense() {
        viewModelScope.launch {
            val fp = DeviceFingerprintManager.getDeviceFingerprint(getApplication())
            var currentLicense = repository.getLicenseSync()
            if (currentLicense == null) {
                currentLicense = LicenseEntity(
                    id = 1,
                    status = LicenseStatus.TRIAL,
                    isLifetime = false,
                    maxAllowedInvoices = 500,
                    deviceFingerprint = fp,
                    lastKnownSystemTimestamp = System.currentTimeMillis()
                )
                repository.saveLicense(currentLicense)
            }

            kotlinx.coroutines.flow.combine(
                repository.licenseFlow,
                repository.totalInvoicesCountFlow
            ) { license, invoiceCount ->
                val lic = license ?: currentLicense!!
                val latestInvTime = repository.getLatestInvoiceTimestampSync() ?: 0L

                // 1. فحص مكافحة التلاعب بالوقت
                val tamperCheck = AntiTamperGuard.verifyTimeIntegrity(
                    currentSystemTime = System.currentTimeMillis(),
                    latestInvoiceTime = latestInvTime,
                    lastKnownSystemTime = lic.lastKnownSystemTimestamp
                )

                if (tamperCheck.isTampered && !lic.isTimeTampered) {
                    repository.updateTamperState(true, tamperCheck.reasonArabic)
                }

                val effectiveTampered = lic.isTimeTampered || tamperCheck.isTampered

                // 2. تقييم الترخيص
                val evaluation = OfflineLicenseManager.evaluateLicense(
                    status = if (effectiveTampered) LicenseStatus.TAMPERED else lic.status,
                    isLifetime = lic.isLifetime,
                    expiryTimestamp = lic.expiryTimestamp,
                    maxAllowedInvoices = lic.maxAllowedInvoices,
                    totalInvoicesIssued = invoiceCount,
                    deviceFingerprint = fp,
                    isTimeTampered = effectiveTampered
                )

                evaluation
            }.collect { evaluation ->
                _uiState.value = _uiState.value.copy(
                    licenseEvaluation = evaluation
                )
            }
        }
    }

    fun selectPlanForRequest(plan: ActivationPlan) {
        val fp = _uiState.value.deviceFingerprint
        val newCode = OfflineLicenseManager.generateChallengeCode(fp, plan)
        _uiState.value = _uiState.value.copy(
            selectedPlanForRequest = plan,
            generatedChallengeCode = newCode,
            keyGenRequestCodeInput = newCode
        )
    }

    fun refreshChallengeCode() {
        selectPlanForRequest(_uiState.value.selectedPlanForRequest)
    }

    fun updateActivationCodeInput(code: String) {
        _uiState.value = _uiState.value.copy(
            activationCodeInput = code,
            activationFeedbackMessage = null
        )
    }

    fun applyActivationCode() {
        val code = _uiState.value.activationCodeInput.trim()
        if (code.isBlank()) {
            _uiState.value = _uiState.value.copy(activationFeedbackMessage = "يرجى إدخال كود التفعيل أولاً")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isActivating = true)
            try {
                val fp = _uiState.value.deviceFingerprint
                val invCount = repository.getTotalInvoicesCountSync()
                val result = OfflineLicenseManager.verifyAndApplyActivationCode(code, fp, invCount)

                if (result.isSuccess) {
                    val currentLicense = repository.getLicenseSync() ?: LicenseEntity(deviceFingerprint = fp)
                    val updated = currentLicense.copy(
                        status = result.newStatus,
                        isLifetime = result.isLifetime,
                        expiryTimestamp = result.expiryTimestamp,
                        maxAllowedInvoices = result.maxAllowedInvoices,
                        activatedAt = System.currentTimeMillis(),
                        lastKnownSystemTimestamp = System.currentTimeMillis(),
                        isTimeTampered = false,
                        tamperReason = null,
                        appliedActivationCode = code,
                        activePlanCode = result.plan.code
                    )
                    repository.saveLicense(updated)

                    _uiState.value = _uiState.value.copy(
                        isActivating = false,
                        activationCodeInput = "",
                        activationFeedbackMessage = result.messageArabic,
                        userNotification = result.messageArabic,
                        showLicenseLockDialog = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isActivating = false,
                        activationFeedbackMessage = result.messageArabic,
                        userNotification = result.messageArabic
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isActivating = false,
                    activationFeedbackMessage = "خطأ أثناء معالجة كود التفعيل: ${e.message}"
                )
            }
        }
    }

    fun dismissLicenseLockDialog() {
        _uiState.value = _uiState.value.copy(showLicenseLockDialog = false)
    }

    fun toggleDeveloperKeyGen() {
        _uiState.value = _uiState.value.copy(isDeveloperKeyGenExpanded = !_uiState.value.isDeveloperKeyGenExpanded)
    }

    fun updateKeyGenRequestInput(input: String) {
        _uiState.value = _uiState.value.copy(keyGenRequestCodeInput = input)
    }

    fun updateKeyGenSelectedPlan(plan: ActivationPlan) {
        _uiState.value = _uiState.value.copy(keyGenSelectedPlan = plan)
    }

    fun updateKeyGenCustomDays(days: String) {
        _uiState.value = _uiState.value.copy(keyGenCustomDaysInput = days)
    }

    fun generateKeyGenCode() {
        val req = _uiState.value.keyGenRequestCodeInput.ifBlank { _uiState.value.generatedChallengeCode }
        val plan = _uiState.value.keyGenSelectedPlan
        val customDays = _uiState.value.keyGenCustomDaysInput.toIntOrNull()

        val genResult = DokkaniKeyGenerator.generateActivationCodeFromRequest(req, plan, customDays)
        _uiState.value = _uiState.value.copy(
            keyGenGeneratedResult = genResult,
            activationCodeInput = if (genResult.isSuccess) genResult.activationCode else _uiState.value.activationCodeInput,
            userNotification = if (genResult.isSuccess) "تم توليد كود التفعيل: ${genResult.activationCode}" else genResult.messageArabic
        )
    }

    fun applyGeneratedKeyGenCodeDirectly() {
        val genResult = _uiState.value.keyGenGeneratedResult ?: return
        if (genResult.isSuccess && genResult.activationCode.isNotBlank()) {
            _uiState.value = _uiState.value.copy(activationCodeInput = genResult.activationCode)
            applyActivationCode()
        }
    }

    fun resetTrialForTesting() {
        viewModelScope.launch {
            val fp = _uiState.value.deviceFingerprint
            val trialLicense = LicenseEntity(
                id = 1,
                status = LicenseStatus.TRIAL,
                isLifetime = false,
                expiryTimestamp = null,
                maxAllowedInvoices = 500,
                activatedAt = null,
                lastKnownSystemTimestamp = System.currentTimeMillis(),
                isTimeTampered = false,
                tamperReason = null,
                deviceFingerprint = fp,
                appliedActivationCode = null,
                activePlanCode = "TRL"
            )
            repository.saveLicense(trialLicense)
            _uiState.value = _uiState.value.copy(
                userNotification = "تمت إعادة ضبط الترخيص إلى النسخة التجريبية (500 عملية) للاختبار",
                activationFeedbackMessage = null
            )
        }
    }

    fun simulateTimeTamperForTesting() {
        viewModelScope.launch {
            repository.updateTamperState(true, "محاكاة تلاعب تجريبية: تم اكتشاف تأخير ساعة الجهاز للوراء!")
            _uiState.value = _uiState.value.copy(userNotification = "تم تفعيل محاكاة التلاعب بالوقت وقفل النظام للاختبار!")
        }
    }

    fun clearTimeTamper() {
        viewModelScope.launch {
            repository.updateTamperState(false, null)
            repository.updateLastKnownTime(System.currentTimeMillis())
            _uiState.value = _uiState.value.copy(userNotification = "تم إلغاء قفل التلاعب وتحديث ساعة النظام بنجاح")
        }
    }

    fun copyToClipboard(context: Context, text: String, label: String = "كود دكاني") {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText(label, text)
            clipboard.setPrimaryClip(clip)
            _uiState.value = _uiState.value.copy(userNotification = "تم نسخ $label إلى الحافظة بنجاح!")
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(userNotification = "فشل النسخ: ${e.message}")
        }
    }

    fun shareViaWhatsApp(context: Context, text: String) {
        val cleanMsg = "طلب تفعيل ترخيص دكاني (Dokkani POS):\n\n$text\n\nيرجى تزويدي بكود التفعيل المعتمد."
        try {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                setPackage("com.whatsapp")
                putExtra(Intent.EXTRA_TEXT, cleanMsg)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(sendIntent)
        } catch (e: Exception) {
            try {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, cleanMsg)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(Intent.createChooser(shareIntent, "مشاركة كود الطلب"))
            } catch (ex: Exception) {
                _uiState.value = _uiState.value.copy(userNotification = "تعذر فتح المشاركة: ${ex.message}")
            }
        }
    }
}
