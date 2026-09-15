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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import com.example.dokkani.ui.DokkaniViewModel
import com.example.dokkani.ui.OpeningBalanceCustomer
import com.example.dokkani.ui.OpeningBalanceItem
import com.example.dokkani.ui.OpeningBalanceSupplier

enum class GrocerySetupMode {
    NEW_GROCERY,       // بقالة جديدة: تحميل تصنيفات افتراضية والبدء فوراً
    EXISTING_GROCERY   // بقالة قائمة: معالج جرد بضاعة أول المدة ونقل الدفتر
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingWizardScreen(
    viewModel: DokkaniViewModel,
    onFinish: () -> Unit
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        val uiState by viewModel.uiState.collectAsState()

        var currentStep by remember { mutableIntStateOf(0) }
        var setupMode by remember { mutableStateOf(GrocerySetupMode.NEW_GROCERY) }

        // العملة الأساسية المختارة
        var selectedBaseCurrency by remember { mutableStateOf<com.example.dokkani.data.local.entities.CurrencyEntity?>(null) }

        // المزامنة التلقائية مع العملة الأساسية من حالة النظام
        LaunchedEffect(uiState.baseCurrency, uiState.currencies) {
            if (selectedBaseCurrency == null) {
                selectedBaseCurrency = uiState.baseCurrency
                    ?: uiState.currencies.find { it.isBaseCurrency }
                    ?: uiState.currencies.firstOrNull()
            }
        }

        val activeCurrencySymbol = uiState.baseCurrency?.symbol ?: selectedBaseCurrency?.symbol ?: "ر.س"
        val activeCurrencyName = uiState.baseCurrency?.name ?: selectedBaseCurrency?.name ?: "الريال السعودي"

        // بيانات التهيئة
        var storeName by remember { mutableStateOf("تموينات ومخضار السعادة") }
        var cashierName by remember { mutableStateOf("كاشير 1") }
        var openingCash by remember { mutableStateOf("300.0") }
        var valuationMethod by remember { mutableStateOf(CostValuationMethod.WAC) }

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

        // ديون العملاء الافتتاحية (دفتر الحساب الورقي)
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

        // مستحقات الموردين الافتتاحية
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

        // عدد الخطوات يعتمد على النمط (إضافة خطوة اختيار العملة الأساسية كخطوة 0 إجبارية)
        val totalSteps = if (setupMode == GrocerySetupMode.NEW_GROCERY) 4 else 6

        // التحقق من صحة خطوة العملة
        val isCurrencyStepValid = uiState.baseCurrency != null || selectedBaseCurrency != null
        val canProceedNext = if (currentStep == 0) isCurrencyStepValid else true

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "معالج التهيئة الأولى والرقابة المحاسبية",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "الخطوة ${currentStep + 1} من $totalSteps",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF1B5E20)
                    )
                )
            },
            bottomBar = {
                Surface(
                    tonalElevation = 8.dp,
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        if (currentStep == 0 && !isCurrencyStepValid) {
                            Text(
                                text = "⚠️ يرجى اختيار وتثبيت العملة الأساسية أولاً للمتابعة",
                                fontSize = 12.sp,
                                color = Color(0xFFD32F2F),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (currentStep > 0) {
                                OutlinedButton(
                                    onClick = { currentStep-- },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("السابق", fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Spacer(modifier = Modifier.width(1.dp))
                            }

                            if (currentStep < totalSteps - 1) {
                                Button(
                                    onClick = {
                                        if (canProceedNext) {
                                            if (currentStep == 0 && selectedBaseCurrency != null) {
                                                viewModel.setAsBaseCurrency(selectedBaseCurrency!!)
                                            }
                                            currentStep++
                                        }
                                    },
                                    enabled = canProceedNext,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF1B5E20),
                                        disabledContainerColor = Color(0xFFA5D6A7)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("التالي", fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            } else {
                                Button(
                                    onClick = {
                                        if (!isSubmitting) {
                                            isSubmitting = true
                                            val openingCashVal = openingCash.toDoubleOrNull() ?: 200.0
                                            val finalBaseCurr = uiState.baseCurrency ?: selectedBaseCurrency
                                            viewModel.completeOnboarding(
                                                selectedBaseCurrency = finalBaseCurr,
                                                isNewGrocery = setupMode == GrocerySetupMode.NEW_GROCERY,
                                                storeName = storeName.ifBlank { "تموينات ومخضار السعادة" },
                                                cashierName = cashierName.ifBlank { "كاشير 1" },
                                                openingCash = openingCashVal,
                                                valuationMethod = valuationMethod,
                                                openingItems = if (setupMode == GrocerySetupMode.EXISTING_GROCERY) openingItems else emptyList(),
                                                openingCustomers = if (setupMode == GrocerySetupMode.EXISTING_GROCERY) customers else emptyList(),
                                                openingSuppliers = if (setupMode == GrocerySetupMode.EXISTING_GROCERY) suppliers else emptyList(),
                                                onCompleted = onFinish
                                            )
                                        }
                                    },
                                    enabled = isCurrencyStepValid,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D5324)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    if (isSubmitting) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("جارِ تهيئة النظام...")
                                    } else {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("إنهاء التهيئة وبدء العمل", fontWeight = FontWeight.Bold)
                                    }
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
                    .background(Color(0xFFF8FAF9))
            ) {
                // شريط مؤشر الخطوات الدائري
                WizardStepIndicator(
                    totalSteps = totalSteps,
                    currentStep = currentStep,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )

                // محتوى الخطوة بتأثير حركي
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    AnimatedContent(
                        targetState = currentStep,
                        transitionSpec = {
                            if (targetState > initialState) {
                                (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                                    slideOutHorizontally { width -> -width } + fadeOut()
                                )
                            } else {
                                (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                                    slideOutHorizontally { width -> width } + fadeOut()
                                )
                            }
                        },
                        label = "WizardStepAnimation"
                    ) { step ->
                        when (step) {
                            0 -> StepSelectBaseCurrency(
                                currencies = uiState.currencies,
                                selectedCurrency = selectedBaseCurrency ?: uiState.baseCurrency,
                                onSelectCurrency = { curr ->
                                    selectedBaseCurrency = curr
                                    viewModel.setAsBaseCurrency(curr)
                                }
                            )
                            1 -> StepSelectMode(
                                selectedMode = setupMode,
                                onSelectMode = { setupMode = it }
                            )
                            2 -> StepStoreInfo(
                                storeName = storeName,
                                onStoreNameChange = { storeName = it },
                                cashierName = cashierName,
                                onCashierNameChange = { cashierName = it },
                                openingCash = openingCash,
                                onOpeningCashChange = { openingCash = it },
                                valuationMethod = valuationMethod,
                                onValuationMethodChange = { valuationMethod = it },
                                currencySymbol = activeCurrencySymbol
                            )
                            3 -> {
                                if (setupMode == GrocerySetupMode.NEW_GROCERY) {
                                    // الخطوة الأخيرة للبقالة الجديدة: مراجعة وملخص
                                    StepNewGrocerySummary(
                                        storeName = storeName,
                                        openingCash = openingCash,
                                        valuationMethod = valuationMethod,
                                        currencyName = activeCurrencyName,
                                        currencySymbol = activeCurrencySymbol
                                    )
                                } else {
                                    // بضاعة أول المدة للبقالة القائمة
                                    StepOpeningStock(
                                        items = openingItems,
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
                                        currencySymbol = activeCurrencySymbol
                                    )
                                }
                            }
                            4 -> {
                                // دفتر ديون العملاء الافتتاحي (للبقالة القائمة)
                                StepOpeningCustomers(
                currencySymbol = activeCurrencySymbol,
                                    customers = customers,
                                    name = newCustName,
                                    onNameChange = { newCustName = it },
                                    phone = newCustPhone,
                                    onPhoneChange = { newCustPhone = it },
                                    balance = newCustBalance,
                                    onBalanceChange = { newCustBalance = it },
                                    onAdd = {
                                        val bal = newCustBalance.toDoubleOrNull() ?: 0.0
                                        if (newCustName.isNotBlank()) {
                                            customers = customers + OpeningBalanceCustomer(newCustName, newCustPhone, bal)
                                            newCustName = ""
                                            newCustPhone = ""
                                            newCustBalance = ""
                                        }
                                    },
                                    onRemove = { c ->
                                        customers = customers.filter { it != c }
                                    }
                                )
                            }
                            5 -> {
                                // مستحقات الموردين وتأكيد الرقابة (للبقالة القائمة)
                                StepOpeningSuppliersAndReview(
                                    currencySymbol = activeCurrencySymbol,
                                    suppliers = suppliers,
                                    name = newSuppName,
                                    onNameChange = { newSuppName = it },
                                    phone = newSuppPhone,
                                    onPhoneChange = { newSuppPhone = it },
                                    balance = newSuppBalance,
                                    onBalanceChange = { newSuppBalance = it },
                                    onAdd = {
                                        val bal = newSuppBalance.toDoubleOrNull() ?: 0.0
                                        if (newSuppName.isNotBlank()) {
                                            suppliers = suppliers + OpeningBalanceSupplier(newSuppName, newSuppPhone, bal)
                                            newSuppName = ""
                                            newSuppPhone = ""
                                            newSuppBalance = ""
                                        }
                                    },
                                    onRemove = { s ->
                                        suppliers = suppliers.filter { it != s }
                                    },
                                    storeName = storeName,
                                    openingCash = openingCash,
                                    stockCount = openingItems.size,
                                    customersCount = customers.size
                                )
                            }
                        }
                    }
                }
            }
        }
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
                isCompleted -> Color(0xFF1B5E20)
                isCurrent -> Color(0xFF2E7D32)
                else -> Color(0xFFE0E0E0)
            }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Text(
                        text = "${i + 1}",
                        color = if (isCurrent) Color.White else Color(0xFF757575),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            if (i < totalSteps - 1) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(3.dp)
                        .padding(horizontal = 4.dp)
                        .background(if (i < currentStep) Color(0xFF1B5E20) else Color(0xFFE0E0E0))
                )
            }
        }
    }
}

