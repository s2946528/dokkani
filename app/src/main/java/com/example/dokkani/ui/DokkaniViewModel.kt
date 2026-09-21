package com.example.dokkani.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.example.dokkani.data.local.DokkaniDatabase
import com.example.dokkani.data.local.SessionManager
import com.example.dokkani.data.repository.DokkaniRepository
import com.example.dokkani.data.local.entities.CashShiftEntity
import com.example.dokkani.data.local.entities.CostValuationMethod
import com.example.dokkani.data.local.entities.CurrencyEntity
import com.example.dokkani.data.local.entities.ExpenseEntity
import com.example.dokkani.data.local.entities.InvoiceEntity
import com.example.dokkani.data.local.entities.InvoiceItemEntity
import com.example.dokkani.data.local.entities.InvoiceType
import com.example.dokkani.data.local.entities.LicenseEntity
import com.example.dokkani.data.local.entities.MovementType
import com.example.dokkani.data.local.entities.PartyEntity
import com.example.dokkani.data.local.entities.PartyType
import com.example.dokkani.data.local.entities.PaymentMethod
import com.example.dokkani.data.local.entities.PaymentVoucherEntity
import com.example.dokkani.data.local.entities.VoucherType
import com.example.dokkani.data.local.entities.ProductEntity
import com.example.dokkani.data.local.entities.ProductUnitEntity
import com.example.dokkani.data.local.entities.ProductWithUnits
import com.example.dokkani.data.local.entities.StockMovementEntity
import com.example.dokkani.data.local.entities.SystemSettingsEntity
import com.example.dokkani.data.local.entities.UserEntity
import com.example.dokkani.data.local.entities.UserRole
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
import com.example.dokkani.data.local.entities.FixedAssetEntity
import com.example.dokkani.data.local.entities.LeaseholdRightEntity
import com.example.dokkani.data.local.entities.OwnerTransactionEntity
import com.example.dokkani.data.local.entities.OwnerTransactionType
import com.example.dokkani.domain.assets.AssetCategories
import com.example.dokkani.data.local.entities.FinancialAccountEntity
import com.example.dokkani.data.local.entities.FinancialAccountType
import com.example.dokkani.data.local.entities.ChartOfAccountsDefaults
import com.example.dokkani.domain.assets.AssetsAndEquityEngine
import com.example.dokkani.domain.assets.EquityCalculationResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * نتيجة فحص الأمان المحاسبي عند محاولة حذف حساب
 */
data class AccountUsageCheckResult(
    val accountName: String,
    val accountCode: String,
    val hasRecords: Boolean,
    val totalRecordCount: Int,
    val invoicesCount: Int,
    val vouchersCount: Int,
    val expensesCount: Int,
    val currentBalance: Double,
    val message: String,
    val isFinancialAccount: Boolean = true,
    val financialAccount: FinancialAccountEntity? = null,
    val partyEntity: PartyEntity? = null
)

data class OpeningBalanceItem(
    val name: String,
    val category: String,
    val quantity: Double,
    val costPrice: Double,
    val sellingPrice: Double,
    val barcode: String = "",
    val unitName: String = "حبة/قطعة"
)

data class OpeningBalanceCustomer(
    val name: String,
    val phone: String,
    val openingBalance: Double
)

data class OpeningBalanceSupplier(
    val name: String,
    val phone: String,
    val openingPayable: Double
)

data class FixedAssetInput(
    val name: String,
    val category: String,
    val purchaseCost: Double,
    val notes: String = ""
)

enum class PropertyStatus {
    OWNED,
    RENTED
}

data class DokkaniUiState(
    val selectedTab: Int = 0,
    val products: List<ProductWithUnits> = emptyList(),
    val parties: List<PartyEntity> = emptyList(),
    val expenses: List<ExpenseEntity> = emptyList(),
    val cashShifts: List<CashShiftEntity> = emptyList(),
    val invoices: List<InvoiceEntity> = emptyList(),
    val vouchers: List<PaymentVoucherEntity> = emptyList(),
    val currencies: List<CurrencyEntity> = emptyList(),
    val baseCurrency: CurrencyEntity? = null,
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
    val keyGenGeneratedResult: KeyGeneratorResult? = null,

    // Assets & Equity
    val fixedAssets: List<FixedAssetEntity> = emptyList(),
    val leaseholdRights: List<LeaseholdRightEntity> = emptyList(),
    val ownerTransactions: List<OwnerTransactionEntity> = emptyList(),
    val stockMovements: List<StockMovementEntity> = emptyList(),
    val equityResult: EquityCalculationResult? = null,
    val assetsSubTab: Int = 0,
    val showAddAssetDialog: Boolean = false,
    val assetCodeInput: String = "",
    val assetNameInput: String = "",
    val assetCategoryInput: String = AssetCategories.REFRIGERATION,
    val assetCostInput: String = "",
    val assetSupplierInput: String = "",
    val assetNotesInput: String = "",
    val assetPaymentMethod: PaymentMethod = PaymentMethod.CASH,
    val showOwnerTransDialog: Boolean = false,
    val ownerTransTypeInput: OwnerTransactionType = OwnerTransactionType.CASH_DRAWING,
    val ownerTransAmountInput: String = "",
    val ownerTransProductId: Long? = null,
    val ownerTransQuantityInput: String = "",
    val ownerTransDetailsInput: String = "",
    val ownerTransPaymentMethod: PaymentMethod = PaymentMethod.CASH,

    // Leasehold & Goodwill
    val showAddLeaseholdDialog: Boolean = false,
    val showAmortizeLeaseholdDialog: Boolean = false,
    val showSellLeaseholdDialog: Boolean = false,
    val selectedLeaseholdItem: LeaseholdRightEntity? = null,
    val leaseholdCodeInput: String = "",
    val leaseholdNameInput: String = "",
    val leaseholdCostInput: String = "",
    val leaseholdYearsInput: String = "5",
    val leaseholdNotesInput: String = "",
    val leaseholdAmortizeAmountInput: String = "",
    val leaseholdSellPriceInput: String = "",
    val leaseholdSellPaymentMethod: PaymentMethod = PaymentMethod.CASH,

    // Financial Accounts & Chart of Accounts (إدارة الحسابات والدليل المحاسبي)
    val financialAccounts: List<FinancialAccountEntity> = emptyList(),
    val showAddEditAccountDialog: Boolean = false,
    val selectedAccountForEdit: FinancialAccountEntity? = null,
    val accountDeletionBlockedDialog: AccountUsageCheckResult? = null,
    val accountToDelete: FinancialAccountEntity? = null,
    val accountsSearchQuery: String = "",
    val accountsFilterType: FinancialAccountType? = null
) {
    val currencySymbol: String get() = baseCurrency?.symbol ?: "ر.ي"
    val currencyName: String get() = baseCurrency?.name ?: "الريال اليمني"
    val currencyId: Long get() = baseCurrency?.id ?: 1L
}

