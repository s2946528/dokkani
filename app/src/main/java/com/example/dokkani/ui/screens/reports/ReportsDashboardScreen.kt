package com.example.dokkani.ui.screens.reports

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ProductionQuantityLimits
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dokkani.data.local.entities.CostValuationMethod
import com.example.dokkani.data.local.entities.PartyType
import com.example.dokkani.data.local.entities.PaymentMethod
import com.example.dokkani.domain.credit.CreditNotebookEngine
import com.example.dokkani.domain.reports.ExpiryStatus
import com.example.dokkani.domain.reports.InventoryHealthReport
import com.example.dokkani.domain.reports.ProfitAndLossReport
import com.example.dokkani.domain.reports.TopProductsReport
import com.example.dokkani.ui.DokkaniUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * شاشة لوحة التحكم والتقارير المالية والمخزنية (Dashboard & Reports)
 */
@Composable
fun ReportsDashboardScreen(
    uiState: DokkaniUiState,
    onSelectSubTab: (Int) -> Unit,
    onSelectValuationMethod: (CostValuationMethod) -> Unit,
    onRefreshReports: () -> Unit
) {
    LaunchedEffect(Unit) {
        onRefreshReports()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
            .padding(12.dp)
    ) {
        // شريط العنوان وأزرار التحديث
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "التقارير المالية ولوحة المؤشرات",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = "أرباح وخسائر P&L، تحليل تكلفة COGS، والأصناف الأعلى ربحية والصلاحيات",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B)
                )
            }

            IconButton(onClick = onRefreshReports) {
                if (uiState.isLoadingReports) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = "تحديث", tint = Color(0xFF0F5132))
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // شريط التبويبات الفرعية
        TabRow(
            selectedTabIndex = uiState.reportSubTab,
            containerColor = Color.White,
            contentColor = Color(0xFF0F5132),
            modifier = Modifier.fillMaxWidth().testTag("reports_subtab_row")
        ) {
            Tab(
                selected = uiState.reportSubTab == 0,
                onClick = { onSelectSubTab(0) },
                text = { Text("الأرباح والخسائر (P&L)", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                icon = { Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.testTag("tab_pnl_report")
            )
            Tab(
                selected = uiState.reportSubTab == 1,
                onClick = { onSelectSubTab(1) },
                text = { Text("الأصناف والربحية", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                icon = { Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.testTag("tab_top_products_report")
            )
            Tab(
                selected = uiState.reportSubTab == 2,
                onClick = { onSelectSubTab(2) },
                text = { Text("النواقص والصلاحيات", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                icon = { Icon(Icons.Default.ProductionQuantityLimits, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.testTag("tab_inventory_health_report")
            )
            Tab(
                selected = uiState.reportSubTab == 3,
                onClick = { onSelectSubTab(3) },
                text = { Text("كشوفات الحسابات", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                icon = { Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.testTag("tab_account_statements_report")
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (uiState.isLoadingReports && uiState.pnlReport == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(40.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF0F5132))
            }
        } else {
            when (uiState.reportSubTab) {
                0 -> ProfitAndLossView(
                    report = uiState.pnlReport,
                    selectedMethod = uiState.selectedReportValuationMethod,
                    onSelectMethod = onSelectValuationMethod
                )
                1 -> TopProductsView(report = uiState.topProductsReport)
                2 -> InventoryHealthView(report = uiState.inventoryHealthReport)
                3 -> AccountStatementsReportView(uiState = uiState)
            }
        }
    }
}

/**
 * تبويب تقرير الأرباح والخسائر (P&L) مع دعم معايير WAC / FIFO / Last Purchase
 */
@Composable
private fun ProfitAndLossView(
    report: ProfitAndLossReport?,
    selectedMethod: CostValuationMethod,
    onSelectMethod: (CostValuationMethod) -> Unit,
    currencySymbol: String = "ر.ي"
) {
    if (report == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("لا تتوفر بيانات كافية لتوليد تقرير الأرباح والخسائر", color = Color(0xFF64748B))
        }
        return
    }

    val isProfitable = report.netOperatingProfit >= 0

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // شريط اختيار طريقة تقييم التكلفة لحساب COGS
        item {
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
                    Text(
                        text = "معيار تقييم التكلفة:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color(0xFF334155)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CostValuationMethod.values().forEach { method ->
                            val isSelected = method == selectedMethod
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelected) Color(0xFF0F5132) else Color(0xFFF1F5F9),
                                modifier = Modifier
                                    .clickable { onSelectMethod(method) }
                                    .testTag("btn_report_method_${method.name}")
                            ) {
                                Text(
                                    text = when (method) {
                                        CostValuationMethod.WAC -> "المتوسط المرجح (WAC)"
                                        CostValuationMethod.FIFO -> "الوارد أولاً (FIFO)"
                                        CostValuationMethod.LIFO -> "الوارد أخيراً (LIFO)"
                                        CostValuationMethod.LAST_PURCHASE_PRICE -> "آخر سعر شراء"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else Color(0xFF475569),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // بطاقة صافي الربح التشغيلي الكبير
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isProfitable) Color(0xFF0D3B2E) else Color(0xFF7F1D1D)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "صافي الربح التشغيلي النهائي (Net Operating Profit)",
                        fontSize = 12.sp,
                        color = Color(0xFFD1E7DD)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${if (isProfitable) "+" else ""}${"%.2f".format(report.netOperatingProfit)} $currencySymbol",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isProfitable) Color(0xFF198754) else Color(0xFFDC2626)
                        ) {
                            Text(
                                text = "نسبة الصافي: ${"%.1f".format(report.netProfitMarginPercent)}%",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color(0xFF198754))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("مجمل الربح التجاري", fontSize = 11.sp, color = Color(0xFFD1E7DD))
                            Text(
                                text = "${"%.2f".format(report.grossProfit)} $currencySymbol (${"%.1f".format(report.grossProfitMarginPercent)}%)",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF86EFAC),
                                fontSize = 13.sp
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("إجمالي المصروفات", fontSize = 11.sp, color = Color(0xFFD1E7DD))
                            Text(
                                text = "-${"%.2f".format(report.totalOperatingExpenses)} $currencySymbol",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFCA5A5),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        // جدول تفصيل شلال الأرباح والخسائر (P&L Statement Waterfall)
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "تفاصيل قائمة الدخل والعمليات:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    PnlLineItem("إجمالي المبيعات (Gross Sales)", "${"%.2f".format(report.grossSales)} $currencySymbol", Color(0xFF0F172A), isBold = true)
                    PnlLineItem("الخصومات الممنوحة للعملاء", "-${"%.2f".format(report.totalDiscounts)} $currencySymbol", Color(0xFFDC2626))
                    PnlLineItem("صافي الإيرادات (Net Revenue)", "${"%.2f".format(report.netSalesRevenue)} $currencySymbol", Color(0xFF0F5132), isBold = true)
                    HorizontalDivider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(vertical = 4.dp))

                    PnlLineItem("تكلفة البضاعة المباعة (COGS) [${report.valuationMethodUsed.name}]", "-${"%.2f".format(report.cogs)} $currencySymbol", Color(0xFFDC2626))
                    PnlLineItem("مجمل الربح التجاري (Gross Profit)", "${"%.2f".format(report.grossProfit)} $currencySymbol", Color(0xFF16A34A), isBold = true)
                    HorizontalDivider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(vertical = 4.dp))

                    PnlLineItem("المصروفات والنثريات التشغيلية", "-${"%.2f".format(report.totalOperatingExpenses)} $currencySymbol", Color(0xFFDC2626))
                    PnlLineItem("صافي الربح التشغيلي النهائي", "${"%.2f".format(report.netOperatingProfit)} $currencySymbol", if (isProfitable) Color(0xFF16A34A) else Color(0xFFDC2626), isBold = true)
                }
            }
        }

        // تفصيل المبيعات والمصروفات
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // المبيعات حسب طريقة الدفع
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("توزيع المبيعات:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF334155))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("• نقداً (كاش): ${"%.2f".format(report.totalCashSales)} $currencySymbol", fontSize = 11.sp, color = Color(0xFF475569))
                        Text("• مدى وبطاقات: ${"%.2f".format(report.totalMadaSales)} $currencySymbol", fontSize = 11.sp, color = Color(0xFF475569))
                        Text("• آجل وشكك: ${"%.2f".format(report.totalCreditSales)} $currencySymbol", fontSize = 11.sp, color = Color(0xFFDC2626))
                    }
                }

                // تفصيل بنود المصروفات
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("المصروفات حسب البند:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF334155))
                        Spacer(modifier = Modifier.height(6.dp))
                        if (report.expensesByCategory.isEmpty()) {
                            Text("لا توجد مصروفات", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        } else {
                            report.expensesByCategory.entries.take(4).forEach { (cat, amt) ->
                                Text("• $cat: ${"%.2f".format(amt)} $currencySymbol", fontSize = 11.sp, color = Color(0xFF475569))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PnlLineItem(label: String, value: String, color: Color, isBold: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = Color(0xFF475569), fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal)
        Text(text = value, fontSize = 13.sp, color = color, fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold)
    }
}

/**
 * تبويب تقرير الأصناف الأكثر حركة والأعلى ربحية
 */
@Composable
private fun TopProductsView(report: TopProductsReport?, currencySymbol: String = "ر.ي") {
    if (report == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("لا تتوفر مبيعات سابقة لحساب ربحية وحركة الأصناف", color = Color(0xFF64748B))
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "الأصناف الأكثر مبيعاً وحركة (الأعلى تصريفاً):",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFF1E293B)
            )
        }

        items(report.topMovingByQuantity.take(5)) { item ->
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
                    Column {
                        Text(text = item.productName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                        Text(
                            text = "إجمالي المبيعات: ${"%.2f".format(item.totalRevenue)} $currencySymbol",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${"%.1f".format(item.totalQuantitySold)} ${item.baseUnitName}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF0F5132)
                        )
                        Text(
                            text = "ربح: ${"%.2f".format(item.grossProfit)} $currencySymbol",
                            fontSize = 11.sp,
                            color = Color(0xFF16A34A)
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "الأصناف الأكثر توليداً للأرباح (الأعلى ربحية):",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFF1E293B)
            )
        }

        items(report.topProfitableByMargin.take(5)) { item ->
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
                    Column {
                        Text(text = item.productName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                        Text(
                            text = "مبيعات: ${"%.2f".format(item.totalRevenue)} $currencySymbol | كمية: ${"%.1f".format(item.totalQuantitySold)}",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "+${"%.2f".format(item.grossProfit)} $currencySymbol",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF16A34A)
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFDCFCE7)
                        ) {
                            Text(
                                text = "هامش ${"%.1f".format(item.profitMarginPercent)}%",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF16A34A),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * تبويب تقرير نواقص المخزون وتواريخ الصلاحية
 */
@Composable
private fun InventoryHealthView(report: InventoryHealthReport?) {
    if (report == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("لا تتوفر بيانات مخزون حالياً", color = Color(0xFF64748B))
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // بطاقات المؤشرات السريعة للنواقص والصلاحية
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("النواقص تحت الطلب", fontSize = 11.sp, color = Color(0xFFDC2626))
                        Text(
                            text = "${report.totalLowStockCount} أصناف",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFDC2626)
                        )
                    }
                }

                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("أوشكت على الانتهاء", fontSize = 11.sp, color = Color(0xFFD97706))
                        Text(
                            text = "${report.totalExpiredOrNearExpiryCount} أصناف",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD97706)
                        )
                    }
                }
            }
        }

        // قائمة النواقص
        item {
            Text(
                text = "الأصناف التي وصلت أو تجاوزت حد إعادة الطلب:",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFF1E293B)
            )
        }

        items(report.lowStockItems) { item ->
            val shortage = (item.minStockAlert - item.currentStock).coerceAtLeast(0.0)
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
                            color = Color(0xFFFEE2E2),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(text = item.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                            Text(
                                text = "حد إعادة الطلب الأدنى: ${item.minStockAlert} ${item.baseUnitName}",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${"%.1f".format(item.currentStock)} ${item.baseUnitName}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFFDC2626)
                        )
                        Text(
                            text = "العجز: ${"%.1f".format(shortage)}",
                            fontSize = 11.sp,
                            color = Color(0xFFEF4444)
                        )
                    }
                }
            }
        }

        if (report.lowStockItems.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("المخزون كافٍ ولا توجد نواقص تحت حد الطلب", color = Color(0xFF16A34A), fontSize = 12.sp)
                }
            }
        }

        // قائمة تواريخ الصلاحية والتنبيهات
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "تنبيهات تواريخ الصلاحية للأصناف:",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFF1E293B)
            )
        }

        items(report.expiryAlerts) { item ->
            val (badgeBg, badgeText) = when (item.status) {
                ExpiryStatus.EXPIRED -> Color(0xFFFEE2E2) to Color(0xFFDC2626)
                ExpiryStatus.CRITICAL -> Color(0xFFFFEDD5) to Color(0xFFEA580C)
                ExpiryStatus.WARNING -> Color(0xFFFEF3C7) to Color(0xFFD97706)
                ExpiryStatus.GOOD -> Color(0xFFDCFCE7) to Color(0xFF16A34A)
            }

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
                            color = badgeBg,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.EventBusy, contentDescription = null, tint = badgeText, modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(text = item.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                            Text(
                                text = "تاريخ الانتهاء: ${item.expiryDateFormatted}",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = badgeBg
                        ) {
                            Text(
                                text = item.status.labelArabic,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = badgeText,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = if (item.daysRemaining < 0) "منتهي منذ ${-item.daysRemaining} يوم" else "متبقي ${item.daysRemaining} يوم",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }
        }
    }
}

