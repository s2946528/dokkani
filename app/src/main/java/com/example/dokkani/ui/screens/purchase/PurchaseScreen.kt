package com.example.dokkani.ui.screens.purchase

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dokkani.data.local.entities.PartyEntity
import com.example.dokkani.data.local.entities.PaymentMethod
import com.example.dokkani.data.local.entities.ProductUnitEntity
import com.example.dokkani.data.local.entities.ProductWithUnits

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseScreen(
    viewModel: PurchaseViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var showAddSupplierDialog by remember { mutableStateOf(false) }
    var newSupplierName by remember { mutableStateOf("") }
    var newSupplierPhone by remember { mutableStateOf("") }
    var newSupplierTax by remember { mutableStateOf("") }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            snackbarHost = {
                uiState.feedbackMessage?.let { msg ->
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
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color(0xFFF4F6F8))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // الجانب الأيمن: اختيار المورد والبحث عن المنتجات وإضافتها
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        // ترويسة اختيار المورد
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.LocalShipping,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("بيانات المورد والتوريد", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }

                            FilledTonalButton(
                                onClick = { showAddSupplierDialog = true },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("مورد جديد", fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // اختيار المورد
                        var supplierDropdownExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = supplierDropdownExpanded,
                            onExpandedChange = { supplierDropdownExpanded = it },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = uiState.selectedSupplier?.let { "${it.name} (${if (it.currentBalance < 0) "دائن: %.2f".format(-it.currentBalance) else "رصيد: %.2f".format(it.currentBalance)} ر.س)" } ?: "اختر المورد...",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = supplierDropdownExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                label = { Text("المورد") }
                            )

                            ExposedDropdownMenu(
                                expanded = supplierDropdownExpanded,
                                onDismissRequest = { supplierDropdownExpanded = false }
                            ) {
                                uiState.suppliers.forEach { supplier ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(supplier.name, fontWeight = FontWeight.Bold)
                                                Text(
                                                    text = if (supplier.currentBalance < 0) "مستحق له (دائن): %.2f ر.س".format(-supplier.currentBalance) else "رصيده: %.2f ر.س".format(supplier.currentBalance),
                                                    fontSize = 11.sp,
                                                    color = if (supplier.currentBalance < 0) Color(0xFFD32F2F) else Color.Gray
                                                )
                                            }
                                        },
                                        onClick = {
                                            viewModel.selectSupplier(supplier)
                                            supplierDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // رقم فاتورة المورد الورقية
                        OutlinedTextField(
                            value = uiState.supplierInvoiceNumber,
                            onValueChange = { viewModel.setSupplierInvoiceNumber(it) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("رقم فاتورة المورد / سند الاستلام الورقي") },
                            leadingIcon = { Icon(Icons.Default.Receipt, contentDescription = null) },
                            singleLine = true
                        )

                        Divider(modifier = Modifier.padding(vertical = 10.dp))

                        // البحث عن المنتجات
                        Text("البحث عن الأصناف للتوريد", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("ابحث بالاسم أو الباركود...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (uiState.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = null)
                                    }
                                }
                            },
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // قائمة المنتجات والوحدات المتاحة للإضافة
                        val filteredProducts = uiState.productsWithUnits.filter { p ->
                            val query = uiState.searchQuery.trim().lowercase()
                            query.isEmpty() || p.product.name.lowercase().contains(query) ||
                                    p.product.code.lowercase().contains(query) ||
                                    p.units.any { it.barcode.lowercase().contains(query) }
                        }

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(filteredProducts) { pw ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
                                    border = CardDefaults.outlinedCardBorder()
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                pw.product.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Surface(
                                                color = Color(0xFFE3F2FD),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    pw.product.category,
                                                    fontSize = 10.sp,
                                                    color = Color(0xFF1976D2),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        // وحدات الصنف المتوفرة للإضافة
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            items(pw.units) { unit ->
                                                OutlinedButton(
                                                    onClick = {
                                                        viewModel.addProductItem(pw.product, unit, 1.0, unit.costPrice)
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    modifier = Modifier.height(32.dp)
                                                ) {
                                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("${unit.unitName} (تكلفة: ${unit.costPrice} ر.س)", fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // الجانب الأيسر: جدول بنود الفاتورة وحساب التكلفة والإجماليات
                Card(
                    modifier = Modifier
                        .weight(1.3f)
                        .fillMaxHeight(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("بنود فاتورة الشراء الواردة", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("الكميات المدخلة ترفع المخزون وتحدّث التكلفة (WAC)", fontSize = 11.sp, color = Color.Gray)
                            }

                            if (uiState.items.isNotEmpty()) {
                                TextButton(onClick = { viewModel.clearInvoice() }) {
                                    Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("مسح الكل", color = Color.Red, fontSize = 12.sp)
                                }
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        // قائمة بنود الفاتورة
                        if (uiState.items.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.LightGray)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("لم تتم إضافة أي أصناف للفاتورة بعد", color = Color.Gray)
                                    Text("اختر المورد ثم أضف المنتجات من القائمة", fontSize = 12.sp, color = Color.LightGray)
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(uiState.items) { item ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4F8)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(item.productName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                    Text("الوحدة: ${item.unitName} (التكلفة الحالية المسجلة: ${item.oldCostPrice} ر.س)", fontSize = 11.sp, color = Color.DarkGray)
                                                }

                                                IconButton(
                                                    onClick = { viewModel.removeItem(item.productId, item.unitId) },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.Red)
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // حقل الكمية
                                                OutlinedTextField(
                                                    value = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString(),
                                                    onValueChange = { str ->
                                                        str.toDoubleOrNull()?.let { qty ->
                                                            viewModel.updateItemQuantity(item.productId, item.unitId, qty)
                                                        }
                                                    },
                                                    label = { Text("الكمية") },
                                                    modifier = Modifier.weight(1f),
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                    singleLine = true
                                                )

                                                // حقل سعر الشراء والتكلفة
                                                OutlinedTextField(
                                                    value = if (item.costPrice % 1.0 == 0.0) item.costPrice.toInt().toString() else item.costPrice.toString(),
                                                    onValueChange = { str ->
                                                        str.toDoubleOrNull()?.let { cost ->
                                                            viewModel.updateItemCostPrice(item.productId, item.unitId, cost)
                                                        }
                                                    },
                                                    label = { Text("سعر التكلفة الجديد") },
                                                    modifier = Modifier.weight(1.2f),
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                    singleLine = true
                                                )

                                                // إجمالي البند
                                                Column(
                                                    modifier = Modifier.weight(1f),
                                                    horizontalAlignment = Alignment.End
                                                ) {
                                                    Text("إجمالي البند", fontSize = 11.sp, color = Color.Gray)
                                                    Text(
                                                        "%.2f ر.س".format(item.totalCost),
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF1976D2),
                                                        fontSize = 14.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        // طريقة السداد والضريبة
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("طريقة السداد:", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(
                                    selected = uiState.paymentMethod == PaymentMethod.CASH,
                                    onClick = { viewModel.setPaymentMethod(PaymentMethod.CASH) },
                                    label = { Text("نقداً") },
                                    leadingIcon = { Icon(Icons.Default.Money, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                )
                                FilterChip(
                                    selected = uiState.paymentMethod == PaymentMethod.CREDIT,
                                    onClick = { viewModel.setPaymentMethod(PaymentMethod.CREDIT) },
                                    label = { Text("آجل (حساب المورد)") },
                                    leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                )
                                FilterChip(
                                    selected = uiState.paymentMethod == PaymentMethod.BANK_TRANSFER,
                                    onClick = { viewModel.setPaymentMethod(PaymentMethod.BANK_TRANSFER) },
                                    label = { Text("تحويل بنكي") }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // ملخص المبالغ
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = uiState.isTaxApplied,
                                    onCheckedChange = { viewModel.setTaxApplied(it) }
                                )
                                Text("تطبيق ضريبة القيمة المضافة (15%)", fontSize = 12.sp)
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text("المجموع قبل الضريبة: %.2f ر.س".format(uiState.subtotal), fontSize = 12.sp, color = Color.DarkGray)
                                if (uiState.isTaxApplied) {
                                    Text("ضريبة 15%: %.2f ر.س".format(uiState.taxAmount), fontSize = 12.sp, color = Color.DarkGray)
                                }
                                Text(
                                    "الصافي الإجمالي: %.2f ر.س".format(uiState.finalTotal),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = Color(0xFF1976D2)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = { viewModel.executePurchaseTransaction() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                            enabled = uiState.items.isNotEmpty() && !uiState.isProcessing
                        ) {
                            if (uiState.isProcessing) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                            } else {
                                Icon(Icons.Default.CheckCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("حفظ واعتماد فاتورة التوريد وتطبيق WAC", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }
                }
            }
        }

        // نافذة نتائج احتساب المتوسط المرجح للتكلفة WAC
        if (uiState.showSuccessDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissSuccessDialog() },
                icon = { Icon(Icons.Default.Calculate, contentDescription = null, tint = Color(0xFF1976D2), modifier = Modifier.size(36.dp)) },
                title = { Text("تم اعتماد الفاتورة وتحديث تكلفة WAC", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            "رقم الفاتورة المسجلة: ${uiState.lastSavedInvoiceNumber}",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1976D2)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("تم إعادة احتساب تكلفة المخزون بالمتوسط المرجح (WAC):", fontSize = 12.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(uiState.wacUpdates) { wac ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F7FF))
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text("${wac.productName} (${wac.unitName})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("التكلفة السابقة: %.2f".format(wac.oldCost), fontSize = 11.sp, color = Color.Gray)
                                            Text("شراء جديد: %.2f".format(wac.purchaseCost), fontSize = 11.sp, color = Color.Gray)
                                            Text("WAC الجديد: %.2f ر.س".format(wac.newWacCost), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF1B5E20))
                                        }
                                        Text("الرصيد الجديد: %.2f ${wac.unitName}".format(wac.newStock), fontSize = 10.sp, color = Color.DarkGray)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { viewModel.dismissSuccessDialog() }) {
                        Text("تم")
                    }
                }
            )
        }

        // نافذة إضافة مورد جديد
        if (showAddSupplierDialog) {
            AlertDialog(
                onDismissRequest = { showAddSupplierDialog = false },
                title = { Text("إضافة مورد جديد", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newSupplierName,
                            onValueChange = { newSupplierName = it },
                            label = { Text("اسم المورد أو الشركة") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = newSupplierPhone,
                            onValueChange = { newSupplierPhone = it },
                            label = { Text("رقم الجوال") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = newSupplierTax,
                            onValueChange = { newSupplierTax = it },
                            label = { Text("الرقم الضريبي (اختياري)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newSupplierName.isNotBlank()) {
                                viewModel.addNewSupplier(newSupplierName, newSupplierPhone, newSupplierTax)
                                newSupplierName = ""
                                newSupplierPhone = ""
                                newSupplierTax = ""
                                showAddSupplierDialog = false
                            }
                        }
                    ) {
                        Text("حفظ")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddSupplierDialog = false }) {
                        Text("إلغاء")
                    }
                }
            )
        }
    }
}
