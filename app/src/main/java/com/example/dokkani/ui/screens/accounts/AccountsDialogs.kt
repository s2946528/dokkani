package com.example.dokkani.ui.screens.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dokkani.data.local.entities.ChartOfAccountsDefaults
import com.example.dokkani.data.local.entities.FinancialAccountEntity
import com.example.dokkani.data.local.entities.FinancialAccountType
import com.example.dokkani.ui.AccountUsageCheckResult
import java.util.Locale

/**
 * حوار إضافة أو تعديل حساب مالي (بنك، محفظة إلكترونية، صندوق نقدية، حساب دليل محاسبي)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAccountDialog(
    initialAccount: FinancialAccountEntity? = null,
    existingAccounts: List<FinancialAccountEntity> = emptyList(),
    currencySymbol: String = "ر.ي",
    onSave: (FinancialAccountEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val isEdit = initialAccount != null

    var name by remember { mutableStateOf(initialAccount?.name ?: "") }
    var accountType by remember { mutableStateOf(initialAccount?.accountType ?: FinancialAccountType.BANK) }

    // الحساب الرئيسي الافتراضي المقترن بنوع الحساب
    val defaultParent = ChartOfAccountsDefaults.PARENT_ACCOUNTS.firstOrNull { it.defaultType == accountType }
        ?: ChartOfAccountsDefaults.PARENT_ACCOUNTS[1]

    var selectedParentCode by remember {
        mutableStateOf(initialAccount?.parentAccountCode ?: defaultParent.code)
    }
    var selectedParentName by remember {
        mutableStateOf(initialAccount?.parentAccountName ?: defaultParent.name)
    }

    // اقتراح كود فرعي تلقائي في حالة الإضافة
    val suggestedCode = remember(accountType, selectedParentCode) {
        if (isEdit) initialAccount?.code ?: ""
        else {
            val prefix = selectedParentCode
            val existingCodes = existingAccounts
                .filter { it.parentAccountCode == prefix }
                .mapNotNull { it.code.toIntOrNull() }
            val nextNum = if (existingCodes.isEmpty()) (prefix.toIntOrNull() ?: 100) * 100 + 1 else existingCodes.maxOrNull()!! + 1
            nextNum.toString()
        }
    }

    var code by remember { mutableStateOf(suggestedCode) }
    var accountNumber by remember { mutableStateOf(initialAccount?.accountNumber ?: "") }
    var openingBalance by remember { mutableStateOf(initialAccount?.openingBalance?.toString() ?: "0.0") }
    var isActive by remember { mutableStateOf(initialAccount?.isActive ?: true) }
    var notes by remember { mutableStateOf(initialAccount?.notes ?: "") }

    var parentDropdownExpanded by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("add_edit_account_dialog"),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isEdit) Icons.Default.Edit else Icons.Default.AccountBalance,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isEdit) "تعديل بيانات الحساب" else "إضافة حساب جديد بالدليل المحاسبي",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // نوع الحساب المالي
                Text("نوع الحساب المالي *", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FinancialAccountType.values().take(3).forEach { type ->
                        FilterChip(
                            selected = accountType == type,
                            onClick = {
                                accountType = type
                                val parent = ChartOfAccountsDefaults.PARENT_ACCOUNTS.firstOrNull { it.defaultType == type }
                                if (parent != null) {
                                    selectedParentCode = parent.code
                                    selectedParentName = parent.name
                                    if (!isEdit) {
                                        val prefix = parent.code
                                        val existing = existingAccounts
                                            .filter { it.parentAccountCode == prefix }
                                            .mapNotNull { it.code.toIntOrNull() }
                                        code = if (existing.isEmpty()) "${prefix}01" else (existing.maxOrNull()!! + 1).toString()
                                    }
                                }
                            },
                            label = { Text(type.labelArabic, fontSize = 11.sp) },
                            modifier = Modifier.testTag("chip_type_${type.name}")
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FinancialAccountType.values().drop(3).forEach { type ->
                        FilterChip(
                            selected = accountType == type,
                            onClick = {
                                accountType = type
                                val parent = ChartOfAccountsDefaults.PARENT_ACCOUNTS.firstOrNull { it.defaultType == type }
                                if (parent != null) {
                                    selectedParentCode = parent.code
                                    selectedParentName = parent.name
                                }
                            },
                            label = { Text(type.labelArabic, fontSize = 11.sp) }
                        )
                    }
                }

                // ربطه بالحساب الرئيسي في الدليل المحاسبي
                ExposedDropdownMenuBox(
                    expanded = parentDropdownExpanded,
                    onExpandedChange = { parentDropdownExpanded = !parentDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedParentName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("الحساب الرئيسي بالدليل المحاسبي *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = parentDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("parent_account_dropdown"),
                        shape = RoundedCornerShape(10.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = parentDropdownExpanded,
                        onDismissRequest = { parentDropdownExpanded = false }
                    ) {
                        ChartOfAccountsDefaults.PARENT_ACCOUNTS.forEach { parent ->
                            DropdownMenuItem(
                                text = { Text(parent.name, fontSize = 13.sp) },
                                onClick = {
                                    selectedParentCode = parent.code
                                    selectedParentName = parent.name
                                    parentDropdownExpanded = false
                                    if (!isEdit) {
                                        val prefix = parent.code
                                        val existing = existingAccounts
                                            .filter { it.parentAccountCode == prefix }
                                            .mapNotNull { it.code.toIntOrNull() }
                                        code = if (existing.isEmpty()) "${prefix}01" else (existing.maxOrNull()!! + 1).toString()
                                    }
                                }
                            )
                        }
                    }
                }

                // اسم الحساب
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم الحساب (مثل: مصرف الراجحي، محفظة STC Pay) *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("account_name_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                // كود الحساب
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("كود الحساب في الدليل (مثل: 10201) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("account_code_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                // رقم الحساب المصرفي أو الآيبان أو رقم المحفظة
                OutlinedTextField(
                    value = accountNumber,
                    onValueChange = { accountNumber = it },
                    label = { Text("رقم الحساب / الآيبان IBAN / رقم المحفظة") },
                    placeholder = { Text("مثال: SA0380000000000000000000") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("account_number_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                // الرصيد الافتتاحي
                OutlinedTextField(
                    value = openingBalance,
                    onValueChange = { openingBalance = it },
                    label = { Text("الرصيد الافتتاحي (${currencySymbol})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    enabled = !isEdit, // يعدل فقط عند الإنشاء للمحافظة على الأمان المحاسبي
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("account_opening_balance_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                // ملاحظات
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات وإعدادات الحساب") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                // حالة الحساب
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    Switch(
                        checked = isActive,
                        onCheckedChange = { isActive = it },
                        modifier = Modifier.testTag("account_active_switch")
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isActive) "حساب نشط ومتاح للعمليات المالية" else "حساب معطل (موقوف مؤقتاً)",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = if (isActive) Color(0xFF0F5132) else MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "الحسابات المعطلة لا تظهر في عمليات البيع والتحصيل الفوري",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && code.isNotBlank()) {
                        val parsedOpening = openingBalance.toDoubleOrNull() ?: 0.0
                        val currentBal = if (isEdit) (initialAccount?.currentBalance ?: 0.0) else parsedOpening
                        val account = (initialAccount ?: FinancialAccountEntity(
                            code = code.trim(),
                            name = name.trim(),
                            accountType = accountType,
                            parentAccountCode = selectedParentCode,
                            parentAccountName = selectedParentName,
                            accountNumber = accountNumber.trim(),
                            openingBalance = parsedOpening,
                            currentBalance = currentBal,
                            currency = currencySymbol,
                            isActive = isActive,
                            notes = notes.trim()
                        )).copy(
                            code = code.trim(),
                            name = name.trim(),
                            accountType = accountType,
                            parentAccountCode = selectedParentCode,
                            parentAccountName = selectedParentName,
                            accountNumber = accountNumber.trim(),
                            openingBalance = parsedOpening,
                            currentBalance = currentBal,
                            currency = currencySymbol,
                            isActive = isActive,
                            notes = notes.trim()
                        )
                        onSave(account)
                    }
                },
                modifier = Modifier.testTag("btn_save_account")
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("حفظ الحساب")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

/**
 * نافذة تنبيه الأمان المحاسبي عند محاولة حذف حساب يحتوي على حركات وسجلات
 * تنفذ شرط المستخدم بدقة:
 * "في حال وجود سجلات: يتم منع الحذف نهائياً، وإظهار رسالة تنبيه واضحة للمستخدم
 * (مثل: 'عذراً، لا يمكن حذف هذا الحساب لوجود حركات وسجلات مالية مرتبطة به، يمكنك تعطيله بدلاً من ذلك')"
 */
