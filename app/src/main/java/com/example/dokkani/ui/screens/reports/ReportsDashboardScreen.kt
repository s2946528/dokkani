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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.ProductionQuantityLimits
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dokkani.data.local.entities.CostValuationMethod
import com.example.dokkani.domain.reports.ExpiryStatus
import com.example.dokkani.domain.reports.InventoryHealthReport
import com.example.dokkani.domain.reports.ProfitAndLossReport
import com.example.dokkani.domain.reports.TopProductsReport
import com.example.dokkani.ui.DokkaniUiState

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
    onSelectMethod: (CostValuationMethod) -> Unit
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
                            text = "${if (isProfitable) "+" else ""}${"%.2f".format(report.netOperatingProfit)} ر.س",
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
                                text = "${"%.2f".format(report.grossProfit)} ر.س (${"%.1f".format(report.grossProfitMarginPercent)}%)",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF86EFAC),
                                fontSize = 13.sp
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("إجمالي المصروفات", fontSize = 11.sp, color = Color(0xFFD1E7DD))
                            Text(
                                text = "-${"%.2f".format(report.totalOperatingExpenses)} ر.س",
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

                    PnlLineItem("إجمالي المبيعات (Gross Sales)", "${"%.2f".format(report.grossSales)} ر.س", Color(0xFF0F172A), isBold = true)
                    PnlLineItem("الخصومات الممنوحة للعملاء", "-${"%.2f".format(report.totalDiscounts)} ر.س", Color(0xFFDC2626))
                    PnlLineItem("صافي الإيرادات (Net Revenue)", "${"%.2f".format(report.netSalesRevenue)} ر.س", Color(0xFF0F5132), isBold = true)
                    HorizontalDivider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(vertical = 4.dp))

                    PnlLineItem("تكلفة البضاعة المباعة (COGS) [${report.valuationMethodUsed.name}]", "-${"%.2f".format(report.cogs)} ر.س", Color(0xFFDC2626))
                    PnlLineItem("مجمل الربح التجاري (Gross Profit)", "${"%.2f".format(report.grossProfit)} ر.س", Color(0xFF16A34A), isBold = true)
                    HorizontalDivider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(vertical = 4.dp))

                    PnlLineItem("المصروفات والنثريات التشغيلية", "-${"%.2f".format(report.totalOperatingExpenses)} ر.س", Color(0xFFDC2626))
                    PnlLineItem("صافي الربح التشغيلي النهائي", "${"%.2f".format(report.netOperatingProfit)} ر.س", if (isProfitable) Color(0xFF16A34A) else Color(0xFFDC2626), isBold = true)
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
                        Text("• نقداً (كاش): ${"%.2f".format(report.totalCashSales)} ر.س", fontSize = 11.sp, color = Color(0xFF475569))
                        Text("• مدى وبطاقات: ${"%.2f".format(report.totalMadaSales)} ر.س", fontSize = 11.sp, color = Color(0xFF475569))
                        Text("• آجل وشكك: ${"%.2f".format(report.totalCreditSales)} ر.س", fontSize = 11.sp, color = Color(0xFFDC2626))
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
                                Text("• $cat: ${"%.2f".format(amt)} ر.س", fontSize = 11.sp, color = Color(0xFF475569))
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
private fun TopProductsView(report: TopProductsReport?) {
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
                            text = "إجمالي المبيعات: ${"%.2f".format(item.totalRevenue)} ر.س",
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
                            text = "ربح: ${"%.2f".format(item.grossProfit)} ر.س",
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
                            text = "مبيعات: ${"%.2f".format(item.totalRevenue)} ر.س | كمية: ${"%.1f".format(item.totalQuantitySold)}",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "+${"%.2f".format(item.grossProfit)} ر.س",
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
