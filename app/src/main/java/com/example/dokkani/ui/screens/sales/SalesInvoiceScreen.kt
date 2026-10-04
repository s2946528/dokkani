package com.example.dokkani.ui.screens.sales

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dokkani.data.local.entities.InvoiceEntity
import com.example.dokkani.data.local.entities.InvoiceItemEntity
import com.example.dokkani.data.local.entities.PartyEntity
import com.example.dokkani.data.local.entities.PaymentMethod
import com.example.dokkani.data.local.entities.ProductUnitEntity
import com.example.dokkani.data.local.entities.UserRole
import com.example.dokkani.ui.components.AppSearchBar
import com.example.dokkani.ui.components.BarcodeTextField
import com.example.dokkani.ui.components.PaymentMethodSelector
import com.example.dokkani.ui.components.ProductSortSelector
import com.example.dokkani.ui.components.isItemMatchQuery
import com.example.dokkani.ui.models.sortProducts
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesInvoiceScreen(
    currentUserRole: UserRole = UserRole.ADMIN,
    viewModel: SalesInvoiceViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var newCustomerName by remember { mutableStateOf("") }
    var newCustomerPhone by remember { mutableStateOf("") }
    var newCustomerTax by remember { mutableStateOf("") }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            snackbarHost = {
                uiState.feedbackMessage?.let { msg ->
                    Snackbar(
                        modifier = Modifier.padding(12.dp),
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
            val mainScrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(mainScrollState)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // شريط عنوان الشاشة والوصول السريع
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "إدارة فواتير البيع والأجل",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        val cashDrawerBalance = uiState.financialAccounts
                            .filter { it.accountType == com.example.dokkani.data.local.entities.FinancialAccountType.CASH_DRAWER }
                            .sumOf { it.currentBalance }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PointOfSale,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "إجمالي النقدية بالصندوق: %.2f %s".format(cashDrawerBalance, uiState.baseCurrencySymbol),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                // قسم المكونات التفاعلية بنسق متناسق وبدون أي فراغات
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val isTabletOrWide = maxWidth >= 840.dp

                    if (isTabletOrWide) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            // العمود الأيمن: بيانات العميل والبحث عن الأصناف
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CustomerDataCard(
                                    uiState = uiState,
                                    viewModel = viewModel,
                                    onAddNewCustomer = { showAddCustomerDialog = true }
                                )

                                SalesProductSearchCard(
                                    uiState = uiState,
                                    viewModel = viewModel
                                )
                            }

                            // العمود الأيسر: بنود الفاتورة وحساب الإجماليات
                            SalesInvoiceItemsAndTotalsCard(
                                uiState = uiState,
                                viewModel = viewModel,
                                modifier = Modifier.weight(1.25f)
                            )
                        }
                    } else {
                        // وضع الهواتف بالشاشات الرأسية
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CustomerDataCard(
                                uiState = uiState,
                                viewModel = viewModel,
                                onAddNewCustomer = { showAddCustomerDialog = true }
                            )

                            SalesProductSearchCard(
                                uiState = uiState,
                                viewModel = viewModel
                            )

                            SalesInvoiceItemsAndTotalsCard(
                                uiState = uiState,
                                viewModel = viewModel,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // القسم السفلي: استعراض فواتير البيع السابقة والتعديل والحذف
                SalesBottomHistorySection(
                    uiState = uiState,
                    viewModel = viewModel,
                    currentUserRole = currentUserRole,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // نافذة نجاح الاعتماد
        if (uiState.showSuccessDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissSuccessDialog() },
                icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(36.dp)) },
                title = { Text("تم اعتماد فاتورة البيع بنجاح", fontWeight = FontWeight.Bold) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "رقم الفاتورة المسجلة: ${uiState.lastSavedInvoiceNumber}",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = { viewModel.dismissSuccessDialog() }) {
                        Text("موافق")
                    }
                }
            )
        }

        // نافذة إضافة عميل جديد
        if (showAddCustomerDialog) {
            AlertDialog(
                onDismissRequest = { showAddCustomerDialog = false },
                title = { Text("إضافة عميل جديد", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newCustomerName,
                            onValueChange = { newCustomerName = it },
                            label = { Text("اسم العميل *") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = newCustomerPhone,
                            onValueChange = { newCustomerPhone = it },
                            label = { Text("رقم الهاتف") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = newCustomerTax,
                            onValueChange = { newCustomerTax = it },
                            label = { Text("الرقم الضريبي (إن وجد)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.addQuickCustomer(newCustomerName, newCustomerPhone, newCustomerTax)
                            showAddCustomerDialog = false
                            newCustomerName = ""
                            newCustomerPhone = ""
                            newCustomerTax = ""
                        },
                        enabled = newCustomerName.isNotBlank()
                    ) {
                        Text("حفظ وإضافة")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddCustomerDialog = false }) {
                        Text("إلغاء")
                    }
                }
            )
        }

        // نافذة إدخال PIN مدير النظام عند الحذف
        if (uiState.showAdminPinDialog) {
            var pinInput by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { viewModel.dismissAdminPinDialog() },
                icon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                title = { Text("صلاحيات مدير النظام", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("يرجى إدخال رمز PIN لمدير النظام لتنفيذ الإجراء:")
                        OutlinedTextField(
                            value = pinInput,
                            onValueChange = { pinInput = it },
                            label = { Text("رمز PIN") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        uiState.adminPinError?.let {
                            Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { viewModel.confirmAdminPin(pinInput) }) {
                        Text("تأكيد")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissAdminPinDialog() }) {
                        Text("إلغاء")
                    }
                }
            )
        }

        // نافذة معاينة تفاصيل الفاتورة
        if (uiState.showInvoiceDetailsDialog && uiState.selectedInvoice != null) {
            val inv = uiState.selectedInvoice!!
            val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.ENGLISH)
            val dateStr = dateFormat.format(Date(inv.date))

            AlertDialog(
                onDismissRequest = { viewModel.dismissInvoiceDetailsDialog() },
                title = { Text("تفاصيل فاتورة البيع #${inv.invoiceNumber}", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 350.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("التاريخ: $dateStr", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("طريقة السداد: ${inv.paymentMethod.labelArabic}", fontSize = 12.sp)
                            Text("الإجمالي النهائي: %.2f".format(inv.total), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Divider()
                        Text("بنود الفاتورة:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        uiState.selectedInvoiceWithDetails.forEach { item ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("بند صنف رقم: ${item.productId}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("الكمية: %.2f × %.2f".format(item.quantity, item.unitSellingPrice), fontSize = 11.sp)
                                    }
                                    Text("%.2f".format(item.totalPrice), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { viewModel.dismissInvoiceDetailsDialog() }) {
                        Text("إغلاق")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDataCard(
    uiState: SalesInvoiceUiState,
    viewModel: SalesInvoiceViewModel,
    onAddNewCustomer: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("بيانات العميل والفاتورة", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Button(
                    onClick = onAddNewCustomer,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("عميل جديد", fontSize = 11.sp)
                }
            }

            // اختيار العميل
            var customerDropdownExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = customerDropdownExpanded,
                onExpandedChange = { customerDropdownExpanded = !customerDropdownExpanded }
            ) {
                OutlinedTextField(
                    value = uiState.selectedCustomer?.name ?: "عميل سفري / نقدي (اضغط للاختيار)",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("اسم العميل") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = customerDropdownExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                ExposedDropdownMenu(
                    expanded = customerDropdownExpanded,
                    onDismissRequest = { customerDropdownExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("عميل سفري / نقدي العام", fontWeight = FontWeight.Bold) },
                        onClick = {
                            viewModel.selectCustomer(null)
                            customerDropdownExpanded = false
                        }
                    )
                    uiState.customers.forEach { customer ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(customer.name, fontWeight = FontWeight.Bold)
                                    Text("الهاتف: ${customer.phone} | الرصيد الحالي: %.2f".format(customer.currentBalance), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            },
                            onClick = {
                                viewModel.selectCustomer(customer)
                                customerDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.customerInvoiceNumber,
                    onValueChange = { viewModel.setCustomerInvoiceNumber(it) },
                    label = { Text("رقم مرجع/طلب العميل") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                )

                // اختيار مركز التكلفة
                var costCenterExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = costCenterExpanded,
                    onExpandedChange = { costCenterExpanded = !costCenterExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    val currentCenter = uiState.costCenters.firstOrNull { it.centerId == uiState.selectedCostCenterId }
                    OutlinedTextField(
                        value = currentCenter?.centerName ?: "الرئيسي",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("مركز التكلفة") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = costCenterExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = costCenterExpanded,
                        onDismissRequest = { costCenterExpanded = false }
                    ) {
                        uiState.costCenters.forEach { center ->
                            DropdownMenuItem(
                                text = { Text(center.centerName) },
                                onClick = {
                                    viewModel.selectCostCenter(center.centerId)
                                    costCenterExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SalesProductSearchCard(
    uiState: SalesInvoiceUiState,
    viewModel: SalesInvoiceViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("البحث وإضافة الأصناف للفاتورة", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                ProductSortSelector(
                    selectedOption = uiState.sortOption,
                    onOptionSelected = { viewModel.setSortOption(it) }
                )
            }

            // شريط إدخال البحث الموحد
            AppSearchBar(
                value = uiState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = "ابحث عن صنف بالاسم، الكود، أو بالميكروفون...",
                onBarcodeScanned = { scannedCode ->
                    viewModel.setSearchQuery(scannedCode)
                }
            )

            // شريط التصنيفات
            if (uiState.categories.size > 1) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(uiState.categories) { cat ->
                        FilterChip(
                            selected = uiState.selectedCategory == cat,
                            onClick = { viewModel.setSelectedCategory(cat) },
                            label = { Text(cat, fontSize = 11.sp) },
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }
            }

            // قائمة الأصناف المفلترة
            val filteredProducts = remember(uiState.productsWithUnits, uiState.searchQuery, uiState.selectedCategory, uiState.sortOption, uiState.productStockMap) {
                uiState.productsWithUnits.filter { p ->
                    val query = uiState.searchQuery
                    val matchesQuery = query.isBlank() ||
                            isItemMatchQuery(p.product.name, p.product.code, p.units.firstOrNull()?.barcode ?: "", p.product.category, query) ||
                            p.units.any { u -> isItemMatchQuery(p.product.name, p.product.code, u.barcode ?: "", p.product.category, query) }
                    val matchesCat = uiState.selectedCategory == "الكل" || p.product.category.trim() == uiState.selectedCategory.trim()
                    matchesQuery && matchesCat
                }.sortProducts(uiState.sortOption, uiState.productStockMap)
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 220.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filteredProducts) { item ->
                    val defaultUnit = item.units.firstOrNull()
                    val totalStock = uiState.productStockMap.getOrDefault(item.product.id, 0.0)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (defaultUnit != null) {
                                    viewModel.addProductItem(item.product, defaultUnit)
                                }
                            },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.product.name, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("كود: ${item.product.code}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        "الرصيد المتاح: %.2f".format(totalStock),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (totalStock > 0) Color(0xFF1B5E20) else MaterialTheme.colorScheme.error
                                    )
                                }
                            }

                            if (defaultUnit != null) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        "%.2f %s".format(defaultUnit.sellingPrice, uiState.currencySymbol),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    IconButton(
                                        onClick = { viewModel.addProductItem(item.product, defaultUnit) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.AddCircle, contentDescription = "إضافة", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SalesInvoiceItemsAndTotalsCard(
    uiState: SalesInvoiceUiState,
    viewModel: SalesInvoiceViewModel,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("بنود الفاتورة (${uiState.items.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                if (uiState.items.isNotEmpty()) {
                    TextButton(
                        onClick = { viewModel.clearInvoice() },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("محي الفاتورة", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            // جدول بنود الفاتورة
            if (uiState.items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), shape = RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("السلة فارغة. قم باختيار أو مسح باركود المنتجات لإضافتها.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(uiState.items) { line ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1.2f)) {
                                    Text(line.productName, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("الوحدة: ${line.unitName}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                // تعديل سعر البيع والكمية
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.weight(1.5f)
                                ) {
                                    OutlinedTextField(
                                        value = if (line.sellingPrice == 0.0) "" else line.sellingPrice.toString(),
                                        onValueChange = { v ->
                                            val price = v.toDoubleOrNull() ?: 0.0
                                            viewModel.updateItemSellingPrice(line.productId, line.unitId, price)
                                        },
                                        label = { Text("السعر", fontSize = 9.sp) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(6.dp)
                                    )

                                    OutlinedTextField(
                                        value = if (line.quantity == 0.0) "" else line.quantity.toString(),
                                        onValueChange = { v ->
                                            val qty = v.toDoubleOrNull() ?: 0.0
                                            viewModel.updateItemQuantity(line.productId, line.unitId, qty)
                                        },
                                        label = { Text("الكمية", fontSize = 9.sp) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                }

                                Column(
                                    horizontalAlignment = Alignment.End,
                                    modifier = Modifier.padding(start = 6.dp)
                                ) {
                                    Text("%.2f".format(line.totalPrice), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                    IconButton(
                                        onClick = { viewModel.removeItem(line.productId, line.unitId) },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Divider()

            // اختيار طريقة الدفع والتحصيل الديناميكية
            PaymentMethodSelector(
                selectedMethod = uiState.paymentMethod,
                onMethodSelected = { viewModel.setPaymentMethod(it) },
                financialAccounts = uiState.financialAccounts,
                selectedAccountId = uiState.selectedPaymentAccountId,
                onAccountSelected = { viewModel.setSelectedPaymentAccount(it) },
                modifier = Modifier.fillMaxWidth()
            )

            // الخصم والضريبة
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = if (uiState.discount == 0.0) "" else uiState.discount.toString(),
                    onValueChange = { v -> viewModel.setDiscount(v.toDoubleOrNull() ?: 0.0) },
                    label = { Text("الخصم") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Checkbox(
                        checked = uiState.isTaxApplied,
                        onCheckedChange = { viewModel.setTaxApplied(it) }
                    )
                    Text("ضريبة مبيعات (%.1f%%)".format(uiState.salesTaxRate * 100), fontSize = 11.sp)
                }
            }

            // ملاحظات الفاتورة
            OutlinedTextField(
                value = uiState.notes,
                onValueChange = { viewModel.setNotes(it) },
                label = { Text("ملاحظات الفاتورة") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            )

            // ملخص الإجماليات
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("المجموع الفرعي:", fontSize = 11.sp)
                        Text("%.2f %s".format(uiState.subtotal, uiState.currencySymbol), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                    if (uiState.discount > 0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("إجمالي الخصم:", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                            Text("-%.2f %s".format(uiState.discount, uiState.currencySymbol), fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                    if (uiState.isTaxApplied) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("مبلغ الضريبة:", fontSize = 11.sp)
                            Text("+%.2f %s".format(uiState.taxAmount, uiState.currencySymbol), fontSize = 11.sp)
                        }
                    }
                    Divider(modifier = Modifier.padding(vertical = 2.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("الإجمالي النهائي المستحق:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(
                            "%.2f %s".format(uiState.finalTotal, uiState.currencySymbol),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // زر الاعتماد الحفظ
            Button(
                onClick = { viewModel.executeSalesTransaction() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp),
                enabled = !uiState.isProcessing && uiState.items.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
            ) {
                if (uiState.isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("اعتماد وحفظ فاتورة البيع", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun SalesBottomHistorySection(
    uiState: SalesInvoiceUiState,
    viewModel: SalesInvoiceViewModel,
    currentUserRole: UserRole,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.toggleBottomHistoryExpanded() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("سجل وأرشيف فواتير البيع (${uiState.salesInvoices.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Icon(
                    imageVector = if (uiState.isBottomHistoryExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }

            AnimatedVisibility(visible = uiState.isBottomHistoryExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppSearchBar(
                        value = uiState.invoiceSearchQuery,
                        onValueChange = { viewModel.setInvoiceSearchQuery(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = "البحث برقم الفاتورة..."
                    )

                    val filteredInvoices = remember(uiState.salesInvoices, uiState.invoiceSearchQuery) {
                        uiState.salesInvoices.filter { inv ->
                            uiState.invoiceSearchQuery.isBlank() ||
                                inv.invoiceNumber.contains(uiState.invoiceSearchQuery, ignoreCase = true)
                        }
                    }

                    if (filteredInvoices.isEmpty()) {
                        Text("لا توجد فواتير بيع مسجلة حالياً.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(filteredInvoices) { inv ->
                                val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.ENGLISH)
                                val dateStr = dateFormat.format(Date(inv.date))

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("#${inv.invoiceNumber}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text("التاريخ: $dateStr | السداد: ${inv.paymentMethod.labelArabic}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("%.2f %s".format(inv.total, uiState.baseCurrencySymbol), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                                            Row {
                                                IconButton(
                                                    onClick = { viewModel.viewInvoiceDetails(inv) },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(Icons.Default.Visibility, contentDescription = "معاينة", tint = MaterialTheme.colorScheme.primary)
                                                }

                                                IconButton(
                                                    onClick = { viewModel.requestDeleteInvoice(inv, currentUserRole == UserRole.ADMIN) },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