class DokkaniViewModel(application: Application) : AndroidViewModel(application) {

    private val db = DokkaniDatabase.getDatabase(application, viewModelScope)
    private val sessionManager = SessionManager(application)
    val repository = DokkaniRepository(db)

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
                recalculateEquity()
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            db.partyDao().getAllParties().collectLatest { parties ->
                _uiState.update { it.copy(parties = parties) }
                recalculateEquity()
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            db.expenseDao().getAllExpenses().collectLatest { expenses ->
                _uiState.update { it.copy(expenses = expenses) }
                recalculateEquity()
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            db.cashShiftDao().getAllShifts().collectLatest { shifts ->
                _uiState.update { it.copy(cashShifts = shifts) }
                recalculateEquity()
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            db.invoiceDao().getAllInvoices().collectLatest { invoices ->
                _uiState.update { it.copy(invoices = invoices) }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            db.paymentVoucherDao().getAllVouchers().collectLatest { vouchers ->
                _uiState.update { it.copy(vouchers = vouchers) }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            db.currencyDao().getAllCurrencies().collectLatest { currencies ->
                _uiState.update { it.copy(currencies = currencies) }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            repository.baseCurrencyFlow.collectLatest { baseCurr ->
                _uiState.update { it.copy(baseCurrency = baseCurr) }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            db.systemSettingsDao().getSettings().collectLatest { settings ->
                _uiState.update { it.copy(settings = settings) }
                recalculateEquity()
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            db.fixedAssetDao().getAllAssets().collectLatest { assets ->
                _uiState.update { it.copy(fixedAssets = assets) }
                recalculateEquity()
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            db.leaseholdRightDao().getAllLeaseholdRights().collectLatest { leaseholds ->
                _uiState.update { it.copy(leaseholdRights = leaseholds) }
                recalculateEquity()
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            db.ownerTransactionDao().getAllTransactions().collectLatest { trans ->
                _uiState.update { it.copy(ownerTransactions = trans) }
                recalculateEquity()
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            db.stockMovementDao().getAllMovements().collectLatest { movements ->
                _uiState.update { it.copy(stockMovements = movements) }
                recalculateEquity()
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
        viewModelScope.launch(Dispatchers.IO) {
            db.financialAccountDao().getAllAccounts().collectLatest { accounts ->
                _uiState.update { it.copy(financialAccounts = accounts) }
                recalculateEquity()
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
        val vouchers = state.vouchers

        val cashSalesList = invoices.filter { it.type == InvoiceType.SALE && it.paymentMethod == PaymentMethod.CASH }
            .map { it.paidAmount }
        val cashExpensesList = expenses.filter { it.paymentMethod == PaymentMethod.CASH }
            .map { it.amount }

        val cashCollectionsList = vouchers.filter { it.paymentMethod == PaymentMethod.CASH && !it.isPayment }
            .map { it.amount }
        val cashVoucherExpensesList = vouchers.filter { it.paymentMethod == PaymentMethod.CASH && it.isPayment }
            .map { it.amount }

        val totalCashExpensesList = cashExpensesList + cashVoucherExpensesList

        val res = CashDrawerEngine.calculateReconciliation(
            openingCash = opening,
            cashSales = cashSalesList,
            cashCollections = cashCollectionsList,
            cashExpenses = totalCashExpensesList,
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

            if (kotlin.math.abs(party.currentBalance - summary.currentBalance) > 0.001) {
                db.partyDao().updateParty(party.copy(currentBalance = summary.currentBalance))
            }

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
            val party = db.partyDao().getPartyById(partyId)
            val isSupplier = party?.type == PartyType.SUPPLIER

            val vType = if (isSupplier) VoucherType.PAYMENT else VoucherType.RECEIPT
            val prefix = if (isSupplier) "PAY-" else "RCV-"
            val voucher = PaymentVoucherEntity(
                voucherNumber = "$prefix${now.toString().takeLast(6)}",
                partyId = partyId,
                amount = amount,
                voucherType = vType,
                paymentMethod = state.voucherPaymentMethod,
                notes = state.voucherNotesInput,
                receivedBy = "كاشير النظام"
            )
            db.paymentVoucherDao().insertVoucher(voucher)

            // 1. تحديث رصيد الحساب للعميل أو المورد
            val balanceDiff = if (isSupplier) +amount else -amount
            db.partyDao().updateBalance(partyId, balanceDiff)

            // 2. تحديث عهدة الصندوق للشفت المفتوح فوراً عند الدفع النقدي
            if (state.voucherPaymentMethod == PaymentMethod.CASH) {
                val openShift = db.cashShiftDao().getOpenShift()
                if (openShift != null) {
                    if (isSupplier) {
                        // سند صرف للمورد -> يضاف لمصاريف الشفت ويخصم من النقدية المتوقعة بالدرج
                        val newExpenses = openShift.totalCashExpenses + amount
                        db.cashShiftDao().updateExpenses(openShift.id, newExpenses)
                    } else {
                        // سند قبض من عميل -> يضاف لمقبوضات الشفت ويزيد النقدية المتوقعة بالدرج
                        val newCollections = openShift.totalCashCollections + amount
                        db.cashShiftDao().updateCollections(openShift.id, newCollections)
                    }
                }
            }

            // 3. تحديث رصيد الحساب المالي (الصندوق / البنك)
            val accountType = when (state.voucherPaymentMethod) {
                PaymentMethod.CASH -> FinancialAccountType.CASH_DRAWER
                else -> FinancialAccountType.BANK
            }
            val targetAccount = db.financialAccountDao().getAllAccountsSync().firstOrNull { it.accountType == accountType && it.isActive }
            if (targetAccount != null) {
                val accountDiff = if (isSupplier) -amount else +amount
                db.financialAccountDao().updateAccount(targetAccount.copy(currentBalance = targetAccount.currentBalance + accountDiff))
            }

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

    fun renameCategory(oldName: String, newName: String) {
        if (oldName.isBlank() || newName.isBlank() || oldName == newName) return
        viewModelScope.launch(Dispatchers.IO) {
            val productsToUpdate = db.productDao().getAllProductsSync().filter { it.category.trim() == oldName.trim() }
            productsToUpdate.forEach { prod ->
                db.productDao().updateProduct(prod.copy(category = newName.trim()))
            }
        }
    }

    fun deleteCategory(categoryName: String, reassignTo: String = "عام") {
        if (categoryName.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val targetCategory = reassignTo.ifBlank { "عام" }
            val productsToUpdate = db.productDao().getAllProductsSync().filter { it.category.trim() == categoryName.trim() }
            productsToUpdate.forEach { prod ->
                db.productDao().updateProduct(prod.copy(category = targetCategory))
            }
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
            if (currency.isBaseCurrency) {
                val allCurrencies = db.currencyDao().getAllCurrenciesSync()
                val updatedCurrencies = mutableListOf<CurrencyEntity>()
                var handledTarget = false

                allCurrencies.forEach { curr ->
                    if (curr.id == currency.id || (currency.id == 0L && curr.code.equals(currency.code, ignoreCase = true))) {
                        updatedCurrencies.add(currency.copy(isBaseCurrency = true, exchangeRateToBase = 1.0))
                        handledTarget = true
                    } else {
                        updatedCurrencies.add(curr.copy(isBaseCurrency = false))
                    }
                }

                if (!handledTarget) {
                    updatedCurrencies.add(currency.copy(isBaseCurrency = true, exchangeRateToBase = 1.0))
                }

                db.currencyDao().insertCurrencies(updatedCurrencies)
            } else {
                db.currencyDao().insertCurrency(currency)
            }
        }
    }

    fun setAsBaseCurrency(currency: CurrencyEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setAsBaseCurrency(currency)
        }
    }

    fun setAsBaseCurrency(currencyId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val allCurrencies = db.currencyDao().getAllCurrenciesSync()
            val targetCurrency = allCurrencies.find { it.id == currencyId } ?: return@launch
            repository.setAsBaseCurrency(targetCurrency)
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

    fun updateTaxSettings(enabled: Boolean, taxRate: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentSettings = db.systemSettingsDao().getSettingsSync() ?: SystemSettingsEntity()
            val updated = currentSettings.copy(
                isTaxEnabled = enabled,
                defaultTaxRate = taxRate,
                lastUpdated = System.currentTimeMillis()
            )
            db.systemSettingsDao().insertOrUpdateSettings(updated)
        }
    }

    fun updatePurchaseTaxSettings(enabled: Boolean, taxRate: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentSettings = db.systemSettingsDao().getSettingsSync() ?: SystemSettingsEntity()
            val updated = currentSettings.copy(
                isPurchaseTaxEnabled = enabled,
                purchaseTaxRate = taxRate,
                lastUpdated = System.currentTimeMillis()
            )
            db.systemSettingsDao().insertOrUpdateSettings(updated)
        }
    }

    fun updateStoreProfile(
        storeName: String,
        storeAddress: String,
        storePhone: String,
        taxNumber: String,
        invoiceFooterText: String,
        showPreviousBalanceOnInvoice: Boolean
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentSettings = db.systemSettingsDao().getSettingsSync() ?: SystemSettingsEntity()
            val updated = currentSettings.copy(
                storeName = storeName.ifBlank { "دكاني" },
                storeAddress = storeAddress,
                storePhone = storePhone,
                taxNumber = taxNumber,
                invoiceFooterText = invoiceFooterText,
                showPreviousBalanceOnInvoice = showPreviousBalanceOnInvoice,
                lastUpdated = System.currentTimeMillis()
            )
            db.systemSettingsDao().insertOrUpdateSettings(updated)
        }
    }

    fun updateShowPreviousBalance(show: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentSettings = db.systemSettingsDao().getSettingsSync() ?: SystemSettingsEntity()
            val updated = currentSettings.copy(
                showPreviousBalanceOnInvoice = show,
                lastUpdated = System.currentTimeMillis()
            )
            db.systemSettingsDao().insertOrUpdateSettings(updated)
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
                    val isPay = v.isPayment
                    val party = db.partyDao().getPartyById(v.partyId)
                    if (party != null) {
                        val isSupplier = party.type == PartyType.SUPPLIER
                        val newBal = if (isSupplier || isPay) party.currentBalance - v.amount else party.currentBalance + v.amount
                        db.partyDao().updateParty(party.copy(currentBalance = newBal))
                    }
                    if (v.paymentMethod == PaymentMethod.CASH) {
                        val openShift = db.cashShiftDao().getOpenShift()
                        if (openShift != null) {
                            if (isPay) {
                                val newExp = (openShift.totalCashExpenses - v.amount).coerceAtLeast(0.0)
                                db.cashShiftDao().updateExpenses(openShift.id, newExp)
                            } else {
                                val newCollections = (openShift.totalCashCollections - v.amount).coerceAtLeast(0.0)
                                db.cashShiftDao().updateCollections(openShift.id, newCollections)
                            }
                        }
                    }
                    val accountType = when (v.paymentMethod) {
                        PaymentMethod.CASH -> FinancialAccountType.CASH_DRAWER
                        else -> FinancialAccountType.BANK
                    }
                    val targetAccount = db.financialAccountDao().getAllAccountsSync().firstOrNull { it.accountType == accountType && it.isActive }
                    if (targetAccount != null) {
                        val accountDiff = if (isPay) +v.amount else -v.amount
                        db.financialAccountDao().updateAccount(targetAccount.copy(currentBalance = targetAccount.currentBalance + accountDiff))
                    }
                    db.paymentVoucherDao().deleteVoucher(v)
                }
            }
        }
    }

    // --- معالج التهيئة الأولى والرقابة المحاسبية (Onboarding Wizard) ---
    fun completeOnboarding(
        selectedBaseCurrency: CurrencyEntity? = null,
        isNewGrocery: Boolean,
        storeName: String,
        adminPin: String = "1234",
        cashierName: String = "كاشير 1",
        cashierPin: String = "1234",
        initialCapital: Double = 0.0,
        openingCashDrawer: Double = 0.0,
        bankBalance: Double = 0.0,
        valuationMethod: CostValuationMethod = CostValuationMethod.WAC,
        propertyStatus: PropertyStatus = PropertyStatus.OWNED,
        monthlyRent: Double = 0.0,
        prepaidMonths: Int = 0,
        fixedAssets: List<FixedAssetInput> = emptyList(),
        openingItems: List<OpeningBalanceItem> = emptyList(),
        openingCustomers: List<OpeningBalanceCustomer> = emptyList(),
        openingSuppliers: List<OpeningBalanceSupplier> = emptyList(),
        onCompleted: () -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            db.withTransaction {
                // 0. تثبيت العملة الأساسية عبر DokkaniRepository
                if (selectedBaseCurrency != null) {
                    repository.setAsBaseCurrency(selectedBaseCurrency)
                }

                val now = System.currentTimeMillis()

                // 1. تحديث إعدادات النظام وتوثيق رأس المال ونقدية الصندوق والبنك المعتمدة
                val currentSettings = db.systemSettingsDao().getSettingsSync() ?: SystemSettingsEntity()
                val effectiveOpeningCash = if (openingCashDrawer > 0.0) openingCashDrawer else (if (initialCapital > 0.0) initialCapital else 0.0)
                val updatedSettings = currentSettings.copy(
                    storeName = storeName.ifBlank { "تموينات ومخضار السعادة" },
                    defaultCurrencyCode = selectedBaseCurrency?.code ?: "YER",
                    costValuationMethod = valuationMethod,
                    initialCapital = initialCapital,
                    openingCashDrawer = effectiveOpeningCash,
                    initialBankBalance = bankBalance,
                    lastUpdated = now
                )
                db.systemSettingsDao().insertOrUpdateSettings(updatedSettings)

                // تحديث أرصدة الحسابات المالية (صندوق النقدية والبنك) في الدليل المحاسبي
                val cashAccount = db.financialAccountDao().getAccountByCode("10101")
                if (cashAccount != null) {
                    db.financialAccountDao().updateAccount(
                        cashAccount.copy(
                            openingBalance = effectiveOpeningCash,
                            currentBalance = effectiveOpeningCash
                        )
                    )
                }
                val bankAccount = db.financialAccountDao().getAccountByCode("10201")
                if (bankAccount != null && bankBalance > 0.0) {
                    db.financialAccountDao().updateAccount(
                        bankAccount.copy(
                            openingBalance = bankBalance,
                            currentBalance = bankBalance
                        )
                    )
                }

                // 2. تحديث وتثبيت مستخدم مدير النظام والكاشير
                db.userDao().insertUser(
                    UserEntity(
                        id = 1,
                        username = "admin",
                        fullName = "مدير النظام",
                        pinCode = adminPin.ifBlank { "1234" },
                        role = UserRole.ADMIN,
                        isActive = true
                    )
                )
                db.userDao().insertUser(
                    UserEntity(
                        id = 2,
                        username = "cashier1",
                        fullName = cashierName.ifBlank { "كاشير 1" },
                        pinCode = cashierPin.ifBlank { "1234" },
                        role = UserRole.CASHIER,
                        isActive = true
                    )
                )

                // 3. إنشاء وفتح أول شفت مالي كاشير
                val shiftNumber = "SHF-${SimpleDateFormat("yyyyMMdd-HHmm", Locale.getDefault()).format(Date(now))}"
                val initialShift = CashShiftEntity(
                    shiftNumber = shiftNumber,
                    cashierName = cashierName.ifBlank { "كاشير 1" },
                    startTime = now,
                    openingCash = effectiveOpeningCash,
                    totalCashSales = 0.0,
                    totalCashCollections = 0.0,
                    totalCashExpenses = 0.0,
                    expectedCashInDrawer = effectiveOpeningCash,
                    actualPhysicalCash = effectiveOpeningCash,
                    cashDiscrepancy = 0.0,
                    status = "OPEN",
                    notes = if (isNewGrocery) "افتتاح أول فترة مالية - تأسيس بقالة جديدة" else "افتتاح أول فترة مالية - ترحيل أرصدة ودفاتر"
                )
                db.cashShiftDao().insertShift(initialShift)

                // 4. تسجيل الأصول الثابتة المدخلة إن وجدت
                if (fixedAssets.isNotEmpty()) {
                    fixedAssets.forEachIndexed { index, asset ->
                        if (asset.purchaseCost > 0.0) {
                            db.fixedAssetDao().insertAsset(
                                FixedAssetEntity(
                                    assetCode = "AST-INIT-${100 + index}",
                                    name = asset.name,
                                    category = asset.category,
                                    purchaseCost = asset.purchaseCost,
                                    currentValue = asset.purchaseCost,
                                    purchaseDate = now,
                                    notes = asset.notes.ifBlank { "أصل ثابت افتتاحي عند التأسيس" }
                                )
                            )
                        }
                    }
                }

                // 5. تسجيل مصروف الإيجار المدفوع مقدماً إذا كان العقار مستأجراً
                if (propertyStatus == PropertyStatus.RENTED && monthlyRent * prepaidMonths > 0.0) {
                    val prepaidTotal = monthlyRent * prepaidMonths
                    db.leaseholdRightDao().insertLeaseholdRight(
                        LeaseholdRightEntity(
                            code = "PREPAID-RENT-${SimpleDateFormat("yyyyMM", Locale.getDefault()).format(Date(now))}",
                            name = "إيجار المحل المدفوع مقدماً ($prepaidMonths أشهر)",
                            initialCost = prepaidTotal,
                            currentBookValue = prepaidTotal,
                            contractStartDate = now,
                            contractDurationYears = 1,
                            notes = "إيجار شهري قدره $monthlyRent لمدة $prepaidMonths أشهر"
                        )
                    )
                }

                // 6. في حال البقالة القائمة: إدراج بضاعة أول المدة
                if (!isNewGrocery && openingItems.isNotEmpty()) {
                    openingItems.forEachIndexed { index, item ->
                        val code = "INIT-${1000 + index}"
                        val prodId = db.productDao().insertProduct(
                            ProductEntity(
                                code = code,
                                name = item.name,
                                category = item.category,
                                isWeighted = false,
                                minStockAlert = 5.0
                            )
                        )
                        val itemBarcode = if (item.barcode.isNotBlank()) item.barcode.trim() else "628${(System.currentTimeMillis() + index).toString().takeLast(9)}"
                        val itemUnitName = if (item.unitName.isNotBlank()) item.unitName.trim() else "حبة/قطعة"
                        val unitId = db.productDao().insertUnit(
                            ProductUnitEntity(
                                productId = prodId,
                                unitName = itemUnitName,
                                conversionFactor = 1.0,
                                barcode = itemBarcode,
                                costPrice = item.costPrice,
                                sellingPrice = item.sellingPrice,
                                isBaseUnit = true
                            )
                        )
                        // قيد حركة مخزون بضاعة أول المدة
                        db.stockMovementDao().insertMovement(
                            StockMovementEntity(
                                productId = prodId,
                                productUnitId = unitId,
                                movementType = MovementType.PURCHASE_IN,
                                quantityBaseUnit = item.quantity,
                                remainingQuantityForFifo = item.quantity,
                                unitCostPriceBase = item.costPrice,
                                timestamp = now,
                                referenceNumber = "OPENING-STOCK"
                            )
                        )
                    }
                }

                // 7. إدراج ديون العملاء الافتتاحية من الدفتر القديم
                if (!isNewGrocery && openingCustomers.isNotEmpty()) {
                    openingCustomers.forEach { cust ->
                        db.partyDao().insertParty(
                            PartyEntity(
                                name = cust.name,
                                type = PartyType.CUSTOMER,
                                phone = cust.phone,
                                currentBalance = cust.openingBalance,
                                creditLimit = (cust.openingBalance * 2).coerceAtLeast(500.0),
                                notes = "رصيد افتتاحي مرحل من الدفتر الورقي"
                            )
                        )
                    }
                }

                // 8. إدراج مستحقات الموردين الافتتاحية
                if (!isNewGrocery && openingSuppliers.isNotEmpty()) {
                    openingSuppliers.forEach { supp ->
                        db.partyDao().insertParty(
                            PartyEntity(
                                name = supp.name,
                                type = PartyType.SUPPLIER,
                                phone = supp.phone,
                                currentBalance = -supp.openingPayable, // سالب دائن له عندنا
                                notes = "مستحق افتتاحي سابق مرحل للمورد"
                            )
                        )
                    }
                }

                // 9. حفظ اكتمال التهيئة في تفضيلات الجلسة
                sessionManager.setOnboardingCompleted(true)
            }

            launch(Dispatchers.Main) {
                onCompleted()
            }
        }
    }

    // --- Assets & Equity Actions ---
    fun selectAssetsSubTab(index: Int) {
        _uiState.update { it.copy(assetsSubTab = index) }
    }

    fun recalculateEquity() {
        val state = _uiState.value

        // 1. استخراج نقدية الصندوق والدرج في البداية ديناميكياً
        // يقرأ القيمة الحقيقية من إعدادات التهيئة المسجلة أولاً، ثم من حساب الصندوق الرئيسي (10101)، ثم من الشفت الافتتاحي
        val cashFromSettings = state.settings?.openingCashDrawer?.takeIf { it > 0.0 }
            ?: state.settings?.initialCapital?.takeIf { it > 0.0 }
        val cashFromAccount = state.financialAccounts.firstOrNull { it.code == "10101" || it.accountType == FinancialAccountType.CASH_DRAWER }?.let {
            if (it.openingBalance > 0.0) it.openingBalance else it.currentBalance
        }?.takeIf { it > 0.0 }
        val cashFromShift = state.cashShifts.minByOrNull { it.startTime }?.openingCash?.takeIf { it > 0.0 }
            ?: state.cashShifts.firstOrNull { it.status == "OPEN" }?.openingCash?.takeIf { it > 0.0 }

        val dynamicCashInDrawer = cashFromSettings ?: cashFromAccount ?: cashFromShift ?: 0.0

        // 2. استخراج أرصدة البنوك والشبكة ديناميكياً
        val bankFromSettings = state.settings?.initialBankBalance?.takeIf { it > 0.0 }
        val bankFromAccount = state.financialAccounts.firstOrNull { it.code == "10201" || it.accountType == FinancialAccountType.BANK }?.let {
            if (it.openingBalance > 0.0) it.openingBalance else it.currentBalance
        }?.takeIf { it > 0.0 }
        val expensesBank = state.expenses
            .filter { it.paymentMethod == PaymentMethod.BANK_TRANSFER || it.paymentMethod == PaymentMethod.MADA }
            .sumOf { it.amount }
        val dynamicBankBalance = (bankFromSettings ?: bankFromAccount ?: 0.0) + expensesBank

        val pnl = state.pnlReport
        val netOperatingProfit = pnl?.netOperatingProfit ?: 0.0

        val result = AssetsAndEquityEngine.calculateInitialCapitalAndEquity(
            cashInDrawer = dynamicCashInDrawer,
            bankBalances = dynamicBankBalance,
            productsWithUnits = state.products,
            stockMovements = state.stockMovements,
            parties = state.parties,
            fixedAssets = state.fixedAssets,
            leaseholdRights = state.leaseholdRights,
            ownerTransactions = state.ownerTransactions,
            netOperatingProfit = netOperatingProfit
        )

        _uiState.update { it.copy(equityResult = result) }
    }

    fun openAddAssetDialog() {
        val nextCode = "AST-${(100..999).random()}"
        _uiState.update {
            it.copy(
                showAddAssetDialog = true,
                assetCodeInput = nextCode,
                assetNameInput = "",
                assetCategoryInput = AssetCategories.REFRIGERATION,
                assetCostInput = "",
                assetSupplierInput = "",
                assetNotesInput = "",
                assetPaymentMethod = PaymentMethod.CASH
            )
        }
    }

    fun dismissAddAssetDialog() {
        _uiState.update { it.copy(showAddAssetDialog = false) }
    }

    fun updateAssetInputs(
        code: String,
        name: String,
        category: String,
        cost: String,
        supplier: String,
        notes: String,
        paymentMethod: PaymentMethod
    ) {
        _uiState.update {
            it.copy(
                assetCodeInput = code,
                assetNameInput = name,
                assetCategoryInput = category,
                assetCostInput = cost,
                assetSupplierInput = supplier,
                assetNotesInput = notes,
                assetPaymentMethod = paymentMethod
            )
        }
    }

    fun submitAddAsset() {
        val state = _uiState.value
        val name = state.assetNameInput.trim()
        val cost = state.assetCostInput.toDoubleOrNull() ?: 0.0
        if (name.isEmpty() || cost <= 0) return

        val code = state.assetCodeInput.trim().ifEmpty { "AST-${System.currentTimeMillis() % 10000}" }

        viewModelScope.launch(Dispatchers.IO) {
            db.withTransaction {
                // 1. إدراج الأصل الثابت
                db.fixedAssetDao().insertAsset(
                    FixedAssetEntity(
                        assetCode = code,
                        name = name,
                        category = state.assetCategoryInput,
                        purchaseCost = cost,
                        currentValue = cost,
                        purchaseDate = System.currentTimeMillis(),
                        supplierName = state.assetSupplierInput.trim(),
                        paymentMethod = state.assetPaymentMethod,
                        status = "ACTIVE",
                        notes = state.assetNotesInput.trim()
                    )
                )

                // 2. تسجيل سند صرف أصول ثابتة (CapEx) للخصم من النقدية/البنك دون التأثير على المصروفات التشغيلية
                val now = System.currentTimeMillis()
                val expCount = db.expenseDao().getAllExpenses()
                val expNum = "AST-EXP-${now % 100000}"
                db.expenseDao().insertExpense(
                    ExpenseEntity(
                        expenseNumber = expNum,
                        category = "شراء أصل ثابت (ثلاجات/أرفف/موازين)",
                        amount = cost,
                        paymentMethod = state.assetPaymentMethod,
                        date = now,
                        paidTo = state.assetSupplierInput.trim().ifEmpty { "مورد أصول" },
                        notes = "شراء أصل ثابت: $name ($code)",
                        recordedBy = "المدير العام"
                    )
                )
            }

            _uiState.update { it.copy(showAddAssetDialog = false) }
        }
    }

    fun deleteAsset(assetId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            db.fixedAssetDao().deleteAssetById(assetId)
        }
    }

    fun openOwnerTransDialog(type: OwnerTransactionType) {
        _uiState.update { currentState ->
            currentState.copy(
                showOwnerTransDialog = true,
                ownerTransTypeInput = type,
                ownerTransAmountInput = "",
                ownerTransProductId = currentState.products.firstOrNull()?.product?.id,
                ownerTransQuantityInput = "1.0",
                ownerTransDetailsInput = "",
                ownerTransPaymentMethod = PaymentMethod.CASH
            )
        }
    }

    fun dismissOwnerTransDialog() {
        _uiState.update { it.copy(showOwnerTransDialog = false) }
    }

    fun updateOwnerTransInputs(
        type: OwnerTransactionType,
        amount: String,
        productId: Long?,
        quantity: String,
        details: String,
        paymentMethod: PaymentMethod
    ) {
        _uiState.update {
            it.copy(
                ownerTransTypeInput = type,
                ownerTransAmountInput = amount,
                ownerTransProductId = productId,
                ownerTransQuantityInput = quantity,
                ownerTransDetailsInput = details,
                ownerTransPaymentMethod = paymentMethod
            )
        }
    }

    fun submitOwnerTrans() {
        val state = _uiState.value
        val type = state.ownerTransTypeInput
        val now = System.currentTimeMillis()
        val transNum = "EQ-${now % 100000}"

        viewModelScope.launch(Dispatchers.IO) {
            db.withTransaction {
                when (type) {
                    OwnerTransactionType.CASH_DRAWING -> {
                        val amt = state.ownerTransAmountInput.toDoubleOrNull() ?: 0.0
                        if (amt <= 0) return@withTransaction

                        db.ownerTransactionDao().insertTransaction(
                            OwnerTransactionEntity(
                                transactionNumber = transNum,
                                type = OwnerTransactionType.CASH_DRAWING,
                                amount = amt,
                                paymentMethod = state.ownerTransPaymentMethod,
                                date = now,
                                details = state.ownerTransDetailsInput.ifEmpty { "مسحوبات نقدية للمالك" },
                                recordedBy = "المدير العام"
                            )
                        )

                        // تسجيل خصم نقدية CapEx
                        db.expenseDao().insertExpense(
                            ExpenseEntity(
                                expenseNumber = "DRW-$transNum",
                                category = "مسحوبات شخصية للمالك (نقدية)",
                                amount = amt,
                                paymentMethod = state.ownerTransPaymentMethod,
                                date = now,
                                paidTo = "المالك شخصياً",
                                notes = state.ownerTransDetailsInput,
                                recordedBy = "المدير العام"
                            )
                        )
                    }
                    OwnerTransactionType.GOODS_DRAWING -> {
                        val prodId = state.ownerTransProductId ?: return@withTransaction
                        val qty = state.ownerTransQuantityInput.toDoubleOrNull() ?: 0.0
                        if (qty <= 0) return@withTransaction

                        val pwu = state.products.find { it.product.id == prodId } ?: return@withTransaction
                        val baseUnit = pwu.units.firstOrNull { it.isBaseUnit } ?: pwu.units.firstOrNull() ?: return@withTransaction
                        val unitCost = baseUnit.costPrice
                        val totalCostAmount = qty * unitCost

                        db.ownerTransactionDao().insertTransaction(
                            OwnerTransactionEntity(
                                transactionNumber = transNum,
                                type = OwnerTransactionType.GOODS_DRAWING,
                                amount = totalCostAmount,
                                productId = prodId,
                                quantity = qty,
                                unitCost = unitCost,
                                paymentMethod = PaymentMethod.CASH,
                                date = now,
                                details = "سحب بضاعة: ${pwu.product.name} (كمية $qty بسعر تكلفة $unitCost ${_uiState.value.currencySymbol})",
                                recordedBy = "المدير العام"
                            )
                        )

                        // خصم البضاعة من المخزون بسعر التكلفة دون تسجيل مبيعات أو إيرادات
                        db.stockMovementDao().insertMovement(
                            StockMovementEntity(
                                productId = prodId,
                                productUnitId = baseUnit.id,
                                movementType = MovementType.INVENTORY_ADJUSTMENT,
                                quantityBaseUnit = -qty,
                                remainingQuantityForFifo = 0.0,
                                unitCostPriceBase = unitCost,
                                timestamp = now,
                                referenceNumber = transNum,
                                notes = "مسحوبات المالك الشخصية بسعر التكلفة"
                            )
                        )
                    }
                    OwnerTransactionType.CAPITAL_DEPOSIT -> {
                        val amt = state.ownerTransAmountInput.toDoubleOrNull() ?: 0.0
                        if (amt <= 0) return@withTransaction

                        db.ownerTransactionDao().insertTransaction(
                            OwnerTransactionEntity(
                                transactionNumber = transNum,
                                type = OwnerTransactionType.CAPITAL_DEPOSIT,
                                amount = amt,
                                paymentMethod = state.ownerTransPaymentMethod,
                                date = now,
                                details = state.ownerTransDetailsInput.ifEmpty { "إيداع ضخ سيولة إضافية في رأس المال" },
                                recordedBy = "المدير العام"
                            )
                        )
                    }
                }
            }

            _uiState.update { it.copy(showOwnerTransDialog = false) }
        }
    }

    fun deleteOwnerTrans(transId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            db.ownerTransactionDao().deleteTransactionById(transId)
        }
    }

    // --- Leasehold Rights & Goodwill Handlers ---

    fun openAddLeaseholdDialog() {
        val nextCode = "GW-${(100..999).random()}"
        _uiState.update {
            it.copy(
                showAddLeaseholdDialog = true,
                leaseholdCodeInput = nextCode,
                leaseholdNameInput = "",
                leaseholdCostInput = "",
                leaseholdYearsInput = "5",
                leaseholdNotesInput = ""
            )
        }
    }

    fun dismissAddLeaseholdDialog() {
        _uiState.update { it.copy(showAddLeaseholdDialog = false) }
    }

    fun updateLeaseholdInputs(code: String, name: String, cost: String, years: String, notes: String) {
        _uiState.update {
            it.copy(
                leaseholdCodeInput = code,
                leaseholdNameInput = name,
                leaseholdCostInput = cost,
                leaseholdYearsInput = years,
                leaseholdNotesInput = notes
            )
        }
    }

    fun submitAddLeasehold() {
        val state = _uiState.value
        val name = state.leaseholdNameInput.trim()
        val cost = state.leaseholdCostInput.toDoubleOrNull() ?: 0.0
        val years = state.leaseholdYearsInput.toIntOrNull() ?: 5
        if (name.isEmpty() || cost <= 0) return

        val code = state.leaseholdCodeInput.trim().ifEmpty { "GW-${System.currentTimeMillis() % 10000}" }

        viewModelScope.launch(Dispatchers.IO) {
            db.leaseholdRightDao().insertLeaseholdRight(
                LeaseholdRightEntity(
                    code = code,
                    name = name,
                    initialCost = cost,
                    currentBookValue = cost,
                    accumulatedAmortization = 0.0,
                    contractStartDate = System.currentTimeMillis(),
                    contractDurationYears = years,
                    status = "ACTIVE",
                    notes = state.leaseholdNotesInput.trim()
                )
            )
            _uiState.update { it.copy(showAddLeaseholdDialog = false) }
        }
    }

    fun openAmortizeLeaseholdDialog(item: LeaseholdRightEntity) {
        val yearlyAmort = if (item.contractDurationYears > 0) item.initialCost / item.contractDurationYears else item.currentBookValue
        _uiState.update {
            it.copy(
                showAmortizeLeaseholdDialog = true,
                selectedLeaseholdItem = item,
                leaseholdAmortizeAmountInput = String.format(java.util.Locale.US, "%.2f", yearlyAmort)
            )
        }
    }

    fun dismissAmortizeLeaseholdDialog() {
        _uiState.update { it.copy(showAmortizeLeaseholdDialog = false, selectedLeaseholdItem = null) }
    }

    fun updateAmortizeAmountInput(amount: String) {
        _uiState.update { it.copy(leaseholdAmortizeAmountInput = amount) }
    }

    fun submitAmortizeLeasehold() {
        val state = _uiState.value
        val item = state.selectedLeaseholdItem ?: return
        val amortAmount = state.leaseholdAmortizeAmountInput.toDoubleOrNull() ?: 0.0
        if (amortAmount <= 0) return

        viewModelScope.launch(Dispatchers.IO) {
            db.withTransaction {
                val newAccum = item.accumulatedAmortization + amortAmount
                val newBookVal = (item.initialCost - newAccum).coerceAtLeast(0.0)
                val newStatus = if (newBookVal <= 0.0001) "FULLY_AMORTIZED" else item.status

                val updatedItem = item.copy(
                    accumulatedAmortization = newAccum,
                    currentBookValue = newBookVal,
                    status = newStatus
                )
                db.leaseholdRightDao().updateLeaseholdRight(updatedItem)

                // تحميل قسط الإطفاء كمصروف تشغيلي دوري على الأرباح والخسائر
                val now = System.currentTimeMillis()
                val expNum = "AMORT-${now % 100000}"
                db.expenseDao().insertExpense(
                    ExpenseEntity(
                        expenseNumber = expNum,
                        category = "إطفاء خلو المحل (Amortization)",
                        amount = amortAmount,
                        paymentMethod = PaymentMethod.CASH,
                        date = now,
                        paidTo = "إطفاء أصول غير ملموسة",
                        notes = "إطفاء دوري لخلو المحل: ${item.name} (${item.code})",
                        recordedBy = "المدير العام"
                    )
                )
            }
            _uiState.update { it.copy(showAmortizeLeaseholdDialog = false, selectedLeaseholdItem = null) }
        }
    }

    fun openSellLeaseholdDialog(item: LeaseholdRightEntity) {
        _uiState.update {
            it.copy(
                showSellLeaseholdDialog = true,
                selectedLeaseholdItem = item,
                leaseholdSellPriceInput = String.format(java.util.Locale.US, "%.2f", item.currentBookValue),
                leaseholdSellPaymentMethod = PaymentMethod.CASH
            )
        }
    }

    fun dismissSellLeaseholdDialog() {
        _uiState.update { it.copy(showSellLeaseholdDialog = false, selectedLeaseholdItem = null) }
    }

    fun updateSellLeaseholdInputs(price: String, paymentMethod: PaymentMethod) {
        _uiState.update {
            it.copy(
                leaseholdSellPriceInput = price,
                leaseholdSellPaymentMethod = paymentMethod
            )
        }
    }

    fun submitSellLeasehold() {
        val state = _uiState.value
        val item = state.selectedLeaseholdItem ?: return
        val salePrice = state.leaseholdSellPriceInput.toDoubleOrNull() ?: 0.0
        if (salePrice < 0) return

        viewModelScope.launch(Dispatchers.IO) {
            db.withTransaction {
                val currentBook = item.currentBookValue
                val gainOrLoss = salePrice - currentBook
                val now = System.currentTimeMillis()
                val transNum = "SELL-GW-${now % 100000}"

                // 1. تحديث حالة الأصل غير الملموس إلى مباع/متنازل عنه
                val updatedItem = item.copy(
                    currentBookValue = 0.0,
                    status = "SOLD_TRANSFERRED",
                    notes = item.notes + " | تم التنازل/إعادة البيع بسعر $salePrice ${_uiState.value.currencySymbol} بتاريخ ${java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(now)}"
                )
                db.leaseholdRightDao().updateLeaseholdRight(updatedItem)

                // 2. إدراج سند إيداع/قبض للمالك بتفاصيل بيع الخلو والأرباح/الخسائر الرأسمالية
                val gainLossDetails = if (gainOrLoss >= 0) {
                    "ربح رأسمالي قدره ${String.format(java.util.Locale.US, "%.2f", gainOrLoss)} ${_uiState.value.currencySymbol}"
                } else {
                    "خسارة رأسمالية قدرها ${String.format(java.util.Locale.US, "%.2f", kotlin.math.abs(gainOrLoss))} ${_uiState.value.currencySymbol}"
                }

                db.ownerTransactionDao().insertTransaction(
                    OwnerTransactionEntity(
                        transactionNumber = transNum,
                        type = OwnerTransactionType.CAPITAL_DEPOSIT,
                        amount = salePrice,
                        paymentMethod = state.leaseholdSellPaymentMethod,
                        date = now,
                        details = "حصيلة إعادة بيع/التنازل عن الخلو: ${item.name} ($gainLossDetails)",
                        recordedBy = "المدير العام"
                    )
                )
            }
            _uiState.update { it.copy(showSellLeaseholdDialog = false, selectedLeaseholdItem = null) }
        }
    }

    fun deleteLeasehold(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            db.leaseholdRightDao().deleteLeaseholdRightById(id)
        }
    }

    // =========================================================================
    // إدارة الحسابات المالية والدليل المحاسبي (Financial Accounts & Security)
    // =========================================================================

    fun setAccountsSearchQuery(query: String) {
        _uiState.update { it.copy(accountsSearchQuery = query) }
    }

    fun setAccountsFilterType(type: FinancialAccountType?) {
        _uiState.update { it.copy(accountsFilterType = type) }
    }

    fun openAddAccountDialog() {
        _uiState.update {
            it.copy(
                showAddEditAccountDialog = true,
                selectedAccountForEdit = null
            )
        }
    }

    fun openEditAccountDialog(account: FinancialAccountEntity) {
        _uiState.update {
            it.copy(
                showAddEditAccountDialog = true,
                selectedAccountForEdit = account
            )
        }
    }

    fun dismissAddEditAccountDialog() {
        _uiState.update {
            it.copy(
                showAddEditAccountDialog = false,
                selectedAccountForEdit = null
            )
        }
    }

    fun saveFinancialAccount(account: FinancialAccountEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            db.financialAccountDao().insertAccount(account)
            _uiState.update {
                it.copy(
                    showAddEditAccountDialog = false,
                    selectedAccountForEdit = null
                )
            }
        }
    }

    fun toggleFinancialAccountActive(account: FinancialAccountEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = account.copy(isActive = !account.isActive)
            db.financialAccountDao().updateAccount(updated)
        }
    }

    /**
     * فحص الأمان المحاسبي قبل الحذف:
     * التحقق مما إذا كان الحساب يحتوي على أي حركات مالية مسجلة (سندات، فواتير، قيود، أو رصيد قائم)
     */
    fun requestDeleteFinancialAccount(account: FinancialAccountEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val invoices = _uiState.value.invoices
            val vouchers = _uiState.value.vouchers
            val expenses = _uiState.value.expenses

            // حساب عدد الفواتير المرتبطة بالحساب (عن طريق الاسم أو الكود أو طريقة السداد)
            val invCount = invoices.count { inv ->
                inv.notes.contains(account.name, ignoreCase = true) ||
                inv.notes.contains(account.code, ignoreCase = true) ||
                (account.accountType == FinancialAccountType.BANK && inv.paymentMethod == PaymentMethod.BANK_TRANSFER) ||
                (account.accountType == FinancialAccountType.CASH_DRAWER && inv.paymentMethod == PaymentMethod.CASH && invoices.isNotEmpty())
            }

            // حساب عدد السندات المرتبطة بالحساب
            val vouchCount = vouchers.count { v ->
                v.notes.contains(account.name, ignoreCase = true) ||
                v.notes.contains(account.code, ignoreCase = true) ||
                (account.accountType == FinancialAccountType.BANK && v.paymentMethod == PaymentMethod.BANK_TRANSFER)
            }

            // حساب عدد المصروفات المسددة عبر الحساب
            val expCount = expenses.count { exp ->
                exp.paidTo.contains(account.name, ignoreCase = true) ||
                exp.notes.contains(account.name, ignoreCase = true) ||
                exp.notes.contains(account.code, ignoreCase = true)
            }

            val hasBalance = abs(account.currentBalance) > 0.001 || abs(account.openingBalance) > 0.001
            val totalRecords = invCount + vouchCount + expCount + (if (hasBalance) 1 else 0)

            if (totalRecords > 0) {
                // منع الحذف وتفعيل شرط الأمان المحاسبي
                val result = AccountUsageCheckResult(
                    accountName = account.name,
                    accountCode = account.code,
                    hasRecords = true,
                    totalRecordCount = totalRecords,
                    invoicesCount = invCount,
                    vouchersCount = vouchCount,
                    expensesCount = expCount,
                    currentBalance = account.currentBalance,
                    message = "عذراً، لا يمكن حذف هذا الحساب لوجود حركات وسجلات مالية مرتبطة به، يمكنك تعطيله بدلاً من ذلك.",
                    isFinancialAccount = true,
                    financialAccount = account
                )
                _uiState.update { it.copy(accountDeletionBlockedDialog = result, accountToDelete = null) }
            } else {
                // الحساب خالٍ تماماً -> السماح بالحذف
                _uiState.update { it.copy(accountToDelete = account, accountDeletionBlockedDialog = null) }
            }
        }
    }

    fun confirmDeleteFinancialAccount(account: FinancialAccountEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            db.financialAccountDao().deleteAccount(account)
            _uiState.update { it.copy(accountToDelete = null) }
        }
    }

    /**
     * فحص أمان مماثل لحسابات العملاء والموردين لمنع حذف عميل/مورد مرتبط بفواتير أو سندات
     */
    fun requestDeletePartyWithProtection(party: PartyEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val invCount = _uiState.value.invoices.count { it.partyId == party.id }
            val vouchCount = _uiState.value.vouchers.count { it.partyId == party.id }
            val hasBalance = abs(party.currentBalance) > 0.001
            val totalRecords = invCount + vouchCount + (if (hasBalance) 1 else 0)

            if (totalRecords > 0) {
                val result = AccountUsageCheckResult(
                    accountName = party.name,
                    accountCode = "PARTY-${party.id}",
                    hasRecords = true,
                    totalRecordCount = totalRecords,
                    invoicesCount = invCount,
                    vouchersCount = vouchCount,
                    expensesCount = 0,
                    currentBalance = party.currentBalance,
                    message = "عذراً، لا يمكن حذف هذا الحساب لوجود حركات وسجلات مالية مرتبطة به، يمكنك تعطيله بدلاً من ذلك.",
                    isFinancialAccount = false,
                    partyEntity = party
                )
                _uiState.update { it.copy(accountDeletionBlockedDialog = result) }
            } else {
                deleteParty(party)
            }
        }
    }

    fun disableAccountInstead(result: AccountUsageCheckResult) {
        viewModelScope.launch(Dispatchers.IO) {
            if (result.isFinancialAccount && result.financialAccount != null) {
                val deactivated = result.financialAccount.copy(isActive = false)
                db.financialAccountDao().updateAccount(deactivated)
            } else if (!result.isFinancialAccount && result.partyEntity != null) {
                val party = result.partyEntity.copy(creditLimit = 0.0)
                db.partyDao().updateParty(party)
            }
            _uiState.update { it.copy(accountDeletionBlockedDialog = null) }
        }
    }

    fun dismissAccountDeleteDialogs() {
        _uiState.update { it.copy(accountDeletionBlockedDialog = null, accountToDelete = null) }
    }
}
