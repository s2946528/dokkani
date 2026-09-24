package com.example.dokkani.ui.components

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dokkani.data.local.entities.InvoiceEntity
import com.example.dokkani.data.local.entities.PaymentMethod
import com.example.dokkani.data.local.entities.PaymentVoucherEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * نافذة منبثقة لتعديل بيانات الفاتورة المباشرة من سجل الحركات
 * تقتصر صلاحية الحفظ على مدير النظام، وحقل التاريخ غير قابل للتعديل (ثابت Read-Only).
 */
@Composable
fun DirectEditInvoiceDialog(
    invoice: InvoiceEntity,
    currencySymbol: String = "ر.ي",
    isAdmin: Boolean = true,
    onDismiss: () -> Unit,
    onSave: (
        newTotal: Double,
        newPaidAmount: Double,
        newDiscount: Double,
        newPaymentMethod: PaymentMethod,
        newRef: String,
        newNotes: String,
        newReceiptImagePath: String?
    ) -> Unit
) {
    var totalInput by remember { mutableStateOf(invoice.total.toString()) }
    var paidInput by remember { mutableStateOf(invoice.paidAmount.toString()) }
    var discountInput by remember { mutableStateOf(invoice.discount.toString()) }
    var selectedMethod by remember { mutableStateOf(invoice.paymentMethod) }
    var refInput by remember { mutableStateOf(invoice.transactionRef) }
    var notesInput by remember { mutableStateOf(invoice.notes) }
    var receiptImagePath by remember { mutableStateOf(invoice.receiptImagePath) }

    val formattedDate = remember(invoice.date) {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(invoice.date))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "تعديل المستند: ${invoice.invoiceNumber}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "نوع الحركة: ${invoice.type.labelArabic}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                if (!isAdmin) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "عذراً، تقتصر صلاحية تعديل المستندات على مدير النظام فقط.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // حقل التاريخ (ثابت غير قابل للتعديل Read-Only)
                OutlinedTextField(
                    value = formattedDate,
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    label = { Text("تاريخ المستند (ثابت - غير قابل للتعديل)") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "ثابت",
                            tint = Color(0xFF64748B)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledBorderColor = Color(0xFFCBD5E1),
                        disabledTextColor = Color(0xFF334155),
                        disabledLabelColor = Color(0xFF64748B)
                    )
                )

                // اختيار طريقة الدفع
                PaymentMethodSelector(
                    selectedMethod = selectedMethod,
                    onMethodSelected = { if (isAdmin) selectedMethod = it },
                    transactionRef = refInput,
                    onTransactionRefChange = { if (isAdmin) refInput = it }
                )

                // مرفق صورة إشعار السداد
                ReceiptAttachmentComponent(
                    receiptImagePath = receiptImagePath,
                    onReceiptImageChanged = { receiptImagePath = it },
                    label = "صورة إشعار السداد / الحوالة"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // المبلغ الإجمالي
                    OutlinedTextField(
                        value = totalInput,
                        onValueChange = { if (isAdmin) totalInput = it },
                        readOnly = !isAdmin,
                        label = { Text("الإجمالي ($currencySymbol)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )

                    // المدفوع
                    OutlinedTextField(
                        value = paidInput,
                        onValueChange = { if (isAdmin) paidInput = it },
                        readOnly = !isAdmin,
                        label = { Text("المدفوع ($currencySymbol)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                // الخصم
                OutlinedTextField(
                    value = discountInput,
                    onValueChange = { if (isAdmin) discountInput = it },
                    readOnly = !isAdmin,
                    label = { Text("الخصم ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                // البيان / الملاحظات
                OutlinedTextField(
                    value = notesInput,
                    onValueChange = { if (isAdmin) notesInput = it },
                    readOnly = !isAdmin,
                    label = { Text("البيان / الملاحظات") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            if (isAdmin) {
                Button(
                    onClick = {
                        val newTotal = totalInput.toDoubleOrNull() ?: invoice.total
                        val newPaid = paidInput.toDoubleOrNull() ?: invoice.paidAmount
                        val newDiscount = discountInput.toDoubleOrNull() ?: invoice.discount
                        onSave(
                            newTotal,
                            newPaid,
                            newDiscount,
                            selectedMethod,
                            refInput,
                            notesInput,
                            receiptImagePath
                        )
                    }
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("حفظ التعديلات")
                }
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
 * نافذة منبثقة لتعديل بيانات سند القبض أو الصرف المباشرة من سجل الحركات
 * تقتصر صلاحية الحفظ على مدير النظام، وحقل التاريخ غير قابل للتعديل (ثابت Read-Only).
 */
@Composable
fun DirectEditVoucherDialog(
    voucher: PaymentVoucherEntity,
    currencySymbol: String = "ر.ي",
    isAdmin: Boolean = true,
    onDismiss: () -> Unit,
    onSave: (
        newAmount: Double,
        newPaymentMethod: PaymentMethod,
        newRef: String,
        newNotes: String,
        newReceiptImagePath: String?
    ) -> Unit
) {
    var amountInput by remember { mutableStateOf(voucher.amount.toString()) }
    var selectedMethod by remember { mutableStateOf(voucher.paymentMethod) }
    var refInput by remember { mutableStateOf(voucher.transactionRef) }
    var notesInput by remember { mutableStateOf(voucher.notes) }
    var receiptImagePath by remember { mutableStateOf(voucher.receiptImagePath) }

    val formattedDate = remember(voucher.date) {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(voucher.date))
    }

    val voucherTitle = if (voucher.isPayment) "سند صرف" else "سند قبض"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "تعديل $voucherTitle: ${voucher.voucherNumber}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
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
                if (!isAdmin) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "عذراً، تقتصر صلاحية تعديل السندات على مدير النظام فقط.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // حقل التاريخ (ثابت غير قابل للتعديل Read-Only)
                OutlinedTextField(
                    value = formattedDate,
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    label = { Text("تاريخ السند (ثابت - غير قابل للتعديل)") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "ثابت",
                            tint = Color(0xFF64748B)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledBorderColor = Color(0xFFCBD5E1),
                        disabledTextColor = Color(0xFF334155),
                        disabledLabelColor = Color(0xFF64748B)
                    )
                )

                // مبلغ السند
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { if (isAdmin) amountInput = it },
                    readOnly = !isAdmin,
                    label = { Text("مبلغ السند ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                // طريقة السداد
                PaymentMethodSelector(
                    selectedMethod = selectedMethod,
                    onMethodSelected = { if (isAdmin) selectedMethod = it },
                    transactionRef = refInput,
                    onTransactionRefChange = { if (isAdmin) refInput = it }
                )

                // مرفق صورة إشعار السداد
                ReceiptAttachmentComponent(
                    receiptImagePath = receiptImagePath,
                    onReceiptImageChanged = { receiptImagePath = it },
                    label = "صورة إشعار السداد / الحوالة"
                )

                // البيان / الملاحظات
                OutlinedTextField(
                    value = notesInput,
                    onValueChange = { if (isAdmin) notesInput = it },
                    readOnly = !isAdmin,
                    label = { Text("البيان / السبب") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            if (isAdmin) {
                Button(
                    onClick = {
                        val newAmount = amountInput.toDoubleOrNull() ?: voucher.amount
                        onSave(
                            newAmount,
                            selectedMethod,
                            refInput,
                            notesInput,
                            receiptImagePath
                        )
                    }
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("حفظ التعديلات")
                }
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
 * نافذة تأكيد تحذيرية للحذف تحتوي على زر "نعم" وزر "إلغاء"
 */
@Composable
fun ConfirmDeleteTransactionDialog(
    title: String = "تأكيد حذف المستند / الحركة المالية",
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "تنبيه: سيتم إلغاء أثر هذا المستند بالكامل وعكس القيود المالية والمخزنية المرتبطة به في قاعدة البيانات.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("نعم (تأكيد الحذف)")
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
