package com.example.dokkani.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dokkani.data.local.entities.CostValuationMethod
import com.example.dokkani.data.local.entities.ProductWithUnits
import com.example.dokkani.data.local.entities.StockMovementEntity
import com.example.dokkani.domain.costing.CostCalculationResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CostingEngineScreen(
    productsWithUnits: List<ProductWithUnits> = emptyList(),
    selectedProductId: Long? = null,
    selectedMethod: CostValuationMethod = CostValuationMethod.WAC,
    costingResult: CostCalculationResult? = null,
    wacResult: CostCalculationResult? = null,
    fifoResult: CostCalculationResult? = null,
    lppResult: CostCalculationResult? = null,
    activeLots: List<StockMovementEntity> = emptyList(),
    onSelectProduct: (Long) -> Unit = {},
    onSelectMethod: (CostValuationMethod) -> Unit = {},
    onSaveMethodToSettings: (CostValuationMethod) -> Unit = {},
    onAddSimulatedPurchaseBatch: (quantity: Double, unitCost: Double) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var showAddBatchDialog by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // بطاقة العنوان والمفهوم المحاسبي
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "محرك حساب التكلفة المحاسبي (Costing Engine)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "يقوم بحساب تكلفة البضاعة بدقة استناداً إلى خيارات النظام الثلاثة (المتوسط المرجح WAC / الوارد أولاً صادر أولاً FIFO / آخر سعر شراء)، مع التحويل الرياضي للوحدات المتعددة.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                }
            }
        }

        // اختيار الصنف المراد احتساب تكلفته
        item {
            Column {
                Text(
                    text = "اختر الصنف لاختبار وتطبيق محرك التكلفة:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(productsWithUnits) { pwu ->
                        val isSelected = pwu.product.id == selectedProductId
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectProduct(pwu.product.id) },
                            label = { Text(pwu.product.name) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            modifier = Modifier.testTag("product_chip_${pwu.product.id}")
                        )
                    }
                }
            }
        }

        // بطاقة المقارنة الثلاثية المباشرة (Side-by-Side Comparison)
        item {
            Text(
                text = "مقارنة طرق التقييم الثلاث في الوقت الفعلي:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MethodComparisonCard(
                    title = "المتوسط المرجح (WAC)",
                    cost = wacResult?.unitCostBase ?: 0.0,
                    unitName = wacResult?.targetUnitName ?: "وحدة",
                    isSelected = selectedMethod == CostValuationMethod.WAC,
                    onClick = { onSelectMethod(CostValuationMethod.WAC) },
                    modifier = Modifier.weight(1f).testTag("card_method_wac")
                )
                MethodComparisonCard(
                    title = "FIFO الوارد أولاً",
                    cost = fifoResult?.unitCostBase ?: 0.0,
                    unitName = fifoResult?.targetUnitName ?: "وحدة",
                    isSelected = selectedMethod == CostValuationMethod.FIFO,
                    onClick = { onSelectMethod(CostValuationMethod.FIFO) },
                    modifier = Modifier.weight(1f).testTag("card_method_fifo")
                )
                MethodComparisonCard(
                    title = "آخر سعر شراء",
                    cost = lppResult?.unitCostBase ?: 0.0,
                    unitName = lppResult?.targetUnitName ?: "وحدة",
                    isSelected = selectedMethod == CostValuationMethod.LAST_PURCHASE_PRICE,
                    onClick = { onSelectMethod(CostValuationMethod.LAST_PURCHASE_PRICE) },
                    modifier = Modifier.weight(1f).testTag("card_method_lpp")
                )
            }
        }

        // بطاقة النتيجة المفصلة للطريقة المختارة
        item {
            costingResult?.let { res ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("costing_result_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = res.productName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "طريقة الحساب: ${res.methodUsed.labelArabic}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "%.4f ر.س".format(res.unitCostBase),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "لكل ${res.targetUnitName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // شرح المعادلة
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "القانون: ${res.formulaExplanation}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(10.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // خطوات الحساب والشفافية المحاسبية
                        Text(
                            text = "سجل خطوات الاحتساب المحاسبي التلقائي:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        for (step in res.calculationSteps) {
                            Text(
                                text = step,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // زر اعتماد هذه الطريقة لإعدادات نظام دكاني
                        Button(
                            onClick = { onSaveMethodToSettings(res.methodUsed) },
                            modifier = Modifier.fillMaxWidth().testTag("save_as_system_method_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F5132))
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("اعتماد [${res.methodUsed.labelArabic}] كإعداد رسمي لبرنامج دكاني")
                        }
                    }
                }
            }
        }

        // طبقات الشراء وتجربة إضافة دفعة جديدة
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "سجل شحنات وتوريدات المخزن (Inventory Lots):",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                OutlinedButton(
                    onClick = { showAddBatchDialog = true },
                    modifier = Modifier.testTag("add_test_batch_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("توريد دفعة جديدة")
                }
            }
        }

        if (activeLots.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "لا توجد حركات توريد سابقة لهذا الصنف، يمكنك إضافة دفعة شراء تجريبية لمشاهدة الاحتساب الفوري.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(activeLots) { lot ->
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = lot.referenceNumber ?: "توريد مخزن #${lot.id}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = dateFormat.format(Date(lot.timestamp)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            if (lot.notes.isNotBlank()) {
                                Text(
                                    text = lot.notes,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "%.2f ر.س".format(lot.unitCostPriceBase),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F5132)
                            )
                            Text(
                                text = "متبقي FIFO: ${lot.remainingQuantityForFifo} / ${lot.quantityBaseUnit}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }

    // نافذة محاكاة إضافة دفعة شراء جديدة لتجربة أثرها على الحسابات
    if (showAddBatchDialog) {
        AddSimulatedBatchDialog(
            onDismiss = { showAddBatchDialog = false },
            onConfirm = { qty, cost ->
                onAddSimulatedPurchaseBatch(qty, cost)
                showAddBatchDialog = false
            }
        )
    }
}

@Composable
private fun MethodComparisonCard(
    title: String,
    cost: Double,
    unitName: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "%.3f".format(cost),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "ر.س / $unitName",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun AddSimulatedBatchDialog(
    onDismiss: () -> Unit,
    onConfirm: (quantity: Double, unitCost: Double) -> Unit
) {
    var qtyText by remember { mutableStateOf("15.0") }
    var costText by remember { mutableStateOf("4.20") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("توريد دفعة شراء تجريبية جديدة") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "أدخل كمية وسعر الدفعة الجديدة لتشاهد مباشرة كيف تتغير قيم المتوسط المرجح (WAC) و FIFO وآخر سعر شراء في قاعدة البيانات:",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { qtyText = it },
                    label = { Text("الكمية المشتراة") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("dialog_batch_qty")
                )
                OutlinedTextField(
                    value = costText,
                    onValueChange = { costText = it },
                    label = { Text("سعر تكلفة الشراء للوحدة (ر.س)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("dialog_batch_cost")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val q = qtyText.toDoubleOrNull() ?: 10.0
                    val c = costText.toDoubleOrNull() ?: 4.0
                    onConfirm(q, c)
                },
                modifier = Modifier.testTag("dialog_batch_confirm")
            ) {
                Text("إضافة وتحديث التكلفة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
