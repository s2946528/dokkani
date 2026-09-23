package com.example.dokkani.ui.screens.cash

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Warning
import com.example.dokkani.data.local.entities.FinancialAccountEntity
import com.example.dokkani.data.local.entities.FinancialAccountType
import com.example.dokkani.ui.AccountUsageCheckResult
import com.example.dokkani.ui.screens.accounts.AccountsManagementScreen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dokkani.data.local.entities.CashShiftEntity
import com.example.dokkani.data.local.entities.ExpenseEntity
import com.example.dokkani.data.local.entities.PaymentMethod
import com.example.dokkani.domain.cash.CashDiscrepancyType
import com.example.dokkani.domain.cash.CashReconciliationResult
import com.example.dokkani.domain.cash.ExpenseCategories
import com.example.dokkani.ui.DokkaniUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * شاشة حركة الخزينة والمصروفات ومطابقة الشفت (Cash & Expenses)
 */
@Composable
fun CashAndExpensesScreen(
    expenses: List<ExpenseEntity>,
    cashShifts: List<CashShiftEntity>,
    uiState: DokkaniUiState,
    currentUserRole: com.example.dokkani.data.local.entities.UserRole = com.example.dokkani.data.local.entities.UserRole.ADMIN,
    onSelectSubTab: (Int) -> Unit,
    onOpenAddExpenseDialog: () -> Unit,
    onDismissAddExpenseDialog: () -> Unit,
    onExpenseInputsChanged: (String, String, String, String, PaymentMethod) -> Unit,
    onSubmitExpense: () -> Unit,
    onDeleteExpense: (ExpenseEntity) -> Unit = {},
    onDrawerInputsChanged: (String, String, String) -> Unit,
    onCalculateDrawerReconciliation: () -> Unit,
    onCloseShiftAndSave: () -> Unit,
    onOpenShiftSettlementDialog: (CashShiftEntity) -> Unit = {},
    onDismissShiftSettlementDialog: () -> Unit = {},
    onUpdateSettlementInputs: (String, String) -> Unit = { _, _ -> },
    onSubmitShiftSettlement: () -> Unit = {},
    onAccountsSearchChanged: (String) -> Unit = {},
    onAccountsFilterTypeChanged: (FinancialAccountType?) -> Unit = {},
    onOpenAddAccountDialog: () -> Unit = {},
    onOpenEditAccountDialog: (FinancialAccountEntity) -> Unit = {},
    onDismissAddEditAccountDialog: () -> Unit = {},
    onSaveAccount: (FinancialAccountEntity) -> Unit = {},
    onToggleAccountActive: (FinancialAccountEntity) -> Unit = {},
    onRequestDeleteAccount: (FinancialAccountEntity) -> Unit = {},
    onConfirmDeleteAccount: (FinancialAccountEntity) -> Unit = {},
    onDisableAccountInstead: (AccountUsageCheckResult) -> Unit = {},
    onDismissAccountDeleteDialogs: () -> Unit = {}
) {
    LaunchedEffect(Unit) {
        onCalculateDrawerReconciliation()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(12.dp)
    ) {
        // شريط التبويبات الفرعية
        TabRow(
            selectedTabIndex = uiState.cashSubTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("cash_subtab_row")
        ) {
            Tab(
                selected = uiState.cashSubTab == 0,
                onClick = { onSelectSubTab(0) },
                text = { Text("المصروفات والنثريات", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                icon = { Icon(Icons.Default.MoneyOff, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("tab_expenses")
            )
            Tab(
                selected = uiState.cashSubTab == 1,
                onClick = { onSelectSubTab(1) },
                text = { Text("مطابقة الصندوق وإغلاق الشفت", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                icon = { Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("tab_shift_reconciliation")
            )
            Tab(
                selected = uiState.cashSubTab == 2,
                onClick = { onSelectSubTab(2) },
                text = { Text("إدارة الحسابات والبنوك (الدليل)", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                icon = { Icon(Icons.Default.AccountTree, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("tab_accounts_management")
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        when (uiState.cashSubTab) {
            0 -> {
                // محتوى تبويب المصروفات والنثريات
                ExpensesContent(
                    expenses = expenses,
                    onOpenAddDialog = onOpenAddExpenseDialog,
                    currencySymbol = uiState.currencySymbol
                )
            }
            1 -> {
                // محتوى تبويب مطابقة الصندوق وإغلاق الشفت
                ShiftReconciliationContent(
                    uiState = uiState,
                    cashShifts = cashShifts,
                    currentUserRole = currentUserRole,
                    onDrawerInputsChanged = onDrawerInputsChanged,
                    onCalculateReconciliation = onCalculateDrawerReconciliation,
                    onCloseShiftAndSave = onCloseShiftAndSave,
                    onOpenShiftSettlementDialog = onOpenShiftSettlementDialog
                )
            }
            else -> {
                // محتوى تبويب إدارة الحسابات والبنوك والدليل المحاسبي
                AccountsManagementScreen(
                    uiState = uiState,
                    onSearchChanged = onAccountsSearchChanged,
                    onFilterTypeChanged = onAccountsFilterTypeChanged,
                    onOpenAddDialog = onOpenAddAccountDialog,
                    onOpenEditDialog = onOpenEditAccountDialog,
                    onDismissAddEditDialog = onDismissAddEditAccountDialog,
                    onSaveAccount = onSaveAccount,
                    onToggleActive = onToggleAccountActive,
                    onRequestDeleteAccount = onRequestDeleteAccount,
                    onConfirmDeleteAccount = onConfirmDeleteAccount,
                    onDisableInstead = onDisableAccountInstead,
                    onDismissDeleteDialogs = onDismissAccountDeleteDialogs
                )
            }
        }
    }

    // نافذة تسجيل مصروف جديد
    if (uiState.showAddExpenseDialog) {
        AddExpenseDialog(
            category = uiState.expenseCategoryInput,
            amount = uiState.expenseAmountInput,
            paidTo = uiState.expensePaidToInput,
            notes = uiState.expenseNotesInput,
            selectedMethod = uiState.expensePaymentMethod,
            isSubmitting = uiState.isSubmittingExpense,
            onInputsChanged = onExpenseInputsChanged,
            onDismiss = onDismissAddExpenseDialog,
            onSubmit = onSubmitExpense,
            currencySymbol = uiState.currencySymbol
        )
    }

    // نافذة تسوية المدير للفروقات والجرد
    if (uiState.showSettlementDialog && uiState.selectedShiftForSettlement != null) {
        ShiftSettlementDialog(
            shift = uiState.selectedShiftForSettlement,
            actionType = uiState.settlementActionType,
            notes = uiState.settlementNotesInput,
            isSubmitting = uiState.isSubmittingSettlement,
            onInputsChanged = onUpdateSettlementInputs,
            onDismiss = onDismissShiftSettlementDialog,
            onSubmit = onSubmitShiftSettlement,
            currencySymbol = uiState.currencySymbol
        )
    }
}

/**
 * واجهة المصروفات والنثريات
 */
@Composable
private fun ExpensesContent(
    expenses: List<ExpenseEntity>,
    onOpenAddDialog: () -> Unit,
    currencySymbol: String = "ر.ي"
) {
    val totalExpenses = expenses.sumOf { it.amount }
    val cashExpenses = expenses.filter { it.paymentMethod == PaymentMethod.CASH }.sumOf { it.amount }
    val bankExpenses = expenses.filter { it.paymentMethod != PaymentMethod.CASH }.sumOf { it.amount }

    Column(modifier = Modifier.fillMaxSize()) {
        // بطاقات المؤشرات المالية للمصروفات
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ExpenseKpiCard(
                title = "إجمالي المصروفات",
                value = "${"%.2f".format(totalExpenses)} $currencySymbol",
                subtitle = "${expenses.size} عملية مسجلة",
                backgroundColor = Color(0xFFFEF2F2),
                textColor = Color(0xFFDC2626),
                modifier = Modifier.weight(1f)
            )
            ExpenseKpiCard(
                title = "مسدد نقداً من الدرج",
                value = "${"%.2f".format(cashExpenses)} $currencySymbol",
                subtitle = "يخصم من نقدية الصندوق",
                backgroundColor = Color(0xFFFFFBEB),
                textColor = Color(0xFFD97706),
                modifier = Modifier.weight(1f)
            )
            ExpenseKpiCard(
                title = "مسدد شبكة / بنك",
                value = "${"%.2f".format(bankExpenses)} $currencySymbol",
                subtitle = "حوالات وبطاقات",
                backgroundColor = Color(0xFFEFF6FF),
                textColor = Color(0xFF2563EB),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // زر إضافة مصروف جديد
        Button(
            onClick = onOpenAddDialog,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("btn_add_expense")
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("تسجيل مصروف تشغيلي جديد", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // قائمة المصروفات المسجلة
        Text(
            text = "سجل المصروفات والنثريات التشغيلية:",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Color(0xFF334155)
        )
        Spacer(modifier = Modifier.height(6.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(expenses, key = { it.id }) { item ->
                ExpenseItemCard(expense = item, currencySymbol = currencySymbol)
            }

            if (expenses.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("لا توجد مصروفات مسجلة حتى الآن", color = Color(0xFF64748B))
                    }
                }
            }
        }
    }
}

/**
 * بطاقة إحصائية للمصروفات
 */
@Composable
private fun ExpenseKpiCard(
    title: String,
    value: String,
    subtitle: String,
    backgroundColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(text = title, fontSize = 11.sp, color = Color(0xFF64748B))
            Text(text = value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textColor)
            Text(text = subtitle, fontSize = 10.sp, color = Color(0xFF94A3B8), maxLines = 1)
        }
    }
}

/**
 * بطاقة تفاصيل عنصر المصروف
 */
@Composable
private fun ExpenseItemCard(expense: ExpenseEntity, currencySymbol: String = "ر.ي") {
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd hh:mm a", Locale.getDefault()) }
    val dateString = remember(expense.date) { dateFormat.format(Date(expense.date)) }

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.MoneyOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = expense.expenseNumber,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = expense.category,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (expense.paidTo.isNotBlank()) {
                        Text(
                            text = "المدفوع له: ${expense.paidTo}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (expense.notes.isNotBlank()) {
                        Text(
                            text = expense.notes,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Text(
                        text = "$dateString • بواسطة ${expense.recordedBy}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            // المبلغ وطريقة الدفع
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "-${"%.2f".format(expense.amount)} $currencySymbol",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = expense.paymentMethod.labelArabic,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * نافذة حوار إضافة مصروف تشغيلي
 */
@Composable
private fun AddExpenseDialog(
    category: String,
    amount: String,
    paidTo: String,
    notes: String,
    selectedMethod: PaymentMethod,
    isSubmitting: Boolean,
    onInputsChanged: (String, String, String, String, PaymentMethod) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit,
    currencySymbol: String = "ر.ي"
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("تسجيل مصروف تشغيلي", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // تصنيفات المصروف السريعة
                Text("التصنيف:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(ExpenseCategories.ALL) { cat ->
                        val isSelected = cat == category
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) Color(0xFF0F5132) else Color(0xFFF1F5F9),
                            modifier = Modifier.clickable {
                                onInputsChanged(cat, amount, paidTo, notes, selectedMethod)
                            }
                        ) {
                            Text(
                                text = cat,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else Color(0xFF334155),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // حقل المبلغ
                OutlinedTextField(
                    value = amount,
                    onValueChange = { onInputsChanged(category, it, paidTo, notes, selectedMethod) },
                    label = { Text("مبلغ المصروف (${currencySymbol})*") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_amount_field")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // طريقة الصرف
                Text("طريقة الصرف:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(PaymentMethod.CASH, PaymentMethod.MADA, PaymentMethod.BANK_TRANSFER).forEach { method ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                onInputsChanged(category, amount, paidTo, notes, method)
                            }
                        ) {
                            RadioButton(
                                selected = selectedMethod == method,
                                onClick = { onInputsChanged(category, amount, paidTo, notes, method) }
                            )
                            Text(text = method.labelArabic, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // المدفوع له
                OutlinedTextField(
                    value = paidTo,
                    onValueChange = { onInputsChanged(category, amount, it, notes, selectedMethod) },
                    label = { Text("المدفوع له (اسم العامل / المحل / الفني)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                // البيان والملاحظات
                OutlinedTextField(
                    value = notes,
                    onValueChange = { onInputsChanged(category, amount, paidTo, it, selectedMethod) },
                    label = { Text("البيان والسبب") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSubmit,
                enabled = !isSubmitting && (amount.toDoubleOrNull() ?: 0.0) > 0.0,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                modifier = Modifier.testTag("btn_confirm_expense")
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                } else {
                    Text("حفظ المصروف", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

/**
 * واجهة مطابقة الصندوق وإغلاق الشفت (Z-Report / Cash Shift Reconciliation)
 */
@Composable
private fun ShiftReconciliationContent(
    uiState: DokkaniUiState,
    cashShifts: List<CashShiftEntity>,
    currentUserRole: com.example.dokkani.data.local.entities.UserRole = com.example.dokkani.data.local.entities.UserRole.ADMIN,
    onDrawerInputsChanged: (String, String, String) -> Unit,
    onCalculateReconciliation: () -> Unit,
    onCloseShiftAndSave: () -> Unit,
    onOpenShiftSettlementDialog: (CashShiftEntity) -> Unit = {}
) {
    val recon = uiState.reconciliationResult

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // --- 1. تفاصيل الواردات والصادرات والجرد المتوقع (Z-Report Table) ---
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Calculate,
                                contentDescription = null,
                                tint = Color(0xFF0F5132),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "تقرير Z المحاسبي وإغلاق الشفت اللحظي",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFF0F172A)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFE2E8F0)
                        ) {
                            Text(
                                text = "الشفت الحالي المفتوح",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF334155),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // --- قسم 1: بنود الواردات (إجمالي الدخل النقدي) ---
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "🟢 الواردات النقدية (إجمالي الدخل النقدي)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF166534)
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            InflowOutflowRow(
                                label = "العهدة الافتتاحية (رصيد بداية اليوم/الشفت)",
                                amount = recon?.openingCash ?: 200.0,
                                currencySymbol = uiState.currencySymbol,
                                isHighlight = false
                            )
                            InflowOutflowRow(
                                label = "المبيعات النقدية المسددة كاش",
                                amount = recon?.totalCashSales ?: 0.0,
                                currencySymbol = uiState.currencySymbol,
                                count = recon?.cashSalesCount ?: 0,
                                countUnit = "فاتورة"
                            )
                            InflowOutflowRow(
                                label = "سندات القبض النقدية (مقبوضات العملاء)",
                                amount = recon?.totalCashCollections ?: 0.0,
                                currencySymbol = uiState.currencySymbol,
                                count = recon?.cashCollectionsCount ?: 0,
                                countUnit = "سند"
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = Color(0xFFBBF7D0))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("إجمالي الدخل والوارد النقدي:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF14532D))
                                Text(
                                    "${"%.2f".format(recon?.totalInflows ?: 0.0)} ${uiState.currencySymbol}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF15803D)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // --- قسم 2: بنود الصادرات (إجمالي الخرج النقدي) ---
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "🔴 الصادرات النقدية (إجمالي الخرج النقدي)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF991B1B)
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            InflowOutflowRow(
                                label = "المصروفات التشغيلية النقدية",
                                amount = recon?.totalCashExpenses ?: 0.0,
                                currencySymbol = uiState.currencySymbol,
                                count = recon?.cashExpensesCount ?: 0,
                                countUnit = "مصروف",
                                isOutflow = true
                            )
                            InflowOutflowRow(
                                label = "تسديد الموردين نقداً (سندات الصرف)",
                                amount = recon?.totalSupplierPayments ?: 0.0,
                                currencySymbol = uiState.currencySymbol,
                                count = recon?.supplierPaymentsCount ?: 0,
                                countUnit = "سند",
                                isOutflow = true
                            )
                            InflowOutflowRow(
                                label = "المشتريات النقدية من الدرج",
                                amount = recon?.totalCashPurchases ?: 0.0,
                                currencySymbol = uiState.currencySymbol,
                                count = recon?.cashPurchasesCount ?: 0,
                                countUnit = "فاتورة",
                                isOutflow = true
                            )
                            InflowOutflowRow(
                                label = "مسحوبات صاحب البقالة النقدية (حـ/المسحوبات)",
                                amount = recon?.totalOwnerDrawings ?: 0.0,
                                currencySymbol = uiState.currencySymbol,
                                count = recon?.ownerDrawingsCount ?: 0,
                                countUnit = "حركة",
                                isOutflow = true
                            )
                            InflowOutflowRow(
                                label = "سلف ومسحوبات العمال والموظفين (عهدة موظف)",
                                amount = recon?.totalStaffAdvances ?: 0.0,
                                currencySymbol = uiState.currencySymbol,
                                count = recon?.staffAdvancesCount ?: 0,
                                countUnit = "سلفة",
                                isOutflow = true
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = Color(0xFFFECACA))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("إجمالي الخرج والنقدية الصادرة:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF7F1D1D))
                                Text(
                                    "-${"%.2f".format(recon?.totalOutflows ?: 0.0)} ${uiState.currencySymbol}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFFB91C1C)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // --- قسم 3: صافي النقدية الدفترية المتوقعة بالدرج ---
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F5132)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "صافي النقدية الدفترية المتوقعة بالدرج:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "(العهدة الافتتاحية + إجمالي الوارد - إجمالي الخرج)",
                                    fontSize = 10.sp,
                                    color = Color(0xFFA7F3D0)
                                )
                            }
                            Text(
                                text = "${"%.2f".format(recon?.expectedCashInDrawer ?: 0.0)} ${uiState.currencySymbol}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color(0xFFFEF08A)
                            )
                        }
                    }
                }
            }
        }

        // مدخلات جرد النقدية باليد
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "جرد النقدية الفعلي وإغلاق الوردية:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = uiState.drawerOpeningCashInput,
                            onValueChange = {
                                onDrawerInputsChanged(it, uiState.drawerPhysicalCashInput, uiState.drawerShiftNotesInput)
                            },
                            label = { Text("العهدة الافتتاحية (${uiState.currencySymbol})") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("drawer_opening_cash_field")
                        )

                        OutlinedTextField(
                            value = uiState.drawerPhysicalCashInput,
                            onValueChange = {
                                onDrawerInputsChanged(uiState.drawerOpeningCashInput, it, uiState.drawerShiftNotesInput)
                            },
                            label = { Text("النقد الفعلي المجرود باليد (${uiState.currencySymbol})*") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1.3f)
                                .testTag("drawer_physical_cash_field")
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = uiState.drawerShiftNotesInput,
                        onValueChange = {
                            onDrawerInputsChanged(uiState.drawerOpeningCashInput, uiState.drawerPhysicalCashInput, it)
                        },
                        label = { Text("ملاحظات إغلاق الشفت والتسليم (اختياري)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onCalculateReconciliation,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("تحديث المطابقة اللحظية")
                        }

                        Button(
                            onClick = onCloseShiftAndSave,
                            enabled = !uiState.isClosingShift && uiState.drawerPhysicalCashInput.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F5132)),
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("btn_close_shift")
                        ) {
                            if (uiState.isClosingShift) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                            } else {
                                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("إغلاق الشفت واعتماد المطابقة", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // نتيجة المطابقة اللحظية (عجز / زيادة / مطابق)
        if (recon != null) {
            item {
                ReconciliationResultCard(result = recon, currencySymbol = uiState.currencySymbol)
            }
        }

        // سجل الشفتات السابقة وإغلاقات الصندوق
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "سجل تقارير الشفتات والإغلاقات السابقة (Z-Reports):",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFF334155)
            )
        }

        items(cashShifts, key = { it.id }) { shift ->
            ClosedShiftCard(
                shift = shift,
                currencySymbol = uiState.currencySymbol,
                currentUserRole = currentUserRole,
                onOpenSettlement = { onOpenShiftSettlementDialog(shift) }
            )
        }

        if (cashShifts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("لا توجد شفتات مغلقة مسبقاً", color = Color(0xFF64748B))
                }
            }
        }
    }
}

@Composable
private fun InflowOutflowRow(
    label: String,
    amount: Double,
    currencySymbol: String,
    count: Int = -1,
    countUnit: String = "",
    isOutflow: Boolean = false,
    isHighlight: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = label, fontSize = 11.sp, color = Color(0xFF334155))
            if (count >= 0) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "($count $countUnit)", fontSize = 10.sp, color = Color(0xFF64748B))
            }
        }
        val prefix = if (isOutflow && amount > 0) "-" else ""
        val color = if (isOutflow) Color(0xFFDC2626) else if (isHighlight) Color(0xFF16A34A) else Color(0xFF0F172A)
        Text(
            text = "$prefix${"%.2f".format(amount)} $currencySymbol",
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            color = color
        )
    }
}

/**
 * بطاقة نتيجة المطابقة المباشرة
 */
@Composable
private fun ReconciliationResultCard(result: CashReconciliationResult, currencySymbol: String = "ر.ي") {
    val bgColor: Color
    val textColor: Color
    val title: String
    val icon: androidx.compose.ui.graphics.vector.ImageVector

    when (result.discrepancyType) {
        CashDiscrepancyType.MATCHED -> {
            bgColor = Color(0xFFF0FDF4)
            textColor = Color(0xFF16A34A)
            title = "مطابق تماماً - لا يوجد أي عجز أو زيادة في الصندوق"
            icon = Icons.Default.CheckCircle
        }
        CashDiscrepancyType.SHORTAGE -> {
            bgColor = Color(0xFFFEF2F2)
            textColor = Color(0xFFDC2626)
            title = "يوجد عجز نقدي في الصندوق (نقص في الدرج)"
            icon = Icons.Default.Warning
        }
        CashDiscrepancyType.SURPLUS -> {
            bgColor = Color(0xFFEFF6FF)
            textColor = Color(0xFF2563EB)
            title = "توجد زيادة نقدية في الصندوق (فائض في الدرج)"
            icon = Icons.Default.CheckCircle
        }
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = textColor
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "الدفتر المتوقع: ${"%.2f".format(result.expectedCashInDrawer)} $currencySymbol", fontSize = 12.sp, color = Color(0xFF475569))
                Text(text = "المجرود الفعلي: ${"%.2f".format(result.actualPhysicalCash)} $currencySymbol", fontSize = 12.sp, color = Color(0xFF475569))
                Text(
                    text = "الفارق: ${if (result.discrepancy >= 0) "+" else ""}${"%.2f".format(result.discrepancy)} $currencySymbol",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = textColor
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = result.discrepancyType.labelArabic,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
        }
    }
}

/**
 * بطاقة الشفت المغلق سابقاً
 */
@Composable
private fun ClosedShiftCard(
    shift: CashShiftEntity,
    currencySymbol: String = "ر.ي",
    currentUserRole: com.example.dokkani.data.local.entities.UserRole = com.example.dokkani.data.local.entities.UserRole.ADMIN,
    onOpenSettlement: () -> Unit = {}
) {
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd hh:mm a", Locale.getDefault()) }
    val closedDateString = remember(shift.endTime) {
        shift.endTime?.let { dateFormat.format(Date(it)) } ?: "مفتوح"
    }

    val canSettle = currentUserRole == com.example.dokkani.data.local.entities.UserRole.ADMIN &&
            shift.cashDiscrepancy != 0.0 &&
            shift.settlementStatus == "UNSETTLED"

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = shift.shiftNumber,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF0F172A)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (shift.cashDiscrepancy == 0.0) Color(0xFFDCFCE7) else if (shift.cashDiscrepancy < 0) Color(0xFFFEE2E2) else Color(0xFFDBEAFE)
                        ) {
                            Text(
                                text = if (shift.cashDiscrepancy == 0.0) "مطابق تماماً" else if (shift.cashDiscrepancy < 0) "عجز ${"%.2f".format(shift.cashDiscrepancy)}" else "زيادة +${"%.2f".format(shift.cashDiscrepancy)}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (shift.cashDiscrepancy == 0.0) Color(0xFF16A34A) else if (shift.cashDiscrepancy < 0) Color(0xFFDC2626) else Color(0xFF2563EB),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        if (shift.settlementStatus != "UNSETTLED") {
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFF1F5F9)
                            ) {
                                Text(
                                    text = when(shift.settlementStatus) {
                                        "SETTLED_EXPENSE" -> "تمت التسوية (مصروف)"
                                        "WAIVED" -> "تمت التسوية (ملغى)"
                                        "SETTLED_STAFF" -> "تمت التسوية (خصم كاشير)"
                                        "SETTLED_SURPLUS" -> "تمت التسوية (إيراد)"
                                        else -> "تمت التسوية"
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F5132),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = "الكاشير: ${shift.cashierName} • تم الإغلاق: $closedDateString",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "الفعلي: ${"%.2f".format(shift.actualPhysicalCash)} $currencySymbol",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "المتوقع: ${"%.2f".format(shift.expectedCashInDrawer)} $currencySymbol",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }

            // ملخص الحركة
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("الافتتاحية: ${"%.2f".format(shift.openingCash)}", fontSize = 10.sp, color = Color(0xFF64748B))
                Text("الوارد: +${"%.2f".format(shift.totalInflows)}", fontSize = 10.sp, color = Color(0xFF16A34A))
                Text("الخرج: -${"%.2f".format(shift.totalOutflows)}", fontSize = 10.sp, color = Color(0xFFDC2626))
            }

            if (shift.notes.isNotBlank() || shift.settlementNotes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                if (shift.notes.isNotBlank()) {
                    Text(text = "ملاحظات الشفت: ${shift.notes}", fontSize = 10.sp, color = Color(0xFF475569))
                }
                if (shift.settlementNotes.isNotBlank()) {
                    Text(text = "بيان التسوية: ${shift.settlementNotes}", fontSize = 10.sp, color = Color(0xFF0F5132), fontWeight = FontWeight.SemiBold)
                }
            }

            if (canSettle) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onOpenSettlement,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تسوية الفروقات بالجرد (مدير النظام)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * نافذة تسوية الفروقات والجرد من مدير النظام
 */
@Composable
private fun ShiftSettlementDialog(
    shift: CashShiftEntity,
    actionType: String,
    notes: String,
    isSubmitting: Boolean,
    onInputsChanged: (String, String) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit,
    currencySymbol: String = "ر.ي"
) {
    val isShortage = shift.cashDiscrepancy < 0
    val amount = kotlin.math.abs(shift.cashDiscrepancy)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("تسوية فروقات الجرد لشفت: ${shift.shiftNumber}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = if (isShortage) Color(0xFFFEF2F2) else Color(0xFFEFF6FF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isShortage) Icons.Default.Warning else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (isShortage) Color(0xFFDC2626) else Color(0xFF2563EB),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (isShortage) "عجز نقدي بالصندوق بمبلغ: ${"%.2f".format(amount)} $currencySymbol" else "زيادة نقدية بالصندوق بمبلغ: ${"%.2f".format(amount)} $currencySymbol",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isShortage) Color(0xFF991B1B) else Color(0xFF1E40AF)
                            )
                            Text("الكاشير المسؤول: ${shift.cashierName}", fontSize = 11.sp, color = Color(0xFF475569))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("خيارات المعالجة والتسوية المحاسبية:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                Spacer(modifier = Modifier.height(6.dp))

                if (isShortage) {
                    SettlementOptionRow(
                        title = "تحويل العجز لمصروف تشغيلي",
                        subtitle = "تُرحل قيمة العجز لحساب (نثريات وفروقات درج) كمصروف",
                        selected = actionType == "EXPENSE",
                        onClick = { onInputsChanged("EXPENSE", notes) }
                    )
                    SettlementOptionRow(
                        title = "خصم العجز كعهدة وسلفة على الكاشير",
                        subtitle = "تُسجل كعهدة/سلفة مستحقة على الموظف الكاشير (${shift.cashierName})",
                        selected = actionType == "STAFF_CUSTODY",
                        onClick = { onInputsChanged("STAFF_CUSTODY", notes) }
                    )
                    SettlementOptionRow(
                        title = "إلغاء/تسوية الفارق كخطأ قيد أو سند غير مقيد",
                        subtitle = "تُعتمد التسوية دون قيد مصروف إضافي (اكتشاف سند صرف سليم)",
                        selected = actionType == "WAIVED",
                        onClick = { onInputsChanged("WAIVED", notes) }
                    )
                } else {
                    SettlementOptionRow(
                        title = "تقييد الزيادة كإيراد صندوق متنوع",
                        subtitle = "تُسجل الزيادة كإيرادات وأرباح متنوعة للخزينة",
                        selected = actionType == "EXTRA_INCOME",
                        onClick = { onInputsChanged("EXTRA_INCOME", notes) }
                    )
                    SettlementOptionRow(
                        title = "إلغاء/تسوية الفارق كخطأ قيد",
                        subtitle = "تُسوى الزيادة دون تسجيل قيد إيراد (خطأ قيد قبض)",
                        selected = actionType == "WAIVED",
                        onClick = { onInputsChanged("WAIVED", notes) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { onInputsChanged(actionType, it) },
                    label = { Text("بيان وملاحظات التسوية من المدير") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSubmit,
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F5132))
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                } else {
                    Text("اعتماد وتسوية الجرد", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
private fun SettlementOptionRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = if (selected) Color(0xFFF0FDF4) else Color(0xFFF8FAFC)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = selected, onClick = onClick)
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (selected) Color(0xFF0F5132) else Color(0xFF1E293B))
                Text(text = subtitle, fontSize = 10.sp, color = Color(0xFF64748B))
            }
        }
    }
}
