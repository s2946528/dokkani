package com.example.dokkani.ui.screens.vouchers

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.example.dokkani.data.local.DokkaniDatabase
import com.example.dokkani.data.local.entities.CostCenterEntity
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

data class ReceiptVoucherUiState(
    val customers: List<PartyEntity> = emptyList(),
    val selectedCustomer: PartyEntity? = null,
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

    // أرشيف سندات القبض
    val receiptVouchers: List<PaymentVoucherEntity> = emptyList(),
    val searchQuery: String = "",
    val isHistoryExpanded: Boolean = true,
    val selectedVoucherForDetails: PaymentVoucherEntity? = null,
    val showDetailsDialog: Boolean = false,
    val selectedVoucherForEdit: PaymentVoucherEntity? = null,
    val showEditDialog: Boolean = false,

    // حماية وصلاحيات مدير النظام
    val showAdminPinDialog: Boolean = false,
    val pendingDeleteVoucher: PaymentVoucherEntity? = null,
    val adminPinError: String? = null,

    // إضافة عميل جديد
    val showAddCustomerDialog: Boolean = false,

    // عملة النظام
    val baseCurrencySymbol: String = "ر.ي"
)

class ReceiptVoucherViewModel(application: Application) : AndroidViewModel(application) {
    private val db = DokkaniDatabase.getDatabase(application, viewModelScope)
    private val partyDao = db.partyDao()
    private val voucherDao = db.paymentVoucherDao()
    private val costCenterDao = db.costCenterDao()
    private val financialAccountDao = db.financialAccountDao()
    private val cashShiftDao = db.cashShiftDao()
    private val currencyDao = db.currencyDao()

    private val _uiState = MutableStateFlow(ReceiptVoucherUiState())
    val uiState: StateFlow<ReceiptVoucherUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch(Dispatchers.IO) {
            partyDao.getPartiesByType(PartyType.CUSTOMER).collectLatest { customerList ->
                _uiState.update { it.copy(customers = customerList) }
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
                val receiptsOnly = list.filter { it.voucherType == VoucherType.RECEIPT }
                _uiState.update { it.copy(receiptVouchers = receiptsOnly) }
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

    fun selectCustomer(customer: PartyEntity?) {
        _uiState.update { it.copy(selectedCustomer = customer) }
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

    fun toggleHistoryExpanded() {
        _uiState.update { it.copy(isHistoryExpanded = !it.isHistoryExpanded) }
    }

    fun dismissFeedback() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }

    fun dismissSuccessDialog() {
        _uiState.update { it.copy(showSuccessDialog = false) }
    }

    fun showAddCustomerDialog() {
        _uiState.update { it.copy(showAddCustomerDialog = true) }
    }

    fun dismissAddCustomerDialog() {
        _uiState.update { it.copy(showAddCustomerDialog = false) }
    }

    fun addQuickCustomer(name: String, phone: String) {
        if (name.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val customer = PartyEntity(
                name = name.trim(),
                phone = phone.trim(),
                type = PartyType.CUSTOMER,
                currentBalance = 0.0,
                creditLimit = 10000.0,
                notes = "إضافة سريعة من واجهة سندات القبض"
            )
            val newId = partyDao.insertParty(customer)
            val inserted = customer.copy(id = newId)
            _uiState.update { it.copy(selectedCustomer = inserted, showAddCustomerDialog = false) }
        }
    }

    fun executeSubmitReceiptVoucher() {
        val state = _uiState.value
        val amount = state.amountInput.toDoubleOrNull() ?: 0.0

        if (amount <= 0.0) {
            _uiState.update { it.copy(feedbackMessage = "يرجى إدخال مبلغ صحيح أكبر من الصفر لسند القبض.", isError = true) }
            return
        }

        _uiState.update { it.copy(isProcessing = true) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val timestamp = System.currentTimeMillis()
                val count = voucherDao.countVouchers() + 1
                val voucherNumber = "RCV-2026-%04d".format(count)

                db.withTransaction {
                    val voucher = PaymentVoucherEntity(
                        voucherNumber = voucherNumber,
                        partyId = state.selectedCustomer?.id,
                        amount = amount,
                        voucherType = VoucherType.RECEIPT,
                        paymentMethod = state.paymentMethod,
                        paymentAccountId = state.selectedAccountId,
                        transactionRef = state.transactionRef,
                        receiptImagePath = state.receiptImagePath,
                        date = timestamp,
                        receivedBy = "كاشير 1",
                        notes = state.notesInput.ifBlank { "سند قبض نقدية/حوالة من ${state.selectedCustomer?.name ?: "عميل نقدي عام"}" },
                        costCenterId = state.selectedCostCenterId
                    )

                    voucherDao.insertVoucher(voucher)

                    // تخفيض ديون العميل
                    state.selectedCustomer?.let { customer ->
                        partyDao.updateBalance(customer.id, -amount)
                    }

                    // إضافة الحصيلة للحساب المالي
                    state.selectedAccountId?.let { accId ->
                        financialAccountDao.updateBalance(accId, amount)
                    }

                    // تحديث الخزينة والشفت إن كان نقدياً
                    if (state.paymentMethod == PaymentMethod.CASH) {
                        val openShift = cashShiftDao.getOpenShift()
                        if (openShift != null) {
                            cashShiftDao.updateCollections(openShift.id, openShift.totalCashCollections + amount)
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
                        showSuccessDialog = true,
                        feedbackMessage = "تم حفظ وترحيل سند القبض برقم: $voucherNumber بنجاح!",
                        isError = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        feedbackMessage = "حدث خطأ أثناء حفظ سند القبض: ${e.localizedMessage}",
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
                        feedbackMessage = "تم تحديث بيانات سند القبض رقم ${voucher.voucherNumber} بنجاح",
                        isError = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        feedbackMessage = "فشل تعديل سند القبض: ${e.localizedMessage}",
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
                        feedbackMessage = "تم حذف سند القبض رقم ${voucher.voucherNumber} بنجاح",
                        isError = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        feedbackMessage = "فشل حذف سند القبض: ${e.localizedMessage}",
                        isError = true
                    )
                }
            }
        }
    }
}
