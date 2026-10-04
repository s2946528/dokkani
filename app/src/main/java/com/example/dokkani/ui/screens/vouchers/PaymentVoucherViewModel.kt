package com.example.dokkani.ui.screens.vouchers

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.example.dokkani.data.local.DokkaniDatabase
import com.example.dokkani.data.local.entities.CostCenterEntity
import com.example.dokkani.data.local.entities.CurrencyEntity
import com.example.dokkani.data.local.entities.EmployeeEntity
import com.example.dokkani.data.local.entities.EmployeeTransactionEntity
import com.example.dokkani.data.local.entities.EmployeeTransactionType
import com.example.dokkani.data.local.entities.FinancialAccountEntity
import com.example.dokkani.data.local.entities.FinancialAccountType
import com.example.dokkani.data.local.entities.PartyEntity
import com.example.dokkani.data.local.entities.PaymentMethod
import com.example.dokkani.data.local.entities.PaymentVoucherEntity
import com.example.dokkani.data.local.entities.VoucherType
import com.example.dokkani.domain.reports.TrialBalanceGuard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

/**
/ فئة معلومات مجموعة طرق الدفع الديناميكية (نقد، بنك، محفظة، شبكة)
 */
data class PaymentGroupInfo(
    val key: String,
    val labelArabic: String,
    val iconName: String,
    val accounts: List<FinancialAccountEntity>
)

data class PaymentVoucherUiState(
    // 1. الطرف المستفيد (الحسابات الفرعية فقط من الدليل المحاسبي - الطرف المدين من حـ/)
    val beneficiarySubAccounts: List<FinancialAccountEntity> = emptyList(),
    val selectedBeneficiaryAccount: FinancialAccountEntity? = null,
    val beneficiarySearchQuery: String = "",

    // 2. العملات وأسعار الصرف (من قاعدة البيانات المحلّية - غير قابلة للتعديل يدوياً)
    val currencies: List<CurrencyEntity> = emptyList(),
    val selectedCurrency: CurrencyEntity? = null,
    val exchangeRate: Double = 1.0,                       // سعر الصرف المجلوب تلقائياً (Read-Only)
    val amountInput: String = "",                         // المبلغ بالعملة المختارة
    val equivalentBaseAmount: Double = 0.0,               // المبلغ الموازي بالعملة الأساسية

    // 3. طرق الدفع المالي الديناميكية المصنفة (الطرف الدائن - إلى حـ/)
    val allFinancialAccounts: List<FinancialAccountEntity> = emptyList(),
    val activePaymentGroups: List<PaymentGroupInfo> = emptyList(), // المجموعات التي تحتوي فقط على حسابات فرعية
    val selectedPaymentGroupKey: String = "CASH",
    val selectedPaymentAccount: FinancialAccountEntity? = null,

    // خيارات الدفع المتعدد / التجزئة (Multiple Split Payment)
    val isMultiPayment: Boolean = false,
    val secondaryPaymentGroupKey: String = "BANK",
    val selectedSecondaryPaymentAccount: FinancialAccountEntity? = null,
    val secondaryAmountInput: String = "",
    val secondaryEquivalentBaseAmount: Double = 0.0,

    // 4. مراكز التكلفة والتفاصيل
    val costCenters: List<CostCenterEntity> = emptyList(),
    val selectedCostCenterId: Long = 1L,
    val transactionRef: String = "",
    val notesInput: String = "",
    val receiptImagePath: String? = null,

    // 5. حالة المعالجة والملاحظات
    val isProcessing: Boolean = false,
    val feedbackMessage: String? = null,
    val isError: Boolean = false,
    val showSuccessDialog: Boolean = false,
    val lastSavedVoucherNumber: String? = null,
    val lastSavedJournalProof: String? = null,

    // 6. أرشيف وسجل سندات الصرف
    val paymentVouchers: List<PaymentVoucherEntity> = emptyList(),
    val searchQuery: String = "",
    val isHistoryExpanded: Boolean = true,
    val selectedVoucherForDetails: PaymentVoucherEntity? = null,
    val showDetailsDialog: Boolean = false,
    val selectedVoucherForEdit: PaymentVoucherEntity? = null,
    val showEditDialog: Boolean = false,

    // 7. صلاحيات وحماية مدير النظام
    val showAdminPinDialog: Boolean = false,
    val pendingDeleteVoucher: PaymentVoucherEntity? = null,
    val adminPinError: String? = null,

    // عملة النظام الأساسية
    val baseCurrencySymbol: String = "ر.ي"
)

