package com.example.dokkani.ui.screens.assets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dokkani.data.local.entities.CurrencyEntity
import com.example.dokkani.data.local.entities.FinancialAccountEntity
import com.example.dokkani.data.local.entities.SystemSettingsEntity
import com.example.dokkani.domain.assets.EquityCalculationResult

/**
 * شاشة ومكون إستراتيجي لإدارة وتعديل رأس المال الافتتاحي (Opening Capital Management Screen)
 * يضمن إدخال وتعديل رأس المال الافتتاحي مع توليد القيود الافتتاحية تلقائياً
 * وانعكاسها في ميزان المراجعة والميزانية العمومية دون التأثير على الحركات التشغيلية.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpeningCapitalManagementScreen(
    currentSettings: SystemSettingsEntity?,
    equityResult: EquityCalculationResult?,
    availableAccounts: List<FinancialAccountEntity>,
    availableCurrencies: List<CurrencyEntity>,
    amountInput: String,
    currencyInput: String,
    accountCodeInput: String,
    notesInput: String,
    onInputsChanged: (amount: String, currency: String, accountCode: String, notes: String) -> Unit,
    onSaveOpeningCapital: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    var showConfirmDialog by remember { mutableStateOf(false) }
    var currencyDropdownExpanded by remember { mutableStateOf(false) }
    var accountDropdownExpanded by remember { mutableStateOf(false) }

    val numericAmount = amountInput.toDoubleOrNull() ?: 0.0
    val isValidAmount = numericAmount > 0.0

    // الحساب المالي المحدد حالياً في دليل الحسابات
    val selectedAccount = availableAccounts.firstOrNull { it.code == accountCodeInput }
    val selectedAccountName = selectedAccount?.name ?: "30100 - رأس المال الافتتاحي الثابت"

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "إدارة وتعديل رأس المال الافتتاحي",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "توليد القيد الافتتاحي ورصد حقوق الملكية بـ ميزان المراجعة",
                                fontSize = 11.sp,
                                color = if (isDark) Color(0xFFCBD5E1) else Color.LightGray
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = if (isDark) Color(0xFF0F291E) else Color(0xFF133E32),
                        titleContentColor = Color.White
                    )
                )
            },
            modifier = modifier
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ==========================================
                // 1. بطاقات المؤشرات الحالية لرأس المال الافتتاحي
                // ==========================================
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)
                    ),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = Color(0xFF15803D),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "الوضع الراهن لرأس المال وحقوق الملكية",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "رأس المال الافتتاحي المسجل:",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${"%.2f".format(currentSettings?.initialCapital ?: 0.0)} $currencyInput",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E3A8A)
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "رأس المال الآلي المحسوب من الأصول:",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${"%.2f".format(equityResult?.calculatedInitialCapital ?: 0.0)} $currencyInput",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF15803D)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFE8F5E9),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF15803D), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "الحساب المرتبط بدليل الحسابات: $selectedAccountName",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF15803D)
                                )
                            }
                        }
                    }
                }

                // ==========================================
                // 2. نموذج إدخال وتعديل بيانات رأس المال الافتتاحي
                // ==========================================
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "بيانات القيد الافتتاحي ورأس المال الجديد",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // حقل إدخال مبلغ رأس المال الافتتاحي رقمي مدقق
                        OutlinedTextField(
                            value = amountInput,
                            onValueChange = { input ->
                                // تصفية المدخلات للأرقام والنقطة العشرية فقط
                                val filtered = input.filter { it.isDigit() || it == '.' }
                                onInputsChanged(filtered, currencyInput, accountCodeInput, notesInput)
                            },
                            label = { Text("قيمة رأس المال الافتتاحي (الرقم المالي المعتمد)") },
                            placeholder = { Text("مثال: 50000.00") },
                            leadingIcon = { Icon(Icons.Default.Calculate, contentDescription = null, tint = Color(0xFF15803D)) },
                            trailingIcon = { Text(currencyInput, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 12.dp)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            isError = amountInput.isNotEmpty() && !isValidAmount,
                            supportingText = {
                                if (amountInput.isNotEmpty() && !isValidAmount) {
                                    Text("يجب إدخال قيمة مالية أكبر من الصفر (0)", color = MaterialTheme.colorScheme.error)
                                } else {
                                    Text("أدخل القيمة الحقيقية لتأسيس رأس مال المنشأة")
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_opening_capital_amount")
                        )

                        // اختيار العملة الرئيسية للمشروع
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = currencyInput,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("العملة الرئيسية المعتمدة لرأس المال") },
                                leadingIcon = { Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = Color(0xFF15803D)) },
                                trailingIcon = {
                                    IconButton(onClick = { currencyDropdownExpanded = true }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "اختيار العملة")
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { currencyDropdownExpanded = true }
                                    .testTag("dropdown_capital_currency")
                            )

                            DropdownMenu(
                                expanded = currencyDropdownExpanded,
                                onDismissRequest = { currencyDropdownExpanded = false }
                            ) {
                                val currenciesList = availableCurrencies.ifEmpty {
                                    listOf(
                                        CurrencyEntity(code = "YER", name = "ريال يمني", symbol = "ر.ي", exchangeRateToBase = 1.0, isBaseCurrency = true),
                                        CurrencyEntity(code = "SAR", name = "ريال سعودي", symbol = "ر.س", exchangeRateToBase = 0.0024, isBaseCurrency = false),
                                        CurrencyEntity(code = "USD", name = "دولار أمريكي", symbol = "$", exchangeRateToBase = 0.00063, isBaseCurrency = false)
                                    )
                                }
                                currenciesList.forEach { curr ->
                                    DropdownMenuItem(
                                        text = { Text("${curr.name} (${curr.code} / ${curr.symbol})", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                        onClick = {
                                            onInputsChanged(amountInput, curr.code, accountCodeInput, notesInput)
                                            currencyDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // اختيار أو توثيق حساب حقوق الملكية والمرتبط بدليل الحسابات
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = selectedAccountName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("الحساب المالي المرتبط بحقوق الملكية (دليل الحسابات)") },
                                leadingIcon = { Icon(Icons.Default.AccountTree, contentDescription = null, tint = Color(0xFF15803D)) },
                                trailingIcon = {
                                    IconButton(onClick = { accountDropdownExpanded = true }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "اختيار الحساب")
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { accountDropdownExpanded = true }
                                    .testTag("dropdown_capital_account")
                            )

                            DropdownMenu(
                                expanded = accountDropdownExpanded,
                                onDismissRequest = { accountDropdownExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("30100 - رأس المال الافتتاحي الثابت (افتراضي)", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                    onClick = {
                                        onInputsChanged(amountInput, currencyInput, "30100", notesInput)
                                        accountDropdownExpanded = false
                                    }
                                )
                                availableAccounts.filter { it.code.startsWith("30") || it.parentAccountCode == "301" }.forEach { acc ->
                                    DropdownMenuItem(
                                        text = { Text("${acc.code} - ${acc.name}", fontSize = 12.sp) },
                                        onClick = {
                                            onInputsChanged(amountInput, currencyInput, acc.code, notesInput)
                                            accountDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // حقل ملاحظات وبيان القيد الافتتاحي
                        OutlinedTextField(
                            value = notesInput,
                            onValueChange = { onInputsChanged(amountInput, currencyInput, accountCodeInput, it) },
                            label = { Text("ملاحظات وبيان القيد الافتتاحي") },
                            placeholder = { Text("مثال: القيد الافتتاحي المعتمد لتأسيس ورصد رأس المال") },
                            leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null, tint = Color(0xFF64748B)) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_opening_capital_notes")
                        )
                    }
                }

                // ==========================================
                // 3. المعاينة المحاسبية التلقائية للقيد الافتتاحي (Journal Entry Preview)
                // ==========================================
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                    border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = Color(0xFF047857))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "معاينة القيد الافتتاحي والتوليد الآلي لميزان المراجعة",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF065F46)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // الطرف المدين
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "الطرف المدين (Debit): أصول التأسيس (نقدية/بنك/مخزون/أصول)",
                                fontSize = 11.sp,
                                color = Color(0xFF047857)
                            )
                            Text(
                                text = "${"%.2f".format(numericAmount)} $currencyInput",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF047857)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // الطرف الدائن
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "الطرف الدائن (Credit): $selectedAccountName",
                                fontSize = 11.sp,
                                color = Color(0xFF047857)
                            )
                            Text(
                                text = "${"%.2f".format(numericAmount)} $currencyInput",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF047857)
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFA7F3D0))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF047857), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "القيد الافتتاحي متوازن 100% وينعكس فوراً بالميزانية وميزان المراجعة ✓",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF065F46)
                            )
                        }
                    }
                }

                // ==========================================
                // 4. أزرار الحفظ والإلغاء مع نظام التحقق Validation Check
                // ==========================================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onNavigateBack,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("إلغاء / عودة")
                    }

                    Button(
                        onClick = {
                            if (isValidAmount) {
                                showConfirmDialog = true
                            }
                        },
                        enabled = isValidAmount,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D)),
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("btn_save_opening_capital")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("حفظ وتوليد القيد الافتتاحي", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // ==========================================
        // 5. حوار التوكيد المحاسبي قبل الحفظ النهائي
        // ==========================================
        if (showConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showConfirmDialog = false },
                icon = { Icon(Icons.Default.AccountBalance, contentDescription = null, tint = Color(0xFF15803D)) },
                title = { Text("تأكيد اعتماد رأس المال والقيد الافتتاحي", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("هل أنت أُكيد من اعتماد القيد الافتتاحي ورأس المال بمبلغ:")
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFE8F5E9),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "${"%.2f".format(numericAmount)} $currencyInput",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color(0xFF15803D),
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                        Text(
                            text = "سيتم تحديث إعدادات النظام، وتحديث حساب حقوق الملكية ($selectedAccountName)، وتحديث ميزان المراجعة والميزانية العمومية فوراً.",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showConfirmDialog = false
                            onSaveOpeningCapital()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D))
                    ) {
                        Text("تأكيد واعتماد القيد")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmDialog = false }) {
                        Text("إلغاء")
                    }
                }
            )
        }
    }
}

/**
 * حوار معالج رأس المال الافتتاحي (Opening Capital Management Dialog)
 * يمكن استدعاؤه بمرونة كـ Dialog فوق شاشة الأصول وحقوق الملكية
 */
@Composable
fun OpeningCapitalManagementDialog(
    currentSettings: SystemSettingsEntity?,
    equityResult: EquityCalculationResult?,
    availableAccounts: List<FinancialAccountEntity>,
    availableCurrencies: List<CurrencyEntity>,
    amountInput: String,
    currencyInput: String,
    accountCodeInput: String,
    notesInput: String,
    onInputsChanged: (amount: String, currency: String, accountCode: String, notes: String) -> Unit,
    onSaveOpeningCapital: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {},
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 620.dp)
            ) {
                OpeningCapitalManagementScreen(
                    currentSettings = currentSettings,
                    equityResult = equityResult,
                    availableAccounts = availableAccounts,
                    availableCurrencies = availableCurrencies,
                    amountInput = amountInput,
                    currencyInput = currencyInput,
                    accountCodeInput = accountCodeInput,
                    notesInput = notesInput,
                    onInputsChanged = onInputsChanged,
                    onSaveOpeningCapital = onSaveOpeningCapital,
                    onNavigateBack = onDismiss
                )
            }
        }
    )
}
