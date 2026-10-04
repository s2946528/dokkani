package com.example.dokkani.ui.screens.vouchers

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.example.dokkani.data.local.DokkaniDatabase
import com.example.dokkani.data.local.entities.CostCenterEntity
import com.example.dokkani.data.local.entities.CurrencyEntity
import com.example.dokkani.data.local.entities.FinancialAccountEntity
import com.example.dokkani.data.local.entities.PartyEntity
import com.example.dokkani.data.local.entities.PartyType
import com.example.dokkani.data.local.entities.PaymentMethod
import com.example.dokkani.data.local.entities.PaymentVoucherEntity
import com.example.dokkani.data.local.entities.VoucherType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VouchersUiState(
    val selectedVoucherType: VoucherType = VoucherType.RECEIPT,
    val parties: List<PartyEntity> = emptyList(),
    val selectedParty: PartyEntity? = null,
    val amountInput: String = "",
    val costCenters: List<CostCenterEntity> = emptyList(),
    val selectedCostCenterId: Long = 1L,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val financialAccounts: List<FinancialAccountEntity> = emptyList(),
    val selectedAccountId: Long? = null,
    val transactionRef: String = "",
    val receiptImagePath: String? = null,
    val notesInput: String = "",
    val isProcessing: Boolean = false,
    val feedbackMessage: String? = null,
    val isError: Boolean = false,
    val showSuccessDialog: Boolean = false,
    val lastSavedVoucherNumber: String? = null,

    // أرشيف واستعراض السندات
    val allVouchers: List<PaymentVoucherEntity> = emptyList(),
    val searchQuery: String = "",
    val historyFilter: String = "ALL", // "ALL", "RECEIPT", "PAYMENT"
    val isHistoryExpanded: Boolean = true,
    val selectedVoucherForDetails: PaymentVoucherEntity? = null,
    val showDetailsDialog: Boolean = false,
    val selectedVoucherForEdit: PaymentVoucherEntity? = null,
    val showEditDialog: Boolean = false,

    // حماية وصلاحيات مدير النظام
    val showAdminPinDialog: Boolean = false,
    val pendingDeleteVoucher: PaymentVoucherEntity? = null,
    val adminPinError: String? = null,

    // إضافة طرف جديد
    val showAddPartyDialog: Boolean = false,

    // عملة النظام
    val baseCurrencySymbol: String = "ر.ي"
)

class VouchersViewModel(application: Application) : AndroidViewModel(application) {
    private val db = DokkaniDatabase.getDatabase(application, viewModelScope)
    private val partyDao = db.partyDao()
    private val voucherDao = db.paymentVoucherDao()
    private val costCenterDao = db.costCenterDao()
    private val financialAccountDao = db.financialAccountDao()
    private val cashShiftDao = db.cashShiftDao()
    private val currencyDao = db.currencyDao()

