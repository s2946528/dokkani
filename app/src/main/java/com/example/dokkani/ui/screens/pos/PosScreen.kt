package com.example.dokkani.ui.screens.pos

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.example.dokkani.ui.components.BarcodeTextField
import com.example.dokkani.ui.components.PaymentMethodSelector
import com.example.dokkani.ui.components.ProductSortSelector
import com.example.dokkani.ui.components.ShiftStatusBar
import com.example.dokkani.ui.components.ShiftBottomStatusBar
import com.example.dokkani.ui.components.CashDrawerBreakdownDialog
import com.example.dokkani.ui.components.BankWalletsBreakdownDialog
import com.example.dokkani.ui.models.sortProducts
import com.example.dokkani.data.local.entities.InvoiceEntity
import com.example.dokkani.data.local.entities.PartyEntity
import com.example.dokkani.data.local.entities.PartyType
import com.example.dokkani.data.local.entities.PaymentMethod
import com.example.dokkani.data.local.entities.UserRole
import com.example.dokkani.domain.pos.PosOperation
import com.example.dokkani.domain.pos.PosTransactionRecord

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    viewModel: PosViewModel = viewModel(),
    currentUserRole: UserRole = UserRole.ADMIN
) {
    val uiState by viewModel.uiState.collectAsState()

    var showCashBreakdownDialog by remember { mutableStateOf(false) }
    var showBankBreakdownDialog by remember { mutableStateOf(false) }

    var showAddPartyDialog by remember { mutableStateOf(false) }
    var newPartyName by remember { mutableStateOf("") }
    var newPartyPhone by remember { mutableStateOf("") }
    var newPartyLimit by remember { mutableStateOf("1000") }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            snackbarHost = {
                uiState.userFeedbackMessage?.let { message ->
                    Snackbar(
                        modifier = Modifier.padding(16.dp),
                        containerColor = if (uiState.isError) MaterialTheme.colorScheme.errorContainer else Color(0xFF1B5E20),
                        contentColor = if (uiState.isError) MaterialTheme.colorScheme.onErrorContainer else Color.White,
                        action = {
                            TextButton(onClick = { viewModel.dismissFeedback() }) {
                                Text("حسناً", color = Color.White)
                            }
                        }
                    ) {
                        Text(message)
                    }
                }
            }
        ) { paddingValues ->
            @OptIn(ExperimentalMaterial3Api::class)
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = { viewModel.refreshData() },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                val isCompact = maxWidth < 700.dp

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(if (isCompact) 6.dp else 8.dp)
                ) {
                    // 1. شريط ترويسة علوي مدمج وأنيق يوفر المساحة الرأسية الكاملة لقائمة البنود والسلة
                    var showShiftSummaryModal by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // محدد نوع العملية الحالية (افتراضياً: فاتورة بيع)
                        var opMenuExpanded by remember { mutableStateOf(false) }
                        Box {
                            Surface(
                                onClick = { opMenuExpanded = true },
                                shape = RoundedCornerShape(8.dp),
                                color = when (uiState.activeOperation) {
                                    PosOperation.SALE -> Color(0xFF1B5E20)
                                    PosOperation.PURCHASE -> Color(0xFF1976D2)
                                    PosOperation.SALE_RETURN, PosOperation.PURCHASE_RETURN -> Color(0xFFD32F2F)
                                    PosOperation.RECEIPT -> Color(0xFF00796B)
                                    PosOperation.EXPENSE -> Color(0xFFE65100)
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = when (uiState.activeOperation) {
                                            PosOperation.SALE -> Icons.Default.PointOfSale
                                            PosOperation.PURCHASE -> Icons.Default.ShoppingBag
                                            PosOperation.SALE_RETURN -> Icons.Default.AssignmentReturn
                                            PosOperation.PURCHASE_RETURN -> Icons.Default.RemoveShoppingCart
                                            PosOperation.RECEIPT -> Icons.Default.ArrowDownward
                                            PosOperation.EXPENSE -> Icons.Default.ArrowUpward
                                        },
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = uiState.activeOperation.titleArabic,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "تغيير نوع العملية",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = opMenuExpanded,
                                onDismissRequest = { opMenuExpanded = false }
                            ) {
                                PosOperation.entries.forEach { op ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = when (op) {
                                                        PosOperation.SALE -> Icons.Default.PointOfSale
                                                        PosOperation.PURCHASE -> Icons.Default.ShoppingBag
                                                        PosOperation.SALE_RETURN -> Icons.Default.AssignmentReturn
                                                        PosOperation.PURCHASE_RETURN -> Icons.Default.RemoveShoppingCart
                                                        PosOperation.RECEIPT -> Icons.Default.ArrowDownward
                                                        PosOperation.EXPENSE -> Icons.Default.ArrowUpward
                                                    },
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp),
                                                    tint = when (op) {
                                                        PosOperation.SALE -> Color(0xFF1B5E20)
                                                        PosOperation.PURCHASE -> Color(0xFF1976D2)
                                                        PosOperation.SALE_RETURN, PosOperation.PURCHASE_RETURN -> Color(0xFFD32F2F)
                                                        PosOperation.RECEIPT -> Color(0xFF00796B)
                                                        PosOperation.EXPENSE -> Color(0xFFE65100)
                                                    }
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    op.titleArabic,
                                                    fontWeight = if (uiState.activeOperation == op) FontWeight.Bold else FontWeight.Normal,
                                                    fontSize = 13.sp
                                                )
                                            }
                                        },
                                        onClick = {
                                            viewModel.selectOperation(op)
                                            opMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // أزرار وصول سريع ملائمة: فاتورة جديدة والسجلات
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // زر فاتورة جديدة السريع
                            Button(
                                onClick = { viewModel.startNewInvoice() },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20))
                            ) {
                                Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("فاتورة جديدة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            // زر ملخص وتقارير الشفت
                            IconButton(
                                onClick = { showShiftSummaryModal = true },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    Icons.Default.Analytics,
                                    contentDescription = "ملخص وتقارير الشفت",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            IconButton(
                                onClick = { viewModel.toggleHistoryDialog(true) },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Default.ReceiptLong, contentDescription = "سجل الحركات", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    // نافذة ملخص الشفت والتقرير
                    if (showShiftSummaryModal) {
                        PosShiftSummaryModal(
                            uiState = uiState,
                            onCloseShift = { viewModel.openShiftCloseDialog() },
                            onOpenHistory = { viewModel.toggleHistoryDialog(true) },
                            onDismiss = { showShiftSummaryModal = false }
                        )
                    }

                    // 2. كرت الشفت مثبت في مكانه الصحيح (بين قسم الفواتير والسندات بالأعلى، وبين قسم الأصناف والسلة بالأسفل)
                    ShiftStatusBar(
                        currentShift = uiState.currentShift,
                        cashInDrawer = uiState.cashInDrawer,
                        financialAccounts = uiState.financialAccounts,
                        currencySymbol = uiState.currencySymbol,
                        onOpenCashBreakdown = { showCashBreakdownDialog = true },
                        onOpenBankBreakdown = { showBankBreakdownDialog = true },
                        onOpenShiftCloseDialog = { viewModel.openShiftCloseDialog() },
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    // 3. جسم الشاشة الرئيسي: يستهلك المساحة المتاحة بالكامل دون قص أو اختفاء
                    if (uiState.activeOperation.isVoucher) {
                        // وضع السندات المالية (قبض / صرف)
                        PosVoucherSection(
                            uiState = uiState,
                            viewModel = viewModel,
                            onOpenAddPartyDialog = { showAddPartyDialog = true },
                            isCompact = isCompact,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )
                    } else {
                        // وضع الفواتير والمردودات (بيع، شراء، مردود بيع، مردود شراء)
                        PosInvoiceSection(
                            uiState = uiState,
                            viewModel = viewModel,
                            onOpenAddPartyDialog = { showAddPartyDialog = true },
                            isCompact = isCompact,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )
                    }
                }
            }
        }

        // نافذة اختيار فاتورة للمردود والبحث برقم الفاتورة
        if (uiState.showSelectInvoiceForReturnDialog) {
            SelectInvoiceForReturnDialog(
                uiState = uiState,
                onDismiss = { viewModel.dismissSelectInvoiceForReturnDialog() },
                onSearchChange = { viewModel.updateInvoiceSearchQueryForReturn(it) },
                onSelectInvoice = { viewModel.selectInvoiceForReturn(it) }
            )
        }

        // نافذة تعديل الفاتورة لمدير النظام
        if (uiState.showEditInvoiceDialog && uiState.editingInvoice != null) {
            EditInvoiceDialog(
                invoice = uiState.editingInvoice!!,
                initialNotes = uiState.editInvoiceNotes,
                initialPaymentMethod = uiState.editInvoicePaymentMethod,
                onDismiss = { viewModel.dismissEditDialogs() },
                onSave = { notes, method -> viewModel.saveEditedInvoice(notes, method) }
            )
        }

        // نافذة تعديل السند لمدير النظام
        if (uiState.showEditVoucherDialog && uiState.editingVoucherRecord != null) {
            EditVoucherDialog(
                record = uiState.editingVoucherRecord!!,
                initialNotes = uiState.editVoucherNotes,
                initialPaymentMethod = uiState.editVoucherPaymentMethod,
                onDismiss = { viewModel.dismissEditDialogs() },
                onSave = { notes, method -> viewModel.saveEditedVoucher(notes, method) }
            )
        }

        // نافذة إغلاق ومطابقة الشفت في الصندوق
        if (uiState.showShiftCloseDialog) {
            PosShiftCloseDialog(
                uiState = uiState,
                viewModel = viewModel,
                onDismiss = { viewModel.dismissShiftCloseDialog() }
            )
        }

        // نافذة استعراض الفواتير والسندات
        if (uiState.showHistoryDialog) {
            PosHistoryDialog(
                uiState = uiState,
                currentUserRole = currentUserRole,
                viewModel = viewModel,
                onDismiss = { viewModel.toggleHistoryDialog(false) }
            )
        }

        // نافذة معاينة الإيصال بعد إتمام الفاتورة
        if (uiState.showReceiptDialog && uiState.lastCheckoutResult != null) {
            val context = androidx.compose.ui.platform.LocalContext.current
            val scope = androidx.compose.runtime.rememberCoroutineScope()
            PosReceiptDialog(
                checkoutResult = uiState.lastCheckoutResult!!,
                onPrintAgain = {
                    scope.launch {
                        com.example.dokkani.domain.hardware.BluetoothPrinterManager(context)
                            .printReceipt(uiState.lastCheckoutResult!!.receiptData)
                    }
                },
                onDismiss = { viewModel.startNewInvoice() }
            )
        }

        // نافذة إضافة عميل أو مورد جديد سريعاً
        if (showAddPartyDialog) {
            val isForCustomer = uiState.activeOperation in listOf(PosOperation.SALE, PosOperation.SALE_RETURN, PosOperation.RECEIPT)
            AlertDialog(
                onDismissRequest = { showAddPartyDialog = false },
                title = {
                    Text(
                        if (isForCustomer) "إضافة عميل جديد للدفتر" else "إضافة مورد جديد",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newPartyName,
                            onValueChange = { newPartyName = it },
                            label = { Text(if (isForCustomer) "اسم العميل" else "اسم المورد / الشركة") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = newPartyPhone,
                            onValueChange = { newPartyPhone = it },
                            label = { Text("رقم الجوال") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true
                        )
                        if (isForCustomer) {
                            OutlinedTextField(
                                value = newPartyLimit,
                                onValueChange = { newPartyLimit = it },
                                label = { Text("سقف الدين المسموح به (${uiState.currencySymbol})") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newPartyName.isNotBlank()) {
                                val type = if (isForCustomer) PartyType.CUSTOMER else PartyType.SUPPLIER
                                val limit = newPartyLimit.toDoubleOrNull() ?: 1000.0
                                viewModel.addNewParty(newPartyName, newPartyPhone, type, limit)
                                newPartyName = ""
                                newPartyPhone = ""
                                showAddPartyDialog = false
                            }
                        }
                    ) {
                        Text("حفظ")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddPartyDialog = false }) {
                        Text("إلغاء")
                    }
                }
            )
        }

        // نافذة تفاصيل عهدة الصندوق والدرج الكاش
        if (showCashBreakdownDialog) {
            CashDrawerBreakdownDialog(
                shift = uiState.currentShift,
                currencySymbol = uiState.currencySymbol,
                onDismiss = { showCashBreakdownDialog = false },
                onOpenShiftClose = { viewModel.openShiftCloseDialog() },
                onRefresh = { viewModel.refreshData() }
            )
        }

        // نافذة تفاصيل حركة البنوك والمحافظ والشبكات
        if (showBankBreakdownDialog) {
            BankWalletsBreakdownDialog(
                shift = uiState.currentShift,
                financialAccounts = uiState.financialAccounts,
                currencySymbol = uiState.currencySymbol,
                onDismiss = { showBankBreakdownDialog = false },
                onRefresh = { viewModel.refreshData() }
            )
        }
    }
}
}

