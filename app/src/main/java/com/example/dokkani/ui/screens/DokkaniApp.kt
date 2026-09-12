package com.example.dokkani.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dokkani.data.local.entities.UserRole
import com.example.dokkani.ui.DokkaniViewModel
import com.example.dokkani.ui.screens.barcode.BarcodeLabelPrinterScreen
import com.example.dokkani.ui.screens.cash.CashAndExpensesScreen
import com.example.dokkani.ui.screens.credit.CreditLedgerScreen
import com.example.dokkani.ui.screens.license.LicenseScreen
import com.example.dokkani.ui.screens.pos.PosScreen
import com.example.dokkani.ui.screens.purchase.PurchaseScreen
import com.example.dokkani.ui.screens.reports.ReportsDashboardScreen
import androidx.compose.material.icons.filled.ShoppingBag
import com.example.dokkani.ui.screens.users.UserManagementScreen

data class NavTabItem(
    val title: String,
    val icon: ImageVector,
    val requiredRoles: Set<UserRole>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DokkaniApp(
    viewModel: DokkaniViewModel,
    currentUserRole: UserRole = UserRole.ADMIN,
    onLogout: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    val allTabs = listOf(
        NavTabItem("نقطة البيع", Icons.Default.PointOfSale, setOf(UserRole.ADMIN, UserRole.CASHIER, UserRole.INVENTORY)),
        NavTabItem("فواتير الشراء", Icons.Default.ShoppingBag, setOf(UserRole.ADMIN, UserRole.INVENTORY)),
        NavTabItem("المنتجات والوحدات", Icons.Default.Inventory, setOf(UserRole.ADMIN, UserRole.INVENTORY)),
        NavTabItem("التكلفة والخضار", Icons.Default.Calculate, setOf(UserRole.ADMIN, UserRole.INVENTORY)),
        NavTabItem("الخزينة والمصروفات", Icons.Default.AccountBalanceWallet, setOf(UserRole.ADMIN, UserRole.CASHIER)),
        NavTabItem("دفتر الديون", Icons.Default.CreditCard, setOf(UserRole.ADMIN, UserRole.CASHIER)),
        NavTabItem("التقارير", Icons.Default.Analytics, setOf(UserRole.ADMIN)),
        NavTabItem("طباعة الباركود", Icons.Default.QrCode, setOf(UserRole.ADMIN, UserRole.INVENTORY)),
        NavTabItem("الترخيص والحماية", Icons.Default.Security, setOf(UserRole.ADMIN)),
        NavTabItem("إعدادات النظام", Icons.Default.Settings, setOf(UserRole.ADMIN)),
        NavTabItem("المستخدمين والصلاحيات", Icons.Default.People, setOf(UserRole.ADMIN))
    )

    val allowedTabs = allTabs.filter { currentUserRole in it.requiredRoles }
    val currentTabIndex = uiState.selectedTab.coerceIn(0, (allowedTabs.size - 1).coerceAtLeast(0))

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("نظام دكاني - Dokkani POS", fontSize = 18.sp)
                        Text("الدور الحالي: ${currentUserRole.labelArabic}", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(
                            Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "تسجيل الخروج",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (allowedTabs.isNotEmpty()) {
                ScrollableTabRow(
                    selectedTabIndex = currentTabIndex,
                    edgePadding = 8.dp
                ) {
                    allowedTabs.forEachIndexed { index, tab ->
                        Tab(
                            selected = currentTabIndex == index,
                            onClick = { viewModel.selectTab(index) },
                            text = { Text(tab.title, fontSize = 13.sp) },
                            icon = { Icon(tab.icon, contentDescription = tab.title) }
                        )
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    val activeTabTitle = allowedTabs.getOrNull(currentTabIndex)?.title ?: ""
                    when (activeTabTitle) {
                        "نقطة البيع" -> {
                            PosScreen()
                        }
                        "فواتير الشراء" -> {
                            PurchaseScreen()
                        }
                        "المنتجات والوحدات" -> {
                            ProductsAndUnitsScreen(
                                productsWithUnits = uiState.products,
                                currentUserRole = currentUserRole,
                                onSaveProduct = viewModel::saveProduct,
                                onDeleteProduct = viewModel::deleteProduct,
                                onSaveUnit = viewModel::saveProductUnit,
                                onDeleteUnit = viewModel::deleteProductUnit
                            )
                        }
                        "التكلفة والخضار" -> {
                            CostingEngineScreen(productsWithUnits = uiState.products)
                        }
                        "الخزينة والمصروفات" -> {
                            CashAndExpensesScreen(
                                expenses = uiState.expenses,
                                cashShifts = uiState.cashShifts,
                                uiState = uiState,
                                currentUserRole = currentUserRole,
                                onSelectSubTab = viewModel::selectCashSubTab,
                                onOpenAddExpenseDialog = viewModel::openAddExpenseDialog,
                                onDismissAddExpenseDialog = viewModel::dismissAddExpenseDialog,
                                onExpenseInputsChanged = viewModel::updateExpenseInputs,
                                onSubmitExpense = viewModel::submitExpense,
                                onDeleteExpense = viewModel::deleteExpense,
                                onDrawerInputsChanged = viewModel::updateDrawerInputs,
                                onCalculateDrawerReconciliation = viewModel::calculateDrawerReconciliation,
                                onCloseShiftAndSave = viewModel::closeShiftAndSave
                            )
                        }
                        "دفتر الديون" -> {
                            CreditLedgerScreen(
                                parties = uiState.parties,
                                uiState = uiState,
                                currentUserRole = currentUserRole,
                                onSearchChanged = viewModel::updateCreditSearchQuery,
                                onSelectParty = viewModel::selectPartyForStatement,
                                onOpenPaymentVoucherDialog = viewModel::openPaymentVoucherDialog,
                                onDismissPaymentVoucherDialog = viewModel::dismissPaymentVoucherDialog,
                                onVoucherInputsChanged = viewModel::updateVoucherInputs,
                                onSubmitPaymentVoucher = viewModel::submitPaymentVoucher,
                                onSaveParty = viewModel::saveParty,
                                onDeleteParty = viewModel::deleteParty,
                                onDeleteVoucher = viewModel::deletePaymentVoucher,
                                onDeleteInvoice = viewModel::deleteInvoice,
                                onSendWhatsAppReminder = { _, _, _ -> }
                            )
                        }
                        "التقارير" -> {
                            ReportsDashboardScreen(
                                uiState = uiState,
                                onSelectSubTab = viewModel::selectReportSubTab,
                                onSelectValuationMethod = viewModel::selectValuationMethod,
                                onRefreshReports = viewModel::refreshReports
                            )
                        }
                        "طباعة الباركود" -> {
                            BarcodeLabelPrinterScreen(productsWithUnits = uiState.products)
                        }
                        "الترخيص والحماية" -> {
                            LicenseScreen(
                                uiState = uiState,
                                onSelectPlanForRequest = viewModel::selectPlanForRequest,
                                onRefreshChallengeCode = viewModel::refreshChallengeCode,
                                onActivationCodeChanged = viewModel::updateActivationCodeInput,
                                onApplyActivationCode = viewModel::applyActivationCode,
                                onToggleDeveloperKeyGen = viewModel::toggleDeveloperKeyGen,
                                onKeyGenRequestInputChanged = viewModel::updateKeyGenRequestInput,
                                onKeyGenPlanChanged = viewModel::updateKeyGenPlan,
                                onKeyGenCustomDaysChanged = { },
                                onGenerateKeyGenCode = viewModel::generateKeyGenCode
                            )
                        }
                        "إعدادات النظام" -> {
                            SystemSettingsScreen(
                                settings = uiState.settings,
                                currencies = uiState.currencies,
                                parties = uiState.parties,
                                invoices = uiState.invoices,
                                currentUserRole = currentUserRole,
                                onSaveCurrency = viewModel::saveCurrency,
                                onDeleteCurrency = viewModel::deleteCurrency,
                                onSaveParty = viewModel::saveParty,
                                onDeleteParty = viewModel::deleteParty,
                                onDeleteInvoice = viewModel::deleteInvoice
                            )
                        }
                        "المستخدمين والصلاحيات" -> {
                            UserManagementScreen()
                        }
                    }
                }
            }
        }
    }
}
