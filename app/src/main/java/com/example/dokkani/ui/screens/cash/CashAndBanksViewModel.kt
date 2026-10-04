package com.example.dokkani.ui.screens.cash

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dokkani.data.local.DokkaniDatabase
import com.example.dokkani.data.local.entities.ChartOfAccountsDefaults
import com.example.dokkani.data.local.entities.FinancialAccountEntity
import com.example.dokkani.data.local.entities.FinancialAccountType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * فئات التصفية الحصرية والمختصرة لشاشة إدارة الصناديق والبنوك
 */
enum class CashAndBankCategoryFilter(val labelArabic: String) {
    ALL("الكل"),
    CASH("الصناديق"),
    BANK("البنوك"),
    WALLET("المحافظ"),
    CARD("الشبكات")
}

data class CashAndBanksUiState(
    val accounts: List<FinancialAccountEntity> = emptyList(),
    val filteredAccounts: List<FinancialAccountEntity> = emptyList(),
    val selectedCategory: CashAndBankCategoryFilter = CashAndBankCategoryFilter.ALL,
    val searchQuery: String = "",
    val totalCashBalance: Double = 0.0,
    val totalBankBalance: Double = 0.0,
    val totalWalletBalance: Double = 0.0,
    val grandTotalBalance: Double = 0.0,
    val showAddEditDialog: Boolean = false,
    val editingAccount: FinancialAccountEntity? = null,
    val accountToDelete: FinancialAccountEntity? = null,
    val showDeleteConfirmDialog: Boolean = false,
    val defaultAccountType: FinancialAccountType = FinancialAccountType.CASH_DRAWER,
    val feedbackMessage: String? = null,
    val isError: Boolean = false,
    val baseCurrencySymbol: String = "ر.ي"
)

class CashAndBanksViewModel(application: Application) : AndroidViewModel(application) {
    private val db = DokkaniDatabase.getDatabase(application, viewModelScope)
    private val financialAccountDao = db.financialAccountDao()
    private val currencyDao = db.currencyDao()

    private val _uiState = MutableStateFlow(CashAndBanksUiState())
    val uiState: StateFlow<CashAndBanksUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        // 1. جلب عملة النظام الأساسية
        viewModelScope.launch(Dispatchers.IO) {
            currencyDao.getAllCurrencies().collectLatest { currList ->
                val baseCurr = currList.firstOrNull { it.isBaseCurrency }?.symbol ?: "ر.ي"
                _uiState.update { it.copy(baseCurrencySymbol = baseCurr) }
            }
        }

