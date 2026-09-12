package com.example.dokkani.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.example.dokkani.data.local.DokkaniDatabase
import com.example.dokkani.data.local.entities.CashShiftEntity
import com.example.dokkani.data.local.entities.CostValuationMethod
import com.example.dokkani.data.local.entities.CurrencyEntity
import com.example.dokkani.data.local.entities.ExpenseEntity
import com.example.dokkani.data.local.entities.InvoiceEntity
import com.example.dokkani.data.local.entities.InvoiceItemEntity
import com.example.dokkani.data.local.entities.InvoiceType
import com.example.dokkani.data.local.entities.LicenseEntity
import com.example.dokkani.data.local.entities.PartyEntity
import com.example.dokkani.data.local.entities.PaymentMethod
import com.example.dokkani.data.local.entities.PaymentVoucherEntity
import com.example.dokkani.data.local.entities.ProductEntity
import com.example.dokkani.data.local.entities.ProductUnitEntity
import com.example.dokkani.data.local.entities.ProductWithUnits
import com.example.dokkani.data.local.entities.SystemSettingsEntity
import com.example.dokkani.domain.cash.CashDrawerEngine
import com.example.dokkani.domain.cash.CashReconciliationResult
import com.example.dokkani.domain.credit.CreditNotebookEngine
import com.example.dokkani.domain.credit.CustomerStatementSummary
import com.example.dokkani.domain.reports.FinancialReportsEngine
import com.example.dokkani.domain.reports.InventoryHealthReport
import com.example.dokkani.domain.reports.ProfitAndLossReport
import com.example.dokkani.domain.reports.TopProductsReport
import com.example.dokkani.domain.security.ActivationPlan
import com.example.dokkani.domain.security.DeviceFingerprintManager
import com.example.dokkani.domain.security.DokkaniKeyGenerator
import com.example.dokkani.domain.security.KeyGeneratorResult
import com.example.dokkani.domain.security.LicenseEvaluationResult
import com.example.dokkani.domain.security.LicenseStatus
import com.example.dokkani.domain.security.OfflineLicenseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DokkaniUiState(
    val selectedTab: Int = 0,
    val products: List<ProductWithUnits> = emptyList(),
    val parties: List<PartyEntity> = emptyList(),
    val expenses: List<ExpenseEntity> = emptyList(),
    val cashShifts: List<CashShiftEntity> = emptyList(),
    val invoices: List<InvoiceEntity> = emptyList(),
    val currencies: List<CurrencyEntity> = emptyList(),
    val settings: SystemSettingsEntity? = null,

    // Cash & Expenses
    val cashSubTab: Int = 0,
    val showAddExpenseDialog: Boolean = false,
    val expenseCategoryInput: String = "كهرباء ومياه",
    val expenseAmountInput: String = "",
    val expensePaidToInput: String = "",
    val expenseNotesInput: String = "",
    val expensePaymentMethod: PaymentMethod = PaymentMethod.CASH,
    val isSubmittingExpense: Boolean = false,
    val reconciliationResult: CashReconciliationResult? = null,
    val drawerOpeningCashInput: String = "200.0",
    val drawerPhysicalCashInput: String = "",
    val drawerShiftNotesInput: String = "",
    val isClosingShift: Boolean = false,

    // Credit Ledger
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

    // Reports
    val isLoadingReports: Boolean = false,
    val reportSubTab: Int = 0,
    val pnlReport: ProfitAndLossReport? = null,
    val selectedReportValuationMethod: CostValuationMethod = CostValuationMethod.WAC,
    val topProductsReport: TopProductsReport? = null,
    val inventoryHealthReport: InventoryHealthReport? = null,

    // License
    val licenseEvaluation: LicenseEvaluationResult? = null,
    val deviceFingerprint: String = "",
    val selectedPlanForRequest: ActivationPlan = ActivationPlan.TRIAL_500,
    val generatedChallengeCode: String = "",
    val activationCodeInput: String = "",
    val activationFeedbackMessage: String? = null,
    val isActivating: Boolean = false,
    val isDeveloperKeyGenExpanded: Boolean = false,
    val keyGenRequestCodeInput: String = "",
    val keyGenSelectedPlan: ActivationPlan = ActivationPlan.YEARLY_1,
    val keyGenGeneratedResult: KeyGeneratorResult? = null
)

