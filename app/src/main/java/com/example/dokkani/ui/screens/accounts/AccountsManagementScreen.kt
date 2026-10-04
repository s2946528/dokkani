package com.example.dokkani.ui.screens.accounts

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dokkani.data.local.entities.ChartOfAccountsDefaults
import com.example.dokkani.data.local.entities.EmployeeTransactionType
import com.example.dokkani.data.local.entities.FinancialAccountEntity
import com.example.dokkani.data.local.entities.FinancialAccountType
import com.example.dokkani.data.local.entities.InvoiceType
import com.example.dokkani.data.local.entities.PartyType
import com.example.dokkani.ui.DokkaniUiState
import com.example.dokkani.ui.components.AppSearchBar
import com.example.dokkani.ui.components.normalizeArabicItemSearch
import java.util.Locale
import kotlin.math.abs

/**
 * مجموعة جذور الدليل المحاسبي الرئيسية الخمسة (Level 1 Root Categories)
 */
data class ChartOfAccountsRootGroup(
    val code: String,
    val nameArabic: String,
    val nameEnglish: String,
    val nature: String,            // "DEBIT" or "CREDIT"
    val finalAccount: String,      // "BALANCE_SHEET" or "PROFIT_LOSS"
    val icon: ImageVector,
    val primaryColor: Color
)

val ROOT_ACCOUNT_GROUPS = listOf(
    ChartOfAccountsRootGroup("1", "1 - الأصول", "Assets", "DEBIT", "BALANCE_SHEET", Icons.Default.AccountBalance, Color(0xFF1B5E20)),
    ChartOfAccountsRootGroup("2", "2 - الخصوم والالتزامات", "Liabilities", "CREDIT", "BALANCE_SHEET", Icons.Default.AccountBalanceWallet, Color(0xFFC62828)),
    ChartOfAccountsRootGroup("3", "3 - حقوق الملكية", "Equity", "CREDIT", "BALANCE_SHEET", Icons.Default.PieChart, Color(0xFF6A1B9A)),
    ChartOfAccountsRootGroup("4", "4 - الإيرادات والمبيعات", "Revenues", "CREDIT", "PROFIT_LOSS", Icons.Default.TrendingUp, Color(0xFF0D47A1)),
    ChartOfAccountsRootGroup("5", "5 - المصروفات والتكاليف", "Expenses", "DEBIT", "PROFIT_LOSS", Icons.Default.TrendingDown, Color(0xFFE65100))
)

