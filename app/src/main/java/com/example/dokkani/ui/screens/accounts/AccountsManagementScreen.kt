package com.example.dokkani.ui.screens.accounts

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import com.example.dokkani.ui.components.AppSearchBar
import com.example.dokkani.ui.components.normalizeArabicItemSearch
import java.util.Locale
import kotlin.math.abs

/**
 * واجهة تحكم متكاملة لإدارة الحسابات (بنوك، محافظ إلكترونية، صناديق، وحسابات الدليل المحاسبي)
 * مع إعادة هيكلة التصميم لإزالة التكدس البصري، تنظيم المساحات البيضاء، وتفعيل البحث الصوتي باللغة العربية.
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

    // تسيير وإخفاء الحسابات الصفرية وفق القواعد المحاسبية
    var showZeroAccounts by remember { mutableStateOf(false) }

    // تصفية الحسابات مرناً مع دعم البحث المكتوب والمنطوق صوتاً
    val filteredAccounts = remember(accounts, searchQuery, filterType, showZeroAccounts) {
        val cleanQuery = normalizeArabicItemSearch(searchQuery)
        accounts.filter { acc ->
            val matchesFilter = filterType == null || acc.accountType == filterType
            val matchesSearch = searchQuery.isBlank() ||
                    normalizeArabicItemSearch(acc.name).contains(cleanQuery) ||
                    normalizeArabicItemSearch(acc.code).contains(cleanQuery) ||
                    normalizeArabicItemSearch(acc.accountNumber).contains(cleanQuery) ||
                    normalizeArabicItemSearch(acc.parentAccountName).contains(cleanQuery)

            val isNonZero = abs(acc.currentBalance) > 0.001 || abs(acc.openingBalance) > 0.001
            val satisfiesZeroRule = showZeroAccounts || isNonZero || searchQuery.isNotBlank()

            matchesFilter && matchesSearch && satisfiesZeroRule
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
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .testTag("accounts_management_screen")
    ) {
        // 1. بطاقة الترويسة الرئيسية مع زر إضافة حساب جديد
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
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
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountTree,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "دليل الحسابات والبنوك",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "تحكم كامل بالبنوك، المحافظ، والصناديق مع حماية الأمان المحاسبي",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = onOpenAddDialog,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        modifier = Modifier.testTag("btn_add_new_account")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إضافة حساب", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. بطاقات المؤشرات المالية والنشطة (Financial & Active Stats Cards)
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            item {
                AccountKpiCard(
                    title = "حسابات نشطة",
                    value = "$activeAccounts من $totalAccounts",
                    icon = Icons.Default.CheckCircle,
                    color = Color(0xFF2E7D32)
                )
            }
            item {
                AccountKpiCard(
                    title = "أرصدة البنوك",
                    value = "${String.format(Locale.US, "%,.0f", bankBalances)} $currencySymbol",
                    icon = Icons.Default.AccountBalance,
                    color = Color(0xFF1565C0)
                )
            }
            item {
                AccountKpiCard(
                    title = "المحافظ الإلكترونية",
                    value = "${String.format(Locale.US, "%,.0f", walletBalances)} $currencySymbol",
                    icon = Icons.Default.Smartphone,
                    color = Color(0xFF7B1FA2)
                )
            }
            item {
                AccountKpiCard(
                    title = "النقدية بالصناديق",
                    value = "${String.format(Locale.US, "%,.0f", cashBalances)} $currencySymbol",
                    icon = Icons.Default.PointOfSale,
                    color = Color(0xFFC25E00)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3. شريط أزرار التصفية وحقل البحث مع دمج البحث الصوتي
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // حقل البحث الذكي المدمج بالبحث الصوتي
                AppSearchBar(
                    value = searchQuery,
                    onValueChange = onSearchChanged,
                    placeholder = "بحث برمز الحساب، الاسم، رقم الحساب أو الآيبان...",
                    label = "بحث دليل الحسابات (صوتي / نصي)",
                    enableVoiceSearch = true,
                    enableBarcodeScanner = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("accounts_search_field")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // أزرار التصفية والأرصدة الصفرية
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = filterType == null,
                        onClick = { onFilterTypeChanged(null) },
                        label = { Text("الكل (${accounts.size})", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        leadingIcon = { Icon(Icons.Default.Apps, null, Modifier.size(14.dp)) },
                        modifier = Modifier.testTag("filter_all_accounts")
                    )
                    FilterChip(
                        selected = filterType == FinancialAccountType.BANK,
                        onClick = { onFilterTypeChanged(FinancialAccountType.BANK) },
                        label = { Text("البنوك", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        leadingIcon = { Icon(Icons.Default.AccountBalance, null, Modifier.size(14.dp)) },
                        modifier = Modifier.testTag("filter_bank_accounts")
                    )
                    FilterChip(
                        selected = filterType == FinancialAccountType.E_WALLET,
                        onClick = { onFilterTypeChanged(FinancialAccountType.E_WALLET) },
                        label = { Text("المحافظ", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        leadingIcon = { Icon(Icons.Default.Smartphone, null, Modifier.size(14.dp)) },
                        modifier = Modifier.testTag("filter_wallet_accounts")
                    )
                    FilterChip(
                        selected = filterType == FinancialAccountType.CASH_DRAWER,
                        onClick = { onFilterTypeChanged(FinancialAccountType.CASH_DRAWER) },
                        label = { Text("الصناديق", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        leadingIcon = { Icon(Icons.Default.PointOfSale, null, Modifier.size(14.dp)) }
                    )
                    FilterChip(
                        selected = filterType == FinancialAccountType.CHART_ACCOUNT,
                        onClick = { onFilterTypeChanged(FinancialAccountType.CHART_ACCOUNT) },
                        label = { Text("دليل الحسابات", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        leadingIcon = { Icon(Icons.Default.AccountTree, null, Modifier.size(14.dp)) }
                    )
                    FilterChip(
                        selected = showZeroAccounts,
                        onClick = { showZeroAccounts = !showZeroAccounts },
                        label = { Text(if (showZeroAccounts) "إخفاء الصفرية" else "إظهار الصفرية", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        leadingIcon = { Icon(if (showZeroAccounts) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, Modifier.size(14.dp)) }
                    )
                }
            }
        }

        // مسافة فاصلة مريحة تمنع تداخل الكروت مع شريط البحث والتحكم
        Spacer(modifier = Modifier.height(16.dp))

        // 4. قائمة بطاقات الحسابات
        if (filteredAccounts.isEmpty()) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SearchOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "لا توجد حسابات مطابقة للبحث",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "جرّب تغيير عبارة البحث، تفعيل الخيار الصفري، أو إضافة حساب جديد",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("accounts_list"),
                contentPadding = PaddingValues(top = 2.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredAccounts, key = { it.id }) { account ->
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

    // حوار تأكيد الحذف للحسابات الخالية من السجلات
    uiState.accountToDelete?.let { cleanAccount ->
        ConfirmDeleteAccountDialog(
            account = cleanAccount,
            onConfirm = { onConfirmDeleteAccount(cleanAccount) },
            onDismiss = onDismissDeleteDialogs
        )
    }
}

/**
 * بطاقة عرض مؤشر إحصائي فردي بمسافات مريحة وتنسيق متناسق
 */
@Composable
private fun AccountKpiCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.18f)),
        modifier = modifier.widthIn(min = 145.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    color = color,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * بطاقة عرض حساب مالي فردي في القائمة بتصميم متناسق وفق معايير Material 3
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
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("account_card_${account.code}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
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
                            .size(40.dp)
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
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = account.code,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "الحساب الرئيسي: ${account.parentAccountName}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // راية الحالة نشط / معطل
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (account.isActive) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (account.isActive) Color(0xFF2E7D32) else Color(0xFFC62828))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (account.isActive) "نشط" else "معطل",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (account.isActive) Color(0xFF2E7D32) else Color(0xFFC62828)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // معلومات إضافية ورقم الحساب والرصيد
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (account.accountNumber.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Pin, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = account.accountNumber,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                    }

                    // راية الأمان المحاسبي
                    Row(verticalAlignment = Alignment.CenterVertically) {
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
                    Text("الرصيد الحالي", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${String.format(Locale.US, "%,.2f", account.currentBalance)} $currencySymbol",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (account.currentBalance >= 0) Color(0xFF0F5132) else Color(0xFFC62828)
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

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
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // زر التعديل
                    OutlinedButton(
                        onClick = onEdit,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(34.dp)
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
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(34.dp)
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