class DokkaniViewModel(application: Application) : AndroidViewModel(application) {

    private val db = DokkaniDatabase.getDatabase(application, viewModelScope)

    private val _uiState = MutableStateFlow(DokkaniUiState())
    val uiState: StateFlow<DokkaniUiState> = _uiState.asStateFlow()

    init {
        val fingerprint = DeviceFingerprintManager.getDeviceFingerprint(application)
        val challenge = OfflineLicenseManager.generateChallengeCode(fingerprint, ActivationPlan.TRIAL_500)
        _uiState.update {
            it.copy(
                deviceFingerprint = fingerprint,
                generatedChallengeCode = challenge
            )
        }

        observeData()
    }

    private fun observeData() {
        viewModelScope.launch(Dispatchers.IO) {
            db.productDao().getProductsWithUnits().collectLatest { products ->
                _uiState.update { it.copy(products = products) }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            db.partyDao().getAllParties().collectLatest { parties ->
                _uiState.update { it.copy(parties = parties) }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            db.expenseDao().getAllExpenses().collectLatest { expenses ->
                _uiState.update { it.copy(expenses = expenses) }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            db.cashShiftDao().getAllShifts().collectLatest { shifts ->
                _uiState.update { it.copy(cashShifts = shifts) }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            db.invoiceDao().getAllInvoices().collectLatest { invoices ->
                _uiState.update { it.copy(invoices = invoices) }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            db.currencyDao().getAllCurrencies().collectLatest { currencies ->
                _uiState.update { it.copy(currencies = currencies) }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            db.systemSettingsDao().getSettings().collectLatest { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            val fingerprint = _uiState.value.deviceFingerprint
            db.licenseDao().getLicenseFlow().collectLatest { license ->
                val currentInvoices = db.invoiceDao().countInvoices()
                val eval = OfflineLicenseManager.evaluateLicense(
                    status = license?.status ?: LicenseStatus.TRIAL,
                    isLifetime = license?.isLifetime ?: false,
                    expiryTimestamp = license?.expiryTimestamp,
                    maxAllowedInvoices = license?.maxAllowedInvoices ?: 500,
                    totalInvoicesIssued = currentInvoices,
                    deviceFingerprint = license?.deviceFingerprint ?: fingerprint,
                    isTimeTampered = license?.isTimeTampered ?: false
                )
                _uiState.update { it.copy(licenseEvaluation = eval) }
            }
        }
    }

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }

    // --- Cash & Expenses Actions ---
    fun selectCashSubTab(index: Int) {
        _uiState.update { it.copy(cashSubTab = index) }
    }

    fun openAddExpenseDialog() {
        _uiState.update { it.copy(showAddExpenseDialog = true) }
    }

    fun dismissAddExpenseDialog() {
        _uiState.update {
            it.copy(
                showAddExpenseDialog = false,
                expenseAmountInput = "",
                expensePaidToInput = "",
                expenseNotesInput = ""
            )
        }
    }

    fun updateExpenseInputs(
        category: String,
        amount: String,
        paidTo: String,
        notes: String,
        method: PaymentMethod
    ) {
        _uiState.update {
            it.copy(
                expenseCategoryInput = category,
                expenseAmountInput = amount,
                expensePaidToInput = paidTo,
                expenseNotesInput = notes,
                expensePaymentMethod = method
            )
        }
    }

    fun submitExpense() {
        val state = _uiState.value
        val amount = state.expenseAmountInput.toDoubleOrNull() ?: return
        if (amount <= 0) return

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isSubmittingExpense = true) }
            val exp = ExpenseEntity(
                expenseNumber = "EXP-${System.currentTimeMillis().toString().takeLast(6)}",
                category = state.expenseCategoryInput,
                amount = amount,
                paymentMethod = state.expensePaymentMethod,
                paidTo = state.expensePaidToInput,
                notes = state.expenseNotesInput
            )
            db.expenseDao().insertExpense(exp)
            _uiState.update {
                it.copy(
                    isSubmittingExpense = false,
                    showAddExpenseDialog = false,
                    expenseAmountInput = "",
                    expensePaidToInput = "",
                    expenseNotesInput = ""
                )
            }
        }
    }

    fun updateDrawerInputs(opening: String, physical: String, notes: String) {
        _uiState.update {
            it.copy(
                drawerOpeningCashInput = opening,
                drawerPhysicalCashInput = physical,
                drawerShiftNotesInput = notes
            )
        }
        calculateDrawerReconciliation()
    }

    fun calculateDrawerReconciliation() {
        val state = _uiState.value
        val opening = state.drawerOpeningCashInput.toDoubleOrNull() ?: 200.0
        val physical = state.drawerPhysicalCashInput.toDoubleOrNull() ?: 0.0

        val invoices = state.invoices
        val expenses = state.expenses

        val cashSalesList = invoices.filter { it.type == InvoiceType.SALE && it.paymentMethod == PaymentMethod.CASH }
            .map { it.paidAmount }
        val cashExpensesList = expenses.filter { it.paymentMethod == PaymentMethod.CASH }
            .map { it.amount }

        val res = CashDrawerEngine.calculateReconciliation(
            openingCash = opening,
            cashSales = cashSalesList,
            cashCollections = emptyList(),
            cashExpenses = cashExpensesList,
            actualPhysicalCash = physical
        )
        _uiState.update { it.copy(reconciliationResult = res) }
    }

    fun closeShiftAndSave() {
        val state = _uiState.value
        val physical = state.drawerPhysicalCashInput.toDoubleOrNull() ?: return
        val opening = state.drawerOpeningCashInput.toDoubleOrNull() ?: 200.0
        val recon = state.reconciliationResult ?: return

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isClosingShift = true) }
            val now = System.currentTimeMillis()
            val shift = CashShiftEntity(
                shiftNumber = "SHF-${now.toString().takeLast(6)}",
                cashierName = "كاشير النظام",
                startTime = now - (8 * 3600000L),
                endTime = now,
                openingCash = opening,
                totalCashSales = recon.totalCashSales,
                totalCashCollections = recon.totalCashCollections,
                totalCashExpenses = recon.totalCashExpenses,
                expectedCashInDrawer = recon.expectedCashInDrawer,
                actualPhysicalCash = physical,
                cashDiscrepancy = recon.discrepancy,
                notes = state.drawerShiftNotesInput,
                status = "CLOSED"
            )
            db.cashShiftDao().insertShift(shift)
            _uiState.update {
                it.copy(
                    isClosingShift = false,
                    drawerPhysicalCashInput = "",
                    drawerShiftNotesInput = ""
                )
            }
        }
    }

