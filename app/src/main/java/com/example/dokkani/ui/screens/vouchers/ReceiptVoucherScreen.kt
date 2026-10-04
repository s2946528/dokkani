package com.example.dokkani.ui.screens.vouchers

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.dokkani.data.local.entities.PartyType
import com.example.dokkani.data.local.entities.PaymentVoucherEntity
import com.example.dokkani.data.local.entities.UserRole
import com.example.dokkani.ui.components.AppSearchBar
import com.example.dokkani.ui.components.DirectEditVoucherDialog
import com.example.dokkani.ui.components.PaymentMethodSelector
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptVoucherScreen(
    currentUserRole: UserRole = UserRole.ADMIN,
    viewModel: ReceiptVoucherViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var newCustomerName by remember { mutableStateOf("") }
    var newCustomerPhone by remember { mutableStateOf("") }

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
                // شريط عنوان الواجهة المستقلة - سند قبض
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF2E7D32)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier
                                        .padding(6.dp)
                                        .size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    "واجهة سند قبض (تحصيل مالي / مقبوضات)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color(0xFF1B5E20)
                                )
                                Text(
                                    "إصدار سندات التحصيل المالي المباشر من العملاء والذمم الحرة",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        val cashDrawerBalance = uiState.financialAccounts
                            .filter { it.accountType == com.example.dokkani.data.local.entities.FinancialAccountType.CASH_DRAWER }
                            .sumOf { it.currentBalance }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFE8F5E9),
                            border = BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PointOfSale,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "رصيد الصندوق: %.2f %s".format(cashDrawerBalance, uiState.baseCurrencySymbol),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1B5E20)
                                )
                            }
                        }
                    }
                }

                // كارت تحرير بيانات سند القبض
                ReceiptVoucherCreationCard(
                    uiState = uiState,
                    viewModel = viewModel,
                    onAddNewCustomer = { viewModel.showAddCustomerDialog() }
                )

                // قسم السجل والأرشيف الخاص بسندات القبض
                ReceiptVouchersHistorySection(
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
                title = {
                    Text("تم اعتماد سند القبض بنجاح", fontWeight = FontWeight.Bold)
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "رقم سند القبض المسجل: ${uiState.lastSavedVoucherNumber}",
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
        if (uiState.showAddCustomerDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissAddCustomerDialog() },
                title = {
                    Text("إضافة عميل جديد", fontWeight = FontWeight.Bold)
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newCustomerName,
                            onValueChange = { newCustomerName = it },
                            label = { Text("اسم العميل الكامل *") },
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
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.addQuickCustomer(newCustomerName, newCustomerPhone)
                            newCustomerName = ""
                            newCustomerPhone = ""
                        },
                        enabled = newCustomerName.isNotBlank()
                    ) {
                        Text("حفظ وإضافة")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissAddCustomerDialog() }) {
                        Text("إلغاء")
                    }
                }
            )
        }

        // نافذة معاينة تفاصيل سند القبض
        if (uiState.showDetailsDialog && uiState.selectedVoucherForDetails != null) {
            val v = uiState.selectedVoucherForDetails!!
            val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.ENGLISH)
            val dateStr = dateFormat.format(Date(v.date))

            AlertDialog(
                onDismissRequest = { viewModel.dismissDetailsDialog() },
                title = { Text("تفاصيل سند القبض #${v.voucherNumber}", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("نوع السند: سند قبض", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                            Text("المبلغ: %.2f %s".format(v.amount, uiState.baseCurrencySymbol), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Text("التاريخ والوقت: $dateStr", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("طريقة السداد: ${v.paymentMethod.labelArabic}", fontSize = 12.sp)
                        if (v.transactionRef.isNotBlank()) {
                            Text("رقم المرجع/الحوالة: ${v.transactionRef}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        if (v.notes.isNotBlank()) {
                            Text("البيان والملاحظات: ${v.notes}", fontSize = 12.sp)
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { viewModel.dismissDetailsDialog() }) {
                        Text("إغلاق")
                    }
                }
            )
        }

        // نافذة تعديل سند القبض
        if (uiState.showEditDialog && uiState.selectedVoucherForEdit != null) {
            DirectEditVoucherDialog(
                voucher = uiState.selectedVoucherForEdit!!,
                financialAccounts = uiState.financialAccounts,
                currencySymbol = uiState.baseCurrencySymbol,
                isAdmin = currentUserRole == UserRole.ADMIN,
                onDismiss = { viewModel.dismissEditDialog() },
                onSave = { newAmount, newMethod, newRef, newNotes, newImagePath, newAccountId ->
                    viewModel.saveEditedVoucher(
                        voucher = uiState.selectedVoucherForEdit!!,
                        newAmount = newAmount,
                        newMethod = newMethod,
                        newRef = newRef,
                        newNotes = newNotes,
                        newImagePath = newImagePath,
                        newAccountId = newAccountId
                    )
                }
            )
        }

        // نافذة رمز PIN مدير النظام عند الحذف
        if (uiState.showAdminPinDialog) {
            var pinInput by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { viewModel.dismissAdminPinDialog() },
                icon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                title = { Text("صلاحيات مدير النظام", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("يرجى إدخال رمز PIN لمدير النظام لتأكيد حذف سند القبض:")
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptVoucherCreationCard(
    uiState: ReceiptVoucherUiState,
    viewModel: ReceiptVoucherViewModel,
    onAddNewCustomer: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.3f))
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
                    Icon(
                        Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "تحرير بيانات سند القبض",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFF1B5E20)
                    )
                }

                Button(
                    onClick = onAddNewCustomer,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("عميل جديد", fontSize = 11.sp)
                }
            }

            // اختيار العميل / المسدد
            var customerDropdownExpanded by remember { mutableStateOf(false) }

            ExposedDropdownMenuBox(
                expanded = customerDropdownExpanded,
                onExpandedChange = { customerDropdownExpanded = !customerDropdownExpanded }
            ) {
                OutlinedTextField(
                    value = uiState.selectedCustomer?.name ?: "اختر العميل من القائمة (أو اترك فارغاً للعام)",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("اسم العميل / الطرف المسدد") },
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
                        text = { Text("عميل نقدي عام / بدون حساب", fontWeight = FontWeight.Bold) },
                        onClick = {
                            viewModel.selectCustomer(null)
                            customerDropdownExpanded = false
                        }
                    )
                    uiState.customers.forEach { c ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(c.name, fontWeight = FontWeight.Bold)
                                    Text("الهاتف: ${c.phone} | الرصيد الحالي: %.2f".format(c.currentBalance), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            },
                            onClick = {
                                viewModel.selectCustomer(c)
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
                // مبلغ السند
                OutlinedTextField(
                    value = uiState.amountInput,
                    onValueChange = { viewModel.setAmountInput(it) },
                    label = { Text("مبلغ سند القبض (${uiState.baseCurrencySymbol}) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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

            // طريقة السداد والتحصيل الديناميكية مع إخفاء الحسابات الفارغة تلقائياً
            PaymentMethodSelector(
                selectedMethod = uiState.paymentMethod,
                onMethodSelected = { viewModel.setPaymentMethod(it) },
                financialAccounts = uiState.financialAccounts,
                selectedAccountId = uiState.selectedAccountId,
                onAccountSelected = { viewModel.setSelectedAccountId(it) },
                transactionRef = uiState.transactionRef,
                onTransactionRefChange = { viewModel.setTransactionRef(it) },
                receiptImagePath = uiState.receiptImagePath,
                onReceiptImageChange = { viewModel.setReceiptImagePath(it) },
                allowCredit = false,
                hideEmptyAccounts = true,
                currencySymbol = uiState.baseCurrencySymbol,
                modifier = Modifier.fillMaxWidth()
            )

            // البيان والملاحظات
            OutlinedTextField(
                value = uiState.notesInput,
                onValueChange = { viewModel.setNotesInput(it) },
                label = { Text("البيان / السبب / التفاصيل") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            )

            // زر الاعتماد الحفظ
            Button(
                onClick = { viewModel.executeSubmitReceiptVoucher() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                enabled = !uiState.isProcessing
            ) {
                if (uiState.isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("جاري حفظ وترحيل سند القبض...")
                } else {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("اعتماد وحفظ سند القبض", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun ReceiptVouchersHistorySection(
    uiState: ReceiptVoucherUiState,
    viewModel: ReceiptVoucherViewModel,
    currentUserRole: UserRole,
    modifier: Modifier = Modifier
) {
    val filteredList = remember(uiState.receiptVouchers, uiState.searchQuery) {
        if (uiState.searchQuery.isBlank()) {
            uiState.receiptVouchers
        } else {
            val q = uiState.searchQuery.trim().lowercase()
            uiState.receiptVouchers.filter {
                it.voucherNumber.lowercase().contains(q) ||
                it.notes.lowercase().contains(q) ||
                it.transactionRef.lowercase().contains(q)
            }
        }
    }

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
                    .clickable { viewModel.toggleHistoryExpanded() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "أرشيف وسجل سندات القبض (${filteredList.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Icon(
                    if (uiState.isHistoryExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }

            AnimatedVisibility(visible = uiState.isHistoryExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppSearchBar(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = "ابحث برقم سند القبض أو البيان أو رقم المرجع...",
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (filteredList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "لا توجد سندات قبض مسجلة حتى الآن",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.ENGLISH) }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 320.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            filteredList.forEach { voucher ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = Color(0xFF2E7D32)
                                                ) {
                                                    Text(
                                                        "سند قبض",
                                                        color = Color.White,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    "#${voucher.voucherNumber}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                voucher.notes,
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                "التاريخ: ${dateFormat.format(Date(voucher.date))} | طريقة السداد: ${voucher.paymentMethod.labelArabic}",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                "%.2f %s".format(voucher.amount, uiState.baseCurrencySymbol),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = Color(0xFF1B5E20)
                                            )

                                            IconButton(
                                                onClick = { viewModel.viewVoucherDetails(voucher) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.Visibility, contentDescription = "معاينة", modifier = Modifier.size(16.dp))
                                            }

                                            IconButton(
                                                onClick = { viewModel.requestEditVoucher(voucher) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = "تعديل", modifier = Modifier.size(16.dp))
                                            }

                                            IconButton(
                                                onClick = { viewModel.requestDeleteVoucher(voucher, currentUserRole == UserRole.ADMIN) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
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