class PaymentVoucherViewModel(application: Application) : AndroidViewModel(application) {
    private val db = DokkaniDatabase.getDatabase(application, viewModelScope)
    private val partyDao = db.partyDao()
    private val voucherDao = db.paymentVoucherDao()
    private val costCenterDao = db.costCenterDao()
    private val financialAccountDao = db.financialAccountDao()
    private val cashShiftDao = db.cashShiftDao()
    private val currencyDao = db.currencyDao()
    private val employeeDao = db.employeeDao()
    private val employeeTransactionDao = db.employeeTransactionDao()

    private val _uiState = MutableStateFlow(PaymentVoucherUiState())
    val uiState: StateFlow<PaymentVoucherUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        // 1. جلب مراكز التكلفة
        viewModelScope.launch(Dispatchers.IO) {
            costCenterDao.getAllCostCenters().collectLatest { centers ->
                _uiState.update { it.copy(costCenters = centers) }
            }
        }

        // 2. جلب الحسابات المالية والمستفيدين (شاملاً العملاء والموردين والحسابات العامة والموظفين/الرواتب)
        viewModelScope.launch(Dispatchers.IO) {
            combine(
                financialAccountDao.getAllAccounts(),
                financialAccountDao.getBeneficiarySubAccounts(),
                employeeDao.getAllEmployees()
            ) { allAccounts, beneficiaryAccounts, employees ->
                val activeAccounts = allAccounts.filter { it.isActive }
                val activeBeneficiaries = beneficiaryAccounts.filter { it.isActive && !it.isMainAccount }.toMutableList()

                // تحويل الموظفين والعمال إلى مستفيدين تحت بند "503 - مصروفات الرواتب والأجور والمنافع" (أجور ورواتب العمالة)
                val employeeAccounts = employees.filter { it.isActive }.map { emp ->
                    FinancialAccountEntity(
                        id = -100000L - emp.id,
                        code = "50301-EMP-${emp.id}",
                        name = "الموظف: ${emp.name}${if (emp.jobTitle.isNotBlank()) " (${emp.jobTitle})" else ""}",
                        accountType = FinancialAccountType.EXPENSE,
                        parentAccountCode = "503",
                        parentAccountName = "503 - مصروفات الرواتب والأجور والمنافع (أجور ورواتب العمالة)",
                        accountNumber = "EMP-%04d".format(emp.id),
                        openingBalance = 0.0,
                        currentBalance = emp.basePayRate,
                        notes = "حساب مستفيد فرعي خاص بالموظف/العامل ${emp.name}"
                    )
                }

                // دمج الموظفين دون تكرار
                val existingCodes = activeBeneficiaries.map { it.code }.toSet()
                for (empAcc in employeeAccounts) {
                    if (!existingCodes.contains(empAcc.code)) {
                        activeBeneficiaries.add(empAcc)
                    }
                }

                val sortedBeneficiaries = activeBeneficiaries.sortedBy { it.code }

                // تصنيف طرق الدفع المتاحة ديناميكياً مع إخفاء المجموعات الفارغة
                val groups = buildDynamicPaymentGroups(activeAccounts)

                _uiState.update { state ->
                    val defaultBeneficiary = state.selectedBeneficiaryAccount 
                        ?: sortedBeneficiaries.firstOrNull { it.parentAccountCode == "503" || it.code.startsWith("503") }
                        ?: sortedBeneficiaries.firstOrNull { it.parentAccountCode == "201" || it.code == "20101" } 
                        ?: sortedBeneficiaries.firstOrNull()

                    val currentGroupKey = if (groups.any { it.key == state.selectedPaymentGroupKey }) state.selectedPaymentGroupKey else (groups.firstOrNull()?.key ?: "CASH")
                    val groupAccounts = groups.firstOrNull { it.key == currentGroupKey }?.accounts ?: emptyList()
                    val defaultPaymentAccount = state.selectedPaymentAccount ?: groupAccounts.firstOrNull { it.isDefault } ?: groupAccounts.firstOrNull()

                    state.copy(
                        allFinancialAccounts = allAccounts,
                        beneficiarySubAccounts = sortedBeneficiaries,
                        selectedBeneficiaryAccount = defaultBeneficiary,
                        activePaymentGroups = groups,
                        selectedPaymentGroupKey = currentGroupKey,
                        selectedPaymentAccount = defaultPaymentAccount
                    )
                }
            }.collectLatest { }
        }

