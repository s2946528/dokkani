package com.example.dokkani.ui.screens.vouchers

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
import com.example.dokkani.data.local.entities.PartyEntity
import com.example.dokkani.data.local.entities.PartyType
import com.example.dokkani.data.local.entities.PaymentMethod
import com.example.dokkani.data.local.entities.PaymentVoucherEntity
import com.example.dokkani.data.local.entities.UserRole
import com.example.dokkani.data.local.entities.VoucherType
import com.example.dokkani.ui.components.AppSearchBar
import com.example.dokkani.ui.components.DirectEditVoucherDialog
import com.example.dokkani.ui.components.PaymentMethodSelector
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VouchersScreen(
    initialVoucherType: VoucherType = VoucherType.RECEIPT,
    currentUserRole: UserRole = UserRole.ADMIN,
    viewModel: VouchersViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(initialVoucherType) {
        viewModel.setVoucherType(initialVoucherType)
    }

    var newPartyName by remember { mutableStateOf("") }
    var newPartyPhone by remember { mutableStateOf("") }
    var newPartyType by remember { mutableStateOf(PartyType.CUSTOMER) }

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
                // شريط عنوان الشاشة العلوي
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
                                if (uiState.selectedVoucherType == VoucherType.RECEIPT) Icons.Default.PointOfSale else Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = if (uiState.selectedVoucherType == VoucherType.RECEIPT) Color(0xFF2E7D32) else Color(0xFFE65100),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "إدارة وإصدار سندات القبض والصرف الفورية",
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

                // محدد نوع السند (سند قبض / سند صرف)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (uiState.selectedVoucherType == VoucherType.RECEIPT) Color(0xFF2E7D32) else MaterialTheme.colorScheme.surfaceVariant,
                            onClick = { viewModel.setVoucherType(VoucherType.RECEIPT) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                    tint = if (uiState.selectedVoucherType == VoucherType.RECEIPT) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "سند قبض (تحصيل مالي / مقبوضات)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (uiState.selectedVoucherType == VoucherType.RECEIPT) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (uiState.selectedVoucherType == VoucherType.PAYMENT) Color(0xFFE65100) else MaterialTheme.colorScheme.surfaceVariant,
                            onClick = { viewModel.setVoucherType(VoucherType.PAYMENT) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.ArrowUpward,
                                    contentDescription = null,
                                    tint = if (uiState.selectedVoucherType == VoucherType.PAYMENT) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "سند صرف (سداد مالي / مدفوعات)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (uiState.selectedVoucherType == VoucherType.PAYMENT) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // كارت تحرير بيانات السند
                VoucherCreationCard(
                    uiState = uiState,
                    viewModel = viewModel,
                    onAddNewParty = {
                        newPartyType = if (uiState.selectedVoucherType == VoucherType.RECEIPT) PartyType.CUSTOMER else PartyType.SUPPLIER
                        viewModel.showAddPartyDialog()
                    }
                )

                // قسم السجل والأرشيف
                VouchersHistorySection(
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
                    Text(
                        if (uiState.selectedVoucherType == VoucherType.RECEIPT) "تم اعتماد سند القبض بنجاح" else "تم اعتماد سند الصرف بنجاح",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "رقم السند المسجل: ${uiState.lastSavedVoucherNumber}",
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

        // نافذة إضافة طرف جديد
        if (uiState.showAddPartyDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissAddPartyDialog() },
                title = {
                    Text(
                        if (newPartyType == PartyType.CUSTOMER) "إضافة عميل جديد" else "إضافة مورد جديد",
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
                            label = { Text("الاسم الكامل *") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = newPartyPhone,
                            onValueChange = { newPartyPhone = it },
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
                            viewModel.addQuickParty(newPartyName, newPartyPhone, newPartyType)
                            newPartyName = ""
                            newPartyPhone = ""
                        },
                        enabled = newPartyName.isNotBlank()
                    ) {
                        Text("حفظ وإضافة")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissAddPartyDialog() }) {
                        Text("إلغاء")
                    }
                }
            )
        }

        // نافذة معاينة تفاصيل السند
        if (uiState.showDetailsDialog && uiState.selectedVoucherForDetails != null) {
            val v = uiState.selectedVoucherForDetails!!
            val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.ENGLISH)
            val dateStr = dateFormat.format(Date(v.date))

            AlertDialog(
                onDismissRequest = { viewModel.dismissDetailsDialog() },
                title = { Text("تفاصيل السند #${v.voucherNumber}", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("نوع السند: ${if (v.isPayment) "سند صرف" else "سند قبض"}", fontWeight = FontWeight.Bold, color = if (v.isPayment) Color(0xFFE65100) else Color(0xFF2E7D32))
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

        // نافذة تعديل السند (DirectEditVoucherDialog)
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
                        Text("يرجى إدخال رمز PIN لمدير النظام لتأكيد حذف السند:")
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
fun VoucherCreationCard(
    uiState: VouchersUiState,
    viewModel: VouchersViewModel,
    onAddNewParty: () -> Unit
) {
    val isReceipt = uiState.selectedVoucherType == VoucherType.RECEIPT

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
                    Icon(
                        if (isReceipt) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                        contentDescription = null,
                        tint = if (isReceipt) Color(0xFF2E7D32) else Color(0xFFE65100),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (isReceipt) "تحرير بيانات سند القبض" else "تحرير بيانات سند الصرف",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Button(
                    onClick = onAddNewParty,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isReceipt) "عميل جديد" else "مورد جديد", fontSize = 11.sp)
                }
            }

            // اختيار الطرف
            val partyLabel = if (isReceipt) "اسم العميل / الطرف المسدد" else "اسم المورد / الجهة المسدد لها"
            var partyDropdownExpanded by remember { mutableStateOf(false) }

            val filteredParties = remember(uiState.parties, uiState.selectedVoucherType) {
                val targetType = if (isReceipt) PartyType.CUSTOMER else PartyType.SUPPLIER
                uiState.parties.filter { it.type == targetType || it.type == PartyType.BOTH }
            }

            ExposedDropdownMenuBox(
                expanded = partyDropdownExpanded,
                onExpandedChange = { partyDropdownExpanded = !partyDropdownExpanded }
            ) {
                OutlinedTextField(
                    value = uiState.selectedParty?.name ?: "اختر من القائمة (أو اترك فارغاً للعام)",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(partyLabel) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = partyDropdownExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                ExposedDropdownMenu(
                    expanded = partyDropdownExpanded,
                    onDismissRequest = { partyDropdownExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("عام / بدون تحديد حساب", fontWeight = FontWeight.Bold) },
                        onClick = {
                            viewModel.selectParty(null)
                            partyDropdownExpanded = false
                        }
                    )
                    filteredParties.forEach { p ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(p.name, fontWeight = FontWeight.Bold)
                                    Text("الهاتف: ${p.phone} | الرصيد الحالي: %.2f".format(p.currentBalance), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            },
                            onClick = {
                                viewModel.selectParty(p)
                                partyDropdownExpanded = false
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
                    label = { Text("مبلغ السند (${uiState.baseCurrencySymbol}) *") },
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

            // طريقة السداد والتحصيل الديناميكية
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
                onClick = { viewModel.executeSubmitVoucher() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp),
                enabled = !uiState.isProcessing && uiState.amountInput.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = if (isReceipt) Color(0xFF2E7D32) else Color(0xFFE65100))
            ) {
                if (uiState.isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (isReceipt) "اعتماد وحفظ سند القبض" else "اعتماد وحفظ سند الصرف",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun VouchersHistorySection(
    uiState: VouchersUiState,
    viewModel: VouchersViewModel,
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
                    .clickable { viewModel.toggleHistoryExpanded() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("سجل وأرشيف سندات القبض والصرف (${uiState.allVouchers.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Icon(
                    imageVector = if (uiState.isHistoryExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }

            AnimatedVisibility(visible = uiState.isHistoryExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // فلترة نوع السند
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = uiState.historyFilter == "ALL",
                            onClick = { viewModel.setHistoryFilter("ALL") },
                            label = { Text("الكل", fontSize = 11.sp) },
                            shape = RoundedCornerShape(16.dp)
                        )
                        FilterChip(
                            selected = uiState.historyFilter == "RECEIPT",
                            onClick = { viewModel.setHistoryFilter("RECEIPT") },
                            label = { Text("سندات القبض", fontSize = 11.sp) },
                            shape = RoundedCornerShape(16.dp)
                        )
                        FilterChip(
                            selected = uiState.historyFilter == "PAYMENT",
                            onClick = { viewModel.setHistoryFilter("PAYMENT") },
                            label = { Text("سندات الصرف", fontSize = 11.sp) },
                            shape = RoundedCornerShape(16.dp)
                        )
                    }

                    AppSearchBar(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = "البحث برقم السند أو اسم الطرف أو البيان..."
                    )

                    val filteredVouchers = remember(uiState.allVouchers, uiState.historyFilter, uiState.searchQuery) {
                        uiState.allVouchers.filter { v ->
                            val matchType = when (uiState.historyFilter) {
                                "RECEIPT" -> !v.isPayment
                                "PAYMENT" -> v.isPayment
                                else -> true
                            }
                            val matchQuery = uiState.searchQuery.isBlank() ||
                                v.voucherNumber.contains(uiState.searchQuery, ignoreCase = true) ||
                                v.notes.contains(uiState.searchQuery, ignoreCase = true) ||
                                v.transactionRef.contains(uiState.searchQuery, ignoreCase = true)
                            matchType && matchQuery
                        }
                    }

                    if (filteredVouchers.isEmpty()) {
                        Text("لا توجد سندات مسجلة حالياً تطابق البحث.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 260.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(filteredVouchers) { v ->
                                val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.ENGLISH)
                                val dateStr = dateFormat.format(Date(v.date))

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
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = if (v.isPayment) Color(0xFFFFF3E0) else Color(0xFFE8F5E9)
                                                ) {
                                                    Text(
                                                        if (v.isPayment) "سند صرف" else "سند قبض",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (v.isPayment) Color(0xFFE65100) else Color(0xFF1B5E20),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("#${v.voucherNumber}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }
                                            Text("التاريخ: $dateStr | السداد: ${v.paymentMethod.labelArabic}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            if (v.notes.isNotBlank()) {
                                                Text(v.notes, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                "%.2f %s".format(v.amount, uiState.baseCurrencySymbol),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = if (v.isPayment) Color(0xFFE65100) else Color(0xFF2E7D32)
                                            )
                                            Row {
                                                IconButton(
                                                    onClick = { viewModel.viewVoucherDetails(v) },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(Icons.Default.Visibility, contentDescription = "معاينة", tint = MaterialTheme.colorScheme.primary)
                                                }

                                                IconButton(
                                                    onClick = { viewModel.requestEditVoucher(v) },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.secondary)
                                                }

                                                IconButton(
                                                    onClick = { viewModel.requestDeleteVoucher(v, currentUserRole == UserRole.ADMIN) },
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