/**
 * نافذة حوارية لعرض تفاصيل ومبيعات الشفت وصندوق الكاشير
 */
@Composable
private fun PosShiftSummaryModal(
    uiState: PosUiState,
    onCloseShift: () -> Unit,
    onOpenHistory: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("تفاصيل الشفت الحالي والمبيعات", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // معلومات الكاشير والشفت
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("رقم الشفت:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(uiState.currentShift?.shiftNumber ?: "SHF-1", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("الكاشير الحالي:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(uiState.currentShift?.cashierName ?: "مدير النظام", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("حالة الشفت:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("مفتوح (نشط)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                        }
                    }
                }

                // إجمالي المبيعات والنقدية بالدرج
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B5E20)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("إجمالي مبيعات الشفت", color = Color(0xFFC8E6C9), fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "%.2f %s".format(uiState.shiftTotalSales, uiState.currencySymbol),
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF004D40)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("النقدية بالدرج", color = Color(0xFFB2DFDB), fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "%.2f %s".format(uiState.cashInDrawer, uiState.currencySymbol),
                                color = Color(0xFFFFD54F),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                    onCloseShift()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("إغلاق ومطابقة الشفت", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onOpenHistory()
                    }
                ) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("السجل")
                }
                TextButton(onClick = onDismiss) {
                    Text("إغلاق")
                }
            }
        }
    )
}