/**
 * تبويب كشوفات الحسابات التفصيلية (Account Statements & Ledgers)
 */
@Composable
private fun AccountStatementsReportView(
    uiState: DokkaniUiState,
    currencySymbol: String = "ر.ي"
) {
    var selectedCategory by remember { mutableIntStateOf(0) } // 0: العملاء, 1: الموردين, 2: الصندوق, 3: المصروفات
    var selectedPartyId by remember { mutableStateOf<Long?>(null) } // null = الكل
    var selectedPeriod by remember { mutableIntStateOf(0) } // 0: الكل, 1: اليوم, 2: هذا الأسبوع, 3: هذا الشهر
    var partyDropdownExpanded by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    // إعداد فلترة الفترة الزمنية
    val minTimestamp = remember(selectedPeriod) {
        val now = System.currentTimeMillis()
        when (selectedPeriod) {
            1 -> now - (24 * 60 * 60 * 1000L) // اليوم
            2 -> now - (7 * 24 * 60 * 60 * 1000L) // هذا الأسبوع
            3 -> now - (30L * 24 * 60 * 60 * 1000L) // هذا الشهر
            else -> 0L
        }
    }

    // تصفية العملاء أو الموردين المتاحين
    val filteredParties = remember(uiState.parties, selectedCategory) {
        when (selectedCategory) {
            0 -> uiState.parties.filter { it.type == PartyType.CUSTOMER || it.type == PartyType.BOTH }
            1 -> uiState.parties.filter { it.type == PartyType.SUPPLIER || it.type == PartyType.BOTH }
            else -> emptyList()
        }
    }

    // بناء قائمة الحركات بحسب الفئة المختصرة
    data class GeneralLedgerItem(
        val date: Long,
        val typeLabel: String,
        val refNumber: String,
        val description: String,
        val debit: Double,   // مدين (+)
        val credit: Double,  // دائن (-)
        val isIncome: Boolean,
        val runningBalance: Double
    )

    val ledgerData = remember(
        selectedCategory,
        selectedPartyId,
        minTimestamp,
        uiState.invoices,
        uiState.vouchers,
        uiState.expenses,
        uiState.cashShifts
    ) {
        val items = mutableListOf<GeneralLedgerItem>()

        when (selectedCategory) {
            0 -> { // العملاء
                val partyInvoices = uiState.invoices.filter { inv ->
                    (selectedPartyId == null || inv.partyId == selectedPartyId) && inv.date >= minTimestamp
                }
                val partyVouchers = uiState.vouchers.filter { v ->
                    (selectedPartyId == null || v.partyId == selectedPartyId) && v.date >= minTimestamp
                }

                partyInvoices.forEach { inv ->
                    val amt = if (inv.remainingAmount > 0.001) inv.remainingAmount else inv.total
                    items.add(
                        GeneralLedgerItem(
                            date = inv.date,
                            typeLabel = "فاتورة مبيعات آجل",
                            refNumber = inv.invoiceNumber,
                            description = if (inv.notes.isNotBlank()) inv.notes else "مشتريات على الحساب",
                            debit = amt,
                            credit = 0.0,
                            isIncome = false,
                            runningBalance = 0.0
                        )
                    )
                }

                partyVouchers.forEach { v ->
                    items.add(
                        GeneralLedgerItem(
                            date = v.date,
                            typeLabel = "سند قبض وتسديد",
                            refNumber = v.voucherNumber,
                            description = if (v.notes.isNotBlank()) v.notes else "سداد دفعة نقدية - ${v.paymentMethod.labelArabic}",
                            debit = 0.0,
                            credit = v.amount,
                            isIncome = true,
                            runningBalance = 0.0
                        )
                    )
                }
            }

            1 -> { // الموردين
                val supplierInvoices = uiState.invoices.filter { inv ->
                    (selectedPartyId == null || inv.partyId == selectedPartyId) && inv.date >= minTimestamp
                }
                val supplierVouchers = uiState.vouchers.filter { v ->
                    (selectedPartyId == null || v.partyId == selectedPartyId) && v.date >= minTimestamp
                }

                supplierInvoices.forEach { inv ->
                    val amt = if (inv.remainingAmount > 0.001) inv.remainingAmount else inv.total
                    items.add(
                        GeneralLedgerItem(
                            date = inv.date,
                            typeLabel = "فاتورة مشتريات",
                            refNumber = inv.invoiceNumber,
                            description = if (inv.notes.isNotBlank()) inv.notes else "توريد بضاعة بالآجل",
                            debit = 0.0,
                            credit = amt,
                            isIncome = false,
                            runningBalance = 0.0
                        )
                    )
                }

                supplierVouchers.forEach { v ->
                    items.add(
                        GeneralLedgerItem(
                            date = v.date,
                            typeLabel = "سند صرف وتدفيع",
                            refNumber = v.voucherNumber,
                            description = if (v.notes.isNotBlank()) v.notes else "دفعة سداد للمورد - ${v.paymentMethod.labelArabic}",
                            debit = v.amount,
                            credit = 0.0,
                            isIncome = true,
                            runningBalance = 0.0
                        )
                    )
                }
            }

            2 -> { // الصندوق والخزينة
                val cashInvoices = uiState.invoices.filter {
                    it.paymentMethod == PaymentMethod.CASH && it.date >= minTimestamp
                }
                val cashVouchers = uiState.vouchers.filter {
                    it.paymentMethod == PaymentMethod.CASH && it.date >= minTimestamp
                }
                val cashExpenses = uiState.expenses.filter {
                    it.paymentMethod == PaymentMethod.CASH && it.date >= minTimestamp
                }

                cashInvoices.forEach { inv ->
                    items.add(
                        GeneralLedgerItem(
                            date = inv.date,
                            typeLabel = "مبيعات نقدية (درج)",
                            refNumber = inv.invoiceNumber,
                            description = "تحصيل نقدي مبادلة مبيعات",
                            debit = inv.paidAmount,
                            credit = 0.0,
                            isIncome = true,
                            runningBalance = 0.0
                        )
                    )
                }

                cashVouchers.forEach { v ->
                    val isPay = v.isPayment || v.voucherNumber.startsWith("PAY")
                    if (isPay) {
                        items.add(
                            GeneralLedgerItem(
                                date = v.date,
                                typeLabel = "سند صرف نقدي للمورد",
                                refNumber = v.voucherNumber,
                                description = if (v.notes.isNotBlank()) v.notes else "سداد نقدي للمورد من الخزينة",
                                debit = 0.0,
                                credit = v.amount,
                                isIncome = false,
                                runningBalance = 0.0
                            )
                        )
                    } else {
                        items.add(
                            GeneralLedgerItem(
                                date = v.date,
                                typeLabel = "سند قبض نقدي",
                                refNumber = v.voucherNumber,
                                description = if (v.notes.isNotBlank()) v.notes else "إيداع نقدي الخزينة",
                                debit = v.amount,
                                credit = 0.0,
                                isIncome = true,
                                runningBalance = 0.0
                            )
                        )
                    }
                }

                cashExpenses.forEach { exp ->
                    items.add(
                        GeneralLedgerItem(
                            date = exp.date,
                            typeLabel = "مصروف نقدي من الصندوق",
                            refNumber = exp.expenseNumber,
                            description = "${exp.category} - ${exp.paidTo.ifBlank { "مصروف عام" }}",
                            debit = 0.0,
                            credit = exp.amount,
                            isIncome = false,
                            runningBalance = 0.0
                        )
                    )
                }
            }

            3 -> { // المصروفات
                val filteredExpenses = uiState.expenses.filter { it.date >= minTimestamp }
                filteredExpenses.forEach { exp ->
                    items.add(
                        GeneralLedgerItem(
                            date = exp.date,
                            typeLabel = exp.category,
                            refNumber = exp.expenseNumber,
                            description = if (exp.paidTo.isNotBlank()) "المدفوع له: ${exp.paidTo} (${exp.notes})" else exp.notes.ifBlank { "مصروف تشغيلي" },
                            debit = exp.amount,
                            credit = 0.0,
                            isIncome = false,
                            runningBalance = 0.0
                        )
                    )
                }
            }
        }

        // ترتيب الحركات زمنياً وحساب الرصيد التراكمي
        items.sortBy { it.date }

        var cumulative = 0.0
        val computedList = items.map { item ->
            cumulative += (item.debit - item.credit)
            item.copy(runningBalance = cumulative)
        }

        computedList.reversed() // إظهار الأحدث في الأعلى
    }

    val totalDebitSum = ledgerData.sumOf { it.debit }
    val totalCreditSum = ledgerData.sumOf { it.credit }
    val finalBalance = totalDebitSum - totalCreditSum

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // شريط اختيار فئة الحساب
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "اختر فئة الحساب لإصدار كشف الحساب التفصيلي:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val categories = listOf(
                            Pair("حسابات العملاء", Icons.Default.Person),
                            Pair("حسابات الموردين", Icons.Default.LocalShipping),
                            Pair("الصندوق والخزينة", Icons.Default.PointOfSale),
                            Pair("المصروفات والنثريات", Icons.Default.MoneyOff)
                        )
                        items(categories.size) { index ->
                            val (catName, icon) = categories[index]
                            val isSelected = selectedCategory == index
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedCategory = index
                                    selectedPartyId = null
                                },
                                label = { Text(catName, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // اختيار الحساب المحدد والفترة الزمنية
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selectedCategory == 0 || selectedCategory == 1) {
                            val selectedPartyName = filteredParties.find { it.id == selectedPartyId }?.name ?: "جميع الحسابات"
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedButton(
                                    onClick = { partyDropdownExpanded = true },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(selectedPartyName, fontSize = 12.sp, maxLines = 1)
                                }

                                DropdownMenu(
                                    expanded = partyDropdownExpanded,
                                    onDismissRequest = { partyDropdownExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("جميع الحسابات", fontWeight = FontWeight.Bold) },
                                        onClick = {
                                            selectedPartyId = null
                                            partyDropdownExpanded = false
                                        }
                                    )
                                    filteredParties.forEach { p ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(p.name)
                                                    Text(
                                                        "${"%.2f".format(abs(p.currentBalance))} $currencySymbol",
                                                        fontSize = 11.sp,
                                                        color = Color.Gray
                                                    )
                                                }
                                            },
                                            onClick = {
                                                selectedPartyId = p.id
                                                partyDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // فلتر الفترة الزمنية
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            val periods = listOf("الكل", "اليوم", "الأسبوع", "الشهر")
                            items(periods.size) { idx ->
                                FilterChip(
                                    selected = selectedPeriod == idx,
                                    onClick = { selectedPeriod = idx },
                                    label = { Text(periods[idx], fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // بطاقات المؤشرات المدمجة لكشف الحساب
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("إجمالي المقبوضات/مدين (+)", fontSize = 11.sp, color = Color(0xFF166534))
                        Text("${"%.2f".format(totalDebitSum)} $currencySymbol", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("إجمالي المدفوعات/دائن (-)", fontSize = 11.sp, color = Color(0xFF991B1B))
                        Text("${"%.2f".format(totalCreditSum)} $currencySymbol", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("صافي رصيد الحركة", fontSize = 11.sp, color = Color(0xFF1E40AF))
                        Text("${"%.2f".format(finalBalance)} $currencySymbol", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D4ED8))
                    }
                }
            }
        }

        // قائمة كشف الحساب التفصيلية
        items(ledgerData) { item ->
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (item.isIncome) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (item.isIncome) Icons.Default.ReceiptLong else Icons.Default.Receipt,
                                    contentDescription = null,
                                    tint = if (item.isIncome) Color(0xFF16A34A) else Color(0xFFDC2626),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(item.typeLabel, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFFF1F5F9)
                                ) {
                                    Text(
                                        text = "#${item.refNumber}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF475569),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(item.description, fontSize = 11.sp, color = Color(0xFF64748B), maxLines = 1)
                            Text(
                                text = dateFormat.format(Date(item.date)),
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        if (item.debit > 0) {
                            Text(
                                text = "+${"%.2f".format(item.debit)} $currencySymbol",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF16A34A)
                            )
                        } else if (item.credit > 0) {
                            Text(
                                text = "-${"%.2f".format(item.credit)} $currencySymbol",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFFDC2626)
                            )
                        }
                        Text(
                            text = "الرصيد: ${"%.2f".format(item.runningBalance)} $currencySymbol",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF475569)
                        )
                    }
                }
            }
        }

        if (ledgerData.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لا توجد حركات مسجلة لهذا الحساب خلال الفترة المحددة",
                        color = Color(0xFF64748B),
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