    private val _uiState = MutableStateFlow(VouchersUiState())
    val uiState: StateFlow<VouchersUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch(Dispatchers.IO) {
            partyDao.getAllParties().collectLatest { partyList ->
                _uiState.update { it.copy(parties = partyList) }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            costCenterDao.getAllCostCenters().collectLatest { centers ->
                _uiState.update { it.copy(costCenters = centers) }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            financialAccountDao.getAllAccounts().collectLatest { accounts ->
                _uiState.update { state ->
                    val defaultAccount = accounts.firstOrNull { it.isDefault } ?: accounts.firstOrNull()
                    state.copy(
                        financialAccounts = accounts,
                        selectedAccountId = state.selectedAccountId ?: defaultAccount?.id
                    )
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            voucherDao.getAllVouchers().collectLatest { list ->
                _uiState.update { it.copy(allVouchers = list) }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            currencyDao.getAllCurrencies().collectLatest { currencies ->
                val base = currencies.firstOrNull { it.isBaseCurrency }
                if (base != null) {
                    _uiState.update { it.copy(baseCurrencySymbol = base.symbol) }
                }
            }
        }
    }

    fun setVoucherType(type: VoucherType) {
        _uiState.update { it.copy(selectedVoucherType = type, selectedParty = null) }
    }

    fun selectParty(party: PartyEntity?) {
        _uiState.update { it.copy(selectedParty = party) }
    }

    fun setAmountInput(amount: String) {
        _uiState.update { it.copy(amountInput = amount) }
    }

    fun selectCostCenter(costCenterId: Long) {
        _uiState.update { it.copy(selectedCostCenterId = costCenterId) }
    }

    fun setPaymentMethod(method: PaymentMethod) {
        _uiState.update { it.copy(paymentMethod = method) }
    }

    fun setSelectedAccountId(accountId: Long?) {
        _uiState.update { it.copy(selectedAccountId = accountId) }
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

    fun setHistoryFilter(filter: String) {
        _uiState.update { it.copy(historyFilter = filter) }
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

    fun showAddPartyDialog() {
        _uiState.update { it.copy(showAddPartyDialog = true) }
    }

    fun dismissAddPartyDialog() {
        _uiState.update { it.copy(showAddPartyDialog = false) }
    }

    fun addQuickParty(name: String, phone: String, type: PartyType) {
        if (name.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val party = PartyEntity(
                name = name.trim(),
                phone = phone.trim(),
                type = type,
                currentBalance = 0.0,
                creditLimit = 10000.0,
                notes = "إضافة سريعة من شاشة السندات"
            )
            val newId = partyDao.insertParty(party)
            val inserted = party.copy(id = newId)
            _uiState.update { it.copy(selectedParty = inserted, showAddPartyDialog = false) }
        }
    }

    fun executeSubmitVoucher() {
        val state = _uiState.value
        val amount = state.amountInput.toDoubleOrNull() ?: 0.0

        if (amount <= 0.0) {
            _uiState.update { it.copy(feedbackMessage = "يرجى إدخال مبلغ صحيح أكبر من الصفر للسند.", isError = true) }
            return
        }

        _uiState.update { it.copy(isProcessing = true) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val timestamp = System.currentTimeMillis()
                val count = voucherDao.countVouchers() + 1
                val prefix = if (state.selectedVoucherType == VoucherType.RECEIPT) "RCV" else "PAY"
                val voucherNumber = "$prefix-2026-%04d".format(count)

                db.withTransaction {
                    val voucher = PaymentVoucherEntity(
                        voucherNumber = voucherNumber,
                        partyId = state.selectedParty?.id,
                        amount = amount,
                        voucherType = state.selectedVoucherType,
                        paymentMethod = state.paymentMethod,
                        paymentAccountId = state.selectedAccountId,
                        transactionRef = state.transactionRef,
                        receiptImagePath = state.receiptImagePath,
                        date = timestamp,
                        receivedBy = "كاشير 1",
                        notes = state.notesInput.ifBlank {
                            if (state.selectedVoucherType == VoucherType.RECEIPT) {
                                "سند قبض من ${state.selectedParty?.name ?: "نقدي عام"}"
                            } else {
                                "سند صرف إلى ${state.selectedParty?.name ?: "مصروفات عامة"}"
                            }
                        },
                        costCenterId = state.selectedCostCenterId
                    )

                    voucherDao.insertVoucher(voucher)

                    if (state.selectedVoucherType == VoucherType.RECEIPT) {
                        // سند قبض: يقلل دين العميل (أو يرفع رصيده الدائن)
                        state.selectedParty?.let { party ->
                            partyDao.updateBalance(party.id, -amount)
                        }

                        // زيادة رصيد الحساب المالي المختار
                        state.selectedAccountId?.let { accId ->
                            financialAccountDao.updateBalance(accId, amount)
                        }

                        // تحديث الخزينة والشفت النشط إذا كان نقدياً
                        if (state.paymentMethod == PaymentMethod.CASH) {
                            val openShift = cashShiftDao.getOpenShift()
                            if (openShift != null) {
                                cashShiftDao.updateCollections(openShift.id, openShift.totalCashCollections + amount)
                            }
                        }
                    } else {
                        // سند صرف: يقلل التزام المورد (أو يزيد مديونيته)
                        state.selectedParty?.let { party ->
                            partyDao.updateBalance(party.id, amount)
                        }

                        // خصم رصيد الحساب المالي المختار
                        state.selectedAccountId?.let { accId ->
                            financialAccountDao.updateBalance(accId, -amount)
                        }

                        // تحديث الخزينة والشفت النشط إذا كان نقدياً
                        if (state.paymentMethod == PaymentMethod.CASH) {
                            val openShift = cashShiftDao.getOpenShift()
                            if (openShift != null) {
                                cashShiftDao.updateExpenses(openShift.id, openShift.totalCashExpenses + amount)
                            }
                        }
                    }
                }

                val typeLabel = if (state.selectedVoucherType == VoucherType.RECEIPT) "سند القبض" else "سند الصرف"

                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        amountInput = "",
                        notesInput = "",
                        transactionRef = "",
                        receiptImagePath = null,
                        lastSavedVoucherNumber = voucherNumber,
                        showSuccessDialog = true,
                        feedbackMessage = "تم حفظ وترحيل $typeLabel برقم: $voucherNumber بنجاح!",
                        isError = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        feedbackMessage = "حدث خطأ أثناء حفظ السند: ${e.localizedMessage}",
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
                        feedbackMessage = "تم تحديث بيانات السند رقم ${voucher.voucherNumber} بنجاح",
                        isError = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        feedbackMessage = "فشل تعديل السند: ${e.localizedMessage}",
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
                    voucherDao.deleteVoucher(voucher)
                }
                _uiState.update {
                    it.copy(
                        feedbackMessage = "تم حذف السند رقم ${voucher.voucherNumber} بنجاح",
                        isError = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        feedbackMessage = "فشل حذف السند: ${e.localizedMessage}",
                        isError = true
                    )
                }
            }
        }
    }
}
