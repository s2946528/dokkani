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
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
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
import com.example.dokkani.domain.credit.CreditNotebookEngine
import com.example.dokkani.domain.credit.CustomerStatementSummary
import com.example.dokkani.domain.credit.StatementEntryType
import com.example.dokkani.domain.credit.StatementItem
import com.example.dokkani.ui.DokkaniUiState
import com.example.dokkani.ui.screens.crud.AddEditPartyDialog
import com.example.dokkani.ui.screens.crud.ConfirmDeleteDialog
import kotlin.math.abs

/**
 * شاشة إدارة حسابات العملاء والموردين (الديون والذمم)
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

    var selectedTab by remember { mutableIntStateOf(0) } // 0: العملاء, 1: الموردين

    var showAddPartyDialog by remember { mutableStateOf(false) }
    var editingParty by remember { mutableStateOf<PartyEntity?>(null) }
    var deletingParty by remember { mutableStateOf<PartyEntity?>(null) }

    var deletingVoucherId by remember { mutableStateOf<Long?>(null) }
    var deletingInvoiceId by remember { mutableStateOf<Long?>(null) }

    // تقسيم الحسابات حسب التبويب المفتوح
    val customers = remember(parties) {
        parties.filter { it.type == PartyType.CUSTOMER || it.type == PartyType.BOTH }
    }
    val suppliers = remember(parties) {
        parties.filter { it.type == PartyType.SUPPLIER || it.type == PartyType.BOTH }
    }

    val activeList = if (selectedTab == 0) customers else suppliers

    val filteredParties = remember(activeList, uiState.creditSearchQuery) {
        activeList.filter { party ->
            if (uiState.creditSearchQuery.isBlank()) true
            else party.name.contains(uiState.creditSearchQuery.trim(), ignoreCase = true) ||
                    party.phone.contains(uiState.creditSearchQuery.trim()) ||
                    party.taxNumber.contains(uiState.creditSearchQuery.trim())
        }
    }

    // إحصائيات العملاء
    val totalCustomerDebt = customers.filter { it.currentBalance > 0 }.sumOf { it.currentBalance }
    val debtorsCount = customers.count { it.currentBalance > 0 }
    val topDebtor = customers.maxByOrNull { it.currentBalance }

    // إحصائيات الموردين
    val totalSupplierPayable = suppliers.filter { it.currentBalance < 0 }.sumOf { abs(it.currentBalance) }
    val suppliersOwedCount = suppliers.count { it.currentBalance < 0 }
    val topSupplierOwed = suppliers.minByOrNull { it.currentBalance }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FA))
                .padding(12.dp)
        ) {
            // شريط العنوان الرئيسي المباشر
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "العملاء والموردين",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = if (selectedTab == 0)
                            "متابعة الديون المستحقة على العملاء، كشوفات حساباتهم، وسندات القبض الفورية"
                        else
                            "متابعة الذمم المالية للموردين، المبالغ المستحقة لهم، وحركات ومشتريات الموردين",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )
                }

                if (isAdmin) {
                    Button(
                        onClick = { showAddPartyDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedTab == 0) Color(0xFF0F5132) else Color(0xFF0284C7)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (selectedTab == 0) "+ عميل جديد" else "+ مورد جديد")
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // نظام التبويبات (Tabs)
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        onSearchChanged("")
                    },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("حسابات العملاء", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(6.dp))
                            BadgeCount(customers.size, color = Color(0xFFDC2626))
                        }
                    }
                )

                Tab(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        onSearchChanged("")
                    },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocalShipping,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("حسابات الموردين", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(6.dp))
                            BadgeCount(suppliers.size, color = Color(0xFFEA580C))
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // بطاقات المؤشرات المالية للتبويب المحدد
            if (selectedTab == 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CreditKpiCard(
                        title = "إجمالي الديون المستحقة",
                        value = "${"%.2f".format(totalCustomerDebt)} ${uiState.currencySymbol}",
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
                        value = if (topDebtor != null && topDebtor.currentBalance > 0) "${"%.2f".format(topDebtor.currentBalance)} ${uiState.currencySymbol}" else "0.00",
                        subtitle = topDebtor?.name ?: "لا يوجد",
                        backgroundColor = Color(0xFFF0FDF4),
                        textColor = Color(0xFF16A34A),
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CreditKpiCard(
                        title = "إجمالي الذمم والمستحقات",
                        value = "${"%.2f".format(totalSupplierPayable)} ${uiState.currencySymbol}",
                        subtitle = "مستحق للموردين",
                        backgroundColor = Color(0xFFFFF7ED),
                        textColor = Color(0xFFC2410C),
                        modifier = Modifier.weight(1f)
                    )
                    CreditKpiCard(
                        title = "عدد الموردين المستحقين",
                        value = "$suppliersOwedCount مورد",
                        subtitle = "ننتظر التسديد",
                        backgroundColor = Color(0xFFEFF6FF),
                        textColor = Color(0xFF1D4ED8),
                        modifier = Modifier.weight(1f)
                    )
                    CreditKpiCard(
                        title = "أعلى مستحق للموردين",
                        value = if (topSupplierOwed != null && topSupplierOwed.currentBalance < 0) "${"%.2f".format(abs(topSupplierOwed.currentBalance))} ${uiState.currencySymbol}" else "0.00",
                        subtitle = topSupplierOwed?.name ?: "لا يوجد",
                        backgroundColor = Color(0xFFF5F3FF),
                        textColor = Color(0xFF6D28D9),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // حقل البحث
            OutlinedTextField(
                value = uiState.creditSearchQuery,
                onValueChange = onSearchChanged,
                placeholder = {
                    Text(if (selectedTab == 0) "ابحث باسم العميل أو رقم الجوال..." else "ابحث باسم المورد، الرقم الضريبي، أو رقم الجوال...")
                },
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

            // إذا تم اختيار عميل أو مورد لعرض كشف حسابه التفصيلي
            if (uiState.selectedPartyForStatement != null) {
                PartyStatementView(
                    statement = uiState.customerStatementSummary,
                    isLoading = uiState.isLoadingStatement,
                    isAdmin = isAdmin,
                    onBack = { onSelectParty(null) },
                    onAddPayment = { onOpenPaymentVoucherDialog(it) },
                    onDeleteInvoice = { deletingInvoiceId = it },
                    onDeleteVoucher = { deletingVoucherId = it },
                    onSendWhatsApp = { phone, text -> onSendWhatsAppReminder(context, phone, text) },
                    currencySymbol = uiState.currencySymbol
                )
            } else {
                // قائمة الحسابات المسجلة
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredParties, key = { it.id }) { party ->
                        PartyItemCard(
                            party = party,
                            isSupplier = selectedTab == 1,
                            isAdmin = isAdmin,
                            onOpenStatement = { onSelectParty(party.id) },
                            onQuickPay = { onOpenPaymentVoucherDialog(party.id) },
                            onEditParty = { editingParty = party },
                            onDeleteParty = { deletingParty = party },
                            onSendWhatsApp = {
                                val currentStoreName = uiState.settings?.storeName.orEmpty().ifBlank { "دكاني" }
                                val text = if (selectedTab == 0) {
                                    CreditNotebookEngine.generateWhatsAppReminderMessage(
                                        customerName = party.name,
                                        balance = party.currentBalance,
                                        storeName = currentStoreName,
                                        currencySymbol = uiState.currencySymbol,
                                        showDecimals = uiState.showDecimals
                                    )
                                } else {
                                    CreditNotebookEngine.generateSupplierWhatsAppMessage(
                                        supplierName = party.name,
                                        balance = party.currentBalance,
                                        storeName = currentStoreName,
                                        currencySymbol = uiState.currencySymbol,
                                        showDecimals = uiState.showDecimals
                                    )
                                }
                                onSendWhatsAppReminder(context, party.phone, text)
                            },
                            currencySymbol = uiState.currencySymbol
                        )
                    }

                    if (filteredParties.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (selectedTab == 0) "لم يتم العثور على عملاء مطابقين للبحث" else "لم يتم العثور على موردين مطابقين للبحث",
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
            defaultPartyType = if (selectedTab == 1) PartyType.SUPPLIER else PartyType.CUSTOMER,
            onSaveParty = { p ->
                onSaveParty(p)
                showAddPartyDialog = false
                editingParty = null
            },
            onDismiss = {
                showAddPartyDialog = false
                editingParty = null
            },
            currencySymbol = uiState.currencySymbol
        )
    }

    if (deletingParty != null) {
        ConfirmDeleteDialog(
            message = "هل أنت تأكيد من حذف الحساب '${deletingParty?.name}'؟",
            onConfirm = {
                onDeleteParty(deletingParty!!)
                deletingParty = null
            },
            onDismiss = { deletingParty = null }
        )
    }

    if (deletingInvoiceId != null) {
        ConfirmDeleteDialog(
            message = "هل أنت تأكيد من حذف الفاتورة رقم #$deletingInvoiceId؟",
            onConfirm = {
                onDeleteInvoice(deletingInvoiceId!!)
                deletingInvoiceId = null
            },
            onDismiss = { deletingInvoiceId = null }
        )
    }

    if (deletingVoucherId != null) {
        ConfirmDeleteDialog(
            message = "هل أنت تأكيد من إلغاء وحذف السند رقم #$deletingVoucherId؟",
            onConfirm = {
                onDeleteVoucher(deletingVoucherId!!)
                deletingVoucherId = null
            },
            onDismiss = { deletingVoucherId = null }
        )
    }

    // نافذة حوار تسجيل سند القبض / سند الصرف
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
            onSubmit = onSubmitPaymentVoucher,
            currencySymbol = uiState.currencySymbol
        )
    }
}

/**
 * شارة عدد الحسابات للتبويب
 */
