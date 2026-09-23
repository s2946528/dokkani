package com.example.dokkani.ui.screens.crud

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dokkani.data.local.entities.CurrencyEntity
import com.example.dokkani.data.local.entities.PartyEntity
import com.example.dokkani.data.local.entities.PartyType
import com.example.dokkani.data.local.entities.ProductEntity
import com.example.dokkani.data.local.entities.ProductUnitEntity
import com.example.dokkani.ui.components.BarcodeTextField

/**
 * حوار تأكيد الحذف عام
 */
@Composable
fun ConfirmDeleteDialog(
    title: String = "تأكيد الحذف",
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("حذف")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

/**
 * دالة مساعدة لتنقية الإدخال الرقمي واشتراط الأرقام الصحيحة
 */
private fun filterIntegerInput(input: String): String {
    val digits = input.filter { it.isDigit() }
    if (digits.isEmpty()) return ""
    val parsed = digits.toLongOrNull() ?: 0L
    return parsed.toString()
}

/**
 * مكون اختيار الوحدة من قائمة منسدلة مع إمكانية إضافة وحدة جديدة
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitDropdownSelector(
    selectedUnit: String,
    onUnitSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "اختر الوحدة",
    availableUnitsList: List<String>? = null,
    onAddNewUnitClick: (() -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    var availableUnits by remember {
        mutableStateOf(
            availableUnitsList ?: listOf("حبة/قطعة", "حبة", "كرتون", "كيلو", "درزن", "صندوق", "سحارة", "ربطة", "عبوة", "باكيت", "طرد", "جرام", "لتر", "متر", "شوال")
        )
    }
    var showAddCustomUnitDialog by remember { mutableStateOf(false) }
    var customUnitInput by remember { mutableStateOf("") }

    if (showAddCustomUnitDialog) {
        AlertDialog(
            onDismissRequest = { showAddCustomUnitDialog = false },
            title = { Text("إضافة وحدة قياس جديدة", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = customUnitInput,
                    onValueChange = { customUnitInput = it },
                    label = { Text("اسم الوحدة (مثال: طقم، بندل، سطل...)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = customUnitInput.trim()
                        if (trimmed.isNotBlank()) {
                            if (!availableUnits.contains(trimmed)) {
                                availableUnits = availableUnits + trimmed
                            }
                            onUnitSelected(trimmed)
                            customUnitInput = ""
                            showAddCustomUnitDialog = false
                        }
                    }
                ) {
                    Text("إضافة")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddCustomUnitDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedUnit,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            val listToDisplay = availableUnitsList ?: availableUnits
            listToDisplay.forEach { unit ->
                DropdownMenuItem(
                    text = { Text(unit, fontWeight = if (unit == selectedUnit) FontWeight.Bold else FontWeight.Normal) },
                    onClick = {
                        onUnitSelected(unit)
                        expanded = false
                    }
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("+ إضافة وحدة جديدة...", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                },
                onClick = {
                    expanded = false
                    if (onAddNewUnitClick != null) {
                        onAddNewUnitClick()
                    } else {
                        showAddCustomUnitDialog = true
                    }
                }
            )
        }
    }
}

/**
 * مكون اختيار العملة من قائمة منسدلة تعبأ من قاعدة البيانات مع خيار إضافة عملة جديدة في الذيل
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyDropdownSelector(
    selectedCurrency: CurrencyEntity?,
    currencies: List<CurrencyEntity>,
    onCurrencySelected: (CurrencyEntity) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "العملة *",
    onAddNewCurrencyClick: (() -> Unit)? = null,
    onSaveCurrency: ((CurrencyEntity) -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    var showAddCurrencyDialog by remember { mutableStateOf(false) }

    if (showAddCurrencyDialog && onSaveCurrency != null) {
        AddEditCurrencyDialog(
            initialCurrency = null,
            onSaveCurrency = { newCurrency ->
                onSaveCurrency(newCurrency)
                onCurrencySelected(newCurrency)
                showAddCurrencyDialog = false
            },
            onDismiss = { showAddCurrencyDialog = false }
        )
    }

    val displayValue = if (selectedCurrency != null) {
        "${selectedCurrency.name} (${selectedCurrency.symbol})"
    } else {
        "اختر العملة"
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = displayValue,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            currencies.forEach { curr ->
                DropdownMenuItem(
                    text = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${curr.name} (${curr.symbol})",
                                fontWeight = if (curr.id == selectedCurrency?.id || curr.code == selectedCurrency?.code) FontWeight.Bold else FontWeight.Normal
                            )
                            if (curr.isBaseCurrency) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "الأساسية",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    },
                    onClick = {
                        onCurrencySelected(curr)
                        expanded = false
                    }
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "+ إضافة عملة جديدة...",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                onClick = {
                    expanded = false
                    if (onAddNewCurrencyClick != null) {
                        onAddNewCurrencyClick()
                    } else {
                        showAddCurrencyDialog = true
                    }
                }
            )
        }
    }
}

/**
 * حوار إضافة / تعديل صنف
 */
@Composable
fun AddEditProductDialog(
    initialProduct: ProductEntity? = null,
    onSaveProduct: (ProductEntity, String, Double, Double, String, Boolean) -> Unit, // Product, baseUnitName, cost, sell, barcode, isBaseUnit
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialProduct?.name ?: "") }
    var code by remember { mutableStateOf(initialProduct?.code ?: "") }
    var category by remember { mutableStateOf(initialProduct?.category ?: "عام") }
    var englishName by remember { mutableStateOf(initialProduct?.englishName ?: "") }
    var isWeighted by remember { mutableStateOf(initialProduct?.isWeighted ?: false) }
    var minStockAlert by remember {
        mutableStateOf(initialProduct?.minStockAlert?.toLong()?.toString() ?: "5")
    }
    
    // Base unit initial values if creating new product
    var baseUnitName by remember { mutableStateOf("حبة") }
    var isBaseUnit by remember { mutableStateOf(true) }
    var costPrice by remember { mutableStateOf("0") }
    var sellingPrice by remember { mutableStateOf("0") }
    var barcode by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialProduct == null) "إضافة صنف جديد" else "تعديل بيانات الصنف",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم الصنف *") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("كود الصنف / SKU *") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("القسم / الفئة (مثال: معلبات، ألبان، خضار)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = englishName,
                    onValueChange = { englishName = it },
                    label = { Text("الاسم بالإنجليزية (اختياري)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = isWeighted,
                        onCheckedChange = { isWeighted = it }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("يباع بالوزن / الميزان (خضار وفواكه)", fontSize = 14.sp)
                }

                OutlinedTextField(
                    value = minStockAlert,
                    onValueChange = { minStockAlert = filterIntegerInput(it) },
                    label = { Text("حد إعادة الطلب للتنبيه بالنواقص") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                if (initialProduct == null) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        text = "الوحدة الخاصة للصنف:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    UnitDropdownSelector(
                        selectedUnit = baseUnitName,
                        onUnitSelected = { baseUnitName = it },
                        label = "اسم الوحدة للصنف *",
                        modifier = Modifier.fillMaxWidth()
                    )

                    // خيار تعيين كـ وحدة أساسية للصنف
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    ) {
                        Checkbox(
                            checked = isBaseUnit,
                            onCheckedChange = { isBaseUnit = it }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "تعيين كـ وحدة أساسية للصنف",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = costPrice,
                            onValueChange = { costPrice = filterIntegerInput(it) },
                            label = { Text("سعر الشراء (ر.ي)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = sellingPrice,
                            onValueChange = { sellingPrice = filterIntegerInput(it) },
                            label = { Text("سعر البيع (ر.ي)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    BarcodeTextField(
                        value = barcode,
                        onValueChange = { barcode = it },
                        label = "الباركود الخاص بالوحدة",
                        placeholder = "امسح باركود السلعة بالكاميرا أو اكتبه...",
                        onBarcodeScanned = { scannedCode ->
                            barcode = scannedCode
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && code.isNotBlank()) {
                        val prod = (initialProduct ?: ProductEntity(
                            name = name.trim(),
                            code = code.trim(),
                            category = category.ifBlank { "عام" }.trim(),
                            englishName = englishName.trim(),
                            isWeighted = isWeighted,
                            minStockAlert = minStockAlert.toDoubleOrNull() ?: 5.0
                        )).copy(
                            name = name.trim(),
                            code = code.trim(),
                            category = category.ifBlank { "عام" }.trim(),
                            englishName = englishName.trim(),
                            isWeighted = isWeighted,
                            minStockAlert = minStockAlert.toDoubleOrNull() ?: 5.0
                        )
                        onSaveProduct(
                            prod,
                            baseUnitName.ifBlank { "حبة" }.trim(),
                            costPrice.toDoubleOrNull() ?: 0.0,
                            sellingPrice.toDoubleOrNull() ?: 0.0,
                            barcode.trim(),
                            isBaseUnit
                        )
                    }
                }
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

/**
 * حوار إضافة / تعديل وحدة عامة
 */
@Composable
fun AddEditUnitDialog(
    productId: Long = 0L,
    initialUnit: ProductUnitEntity? = null,
    onSaveUnit: (ProductUnitEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var unitName by remember { mutableStateOf(initialUnit?.unitName ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialUnit == null) "إضافة وحدة جديدة" else "تعديل اسم الوحدة",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = unitName,
                    onValueChange = { unitName = it },
                    label = { Text("اسم الوحدة *") },
                    placeholder = { Text("مثال: حبة، كرتون، كيلو، درزن، صندوق...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (unitName.isNotBlank()) {
                        val unitToSave = initialUnit?.copy(
                            unitName = unitName.trim()
                        ) ?: ProductUnitEntity(
                            id = 0L,
                            productId = productId,
                            unitName = unitName.trim(),
                            conversionFactor = 1.0,
                            isBaseUnit = false,
                            costPrice = 0.0,
                            sellingPrice = 0.0,
                            barcode = ""
                        )
                        onSaveUnit(unitToSave)
                    }
                },
                enabled = unitName.isNotBlank(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(if (initialUnit == null) "حفظ الوحدة" else "تحديث")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) {
                Text("إلغاء")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

/**
 * حوار إضافة / تعديل طرف (عميل / مورد)
 */
@Composable
fun AddEditPartyDialog(
    initialParty: PartyEntity? = null,
    defaultPartyType: PartyType = PartyType.CUSTOMER,
    onSaveParty: (PartyEntity) -> Unit,
    onDismiss: () -> Unit,
    currencySymbol: String = "ر.ي"
) {
    var name by remember { mutableStateOf(initialParty?.name ?: "") }
    var phone by remember { mutableStateOf(initialParty?.phone ?: "") }
    var partyType by remember { mutableStateOf(initialParty?.type ?: defaultPartyType) }
    var taxNumber by remember { mutableStateOf(initialParty?.taxNumber ?: "") }
    var creditLimit by remember { mutableStateOf(initialParty?.creditLimit?.toString() ?: "1000.0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialParty == null) "إضافة عميل / مورد جديد" else "تعديل بيانات الحساب",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("الاسم بالكامل *") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("رقم الهاتف / الواتساب") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                Text("نوع الحساب:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = partyType == PartyType.CUSTOMER,
                        onClick = { partyType = PartyType.CUSTOMER },
                        label = { Text("عميل") }
                    )
                    FilterChip(
                        selected = partyType == PartyType.SUPPLIER,
                        onClick = { partyType = PartyType.SUPPLIER },
                        label = { Text("مورد") }
                    )
                    FilterChip(
                        selected = partyType == PartyType.BOTH,
                        onClick = { partyType = PartyType.BOTH },
                        label = { Text("عميل ومورد") }
                    )
                }

                OutlinedTextField(
                    value = creditLimit,
                    onValueChange = { creditLimit = it },
                    label = { Text("سقف الدين المسموح به (${currencySymbol})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = taxNumber,
                    onValueChange = { taxNumber = it },
                    label = { Text("الرقم الضريبي (إن وجد)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val party = (initialParty ?: PartyEntity(
                            name = name.trim(),
                            phone = phone.trim(),
                            type = partyType,
                            taxNumber = taxNumber.trim(),
                            creditLimit = creditLimit.toDoubleOrNull() ?: 1000.0
                        )).copy(
                            name = name.trim(),
                            phone = phone.trim(),
                            type = partyType,
                            taxNumber = taxNumber.trim(),
                            creditLimit = creditLimit.toDoubleOrNull() ?: 1000.0
                        )
                        onSaveParty(party)
                    }
                }
            ) {
                Text("حفظ البيانات")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

/**
 * حوار إضافة / تعديل عملة
 */
@Composable
fun AddEditCurrencyDialog(
    initialCurrency: CurrencyEntity? = null,
    onSaveCurrency: (CurrencyEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var code by remember { mutableStateOf(initialCurrency?.code ?: "") }
    var name by remember { mutableStateOf(initialCurrency?.name ?: "") }
    var symbol by remember { mutableStateOf(initialCurrency?.symbol ?: "") }
    var exchangeRate by remember { mutableStateOf(initialCurrency?.exchangeRateToBase?.toString() ?: "1.0") }
    val isBaseCurrency = initialCurrency?.isBaseCurrency ?: false
    var isDefault by remember { mutableStateOf(initialCurrency?.isDefault ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialCurrency == null) "إضافة عملة جديدة" else "تعديل بيانات العملة",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم العملة (مثال: ريال يمني، دولار، دينار) *") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it.uppercase() },
                        label = { Text("رمز ISO (Code)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = symbol,
                        onValueChange = { symbol = it },
                        label = { Text("الرمز المختصر") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                OutlinedTextField(
                    value = if (isBaseCurrency) "1.0" else exchangeRate,
                    onValueChange = { if (!isBaseCurrency) exchangeRate = it },
                    enabled = !isBaseCurrency,
                    label = { Text(if (isBaseCurrency) "سعر الصرف (العملة الأساسية = 1.0 ثابتاً)" else "سعر الصرف مقابل العملة الأساسية") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                if (isBaseCurrency) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "هذه هي العملة الأساسية المعتمدة للنظام (تغيير العملة الأساسية متاح حصرياً في معالج التهيئة الأولي).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isDefault || isBaseCurrency,
                        onCheckedChange = { if (!isBaseCurrency) isDefault = it },
                        enabled = !isBaseCurrency
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("العملة الافتراضية للمعاملات والفواتير", fontSize = 14.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val finalRate = if (isBaseCurrency) 1.0 else (exchangeRate.toDoubleOrNull() ?: 1.0)
                        val currency = (initialCurrency ?: CurrencyEntity(
                            code = code.ifBlank { name.take(3) }.trim().uppercase(),
                            name = name.trim(),
                            symbol = symbol.ifBlank { code }.trim(),
                            exchangeRateToBase = finalRate,
                            isBaseCurrency = isBaseCurrency,
                            isDefault = isDefault || isBaseCurrency
                        )).copy(
                            code = code.ifBlank { name.take(3) }.trim().uppercase(),
                            name = name.trim(),
                            symbol = symbol.ifBlank { code }.trim(),
                            exchangeRateToBase = finalRate,
                            isBaseCurrency = isBaseCurrency,
                            isDefault = isDefault || isBaseCurrency
                        )
                        onSaveCurrency(currency)
                    }
                },
                enabled = name.isNotBlank(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(if (initialCurrency == null) "حفظ العملة" else "تحديث")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}
