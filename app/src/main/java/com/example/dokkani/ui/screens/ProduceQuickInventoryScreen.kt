package com.example.dokkani.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dokkani.data.local.entities.BatchWithYields
import com.example.dokkani.domain.produce.ProduceQuickCalcSummary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProduceQuickInventoryScreen(
    grossWeightInput: String,
    costInput: String,
    expenseInput: String,
    wasteInput: String,
    marginInput: String,
    crateDescription: String,
    calcSummary: ProduceQuickCalcSummary?,
    isSaving: Boolean,
    historicalBatches: List<BatchWithYields>,
    onInputsChanged: (gross: String?, cost: String?, expense: String?, waste: String?, margin: String?, desc: String?) -> Unit,
    onSaveBatch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // بطاقة التعريف المحاسبي للجرد السريع لخضار المشكل
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B4D3E)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Eco,
                            contentDescription = null,
                            tint = Color(0xFF81C784),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "جرد خضار المشكل والهدر (Mixed Produce Inventory)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "محرك متخصص لحساب سحاحير الخضار المشكل: احتساب الهدر والتالف بدقة، وتوزيع التكلفة على الوزن الصافي الصالح للبيع، وضمان عدم تآكل الأرباح بسبب التلف الطبيعي.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE8F5E9)
                    )
                }
            }
        }

        // مدخلات السحارة والجرد
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "بيانات السحارة / الدفعة المستلمة:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = crateDescription,
                        onValueChange = { onInputsChanged(null, null, null, null, null, it) },
                        label = { Text("بيان أو مصدر السحارة المشكلة") },
                        modifier = Modifier.fillMaxWidth().testTag("produce_input_desc")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = grossWeightInput,
                            onValueChange = { onInputsChanged(it, null, null, null, null, null) },
                            label = { Text("الوزن القائم (كجم)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f).testTag("produce_input_gross")
                        )
                        OutlinedTextField(
                            value = costInput,
                            onValueChange = { onInputsChanged(null, it, null, null, null, null) },
                            label = { Text("تكلفة الشراء (ر.س)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f).testTag("produce_input_cost")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = expenseInput,
                            onValueChange = { onInputsChanged(null, null, it, null, null, null) },
                            label = { Text("مصاريف نقل وعتالة (ر.س)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f).testTag("produce_input_expense")
                        )
                        OutlinedTextField(
                            value = wasteInput,
                            onValueChange = { onInputsChanged(null, null, null, it, null, null) },
                            label = { Text("الوزن التالف/الهدر (كجم)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f).testTag("produce_input_waste")
                        )
                    }

                    OutlinedTextField(
                        value = marginInput,
                        onValueChange = { onInputsChanged(null, null, null, null, it, null) },
                        label = { Text("هامش الربح المستهدف (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth().testTag("produce_input_margin")
                    )
                }
            }
        }

        // بطاقة المخرجات المحاسبية الفورية
        item {
            calcSummary?.let { summary ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    border = BorderStroke(1.5.dp, Color(0xFF198754)),
                    modifier = Modifier.fillMaxWidth().testTag("produce_calc_summary_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "نتائج الاحتساب المحاسبي الفوري للصافي والتكلفة:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F5132)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // أرقام رئيسية
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            ProduceMetricBox(
                                title = "الوزن الصافي الصالح",
                                value = "%.2f".format(summary.netSalableWeightKg),
                                unit = "كجم",
                                color = Color(0xFF0F5132),
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            ProduceMetricBox(
                                title = "نسبة الهدر والتالف",
                                value = "%.1f%%".format(summary.wastePercentage),
                                unit = "${summary.wasteWeightKg} كجم تالف",
                                color = Color(0xFFDC3545),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            ProduceMetricBox(
                                title = "التكلفة الفعلية للصافي",
                                value = "%.3f".format(summary.effectiveCostPerSalableKg),
                                unit = "ر.س / كجم",
                                color = Color(0xFF0D6EFD),
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            ProduceMetricBox(
                                title = "سعر البيع المقترح",
                                value = "%.2f".format(summary.suggestedSalePricePerKg),
                                unit = "هامش ${summary.targetProfitMarginPercent}%",
                                color = Color(0xFFD97706),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // التوضيح المحاسبي الدقيق للفارق
                        val nominalCost = summary.totalEffectiveInvestedCost / summary.grossWeightKg
                        val costDiff = summary.effectiveCostPerSalableKg - nominalCost

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFFF3CD),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "💡 ملحوظة محاسبية جوهرية للبقال:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF664D03)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "لو تم احتساب الكيلو قبل استبعاد التالف لكانت التكلفة %.3f ر.س/كجم. وبسبب التالف (%.1f%%) ارتفعت التكلفة الحقيقية إلى %.3f ر.س/كجم (+%.3f ر.س). اعتماد التكلفة الصافية يحميك من الخسارة الخفية!".format(
                                        nominalCost,
                                        summary.wastePercentage,
                                        summary.effectiveCostPerSalableKg,
                                        costDiff
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF664D03)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // زر الحفظ في قاعدة البيانات
                        Button(
                            onClick = onSaveBatch,
                            enabled = !isSaving,
                            modifier = Modifier.fillMaxWidth().testTag("save_produce_batch_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F5132))
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("جاري الحفظ في قاعدة البيانات...")
                            } else {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("حفظ وتوثيق دفعة الجرد في جدول خضار المشكل")
                            }
                        }
                    }
                }
            }
        }

        // السجل التاريخي لدفعات الجرد السابقة في قاعدة البيانات
        item {
            Text(
                text = "سجل دفعات خضار المشكل المحفوظة في قاعدة البيانات (${historicalBatches.size}):",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }

        if (historicalBatches.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "لا توجد دفعات خضار سابقة مسجلة، اضغط على زر الحفظ أعلاه لإضافة أول دفعة.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(historicalBatches) { item ->
                val b = item.batch
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = b.sourceDescription,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "دفعة #${b.batchNumber} • ${dateFormat.format(Date(b.date))}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFD1E7DD),
                                modifier = Modifier.padding(4.dp)
                            ) {
                                Text(
                                    text = b.status.labelArabic,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F5132),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "قائم: ${b.totalGrossWeightKg} كجم | تالف: ${b.wasteWeightKg} كجم (${b.wastePercentage}%)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "تكلفة الصافي: %.2f ر.س/كجم".format(b.effectiveCostPerKg),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F5132)
                            )
                        }

                        if (item.yieldItems.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "الأصناف المفروزة: " + item.yieldItems.joinToString("، ") { "${it.productName} (${it.sortedWeightKg} كجم @ ${it.targetSellingPricePerKg} ر.س)" },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProduceMetricBox(
    title: String,
    value: String,
    unit: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
