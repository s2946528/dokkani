package com.example.dokkani.ui.screens.cash

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Warning
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
    onCloseShiftAndSave: () -> Unit
) {
    LaunchedEffect(Unit) {
        onCalculateDrawerReconciliation()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
            .padding(12.dp)
    ) {
        // شريط التبويبات الفرعية
        TabRow(
            selectedTabIndex = uiState.cashSubTab,
            containerColor = Color.White,
            contentColor = Color(0xFF0F5132),
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
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (uiState.cashSubTab == 0) {
            // محتوى تبويب المصروفات والنثريات
            ExpensesContent(
                expenses = expenses,
                onOpenAddDialog = onOpenAddExpenseDialog
            )
        } else {
            // محتوى تبويب مطابقة الصندوق وإغلاق الشفت
            ShiftReconciliationContent(
                uiState = uiState,
                cashShifts = cashShifts,
                onDrawerInputsChanged = onDrawerInputsChanged,
                onCalculateReconciliation = onCalculateDrawerReconciliation,
                onCloseShiftAndSave = onCloseShiftAndSave
            )
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
            onSubmit = onSubmitExpense
        )
    }
}

/**
 * واجهة المصروفات والنثريات
 */
@Composable
private fun ExpensesContent(
    expenses: List<ExpenseEntity>,
    onOpenAddDialog: () -> Unit
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
                value = "${"%.2f".format(totalExpenses)} ر.س",
                subtitle = "${expenses.size} عملية مسجلة",
                backgroundColor = Color(0xFFFEF2F2),
                textColor = Color(0xFFDC2626),
                modifier = Modifier.weight(1f)
            )
            ExpenseKpiCard(
                title = "مسدد نقداً من الدرج",
                value = "${"%.2f".format(cashExpenses)} ر.س",
                subtitle = "يخصم من نقدية الصندوق",
                backgroundColor = Color(0xFFFFFBEB),
                textColor = Color(0xFFD97706),
                modifier = Modifier.weight(1f)
            )
            ExpenseKpiCard(
                title = "مسدد شبكة / بنك",
                value = "${"%.2f".format(bankExpenses)} ر.س",
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
                ExpenseItemCard(expense = item)
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
private fun ExpenseItemCard(expense: ExpenseEntity) {
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd hh:mm a", Locale.getDefault()) }
    val dateString = remember(expense.date) { dateFormat.format(Date(expense.date)) }

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
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
                    color = Color(0xFFFEF2F2),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.MoneyOff,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
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
                            color = Color(0xFF0F172A)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFE2E8F0)
                        ) {
                            Text(
                                text = expense.category,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF334155),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (expense.paidTo.isNotBlank()) {
                        Text(
                            text = "المدفوع له: ${expense.paidTo}",
                            fontSize = 12.sp,
                            color = Color(0xFF475569)
                        )
                    }
                    if (expense.notes.isNotBlank()) {
                        Text(
                            text = expense.notes,
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                    Text(
                        text = "$dateString • بواسطة ${expense.recordedBy}",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            // المبلغ وطريقة الدفع
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "-${"%.2f".format(expense.amount)} ر.س",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFFDC2626)
                )
                Text(
                    text = expense.paymentMethod.labelArabic,
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
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
    onSubmit: () -> Unit
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
                    label = { Text("مبلغ المصروف (ر.س)*") },
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
 * واجهة مطابقة الصندوق وإغلاق الشفت (Cash Shift Reconciliation)
 */
@Composable
private fun ShiftReconciliationContent(
    uiState: DokkaniUiState,
    cashShifts: List<CashShiftEntity>,
    onDrawerInputsChanged: (String, String, String) -> Unit,
    onCalculateReconciliation: () -> Unit,
    onCloseShiftAndSave: () -> Unit
) {
    val recon = uiState.reconciliationResult

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // بطاقة معادلة المطابقة الدفترية
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0D3B2E)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = null,
                            tint = Color(0xFF34D399),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "معادلة مطابقة النقدية اللحظية بالدرج",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("العهدة الافتتاحية", fontSize = 11.sp, color = Color(0xFFD1E7DD))
                            Text(
                                text = "${"%.2f".format(recon?.openingCash ?: 200.0)} ر.س",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                        Column {
                            Text("+ مبيعات نقدية", fontSize = 11.sp, color = Color(0xFFD1E7DD))
                            Text(
                                text = "${"%.2f".format(recon?.totalCashSales ?: 0.0)} ر.س",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF86EFAC),
                                fontSize = 14.sp
                            )
                        }
                        Column {
                            Text("+ مقبوضات ديون", fontSize = 11.sp, color = Color(0xFFD1E7DD))
                            Text(
                                text = "${"%.2f".format(recon?.totalCashCollections ?: 0.0)} ر.س",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF86EFAC),
                                fontSize = 14.sp
                            )
                        }
                        Column {
                            Text("- مصروفات درج", fontSize = 11.sp, color = Color(0xFFD1E7DD))
                            Text(
                                text = "-${"%.2f".format(recon?.totalCashExpenses ?: 0.0)} ر.س",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFCA5A5),
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = Color(0xFF198754))
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "النقدية الدفترية المتوقعة في الصندوق:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                        Text(
                            text = "${"%.2f".format(recon?.expectedCashInDrawer ?: 0.0)} ر.س",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFFFEF08A)
                        )
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
                        text = "جرد النقدية الفعلي ومطابقة الشفت:",
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
                            label = { Text("العهدة الافتتاحية (ر.س)") },
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
                            label = { Text("النقد الفعلي بالدرج (ر.س)*") },
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
                        label = { Text("ملاحظات إغلاق الشفت (اختياري)") },
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
                ReconciliationResultCard(result = recon)
            }
        }

        // سجل الشفتات السابقة وإغلاقات الصندوق
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "سجل الشفتات المغلقة سابقاً:",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFF334155)
            )
        }

        items(cashShifts, key = { it.id }) { shift ->
            ClosedShiftCard(shift = shift)
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

/**
 * بطاقة نتيجة المطابقة المباشرة
 */
@Composable
private fun ReconciliationResultCard(result: CashReconciliationResult) {
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
                Text(text = "الدفتر المتوقع: ${"%.2f".format(result.expectedCashInDrawer)} ر.س", fontSize = 12.sp, color = Color(0xFF475569))
                Text(text = "المجرود الفعلي: ${"%.2f".format(result.actualPhysicalCash)} ر.س", fontSize = 12.sp, color = Color(0xFF475569))
                Text(
                    text = "الفارق: ${if (result.discrepancy >= 0) "+" else ""}${"%.2f".format(result.discrepancy)} ر.س",
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
private fun ClosedShiftCard(shift: CashShiftEntity) {
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd hh:mm a", Locale.getDefault()) }
    val closedDateString = remember(shift.endTime) {
        shift.endTime?.let { dateFormat.format(Date(it)) } ?: "مفتوح"
    }

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = shift.shiftNumber,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (shift.cashDiscrepancy == 0.0) Color(0xFFDCFCE7) else if (shift.cashDiscrepancy < 0) Color(0xFFFEE2E2) else Color(0xFFDBEAFE)
                    ) {
                        Text(
                            text = if (shift.cashDiscrepancy == 0.0) "مطابق" else if (shift.cashDiscrepancy < 0) "عجز ${"%.2f".format(shift.cashDiscrepancy)}" else "فائض +${"%.2f".format(shift.cashDiscrepancy)}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (shift.cashDiscrepancy == 0.0) Color(0xFF16A34A) else if (shift.cashDiscrepancy < 0) Color(0xFFDC2626) else Color(0xFF2563EB),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = "الكاشير: ${shift.cashierName} • تم الإغلاق: $closedDateString",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )

                if (shift.notes.isNotBlank()) {
                    Text(text = shift.notes, fontSize = 10.sp, color = Color(0xFF94A3B8))
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "الفعلي: ${"%.2f".format(shift.actualPhysicalCash)} ر.س",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "المتوقع: ${"%.2f".format(shift.expectedCashInDrawer)} ر.س",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}
