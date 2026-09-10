package com.example.dokkani.ui.screens.pos

import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dokkani.domain.pos.PosCheckoutResult

/**
 * نافذة معاينة الفاتورة الحرارية وإيصال نقطة البيع بعد الدفع
 * تعرض شريط الفاتورة كأنها مطبوعة على الورق الحراري (80mm أو 58mm)
 */
@Composable
fun PosReceiptDialog(
    checkoutResult: PosCheckoutResult,
    paperWidthLabel: String = "80 مم",
    onPrintAgain: () -> Unit,
    onDismiss: () -> Unit
) {
    val receipt = checkoutResult.receiptData

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
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "تم إصدار الفاتورة بنجاح",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = paperWidthLabel,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // شارة تنبيه فتح درج النقدية
                if (checkoutResult.drawerKickTriggered) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFE8F5E9),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LockOpen,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "تم إرسال أمر فتح درج النقدية ESC p 0 25 250 بنجاح!",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B5E20)
                            )
                        }
                    }
                }

                // بطاقة شريط الورق الحراري (Thermal Paper Look)
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFDFDFD)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .padding(14.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // ترويسة المحل
                        Text(
                            text = receipt.storeName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                            color = Color(0xFF1A1A1A)
                        )
                        if (receipt.storePhone.isNotBlank()) {
                            Text(
                                text = "هاتف: ${receipt.storePhone}",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = Color(0xFF555555)
                            )
                        }
                        if (receipt.taxNumber.isNotBlank()) {
                            Text(
                                text = "الرقم الضريبي: ${receipt.taxNumber}",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = Color(0xFF555555)
                            )
                        }

                        ReceiptDashedSeparator()

                        Text(
                            text = "** فاتورة مبيعات ضريبية مبسطة **",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF222222)
                        )

                        ReceiptDashedSeparator()

                        ReceiptRow("رقم الفاتورة:", receipt.invoiceNumber)
                        ReceiptRow("التاريخ:", receipt.invoiceDateFormatted)
                        ReceiptRow("الكاشير:", receipt.cashierName)
                        ReceiptRow("طريقة الدفع:", receipt.paymentMethodArabic)

                        if (!receipt.customerName.isNullOrBlank()) {
                            ReceiptRow("العميل (الحساب):", receipt.customerName)
                        }

                        ReceiptDashedSeparator()

                        // جدول البنود
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("الصنف", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Text("الكمية", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Text("السعر", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Text("الإجمالي", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }

                        Divider(modifier = Modifier.padding(vertical = 4.dp), color = Color.LightGray)

                        receipt.items.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    modifier = Modifier.weight(1.5f)
                                )
                                Text(
                                    text = item.quantityFormatted,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "%.2f".format(item.unitPrice),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "%.2f".format(item.totalPrice),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        ReceiptDashedSeparator()

                        // الإجماليات
                        ReceiptRow("المجموع الفرعي:", "%.2f %s".format(receipt.subtotal, receipt.currencySymbol))
                        if (receipt.discount > 0.0) {
                            ReceiptRow("الخصم:", "-%.2f %s".format(receipt.discount, receipt.currencySymbol))
                        }
                        ReceiptRow("ضريبة القيمة المضافة (15%):", "%.2f %s".format(receipt.taxAmount, receipt.currencySymbol))

                        ReceiptDashedSeparator()

                        // الإجمالي الكبير
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "الإجمالي المستحق:",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF111111)
                            )
                            Text(
                                text = "%.2f %s".format(receipt.total, receipt.currencySymbol),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF111111)
                            )
                        }

                        ReceiptRow("المبلغ المدفوع:", "%.2f %s".format(receipt.paidAmount, receipt.currencySymbol))
                        if (receipt.remainingAmount > 0.001) {
                            ReceiptRow("المتبقي (آجل/شكك):", "%.2f %s".format(receipt.remainingAmount, receipt.currencySymbol))
                        } else {
                            val change = (receipt.paidAmount - receipt.total).coerceAtLeast(0.0)
                            if (change > 0.0) {
                                ReceiptRow("الباقي للعميل:", "%.2f %s".format(change, receipt.currencySymbol))
                            }
                        }

                        // رصيد العميل إن وجد
                        if (receipt.customerOldBalance != null && receipt.customerNewBalance != null) {
                            ReceiptDashedSeparator()
                            ReceiptRow("الرصيد السابق:", "%.2f %s".format(receipt.customerOldBalance, receipt.currencySymbol))
                            ReceiptRow("الرصيد الجديد المستحق:", "%.2f %s".format(receipt.customerNewBalance, receipt.currencySymbol))
                        }

                        ReceiptDashedSeparator()

                        // رمز الاستجابة السريعة (QR Code Simulation)
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(Color(0xFFEEEEEE), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "QR CODE\nفاتورة زاتكا",
                                fontSize = 9.sp,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                                color = Color.DarkGray
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = receipt.footerText,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            color = Color(0xFF666666)
                        )
                        Text(
                            text = "دكاني POS & ERP",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onPrintAgain,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("print_receipt_again_button")
            ) {
                Icon(imageVector = Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("إعادة طباعة")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("new_sale_button")
            ) {
                Text("فاتورة جديدة")
            }
        }
    )
}

@Composable
private fun ReceiptRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, fontSize = 11.sp, color = Color(0xFF444444))
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF111111))
    }
}

@Composable
private fun ReceiptDashedSeparator() {
    Text(
        text = "- - - - - - - - - - - - - - - - - - - - - - - - -",
        style = MaterialTheme.typography.labelSmall,
        color = Color.LightGray,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    )
}
