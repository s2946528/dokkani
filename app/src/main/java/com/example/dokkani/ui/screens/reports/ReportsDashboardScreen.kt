package com.example.dokkani.ui.screens.reports

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dokkani.data.local.entities.CostValuationMethod
import com.example.dokkani.data.local.entities.PartyType
import com.example.dokkani.data.local.entities.PaymentMethod
import com.example.dokkani.domain.reports.BalanceSheetReport
import com.example.dokkani.domain.reports.InventoryHealthReport
import com.example.dokkani.domain.reports.ProfitAndLossReport
import com.example.dokkani.domain.reports.TopProductsReport
import com.example.dokkani.domain.reports.TrialBalanceReport
import com.example.dokkani.ui.DokkaniUiState
import com.example.dokkani.util.ReportPdfPrinter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * شاشة لوحة التقارير المالية والختامية الموحدة
 */
@Composable
fun ReportsDashboardScreen(
    uiState: DokkaniUiState,
    onSelectSubTab: (Int) -> Unit,
    onSelectValuationMethod: (CostValuationMethod) -> Unit,
    onSelectStatementMode: (Int) -> Unit = {},
    onRefreshReports: () -> Unit
) {
    val context = LocalContext.current
    var showPrintDialog by remember { mutableStateOf(false) }
    var hideZeroAccounts by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        onRefreshReports()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag("reports_dashboard_screen")
    ) {
        // 1. شريط العنوان والتحكم الموحد بمنتصف الشاشة وبمساحة ضيقة لرفع المحتوى للأعلى
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // العنوان الرئيسي والفرعي موسطان بصرياً
                Text(
                    text = "التقارير المالية والختامية",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "قائمة الدخل P&L، الميزانية العمومية، ميزان المراجعة، كشوفات الحسابات، والتحليلات",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    fontSize = 10.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                // شريط الأدوات السريعة: أزرار الطباعة والتصدير والإنعاش وتصفية الحسابات الصفرية
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // مفتاح تصفية الحسابات الصفرية
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(
                                color = if (hideZeroAccounts) Color(0xFFE8F5E9) else Color(0xFFF1F5F9),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .clickable { hideZeroAccounts = !hideZeroAccounts }
                            .testTag("switch_hide_zero_accounts")
                    ) {
                        Icon(
                            imageVector = if (hideZeroAccounts) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null,
                            tint = if (hideZeroAccounts) Color(0xFF15803D) else Color(0xFF64748B),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "إخفاء الحسابات الصفرية",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (hideZeroAccounts) Color(0xFF15803D) else Color(0xFF475569)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Switch(
                            checked = hideZeroAccounts,
                            onCheckedChange = { hideZeroAccounts = it },
                            modifier = Modifier.size(width = 28.dp, height = 18.dp),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF15803D),
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color(0xFFCBD5E1)
                            )
                        )
                    }

                    // أزرار الإجراءات السريعة (طباعة مباشرة + تصدير PDF)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        // زر الطباعة المباشرة
                        Button(
                            onClick = {
                                handlePrintOrExport(
                                    context = context,
                                    uiState = uiState,
                                    action = ReportPdfPrinter.PrintAction.PRINT,
                                    hideZeroAccounts = hideZeroAccounts
                                )
                            },
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F5132)),
                            modifier = Modifier.testTag("btn_direct_print")
                        ) {
                            Icon(Icons.Default.Print, contentDescription = "طباعة", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("طباعة", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        // زر تصدير/مشاركة PDF
                        OutlinedButton(
                            onClick = {
                                handlePrintOrExport(
                                    context = context,
                                    uiState = uiState,
                                    action = ReportPdfPrinter.PrintAction.SHARE,
                                    hideZeroAccounts = hideZeroAccounts
                                )
                            },
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            border = BorderStroke(1.dp, Color(0xFF0F5132)),
                            modifier = Modifier.testTag("btn_export_pdf_share")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "تصدير PDF", tint = Color(0xFF0F5132), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تصدير PDF", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F5132))
                        }

                        // زر التحديث
                        IconButton(
                            onClick = onRefreshReports,
                            modifier = Modifier.size(28.dp)
                        ) {
                            if (uiState.isLoadingReports) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = "تحديث", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 2. شريط التبويبات الأفقية المرفوع لأعلى الشاشة وتوفير أقصى مساحة للبيانات
        ScrollableTabRow(
            selectedTabIndex = uiState.reportSubTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            edgePadding = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("reports_scrollable_tabs")
        ) {
            val tabs = listOf(
                Pair("قائمة الدخل (P&L)", Icons.Default.Assessment),
                Pair("الميزانية العمومية", Icons.Default.AccountBalance),
                Pair("ميزان المراجعة", Icons.Default.Calculate),
                Pair("كشوفات الحسابات", Icons.Default.ReceiptLong),
                Pair("الحركة والتحليلات", Icons.Default.TrendingUp)
            )

            tabs.forEachIndexed { index, (title, icon) ->
                Tab(
                    selected = uiState.reportSubTab == index,
                    onClick = { onSelectSubTab(index) },
                    text = { Text(title, fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                    icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp)) },
                    modifier = Modifier.testTag("tab_report_$index")
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 3. عرض محتوى التقرير المختار بدون فراغات زائفة
        if (uiState.isLoadingReports && uiState.pnlReport == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF0F5132))
            }
        } else {
            when (uiState.reportSubTab) {
                0 -> ProfitAndLossView(
                    report = uiState.pnlReport,
                    selectedMethod = uiState.selectedReportValuationMethod,
                    onSelectMethod = onSelectValuationMethod,
                    currencySymbol = uiState.currencySymbol
                )
                1 -> BalanceSheetView(
                    report = uiState.balanceSheetReport,
                    currencySymbol = uiState.currencySymbol
                )
                2 -> TrialBalanceView(
                    report = uiState.trialBalanceReport,
                    hideZeroAccounts = hideZeroAccounts,
                    currencySymbol = uiState.currencySymbol
                )
                3 -> AccountStatementsReportView(
                    uiState = uiState,
                    hideZeroAccounts = hideZeroAccounts,
                    onSelectStatementMode = onSelectStatementMode,
                    currencySymbol = uiState.currencySymbol
                )
                4 -> TopProductsAndHealthView(
                    topReport = uiState.topProductsReport,
                    healthReport = uiState.inventoryHealthReport,
                    currencySymbol = uiState.currencySymbol
                )
            }
        }
    }

    // حوار الطباعة وتصدير PDF الاختياري عند الحاجة
    if (showPrintDialog) {
        val activeReportTitle = when (uiState.reportSubTab) {
            0 -> "تقرير قائمة الدخل والأرباح والخسائر"
            1 -> "تقرير الميزانية العمومية الختامية"
            2 -> "تقرير ميزان المراجعة المحاسبي"
            3 -> if (uiState.reportStatementMode == 0) "كشف الحسابات التفصيلي" else "كشف أرصدة الحسابات الإجمالي"
            else -> "تقرير تحليل المبيعات والمخزون"
        }

        AlertDialog(
            onDismissRequest = { showPrintDialog = false },
            title = { Text("طباعة وتصدير التقرير الحالي", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("التقرير المحدد: $activeReportTitle", fontWeight = FontWeight.Bold, color = Color(0xFF0F5132), fontSize = 12.sp)
                    if (hideZeroAccounts) {
                        Text("• التصفية مفعلة: سيتم استبعاد الحسابات الصفرية تلقائياً من المستند.", fontSize = 11.sp, color = Color(0xFF15803D))
                    }
                    Text("اختر إجراء الطباعة المباشرة عبر الطابعة أو التصدير كمستند PDF متوافق:", fontSize = 11.sp, color = Color(0xFF475569))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPrintDialog = false
                        handlePrintOrExport(
                            context = context,
                            uiState = uiState,
                            action = ReportPdfPrinter.PrintAction.PRINT,
                            hideZeroAccounts = hideZeroAccounts
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F5132))
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("طباعة مباشرة")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showPrintDialog = false
                        handlePrintOrExport(
                            context = context,
                            uiState = uiState,
                            action = ReportPdfPrinter.PrintAction.SHARE,
                            hideZeroAccounts = hideZeroAccounts
                        )
                    }
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تصدير/مشاركة PDF")
                }
            }
        )
    }
}

