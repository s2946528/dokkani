package com.example.dokkani.ui.screens.pos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dokkani.data.local.entities.PartyEntity
import com.example.dokkani.data.local.entities.PartyType
import com.example.dokkani.data.local.entities.PaymentMethod
import com.example.dokkani.domain.pos.CartSummary

/**
 * نافذة الدفع وإصدار الفاتورة لنقطة البيع (POS Checkout Dialog)
 * تدعم طرق الدفع: [كاش / نقدي مع فتح الدرج]، [شبكة مدى]، [آجل / دفتر الشكك مع تحديث كشف الحساب]
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosCheckoutDialog(
    cartSummary: CartSummary,
    selectedPaymentMethod: PaymentMethod,
    customers: List<PartyEntity>,
    selectedCustomerPartyId: Long?,
    paidAmountInput: String,
    discountInput: String,
    openDrawerOnCash: Boolean,
    isProcessing: Boolean,
    currencySymbol: String = "ر.س",
    onPaymentMethodSelect: (PaymentMethod) -> Unit,
    onCustomerSelect: (Long?) -> Unit,
    onPaidAmountChange: (String) -> Unit,
    onDiscountChange: (String) -> Unit,
    onToggleOpenDrawer: (Boolean) -> Unit,
    onConfirmCheckout: () -> Unit,
    onDismiss: () -> Unit
) {
    val total = cartSummary.finalTotal
    val paidValue = paidAmountInput.toDoubleOrNull() ?: total
    val change = (paidValue - total).coerceAtLeast(0.0)

    val selectedCustomer = customers.firstOrNull { it.id == selectedCustomerPartyId }
    var customerDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "إتمام الفاتورة والدفع",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "%.2f %s".format(total, currencySymbol),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. اختيار طريقة الدفع
                Text(
                    text = "طريقة الدفع:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // كاش
                    PaymentMethodChip(
                        title = "كاش (نقدي)",
                        icon = Icons.Default.Payments,
                        isSelected = selectedPaymentMethod == PaymentMethod.CASH,
                        onClick = { onPaymentMethodSelect(PaymentMethod.CASH) },
                        modifier = Modifier.weight(1f)
                    )

                    // شبكة مدى
                    PaymentMethodChip(
                        title = "شبكة (مدى)",
                        icon = Icons.Default.CreditCard,
                        isSelected = selectedPaymentMethod == PaymentMethod.MADA,
                        onClick = { onPaymentMethodSelect(PaymentMethod.MADA) },
                        modifier = Modifier.weight(1f)
                    )

                    // آجل (الشكك)
                    PaymentMethodChip(
                        title = "آجل (الشكك)",
                        icon = Icons.Default.AccountBalanceWallet,
                        isSelected = selectedPaymentMethod == PaymentMethod.CREDIT,
                        onClick = { onPaymentMethodSelect(PaymentMethod.CREDIT) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant)

                // 2. تفاصيل خاصة بطريقة الدفع المختارة
                when (selectedPaymentMethod) {
                    PaymentMethod.CASH -> {
                        // دفع نقدي
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = paidAmountInput,
                                onValueChange = onPaidAmountChange,
                                label = { Text("المبلغ المدفوع نقدياً ($currencySymbol)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("cash_paid_input")
                            )

                            // أزرار المبالغ النقدية السريعة
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                QuickAmountButton("مطابق", total) { onPaidAmountChange("%.2f".format(total)) }
                                QuickAmountButton("+10", total + 10) { onPaidAmountChange("%.2f".format(total + 10)) }
                                QuickAmountButton("+50", 50.0) { onPaidAmountChange("50.00") }
                                QuickAmountButton("+100", 100.0) { onPaidAmountChange("100.00") }
                            }

                            // عرض الباقي
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (change > 0.0) MaterialTheme.colorScheme.secondaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "الباقي المسترجع للعميل:",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "%.2f %s".format(change, currencySymbol),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            // خيار فتح درج النقدية
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onToggleOpenDrawer(!openDrawerOnCash) }
                                    .padding(vertical = 4.dp)
                            ) {
                                Checkbox(
                                    checked = openDrawerOnCash,
                                    onCheckedChange = onToggleOpenDrawer
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.LockOpen,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "فتح درج النقدية تلقائياً (ESC p 0 25 250)",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "إرسال نبضة الطابعة الحرارية لفتح الدرج",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    PaymentMethod.MADA -> {
                        // شبكة مدى
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CreditCard,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "الدفع عبر جهاز الشبكة (مدى / فيزا)",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "المبلغ المطلوب تمريره في جهاز الشبكة: %.2f %s".format(total, currencySymbol),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    PaymentMethod.CREDIT -> {
                        // آجل (دفتر الشكك)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "تحديد العميل (صاحب الحساب):",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )

                            ExposedDropdownMenuBox(
                                expanded = customerDropdownExpanded,
                                onExpandedChange = { customerDropdownExpanded = it },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = selectedCustomer?.name ?: "اختر العميل من دفتر الشكك...",
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = customerDropdownExpanded) },
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                        .testTag("customer_dropdown")
                                )

                                ExposedDropdownMenu(
                                    expanded = customerDropdownExpanded,
                                    onDismissRequest = { customerDropdownExpanded = false }
                                ) {
                                    customers.forEach { customer ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(customer.name, fontWeight = FontWeight.Bold)
                                                    Text(
                                                        text = "الرصيد السابق: %.2f %s".format(customer.currentBalance, currencySymbol),
                                                        fontSize = 11.sp,
                                                        color = if (customer.currentBalance > 0) MaterialTheme.colorScheme.error
                                                        else MaterialTheme.colorScheme.outline
                                                    )
                                                }
                                            },
                                            onClick = {
                                                onCustomerSelect(customer.id)
                                                customerDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            // كشف حساب العميل والتحذيرات
                            if (selectedCustomer != null) {
                                val oldBalance = selectedCustomer.currentBalance
                                val newBalance = oldBalance + total

                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("الرصيد السابق:", style = MaterialTheme.typography.bodySmall)
                                            Text(
                                                "%.2f %s".format(oldBalance, currencySymbol),
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("قيمة الفاتورة الحالية:", style = MaterialTheme.typography.bodySmall)
                                            Text(
                                                "+%.2f %s".format(total, currencySymbol),
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Divider(modifier = Modifier.padding(vertical = 4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                "الرصيد الجديد المستحق:",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                "%.2f %s".format(newBalance, currencySymbol),
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    else -> {}
                }

                // 3. حقل الخصم الإضافي (إن وجد)
                OutlinedTextField(
                    value = discountInput,
                    onValueChange = onDiscountChange,
                    label = { Text("خصم الفاتورة ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmCheckout,
                enabled = !isProcessing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("confirm_checkout_button")
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("جاري المعالجة والطباعة...")
                } else {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تأكيد وطباعة الفاتورة")
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
private fun PaymentMethodChip(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.QuickAmountButton(
    label: String,
    value: Double,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .weight(1f)
            .height(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
