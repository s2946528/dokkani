package com.example.dokkani.ui.screens

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
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Settings
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
fun DokkaniApp(viewModel: DokkaniViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val productsWithUnits by viewModel.productsWithUnits.collectAsStateWithLifecycle()
    val currencies by viewModel.currencies.collectAsStateWithLifecycle()
    val parties by viewModel.parties.collectAsStateWithLifecycle()
    val recentInvoices by viewModel.recentInvoices.collectAsStateWithLifecycle()
    val produceBatches by viewModel.produceBatches.collectAsStateWithLifecycle()
    val settings by viewModel.systemSettings.collectAsStateWithLifecycle()

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
                    "المخطط وقاعدة البيانات" to Icons.Default.AccountBalance,
                    "حاسبة التكلفة (WAC/FIFO)" to Icons.Default.Calculate,
                    "جرد خضار المشكل" to Icons.Default.Eco,
                    "الأصناف والوحدات" to Icons.Default.Inventory2,
                    "إعدادات النظام" to Icons.Default.Settings
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
                when (uiState.selectedTab) {
                    0 -> com.example.dokkani.ui.screens.pos.PosScreen(
                        viewModel = viewModel
                    )
                    1 -> SchemaOverviewScreen(
                        productsWithUnits = productsWithUnits,
                        settings = settings,
                        currenciesCount = currencies.size,
                        partiesCount = parties.size,
                        invoicesCount = recentInvoices.size,
                        produceBatchesCount = produceBatches.size
                    )
                    2 -> CostingEngineScreen(
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
                    3 -> ProduceQuickInventoryScreen(
                        grossWeightInput = uiState.produceGrossWeightInput,
                        costInput = uiState.produceCostInput,
                        expenseInput = uiState.produceExpenseInput,
                        wasteInput = uiState.produceWasteInput,
                        marginInput = uiState.produceMarginInput,
                        crateDescription = uiState.produceCrateDescription,
                        calcSummary = uiState.produceCalcSummary,
                        isSaving = uiState.isSavingProduceBatch,
                        historicalBatches = produceBatches,
                        onInputsChanged = { g, c, e, w, m, d -> viewModel.updateProduceInputs(g, c, e, w, m, d) },
                        onSaveBatch = { viewModel.saveProduceBatchToDatabase() }
                    )
                    4 -> ProductsAndUnitsScreen(
                        productsWithUnits = productsWithUnits
                    )
                    5 -> SystemSettingsScreen(
                        settings = settings,
                        currencies = currencies,
                        parties = parties,
                        invoices = recentInvoices,
                        onUpdateValuationMethod = { viewModel.updateSystemCostingMethod(it) }
                    )
                }
            }
        }
    }
}