/**
 * لوحة الأصناف والمنتجات مع البحث والتصنيفات وعرض الشبكة المتجاوب
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PosProductsPanel(
    uiState: PosUiState,
    viewModel: PosViewModel,
    isCompact: Boolean,
    onSwitchToCart: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxHeight(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isCompact) 8.dp else 10.dp)
        ) {
            // شريط البحث والباركود بالكاميرا
            BarcodeTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = "ابحث بالاسم أو امسح الباركود...",
                label = "البحث أو مسح الباركود",
                onBarcodeScanned = { scannedCode ->
                    viewModel.setSearchQuery(scannedCode)
                    val matchedProduct = uiState.productsWithUnits.find { prod ->
                        prod.units.any { it.barcode.trim().equals(scannedCode.trim(), ignoreCase = true) }
                    }
                    if (matchedProduct != null) {
                        val matchedUnit = matchedProduct.units.find { it.barcode.trim().equals(scannedCode.trim(), ignoreCase = true) }
                            ?: matchedProduct.units.firstOrNull()
                        if (matchedUnit != null) {
                            viewModel.addToCart(matchedProduct.product, matchedUnit, 1.0)
                        }
                    }
                },
                singleLine = true
            )

            // شريط ربط المردود بفاتورة سابقة والبحث برقم الفاتورة
            val isReturnOp = uiState.activeOperation in listOf(PosOperation.SALE_RETURN, PosOperation.PURCHASE_RETURN)
            if (isReturnOp) {
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (uiState.originalInvoiceForReturn != null) Color(0xFFE8F5E9) else Color(0xFFFFF8E1)
                    ),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            if (uiState.originalInvoiceForReturn == null) {
                                Text(
                                    text = "اختيار فاتورة سابقة للمردود (بحث برقم الفاتورة):",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = Color(0xFFE65100)
                                )
                                Text(
                                    text = "اختر الفاتورة الأصلية لتحميل الأصناف والأسعار المعتمدة آلياً",
                                    fontSize = 10.sp,
                                    color = Color.DarkGray
                                )
                            } else {
                                Text(
                                    text = "مرتبط بالفاتورة الأصلية: ${uiState.originalInvoiceForReturn.invoiceNumber}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFF1B5E20)
                                )
                                Text(
                                    text = "الإجمالي: %.2f %s | طريقة الدفع: %s".format(
                                        uiState.originalInvoiceForReturn.total,
                                        uiState.currencySymbol,
                                        uiState.originalInvoiceForReturn.paymentMethod.labelArabic
                                    ),
                                    fontSize = 10.sp,
                                    color = Color.DarkGray
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (uiState.originalInvoiceForReturn != null) {
                                IconButton(
                                    onClick = { viewModel.clearOriginalInvoiceForReturn() },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Clear, contentDescription = "إلغاء الربط", tint = Color.Red, modifier = Modifier.size(18.dp))
                                }
                            }
                            Button(
                                onClick = { viewModel.openSelectInvoiceForReturnDialog() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (uiState.originalInvoiceForReturn != null) Color(0xFF2E7D32) else Color(0xFFE65100)
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (uiState.originalInvoiceForReturn == null) "بحث برقم الفاتورة" else "تغيير", fontSize = 10.sp)
                            }
                        }
                    }
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

            // شبكة الأصناف المتجاوبة مع حماية التباين WCAG AAA
            val filtered = uiState.productsWithUnits.filter { p ->
                val query = uiState.searchQuery.trim().lowercase()
                val matchesQuery = query.isEmpty() ||
                        p.product.name.lowercase().contains(query) ||
                        p.product.code.lowercase().contains(query) ||
                        p.units.any { it.barcode.lowercase().contains(query) }
                val matchesCat = uiState.selectedCategory == "الكل" || p.product.category.trim() == uiState.selectedCategory.trim()
                matchesQuery && matchesCat
            }.sortProducts(uiState.sortOption, uiState.productStockMap)

            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(40.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("لا توجد أصناف مطابقة", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 135.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    items(filtered) { pw ->
                        val baseUnit = pw.units.firstOrNull { it.isBaseUnit } ?: pw.units.firstOrNull()
                        val displayPrice = if (uiState.activeOperation in listOf(PosOperation.PURCHASE, PosOperation.PURCHASE_RETURN)) {
                            baseUnit?.costPrice ?: 0.0
                        } else {
                            baseUnit?.sellingPrice ?: 0.0
                        }

                        val currentStock = uiState.productStockMap[pw.product.id] ?: 0.0
                        val allowNegative = uiState.systemSettings?.enableNegativeStock ?: false

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(128.dp)
                                .clickable {
                                    if (baseUnit != null) {
                                        viewModel.addToCart(pw.product, baseUnit, 1.0)
                                    }
                                },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = pw.product.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        // شارة الكمية عالية التباين للوضع الداكن (WCAG AAA)
                                        Surface(
                                            color = when {
                                                currentStock > 0 -> Color(0xFF1B5E20)
                                                allowNegative -> Color(0xFFE65100)
                                                else -> Color(0xFFB71C1C)
                                            },
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = when {
                                                    currentStock > 0 -> "متوفر: %.1f".format(currentStock)
                                                    allowNegative -> "سالب: %.1f".format(currentStock)
                                                    else -> "نفد (0)"
                                                },
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = pw.product.category,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "%.2f %s".format(displayPrice, uiState.currencySymbol),
                                        color = when (uiState.activeOperation) {
                                            PosOperation.PURCHASE -> Color(0xFF64B5F6)
                                            PosOperation.SALE_RETURN, PosOperation.PURCHASE_RETURN -> Color(0xFFFF8A80)
                                            else -> Color(0xFF81C784)
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )

                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = baseUnit?.unitName ?: "حبة",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // شريط التنقل السريع للسلة للهواتف
            if (isCompact && uiState.cartItems.isNotEmpty() && onSwitchToCart != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = Color(0xFF1B5E20),
                    shape = RoundedCornerShape(10.dp),
                    shadowElevation = 3.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSwitchToCart() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "السلة: ${uiState.cartSummary.itemsCount} صنف (%.2f %s)".format(uiState.cartSummary.finalTotal, uiState.currencySymbol),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("الدفع", color = Color(0xFFFFD54F), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color(0xFFFFD54F), modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * لوحة سلة المشتريات/المبيعات وإتمام العملية بتصميم سلس وقابل للسحب بالكامل
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PosCartPanel(
    uiState: PosUiState,
    viewModel: PosViewModel,
    onOpenAddPartyDialog: () -> Unit,
    isCompact: Boolean,
    onSwitchToCatalog: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxHeight(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isCompact) 10.dp else 12.dp)
        ) {
            // ترويسة السلة مع اختيار الطرف (عميل / مورد)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isCompact && onSwitchToCatalog != null) {
                        IconButton(onClick = onSwitchToCatalog, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "الأصناف", modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Column {
                        Text(
                            text = when (uiState.activeOperation) {
                                PosOperation.SALE -> "سلة فاتورة البيع"
                                PosOperation.PURCHASE -> "سلة فاتورة الشراء"
                                PosOperation.SALE_RETURN -> "سلة مردود البيع"
                                PosOperation.PURCHASE_RETURN -> "سلة مردود الشراء"
                                else -> "سلة الفاتورة"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${uiState.cartSummary.itemsCount} صنف (${uiState.cartSummary.totalQuantity.toInt()} قطعة)",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = { viewModel.startNewInvoice() },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.AddShoppingCart, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("فاتورة جديدة", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    if (uiState.cartItems.isNotEmpty()) {
                        TextButton(onClick = { viewModel.clearCart() }, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تفريغ", color = Color.Red, fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // اختيار الطرف (العميل في البيع، المورد في الشراء)
            val isCustomerMode = uiState.activeOperation in listOf(PosOperation.SALE, PosOperation.SALE_RETURN)
            val partyLabel = if (isCustomerMode) "العميل: " else "المورد: "

            var partyDropdownExpanded by remember { mutableStateOf(false) }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { partyDropdownExpanded = true },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (isCustomerMode) Icons.Default.Person else Icons.Default.LocalShipping,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = partyLabel + (uiState.selectedParty?.name ?: if (isCustomerMode) "عميل نقدي (كاش)" else "اختر المورد"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        uiState.selectedParty?.let { p ->
                            val balText = if (p.currentBalance > 0) {
                                "مدين لنا: %.2f %s".format(p.currentBalance, uiState.currencySymbol)
                            } else if (p.currentBalance < 0) {
                                "دائن له: %.2f %s".format(-p.currentBalance, uiState.currencySymbol)
                            } else {
                                "الرصيد: 0.00 %s".format(uiState.currencySymbol)
                            }
                            Text(balText, fontSize = 10.sp, color = if (p.currentBalance > 0) Color(0xFFD32F2F) else Color(0xFF2E7D32))
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onOpenAddPartyDialog, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "إضافة", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }

                    IconButton(onClick = { partyDropdownExpanded = true }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                }

                // قائمة منسدلة لاختيار الطرف
                DropdownMenu(
                    expanded = partyDropdownExpanded,
                    onDismissRequest = { partyDropdownExpanded = false }
                ) {
                    if (isCustomerMode) {
                        DropdownMenuItem(
                            text = { Text("عميل نقدي (بدون حساب)") },
                            onClick = {
                                viewModel.selectParty(null)
                                partyDropdownExpanded = false
                            }
                        )
                    }

                    val eligibleParties = uiState.parties.filter {
                        if (isCustomerMode) it.type == PartyType.CUSTOMER || it.type == PartyType.BOTH
                        else it.type == PartyType.SUPPLIER || it.type == PartyType.BOTH
                    }

                    eligibleParties.forEach { party ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(party.name, fontWeight = FontWeight.Bold)
                                    Text(
                                        if (party.currentBalance > 0) "مدين: %.2f %s".format(party.currentBalance, uiState.currencySymbol)
                                        else if (party.currentBalance < 0) "دائن: %.2f %s".format(-party.currentBalance, uiState.currencySymbol)
                                        else "رصيد صفر",
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                }
                            },
                            onClick = {
                                viewModel.selectParty(party)
                                partyDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 6.dp))

            // قائمة عناصر السلة مع سحب سلس
            if (uiState.cartItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(40.dp), tint = Color.LightGray)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("السلة فارغة", color = Color.Gray, fontSize = 13.sp)
                        Text("اضغط على أي صنف لإضافته", color = Color.LightGray, fontSize = 11.sp)
                        if (isCompact && onSwitchToCatalog != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = onSwitchToCatalog,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("تصفح قائمة الأصناف", fontSize = 12.sp)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(uiState.cartItems, key = { it.cartItemId }) { item ->
                        PosCartItemRow(
                            item = item,
                            currencySymbol = uiState.currencySymbol,
                            showDecimals = uiState.showDecimals,
                            onQuantityChange = { newQty -> viewModel.updateCartQuantity(item.cartItemId, newQty) },
                            onUnitPriceChange = { newPrice -> viewModel.updateCartItemPrice(item.cartItemId, newPrice) },
                            onUnitChange = { newUnit -> viewModel.changeCartItemUnit(item.cartItemId, newUnit) },
                            onNoteChange = { note -> viewModel.updateCartItemNote(item.cartItemId, note) },
                            onRemove = { viewModel.removeCartItem(item.cartItemId) }
                        )
                    }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 6.dp))

            // طريقة السداد وإدارة الحسابات البنكية والمحافظ
            PaymentMethodSelector(
                selectedMethod = uiState.paymentMethod,
                onMethodSelected = { viewModel.setPaymentMethod(it) },
                financialAccounts = uiState.financialAccounts,
                selectedAccountId = uiState.selectedPaymentAccountId,
                onAccountSelected = { viewModel.setSelectedPaymentAccount(it) },
                transactionRef = uiState.paymentTransactionRef,
                onTransactionRefChange = { viewModel.setPaymentTransactionRef(it) },
                currencySymbol = uiState.currencySymbol
            )

            Spacer(modifier = Modifier.height(4.dp))

            // تفاصيل الإجمالي
            if (uiState.cartSummary.taxRatePercent > 0.0) {
                val rateStr = if (uiState.cartSummary.taxRatePercent % 1.0 == 0.0) "${uiState.cartSummary.taxRatePercent.toInt()}%" else "%.1f%%".format(uiState.cartSummary.taxRatePercent)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("المجموع قبل الضريبة:", fontSize = 11.sp, color = Color.Gray)
                    Text("%.2f %s".format(uiState.cartSummary.taxableAmount, uiState.currencySymbol), fontSize = 11.sp)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("الضريبة المضافة ($rateStr):", fontSize = 11.sp, color = Color.Gray)
                    Text("%.2f %s".format(uiState.cartSummary.taxAmount, uiState.currencySymbol), fontSize = 11.sp)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("المبلغ الإجمالي الصافي:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(
                    "%.2f %s".format(uiState.cartSummary.finalTotal, uiState.currencySymbol),
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = when (uiState.activeOperation) {
                        PosOperation.PURCHASE -> Color(0xFF1976D2)
                        PosOperation.SALE_RETURN, PosOperation.PURCHASE_RETURN -> Color(0xFFD32F2F)
                        else -> Color(0xFF1B5E20)
                    }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Button(
                onClick = { viewModel.executeInvoiceTransaction() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when (uiState.activeOperation) {
                        PosOperation.PURCHASE -> Color(0xFF1976D2)
                        PosOperation.SALE_RETURN, PosOperation.PURCHASE_RETURN -> Color(0xFFD32F2F)
                        else -> Color(0xFF1B5E20)
                    }
                ),
                enabled = uiState.cartItems.isNotEmpty() && !uiState.isProcessingCheckout
            ) {
                if (uiState.isProcessingCheckout) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                } else {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (uiState.activeOperation) {
                            PosOperation.PURCHASE -> "اعتماد وحفظ فاتورة الشراء"
                            PosOperation.SALE_RETURN -> "اعتماد وحفظ مردود البيع"
                            PosOperation.PURCHASE_RETURN -> "اعتماد وحفظ مردود الشراء"
                            else -> "إتمام عملية البيع وطباعة الإيصال"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

/**
 * قسم الفواتير والمردودات: الأصناف + السلة + أطراف التعامل
 */
