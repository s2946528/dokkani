package com.example.dokkani.ui.screens.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dokkani.data.local.entities.CostValuationMethod
import com.example.dokkani.data.local.entities.CurrencyEntity
import com.example.dokkani.ui.DokkaniViewModel
import com.example.dokkani.ui.FixedAssetInput
import com.example.dokkani.ui.OpeningBalanceCustomer
import com.example.dokkani.ui.OpeningBalanceItem
import com.example.dokkani.ui.OpeningBalanceSupplier
import com.example.dokkani.ui.PropertyStatus
import com.example.ui.theme.DokkaniTheme

enum class GrocerySetupMode {
    NEW_GROCERY,       // [أ] بقالة جديدة كلياً
    EXISTING_GROCERY   // [ب] بقالة قائمة / نقل الدفاتر والجرد
}

enum class WizardPhase {
    WELCOME,          // 1. Entry Point Welcome Screen
    PATH_SELECTION,   // 2. Path Selection Screen
    STEPS,            // Step-by-Step setup screens
    COMPLETION        // Final Completion Screen
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingWizardScreen(
    viewModel: DokkaniViewModel,
    onFinish: () -> Unit
) {
    DokkaniTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            val uiState by viewModel.uiState.collectAsState()

        var wizardPhase by remember { mutableStateOf(WizardPhase.WELCOME) }
        var setupMode by remember { mutableStateOf(GrocerySetupMode.NEW_GROCERY) }
        var currentStep by remember { mutableIntStateOf(0) }

        // العملة الأساسية المختارة
        var selectedBaseCurrency by remember { mutableStateOf<CurrencyEntity?>(null) }

        LaunchedEffect(uiState.baseCurrency, uiState.currencies) {
            if (selectedBaseCurrency == null) {
                selectedBaseCurrency = uiState.baseCurrency
                    ?: uiState.currencies.find { it.isBaseCurrency }
                    ?: uiState.currencies.firstOrNull()
            }
        }

        val activeCurrencySymbol = uiState.baseCurrency?.symbol ?: selectedBaseCurrency?.symbol ?: "ر.س"
        val activeCurrencyName = uiState.baseCurrency?.name ?: selectedBaseCurrency?.name ?: "الريال السعودي"

        // بيانات التهيئة المحاسبية والبيئة العامة
        var storeName by remember { mutableStateOf("تموينات ومخضار السعادة") }
        var adminPin by remember { mutableStateOf("1234") }
        var cashierName by remember { mutableStateOf("كاشير 1") }
        var cashierPin by remember { mutableStateOf("1234") }

        // رأس المال والنقدية
        var initialCapital by remember { mutableStateOf("15000.0") }
        var openingCash by remember { mutableStateOf("500.0") }
        var bankBalance by remember { mutableStateOf("0.0") }
        var valuationMethod by remember { mutableStateOf(CostValuationMethod.WAC) }

        // العقارات والإيجار
        var propertyStatus by remember { mutableStateOf(PropertyStatus.OWNED) }
        var monthlyRent by remember { mutableStateOf("1000.0") }
        var prepaidMonths by remember { mutableStateOf("6") }
        var contractStartDate by remember { mutableStateOf("2026-01-01") }

        // الأصول الثابتة والديكور
        var refrigCost by remember { mutableStateOf("4500.0") }
        var shelvesCost by remember { mutableStateOf("2500.0") }
        var posDevicesCost by remember { mutableStateOf("1800.0") }
        var acLightingCost by remember { mutableStateOf("2000.0") }
        var otherAssetsCost by remember { mutableStateOf("0.0") }

        // بضاعة أول المدة
        var openingItems by remember {
            mutableStateOf(
                listOf(
                    OpeningBalanceItem("أرز بسمتي 5 كجم", "خردوات وتموينات", 15.0, 32.0, 42.0, "6281007010015", "كيس"),
                    OpeningBalanceItem("سكر الأسرة 2 كجم", "تموينات وسكريات", 20.0, 7.5, 9.5, "6281007010022", "حبة/قطعة"),
                    OpeningBalanceItem("زيت دوار الشمس 1.5 لتر", "زيوت ودهون", 12.0, 14.0, 18.0, "6281007010039", "حبة/قطعة")
                )
            )
        }
        var newProdName by remember { mutableStateOf("") }
        var newProdCategory by remember { mutableStateOf("تموينات عامة") }
        var newProdBarcode by remember { mutableStateOf("") }
        var newProdUnit by remember { mutableStateOf("حبة/قطعة") }
        var newProdQty by remember { mutableStateOf("") }
        var newProdCost by remember { mutableStateOf("") }
        var newProdPrice by remember { mutableStateOf("") }

        // ديون العملاء
        var customers by remember {
            mutableStateOf(
                listOf(
                    OpeningBalanceCustomer("أبو أحمد (الحارة)", "0551234567", 350.0),
                    OpeningBalanceCustomer("أم فهد (شقة 4)", "0509876543", 185.5)
                )
            )
        }
        var newCustName by remember { mutableStateOf("") }
        var newCustPhone by remember { mutableStateOf("") }
        var newCustBalance by remember { mutableStateOf("") }

        // مستحقات الموردين
        var suppliers by remember {
            mutableStateOf(
                listOf(
                    OpeningBalanceSupplier("شركة المراعي للألبان", "0561112233", 1200.0),
                    OpeningBalanceSupplier("مورد خضار سوق الجملة", "0544332211", 650.0)
                )
            )
        }
        var newSuppName by remember { mutableStateOf("") }
        var newSuppPhone by remember { mutableStateOf("") }
        var newSuppBalance by remember { mutableStateOf("") }

        var isSubmitting by remember { mutableStateOf(false) }

        val totalSteps = if (setupMode == GrocerySetupMode.NEW_GROCERY) 4 else 5

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = when (wizardPhase) {
                                    WizardPhase.WELCOME -> "معالج دكاني المحاسبي"
                                    WizardPhase.PATH_SELECTION -> "اختر سيناريو النشاط المحاسبي"
                                    WizardPhase.STEPS -> if (setupMode == GrocerySetupMode.NEW_GROCERY) "إعداد بقالة جديدة [أ]" else "ترحيل بقالة قائمة [ب]"
                                    WizardPhase.COMPLETION -> "اكتمل إعداد وتجهيز النظام"
                                },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            if (wizardPhase == WizardPhase.STEPS) {
                                Text(
                                    text = "الخطوة ${currentStep + 1} من $totalSteps",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            },
            bottomBar = {
                if (wizardPhase == WizardPhase.STEPS) {
                    Surface(
                        tonalElevation = 8.dp,
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (currentStep > 0) {
                                        currentStep--
                                    } else {
                                        wizardPhase = WizardPhase.PATH_SELECTION
                                    }
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("السابق", fontWeight = FontWeight.Bold)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // زر تخطي اختياري لخطوة الأصول في البقالة القائمة
                                if (setupMode == GrocerySetupMode.EXISTING_GROCERY && currentStep == 2) {
                                    TextButton(
                                        onClick = {
                                            refrigCost = "0.0"
                                            shelvesCost = "0.0"
                                            posDevicesCost = "0.0"
                                            acLightingCost = "0.0"
                                            otherAssetsCost = "0.0"
                                            currentStep++
                                        },
                                        modifier = Modifier.padding(end = 8.dp)
                                    ) {
                                        Text("تخطي هذه الخطوة", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Button(
                                    onClick = {
                                        if (currentStep < totalSteps - 1) {
                                            if (currentStep == 0 && selectedBaseCurrency != null) {
                                                viewModel.setAsBaseCurrency(selectedBaseCurrency!!)
                                            }
                                            currentStep++
                                        } else {
                                            wizardPhase = WizardPhase.COMPLETION
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = if (currentStep == totalSteps - 1) "مراجعة وإنهاء" else "التالي",
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                if (wizardPhase == WizardPhase.STEPS) {
                    WizardStepIndicator(
                        totalSteps = totalSteps,
                        currentStep = currentStep,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    AnimatedContent(
                        targetState = wizardPhase to currentStep,
                        transitionSpec = {
                            (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> -width } + fadeOut()
                            )
                        },
                        label = "WizardPhaseAnimation"
                    ) { (phase, step) ->
                        when (phase) {
                            WizardPhase.WELCOME -> StepWelcomeScreen(
                                onStartClick = { wizardPhase = WizardPhase.PATH_SELECTION }
                            )

                            WizardPhase.PATH_SELECTION -> StepPathSelection(
                                selectedMode = setupMode,
                                onSelectMode = { setupMode = it },
                                onContinue = {
                                    currentStep = 0
                                    wizardPhase = WizardPhase.STEPS
                                }
                            )

                            WizardPhase.STEPS -> {
                                if (setupMode == GrocerySetupMode.NEW_GROCERY) {
                                    // Branch A steps
                                    when (step) {
                                        0 -> StepCurrencyCapitalValuation(
                                            currencies = uiState.currencies,
                                            selectedCurrency = selectedBaseCurrency ?: uiState.baseCurrency,
                                            onSelectCurrency = { curr ->
                                                selectedBaseCurrency = curr
                                                viewModel.setAsBaseCurrency(curr)
                                            },
                                            capitalText = initialCapital,
                                            onCapitalChange = { initialCapital = it },
                                            valuationMethod = valuationMethod,
                                            onValuationMethodChange = { valuationMethod = it },
                                            currencySymbol = activeCurrencySymbol,
                                            isNewGrocery = true
                                        )

                                        1 -> StepPropertyAndRent(
                                            propertyStatus = propertyStatus,
                                            onPropertyStatusChange = { propertyStatus = it },
                                            monthlyRent = monthlyRent,
                                            onMonthlyRentChange = { monthlyRent = it },
                                            prepaidMonths = prepaidMonths,
                                            onPrepaidMonthsChange = { prepaidMonths = it },
                                            contractStartDate = contractStartDate,
                                            onContractStartDateChange = { contractStartDate = it },
                                            currencySymbol = activeCurrencySymbol
                                        )

                                        2 -> StepFixedAssetsAndEquipment(
                                            refrigCost = refrigCost,
                                            onRefrigChange = { refrigCost = it },
                                            shelvesCost = shelvesCost,
                                            onShelvesChange = { shelvesCost = it },
                                            posDevicesCost = posDevicesCost,
                                            onPosDevicesChange = { posDevicesCost = it },
                                            acLightingCost = acLightingCost,
                                            onAcLightingChange = { acLightingCost = it },
                                            otherAssetsCost = otherAssetsCost,
                                            onOtherAssetsChange = { otherAssetsCost = it },
                                            currencySymbol = activeCurrencySymbol,
                                            isOptional = false
                                        )

                                        3 -> StepSecurityAndUsers(
                                            storeName = storeName,
                                            onStoreNameChange = { storeName = it },
                                            adminPin = adminPin,
                                            onAdminPinChange = { adminPin = it },
                                            cashierName = cashierName,
                                            onCashierNameChange = { cashierName = it },
                                            cashierPin = cashierPin,
                                            onCashierPinChange = { cashierPin = it }
                                        )
                                    }
                                } else {
                                    // Branch B steps
                                    when (step) {
                                        0 -> StepCurrencyCapitalValuation(
                                            currencies = uiState.currencies,
                                            selectedCurrency = selectedBaseCurrency ?: uiState.baseCurrency,
                                            onSelectCurrency = { curr ->
                                                selectedBaseCurrency = curr
                                                viewModel.setAsBaseCurrency(curr)
                                            },
                                            capitalText = initialCapital,
                                            onCapitalChange = { initialCapital = it },
                                            valuationMethod = valuationMethod,
                                            onValuationMethodChange = { valuationMethod = it },
                                            currencySymbol = activeCurrencySymbol,
                                            isNewGrocery = false
                                        )

                                        1 -> StepCashDrawerAndBank(
                                            openingCash = openingCash,
                                            onOpeningCashChange = { openingCash = it },
                                            bankBalance = bankBalance,
                                            onBankBalanceChange = { bankBalance = it },
                                            propertyStatus = propertyStatus,
                                            onPropertyStatusChange = { propertyStatus = it },
                                            monthlyRent = monthlyRent,
                                            onMonthlyRentChange = { monthlyRent = it },
                                            prepaidMonths = prepaidMonths,
                                            onPrepaidMonthsChange = { prepaidMonths = it },
                                            currencySymbol = activeCurrencySymbol
                                        )

                                        2 -> StepFixedAssetsAndEquipment(
                                            refrigCost = refrigCost,
                                            onRefrigChange = { refrigCost = it },
                                            shelvesCost = shelvesCost,
                                            onShelvesChange = { shelvesCost = it },
                                            posDevicesCost = posDevicesCost,
                                            onPosDevicesChange = { posDevicesCost = it },
                                            acLightingCost = acLightingCost,
                                            onAcLightingChange = { acLightingCost = it },
                                            otherAssetsCost = otherAssetsCost,
                                            onOtherAssetsChange = { otherAssetsCost = it },
                                            currencySymbol = activeCurrencySymbol,
                                            isOptional = true
                                        )

                                        3 -> StepFlexibleOpeningBalances(
                                            currencySymbol = activeCurrencySymbol,
                                            openingItems = openingItems,
                                            newProdName = newProdName,
                                            onNewProdNameChange = { newProdName = it },
                                            newProdCategory = newProdCategory,
                                            onNewProdCategoryChange = { newProdCategory = it },
                                            newProdBarcode = newProdBarcode,
                                            onNewProdBarcodeChange = { newProdBarcode = it },
                                            newProdUnit = newProdUnit,
                                            onNewProdUnitChange = { newProdUnit = it },
                                            newProdQty = newProdQty,
                                            onNewProdQtyChange = { newProdQty = it },
                                            newProdCost = newProdCost,
                                            onNewProdCostChange = { newProdCost = it },
                                            newProdPrice = newProdPrice,
                                            onNewProdPriceChange = { newProdPrice = it },
                                            onAddItem = {
                                                val qty = newProdQty.toDoubleOrNull() ?: 0.0
                                                val cost = newProdCost.toDoubleOrNull() ?: 0.0
                                                val price = newProdPrice.toDoubleOrNull() ?: cost
                                                if (newProdName.isNotBlank() && qty > 0.0) {
                                                    openingItems = openingItems + OpeningBalanceItem(
                                                        name = newProdName.trim(),
                                                        category = newProdCategory.trim(),
                                                        quantity = qty,
                                                        costPrice = cost,
                                                        sellingPrice = price,
                                                        barcode = newProdBarcode.trim(),
                                                        unitName = newProdUnit.trim().ifBlank { "حبة/قطعة" }
                                                    )
                                                    newProdName = ""
                                                    newProdBarcode = ""
                                                    newProdUnit = "حبة/قطعة"
                                                    newProdQty = ""
                                                    newProdCost = ""
                                                    newProdPrice = ""
                                                }
                                            },
                                            onRemoveItem = { item ->
                                                openingItems = openingItems.filter { it != item }
                                            },
                                            customers = customers,
                                            newCustName = newCustName,
                                            onCustNameChange = { newCustName = it },
                                            newCustPhone = newCustPhone,
                                            onCustPhoneChange = { newCustPhone = it },
                                            newCustBalance = newCustBalance,
                                            onCustBalanceChange = { newCustBalance = it },
                                            onAddCustomer = {
                                                val bal = newCustBalance.toDoubleOrNull() ?: 0.0
                                                if (newCustName.isNotBlank()) {
                                                    customers = customers + OpeningBalanceCustomer(newCustName, newCustPhone, bal)
                                                    newCustName = ""
                                                    newCustPhone = ""
                                                    newCustBalance = ""
                                                }
                                            },
                                            onRemoveCustomer = { c -> customers = customers.filter { it != c } },
                                            suppliers = suppliers,
                                            newSuppName = newSuppName,
                                            onSuppNameChange = { newSuppName = it },
                                            newSuppPhone = newSuppPhone,
                                            onSuppPhoneChange = { newSuppPhone = it },
                                            newSuppBalance = newSuppBalance,
                                            onSuppBalanceChange = { newSuppBalance = it },
                                            onAddSupplier = {
                                                val bal = newSuppBalance.toDoubleOrNull() ?: 0.0
                                                if (newSuppName.isNotBlank()) {
                                                    suppliers = suppliers + OpeningBalanceSupplier(newSuppName, newSuppPhone, bal)
                                                    newSuppName = ""
                                                    newSuppPhone = ""
                                                    newSuppBalance = ""
                                                }
                                            },
                                            onRemoveSupplier = { s -> suppliers = suppliers.filter { it != s } }
                                        )

                                        4 -> StepSecurityAndUsers(
                                            storeName = storeName,
                                            onStoreNameChange = { storeName = it },
                                            adminPin = adminPin,
                                            onAdminPinChange = { adminPin = it },
                                            cashierName = cashierName,
                                            onCashierNameChange = { cashierName = it },
                                            cashierPin = cashierPin,
                                            onCashierPinChange = { cashierPin = it }
                                        )
                                    }
                                }
                            }

                            WizardPhase.COMPLETION -> StepCompletionScreen(
                                setupMode = setupMode,
                                storeName = storeName,
                                currencyName = activeCurrencyName,
                                currencySymbol = activeCurrencySymbol,
                                initialCapital = initialCapital,
                                openingCash = openingCash,
                                valuationMethod = valuationMethod,
                                propertyStatus = propertyStatus,
                                monthlyRent = monthlyRent,
                                prepaidMonths = prepaidMonths,
                                isSubmitting = isSubmitting,
                                onLaunchSystem = {
                                    if (!isSubmitting) {
                                        isSubmitting = true
                                        val fixedAssetList = listOf(
                                            FixedAssetInput("ثلاجات ومعدات تبريد", "ثلاجات وتبريد", refrigCost.toDoubleOrNull() ?: 0.0),
                                            FixedAssetInput("أرفف وديكور المحل", "أرفف وتجهيزات", shelvesCost.toDoubleOrNull() ?: 0.0),
                                            FixedAssetInput("أجهزة نقاط البيع والباربود", "أجهزة وموازين", posDevicesCost.toDoubleOrNull() ?: 0.0),
                                            FixedAssetInput("أجهزة تكييف وإضاءة", "تكييف وإضاءة", acLightingCost.toDoubleOrNull() ?: 0.0),
                                            FixedAssetInput("أصول وتجهيزات أخرى", "أصول أخرى", otherAssetsCost.toDoubleOrNull() ?: 0.0)
                                        ).filter { it.purchaseCost > 0.0 }

                                        viewModel.completeOnboarding(
                                            selectedBaseCurrency = selectedBaseCurrency ?: uiState.baseCurrency,
                                            isNewGrocery = setupMode == GrocerySetupMode.NEW_GROCERY,
                                            storeName = storeName.ifBlank { "تموينات ومخضار السعادة" },
                                            adminPin = adminPin,
                                            cashierName = cashierName,
                                            cashierPin = cashierPin,
                                            initialCapital = initialCapital.toDoubleOrNull() ?: 0.0,
                                            openingCashDrawer = openingCash.toDoubleOrNull() ?: 0.0,
                                            bankBalance = bankBalance.toDoubleOrNull() ?: 0.0,
                                            valuationMethod = valuationMethod,
                                            propertyStatus = propertyStatus,
                                            monthlyRent = monthlyRent.toDoubleOrNull() ?: 0.0,
                                            prepaidMonths = prepaidMonths.toIntOrNull() ?: 0,
                                            fixedAssets = fixedAssetList,
                                            openingItems = if (setupMode == GrocerySetupMode.EXISTING_GROCERY) openingItems else emptyList(),
                                            openingCustomers = if (setupMode == GrocerySetupMode.EXISTING_GROCERY) customers else emptyList(),
                                            openingSuppliers = if (setupMode == GrocerySetupMode.EXISTING_GROCERY) suppliers else emptyList(),
                                            onCompleted = onFinish
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
}

// --- Component 1: Entry Point Welcome Screen ---
@Composable
private fun StepWelcomeScreen(onStartClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(90.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Storefront,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "مرحباً بك في نظام دكاني الذكي",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Text(
                text = "منظومة محاسبية متكاملة لنقاط البيع وإدارة البقالات والتموينات بأعلى دقة",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Highlight Cards
            FeatureHighlightCard(
                title = "مسار تأسيس بقالة جديدة",
                desc = "تجهيز تلقائي للعملة، رأس المال، عقارات وإيجار المحل، والأصول الثابتة.",
                icon = Icons.Default.AddBusiness
            )

            Spacer(modifier = Modifier.height(12.dp))

            FeatureHighlightCard(
                title = "مسار ترحيل بقالة قائمة",
                desc = "ترحيل مرن للدفاتر الورقية، بضاعة أول المدة، ديون العملاء، ومستحقات الموردين.",
                icon = Icons.Default.MenuBook
            )

            Spacer(modifier = Modifier.height(12.dp))

            FeatureHighlightCard(
                title = "رقابة مالية وسياسات تقييم معتمدة",
                desc = "دعم كامل للمتوسط المرجح WAC، الوارد أولاً FIFO، والوارد أخيراً LIFO مع أمان الكاشير.",
                icon = Icons.Default.Assessment
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onStartClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("ابدأ إعداد متجرك الآن", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun FeatureHighlightCard(title: String, desc: String, icon: ImageVector) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(text = desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp)
            }
        }
    }
}

// --- Component 2: Path Selection Screen ---
@Composable
private fun StepPathSelection(
    selectedMode: GrocerySetupMode,
    onSelectMode: (GrocerySetupMode) -> Unit,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "اختر سيناريو نشاطك للبدء",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "يحدد هذا الخيار مسار خطوات التهيئة والملفات المحاسبية المطلوبة:",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Branch A: New Grocery
            SetupModeCard(
                title = "[أ] بقالة جديدة كلياً (New Business Setup)",
                description = "إعداد شامل متسلسل لبيئة المتجر: العملة الأساسية، رأس المال الافتتاحي، عقارات وإيجارات المحل، الأصول الثابتة والديكور، وأمان المستخدمين.",
                icon = Icons.Default.Storefront,
                isSelected = selectedMode == GrocerySetupMode.NEW_GROCERY,
                badge = "موصى به للمتاجر الحديثة",
                onClick = { onSelectMode(GrocerySetupMode.NEW_GROCERY) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Branch B: Existing Grocery Migration
            SetupModeCard(
                title = "[ب] بقالة قائمة / نقل الدفاتر والجرد (Migration)",
                description = "ترحيل مرن وخطوة بخطوة: نقدية الدرج والبنوك، الجرد الافتتاحي للبضائع، ديون الزبائن الورقية، ومستحقات الموردين دون إلزام تقييدي.",
                icon = Icons.Default.MenuBook,
                isSelected = selectedMode == GrocerySetupMode.EXISTING_GROCERY,
                badge = "ترحيل الدفاتر والأرصدة القائمة",
                onClick = { onSelectMode(GrocerySetupMode.EXISTING_GROCERY) }
            )
        }

        Button(
            onClick = onContinue,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .padding(top = 16.dp)
        ) {
            Text("المتابعة لإعداد الخطوات", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun SetupModeCard(
    title: String,
    description: String,
    icon: ImageVector,
    isSelected: Boolean,
    badge: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = badge,
                        fontSize = 11.sp,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                RadioButton(
                    selected = isSelected,
                    onClick = onClick,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = MaterialTheme.colorScheme.primary,
                        unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }
    }
}

// --- Step: Currency, Capital & Inventory Valuation ---
@Composable
private fun StepCurrencyCapitalValuation(
    currencies: List<CurrencyEntity>,
    selectedCurrency: CurrencyEntity?,
    onSelectCurrency: (CurrencyEntity) -> Unit,
    capitalText: String,
    onCapitalChange: (String) -> Unit,
    valuationMethod: CostValuationMethod,
    onValuationMethodChange: (CostValuationMethod) -> Unit,
    currencySymbol: String,
    isNewGrocery: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = "العملة الأساسية ورأس المال وسياسة المخزون",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "تحديد العملة المعتمدة في القوائم ورأس المال وسياسة تقييم التكلفة:",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Currency Selector Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(text = "1. اختيار العملة الأساسية للنظام:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(currencies) { curr ->
                        val isSel = selectedCurrency?.id == curr.id || (selectedCurrency == null && curr.isBaseCurrency)
                        FilterChip(
                            selected = isSel,
                            onClick = { onSelectCurrency(curr) },
                            label = { Text("${curr.name} (${curr.symbol})") },
                            leadingIcon = if (isSel) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Initial Capital / Book Capital Input
        OutlinedTextField(
            value = capitalText,
            onValueChange = onCapitalChange,
            label = { Text(if (isNewGrocery) "رأس المال الافتتاحي للنقدية والخزينة" else "رأس المال الدفتري / القائم الفعلي") },
            suffix = { Text(currencySymbol) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "2. سياسة تقييم المخزون والتكلفة المحاسبية:",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Valuation Method 1: WAC
        ValuationPolicyCard(
            title = "المتوسط المرجح (Weighted Average - WAC) [الافتراضي]",
            note = "يقوم باحتساب متوسط تكلفة الشراء المرجحة للوحدات المتبقية تلقائياً عند كل توريد. الموصى به للبقالات والسوبرماركت لكثرة الأصناف وتذبذب الأسعار.",
            isSelected = valuationMethod == CostValuationMethod.WAC,
            onClick = { onValuationMethodChange(CostValuationMethod.WAC) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Valuation Method 2: FIFO
        ValuationPolicyCard(
            title = "الوارد أولاً يصدر أولاً (FIFO - First In, First Out)",
            note = "يفترض بيع وتقييم تكلفة البضاعة القديمة أولاً. ممتاز للمنتجات ذات تاريخ الصلاحية للحد من الهدر والتلف.",
            isSelected = valuationMethod == CostValuationMethod.FIFO,
            onClick = { onValuationMethodChange(CostValuationMethod.FIFO) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Valuation Method 3: LIFO
        ValuationPolicyCard(
            title = "الوارد أخيراً يصدر أولاً (LIFO - Last In, First Out)",
            note = "يفترض بيع وتقييم تكلفة أحدث شحنات الشراء أولاً. يُستخدم لتغطية الارتفاع السريع في أسعار السوق والتضخم.",
            isSelected = valuationMethod == CostValuationMethod.LIFO,
            onClick = { onValuationMethodChange(CostValuationMethod.LIFO) }
        )
    }
}

@Composable
private fun ValuationPolicyCard(
    title: String,
    note: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary,
                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = note, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp)
            }
        }
    }
}

// --- Step: Property & Rent ---
@Composable
private fun StepPropertyAndRent(
    propertyStatus: PropertyStatus,
    onPropertyStatusChange: (PropertyStatus) -> Unit,
    monthlyRent: String,
    onMonthlyRentChange: (String) -> Unit,
    prepaidMonths: String,
    onPrepaidMonthsChange: (String) -> Unit,
    contractStartDate: String,
    onContractStartDateChange: (String) -> Unit,
    currencySymbol: String
) {
    val mRent = monthlyRent.toDoubleOrNull() ?: 0.0
    val pMonths = prepaidMonths.toIntOrNull() ?: 0
    val prepaidTotal = mRent * pMonths

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = "أصول العقار وإيجار المتجر",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "تحديد حالة ملكية العقار وتوثيق مصروف الإيجار المدفوع مقدماً:",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PropertyTypeChip(
                label = "ملكية العقار (مملوك)",
                isSelected = propertyStatus == PropertyStatus.OWNED,
                onClick = { onPropertyStatusChange(PropertyStatus.OWNED) },
                modifier = Modifier.weight(1f)
            )
            PropertyTypeChip(
                label = "عقار مستأجر",
                isSelected = propertyStatus == PropertyStatus.RENTED,
                onClick = { onPropertyStatusChange(PropertyStatus.RENTED) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (propertyStatus == PropertyStatus.OWNED) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.HomeWork, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "العقار ملك للمتجر. لن يتم قيد مصروفات إيجار مقدمة أو التزامات عقارية في الشجرة المحاسبية.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        } else {
            OutlinedTextField(
                value = monthlyRent,
                onValueChange = onMonthlyRentChange,
                label = { Text("قيمة الإيجار الشهري") },
                suffix = { Text(currencySymbol) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = prepaidMonths,
                onValueChange = onPrepaidMonthsChange,
                label = { Text("عدد الأشهر المدفوعة مقدماً") },
                suffix = { Text("شهر") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = contractStartDate,
                onValueChange = onContractStartDateChange,
                label = { Text("تاريخ بداية العقد") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Calculated Prepaid Total Card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "إجمالي الإيجار المدفوع مقدماً:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(
                            text = "${"%.2f".format(prepaidTotal)} $currencySymbol",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Icon(Icons.Default.Calculate, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Accounting Warning Alert Box
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "تنبيه محاسبي: سيتم تسجيل مبلغ الإيجار المدفوع مقدماً كـ [مصروف إيجار مقدم] ضمن الأصول المتداولة، وإطفاء قيمته شهرياً ضمن قائمة الدخل.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun PropertyTypeChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// --- Step: Cash Drawer & Bank (for Branch B) ---
@Composable
private fun StepCashDrawerAndBank(
    openingCash: String,
    onOpeningCashChange: (String) -> Unit,
    bankBalance: String,
    onBankBalanceChange: (String) -> Unit,
    propertyStatus: PropertyStatus,
    onPropertyStatusChange: (PropertyStatus) -> Unit,
    monthlyRent: String,
    onMonthlyRentChange: (String) -> Unit,
    prepaidMonths: String,
    onPrepaidMonthsChange: (String) -> Unit,
    currencySymbol: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = "نقدية الدرج والبنوك والعقارات القائمة",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "إدخال أرصدة الصندوق النقدي والحسابات البنكية وحالة العقار:",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = openingCash,
            onValueChange = onOpeningCashChange,
            label = { Text("رصيد درج النقدية عند الافتتاح") },
            suffix = { Text(currencySymbol) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = bankBalance,
            onValueChange = onBankBalanceChange,
            label = { Text("رصيد الحسابات البنكية القائمة") },
            suffix = { Text(currencySymbol) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "حالة عقار المحل:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PropertyTypeChip(
                label = "ملكية العقار",
                isSelected = propertyStatus == PropertyStatus.OWNED,
                onClick = { onPropertyStatusChange(PropertyStatus.OWNED) },
                modifier = Modifier.weight(1f)
            )
            PropertyTypeChip(
                label = "عقار مستأجر",
                isSelected = propertyStatus == PropertyStatus.RENTED,
                onClick = { onPropertyStatusChange(PropertyStatus.RENTED) },
                modifier = Modifier.weight(1f)
            )
        }

        if (propertyStatus == PropertyStatus.RENTED) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = monthlyRent,
                onValueChange = onMonthlyRentChange,
                label = { Text("قيمة الإيجار الشهري") },
                suffix = { Text(currencySymbol) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = prepaidMonths,
                onValueChange = onPrepaidMonthsChange,
                label = { Text("عدد الأشهر المدفوعة مقدماً") },
                suffix = { Text("شهر") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )
        }
    }
}

// --- Step: Fixed Assets & Equipment ---
@Composable
private fun StepFixedAssetsAndEquipment(
    refrigCost: String,
    onRefrigChange: (String) -> Unit,
    shelvesCost: String,
    onShelvesChange: (String) -> Unit,
    posDevicesCost: String,
    onPosDevicesChange: (String) -> Unit,
    acLightingCost: String,
    onAcLightingChange: (String) -> Unit,
    otherAssetsCost: String,
    onOtherAssetsChange: (String) -> Unit,
    currencySymbol: String,
    isOptional: Boolean
) {
    val rVal = refrigCost.toDoubleOrNull() ?: 0.0
    val sVal = shelvesCost.toDoubleOrNull() ?: 0.0
    val pVal = posDevicesCost.toDoubleOrNull() ?: 0.0
    val aVal = acLightingCost.toDoubleOrNull() ?: 0.0
    val oVal = otherAssetsCost.toDoubleOrNull() ?: 0.0
    val totalAssets = rVal + sVal + pVal + aVal + oVal

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "الأصول الثابتة والديكور والتجهيزات",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (isOptional) "خطوة اختيارية - يمكنك تسليم وتوثيق أصولك أو تخطيها والمتابعة:" else "توثيق قيم الأصول الثابتة لتحديد رأس المال كلياً:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Total Assets Live Summary Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "إجمالي قيمة الأصول الثابتة والتجهيزات:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(
                        text = "${"%.2f".format(totalAssets)} $currencySymbol",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Icon(Icons.Default.AccountBalance, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        AssetCategoryInput(label = "ثلاجات ومعدات تبريد", value = refrigCost, onValueChange = onRefrigChange, currencySymbol = currencySymbol, icon = Icons.Default.AcUnit)
        Spacer(modifier = Modifier.height(10.dp))
        AssetCategoryInput(label = "أرفف وديكور المحل", value = shelvesCost, onValueChange = onShelvesChange, currencySymbol = currencySymbol, icon = Icons.Default.ViewWeek)
        Spacer(modifier = Modifier.height(10.dp))
        AssetCategoryInput(label = "أجهزة نقاط البيع والباربود", value = posDevicesCost, onValueChange = onPosDevicesChange, currencySymbol = currencySymbol, icon = Icons.Default.PointOfSale)
        Spacer(modifier = Modifier.height(10.dp))
        AssetCategoryInput(label = "أجهزة تكييف وإضاءة ومولدات", value = acLightingCost, onValueChange = onAcLightingChange, currencySymbol = currencySymbol, icon = Icons.Default.Lightbulb)
        Spacer(modifier = Modifier.height(10.dp))
        AssetCategoryInput(label = "أصول وتجهيزات أخرى", value = otherAssetsCost, onValueChange = onOtherAssetsChange, currencySymbol = currencySymbol, icon = Icons.Default.Category)
    }
}

@Composable
private fun AssetCategoryInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    currencySymbol: String,
    icon: ImageVector
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        suffix = { Text(currencySymbol) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp)
    )
}

// --- Step: Flexible Opening Balances (for Branch B) ---
@Composable
private fun StepFlexibleOpeningBalances(
    currencySymbol: String,
    openingItems: List<OpeningBalanceItem>,
    newProdName: String,
    onNewProdNameChange: (String) -> Unit,
    newProdCategory: String,
    onNewProdCategoryChange: (String) -> Unit,
    newProdBarcode: String,
    onNewProdBarcodeChange: (String) -> Unit,
    newProdUnit: String,
    onNewProdUnitChange: (String) -> Unit,
    newProdQty: String,
    onNewProdQtyChange: (String) -> Unit,
    newProdCost: String,
    onNewProdCostChange: (String) -> Unit,
    newProdPrice: String,
    onNewProdPriceChange: (String) -> Unit,
    onAddItem: () -> Unit,
    onRemoveItem: (OpeningBalanceItem) -> Unit,
    customers: List<OpeningBalanceCustomer>,
    newCustName: String,
    onCustNameChange: (String) -> Unit,
    newCustPhone: String,
    onCustPhoneChange: (String) -> Unit,
    newCustBalance: String,
    onCustBalanceChange: (String) -> Unit,
    onAddCustomer: () -> Unit,
    onRemoveCustomer: (OpeningBalanceCustomer) -> Unit,
    suppliers: List<OpeningBalanceSupplier>,
    newSuppName: String,
    onSuppNameChange: (String) -> Unit,
    newSuppPhone: String,
    onSuppPhoneChange: (String) -> Unit,
    newSuppBalance: String,
    onSuppBalanceChange: (String) -> Unit,
    onAddSupplier: () -> Unit,
    onRemoveSupplier: (OpeningBalanceSupplier) -> Unit
) {
    var subTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = "الجرد الافتتاحي والديون والمستحقات",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Flexible Non-blocking Info Banner
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ℹ️ إدخال هذه البيانات اختياري ومرن بالكامل. يمكنك المتابعة فوراً واستكمال الدفاتر لاحقاً من شاشات المبيعات والمشتريات.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    lineHeight = 15.sp
                )
            }
        }

        TabRow(
            selectedTabIndex = subTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(selected = subTab == 0, onClick = { subTab = 0 }) {
                Text("بضاعة أول المدة (${openingItems.size})", fontSize = 12.sp, modifier = Modifier.padding(10.dp))
            }
            Tab(selected = subTab == 1, onClick = { subTab = 1 }) {
                Text("ديون العملاء (${customers.size})", fontSize = 12.sp, modifier = Modifier.padding(10.dp))
            }
            Tab(selected = subTab == 2, onClick = { subTab = 2 }) {
                Text("الموردين (${suppliers.size})", fontSize = 12.sp, modifier = Modifier.padding(10.dp))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Box(modifier = Modifier.weight(1f)) {
            when (subTab) {
                0 -> Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(value = newProdName, onValueChange = onNewProdNameChange, label = { Text("اسم الصنف") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = newProdQty, onValueChange = onNewProdQtyChange, label = { Text("الكمية") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(0.6f))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(value = newProdCost, onValueChange = onNewProdCostChange, label = { Text("التكلفة") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                        OutlinedTextField(value = newProdPrice, onValueChange = onNewProdPriceChange, label = { Text("سعر البيع") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onAddItem,
                        enabled = newProdName.isNotBlank() && (newProdQty.toDoubleOrNull() ?: 0.0) > 0,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text("إضافة الصنف للجرد")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    openingItems.forEach { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(item.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Text("كمية: ${item.quantity} - تكلفة: ${item.costPrice} $currencySymbol", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { onRemoveItem(item) }) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }

                1 -> Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(value = newCustName, onValueChange = onCustNameChange, label = { Text("اسم الزبون") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = newCustPhone, onValueChange = onCustPhoneChange, label = { Text("الهاتف") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = newCustBalance, onValueChange = onCustBalanceChange, label = { Text("الرصيد الافتتاحي") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onAddCustomer,
                        enabled = newCustName.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null)
                        Text("إضافة حلقة الدين")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    customers.forEach { c ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text(c.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Text("دين قائم: ${c.openingBalance} $currencySymbol", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                                }
                                IconButton(onClick = { onRemoveCustomer(c) }) { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                            }
                        }
                    }
                }

                2 -> Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(value = newSuppName, onValueChange = onSuppNameChange, label = { Text("اسم المورد / الشركة") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = newSuppPhone, onValueChange = onSuppPhoneChange, label = { Text("الهاتف") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = newSuppBalance, onValueChange = onSuppBalanceChange, label = { Text("المستحق له") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onAddSupplier,
                        enabled = newSuppName.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = null)
                        Text("إضافة المورد")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    suppliers.forEach { s ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text(s.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Text("مستحق له: ${s.openingPayable} $currencySymbol", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { onRemoveSupplier(s) }) { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Step: Security & Users ---
@Composable
private fun StepSecurityAndUsers(
    storeName: String,
    onStoreNameChange: (String) -> Unit,
    adminPin: String,
    onAdminPinChange: (String) -> Unit,
    cashierName: String,
    onCashierNameChange: (String) -> Unit,
    cashierPin: String,
    onCashierPinChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = "اسم المتجر وأمان الكاشير والمدير",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "تحديد الهوية والرموز السرية للدخول ومطابقة الشفتات:",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = storeName,
            onValueChange = onStoreNameChange,
            label = { Text("اسم البقالة / المتجر") },
            leadingIcon = { Icon(Icons.Default.Store, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = adminPin,
            onValueChange = onAdminPinChange,
            label = { Text("رمز PIN لمدير النظام (Admin)") },
            leadingIcon = { Icon(Icons.Default.Security, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = cashierName,
            onValueChange = onCashierNameChange,
            label = { Text("اسم الكاشير الافتراضي") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = cashierPin,
            onValueChange = onCashierPinChange,
            label = { Text("رمز PIN الافتراضي للكاشير") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Default Cashier PIN Warning Box
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.errorContainer,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "⚠️ تنبيه أمني هاما: رمز PIN الافتراضي للكاشير هو (1234). يُنصح بتغييره فوراً لمنع أي تلاعب بالشفتات المالية أو الدخول غير المصرح به.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// --- Component: Final Completion Screen ---
@Composable
private fun StepCompletionScreen(
    setupMode: GrocerySetupMode,
    storeName: String,
    currencyName: String,
    currencySymbol: String,
    initialCapital: String,
    openingCash: String,
    valuationMethod: CostValuationMethod,
    propertyStatus: PropertyStatus,
    monthlyRent: String,
    prepaidMonths: String,
    isSubmitting: Boolean,
    onLaunchSystem: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(50.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "جاهز للانطلاق!",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "تمت مراجعة وتوثيق جميع معايير التهيئة الافتتاحية بنجاح:",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Summary Card
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SummaryRow(label = "اسم المتجر:", value = storeName)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    SummaryRow(label = "العملة الأساسية:", value = "$currencyName ($currencySymbol)")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    SummaryRow(
                        label = if (setupMode == GrocerySetupMode.NEW_GROCERY) "رأس المال الافتتاحي:" else "نقدية الدرج الافتتاحية:",
                        value = if (setupMode == GrocerySetupMode.NEW_GROCERY) "$initialCapital $currencySymbol" else "$openingCash $currencySymbol"
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    SummaryRow(
                        label = "سياسة تقييم التكلفة:",
                        value = when (valuationMethod) {
                            CostValuationMethod.WAC -> "المتوسط المرجح (WAC)"
                            CostValuationMethod.FIFO -> "الوارد أولاً يصدر أولاً (FIFO)"
                            CostValuationMethod.LIFO -> "الوارد أخيراً يصدر أولاً (LIFO)"
                            else -> "المتوسط المرجح (WAC)"
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    SummaryRow(
                        label = "حالة العقار والإيجار:",
                        value = if (propertyStatus == PropertyStatus.OWNED) "مملوك للمتجر" else "مستأجر ($prepaidMonths أشهر مدفوعة)"
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onLaunchSystem,
            enabled = !isSubmitting,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("جارِ حفظ الإعدادات وتأهيل النظام...")
            } else {
                Icon(Icons.Default.RocketLaunch, contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (setupMode == GrocerySetupMode.NEW_GROCERY) "انطلق إلى النظام للبدء" else "انطلق إلى النظام لاستكمال الإدارة والبيع",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun WizardStepIndicator(
    totalSteps: Int,
    currentStep: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until totalSteps) {
            val isCompleted = i < currentStep
            val isCurrent = i == currentStep

            val bgColor = when {
                isCompleted -> MaterialTheme.colorScheme.primary
                isCurrent -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.surfaceVariant
            }

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Text(
                        text = "${i + 1}",
                        color = if (isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            if (i < totalSteps - 1) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(3.dp)
                        .padding(horizontal = 4.dp)
                        .background(if (i < currentStep) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                )
            }
        }
    }
}
