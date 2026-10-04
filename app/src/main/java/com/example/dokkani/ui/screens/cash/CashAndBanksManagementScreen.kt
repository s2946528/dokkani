package com.example.dokkani.ui.screens.cash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dokkani.data.local.entities.FinancialAccountEntity
import com.example.dokkani.data.local.entities.FinancialAccountType
import com.example.dokkani.data.local.entities.UserRole
import com.example.dokkani.ui.components.AppSearchBar
import com.example.dokkani.ui.components.NumericOutlinedTextField
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashAndBanksManagementScreen(
    currentUserRole: UserRole,
    onNavigateBack: () -> Unit = {},
    viewModel: CashAndBanksViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // رسالة التغذية الراجعة والتنبيهات
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.feedbackMessage) {
        uiState.feedbackMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissFeedback()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (currentUserRole == UserRole.ADMIN || currentUserRole == UserRole.CASHIER) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.openAddDialog(FinancialAccountType.CASH_DRAWER) },
                    icon = { Icon(Icons.Default.Add, contentDescription = "إضافة صندوق أو بنك جديد") },
                    text = { Text("إضافة صندوق / بنك / محفظة", fontWeight = FontWeight.Bold) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("fab_add_cash_bank")
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. بطاقات المؤشرات والسيولة النقدية الشاملة
            CashAndBanksKpiSummaryHeader(
                totalCash = uiState.totalCashBalance,
                totalBank = uiState.totalBankBalance,
                totalWallet = uiState.totalWalletBalance,
                grandTotal = uiState.grandTotalBalance,
                currencySymbol = uiState.baseCurrencySymbol
            )

            // 2. شريط البحث المباشر
            AppSearchBar(
                value = uiState.searchQuery,
                onValueChange = viewModel::setSearchQuery,
                placeholder = "ابحث بالاسم، رقم الحساب، IBAN، أو الكود..."
            )

            // 3. شريط أزرار التصفية الفئوية القابل للسحب الأفقي (Horizontal Scrollable Tabs)
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(CashAndBankCategoryFilter.values()) { cat ->
                    val isSelected = uiState.selectedCategory == cat
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setSelectedCategory(cat) },
                        label = {
                            Text(
                                text = cat.labelArabic,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            // 4. قائمة الصناديق والبنوك المتاحة
            if (uiState.filteredAccounts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (uiState.searchQuery.isBlank()) "لا توجد حسابات صناديق أو بنوك مضافة حالياً" else "لا توجد نتائج تطابق بحثك",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = uiState.filteredAccounts,
                        key = { it.id }
                    ) { account ->
                        CashAndBankAccountCard(
                            account = account,
                            currencySymbol = uiState.baseCurrencySymbol,
                            canManage = currentUserRole == UserRole.ADMIN || currentUserRole == UserRole.CASHIER,
                            onEdit = { viewModel.openEditDialog(account) },
                            onDelete = { viewModel.requestDeleteAccount(account) }
                        )
                    }
                }
            }
        }
    }

    // نافذة إضافة أو تعديل حساب صندوق/بنك
    if (uiState.showAddEditDialog) {
        AddEditCashBankDialog(
            editingAccount = uiState.editingAccount,
            defaultType = uiState.defaultAccountType,
            currencySymbol = uiState.baseCurrencySymbol,
            onDismiss = viewModel::dismissAddEditDialog,
            onSave = { type, name, accNum, openingBal, isDef, isActive, notes ->
                viewModel.saveFundOrBank(type, name, accNum, openingBal, isDef, isActive, notes)
            }
        )
    }

    // حوار تأكيد الحذف القاطع (Delete Confirmation Dialog)
    if (uiState.showDeleteConfirmDialog && uiState.accountToDelete != null) {
        val accToDelete = uiState.accountToDelete!!
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteDialog,
            icon = {
                Icon(
                    Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "حذف الحساب المالي",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "هل أنت متأكد من رغبتك في حذف الحساب (${accToDelete.name}) رقم كود (${accToDelete.code})؟ لا يمكن التراجع عن هذه العملية.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = viewModel::confirmDeleteAccount,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("حذف نهائي", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = viewModel::dismissDeleteDialog) {
                    Text("إلغاء")
                }
            }
        )
    }
}

/**
 * شريط المؤشرات والسيولة النقدية والمصرفية
 */
@Composable
private fun CashAndBanksKpiSummaryHeader(
    totalCash: Double,
    totalBank: Double,
    totalWallet: Double,
    grandTotal: Double,
    currencySymbol: String
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MonetizationOn,
                        contentDescription = null,
                        tint = Color(0xFF15803D),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "إجمالي السيولة النقدية والمصرفية المتوفرة",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "%.2f %s".format(Locale.US, grandTotal, currencySymbol),
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    color = if (grandTotal >= 0) Color(0xFF15803D) else Color(0xFFC2410C)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // الصناديق
                KpiSubItem(
                    label = "الصناديق والدرج",
                    amount = totalCash,
                    currencySymbol = currencySymbol,
                    icon = Icons.Default.Payments,
                    color = Color(0xFF2E7D32)
                )

                // البنوك
                KpiSubItem(
                    label = "الحسابات البنكية",
                    amount = totalBank,
                    currencySymbol = currencySymbol,
                    icon = Icons.Default.AccountBalance,
                    color = Color(0xFF1565C0)
                )

                // المحافظ
                KpiSubItem(
                    label = "المحافظ الإلكترونية",
                    amount = totalWallet,
                    currencySymbol = currencySymbol,
                    icon = Icons.Default.AccountBalanceWallet,
                    color = Color(0xFF6A1B9A)
                )
            }
        }
    }
}