@Composable
fun AccountDeletionBlockedDialog(
    result: AccountUsageCheckResult,
    currencySymbol: String = "ر.ي",
    onDisableInstead: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("account_deletion_blocked_dialog"),
        icon = {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(26.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        title = {
            Text(
                text = "حماية البيانات المحاسبية - منع الحذف",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // نص الرسالة المطلوب حرفياً
                Text(
                    text = "عذراً، لا يمكن حذف هذا الحساب لوجود حركات وسجلات مالية مرتبطة به، يمكنك تعطيله بدلاً من ذلك.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // بطاقة تفاصيل القيود والسجلات المانعة للحذف
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "بيانات الحساب: ${result.accountName} (${result.accountCode})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("إجمالي الحركات المرتبطة:", fontSize = 12.sp)
                            Text("${result.totalRecordCount} سجل", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                        }
                        if (result.invoicesCount > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("• فواتير مسجلة:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${result.invoicesCount}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        if (result.vouchersCount > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("• سندات قبض وصرف:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${result.vouchersCount}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        if (result.expensesCount > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("• قيود مصروفات:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${result.expensesCount}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("الرصيد المالي الحالي:", fontSize = 12.sp)
                            Text(
                                "${String.format(Locale.US, "%.2f", result.currentBalance)} $currencySymbol",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (result.currentBalance >= 0) Color(0xFF0F5132) else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                Text(
                    text = "ملاحظة: يمنع النظام المحاسبي حذف الحسابات التي تحتوي على معاملات لحماية التقارير المالية وموازين المراجعة من التلف أو الاختلال.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDisableInstead,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.testTag("btn_disable_account_instead")
            ) {
                Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("تعطيل الحساب بدلاً من حذفه")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("btn_close_blocked_dialog")
            ) {
                Text("إغلاق")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

/**
 * نافذة تأكيد حذف حساب خالٍ تماماً من السجلات المالية
 */
@Composable
fun ConfirmDeleteAccountDialog(
    account: FinancialAccountEntity,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("confirm_delete_clean_account_dialog"),
        icon = {
            Icon(
                imageVector = Icons.Default.DeleteForever,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = "تأكيد حذف الحساب",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "هل أنت متأكد من رغبتك في حذف الحساب التالي؟",
                    style = MaterialTheme.typography.bodyMedium
                )
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8F5)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("${account.name} (${account.code})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("الحساب الرئيسي: ${account.parentAccountName}", fontSize = 11.sp, color = Color(0xFF0F5132))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "✓ تم التحقق: هذا الحساب جديد ولا يحتوي على أي حركات أو قيود مالية سابقة، ويمكن حذفه بأمان.",
                            fontSize = 11.sp,
                            color = Color(0xFF0F5132)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.testTag("btn_confirm_delete_clean_account")
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("تأكيد الحذف")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}
