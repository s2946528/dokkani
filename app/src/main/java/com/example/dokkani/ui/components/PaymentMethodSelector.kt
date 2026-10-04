package com.example.dokkani.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import com.example.dokkani.data.local.entities.FinancialAccountEntity
import com.example.dokkani.data.local.entities.FinancialAccountType
import com.example.dokkani.data.local.entities.PaymentMethod

/**
 * بيانات طرق الدفع الأساسية الثابتة لعرضها في شريط LazyRow
 */
private data class PaymentMethodOption(
    val method: PaymentMethod,
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val activeColor: Color,
    val description: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentMethodSelector(
    selectedMethod: PaymentMethod,
    onMethodSelected: (PaymentMethod) -> Unit,
    financialAccounts: List<FinancialAccountEntity> = emptyList(),
    selectedAccountId: Long? = null,
    onAccountSelected: (Long?) -> Unit = {},
    transactionRef: String = "",
    onTransactionRefChange: (String) -> Unit = {},
    secondaryMethod: PaymentMethod? = null,
    onSecondaryMethodSelected: (PaymentMethod?) -> Unit = {},
    secondaryPaidAmount: Double = 0.0,
    onSecondaryPaidAmountChange: (Double) -> Unit = {},
    receiptImagePath: String? = null,
    onReceiptImageChange: (String?) -> Unit = {},
    allowCredit: Boolean = true,
    allowMulti: Boolean = true,
    hideEmptyAccounts: Boolean = true,
    currencySymbol: String = "ر.س",
    modifier: Modifier = Modifier
) {
    var accountDropdownExpanded by remember { mutableStateOf(false) }

    val hasBankAccounts = remember(financialAccounts) {
        financialAccounts.any { it.accountType == FinancialAccountType.BANK && it.isActive }
    }
    val hasWalletAccounts = remember(financialAccounts) {
        financialAccounts.any { it.accountType == FinancialAccountType.E_WALLET && it.isActive }
    }
    val hasPosAccounts = remember(financialAccounts) {
        financialAccounts.any { (it.accountType == FinancialAccountType.BANK || it.accountType == FinancialAccountType.E_WALLET) && it.isActive }
    }
    val hasNonCashAccounts = remember(financialAccounts) {
        financialAccounts.any { it.accountType != FinancialAccountType.CASH_DRAWER && it.isActive }
    }

    // تعريف قائمة طرق السداد الأساسية للشريط الأفقي مع شرط إخفاء الخيارات الفارغة
    val options = remember(allowCredit, allowMulti, hideEmptyAccounts, financialAccounts) {
        buildList {
            add(
                PaymentMethodOption(
                    method = PaymentMethod.CASH,
                    title = "💵 نقد",
                    icon = Icons.Default.Payments,
                    activeColor = Color(0xFF2E7D32),
                    description = "تسديد كاش مبسط لحساب الخزينة"
                )
            )
            if (allowCredit) {
                add(
                    PaymentMethodOption(
                        method = PaymentMethod.CREDIT,
                        title = "📝 أجل",
                        icon = Icons.Default.ReceiptLong,
                        activeColor = Color(0xFFD84315),
                        description = "قيد ذمم مستحقة على العميل/المورد"
                    )
                )
            }
            if (!hideEmptyAccounts || hasBankAccounts) {
                add(
                    PaymentMethodOption(
                        method = PaymentMethod.BANK_TRANSFER,
                        title = "🏦 بنكي",
                        icon = Icons.Default.AccountBalance,
                        activeColor = Color(0xFF00838F),
                        description = "إيداع أو تحويل لحساب بنكي"
                    )
                )
            }
            if (!hideEmptyAccounts || hasWalletAccounts) {
                add(
                    PaymentMethodOption(
                        method = PaymentMethod.E_WALLET,
                        title = "📱 محفظة",
                        icon = Icons.Default.Smartphone,
                        activeColor = Color(0xFF6A1B9A),
                        description = "تحصيل عبر المحافظ الإلكترونية"
                    )
                )
            }
            if (!hideEmptyAccounts || hasPosAccounts) {
                add(
                    PaymentMethodOption(
                        method = PaymentMethod.POS_CARD,
                        title = "💳 شبكة",
                        icon = Icons.Default.CreditCard,
                        activeColor = Color(0xFF1565C0),
                        description = "دفع بطاقة أو جهاز نقاط بيع"
                    )
                )
            }
            if (allowMulti && (!hideEmptyAccounts || hasNonCashAccounts)) {
                add(
                    PaymentMethodOption(
                        method = PaymentMethod.MULTI,
                        title = "🔀 متعدد",
                        icon = Icons.Default.AltRoute,
                        activeColor = Color(0xFF37474F),
                        description = "توزيع السداد بين أكثر من حساب"
                    )
                )
            }
        }
    }

    // تصفية الحسابات الديناميكية المسترجعة من قاعدة البيانات بحسب نوع زر طريقة الدفع
    val filteredAccounts = remember(financialAccounts, selectedMethod) {
        when (selectedMethod) {
            PaymentMethod.BANK_TRANSFER, PaymentMethod.EXCHANGE_NETWORK ->
                financialAccounts.filter { it.accountType == FinancialAccountType.BANK }
            PaymentMethod.E_WALLET ->
                financialAccounts.filter { it.accountType == FinancialAccountType.E_WALLET }
            PaymentMethod.POS_CARD, PaymentMethod.MADA ->
                financialAccounts.filter {
                    it.accountType == FinancialAccountType.BANK || it.accountType == FinancialAccountType.E_WALLET
                }
            PaymentMethod.MULTI ->
                financialAccounts.filter { it.accountType != FinancialAccountType.CASH_DRAWER }
            else -> emptyList()
        }.ifEmpty {
            if (selectedMethod.isElectronic || selectedMethod == PaymentMethod.MULTI) {
                financialAccounts.filter { it.accountType != FinancialAccountType.CASH_DRAWER }
            } else emptyList()
        }
    }

    // التحديث التلقائي واختيار الحساب الافتراضي عند تغيير طريقة السداد
    LaunchedEffect(selectedMethod, financialAccounts) {
        if (selectedMethod.isElectronic || selectedMethod == PaymentMethod.MULTI) {
            if (filteredAccounts.isNotEmpty()) {
                val exists = filteredAccounts.any { it.id == selectedAccountId }
                if (!exists) {
                    val defaultAcc = filteredAccounts.firstOrNull { it.isDefault } ?: filteredAccounts.first()
                    onAccountSelected(defaultAcc.id)
                }
            }
        } else {
            onAccountSelected(null)
        }
    }

    val currentAccount = remember(financialAccounts, selectedAccountId) {
        financialAccounts.find { it.id == selectedAccountId }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // العنونة والتنبيه بالطريقة المختارة
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Payments,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "طريقة الدفع والتحصيل الديناميكية:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            val activeOpt = options.find {
                it.method == selectedMethod ||
                        (selectedMethod == PaymentMethod.EXCHANGE_NETWORK && it.method == PaymentMethod.BANK_TRANSFER) ||
                        (selectedMethod == PaymentMethod.MADA && it.method == PaymentMethod.POS_CARD)
            }
            if (activeOpt != null) {
                Surface(
                    color = activeOpt.activeColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, activeOpt.activeColor.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = activeOpt.icon,
                            contentDescription = null,
                            tint = activeOpt.activeColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = activeOpt.title,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = activeOpt.activeColor
                        )
                    }
                }
            }
        }

        // 1. شريط أزرار طرق السداد الرئيسية الأفقي (LazyRow)
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("payment_methods_lazy_row"),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 2.dp)
        ) {
            items(options, key = { it.method.name }) { option ->
                val isSelected = selectedMethod == option.method ||
                        (option.method == PaymentMethod.BANK_TRANSFER && selectedMethod == PaymentMethod.EXCHANGE_NETWORK) ||
                        (option.method == PaymentMethod.POS_CARD && selectedMethod == PaymentMethod.MADA)

                Surface(
                    selected = isSelected,
                    onClick = {
                        onMethodSelected(option.method)
                        accountDropdownExpanded = (option.method.isElectronic || option.method == PaymentMethod.MULTI)
                    },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) option.activeColor else MaterialTheme.colorScheme.surface,
                    contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                    border = BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) option.activeColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    ),
                    tonalElevation = if (isSelected) 3.dp else 0.dp,
                    modifier = Modifier.height(42.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = option.icon,
                            contentDescription = option.title,
                            tint = if (isSelected) Color.White else option.activeColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = option.title,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        // 2. جلب وعرض الحسابات الديناميكية المسترجعة من قاعدة البيانات عند اختيار بنكي، محفظة، شبكة، أو متعدد
        AnimatedVisibility(
            visible = selectedMethod.isElectronic || selectedMethod == PaymentMethod.MULTI,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(10.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val dropdownTitle = when (selectedMethod) {
                    PaymentMethod.BANK_TRANSFER, PaymentMethod.EXCHANGE_NETWORK -> "اختر الحساب البنكي للتنقيل/الإيداع *"
                    PaymentMethod.E_WALLET -> "اختر المحفظة الإلكترونية المستهدفة *"
                    PaymentMethod.POS_CARD, PaymentMethod.MADA -> "اختر حساب شبكة / نقاط البيع POS *"
                    PaymentMethod.MULTI -> "اختر الحساب المالي الرئيسي للتوزيع *"
                    else -> "اختر الحساب المالي *"
                }

                val emptyMessage = when (selectedMethod) {
                    PaymentMethod.BANK_TRANSFER, PaymentMethod.EXCHANGE_NETWORK -> "لا توجد حسابات بنكية محفوظة في قاعدة البيانات"
                    PaymentMethod.E_WALLET -> "لا توجد محافظ إلكترونية مضافة في قاعدة البيانات"
                    PaymentMethod.POS_CARD, PaymentMethod.MADA -> "لا توجد حسابات شبكة / نقاط بيع مسجلة"
                    else -> "لا توجد حسابات مالية مضافة بعد"
                }

                // القائمة المنسدلة لاختيار الحساب المالي المرتبط بالدفع الإلكتروني أو المتعدد
                ExposedDropdownMenuBox(
                    expanded = accountDropdownExpanded,
                    onExpandedChange = { accountDropdownExpanded = !accountDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = currentAccount?.let { "${it.name} (${it.code})" } ?: dropdownTitle,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(dropdownTitle, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountDropdownExpanded) },
                        leadingIcon = {
                            Icon(
                                imageVector = when (selectedMethod) {
                                    PaymentMethod.E_WALLET -> Icons.Default.Smartphone
                                    PaymentMethod.BANK_TRANSFER, PaymentMethod.EXCHANGE_NETWORK -> Icons.Default.AccountBalance
                                    PaymentMethod.MULTI -> Icons.Default.AccountBalanceWallet
                                    else -> Icons.Default.CreditCard
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("dynamic_financial_accounts_dropdown"),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = accountDropdownExpanded,
                        onDismissRequest = { accountDropdownExpanded = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        if (filteredAccounts.isEmpty()) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = emptyMessage,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                onClick = { accountDropdownExpanded = false }
                            )
                        } else {
                            filteredAccounts.forEach { acc ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = acc.name,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    if (acc.isDefault) {
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Surface(
                                                            color = Color(0xFF2E7D32).copy(alpha = 0.15f),
                                                            shape = RoundedCornerShape(4.dp)
                                                        ) {
                                                            Text(
                                                                text = "افتراضي",
                                                                fontSize = 9.sp,
                                                                color = Color(0xFF2E7D32),
                                                                fontWeight = FontWeight.Bold,
                                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                                Text(
                                                    text = "كود: ${acc.code} | ${acc.accountType.labelArabic}" +
                                                            if (acc.accountNumber.isNotBlank()) " | رقم: ${acc.accountNumber}" else "" +
                                                            if (acc.notes.isNotBlank()) " | ${acc.notes}" else "",
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "%.2f %s".format(acc.currentBalance, currencySymbol),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (acc.currentBalance >= 0) Color(0xFF1B5E20) else Color(0xFFC2410C)
                                            )
                                        }
                                    },
                                    onClick = {
                                        onAccountSelected(acc.id)
                                        accountDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // عرض بطاقة تفاعلية تعكس بيانات الحساب المالي المختار من قاعدة البيانات
                if (currentAccount != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "الحساب النشط: ${currentAccount.name}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "الرمز: ${currentAccount.code} • النوع: ${currentAccount.accountType.labelArabic}" +
                                            if (currentAccount.accountNumber.isNotBlank()) " • الحساب: ${currentAccount.accountNumber}" else "" +
                                            if (currentAccount.notes.isNotBlank()) " • ملاحظات: ${currentAccount.notes}" else "",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = Color.White.copy(alpha = 0.8f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "الرصيد: %.2f %s".format(currentAccount.currentBalance, currencySymbol),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (currentAccount.currentBalance >= 0) Color(0xFF1B5E20) else Color(0xFFC2410C),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }

                // تفاصيل التوزيع إضافية عند اختيار الخيار (متعدد)
                if (selectedMethod == PaymentMethod.MULTI) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFECEFF1), shape = RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "توزيع خيارات السداد متعددة الحسابات:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF37474F)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // طريقة الدفع الثانوية
                            OutlinedTextField(
                                value = when (secondaryMethod) {
                                    PaymentMethod.CASH -> "نقد (كاش)"
                                    PaymentMethod.BANK_TRANSFER -> "تحويل بنكي ثانوي"
                                    PaymentMethod.POS_CARD -> "شبكة ثانوية"
                                    PaymentMethod.E_WALLET -> "محفظة ثانوية"
                                    else -> "طريقة سداد ثانوية"
                                },
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("الطريقة الثانوية", fontSize = 10.sp) },
                                modifier = Modifier.weight(1f),
                                trailingIcon = {
                                    IconButton(onClick = {
                                        val nextSec = when (secondaryMethod) {
                                            null -> PaymentMethod.CASH
                                            PaymentMethod.CASH -> PaymentMethod.POS_CARD
                                            PaymentMethod.POS_CARD -> PaymentMethod.BANK_TRANSFER
                                            else -> null
                                        }
                                        onSecondaryMethodSelected(nextSec)
                                    }) {
                                        Icon(Icons.Default.SwapHoriz, contentDescription = "تغيير")
                                    }
                                }
                            )

                            // المبلغ المسدد بالطريقة الثانوية
                            OutlinedTextField(
                                value = if (secondaryPaidAmount > 0) secondaryPaidAmount.toString() else "",
                                onValueChange = { input ->
                                    val valDouble = input.toDoubleOrNull() ?: 0.0
                                    onSecondaryPaidAmountChange(valDouble)
                                },
                                label = { Text("المبلغ الثانوي ($currencySymbol)", fontSize = 10.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // حقل إدخال رقم العملية / رقم المرجع / رقم الحوالة
                OutlinedTextField(
                    value = transactionRef,
                    onValueChange = onTransactionRefChange,
                    label = { Text("رقم العملية / المرجع / رقم الحوالة (اختياري)", fontSize = 11.sp) },
                    placeholder = { Text("مثال: TRX-9821 أو رقم التفويض", fontSize = 11.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.ConfirmationNumber,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    shape = RoundedCornerShape(8.dp)
                )

                // إرفاق صورة إشعار السداد / الحوالة
                ReceiptAttachmentComponent(
                    receiptImagePath = receiptImagePath,
                    onReceiptImageChanged = onReceiptImageChange
                )
            }
        }
    }
}
