package com.example.dokkani.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Surface
import com.example.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
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
import androidx.compose.material.icons.filled.Badge
import com.example.dokkani.ui.screens.hr.HrAndPayrollScreen
import kotlinx.coroutines.launch

import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Store
import com.example.dokkani.ui.screens.costcenters.CostCentersManagementScreen
import com.example.dokkani.ui.screens.ValueSellingManagementScreen
import com.example.dokkani.ui.screens.notifications.NotificationsScreen
import com.example.dokkani.ui.screens.backup.BackupRestoreScreen
import com.example.dokkani.ui.screens.inventory.WarehousesManagementScreen
import com.example.dokkani.ui.components.ZoomableBox

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
    var showValueSellingManagementScreen by remember { mutableStateOf(false) }

    val allTabs = listOf(
        NavTabItem("فاتورة بيع مباشر", Icons.Default.PointOfSale, setOf(UserRole.ADMIN, UserRole.CASHIER, UserRole.INVENTORY)),
        NavTabItem("فاتورة البيع", Icons.Default.ReceiptLong, setOf(UserRole.ADMIN, UserRole.CASHIER, UserRole.INVENTORY)),
        NavTabItem("فاتورة الشراء", Icons.Default.ShoppingBag, setOf(UserRole.ADMIN, UserRole.INVENTORY)),
        NavTabItem("فاتورة مردود البيع", Icons.Default.ReceiptLong, setOf(UserRole.ADMIN, UserRole.CASHIER, UserRole.INVENTORY)),
        NavTabItem("فاتورة مردود الشراء", Icons.Default.ShoppingBag, setOf(UserRole.ADMIN, UserRole.INVENTORY)),
        NavTabItem("سندات القبض", Icons.Default.PointOfSale, setOf(UserRole.ADMIN, UserRole.CASHIER)),
        NavTabItem("سندات الصرف", Icons.Default.AccountBalanceWallet, setOf(UserRole.ADMIN, UserRole.CASHIER)),
        NavTabItem("إدارة البيع بالقيمة", Icons.Default.MonetizationOn, setOf(UserRole.ADMIN, UserRole.CASHIER, UserRole.INVENTORY)),
        NavTabItem("المنتجات والأصناف", Icons.Default.Inventory, setOf(UserRole.ADMIN, UserRole.INVENTORY)),
        NavTabItem("دليل المخازن", Icons.Default.Store, setOf(UserRole.ADMIN, UserRole.INVENTORY)),
        NavTabItem("مخازن المستخدمين", Icons.Default.Storefront, setOf(UserRole.ADMIN, UserRole.INVENTORY)),
        NavTabItem("شاشة الجرد وقائمة الجرد", Icons.Default.FactCheck, setOf(UserRole.ADMIN, UserRole.INVENTORY)),
        NavTabItem("الخزينة والمصروفات", Icons.Default.AccountBalanceWallet, setOf(UserRole.ADMIN, UserRole.CASHIER)),
        NavTabItem("إدارة الشفتات والدرج", Icons.Default.ReceiptLong, setOf(UserRole.ADMIN, UserRole.CASHIER)),
        NavTabItem("الدليل المحاسبي", Icons.Default.AccountTree, setOf(UserRole.ADMIN, UserRole.CASHIER)),
        NavTabItem("إدارة الصناديق والبنوك", Icons.Default.AccountBalance, setOf(UserRole.ADMIN, UserRole.CASHIER)),
        NavTabItem("العملاء والموردين", Icons.Default.People, setOf(UserRole.ADMIN, UserRole.CASHIER)),
        NavTabItem("شؤون العمال والرواتب", Icons.Default.Badge, setOf(UserRole.ADMIN)),
        NavTabItem("الأصول والملكية", Icons.Default.AccountBalance, setOf(UserRole.ADMIN)),
        NavTabItem("التقارير", Icons.Default.Analytics, setOf(UserRole.ADMIN)),
        NavTabItem("طباعة الباركود", Icons.Default.QrCode, setOf(UserRole.ADMIN, UserRole.INVENTORY)),
        NavTabItem("الترخيص والحماية", Icons.Default.Security, setOf(UserRole.ADMIN)),
        NavTabItem("إدارة العملات", Icons.Default.AccountBalanceWallet, setOf(UserRole.ADMIN, UserRole.CASHIER, UserRole.INVENTORY)),
        NavTabItem("إدارة مراكز التكلفة", Icons.Default.Business, setOf(UserRole.ADMIN)),
        NavTabItem("إدارة المجموعات والتصنيفات", Icons.Default.Category, setOf(UserRole.ADMIN, UserRole.INVENTORY, UserRole.CASHIER)),
        NavTabItem("إعدادات النظام", Icons.Default.Settings, setOf(UserRole.ADMIN)),
        NavTabItem("النسخ الاحتياطي والاستعادة", Icons.Default.Backup, setOf(UserRole.ADMIN)),
        NavTabItem("المهام والإشعارات", Icons.Default.Notifications, setOf(UserRole.ADMIN, UserRole.CASHIER, UserRole.INVENTORY)),
        NavTabItem("المستخدمين والصلاحيات", Icons.Default.People, setOf(UserRole.ADMIN))
    )

    val allowedTabs = allTabs.filter { currentUserRole in it.requiredRoles }
    val currentTabIndex = uiState.selectedTab.coerceIn(0, (allowedTabs.size - 1).coerceAtLeast(0))
    val activeTabTitle = allowedTabs.getOrNull(currentTabIndex)?.title ?: ""

    // بناء قائمة الإدارات والأقسام للـ Collapsible Navigation Drawer
    val allDepartments = remember {
        listOf(
            com.example.dokkani.ui.components.NavDepartmentGroup(
                id = "operations",
                titleArabic = "العمليات",
                titleEnglish = "Operations",
                icon = Icons.Default.ReceiptLong,
                headerColor = Color(0xFF2E7D32),
                items = listOf(
                    com.example.dokkani.ui.components.NavTabItem("فاتورة بيع مباشر", Icons.Default.PointOfSale, setOf(UserRole.ADMIN, UserRole.CASHIER, UserRole.INVENTORY)),
                    com.example.dokkani.ui.components.NavTabItem("فاتورة البيع", Icons.Default.ReceiptLong, setOf(UserRole.ADMIN, UserRole.CASHIER, UserRole.INVENTORY)),
                    com.example.dokkani.ui.components.NavTabItem("فاتورة الشراء", Icons.Default.ShoppingBag, setOf(UserRole.ADMIN, UserRole.INVENTORY)),
                    com.example.dokkani.ui.components.NavTabItem("فاتورة مردود البيع", Icons.Default.ReceiptLong, setOf(UserRole.ADMIN, UserRole.CASHIER, UserRole.INVENTORY)),
                    com.example.dokkani.ui.components.NavTabItem("فاتورة مردود الشراء", Icons.Default.ShoppingBag, setOf(UserRole.ADMIN, UserRole.INVENTORY)),
                    com.example.dokkani.ui.components.NavTabItem("سندات القبض", Icons.Default.PointOfSale, setOf(UserRole.ADMIN, UserRole.CASHIER)),
                    com.example.dokkani.ui.components.NavTabItem("سندات الصرف", Icons.Default.AccountBalanceWallet, setOf(UserRole.ADMIN, UserRole.CASHIER))
                )
            ),
            com.example.dokkani.ui.components.NavDepartmentGroup(
                id = "general_ledger",
                titleArabic = "الأستاذ العام والحسابات العامة",
                titleEnglish = "General Ledger & Accounts",
                icon = Icons.Default.AccountBalance,
                headerColor = Color(0xFF1B5E20),
                items = listOf(
                    com.example.dokkani.ui.components.NavTabItem("الدليل المحاسبي", Icons.Default.AccountTree, setOf(UserRole.ADMIN, UserRole.CASHIER)),
                    com.example.dokkani.ui.components.NavTabItem("إدارة الصناديق والبنوك", Icons.Default.AccountBalance, setOf(UserRole.ADMIN, UserRole.CASHIER)),
                    com.example.dokkani.ui.components.NavTabItem("الخزينة والمصروفات", Icons.Default.AccountBalanceWallet, setOf(UserRole.ADMIN, UserRole.CASHIER)),
                    com.example.dokkani.ui.components.NavTabItem("إدارة الشفتات والدرج", Icons.Default.ReceiptLong, setOf(UserRole.ADMIN, UserRole.CASHIER)),
                    com.example.dokkani.ui.components.NavTabItem("إدارة مراكز التكلفة", Icons.Default.Business, setOf(UserRole.ADMIN)),
                    com.example.dokkani.ui.components.NavTabItem("إدارة العملات", Icons.Default.MonetizationOn, setOf(UserRole.ADMIN, UserRole.CASHIER, UserRole.INVENTORY)),
                    com.example.dokkani.ui.components.NavTabItem("الأصول والملكية", Icons.Default.AccountBalance, setOf(UserRole.ADMIN)),
                    com.example.dokkani.ui.components.NavTabItem("التقارير", Icons.Default.Analytics, setOf(UserRole.ADMIN))
                )
            ),
            com.example.dokkani.ui.components.NavDepartmentGroup(
                id = "sales",
                titleArabic = "إدارة المبيعات والعملاء",
                titleEnglish = "Sales & Customer Management",
                icon = Icons.Default.PointOfSale,
                headerColor = Color(0xFF0D47A1),
                items = listOf(
                    com.example.dokkani.ui.components.NavTabItem("إدارة البيع بالقيمة", Icons.Default.MonetizationOn, setOf(UserRole.ADMIN, UserRole.CASHIER, UserRole.INVENTORY)),
                    com.example.dokkani.ui.components.NavTabItem("العملاء والموردين", Icons.Default.People, setOf(UserRole.ADMIN, UserRole.CASHIER))
                )
            ),
            com.example.dokkani.ui.components.NavDepartmentGroup(
                id = "inventory_procurement",
                titleArabic = "إدارة المخازن والمشتريات",
                titleEnglish = "Inventory & Procurement",
                icon = Icons.Default.Storefront,
                headerColor = Color(0xFFE65100),
                items = listOf(
                    com.example.dokkani.ui.components.NavTabItem("المنتجات والأصناف", Icons.Default.Inventory, setOf(UserRole.ADMIN, UserRole.INVENTORY)),
                    com.example.dokkani.ui.components.NavTabItem("دليل المخازن", Icons.Default.Store, setOf(UserRole.ADMIN, UserRole.INVENTORY)),
                    com.example.dokkani.ui.components.NavTabItem("مخازن المستخدمين", Icons.Default.Storefront, setOf(UserRole.ADMIN, UserRole.INVENTORY)),
                    com.example.dokkani.ui.components.NavTabItem("شاشة الجرد وقائمة الجرد", Icons.Default.FactCheck, setOf(UserRole.ADMIN, UserRole.INVENTORY)),
                    com.example.dokkani.ui.components.NavTabItem("طباعة الباركود", Icons.Default.QrCode, setOf(UserRole.ADMIN, UserRole.INVENTORY))
                )
            ),
            com.example.dokkani.ui.components.NavDepartmentGroup(
                id = "system_settings",
                titleArabic = "إدارة النظام والإعدادات",
                titleEnglish = "System Administration & Settings",
                icon = Icons.Default.Settings,
                headerColor = Color(0xFF37474F),
                items = listOf(
                    com.example.dokkani.ui.components.NavTabItem("إدارة المجموعات والتصنيفات", Icons.Default.Category, setOf(UserRole.ADMIN, UserRole.INVENTORY, UserRole.CASHIER)),
                    com.example.dokkani.ui.components.NavTabItem("إعدادات النظام", Icons.Default.Settings, setOf(UserRole.ADMIN)),
                    com.example.dokkani.ui.components.NavTabItem("النسخ الاحتياطي والاستعادة", Icons.Default.Backup, setOf(UserRole.ADMIN)),
                    com.example.dokkani.ui.components.NavTabItem("المهام والإشعارات", Icons.Default.Notifications, setOf(UserRole.ADMIN, UserRole.CASHIER, UserRole.INVENTORY)),
                    com.example.dokkani.ui.components.NavTabItem("المستخدمين والصلاحيات", Icons.Default.People, setOf(UserRole.ADMIN)),
                    com.example.dokkani.ui.components.NavTabItem("شؤون العمال والرواتب", Icons.Default.Badge, setOf(UserRole.ADMIN)),
                    com.example.dokkani.ui.components.NavTabItem("الترخيص والحماية", Icons.Default.Security, setOf(UserRole.ADMIN))
                )
            )
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            com.example.dokkani.ui.components.CollapsibleNavDrawerSheet(
                currentUserRole = currentUserRole,
                activeTabTitle = activeTabTitle,
                allDepartments = allDepartments,
                onSelectTabByTitle = { selectedTitle ->
                    showValueSellingManagementScreen = false
                    val index = allowedTabs.indexOfFirst {
                        it.title == selectedTitle || 
                        (selectedTitle == "المنتجات والأصناف" && it.title == "المنتجات والوحدات") ||
                        (selectedTitle == "فواتير الشراء" && it.title == "فاتورة الشراء")
                    }
                    if (index >= 0) {
                        viewModel.selectTab(index)
                        if (selectedTitle == "إدارة الشفتات والدرج") {
                            viewModel.selectCashSubTab(1)
                        } else if (selectedTitle == "الخزينة والمصروفات" || selectedTitle == "سندات الصرف") {
                            viewModel.selectCashSubTab(0)
                        }
                    }
                },
                onOpenOnboardingWizard = onOpenOnboardingWizard,
                onLogout = onLogout,
                onCloseDrawer = {
                    scope.launch { drawerState.close() }
                }
            )
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
                        Button(
                            onClick = {
                                val shiftIndex = allowedTabs.indexOfFirst {
                                    it.title == "إدارة الشفتات والدرج" || it.title == "الخزينة والمصروفات" || it.title == "فاتورة البيع المباشر"
                                }
                                if (shiftIndex >= 0) {
                                    viewModel.selectTab(shiftIndex)
                                    viewModel.selectCashSubTab(1)
                                }
                                val currentOrOpenShift = uiState.cashShifts.firstOrNull { it.status == "OPEN" }
                                    ?: uiState.cashShifts.firstOrNull()
                                    ?: com.example.dokkani.data.local.entities.CashShiftEntity(shiftNumber = "SHF-0001")
                                viewModel.openShiftSettlementDialog(currentOrOpenShift)
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD32F2F),
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "إغلاق الشفت",
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "إغلاق الشفت",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(
                            onClick = {
                                val idx = allowedTabs.indexOfFirst { it.title == "المهام والإشعارات" }
                                if (idx >= 0) viewModel.selectTab(idx)
                            }
                        ) {
                            if (uiState.unreadNotificationCount > 0) {
                                BadgedBox(
                                    badge = {
                                        Badge {
                                            Text("${uiState.unreadNotificationCount}")
                                        }
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Notifications,
                                        contentDescription = "الإشعارات والمهام",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            } else {
                                Icon(
                                    Icons.Default.Notifications,
                                    contentDescription = "الإشعارات والمهام",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
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
            ZoomableBox(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (showValueSellingManagementScreen) {
                    ValueSellingManagementScreen(
                        currentUserRole = currentUserRole,
                        onNavigateBack = { showValueSellingManagementScreen = false }
                    )
                } else if (allowedTabs.isNotEmpty()) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        val activeTabTitle = allowedTabs.getOrNull(currentTabIndex)?.title ?: ""
                        when (activeTabTitle) {
                            "فاتورة بيع مباشر", "البيع المباشر", "الفواتير والسندات", "فاتورة مردود البيع" -> {
                                PosScreen(
                                    currentUserRole = currentUserRole,
                                    onNavigateToValueSellingManagement = { showValueSellingManagementScreen = true }
                                )
                            }
                            "فاتورة البيع", "فواتير البيع" -> {
                                com.example.dokkani.ui.screens.sales.SalesInvoiceScreen(
                                    currentUserRole = currentUserRole
                                )
                            }
                            "فاتورة الشراء", "فواتير الشراء" -> {
                                PurchaseScreen(currentUserRole = currentUserRole)
                            }
                            "فاتورة مردود الشراء" -> {
                                com.example.dokkani.ui.screens.purchasereturn.PurchaseReturnScreen(
                                    onNavigateBack = {
                                        val idx = allowedTabs.indexOfFirst { it.title == "فاتورة الشراء" || it.title == "فواتير الشراء" }
                                        if (idx >= 0) viewModel.selectTab(idx)
                                    }
                                )
                            }
                            "سندات القبض", "سند قبض" -> {
                                com.example.dokkani.ui.screens.vouchers.ReceiptVoucherScreen(
                                    currentUserRole = currentUserRole
                                )
                            }
                            "سندات الصرف", "سند صرف" -> {
                                com.example.dokkani.ui.screens.vouchers.PaymentVoucherScreen(
                                    currentUserRole = currentUserRole
                                )
                            }
                            "إدارة البيع بالقيمة" -> {
                                ValueSellingManagementScreen(
                                    currentUserRole = currentUserRole,
                                    onNavigateBack = { viewModel.selectTab(0) }
                                )
                            }
                            "المنتجات والوحدات", "المنتجات والأصناف" -> {
                                ProductsAndUnitsScreen(
                                    productsWithUnits = uiState.products,
                                    wasteRecords = uiState.wasteRecords,
                                    currencies = uiState.currencies,
                                    costCenters = uiState.costCenters,
                                    currentUserRole = currentUserRole,
                                    currencySymbol = uiState.currencySymbol,
                                    onSaveProduct = viewModel::saveProduct,
                                    onDeleteProduct = viewModel::deleteProduct,
                                    onSaveUnit = viewModel::saveProductUnit,
                                    onDeleteUnit = viewModel::deleteProductUnit,
                                    onRenameCategory = viewModel::renameCategory,
                                    onDeleteCategory = viewModel::deleteCategory,
                                    onSaveWasteRecord = viewModel::saveWasteRecord,
                                    onDeleteWasteRecord = viewModel::deleteWasteRecord
                                )
                            }
                            "دليل المخازن" -> {
                                WarehousesManagementScreen(
                                    uiState = uiState,
                                    currentUserRole = currentUserRole,
                                    onSaveWarehouse = viewModel::saveWarehouse,
                                    onDeleteWarehouse = viewModel::deleteWarehouse,
                                    onSetDefaultWarehouse = viewModel::setDefaultWarehouse,
                                    onSetWarehouseActive = viewModel::setWarehouseActive,
                                    onNavigateBack = { viewModel.selectTab(0) }
                                )
                            }
                            "مخازن المستخدمين" -> {
                                com.example.dokkani.ui.screens.inventory.UserWarehousesScreen(
                                    uiState = uiState,
                                    currentUserRole = currentUserRole,
                                    onSaveUserWarehouse = viewModel::saveUserWarehouse,
                                    onDeleteUserWarehouse = viewModel::deleteUserWarehouse,
                                    onNavigateBack = { viewModel.selectTab(0) }
                                )
                            }
                            "شاشة الجرد وقائمة الجرد" -> {
                                com.example.dokkani.ui.screens.inventory.InventoryAuditCountSheetScreen(
                                    currentUserRole = currentUserRole,
                                    onNavigateToValueSelling = {
                                        val valIndex = allowedTabs.indexOfFirst { it.title == "إدارة البيع بالقيمة" }
                                        if (valIndex >= 0) {
                                            viewModel.selectTab(valIndex)
                                        }
                                    }
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
                                    onOpenShiftSettlementDialog = viewModel::openShiftSettlementDialog,
                                    onDismissShiftSettlementDialog = viewModel::dismissShiftSettlementDialog,
                                    onUpdateSettlementInputs = viewModel::updateSettlementInputs,
                                    onSubmitShiftSettlement = viewModel::submitShiftSettlement,
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
                                    onDismissAccountDeleteDialogs = viewModel::dismissAccountDeleteDialogs,
                                    onUpdateExpensePaymentMethod = viewModel::updateExpensePaymentMethod,
                                    onUpdateExpensePaymentAccountId = viewModel::updateExpensePaymentAccountId,
                                    onUpdateExpenseTransactionRef = viewModel::updateExpenseTransactionRef,
                                    onUpdateExpenseReceiptImagePath = viewModel::updateExpenseReceiptImagePath,
                                    onUpdateExpenseSecondaryMethod = viewModel::updateExpenseSecondaryMethod,
                                    onUpdateExpenseSecondaryPaidAmount = viewModel::updateExpenseSecondaryPaidAmount
                                )
                            }
                            "إدارة الشفتات والدرج" -> {
                                androidx.compose.runtime.LaunchedEffect(Unit) {
                                    viewModel.selectCashSubTab(1)
                                }
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
                                    onOpenShiftSettlementDialog = viewModel::openShiftSettlementDialog,
                                    onDismissShiftSettlementDialog = viewModel::dismissShiftSettlementDialog,
                                    onUpdateSettlementInputs = viewModel::updateSettlementInputs,
                                    onSubmitShiftSettlement = viewModel::submitShiftSettlement,
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
                                    onDismissAccountDeleteDialogs = viewModel::dismissAccountDeleteDialogs,
                                    onUpdateExpensePaymentMethod = viewModel::updateExpensePaymentMethod,
                                    onUpdateExpensePaymentAccountId = viewModel::updateExpensePaymentAccountId,
                                    onUpdateExpenseTransactionRef = viewModel::updateExpenseTransactionRef,
                                    onUpdateExpenseReceiptImagePath = viewModel::updateExpenseReceiptImagePath,
                                    onUpdateExpenseSecondaryMethod = viewModel::updateExpenseSecondaryMethod,
                                    onUpdateExpenseSecondaryPaidAmount = viewModel::updateExpenseSecondaryPaidAmount
                                )
                            }
                            "الدليل المحاسبي", "دليل الحسابات والبنوك" -> {
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
                            "إدارة الصناديق والبنوك" -> {
                                com.example.dokkani.ui.screens.cash.CashAndBanksManagementScreen(
                                    currentUserRole = currentUserRole,
                                    onNavigateBack = { viewModel.selectTab(0) }
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
                                    onVoucherReceiptImagePathChanged = viewModel::updateVoucherReceiptImagePath,
                                    onVoucherCostCenterSelected = viewModel::setVoucherCostCenterId,
                                    onVoucherAccountSelected = viewModel::setVoucherAccountId,
                                    onSubmitPaymentVoucher = viewModel::submitPaymentVoucher,
                                    onSaveParty = viewModel::saveParty,
                                    onDeleteParty = viewModel::requestDeletePartyWithProtection,
                                    onDeleteVoucher = viewModel::deletePaymentVoucher,
                                    onDeleteInvoice = viewModel::deleteInvoice,
                                    onUpdateInvoice = viewModel::updateDirectInvoice,
                                    onUpdateVoucher = viewModel::updateDirectVoucher,
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
                                    onDeleteLeasehold = viewModel::deleteLeasehold,
                                    showOpeningCapitalDialog = uiState.showOpeningCapitalDialog,
                                    openingCapitalAmountInput = uiState.openingCapitalAmountInput,
                                    openingCapitalCurrencyInput = uiState.openingCapitalCurrencyInput,
                                    openingCapitalAccountCodeInput = uiState.openingCapitalAccountCodeInput,
                                    openingCapitalNotesInput = uiState.openingCapitalNotesInput,
                                    currentSettings = uiState.settings,
                                    financialAccounts = uiState.financialAccounts,
                                    currencies = uiState.currencies,
                                    onOpenOpeningCapitalDialog = viewModel::openOpeningCapitalDialog,
                                    onDismissOpeningCapitalDialog = viewModel::dismissOpeningCapitalDialog,
                                    onOpeningCapitalInputsChanged = viewModel::updateOpeningCapitalInputs,
                                    onSaveOpeningCapital = viewModel::saveOpeningCapital
                                )
                            }
                            "التقارير" -> {
                                ReportsDashboardScreen(
                                    uiState = uiState,
                                    onSelectSubTab = viewModel::selectReportSubTab,
                                    onSelectValuationMethod = viewModel::selectValuationMethod,
                                    onSelectCostCenter = viewModel::selectReportCostCenterId,
                                    onSelectStatementMode = viewModel::selectReportStatementMode,
                                    onToggleHideZeroBalances = viewModel::toggleHideZeroBalances,
                                    onRefreshReports = viewModel::refreshReports,
                                    onOpenItemLedger = viewModel::openProductItemLedger,
                                    onCloseItemLedger = viewModel::closeProductItemLedger
                                )
                            }
                            "طباعة الباركود" -> {
                                BarcodeLabelPrinterScreen(
                                    productsWithUnits = uiState.products,
                                    currencySymbol = uiState.currencySymbol
                                )
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
                            "شؤون العمال والرواتب" -> {
                                HrAndPayrollScreen(
                                    uiState = uiState,
                                    currentUserRole = currentUserRole,
                                    onSelectSubTab = viewModel::selectHrSubTab,
                                    onOpenAddEmployeeDialog = viewModel::openAddEmployeeDialog,
                                    onDismissAddEmployeeDialog = viewModel::dismissAddEmployeeDialog,
                                    onEmployeeInputsChanged = viewModel::updateEmployeeInputs,
                                    onSaveEmployee = viewModel::saveEmployee,
                                    onToggleEmployeeActive = viewModel::toggleEmployeeActive,
                                    onOpenAttendanceDialog = viewModel::openAttendanceDialog,
                                    onDismissAttendanceDialog = viewModel::dismissAttendanceDialog,
                                    onAttendanceInputsChanged = viewModel::updateAttendanceInputs,
                                    onSaveAttendance = viewModel::saveAttendance,
                                    onOpenHrTransactionDialog = viewModel::openHrTransactionDialog,
                                    onDismissHrTransactionDialog = viewModel::dismissHrTransactionDialog,
                                    onHrTransactionInputsChanged = viewModel::updateHrTransactionInputs,
                                    onSaveHrTransaction = viewModel::saveHrTransaction,
                                    onGeneratePayrollRun = viewModel::generateMonthlyPayrollRun,
                                    onPayoutPayrollRecord = viewModel::payoutPayrollRecord,
                                    onCancelPayrollPayout = { record -> viewModel.cancelPayrollPayout(record, currentUserRole) },
                                    onOpenAdjustSalaryDialog = viewModel::openAdjustSalaryDialog,
                                    onDismissAdjustSalaryDialog = viewModel::dismissAdjustSalaryDialog,
                                    onAdjustSalaryInputsChanged = viewModel::updateAdjustSalaryInputs,
                                    onSaveSalaryAdjustment = viewModel::saveSalaryAdjustment,
                                    onOpenEmployeeDocumentDialog = viewModel::openEmployeeDocumentDialog,
                                    onDismissEmployeeDocumentDialog = viewModel::dismissEmployeeDocumentDialog,
                                    onUpdateEmployeePhotoPath = viewModel::updateEmployeePhotoPath,
                                    onDismissHrErrorMessage = viewModel::dismissHrErrorMessage
                                )
                            }
                            "إدارة العملات" -> {
                                com.example.dokkani.ui.screens.currency.DailyExchangeRatesScreen(
                                    currencies = uiState.currencies,
                                    exchangeRateLogs = uiState.exchangeRateLogs,
                                    settings = uiState.settings,
                                    currentUserRole = currentUserRole,
                                    onSaveCurrency = viewModel::saveCurrency,
                                    onSetBaseCurrency = viewModel::setAsBaseCurrency,
                                    onDeleteCurrency = viewModel::deleteCurrency,
                                    onUpdateForeignPricingMode = viewModel::updateForeignCurrencyPricingMode,
                                    onUpdateEnableDailyExchangePrompt = viewModel::updateEnableDailyExchangeRatePrompt
                                )
                            }
                            "إدارة مراكز التكلفة" -> {
                                CostCentersManagementScreen(
                                    currentUserRole = currentUserRole,
                                    onNavigateBack = { viewModel.selectTab(0) }
                                )
                            }
                            "إدارة المجموعات والتصنيفات" -> {
                                com.example.dokkani.ui.screens.groups.GlobalGroupsManagementScreen(
                                    currentUserRole = currentUserRole,
                                    onNavigateBack = { viewModel.selectTab(0) }
                                )
                            }
                            "إعدادات النظام" -> {
                                SystemSettingsScreen(
                                    settings = uiState.settings,
                                    currencies = uiState.currencies,
                                    parties = uiState.parties,
                                    invoices = uiState.invoices,
                                    financialAccounts = uiState.financialAccounts,
                                    currentUserRole = currentUserRole,
                                    fcmToken = uiState.fcmToken,
                                    onRefreshFcmToken = viewModel::refreshFcmToken,
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
                                    onUpdateInvoice = viewModel::updateDirectInvoice,
                                    onUpdateStoreProfile = viewModel::updateStoreProfile,
                                    onUpdateShowPreviousBalance = viewModel::updateShowPreviousBalance,
                                    onUpdateShowDecimals = viewModel::updateShowDecimals,
                                    onUpdateAutoLockSettings = viewModel::updateAutoLockSettings,
                                    onUpdatePasswordPolicySettings = viewModel::updatePasswordPolicySettings
                                )
                            }
                            "المستخدمين والصلاحيات" -> {
                                UserManagementScreen()
                            }
                            "المهام والإشعارات" -> {
                                NotificationsScreen(
                                    viewModel = viewModel,
                                    uiState = uiState
                                )
                            }
                            "النسخ الاحتياطي والاستعادة" -> {
                                BackupRestoreScreen(
                                    viewModel = viewModel,
                                    uiState = uiState
                                )
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

    // نافذة منع إدخال قيد غير متوازن في ميزان المراجعة والقيد المزدوج
    uiState.accountingErrorMessage?.let { errorMsg ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissAccountingError() },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "إيقاف حارم - القيد المزدوج غير متوازن",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Text(
                    text = errorMsg,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissAccountingError() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("موافق (تصحيح المبالغ)", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
        }
        }
    }
}
