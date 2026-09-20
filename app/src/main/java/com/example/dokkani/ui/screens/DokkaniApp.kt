package com.example.dokkani.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dokkani.data.local.entities.UserRole
import com.example.dokkani.ui.DokkaniViewModel
import com.example.dokkani.ui.screens.accounts.AccountsManagementScreen
import com.example.dokkani.ui.screens.accounts.AccountDeletionBlockedDialog
import com.example.dokkani.ui.screens.assets.AssetsAndEquityScreen
import com.example.dokkani.ui.screens.barcode.BarcodeLabelPrinterScreen
import com.example.dokkani.ui.screens.cash.CashAndExpensesScreen
import com.example.dokkani.ui.screens.credit.CreditLedgerScreen
import com.example.dokkani.ui.screens.license.LicenseScreen
import com.example.dokkani.ui.screens.pos.PosScreen
import com.example.dokkani.ui.screens.purchase.PurchaseScreen
import com.example.dokkani.ui.screens.reports.ReportsDashboardScreen
import com.example.dokkani.ui.screens.users.UserManagementScreen
import kotlinx.coroutines.launch

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
    onLogout: () -> Unit = {},
    onOpenOnboardingWizard: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val allTabs = listOf(
        NavTabItem("الفواتير والسندات", Icons.Default.PointOfSale, setOf(UserRole.ADMIN, UserRole.CASHIER, UserRole.INVENTORY)),
        NavTabItem("فواتير الشراء", Icons.Default.ShoppingBag, setOf(UserRole.ADMIN, UserRole.INVENTORY)),
        NavTabItem("المنتجات والوحدات", Icons.Default.Inventory, setOf(UserRole.ADMIN, UserRole.INVENTORY)),
        NavTabItem("التكلفة والخضار", Icons.Default.Calculate, setOf(UserRole.ADMIN, UserRole.INVENTORY)),
        NavTabItem("الخزينة والمصروفات", Icons.Default.AccountBalanceWallet, setOf(UserRole.ADMIN, UserRole.CASHIER)),
        NavTabItem("دليل الحسابات والبنوك", Icons.Default.AccountTree, setOf(UserRole.ADMIN, UserRole.CASHIER)),
        NavTabItem("العملاء والموردين", Icons.Default.People, setOf(UserRole.ADMIN, UserRole.CASHIER)),
        NavTabItem("الأصول والملكية", Icons.Default.AccountBalance, setOf(UserRole.ADMIN)),
        NavTabItem("التقارير", Icons.Default.Analytics, setOf(UserRole.ADMIN)),
        NavTabItem("طباعة الباركود", Icons.Default.QrCode, setOf(UserRole.ADMIN, UserRole.INVENTORY)),
        NavTabItem("الترخيص والحماية", Icons.Default.Security, setOf(UserRole.ADMIN)),
        NavTabItem("إعدادات النظام", Icons.Default.Settings, setOf(UserRole.ADMIN)),
        NavTabItem("المستخدمين والصلاحيات", Icons.Default.People, setOf(UserRole.ADMIN))
    )

    val allowedTabs = allTabs.filter { currentUserRole in it.requiredRoles }
    val currentTabIndex = uiState.selectedTab.coerceIn(0, (allowedTabs.size - 1).coerceAtLeast(0))

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // ترويسة القائمة الجانبية الشاملة
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        Icon(
                            Icons.Default.Storefront,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "نظام دكاني - Dokkani",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "الدور: ${currentUserRole.labelArabic}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // القائمة الجانبية المحدثة لجميع شاشات وأقسام النظام دون فواتير الشراء
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        allowedTabs.filter { it.title != "فواتير الشراء" }.forEach { tab ->
                            val originalIndex = allowedTabs.indexOf(tab)
                            val isSelected = currentTabIndex == originalIndex

                            NavigationDrawerItem(
                                label = {
                                    Text(
                                        tab.title,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                icon = {
                                    Icon(
                                        tab.icon,
                                        contentDescription = tab.title,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                selected = isSelected,
                                onClick = {
                                    viewModel.selectTab(originalIndex)
                                    scope.launch { drawerState.close() }
                                },
                                colors = NavigationDrawerItemDefaults.colors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                    selectedTextColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // خيارات وإجراءات النظام أسفل القائمة
                    if (currentUserRole == UserRole.ADMIN) {
                        NavigationDrawerItem(
                            label = { Text("معالج التهيئة الأولى والرقابة", fontSize = 13.sp) },
                            icon = { Icon(Icons.Default.AutoFixHigh, contentDescription = null) },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                onOpenOnboardingWizard()
                            }
                        )
                    }

                    NavigationDrawerItem(
                        label = { Text("تسجيل الخروج", fontSize = 13.sp, color = MaterialTheme.colorScheme.error) },
                        icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onLogout()
                        }
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = "فتح القائمة الجانبية",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    title = {
                        // عرض عنوان الشاشة النشطة حالياً ديناميكياً
                        val activeTitle = allowedTabs.getOrNull(currentTabIndex)?.title ?: "دكاني POS"
                        Column {
                            Text(
                                activeTitle,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "نظام دكاني - ${currentUserRole.labelArabic}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    actions = {
                        if (currentUserRole == UserRole.ADMIN) {
                            IconButton(onClick = onOpenOnboardingWizard) {
                                Icon(
                                    Icons.Default.AutoFixHigh,
                                    contentDescription = "معالج التهيئة الأولى",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        IconButton(onClick = onLogout) {
                            Icon(
                                Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = "تسجيل الخروج",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.primary,
                        actionIconContentColor = MaterialTheme.colorScheme.primary
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
                    Box(modifier = Modifier.fillMaxSize()) {
                        val activeTabTitle = allowedTabs.getOrNull(currentTabIndex)?.title ?: ""
                        when (activeTabTitle) {
                            "الفواتير والسندات" -> {
                                PosScreen(currentUserRole = currentUserRole)
                            }
                            "فواتير الشراء" -> {
                                PurchaseScreen(currentUserRole = currentUserRole)
                            }
                            "المنتجات والوحدات" -> {
                                ProductsAndUnitsScreen(
                                    productsWithUnits = uiState.products,
                                    currentUserRole = currentUserRole,
                                    currencySymbol = uiState.currencySymbol,
                                    onSaveProduct = viewModel::saveProduct,
                                    onDeleteProduct = viewModel::deleteProduct,
                                    onSaveUnit = viewModel::saveProductUnit,
                                    onDeleteUnit = viewModel::deleteProductUnit,
                                    onRenameCategory = viewModel::renameCategory,
                                    onDeleteCategory = viewModel::deleteCategory
                                )
                            }
                            "التكلفة والخضار" -> {
                                CostingEngineScreen(
                                    productsWithUnits = uiState.products,
                                    currencySymbol = uiState.currencySymbol
                                )
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
                                    onCloseShiftAndSave = viewModel::closeShiftAndSave,
                                    onAccountsSearchChanged = viewModel::setAccountsSearchQuery,
                                    onAccountsFilterTypeChanged = viewModel::setAccountsFilterType,
                                    onOpenAddAccountDialog = viewModel::openAddAccountDialog,
                                    onOpenEditAccountDialog = viewModel::openEditAccountDialog,
                                    onDismissAddEditAccountDialog = viewModel::dismissAddEditAccountDialog,
                                    onSaveAccount = viewModel::saveFinancialAccount,
                                    onToggleAccountActive = viewModel::toggleFinancialAccountActive,
                                    onRequestDeleteAccount = viewModel::requestDeleteFinancialAccount,
                                    onConfirmDeleteAccount = viewModel::confirmDeleteFinancialAccount,
                                    onDisableAccountInstead = viewModel::disableAccountInstead,
                                    onDismissAccountDeleteDialogs = viewModel::dismissAccountDeleteDialogs
                                )
                            }
                            "دليل الحسابات والبنوك" -> {
                                AccountsManagementScreen(
                                    uiState = uiState,
                                    onSearchChanged = viewModel::setAccountsSearchQuery,
                                    onFilterTypeChanged = viewModel::setAccountsFilterType,
                                    onOpenAddDialog = viewModel::openAddAccountDialog,
                                    onOpenEditDialog = viewModel::openEditAccountDialog,
                                    onDismissAddEditDialog = viewModel::dismissAddEditAccountDialog,
                                    onSaveAccount = viewModel::saveFinancialAccount,
                                    onToggleActive = viewModel::toggleFinancialAccountActive,
                                    onRequestDeleteAccount = viewModel::requestDeleteFinancialAccount,
                                    onConfirmDeleteAccount = viewModel::confirmDeleteFinancialAccount,
                                    onDisableInstead = viewModel::disableAccountInstead,
                                    onDismissDeleteDialogs = viewModel::dismissAccountDeleteDialogs
                                )
                            }
                            "العملاء والموردين" -> {
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
                                    onDeleteParty = viewModel::requestDeletePartyWithProtection,
                                    onDeleteVoucher = viewModel::deletePaymentVoucher,
                                    onDeleteInvoice = viewModel::deleteInvoice,
                                    onSendWhatsAppReminder = { ctx, phone, text ->
                                        try {
                                            val cleanPhone = phone.replace("+", "").replace(" ", "")
                                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                                data = android.net.Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=${java.net.URLEncoder.encode(text, "UTF-8")}")
                                            }
                                            ctx.startActivity(intent)
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                )
                            }
                            "الأصول والملكية" -> {
                                AssetsAndEquityScreen(
                                    equityResult = uiState.equityResult,
                                    fixedAssets = uiState.fixedAssets,
                                    leaseholdRights = uiState.leaseholdRights,
                                    ownerTransactions = uiState.ownerTransactions,
                                    products = uiState.products,
                                    subTab = uiState.assetsSubTab,
                                    currencySymbol = uiState.currencySymbol,
                                    showAddAssetDialog = uiState.showAddAssetDialog,
                                    showOwnerTransDialog = uiState.showOwnerTransDialog,
                                    ownerTransType = uiState.ownerTransTypeInput,
                                    assetCodeInput = uiState.assetCodeInput,
                                    assetNameInput = uiState.assetNameInput,
                                    assetCategoryInput = uiState.assetCategoryInput,
                                    assetCostInput = uiState.assetCostInput,
                                    assetSupplierInput = uiState.assetSupplierInput,
                                    assetNotesInput = uiState.assetNotesInput,
                                    assetPaymentMethod = uiState.assetPaymentMethod,
                                    ownerTransAmountInput = uiState.ownerTransAmountInput,
                                    ownerTransProductId = uiState.ownerTransProductId,
                                    ownerTransQuantityInput = uiState.ownerTransQuantityInput,
                                    ownerTransDetailsInput = uiState.ownerTransDetailsInput,
                                    ownerTransPaymentMethod = uiState.ownerTransPaymentMethod,
                                    showAddLeaseholdDialog = uiState.showAddLeaseholdDialog,
                                    showAmortizeLeaseholdDialog = uiState.showAmortizeLeaseholdDialog,
                                    showSellLeaseholdDialog = uiState.showSellLeaseholdDialog,
                                    selectedLeaseholdItem = uiState.selectedLeaseholdItem,
                                    leaseholdCodeInput = uiState.leaseholdCodeInput,
                                    leaseholdNameInput = uiState.leaseholdNameInput,
                                    leaseholdCostInput = uiState.leaseholdCostInput,
                                    leaseholdYearsInput = uiState.leaseholdYearsInput,
                                    leaseholdNotesInput = uiState.leaseholdNotesInput,
                                    leaseholdAmortizeAmountInput = uiState.leaseholdAmortizeAmountInput,
                                    leaseholdSellPriceInput = uiState.leaseholdSellPriceInput,
                                    leaseholdSellPaymentMethod = uiState.leaseholdSellPaymentMethod,
                                    onSelectSubTab = viewModel::selectAssetsSubTab,
                                    onOpenAddAssetDialog = viewModel::openAddAssetDialog,
                                    onDismissAddAssetDialog = viewModel::dismissAddAssetDialog,
                                    onAssetInputsChanged = viewModel::updateAssetInputs,
                                    onSubmitAddAsset = viewModel::submitAddAsset,
                                    onDeleteAsset = viewModel::deleteAsset,
                                    onOpenOwnerTransDialog = viewModel::openOwnerTransDialog,
                                    onDismissOwnerTransDialog = viewModel::dismissOwnerTransDialog,
                                    onOwnerTransInputsChanged = viewModel::updateOwnerTransInputs,
                                    onSubmitOwnerTrans = viewModel::submitOwnerTrans,
                                    onDeleteOwnerTrans = viewModel::deleteOwnerTrans,
                                    onOpenAddLeaseholdDialog = viewModel::openAddLeaseholdDialog,
                                    onDismissAddLeaseholdDialog = viewModel::dismissAddLeaseholdDialog,
                                    onLeaseholdInputsChanged = viewModel::updateLeaseholdInputs,
                                    onSubmitAddLeasehold = viewModel::submitAddLeasehold,
                                    onOpenAmortizeLeaseholdDialog = viewModel::openAmortizeLeaseholdDialog,
                                    onDismissAmortizeLeaseholdDialog = viewModel::dismissAmortizeLeaseholdDialog,
                                    onAmortizeAmountChanged = viewModel::updateAmortizeAmountInput,
                                    onSubmitAmortizeLeasehold = viewModel::submitAmortizeLeasehold,
                                    onOpenSellLeaseholdDialog = viewModel::openSellLeaseholdDialog,
                                    onDismissSellLeaseholdDialog = viewModel::dismissSellLeaseholdDialog,
                                    onSellLeaseholdInputsChanged = viewModel::updateSellLeaseholdInputs,
                                    onSubmitSellLeasehold = viewModel::submitSellLeasehold,
                                    onDeleteLeasehold = viewModel::deleteLeasehold
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
                                    onUpdateValuationMethod = viewModel::selectValuationMethod,
                                    onUpdateEnableNegativeStock = viewModel::updateEnableNegativeStock,
                                    onUpdateTaxSettings = viewModel::updateTaxSettings,
                                    onUpdatePurchaseTaxSettings = viewModel::updatePurchaseTaxSettings,
                                    onSaveCurrency = viewModel::saveCurrency,
                                    onSetBaseCurrency = viewModel::setAsBaseCurrency,
                                    onDeleteCurrency = viewModel::deleteCurrency,
                                    onSaveParty = viewModel::saveParty,
                                    onDeleteParty = viewModel::deleteParty,
                                    onDeleteInvoice = viewModel::deleteInvoice,
                                    onUpdateStoreProfile = viewModel::updateStoreProfile,
                                    onUpdateShowPreviousBalance = viewModel::updateShowPreviousBalance
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

    // نافذة تنبيه الأمان المحاسبي الشاملة (تظهر في حال محاولة حذف أي حساب أو عميل/مورد مرتبط بسجلات)
    uiState.accountDeletionBlockedDialog?.let { blockedResult ->
        AccountDeletionBlockedDialog(
            result = blockedResult,
            currencySymbol = uiState.currencySymbol,
            onDisableInstead = { viewModel.disableAccountInstead(blockedResult) },
            onDismiss = viewModel::dismissAccountDeleteDialogs
        )
    }
}
