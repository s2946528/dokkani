package com.example.dokkani.ui.screens.currency

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyExchangeRatesScreen(
    currencies: List<CurrencyEntity>,
    settings: SystemSettingsEntity?,
    currentUserRole: UserRole = UserRole.ADMIN,
    onSaveCurrency: (CurrencyEntity) -> Unit,
    onSetBaseCurrency: (Long) -> Unit,
    onDeleteCurrency: (CurrencyEntity) -> Unit,
    onUpdateForeignPricingMode: (ForeignCurrencyPricingMode) -> Unit,
    onUpdateEnableDailyExchangePrompt: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val isAdmin = currentUserRole == UserRole.ADMIN
    val baseCurrency = remember(currencies) {
        currencies.find { it.isBaseCurrency } ?: currencies.firstOrNull() ?: CurrencyEntity(code = "YER", name = "ريال يمني", symbol = "ر.ي", exchangeRateToBase = 1.0, isBaseCurrency = true)
    }

    var editedRates by remember(currencies) {
        mutableStateOf(currencies.associate { it.id to (it.exchangeRateToBase.toLong().toString().takeIf { s -> s != "0" } ?: "1") })
    }

    var showAddCurrencyDialog by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    if (showAddCurrencyDialog) {
        AddCurrencyDialog(
            baseCurrencySymbol = baseCurrency.symbol,
            onSave = { newCurr ->
                onSaveCurrency(newCurr)
                showAddCurrencyDialog = false
                feedbackMessage = "تم إضافة العملة (${newCurr.name}) بنجاح"
            },
            onDismiss = { showAddCurrencyDialog = false }
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ترويسة الشاشة
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth().testTag("exchange_rates_header_card")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.CurrencyExchange,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "إدارة العملات وأسعار الصرف اليومية",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "العملة الأساسية للنظام: ${baseCurrency.name} (${baseCurrency.symbol})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (isAdmin) {
                            Button(
                                onClick = { showAddCurrencyDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("إضافة عملة")
                            }
                        }
                    }
                }
            }

            // خيارات وإعدادات حساب الصرف بالجمهورية اليمنية
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth().testTag("currency_pricing_options_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
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

            // قائمة أسعار الصرف اليومية للعملات
            item {
                Text(
                    text = "أسعار الصرف الحالية بالنسبة للعملة الأساسية (${baseCurrency.symbol}):",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            items(currencies, key = { it.id }) { currency ->
                val currentInput = editedRates[currency.id] ?: currency.exchangeRateToBase.toLong().toString()
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, if (currency.isBaseCurrency) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth().testTag("currency_item_${currency.code}")
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
                                if (currency.isBaseCurrency) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(start = 4.dp)
                                    ) {
                                        Text(
                                            text = "العملة الأساسية",
                                            fontSize = 10.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            if (!currency.isBaseCurrency) {
                                Text(
                                    text = "1 ${currency.code} = $currentInput ${baseCurrency.symbol}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (!currency.isBaseCurrency) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                            feedbackMessage = "تم حفظ سعر صرف ${currency.name} ($rateVal ${baseCurrency.symbol})"
                                        }
                                    ) {
                                        Icon(Icons.Default.Save, contentDescription = "حفظ", tint = MaterialTheme.colorScheme.primary)
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
fun AddCurrencyDialog(
    baseCurrencySymbol: String,
    onSave: (CurrencyEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var symbol by remember { mutableStateOf("") }
    var rateInput by remember { mutableStateOf("1") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "إضافة عملة جديدة للنظام", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم العملة (مثال: درهم إماراتي)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase() },
                    label = { Text("كود العملة (مثال: AED)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = symbol,
                    onValueChange = { symbol = it },
                    label = { Text("رمز العملة للعرض (مثال: د.إ)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = rateInput,
                    onValueChange = { rateInput = it.filter { c -> c.isDigit() } },
                    label = { Text("سعر الصرف مقابل العملة الأساسية ($baseCurrencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && code.isNotBlank()) {
                        val rate = rateInput.toDoubleOrNull() ?: 1.0
                        onSave(
                            CurrencyEntity(
                                name = name.trim(),
                                code = code.trim().uppercase(),
                                symbol = symbol.ifBlank { code }.trim(),
                                exchangeRateToBase = rate,
                                isBaseCurrency = false,
                                isDefault = false
                            )
                        )
                    }
                }
            ) {
                Text("حفظ العملة")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
