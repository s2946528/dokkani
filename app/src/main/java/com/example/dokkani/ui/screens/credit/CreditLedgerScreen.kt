package com.example.dokkani.ui.screens.credit

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dokkani.data.local.entities.PartyEntity
import com.example.dokkani.data.local.entities.PartyType
import com.example.dokkani.data.local.entities.PaymentMethod
import com.example.dokkani.data.local.entities.UserRole
import com.example.dokkani.domain.credit.CustomerStatementSummary
import com.example.dokkani.domain.credit.StatementEntryType
import com.example.dokkani.domain.credit.StatementItem
import com.example.dokkani.ui.DokkaniUiState
import com.example.dokkani.ui.screens.crud.AddEditPartyDialog
import com.example.dokkani.ui.screens.crud.ConfirmDeleteDialog

/**
 * شاشة إدارة الديون ودفتر الشكك والعملاء (Credit & Customer Ledger)
 */
@Composable
fun CreditLedgerScreen(
    parties: List<PartyEntity>,
    uiState: DokkaniUiState,
    currentUserRole: UserRole = UserRole.ADMIN,
    onSearchChanged: (String) -> Unit,
    onSelectParty: (Long?) -> Unit,
    onOpenPaymentVoucherDialog: (Long) -> Unit,
    onDismissPaymentVoucherDialog: () -> Unit,
    onVoucherInputsChanged: (String, String, PaymentMethod) -> Unit,
    onSubmitPaymentVoucher: () -> Unit,
    onSaveParty: (PartyEntity) -> Unit = {},
    onDeleteParty: (PartyEntity) -> Unit = {},
    onDeleteVoucher: (Long) -> Unit = {},
    onDeleteInvoice: (Long) -> Unit = {},
    onSendWhatsAppReminder: (Context, String, String) -> Unit
) {
    val context = LocalContext.current
    val isAdmin = currentUserRole == UserRole.ADMIN

    var showAddPartyDialog by remember { mutableStateOf(false) }
    var editingParty by remember { mutableStateOf<PartyEntity?>(null) }
    var deletingParty by remember { mutableStateOf<PartyEntity?>(null) }

    var deletingVoucherId by remember { mutableStateOf<Long?>(null) }
    var deletingInvoiceId by remember { mutableStateOf<Long?>(null) }

    // تصفية العملاء (العملاء فقط والمشتركين، واستبعاد الموردين الخالصين)
    val customers = parties.filter { it.type == PartyType.CUSTOMER || it.type == PartyType.BOTH }
    val filteredCustomers = customers.filter { cust ->
        if (uiState.creditSearchQuery.isBlank()) true
        else cust.name.contains(uiState.creditSearchQuery.trim(), ignoreCase = true) ||
                cust.phone.contains(uiState.creditSearchQuery.trim())
    }

    val totalDebtAmount = customers.filter { it.currentBalance > 0 }.sumOf { it.currentBalance }
    val debtorsCount = customers.count { it.currentBalance > 0 }
    val topDebtor = customers.maxByOrNull { it.currentBalance }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FA))
                .padding(12.dp)
        ) {
            // شريط العنوان والبحث
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "دفتر الشكك وحسابات العملاء",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = "متابعة الديون المستحقة، كشوفات الحساب الزمنية، وسندات القبض الفورية",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )
                }

                if (isAdmin) {
                    Button(
                        onClick = { showAddPartyDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F5132)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("عميل جديد")
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // بطاقات المؤشرات المالية السريعة للديون
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CreditKpiCard(
                    title = "إجمالي الديون المستحقة",
                    value = "${"%.2f".format(totalDebtAmount)} ر.س",
                    subtitle = "في ذمة العملاء",
                    backgroundColor = Color(0xFFFEF2F2),
                    textColor = Color(0xFFDC2626),
                    modifier = Modifier.weight(1f)
                )
                CreditKpiCard(
                    title = "عدد العملاء المدينين",
                    value = "$debtorsCount عميل",
                    subtitle = "لديهم رصيد آجل",
                    backgroundColor = Color(0xFFFFFBEB),
                    textColor = Color(0xFFD97706),
                    modifier = Modifier.weight(1f)
                )
                CreditKpiCard(
                    title = "أعلى مديونية",
                    value = if (topDebtor != null && topDebtor.currentBalance > 0) "${"%.2f".format(topDebtor.currentBalance)} ر.س" else "0.00",
                    subtitle = topDebtor?.name ?: "لا يوجد",
                    backgroundColor = Color(0xFFF0FDF4),
                    textColor = Color(0xFF16A34A),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // حقل البحث
            OutlinedTextField(
                value = uiState.creditSearchQuery,
                onValueChange = onSearchChanged,
                placeholder = { Text("ابحث باسم العميل أو رقم الجوال...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "بحث") },
                trailingIcon = {
                    if (uiState.creditSearchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChanged("") }) {
                            Icon(Icons.Default.Close, contentDescription = "مسح")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("credit_search_field")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // إذا تم اختيار عميل لعرض كشف حسابه التفصيلي
            if (uiState.selectedPartyForStatement != null) {
                CustomerStatementView(
                    statement = uiState.customerStatementSummary,
                    isLoading = uiState.isLoadingStatement,
                    isAdmin = isAdmin,
                    onBack = { onSelectParty(null) },
                    onAddPayment = { onOpenPaymentVoucherDialog(it) },
                    onDeleteInvoice = { deletingInvoiceId = it },
                    onDeleteVoucher = { deletingVoucherId = it },
                    onSendWhatsApp = { phone, text -> onSendWhatsAppReminder(context, phone, text) }
                )
            } else {
                // قائمة العملاء المسجلين
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredCustomers, key = { it.id }) { customer ->
                        CustomerLedgerItemCard(
                            customer = customer,
                            isAdmin = isAdmin,
                            onOpenStatement = { onSelectParty(customer.id) },
                            onQuickPay = { onOpenPaymentVoucherDialog(customer.id) },
                            onEditParty = { editingParty = customer },
                            onDeleteParty = { deletingParty = customer },
                            onSendWhatsApp = {
                                val text = com.example.dokkani.domain.credit.CreditNotebookEngine.generateWhatsAppReminderMessage(
                                    customerName = customer.name,
                                    balance = customer.currentBalance,
                                    storeName = "تموينات دكاني"
                                )
                                onSendWhatsAppReminder(context, customer.phone, text)
                            }
                        )
                    }

                    if (filteredCustomers.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "لم يتم العثور على عملاء مطابقين للبحث",
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (showAddPartyDialog || editingParty != null) {
        AddEditPartyDialog(
            initialParty = editingParty,
            onSaveParty = { p ->
                onSaveParty(p)
                showAddPartyDialog = false
                editingParty = null
            },
            onDismiss = {
                showAddPartyDialog = false
                editingParty = null
            }
        )
    }

    if (deletingParty != null) {
        ConfirmDeleteDialog(
            message = "هل أنت ألكيد من حذف العميل '${deletingParty?.name}'؟",
            onConfirm = {
                onDeleteParty(deletingParty!!)
                deletingParty = null
            },
            onDismiss = { deletingParty = null }
        )
    }

    if (deletingInvoiceId != null) {
        ConfirmDeleteDialog(
            message = "هل أنت ألكيد من حذف الفاتورة رقم #$deletingInvoiceId؟",
            onConfirm = {
                onDeleteInvoice(deletingInvoiceId!!)
                deletingInvoiceId = null
            },
            onDismiss = { deletingInvoiceId = null }
        )
    }

    if (deletingVoucherId != null) {
        ConfirmDeleteDialog(
            message = "هل أنت ألكيد من إلغاء وحذف سند القبض رقم #$deletingVoucherId؟",
            onConfirm = {
                onDeleteVoucher(deletingVoucherId!!)
                deletingVoucherId = null
            },
            onDismiss = { deletingVoucherId = null }
        )
    }

    // نافذة حوار تسجيل سند قبض وتسديد دفعة
    if (uiState.showPaymentVoucherDialog && uiState.voucherPartyId != null) {
        val currentParty = parties.firstOrNull { it.id == uiState.voucherPartyId }
        PaymentVoucherDialog(
            party = currentParty,
            amountInput = uiState.voucherAmountInput,
            notesInput = uiState.voucherNotesInput,
            selectedMethod = uiState.voucherPaymentMethod,
            isSubmitting = uiState.isSubmittingVoucher,
            onInputsChanged = onVoucherInputsChanged,
            onDismiss = onDismissPaymentVoucherDialog,
            onSubmit = onSubmitPaymentVoucher
        )
    }
}

/**
 * بطاقة إحصائية مدمجة
 */
@Composable
private fun CreditKpiCard(
    title: String,
    value: String,
    subtitle: String,
    backgroundColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(text = title, fontSize = 11.sp, color = Color(0xFF64748B))
            Text(text = value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textColor)
            Text(text = subtitle, fontSize = 10.sp, color = Color(0xFF94A3B8), maxLines = 1)
        }
    }
}

/**
 * بطاقة العميل في دفتر الشكك
 */
@Composable
private fun CustomerLedgerItemCard(
    customer: PartyEntity,
    isAdmin: Boolean,
    onOpenStatement: () -> Unit,
    onQuickPay: () -> Unit,
    onEditParty: () -> Unit,
    onDeleteParty: () -> Unit,
    onSendWhatsApp: () -> Unit
) {
    val hasDebt = customer.currentBalance > 0.001
    val isOverLimit = customer.creditLimit > 0 && customer.currentBalance > customer.creditLimit

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenStatement() }
            .testTag("party_item_${customer.id}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = if (hasDebt) Color(0xFFFEE2E2) else Color(0xFFDCFCE7),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "عميل",
                                tint = if (hasDebt) Color(0xFFDC2626) else Color(0xFF16A34A),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = customer.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF0F172A)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (customer.phone.isNotBlank()) customer.phone else "بدون جوال",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }

                // رصيد الدين الحالي مع أزرار الإدارة
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${"%.2f".format(customer.currentBalance)} ر.س",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = if (hasDebt) Color(0xFFDC2626) else Color(0xFF16A34A)
                        )
                        Text(
                            text = if (hasDebt) "مستحق على العميل" else "الحساب خالص",
                            fontSize = 11.sp,
                            color = if (hasDebt) Color(0xFFEF4444) else Color(0xFF22C55E)
                        )
                    }

                    if (isAdmin) {
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = onEditParty,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "تعديل",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = onDeleteParty,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "حذف",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // تنبيه تجاوز الحد الائتماني
            if (isOverLimit) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = Color(0xFFFFF7ED),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "تحذير",
                            tint = Color(0xFFEA580C),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "تجاوز السقف الائتماني (${"%.2f".format(customer.creditLimit)} ر.س)",
                            fontSize = 11.sp,
                            color = Color(0xFFC2410C),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // أزرار العمليات السريعة
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onQuickPay,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F5132)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                ) {
                    Text("+ تسديد / سند قبض", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onOpenStatement,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                ) {
                    Text("كشف الحساب", fontSize = 12.sp, color = Color(0xFF0F172A))
                }

                if (customer.phone.isNotBlank() && hasDebt) {
                    Button(
                        onClick = onSendWhatsApp,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("واتساب", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

/**
 * شاشة تفاصيل كشف حساب العميل
 */
@Composable
private fun CustomerStatementView(
    statement: CustomerStatementSummary?,
    isLoading: Boolean,
    isAdmin: Boolean,
    onBack: () -> Unit,
    onAddPayment: (Long) -> Unit,
    onDeleteInvoice: (Long) -> Unit,
    onDeleteVoucher: (Long) -> Unit,
    onSendWhatsApp: (String, String) -> Unit
) {
    if (isLoading || statement == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(40.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFF0F5132))
        }
        return
    }

    val party = statement.party

    Column(modifier = Modifier.fillMaxSize()) {
        // شريط العودة ورأس العميل
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = "كشف حساب: ${party.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = "جوال: ${party.phone.ifBlank { "غير مسجل" }} | السقف: ${if (party.creditLimit > 0) "${party.creditLimit} ر.س" else "مفتوح"}",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // بطاقة ملخص الحساب
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F3B2E)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("الرصيد القائم المستحق حالياً", fontSize = 12.sp, color = Color(0xFFD1E7DD))
                        Text(
                            text = "${"%.2f".format(statement.currentBalance)} ر.س",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onAddPayment(party.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("+ سند قبض", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        if (party.phone.isNotBlank() && statement.currentBalance > 0) {
                            Button(
                                onClick = { onSendWhatsApp(party.phone, statement.whatsAppReminderText) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("واتساب", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Color(0xFF198754))
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "إجمالي المشتريات بالآجل: ${"%.2f".format(statement.totalPurchasesOnCredit)} ر.س",
                        fontSize = 11.sp,
                        color = Color(0xFFE2E8F0)
                    )
                    Text(
                        text = "إجمالي المسدد: ${"%.2f".format(statement.totalPayments)} ر.س",
                        fontSize = 11.sp,
                        color = Color(0xFF86EFAC)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "السجل الزمني للحركات (فواتير الآجل وسندات القبض):",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF334155)
        )
        Spacer(modifier = Modifier.height(6.dp))

        // جدول الحركات الزمنية
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(statement.timeline, key = { it.id }) { item ->
                StatementRowCard(
                    item = item,
                    isAdmin = isAdmin,
                    onDeleteInvoice = onDeleteInvoice,
                    onDeleteVoucher = onDeleteVoucher
                )
            }

            if (statement.timeline.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(30.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("لا توجد حركات مسجلة لهذا العميل حتى الآن", color = Color(0xFF64748B))
                    }
                }
            }
        }
    }
}

/**
 * صف الحركة الزمنية في كشف الحساب
 */
@Composable
private fun StatementRowCard(
    item: StatementItem,
    isAdmin: Boolean,
    onDeleteInvoice: (Long) -> Unit,
    onDeleteVoucher: (Long) -> Unit
) {
    val isInvoice = item.type == StatementEntryType.SALE_INVOICE

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = if (isInvoice) Color(0xFFFEF2F2) else Color(0xFFF0FDF4),
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = if (isInvoice) "فاتورة" else "سند",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isInvoice) Color(0xFFDC2626) else Color(0xFF16A34A)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.refNumber,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF0F172A)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(${item.paymentMethodArabic})",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                    Text(
                        text = item.description,
                        fontSize = 11.sp,
                        color = Color(0xFF475569)
                    )
                    Text(
                        text = item.dateFormatted,
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            // المبالغ (مدين / دائن والرصيد التراكمي)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    if (isInvoice) {
                        Text(
                            text = "+${"%.2f".format(item.debit)} ر.س",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFFDC2626)
                        )
                    } else {
                        Text(
                            text = "-${"%.2f".format(item.credit)} ر.س",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF16A34A)
                        )
                    }
                    Text(
                        text = "الرصيد: ${"%.2f".format(item.runningBalance)}",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }

                if (isAdmin) {
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = {
                            if (isInvoice) onDeleteInvoice(item.rawId)
                            else onDeleteVoucher(item.rawId)
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "حذف الحركة",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * نافذة حوار إضافة سند قبض وسداد دفعة
 */
@Composable
private fun PaymentVoucherDialog(
    party: PartyEntity?,
    amountInput: String,
    notesInput: String,
    selectedMethod: PaymentMethod,
    isSubmitting: Boolean,
    onInputsChanged: (String, String, PaymentMethod) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFDCFCE7),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("تسجيل سند قبض لحساب: ${party?.name ?: ""}", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (party != null) {
                    Surface(
                        color = Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("الرصيد المتبقي الحالي:", fontSize = 12.sp, color = Color(0xFF475569))
                            Text(
                                text = "${"%.2f".format(party.currentBalance)} ر.س",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { onInputsChanged(it, notesInput, selectedMethod) },
                    label = { Text("المبلغ المستلم (ر.س)*") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("voucher_amount_field")
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text("طريقة الاستلام:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(PaymentMethod.CASH, PaymentMethod.MADA, PaymentMethod.BANK_TRANSFER).forEach { method ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { onInputsChanged(amountInput, notesInput, method) }
                        ) {
                            RadioButton(
                                selected = selectedMethod == method,
                                onClick = { onInputsChanged(amountInput, notesInput, method) }
                            )
                            Text(method.labelArabic, fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = notesInput,
                    onValueChange = { onInputsChanged(amountInput, it, selectedMethod) },
                    label = { Text("ملاحظات إضافية / رقم السند") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSubmit,
                enabled = !isSubmitting && amountInput.toDoubleOrNull()?.let { it > 0 } == true,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F5132))
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                } else {
                    Text("حفظ السند وإيداع الخزينة", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
