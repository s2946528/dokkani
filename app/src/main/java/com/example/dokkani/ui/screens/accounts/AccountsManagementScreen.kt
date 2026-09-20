package com.example.dokkani.ui.screens.accounts

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dokkani.data.local.entities.FinancialAccountEntity
import com.example.dokkani.data.local.entities.FinancialAccountType
import com.example.dokkani.ui.DokkaniUiState
import java.util.Locale
import kotlin.math.abs

/**
 * واجهة تحكم متكاملة لإدارة الحسابات (بنوك، محافظ إلكترونية، صناديق، وحسابات الدليل المحاسبي)
 * تتيح الإضافة، التعديل، والحذف مع تفعيل شرط الأمان المحاسبي وحماية الحسابات المرتبطة بسجلات.
 */
@Composable
fun AccountsManagementScreen(
    uiState: DokkaniUiState,
    onSearchChanged: (String) -> Unit,
    onFilterTypeChanged: (FinancialAccountType?) -> Unit,
    onOpenAddDialog: () -> Unit,
    onOpenEditDialog: (FinancialAccountEntity) -> Unit,
    onDismissAddEditDialog: () -> Unit,
    onSaveAccount: (FinancialAccountEntity) -> Unit,
    onToggleActive: (FinancialAccountEntity) -> Unit,
    onRequestDeleteAccount: (FinancialAccountEntity) -> Unit,
    onConfirmDeleteAccount: (FinancialAccountEntity) -> Unit,
    onDisableInstead: (com.example.dokkani.ui.AccountUsageCheckResult) -> Unit,
    onDismissDeleteDialogs: () -> Unit
) {
    val accounts = uiState.financialAccounts
    val searchQuery = uiState.accountsSearchQuery
    val filterType = uiState.accountsFilterType
    val currencySymbol = uiState.currencySymbol

    // تصفية الحسابات حسب البحث والنوع
    val filteredAccounts = remember(accounts, searchQuery, filterType) {
        accounts.filter { acc ->
            val matchesFilter = filterType == null || acc.accountType == filterType
            val matchesSearch = searchQuery.isBlank() ||
                    acc.name.contains(searchQuery, ignoreCase = true) ||
                    acc.code.contains(searchQuery, ignoreCase = true) ||
                    acc.accountNumber.contains(searchQuery, ignoreCase = true) ||
                    acc.parentAccountName.contains(searchQuery, ignoreCase = true)
            matchesFilter && matchesSearch
        }
    }

    // إحصائيات سريعة للحسابات
    val totalAccounts = accounts.size
    val activeAccounts = accounts.count { it.isActive }
    val bankBalances = accounts.filter { it.accountType == FinancialAccountType.BANK }.sumOf { it.currentBalance }
    val walletBalances = accounts.filter { it.accountType == FinancialAccountType.E_WALLET }.sumOf { it.currentBalance }
    val cashBalances = accounts.filter { it.accountType == FinancialAccountType.CASH_DRAWER }.sumOf { it.currentBalance }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
            .padding(14.dp)
            .testTag("accounts_management_screen")
    ) {
        // بطاقة الترويسة الرئيسية مع زر الإضافة
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE8F5E9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountTree,
                                contentDescription = null,
                                tint = Color(0xFF0F5132),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "إدارة الحسابات والدليل المحاسبي",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                            Text(
                                text = "تحكم كامل بالبنوك، المحافظ، والصناديق مع حماية الأمان المحاسبي",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B),
                                fontSize = 11.sp
                            )
                        }
                    }

                    Button(
                        onClick = onOpenAddDialog,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F5132)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("btn_add_new_account")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إضافة حساب", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // مؤشرات ملخصة سريعة (KPIs)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AccountKpiCard(
                        title = "حسابات نشطة",
                        value = "$activeAccounts من $totalAccounts",
                        color = Color(0xFF2E7D32),
                        modifier = Modifier.weight(1f)
                    )
                    AccountKpiCard(
                        title = "أرصدة البنوك",
                        value = "${String.format(Locale.US, "%.0f", bankBalances)} $currencySymbol",
                        color = Color(0xFF1565C0),
                        modifier = Modifier.weight(1f)
                    )
                    AccountKpiCard(
                        title = "المحافظ الإلكترونية",
                        value = "${String.format(Locale.US, "%.0f", walletBalances)} $currencySymbol",
                        color = Color(0xFF7B1FA2),
                        modifier = Modifier.weight(1f)
                    )
                    AccountKpiCard(
                        title = "النقدية بالصناديق",
                        value = "${String.format(Locale.US, "%.0f", cashBalances)} $currencySymbol",
                        color = Color(0xFFC25E00),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // شريط البحث والتصفية
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChanged,
                    placeholder = { Text("بحث برمز الحساب، الاسم، رقم الحساب أو الآيبان...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF64748B)) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { onSearchChanged("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "مسح")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("accounts_search_field"),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // رقائق التصفية حسب النوع
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = filterType == null,
                        onClick = { onFilterTypeChanged(null) },
                        label = { Text("الكل (${accounts.size})", fontSize = 11.sp) },
                        modifier = Modifier.testTag("filter_all_accounts")
                    )
                    FilterChip(
                        selected = filterType == FinancialAccountType.BANK,
                        onClick = { onFilterTypeChanged(FinancialAccountType.BANK) },
                        label = { Text("البنوك", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.AccountBalance, null, Modifier.size(14.dp)) },
                        modifier = Modifier.testTag("filter_bank_accounts")
                    )
                    FilterChip(
                        selected = filterType == FinancialAccountType.E_WALLET,
                        onClick = { onFilterTypeChanged(FinancialAccountType.E_WALLET) },
                        label = { Text("المحافظ", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Smartphone, null, Modifier.size(14.dp)) },
                        modifier = Modifier.testTag("filter_wallet_accounts")
                    )
                    FilterChip(
                        selected = filterType == FinancialAccountType.CASH_DRAWER,
                        onClick = { onFilterTypeChanged(FinancialAccountType.CASH_DRAWER) },
                        label = { Text("الصناديق", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.PointOfSale, null, Modifier.size(14.dp)) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // قائمة كروت الحسابات
        if (filteredAccounts.isEmpty()) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("لا توجد حسابات مطابقة للبحث", color = Color(0xFF64748B), fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("يمكنك إضافة حساب جديد عبر زر 'إضافة حساب'", fontSize = 12.sp, color = Color(0xFF94A3B8))
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("accounts_list"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredAccounts, key = { it.id }) { account ->
                    // حساب ما إذا كان الحساب مرتبطاً بحركات مالية للعرض المرئي
                    val hasRecords = remember(account, uiState.invoices, uiState.vouchers, uiState.expenses) {
                        abs(account.currentBalance) > 0.001 ||
                        abs(account.openingBalance) > 0.001 ||
                        uiState.invoices.any { it.notes.contains(account.name, true) || it.notes.contains(account.code, true) } ||
                        uiState.vouchers.any { it.notes.contains(account.name, true) || it.notes.contains(account.code, true) } ||
                        uiState.expenses.any { it.paidTo.contains(account.name, true) || it.notes.contains(account.name, true) }
                    }

                    AccountCardItem(
                        account = account,
                        hasRecords = hasRecords,
                        currencySymbol = currencySymbol,
                        onEdit = { onOpenEditDialog(account) },
                        onDelete = { onRequestDeleteAccount(account) },
                        onToggleActive = { onToggleActive(account) }
                    )
                }
            }
        }
    }

    // حوار الإضافة والتعديل
    if (uiState.showAddEditAccountDialog) {
        AddEditAccountDialog(
            initialAccount = uiState.selectedAccountForEdit,
            existingAccounts = accounts,
            currencySymbol = currencySymbol,
            onSave = onSaveAccount,
            onDismiss = onDismissAddEditDialog
        )
    }

    // حوار منع الحذف المحاسبي
    uiState.accountDeletionBlockedDialog?.let { blockedResult ->
        AccountDeletionBlockedDialog(
            result = blockedResult,
            currencySymbol = currencySymbol,
            onDisableInstead = { onDisableInstead(blockedResult) },
            onDismiss = onDismissDeleteDialogs
        )
    }

    // حوار تأكيد الحذف للحسابات الفارغة والخالية من السجلات
    uiState.accountToDelete?.let { cleanAccount ->
        ConfirmDeleteAccountDialog(
            account = cleanAccount,
            onConfirm = { onConfirmDeleteAccount(cleanAccount) },
            onDismiss = onDismissDeleteDialogs
        )
    }
}