/**
 * قسم الفواتير والمردودات: الأصناف + السلة + أطراف التعامل
 * متجاوب بالكامل: شاشات التابليت والكمبيوتر (عرض مزدوج) وشاشات الهواتف (تبويب وسحب سلس لأسفل)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PosInvoiceSection(
    uiState: PosUiState,
    viewModel: PosViewModel,
    onOpenAddPartyDialog: () -> Unit,
    isCompact: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (!isCompact) {
        // الشاشات الكبيرة والتابليت: الأصناف على اليمين والسلة على اليسار جنباً إلى جنب
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PosProductsPanel(
                uiState = uiState,
                viewModel = viewModel,
                isCompact = false,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )

            PosCartPanel(
                uiState = uiState,
                viewModel = viewModel,
                onOpenAddPartyDialog = onOpenAddPartyDialog,
                isCompact = false,
                modifier = Modifier
                    .weight(1.15f)
                    .fillMaxHeight()
            )
        }
    } else {
        // الشاشات الصغيرة والهواتف: تبويب علوي وسحب رأسي لأسفل دون تراكم أو تداخل
        Column(modifier = modifier.fillMaxSize()) {
            TabRow(
                selectedTabIndex = uiState.activeMobileTab.ordinal,
                containerColor = Color.White,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
            ) {
                Tab(
                    selected = uiState.activeMobileTab == PosMobileTab.CATALOG,
                    onClick = { viewModel.setActiveMobileTab(PosMobileTab.CATALOG) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("قائمة الأصناف", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                )

                Tab(
                    selected = uiState.activeMobileTab == PosMobileTab.CART,
                    onClick = { viewModel.setActiveMobileTab(PosMobileTab.CART) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BadgedBox(
                                badge = {
                                    if (uiState.cartItems.isNotEmpty()) {
                                        Badge(
                                            containerColor = Color(0xFF1B5E20),
                                            contentColor = Color.White
                                        ) {
                                            Text(uiState.cartSummary.itemsCount.toString())
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (uiState.cartItems.isNotEmpty()) {
                                    "السلة (%.2f %s)".format(uiState.cartSummary.finalTotal, uiState.currencySymbol)
                                } else {
                                    "السلة"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            when (uiState.activeMobileTab) {
                PosMobileTab.CATALOG -> {
                    PosProductsPanel(
                        uiState = uiState,
                        viewModel = viewModel,
                        isCompact = true,
                        onSwitchToCart = { viewModel.setActiveMobileTab(PosMobileTab.CART) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
                PosMobileTab.CART -> {
                    PosCartPanel(
                        uiState = uiState,
                        viewModel = viewModel,
                        onOpenAddPartyDialog = onOpenAddPartyDialog,
                        isCompact = true,
                        onSwitchToCatalog = { viewModel.setActiveMobileTab(PosMobileTab.CATALOG) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
            }
        }
    }
}

@Suppress("unused")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PosInvoiceSectionOld(
    uiState: PosUiState,
    viewModel: PosViewModel,
    onOpenAddPartyDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // قسم المنتجات (يمين)
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
                    .padding(10.dp)
            ) {
                // شريط البحث والباركود بالكاميرا
                BarcodeTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "ابحث بالاسم أو امسح الباركود...",
                    label = "البحث أو مسح الباركود",
                    onBarcodeScanned = { scannedCode ->
                        viewModel.setSearchQuery(scannedCode)
                        val matchedProduct = uiState.productsWithUnits.find { prod ->
                            prod.units.any { it.barcode.trim().equals(scannedCode.trim(), ignoreCase = true) }
                        }
                        if (matchedProduct != null) {
                            val matchedUnit = matchedProduct.units.find { it.barcode.trim().equals(scannedCode.trim(), ignoreCase = true) }
                                ?: matchedProduct.units.firstOrNull()
                            if (matchedUnit != null) {
                                viewModel.addToCart(matchedProduct.product, matchedUnit, 1.0)
                            }
                        }
                    },
                    singleLine = true
                )

                // شريط ربط المردود بفاتورة سابقة والبحث برقم الفاتورة
                val isReturnOp = uiState.activeOperation in listOf(PosOperation.SALE_RETURN, PosOperation.PURCHASE_RETURN)
                if (isReturnOp) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = if (uiState.originalInvoiceForReturn != null) Color(0xFFE8F5E9) else Color(0xFFFFF8E1)),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                if (uiState.originalInvoiceForReturn == null) {
                                    Text(
                                        text = "اختيار فاتورة سابقة للمردود (بحث برقم الفاتورة):",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = Color(0xFFE65100)
                                    )
                                    Text(
                                        text = "اختر الفاتورة الأصلية لتحميل الأصناف والأسعار المعتمدة آلياً",
                                        fontSize = 10.sp,
                                        color = Color.DarkGray
                                    )
                                } else {
                                    Text(
                                        text = "مرتبط بالفاتورة الأصلية: ${uiState.originalInvoiceForReturn.invoiceNumber}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color(0xFF1B5E20)
                                    )
                                    Text(
                                        text = "الإجمالي: %.2f %s | طريقة الدفع: %s".format(
                                            uiState.originalInvoiceForReturn.total,
                                            uiState.currencySymbol,
                                            uiState.originalInvoiceForReturn.paymentMethod.labelArabic
                                        ),
                                        fontSize = 10.sp,
                                        color = Color.DarkGray
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (uiState.originalInvoiceForReturn != null) {
                                    IconButton(
                                        onClick = { viewModel.clearOriginalInvoiceForReturn() },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Clear, contentDescription = "إلغاء الربط", tint = Color.Red, modifier = Modifier.size(18.dp))
                                    }
                                }
                                Button(
                                    onClick = { viewModel.openSelectInvoiceForReturnDialog() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (uiState.originalInvoiceForReturn != null) Color(0xFF2E7D32) else Color(0xFFE65100)
                                    ),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (uiState.originalInvoiceForReturn == null) "بحث برقم الفاتورة" else "تغيير", fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // تصنيفات سريعة
                val categories = uiState.categories.ifEmpty { listOf("الكل") }
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = uiState.selectedCategory == cat,
                            onClick = { viewModel.setSelectedCategory(cat) },
                            label = { Text(cat, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // أداة الفرز والترتيب أفقياً بين التصنيفات وشبكة الأصناف
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

                // شبكة الأصناف
                val filtered = uiState.productsWithUnits.filter { p ->
                    val query = uiState.searchQuery.trim().lowercase()
                    val matchesQuery = query.isEmpty() ||
                            p.product.name.lowercase().contains(query) ||
                            p.product.code.lowercase().contains(query) ||
                            p.units.any { it.barcode.lowercase().contains(query) }
                    val matchesCat = uiState.selectedCategory == "الكل" || p.product.category.trim() == uiState.selectedCategory.trim()
                    matchesQuery && matchesCat
                }.sortProducts(uiState.sortOption, uiState.productStockMap)

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filtered) { pw ->
                        val baseUnit = pw.units.firstOrNull { it.isBaseUnit } ?: pw.units.firstOrNull()
                        val displayPrice = if (uiState.activeOperation in listOf(PosOperation.PURCHASE, PosOperation.PURCHASE_RETURN)) {
                            baseUnit?.costPrice ?: 0.0
                        } else {
                            baseUnit?.sellingPrice ?: 0.0
                        }

                        val currentStock = uiState.productStockMap[pw.product.id] ?: 0.0
                        val allowNegative = uiState.systemSettings?.enableNegativeStock ?: false
                        val isStockZero = currentStock <= 0.0

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(125.dp)
                                .clickable {
                                    if (baseUnit != null) {
                                        viewModel.addToCart(pw.product, baseUnit, 1.0)
                                    }
                                },
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = pw.product.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Surface(
                                            color = when {
                                                currentStock > 0 -> Color(0xFFE8F5E9)
                                                allowNegative -> Color(0xFFFFF3E0)
                                                else -> Color(0xFFFFEBEE)
                                            },
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = when {
                                                    currentStock > 0 -> "متوفر: %.1f".format(currentStock)
                                                    allowNegative -> "سالب: %.1f".format(currentStock)
                                                    else -> "نفد (0)"
                                                },
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = when {
                                                    currentStock > 0 -> Color(0xFF2E7D32)
                                                    allowNegative -> Color(0xFFE65100)
                                                    else -> Color(0xFFC62828)
                                                },
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = pw.product.category,
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "%.2f %s".format(displayPrice, uiState.currencySymbol),
                                        color = when (uiState.activeOperation) {
                                            PosOperation.PURCHASE -> Color(0xFF1976D2)
                                            PosOperation.SALE_RETURN, PosOperation.PURCHASE_RETURN -> Color(0xFFD32F2F)
                                            else -> Color(0xFF1B5E20)
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )

                                    Surface(
                                        color = Color(0xFFE8F5E9),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = baseUnit?.unitName ?: "حبة",
                                            fontSize = 10.sp,
                                            color = Color(0xFF2E7D32),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // قسم السلة وإتمام العملية (يسار)
        Card(
            modifier = Modifier
                .weight(1.15f)
                .fillMaxHeight(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                // ترويسة السلة مع اختيار الطرف (عميل / مورد)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = when (uiState.activeOperation) {
                                PosOperation.SALE -> "سلة فاتورة البيع"
                                PosOperation.PURCHASE -> "سلة فاتورة الشراء"
                                PosOperation.SALE_RETURN -> "سلة مردود البيع"
                                PosOperation.PURCHASE_RETURN -> "سلة مردود الشراء"
                                else -> "سلة الفاتورة"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "${uiState.cartSummary.itemsCount} صنف (${uiState.cartSummary.totalQuantity} كمية)",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }

                    if (uiState.cartItems.isNotEmpty()) {
                        TextButton(onClick = { viewModel.clearCart() }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تفريغ", color = Color.Red, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // اختيار الطرف (العميل في البيع، المورد في الشراء)
                val isCustomerMode = uiState.activeOperation in listOf(PosOperation.SALE, PosOperation.SALE_RETURN)
                val partyLabel = if (isCustomerMode) "العميل: " else "المورد: "

                var partyDropdownExpanded by remember { mutableStateOf(false) }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF0F4F8))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { partyDropdownExpanded = true },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (isCustomerMode) Icons.Default.Person else Icons.Default.LocalShipping,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = partyLabel + (uiState.selectedParty?.name ?: if (isCustomerMode) "عميل نقدي (كاش)" else "اختر المورد"),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            uiState.selectedParty?.let { p ->
                                val balText = if (p.currentBalance > 0) {
                                    "مدين لنا: %.2f %s".format(p.currentBalance, uiState.currencySymbol)
                                } else if (p.currentBalance < 0) {
                                    "دائن له: %.2f %s".format(-p.currentBalance, uiState.currencySymbol)
                                } else {
                                    "الرصيد: 0.00 %s".format(uiState.currencySymbol)
                                }
                                Text(balText, fontSize = 10.sp, color = if (p.currentBalance > 0) Color(0xFFD32F2F) else Color(0xFF2E7D32))
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onOpenAddPartyDialog, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.PersonAdd, contentDescription = "إضافة", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }

                        IconButton(onClick = { partyDropdownExpanded = true }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }

                    // قائمة منسدلة لاختيار الطرف
                    DropdownMenu(
                        expanded = partyDropdownExpanded,
                        onDismissRequest = { partyDropdownExpanded = false }
                    ) {
                        if (isCustomerMode) {
                            DropdownMenuItem(
                                text = { Text("عميل نقدي (بدون حساب)") },
                                onClick = {
                                    viewModel.selectParty(null)
                                    partyDropdownExpanded = false
                                }
                            )
                        }

                        val eligibleParties = uiState.parties.filter {
                            if (isCustomerMode) it.type == PartyType.CUSTOMER || it.type == PartyType.BOTH
                            else it.type == PartyType.SUPPLIER || it.type == PartyType.BOTH
                        }

                        eligibleParties.forEach { party ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(party.name, fontWeight = FontWeight.Bold)
                                        Text(
                                            if (party.currentBalance > 0) "مدين: %.2f %s".format(party.currentBalance, uiState.currencySymbol)
                                            else if (party.currentBalance < 0) "دائن: %.2f %s".format(-party.currentBalance, uiState.currencySymbol)
                                            else "رصيد صفر",
                                            fontSize = 10.sp,
                                            color = Color.Gray
                                        )
                                    }
                                },
                                onClick = {
                                    viewModel.selectParty(party)
                                    partyDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // قائمة عناصر السلة الموسعة
                if (uiState.cartItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(40.dp), tint = Color.LightGray)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("السلة فارغة", color = Color.Gray, fontSize = 13.sp)
                            Text("اضغط على أي صنف لإضافته", color = Color.LightGray, fontSize = 11.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(uiState.cartItems, key = { it.cartItemId }) { item ->
                            PosCartItemRow(
                                item = item,
                                currencySymbol = uiState.currencySymbol,
                                showDecimals = uiState.showDecimals,
                                onQuantityChange = { newQty -> viewModel.updateCartQuantity(item.cartItemId, newQty) },
                                onUnitPriceChange = { newPrice -> viewModel.updateCartItemPrice(item.cartItemId, newPrice) },
                                onUnitChange = { newUnit -> viewModel.changeCartItemUnit(item.cartItemId, newUnit) },
                                onNoteChange = { note -> viewModel.updateCartItemNote(item.cartItemId, note) },
                                onRemove = { viewModel.removeCartItem(item.cartItemId) }
                            )
                        }
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 6.dp))

                // طريقة السداد وإدارة الحسابات البنكية والمحافظ
                PaymentMethodSelector(
                    selectedMethod = uiState.paymentMethod,
                    onMethodSelected = { viewModel.setPaymentMethod(it) },
                    financialAccounts = uiState.financialAccounts,
                    selectedAccountId = uiState.selectedPaymentAccountId,
                    onAccountSelected = { viewModel.setSelectedPaymentAccount(it) },
                    transactionRef = uiState.paymentTransactionRef,
                    onTransactionRefChange = { viewModel.setPaymentTransactionRef(it) },
                    currencySymbol = uiState.currencySymbol
                )

                Spacer(modifier = Modifier.height(4.dp))

                // تفاصيل الإجمالي
                if (uiState.cartSummary.taxRatePercent > 0.0) {
                    val rateStr = if (uiState.cartSummary.taxRatePercent % 1.0 == 0.0) "${uiState.cartSummary.taxRatePercent.toInt()}%" else "%.1f%%".format(uiState.cartSummary.taxRatePercent)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("المجموع قبل الضريبة:", fontSize = 12.sp, color = Color.Gray)
                        Text("%.2f %s".format(uiState.cartSummary.taxableAmount, uiState.currencySymbol), fontSize = 12.sp)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("الضريبة المضافة ($rateStr):", fontSize = 12.sp, color = Color.Gray)
                        Text("%.2f %s".format(uiState.cartSummary.taxAmount, uiState.currencySymbol), fontSize = 12.sp)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("المبلغ الإجمالي الصافي:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        "%.2f %s".format(uiState.cartSummary.finalTotal, uiState.currencySymbol),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = when (uiState.activeOperation) {
                            PosOperation.PURCHASE -> Color(0xFF1976D2)
                            PosOperation.SALE_RETURN, PosOperation.PURCHASE_RETURN -> Color(0xFFD32F2F)
                            else -> Color(0xFF1B5E20)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { viewModel.executeInvoiceTransaction() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when (uiState.activeOperation) {
                            PosOperation.PURCHASE -> Color(0xFF1976D2)
                            PosOperation.SALE_RETURN, PosOperation.PURCHASE_RETURN -> Color(0xFFD32F2F)
                            else -> Color(0xFF1B5E20)
                        }
                    ),
                    enabled = uiState.cartItems.isNotEmpty() && !uiState.isProcessingCheckout
                ) {
                    if (uiState.isProcessingCheckout) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                    } else {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (uiState.activeOperation) {
                                PosOperation.PURCHASE -> "اعتماد وحفظ فاتورة الشراء"
                                PosOperation.SALE_RETURN -> "اعتماد وحفظ مردود البيع"
                                PosOperation.PURCHASE_RETURN -> "اعتماد وحفظ مردود الشراء"
                                else -> "إتمام عملية البيع وطباعة الإيصال"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * قسم السندات المالية (سند القبض وسند الصرف) - تنسيق محاسبي احترافي ودقيق
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PosVoucherSection(
    uiState: PosUiState,
    viewModel: PosViewModel,
    onOpenAddPartyDialog: () -> Unit,
    isCompact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isReceipt = uiState.activeOperation == PosOperation.RECEIPT
    val primaryColor = if (isReceipt) Color(0xFF00796B) else Color(0xFFE65100)
    val parsedAmount = uiState.voucherAmountInput.toDoubleOrNull() ?: 0.0

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isCompact) 10.dp else 16.dp)
                .then(if (isCompact) Modifier.verticalScroll(rememberScrollState()) else Modifier),
            verticalArrangement = if (isCompact) Arrangement.spacedBy(12.dp) else Arrangement.SpaceBetween
        ) {
            Column {
                // 1. ترويسة السند وحالة الصندوق الحالية
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = primaryColor.copy(alpha = 0.12f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    if (isReceipt) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                    contentDescription = null,
                                    tint = primaryColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isReceipt) "سند قبض مالي (Receipt Voucher)" else "سند صرف مالي (Payment Voucher)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = primaryColor
                            )
                            Text(
                                text = if (isReceipt) "استلام نقدية من عميل (يخفض مديونيته ويزيد نقدية الصندوق)" else "صرف نقدية لمورد أو مصروفات عامة (يخصم من نقدية الدرج)",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    // رصيد الصندوق الحالي
                    Surface(
                        color = Color(0xFFF5F5F5),
                        shape = RoundedCornerShape(8.dp),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PointOfSale, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "نقدية الصندوق: %.2f %s".format(uiState.cashInDrawer, uiState.currencySymbol),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B5E20)
                            )
                        }
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                // 2. الحقول الأساسية للسند في صف متوازن
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // الطرف (العميل أو المورد)
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isReceipt) "العميل المستلم منه (الحساب المدين):" else "المورد أو المستفيد:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            TextButton(onClick = onOpenAddPartyDialog, contentPadding = PaddingValues(0.dp)) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(if (isReceipt) "عميل جديد" else "مورد جديد", fontSize = 11.sp)
                            }
                        }

                        var partyMenuExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = partyMenuExpanded,
                            onExpandedChange = { partyMenuExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = uiState.voucherParty?.let { p ->
                                    "${p.name} - ${if (p.currentBalance > 0) "مدين لنا: %.2f".format(p.currentBalance) else if (p.currentBalance < 0) "دائن: %.2f".format(-p.currentBalance) else "خالص"} ${uiState.currencySymbol}"
                                } ?: if (isReceipt) "اختر العميل المسدد..." else "اختر المورد (أو اتركه لمصروف عام)...",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = partyMenuExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )

                            ExposedDropdownMenu(
                                expanded = partyMenuExpanded,
                                onDismissRequest = { partyMenuExpanded = false }
                            ) {
                                if (!isReceipt) {
                                    DropdownMenuItem(
                                        text = { Text("بدون مورد (مصروف تشغيلي عام ونثريات)") },
                                        onClick = {
                                            viewModel.selectVoucherParty(null)
                                            partyMenuExpanded = false
                                        }
                                    )
                                }

                                val parties = uiState.parties.filter {
                                    if (isReceipt) it.type == PartyType.CUSTOMER || it.type == PartyType.BOTH
                                    else it.type == PartyType.SUPPLIER || it.type == PartyType.BOTH
                                }

                                parties.forEach { party ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(party.name, fontWeight = FontWeight.Bold)
                                                Text(
                                                    if (party.currentBalance > 0) "مدين لنا: %.2f %s".format(party.currentBalance, uiState.currencySymbol)
                                                    else if (party.currentBalance < 0) "دائن له: %.2f %s".format(-party.currentBalance, uiState.currencySymbol)
                                                    else "الرصيد: 0.00 %s".format(uiState.currencySymbol),
                                                    fontSize = 11.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                        },
                                        onClick = {
                                            viewModel.selectVoucherParty(party)
                                            partyMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // المبلغ
                    Column(modifier = Modifier.weight(1f)) {
                        Text("مبلغ السند المطلوب قيده:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = uiState.voucherAmountInput,
                            onValueChange = { viewModel.updateVoucherAmount(it) },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("0.00") },
                            suffix = { Text(uiState.currencySymbol, fontWeight = FontWeight.Bold, color = primaryColor) },
                            leadingIcon = { Icon(Icons.Default.Payments, contentDescription = null, tint = primaryColor) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )

                        // أزرار المبالغ السريعة
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(50, 100, 200, 500).forEach { quickAmt ->
                                Surface(
                                    modifier = Modifier.clickable {
                                        val current = uiState.voucherAmountInput.toDoubleOrNull() ?: 0.0
                                        viewModel.updateVoucherAmount((current + quickAmt).toString())
                                    },
                                    color = Color(0xFFECEFF1),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "+$quickAmt",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // تصنيف المصروف أو المدفوع له (إذا لم يكن هناك مورد في سند الصرف)
                if (!isReceipt && uiState.voucherParty == null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("المدفوع له (الشخص أو الجهة المستلمة):", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = uiState.voucherPaidToInput,
                                onValueChange = { viewModel.updateVoucherPaidTo(it) },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("مثال: شركة الكهرباء / عامل الصيانة...") },
                                singleLine = true
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("بند التصنيف المحاسبي:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            val categories = listOf("نثريات ومستلزمات", "كهرباء ومياه", "إيجار", "رواتب وعمالة", "صيانة ومعدات")
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                items(categories) { cat ->
                                    FilterChip(
                                        selected = uiState.voucherCategory == cat,
                                        onClick = { viewModel.updateVoucherCategory(cat) },
                                        label = { Text(cat, fontSize = 10.sp) }
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // طريقة السداد والبيان
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PaymentMethodSelector(
                        selectedMethod = uiState.voucherPaymentMethod,
                        onMethodSelected = { viewModel.updateVoucherPaymentMethod(it) },
                        financialAccounts = uiState.financialAccounts,
                        selectedAccountId = uiState.selectedPaymentAccountId,
                        onAccountSelected = { viewModel.setSelectedPaymentAccount(it) },
                        transactionRef = uiState.paymentTransactionRef,
                        onTransactionRefChange = { viewModel.setPaymentTransactionRef(it) },
                        allowCredit = false,
                        currencySymbol = uiState.currencySymbol
                    )

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("البيان والملاحظات:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = uiState.voucherNotesInput,
                            onValueChange = { viewModel.updateVoucherNotes(it) },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("شرح مختصر لتوثيق السند بالسجلات...") },
                            singleLine = true
                        )
                    }
                }

                // بطاقة المعاينة والأثر المحاسبي
                if (parsedAmount > 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = if (isReceipt) Color(0xFFE0F2F1) else Color(0xFFFFF3E0)),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (isReceipt) "الأثر المحاسبي لسند القبض:" else "الأثر المحاسبي لسند الصرف:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = primaryColor
                                )
                                if (uiState.voucherParty != null) {
                                    val currentBal = uiState.voucherParty.currentBalance
                                    val newBal = if (isReceipt) currentBal - parsedAmount else currentBal + parsedAmount
                                    Text(
                                        text = "رصيد الحساب الحالي: %.2f %s ➔ الرصيد بعد السند: %.2f %s".format(currentBal, uiState.currencySymbol, newBal, uiState.currencySymbol),
                                        fontSize = 11.sp,
                                        color = Color.DarkGray
                                    )
                                }
                                if (uiState.voucherPaymentMethod == PaymentMethod.CASH) {
                                    Text(
                                        text = if (isReceipt) "سيتم إضافة %.2f %s لدرج الكاشير".format(parsedAmount, uiState.currencySymbol)
                                        else "سيتم خصم %.2f %s من درج الكاشير".format(parsedAmount, uiState.currencySymbol),
                                        fontSize = 10.sp,
                                        color = if (isReceipt) Color(0xFF1B5E20) else Color(0xFFC62828),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Text(
                                text = "المبلغ: %.2f %s".format(parsedAmount, uiState.currencySymbol),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = primaryColor
                            )
                        }
                    }
                }
            }

            // زر الحفظ والاعتماد
            Button(
                onClick = { viewModel.executeVoucherTransaction() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                enabled = !uiState.isVoucherSubmitting
            ) {
                if (uiState.isVoucherSubmitting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                } else {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isReceipt) "اعتماد وحفظ سند القبض وتحديث الحساب" else "اعتماد وحفظ سند الصرف والخصم من الصندوق",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

/**
 * شريط وقائمة استعراض فواتير البيع والشراء والسندات أسفل شاشة الـ POS
 * يتضمن إمكانية التعديل والحذف لمدير النظام
 */
