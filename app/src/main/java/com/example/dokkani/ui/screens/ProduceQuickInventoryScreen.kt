package com.example.dokkani.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dokkani.data.local.entities.BatchWithYields
import com.example.dokkani.data.local.entities.ProductWithUnits
import com.example.dokkani.domain.produce.ProduceAuditResult
import com.example.dokkani.domain.produce.ProduceQuickCalcSummary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ProduceQuickInventoryScreen(
    // حالة تبويب الجرد و COGS
    produceAuditSubTab: Int,
    productsWithUnits: List<ProductWithUnits>,
    selectedProduceProductId: Long?,
    auditBeginningQty: String,
    auditBeginningCost: String,
    auditPurchasesQty: String,
    auditPurchasesCost: String,
    auditEndingQty: String,
    auditWasteQty: String,
    auditPosSoldQty: String,
    auditPosRevenue: String,
    auditResult: ProduceAuditResult?,
    isSubmittingAudit: Boolean,
    onSelectSubTab: (Int) -> Unit,
    onSelectProduceProduct: (Long) -> Unit,
    onAuditInputsChanged: (begQty: String?, begCost: String?, purQty: String?, purCost: String?, endingQty: String?, wasteQty: String?, posSold: String?, posRev: String?) -> Unit,
    onCommitSilentAdjustments: () -> Unit,
    onNavigateToBarcodePrinter: (Long) -> Unit,

    // حالة حاسبة سحارة الخضار المشكل القديمة
    grossWeightInput: String,
    costInput: String,
    expenseInput: String,
    wasteInput: String,
    marginInput: String,
    crateDescription: String,
    calcSummary: ProduceQuickCalcSummary?,
    isSavingBatch: Boolean,
    historicalBatches: List<BatchWithYields>,
    onCrateInputsChanged: (gross: String?, cost: String?, expense: String?, waste: String?, margin: String?, desc: String?) -> Unit,
    onSaveCrateBatch: () -> Unit,

    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("produce_quick_inventory_screen")
    ) {
        // شريط التبويبات الفرعية للجرد
        SecondaryTabRow(
            selectedTabIndex = produceAuditSubTab,
            containerColor = Color(0xFF133E32),
            contentColor = Color.White
        ) {
            Tab(
                selected = produceAuditSubTab == 0,
                onClick = { onSelectSubTab(0) },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FactCheck, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("الجرد الدوري وحساب COGS", fontWeight = FontWeight.Bold)
                    }
                }
            )
            Tab(
                selected = produceAuditSubTab == 1,
                onClick = { onSelectSubTab(1) },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Eco, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("حاسبة سحارة المشكل وفرز الهدر", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        if (produceAuditSubTab == 0) {
            // ==========================================
            // التبويب الأول: الجرد الدوري السريع وحساب COGS والتسويات الصامتة
            // ==========================================
            DailyProduceAuditView(
                productsWithUnits = productsWithUnits,
                selectedProduceProductId = selectedProduceProductId,
                auditBeginningQty = auditBeginningQty,
                auditBeginningCost = auditBeginningCost,
                auditPurchasesQty = auditPurchasesQty,
                auditPurchasesCost = auditPurchasesCost,
                auditEndingQty = auditEndingQty,
                auditWasteQty = auditWasteQty,
                auditPosSoldQty = auditPosSoldQty,
                auditPosRevenue = auditPosRevenue,
                auditResult = auditResult,
                isSubmittingAudit = isSubmittingAudit,
                onSelectProduceProduct = onSelectProduceProduct,
                onAuditInputsChanged = onAuditInputsChanged,
                onCommitSilentAdjustments = onCommitSilentAdjustments,
                onNavigateToBarcodePrinter = onNavigateToBarcodePrinter
            )
        } else {
            // ==========================================
            // التبويب الثاني: حاسبة سحارة المشكل وفرز الهدر وسجل الدفعات
            // ==========================================
            MixedProduceCrateCalculatorView(
                grossWeightInput = grossWeightInput,
                costInput = costInput,
                expenseInput = expenseInput,
                wasteInput = wasteInput,
                marginInput = marginInput,
                crateDescription = crateDescription,
                calcSummary = calcSummary,
                isSaving = isSavingBatch,
                historicalBatches = historicalBatches,
                dateFormat = dateFormat,
                onInputsChanged = onCrateInputsChanged,
                onSaveBatch = onSaveCrateBatch
            )
        }
    }
}