@Composable
private fun StepSelectMode(
    selectedMode: GrocerySetupMode,
    onSelectMode: (GrocerySetupMode) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "مرحباً بك في نظام دكاني الذكي",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1B5E20),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "اختر طبيعة منشأتك لتخصيص بيئة المحاسبة ونقاط البيع بدقة:",
            fontSize = 14.sp,
            color = Color(0xFF555555),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // خيار بقالة جديدة
        SetupModeCard(
            title = "أ) بقالة جديدة (بدء فوري)",
            description = "تحميل دليل تصنيفات وبنود جاهزة تلقائياً، إعداد الدرج بنقدية افتتاحية، والبدء بعمليات البيع المباشر دون الحاجة لجرد سابق.",
            icon = Icons.Default.Storefront,
            isSelected = selectedMode == GrocerySetupMode.NEW_GROCERY,
            badge = "موصى به للمتاجر الحديثة",
            onClick = { onSelectMode(GrocerySetupMode.NEW_GROCERY) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // خيار بقالة قائمة
        SetupModeCard(
            title = "ب) بقالة قائمة (نقل الدفتر والجرد)",
            description = "معالج ترحيل أرصدة الدفتر الورقي، إدخال بضاعة أول المدة، تسجيل مديونيات الزبائن المسجلة في الكشكول، ومستحقات الموردين.",
            icon = Icons.Default.MenuBook,
            isSelected = selectedMode == GrocerySetupMode.EXISTING_GROCERY,
            badge = "ترحيل الدفاتر والأرصدة القائمة",
            onClick = { onSelectMode(GrocerySetupMode.EXISTING_GROCERY) }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // تنبيه إرشادي (Coachmark)
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFE8F5E9),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA5D6A7)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF2E7D32))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "جميع البيانات المسجلة تعمل دون الحاجة للاتصال بالإنترنت مع دعم دورة محاسبية كاملة وجرد فوري ومطابقة دقيقة لشفت الكاشير.",
                    fontSize = 12.sp,
                    color = Color(0xFF1B5E20),
                    lineHeight = 18.sp
                )
            }
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
            containerColor = if (isSelected) Color(0xFFF1F8E9) else Color.White
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) Color(0xFF2E7D32) else Color(0xFFE0E0E0)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) Color(0xFF1B5E20) else Color(0xFFE8F5E9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) Color.White else Color(0xFF2E7D32)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color(0xFF1B5E20) else Color(0xFF212121)
                    )
                }

                RadioButton(
                    selected = isSelected,
                    onClick = onClick,
                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF1B5E20))
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = description,
                fontSize = 13.sp,
                color = Color(0xFF616161),
                lineHeight = 19.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (isSelected) Color(0xFFC8E6C9) else Color(0xFFEEEEEE)
            ) {
                Text(
                    text = badge,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) Color(0xFF1B5E20) else Color(0xFF616161),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun StepSelectBaseCurrency(
    currencies: List<com.example.dokkani.data.local.entities.CurrencyEntity>,
    selectedCurrency: com.example.dokkani.data.local.entities.CurrencyEntity?,
    onSelectCurrency: (com.example.dokkani.data.local.entities.CurrencyEntity) -> Unit
) {
    val defaultCurrencies = listOf(
        com.example.dokkani.data.local.entities.CurrencyEntity(code = "YER", name = "الريال اليمني", symbol = "ر.ي", exchangeRateToBase = 1.0, isBaseCurrency = true, isDefault = true),
        com.example.dokkani.data.local.entities.CurrencyEntity(code = "SAR", name = "الريال السعودي", symbol = "ر.س", exchangeRateToBase = 1.0, isBaseCurrency = false, isDefault = false),
        com.example.dokkani.data.local.entities.CurrencyEntity(code = "USD", name = "الدولار الأمريكي", symbol = "$", exchangeRateToBase = 3.75, isBaseCurrency = false, isDefault = false),
        com.example.dokkani.data.local.entities.CurrencyEntity(code = "EGP", name = "الجنيه المصري", symbol = "ج.م", exchangeRateToBase = 0.076, isBaseCurrency = false, isDefault = false),
        com.example.dokkani.data.local.entities.CurrencyEntity(code = "AED", name = "الدرهم الإماراتي", symbol = "د.إ", exchangeRateToBase = 1.02, isBaseCurrency = false, isDefault = false)
    )

    val displayList = if (currencies.isNotEmpty()) {
        val existingCodes = currencies.map { it.code }.toSet()
        val missingDefaults = defaultCurrencies.filter { it.code !in existingCodes }
        currencies + missingDefaults
    } else {
        defaultCurrencies
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "الخطوة الأولى: تحديد العملة الأساسية للمتجر (Base Currency)",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B5E20)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "اختر العملة الرئيسية المعتمدة لكافة حسابات المتجر والميزانية العمومية والتعاملات المالّية:",
                fontSize = 13.sp,
                color = Color(0xFF555555),
                lineHeight = 19.sp
            )
        }

        items(displayList) { curr ->
            val isSelected = selectedCurrency?.code == curr.code || (selectedCurrency == null && curr.isBaseCurrency)
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) Color(0xFFF1F8E9) else Color.White
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) Color(0xFF2E7D32) else Color(0xFFE0E0E0)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectCurrency(curr) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Color(0xFF1B5E20) else Color(0xFFE8F5E9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = curr.symbol,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else Color(0xFF2E7D32)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = curr.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color(0xFF1B5E20) else Color(0xFF212121)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFFE0E0E0)
                                ) {
                                    Text(
                                        text = curr.code,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF424242),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "رمز التعامل: ${curr.symbol} • سعر الصرف الأساسي 1.0",
                                fontSize = 12.sp,
                                color = Color(0xFF757575)
                            )
                        }
                    }

                    RadioButton(
                        selected = isSelected,
                        onClick = { onSelectCurrency(curr) },
                        colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF1B5E20))
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFE8F5E9),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA5D6A7)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color(0xFF2E7D32))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "عند تثبيت العملة المختارة كعملة أساسية، يتم تصفير أي رايات أساسية سابقة تلقائياً وبشكل حتمي في قاعدة البيانات.",
                        fontSize = 12.sp,
                        color = Color(0xFF1B5E20),
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun StepStoreInfo(
    storeName: String,
    onStoreNameChange: (String) -> Unit,
    cashierName: String,
    onCashierNameChange: (String) -> Unit,
    openingCash: String,
    onOpeningCashChange: (String) -> Unit,
    valuationMethod: CostValuationMethod,
    onValuationMethodChange: (CostValuationMethod) -> Unit,
    currencySymbol: String = "ر.س"
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "بيانات المنشأة وإعدادات الرقابة والصندوق",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B5E20)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "حدد اسم المحل، العهدة النقدية الافتتاحية في درج الكاشير، وسياسة تقييم التكلفة:",
                fontSize = 13.sp,
                color = Color(0xFF666666)
            )
        }

        item {
            OutlinedTextField(
                value = storeName,
                onValueChange = onStoreNameChange,
                label = { Text("اسم البقالة / المتجر") },
                leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = null, tint = Color(0xFF2E7D32)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        item {
            OutlinedTextField(
                value = cashierName,
                onValueChange = onCashierNameChange,
                label = { Text("اسم كاشير الفترة الافتتاحية") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF2E7D32)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        item {
            OutlinedTextField(
                value = openingCash,
                onValueChange = onOpeningCashChange,
                label = { Text("العهدة الافتتاحية في درج الكاشير (الفكة) - $currencySymbol") },
                leadingIcon = { Icon(Icons.Default.Payments, contentDescription = null, tint = Color(0xFF2E7D32)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "طريقة تقييم تكلفة المخزون والأرباح:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF212121)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    CostValuationMethod.values().forEach { method ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onValuationMethodChange(method) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = valuationMethod == method,
                                onClick = { onValuationMethodChange(method) },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF1B5E20))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = method.labelArabic,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = when (method) {
                                        CostValuationMethod.WAC -> "المتوسط المرجح للتكلفة (الأكثر دقة واستقراراً للبقالات)"
                                        CostValuationMethod.FIFO -> "الوارد أولاً صادر أولاً (مثالي للسلع الاستهلاكية سريعة الحركة)"
                                        CostValuationMethod.LAST_PURCHASE_PRICE -> "آخر سعر شراء من المورد (يحاكي تضخم الأسعار اللحظي)"
                                    },
                                    fontSize = 11.sp,
                                    color = Color(0xFF757575)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepNewGrocerySummary(
    storeName: String,
    openingCash: String,
    valuationMethod: CostValuationMethod,
    currencyName: String = "الريال السعودي",
    currencySymbol: String = "ر.س"
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = "ملخص التهيئة وجاهزية البيع",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1B5E20)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "سيتم تفعيل النظام وبدء أول شفت مالي فوراً مع الخيارات التالية:",
            fontSize = 13.sp,
            color = Color(0xFF666666)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC8E6C9)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SummaryRow("العملة الأساسية للنظام:", "$currencyName ($currencySymbol)")
                Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFEEEEEE))
                SummaryRow("اسم المتجر:", storeName)
                Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFEEEEEE))
                SummaryRow("عهدة الدرج الافتتاحية:", "$openingCash $currencySymbol")
                Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFEEEEEE))
                SummaryRow("طريقة التقييم المحاسبي:", valuationMethod.labelArabic)
                Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFEEEEEE))
                SummaryRow("أصناف البقالة الافتراضية:", "محملة ومحدثة مع وحداتها (خضار، ألبان، تموينات)")
                Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFEEEEEE))
                SummaryRow("حالة الشفت المالي:", "جاهز للفتح التلقائي (Open Shift)")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFFFF8E1),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFE082)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFFF57F17))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "عند الضغط على (إنهاء التهيئة)، ستنتقل مباشرة إلى شاشة الكاشير لبدء الفواتير والسندات ومطابقة حركة الصندوق اللحظية.",
                    fontSize = 12.sp,
                    color = Color(0xFFE65100),
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun StepOpeningStock(
    items: List<OpeningBalanceItem>,
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
    currencySymbol: String = "ر.س"
) {
    val commonUnits = listOf("حبة/قطعة", "كرتون", "درزن", "كيلو", "كيس", "حزمة", "صندوق", "شدة")

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "جرد بضاعة أول المدة (Opening Inventory)",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B5E20)
            )
            Text(
                text = "أدخل الأصناف الموجودة فعلياً على الرفوف مع الباركود والوحدة لتوليد قيود مخزون افتتاحية:",
                fontSize = 12.sp,
                color = Color(0xFF666666)
            )
        }

        // نموذج إضافة صنف جرد سريع
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("إضافة صنف جرد سريع مع الباركود والوحدة:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))

                    com.example.dokkani.ui.components.BarcodeTextField(
                        value = newProdBarcode,
                        onValueChange = onNewProdBarcodeChange,
                        label = "مسح باركود المنتج (بالكاميرا أو يدوياً)",
                        placeholder = "امسح الباركود بالكاميرا لحفظه مع الصنف...",
                        onBarcodeScanned = { scannedCode ->
                            onNewProdBarcodeChange(scannedCode)
                            if (newProdName.isBlank()) {
                                onNewProdNameChange("صنف باركود $scannedCode")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = newProdName,
                        onValueChange = onNewProdNameChange,
                        label = { Text("اسم الصنف / المنتج *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // اختيار وحدة الصنف
                    Text("وحدة القياس / العبوة:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF333333))
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(commonUnits) { u ->
                            FilterChip(
                                selected = newProdUnit == u,
                                onClick = { onNewProdUnitChange(u) },
                                label = { Text(u, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = newProdUnit,
                        onValueChange = onNewProdUnitChange,
                        label = { Text("أو اكتب وحدة مخصصة (مثال: جالون، ربطة...)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = newProdQty,
                            onValueChange = onNewProdQtyChange,
                            label = { Text("الكمية ($newProdUnit)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = newProdCost,
                            onValueChange = onNewProdCostChange,
                            label = { Text("سعر التكلفة") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = newProdPrice,
                            onValueChange = onNewProdPriceChange,
                            label = { Text("سعر البيع") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = onAddItem,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إضافة إلى قائمة الجرد الافتتاحي", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        item {
            Text(
                text = "الأصناف المحصورة (${items.size}):",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = Color(0xFF333333)
            )
        }

        items(items) { item ->
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE))
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
                            Text(item.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFE8F5E9)
                            ) {
                                Text(
                                    text = item.unitName,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (item.barcode.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(13.dp), tint = Color(0xFF1976D2))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("الباركود: ${item.barcode}", fontSize = 11.sp, color = Color(0xFF1976D2), fontWeight = FontWeight.Medium)
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "الكمية: ${item.quantity} ${item.unitName} | التكلفة: ${item.costPrice} $currencySymbol | البيع: ${item.sellingPrice} $currencySymbol",
                            fontSize = 11.sp,
                            color = Color(0xFF666666)
                        )
                    }
                    IconButton(onClick = { onRemoveItem(item) }) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFE53935), modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun StepOpeningCustomers(
    currencySymbol: String = "ر.س",
    customers: List<OpeningBalanceCustomer>,
    name: String,
    onNameChange: (String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    balance: String,
    onBalanceChange: (String) -> Unit,
    onAdd: () -> Unit,
    onRemove: (OpeningBalanceCustomer) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "نقل دفتر ديون العملاء (الشكك الورقي)",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B5E20)
            )
            Text(
                text = "سجل الزبائن الذين لديهم حسابات سابقة لترحيل مديونياتهم لدفتر الحساب الإلكتروني:",
                fontSize = 12.sp,
                color = Color(0xFF666666)
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("إضافة عميل من الدفتر القديم:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = onNameChange,
                        label = { Text("اسم العميل / الزبون*") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = onPhoneChange,
                            label = { Text("رقم الجوال") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = balance,
                            onValueChange = onBalanceChange,
                            label = { Text("الرصيد السابق (مدين لنا)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = onAdd,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("قيد العميل في الدفتر الافتتاحي", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        item {
            val totalCustomerDebt = customers.sumOf { it.openingBalance }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("قائمة عملاء الدفتر (${customers.size}):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("إجمالي ديون الدفتر: ${"%.2f".format(totalCustomerDebt)} $currencySymbol", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1B5E20))
            }
        }

        items(customers) { c ->
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(c.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("الجوال: ${c.phone.ifBlank { "غير مسجل" }}", fontSize = 11.sp, color = Color.Gray)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${"%.2f".format(c.openingBalance)} $currencySymbol",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC62828),
                            fontSize = 13.sp
                        )
                        IconButton(onClick = { onRemove(c) }) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFE53935), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepOpeningSuppliersAndReview(
    currencySymbol: String = "ر.س",
    suppliers: List<OpeningBalanceSupplier>,
    name: String,
    onNameChange: (String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    balance: String,
    onBalanceChange: (String) -> Unit,
    onAdd: () -> Unit,
    onRemove: (OpeningBalanceSupplier) -> Unit,
    storeName: String,
    openingCash: String,
    stockCount: Int,
    customersCount: Int
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "مستحقات الموردين وتأكيد الرقابة المحاسبية",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B5E20)
            )
            Text(
                text = "سجل أي فواتير مؤجلة للموردين (ألبان، مخابز، خضار) لاعتماد رصيدهم الافتتاحي:",
                fontSize = 12.sp,
                color = Color(0xFF666666)
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("إضافة مورد بمستحق سابق:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = onNameChange,
                        label = { Text("اسم المورد / الشركة*") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = onPhoneChange,
                            label = { Text("رقم التواصل") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = balance,
                            onValueChange = onBalanceChange,
                            label = { Text("المبلغ المستحق له (${currencySymbol})") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = onAdd,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AddBusiness, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("قيد مستحق المورد", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        items(suppliers) { s ->
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(s.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("الجوال: ${s.phone.ifBlank { "غير مسجل" }}", fontSize = 11.sp, color = Color.Gray)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${"%.2f".format(s.openingPayable)} $currencySymbol",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE65100),
                            fontSize = 13.sp
                        )
                        IconButton(onClick = { onRemove(s) }) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFE53935), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA5D6A7))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "جاهزية تدقيق البيانات والأرصدة الافتتاحية:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF1B5E20)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("• متجر: $storeName | العهدة النقدية في الدرج: $openingCash $currencySymbol", fontSize = 12.sp)
                    Text("• تم تقييد $stockCount صنفاً ضمن بضاعة أول المدة", fontSize = 12.sp)
                    Text("• تم ترحيل $customersCount عميلاً من الدفتر القديم", fontSize = 12.sp)
                    Text("• تم قيد ${suppliers.size} مورداً مع التزاماتهم المالية", fontSize = 12.sp)
                }
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
        Text(text = label, color = Color(0xFF616161), fontSize = 13.sp)
        Text(text = value, fontWeight = FontWeight.Bold, color = Color(0xFF212121), fontSize = 13.sp)
    }
}