    // --- Credit Ledger Actions ---
    fun updateCreditSearchQuery(query: String) {
        _uiState.update { it.copy(creditSearchQuery = query) }
    }

    fun selectPartyForStatement(partyId: Long?) {
        _uiState.update { it.copy(selectedPartyForStatement = partyId, isLoadingStatement = partyId != null) }
        if (partyId != null) {
            loadCustomerStatement(partyId)
        }
    }

    private fun loadCustomerStatement(partyId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val party = db.partyDao().getPartyById(partyId) ?: return@launch
            val invoices = db.invoiceDao().getInvoicesForPartySync(partyId)
            val vouchers = db.paymentVoucherDao().getVouchersForPartySync(partyId)

            val summary = CreditNotebookEngine.buildCustomerStatement(
                party = party,
                invoices = invoices,
                vouchers = vouchers,
                storeName = "دكاني"
            )
            _uiState.update { it.copy(customerStatementSummary = summary, isLoadingStatement = false) }
        }
    }

    fun openPaymentVoucherDialog(partyId: Long) {
        _uiState.update { it.copy(showPaymentVoucherDialog = true, voucherPartyId = partyId) }
    }

    fun dismissPaymentVoucherDialog() {
        _uiState.update {
            it.copy(
                showPaymentVoucherDialog = false,
                voucherPartyId = null,
                voucherAmountInput = "",
                voucherNotesInput = ""
            )
        }
    }

    fun updateVoucherInputs(amount: String, notes: String, method: PaymentMethod) {
        _uiState.update {
            it.copy(
                voucherAmountInput = amount,
                voucherNotesInput = notes,
                voucherPaymentMethod = method
            )
        }
    }

    fun submitPaymentVoucher() {
        val state = _uiState.value
        val partyId = state.voucherPartyId ?: return
        val amount = state.voucherAmountInput.toDoubleOrNull() ?: return
        if (amount <= 0) return

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isSubmittingVoucher = true) }
            val now = System.currentTimeMillis()
            val voucher = PaymentVoucherEntity(
                voucherNumber = "RCV-${now.toString().takeLast(6)}",
                partyId = partyId,
                amount = amount,
                paymentMethod = state.voucherPaymentMethod,
                notes = state.voucherNotesInput,
                receivedBy = "كاشير النظام"
            )
            db.paymentVoucherDao().insertVoucher(voucher)
            db.partyDao().updateBalance(partyId, -amount)

            _uiState.update {
                it.copy(
                    isSubmittingVoucher = false,
                    showPaymentVoucherDialog = false,
                    voucherPartyId = null,
                    voucherAmountInput = "",
                    voucherNotesInput = ""
                )
            }
            loadCustomerStatement(partyId)
        }
    }

    // --- Reports Actions ---
    fun selectReportSubTab(index: Int) {
        _uiState.update { it.copy(reportSubTab = index) }
    }

    fun selectValuationMethod(method: CostValuationMethod) {
        _uiState.update { it.copy(selectedReportValuationMethod = method) }
        refreshReports()
    }

    fun refreshReports() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoadingReports = true) }
            val invoices = db.invoiceDao().getAllInvoicesSync()
            val invoiceItems = db.invoiceDao().getAllInvoiceItemsSync()
            val expenses = db.expenseDao().getExpensesByDateRangeSync(0, System.currentTimeMillis())
            val products = db.productDao().getProductsWithUnitsSync()
            val movements = db.stockMovementDao().getAllMovementsSync()

            val productUnitCosts = products.associate { pwu ->
                pwu.product.id to (pwu.units.firstOrNull { it.isBaseUnit }?.costPrice ?: pwu.units.firstOrNull()?.costPrice ?: 0.0)
            }

            val pnl = FinancialReportsEngine.generateProfitAndLossReport(
                invoices = invoices,
                invoiceItems = invoiceItems,
                expenses = expenses,
                productUnitCosts = productUnitCosts,
                valuationMethod = _uiState.value.selectedReportValuationMethod
            )
            val topProds = FinancialReportsEngine.generateTopProductsReport(
                productsWithUnits = products,
                invoiceItems = invoiceItems,
                productUnitCosts = productUnitCosts
            )
            val inventoryHealth = FinancialReportsEngine.generateInventoryHealthReport(
                productsWithUnits = products,
                stockMovements = movements
            )

            _uiState.update {
                it.copy(
                    pnlReport = pnl,
                    topProductsReport = topProds,
                    inventoryHealthReport = inventoryHealth,
                    isLoadingReports = false
                )
            }
        }
    }

    // --- License Actions ---
    fun selectPlanForRequest(plan: ActivationPlan) {
        val fingerprint = _uiState.value.deviceFingerprint
        val challenge = OfflineLicenseManager.generateChallengeCode(fingerprint, plan)
        _uiState.update {
            it.copy(
                selectedPlanForRequest = plan,
                generatedChallengeCode = challenge
            )
        }
    }

    fun refreshChallengeCode() {
        selectPlanForRequest(_uiState.value.selectedPlanForRequest)
    }

    fun updateActivationCodeInput(code: String) {
        _uiState.update { it.copy(activationCodeInput = code) }
    }

    fun applyActivationCode() {
        val state = _uiState.value
        val code = state.activationCodeInput.trim()
        if (code.isBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isActivating = true, activationFeedbackMessage = null) }
            val fingerprint = state.deviceFingerprint
            val currentLicense = db.licenseDao().getLicenseSync() ?: LicenseEntity()
            val verifyResult = OfflineLicenseManager.verifyAndApplyActivationCode(
                activationCode = code,
                currentDeviceFingerprint = fingerprint,
                currentInvoicesCount = db.invoiceDao().countInvoices()
            )

            if (verifyResult.isSuccess) {
                val updatedLicense = currentLicense.copy(
                    status = verifyResult.newStatus,
                    isLifetime = verifyResult.isLifetime,
                    expiryTimestamp = verifyResult.expiryTimestamp,
                    maxAllowedInvoices = verifyResult.maxAllowedInvoices,
                    activatedAt = System.currentTimeMillis(),
                    appliedActivationCode = code,
                    activePlanCode = verifyResult.plan.code
                )
                db.licenseDao().insertOrUpdate(updatedLicense)
                _uiState.update {
                    it.copy(
                        isActivating = false,
                        activationFeedbackMessage = verifyResult.messageArabic,
                        activationCodeInput = ""
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isActivating = false,
                        activationFeedbackMessage = verifyResult.messageArabic
                    )
                }
            }
        }
    }

    fun toggleDeveloperKeyGen() {
        _uiState.update { it.copy(isDeveloperKeyGenExpanded = !it.isDeveloperKeyGenExpanded) }
    }

    fun updateKeyGenRequestInput(req: String) {
        _uiState.update { it.copy(keyGenRequestCodeInput = req) }
    }

    fun updateKeyGenPlan(plan: ActivationPlan) {
        _uiState.update { it.copy(keyGenSelectedPlan = plan) }
    }

    fun generateKeyGenCode() {
        val state = _uiState.value
        val req = state.keyGenRequestCodeInput.trim()
        if (req.isBlank()) return

        val generated = DokkaniKeyGenerator.generateActivationCodeFromRequest(req, state.keyGenSelectedPlan)
        _uiState.update { it.copy(keyGenGeneratedResult = generated) }
    }

    // --- CRUD Management Actions ---
    fun saveProduct(product: ProductEntity, baseUnitName: String = "حبة", cost: Double = 0.0, sell: Double = 0.0, barcode: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            val prodId = db.productDao().insertProduct(product)
            if (product.id == 0L) {
                val baseUnit = ProductUnitEntity(
                    productId = prodId,
                    unitName = baseUnitName.ifBlank { "حبة" },
                    conversionFactor = 1.0,
                    barcode = barcode,
                    costPrice = cost,
                    sellingPrice = sell,
                    isBaseUnit = true
                )
                db.productDao().insertUnit(baseUnit)
            }
        }
    }

    fun deleteProduct(productId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val prod = db.productDao().getProductById(productId)
            if (prod != null) {
                db.productDao().deleteUnitsByProductId(productId)
                db.productDao().deleteProduct(prod)
            }
        }
    }

    fun saveProductUnit(unit: ProductUnitEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            db.productDao().insertUnit(unit)
        }
    }

    fun deleteProductUnit(unit: ProductUnitEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            db.productDao().deleteUnit(unit)
        }
    }

    fun saveParty(party: PartyEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            db.partyDao().insertParty(party)
        }
    }

    fun deleteParty(party: PartyEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            db.partyDao().deleteParty(party)
        }
    }

    fun saveCurrency(currency: CurrencyEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            db.currencyDao().insertCurrency(currency)
        }
    }

    fun deleteCurrency(currency: CurrencyEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            db.currencyDao().deleteCurrency(currency)
        }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            db.expenseDao().deleteExpense(expense)
        }
    }

    fun updateEnableNegativeStock(enable: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            db.systemSettingsDao().updateEnableNegativeStock(enable)
        }
    }

    fun deleteInvoice(invoiceId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            db.withTransaction {
                val inv = db.invoiceDao().getInvoiceById(invoiceId)
                if (inv != null) {
                    // 1. التراجع عن حركات المخزون
                    db.stockMovementDao().deleteMovementsByReferenceNumber(inv.invoiceNumber)

                    // 2. التراجع عن رصيد العميل أو المورد في حال البيع/الشراء الآجل
                    inv.partyId?.let { pId ->
                        val party = db.partyDao().getPartyById(pId)
                        if (party != null && inv.paymentMethod == PaymentMethod.CREDIT) {
                            val newBalance = when (inv.type) {
                                InvoiceType.SALE -> party.currentBalance - inv.total
                                InvoiceType.PURCHASE -> party.currentBalance + inv.total
                                InvoiceType.SALE_RETURN -> party.currentBalance + inv.total
                                InvoiceType.PURCHASE_RETURN -> party.currentBalance - inv.total
                            }
                            db.partyDao().updateParty(party.copy(currentBalance = newBalance))
                        }
                    }

                    // 3. التراجع عن مبيعات الصندوق في الشفت المفتوح إذا كان الدفع نقداً
                    if (inv.paymentMethod == PaymentMethod.CASH) {
                        val shifts = db.cashShiftDao().getAllShiftsSync()
                        val currentShift = shifts.firstOrNull { it.status == "OPEN" } ?: shifts.firstOrNull()
                        if (currentShift != null) {
                            when (inv.type) {
                                InvoiceType.SALE -> {
                                    val newSales = (currentShift.totalCashSales - inv.total).coerceAtLeast(0.0)
                                    db.cashShiftDao().updateSales(currentShift.id, newSales)
                                }
                                InvoiceType.PURCHASE -> {
                                    val newExp = (currentShift.totalCashExpenses - inv.total).coerceAtLeast(0.0)
                                    db.cashShiftDao().updateExpenses(currentShift.id, newExp)
                                }
                                else -> {}
                            }
                        }
                    }

                    // 4. حذف الفاتورة وبنودها
                    db.invoiceDao().deleteInvoice(inv)
                }
            }
        }
    }

    fun deletePaymentVoucher(voucherId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            db.withTransaction {
                val v = db.paymentVoucherDao().getVoucherById(voucherId)
                if (v != null) {
                    // التراجع عن رصيد العميل (تم سداد مبلغ، فعند الحذف نعيد المديونية)
                    val party = db.partyDao().getPartyById(v.partyId)
                    if (party != null) {
                        db.partyDao().updateParty(party.copy(currentBalance = party.currentBalance + v.amount))
                    }
                    if (v.paymentMethod == PaymentMethod.CASH) {
                        val shifts = db.cashShiftDao().getAllShiftsSync()
                        val currentShift = shifts.firstOrNull { it.status == "OPEN" } ?: shifts.firstOrNull()
                        if (currentShift != null) {
                            val newCollections = (currentShift.totalCashCollections - v.amount).coerceAtLeast(0.0)
                            db.cashShiftDao().updateCollections(currentShift.id, newCollections)
                        }
                    }
                    db.paymentVoucherDao().deleteVoucher(v)
                }
            }
        }
    }
}
