package com.example.dokkani.ui.screens.currency

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dokkani.data.local.entities.CurrencyEntity
import com.example.dokkani.data.local.entities.ForeignCurrencyPricingMode
import com.example.dokkani.data.local.entities.SystemSettingsEntity
import com.example.dokkani.data.local.entities.UserRole
import com.example.dokkani.ui.screens.crud.AddEditCurrencyDialog
import com.example.dokkani.ui.screens.crud.ConfirmDeleteDialog

/**
 * واجهة إدارة العملات وأسعار الصرف بنظام التبويبات
 * 1. تبويب "العملات" (عرض، إضافة، تعديل، حذف العملات مع تثبيت العملة الأساسية)
 * 2. تبويب "أسعار الصرف" (إدارة أسعار الصرف، تفعيل التحديث اليومي، وسيارات التسعير للأصناف الأجنبية)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyExchangeRatesScreen(
    currencies: List<CurrencyEntity>,
    settings: SystemSettingsEntity?,
    currentUserRole: UserRole = UserRole.ADMIN,
    onSaveCurrency: (CurrencyEntity) -> Unit,
    onSetBaseCurrency: (Long) -> Unit = {},
    onDeleteCurrency: (CurrencyEntity) -> Unit,
    onUpdateForeignPricingMode: (ForeignCurrencyPricingMode) -> Unit,
    onUpdateEnableDailyExchangePrompt: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val isAdmin = currentUserRole == UserRole.ADMIN
    var selectedTab by remember { mutableIntStateOf(0) } // 0: العملات, 1: أسعار الصرف

    // تحديد العملة الأساسية للنظام
    val baseCurrency = remember(currencies) {
        currencies.find { it.isBaseCurrency }
            ?: currencies.firstOrNull()
            ?: CurrencyEntity(code = "YER", name = "ريال يمني", symbol = "ر.ي", exchangeRateToBase = 1.0, isBaseCurrency = true)
    }

    Column(modifier = modifier.fillMaxSize()) {
        // --- نظام التبويبات الرئيسي ---
        PrimaryTabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                modifier = Modifier.testTag("currencies_tab"),
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "إدارة العملات",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                modifier = Modifier.testTag("exchange_rates_tab"),
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CurrencyExchange,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "أسعار الصرف والسياسات",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            )
        }

        // --- محتوى التبويبات ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (selectedTab) {
                0 -> CurrenciesManagementTab(
                    currencies = currencies,
                    baseCurrency = baseCurrency,
                    isAdmin = isAdmin,
                    onSaveCurrency = onSaveCurrency,
                    onDeleteCurrency = onDeleteCurrency
                )

                1 -> ExchangeRatesAndPoliciesTab(
                    currencies = currencies,
                    baseCurrency = baseCurrency,
                    settings = settings,
                    isAdmin = isAdmin,
                    onSaveCurrency = onSaveCurrency,
                    onUpdateForeignPricingMode = onUpdateForeignPricingMode,
                    onUpdateEnableDailyExchangePrompt = onUpdateEnableDailyExchangePrompt
                )
            }
        }
    }
}

/**
 * التبويب الأول: إدارة العملات (عرض، إضافة، تعديل، وحذف العملات)
 */
