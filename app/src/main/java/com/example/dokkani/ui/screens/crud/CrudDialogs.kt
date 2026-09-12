package com.example.dokkani.ui.screens.crud

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
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
 * حوار إضافة / تعديل صنف
 */
@Composable
fun AddEditProductDialog(
    initialProduct: ProductEntity? = null,
    onSaveProduct: (ProductEntity, String, Double, Double, String) -> Unit, // Product, baseUnitName, cost, sell, barcode
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialProduct?.name ?: "") }
    var code by remember { mutableStateOf(initialProduct?.code ?: "") }
    var category by remember { mutableStateOf(initialProduct?.category ?: "عام") }
    var englishName by remember { mutableStateOf(initialProduct?.englishName ?: "") }
    var isWeighted by remember { mutableStateOf(initialProduct?.isWeighted ?: false) }
    var minStockAlert by remember { mutableStateOf(initialProduct?.minStockAlert?.toString() ?: "5.0") }
    
    // Base unit initial values if creating new product
    var baseUnitName by remember { mutableStateOf("حبة") }
    var costPrice by remember { mutableStateOf("0.0") }
    var sellingPrice by remember { mutableStateOf("0.0") }
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
                    onValueChange = { minStockAlert = it },
                    label = { Text("حد إعادة الطلب للتنبيه بالنواقص") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                if (initialProduct == null) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        text = "بيانات الوحدة الأساسية للصنف:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = baseUnitName,
                        onValueChange = { baseUnitName = it },
                        label = { Text("اسم الوحدة الأساسية (حبة، كيلو، كرتون)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = costPrice,
                            onValueChange = { costPrice = it },
                            label = { Text("سعر الشراء") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = sellingPrice,
                            onValueChange = { sellingPrice = it },
                            label = { Text("سعر البيع") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    OutlinedTextField(
                        value = barcode,
                        onValueChange = { barcode = it },
                        label = { Text("الباركود الخاص بالوحدة") },
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
                            barcode.trim()
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
 * حوار إضافة / تعديل وحدة لصنف
 */
@Composable
fun AddEditUnitDialog(
    productId: Long,
    initialUnit: ProductUnitEntity? = null,
    onSaveUnit: (ProductUnitEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var unitName by remember { mutableStateOf(initialUnit?.unitName ?: "درزن") }
    var conversionFactor by remember { mutableStateOf(initialUnit?.conversionFactor?.toString() ?: "12.0") }
    var barcode by remember { mutableStateOf(initialUnit?.barcode ?: "") }
    var costPrice by remember { mutableStateOf(initialUnit?.costPrice?.toString() ?: "0.0") }
    var sellingPrice by remember { mutableStateOf(initialUnit?.sellingPrice?.toString() ?: "0.0") }
    var isBaseUnit by remember { mutableStateOf(initialUnit?.isBaseUnit ?: false) }

    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialUnit == null) "إضافة وحدة جديدة للصنف" else "تعديل الوحدة",
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
                    value = unitName,
                    onValueChange = { unitName = it },
                    label = { Text("اسم الوحدة (كرتون، درزن، صندوق...) *") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = conversionFactor,
                    onValueChange = { conversionFactor = it },
                    label = { Text("معامل التحويل للوحدة الأساسية (مثال: كرتون = 24)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = barcode,
                    onValueChange = { barcode = it },
                    label = { Text("باركود الوحدة للماسح الضوئي") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = costPrice,
                        onValueChange = { costPrice = it },
                        label = { Text("سعر الشراء") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = sellingPrice,
                        onValueChange = { sellingPrice = it },
                        label = { Text("سعر البيع") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isBaseUnit, onCheckedChange = { isBaseUnit = it })
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تعيين كـ وحدة أساسية للصنف", fontSize = 14.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (unitName.isNotBlank()) {
                        val unit = (initialUnit ?: ProductUnitEntity(
                            productId = productId,
                            unitName = unitName.trim(),
                            conversionFactor = conversionFactor.toDoubleOrNull() ?: 1.0,
                            barcode = barcode.trim(),
                            costPrice = costPrice.toDoubleOrNull() ?: 0.0,
                            sellingPrice = sellingPrice.toDoubleOrNull() ?: 0.0,
                            isBaseUnit = isBaseUnit
                        )).copy(
                            unitName = unitName.trim(),
                            conversionFactor = conversionFactor.toDoubleOrNull() ?: 1.0,
                            barcode = barcode.trim(),
                            costPrice = costPrice.toDoubleOrNull() ?: 0.0,
                            sellingPrice = sellingPrice.toDoubleOrNull() ?: 0.0,
                            isBaseUnit = isBaseUnit
                        )
                        onSaveUnit(unit)
                    }
                }
            ) {
                Text("حفظ الوحدة")
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
 * حوار إضافة / تعديل طرف (عميل / مورد)
 */
@Composable
fun AddEditPartyDialog(
    initialParty: PartyEntity? = null,
    onSaveParty: (PartyEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialParty?.name ?: "") }
    var phone by remember { mutableStateOf(initialParty?.phone ?: "") }
    var partyType by remember { mutableStateOf(initialParty?.type ?: PartyType.CUSTOMER) }
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
                    label = { Text("سقف الدين المسموح به (ر.س)") },
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
    var code by remember { mutableStateOf(initialCurrency?.code ?: "YER") }
    var name by remember { mutableStateOf(initialCurrency?.name ?: "ريال يمني") }
    var symbol by remember { mutableStateOf(initialCurrency?.symbol ?: "ر.ي") }
    var exchangeRate by remember { mutableStateOf(initialCurrency?.exchangeRateToBase?.toString() ?: "1.0") }
    var isDefault by remember { mutableStateOf(initialCurrency?.isDefault ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialCurrency == null) "إضافة عملة جديدة" else "تعديل العملة",
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
                    label = { Text("اسم العملة (مثال: ريال يمني، دولار) *") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("رمز العملة (Code)") },
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
                    value = exchangeRate,
                    onValueChange = { exchangeRate = it },
                    label = { Text("سعر الصرف مقابل العملة الأساسية") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isDefault, onCheckedChange = { isDefault = it })
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("العملة الافتراضية للنظام", fontSize = 14.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val currency = (initialCurrency ?: CurrencyEntity(
                            code = code.trim(),
                            name = name.trim(),
                            symbol = symbol.trim(),
                            exchangeRateToBase = exchangeRate.toDoubleOrNull() ?: 1.0,
                            isDefault = isDefault
                        )).copy(
                            code = code.trim(),
                            name = name.trim(),
                            symbol = symbol.trim(),
                            exchangeRateToBase = exchangeRate.toDoubleOrNull() ?: 1.0,
                            isDefault = isDefault
                        )
                        onSaveCurrency(currency)
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
        },
        shape = RoundedCornerShape(16.dp)
    )
}