/**
 * معالج تجهيز وتنفيذ طباعة أو تصدير الـ PDF حسب التبويب النشط وتصفية الحسابات الصفرية
 */
private fun handlePrintOrExport(
    context: android.content.Context,
    uiState: DokkaniUiState,
    action: ReportPdfPrinter.PrintAction,
    hideZeroAccounts: Boolean = true
) {
    val storeName = uiState.settings?.storeName ?: "دكاني POS"
    val currency = uiState.currencySymbol

    when (uiState.reportSubTab) {
        0 -> { // P&L
            val pnl = uiState.pnlReport ?: return
            val kpi = listOf(
                "صافي الربح التشغيلي" to "${"%.2f".format(pnl.netOperatingProfit)} $currency",
                "إجمالي المبيعات" to "${"%.2f".format(pnl.grossSales)} $currency",
                "تكلفة البضاعة المباعة" to "${"%.2f".format(pnl.cogs)} $currency",
                "إجمالي المصروفات" to "${"%.2f".format(pnl.totalOperatingExpenses)} $currency"
            )
            val rows = listOf(
                ReportPdfPrinter.TableRowData("البند", "المبلغ ($currency)", "النسبة / التفاصيل", isHeader = true),
                ReportPdfPrinter.TableRowData("إجمالي المبيعات", "%.2f".format(pnl.grossSales), "-"),
                ReportPdfPrinter.TableRowData("الخصومات الممنوحة", "%.2f".format(pnl.totalDiscounts), "-"),
                ReportPdfPrinter.TableRowData("صافي الإيرادات", "%.2f".format(pnl.netSalesRevenue), "100%"),
                ReportPdfPrinter.TableRowData("تكلفة المبيعات (COGS)", "%.2f".format(pnl.cogs), pnl.valuationMethodUsed.name),
                ReportPdfPrinter.TableRowData("مجمل الربح التجاري", "%.2f".format(pnl.grossProfit), "%.1f%%".format(pnl.grossProfitMarginPercent)),
                ReportPdfPrinter.TableRowData("المصروفات التشغيلية", "%.2f".format(pnl.totalOperatingExpenses), "-"),
                ReportPdfPrinter.TableRowData("صافي الربح التشغيلي", "%.2f".format(pnl.netOperatingProfit), "%.1f%%".format(pnl.netProfitMarginPercent), isTotal = true)
            )
            ReportPdfPrinter.generateAndPrintReport(context, storeName, "تقرير قائمة الدخل الأرباح والخسائر", "معيار: ${pnl.valuationMethodUsed.name}", kpi, rows, action)
        }
        1 -> { // Balance Sheet
            val bs = uiState.balanceSheetReport ?: return
            val kpi = listOf(
                "إجمالي الأصول" to "${"%.2f".format(bs.totalAssets)} $currency",
                "إجمالي الخصوم" to "${"%.2f".format(bs.totalLiabilities)} $currency",
                "حقوق الملكية" to "${"%.2f".format(bs.totalEquity)} $currency"
            )
            val rows = listOf(
                ReportPdfPrinter.TableRowData("حساب الميزانية العمومية", "الأصول ($currency)", "الخصوم والملكية ($currency)", isHeader = true),
                ReportPdfPrinter.TableRowData("النقدية بالصناديق والدرج", "%.2f".format(bs.currentAssetsCashInDrawer), "-"),
                ReportPdfPrinter.TableRowData("أرصدة البنوك والشبكة", "%.2f".format(bs.currentAssetsBankBalances), "-"),
                ReportPdfPrinter.TableRowData("تقييم مخزون البضاعة", "%.2f".format(bs.currentAssetsInventoryValuation), "-"),
                ReportPdfPrinter.TableRowData("ديون العملاء (الأرصدة المدينة)", "%.2f".format(bs.currentAssetsReceivables), "-"),
                ReportPdfPrinter.TableRowData("الأصول الثابتة والخلو", "%.2f".format(bs.totalNonCurrentAssets), "-"),
                ReportPdfPrinter.TableRowData("ديون الموردين والالتزامات", "-", "%.2f".format(bs.supplierPayables)),
                ReportPdfPrinter.TableRowData("رأس المال الافتتاحي الثابت", "-", "%.2f".format(bs.fixedOpeningCapital)),
                ReportPdfPrinter.TableRowData("رأس المال الجاري / المتأثر", "-", "%.2f".format(bs.currentAffectedCapital)),
                ReportPdfPrinter.TableRowData("إجمالي الميزانية العمومية", "%.2f".format(bs.totalAssets), "%.2f".format(bs.totalLiabilitiesAndEquity), isTotal = true)
            )
            ReportPdfPrinter.generateAndPrintReport(context, storeName, "تقرير الميزانية العمومية الختامية", "مركز المالي الموحد", kpi, rows, action)
        }
        2 -> { // Trial Balance
            val tb = uiState.trialBalanceReport ?: return
            val filteredItems = if (hideZeroAccounts) {
                tb.items.filter { abs(it.debit) > 0.001 || abs(it.credit) > 0.001 }
            } else tb.items

            val kpi = listOf(
                "إجمالي المدين (+)" to "${"%.2f".format(tb.totalDebit)} $currency",
                "إجمالي الدائن (-)" to "${"%.2f".format(tb.totalCredit)} $currency",
                "الحالة المحاسبية" to if (tb.isBalanced) "متوازن تماماً ✓" else "غير متوازن"
            )
            val rows = mutableListOf(
                ReportPdfPrinter.TableRowData("الكود", "اسم الحساب", "مدين ($currency)", "دائن ($currency)", isHeader = true)
            )
            filteredItems.forEach { item ->
                rows.add(ReportPdfPrinter.TableRowData(item.accountCode, item.accountName, "%.2f".format(item.debit), "%.2f".format(item.credit)))
            }
            rows.add(ReportPdfPrinter.TableRowData("الجامع", "إجمالي ميزان المراجعة", "%.2f".format(tb.totalDebit), "%.2f".format(tb.totalCredit), isTotal = true))
            ReportPdfPrinter.generateAndPrintReport(context, storeName, "تقرير ميزان المراجعة المحاسبي", "توازن القيد المزدوج ${if (hideZeroAccounts) "(تم استبعاد الصفرية)" else ""}", kpi, rows, action)
        }
        3 -> { // Account Statements
            val isDetailed = uiState.reportStatementMode == 0
            val reportName = if (isDetailed) "كشف حساب تفصيلي" else "كشف أرصدة الحسابات الإجمالي"
            val kpi = listOf(
                "نوع التقرير" to reportName,
                "العملة" to currency
            )
            val rows = mutableListOf<ReportPdfPrinter.TableRowData>()

            if (isDetailed) {
                rows.add(ReportPdfPrinter.TableRowData("التاريخ والوقت", "رقم المستند / البيان", "مدين (+)", "دائن (-)", isHeader = true))
                val recentInvoices = uiState.invoices.sortedByDescending { it.date }.take(50)
                recentInvoices.forEach { inv ->
                    rows.add(
                        ReportPdfPrinter.TableRowData(
                            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(inv.date)),
                            "فاتورة #${inv.invoiceNumber}",
                            "%.2f".format(inv.total),
                            "0.00"
                        )
                    )
                }
            } else {
                rows.add(ReportPdfPrinter.TableRowData("اسم الحساب / الجهة", "فئة الحساب", "الرصيد الحالي ($currency)", "-", isHeader = true))
                val displayedParties = if (hideZeroAccounts) {
                    uiState.parties.filter { abs(it.currentBalance) > 0.001 }
                } else uiState.parties

                displayedParties.forEach { p ->
                    rows.add(ReportPdfPrinter.TableRowData(p.name, if (p.type == PartyType.CUSTOMER) "عميل" else "مورد", "%.2f".format(p.currentBalance), "-"))
                }
            }
            ReportPdfPrinter.generateAndPrintReport(context, storeName, reportName, "نظام دكاني POS المحاسبي", kpi, rows, action)
        }
        else -> {
            val top = uiState.topProductsReport
            val kpi = listOf(
                "إجمالي الأنواع المباعة" to "${top?.topMovingByQuantity?.size ?: 0}",
                "العملة" to currency
            )
            val rows = mutableListOf(
                ReportPdfPrinter.TableRowData("اسم الصنف", "الكمية المباعة", "الإيراد ($currency)", "الربح ($currency)", isHeader = true)
            )
            top?.topMovingByQuantity?.forEach { item ->
                rows.add(ReportPdfPrinter.TableRowData(item.productName, "%.1f %s".format(item.totalQuantitySold, item.baseUnitName), "%.2f".format(item.totalRevenue), "%.2f".format(item.grossProfit)))
            }
            ReportPdfPrinter.generateAndPrintReport(context, storeName, "تقرير حركة الأصناف والنواقص", "تحليلات المخزون والمبيعات", kpi, rows, action)
        }
    }
}

