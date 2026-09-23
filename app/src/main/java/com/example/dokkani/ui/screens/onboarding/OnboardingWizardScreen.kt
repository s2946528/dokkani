package com.example.dokkani.ui.screens.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.dokkani.ui.components.BarcodeTextField
import com.example.dokkani.data.local.entities.CostValuationMethod
import com.example.dokkani.data.local.entities.CurrencyEntity
import com.example.dokkani.data.local.entities.ProductUnitEntity
import com.example.dokkani.ui.screens.crud.AddEditCurrencyDialog
import com.example.dokkani.ui.screens.crud.AddEditUnitDialog
import com.example.dokkani.ui.screens.crud.CurrencyDropdownSelector
import com.example.dokkani.ui.screens.crud.UnitDropdownSelector
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

data class DuplicateItemDialogData(
    val existingIndex: Int,
    val existingItem: OpeningBalanceItem,
    val newItem: OpeningBalanceItem,
    val existingQty: Double,
    val newQty: Double,
    val totalQty: Double
)

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

        val activeCurrencySymbol = uiState.baseCurrency?.symbol ?: selectedBaseCurrency?.symbol ?: "ر.ي"
        val activeCurrencyName = uiState.baseCurrency?.name ?: selectedBaseCurrency?.name ?: "الريال اليمني"

        // بيانات التهيئة المحاسبية والبيئة العامة
        var storeName by remember { mutableStateOf("تموينات ومخضار السعادة") }
        var adminPin by remember { mutableStateOf("1234") }
        var cashierName by remember { mutableStateOf("كاشير 1") }
        var cashierPin by remember { mutableStateOf("1234") }

        // رأس المال والنقدية
        var initialCapital by remember { mutableStateOf("15000") }
        var openingCash by remember { mutableStateOf("500") }
        var bankBalance by remember { mutableStateOf("0") }
        var valuationMethod by remember { mutableStateOf(CostValuationMethod.WAC) }

        // العقارات والإيجار ونقل قدم (خلو)
        var propertyStatus by remember { mutableStateOf(PropertyStatus.OWNED) }
        var monthlyRent by remember { mutableStateOf("1000") }
        var prepaidMonths by remember { mutableStateOf("6") }
        var contractStartDate by remember { mutableStateOf("2026-01-01") }
        var leaseholdAmount by remember { mutableStateOf("") }
        var leaseholdYears by remember { mutableStateOf("5") }
        var leaseholdNotes by remember { mutableStateOf("") }

        // الأصول الثابتة والديكور
        var refrigCost by remember { mutableStateOf("4500") }
        var shelvesCost by remember { mutableStateOf("2500") }
        var posDevicesCost by remember { mutableStateOf("1800") }
        var acLightingCost by remember { mutableStateOf("2000") }
        var otherAssetsCost by remember { mutableStateOf("0") }

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
        var duplicateDialogData by remember { mutableStateOf<DuplicateItemDialogData?>(null) }

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
                                    WizardPhase.PATH_SELECTION -> "اختر نوع النشاط المحاسبي"
                                    WizardPhase.STEPS -> if (setupMode == GrocerySetupMode.NEW_GROCERY) "إعداد مشروع جديد [أ]" else "ترحيل مشروع قائم [ب]"
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
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
                                            onSaveCurrency = { newCurrency ->
                                                viewModel.saveCurrency(newCurrency)
                                                selectedBaseCurrency = newCurrency
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
                                            leaseholdAmount = leaseholdAmount,
                                            onLeaseholdAmountChange = { leaseholdAmount = it },
                                            leaseholdYears = leaseholdYears,
                                            onLeaseholdYearsChange = { leaseholdYears = it },
                                            leaseholdNotes = leaseholdNotes,
                                            onLeaseholdNotesChange = { leaseholdNotes = it },
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
                                            onSaveCurrency = { newCurrency ->
                                                viewModel.saveCurrency(newCurrency)
                                                selectedBaseCurrency = newCurrency
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
                                            leaseholdAmount = leaseholdAmount,
                                            onLeaseholdAmountChange = { leaseholdAmount = it },
                                            leaseholdYears = leaseholdYears,
                                            onLeaseholdYearsChange = { leaseholdYears = it },
                                            leaseholdNotes = leaseholdNotes,
                                            onLeaseholdNotesChange = { leaseholdNotes = it },
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
                                            currencies = uiState.currencies,
                                            onSaveCurrency = { currency -> viewModel.saveCurrency(currency) },
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
                                                val barcode = newProdBarcode.trim()
                                                val name = newProdName.trim()
                                                val qty = newProdQty.toDoubleOrNull() ?: 0.0
                                                val cost = newProdCost.toDoubleOrNull() ?: 0.0
                                                val price = newProdPrice.toDoubleOrNull() ?: cost
                                                val unit = newProdUnit.trim().ifBlank { "حبة/قطعة" }
                                                val category = newProdCategory.trim().ifBlank { "تموينات عامة" }

                                                if ((name.isNotBlank() || barcode.isNotBlank()) && qty > 0.0) {
                                                    val newItem = OpeningBalanceItem(
                                                        name = name.ifBlank { "صنف $barcode" },
                                                        category = category,
                                                        quantity = qty,
                                                        costPrice = cost,
                                                        sellingPrice = price,
                                                        barcode = barcode,
                                                        unitName = unit
                                                    )

                                                    // التحقق الذكي من التكرار عبر الباركود أولاً ثم الاسم
                                                    val existingIndex = openingItems.indexOfFirst { existing ->
                                                        (barcode.isNotBlank() && existing.barcode.isNotBlank() && existing.barcode == barcode) ||
                                                        (name.isNotBlank() && existing.name.trim().equals(name, ignoreCase = true))
                                                    }

                                                    if (existingIndex != -1) {
                                                        val existingItem = openingItems[existingIndex]
                                                        val existingQty = existingItem.quantity
                                                        val totalQty = existingQty + qty

                                                        duplicateDialogData = DuplicateItemDialogData(
                                                            existingIndex = existingIndex,
                                                            existingItem = existingItem,
                                                            newItem = newItem,
                                                            existingQty = existingQty,
                                                            newQty = qty,
                                                            totalQty = totalQty
                                                        )
                                                    } else {
                                                        openingItems = openingItems + newItem
                                                        newProdName = ""
                                                        newProdBarcode = ""
                                                        newProdUnit = "حبة/قطعة"
                                                        newProdQty = ""
                                                        newProdCost = ""
                                                        newProdPrice = ""
                                                    }
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
                                            onRemoveSupplier = { s -> suppliers = suppliers.filter { it != s } },
                                            onSaveUnit = { unit -> viewModel.saveProductUnit(unit) },
                                            availableUnitsList = (uiState.allUnits.map { it.unitName } + uiState.products.flatMap { p -> p.units.map { u -> u.unitName } } + listOf("حبة/قطعة", "حبة", "كرتون", "كيلو", "درزن", "صندوق", "سحارة", "ربطة", "عبوة", "باكيت", "طرد", "جرام", "لتر", "متر", "شوال")).filter { it.isNotBlank() }.distinct()
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
                                leaseholdAmount = leaseholdAmount,
                                leaseholdYears = leaseholdYears,
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
                                            leaseholdAmount = leaseholdAmount.toDoubleOrNull() ?: 0.0,
                                            leaseholdYears = leaseholdYears.toIntOrNull() ?: 5,
                                            leaseholdNotes = leaseholdNotes,
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

            // --- نافذة التنبيه والدمج الذكي للصنف المكرر (Confirmation Dialog) ---
            duplicateDialogData?.let { dialogData ->
                val existingQtyStr = if (dialogData.existingQty % 1.0 == 0.0) dialogData.existingQty.toInt().toString() else "%.2f".format(dialogData.existingQty)
                val newQtyStr = if (dialogData.newQty % 1.0 == 0.0) dialogData.newQty.toInt().toString() else "%.2f".format(dialogData.newQty)
                val totalQtyStr = if (dialogData.totalQty % 1.0 == 0.0) dialogData.totalQty.toInt().toString() else "%.2f".format(dialogData.totalQty)

                AlertDialog(
                    onDismissRequest = { duplicateDialogData = null },
                    icon = {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.MergeType,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                    },
                    title = {
                        Text(
                            text = "تأكيد دمج الكمية للصنف المكرر",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                    },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = dialogData.existingItem.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    if (dialogData.existingItem.barcode.isNotBlank()) {
                                        Text(
                                            text = "رمز الباركود: ${dialogData.existingItem.barcode}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            Text(
                                text = "هذا الصنف تم إدخاله مسبقاً بكمية ($existingQtyStr). هل تريد دمج الكمية الجديدة ($newQtyStr) لتصبح الإجمالي ($totalQtyStr ${dialogData.existingItem.unitName})؟",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 19.sp,
                                textAlign = TextAlign.Start
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val updatedList = openingItems.toMutableList()
                                val prevItem = updatedList[dialogData.existingIndex]
                                val mergedItem = prevItem.copy(
                                    quantity = dialogData.totalQty,
                                    costPrice = if (dialogData.newItem.costPrice > 0) dialogData.newItem.costPrice else prevItem.costPrice,
                                    sellingPrice = if (dialogData.newItem.sellingPrice > 0) dialogData.newItem.sellingPrice else prevItem.sellingPrice
                                )
                                updatedList[dialogData.existingIndex] = mergedItem
                                openingItems = updatedList

                                newProdName = ""
                                newProdBarcode = ""
                                newProdUnit = "حبة/قطعة"
                                newProdQty = ""
                                newProdCost = ""
                                newProdPrice = ""
                                duplicateDialogData = null
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("نعم", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    },
                    dismissButton = {
                        OutlinedButton(
                            onClick = { duplicateDialogData = null },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("لا", fontSize = 14.sp)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
    }
}
}



// --- Component 1: Entry Point Welcome Screen with 5-Page Educational Pager ---
private data class OnboardingSlideData(
    val badge: String,
    val title: String,
    val description: String,
    val imageRes: Int,
    val isModuleGrid: Boolean = false,
    val modules: List<Pair<String, ImageVector>> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StepWelcomeScreen(onStartClick: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val slides = remember {
        listOf(
            OnboardingSlideData(
                badge = "توعية مالية",
                title = "من أكل وما حسب.. فقر وما دري",
                description = "إدارة أموالك بدون حساب دقيق تؤدي لتآكل أرباحك بصمت. التخطيط والرقابة المالية اليومية هما أساس نمو واستقرار تجارتك.",
                imageRes = R.drawable.img_onboarding_1_loss
            ),
            OnboardingSlideData(
                badge = "واقع الدفاتر الورقية",
                title = "كفاية خسائر الدفاتر",
                description = "وداعاً لأخطاء الورق والنسيان! الأخطاء الحسابية الخفية تكلفك أرباحك كل يوم، ومعرفة أرباحك الحقيقية أصبحت مهمة متعبة ومكلفة.",
                imageRes = R.drawable.img_onboarding_2_notebook
            ),
            OnboardingSlideData(
                badge = "توفير المرونة والتكاليف",
                title = "لا تكاليف باهظة",
                description = "وداعاً لأجهزة الكمبيوتر وقارئات الباركود المكلفة! النظام المحاسبي الاحترافي لم يعد مخصصاً للشركات الكبرى فقط، بل في متناول يديك.",
                imageRes = R.drawable.img_onboarding_3_pc
            ),
            OnboardingSlideData(
                badge = "منظومة إدارية متكاملة",
                title = "نظام إداري متكامل بين يديك",
                description = "منظومة شاملة تدير كافة أقسام وأنشطة مشروعك بكفاءة عالية واحترافية متناهية:",
                imageRes = R.drawable.img_onboarding_4_modules,
                isModuleGrid = true,
                modules = listOf(
                    "إدارة المخزون والأصناف ونقاط البيع" to Icons.Default.Inventory,
                    "إدارة المشتريات والموردين" to Icons.Default.ShoppingBag,
                    "شؤون الموظفين والعمال وأجورهم" to Icons.Default.People,
                    "الأستاذ العام ودليل الحسابات" to Icons.Default.AccountTree,
                    "إدارة المستخدمين وتحديد الصلاحيات" to Icons.Default.Security,
                    "تقارير دقيقة للأرباح والخسائر" to Icons.Default.Analytics
                )
            ),
            OnboardingSlideData(
                badge = "الحل المحاسبي الذكي",
                title = "مشروعك في جيبك",
                description = "هاتفك الذكي هو نظامك الكامل الآن! كاميرا هاتفك تقرأ الباركود، وحساباتك وأرباحك تظهر أمامك بلحظتها. ابدأ الآن وتولى إدارة متجرك بكل ثقة!",
                imageRes = R.drawable.img_onboarding_5_success
            )
        )
    }

    val pagerState = rememberPagerState(pageCount = { slides.size })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // --- الشريط العلوي (Top Control Bar) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF15803D)),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_dokkani_official_logo),
                            contentDescription = "شعار دكاني",
                            modifier = Modifier
                                .size(32.dp)
                                .padding(2.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
                Text(
                    text = "دكاني | نقاط البيع",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // مؤشر رقم الصفحة
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = "${pagerState.currentPage + 1} من ${slides.size}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                // زر "تخطي"
                if (pagerState.currentPage < slides.size - 1) {
                    TextButton(
                        onClick = onStartClick,
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text(
                            text = "تخطي",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // --- محتوى Pager ---
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { pageIndex ->
            val slide = slides[pageIndex]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // --- الرسم التوضيحي (Illustration Card) ---
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = slide.imageRes),
                                contentDescription = slide.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // --- شارة التبويب (Badge Chip) ---
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = slide.badge,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // --- العنوان / المثل ---
                    Text(
                        text = slide.title,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        lineHeight = 28.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // --- الوصف ---
                    Text(
                        text = slide.description,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    // --- شبكة أقسام النظام (في الشاشة الرابعة) ---
                    if (slide.isModuleGrid) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            slide.modules.chunked(2).forEach { rowModules ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    rowModules.forEach { (moduleTitle, moduleIcon) ->
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                            ),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = MaterialTheme.colorScheme.primaryContainer,
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = moduleIcon,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = moduleTitle,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    lineHeight = 15.sp,
                                                    modifier = Modifier.weight(1f)
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

        // --- الشريط السفلي لعلامات الترقيم والأزرار (Bottom Control Bar) ---
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // مؤشرات النقاط الديناميكية (Dynamic Dots Indicator)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 14.dp)
                ) {
                    repeat(slides.size) { index ->
                        val isSelected = pagerState.currentPage == index
                        val width by animateDpAsState(
                            targetValue = if (isSelected) 28.dp else 8.dp,
                            label = "dotWidth"
                        )
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(width)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant
                                )
                        )
                    }
                }

                // الأزرار السفليّة
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (pagerState.currentPage > 0) {
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(0.4f)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("السابق", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (pagerState.currentPage < slides.size - 1) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.weight(if (pagerState.currentPage > 0) 0.6f else 1f)
                        ) {
                            Text("التالي", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    } else {
                        // الشاشة الأخيرة: زر "ابدأ الآن"
                        Button(
                            onClick = onStartClick,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Icon(Icons.Default.RocketLaunch, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("ابدأ الآن وإعداد المتجر", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
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
            .padding(vertical = 12.dp)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "اختر نوع نشاطك للبدء",
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

            // Branch A: New Project / Business
            SetupModeCard(
                title = "[أ] مشروع جديد كلياً (New Business Setup)",
                description = "إعداد شامل متسلسل لبيئة النشاط التجاري: العملة الأساسية، رأس المال الافتتاحي، عقارات وإيجارات المحل أو المقر، الأصول الثابتة والتجهيزات، وأمان المستخدمين.",
                icon = Icons.Default.Storefront,
                isSelected = selectedMode == GrocerySetupMode.NEW_GROCERY,
                badge = "موصى به للمشاريع والمتاجر الحديثة",
                onClick = { onSelectMode(GrocerySetupMode.NEW_GROCERY) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Branch B: Existing Business Migration
            SetupModeCard(
                title = "[ب] مشروع قائم / نقل الدفاتر والجرد (Migration)",
                description = "ترحيل مرن وخطوة بخطوة: نقدية الدرج والبنوك، الجرد الافتتاحي للبضائع، ديون العملاء والزبائن، ومستحقات الموردين دون إلزام تقييدي.",
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

// --- Step: Currency & Inventory Valuation ---
@Composable
private fun StepCurrencyCapitalValuation(
    currencies: List<CurrencyEntity>,
    selectedCurrency: CurrencyEntity?,
    onSelectCurrency: (CurrencyEntity) -> Unit,
    onSaveCurrency: (CurrencyEntity) -> Unit = {},
    capitalText: String = "",
    onCapitalChange: (String) -> Unit = {},
    valuationMethod: CostValuationMethod,
    onValuationMethodChange: (CostValuationMethod) -> Unit,
    currencySymbol: String = "",
    isNewGrocery: Boolean = true
) {
    var showAddCurrencyDialog by remember { mutableStateOf(false) }

    if (showAddCurrencyDialog) {
        AddEditCurrencyDialog(
            initialCurrency = null,
            onSaveCurrency = { newCurrency ->
                onSaveCurrency(newCurrency)
                onSelectCurrency(newCurrency)
                showAddCurrencyDialog = false
            },
            onDismiss = { showAddCurrencyDialog = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = "العملة الأساسية وسياسة تقييم المخزون",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "تحديد العملة المعتمدة في القوائم والتقارير وسياسة تقييم تكلفة المخزون المحاسبية:",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Currency Selector Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "1. اختيار العملة الأساسية للنظام:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "تُستخدم هذه العملة في قياس رأس المال وإصدار الفواتير والتقارير المالية.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(currencies) { curr ->
                        val isSel = selectedCurrency?.id == curr.id || (selectedCurrency == null && curr.isBaseCurrency)
                        FilterChip(
                            selected = isSel,
                            onClick = { onSelectCurrency(curr) },
                            label = { Text("${curr.name} (${curr.symbol})", fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) },
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
                    item {
                        FilterChip(
                            selected = false,
                            onClick = { showAddCurrencyDialog = true },
                            label = { Text("+ إضافة عملة جديدة", fontWeight = FontWeight.Bold) },
                            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                labelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "2. سياسة تقييم المخزون والتكلفة المحاسبية:",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "تحديد الآلية التي يستند إليها النظام في حساب تكلفة البضاعة المباعة وقيمة البضاعة المتبقية:",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))

        // Valuation Method 1: WAC
        ValuationPolicyCard(
            title = "المتوسط المرجح (Weighted Average - WAC) [الافتراضي والأنسب]",
            note = "يقوم باحتساب متوسط تكلفة الشراء المرجحة للوحدات المتبقية تلقائياً عند كل توريد. الموصى به للمشاريع والمتاجر لكثرة الأصناف وتذبذب الأسعار.",
            isSelected = valuationMethod == CostValuationMethod.WAC,
            onClick = { onValuationMethodChange(CostValuationMethod.WAC) }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Valuation Method 2: FIFO
        ValuationPolicyCard(
            title = "الوارد أولاً يصدر أولاً (FIFO - First In, First Out)",
            note = "يفترض بيع وتقييم تكلفة البضاعة القديمة أولاً. ممتاز للمنتجات ذات تاريخ الصلاحية المحدود للحد من الهدر والتلف.",
            isSelected = valuationMethod == CostValuationMethod.FIFO,
            onClick = { onValuationMethodChange(CostValuationMethod.FIFO) }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Valuation Method 3: LIFO
        ValuationPolicyCard(
            title = "الوارد أخيراً يصدر أولاً (LIFO - Last In, First Out)",
            note = "يفترض بيع وتقييم تكلفة أحدث شحنات الشراء أولاً. يُستخدم لتغطية الارتفاع السريع في أسعار السوق والتضخم.",
            isSelected = valuationMethod == CostValuationMethod.LIFO,
            onClick = { onValuationMethodChange(CostValuationMethod.LIFO) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Accounting Clarification Note
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "ملاحظة محاسبية: يتم تجميع واحتساب إجمالي رأس المال والأرصدة النقدية تلقائياً بناءً على البيانات التي تُدخل في خطوات النقدية والعقارات والأصول.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    lineHeight = 18.sp
                )
            }
        }
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
@OptIn(ExperimentalMaterial3Api::class)
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
    leaseholdAmount: String,
    onLeaseholdAmountChange: (String) -> Unit,
    leaseholdYears: String,
    onLeaseholdYearsChange: (String) -> Unit,
    leaseholdNotes: String,
    onLeaseholdNotesChange: (String) -> Unit,
    currencySymbol: String
) {
    val mRent = monthlyRent.toDoubleOrNull() ?: 0.0
    val pMonths = prepaidMonths.toIntOrNull() ?: 0
    val prepaidTotal = mRent * pMonths

    var showDatePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = "أصول العقار والإيجار ونقل قدم (خلو)",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "تحديد حالة ملكية العقار وتوثيق مصروف الإيجار ونقل قدم (خلو) الموقع التجاري:",
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
            // قيمة الإيجار الشهري (أرقام صحيحة فقط)
            OutlinedTextField(
                value = monthlyRent,
                onValueChange = { input ->
                    val cleanInput = input.filter { it.isDigit() }
                    onMonthlyRentChange(cleanInput)
                },
                label = { Text("قيمة الإيجار الشهري") },
                placeholder = { Text("0") },
                suffix = { Text(currencySymbol) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // عدد الأشهر المدفوعة مقدماً (أرقام صحيحة فقط)
            OutlinedTextField(
                value = prepaidMonths,
                onValueChange = { input ->
                    val cleanInput = input.filter { it.isDigit() }
                    onPrepaidMonthsChange(cleanInput)
                },
                label = { Text("عدد الأشهر المدفوعة مقدماً") },
                placeholder = { Text("0") },
                suffix = { Text("شهر") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // تاريخ بداية العقد بـ DatePicker Dialog
            OutlinedTextField(
                value = contractStartDate,
                onValueChange = {},
                readOnly = true,
                label = { Text("تاريخ بداية العقد") },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "اختر تاريخ العقد",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true },
                shape = RoundedCornerShape(10.dp)
            )

            if (showDatePicker) {
                val datePickerState = rememberDatePickerState()
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val selectedMillis = datePickerState.selectedDateMillis
                                if (selectedMillis != null) {
                                    val calendar = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
                                        timeInMillis = selectedMillis
                                    }
                                    val year = calendar.get(java.util.Calendar.YEAR)
                                    val month = calendar.get(java.util.Calendar.MONTH) + 1
                                    val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
                                    val formattedDate = String.format("%04d-%02d-%02d", year, month, day)
                                    onContractStartDateChange(formattedDate)
                                }
                                showDatePicker = false
                            }
                        ) {
                            Text("موافق")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) {
                            Text("إلغاء")
                        }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }

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

        // --- قسم نقل قدم (خلو) [حق الانتفاع / التنازل] ---
        Spacer(modifier = Modifier.height(20.dp))

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CorporateFare,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "مبلغ نقل قدم (خلو) - [حق الانتفاع بالموقع]",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "مبلغ التنازل أو نقل قدم (خلو) المسدد للحصول على الموقع التجاري (أصل غير ملموس بالدليل المحاسبي):",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = leaseholdAmount,
                    onValueChange = { input ->
                        val cleanInput = input.filter { it.isDigit() }
                        onLeaseholdAmountChange(cleanInput)
                    },
                    label = { Text("مبلغ نقل قدم (خلو)") },
                    placeholder = { Text("0") },
                    suffix = { Text(currencySymbol) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                if ((leaseholdAmount.toDoubleOrNull() ?: 0.0) > 0.0) {
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = leaseholdYears,
                            onValueChange = { input ->
                                val cleanInput = input.filter { it.isDigit() }
                                onLeaseholdYearsChange(cleanInput)
                            },
                            label = { Text("سنوات عقد نقل قدم (خلو)") },
                            suffix = { Text("سنوات") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = leaseholdNotes,
                            onValueChange = onLeaseholdNotesChange,
                            label = { Text("تفاصيل/اسم المتنازل") },
                            singleLine = true,
                            modifier = Modifier.weight(1.5f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "التوجيه المحاسبي: يُقيد كـ [أصل غير ملموس - 10501] تحت الأصول بالدليل المحاسبي، ويتم إطفاء قيمته سنوياً عبر قائمة الدخل.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                lineHeight = 15.sp
                            )
                        }
                    }
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
    leaseholdAmount: String,
    onLeaseholdAmountChange: (String) -> Unit,
    leaseholdYears: String,
    onLeaseholdYearsChange: (String) -> Unit,
    leaseholdNotes: String,
    onLeaseholdNotesChange: (String) -> Unit,
    currencySymbol: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = "نقدية الدرج والبنوك والعقارات والخلو القائم",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "إدخال أرصدة الصندوق النقدي والحسابات البنكية وحالة العقار والخلو:",
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

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "مبلغ نقل قدم (خلو) [أصل غير ملموس بالدليل المحاسبي]",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = leaseholdAmount,
                    onValueChange = { input ->
                        val cleanInput = input.filter { it.isDigit() }
                        onLeaseholdAmountChange(cleanInput)
                    },
                    label = { Text("مبلغ نقل قدم (خلو) القائمة") },
                    suffix = { Text(currencySymbol) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                if ((leaseholdAmount.toDoubleOrNull() ?: 0.0) > 0.0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = leaseholdYears,
                            onValueChange = { input ->
                                val cleanInput = input.filter { it.isDigit() }
                                onLeaseholdYearsChange(cleanInput)
                            },
                            label = { Text("سنوات عقد نقل قدم (خلو)") },
                            suffix = { Text("سنوات") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = leaseholdNotes,
                            onValueChange = onLeaseholdNotesChange,
                            label = { Text("تفاصيل المتنازل") },
                            modifier = Modifier.weight(1.5f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }
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
    currencies: List<CurrencyEntity> = emptyList(),
    onSaveCurrency: (CurrencyEntity) -> Unit = {},
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
    onRemoveSupplier: (OpeningBalanceSupplier) -> Unit,
    onSaveUnit: (ProductUnitEntity) -> Unit = {},
    availableUnitsList: List<String> = emptyList()
) {
    var subTab by remember { mutableIntStateOf(0) }
    var showAddUnitDialog by remember { mutableStateOf(false) }
    var showAddCurrencyDialog by remember { mutableStateOf(false) }
    var selectedCurrency by remember(currencies) {
        mutableStateOf<CurrencyEntity?>(
            currencies.find { it.isBaseCurrency } ?: currencies.firstOrNull()
        )
    }

    if (showAddUnitDialog) {
        AddEditUnitDialog(
            productId = 0L,
            initialUnit = null,
            onSaveUnit = { unit ->
                onSaveUnit(unit)
                onNewProdUnitChange(unit.unitName)
                showAddUnitDialog = false
            },
            onDismiss = { showAddUnitDialog = false }
        )
    }

    if (showAddCurrencyDialog) {
        AddEditCurrencyDialog(
            initialCurrency = null,
            onSaveCurrency = { newCurrency ->
                onSaveCurrency(newCurrency)
                selectedCurrency = newCurrency
                showAddCurrencyDialog = false
            },
            onDismiss = { showAddCurrencyDialog = false }
        )
    }

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
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "إدخال صنف جديد للجرد الافتتاحي",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            // 1. رمز الباركود (مسح أو إدخال)
                            BarcodeTextField(
                                value = newProdBarcode,
                                onValueChange = onNewProdBarcodeChange,
                                label = "رمز الباركود (مسح أو إدخال)",
                                placeholder = "امسح الباركود بالكاميرا أو اكتبه...",
                                onBarcodeScanned = { scannedCode -> onNewProdBarcodeChange(scannedCode) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // 2. اسم الصنف والكمية
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = newProdName,
                                    onValueChange = onNewProdNameChange,
                                    label = { Text("اسم الصنف *") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                OutlinedTextField(
                                    value = newProdQty,
                                    onValueChange = onNewProdQtyChange,
                                    label = { Text("الكمية *") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(0.7f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // 3. الوحدة والعملة (فصل الوحدة في صف متوازن مع العملة لتجنب التكديس)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                UnitDropdownSelector(
                                    selectedUnit = newProdUnit,
                                    onUnitSelected = onNewProdUnitChange,
                                    label = "الوحدة *",
                                    availableUnitsList = availableUnitsList.ifEmpty { listOf("حبة/قطعة", "حبة", "كرتون", "كيلو", "درزن", "صندوق", "سحارة", "ربطة", "عبوة", "باكيت", "طرد", "جرام", "لتر", "متر", "شوال") },
                                    onAddNewUnitClick = { showAddUnitDialog = true },
                                    modifier = Modifier.weight(1f)
                                )

                                CurrencyDropdownSelector(
                                    selectedCurrency = selectedCurrency,
                                    currencies = currencies.ifEmpty {
                                        listOf(
                                            selectedCurrency ?: CurrencyEntity(code = "YER", name = "ريال يمني", symbol = currencySymbol.ifBlank { "ر.ي" }, exchangeRateToBase = 1.0, isBaseCurrency = true)
                                        )
                                    },
                                    onCurrencySelected = { curr -> selectedCurrency = curr },
                                    onAddNewCurrencyClick = { showAddCurrencyDialog = true },
                                    onSaveCurrency = onSaveCurrency,
                                    label = "العملة *",
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // 4. التكلفة وسعر البيع
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = newProdCost,
                                    onValueChange = onNewProdCostChange,
                                    label = { Text("التكلفة (${selectedCurrency?.symbol ?: currencySymbol})") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                OutlinedTextField(
                                    value = newProdPrice,
                                    onValueChange = onNewProdPriceChange,
                                    label = { Text("سعر البيع (${selectedCurrency?.symbol ?: currencySymbol})") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = onAddItem,
                                enabled = (newProdName.isNotBlank() || newProdBarcode.isNotBlank()) && (newProdQty.toDoubleOrNull() ?: 0.0) > 0,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("إضافة الصنف للجرد", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (openingItems.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Inventory2, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("لم يتم إضافة أصناف لجرد أول المدة بعد", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        Text(
                            text = "أصناف بضاعة أول المدة المسجلة (${openingItems.size}):",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        openingItems.forEach { item ->
                            val itemQtyStr = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else "%.2f".format(item.quantity)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(10.dp),
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
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(item.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                            if (item.barcode.isNotBlank()) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = MaterialTheme.colorScheme.secondaryContainer
                                                ) {
                                                    Text(
                                                        text = item.barcode,
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = "كمية: $itemQtyStr ${item.unitName} | تكلفة: ${item.costPrice} $currencySymbol | إجمالي: ${"%.2f".format(item.quantity * item.costPrice)} $currencySymbol",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = { onRemoveItem(item) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "حذف الصنف", tint = MaterialTheme.colorScheme.error)
                                    }
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
    leaseholdAmount: String,
    leaseholdYears: String,
    isSubmitting: Boolean,
    onLaunchSystem: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 16.dp)
            .navigationBarsPadding(),
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
                    if ((leaseholdAmount.toDoubleOrNull() ?: 0.0) > 0.0) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        SummaryRow(
                            label = "نقل قدم (خلو) / حق الانتفاع:",
                            value = "$leaseholdAmount $currencySymbol ($leaseholdYears سنوات)"
                        )
                    }
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