@Composable
private fun PosBottomHistorySection(
    uiState: PosUiState,
    viewModel: PosViewModel,
    currentUserRole: UserRole,
    modifier: Modifier = Modifier
) {
    var deleteCandidate by remember { mutableStateOf<PosTransactionRecord?>(null) }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp)) {
            // شريط العنوان مع التوسيع/الطي وعداد الفواتير
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = Color(0xFF1976D2), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "استعراض فواتير وسجلات العمليات الحديثة",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFF1976D2)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = Color(0xFFE3F2FD),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "${uiState.transactionRecords.size} عملية",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1976D2),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.toggleBottomHistoryExpanded() },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            if (uiState.isBottomHistoryExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "تبديل العرض",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (uiState.isBottomHistoryExpanded) {
                Spacer(modifier = Modifier.height(4.dp))

                // أدوات التصفية والبحث
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = uiState.historySearchQuery,
                        onValueChange = { viewModel.setHistorySearchQuery(it) },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        placeholder = { Text("بحث برقم الفاتورة أو الطرف...", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        singleLine = true
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        item {
                            FilterChip(
                                selected = uiState.historyFilter == "ALL",
                                onClick = { viewModel.setHistoryFilter("ALL") },
                                label = { Text("الكل", fontSize = 10.sp) },
                                modifier = Modifier.height(32.dp)
                            )
                        }
                        item {
                            FilterChip(
                                selected = uiState.historyFilter == "SALE",
                                onClick = { viewModel.setHistoryFilter("SALE") },
                                label = { Text("بيع", fontSize = 10.sp) },
                                modifier = Modifier.height(32.dp)
                            )
                        }
                        item {
                            FilterChip(
                                selected = uiState.historyFilter == "PURCHASE",
                                onClick = { viewModel.setHistoryFilter("PURCHASE") },
                                label = { Text("شراء", fontSize = 10.sp) },
                                modifier = Modifier.height(32.dp)
                            )
                        }
                        item {
                            FilterChip(
                                selected = uiState.historyFilter == "RETURNS",
                                onClick = { viewModel.setHistoryFilter("RETURNS") },
                                label = { Text("مردودات", fontSize = 10.sp) },
                                modifier = Modifier.height(32.dp)
                            )
                        }
                        item {
                            FilterChip(
                                selected = uiState.historyFilter == "VOUCHERS",
                                onClick = { viewModel.setHistoryFilter("VOUCHERS") },
                                label = { Text("سندات", fontSize = 10.sp) },
                                modifier = Modifier.height(32.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // قائمة السجلات
                val filtered = uiState.transactionRecords.filter { rec ->
                    val query = uiState.historySearchQuery.trim().lowercase()
                    val matchesQuery = query.isEmpty() ||
                            rec.id.lowercase().contains(query) ||
                            rec.partyName.lowercase().contains(query)

                    val matchesFilter = when (uiState.historyFilter) {
                        "SALE" -> rec.operation == PosOperation.SALE
                        "PURCHASE" -> rec.operation == PosOperation.PURCHASE
                        "RETURNS" -> rec.operation in listOf(PosOperation.SALE_RETURN, PosOperation.PURCHASE_RETURN)
                        "VOUCHERS" -> rec.operation in listOf(PosOperation.RECEIPT, PosOperation.EXPENSE)
                        else -> true
                    }
                    matchesQuery && matchesFilter
                }

                if (filtered.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("لا توجد فواتير أو سندات مطابقة للبحث", color = Color.Gray, fontSize = 11.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 160.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filtered) { record ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
                                border = CardDefaults.outlinedCardBorder()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // رقم الفاتورة والنوع
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1.5f)
                                    ) {
                                        Surface(
                                            color = when (record.operation) {
                                                PosOperation.SALE -> Color(0xFFE8F5E9)
                                                PosOperation.PURCHASE -> Color(0xFFE3F2FD)
                                                PosOperation.SALE_RETURN, PosOperation.PURCHASE_RETURN -> Color(0xFFFFEBEE)
                                                PosOperation.RECEIPT -> Color(0xFFE0F2F1)
                                                PosOperation.EXPENSE -> Color(0xFFFFF3E0)
                                            },
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = record.operation.titleArabic,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = when (record.operation) {
                                                    PosOperation.SALE -> Color(0xFF1B5E20)
                                                    PosOperation.PURCHASE -> Color(0xFF1976D2)
                                                    PosOperation.SALE_RETURN, PosOperation.PURCHASE_RETURN -> Color(0xFFD32F2F)
                                                    PosOperation.RECEIPT -> Color(0xFF00796B)
                                                    PosOperation.EXPENSE -> Color(0xFFE65100)
                                                },
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(record.id, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            Text(record.partyName, fontSize = 10.sp, color = Color.DarkGray)
                                        }
                                    }

                                    // التاريخ وطريقة الدفع
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(record.date, fontSize = 9.sp, color = Color.Gray)
                                        Text(record.paymentMethod.labelArabic, fontSize = 9.sp, color = Color.DarkGray)
                                    }

                                    // المبلغ والأزرار
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.End,
                                        modifier = Modifier.weight(1.2f)
                                    ) {
                                        Text(
                                            text = "%.2f %s".format(record.amount, uiState.currencySymbol),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = when (record.operation) {
                                                PosOperation.SALE, PosOperation.RECEIPT -> Color(0xFF1B5E20)
                                                PosOperation.PURCHASE -> Color(0xFF1976D2)
                                                else -> Color(0xFFD32F2F)
                                            }
                                        )

                                        // أزرار التعديل والحذف لمدير النظام
                                        if (currentUserRole == UserRole.ADMIN) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            IconButton(
                                                onClick = { viewModel.openEditRecordDialog(record) },
                                                modifier = Modifier.size(26.dp)
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = Color(0xFF1976D2), modifier = Modifier.size(16.dp))
                                            }
                                            IconButton(
                                                onClick = { deleteCandidate = record },
                                                modifier = Modifier.size(26.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color(0xFFD32F2F), modifier = Modifier.size(16.dp))
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

    // نافذة تأكيد الحذف مع عكس القيود
    if (deleteCandidate != null) {
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تأكيد حذف العملية المحاسبية", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            },
            text = {
                Column {
                    Text(
                        text = "هل أنت متأكد من حذف العملية (${deleteCandidate!!.id}) بمبلغ %.2f %s؟".format(deleteCandidate!!.amount, uiState.currencySymbol),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "تحذير: سيقوم النظام آلياً بعكس كافة القيود المحاسبية، وحذف حركات المخزون المرتبطة، وإعادة رصيد الطرف وتعديل نقدية الشفت لضمان مطابقة الدفاتر.",
                        color = Color.Red,
                        fontSize = 11.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toDelete = deleteCandidate!!
                        deleteCandidate = null
                        viewModel.deleteTransactionRecord(toDelete)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("نعم، احذف مع عكس القيود")
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
 * نافذة اختيار فاتورة للمردودات والبحث برقم الفاتورة
 */
@Composable
private fun SelectInvoiceForReturnDialog(
    uiState: PosUiState,
    onDismiss: () -> Unit,
    onSearchChange: (String) -> Unit,
    onSelectInvoice: (InvoiceEntity) -> Unit
) {
    val isSaleReturn = uiState.activeOperation == PosOperation.SALE_RETURN
    val title = if (isSaleReturn) "اختيار فاتورة بيع للمردود" else "اختيار فاتورة شراء للمردود"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق")
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = uiState.invoiceSearchQueryForReturn,
                    onValueChange = onSearchChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("ابحث برقم الفاتورة (مثال: INV-2026-0001)...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (uiState.matchingInvoicesForReturn.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("لا توجد فواتير مطابقة للبحث", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(uiState.matchingInvoicesForReturn) { inv ->
                            val partyName = inv.partyId?.let { pId ->
                                uiState.parties.find { it.id == pId }?.name
                            } ?: if (isSaleReturn) "عميل نقدي" else "مورد عام"

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectInvoice(inv) },
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
                                border = CardDefaults.outlinedCardBorder()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(inv.invoiceNumber, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("الطرف: $partyName", fontSize = 11.sp, color = Color.DarkGray)
                                        Text("طريقة الدفع: ${inv.paymentMethod.labelArabic}", fontSize = 10.sp, color = Color.Gray)
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            "%.2f %s".format(inv.total, uiState.currencySymbol),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = Color(0xFF1B5E20)
                                        )
                                        Button(
                                            onClick = { onSelectInvoice(inv) },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text("اختيار وتعبئة", fontSize = 10.sp)
                                        }
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

/**
 * نافذة تعديل الفاتورة لمدير النظام
 */
@Composable
private fun EditInvoiceDialog(
    invoice: InvoiceEntity,
    initialNotes: String,
    initialPaymentMethod: PaymentMethod,
    onDismiss: () -> Unit,
    onSave: (notes: String, method: PaymentMethod) -> Unit
) {
    var notes by remember { mutableStateOf(initialNotes) }
    var selectedMethod by remember { mutableStateOf(initialPaymentMethod) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("تعديل الفاتورة: ${invoice.invoiceNumber}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("طريقة الدفع:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = selectedMethod == PaymentMethod.CASH,
                        onClick = { selectedMethod = PaymentMethod.CASH },
                        label = { Text("نقداً") }
                    )
                    FilterChip(
                        selected = selectedMethod == PaymentMethod.CREDIT,
                        onClick = { selectedMethod = PaymentMethod.CREDIT },
                        label = { Text("آجل / ذمم") }
                    )
                    FilterChip(
                        selected = selectedMethod == PaymentMethod.MADA,
                        onClick = { selectedMethod = PaymentMethod.MADA },
                        label = { Text("مدى") }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text("الملاحظات والبيان:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(notes, selectedMethod) }) {
                Text("حفظ التعديلات")
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
 * نافذة تعديل السند لمدير النظام
 */
@Composable
private fun EditVoucherDialog(
    record: PosTransactionRecord,
    initialNotes: String,
    initialPaymentMethod: PaymentMethod,
    onDismiss: () -> Unit,
    onSave: (notes: String, method: PaymentMethod) -> Unit
) {
    var notes by remember { mutableStateOf(initialNotes) }
    var selectedMethod by remember { mutableStateOf(initialPaymentMethod) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("تعديل السند: ${record.id}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("طريقة السداد:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = selectedMethod == PaymentMethod.CASH,
                        onClick = { selectedMethod = PaymentMethod.CASH },
                        label = { Text("نقداً") }
                    )
                    FilterChip(
                        selected = selectedMethod == PaymentMethod.MADA,
                        onClick = { selectedMethod = PaymentMethod.MADA },
                        label = { Text("مدى") }
                    )
                    FilterChip(
                        selected = selectedMethod == PaymentMethod.BANK_TRANSFER,
                        onClick = { selectedMethod = PaymentMethod.BANK_TRANSFER },
                        label = { Text("تحويل بنكي") }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text("الملاحظات والبيان:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(notes, selectedMethod) }) {
                Text("حفظ التعديلات")
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
 * نافذة استعراض الفواتير والسجلات التاريخية
 */
@Composable
private fun PosHistoryDialog(
    uiState: PosUiState,
    currentUserRole: UserRole,
    viewModel: PosViewModel,
    onDismiss: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf("ALL") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("سجل الحركات والعمليات المالية", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق")
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // تصفية السجلات
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedFilter == "ALL",
                            onClick = { selectedFilter = "ALL" },
                            label = { Text("الكل", fontSize = 11.sp) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedFilter == "SALE",
                            onClick = { selectedFilter = "SALE" },
                            label = { Text("مبيعات", fontSize = 11.sp) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedFilter == "PURCHASE",
                            onClick = { selectedFilter = "PURCHASE" },
                            label = { Text("مشتريات", fontSize = 11.sp) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedFilter == "RETURNS",
                            onClick = { selectedFilter = "RETURNS" },
                            label = { Text("مردودات", fontSize = 11.sp) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedFilter == "VOUCHERS",
                            onClick = { selectedFilter = "VOUCHERS" },
                            label = { Text("سندات", fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val filteredRecords = uiState.transactionRecords.filter { rec ->
                    when (selectedFilter) {
                        "SALE" -> rec.operation == PosOperation.SALE
                        "PURCHASE" -> rec.operation == PosOperation.PURCHASE
                        "RETURNS" -> rec.operation in listOf(PosOperation.SALE_RETURN, PosOperation.PURCHASE_RETURN)
                        "VOUCHERS" -> rec.operation in listOf(PosOperation.RECEIPT, PosOperation.EXPENSE)
                        else -> true
                    }
                }

                if (filteredRecords.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("لا توجد سجلات مسجلة مطابقة للتصفية", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredRecords) { record ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(record.id, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                color = when (record.operation) {
                                                    PosOperation.SALE -> Color(0xFFE8F5E9)
                                                    PosOperation.PURCHASE -> Color(0xFFE3F2FD)
                                                    PosOperation.SALE_RETURN, PosOperation.PURCHASE_RETURN -> Color(0xFFFFEBEE)
                                                    PosOperation.RECEIPT -> Color(0xFFE0F2F1)
                                                    PosOperation.EXPENSE -> Color(0xFFFFF3E0)
                                                },
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    record.operation.titleArabic,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = when (record.operation) {
                                                        PosOperation.SALE -> Color(0xFF1B5E20)
                                                        PosOperation.PURCHASE -> Color(0xFF1976D2)
                                                        PosOperation.SALE_RETURN, PosOperation.PURCHASE_RETURN -> Color(0xFFD32F2F)
                                                        PosOperation.RECEIPT -> Color(0xFF00796B)
                                                        PosOperation.EXPENSE -> Color(0xFFE65100)
                                                    },
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text(record.partyName, fontSize = 11.sp, color = Color.DarkGray)
                                        Text(record.date, fontSize = 10.sp, color = Color.Gray)
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                "%.2f %s".format(record.amount, uiState.currencySymbol),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = when (record.operation) {
                                                    PosOperation.SALE, PosOperation.RECEIPT -> Color(0xFF1B5E20)
                                                    PosOperation.PURCHASE -> Color(0xFF1976D2)
                                                    else -> Color(0xFFD32F2F)
                                                }
                                            )
                                            Text(record.paymentMethod.labelArabic, fontSize = 10.sp, color = Color.Gray)
                                        }

                                        // زر معاينة وطباعة الفاتورة لعمليات البيع والشراء ومردوداتهما
                                        if (record.operation in listOf(PosOperation.SALE, PosOperation.PURCHASE, PosOperation.SALE_RETURN, PosOperation.PURCHASE_RETURN)) {
                                            IconButton(
                                                onClick = {
                                                    onDismiss()
                                                    viewModel.showReceiptForInvoiceNumber(record.id)
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.ReceiptLong,
                                                    contentDescription = "معاينة وطباعة الفاتورة",
                                                    tint = Color(0xFF2E7D32),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }

                                        if (currentUserRole == UserRole.ADMIN) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            IconButton(
                                                onClick = {
                                                    onDismiss()
                                                    viewModel.openEditRecordDialog(record)
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = Color(0xFF1976D2), modifier = Modifier.size(16.dp))
                                            }
                                            IconButton(
                                                onClick = {
                                                    onDismiss()
                                                    viewModel.deleteTransactionRecord(record)
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color(0xFFD32F2F), modifier = Modifier.size(16.dp))
                                            }
                                        }
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
fun PosShiftCloseDialog(
    uiState: PosUiState,
    viewModel: PosViewModel,
    onDismiss: () -> Unit
) {
    val recon = uiState.shiftReconciliation
    val currentShift = uiState.currentShift

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color(0xFFC62828),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("مطابقة النقدية وإغلاق الشفت", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        text = "رقم الشفت: ${currentShift?.shiftNumber ?: "---"}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // بطاقة المعادلة المحاسبية
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1B5E20))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "حركة الصندوق الدفترية:",
                            color = Color(0xFFC8E6C9),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("العهدة الافتتاحية:", color = Color.White, fontSize = 12.sp)
                            Text("${"%.2f".format(recon?.openingCash ?: 0.0)} ${uiState.currencySymbol}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("+ المبيعات النقدية:", color = Color(0xFF86EFAC), fontSize = 12.sp)
                            Text("+${"%.2f".format(recon?.totalCashSales ?: 0.0)} ${uiState.currencySymbol}", color = Color(0xFF86EFAC), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("+ سندات القبض (الديون):", color = Color(0xFF86EFAC), fontSize = 12.sp)
                            Text("+${"%.2f".format(recon?.totalCashCollections ?: 0.0)} ${uiState.currencySymbol}", color = Color(0xFF86EFAC), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("- سندات الصرف (المصروفات):", color = Color(0xFFFCA5A5), fontSize = 12.sp)
                            Text("-${"%.2f".format(recon?.totalCashExpenses ?: 0.0)} ${uiState.currencySymbol}", color = Color(0xFFFCA5A5), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = Color(0xFF2E7D32))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("النقدية المتوقعة بالدرج:", color = Color(0xFFFFD54F), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(
                                "${"%.2f".format(recon?.expectedCashInDrawer ?: 0.0)} ${uiState.currencySymbol}",
                                color = Color(0xFFFFD54F),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                // إدخال الجرد الفعلي باليد
                OutlinedTextField(
                    value = uiState.shiftActualCashInput,
                    onValueChange = { viewModel.updateShiftActualCashInput(it) },
                    label = { Text("النقدية الفعلية المجرودة باليد في الدرج (${uiState.currencySymbol})*") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // نتيجة المطابقة
                recon?.let { r ->
                    val statusColor = when (r.discrepancyType) {
                        com.example.dokkani.domain.cash.CashDiscrepancyType.MATCHED -> Color(0xFF2E7D32)
                        com.example.dokkani.domain.cash.CashDiscrepancyType.SURPLUS -> Color(0xFF1976D2)
                        com.example.dokkani.domain.cash.CashDiscrepancyType.SHORTAGE -> Color(0xFFC62828)
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = statusColor.copy(alpha = 0.1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "حالة الدرج: ${r.discrepancyType.labelArabic}",
                                color = statusColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "الفرق: ${"%.2f".format(r.discrepancy)} ${uiState.currencySymbol}",
                                color = statusColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                // ملاحظات تسليم الشفت
                OutlinedTextField(
                    value = uiState.shiftCloseNotes,
                    onValueChange = { viewModel.updateShiftCloseNotes(it) },
                    label = { Text("ملاحظات تسليم الشفت والكاشير") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { viewModel.confirmCloseShift() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("إغلاق الشفت واعتماد النقدية", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