@Composable
private fun PnlLineItem(label: String, value: String, color: Color, isBold: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 11.sp, color = Color(0xFF475569), fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal)
        Text(text = value, fontSize = 12.sp, color = color, fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold)
    }
}

/**
 * تبويب تقرير الأرباح والخسائر (P&L) مع قائمة منسدلة أنيقة لاختيار معيار التقييم
 */
@Composable
private fun ProfitAndLossView(
    report: ProfitAndLossReport?,
    selectedMethod: CostValuationMethod,
    onSelectMethod: (CostValuationMethod) -> Unit,
    currencySymbol: String = "ر.ي"
) {
    var methodDropdownExpanded by remember { mutableStateOf(false) }

    if (report == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("لا تتوفر بيانات كافية لتوليد تقرير الأرباح والخسائر", color = Color(0xFF64748B), fontSize = 12.sp)
        }
        return
    }

    val isProfitable = report.netOperatingProfit >= 0

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // عنوان التقرير الموسّط
        item {
            Text(
                text = "تقرير قائمة الدخل (الأرباح والخسائر - P&L)",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F5132),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
            )
        }

        // شريط اختيار طريقة تقييم التكلفة بداخل قائمة منسدلة Dropdown موفرة للمساحة
        item {
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = Color(0xFF0F5132), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "معيار احتساب تكلفة COGS:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color(0xFF334155)
                        )
                    }

                    Box {
                        OutlinedButton(
                            onClick = { methodDropdownExpanded = true },
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.testTag("btn_pnl_valuation_dropdown")
                        ) {
                            Text(
                                text = when (selectedMethod) {
                                    CostValuationMethod.WAC -> "المتوسط المرجح (WAC)"
                                    CostValuationMethod.FIFO -> "الوارد أولاً (FIFO)"
                                    CostValuationMethod.LIFO -> "الوارد أخيراً (LIFO)"
                                    CostValuationMethod.LAST_PURCHASE_PRICE -> "آخر سعر شراء"
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F5132)
                            )
                        }

                        DropdownMenu(
                            expanded = methodDropdownExpanded,
                            onDismissRequest = { methodDropdownExpanded = false }
                        ) {
                            CostValuationMethod.values().forEach { method ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = when (method) {
                                                CostValuationMethod.WAC -> "المتوسط المرجح (WAC)"
                                                CostValuationMethod.FIFO -> "الوارد أولاً صادر أولاً (FIFO)"
                                                CostValuationMethod.LIFO -> "الوارد أخيراً صادر أولاً (LIFO)"
                                                CostValuationMethod.LAST_PURCHASE_PRICE -> "آخر سعر شراء بالفواتير"
                                            },
                                            fontSize = 11.sp,
                                            fontWeight = if (method == selectedMethod) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        onSelectMethod(method)
                                        methodDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // بطاقة صافي الربح التشغيلي الرئيسي
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isProfitable) Color(0xFF0D3B2E) else Color(0xFF7F1D1D)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "صافي الربح التشغيلي النهائي (Net Operating Profit)",
                        fontSize = 11.sp,
                        color = Color(0xFFD1E7DD),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${if (isProfitable) "+" else ""}${"%.2f".format(report.netOperatingProfit)} $currencySymbol",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isProfitable) Color(0xFF198754) else Color(0xFFDC2626)
                        ) {
                            Text(
                                text = "هامش الصافي: ${"%.1f".format(report.netProfitMarginPercent)}%",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    HorizontalDivider(color = Color(0xFF198754))
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("مجمل الربح التجاري", fontSize = 10.sp, color = Color(0xFFD1E7DD))
                            Text(
                                text = "${"%.2f".format(report.grossProfit)} $currencySymbol (${"%.1f".format(report.grossProfitMarginPercent)}%)",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF86EFAC),
                                fontSize = 12.sp
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("إجمالي المصروفات", fontSize = 10.sp, color = Color(0xFFD1E7DD))
                            Text(
                                text = "-${"%.2f".format(report.totalOperatingExpenses)} $currencySymbol",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFCA5A5),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // جدول قائمة الدخل (P&L Waterfall)
        item {
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "تفاصيل قائمة الدخل والعمليات P&L:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    PnlLineItem("إجمالي المبيعات (Gross Sales)", "${"%.2f".format(report.grossSales)} $currencySymbol", Color(0xFF0F172A), isBold = true)
                    PnlLineItem("الخصومات الممنوحة للعملاء", "-${"%.2f".format(report.totalDiscounts)} $currencySymbol", Color(0xFFDC2626))
                    PnlLineItem("صافي الإيرادات (Net Revenue)", "${"%.2f".format(report.netSalesRevenue)} $currencySymbol", Color(0xFF0F5132), isBold = true)
                    HorizontalDivider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(vertical = 2.dp))

                    PnlLineItem("تكلفة البضاعة المباعة (COGS)", "-${"%.2f".format(report.cogs)} $currencySymbol", Color(0xFFDC2626))
                    PnlLineItem("مجمل الربح التجاري (Gross Profit)", "${"%.2f".format(report.grossProfit)} $currencySymbol", Color(0xFF16A34A), isBold = true)
                    HorizontalDivider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(vertical = 2.dp))

                    PnlLineItem("المصروفات التشغيلية والنثريات", "-${"%.2f".format(report.totalOperatingExpenses)} $currencySymbol", Color(0xFFDC2626))
                    PnlLineItem("صافي الربح التشغيلي النهائي", "${"%.2f".format(report.netOperatingProfit)} $currencySymbol", if (isProfitable) Color(0xFF16A34A) else Color(0xFFDC2626), isBold = true)
                }
            }
        }
    }
}