/**
 * شاشة الجرد الدوري السريع للخضار والورقيات ومعادلة COGS
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DailyProduceAuditView(
    productsWithUnits: List<ProductWithUnits>,
    selectedProduceProductId: Long?,
    auditBeginningQty: String,
    auditBeginningCost: String,
    auditPurchasesQty: String,
    auditPurchasesCost: String,
    auditEndingQty: String,
    auditWasteQty: String,
    auditPosSoldQty: String,
    auditPosRevenue: String,
    auditResult: ProduceAuditResult?,
    isSubmittingAudit: Boolean,
    onSelectProduceProduct: (Long) -> Unit,
    onAuditInputsChanged: (begQty: String?, begCost: String?, purQty: String?, purCost: String?, endingQty: String?, wasteQty: String?, posSold: String?, posRev: String?) -> Unit,
    onCommitSilentAdjustments: () -> Unit,
    onNavigateToBarcodePrinter: (Long) -> Unit
) {
    val weightedProducts = remember(productsWithUnits) {
        productsWithUnits.filter { it.product.isWeighted || it.product.category.contains("خضار") || it.product.category.contains("فاكهة") || it.product.category.contains("ورقيات") }
            .ifEmpty { productsWithUnits }
    }

    val currentSelected = productsWithUnits.firstOrNull { it.product.id == selectedProduceProductId }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // بطاقة التعريف المحاسبي للجرد السريع
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF133E32)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF2E7D32),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.FactCheck,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "الجرد الدوري السريع للخضار والفوضويات",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "تطبيق معادلة صافي تكلفة المباع (COGS) وتسجيل قيود التسوية الصامتة",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFA5D6A7)
                            )
                        }
                    }
                }
            }
        }

        // اختيار صنف الخضار / الورقيات
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, Color(0xFFC8E6C9)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "1. اختيار صنف الخضار أو الورقيات للجرد:",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                        if (currentSelected != null) {
                            OutlinedButton(
                                onClick = { onNavigateToBarcodePrinter(currentSelected.product.id) },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("طباعة باركود الصنف", fontSize = 12.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        weightedProducts.forEach { pw ->
                            val isSelected = pw.product.id == selectedProduceProductId
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSelectProduceProduct(pw.product.id) },
                                label = {
                                    Text(
                                        text = pw.product.name,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Eco,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (isSelected) Color(0xFF1B5E20) else Color.Gray
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        // بطاقة مدخلات الجرد (المخزون، المشتريات، التقديري على الرف، التوالف)
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "2. إدخال بيانات يومية الجرد (أول المدة، الوارد، التالف، الرف المتبقي):",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // رصيد أول المدة
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = auditBeginningQty,
                            onValueChange = { onAuditInputsChanged(it, null, null, null, null, null, null, null) },
                            label = { Text("كمية أول المدة (كجم)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = auditBeginningCost,
                            onValueChange = { onAuditInputsChanged(null, it, null, null, null, null, null, null) },
                            label = { Text("تكلفة أول المدة (ر.س)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    // مشتريات وتوريدات اليوم
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = auditPurchasesQty,
                            onValueChange = { onAuditInputsChanged(null, null, it, null, null, null, null, null) },
                            label = { Text("مشتريات اليوم (كجم)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = auditPurchasesCost,
                            onValueChange = { onAuditInputsChanged(null, null, null, it, null, null, null, null) },
                            label = { Text("تكلفة المشتريات (ر.س)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(10.dp))

                    // القيمة التقديرية المتبقية على الرف وقيمة التوالف/الهالك
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = auditEndingQty,
                            onValueChange = { onAuditInputsChanged(null, null, null, null, it, null, null, null) },
                            label = { Text("التقديري على الرف (كجم)") },
                            placeholder = { Text("مخزون آخر المدة") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("audit_shelf_qty_input"),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Inventory2, contentDescription = null, tint = Color(0xFF2E7D32)) }
                        )
                        OutlinedTextField(
                            value = auditWasteQty,
                            onValueChange = { onAuditInputsChanged(null, null, null, null, null, it, null, null) },
                            label = { Text("الهالك والتالف (كجم)") },
                            placeholder = { Text("توالف اليوم") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("audit_waste_qty_input"),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Color(0xFFD32F2F)) }
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    // مبيعات الكاشير لمقارنة العجز
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = auditPosSoldQty,
                            onValueChange = { onAuditInputsChanged(null, null, null, null, null, null, it, null) },
                            label = { Text("مبيعات الكاشير (كجم)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = auditPosRevenue,
                            onValueChange = { onAuditInputsChanged(null, null, null, null, null, null, null, it) },
                            label = { Text("إيراد الكاشير (ر.س)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
            }
        }

        // بطاقة معادلة تكلفة المباع الآلية (COGS Equation Card)
        if (auditResult != null) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9)),
                    border = BorderStroke(1.5.dp, Color(0xFF66BB6A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Calculate, contentDescription = null, tint = Color(0xFF2E7D32))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "المعادلة المحاسبية الآلية لصافي تكلفة المباع (COGS):",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color(0xFF1B5E20)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFC8E6C9)
                            ) {
                                Text(
                                    text = "دقة معيارية",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF1B5E20),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        // عرض صيغة المعادلة نصياً ورياضياً
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFA5D6A7)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "COGS = (مخزون أول المدة + المشتريات) - مخزون آخر المدة - التوالف",
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF2E7D32),
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        // تفقيط الأرقام الحية للمعادلة
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("المتاح للبيع", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text("%.1f كجم".format(auditResult.goodsAvailableQty), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("%.2f ر.س".format(auditResult.goodsAvailableCost), style = MaterialTheme.typography.labelSmall, color = Color(0xFF388E3C))
                            }
                            Text("-", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.align(Alignment.CenterVertically))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("الرف المتبقي", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text("%.1f كجم".format(auditResult.endingInventoryQty), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("%.2f ر.س".format(auditResult.endingInventoryCost), style = MaterialTheme.typography.labelSmall, color = Color(0xFF1976D2))
                            }
                            Text("-", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.align(Alignment.CenterVertically))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("التوالف والهالك", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text("%.1f كجم".format(auditResult.wasteQty), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Red)
                                Text("%.2f ر.س".format(auditResult.wasteCost), style = MaterialTheme.typography.labelSmall, color = Color.Red)
                            }
                            Text("=", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.align(Alignment.CenterVertically))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("COGS المحسوب", style = MaterialTheme.typography.labelSmall, color = Color(0xFF1B5E20), fontWeight = FontWeight.Bold)
                                Text("%.1f كجم".format(auditResult.cogsCalculatedQty), fontWeight = FontWeight.Black, fontSize = 15.sp, color = Color(0xFF1B5E20))
                                Text("%.2f ر.س".format(auditResult.cogsCalculatedCost), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1B5E20))
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = Color(0xFFA5D6A7))
                        Spacer(modifier = Modifier.height(10.dp))

                        // تحليل الربحية والعجز ومقارنة الكاشير
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("متوسط تكلفة الكيلو المتاح:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text("%.2f ر.س / كجم".format(auditResult.averageCostPerUnit), fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("نسبة الهالك والتوالف:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text("%.1f%% من المتاح".format(auditResult.wastePercentageOfAvailable), fontWeight = FontWeight.Bold, color = if (auditResult.wastePercentageOfAvailable > 10) Color.Red else Color(0xFFF57C00))
                            }
                            Column {
                                Text("مجمل الربح المحقق:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text("%.2f ر.س (%.1f%%)".format(auditResult.grossProfit, auditResult.grossProfitMarginPercent), fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                            }
                        }

                        // تنبيه العجز الدفتري غير المسجل
                        if (Math.abs(auditResult.shrinkageDiscrepancyQty) > 0.05) {
                            Spacer(modifier = Modifier.height(10.dp))
                            val isDeficit = auditResult.shrinkageDiscrepancyQty > 0
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isDeficit) Color(0xFFFFF3E0) else Color(0xFFE8F5E9),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isDeficit) Icons.Default.TrendingDown else Icons.Default.TrendingUp,
                                        contentDescription = null,
                                        tint = if (isDeficit) Color(0xFFE65100) else Color(0xFF2E7D32),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isDeficit)
                                            "فارق بين الجرد ومبيعات الكاشير: عجز قدره %.2f كجم (بقيمة تكلفة %.2f ر.س) سيتم استيعابه بالقيود الصامتة."
                                                .format(auditResult.shrinkageDiscrepancyQty, auditResult.shrinkageDiscrepancyCost)
                                        else
                                            "فارق بين الجرد ومبيعات الكاشير: زيادة قدرها %.2f كجم عن المسجل دفترياً."
                                                .format(Math.abs(auditResult.shrinkageDiscrepancyQty)),
                                        fontSize = 12.sp,
                                        color = if (isDeficit) Color(0xFFE65100) else Color(0xFF2E7D32)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // بطاقة قيود التسوية المخزنية الصامتة (Silent Adjustments)
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, Color(0xFFB0BEC5)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color(0xFF37474F))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "3. قيود التسوية المخزنية الصامتة الجاهزة للترحيل:",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFECEFF1)
                            ) {
                                Text(
                                    text = "${auditResult.generatedAdjustments.size} قيود",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        if (auditResult.generatedAdjustments.isEmpty()) {
                            Text(
                                text = "المخزون الفعلي مطابق تماماً ولا يتطلب أي قيود تسوية.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        } else {
                            auditResult.generatedAdjustments.forEach { adj ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFFAFAFA),
                                    border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = adj.reasonArabic,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = "المرجع: ${adj.referenceNumber} | تكلفة الوحدة: %.2f ر.س".format(adj.unitCost),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.Gray,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (adj.quantityChange < 0) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
                                        ) {
                                            Text(
                                                text = "%+.2f كجم".format(adj.quantityChange),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = if (adj.quantityChange < 0) Color(0xFFC62828) else Color(0xFF2E7D32),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // زر الترحيل والتسجيل
                        Button(
                            onClick = { onCommitSilentAdjustments() },
                            enabled = !isSubmittingAudit && auditResult.generatedAdjustments.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("commit_silent_adjustments_button")
                        ) {
                            if (isSubmittingAudit) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("جارٍ ترحيل قيود التسوية الصامتة...")
                            } else {
                                Icon(Icons.Default.Save, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "ترحيل وتسجيل قيود التسوية الصامتة لإغلاق اليوم",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * حاسبة سحارة الخضار المشكل وفرز الهدر وحساب التكلفة وسجل الدفعات التاريخية
 */