@Composable
private fun CurrenciesManagementTab(
    currencies: List<CurrencyEntity>,
    baseCurrency: CurrencyEntity,
    isAdmin: Boolean,
    onSaveCurrency: (CurrencyEntity) -> Unit,
    onDeleteCurrency: (CurrencyEntity) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingCurrency by remember { mutableStateOf<CurrencyEntity?>(null) }
    var deletingCurrency by remember { mutableStateOf<CurrencyEntity?>(null) }

    // تصفية القائمة بالبحث
    val filteredCurrencies = remember(currencies, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) {
            currencies
        } else {
            currencies.filter {
                it.name.lowercase().contains(q) ||
                        it.code.lowercase().contains(q) ||
                        it.symbol.lowercase().contains(q)
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // بطاقة التعريفية بالهيدر
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Payments,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "إدارة العملات المعتمدة",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "العملة الأساسية للنظام: ${baseCurrency.name} (${baseCurrency.symbol})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }

                        if (isAdmin) {
                            Button(
                                onClick = { showAddDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("add_currency_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("إضافة عملة", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // حقل البحث
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("بحث باسم العملة، الرمز، أو ISO Code...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )
        }

        // عنوان نتائج العملات
        item {
            Text(
                text = "قائمة العملات المسجلة (${filteredCurrencies.size}):",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }

        // قائمة عرض العملات المحفوظة
        if (filteredCurrencies.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isBlank()) "لا توجد عملات محفوظة" else "لا توجد عملات تطابق البحث",
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            items(
                items = filteredCurrencies,
                key = { it.id }
            ) { currency ->
                CurrencyCardItem(
                    currency = currency,
                    baseCurrency = baseCurrency,
                    isAdmin = isAdmin,
                    onEdit = { editingCurrency = currency },
                    onDelete = { deletingCurrency = currency }
                )
            }
        }
    }

    // نافذة إضافة عملة جديدة
    if (showAddDialog) {
        AddEditCurrencyDialog(
            initialCurrency = null,
            onSaveCurrency = { newCurr ->
                onSaveCurrency(newCurr)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    // نافذة تعديل بيانات العملة
    if (editingCurrency != null) {
        AddEditCurrencyDialog(
            initialCurrency = editingCurrency,
            onSaveCurrency = { updatedCurr ->
                onSaveCurrency(updatedCurr)
                editingCurrency = null
            },
            onDismiss = { editingCurrency = null }
        )
    }

    // نافذة تأكيد الحذف
    if (deletingCurrency != null) {
        ConfirmDeleteDialog(
            message = "هل أنت متأكد من رغبتك في حذف العملة '${deletingCurrency?.name}' (${deletingCurrency?.symbol})؟",
            onConfirm = {
                deletingCurrency?.let { onDeleteCurrency(it) }
                deletingCurrency = null
            },
            onDismiss = { deletingCurrency = null }
        )
    }
}

/**
 * بطاقة عرض بيانات العملة في القائمة
 */
@Composable
private fun CurrencyCardItem(
    currency: CurrencyEntity,
    baseCurrency: CurrencyEntity,
    isAdmin: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.dp,
            if (currency.isBaseCurrency) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("currency_card_${currency.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (currency.isBaseCurrency) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = currency.symbol.ifBlank { currency.code },
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (currency.isBaseCurrency) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                    )
                }

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = currency.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(${currency.code})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    if (currency.isBaseCurrency) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = "العملة الأساسية ثابته",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else {
                        Text(
                            text = "سعر الصرف: 1 ${currency.code} = ${currency.exchangeRateToBase} ${baseCurrency.symbol}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (isAdmin) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("edit_currency_${currency.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "تعديل العملة",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (!currency.isBaseCurrency) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("delete_currency_${currency.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "حذف العملة",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * التبويب الثاني: أسعار الصرف والسياسات (سياسة تسعير الأصناف الأجنبية، التحديث اليومي، وإدارة أسعار الصرف)
 */
@Composable
private fun ExchangeRatesAndPoliciesTab(
    currencies: List<CurrencyEntity>,
    baseCurrency: CurrencyEntity,
    settings: SystemSettingsEntity?,
    isAdmin: Boolean,
    onSaveCurrency: (CurrencyEntity) -> Unit,
    onUpdateForeignPricingMode: (ForeignCurrencyPricingMode) -> Unit,
    onUpdateEnableDailyExchangePrompt: (Boolean) -> Unit
) {
    val secondaryCurrencies = remember(currencies) { currencies.filter { !it.isBaseCurrency } }
    var editedRates by remember(currencies) {
        mutableStateOf(currencies.associate { it.id to (it.exchangeRateToBase.toLong().toString().takeIf { s -> s != "0" } ?: "1") })
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // بطاقة خيارات وإعدادات حساب الصرف
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("currency_pricing_options_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "سياسة احتساب أسعار الصرف والأصناف الأجنبية:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    HorizontalDivider()

                    Text(
                        text = "1. آلية احتساب سعر الأصناف المسعرة بالعملة الأجنبية عند البيع:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )

                    ForeignCurrencyPricingMode.values().forEach { mode ->
                        val isSelected = (settings?.foreignCurrencyPricingMode ?: ForeignCurrencyPricingMode.SALE_DATE) == mode
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent,
                            border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { if (isAdmin) onUpdateForeignPricingMode(mode) },
                                    enabled = isAdmin
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(text = mode.labelArabic, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(text = mode.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "تحديث الصرف اليومي عند التشغيل:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "جعل شاشة التنبيه لتحديث الصرف اليومي الخيار الافتراضي عند إطلاق التطبيق",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings?.enableDailyExchangeRatePrompt ?: true,
                            onCheckedChange = { if (isAdmin) onUpdateEnableDailyExchangePrompt(it) },
                            enabled = isAdmin
                        )
                    }
                }
            }
        }

        // إدارة أسعار الصرف الحالية للعملات الفرعية
        item {
            Text(
                text = "أسعار الصرف الحالية للعملات الفرعية مقابل العملة الأساسية (${baseCurrency.symbol}):",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }

        if (secondaryCurrencies.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لا توجد عملات فرعية مضافة حتى الآن (يمكنك إضافة عملات من تبويب 'إدارة العملات').",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            items(secondaryCurrencies, key = { it.id }) { currency ->
                val currentInput = editedRates[currency.id] ?: currency.exchangeRateToBase.toLong().toString()
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("exchange_rate_item_${currency.code}")
                ) {
                    Row(
                        modifier = Modifier
                            .padding(14.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${currency.name} (${currency.code})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                ) {
                                    Text(
                                        text = currency.symbol,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Text(
                                text = "1 ${currency.code} = $currentInput ${baseCurrency.symbol}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = currentInput,
                                onValueChange = { newVal ->
                                    val digitsOnly = newVal.filter { it.isDigit() }
                                    editedRates = editedRates + (currency.id to digitsOnly)
                                },
                                label = { Text("سعر الصرف (${baseCurrency.symbol})") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                enabled = isAdmin,
                                modifier = Modifier.width(140.dp)
                            )

                            if (isAdmin) {
                                IconButton(
                                    onClick = {
                                        val rateVal = currentInput.toDoubleOrNull() ?: currency.exchangeRateToBase
                                        onSaveCurrency(currency.copy(exchangeRateToBase = rateVal))
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Save,
                                        contentDescription = "حفظ سعر الصرف",
                                        tint = MaterialTheme.colorScheme.primary
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