/**
 * تبويب تقرير الميزانية العمومية الختامي (Balance Sheet View)
 */
@Composable
private fun BalanceSheetView(
    report: BalanceSheetReport?,
    currencySymbol: String = "ر.ي"
) {
    if (report == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("جاري احتساب الميزانية العمومية...", color = Color(0xFF64748B), fontSize = 12.sp)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // عنوان موسّط
        item {
            Text(
                text = "تقرير الميزانية العمومية الختامية (Balance Sheet)",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F5132),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
            )
        }

        // شريط حالة التوازن المالي
        item {
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
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
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text("الميزانية العمومية الختامية متوازنة", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF15803D))
                            Text("إجمالي الأصول = إجمالي الخصوم وحقوق الملكية", fontSize = 10.sp, color = Color(0xFF166534))
                        }
                    }

                    Text(
                        text = "${"%.2f".format(report.totalAssets)} $currencySymbol",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF15803D)
                    )
                }
            }
        }

        // جانب الأصول (Assets)
        item {
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("أولاً: الأصول (Assets)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF1E3A8A))
                    Spacer(modifier = Modifier.height(4.dp))

                    Text("الأصول المتداولة:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF334155))
                    PnlLineItem("• النقدية بالصناديق والدرج", "${"%.2f".format(report.currentAssetsCashInDrawer)} $currencySymbol", Color(0xFF0F172A))
                    PnlLineItem("• أرصدة البنوك والشبكة", "${"%.2f".format(report.currentAssetsBankBalances)} $currencySymbol", Color(0xFF0F172A))
                    PnlLineItem("• تقييم بضاعة آخر المدة بالتكلفة", "${"%.2f".format(report.currentAssetsInventoryValuation)} $currencySymbol", Color(0xFF0F172A))
                    PnlLineItem("• ديون العملاء (الأرصدة المدينة)", "${"%.2f".format(report.currentAssetsReceivables)} $currencySymbol", Color(0xFF0F172A))
                    PnlLineItem("إجمالي الأصول المتداولة", "${"%.2f".format(report.totalCurrentAssets)} $currencySymbol", Color(0xFF0284C7), isBold = true)

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color(0xFFF1F5F9))

                    Text("الأصول غير المتداولة (الثابتة والتأسيسية):", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF334155))
                    PnlLineItem("• الأصول الثابتة (ثلاجات، أرفف، وموازين)", "${"%.2f".format(report.fixedAssetsTotal)} $currencySymbol", Color(0xFF0F172A))
                    PnlLineItem("• حقوق الخلو ونقل القدم (أصل غير ملموس)", "${"%.2f".format(report.leaseholdGoodwillTotal)} $currencySymbol", Color(0xFF0F172A))
                    PnlLineItem("إجمالي الأصول غير المتداولة", "${"%.2f".format(report.totalNonCurrentAssets)} $currencySymbol", Color(0xFF7C3AED), isBold = true)

                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = Color(0xFFCBD5E1))
                    PnlLineItem("(=) إجمالي الأصول الكلية:", "${"%.2f".format(report.totalAssets)} $currencySymbol", Color(0xFF1E3A8A), isBold = true)
                }
            }
        }

        // جانب الخصوم وحقوق الملكية (Liabilities & Equity)
        item {
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("ثانياً: الخصوم وحقوق الملكية (Liabilities & Equity)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF15803D))
                    Spacer(modifier = Modifier.height(4.dp))

                    Text("الخصوم والالتزامات المتداولة:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF334155))
                    PnlLineItem("• ديون الموردين (الأرصدة الدائنة)", "${"%.2f".format(report.supplierPayables)} $currencySymbol", Color(0xFFDC2626))
                    PnlLineItem("إجمالي الخصوم والالتزامات", "${"%.2f".format(report.totalLiabilities)} $currencySymbol", Color(0xFFDC2626), isBold = true)

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color(0xFFF1F5F9))

                    Text("حقوق الملكية ورأس المال:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF334155))
                    PnlLineItem("• رأس المال الافتتاحي الثابت", "${"%.2f".format(report.fixedOpeningCapital)} $currencySymbol", Color(0xFF0F172A))
                    PnlLineItem("• إيداعات رأس المال الإضافية", "+${"%.2f".format(report.additionalCapitalDeposits)} $currencySymbol", Color(0xFF16A34A))
                    PnlLineItem("• مسحوبات المالك الشخصية", "-${"%.2f".format(report.ownerDrawings)} $currencySymbol", Color(0xFFDC2626))
                    PnlLineItem("• صافي الأرباح/الخسائر المبقاة", "${"%.2f".format(report.netRetainedOperatingProfit)} $currencySymbol", Color(0xFF16A34A))
                    PnlLineItem("إجمالي حقوق الملكية (رأس المال الجاري)", "${"%.2f".format(report.currentAffectedCapital)} $currencySymbol", Color(0xFF15803D), isBold = true)

                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = Color(0xFFCBD5E1))
                    PnlLineItem("(=) إجمالي الخصوم وحقوق الملكية:", "${"%.2f".format(report.totalLiabilitiesAndEquity)} $currencySymbol", Color(0xFF15803D), isBold = true)
                }
            }
        }
    }
}

