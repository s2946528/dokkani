package com.example.dokkani.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dokkani.data.local.entities.FinancialAccountEntity
import com.example.dokkani.data.local.entities.FinancialAccountType
import com.example.dokkani.data.local.entities.PaymentMethod

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PaymentMethodSelector(
    selectedMethod: PaymentMethod,
    onMethodSelected: (PaymentMethod) -> Unit,
    financialAccounts: List<FinancialAccountEntity> = emptyList(),
    selectedAccountId: Long? = null,
    onAccountSelected: (Long?) -> Unit = {},
    transactionRef: String = "",
    onTransactionRefChange: (String) -> Unit = {},
    allowCredit: Boolean = true,
    allowMulti: Boolean = true,
    currencySymbol: String = "ر.س",
    modifier: Modifier = Modifier
) {
    var accountDropdownExpanded by remember { mutableStateOf(false) }

    // التحديث التلقائي للحساب المالي المحدد وفتح القائمة عند تبديل طريقة الدفع
    LaunchedEffect(selectedMethod) {
        val targetAccounts = when (selectedMethod) {
            PaymentMethod.BANK_TRANSFER, PaymentMethod.EXCHANGE_NETWORK -> 
                financialAccounts.filter { it.accountType == FinancialAccountType.BANK }
            PaymentMethod.E_WALLET -> 
                financialAccounts.filter { it.accountType == FinancialAccountType.E_WALLET }
            PaymentMethod.POS_CARD, PaymentMethod.MADA -> 
                financialAccounts.filter { it.accountType == FinancialAccountType.BANK || it.accountType == FinancialAccountType.E_WALLET }
            else -> emptyList()
        }
        if (targetAccounts.isNotEmpty()) {
            val exists = targetAccounts.any { it.id == selectedAccountId }
            if (!exists) {
                val defaultAcc = targetAccounts.firstOrNull { it.isDefault } ?: targetAccounts.first()
                onAccountSelected(defaultAcc.id)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "طريقة الدفع والتحصيل:",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (selectedMethod.isElectronic) {
                Surface(
                    color = Color(0xFF1565C0).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, Color(0xFF1565C0).copy(alpha = 0.3f))
                ) {
                    Text(
                        text = when (selectedMethod) {
                            PaymentMethod.BANK_TRANSFER, PaymentMethod.EXCHANGE_NETWORK -> "تحويل بنكي"
                            PaymentMethod.E_WALLET -> "محفظة إلكترونية"
                            PaymentMethod.POS_CARD, PaymentMethod.MADA -> "دفع شبكة POS"
                            else -> "دفع إلكتروني مالي"
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1565C0),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 1. أزرار اختيار طريقة الدفع بالترتيب المحدد بالضبط: (نقد، أجل، بنكي، محفظة، شبكة)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // 1. نقد
            FilterChip(
                selected = selectedMethod == PaymentMethod.CASH,
                onClick = { 
                    onMethodSelected(PaymentMethod.CASH)
                    accountDropdownExpanded = false
                },
                label = { Text("💵 نقد", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF2E7D32),
                    selectedLabelColor = Color.White
                )
            )

            // 2. أجل
            if (allowCredit) {
                FilterChip(
                    selected = selectedMethod == PaymentMethod.CREDIT,
                    onClick = { 
                        onMethodSelected(PaymentMethod.CREDIT)
                        accountDropdownExpanded = false
                    },
                    label = { Text("📝 أجل (حساب)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFD84315),
                        selectedLabelColor = Color.White
                    )
                )
            }

            // 3. بنكي
            FilterChip(
                selected = selectedMethod == PaymentMethod.BANK_TRANSFER || selectedMethod == PaymentMethod.EXCHANGE_NETWORK,
                onClick = { 
                    onMethodSelected(PaymentMethod.BANK_TRANSFER)
                    accountDropdownExpanded = true
                },
                label = { Text("🏦 بنكي", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF00838F),
                    selectedLabelColor = Color.White
                )
            )

            // 4. محفظة
            FilterChip(
                selected = selectedMethod == PaymentMethod.E_WALLET,
                onClick = { 
                    onMethodSelected(PaymentMethod.E_WALLET)
                    accountDropdownExpanded = true
                },
                label = { Text("📱 محفظة", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF6A1B9A),
                    selectedLabelColor = Color.White
                )
            )

            // 5. شبكة
            FilterChip(
                selected = selectedMethod == PaymentMethod.POS_CARD || selectedMethod == PaymentMethod.MADA,
                onClick = { 
                    onMethodSelected(PaymentMethod.POS_CARD)
                    accountDropdownExpanded = true
                },
                label = { Text("💳 شبكة", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF1565C0),
                    selectedLabelColor = Color.White
                )
            )

            // 6. متعدد
            if (allowMulti) {
                FilterChip(
                    selected = selectedMethod == PaymentMethod.MULTI,
                    onClick = { 
                        onMethodSelected(PaymentMethod.MULTI)
                        accountDropdownExpanded = true
                    },
                    label = { Text("🔀 متعدد", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF424242),
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        // 2. القائمة المنسدلة المخصصة لإدخال وإدارة الحسابات البنكية والمحافظ والشبكة
        AnimatedVisibility(visible = selectedMethod.isElectronic || selectedMethod == PaymentMethod.MULTI) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(8.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // تصفية الحسابات المتاحة ديناميكياً
                val filteredAccounts = remember(financialAccounts, selectedMethod) {
                    when (selectedMethod) {
                        PaymentMethod.BANK_TRANSFER, PaymentMethod.EXCHANGE_NETWORK -> 
                            financialAccounts.filter { it.accountType == FinancialAccountType.BANK }
                        PaymentMethod.E_WALLET -> 
                            financialAccounts.filter { it.accountType == FinancialAccountType.E_WALLET }
                        PaymentMethod.POS_CARD, PaymentMethod.MADA -> 
                            financialAccounts.filter { it.accountType == FinancialAccountType.BANK || it.accountType == FinancialAccountType.E_WALLET }
                        else -> financialAccounts.filter { it.accountType != FinancialAccountType.CASH_DRAWER }
                    }.ifEmpty { financialAccounts.filter { it.accountType != FinancialAccountType.CASH_DRAWER } }
                }

                val currentAccount = financialAccounts.find { it.id == selectedAccountId }

                val dropdownLabel = when (selectedMethod) {
                    PaymentMethod.BANK_TRANSFER, PaymentMethod.EXCHANGE_NETWORK -> "اختر البنك المرحل إليه"
                    PaymentMethod.E_WALLET -> "اختر المحفظة الإلكترونية المستهدفة"
                    PaymentMethod.POS_CARD, PaymentMethod.MADA -> "اختر شبكة / حساب نقاط البيع"
                    else -> "اختر الحساب المالي المرحل إليه"
                }

                val emptyMessage = when (selectedMethod) {
                    PaymentMethod.BANK_TRANSFER, PaymentMethod.EXCHANGE_NETWORK -> "لا توجد حسابات بنكية مضافة بعد"
                    PaymentMethod.E_WALLET -> "لا توجد محافظ إلكترونية مضافة بعد"
                    PaymentMethod.POS_CARD, PaymentMethod.MADA -> "لا توجد حسابات شبكة / نقاط بيع مضافة بعد"
                    else -> "لا توجد حسابات مالية مضافة"
                }

                ExposedDropdownMenuBox(
                    expanded = accountDropdownExpanded,
                    onExpandedChange = { accountDropdownExpanded = !accountDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = currentAccount?.let { "${it.name} (${it.code})" } ?: dropdownLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(dropdownLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountDropdownExpanded) },
                        leadingIcon = {
                            Icon(
                                imageVector = when (selectedMethod) {
                                    PaymentMethod.E_WALLET -> Icons.Default.Smartphone
                                    PaymentMethod.BANK_TRANSFER, PaymentMethod.EXCHANGE_NETWORK -> Icons.Default.AccountBalance
                                    else -> Icons.Default.CreditCard
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    ExposedDropdownMenu(
                        expanded = accountDropdownExpanded,
                        onDismissRequest = { accountDropdownExpanded = false }
                    ) {
                        if (filteredAccounts.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text(emptyMessage, fontSize = 12.sp, color = Color.Gray) },
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
                                            Column {
                                                Text(acc.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                Text(
                                                    text = "${acc.code} - ${acc.accountType.labelArabic}",
                                                    fontSize = 10.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                            Text(
                                                text = "%.2f %s".format(acc.currentBalance, currencySymbol),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1B5E20)
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

                // إدخال رقم العملية / رقم المرجع / رقم الحوالة
                OutlinedTextField(
                    value = transactionRef,
                    onValueChange = onTransactionRefChange,
                    label = { Text("رقم العملية / المرجع / رقم الحوالة (اختياري)", fontSize = 11.sp) },
                    placeholder = { Text("مثال: TRX-9821 أو رقم التفويض", fontSize = 11.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.ConfirmationNumber, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        }
    }
}
