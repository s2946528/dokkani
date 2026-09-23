package com.example.dokkani.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dokkani.data.local.entities.CashShiftEntity
import com.example.dokkani.data.local.entities.FinancialAccountEntity
import com.example.dokkani.data.local.entities.FinancialAccountType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * كرت الشفت التفاعلي والمنظم (Shift Status Bar Card)
 * يتميز بتثبيته بين الترويسة العلوية وقسم الفواتير والسندات ويحتوي على 3 عناصر في سطر أفقي واحد:
 * 1. إجمالي الصندوق (العنوان والحيصات/الرصيد)
 * 2. إجمالي البنوك (العنوان والرصيد الإجمالي للبنوك والمحافظ والشبكات)
 * 3. زر إغلاق الشفت (مع أيقونة القفل)
 */
@Composable
fun ShiftStatusBar(
    currentShift: CashShiftEntity?,
    cashInDrawer: Double,
    financialAccounts: List<FinancialAccountEntity>,
    currencySymbol: String = "ر.س",
    onOpenCashBreakdown: () -> Unit,
    onOpenBankBreakdown: () -> Unit,
    onOpenShiftCloseDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shift = currentShift ?: CashShiftEntity(
        shiftNumber = "SHF-0001",
        cashierName = "كاشير 1",
        openingCash = 0.0,
        expectedCashInDrawer = cashInDrawer
    )

    val totalDigitalInShift = remember(shift, financialAccounts) {
        val bankAndWalletAccountsSum = financialAccounts
            .filter { it.accountType == FinancialAccountType.BANK || it.accountType == FinancialAccountType.E_WALLET }
            .sumOf { it.currentBalance }
        val shiftDigitalSales = shift.totalMadaSales + shift.totalWalletSales + shift.totalTransferSales
        if (bankAndWalletAccountsSum > 0.0) bankAndWalletAccountsSum else shiftDigitalSales
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. عنصر إجمالي الصندوق
            ShiftStatusCard(
                title = "إجمالي الصندوق",
                amount = cashInDrawer,
                currencySymbol = currencySymbol,
                icon = Icons.Default.Payments,
                containerColor = Color(0xFFE8F5E9),
                contentColor = Color(0xFF1B5E20),
                accentColor = Color(0xFF2E7D32),
                subtext = "نقداً 🔍",
                onClick = onOpenCashBreakdown,
                modifier = Modifier.weight(1f)
            )

            // 2. عنصر إجمالي البنوك (يشمل البنوك والمحافظ والشبكات)
            ShiftStatusCard(
                title = "إجمالي البنوك",
                amount = totalDigitalInShift,
                currencySymbol = currencySymbol,
                icon = Icons.Default.AccountBalance,
                containerColor = Color(0xFFE3F2FD),
                contentColor = Color(0xFF0D47A1),
                accentColor = Color(0xFF1565C0),
                subtext = "بنوك/محافظ/شبكات 📊",
                onClick = onOpenBankBreakdown,
                modifier = Modifier.weight(1f)
            )

            // 3. زر إغلاق الشفت
            Button(
                onClick = onOpenShiftCloseDialog,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD32F2F),
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "إغلاق الشفت",
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "إغلاق الشفت",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun ShiftBottomStatusBar(
    currentShift: CashShiftEntity?,
    cashInDrawer: Double,
    financialAccounts: List<FinancialAccountEntity>,
    currencySymbol: String = "ر.س",
    onOpenCashBreakdown: () -> Unit,
    onOpenBankBreakdown: () -> Unit,
    onOpenShiftCloseDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    ShiftStatusBar(
        currentShift = currentShift,
        cashInDrawer = cashInDrawer,
        financialAccounts = financialAccounts,
        currencySymbol = currencySymbol,
        onOpenCashBreakdown = onOpenCashBreakdown,
        onOpenBankBreakdown = onOpenBankBreakdown,
        onOpenShiftCloseDialog = onOpenShiftCloseDialog,
        modifier = modifier
    )
}

@Composable
private fun ShiftStatusCard(
    title: String,
    amount: Double,
    currencySymbol: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    contentColor: Color,
    accentColor: Color,
    subtext: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = contentColor.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "%.2f %s".format(amount, currencySymbol),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = subtext,
                    fontSize = 9.sp,
                    color = accentColor,
                    fontWeight = FontWeight.Medium
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = null,
                tint = accentColor.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * نافذة تفاصيل صناديق النقدية (Cash Drawer Breakdown Dialog)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashDrawerBreakdownDialog(
    shift: CashShiftEntity?,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onOpenShiftClose: () -> Unit,
    onRefresh: () -> Unit = {}
) {
    val currentShift = shift ?: CashShiftEntity(shiftNumber = "SHF-0001")
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    var isRefreshing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = null,
        text = {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    isRefreshing = true
                    onRefresh()
                    coroutineScope.launch {
                        delay(600)
                        isRefreshing = false
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // ترويسة النافذة
                    Surface(
                        color = Color(0xFF1B5E20),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Payments,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        "تفاصيل صناديق النقدية",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.White
                                    )
                                }

                                Surface(
                                    color = Color.White.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = currentShift.shiftNumber,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Text(
                                text = "المسؤول: ${currentShift.cashierName} | البدء: ${dateFormat.format(Date(currentShift.startTime))}",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }

                    // 1. الواردات النقدية (Inflows)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                        border = BorderStroke(1.dp, Color(0xFF81C784)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                "📥 الواردات النقدية (مقبوضات الدرج)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF1B5E20)
                            )
                            HorizontalDivider(color = Color(0xFFA5D6A7))

                            BreakdownRow("عهدة البداية (الافتتاحية):", currentShift.openingCash, currencySymbol, isPositive = true, labelColor = Color(0xFF1B5E20))
                            BreakdownRow("المبيعات النقدية المباشرة:", currentShift.totalCashSales, currencySymbol, isPositive = true, labelColor = Color(0xFF1B5E20))
                            BreakdownRow("سندات القبض والتحصيل:", currentShift.totalCashCollections, currencySymbol, isPositive = true, labelColor = Color(0xFF1B5E20))

                            HorizontalDivider(color = Color(0xFFA5D6A7))
                            BreakdownRow(
                                "إجمالي المقبوضات والعهدة:",
                                currentShift.openingCash + currentShift.totalCashSales + currentShift.totalCashCollections,
                                currencySymbol,
                                isPositive = true,
                                isTotal = true,
                                labelColor = Color(0xFF1B5E20)
                            )
                        }
                    }

                    // 2. الصادرات والمصروفات النقدية (Outflows)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                        border = BorderStroke(1.dp, Color(0xFFE57373)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                "📤 الصادرات والمصروفات النقدية (مدفوعات الدرج)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFFB71C1C)
                            )
                            HorizontalDivider(color = Color(0xFFEF9A9A))

                            BreakdownRow("المصروفات التشغيلية:", currentShift.totalCashExpenses, currencySymbol, isPositive = false, labelColor = Color(0xFFB71C1C))
                            BreakdownRow("تسديدات الموردين نقداً:", currentShift.totalSupplierPayments, currencySymbol, isPositive = false, labelColor = Color(0xFFB71C1C))
                            BreakdownRow("المشتريات النقدية المباشرة:", currentShift.totalCashPurchases, currencySymbol, isPositive = false, labelColor = Color(0xFFB71C1C))
                            BreakdownRow("مسحوبات صاحب المتجر:", currentShift.totalOwnerDrawings, currencySymbol, isPositive = false, labelColor = Color(0xFFB71C1C))
                            BreakdownRow("سلف ومسحوبات العمال:", currentShift.totalStaffAdvances, currencySymbol, isPositive = false, labelColor = Color(0xFFB71C1C))

                            HorizontalDivider(color = Color(0xFFEF9A9A))
                            BreakdownRow(
                                "إجمالي المدفوعات والصادرات:",
                                currentShift.totalOutflows,
                                currencySymbol,
                                isPositive = false,
                                isTotal = true,
                                labelColor = Color(0xFFB71C1C)
                            )
                        }
                    }

                    // 3. صافي النقدية المتوقعة في الدرج
                    Surface(
                        color = Color(0xFF2E7D32),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("صافي النقدية الدفترية المتوقعة:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                Text("المفروض توجد حالياً بالدرج", fontSize = 10.sp, color = Color.White.copy(alpha = 0.85f))
                            }

                            Text(
                                text = "%.2f %s".format(currentShift.expectedCashInDrawer, currencySymbol),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                    onOpenShiftClose()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
            ) {
                Icon(Icons.Default.FactCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("إغلاق الشفت ومطابقة الكاش")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}

/**
 * نافذة تفاصيل البنوك والمحافظ والشبكة (Bank & E-Wallets Breakdown Dialog)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankWalletsBreakdownDialog(
    shift: CashShiftEntity?,
    financialAccounts: List<FinancialAccountEntity>,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit = {}
) {
    val currentShift = shift ?: CashShiftEntity(shiftNumber = "SHF-0001")
    val totalDigitalInShift = currentShift.totalMadaSales + currentShift.totalWalletSales + currentShift.totalTransferSales
    var isRefreshing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = null,
        text = {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    isRefreshing = true
                    onRefresh()
                    coroutineScope.launch {
                        delay(600)
                        isRefreshing = false
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // ترويسة النافذة
                    Surface(
                        color = Color(0xFF1565C0),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        "تفاصيل البنوك والمحافظ والشبكات",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.White
                                    )
                                }

                                Surface(
                                    color = Color.White.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "إلكتروني",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Text(
                                text = "إجمالي التدفقات الرقمية للشفت الحالي: %.2f %s".format(totalDigitalInShift, currencySymbol),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.95f)
                            )
                        }
                    }

                    // 1. توزيع حركات الشفت الإلكترونية حسب القناة
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                        border = BorderStroke(1.dp, Color(0xFF90CAF9)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                "📊 المبيعات الإلكترونية بالشفت الحالي",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF0D47A1)
                            )
                            HorizontalDivider(color = Color(0xFFBBDEFB))

                            BreakdownRow("💳 شبكة نقاط بيع ومدى (POS):", currentShift.totalMadaSales, currencySymbol, isPositive = true, labelColor = Color(0xFF0D47A1))
                            BreakdownRow("📱 المحافظ الإلكترونية (E-Wallets):", currentShift.totalWalletSales, currencySymbol, isPositive = true, labelColor = Color(0xFF0D47A1))
                            BreakdownRow("🏦 التحويلات المصرفية (Bank Transfers):", currentShift.totalTransferSales, currencySymbol, isPositive = true, labelColor = Color(0xFF0D47A1))
                            BreakdownRow("📝 المبيعات الآجلة (على الحساب):", currentShift.totalCreditSales, currencySymbol, isPositive = false, labelColor = Color(0xFF0D47A1))

                            HorizontalDivider(color = Color(0xFFBBDEFB))
                            BreakdownRow(
                                "مجموع السداد الإلكتروني:",
                                totalDigitalInShift,
                                currencySymbol,
                                isPositive = true,
                                isTotal = true,
                                labelColor = Color(0xFF0D47A1)
                            )
                        }
                    }

                    // 2. أرصدة الحسابات المالية والبنوك المربوطة بالمتجر
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "🏦 أرصدة الحسابات البنكية والمحافظ الحالية:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        val nonCashAccounts = remember(financialAccounts) {
                            financialAccounts.filter { it.accountType != FinancialAccountType.CASH_DRAWER }
                        }

                        if (nonCashAccounts.isEmpty()) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "لم يتم ربط حسابات بنكية أو محافظ بعد، يمكنك إضافتها من شاشة الحسابات والمالية.",
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(10.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            nonCashAccounts.forEach { acc ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(
                                                        when (acc.accountType) {
                                                            FinancialAccountType.BANK -> Color(0xFF1565C0).copy(alpha = 0.15f)
                                                            FinancialAccountType.E_WALLET -> Color(0xFF6A1B9A).copy(alpha = 0.15f)
                                                            else -> Color(0xFF00838F).copy(alpha = 0.15f)
                                                        }
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = when (acc.accountType) {
                                                        FinancialAccountType.E_WALLET -> Icons.Default.Smartphone
                                                        FinancialAccountType.BANK -> Icons.Default.AccountBalance
                                                        else -> Icons.Default.CreditCard
                                                    },
                                                    contentDescription = null,
                                                    tint = when (acc.accountType) {
                                                        FinancialAccountType.BANK -> Color(0xFF1565C0)
                                                        FinancialAccountType.E_WALLET -> Color(0xFF6A1B9A)
                                                        else -> Color(0xFF00838F)
                                                    },
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }

                                            Column {
                                                Text(
                                                    acc.name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "${acc.code} - ${acc.accountType.labelArabic}",
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Text(
                                            text = "%.2f %s".format(acc.currentBalance, currencySymbol),
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 12.sp,
                                            color = if (acc.currentBalance >= 0) Color(0xFF1B5E20) else Color(0xFFB71C1C)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
            ) {
                Text("حسناً")
            }
        }
    )
}

@Composable
private fun BreakdownRow(
    label: String,
    amount: Double,
    currencySymbol: String,
    isPositive: Boolean,
    isTotal: Boolean = false,
    labelColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = if (isTotal) 12.sp else 11.sp,
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Medium,
            color = labelColor
        )

        Text(
            text = "${if (isPositive) "+" else "-"}%.2f %s".format(amount, currencySymbol),
            fontSize = if (isTotal) 12.sp else 11.sp,
            fontWeight = if (isTotal) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = if (isPositive) Color(0xFF2E7D32) else Color(0xFFC62828)
        )
    }
}