/**
 * تبويب تقرير ميزان المراجعة المحاسبي (Trial Balance View) مع دعم خيار إخفاء الحسابات الصفرية
 */
@Composable
private fun TrialBalanceView(
    report: TrialBalanceReport?,
    hideZeroAccounts: Boolean,
    currencySymbol: String = "ر.ي"
) {
    if (report == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("جاري توليد ميزان المراجعة...", color = Color(0xFF64748B), fontSize = 12.sp)
        }
        return
    }

    val displayedItems = remember(report, hideZeroAccounts) {
        if (hideZeroAccounts) {
            report.items.filter { abs(it.debit) > 0.001 || abs(it.credit) > 0.001 }
        } else {
            report.items
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // عنوان موسّط
        item {
            Text(
                text = "تقرير ميزان المراجعة المحاسبي ${if (hideZeroAccounts) "(تصفية الحسابات الفعالة)" else ""}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F5132),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
            )
        }

        // شريط رأس التوازن
        item {
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (report.isBalanced) Color(0xFFF0FDF4) else Color(0xFFFEF2F2)
                ),
                border = BorderStroke(1.dp, if (report.isBalanced) Color(0xFFBBF7D0) else Color(0xFFFCA5A5)),
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
                        Icon(
                            imageVector = if (report.isBalanced) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (report.isBalanced) Color(0xFF16A34A) else Color(0xFFDC2626),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = if (report.isBalanced) "ميزان المراجعة متوازن محاسبياً (القيد المزدوج)" else "يوجد عدم توازن في ميزان المراجعة",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (report.isBalanced) Color(0xFF15803D) else Color(0xFF991B1B)
                            )
                            Text("مجموع الأرصدة المدينة = مجموع الأرصدة الدائنة", fontSize = 10.sp, color = Color(0xFF475569))
                        }
                    }

                    Text(
                        text = "${"%.2f".format(report.totalDebit)} $currencySymbol",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF0F172A)
                    )
                }
            }
        }

        // ترويسة الجدول المحاسبي لميزان المراجعة
        item {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFFE2E8F0),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("الكود والاسم", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF0F172A), modifier = Modifier.weight(2f))
                    Text("النوع", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF0F172A), modifier = Modifier.weight(1f))
                    Text("مدين (+)", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF15803D), modifier = Modifier.weight(1f))
                    Text("دائن (-)", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFFDC2626), modifier = Modifier.weight(1f))
                }
            }
        }

        // سطور ميزان المراجعة المفلترة
        items(displayedItems) { item ->
            Card(
                shape = RoundedCornerShape(6.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(2f)) {
                        Text(item.accountName, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF0F172A))
                        Text("#${item.accountCode}", fontSize = 9.sp, color = Color(0xFF64748B))
                    }
                    Text(item.categoryLabel, fontSize = 10.sp, color = Color(0xFF475569), modifier = Modifier.weight(1f))
                    Text(
                        text = if (item.debit > 0) "${"%.2f".format(item.debit)}" else "-",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF15803D),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = if (item.credit > 0) "${"%.2f".format(item.credit)}" else "-",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDC2626),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (displayedItems.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("لا توجد حسابات ذات حركات غير صفرية متوفرة حالياً", fontSize = 11.sp, color = Color(0xFF64748B))
                }
            }
        }

        // سطر المجموع النهائي لجامع ميزان المراجعة
        item {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF0F5132),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("الإجمالي والجامع النهائي:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White, modifier = Modifier.weight(3f))
                    Text("${"%.2f".format(report.totalDebit)} $currencySymbol", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF86EFAC), modifier = Modifier.weight(1f))
                    Text("${"%.2f".format(report.totalCredit)} $currencySymbol", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFFCA5A5), modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * تبويب كشوفات الحسابات المتقدم (تفصيلي وإجمالي) مع دعم تصفية الحسابات الصفرية
 */