@Composable
private fun KpiSubItem(
    label: String,
    amount: Double,
    currencySymbol: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "%.2f %s".format(Locale.US, amount, currencySymbol),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

/**
 * بطاقة عرض بيانات الصندوق/البنك المالي مع أزرار التعديل والحذف المنظمة
 */
@Composable
private fun CashAndBankAccountCard(
    account: FinancialAccountEntity,
    currencySymbol: String,
    canManage: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val (typeColor, typeIcon, typeTitle) = when (account.accountType) {
        FinancialAccountType.BANK -> Triple(Color(0xFF1565C0), Icons.Default.AccountBalance, "حساب بنكي")
        FinancialAccountType.E_WALLET -> Triple(Color(0xFF6A1B9A), Icons.Default.AccountBalanceWallet, "محفظة إلكترونية")
        else -> Triple(Color(0xFF2E7D32), Icons.Default.Payments, "صندوق / درج كاش")
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (account.isActive) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        border = BorderStroke(1.dp, if (account.isDefault) typeColor else MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_cash_bank_${account.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // الصف العلوي: الأيقونة والتصنيف، الاسم، وحالة التفعيل
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(typeColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = typeIcon, contentDescription = null, tint = typeColor, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = account.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (account.isDefault) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = typeColor.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "افتراضي",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = typeColor,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "كود: ${account.code} | $typeTitle | ${account.parentAccountName}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // شارة الحالة (نشط / معطل)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (account.isActive) Color(0xFFDCFCE7) else Color(0xFFF1F5F9)
                ) {
                    Text(
                        text = if (account.isActive) "نشط" else "معطل",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (account.isActive) Color(0xFF15803D) else Color(0xFF64748B),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (account.accountNumber.isNotBlank() || account.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                if (account.accountNumber.isNotBlank()) {
                    Text(
                        text = "رقم الحساب / IBAN: ${account.accountNumber}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (account.notes.isNotBlank()) {
                    Text(
                        text = "ملاحظات: ${account.notes}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(10.dp))

            // الصف السفلي: الرصيد الحالي وأزرار التحكم (تعديل + حذف)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("الرصيد الفعلي الحالي:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "%.2f %s".format(Locale.US, account.currentBalance, currencySymbol),
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = if (account.currentBalance >= 0) typeColor else Color(0xFFC2410C)
                    )
                }

                if (canManage) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = onEdit,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier
                                .height(34.dp)
                                .testTag("btn_edit_account_${account.id}")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تعديل", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onDelete,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error,
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier
                                .height(34.dp)
                                .testTag("btn_delete_account_${account.id}")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("حذف", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * نافذة حوار إضافة أو تعديل صندوق/بنك
 */
@Composable
private fun AddEditCashBankDialog(
    editingAccount: FinancialAccountEntity?,
    defaultType: FinancialAccountType,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (
        type: FinancialAccountType,
        name: String,
        accountNumber: String,
        openingBalance: Double,
        isDefault: Boolean,
        isActive: Boolean,
        notes: String
    ) -> Unit
) {
    var selectedType by remember { mutableStateOf(editingAccount?.accountType ?: defaultType) }
    var nameInput by remember { mutableStateOf(editingAccount?.name ?: "") }
    var accountNumberInput by remember { mutableStateOf(editingAccount?.accountNumber ?: "") }
    var openingBalanceInput by remember { mutableStateOf(editingAccount?.openingBalance?.toString() ?: "0.0") }
    var isDefaultInput by remember { mutableStateOf(editingAccount?.isDefault ?: false) }
    var isActiveInput by remember { mutableStateOf(editingAccount?.isActive ?: true) }
    var notesInput by remember { mutableStateOf(editingAccount?.notes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (editingAccount == null) "إضافة صندوق / بنك / محفظة جديدة" else "تعديل بيانات الحساب المالي",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("نوع الحساب المالي:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = selectedType == FinancialAccountType.CASH_DRAWER,
                        onClick = { selectedType = FinancialAccountType.CASH_DRAWER },
                        label = { Text("صندوق", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    )
                    FilterChip(
                        selected = selectedType == FinancialAccountType.BANK,
                        onClick = { selectedType = FinancialAccountType.BANK },
                        label = { Text("بنك", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    )
                    FilterChip(
                        selected = selectedType == FinancialAccountType.E_WALLET,
                        onClick = { selectedType = FinancialAccountType.E_WALLET },
                        label = { Text("محفظة", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    )
                }

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("اسم الحساب (مثال: الصندوق الرئيسي / بنك الكريمي)", fontSize = 11.sp) },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (selectedType == FinancialAccountType.BANK || selectedType == FinancialAccountType.E_WALLET) {
                    OutlinedTextField(
                        value = accountNumberInput,
                        onValueChange = { accountNumberInput = it },
                        label = { Text("رقم الحساب / IBAN / المحفظة", fontSize = 11.sp) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                NumericOutlinedTextField(
                    value = openingBalanceInput,
                    onValueChange = { openingBalanceInput = it },
                    label = { Text("الرصيد الافتتاحي الأول ($currencySymbol)", fontSize = 11.sp) },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("تعيين كحساب افتراضي للصرف والتحصيل", fontSize = 12.sp)
                    Switch(checked = isDefaultInput, onCheckedChange = { isDefaultInput = it })
                }

                OutlinedTextField(
                    value = notesInput,
                    onValueChange = { notesInput = it },
                    label = { Text("ملاحظات إضافية", fontSize = 11.sp) },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val opening = openingBalanceInput.toDoubleOrNull() ?: 0.0
                    onSave(selectedType, nameInput, accountNumberInput, opening, isDefaultInput, isActiveInput, notesInput)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("حفظ البيانات", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