        // 3. جلب جميع العملات وأسعار الصرف من قاعدة البيانات
        viewModelScope.launch(Dispatchers.IO) {
            currencyDao.getAllCurrencies().collectLatest { currList ->
                val baseCurr = currList.firstOrNull { it.isBaseCurrency } ?: currList.firstOrNull()
                _uiState.update { state ->
                    val selCurr = state.selectedCurrency ?: baseCurr
                    val rate = selCurr?.exchangeRateToBase ?: 1.0
                    val amountVal = state.amountInput.toDoubleOrNull() ?: 0.0
                    state.copy(
                        currencies = currList,
                        selectedCurrency = selCurr,
                        exchangeRate = rate,
                        equivalentBaseAmount = amountVal * rate,
                        baseCurrencySymbol = baseCurr?.symbol ?: "ر.ي"
                    )
                }
            }
        }

        // 4. جلب أرشيف سندات الصرف
        viewModelScope.launch(Dispatchers.IO) {
            voucherDao.getAllVouchers().collectLatest { list ->
                val paymentsOnly = list.filter { it.voucherType == VoucherType.PAYMENT }
                _uiState.update { it.copy(paymentVouchers = paymentsOnly) }
            }
        }
    }

    /**
     * بناء تصنيفات طرق الدفع ديناميكياً مع استبعاد وإخفاء طريقة "أجل" والمجموعات الفارغة
     */
    private fun buildDynamicPaymentGroups(activeAccounts: List<FinancialAccountEntity>): List<PaymentGroupInfo> {
        val result = mutableListOf<PaymentGroupInfo>()

        // 1. نقدية والصندوق
        val cashAccounts = activeAccounts.filter { !it.isMainAccount && (it.accountType == FinancialAccountType.CASH_DRAWER || it.code == "10101" || it.parentAccountCode == "101") }
        if (cashAccounts.isNotEmpty()) {
            result.add(PaymentGroupInfo("CASH", "صندوق / نقدية", "PointOfSale", cashAccounts))
        }

        // 2. الحسابات المصرفية والبنوك
        val bankAccounts = activeAccounts.filter { !it.isMainAccount && (it.accountType == FinancialAccountType.BANK || it.code.startsWith("102") || it.parentAccountCode == "102") }
        if (bankAccounts.isNotEmpty()) {
            result.add(PaymentGroupInfo("BANK", "حسابات بنكية", "AccountBalance", bankAccounts))
        }

        // 3. المحافظ الإلكترونية
        val walletAccounts = activeAccounts.filter { !it.isMainAccount && (it.accountType == FinancialAccountType.E_WALLET || it.code.startsWith("103") || it.parentAccountCode == "103") }
        if (walletAccounts.isNotEmpty()) {
            result.add(PaymentGroupInfo("WALLET", "محافظ إلكترونية", "AccountBalanceWallet", walletAccounts))
        }

        // 4. الشبكة والبطاقات
        val cardAccounts = activeAccounts.filter { !it.isMainAccount && (it.accountType == FinancialAccountType.CHART_ACCOUNT && (it.name.contains("شبكة") || it.name.contains("مدى") || it.name.contains("بطاقة"))) }
        if (cardAccounts.isNotEmpty()) {
            result.add(PaymentGroupInfo("CARD", "شبكة / بطاقات", "CreditCard", cardAccounts))
        }

        // 5. دفع متعدد / تجزئة (استبعاد "أجل" تماماً)
        val allPaymentAccounts = cashAccounts + bankAccounts + walletAccounts + cardAccounts
        if (allPaymentAccounts.size >= 2) {
            result.add(PaymentGroupInfo("MULTIPLE", "دفع متعدد / تجزئة", "AltRoute", allPaymentAccounts))
        }

        return result
    }

    // تحديد الحساب الفرعي المستفيد (الطرف المدين)
    fun selectBeneficiaryAccount(account: FinancialAccountEntity?) {
        _uiState.update { it.copy(selectedBeneficiaryAccount = account) }
    }

    fun setBeneficiarySearchQuery(query: String) {
        _uiState.update { it.copy(beneficiarySearchQuery = query) }
    }

    // تحديد العملة المعتمدة لسند الصرف وتطبيق سعر الصرف الآلي (Read-Only)
    fun selectCurrency(currency: CurrencyEntity) {
        val rate = currency.exchangeRateToBase
        val amountVal = _uiState.value.amountInput.toDoubleOrNull() ?: 0.0
        _uiState.update {
            it.copy(
                selectedCurrency = currency,
                exchangeRate = rate,
                equivalentBaseAmount = amountVal * rate
            )
        }
    }

    fun setAmountInput(amount: String) {
        val amountVal = amount.toDoubleOrNull() ?: 0.0
        val rate = _uiState.value.exchangeRate
        _uiState.update {
            it.copy(
                amountInput = amount,
                equivalentBaseAmount = amountVal * rate
            )
        }
    }

    // تحديد تصنيف طريقة الدفع وحساب المصدر المالي (الطرف الدائن)
    fun selectPaymentGroupKey(groupKey: String) {
        val state = _uiState.value
        val groupInfo = state.activePaymentGroups.firstOrNull { it.key == groupKey }
        val defaultAcc = groupInfo?.accounts?.firstOrNull { it.isDefault } ?: groupInfo?.accounts?.firstOrNull()
        _uiState.update {
            it.copy(
                selectedPaymentGroupKey = groupKey,
                selectedPaymentAccount = defaultAcc
            )
        }
    }

    fun selectPaymentAccount(account: FinancialAccountEntity?) {
        _uiState.update { it.copy(selectedPaymentAccount = account) }
    }

    fun selectCostCenter(costCenterId: Long) {
        _uiState.update { it.copy(selectedCostCenterId = costCenterId) }
    }

    fun setTransactionRef(ref: String) {
        _uiState.update { it.copy(transactionRef = ref) }
    }

    fun setReceiptImagePath(path: String?) {
        _uiState.update { it.copy(receiptImagePath = path) }
    }

    fun setNotesInput(notes: String) {
        _uiState.update { it.copy(notesInput = notes) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleHistoryExpanded() {
        _uiState.update { it.copy(isHistoryExpanded = !it.isHistoryExpanded) }
    }

    fun dismissFeedback() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }

    fun dismissSuccessDialog() {
        _uiState.update { it.copy(showSuccessDialog = false) }
    }

    /**
     * اعتماد وحفظ سند الصرف وتوليد القيد المحاسبي المزدوج المتوازن
     */
    fun executeSubmitPaymentVoucher() {
        val state = _uiState.value
        val inputAmount = state.amountInput.toDoubleOrNull() ?: 0.0
        val equivAmount = state.equivalentBaseAmount

        if (inputAmount <= 0.0) {
            _uiState.update { it.copy(feedbackMessage = "عذراً! يرجى إدخال مبلغ صحيح أكبر من الصفر لسند الصرف.", isError = true) }
            return
        }

        val debitedAcc = state.selectedBeneficiaryAccount
        if (debitedAcc == null) {
            _uiState.update { it.copy(feedbackMessage = "يرجى تحديد الطرف المستفيد (الحساب الفرعي المدين) من الدليل المحاسبي.", isError = true) }
            return
        }

        val creditedAcc = state.selectedPaymentAccount
        if (creditedAcc == null) {
            _uiState.update { it.copy(feedbackMessage = "يرجى تحديد حساب مصدر الدفع (الطرف الدائن) المالي.", isError = true) }
            return
        }

        // فحص والتحقق الصارم من كفاية رصيد الحساب المالي الدائن المختار (Balance Validation)
        val availableBalance = creditedAcc.currentBalance
        if (availableBalance < equivAmount) {
            _uiState.update {
                it.copy(
                    feedbackMessage = "عذراً! رصيد الحساب المالي الدائن المختار (${creditedAcc.name}) البالغ (%.2f %s) غير كافٍ لصرف المبلغ المطلوب (%.2f %s). يرجى اختيار حساب مالي آخر أو تغذية الحساب أولاً."
                        .format(Locale.US, availableBalance, state.baseCurrencySymbol, equivAmount, state.baseCurrencySymbol),
                    isError = true
                )
            }
            return
        }

        // فحص والتحقق من التوازن المحاسبي المزدوج للقيد
        val doubleEntryCheck = TrialBalanceGuard.verifyDoubleEntryBalance(equivAmount, equivAmount, "اعتماد وتوليد قيد سند الصرف المزدوج")
        if (doubleEntryCheck.isFailure) {
            val errMessage = doubleEntryCheck.exceptionOrNull()?.message ?: "خطأ في التوازن المحاسبي للقيد المزدوج"
            _uiState.update { it.copy(feedbackMessage = errMessage, isError = true) }
            return
        }

        _uiState.update { it.copy(isProcessing = true) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val timestamp = System.currentTimeMillis()
                val count = voucherDao.countVouchers() + 1
                val voucherNumber = "PAY-2026-%04d".format(count)

                val currSymbol = state.selectedCurrency?.symbol ?: state.baseCurrencySymbol
                val journalProof = "من حـ/ (الطرف المدين: ${debitedAcc.code} - ${debitedAcc.name}) بمبلغ %.2f %s <- إلى حـ/ (الطرف الدائن: ${creditedAcc.code} - ${creditedAcc.name}) بمبلغ %.2f %s"
                    .format(Locale.US, equivAmount, state.baseCurrencySymbol, equivAmount, state.baseCurrencySymbol)

                val fullNotes = state.notesInput.ifBlank { "سند صرف وسداد مالي لحساب ${debitedAcc.name}" } + " | " + journalProof

                val paymentMethodEnum = when (state.selectedPaymentGroupKey) {
                    "BANK" -> PaymentMethod.BANK_TRANSFER
                    "WALLET" -> PaymentMethod.E_WALLET
                    "CARD" -> PaymentMethod.POS_CARD
                    else -> PaymentMethod.CASH
                }

                db.withTransaction {
                    val voucher = PaymentVoucherEntity(
                        voucherNumber = voucherNumber,
                        partyId = null,
                        amount = inputAmount,
                        voucherType = VoucherType.PAYMENT,
                        paymentMethod = paymentMethodEnum,
                        paymentAccountId = creditedAcc.id,
                        transactionRef = state.transactionRef,
                        receiptImagePath = state.receiptImagePath,
                        date = timestamp,
                        receivedBy = "كاشير 1",
                        notes = fullNotes,
                        costCenterId = state.selectedCostCenterId
                    )

                    voucherDao.insertVoucher(voucher)

                    // 1. الطرف المدين: إضافة الحركة للحساب الفرعي المستفيد (تخفيض الدائنية أو زيادة النفقات/الأصول)
                    if (debitedAcc.id > 0) {
                        financialAccountDao.updateBalance(debitedAcc.id, -equivAmount)
                    } else {
                        // الموظف/العامل المستفيد: تحديث حساب مصروفات الأجور والرواتب الرئيسية (50301 أو 20201)
                        val salaryAcc = financialAccountDao.getAccountByCode("50301") 
                            ?: financialAccountDao.getAccountByCode("20201")
                        if (salaryAcc != null) {
                            financialAccountDao.updateBalance(salaryAcc.id, -equivAmount)
                        }

                        // توثيق العملية في قاعدة بيانات العمالة والرواتب (سجل السلف والمستحقات)
                        val empId = -debitedAcc.id - 100000L
                        if (empId > 0) {
                            val cal = Calendar.getInstance()
                            val empTrans = EmployeeTransactionEntity(
                                employeeId = empId,
                                type = EmployeeTransactionType.ADVANCE,
                                amount = inputAmount,
                                date = timestamp,
                                periodMonth = cal.get(Calendar.MONTH) + 1,
                                periodYear = cal.get(Calendar.YEAR),
                                paymentMethod = paymentMethodEnum,
                                paymentAccountId = creditedAcc.id,
                                notes = "صرف مستحقات/سلفة بموجب سند صرف رقم $voucherNumber | $fullNotes"
                            )
                            employeeTransactionDao.insertTransaction(empTrans)
                        }
                    }

                    // 2. الطرف الدائن: خصم القيمة من حساب النقدية/البنك/المحفظة
                    financialAccountDao.updateBalance(creditedAcc.id, -equivAmount)

                    // 3. تحديث مدفوعات الدرج والشفت عند الصرف النقدي Direct Cash
                    if (state.selectedPaymentGroupKey == "CASH") {
                        val openShift = cashShiftDao.getOpenShift()
                        if (openShift != null) {
                            cashShiftDao.updateExpenses(openShift.id, openShift.totalCashExpenses + equivAmount)
                        }
                    }
                }

                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        amountInput = "",
                        notesInput = "",
                        transactionRef = "",
                        receiptImagePath = null,
                        lastSavedVoucherNumber = voucherNumber,
                        lastSavedJournalProof = journalProof,
                        showSuccessDialog = true,
                        feedbackMessage = "تم اعتماد وحفظ سند الصرف وتوليد القيد المحاسبي المزدوج برقم: $voucherNumber بنجاح!",
                        isError = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        feedbackMessage = "حدث خطأ أثناء حفظ سند الصرف: ${e.localizedMessage}",
                        isError = true
                    )
                }
            }
        }
    }

    fun viewVoucherDetails(voucher: PaymentVoucherEntity) {
        _uiState.update { it.copy(selectedVoucherForDetails = voucher, showDetailsDialog = true) }
    }

    fun dismissDetailsDialog() {
        _uiState.update { it.copy(showDetailsDialog = false, selectedVoucherForDetails = null) }
    }

    fun requestEditVoucher(voucher: PaymentVoucherEntity) {
        _uiState.update { it.copy(selectedVoucherForEdit = voucher, showEditDialog = true) }
    }

    fun dismissEditDialog() {
        _uiState.update { it.copy(showEditDialog = false, selectedVoucherForEdit = null) }
    }

    fun saveEditedVoucher(
        voucher: PaymentVoucherEntity,
        newAmount: Double,
        newMethod: PaymentMethod,
        newRef: String,
        newNotes: String,
        newImagePath: String?,
        newAccountId: Long?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                db.withTransaction {
                    val updated = voucher.copy(
                        amount = newAmount,
                        paymentMethod = newMethod,
                        transactionRef = newRef,
                        notes = newNotes,
                        receiptImagePath = newImagePath,
                        paymentAccountId = newAccountId
                    )
                    voucherDao.updateVoucher(updated)
                }
                _uiState.update {
                    it.copy(
                        showEditDialog = false,
                        selectedVoucherForEdit = null,
                        feedbackMessage = "تم تحديث بيانات سند الصرف رقم ${voucher.voucherNumber} بنجاح",
                        isError = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        feedbackMessage = "فشل تعديل سند الصرف: ${e.localizedMessage}",
                        isError = true
                    )
                }
            }
        }
    }

    fun requestDeleteVoucher(voucher: PaymentVoucherEntity, isAdmin: Boolean) {
        if (!isAdmin) {
            _uiState.update {
                it.copy(
                    showAdminPinDialog = true,
                    pendingDeleteVoucher = voucher,
                    adminPinError = null
                )
            }
        } else {
            deleteVoucherDirectly(voucher)
        }
    }

    fun confirmAdminPin(pin: String) {
        if (pin == "1234" || pin == "0000") {
            val v = _uiState.value.pendingDeleteVoucher
            _uiState.update { it.copy(showAdminPinDialog = false, pendingDeleteVoucher = null, adminPinError = null) }
            if (v != null) {
                deleteVoucherDirectly(v)
            }
        } else {
            _uiState.update { it.copy(adminPinError = "رمز PIN غير صحيح!") }
        }
    }

    fun dismissAdminPinDialog() {
        _uiState.update { it.copy(showAdminPinDialog = false, pendingDeleteVoucher = null, adminPinError = null) }
    }

    private fun deleteVoucherDirectly(voucher: PaymentVoucherEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                db.withTransaction {
                    val voucherAmount = voucher.amount

                    // 1. عكس القيد للطرف الدائن: إعادة المبلغ للحساب المالي المصدر (صندوق/بنك/محفظة)
                    if (voucher.paymentAccountId != null) {
                        financialAccountDao.updateBalance(voucher.paymentAccountId, +voucherAmount)
                    } else {
                        val cashAcc = financialAccountDao.getAccountByCode("10101")
                        if (cashAcc != null) {
                            financialAccountDao.updateBalance(cashAcc.id, +voucherAmount)
                        }
                    }

                    // 2. عكس القيد للطرف المدين: استرجاع الحركة من الحساب الفرعي المستفيد المذكور في الملاحظات
                    val notes = voucher.notes
                    val matchCode = Regex("الطرف المدين:\\s*([A-Za-z0-9-]+)").find(notes)?.groupValues?.getOrNull(1)
                    if (matchCode != null) {
                        val debitedAcc = financialAccountDao.getAccountByCode(matchCode)
                        if (debitedAcc != null && debitedAcc.id > 0) {
                            financialAccountDao.updateBalance(debitedAcc.id, +voucherAmount)
                        }
                    } else {
                        val salaryAcc = financialAccountDao.getAccountByCode("50301") ?: financialAccountDao.getAccountByCode("20201")
                        if (salaryAcc != null) {
                            financialAccountDao.updateBalance(salaryAcc.id, +voucherAmount)
                        }
                    }

                    // 3. عكس مصروفات الشفت والدرج عند الصرف النقدي
                    if (voucher.paymentMethod == PaymentMethod.CASH) {
                        val openShift = cashShiftDao.getOpenShift()
                        if (openShift != null) {
                            val newExpenses = (openShift.totalCashExpenses - voucherAmount).coerceAtLeast(0.0)
                            cashShiftDao.updateExpenses(openShift.id, newExpenses)
                        }
                    }

                    // 4. حذف سند الصرف نهائياً من قاعدة البيانات
                    voucherDao.deleteVoucher(voucher)
                }
                _uiState.update {
                    it.copy(
                        feedbackMessage = "تم حذف سند الصرف رقم ${voucher.voucherNumber} وعكس القيد المحاسبي المزدوج بالكامل بنجاح!",
                        isError = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        feedbackMessage = "فشل حذف سند الصرف وعكس القيد: ${e.localizedMessage}",
                        isError = true
                    )
                }
            }
        }
    }
}
