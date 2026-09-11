package com.example.dokkani.ui.screens.pos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dokkani.data.local.entities.PartyEntity
import com.example.dokkani.data.local.entities.ProductUnitEntity
import com.example.dokkani.data.local.entities.ProductWithUnits
import com.example.dokkani.domain.hardware.PrinterConnectionState
import com.example.dokkani.domain.hardware.PrinterPaperWidth
import com.example.dokkani.ui.DokkaniViewModel

/**
 * الشاشة الرئيسية لنقطة البيع السريعة (POS Screen) لتطبيق دكاني
 * تجمع بين:
 * 1. شريط البحث والباركود (مع دعم باركود الميزان الإلكتروني بادئة 21)
 * 2. أزرار التحكم بالملحقات وفتح درج النقدية ESC p 0 25 250
 * 3. شبكة الأصناف السريعة (Quick Tiles) للخضار والمخبوزات والأسعار المفتوحة
 * 4. سلة المبيعات وحساب الضرائب والإجماليات
 * 5. نوافذ الدفع (كاش / مدى / آجل) وطباعة الفاتورة الحرارية
 */
@Composable
fun PosScreen(
    viewModel: DokkaniViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val allProducts by viewModel.productsWithUnits.collectAsState()
    val allParties by viewModel.parties.collectAsState()
    val printerState by viewModel.printerManager.connectionState.collectAsState()
    val paperWidth by viewModel.printerManager.selectedPaperWidth.collectAsState()

    // فلترة الأصناف حسب شريط البحث
    val filteredProducts = remember(uiState.posSearchQuery, allProducts) {
        val q = uiState.posSearchQuery.trim()
        if (q.isBlank()) {
            emptyList()
        } else {
            allProducts.filter { pw ->
                pw.product.name.contains(q, ignoreCase = true) ||
                        pw.product.code.contains(q, ignoreCase = true) ||
                        pw.units.any { it.barcode.contains(q, ignoreCase = true) || it.unitName.contains(q, ignoreCase = true) }
            }
        }
    }

    // تصفية العملاء لطريقة الدفع الآجل (الشكك)
    val customers = remember(allParties) {
        allParties.filter { it.type != com.example.dokkani.data.local.entities.PartyType.SUPPLIER }
    }

    Column(modifier = modifier.fillMaxSize()) {
        val eval = uiState.licenseEvaluation
        if (eval != null && (eval.isLocked || eval.isNearExpiryWarning)) {
            PosLicenseWarningBanner(
                eval = eval,
                onNavigateToLicense = { viewModel.selectTab(8) },
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val isWideScreen = maxWidth > 650.dp

        if (isWideScreen) {
            // شاشات عريضة / أجهزة لوحية (Landscape / Tablet) - تقسيم عمودين
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // القسم الأيمن: شريط البحث وشبكة الأصناف السريعة والكاتالوج
                Column(
                    modifier = Modifier
                        .weight(1.3f)
                        .fillMaxSize()
                ) {
                    PosTopBar(
                        searchQuery = uiState.posSearchQuery,
                        printerState = printerState,
                        onSearchChange = { viewModel.setPosSearchQuery(it) },
                        onScanOrEnter = { viewModel.handleBarcodeScannedOrEntered(it) },
                        onCameraClick = { viewModel.setShowCameraScannerDialog(true) },
                        onOpenDrawerClick = { viewModel.openCashDrawerManual() },
                        onHardwareClick = { viewModel.openHardwareDialog() }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (uiState.posSearchQuery.isNotBlank()) {
                        SearchResultsList(
                            products = filteredProducts,
                            onProductUnitClick = { product, unit ->
                                viewModel.addProductToCart(product, selectedUnit = unit)
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        PosQuickTilesGrid(
                            tiles = viewModel.defaultQuickTiles,
                            onTileClick = { viewModel.addQuickTileToCart(it) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // القسم الأيسر: سلة المبيعات والإجماليات
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                ) {
                    PosCartComponent(
                        cartItems = uiState.cartItems,
                        cartSummary = uiState.cartSummary,
                        onQuantityChange = { id, qty -> viewModel.updateCartItemQuantity(id, qty) },
                        onRemoveItem = { id -> viewModel.removeCartItem(id) },
                        onClearCart = { viewModel.clearCart() },
                        onCheckoutClick = { viewModel.openCheckoutDialog() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        } else {
            // شاشات الهواتف العمودية (Portrait)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                PosTopBar(
                    searchQuery = uiState.posSearchQuery,
                    printerState = printerState,
                    onSearchChange = { viewModel.setPosSearchQuery(it) },
                    onScanOrEnter = { viewModel.handleBarcodeScannedOrEntered(it) },
                    onCameraClick = { viewModel.setShowCameraScannerDialog(true) },
                    onOpenDrawerClick = { viewModel.openCashDrawerManual() },
                    onHardwareClick = { viewModel.openHardwareDialog() }
                )

                Spacer(modifier = Modifier.height(6.dp))

                // منطقة الأصناف (سريعة أو بحث)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (uiState.posSearchQuery.isNotBlank()) {
                        SearchResultsList(
                            products = filteredProducts,
                            onProductUnitClick = { product, unit ->
                                viewModel.addProductToCart(product, selectedUnit = unit)
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        PosQuickTilesGrid(
                            tiles = viewModel.defaultQuickTiles,
                            onTileClick = { viewModel.addQuickTileToCart(it) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // سلة المبيعات السفلية
                PosCartComponent(
                    cartItems = uiState.cartItems,
                    cartSummary = uiState.cartSummary,
                    onQuantityChange = { id, qty -> viewModel.updateCartItemQuantity(id, qty) },
                    onRemoveItem = { id -> viewModel.removeCartItem(id) },
                    onClearCart = { viewModel.clearCart() },
                    onCheckoutClick = { viewModel.openCheckoutDialog() }
                )
            }
        }
    }
}

    // ==========================================
    // نوافذ الحوار الخاصة بنقطة البيع والملحقات
    // ==========================================

    // 1. نافذة إتمام الدفع (كاش / مدى / آجل)
    if (uiState.showCheckoutDialog) {
        PosCheckoutDialog(
            cartSummary = uiState.cartSummary,
            selectedPaymentMethod = uiState.selectedPaymentMethod,
            customers = customers,
            selectedCustomerPartyId = uiState.selectedCustomerPartyId,
            paidAmountInput = uiState.paidAmountInput,
            discountInput = uiState.discountInput,
            openDrawerOnCash = uiState.openDrawerAutomaticallyOnCash,
            isProcessing = uiState.isProcessingCheckout,
            onPaymentMethodSelect = { viewModel.selectPaymentMethod(it) },
            onCustomerSelect = { viewModel.selectCustomerParty(it) },
            onPaidAmountChange = { viewModel.setPaidAmountInput(it) },
            onDiscountChange = { viewModel.setDiscountInput(it) },
            onToggleOpenDrawer = { viewModel.setOpenDrawerAutomatically(it) },
            onConfirmCheckout = { viewModel.processCheckout() },
            onDismiss = { viewModel.dismissCheckoutDialog() }
        )
    }

    // 2. نافذة إيصال الفاتورة الحرارية بعد البيع
    if (uiState.showReceiptDialog && uiState.lastCheckoutResult != null) {
        PosReceiptDialog(
            checkoutResult = uiState.lastCheckoutResult!!,
            paperWidthLabel = if (paperWidth == PrinterPaperWidth.WIDTH_80MM) "80 مم" else "58 مم",
            onPrintAgain = { viewModel.printCurrentReceiptAgain() },
            onDismiss = { viewModel.dismissReceiptDialog() }
        )
    }

    // 3. نافذة باركود الميزان الإلكتروني
    if (uiState.showScaleBarcodeDialog && uiState.detectedScaleBarcode != null) {
        PosScaleBarcodeDialog(
            detectedBarcode = uiState.detectedScaleBarcode!!,
            availableProducts = allProducts,
            onConfirmProduct = { viewModel.applyDetectedScaleBarcodeToProduct(it) },
            onDismiss = { viewModel.dismissScaleBarcodeDialog() }
        )
    }

    // 4. نافذة إعدادات الملحقات والطابعة ودرج النقدية
    if (uiState.showHardwareDialog) {
        PosHardwareDialog(
            printerManager = viewModel.printerManager,
            onOpenCashDrawerManual = { viewModel.openCashDrawerManual() },
            onDismiss = { viewModel.dismissHardwareDialog() }
        )
    }

    // 5. نافذة السعر المفتوح
    if (uiState.showOpenPriceDialog) {
        PosOpenPriceDialog(
            onConfirm = { name, price, qty, isWeighted ->
                viewModel.addCustomOpenPriceItem(name, price, qty, isWeighted)
            },
            onDismiss = { viewModel.setShowOpenPriceDialog(false) }
        )
    }

    // 6. نافذة محاكي الكاميرا وقارئ الباركود
    if (uiState.showCameraScannerDialog) {
        PosCameraScannerDialog(
            onBarcodeScanned = { viewModel.handleBarcodeScannedOrEntered(it) },
            onDismiss = { viewModel.setShowCameraScannerDialog(false) }
        )
    }
}

/**
 * الشريط العلوي لشاشة نقطة البيع:
 * حقل البحث والباركود + أزرار تشغيل درج النقدية وطابعة البلوتوث والكاميرا
 */
@Composable
private fun PosTopBar(
    searchQuery: String,
    printerState: PrinterConnectionState,
    onSearchChange: (String) -> Unit,
    onScanOrEnter: (String) -> Unit,
    onCameraClick: () -> Unit,
    onOpenDrawerClick: () -> Unit,
    onHardwareClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // شريط إدخال الباركود والبحث
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("امسح أو اكتب الباركود أو اسم الصنف...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "باركود",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "مسح")
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onScanOrEnter(searchQuery) }),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pos_barcode_search_input")
                )

                // زر قراءة الباركود عبر الكاميرا
                IconButton(
                    onClick = onCameraClick,
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(10.dp))
                        .testTag("camera_scan_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "كاميرا الباركود",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // شريط حالة الملحقات وأوامر العتاد (درج النقدية وطابعة البلوتوث)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // زر فتح درج النقدية المباشر ESC p 0 25 250
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onOpenDrawerClick() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LockOpen,
                            contentDescription = "فتح درج النقدية",
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "فتح الدرج (ESC p)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }

                // زر إعدادات طابعة البلوتوث
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (printerState) {
                        is PrinterConnectionState.Connected -> Color(0xFFE8F5E9)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onHardwareClick() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bluetooth,
                            contentDescription = "طابعة البلوتوث",
                            tint = when (printerState) {
                                is PrinterConnectionState.Connected -> Color(0xFF2E7D32)
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when (printerState) {
                                is PrinterConnectionState.Connected -> "طابعة البلوتوث متصلة"
                                else -> "إعدادات الطابعة والملحقات"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = when (printerState) {
                                is PrinterConnectionState.Connected -> Color(0xFF1B5E20)
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * قائمة نتائج البحث عن الأصناف مع عرض الوحدات المتعددة والأسعار
 */
@Composable
private fun SearchResultsList(
    products: List<ProductWithUnits>,
    onProductUnitClick: (ProductWithUnits, ProductUnitEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    if (products.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.height(6.dp))
                Text("لا توجد أصناف مطابقة للبحث", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    } else {
        LazyColumn(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(products, key = { it.product.id }) { item ->
                ProductSearchResultCard(
                    productWithUnits = item,
                    onUnitClick = { unit -> onProductUnitClick(item, unit) }
                )
            }
        }
    }
}

@Composable
private fun ProductSearchResultCard(
    productWithUnits: ProductWithUnits,
    onUnitClick: (ProductUnitEntity) -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = productWithUnits.product.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "كود: ${productWithUnits.product.code}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                if (productWithUnits.product.isWeighted) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Scale, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("موزون", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // أزرار الوحدات المتعددة لهذا الصنف
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                productWithUnits.units.forEach { unit ->
                    FilledTonalButton(
                        onClick = { onUnitClick(unit) },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(unit.unitName, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("%.2f ر.س".format(unit.sellingPrice), fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * شريط تحذيري ذكي في شاشة البيع عند اقتراب انتهاء الترخيص أو قفل النظام
 */
@Composable
fun PosLicenseWarningBanner(
    eval: com.example.dokkani.domain.security.LicenseEvaluationResult,
    onNavigateToLicense: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLocked = eval.isLocked
    val bgColor = if (isLocked) MaterialTheme.colorScheme.errorContainer else Color(0xFFFFF3CD)
    val contentColor = if (isLocked) MaterialTheme.colorScheme.onErrorContainer else Color(0xFF856404)
    val icon = if (isLocked) Icons.Default.Lock else Icons.Default.Warning

    Card(
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onNavigateToLicense() }
            .testTag("pos_license_warning_banner")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = eval.warningMessageArabic ?: if (isLocked) "النظام مقفل! يلزم التفعيل لمتابعة البيع" else "تنبيه اقتراب موعد تجديد الترخيص",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = contentColor,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(
                    text = "تفعيل الآن",
                    color = if (isLocked) MaterialTheme.colorScheme.onError else Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}
