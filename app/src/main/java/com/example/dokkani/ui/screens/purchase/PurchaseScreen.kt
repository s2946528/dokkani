package com.example.dokkani.ui.screens.purchase

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.example.dokkani.ui.components.BarcodeTextField
import com.example.dokkani.ui.components.ReadOnlyReceiptAttachmentView
import com.example.dokkani.ui.components.ProductSortSelector
import com.example.dokkani.ui.models.sortProducts
import com.example.dokkani.data.local.entities.CurrencyEntity
import com.example.dokkani.data.local.entities.InvoiceEntity
import com.example.dokkani.data.local.entities.InvoiceItemEntity
import com.example.dokkani.data.local.entities.PartyEntity
import com.example.dokkani.data.local.entities.PaymentMethod
import com.example.dokkani.data.local.entities.ProductUnitEntity
import com.example.dokkani.data.local.entities.ProductWithUnits
import com.example.dokkani.data.local.entities.UserRole
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseScreen(
    currentUserRole: UserRole = UserRole.ADMIN,
    viewModel: PurchaseViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var showPurchaseReturnScreen by remember { mutableStateOf(false) }
    var invoiceToReturn by remember { mutableStateOf<InvoiceEntity?>(null) }

    if (showPurchaseReturnScreen) {
        val returnVm: com.example.dokkani.ui.screens.purchasereturn.PurchaseReturnViewModel = viewModel()
        LaunchedEffect(invoiceToReturn) {
            invoiceToReturn?.let { inv ->
                returnVm.selectInvoiceForReturn(inv)
            }
        }
        com.example.dokkani.ui.screens.purchasereturn.PurchaseReturnScreen(
            viewModel = returnVm,
            onNavigateBack = {
                showPurchaseReturnScreen = false
                invoiceToReturn = null
            }
        )
        return
    }

    var showAddSupplierDialog by remember { mutableStateOf(false) }
    var newSupplierName by remember { mutableStateOf("") }
    var newSupplierPhone by remember { mutableStateOf("") }
    var newSupplierTax by remember { mutableStateOf("") }

    var showQuickAddProductDialog by remember { mutableStateOf(false) }

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
            val mainScrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(mainScrollState)
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // شريط عنوان الشاشة والوصول السريع لمردود المشتريات
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
                                Icons.Default.ShoppingBag,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "إدارة فواتير الشراء والتوريد",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Button(
                            onClick = { showPurchaseReturnScreen = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.AssignmentReturn, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("مردود مشتريات", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // استخدام BoxWithConstraints لتوسيع وتوزيع الكروت ديناميكياً بحسب حجم الشاشة
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val isTabletOrWide = maxWidth >= 840.dp

                    if (isTabletOrWide) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            // العمود الأيمن: بيانات المورد والبحث عن الأصناف
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                SupplierDataCard(
                                    uiState = uiState,
                                    viewModel = viewModel,
                                    onAddNewSupplier = { showAddSupplierDialog = true }
                                )

                                ProductSearchAndAddCard(
                                    uiState = uiState,
                                    viewModel = viewModel,
                                    onAddNewProduct = { showQuickAddProductDialog = true }
                                )
                            }

                            // العمود الأيسر: بنود الفاتورة وحساب التكلفة والإجماليات
                            InvoiceItemsAndTotalsCard(
                                uiState = uiState,
                                viewModel = viewModel,
                                modifier = Modifier.weight(1.25f)
                            )
                        }
                    } else {
                        // وضع الهاتف والشاشات الرأسية: ترتيب متتابع وسلس
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            SupplierDataCard(
                                uiState = uiState,
                                viewModel = viewModel,
                                onAddNewSupplier = { showAddSupplierDialog = true }
                            )

                            ProductSearchAndAddCard(
                                uiState = uiState,
                                viewModel = viewModel,
                                onAddNewProduct = { showQuickAddProductDialog = true }
                            )

                            InvoiceItemsAndTotalsCard(
                                uiState = uiState,
                                viewModel = viewModel,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // القسم السفلي: استعراض فواتير الشراء والتوريد مع التعديل والحذف لمدير النظام
                PurchaseBottomHistorySection(
                    uiState = uiState,
                    viewModel = viewModel,
                    currentUserRole = currentUserRole,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // نافذة نتائج احتساب المتوسط المرجح للتكلفة WAC
        if (uiState.showSuccessDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissSuccessDialog() },
                icon = { Icon(Icons.Default.Calculate, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp)) },
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
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("تم إعادة احتساب تكلفة المخزون بالمتوسط المرجح (WAC):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
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
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text("${wac.productName} (${wac.unitName})", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("التكلفة السابقة: %.2f".format(wac.oldCost), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("شراء جديد: %.2f".format(wac.purchaseCost), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("WAC الجديد: %.2f %s".format(wac.newWacCost, uiState.currencySymbol), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF2E7D32))
                                        }
                                        Text("الرصيد الجديد: %.2f ${wac.unitName}".format(wac.newStock), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

        // نافذة إضافة صنف جديد وإدراجه مباشرة للفاتورة الحالية
        if (showQuickAddProductDialog) {
            QuickAddProductDialog(
                currencySymbol = uiState.currencySymbol,
                initialSearchQuery = uiState.searchQuery,
                onDismiss = { showQuickAddProductDialog = false },
                onConfirm = { name, category, unitName, costPrice, sellingPrice, barcode, quantity ->
                    viewModel.createAndAddProduct(
                        name = name,
                        category = category,
                        unitName = unitName,
                        costPrice = costPrice,
                        sellingPrice = sellingPrice,
                        barcode = barcode,
                        quantity = quantity,
                        onSuccess = {
                            showQuickAddProductDialog = false
                        }
                    )
                }
            )
        }

        // نافذة استعراض تفاصيل فاتورة الشراء وبنودها
        if (uiState.showInvoiceDetailsDialog && uiState.selectedInvoice != null) {
            PurchaseInvoiceDetailsDialog(
                invoice = uiState.selectedInvoice!!,
                items = uiState.selectedInvoiceWithDetails,
                products = uiState.productsWithUnits,
                parties = uiState.suppliers,
                onDismiss = { viewModel.dismissInvoiceDetails() },
                onStartReturn = { inv ->
                    viewModel.dismissInvoiceDetails()
                    invoiceToReturn = inv
                    showPurchaseReturnScreen = true
                },
                currencySymbol = uiState.currencySymbol,
                currencies = uiState.availableCurrencies,
                baseCurrencySymbol = uiState.baseCurrencySymbol
            )
        }

        // نافذة تعديل فاتورة الشراء لمدير النظام
        if (uiState.showEditPurchaseDialog && uiState.selectedInvoice != null) {
            EditPurchaseInvoiceDialog(
                invoice = uiState.selectedInvoice!!,
                initialNotes = uiState.editPurchaseNotes,
                initialPaymentMethod = uiState.editPurchasePaymentMethod,
                initialSupplier = uiState.editPurchaseSupplier,
                initialCurrency = uiState.editPurchaseCurrency,
                initialExchangeRate = uiState.editPurchaseExchangeRate,
                items = uiState.editPurchaseItems,
                suppliers = uiState.suppliers,
                availableCurrencies = uiState.availableCurrencies,
                baseCurrencySymbol = uiState.baseCurrencySymbol,
                onUpdateItemQty = { pId, uId, qty -> viewModel.updateEditPurchaseItemQty(pId, uId, qty) },
                onUpdateItemCost = { pId, uId, cost -> viewModel.updateEditPurchaseItemCost(pId, uId, cost) },
                onDismiss = { viewModel.dismissEditPurchaseDialog() },
                onSave = { notes, method, supplier, currency, rate ->
                    viewModel.saveEditedPurchaseInvoice(notes, method, supplier, currency, rate)
                }
            )
        }

        // نافذة إدخال رمز مدير النظام للتحقق أماناً Admin PIN
        if (uiState.showAdminPinDialog) {
            AdminPinVerificationDialog(
                uiState = uiState,
                onDismiss = { viewModel.dismissAdminPinDialog() },
                onVerify = { pin ->
                    viewModel.verifyAdminPin(pin) {
                        val pending = uiState.pendingAdminAction
                        if (pending is PendingAdminAction.EditInvoice) {
                            viewModel.openEditPurchaseDialog(pending.invoice)
                        }
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SupplierDataCard(
    uiState: PurchaseUiState,
    viewModel: PurchaseViewModel,
    onAddNewSupplier: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                    Text(
                        "بيانات المورد والتوريد",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                FilledTonalButton(
                    onClick = onAddNewSupplier,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("مورد جديد", fontSize = 12.sp)
                }
            }

            // اختيار المورد والعملة
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                var supplierDropdownExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = supplierDropdownExpanded,
                    onExpandedChange = { supplierDropdownExpanded = it },
                    modifier = Modifier.weight(1.2f)
                ) {
                    OutlinedTextField(
                        value = uiState.selectedSupplier?.let {
                            "${it.name} (${if (it.currentBalance < 0) "دائن: %.2f".format(-it.currentBalance) else "رصيد: %.2f".format(it.currentBalance)} ${uiState.currencySymbol})"
                        } ?: "اختر المورد...",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = supplierDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        label = { Text("المورد") },
                        singleLine = true
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
                                            text = if (supplier.currentBalance < 0) "مستحق له (دائن): %.2f %s".format(-supplier.currentBalance, uiState.currencySymbol) else "رصيده: %.2f %s".format(supplier.currentBalance, uiState.currencySymbol),
                                            fontSize = 11.sp,
                                            color = if (supplier.currentBalance < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
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

                // اختيار عملة الفاتورة
                var currencyDropdownExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = currencyDropdownExpanded,
                    onExpandedChange = { currencyDropdownExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = uiState.selectedCurrency?.let { "${it.name} (${it.symbol})" } ?: "العملة",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        label = { Text("عملة الشراء") },
                        singleLine = true
                    )

                    ExposedDropdownMenu(
                        expanded = currencyDropdownExpanded,
                        onDismissRequest = { currencyDropdownExpanded = false }
                    ) {
                        uiState.availableCurrencies.forEach { curr ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("${curr.name} (${curr.symbol})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text("1 = %.2f %s".format(curr.exchangeRateToBase, uiState.baseCurrencySymbol), fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                    }
                                },
                                onClick = {
                                    viewModel.selectCurrency(curr)
                                    currencyDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            if (uiState.selectedCurrency != null && !uiState.selectedCurrency!!.isBaseCurrency) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "سعر الصرف لـ (${uiState.currencyName}):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    OutlinedTextField(
                        value = "%.4f".format(uiState.exchangeRate),
                        onValueChange = { str ->
                            str.toDoubleOrNull()?.let { viewModel.setExchangeRate(it) }
                        },
                        label = { Text("1 ${uiState.selectedCurrency?.code} = بالـ (${uiState.baseCurrencySymbol})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.width(180.dp),
                        singleLine = true
                    )
                }
            }

            // رقم فاتورة المورد الورقية
            OutlinedTextField(
                value = uiState.supplierInvoiceNumber,
                onValueChange = { viewModel.setSupplierInvoiceNumber(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("رقم فاتورة المورد / سند الاستلام الورقي") },
                leadingIcon = { Icon(Icons.Default.Receipt, contentDescription = null) },
                singleLine = true
            )

            // ملاحظات الفاتورة
            OutlinedTextField(
                value = uiState.notes,
                onValueChange = { viewModel.setNotes(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("ملاحظات الفاتورة والتوريد (اختياري)") },
                leadingIcon = { Icon(Icons.Default.EditNote, contentDescription = null) },
                singleLine = true
            )
        }
    }
}

@Composable
private fun ProductSearchAndAddCard(
    uiState: PurchaseUiState,
    viewModel: PurchaseViewModel,
    onAddNewProduct: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "البحث عن الأصناف للتوريد وإضافتها",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                FilledTonalButton(
                    onClick = onAddNewProduct,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("صنف جديد", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // حقل البحث السريع مع زر + صنف جديد بجانبه مباشرة
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BarcodeTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier.weight(1f),
                    placeholder = "ابحث بالاسم أو امسح الباركود...",
                    label = "البحث أو مسح باركود الصنف",
                    onBarcodeScanned = { scannedCode ->
                        viewModel.setSearchQuery(scannedCode)
                    },
                    singleLine = true
                )

                Button(
                    onClick = onAddNewProduct,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("صنف جديد", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // تصنيفات سريعة
            val categories = uiState.categories.ifEmpty { listOf("الكل") }
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(categories) { cat ->
                    val isSelected = uiState.selectedCategory == cat
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setSelectedCategory(cat) },
                        label = {
                            Text(
                                cat,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // أداة الفرز والترتيب أفقياً ومستقلا بين التصنيفات وشبكة الأصناف
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Sort,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "ترتيب الأصناف حسب:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                ProductSortSelector(
                    selectedOption = uiState.sortOption,
                    onOptionSelected = { viewModel.setSortOption(it) }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            val filteredProducts = uiState.productsWithUnits.filter { p ->
                val query = uiState.searchQuery.trim().lowercase()
                val matchesQuery = query.isEmpty() || p.product.name.lowercase().contains(query) ||
                        p.product.code.lowercase().contains(query) ||
                        p.units.any { it.barcode.lowercase().contains(query) }
                val matchesCat = uiState.selectedCategory == "الكل" || p.product.category.trim() == uiState.selectedCategory.trim()
                matchesQuery && matchesCat
            }.sortProducts(uiState.sortOption, uiState.productStockMap)

            if (filteredProducts.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        if (uiState.searchQuery.isBlank()) "لا توجد أصناف في قاعدة البيانات" else "لا توجد أصناف تطابق: \"${uiState.searchQuery}\"",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = onAddNewProduct,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (uiState.searchQuery.isNotBlank()) "إضافة «${uiState.searchQuery}» كصنف جديد" else "إنشاء صنف جديد الآن",
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredProducts) { pw ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
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
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            pw.product.category,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
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
                                            Text(
                                                "${unit.unitName} (تكلفة: ${unit.costPrice} ${uiState.currencySymbol})",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium
                                            )
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

@Composable
private fun InvoiceItemsAndTotalsCard(
    uiState: PurchaseUiState,
    viewModel: PurchaseViewModel,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ترويسة بنود الفاتورة
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "بنود فاتورة الشراء الواردة (${uiState.items.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "الكميات المدخلة ترفع المخزون وتحدّث التكلفة بالمتوسط المرجح (WAC)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (uiState.items.isNotEmpty()) {
                    TextButton(onClick = { viewModel.clearInvoice() }) {
                        Icon(
                            Icons.Default.DeleteSweep,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "مسح الفاتورة بالكامل",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            HorizontalDivider()

            // قائمة بنود الفاتورة - توسع ديناميكي بدون اقتطاع
            if (uiState.items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.ShoppingBag,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "لم تتم إضافة أي أصناف للفاتورة بعد",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        )
                        Text(
                            "اختر المورد ثم أضف المنتجات من قائمة البحث",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.items.forEach { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            item.productName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            "الوحدة: ${item.unitName} (التكلفة المسجلة سابقاً: ${item.oldCostPrice} ${uiState.currencySymbol})",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    IconButton(
                                        onClick = { viewModel.removeItem(item.productId, item.unitId) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "حذف البند",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    var qtyInput by remember(item.quantity) {
                                        mutableStateOf(if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString())
                                    }
                                    var costInput by remember(item.costPrice) {
                                        mutableStateOf(if (item.costPrice % 1.0 == 0.0) item.costPrice.toInt().toString() else item.costPrice.toString())
                                    }

                                    // حقل الكمية
                                    OutlinedTextField(
                                        value = qtyInput,
                                        onValueChange = { str ->
                                            qtyInput = str
                                            str.toDoubleOrNull()?.let { qty ->
                                                viewModel.updateItemQuantity(item.productId, item.unitId, qty)
                                            }
                                        },
                                        label = { Text("الكمية") },
                                        modifier = Modifier.weight(1f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true
                                    )

                                    // حقل سعر الشراء والتكلفة الجديد
                                    OutlinedTextField(
                                        value = costInput,
                                        onValueChange = { str ->
                                            costInput = str
                                            str.toDoubleOrNull()?.let { cost ->
                                                viewModel.updateItemCostPrice(item.productId, item.unitId, cost)
                                            }
                                        },
                                        label = { Text("سعر الشراء") },
                                        modifier = Modifier.weight(1.2f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true
                                    )

                                    // إجمالي البند
                                    Column(
                                        modifier = Modifier.weight(1.1f),
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        Text(
                                            "إجمالي البند",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            "%.2f %s".format(item.totalCost, uiState.currencySymbol),
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider()

            // طريقة السداد: مرتبة أفقياً بشكل مرن ومنتظم يمنع التكدس
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "طريقة السداد:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = uiState.paymentMethod == PaymentMethod.CASH,
                        onClick = { viewModel.setPaymentMethod(PaymentMethod.CASH) },
                        label = {
                            Text(
                                "نقداً",
                                fontSize = 12.sp,
                                fontWeight = if (uiState.paymentMethod == PaymentMethod.CASH) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Money, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = uiState.paymentMethod == PaymentMethod.CREDIT,
                        onClick = { viewModel.setPaymentMethod(PaymentMethod.CREDIT) },
                        label = {
                            Text(
                                "آجل (حساب المورد)",
                                fontSize = 11.sp,
                                fontWeight = if (uiState.paymentMethod == PaymentMethod.CREDIT) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        modifier = Modifier.weight(1.3f)
                    )

                    FilterChip(
                        selected = uiState.paymentMethod == PaymentMethod.BANK_TRANSFER,
                        onClick = { viewModel.setPaymentMethod(PaymentMethod.BANK_TRANSFER) },
                        label = {
                            Text(
                                "تحويل بنكي",
                                fontSize = 12.sp,
                                fontWeight = if (uiState.paymentMethod == PaymentMethod.BANK_TRANSFER) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        modifier = Modifier.weight(1.1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // ملخص المبالغ والضريبة (معطلة افتراضياً وفق بيئة العمل)
            val purPct = if (uiState.purchaseTaxRate <= 1.0) uiState.purchaseTaxRate * 100.0 else uiState.purchaseTaxRate
            val purPctLabel = if (purPct % 1.0 == 0.0) purPct.toInt().toString() else "%.1f".format(purPct)

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = uiState.isTaxApplied,
                                onCheckedChange = { viewModel.setTaxApplied(it) }
                            )
                            Text(
                                "تطبيق ضريبة المشتريات ($purPctLabel%)",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (!uiState.isTaxApplied) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    "معطلة افتراضياً (0%)",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("المجموع قبل الضريبة:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("%.2f %s".format(uiState.subtotal, uiState.currencySymbol), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }

                    if (uiState.isTaxApplied) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("ضريبة المشتريات ($purPctLabel%):", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("%.2f %s".format(uiState.taxAmount, uiState.currencySymbol), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("الصافي الإجمالي:", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                        Text(
                            "%.2f %s".format(uiState.finalTotal, uiState.currencySymbol),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = { viewModel.executePurchaseTransaction() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
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

/**
 * شريط وقائمة استعراض فواتير الشراء أسفل شاشة المشتريات
 * يتضمن إمكانية التعديل والحذف لمدير النظام بأزرار وإجراءات واضحة
 */
@Composable
private fun PurchaseBottomHistorySection(
    uiState: PurchaseUiState,
    viewModel: PurchaseViewModel,
    currentUserRole: UserRole,
    modifier: Modifier = Modifier
) {
    var deleteCandidate by remember { mutableStateOf<InvoiceEntity?>(null) }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp)) {
            // شريط العنوان مع زر التوسيع/الطي وعداد الفواتير وحقل البحث
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Receipt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "سجل فواتير الشراء والتوريد السابقة",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "${uiState.purchaseInvoices.size} فاتورة",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (uiState.isBottomHistoryExpanded) {
                        OutlinedTextField(
                            value = uiState.invoiceSearchQuery,
                            onValueChange = { viewModel.setInvoiceSearchQuery(it) },
                            placeholder = { Text("بحث برقم الفاتورة أو المورد...", fontSize = 11.sp) },
                            modifier = Modifier
                                .widthIn(max = 220.dp)
                                .height(46.dp),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    TextButton(
                        onClick = { viewModel.toggleBottomHistoryExpanded() },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            if (uiState.isBottomHistoryExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (uiState.isBottomHistoryExpanded) "تصغير السجل" else "استعراض الفواتير", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // محتوى القائمة القابلة للتوسيع
            if (uiState.isBottomHistoryExpanded) {
                Spacer(modifier = Modifier.height(6.dp))

                val filteredInvoices = uiState.purchaseInvoices.filter { inv ->
                    if (uiState.invoiceSearchQuery.isBlank()) true
                    else {
                        val partyName = inv.partyId?.let { pId ->
                            uiState.suppliers.find { it.id == pId }?.name
                        } ?: "مورد عام"
                        inv.invoiceNumber.contains(uiState.invoiceSearchQuery, ignoreCase = true) ||
                                partyName.contains(uiState.invoiceSearchQuery, ignoreCase = true)
                    }
                }

                if (filteredInvoices.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("لا توجد فواتير شراء مسجلة", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredInvoices) { inv ->
                            val supplierName = inv.partyId?.let { pId ->
                                uiState.suppliers.find { it.id == pId }?.name
                            } ?: "مورد نقدي عام"

                            Card(
                                modifier = Modifier
                                    .width(280.dp)
                                    .clickable { viewModel.openInvoiceDetails(inv) },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                border = CardDefaults.outlinedCardBorder()
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            inv.invoiceNumber,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Surface(
                                            color = if (inv.paymentMethod == PaymentMethod.CREDIT) Color(0xFFE65100) else Color(0xFF1B5E20),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                inv.paymentMethod.labelArabic,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text("المورد: $supplierName", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)

                                    val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
                                    Text("التاريخ: ${dateFormat.format(Date(inv.date))}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "%.2f %s".format(inv.total, uiState.currencySymbol),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )

                                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                            // 1. زر عرض
                                            IconButton(
                                                onClick = { viewModel.openInvoiceDetails(inv) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.Visibility, contentDescription = "عرض التفاصيل", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                            }

                                            // 2. زر تعديل مع التحقق من مدير النظام
                                            IconButton(
                                                onClick = {
                                                    if (currentUserRole == UserRole.ADMIN) {
                                                        viewModel.openEditPurchaseDialog(inv)
                                                    } else {
                                                        viewModel.openAdminPinDialog(PendingAdminAction.EditInvoice(inv))
                                                    }
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = "تعديل الفاتورة", tint = Color(0xFF1565C0), modifier = Modifier.size(16.dp))
                                            }

                                            // 3. زر حذف مع التحقق من مدير النظام
                                            IconButton(
                                                onClick = {
                                                    if (currentUserRole == UserRole.ADMIN) {
                                                        deleteCandidate = inv
                                                    } else {
                                                        viewModel.openAdminPinDialog(PendingAdminAction.DeleteInvoice(inv))
                                                    }
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "حذف الفاتورة", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
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

    // نافذة تأكيد الحذف لمدير النظام
    if (deleteCandidate != null) {
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(32.dp)) },
            title = { Text("تأكيد حذف فاتورة الشراء", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "هل أنت متأكد من رغبتك في حذف فاتورة الشراء رقم (${deleteCandidate!!.invoiceNumber}) بقيمة %.2f %s؟".format(deleteCandidate!!.total, uiState.currencySymbol),
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("تنبيه أمان ومحاسبة لمدير النظام:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                            Text("• سيتم إلغاء حركات المخزون وخصم الكميات الواردة وإعادة احتساب WAC تلقائياً.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                            Text("• في حال الفاتورة الآجلة سيتم تعديل رصيد ومستحقات المورد.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                            Text("• في حال الفاتورة النقدية سيتم خصم منصرفات الصندوق للشفت الحالي.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val invToDelete = deleteCandidate!!
                        deleteCandidate = null
                        viewModel.deletePurchaseInvoice(invToDelete.id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("حذف نهائي وعكس القيود و WAC")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidate = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

/**
 * نافذة التحقق من رمز مدير النظام أماناً Permisson & Admin PIN Check
 */
@Composable
private fun AdminPinVerificationDialog(
    uiState: PurchaseUiState,
    onDismiss: () -> Unit,
    onVerify: (pin: String) -> Unit
) {
    var pin by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp)) },
        title = { Text("التحقق من صلاحيات مدير النظام (Admin Only)", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "هذا الإجراء (تعديل أو حذف فاتورة الشراء) يتطلب صلاحيات مدير النظام (مدير النظام). يرجى إدخال رمز PIN الخاص بالمدير لتأكيد العملية وتفعيل العكس المحاسبي وتكلفة WAC:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 8) pin = it },
                    label = { Text("رمز PIN للمدير") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth(),
                    isError = uiState.adminPinError != null
                )
                if (uiState.adminPinError != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(uiState.adminPinError!!, color = MaterialTheme.colorScheme.error, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onVerify(pin) },
                enabled = pin.isNotBlank()
            ) {
                Text("تأكيد الصلاحية")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

/**
 * نافذة استعراض تفاصيل فاتورة الشراء وبنودها
 */
@Composable
private fun PurchaseInvoiceDetailsDialog(
    invoice: InvoiceEntity,
    items: List<InvoiceItemEntity>,
    products: List<ProductWithUnits>,
    parties: List<PartyEntity>,
    onDismiss: () -> Unit,
    onStartReturn: (InvoiceEntity) -> Unit = {},
    currencySymbol: String = "ر.ي",
    currencies: List<CurrencyEntity> = emptyList(),
    baseCurrencySymbol: String = "ر.ي"
) {
    val supplierName = invoice.partyId?.let { pId ->
        parties.find { it.id == pId }?.name
    } ?: "مورد نقدي عام"

    val currency = currencies.find { it.id == invoice.currencyId }
    val displayCurrencySymbol = currency?.symbol ?: currencySymbol
    val rate = if (invoice.exchangeRate > 0) invoice.exchangeRate else 1.0

    val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("تفاصيل فاتورة الشراء: ${invoice.invoiceNumber}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("التاريخ: ${dateFormat.format(Date(invoice.date))}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق")
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // بيانات التوريد والمورد والعملة
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("المورد: $supplierName", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text("طريقة السداد: ${invoice.paymentMethod.labelArabic}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (rate != 1.0) {
                                Text("سعر الصرف: 1 ${currency?.code ?: ""} = %.2f %s".format(rate, baseCurrencySymbol), fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            val invTotalForeign = if (rate > 0) invoice.total / rate else invoice.total
                            Text("الإجمالي: %.2f %s".format(invTotalForeign, displayCurrencySymbol), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                            if (rate != 1.0) {
                                Text("(ما يعادل %.2f %s)".format(invoice.total, baseCurrencySymbol), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text("بنود الأصناف والكميات الواردة (${items.size} بند):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(4.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(items) { item ->
                        val prod = products.find { it.product.id == item.productId }
                        val unit = prod?.units?.find { it.id == item.productUnitId }
                        val unitName = unit?.unitName ?: "وحدة"

                        val itemUnitCostForeign = if (rate > 0) item.unitCostPrice / rate else item.unitCostPrice
                        val itemTotalForeign = if (rate > 0) item.totalPrice / rate else item.totalPrice

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
                                Column {
                                    Text(prod?.product?.name ?: "صنف #${item.productId}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Text("الكمية: %.2f %s × %.2f %s".format(item.quantity, unitName, itemUnitCostForeign, displayCurrencySymbol), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        "%.2f %s".format(itemTotalForeign, displayCurrencySymbol),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    if (rate != 1.0) {
                                        Text("%.2f %s".format(item.totalPrice, baseCurrencySymbol), fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }

                if (invoice.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("الملاحظات: ${invoice.notes}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                if (!invoice.receiptImagePath.isNullOrBlank()) {
                    ReadOnlyReceiptAttachmentView(
                        receiptImagePath = invoice.receiptImagePath
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onStartReturn(invoice) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.AssignmentReturn, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إجراء مردود على الفاتورة", fontSize = 12.sp)
                }
                TextButton(onClick = onDismiss) {
                    Text("إغلاق")
                }
            }
        }
    )
}

/**
 * نافذة تعديل فاتورة الشراء لمدير النظام مع دعم تعدد العملات وبنود الفاتورة
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditPurchaseInvoiceDialog(
    invoice: InvoiceEntity,
    initialNotes: String,
    initialPaymentMethod: PaymentMethod,
    initialSupplier: PartyEntity?,
    initialCurrency: CurrencyEntity?,
    initialExchangeRate: Double,
    items: List<PurchaseLineItem>,
    suppliers: List<PartyEntity>,
    availableCurrencies: List<CurrencyEntity>,
    baseCurrencySymbol: String,
    onUpdateItemQty: (productId: Long, unitId: Long, qty: Double) -> Unit,
    onUpdateItemCost: (productId: Long, unitId: Long, cost: Double) -> Unit,
    onDismiss: () -> Unit,
    onSave: (notes: String, method: PaymentMethod, supplier: PartyEntity?, currency: CurrencyEntity?, rate: Double) -> Unit
) {
    var notes by remember { mutableStateOf(initialNotes) }
    var selectedMethod by remember { mutableStateOf(initialPaymentMethod) }
    var selectedSupplier by remember { mutableStateOf(initialSupplier) }
    var selectedCurrency by remember { mutableStateOf(initialCurrency) }
    var exchangeRate by remember { mutableStateOf(initialExchangeRate) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("تعديل فاتورة الشراء: ${invoice.invoiceNumber}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
            ) {
                // اختيار المورد والعملة
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    var supplierDropdown by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = supplierDropdown,
                        onExpandedChange = { supplierDropdown = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = selectedSupplier?.name ?: "مورد عام",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("المورد", fontSize = 11.sp) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = supplierDropdown) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            singleLine = true
                        )
                        ExposedDropdownMenu(expanded = supplierDropdown, onDismissRequest = { supplierDropdown = false }) {
                            suppliers.forEach { s ->
                                DropdownMenuItem(
                                    text = { Text(s.name, fontWeight = FontWeight.Bold) },
                                    onClick = { selectedSupplier = s; supplierDropdown = false }
                                )
                            }
                        }
                    }

                    var currencyDropdown by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = currencyDropdown,
                        onExpandedChange = { currencyDropdown = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = selectedCurrency?.let { "${it.name} (${it.symbol})" } ?: "العملة",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("العملة", fontSize = 11.sp) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyDropdown) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            singleLine = true
                        )
                        ExposedDropdownMenu(expanded = currencyDropdown, onDismissRequest = { currencyDropdown = false }) {
                            availableCurrencies.forEach { c ->
                                DropdownMenuItem(
                                    text = { Text("${c.name} (${c.symbol})") },
                                    onClick = {
                                        selectedCurrency = c
                                        exchangeRate = c.exchangeRateToBase
                                        currencyDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // طريقة السداد
                Text("طريقة السداد:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilterChip(
                        selected = selectedMethod == PaymentMethod.CASH,
                        onClick = { selectedMethod = PaymentMethod.CASH },
                        label = { Text("نقداً", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = selectedMethod == PaymentMethod.CREDIT,
                        onClick = { selectedMethod = PaymentMethod.CREDIT },
                        label = { Text("آجل", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = selectedMethod == PaymentMethod.BANK_TRANSFER,
                        onClick = { selectedMethod = PaymentMethod.BANK_TRANSFER },
                        label = { Text("تحويل بنكي", fontSize = 11.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text("تعديل كميات وأسعار تكلفة الأصناف:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(items) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(6.dp)) {
                                Text(item.productName, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString(),
                                        onValueChange = { str ->
                                            str.toDoubleOrNull()?.let { onUpdateItemQty(item.productId, item.unitId, it) }
                                        },
                                        label = { Text("الكمية", fontSize = 10.sp) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = if (item.costPrice % 1.0 == 0.0) item.costPrice.toInt().toString() else item.costPrice.toString(),
                                        onValueChange = { str ->
                                            str.toDoubleOrNull()?.let { onUpdateItemCost(item.productId, item.unitId, it) }
                                        },
                                        label = { Text("التكلفة", fontSize = 10.sp) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("الملاحظات", fontSize = 11.sp) },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(notes, selectedMethod, selectedSupplier, selectedCurrency, exchangeRate) }) {
                Text("حفظ التعديلات وعكس القيود")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

/**
 * نافذة منبثقة سريعة لإنشاء صنف جديد وإدراجه مباشرة إلى فاتورة الشراء الحالية
 */
@Composable
fun QuickAddProductDialog(
    currencySymbol: String,
    initialSearchQuery: String = "",
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        category: String,
        unitName: String,
        costPrice: Double,
        sellingPrice: Double,
        barcode: String?,
        quantity: Double
    ) -> Unit
) {
    val cleanQuery = initialSearchQuery.trim()
    val isQueryDigits = cleanQuery.isNotBlank() && cleanQuery.all { it.isDigit() } && cleanQuery.length >= 4

    var name by remember { mutableStateOf(if (!isQueryDigits) cleanQuery else "") }
    var category by remember { mutableStateOf("عام") }
    var unitName by remember { mutableStateOf("حبة") }
    var costPriceText by remember { mutableStateOf("") }
    var sellingPriceText by remember { mutableStateOf("") }
    var barcodeText by remember { mutableStateOf(if (isQueryDigits) cleanQuery else "") }
    var quantityText by remember { mutableStateOf("1") }
    var nameError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.AddBusiness,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                "إضافة صنف جديد وإدراجه للفاتورة",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "سيتم حفظ الصنف في قاعدة البيانات وإدراجه مباشرة في بنود الفاتورة الحالية.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // اسم الصنف (إلزامي)
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (it.isNotBlank()) nameError = false
                    },
                    label = { Text("اسم الصنف *") },
                    isError = nameError,
                    supportingText = if (nameError) {
                        { Text("اسم الصنف مطلوب", color = MaterialTheme.colorScheme.error) }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.ShoppingBag, contentDescription = null) }
                )

                // التصنيف والوحدة الأساسية
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("التصنيف / المجموعة") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Category, contentDescription = null) }
                    )

                    OutlinedTextField(
                        value = unitName,
                        onValueChange = { unitName = it },
                        label = { Text("الوحدة الأساسية") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { Text("حبة، كرتون...") }
                    )
                }

                // سعر التكلفة وسعر البيع
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = costPriceText,
                        onValueChange = { costPriceText = it },
                        label = { Text("سعر التكلفة ($currencySymbol)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = sellingPriceText,
                        onValueChange = { sellingPriceText = it },
                        label = { Text("سعر البيع ($currencySymbol)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }

                // الكمية المشتراة المراد إدراجها للفاتورة
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it },
                    label = { Text("الكمية المراد توريدها للفاتورة") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Inventory2, contentDescription = null) }
                )

                // الباركود مع زر التوليد التلقائي
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = barcodeText,
                        onValueChange = { barcodeText = it },
                        label = { Text("الباركود (اختياري)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = null) }
                    )

                    OutlinedButton(
                        onClick = {
                            barcodeText = (100000000000L + (Math.random() * 900000000000L).toLong()).toString()
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier.padding(top = 6.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("توليد", fontSize = 11.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        nameError = true
                        return@Button
                    }
                    val cost = costPriceText.toDoubleOrNull() ?: 0.0
                    val selling = sellingPriceText.toDoubleOrNull() ?: (cost * 1.2)
                    val qty = (quantityText.toDoubleOrNull() ?: 1.0).coerceAtLeast(0.01)

                    onConfirm(
                        name,
                        category,
                        unitName,
                        cost,
                        selling,
                        barcodeText.ifBlank { null },
                        qty
                    )
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("حفظ وإدراج للفاتورة", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
