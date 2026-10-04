package com.example.dokkani.ui.screens.vouchers

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AltRoute
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dokkani.data.local.entities.CurrencyEntity
import com.example.dokkani.data.local.entities.FinancialAccountEntity
import com.example.dokkani.data.local.entities.FinancialAccountType
import com.example.dokkani.data.local.entities.PaymentMethod
import com.example.dokkani.data.local.entities.PaymentVoucherEntity
import com.example.dokkani.data.local.entities.UserRole
import com.example.dokkani.ui.components.AppSearchBar
import com.example.dokkani.ui.components.VoiceInputIconButton
import com.example.dokkani.ui.components.DirectEditVoucherDialog
import com.example.dokkani.ui.components.normalizeArabicItemSearch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * شاشة إصدار وسجل سندات الصرف وسداد الالتزامات (Payment Voucher)
 * مع المعالجة المحاسبية المزدوجة وسعر الصرف الآلي المستند لقاعدة البيانات وطرق الدفع الديناميكية.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentVoucherScreen(
    currentUserRole: UserRole = UserRole.ADMIN,
    viewModel: PaymentVoucherViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // حالة إظهار حوار القائمة المنبثقة لاختيار الحساب الفرعي المستفيد
    var showBeneficiaryAccountPicker by remember { mutableStateOf(false) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            snackbarHost = {
                uiState.feedbackMessage?.let { msg ->
                    Snackbar(
                        modifier = Modifier.padding(12.dp),
                        containerColor = if (uiState.isError) MaterialTheme.colorScheme.errorContainer else Color(0xFFE65100),
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
                    .padding(10.dp)
                    .testTag("payment_voucher_screen"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. شريط عنوان الواجهة المحترفة وسجل الخزينة والصندوق
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, Color(0xFFE65100).copy(alpha = 0.3f)),
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFE65100)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    "سند صرف وسداد مالي (Payment Voucher)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color(0xFFBF360C)
                                )
                                Text(
                                    "إصدار القيد المحاسبي المزدوج والسداد المباشر من الحسابات المالية للأنشطة والالتزامات",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        val cashDrawerBalance = uiState.allFinancialAccounts
                            .filter { it.accountType == FinancialAccountType.CASH_DRAWER || it.code == "10101" }
                            .sumOf { it.currentBalance }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFFF3E0),
                            border = BorderStroke(1.dp, Color(0xFFE65100).copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = Color(0xFFE65100),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "رصيد الخزينة: %.2f %s".format(Locale.US, cashDrawerBalance, uiState.baseCurrencySymbol),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFBF360C)
                                )
                            }
                        }
                    }
                }

                // 2. بطاقة تحديد الطرف المستفيد (الطرف المدين من حـ/ - الحسابات الفرعية فقط)
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "1. الطرف المستفيد (الطرف المدين: من حـ/)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Text(
                                text = "حساب فرعي تنفيذ",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // عرض الحساب الفرعي المحدد حالياً أو زر الاختيار
                        val selDebited = uiState.selectedBeneficiaryAccount
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (selDebited != null) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            border = BorderStroke(1.dp, if (selDebited != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showBeneficiaryAccountPicker = true }
                                .testTag("btn_select_beneficiary_account")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (selDebited != null) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = selDebited.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "كود: ${selDebited.code} | الأب: ${selDebited.parentAccountName} | الرصيد: %.2f %s".format(Locale.US, selDebited.currentBalance, uiState.baseCurrencySymbol),
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = null,
                                            tint = Color.Gray,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "انقر لاختيار الحساب الفرعي المستفيد من شجرة الدليل المحاسبي...",
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }

                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // 3. بطاقة طريقة الصرف ومصدر السداد المالي (الطرف الدائن إلى حـ/ - طرق ديناميكية)
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Payments,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "2. طريقة الصرف ومصدر السداد (الطرف الدائن: إلى حـ/)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Text(
                                text = "طرق ديناميكية موصلة",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // أشرطة طرق الدفع المصنفة ديناميكياً (إخفاء المجموعات الفارغة)
                        val activeGroups = uiState.activePaymentGroups
                        if (activeGroups.isEmpty()) {
                            Text(
                                "لا توجد حسابات مفعّلة حالياً لطرق الدفع في النظام.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(6.dp)
                            )
                        } else {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(activeGroups) { group ->
                                    val isSelected = uiState.selectedPaymentGroupKey == group.key
                                    val iconVec = when (group.key) {
                                        "BANK" -> Icons.Default.AccountBalance
                                        "WALLET" -> Icons.Default.AccountBalanceWallet
                                        "CARD" -> Icons.Default.CreditCard
                                        "MULTIPLE" -> Icons.AutoMirrored.Filled.AltRoute
                                        else -> Icons.Default.PointOfSale
                                    }

                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.selectPaymentGroupKey(group.key) },
                                        label = {
                                            Text(
                                                "${group.labelArabic} (${group.accounts.size})",
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = iconVec,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        modifier = Modifier.testTag("payment_group_${group.key}")
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // عرض الحسابات المتاحة للمجموعة المختارة
                            val currentGroupAccounts = activeGroups.firstOrNull { it.key == uiState.selectedPaymentGroupKey }?.accounts ?: emptyList()
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(currentGroupAccounts) { acc ->
                                    val isAccSelected = uiState.selectedPaymentAccount?.id == acc.id
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isAccSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                        border = BorderStroke(
                                            if (isAccSelected) 1.5.dp else 0.5.dp,
                                            if (isAccSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                        ),
                                        modifier = Modifier
                                            .clickable { viewModel.selectPaymentAccount(acc) }
                                            .testTag("payment_account_${acc.id}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = isAccSelected,
                                                onClick = { viewModel.selectPaymentAccount(acc) },
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Column {
                                                Text(
                                                    text = acc.name,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isAccSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "الرصيد: %.2f %s".format(Locale.US, acc.currentBalance, uiState.baseCurrencySymbol),
                                                    fontSize = 9.sp,
                                                    color = if (isAccSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. بطاقة المبلغ والعملات وسعر الصرف (سعر الصرف غير قابل للتعديل Read-Only)
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AttachMoney,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "3. المبلغ والعملات وسعر الصرف الآلي",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Text(
                                text = "سعر الصرف آلي من DB",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // اختيار العملة المعتمدة
                        Text("اختر عملة السند:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(uiState.currencies) { curr ->
                                val isCurrSelected = uiState.selectedCurrency?.id == curr.id || (uiState.selectedCurrency == null && curr.isBaseCurrency)
                                FilterChip(
                                    selected = isCurrSelected,
                                    onClick = { viewModel.selectCurrency(curr) },
                                    label = { Text("${curr.name} (${curr.symbol})", fontSize = 11.sp) },
                                    leadingIcon = {
                                        if (curr.isBaseCurrency) {
                                            Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(14.dp))
                                        }
                                    },
                                    modifier = Modifier.testTag("currency_chip_${curr.code}")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // حقل إدخال مبلغ السند
                            OutlinedTextField(
                                value = uiState.amountInput,
                                onValueChange = { viewModel.setAmountInput(it) },
                                label = { Text("المبلغ (${uiState.selectedCurrency?.symbol ?: uiState.baseCurrencySymbol}) *", fontSize = 11.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_voucher_amount")
                            )

                            // حقل سعر الصرف الآلي (غير قابل للتعديل يدوياً - Read-Only)
                            OutlinedTextField(
                                value = "%.2f".format(Locale.US, uiState.exchangeRate),
                                onValueChange = {},
                                readOnly = true,
                                enabled = false,
                                label = { Text("سعر الصرف (آلي)", fontSize = 11.sp) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "قفل سعر الصرف - غير قابل للتعديل",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                    disabledLabelColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_exchange_rate_readonly")
                            )
                        }

                        // عرض القيمة الموازية بالعملة الأساسية إن كانت عملة أجنبية
                        if (uiState.selectedCurrency?.isBaseCurrency == false) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("المبلغ الموازي بالعملة الأساسية:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    Text(
                                        text = "%.2f %s".format(Locale.US, uiState.equivalentBaseAmount, uiState.baseCurrencySymbol),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }

                // 5. بطاقة مركز التكلفة والملاحظات واعتــماد سند الصرف
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = uiState.transactionRef,
                                onValueChange = { viewModel.setTransactionRef(it) },
                                label = { Text("رقم المرجع / الحوالة / الإشعار", fontSize = 11.sp) },
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_transaction_ref")
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = uiState.notesInput,
                            onValueChange = { viewModel.setNotesInput(it) },
                            label = { Text("تفاصيل وملاحظات السند", fontSize = 11.sp) },
                            shape = RoundedCornerShape(10.dp),
                            trailingIcon = {
                                VoiceInputIconButton(
                                    onTextCaptured = { spoken ->
                                        val current = uiState.notesInput
                                        val updated = if (current.isNotBlank()) "$current $spoken" else spoken
                                        viewModel.setNotesInput(updated)
                                    }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_voucher_notes")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // زر اعتماد وحفظ سند الصرف الكبير البارز
                        Button(
                            onClick = { viewModel.executeSubmitPaymentVoucher() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !uiState.isProcessing,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_submit_payment_voucher")
                        ) {
                            if (uiState.isProcessing) {
                                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White)
                            } else {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("اعتماد وحفظ سند الصرف وتوليد القيد المزدوج", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // 6. أرشيف وسجل سندات الصرف المسجلة مسبقاً
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.toggleHistoryExpanded() },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "أرشيف وسجل سندات الصرف (${uiState.paymentVouchers.size} سند)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }

                            IconButton(
                                onClick = { viewModel.toggleHistoryExpanded() },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = if (uiState.isHistoryExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = uiState.isHistoryExpanded,
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            Column {
                                Spacer(modifier = Modifier.height(8.dp))

                                AppSearchBar(
                                    value = uiState.searchQuery,
                                    onValueChange = { viewModel.setSearchQuery(it) },
                                    placeholder = "ابحث برقم السند، البيان، أو الملاحظات..."
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                val filteredVouchers = remember(uiState.paymentVouchers, uiState.searchQuery) {
                                    val cleanQ = normalizeArabicItemSearch(uiState.searchQuery)
                                    if (cleanQ.isBlank()) uiState.paymentVouchers
                                    else uiState.paymentVouchers.filter {
                                        normalizeArabicItemSearch(it.voucherNumber).contains(cleanQ) ||
                                                normalizeArabicItemSearch(it.notes).contains(cleanQ) ||
                                                normalizeArabicItemSearch(it.transactionRef).contains(cleanQ)
                                    }
                                }

                                if (filteredVouchers.isEmpty()) {
                                    Text(
                                        text = "لا توجد سندات صرف مسجلة مطابقة للبحث.",
                                        fontSize = 11.sp,
                                        color = Color.Gray,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                } else {
                                    val sdf = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US) }
                                    filteredVouchers.take(15).forEach { voucher ->
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                                .clickable { viewModel.viewVoucherDetails(voucher) }
                                                .testTag("voucher_item_${voucher.id}")
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = voucher.voucherNumber,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 12.sp,
                                                            color = Color(0xFFBF360C)
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            text = sdf.format(Date(voucher.date)),
                                                            fontSize = 10.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    Text(
                                                        text = voucher.notes,
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }

                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = "%.2f %s".format(Locale.US, voucher.amount, uiState.baseCurrencySymbol),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.error
                                                    )

                                                    IconButton(
                                                        onClick = { viewModel.requestEditVoucher(voucher) },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
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
    }

    // --- نافذة الحوار الاختيار السريع للحساب الفرعي المستفيد ---
    if (showBeneficiaryAccountPicker) {
        AlertDialog(
            onDismissRequest = { showBeneficiaryAccountPicker = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountBalance, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("اختر الطرف المستفيد (الحساب الفرعي)", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                var searchQ by remember { mutableStateOf("") }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp)
                ) {
                    AppSearchBar(
                        value = searchQ,
                        onValueChange = { searchQ = it },
                        placeholder = "ابحث بالاسم أو كود الحساب..."
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val filteredSubs = remember(uiState.beneficiarySubAccounts, searchQ) {
                        val cleanQ = normalizeArabicItemSearch(searchQ)
                        if (cleanQ.isBlank()) uiState.beneficiarySubAccounts
                        else uiState.beneficiarySubAccounts.filter {
                            normalizeArabicItemSearch(it.name).contains(cleanQ) ||
                                    normalizeArabicItemSearch(it.code).contains(cleanQ) ||
                                    normalizeArabicItemSearch(it.parentAccountName).contains(cleanQ)
                        }
                    }

                    val listScrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(listScrollState),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        filteredSubs.forEach { acc ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.selectBeneficiaryAccount(acc)
                                        showBeneficiaryAccountPicker = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(acc.name, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text("كود: ${acc.code} | الأب: ${acc.parentAccountName}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text("%.2f %s".format(Locale.US, acc.currentBalance, uiState.baseCurrencySymbol), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showBeneficiaryAccountPicker = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // --- نافذة الحوار لحفظ وترحيل القيد بنجاح ---
    if (uiState.showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissSuccessDialog() },
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0F5132)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
            },
            title = {
                Text("تم اعتماد وحفظ سند الصرف بنجاح!", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F5132))
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("رقم السند المعتمد: ${uiState.lastSavedVoucherNumber ?: "-"}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Text(
                            text = uiState.lastSavedJournalProof ?: "",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissSuccessDialog() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F5132))
                ) {
                    Text("موافق")
                }
            }
        )
    }

    // --- حوار تعديل سند الصرف ---
    if (uiState.showEditDialog && uiState.selectedVoucherForEdit != null) {
        val voucher = uiState.selectedVoucherForEdit!!
        DirectEditVoucherDialog(
            voucher = voucher,
            financialAccounts = uiState.allFinancialAccounts,
            currencySymbol = uiState.baseCurrencySymbol,
            isAdmin = currentUserRole == UserRole.ADMIN,
            onDismiss = { viewModel.dismissEditDialog() },
            onSave = { amount, method, ref, notes, img, accId ->
                viewModel.saveEditedVoucher(voucher, amount, method, ref, notes, img, accId)
            }
        )
    }

    // --- حوار إدخال PIN لمدير النظام ---
    if (uiState.showAdminPinDialog) {
        var pinInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { viewModel.dismissAdminPinDialog() },
            title = { Text("تأكيد حذف سند الصرف - رمز PIN", fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("يرجى إدخال رمز PIN لمدير النظام لتأكيد إلغاء وحذف السند:", fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { pinInput = it },
                        label = { Text("رمز PIN (مثل: 1234)", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )
                    uiState.adminPinError?.let { err ->
                        Text(err, color = MaterialTheme.colorScheme.error, fontSize = 10.sp)
                    }
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.confirmAdminPin(pinInput) }) {
                    Text("تأكيد الحذف")
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
