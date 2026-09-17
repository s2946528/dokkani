package com.example.dokkani.ui.screens.purchasereturn

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dokkani.data.local.entities.InvoiceEntity
import com.example.dokkani.data.local.entities.PaymentMethod
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseReturnScreen(
    viewModel: PurchaseReturnViewModel = viewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("مردود المشتريات", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text(
                                "إرجاع بضاعة لمورد بالاعتماد على أسعار الفاتورة الأصلية",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowForward, contentDescription = "رجوع")
                        }
                    },
                    actions = {
                        Button(
                            onClick = { viewModel.openSelectInvoiceDialog() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("select_purchase_invoice_button")
                        ) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (uiState.selectedInvoice == null) "اختيار فاتورة شراء" else "تغيير الفاتورة", fontSize = 13.sp)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            snackbarHost = {
                uiState.userFeedbackMessage?.let { msg ->
                    Snackbar(
                        modifier = Modifier.padding(16.dp),
                        containerColor = if (uiState.isError) MaterialTheme.colorScheme.errorContainer else Color(0xFF1B5E20),
                        contentColor = if (uiState.isError) MaterialTheme.colorScheme.onErrorContainer else Color.White,
                        action = {
                            TextButton(onClick = { viewModel.dismissFeedback() }) {
                                Text("إغلاق", color = Color.White)
                            }
                        }
                    ) {
                        Text(msg)
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                if (uiState.selectedInvoice == null) {
                    EmptyInvoiceSelectionPrompt(
                        onSelectClick = { viewModel.openSelectInvoiceDialog() }
                    )
                } else {
                    ActivePurchaseReturnContent(
                        uiState = uiState,
                        viewModel = viewModel
                    )
                }

                // نافذة اختيار فاتورة الشراء الأصلية
                if (uiState.showSelectInvoiceDialog) {
                    SelectPurchaseInvoiceDialog(
                        invoices = uiState.purchaseInvoices,
                        searchQuery = uiState.searchQuery,
                        currencySymbol = uiState.currencySymbol,
                        onSearchChange = { viewModel.setSearchQuery(it) },
                        onSelectInvoice = { invoice -> viewModel.selectInvoiceForReturn(invoice) },
                        onDismiss = { viewModel.dismissSelectInvoiceDialog() }
                    )
                }

                // نافذة نجاح اعتماد المردود
                if (uiState.showSuccessDialog) {
                    PurchaseReturnSuccessDialog(
                        returnInvoiceNumber = uiState.generatedReturnInvoiceNumber ?: "",
                        currencySymbol = uiState.currencySymbol,
                        totalAmount = uiState.totalReturnAmount,
                        itemsCount = uiState.activeItemsCount,
                        paymentMethod = uiState.paymentMethod,
                        onDismiss = { viewModel.dismissSuccessDialog() }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyInvoiceSelectionPrompt(
    onSelectClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
            modifier = Modifier.size(90.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.AssignmentReturn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "اختر فاتورة الشراء المراد إرجاعها",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "سيقوم النظام باسترجاع جميع بنود الفاتورة بأسعار التكلفة التاريخية الدقيقة المسجلة بالفاتورة الأصلية (invoice_items.unit_cost_price) لضمان دقة قيود المخزون وحساب المورد.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onSelectClick,
            modifier = Modifier
                .height(48.dp)
                .testTag("prompt_select_invoice_button")
        ) {
            Icon(Icons.Default.Search, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("استعراض فواتير الشراء", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ActivePurchaseReturnContent(
    uiState: PurchaseReturnUiState,
    viewModel: PurchaseReturnViewModel
) {
    val originalInvoice = uiState.selectedInvoice ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // كرت ملخص الفاتورة الأصلية
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "فاتورة الشراء الأصلية: ${originalInvoice.invoiceNumber}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        uiState.supplier?.let { s ->
                            Text(
                                text = "المورد: ${s.name} ${if (s.phone.isNotBlank()) "(${s.phone})" else ""}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        val dateFormatted = SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH).format(Date(originalInvoice.date))
                        Text(
                            text = dateFormatted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "إجمالي الفاتورة الأصلية: %.2f %s".format(originalInvoice.total, uiState.currencySymbol),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = { viewModel.setAllQuantities(full = true) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("إرجاع الكل", fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = { viewModel.setAllQuantities(full = false) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("تصفير الكميات", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // قائمة البنود المتاحة للإرجاع
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.returnItems, key = { "${it.productId}_${it.unitId}" }) { item ->
                PurchaseReturnItemCard(
                    item = item,
                    currencySymbol = uiState.currencySymbol,
                    onQuantityChange = { qty -> viewModel.updateReturnQuantity(item.productId, item.unitId, qty) },
                    onPriceChange = { price -> viewModel.updateReturnCostPrice(item.productId, item.unitId, price) },
                    onToggleSelect = { isSel -> viewModel.toggleItemSelection(item.productId, item.unitId, isSel) }
                )
            }
        }

        // كرت الإجمالي والخيارات المالية والاعتماد
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("طريقة استرداد القيمة:", fontWeight = FontWeight.Bold, fontSize = 12.sp)

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = uiState.paymentMethod == PaymentMethod.CREDIT,
                            onClick = { viewModel.setPaymentMethod(PaymentMethod.CREDIT) },
                            label = { Text("آجل (تخفيض مديونية المورد)", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = uiState.paymentMethod == PaymentMethod.CASH,
                            onClick = { viewModel.setPaymentMethod(PaymentMethod.CASH) },
                            label = { Text("نقداً (استرداد للصندوق)", fontSize = 11.sp) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "إجمالي قيمة المردود:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "%.2f %s".format(uiState.totalReturnAmount, uiState.currencySymbol),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Button(
                        onClick = { viewModel.confirmPurchaseReturn() },
                        enabled = !uiState.isProcessing && uiState.activeItemsCount > 0,
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("confirm_purchase_return_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) {
                        if (uiState.isProcessing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                        } else {
                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("اعتماد مردود المشتريات (${uiState.activeItemsCount})", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PurchaseReturnItemCard(
    item: PurchaseReturnItem,
    currencySymbol: String,
    onQuantityChange: (Double) -> Unit,
    onPriceChange: (Double) -> Unit,
    onToggleSelect: (Boolean) -> Unit
) {
    var showPriceDialog by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (item.isSelectedForReturn && item.returnQuantity > 0.0) {
            MaterialTheme.colorScheme.surface
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        },
        border = BorderStroke(
            1.dp,
            if (item.isSelectedForReturn && item.returnQuantity > 0.0) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Checkbox(
                        checked = item.isSelectedForReturn,
                        onCheckedChange = onToggleSelect,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = item.productName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (item.productCode.isNotBlank()) {
                            Text(
                                text = item.productCode,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // إجمالي سطر المردود
                Text(
                    text = "%.2f %s".format(item.totalReturnCost, currencySymbol),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (item.isSelectedForReturn && item.returnQuantity > 0.0) MaterialTheme.colorScheme.primary else Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // شريط إظهار سعر الشراء الأصلي والكمية كمرجع ثابت وموثوق
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFFE8F5E9),
                border = BorderStroke(1.dp, Color(0xFFA5D6A7)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "سعر الشراء الأصلي بالفاتورة: %.2f %s".format(item.originalUnitCostPrice, currencySymbol),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B5E20)
                    )
                    Text(
                        text = "الكمية المشتراة: %.2f %s".format(item.originalPurchasedQuantity, item.unitName),
                        fontSize = 11.sp,
                        color = Color(0xFF2E7D32)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // الصف السفلي: أدوات التحكم بالكمية المردودة وسعر الإرجاع
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // سعر الإرجاع مع إمكانية التعديل للمرونة
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { showPriceDialog = true }
                ) {
                    Text(
                        text = "سعر الإرجاع: %.2f %s / %s".format(item.returnCostPrice, currencySymbol, item.unitName),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "تعديل سعر المردود",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                }

                // أدوات تعديل كمية المردود بما لا يتجاوز الكمية المشتراة
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilledTonalButton(
                        onClick = { onQuantityChange((item.returnQuantity - 1.0).coerceAtLeast(0.0)) },
                        modifier = Modifier.size(30.dp),
                        contentPadding = PaddingValues(0.dp),
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "إنقاص", modifier = Modifier.size(16.dp))
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = "%.2f".format(item.returnQuantity),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    FilledTonalButton(
                        onClick = { onQuantityChange((item.returnQuantity + 1.0).coerceAtMost(item.originalPurchasedQuantity)) },
                        modifier = Modifier.size(30.dp),
                        contentPadding = PaddingValues(0.dp),
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "زيادة", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }

    // نافذة تعديل سعر المردود يدوياً إذا وافق المورد على قيمة أخرى
    if (showPriceDialog) {
        var tempPrice by remember { mutableStateOf(item.returnCostPrice.toString()) }
        AlertDialog(
            onDismissRequest = { showPriceDialog = false },
            title = { Text("تعديل سعر الإرجاع", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("الصنف: ${item.productName}", fontSize = 13.sp)
                    Text("سعر الشراء الأصلي المسجل: %.2f %s".format(item.originalUnitCostPrice, currencySymbol), fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = tempPrice,
                        onValueChange = { tempPrice = it },
                        label = { Text("سعر الإرجاع للوحدة") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = tempPrice.toDoubleOrNull()
                        if (parsed != null && parsed >= 0.0) {
                            onPriceChange(parsed)
                        }
                        showPriceDialog = false
                    }
                ) {
                    Text("تطبيق")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPriceDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
private fun SelectPurchaseInvoiceDialog(
    invoices: List<InvoiceEntity>,
    searchQuery: String,
    currencySymbol: String,
    onSearchChange: (String) -> Unit,
    onSelectInvoice: (InvoiceEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val filteredInvoices = remember(invoices, searchQuery) {
        if (searchQuery.isBlank()) invoices
        else invoices.filter { it.invoiceNumber.contains(searchQuery, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("اختيار فاتورة شراء لغرض المردود", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("بحث برقم الفاتورة...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (filteredInvoices.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("لا توجد فواتير شراء متطابقة", color = Color.Gray, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredInvoices, key = { it.id }) { inv ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectInvoice(inv) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(inv.invoiceNumber, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        val dateStr = SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH).format(Date(inv.date))
                                        Text(dateStr, fontSize = 11.sp, color = Color.Gray)
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            "%.2f %s".format(inv.total, currencySymbol),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            if (inv.paymentMethod == PaymentMethod.CASH) "نقداً" else "آجل",
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}

@Composable
private fun PurchaseReturnSuccessDialog(
    returnInvoiceNumber: String,
    currencySymbol: String,
    totalAmount: Double,
    itemsCount: Int,
    paymentMethod: PaymentMethod,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("تم اعتماد مردود المشتريات بنجاح", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("رقم سند المردود: $returnInvoiceNumber", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("عدد الأصناف المردودة: $itemsCount", fontSize = 13.sp)
                Text("إجمالي قيمة المردود: %.2f %s".format(totalAmount, currencySymbol), fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                val methodStr = if (paymentMethod == PaymentMethod.CASH) "تم استرداد النقدية إلى درج الكاشير" else "تم تخفيض مديونية المورد في دفتر الحسابات"
                Text("الأثر المالي: $methodStr", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                Text("الأثر المخزني: تم تخفيض المخزون بالتكلفة التاريخية الدقيقة (RETURN_OUT).", fontSize = 12.sp, color = Color.Gray)
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
            ) {
                Text("حسناً")
            }
        }
    )
}