@Composable
private fun MixedProduceCrateCalculatorView(
    grossWeightInput: String,
    costInput: String,
    expenseInput: String,
    wasteInput: String,
    marginInput: String,
    crateDescription: String,
    calcSummary: ProduceQuickCalcSummary?,
    isSaving: Boolean,
    historicalBatches: List<BatchWithYields>,
    dateFormat: SimpleDateFormat,
    onInputsChanged: (gross: String?, cost: String?, expense: String?, waste: String?, margin: String?, desc: String?) -> Unit,
    onSaveBatch: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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
                            text = "حاسبة فرز سحارة الخضار المشكل (Produce Waste Engine)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "عند شراء سحارة خضار جملة (مثل 25 كجم بـ 90 ر.س مع 10 ر.س نقل) وهدر 3 كجم تالف، يقوم المحرك برمي التكلفة على الوزن الصافي (22 كجم) ورفع تكلفة الكيلو الصافي واقتراح سعر بيع بهامش ربح فوري.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFC8E6C9)
                    )
                }
            }
        }

        // مدخلات السحارة
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "مدخلات السحارة والتكاليف:",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = crateDescription,
                        onValueChange = { onInputsChanged(null, null, null, null, null, it) },
                        label = { Text("بيان السحارة / المصدر") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = grossWeightInput,
                            onValueChange = { onInputsChanged(it, null, null, null, null, null) },
                            label = { Text("الوزن الإجمالي (كجم)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = costInput,
                            onValueChange = { onInputsChanged(null, it, null, null, null, null) },
                            label = { Text("سعر الشراء (ر.س)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = expenseInput,
                            onValueChange = { onInputsChanged(null, null, it, null, null, null) },
                            label = { Text("مصاريف النقل (ر.س)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = wasteInput,
                            onValueChange = { onInputsChanged(null, null, null, it, null, null) },
                            label = { Text("وزن التالف والهالك (كجم)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = marginInput,
                        onValueChange = { onInputsChanged(null, null, null, null, it, null) },
                        label = { Text("هامش الربح المستهدف (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        }

        // النتائج المحاسبية لفرز السحارة
        if (calcSummary != null) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9)),
                    border = BorderStroke(1.dp, Color(0xFF81C784)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "نتائج الفرز وتوزيع تكلفة الهالك:",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B5E20),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("إجمالي التكلفة مع النقل", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text("%.2f ر.س".format(calcSummary.totalEffectiveInvestedCost), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Column {
                                Text("الوزن الصافي القابل للبيع", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text("%.2f كجم".format(calcSummary.netSalableWeightKg), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF2E7D32))
                            }
                            Column {
                                Text("نسبة الهدر والتالف", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text("%.1f%%".format(calcSummary.wastePercentage), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Red)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = Color(0xFFC8E6C9))
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val initialCostPerKg = if (calcSummary.grossWeightKg > 0) calcSummary.totalCrateCost / calcSummary.grossWeightKg else 0.0
                            Column {
                                Text("تكلفة الكيلو الإجمالي القديمة", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text("%.2f ر.س / كجم".format(initialCostPerKg), fontWeight = FontWeight.Medium, color = Color.Gray)
                            }
                            Column {
                                Text("تكلفة الكيلو الصافي بعد الهدر", style = MaterialTheme.typography.labelSmall, color = Color.Black)
                                Text("%.2f ر.س / كجم".format(calcSummary.effectiveCostPerSalableKg), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFFD32F2F))
                            }
                            Column {
                                Text("سعر البيع المقترح للكيلو", style = MaterialTheme.typography.labelSmall, color = Color(0xFF1B5E20))
                                Text("%.2f ر.س / كجم".format(calcSummary.suggestedSalePricePerKg), fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color(0xFF1B5E20))
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = { onSaveBatch() },
                            enabled = !isSaving && calcSummary.netSalableWeightKg > 0,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("جارٍ حفظ الدفعة...")
                            } else {
                                Icon(Icons.Default.Save, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("اعتماد وحفظ دفعة السحارة في قاعدة البيانات")
                            }
                        }
                    }
                }
            }
        }

        // سجل الدفعات السابقة
        if (historicalBatches.isNotEmpty()) {
            item {
                Text(
                    text = "سجل دفعات الخضار السابقة في قاعدة البيانات:",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
            }

            items(historicalBatches) { batchWithYields ->
                val b = batchWithYields.batch
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = b.batchNumber,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = dateFormat.format(Date(b.date)),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        }
                        Text(
                            text = b.sourceDescription,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("الوزن: %.1f كجم".format(b.totalGrossWeightKg), style = MaterialTheme.typography.bodySmall)
                            Text("التالف: %.1f كجم".format(b.wasteWeightKg), style = MaterialTheme.typography.bodySmall, color = Color.Red)
                            Text("الصافي: %.1f كجم".format(b.netSalableWeightKg), style = MaterialTheme.typography.bodySmall, color = Color(0xFF2E7D32))
                            Text("التكلفة: %.2f ر.س".format(b.effectiveCostPerKg), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
