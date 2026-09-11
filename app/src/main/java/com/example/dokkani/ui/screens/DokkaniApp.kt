package com.example.dokkani.ui.screens
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Person

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Settings
import com.example.dokkani.ui.screens.cash.CashAndExpensesScreen
import com.example.dokkani.ui.screens.credit.CreditLedgerScreen
import com.example.dokkani.ui.screens.license.LicenseScreen
import com.example.dokkani.ui.screens.reports.ReportsDashboardScreen
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.dokkani.ui.DokkaniViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DokkaniApp(viewModel: DokkaniViewModel, currentUserRole: com.example.dokkani.data.local.entities.UserRole = com.example.dokkani.data.local.entities.UserRole.ADMIN, onLogout: () -> Unit = {}) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val productsWithUnits by viewModel.productsWithUnits.collectAsStateWithLifecycle()
    val currencies by viewModel.currencies.collectAsStateWithLifecycle()
    val parties by viewModel.parties.collectAsStateWithLifecycle()
    val recentInvoices by viewModel.recentInvoices.collectAsStateWithLifecycle()
    val produceBatches by viewModel.produceBatches.collectAsStateWithLifecycle()
    val settings by viewModel.systemSettings.collectAsStateWithLifecycle()
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val cashShifts by viewModel.cashShifts.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.userNotification) {
        uiState.userNotification?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearNotification()
        }
    }

    // لغة الواجهة من اليمين إلى اليسار (RTL) لدعم المحاسبة والبقالة العربية
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF0F5132),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_dokkani_icon),
                                        contentDescription = "Dokkani Logo",
                                        modifier = Modifier.size(34.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "دكاني (Dokkani POS)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = settings?.storeName ?: "نظام المحاسبة ونقطة البيع",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFD1E7DD)
                                )
                            }
                        }
                    },
                    actions = {
                        androidx.compose.material3.IconButton(onClick = onLogout) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "تسجيل الخروج", tint = Color.White)
                        }
                        // شارة حالة الترخيص والحماية
                        val eval = uiState.licenseEvaluation
                        val badgeColor = when {
                            eval?.isLocked == true -> Color(0xFFDC3545)
                            eval?.isLifetime == true -> Color(0xFF198754)
                            eval?.isNearExpiryWarning == true -> Color(0xFFFD7E14)
                            else -> Color(0xFF0D6EFD)
                        }
                        val badgeText = when {
                            eval?.isTimeTampered == true -> "تلاعب في الوقت ✕"
                            eval?.isLocked == true -> "الترخيص مقفل ✕"
                            eval?.isLifetime == true -> "مرخص دائم ✓"
                            eval?.status == com.example.dokkani.domain.security.LicenseStatus.SUBSCRIPTION -> "${eval.daysRemaining} يوم متبقي"
                            else -> "تجربة (${eval?.invoicesRemaining ?: 0})"
                        }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = badgeColor,
                            onClick = { viewModel.selectTab(8) },
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .testTag("top_license_badge")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = if (eval?.isLocked == true) Icons.Default.Lock else Icons.Default.VpnKey,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = badgeText,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        // شارة طريقة التقييم المحاسبي الحالية
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF198754),
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .testTag("top_valuation_badge")
                        ) {
                            Text(
                                text = "معيار: ${settings?.costValuationMethod?.name ?: "WAC"}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF0D3B2E)
                    )
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // شريط التبويبات الرئيسي
                val tabs = listOf(
                    "نقطة البيع (POS)" to Icons.Default.PointOfSale,
                    "دفتر الشكك والديون" to Icons.Default.MenuBook,
                    "الخزينة والمصروفات" to Icons.Default.Payments,
                    "التقارير ولوحة التحكم" to Icons.Default.BarChart,
                    "حاسبة التكلفة (WAC/FIFO)" to Icons.Default.Calculate,
                    "جرد خضار وفوضويات" to Icons.Default.Eco,
                    "طابعة ملصقات الباركود" to Icons.Default.QrCode,
                    "الأصناف والوحدات" to Icons.Default.Inventory2,
                    "الترخيص وحماية النظام" to Icons.Default.VpnKey,
                    "المخطط وقاعدة البيانات" to Icons.Default.AccountBalance,
                    "إعدادات النظام" to Icons.Default.Settings,
                    "إدارة المستخدمين" to Icons.Default.Person
                )

                ScrollableTabRow(
                    selectedTabIndex = uiState.selectedTab,
                    edgePadding = 12.dp,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth().testTag("primary_tab_row")
                ) {
                    tabs.forEachIndexed { index, pair ->
                        Tab(
                            selected = uiState.selectedTab == index,
                            onClick = { viewModel.selectTab(index) },
                            text = {
                                Text(
                                    text = pair.first,
                                    fontWeight = if (uiState.selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = pair.second,
                                    contentDescription = pair.first,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            modifier = Modifier.testTag("tab_item_$index")
                        )
                    }
                }

                // محتوى التبويب المختار
                
                val isCashier = currentUserRole == com.example.dokkani.data.local.entities.UserRole.CASHIER
                val isInventory = currentUserRole == com.example.dokkani.data.local.entities.UserRole.INVENTORY
                val isAdmin = currentUserRole == com.example.dokkani.data.local.entities.UserRole.ADMIN
                
                val hasAccess = when (uiState.selectedTab) {
                    0, 1, 2 -> isAdmin || isCashier
                    4, 5, 6, 7 -> isAdmin || isInventory
                    else -> isAdmin
                }

                if (!hasAccess) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        androidx.compose.material3.Text("عذراً، ليس لديك صلاحية للوصول إلى هذه الشاشة.")
                    }
                } else when (uiState.selectedTab) {

                    0 -> com.example.dokkani.ui.screens.pos.PosScreen(
                        viewModel = viewModel
                    )
                    1 -> CreditLedgerScreen(
                        parties = parties,
                        uiState = uiState,
                        onSearchChanged = { viewModel.updateCreditSearchQuery(it) },
                        onSelectParty = { viewModel.selectPartyForStatement(it) },
                        onOpenPaymentVoucherDialog = { viewModel.openPaymentVoucherDialog(it) },
                        onDismissPaymentVoucherDialog = { viewModel.dismissPaymentVoucherDialog() },
                        onVoucherInputsChanged = { amt, notes, method ->
                            viewModel.updateVoucherInputs(amt, notes, method)
                        },
                        onSubmitPaymentVoucher = { viewModel.submitPaymentVoucher() },
                        onSendWhatsAppReminder = { ctx, phone, text ->
                            viewModel.sendWhatsAppDebtReminder(ctx, phone, text)
                        }
                    )
                    2 -> CashAndExpensesScreen(
                        expenses = expenses,
                        cashShifts = cashShifts,
                        uiState = uiState,
                        onSelectSubTab = { viewModel.selectCashSubTab(it) },
                        onOpenAddExpenseDialog = { viewModel.openAddExpenseDialog() },
                        onDismissAddExpenseDialog = { viewModel.dismissAddExpenseDialog() },
                        onExpenseInputsChanged = { cat, amt, to, notes, method ->
                            viewModel.updateExpenseInputs(cat, amt, to, notes, method)
                        },
                        onSubmitExpense = { viewModel.submitExpense() },
                        onDrawerInputsChanged = { open, phys, notes ->
                            viewModel.updateDrawerInputs(open, phys, notes)
                        },
                        onCalculateDrawerReconciliation = { viewModel.calculateDrawerReconciliation() },
                        onCloseShiftAndSave = { viewModel.closeShiftAndSave() }
                    )
                    3 -> ReportsDashboardScreen(
                        uiState = uiState,
                        onSelectSubTab = { viewModel.selectReportSubTab(it) },
                        onSelectValuationMethod = { viewModel.selectReportValuationMethod(it) },
                        onRefreshReports = { viewModel.loadAllReports() }
                    )
                    4 -> CostingEngineScreen(
                        productsWithUnits = productsWithUnits,
                        selectedProductId = uiState.selectedProductIdForCosting,
                        selectedMethod = uiState.selectedMethodForCosting,
                        costingResult = uiState.costingResult,
                        wacResult = uiState.wacComparisonResult,
                        fifoResult = uiState.fifoComparisonResult,
                        lppResult = uiState.lastPurchaseComparisonResult,
                        activeLots = uiState.activeLotsForProduct,
                        onSelectProduct = { viewModel.selectProductForCosting(it) },
                        onSelectMethod = { viewModel.selectCostingMethod(it) },
                        onSaveMethodToSettings = { viewModel.updateSystemCostingMethod(it) },
                        onAddSimulatedPurchaseBatch = { qty, cost -> viewModel.addSimulatedPurchaseBatch(qty, cost) }
                    )
                    5 -> ProduceQuickInventoryScreen(
                        produceAuditSubTab = uiState.produceAuditSubTab,
                        productsWithUnits = productsWithUnits,
                        selectedProduceProductId = uiState.selectedProduceProductIdForAudit,
                        auditBeginningQty = uiState.produceAuditBeginningQty,
                        auditBeginningCost = uiState.produceAuditBeginningCost,
                        auditPurchasesQty = uiState.produceAuditPurchasesQty,
                        auditPurchasesCost = uiState.produceAuditPurchasesCost,
                        auditEndingQty = uiState.produceAuditEndingQty,
                        auditWasteQty = uiState.produceAuditWasteQty,
                        auditPosSoldQty = uiState.produceAuditPosSoldQty,
                        auditPosRevenue = uiState.produceAuditPosRevenue,
                        auditResult = uiState.produceAuditResult,
                        isSubmittingAudit = uiState.isSubmittingProduceAudit,
                        onSelectSubTab = { viewModel.selectProduceAuditSubTab(it) },
                        onSelectProduceProduct = { viewModel.selectProduceProductForAudit(it) },
                        onAuditInputsChanged = { bq, bc, pq, pc, eq, wq, ps, pr ->
                            viewModel.updateProduceAuditInputs(bq, bc, pq, pc, eq, wq, ps, pr)
                        },
                        onCommitSilentAdjustments = { viewModel.commitProduceAuditSilentAdjustments() },
                        onNavigateToBarcodePrinter = { prodId -> viewModel.openLabelPrinterForProduct(prodId) },
                        grossWeightInput = uiState.produceGrossWeightInput,
                        costInput = uiState.produceCostInput,
                        expenseInput = uiState.produceExpenseInput,
                        wasteInput = uiState.produceWasteInput,
                        marginInput = uiState.produceMarginInput,
                        crateDescription = uiState.produceCrateDescription,
                        calcSummary = uiState.produceCalcSummary,
                        isSavingBatch = uiState.isSavingProduceBatch,
                        historicalBatches = produceBatches,
                        onCrateInputsChanged = { g, c, e, w, m, d -> viewModel.updateProduceInputs(g, c, e, w, m, d) },
                        onSaveCrateBatch = { viewModel.saveProduceBatchToDatabase() }
                    )
                    6 -> com.example.dokkani.ui.screens.barcode.BarcodeLabelPrinterScreen(
                        productsWithUnits = productsWithUnits,
                        selectedProductId = uiState.labelSelectedProductId,
                        selectedUnitId = uiState.labelSelectedUnitId,
                        labelPaperSize = uiState.labelPaperSize,
                        labelCopies = uiState.labelCopies,
                        showStoreName = uiState.labelShowStoreName,
                        showUnitName = uiState.labelShowUnitName,
                        showPrice = uiState.labelShowPrice,
                        showBarcodeText = uiState.labelShowBarcodeText,
                        showTaxNote = uiState.labelShowTaxNote,
                        customBarcode = uiState.labelCustomBarcode,
                        isGeneratingBarcode = uiState.isGeneratingBarcode,
                        isPrintingLabel = uiState.isPrintingLabel,
                        lastPrintResult = uiState.lastLabelPrintResult,
                        settings = settings,
                        onSelectProduct = { viewModel.selectProductForLabel(it) },
                        onSelectUnit = { viewModel.selectUnitForLabel(it) },
                        onPaperSizeChanged = { viewModel.updateLabelPaperSize(it) },
                        onCopiesChanged = { viewModel.updateLabelCopies(it) },
                        onToggleOption = { sn, un, pr, bt, tn ->
                            viewModel.toggleLabelOption(sn, un, pr, bt, tn)
                        },
                        onBarcodeChanged = { viewModel.updateCustomBarcode(it) },
                        onGenerateUniqueBarcode = { viewModel.generateUniqueBarcodeForCurrentUnit() },
                        onPrintLabel = { viewModel.printBarcodeLabel() }
                    )
                    7 -> ProductsAndUnitsScreen(
                        productsWithUnits = productsWithUnits,
                        onPrintLabel = { prodId, unitId -> viewModel.openLabelPrinterForProduct(prodId, unitId) }
                    )
                    8 -> LicenseScreen(
                        uiState = uiState,
                        onSelectPlanForRequest = { viewModel.selectPlanForRequest(it) },
                        onRefreshChallengeCode = { viewModel.refreshChallengeCode() },
                        onActivationCodeChanged = { viewModel.updateActivationCodeInput(it) },
                        onApplyActivationCode = { viewModel.applyActivationCode() },
                        onToggleDeveloperKeyGen = { viewModel.toggleDeveloperKeyGen() },
                        onKeyGenRequestInputChanged = { viewModel.updateKeyGenRequestInput(it) },
                        onKeyGenPlanChanged = { viewModel.updateKeyGenSelectedPlan(it) },
                        onKeyGenCustomDaysChanged = { viewModel.updateKeyGenCustomDays(it) },
                        onGenerateKeyGenCode = { viewModel.generateKeyGenCode() },
                        onApplyGeneratedKeyGenCodeDirectly = { viewModel.applyGeneratedKeyGenCodeDirectly() },
                        onResetTrialForTesting = { viewModel.resetTrialForTesting() },
                        onSimulateTimeTamperForTesting = { viewModel.simulateTimeTamperForTesting() },
                        onClearTimeTamper = { viewModel.clearTimeTamper() },
                        onCopyToClipboard = { ctx, text, label -> viewModel.copyToClipboard(ctx, text, label) },
                        onShareViaWhatsApp = { ctx, text -> viewModel.shareViaWhatsApp(ctx, text) }
                    )
                    9 -> SchemaOverviewScreen(
                        productsWithUnits = productsWithUnits,
                        settings = settings,
                        currenciesCount = currencies.size,
                        partiesCount = parties.size,
                        invoicesCount = recentInvoices.size,
                        produceBatchesCount = produceBatches.size
                    )
                    10 -> SystemSettingsScreen(
                        settings = settings,
                        currencies = currencies,
                        parties = parties,
                        invoices = recentInvoices,
                        onUpdateValuationMethod = { viewModel.updateSystemCostingMethod(it) }
                    )
                    11 -> {
                        if (currentUserRole == com.example.dokkani.data.local.entities.UserRole.ADMIN) {
                            val userManagementViewModel: com.example.dokkani.ui.screens.users.UserManagementViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                            com.example.dokkani.ui.screens.users.UserManagementScreen(viewModel = userManagementViewModel)
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                                androidx.compose.material3.Text("عذراً، ليس لديك صلاحية للوصول إلى هذه الشاشة.")
                            }
                        }
                    }
                }
            }
        }

        // نافذة قفل التطبيق الأمني في حال انتهاء الصلاحية أو التلاعب
        if (uiState.showLicenseLockDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissLicenseLockDialog() },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = {
                    Text(
                        text = "تنبيه: قفل ترخيص دكاني",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = uiState.licenseLockDialogMessage.ifBlank {
                            "النظام مقفل حالياً. يرجى تفعيل أو تجديد الاشتراك لمتابعة إصدار الفواتير."
                        }
                    )
                },
                confirmButton = {
                    androidx.compose.material3.Button(
                        onClick = {
                            viewModel.dismissLicenseLockDialog()
                            viewModel.selectTab(8)
                        }
                    ) {
                        Text("الذهاب لشاشة الترخيص والتفعيل")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissLicenseLockDialog() }) {
                        Text("إغلاق")
                    }
                }
            )
        }
    }
}