@Composable
private fun AccountStatementsReportView(
    uiState: DokkaniUiState,
    hideZeroAccounts: Boolean,
    onSelectStatementMode: (Int) -> Unit = {},
    currencySymbol: String = "ر.ي"
) {
    val statementMode = uiState.reportStatementMode // 0 = تفصيلي, 1 = إجمالي
    var selectedCategory by remember { mutableIntStateOf(0) }
    var selectedPartyId by remember { mutableStateOf<Long?>(null) }
    var selectedPeriod by remember { mutableIntStateOf(0) }
    var partyDropdownExpanded by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    val minTimestamp = remember(selectedPeriod) {
        val now = System.currentTimeMillis()
        when (selectedPeriod) {
            1 -> now - (24 * 60 * 60 * 1000L)
            2 -> now - (7 * 24 * 60 * 60 * 1000L)
            3 -> now - (30L * 24 * 60 * 60 * 1000L)
            else -> 0L
        }
    }

    val filteredParties = remember(uiState.parties, selectedCategory, hideZeroAccounts) {
        val baseParties = when (selectedCategory) {
            0 -> uiState.parties.filter { it.type == PartyType.CUSTOMER || it.type == PartyType.BOTH }
            1 -> uiState.parties.filter { it.type == PartyType.SUPPLIER || it.type == PartyType.BOTH }
            else -> emptyList()
        }
        if (hideZeroAccounts) {
            baseParties.filter { abs(it.currentBalance) > 0.001 }
        } else {
            baseParties
        }
    }

    // --- بيانات الكشف التفصيلي ---
    data class StatementItem(
        val date: Long,
        val typeLabel: String,
        val refNumber: String,
        val description: String,
        val debit: Double,
        val credit: Double,
        val isIncome: Boolean,
        val runningBalance: Double
    )

    val ledgerData = remember(
        selectedCategory,
        selectedPartyId,
        minTimestamp,
        uiState.invoices,
        uiState.vouchers,
        uiState.expenses
    ) {
        val items = mutableListOf<StatementItem>()

        when (selectedCategory) {
            0 -> {
                val partyInvoices = uiState.invoices.filter { inv -> (selectedPartyId == null || inv.partyId == selectedPartyId) && inv.date >= minTimestamp }
                val partyVouchers = uiState.vouchers.filter { v -> (selectedPartyId == null || v.partyId == selectedPartyId) && v.date >= minTimestamp }

                partyInvoices.forEach { inv ->
                    val amt = if (inv.remainingAmount > 0.001) inv.remainingAmount else inv.total
                    items.add(StatementItem(inv.date, "فاتورة مبيعات آجل", inv.invoiceNumber, inv.notes.ifBlank { "مشتريات على الحساب" }, amt, 0.0, false, 0.0))
                }
                partyVouchers.forEach { v ->
                    items.add(StatementItem(v.date, "سند قبض وتسديد", v.voucherNumber, v.notes.ifBlank { "سداد دفعة نقدية" }, 0.0, v.amount, true, 0.0))
                }
            }
            1 -> {
                val supplierInvoices = uiState.invoices.filter { inv -> (selectedPartyId == null || inv.partyId == selectedPartyId) && inv.date >= minTimestamp }
                val supplierVouchers = uiState.vouchers.filter { v -> (selectedPartyId == null || v.partyId == selectedPartyId) && v.date >= minTimestamp }

                supplierInvoices.forEach { inv ->
                    val amt = if (inv.remainingAmount > 0.001) inv.remainingAmount else inv.total
                    items.add(StatementItem(inv.date, "فاتورة توريد مشتريات", inv.invoiceNumber, inv.notes.ifBlank { "توريد بضاعة" }, 0.0, amt, false, 0.0))
                }
                supplierVouchers.forEach { v ->
                    items.add(StatementItem(v.date, "سند صرف للمورد", v.voucherNumber, v.notes.ifBlank { "سداد دفعة للمورد" }, v.amount, 0.0, true, 0.0))
                }
            }
            2 -> {
                val cashInvoices = uiState.invoices.filter { it.paymentMethod == PaymentMethod.CASH && it.date >= minTimestamp }
                val cashExpenses = uiState.expenses.filter { it.paymentMethod == PaymentMethod.CASH && it.date >= minTimestamp }

                cashInvoices.forEach { inv ->
                    items.add(StatementItem(inv.date, "مبيعات نقدية (صندوق)", inv.invoiceNumber, "تحصيل نقدي بالدرج", inv.paidAmount, 0.0, true, 0.0))
                }
                cashExpenses.forEach { exp ->
                    items.add(StatementItem(exp.date, "مصروف نقدي", exp.expenseNumber, "${exp.category} - ${exp.paidTo}", 0.0, exp.amount, false, 0.0))
                }
            }
            3 -> {
                val filteredExpenses = uiState.expenses.filter { it.date >= minTimestamp }
                filteredExpenses.forEach { exp ->
                    items.add(StatementItem(exp.date, exp.category, exp.expenseNumber, exp.notes.ifBlank { "مصروف تشغيلي" }, exp.amount, 0.0, false, 0.0))
                }
            }
        }

        items.sortBy { it.date }
        var cumulative = 0.0
        val computed = items.map { item ->
            cumulative += (item.debit - item.credit)
            item.copy(runningBalance = cumulative)
        }
        computed.reversed()
    }

    // --- بيانات الكشف الإجمالي (Summary Statement) ---
    data class AccountSummaryRow(
        val accountId: Long,
        val accountName: String,
        val categoryLabel: String,
        val openingBalance: Double,
        val periodDebit: Double,
        val periodCredit: Double,
        val closingBalance: Double
    )

    val summaryRows = remember(
        selectedCategory,
        minTimestamp,
        hideZeroAccounts,
        uiState.parties,
        uiState.invoices,
        uiState.vouchers,
        uiState.expenses
    ) {
        val list = mutableListOf<AccountSummaryRow>()

        when (selectedCategory) {
            0 -> { // العملاء
                val customers = uiState.parties.filter { it.type == PartyType.CUSTOMER || it.type == PartyType.BOTH }
                customers.forEach { p ->
                    val pInvoicesPrev = uiState.invoices.filter { inv -> inv.partyId == p.id && inv.date < minTimestamp }
                    val pInvoicesPeriod = uiState.invoices.filter { inv -> inv.partyId == p.id && inv.date >= minTimestamp }
                    val pVouchersPrev = uiState.vouchers.filter { v -> v.partyId == p.id && v.date < minTimestamp }
                    val pVouchersPeriod = uiState.vouchers.filter { v -> v.partyId == p.id && v.date >= minTimestamp }

                    val openDebit = pInvoicesPrev.sumOf { if (it.remainingAmount > 0.001) it.remainingAmount else it.total }
                    val openCredit = pVouchersPrev.sumOf { it.amount }
                    val openingBal = openDebit - openCredit

                    val periodDebit = pInvoicesPeriod.sumOf { if (it.remainingAmount > 0.001) it.remainingAmount else it.total }
                    val periodCredit = pVouchersPeriod.sumOf { it.amount }
                    val closingBal = openingBal + periodDebit - periodCredit

                    if (!hideZeroAccounts || abs(openingBal) > 0.001 || abs(periodDebit) > 0.001 || abs(periodCredit) > 0.001 || abs(closingBal) > 0.001) {
                        list.add(AccountSummaryRow(p.id, p.name, "عميل", openingBal, periodDebit, periodCredit, closingBal))
                    }
                }
            }
            1 -> { // الموردين
                val suppliers = uiState.parties.filter { it.type == PartyType.SUPPLIER || it.type == PartyType.BOTH }
                suppliers.forEach { p ->
                    val pInvoicesPrev = uiState.invoices.filter { inv -> inv.partyId == p.id && inv.date < minTimestamp }
                    val pInvoicesPeriod = uiState.invoices.filter { inv -> inv.partyId == p.id && inv.date >= minTimestamp }
                    val pVouchersPrev = uiState.vouchers.filter { v -> v.partyId == p.id && v.date < minTimestamp }
                    val pVouchersPeriod = uiState.vouchers.filter { v -> v.partyId == p.id && v.date >= minTimestamp }

                    val openCredit = pInvoicesPrev.sumOf { if (it.remainingAmount > 0.001) it.remainingAmount else it.total }
                    val openDebit = pVouchersPrev.sumOf { it.amount }
                    val openingBal = openCredit - openDebit

                    val periodCredit = pInvoicesPeriod.sumOf { if (it.remainingAmount > 0.001) it.remainingAmount else it.total }
                    val periodDebit = pVouchersPeriod.sumOf { it.amount }
                    val closingBal = openingBal + periodCredit - periodDebit

                    if (!hideZeroAccounts || abs(openingBal) > 0.001 || abs(periodDebit) > 0.001 || abs(periodCredit) > 0.001 || abs(closingBal) > 0.001) {
                        list.add(AccountSummaryRow(p.id, p.name, "مورد", openingBal, periodDebit, periodCredit, closingBal))
                    }
                }
            }
            2 -> { // الصندوق الخزينة
                val prevCashIn = uiState.invoices.filter { it.paymentMethod == PaymentMethod.CASH && it.date < minTimestamp }.sumOf { it.paidAmount }
                val prevCashOut = uiState.expenses.filter { it.paymentMethod == PaymentMethod.CASH && it.date < minTimestamp }.sumOf { it.amount }
                val openingBal = prevCashIn - prevCashOut

                val periodDebit = uiState.invoices.filter { it.paymentMethod == PaymentMethod.CASH && it.date >= minTimestamp }.sumOf { it.paidAmount }
                val periodCredit = uiState.expenses.filter { it.paymentMethod == PaymentMethod.CASH && it.date >= minTimestamp }.sumOf { it.amount }
                val closingBal = openingBal + periodDebit - periodCredit

                if (!hideZeroAccounts || abs(openingBal) > 0.001 || abs(periodDebit) > 0.001 || abs(periodCredit) > 0.001 || abs(closingBal) > 0.001) {
                    list.add(AccountSummaryRow(101L, "صندوق النقدية والدرج الرئيسي", "خزينة", openingBal, periodDebit, periodCredit, closingBal))
                }
            }
            3 -> { // المصروفات
                val categories = uiState.expenses.map { it.category }.distinct().ifEmpty { listOf("عمومية وإدارية") }
                categories.forEachIndexed { idx, cat ->
                    val catExpPrev = uiState.expenses.filter { it.category == cat && it.date < minTimestamp }
                    val catExpPeriod = uiState.expenses.filter { it.category == cat && it.date >= minTimestamp }

                    val openingBal = catExpPrev.sumOf { it.amount }
                    val periodDebit = catExpPeriod.sumOf { it.amount }
                    val periodCredit = 0.0
                    val closingBal = openingBal + periodDebit

                    if (!hideZeroAccounts || abs(openingBal) > 0.001 || abs(periodDebit) > 0.001 || abs(closingBal) > 0.001) {
                        list.add(AccountSummaryRow((500 + idx).toLong(), "حساب مصروفات: $cat", "مصروف", openingBal, periodDebit, periodCredit, closingBal))
                    }
                }
            }
        }
        list
    }

    val totalDebitSum = ledgerData.sumOf { it.debit }
    val totalCreditSum = ledgerData.sumOf { it.credit }

    val totalOpeningSum = summaryRows.sumOf { it.openingBalance }
    val totalPeriodDebitSum = summaryRows.sumOf { it.periodDebit }
    val totalPeriodCreditSum = summaryRows.sumOf { it.periodCredit }
    val totalClosingSum = summaryRows.sumOf { it.closingBalance }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // عنوان التقرير الموسّط
        item {
            Text(
                text = "تقرير كشوفات الحسابات المليء ${if (hideZeroAccounts) "(بدون الحسابات الصفرية)" else ""}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F5132),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
            )
        }

        // 1. أزرار راديو Radio Buttons للتبديل السريع بين الكشف التفصيلي والكشف الإجمالي
        item {
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("نوع تقرير كشف الحساب:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF1E293B))
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF8FAFC), shape = RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { onSelectStatementMode(0) }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                .testTag("radio_mode_detailed")
                        ) {
                            RadioButton(
                                selected = (statementMode == 0),
                                onClick = { onSelectStatementMode(0) },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF0F5132))
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "كشف تفصيلي (Detailed)",
                                fontSize = 11.sp,
                                fontWeight = if (statementMode == 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (statementMode == 0) Color(0xFF0F5132) else Color(0xFF475569)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { onSelectStatementMode(1) }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                .testTag("radio_mode_summary")
                        ) {
                            RadioButton(
                                selected = (statementMode == 1),
                                onClick = { onSelectStatementMode(1) },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF0F5132))
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "كشف إجمالي (Summary)",
                                fontSize = 11.sp,
                                fontWeight = if (statementMode == 1) FontWeight.Bold else FontWeight.Normal,
                                color = if (statementMode == 1) Color(0xFF0F5132) else Color(0xFF475569)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text("اختر فئة الحساب:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF475569))
                    Spacer(modifier = Modifier.height(4.dp))

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val categories = listOf(
                            Pair("حسابات العملاء", Icons.Default.Person),
                            Pair("حسابات الموردين", Icons.Default.LocalShipping),
                            Pair("الصندوق والخزينة", Icons.Default.PointOfSale),
                            Pair("المصروفات بالنثريات", Icons.Default.MoneyOff)
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
                                label = { Text(catName, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (statementMode == 0 && (selectedCategory == 0 || selectedCategory == 1)) {
                            val selectedPartyName = filteredParties.find { it.id == selectedPartyId }?.name ?: "جميع الحسابات"
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedButton(
                                    onClick = { partyDropdownExpanded = true },
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(selectedPartyName, fontSize = 11.sp, maxLines = 1)
                                }

                                DropdownMenu(
                                    expanded = partyDropdownExpanded,
                                    onDismissRequest = { partyDropdownExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("جميع الحسابات", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                                        onClick = {
                                            selectedPartyId = null
                                            partyDropdownExpanded = false
                                        }
                                    )
                                    filteredParties.forEach { p ->
                                        DropdownMenuItem(
                                            text = { Text(p.name, fontSize = 11.sp) },
                                            onClick = {
                                                selectedPartyId = p.id
                                                partyDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            val periods = listOf("الكل", "اليوم", "الأسبوع", "الشهر")
                            items(periods.size) { idx ->
                                FilterChip(
                                    selected = selectedPeriod == idx,
                                    onClick = { selectedPeriod = idx },
                                    label = { Text(periods[idx], fontSize = 10.sp) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. إذا كان نمط العرض هو الكشف التفصيلي (statementMode == 0)
        if (statementMode == 0) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)), shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("إجمالي مدين (+)", fontSize = 10.sp, color = Color(0xFF166534))
                            Text("${"%.2f".format(totalDebitSum)} $currencySymbol", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                        }
                    }
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)), shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("إجمالي دائن (-)", fontSize = 10.sp, color = Color(0xFF991B1B))
                            Text("${"%.2f".format(totalCreditSum)} $currencySymbol", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                        }
                    }
                }
            }

            items(ledgerData) { item ->
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(item.typeLabel, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF0F172A))
                                Spacer(modifier = Modifier.width(4.dp))
                                Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFF1F5F9)) {
                                    Text("#${item.refNumber}", fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                }
                            }
                            Text(item.description, fontSize = 10.sp, color = Color(0xFF64748B), maxLines = 1)
                            Text(dateFormat.format(Date(item.date)), fontSize = 9.sp, color = Color(0xFF94A3B8))
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            if (item.debit > 0) {
                                Text("+${"%.2f".format(item.debit)} $currencySymbol", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF16A34A))
                            } else if (item.credit > 0) {
                                Text("-${"%.2f".format(item.credit)} $currencySymbol", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFDC2626))
                            }
                            Text("الرصيد: ${"%.2f".format(item.runningBalance)} $currencySymbol", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF475569))
                        }
                    }
                }
            }

            if (ledgerData.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                        Text("لا توجد حركات مسجلة بهذا الحساب بالفترة المحددة", color = Color(0xFF64748B), fontSize = 11.sp)
                    }
                }
            }
        }
        // 3. إذا كان نمط العرض هو الكشف الإجمالي (statementMode == 1)
        else {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)), shape = RoundedCornerShape(6.dp), modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(6.dp)) {
                            Text("الرصيد السابق", fontSize = 9.sp, color = Color(0xFF475569))
                            Text("${"%.2f".format(totalOpeningSum)} $currencySymbol", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                        }
                    }
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)), shape = RoundedCornerShape(6.dp), modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(6.dp)) {
                            Text("مدين الفترة (+)", fontSize = 9.sp, color = Color(0xFF166534))
                            Text("${"%.2f".format(totalPeriodDebitSum)} $currencySymbol", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                        }
                    }
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)), shape = RoundedCornerShape(6.dp), modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(6.dp)) {
                            Text("دائن الفترة (-)", fontSize = 9.sp, color = Color(0xFF991B1B))
                            Text("${"%.2f".format(totalPeriodCreditSum)} $currencySymbol", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                        }
                    }
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)), shape = RoundedCornerShape(6.dp), modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(6.dp)) {
                            Text("الرصيد الختامي", fontSize = 9.sp, color = Color(0xFF1E40AF))
                            Text("${"%.2f".format(totalClosingSum)} $currencySymbol", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D4ED8))
                        }
                    }
                }
            }

            // ترويسة الجدول الإجمالي
            item {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF1E293B),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("اسم الحساب / الجهة", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.White, modifier = Modifier.weight(2f))
                        Text("رصيد سابق", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.White, modifier = Modifier.weight(1f))
                        Text("مدين (+)", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color(0xFF86EFAC), modifier = Modifier.weight(1f))
                        Text("دائن (-)", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color(0xFFFCA5A5), modifier = Modifier.weight(1f))
                        Text("رصيد ختامي", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color(0xFF93C5FD), modifier = Modifier.weight(1f))
                    }
                }
            }

            items(summaryRows) { row ->
                Card(
                    shape = RoundedCornerShape(6.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(2f)) {
                            Text(row.accountName, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF0F172A))
                            Text(row.categoryLabel, fontSize = 9.sp, color = Color(0xFF64748B))
                        }
                        Text("${"%.2f".format(row.openingBalance)}", fontSize = 10.sp, color = Color(0xFF475569), modifier = Modifier.weight(1f))
                        Text("${"%.2f".format(row.periodDebit)}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D), modifier = Modifier.weight(1f))
                        Text("${"%.2f".format(row.periodCredit)}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626), modifier = Modifier.weight(1f))
                        Text(
                            text = "${"%.2f".format(row.closingBalance)}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (row.closingBalance >= 0) Color(0xFF1D4ED8) else Color(0xFFB91C1C),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            if (summaryRows.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                        Text("لا توجد أرصدة أو حسابات متوفرة في هذه الفئة", color = Color(0xFF64748B), fontSize = 11.sp)
                    }
                }
            }

            // سطر المجموع الكلي للإجمالي
            item {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF0F5132),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("إجمالي الفئة المحدد:", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.White, modifier = Modifier.weight(2f))
                        Text("${"%.2f".format(totalOpeningSum)}", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.White, modifier = Modifier.weight(1f))
                        Text("${"%.2f".format(totalPeriodDebitSum)}", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color(0xFF86EFAC), modifier = Modifier.weight(1f))
                        Text("${"%.2f".format(totalPeriodCreditSum)}", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color(0xFFFCA5A5), modifier = Modifier.weight(1f))
                        Text("${"%.2f".format(totalClosingSum)} $currencySymbol", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color(0xFF93C5FD), modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/**
 * تبويب الحركة والتحليلات للأصناف والنواقص والصلاحيات
 */
@Composable
private fun TopProductsAndHealthView(
    topReport: TopProductsReport?,
    healthReport: InventoryHealthReport?,
    currencySymbol: String = "ر.ي"
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // عنوان موسّط
        item {
            Text(
                text = "تقرير حركة المبيعات والمخزون والتحليلات",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F5132),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
            )
        }

        // الأصناف الأكثر حركة
        item {
            Text("الأصناف الأكثر مبيعاً وحركة (الأعلى تصريفاً):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF1E293B))
        }

        if (topReport != null) {
            items(topReport.topMovingByQuantity.take(5)) { item ->
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(item.productName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF0F172A))
                            Text("إجمالي المبيعات: ${"%.2f".format(item.totalRevenue)} $currencySymbol", fontSize = 10.sp, color = Color(0xFF64748B))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${"%.1f".format(item.totalQuantitySold)} ${item.baseUnitName}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF0F5132))
                            Text("ربح: ${"%.2f".format(item.grossProfit)} $currencySymbol", fontSize = 10.sp, color = Color(0xFF16A34A))
                        }
                    }
                }
            }
        }

        // النواقص والصلاحيات
        if (healthReport != null) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text("الأصناف التي وصلت حد النواقص وإعادة الطلب (${healthReport.totalLowStockCount}):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF1E293B))
            }

            items(healthReport.lowStockItems.take(5)) { item ->
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(item.name, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF0F172A))
                        }
                        Text("المتبقي: ${"%.1f".format(item.currentStock)} ${item.baseUnitName}", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFFDC2626))
                    }
                }
            }
        }
    }
}