        // 2. جلب جميع حسابات الصناديق والبنوك والمحافظ دون المساس بالدليل العام
        viewModelScope.launch(Dispatchers.IO) {
            financialAccountDao.getAllAccounts().collectLatest { allAccounts ->
                val fundAndBankAccounts = allAccounts.filter { acc ->
                    !acc.isMainAccount && (
                        acc.accountType == FinancialAccountType.CASH_DRAWER ||
                        acc.accountType == FinancialAccountType.BANK ||
                        acc.accountType == FinancialAccountType.E_WALLET ||
                        acc.parentAccountCode in listOf("101", "102", "103") ||
                        acc.code.startsWith("101") || acc.code.startsWith("102") || acc.code.startsWith("103")
                    )
                }

                val cashTotal = fundAndBankAccounts
                    .filter { (it.accountType == FinancialAccountType.CASH_DRAWER || it.parentAccountCode == "101") && !it.name.contains("شبكة") && !it.name.contains("مدى") }
                    .sumOf { it.currentBalance }

                val bankTotal = fundAndBankAccounts
                    .filter { it.accountType == FinancialAccountType.BANK || it.parentAccountCode == "102" }
                    .sumOf { it.currentBalance }

                val walletTotal = fundAndBankAccounts
                    .filter { it.accountType == FinancialAccountType.E_WALLET || it.parentAccountCode == "103" }
                    .sumOf { it.currentBalance }

                val grandTotal = cashTotal + bankTotal + walletTotal

                _uiState.update { state ->
                    val filtered = filterAccountsList(fundAndBankAccounts, state.selectedCategory, state.searchQuery)
                    state.copy(
                        accounts = fundAndBankAccounts,
                        filteredAccounts = filtered,
                        totalCashBalance = cashTotal,
                        totalBankBalance = bankTotal,
                        totalWalletBalance = walletTotal,
                        grandTotalBalance = grandTotal
                    )
                }
            }
        }
    }

    private fun filterAccountsList(
        list: List<FinancialAccountEntity>,
        category: CashAndBankCategoryFilter,
        query: String
    ): List<FinancialAccountEntity> {
        val cleanQuery = query.trim().lowercase()
        return list.filter { acc ->
            val matchesCategory = when (category) {
                CashAndBankCategoryFilter.ALL -> true
                CashAndBankCategoryFilter.CASH -> (acc.accountType == FinancialAccountType.CASH_DRAWER || acc.parentAccountCode == "101") &&
                        !acc.name.contains("شبكة") && !acc.name.contains("مدى") && !acc.name.contains("بطاقة")
                CashAndBankCategoryFilter.BANK -> acc.accountType == FinancialAccountType.BANK || acc.parentAccountCode == "102"
                CashAndBankCategoryFilter.WALLET -> acc.accountType == FinancialAccountType.E_WALLET || acc.parentAccountCode == "103"
                CashAndBankCategoryFilter.CARD -> acc.name.contains("شبكة") || acc.name.contains("مدى") || acc.name.contains("بطاقة") || acc.code.startsWith("10104") || acc.code.startsWith("10304")
            }

            val matchesSearch = cleanQuery.isBlank() ||
                acc.name.lowercase().contains(cleanQuery) ||
                acc.code.lowercase().contains(cleanQuery) ||
                acc.accountNumber.lowercase().contains(cleanQuery) ||
                acc.notes.lowercase().contains(cleanQuery)

            matchesCategory && matchesSearch
        }.sortedBy { it.code }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { state ->
            val filtered = filterAccountsList(state.accounts, state.selectedCategory, query)
            state.copy(searchQuery = query, filteredAccounts = filtered)
        }
    }

    fun setSelectedCategory(category: CashAndBankCategoryFilter) {
        _uiState.update { state ->
            val filtered = filterAccountsList(state.accounts, category, state.searchQuery)
            state.copy(selectedCategory = category, filteredAccounts = filtered)
        }
    }

    fun openAddDialog(type: FinancialAccountType = FinancialAccountType.CASH_DRAWER) {
        _uiState.update {
            it.copy(
                showAddEditDialog = true,
                editingAccount = null,
                defaultAccountType = type
            )
        }
    }

    fun openEditDialog(account: FinancialAccountEntity) {
        _uiState.update {
            it.copy(
                showAddEditDialog = true,
                editingAccount = account,
                defaultAccountType = account.accountType
            )
        }
    }

    fun dismissAddEditDialog() {
        _uiState.update { it.copy(showAddEditDialog = false, editingAccount = null) }
    }

    fun requestDeleteAccount(account: FinancialAccountEntity) {
        _uiState.update {
            it.copy(
                accountToDelete = account,
                showDeleteConfirmDialog = true
            )
        }
    }

    fun dismissDeleteDialog() {
        _uiState.update {
            it.copy(
                accountToDelete = null,
                showDeleteConfirmDialog = false
            )
        }
    }

    fun confirmDeleteAccount() {
        val account = _uiState.value.accountToDelete ?: return
        if (Math.abs(account.currentBalance) > 0.001) {
            _uiState.update {
                it.copy(
                    showDeleteConfirmDialog = false,
                    accountToDelete = null,
                    feedbackMessage = "عذراً! لا يمكن حذف الحساب المالي (${account.name}) نظراً لوجود رصيد قائم البالغ (%.2f %s). يرجى تحويل أو تسوية الرصيد أولاً قبل الحذف."
                        .format(Locale.US, account.currentBalance, _uiState.value.baseCurrencySymbol),
                    isError = true
                )
            }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                financialAccountDao.deleteAccount(account)
                _uiState.update {
                    it.copy(
                        showDeleteConfirmDialog = false,
                        accountToDelete = null,
                        feedbackMessage = "تم حذف الحساب المالي (${account.name}) بنجاح.",
                        isError = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        showDeleteConfirmDialog = false,
                        accountToDelete = null,
                        feedbackMessage = "فشل حذف الحساب المالي: ${e.localizedMessage ?: "خطأ غير معروف"}",
                        isError = true
                    )
                }
            }
        }
    }

    fun dismissFeedback() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }

    fun saveFundOrBank(
        type: FinancialAccountType,
        name: String,
        accountNumber: String,
        openingBalance: Double,
        isDefault: Boolean,
        isActive: Boolean,
        notes: String
    ) {
        if (name.isBlank()) {
            _uiState.update { it.copy(feedbackMessage = "يرجى إدخال اسم الصندوق أو البنك قبل الحفظ.", isError = true) }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val editing = _uiState.value.editingAccount
                if (editing != null) {
                    val updated = editing.copy(
                        name = name.trim(),
                        accountType = type,
                        accountNumber = accountNumber.trim(),
                        openingBalance = openingBalance,
                        currentBalance = editing.currentBalance + (openingBalance - editing.openingBalance),
                        isDefault = isDefault,
                        isActive = isActive,
                        notes = notes.trim()
                    )
                    financialAccountDao.updateAccount(updated)
                    _uiState.update {
                        it.copy(
                            showAddEditDialog = false,
                            editingAccount = null,
                            feedbackMessage = "تم تحديث بيانات ${type.labelArabic} (${name.trim()}) بنجاح.",
                            isError = false
                        )
                    }
                } else {
                    val parentCode = when (type) {
                        FinancialAccountType.BANK -> "102"
                        FinancialAccountType.E_WALLET -> "103"
                        else -> "101"
                    }

                    val parentName = ChartOfAccountsDefaults.PARENT_ACCOUNTS
                        .firstOrNull { it.code == parentCode }?.name
                        ?: when (type) {
                            FinancialAccountType.BANK -> "102 - البنوك والمصارف التجارية"
                            FinancialAccountType.E_WALLET -> "103 - محافظ الدفع والتحصيل الإلكتروني"
                            else -> "101 - النقدية وما في حكمها (الصناديق والدرج)"
                        }

                    val existingSubAccounts = _uiState.value.accounts.filter { it.parentAccountCode == parentCode || it.code.startsWith(parentCode) }
                    val nextSubNumber = existingSubAccounts.size + 1
                    val generatedCode = "%s%02d".format(parentCode, nextSubNumber)

                    val newAccount = FinancialAccountEntity(
                        code = generatedCode,
                        name = name.trim(),
                        accountType = type,
                        parentAccountCode = parentCode,
                        parentAccountName = parentName,
                        isMainAccount = false,
                        level = 2,
                        finalAccountMapping = "BALANCE_SHEET",
                        debitCreditNature = "DEBIT",
                        accountNumber = accountNumber.trim(),
                        openingBalance = openingBalance,
                        currentBalance = openingBalance,
                        currency = _uiState.value.baseCurrencySymbol,
                        isActive = isActive,
                        isDefault = isDefault,
                        notes = notes.trim()
                    )

                    financialAccountDao.insertAccount(newAccount)

                    _uiState.update {
                        it.copy(
                            showAddEditDialog = false,
                            editingAccount = null,
                            feedbackMessage = "تم إضافة ${type.labelArabic} جديد (${name.trim()}) برقم كود $generatedCode بنجاح.",
                            isError = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        feedbackMessage = "حدث خطأ أثناء حفظ الحساب: ${e.localizedMessage ?: "خطأ غير معروف"}",
                        isError = true
                    )
                }
            }
        }
    }
}