/**
 * بطاقة عرض حساب مالي فردي في القائمة
 */
@Composable
private fun AccountCardItem(
    account: FinancialAccountEntity,
    hasRecords: Boolean,
    currencySymbol: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleActive: () -> Unit
) {
    val (typeIcon, typeColor, typeBg) = when (account.accountType) {
        FinancialAccountType.BANK -> Triple(Icons.Default.AccountBalance, Color(0xFF1565C0), Color(0xFFE3F2FD))
        FinancialAccountType.E_WALLET -> Triple(Icons.Default.Smartphone, Color(0xFF7B1FA2), Color(0xFFF3E5F5))
        FinancialAccountType.CASH_DRAWER -> Triple(Icons.Default.PointOfSale, Color(0xFF0F5132), Color(0xFFE8F5E9))
        FinancialAccountType.CHART_ACCOUNT -> Triple(Icons.Default.AccountTree, Color(0xFFC25E00), Color(0xFFFFF3E0))
        FinancialAccountType.LIABILITY -> Triple(Icons.Default.ReceiptLong, Color(0xFFC62828), Color(0xFFFFEBEE))
        FinancialAccountType.EXPENSE -> Triple(Icons.Default.MoneyOff, Color(0xFF455A64), Color(0xFFECEFF1))
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("account_card_${account.code}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // السطر العلوي: أيقونة النوع، الاسم، الكود، وحالة الحساب
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
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(typeBg),
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
                                color = Color(0xFF1E293B)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFF1F5F9)
                            ) {
                                Text(
                                    text = account.code,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF475569),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "الحساب الرئيسي: ${account.parentAccountName}",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                // راية الحالة نشط / معطل
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (account.isActive) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (account.isActive) Color(0xFF2E7D32) else Color(0xFFC62828))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (account.isActive) "نشط" else "معطل",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (account.isActive) Color(0xFF2E7D32) else Color(0xFFC62828)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // معلومات إضافية ورقم الحساب والرصيد
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    if (account.accountNumber.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Pin, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF94A3B8))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = account.accountNumber,
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    // راية الأمان المحاسبي
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                        Icon(
                            imageVector = if (hasRecords) Icons.Default.Shield else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (hasRecords) Color(0xFFD97706) else Color(0xFF0F5132),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (hasRecords) "محمي محاسبياً (مرتبط بحركات)" else "خالٍ من الحركات (قابل للحذف)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (hasRecords) Color(0xFFD97706) else Color(0xFF0F5132)
                        )
                    }
                }

                // الرصيد الحالي
                Column(horizontalAlignment = Alignment.End) {
                    Text("الرصيد الحالي", fontSize = 10.sp, color = Color(0xFF94A3B8))
                    Text(
                        text = "${String.format(Locale.US, "%.2f", account.currentBalance)} $currencySymbol",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (account.currentBalance >= 0) Color(0xFF0F5132) else Color(0xFFC62828)
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFF1F5F9))

            // أزرار العمليات (تعديل، حذف، تفعيل/تعطيل)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // تفعيل / تعطيل
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onToggleActive() }
                ) {
                    Switch(
                        checked = account.isActive,
                        onCheckedChange = { onToggleActive() },
                        modifier = Modifier.height(24.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (account.isActive) "تعطيل الحساب" else "تفعيل الحساب",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // زر التعديل
                    OutlinedButton(
                        onClick = onEdit,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("btn_edit_account_${account.code}")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تعديل", fontSize = 11.sp)
                    }

                    // زر الحذف
                    Button(
                        onClick = onDelete,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("btn_delete_account_${account.code}")
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

@Composable
private fun AccountKpiCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.08f),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(text = title, fontSize = 10.sp, color = color, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}