/**
 * واجهة شجرة الدليل المحاسبي التفاعلية القابلة للطي والتوسيع (Mobile-First Chart of Accounts Tree)
 * مع مفتاح إظهار/إخفاء الأرصدة الصفرية، الربط التلقائي الحي للعملاء والموردين، واللوحة السفلية للتفاصيل والإجراءات.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsManagementScreen(
    uiState: DokkaniUiState,
    onSearchChanged: (String) -> Unit,
    onFilterTypeChanged: (FinancialAccountType?) -> Unit,
    onOpenAddDialog: () -> Unit,
    onOpenEditDialog: (FinancialAccountEntity) -> Unit,
    onDismissAddEditDialog: () -> Unit,
    onSaveAccount: (FinancialAccountEntity) -> Unit,
    onToggleActive: (FinancialAccountEntity) -> Unit,
    onRequestDeleteAccount: (FinancialAccountEntity) -> Unit,
    onConfirmDeleteAccount: (FinancialAccountEntity) -> Unit,
    onDisableInstead: (com.example.dokkani.ui.AccountUsageCheckResult) -> Unit,
    onDismissDeleteDialogs: () -> Unit,
    onToggleShowZeroAccounts: () -> Unit = {},
    onSelectAccountNode: (FinancialAccountEntity?) -> Unit = {}
) {
    val accounts = uiState.financialAccounts
    val searchQuery = uiState.accountsSearchQuery
    val currencySymbol = uiState.currencySymbol

    // مفتاح التحكم بإظهار أو إخفاء الأرصدة الصفرية (Show / Hide Zero Balances Toggle)
    var showZeroAccounts by remember { mutableStateOf(uiState.showZeroAccountsInChart) }

    // حالة فتح/طي مجموعات الحسابات في الشجرة (Tree Expansion State - جميع المجموعات والحسابات الرئيسية مفتوحة تلقائياً)
    val expandedCodes = remember {
        mutableStateMapOf<String, Boolean>().apply {
            ROOT_ACCOUNT_GROUPS.forEach { put(it.code, true) }
            ChartOfAccountsDefaults.PARENT_ACCOUNTS.forEach { put(it.code, true) }
        }
    }

    // الحساب المحدد حالياً في الشجرة لعرض لوحته السفلية
    var selectedAccount by remember { mutableStateOf<FinancialAccountEntity?>(uiState.selectedChartAccountNode) }

    // حساب أب مسبق الإعداد للإضافة الفرعية الفورية
    var parentPresetForAdd by remember { mutableStateOf<FinancialAccountEntity?>(null) }

    // تصفية حسب مجموعة الجذر الرئسية (1-الأصول، 2-الخصوم...)
    var selectedRootGroupCode by remember { mutableStateOf<String?>(null) }

    // دمج تفاعلي حي وشامل لكافة الحسابات المعتمدة + العملاء والموردين والأصول والمبيعات والمصروفات
    val combinedAccounts = remember(
        accounts,
        uiState.parties,
        uiState.fixedAssets,
        uiState.invoices,
        uiState.expenses,
        uiState.payrollRecords,
        uiState.employeeTransactions,
        uiState.cashShifts
    ) {
        val list = accounts.toMutableList()

        // 1. التوصيل التلقائي للعملاء تحت الحساب الأب "104 - الأصول المتداولة (العملاء والذمم المدينة)"
        uiState.parties.filter { it.type == PartyType.CUSTOMER || it.type == PartyType.BOTH }.forEach { customer ->
            val custCode = "104" + String.format(Locale.US, "%04d", customer.id)
            if (list.none { it.code == custCode || it.name == customer.name }) {
                list.add(
                    FinancialAccountEntity(
                        id = -1000L - customer.id,
                        code = custCode,
                        name = "${customer.name} (عميل)",
                        accountType = FinancialAccountType.CHART_ACCOUNT,
                        parentAccountCode = "104",
                        parentAccountName = "104 - الأصول المتداولة (العملاء والذمم المدينة)",
                        isMainAccount = false,
                        level = 3,
                        finalAccountMapping = "BALANCE_SHEET",
                        debitCreditNature = "DEBIT",
                        currentBalance = customer.currentBalance,
                        openingBalance = 0.0,
                        isActive = true,
                        notes = "حساب عميل مرتبط تلقائياً من سجل العملاء والموردين"
                    )
                )
            }
        }

        // 2. التوصيل التلقائي للموردين تحت الحساب الأب "201 - الخصوم المتداولة والدائنون (الموردون)"
        uiState.parties.filter { it.type == PartyType.SUPPLIER || it.type == PartyType.BOTH }.forEach { supplier ->
            val suppCode = "201" + String.format(Locale.US, "%04d", supplier.id)
            if (list.none { it.code == suppCode || it.name == supplier.name }) {
                list.add(
                    FinancialAccountEntity(
                        id = -2000L - supplier.id,
                        code = suppCode,
                        name = "${supplier.name} (مورد)",
                        accountType = FinancialAccountType.LIABILITY,
                        parentAccountCode = "201",
                        parentAccountName = "201 - الخصوم المتداولة والدائنون (الموردون)",
                        isMainAccount = false,
                        level = 3,
                        finalAccountMapping = "BALANCE_SHEET",
                        debitCreditNature = "CREDIT",
                        currentBalance = supplier.currentBalance,
                        openingBalance = 0.0,
                        isActive = true,
                        notes = "حساب مورد مرتبط تلقائياً من سجل العملاء والموردين"
                    )
                )
            }
        }

        // 3. التوصيل التلقائي للأصول الثابتة تحت الحساب "106 - الأصول الثابتة وغير الملموسة"
        uiState.fixedAssets.forEach { asset ->
            val assetCode = "106" + String.format(Locale.US, "%04d", asset.id)
            if (list.none { it.code == assetCode || it.name == asset.name }) {
                list.add(
                    FinancialAccountEntity(
                        id = -3000L - asset.id,
                        code = assetCode,
                        name = asset.name,
                        accountType = FinancialAccountType.CHART_ACCOUNT,
                        parentAccountCode = "106",
                        parentAccountName = "106 - الأصول الثابتة وغير الملموسة",
                        isMainAccount = false,
                        level = 3,
                        finalAccountMapping = "BALANCE_SHEET",
                        debitCreditNature = "DEBIT",
                        currentBalance = asset.purchaseCost,
                        openingBalance = asset.purchaseCost,
                        isActive = true,
                        notes = "أصل ثابت مسجل بالنظام"
                    )
                )
            }
        }

        // 4. التوصيل والربط الحي التلقائي لحسابات المجموعة (4): الإيرادات والمبيعات (Revenues)
        val grossSalesAmount = uiState.invoices.filter { it.type == InvoiceType.SALE }.sumOf { it.total }
        val salesReturnAmount = uiState.invoices.filter { it.type == InvoiceType.SALE_RETURN }.sumOf { it.total }
        val extraRevenueAmount = uiState.cashShifts.filter { it.cashDiscrepancy > 0.0 }.sumOf { it.cashDiscrepancy }

        // 40101: إيرادات مبيعات البضائع والمنتجات المباشرة
        val salesAccountCode = "40101"
        val existingSalesIndex = list.indexOfFirst { it.code == salesAccountCode }
        if (existingSalesIndex != -1) {
            val existing = list[existingSalesIndex]
            list[existingSalesIndex] = existing.copy(currentBalance = grossSalesAmount)
        } else {
            list.add(
                FinancialAccountEntity(
                    id = -4001L,
                    code = salesAccountCode,
                    name = "إيرادات مبيعات البضائع والمنتجات المباشرة",
                    accountType = FinancialAccountType.CHART_ACCOUNT,
                    parentAccountCode = "401",
                    parentAccountName = "401 - إيرادات مبيعات البضائع والخدمات",
                    isMainAccount = false,
                    level = 3,
                    finalAccountMapping = "PROFIT_LOSS",
                    debitCreditNature = "CREDIT",
                    currentBalance = grossSalesAmount,
                    openingBalance = 0.0,
                    isActive = true,
                    notes = "حساب إيراد مبيعات مباشر مرتبط بجميع فواتير البيع"
                )
            )
        }

        // 40102: مردودات ومسموحات المبيعات
        val salesReturnCode = "40102"
        val existingSalesReturnIndex = list.indexOfFirst { it.code == salesReturnCode }
        if (existingSalesReturnIndex != -1) {
            val existing = list[existingSalesReturnIndex]
            list[existingSalesReturnIndex] = existing.copy(currentBalance = salesReturnAmount)
        } else {
            list.add(
                FinancialAccountEntity(
                    id = -4002L,
                    code = salesReturnCode,
                    name = "مردودات ومسموحات المبيعات",
                    accountType = FinancialAccountType.CHART_ACCOUNT,
                    parentAccountCode = "401",
                    parentAccountName = "401 - إيرادات مبيعات البضائع والخدمات",
                    isMainAccount = false,
                    level = 3,
                    finalAccountMapping = "PROFIT_LOSS",
                    debitCreditNature = "DEBIT",
                    currentBalance = salesReturnAmount,
                    openingBalance = 0.0,
                    isActive = true,
                    notes = "حساب مرتجعات المبيعات"
                )
            )
        }

        // 40201: الإيرادات والأرباح المتنوعة والأخرى
        val extraRevCode = "40201"
        val existingExtraRevIndex = list.indexOfFirst { it.code == extraRevCode }
        if (existingExtraRevIndex != -1) {
            val existing = list[existingExtraRevIndex]
            list[existingExtraRevIndex] = existing.copy(currentBalance = extraRevenueAmount)
        } else {
            list.add(
                FinancialAccountEntity(
                    id = -4003L,
                    code = extraRevCode,
                    name = "إيرادات خدمات وأرباح تسوية متنوعة",
                    accountType = FinancialAccountType.CHART_ACCOUNT,
                    parentAccountCode = "402",
                    parentAccountName = "402 - الإيرادات والأرباح المتنوعة والأخرى",
                    isMainAccount = false,
                    level = 3,
                    finalAccountMapping = "PROFIT_LOSS",
                    debitCreditNature = "CREDIT",
                    currentBalance = extraRevenueAmount,
                    openingBalance = 0.0,
                    isActive = true,
                    notes = "إيرادات متنوعة وأرباح تسوية الشفتات"
                )
            )
        }

        // 5. التوصيل والربط الحي التلقائي لحسابات المجموعة (5): المصروفات والتكاليف (Expenses)
        val grossPurchasesAmount = uiState.invoices.filter { it.type == InvoiceType.PURCHASE }.sumOf { it.total }
        val purchaseReturnAmount = uiState.invoices.filter { it.type == InvoiceType.PURCHASE_RETURN }.sumOf { it.total }

        // 50101: تكلفة المشتريات والبضاعة المتاحة للبيع
        val purchaseAccountCode = "50101"
        val existingPurIndex = list.indexOfFirst { it.code == purchaseAccountCode }
        if (existingPurIndex != -1) {
            val existing = list[existingPurIndex]
            list[existingPurIndex] = existing.copy(currentBalance = grossPurchasesAmount)
        } else {
            list.add(
                FinancialAccountEntity(
                    id = -5001L,
                    code = purchaseAccountCode,
                    name = "تكلفة المشتريات والبضاعة المتاحة للبيع",
                    accountType = FinancialAccountType.EXPENSE,
                    parentAccountCode = "501",
                    parentAccountName = "501 - تكلفة البضاعة المباعة (المشتريات)",
                    isMainAccount = false,
                    level = 3,
                    finalAccountMapping = "PROFIT_LOSS",
                    debitCreditNature = "DEBIT",
                    currentBalance = grossPurchasesAmount,
                    openingBalance = 0.0,
                    isActive = true,
                    notes = "حساب مشتريات وتكلفة البضاعة المرتبط بفواتير الشراء"
                )
            )
        }

        // 50102: مردودات ومسموحات المشتريات
        val purReturnCode = "50102"
        val existingPurReturnIndex = list.indexOfFirst { it.code == purReturnCode }
        if (existingPurReturnIndex != -1) {
            val existing = list[existingPurReturnIndex]
            list[existingPurReturnIndex] = existing.copy(currentBalance = purchaseReturnAmount)
        } else {
            list.add(
                FinancialAccountEntity(
                    id = -5002L,
                    code = purReturnCode,
                    name = "مردودات ومسموحات المشتريات",
                    accountType = FinancialAccountType.EXPENSE,
                    parentAccountCode = "501",
                    parentAccountName = "501 - تكلفة البضاعة المباعة (المشتريات)",
                    isMainAccount = false,
                    level = 3,
                    finalAccountMapping = "PROFIT_LOSS",
                    debitCreditNature = "CREDIT",
                    currentBalance = purchaseReturnAmount,
                    openingBalance = 0.0,
                    isActive = true,
                    notes = "حساب مرتجعات المشتريات"
                )
            )
        }

        // 502: المصروفات والنثريات التشغيلية الموزعة حسب تصنيف المصروفات (كهرباء، إيجار، نظافة، صيانة، ضيافة، نثريات...)
        val distinctExpenseCategories = uiState.expenses.map { it.category }.distinct()
        distinctExpenseCategories.forEachIndexed { idx, cat ->
            val catTotal = uiState.expenses.filter { it.category == cat }.sumOf { it.amount }
            val isSalaryCat = cat.contains("رواتب") || cat.contains("أجور") || cat.contains("عمالة")

            val targetParentCode = if (isSalaryCat) "503" else "502"
            val targetParentName = if (isSalaryCat) "503 - مصروفات الرواتب والأجور والمنافع" else "502 - المصروفات والنثريات التشغيلية والإدارية"
            val expSubCode = if (isSalaryCat) "5030${idx + 1}" else "502" + String.format(Locale.US, "%02d", idx + 1)

            val existingIndex = list.indexOfFirst {
                it.code == expSubCode || it.name == "مصروف $cat" || (it.name == cat && it.parentAccountCode == targetParentCode)
            }
            if (existingIndex != -1) {
                val existing = list[existingIndex]
                list[existingIndex] = existing.copy(currentBalance = catTotal)
            } else {
                list.add(
                    FinancialAccountEntity(
                        id = -5100L - idx,
                        code = expSubCode,
                        name = if (cat.startsWith("مصروف")) cat else "مصروف $cat",
                        accountType = FinancialAccountType.EXPENSE,
                        parentAccountCode = targetParentCode,
                        parentAccountName = targetParentName,
                        isMainAccount = false,
                        level = 3,
                        finalAccountMapping = "PROFIT_LOSS",
                        debitCreditNature = "DEBIT",
                        currentBalance = catTotal,
                        openingBalance = 0.0,
                        isActive = true,
                        notes = "حساب مصروف تشغيلي مرتبط بتصنيف $cat"
                    )
                )
            }
        }

        // 50301: أجور ورواتب وطاقم العمل والعمالة
        val totalPayroll = uiState.payrollRecords.sumOf { it.netPayableSalary } +
                uiState.employeeTransactions.filter { it.type == EmployeeTransactionType.ADVANCE || it.type == EmployeeTransactionType.BONUS }.sumOf { it.amount }
        if (totalPayroll > 0.0) {
            val salaryCode = "50301"
            val existingSalaryIndex = list.indexOfFirst { it.code == salaryCode }
            if (existingSalaryIndex != -1) {
                val existing = list[existingSalaryIndex]
                list[existingSalaryIndex] = existing.copy(currentBalance = existing.currentBalance + totalPayroll)
            } else {
                list.add(
                    FinancialAccountEntity(
                        id = -5301L,
                        code = salaryCode,
                        name = "مصروف أجور ورواتب وسلف العمالة والطاقم",
                        accountType = FinancialAccountType.EXPENSE,
                        parentAccountCode = "503",
                        parentAccountName = "503 - مصروفات الرواتب والأجور والمنافع",
                        isMainAccount = false,
                        level = 3,
                        finalAccountMapping = "PROFIT_LOSS",
                        debitCreditNature = "DEBIT",
                        currentBalance = totalPayroll,
                        openingBalance = 0.0,
                        isActive = true,
                        notes = "إجمالي مستحقات ورواتب وسلف طاقم العمل والعمالة"
                    )
                )
            }
        }

        list
    }

    // تصفية الحسابات الفرعية وتطبيق شرط الأرصدة الصفرية (Show/Hide Zero Balances Rule)
    val filteredSubAccounts = remember(combinedAccounts, showZeroAccounts, searchQuery) {
        val cleanQ = normalizeArabicItemSearch(searchQuery)
        combinedAccounts.filter { acc ->
            val isNonZero = abs(acc.currentBalance) > 0.001 || abs(acc.openingBalance) > 0.001
            val satisfiesZeroRule = showZeroAccounts || isNonZero || cleanQ.isNotBlank()

            val matchesSearch = cleanQ.isBlank() ||
                    normalizeArabicItemSearch(acc.name).contains(cleanQ) ||
                    normalizeArabicItemSearch(acc.code).contains(cleanQ) ||
                    normalizeArabicItemSearch(acc.parentAccountName).contains(cleanQ)

            satisfiesZeroRule && matchesSearch
        }
    }

    // توسيع جميع المجموعات عند بدء البحث المكتوب
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank()) {
            ROOT_ACCOUNT_GROUPS.forEach { expandedCodes[it.code] = true }
            ChartOfAccountsDefaults.PARENT_ACCOUNTS.forEach { expandedCodes[it.code] = true }
            combinedAccounts.filter { it.isMainAccount }.forEach { expandedCodes[it.code] = true }
        }
    }

    val totalMainAccountsCount = ChartOfAccountsDefaults.PARENT_ACCOUNTS.size + combinedAccounts.count { it.isMainAccount }
    val totalSubAccountsCount = combinedAccounts.count { !it.isMainAccount }
    val activeAccountsCount = combinedAccounts.count { it.isActive }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(10.dp)
            .testTag("chart_of_accounts_screen")
    ) {
        // 1. الترويسة الهيدر العلوي: زر الإضافة البارز + زر إظهار/إخفاء الأرصدة الصفرية
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountTree,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "شجرة الدليل المحاسبي الشاملة",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "$totalMainAccountsCount حساب رئيسي • $totalSubAccountsCount حساب فرعي ($activeAccountsCount نشط)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // زر إضافة حساب جديد البارز (+)
                    Button(
                        onClick = {
                            parentPresetForAdd = null
                            onOpenAddDialog()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("btn_open_add_account")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إضافة حساب", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // زر مفتاح إظهار / إخفاء الأرصدة الصفرية (Show/Hide Zero Balances Toggle) + أدوات الشجرة
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // مفتاح التحكم بالأرصدة الصفرية
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (showZeroAccounts) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, if (showZeroAccounts) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.clickable {
                            showZeroAccounts = !showZeroAccounts
                            onToggleShowZeroAccounts()
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = if (showZeroAccounts) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = if (showZeroAccounts) MaterialTheme.colorScheme.primary else Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (showZeroAccounts) "الأرصدة الصفرية: ظاهرة" else "الأرصدة الصفرية: مخفية",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (showZeroAccounts) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Switch(
                                checked = showZeroAccounts,
                                onCheckedChange = {
                                    showZeroAccounts = it
                                    onToggleShowZeroAccounts()
                                },
                                modifier = Modifier
                                    .scale(0.7f)
                                    .testTag("toggle_zero_balances")
                            )
                        }
                    }

                    // أزرار توسيع/طي الشجرة
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedButton(
                            onClick = {
                                val allExpanded = ROOT_ACCOUNT_GROUPS.all { expandedCodes[it.code] == true }
                                ROOT_ACCOUNT_GROUPS.forEach { expandedCodes[it.code] = !allExpanded }
                                ChartOfAccountsDefaults.PARENT_ACCOUNTS.forEach { expandedCodes[it.code] = !allExpanded }
                                combinedAccounts.filter { it.isMainAccount }.forEach { expandedCodes[it.code] = !allExpanded }
                            },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (ROOT_ACCOUNT_GROUPS.all { expandedCodes[it.code] == true }) Icons.Default.UnfoldLess else Icons.Default.UnfoldMore,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (ROOT_ACCOUNT_GROUPS.all { expandedCodes[it.code] == true }) "طي الكل" else "توسيع الكل",
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // شريط البحث المكتوب باللغة العربية والإنجليزية
                AppSearchBar(
                    value = searchQuery,
                    onValueChange = onSearchChanged,
                    placeholder = "ابحث باسم الحساب، الكود، العميل، أو المورد..."
                )

                Spacer(modifier = Modifier.height(8.dp))

                // شرائح تصفية المجموعات الرئيسية (الكل، الأصول، الخصوم، حقوق الملكية...)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedRootGroupCode == null,
                            onClick = { selectedRootGroupCode = null },
                            label = { Text("الكل (${filteredSubAccounts.size})", fontSize = 11.sp) },
                            modifier = Modifier.testTag("filter_root_all")
                        )
                    }
                    items(ROOT_ACCOUNT_GROUPS) { root ->
                        FilterChip(
                            selected = selectedRootGroupCode == root.code,
                            onClick = {
                                selectedRootGroupCode = if (selectedRootGroupCode == root.code) null else root.code
                            },
                            label = { Text(root.nameArabic, fontSize = 11.sp) },
                            leadingIcon = {
                                Icon(root.icon, contentDescription = null, tint = root.primaryColor, modifier = Modifier.size(14.dp))
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. القسم الأوسط والشجري (Hierarchical Expandable Tree View)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val filteredRootGroups = remember(selectedRootGroupCode) {
                if (selectedRootGroupCode == null) ROOT_ACCOUNT_GROUPS
                else ROOT_ACCOUNT_GROUPS.filter { it.code == selectedRootGroupCode }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredRootGroups, key = { "root_${it.code}" }) { rootGroup ->
                    val isRootExpanded = expandedCodes[rootGroup.code] ?: true

                    val rootChildParents = remember(combinedAccounts, rootGroup.code) {
                        ChartOfAccountsDefaults.PARENT_ACCOUNTS.filter { it.rootGroupCode == rootGroup.code }
                    }

                    val rootSubAccounts = remember(filteredSubAccounts, rootGroup.code) {
                        filteredSubAccounts.filter { acc ->
                            rootChildParents.any { p -> p.code == acc.parentAccountCode } ||
                                    acc.parentAccountCode.startsWith(rootGroup.code)
                        }
                    }

                    val rootTotalBalance = remember(rootSubAccounts) {
                        rootSubAccounts.sumOf { it.currentBalance }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(rootGroup.primaryColor.copy(alpha = 0.05f))
                            .border(1.dp, rootGroup.primaryColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    ) {
                        // صف المستوى الأول (Root Node Category)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedCodes[rootGroup.code] = !isRootExpanded }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (isRootExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = rootGroup.primaryColor
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = rootGroup.icon,
                                    contentDescription = null,
                                    tint = rootGroup.primaryColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = rootGroup.nameArabic,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = rootGroup.primaryColor
                                    )
                                    Text(
                                        text = "المستوى 1 • ${rootSubAccounts.size} حساب فرعي",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = rootGroup.primaryColor.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "${String.format(Locale.US, "%.2f", rootTotalBalance)} $currencySymbol",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = rootGroup.primaryColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // أطفال المستوى الأول (Level 2 Parent Accounts & Level 3 Sub Accounts)
                        AnimatedVisibility(visible = isRootExpanded) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 12.dp, end = 6.dp, bottom = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                rootChildParents.forEach { parent ->
                                    val isParentExpanded = expandedCodes[parent.code] ?: true

                                    val parentSubAccounts = rootSubAccounts.filter { it.parentAccountCode == parent.code }
                                    val parentBalance = parentSubAccounts.sumOf { it.currentBalance }

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surface)
                                            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                    ) {
                                        // صف المستوى الثاني (Level 2 Parent Account Node)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { expandedCodes[parent.code] = !isParentExpanded }
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(
                                                    imageVector = if (isParentExpanded) Icons.Default.FolderOpen else Icons.Default.Folder,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = parent.name,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "(${parentSubAccounts.size})",
                                                    fontSize = 10.sp,
                                                    color = Color.Gray
                                                )
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "${String.format(Locale.US, "%.2f", parentBalance)} $currencySymbol",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (parentBalance >= 0) Color(0xFF0F5132) else MaterialTheme.colorScheme.error
                                                )
                                                IconButton(
                                                    onClick = {
                                                        parentPresetForAdd = FinancialAccountEntity(
                                                            code = "${parent.code}01",
                                                            name = "",
                                                            accountType = parent.defaultType,
                                                            parentAccountCode = parent.code,
                                                            parentAccountName = parent.name,
                                                            debitCreditNature = parent.defaultNature,
                                                            finalAccountMapping = parent.defaultFinalAccount
                                                        )
                                                        onOpenAddDialog()
                                                    },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Default.AddCircleOutline, contentDescription = "إضافة فرعي", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }

                                        // أطفال المستوى الثالث (Level 3 Sub Accounts Leaf Nodes)
                                        AnimatedVisibility(visible = isParentExpanded || searchQuery.isNotBlank()) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(start = 16.dp, end = 6.dp, bottom = 6.dp),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                if (parentSubAccounts.isEmpty()) {
                                                    Text(
                                                        text = if (!showZeroAccounts) "لا توجد حسابات فرعية برصيد غير صفري (انقر فوق مفتاح العرض لإظهار الجميع)" else "لا توجد حسابات مسجلة تحت هذا الحساب الأب",
                                                        fontSize = 10.sp,
                                                        color = Color.Gray,
                                                        modifier = Modifier.padding(6.dp)
                                                    )
                                                } else {
                                                    parentSubAccounts.forEach { acc ->
                                                        val isSelected = selectedAccount?.code == acc.code

                                                        Surface(
                                                            shape = RoundedCornerShape(8.dp),
                                                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                                            border = BorderStroke(
                                                                if (isSelected) 1.5.dp else 0.5.dp,
                                                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                                            ),
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clickable {
                                                                    selectedAccount = acc
                                                                    onSelectAccountNode(acc)
                                                                }
                                                                .testTag("account_item_${acc.code}")
                                                        ) {
                                                            Row(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Row(
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    modifier = Modifier.weight(1f)
                                                                ) {
                                                                    val typeIcon = when (acc.accountType) {
                                                                        FinancialAccountType.BANK -> Icons.Default.AccountBalance
                                                                        FinancialAccountType.E_WALLET -> Icons.Default.AccountBalanceWallet
                                                                        FinancialAccountType.CASH_DRAWER -> Icons.Default.PointOfSale
                                                                        FinancialAccountType.LIABILITY -> Icons.Default.Receipt
                                                                        FinancialAccountType.EXPENSE -> Icons.Default.MonetizationOn
                                                                        else -> Icons.Default.InsertDriveFile
                                                                    }
                                                                    Icon(typeIcon, contentDescription = null, tint = if (acc.isActive) MaterialTheme.colorScheme.primary else Color.Gray, modifier = Modifier.size(16.dp))
                                                                    Spacer(modifier = Modifier.width(6.dp))
                                                                    Column {
                                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                                            Text(
                                                                                text = acc.name,
                                                                                fontSize = 11.sp,
                                                                                fontWeight = FontWeight.Bold
                                                                            )
                                                                            Spacer(modifier = Modifier.width(4.dp))
                                                                            if (!acc.isActive) {
                                                                                Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.errorContainer) {
                                                                                    Text("معطل", fontSize = 8.sp, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                                                                }
                                                                            }
                                                                        }
                                                                        Text(
                                                                            text = "كود: ${acc.code} | ${acc.accountType.labelArabic}",
                                                                            fontSize = 9.sp,
                                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                        )
                                                                    }
                                                                }

                                                                Text(
                                                                    text = "${String.format(Locale.US, "%.2f", acc.currentBalance)} $currencySymbol",
                                                                    fontSize = 11.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = if (acc.currentBalance >= 0) Color(0xFF0F5132) else MaterialTheme.colorScheme.error
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. القسم السفلي: اللوحة السفلية القابلة للسحب/التمرير لتفاصيل الحساب المحدد (Bottom Detail Panel / Sheet)
        AnimatedVisibility(
            visible = selectedAccount != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            selectedAccount?.let { acc ->
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .testTag("selected_account_detail_sheet")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(14.dp)
                    ) {
                        // مقبض السحب البصري اللطيف (Draggable Handle Bar)
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.outlineVariant)
                                .align(Alignment.CenterHorizontally)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = acc.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "كود الحساب: ${acc.code} | الأب: ${acc.parentAccountName}",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    selectedAccount = null
                                    onSelectAccountNode(null)
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "إغلاق التفاصيل", modifier = Modifier.size(18.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // بطاقات تفاصيل البيانات المحاسبية القياسية
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val natureText = if (acc.debitCreditNature == "DEBIT" || acc.code.startsWith("1") || acc.code.startsWith("5")) "مدين (Debit)" else "دائن (Credit)"
                            val isIncomeStatement = acc.finalAccountMapping == "PROFIT_LOSS" || acc.code.startsWith("4") || acc.code.startsWith("5")
                            val reportText = if (isIncomeStatement) "قائمة الدخل (أرباح وخسائر)" else "الميزانية العمومية"

                            SuggestionChip(
                                onClick = {},
                                label = { Text("طبيعة: $natureText", fontSize = 10.sp, fontWeight = FontWeight.SemiBold) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = if (natureText.startsWith("مدين")) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                )
                            )
                            SuggestionChip(
                                onClick = {},
                                label = { Text("تقرير: $reportText", fontSize = 10.sp, fontWeight = FontWeight.SemiBold) }
                            )
                            SuggestionChip(
                                onClick = {},
                                label = { Text("النوع: ${if (acc.isMainAccount) "رئيسي" else "فرعي"}", fontSize = 10.sp) }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // بطاقة إحصائيات الحركات المالية الحية المرتبطة
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "إحصائيات الحركات والنشاط المالي الحي:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))

                                when {
                                    acc.code.startsWith("5") || acc.parentAccountCode.startsWith("5") -> {
                                        val relatedExps = uiState.expenses.filter {
                                            acc.name.contains(it.category) || it.category.contains(acc.name.removePrefix("مصروف "))
                                        }
                                        val expCount = relatedExps.size
                                        val expTotal = relatedExps.sumOf { it.amount }
                                        Text("• عدد سندات المصروفات المسجلة: $expCount حركة", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("• إجمالي النفقات في السجل: ${String.format(Locale.US, "%.2f", if (expTotal > 0) expTotal else acc.currentBalance)} $currencySymbol", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                    }
                                    acc.code.startsWith("4") || acc.parentAccountCode.startsWith("4") -> {
                                        val salesInvoices = uiState.invoices.filter { it.type == InvoiceType.SALE }
                                        val salesCount = salesInvoices.size
                                        val salesTotal = salesInvoices.sumOf { it.total }
                                        Text("• عدد فواتير المبيعات: $salesCount فاتورة", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("• إجمالي الإيرادات المحققة: ${String.format(Locale.US, "%.2f", if (salesTotal > 0) salesTotal else acc.currentBalance)} $currencySymbol", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F5132))
                                    }
                                    acc.parentAccountCode == "104" -> {
                                        Text("• ذمم مدينة للعميل - رصيد المبيعات والمستحقات القائمة", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("• المتبقي القائم: ${String.format(Locale.US, "%.2f", acc.currentBalance)} $currencySymbol", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                    acc.parentAccountCode == "201" -> {
                                        Text("• مستحقات المورد الدائنة - رصيد المشتريات والتوريدات", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("• الرصيد الدائن للمورد: ${String.format(Locale.US, "%.2f", acc.currentBalance)} $currencySymbol", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                    }
                                    else -> {
                                        Text("• حالة الحساب: ${if (acc.isActive) "نشط ومتاح للاستخدام" else "معطل"}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("• الرصيد الحجمي التجميعي: ${String.format(Locale.US, "%.2f", acc.currentBalance)} $currencySymbol", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // الرصيد الجاري الحالي والرصيد الافتتاحي
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("الرصيد الجاري الحالي:", fontSize = 10.sp, color = Color.Gray)
                                Text(
                                    text = "${String.format(Locale.US, "%.2f", acc.currentBalance)} $currencySymbol",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (acc.currentBalance >= 0) Color(0xFF0F5132) else MaterialTheme.colorScheme.error
                                )
                            }

                            if (acc.openingBalance != 0.0) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("الرصيد الافتتاحي:", fontSize = 10.sp, color = Color.Gray)
                                    Text(
                                        text = "${String.format(Locale.US, "%.2f", acc.openingBalance)} $currencySymbol",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        if (acc.notes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("ملاحظات: ${acc.notes}", fontSize = 10.sp, color = Color.Gray)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // أزرار التحكم السريعة (تعديل، تعطيل، حذف)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (acc.id > 0) {
                                OutlinedButton(
                                    onClick = { onOpenEditDialog(acc) },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("تعديل", fontSize = 11.sp)
                                }

                                TextButton(
                                    onClick = { onToggleActive(acc) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text(if (acc.isActive) "تعطيل" else "تفعيل", fontSize = 11.sp, color = if (acc.isActive) MaterialTheme.colorScheme.error else Color(0xFF0F5132))
                                }

                                Button(
                                    onClick = { onRequestDeleteAccount(acc) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("حذف", fontSize = 11.sp)
                                }
                            } else {
                                Text(
                                    text = "حساب عميل/مورد أو أصل تجميعي مدمج حي من قاعدة البيانات",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // إظهار نافذة إضافة/تعديل حساب إذا كانت الحالة نشطة
    if (uiState.showAddEditAccountDialog) {
        AddEditAccountDialog(
            initialAccount = uiState.selectedAccountForEdit,
            parentPresetAccount = parentPresetForAdd,
            existingAccounts = combinedAccounts,
            currencySymbol = currencySymbol,
            onSave = { account ->
                onSaveAccount(account)
                parentPresetForAdd = null
            },
            onDismiss = {
                parentPresetForAdd = null
                onDismissAddEditDialog()
            }
        )
    }

    // إظهار حوار حماية الأمان المحاسبي لمنع الحذف بوجود سجلات
    uiState.accountDeletionBlockedDialog?.let { usageResult ->
        AccountDeletionBlockedDialog(
            result = usageResult,
            currencySymbol = currencySymbol,
            onDisableInstead = { onDisableInstead(usageResult) },
            onDismiss = onDismissDeleteDialogs
        )
    }

    // إظهار حوار تأكيد الحذف للحسابات الجديدة النظيفة
    uiState.accountToDelete?.let { cleanAccount ->
        ConfirmDeleteAccountDialog(
            account = cleanAccount,
            onConfirm = { onConfirmDeleteAccount(cleanAccount) },
            onDismiss = onDismissDeleteDialogs
        )
    }
}