@Composable
private fun BadgeCount(count: Int, color: Color) {
    Surface(
        shape = CircleShape,
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            text = "$count",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
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
 * بطاقة العميل أو المورد مع الإجراءات السريعة المنظمة والتباين العالي
 */
@Composable
private fun PartyItemCard(
    party: PartyEntity,
    isSupplier: Boolean,
    isAdmin: Boolean,
    onOpenStatement: () -> Unit,
    onQuickPay: () -> Unit,
    onEditParty: () -> Unit,
    onDeleteParty: () -> Unit,
    onSendWhatsApp: () -> Unit,
    currencySymbol: String = "ر.ي"
) {
    val balance = party.currentBalance

    // للعميل: الموجب يعني مستحق على العميل
    // للمورد: السالب يعني مستحق للمورد
    val isOwed = if (!isSupplier) balance > 0.001 else balance < -0.001
    val isSettled = abs(balance) <= 0.001
    val isOverLimit = !isSupplier && party.creditLimit > 0 && balance > party.creditLimit

    val mainBadgeColor = when {
        isSettled -> Color(0xFF16A34A)
        !isSupplier && isOwed -> Color(0xFFDC2626)
        isSupplier && isOwed -> Color(0xFFC2410C)
        else -> Color(0xFF2563EB)
    }

    val mainBadgeBg = when {
        isSettled -> Color(0xFFF0FDF4)
        !isSupplier && isOwed -> Color(0xFFFEF2F2)
        isSupplier && isOwed -> Color(0xFFFFF7ED)
        else -> Color(0xFFEFF6FF)
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenStatement() }
            .testTag("party_item_${party.id}")
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
                        color = mainBadgeBg,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isSupplier) Icons.Default.LocalShipping else Icons.Default.Person,
                                contentDescription = if (isSupplier) "مورد" else "عميل",
                                tint = mainBadgeColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = party.name,
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
                                text = if (party.phone.isNotBlank()) party.phone else "بدون جوال",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                            if (party.taxNumber.isNotBlank()) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.Badge,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "ضريبي: ${party.taxNumber}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }

                // عرض الرصيد والتباين المالي
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${"%.2f".format(abs(balance))} $currencySymbol",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = mainBadgeColor
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = mainBadgeBg
                        ) {
                            Text(
                                text = when {
                                    isSettled -> "الحساب خالص"
                                    !isSupplier && balance > 0 -> "مستحق على العميل"
                                    !isSupplier && balance < 0 -> "رصيد مقدم للعميل"
                                    isSupplier && balance < 0 -> "مستحق للمورد"
                                    else -> "رصيد لصالح المحل"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = mainBadgeColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
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

            // تنبيه تجاوز الحد الائتماني للعملاء
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
                            text = "تجاوز السقف الائتماني (${"%.2f".format(party.creditLimit)} $currencySymbol)",
                            fontSize = 11.sp,
                            color = Color(0xFFC2410C),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // أزرار العمليات السريعة المنظمة
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onQuickPay,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSupplier) Color(0xFF0284C7) else Color(0xFF0F5132)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(36.dp)
                ) {
                    Icon(
                        imageVector = if (isSupplier) Icons.Default.Payments else Icons.Default.ReceiptLong,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isSupplier) "+ تسديد / سند صرف" else "+ تسديد / سند قبض",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onOpenStatement,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("كشف الحساب", fontSize = 11.sp, color = Color(0xFF0F172A))
                }

                if (party.phone.isNotBlank()) {
                    Button(
                        onClick = onSendWhatsApp,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("واتساب", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

/**
 * شاشة تفاصيل كشف الحساب للحساب المحدد (عميل أو مورد)
 */
@Composable
private fun PartyStatementView(
    statement: CustomerStatementSummary?,
    isLoading: Boolean,
    isAdmin: Boolean,
    onBack: () -> Unit,
    onAddPayment: (Long) -> Unit,
    onDeleteInvoice: (Long) -> Unit,
    onDeleteVoucher: (Long) -> Unit,
    onSendWhatsApp: (String, String) -> Unit,
    currencySymbol: String = "ر.ي"
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
    val isSupplier = party.type == PartyType.SUPPLIER

    Column(modifier = Modifier.fillMaxSize()) {
        // شريط العودة ورأس الحساب
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
                    text = "كشف حساب: ${party.name} (${if (isSupplier) "مورد" else "عميل"})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = "جوال: ${party.phone.ifBlank { "غير مسجل" }} ${if (party.taxNumber.isNotBlank()) "| ضريبي: ${party.taxNumber}" else ""}",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // بطاقة ملخص الحساب
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isSupplier) Color(0xFF1E293B) else Color(0xFF0F3B2E)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isSupplier) "الرصيد المستحق القائم للمورد" else "الرصيد المستحق القائم على العميل",
                            fontSize = 12.sp,
                            color = Color(0xFFD1E7DD)
                        )
                        Text(
                            text = "${"%.2f".format(abs(statement.currentBalance))} $currencySymbol",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onAddPayment(party.id) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSupplier) Color(0xFF0284C7) else Color(0xFF22C55E)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (isSupplier) "+ سند صرف" else "+ سند قبض",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        if (party.phone.isNotBlank()) {
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
                HorizontalDivider(color = Color(0xFF334155))
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isSupplier) "إجمالي المشتريات والذمم: ${"%.2f".format(statement.totalPurchasesOnCredit)} $currencySymbol"
                        else "إجمالي المشتريات بالآجل: ${"%.2f".format(statement.totalPurchasesOnCredit)} $currencySymbol",
                        fontSize = 11.sp,
                        color = Color(0xFFE2E8F0)
                    )
                    Text(
                        text = "إجمالي المسدد: ${"%.2f".format(statement.totalPayments)} $currencySymbol",
                        fontSize = 11.sp,
                        color = Color(0xFF86EFAC)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "السجل الزمني للحركات والعمليات:",
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
                    onDeleteVoucher = onDeleteVoucher,
                    currencySymbol = currencySymbol
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
                        Text("لا توجد حركات مسجلة لهذا الحساب حتى الآن", color = Color(0xFF64748B))
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
    onDeleteVoucher: (Long) -> Unit,
    currencySymbol: String = "ر.ي"
) {
    val isInvoice = item.type == StatementEntryType.SALE_INVOICE || item.type == StatementEntryType.PURCHASE_INVOICE
    val isReturn = item.type == StatementEntryType.SALE_RETURN || item.type == StatementEntryType.PURCHASE_RETURN

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
                    color = when (item.type) {
                        StatementEntryType.SALE_INVOICE, StatementEntryType.PURCHASE_INVOICE -> Color(0xFFFEF2F2)
                        StatementEntryType.SALE_RETURN, StatementEntryType.PURCHASE_RETURN -> Color(0xFFEFF6FF)
                        StatementEntryType.OPENING_BALANCE -> Color(0xFFF1F5F9)
                        else -> Color(0xFFF0FDF4)
                    },
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = when (item.type) {
                                StatementEntryType.SALE_INVOICE, StatementEntryType.PURCHASE_INVOICE -> "فاتورة"
                                StatementEntryType.SALE_RETURN, StatementEntryType.PURCHASE_RETURN -> "مرتجع"
                                StatementEntryType.OPENING_BALANCE -> "رصيد"
                                else -> "سند"
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (item.type) {
                                StatementEntryType.SALE_INVOICE, StatementEntryType.PURCHASE_INVOICE -> Color(0xFFDC2626)
                                StatementEntryType.SALE_RETURN, StatementEntryType.PURCHASE_RETURN -> Color(0xFF2563EB)
                                StatementEntryType.OPENING_BALANCE -> Color(0xFF475569)
                                else -> Color(0xFF16A34A)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val refText = if (item.type == StatementEntryType.OPENING_BALANCE) "" else " #${item.refNumber}"
                        Text(
                            text = "${item.type.labelArabic}$refText",
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
                    if (item.debit > 0) {
                        Text(
                            text = "+${"%.2f".format(item.debit)} $currencySymbol",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFFDC2626)
                        )
                    } else {
                        Text(
                            text = "-${"%.2f".format(item.credit)} $currencySymbol",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF16A34A)
                        )
                    }
                    Text(
                        text = "الرصيد: ${"%.2f".format(abs(item.runningBalance))} $currencySymbol",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }

                if (isAdmin && item.type != StatementEntryType.OPENING_BALANCE && item.rawId > 0) {
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
 * نافذة حوار إضافة سند قبض وسند صرف تسديد
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
    onSubmit: () -> Unit,
    currencySymbol: String = "ر.ي"
) {
    val isSupplier = party?.type == PartyType.SUPPLIER

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = if (isSupplier) Color(0xFFE0F2FE) else Color(0xFFDCFCE7),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (isSupplier) Color(0xFF0284C7) else Color(0xFF16A34A),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isSupplier) "تسجيل سند صرف وتسديد للمورد: ${party?.name ?: ""}" else "تسجيل سند قبض لحساب العميل: ${party?.name ?: ""}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
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
                            Text(
                                text = if (isSupplier) "الرصيد المتبقي المستحق للمورد:" else "الرصيد المتبقي المستحق على العميل:",
                                fontSize = 12.sp,
                                color = Color(0xFF475569)
                            )
                            Text(
                                text = "${"%.2f".format(abs(party.currentBalance))} $currencySymbol",
                                fontWeight = FontWeight.Bold,
                                color = if (isSupplier) Color(0xFFC2410C) else Color(0xFFDC2626)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { onInputsChanged(it, notesInput, selectedMethod) },
                    label = { Text(if (isSupplier) "المبلغ المدفوع ($currencySymbol)*" else "المبلغ المستلم ($currencySymbol)*") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("voucher_amount_field")
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(if (isSupplier) "طريقة الدفع والصرف:" else "طريقة الاستلام:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
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
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSupplier) Color(0xFF0284C7) else Color(0xFF0F5132)
                )
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                } else {
                    Text(if (isSupplier) "حفظ سند الصرف وخصم الصندوق" else "حفظ السند وإيداع الخزينة", fontWeight = FontWeight.Bold)
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
