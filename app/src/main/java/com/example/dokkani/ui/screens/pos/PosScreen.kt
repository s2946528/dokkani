package com.example.dokkani.ui.screens.pos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dokkani.data.local.entities.PartyEntity
import com.example.dokkani.data.local.entities.PartyType
import com.example.dokkani.data.local.entities.PaymentMethod
import com.example.dokkani.domain.pos.PosOperation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    viewModel: PosViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var showAddPartyDialog by remember { mutableStateOf(false) }
    var newPartyName by remember { mutableStateOf("") }
    var newPartyPhone by remember { mutableStateOf("") }
    var newPartyLimit by remember { mutableStateOf("1000") }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            snackbarHost = {
                uiState.userFeedbackMessage?.let { message ->
                    Snackbar(
                        modifier = Modifier.padding(16.dp),
                        containerColor = if (uiState.isError) MaterialTheme.colorScheme.errorContainer else Color(0xFF1B5E20),
                        contentColor = if (uiState.isError) MaterialTheme.colorScheme.onErrorContainer else Color.White,
                        action = {
                            TextButton(onClick = { viewModel.dismissFeedback() }) {
                                Text("حسناً", color = Color.White)
                            }
                        }
                    ) {
                        Text(message)
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color(0xFFF4F6F8))
                    .padding(8.dp)
            ) {
                // 1. كرت إجمالي مبيعات الشفت وحالة صندوق الكاشير
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1B5E20)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("إجمالي مبيعات الشفت", color = Color(0xFFC8E6C9), fontSize = 11.sp)
                                Text("%.2f ر.س".format(uiState.shiftTotalSales), color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(24.dp))
                            Column {
                                Text("النقدية الحالية بالدرج", color = Color(0xFFC8E6C9), fontSize = 11.sp)
                                Text("%.2f ر.س".format(uiState.cashInDrawer), color = Color(0xFFFFD54F), fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                onClick = { viewModel.toggleHistoryDialog(true) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("سجل العمليات", color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                }

                // 2. شريط العمليات الست والتبديل السريع
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        FilterChip(
                            selected = uiState.activeOperation == PosOperation.SALE,
                            onClick = { viewModel.selectOperation(PosOperation.SALE) },
                            label = { Text("فاتورة بيع") },
                            leadingIcon = { Icon(Icons.Default.PointOfSale, contentDescription = null) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF1B5E20),
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White
                            )
                        )
                    }
                    item {
                        FilterChip(
                            selected = uiState.activeOperation == PosOperation.PURCHASE,
                            onClick = { viewModel.selectOperation(PosOperation.PURCHASE) },
                            label = { Text("فاتورة شراء") },
                            leadingIcon = { Icon(Icons.Default.ShoppingBag, contentDescription = null) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF1976D2),
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White
                            )
                        )
                    }
                    item {
                        FilterChip(
                            selected = uiState.activeOperation == PosOperation.SALE_RETURN,
                            onClick = { viewModel.selectOperation(PosOperation.SALE_RETURN) },
                            label = { Text("مردود بيع") },
                            leadingIcon = { Icon(Icons.Default.AssignmentReturn, contentDescription = null) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFD32F2F),
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White
                            )
                        )
                    }
                    item {
                        FilterChip(
                            selected = uiState.activeOperation == PosOperation.PURCHASE_RETURN,
                            onClick = { viewModel.selectOperation(PosOperation.PURCHASE_RETURN) },
                            label = { Text("مردود شراء") },
                            leadingIcon = { Icon(Icons.Default.RemoveShoppingCart, contentDescription = null) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF7B1FA2),
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White
                            )
                        )
                    }
                    item {
                        FilterChip(
                            selected = uiState.activeOperation == PosOperation.RECEIPT,
                            onClick = { viewModel.selectOperation(PosOperation.RECEIPT) },
                            label = { Text("سند قبض") },
                            leadingIcon = { Icon(Icons.Default.ArrowDownward, contentDescription = null) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF00796B),
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White
                            )
                        )
                    }
                    item {
                        FilterChip(
                            selected = uiState.activeOperation == PosOperation.EXPENSE,
                            onClick = { viewModel.selectOperation(PosOperation.EXPENSE) },
                            label = { Text("سند صرف") },
                            leadingIcon = { Icon(Icons.Default.ArrowUpward, contentDescription = null) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFE65100),
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White
                            )
                        )
                    }
                }

                // 3. جسم الشاشة الرئيسي: يعتمد على طبيعة العملية (فواتير/مردودات vs سندات مالية)
                if (uiState.activeOperation.isVoucher) {
                    // --- وضع السندات المالية (قبض / صرف): نموذج محاسبي مخصص بالكامل بدون سلة ---
                    PosVoucherSection(
                        uiState = uiState,
                        viewModel = viewModel,
                        onOpenAddPartyDialog = { showAddPartyDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                } else {
                    // --- وضع الفواتير والمردودات (بيع، شراء، مردود بيع، مردود شراء): قسم المنتجات + سلة العمليات ---
                    PosInvoiceSection(
                        uiState = uiState,
                        viewModel = viewModel,
                        onOpenAddPartyDialog = { showAddPartyDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
            }
        }

        // نافذة استعراض الفواتير والسندات
        if (uiState.showHistoryDialog) {
            PosHistoryDialog(
                uiState = uiState,
                onDismiss = { viewModel.toggleHistoryDialog(false) }
            )
        }

        // نافذة معاينة الإيصال بعد إتمام الفاتورة
        if (uiState.showReceiptDialog && uiState.lastCheckoutResult != null) {
            PosReceiptDialog(
                checkoutResult = uiState.lastCheckoutResult!!,
                onPrintAgain = {},
                onDismiss = { viewModel.dismissReceiptDialog() }
            )
        }

        // نافذة إضافة عميل أو مورد جديد سريعاً
        if (showAddPartyDialog) {
            val isForCustomer = uiState.activeOperation in listOf(PosOperation.SALE, PosOperation.SALE_RETURN, PosOperation.RECEIPT)
            AlertDialog(
                onDismissRequest = { showAddPartyDialog = false },
                title = {
                    Text(
                        if (isForCustomer) "إضافة عميل جديد للدفتر" else "إضافة مورد جديد",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newPartyName,
                            onValueChange = { newPartyName = it },
                            label = { Text(if (isForCustomer) "اسم العميل" else "اسم المورد / الشركة") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = newPartyPhone,
                            onValueChange = { newPartyPhone = it },
                            label = { Text("رقم الجوال") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true
                        )
                        if (isForCustomer) {
                            OutlinedTextField(
                                value = newPartyLimit,
                                onValueChange = { newPartyLimit = it },
                                label = { Text("سقف الدين المسموح به (ر.س)") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newPartyName.isNotBlank()) {
                                val type = if (isForCustomer) PartyType.CUSTOMER else PartyType.SUPPLIER
                                val limit = newPartyLimit.toDoubleOrNull() ?: 1000.0
                                viewModel.addNewParty(newPartyName, newPartyPhone, type, limit)
                                newPartyName = ""
                                newPartyPhone = ""
                                showAddPartyDialog = false
                            }
                        }
                    ) {
                        Text("حفظ")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddPartyDialog = false }) {
                        Text("إلغاء")
                    }
                }
            )
        }
    }
}

/**
 * قسم الفواتير والمردودات: الأصناف + السلة + أطراف التعامل
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PosInvoiceSection(
    uiState: PosUiState,
    viewModel: PosViewModel,
    onOpenAddPartyDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // قسم المنتجات (يمين)
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp)
            ) {
                // شريط البحث
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("ابحث بالاسم أو الباركود...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = null)
                            }
                        }
                    },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(6.dp))

                // تصنيفات سريعة
                val categories = listOf("الكل", "خضار وفواكه", "ألبان وأجبان", "مخبوزات", "معلبات ومواد غذائية")
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = uiState.selectedCategory == cat,
                            onClick = { viewModel.setSelectedCategory(cat) },
                            label = { Text(cat, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // شبكة الأصناف
                val filtered = uiState.productsWithUnits.filter { p ->
                    val query = uiState.searchQuery.trim().lowercase()
                    val matchesQuery = query.isEmpty() ||
                            p.product.name.lowercase().contains(query) ||
                            p.product.code.lowercase().contains(query) ||
                            p.units.any { it.barcode.lowercase().contains(query) }
                    val matchesCat = uiState.selectedCategory == "الكل" || p.product.category == uiState.selectedCategory
                    matchesQuery && matchesCat
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filtered) { pw ->
                        val baseUnit = pw.units.firstOrNull { it.isBaseUnit } ?: pw.units.firstOrNull()
                        val displayPrice = if (uiState.activeOperation in listOf(PosOperation.PURCHASE, PosOperation.PURCHASE_RETURN)) {
                            baseUnit?.costPrice ?: 0.0
                        } else {
                            baseUnit?.sellingPrice ?: 0.0
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(115.dp)
                                .clickable {
                                    if (baseUnit != null) {
                                        viewModel.addToCart(pw.product, baseUnit, 1.0)
                                    }
                                },
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = pw.product.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = pw.product.category,
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "%.2f ر.س".format(displayPrice),
                                        color = when (uiState.activeOperation) {
                                            PosOperation.PURCHASE -> Color(0xFF1976D2)
                                            PosOperation.SALE_RETURN, PosOperation.PURCHASE_RETURN -> Color(0xFFD32F2F)
                                            else -> Color(0xFF1B5E20)
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )

                                    Surface(
                                        color = Color(0xFFE8F5E9),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = baseUnit?.unitName ?: "حبة",
                                            fontSize = 10.sp,
                                            color = Color(0xFF2E7D32),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // قسم السلة وإتمام العملية (يسار)
        Card(
            modifier = Modifier
                .weight(1.15f)
                .fillMaxHeight(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                // ترويسة السلة مع اختيار الطرف (عميل / مورد)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = when (uiState.activeOperation) {
                                PosOperation.SALE -> "سلة فاتورة البيع"
                                PosOperation.PURCHASE -> "سلة فاتورة الشراء"
                                PosOperation.SALE_RETURN -> "سلة مردود البيع"
                                PosOperation.PURCHASE_RETURN -> "سلة مردود الشراء"
                                else -> "سلة الفاتورة"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "${uiState.cartSummary.itemsCount} صنف (${uiState.cartSummary.totalQuantity} كمية)",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }

                    if (uiState.cartItems.isNotEmpty()) {
                        TextButton(onClick = { viewModel.clearCart() }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تفريغ", color = Color.Red, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // اختيار الطرف (العميل في البيع، المورد في الشراء)
                val isCustomerMode = uiState.activeOperation in listOf(PosOperation.SALE, PosOperation.SALE_RETURN)
                val partyLabel = if (isCustomerMode) "العميل: " else "المورد: "

                var partyDropdownExpanded by remember { mutableStateOf(false) }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF0F4F8))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { partyDropdownExpanded = true },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (isCustomerMode) Icons.Default.Person else Icons.Default.LocalShipping,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = partyLabel + (uiState.selectedParty?.name ?: if (isCustomerMode) "عميل نقدي (كاش)" else "اختر المورد"),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            uiState.selectedParty?.let { p ->
                                val balText = if (p.currentBalance > 0) {
                                    "مدين لنا: %.2f ر.س".format(p.currentBalance)
                                } else if (p.currentBalance < 0) {
                                    "دائن له: %.2f ر.س".format(-p.currentBalance)
                                } else {
                                    "الرصيد: 0.00 ر.س"
                                }
                                Text(balText, fontSize = 10.sp, color = if (p.currentBalance > 0) Color(0xFFD32F2F) else Color(0xFF2E7D32))
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onOpenAddPartyDialog, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.PersonAdd, contentDescription = "إضافة", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }

                        IconButton(onClick = { partyDropdownExpanded = true }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }

                    // قائمة منسدلة لاختيار الطرف
                    DropdownMenu(
                        expanded = partyDropdownExpanded,
                        onDismissRequest = { partyDropdownExpanded = false }
                    ) {
                        if (isCustomerMode) {
                            DropdownMenuItem(
                                text = { Text("عميل نقدي (بدون حساب)") },
                                onClick = {
                                    viewModel.selectParty(null)
                                    partyDropdownExpanded = false
                                }
                            )
                        }

                        val eligibleParties = uiState.parties.filter {
                            if (isCustomerMode) it.type == PartyType.CUSTOMER || it.type == PartyType.BOTH
                            else it.type == PartyType.SUPPLIER || it.type == PartyType.BOTH
                        }

                        eligibleParties.forEach { party ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(party.name, fontWeight = FontWeight.Bold)
                                        Text(
                                            if (party.currentBalance > 0) "مدين: %.2f ر.س".format(party.currentBalance)
                                            else if (party.currentBalance < 0) "دائن: %.2f ر.س".format(-party.currentBalance)
                                            else "رصيد صفر",
                                            fontSize = 10.sp,
                                            color = Color.Gray
                                        )
                                    }
                                },
                                onClick = {
                                    viewModel.selectParty(party)
                                    partyDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // قائمة عناصر السلة الموسعة
                if (uiState.cartItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(40.dp), tint = Color.LightGray)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("السلة فارغة", color = Color.Gray, fontSize = 13.sp)
                            Text("اضغط على أي صنف لإضافته", color = Color.LightGray, fontSize = 11.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(uiState.cartItems) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFF0F4F8))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.productName, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        "%.2f × %.2f = %.2f ر.س".format(item.unitPrice, item.quantity, item.totalPrice),
                                        fontSize = 11.sp,
                                        color = Color.DarkGray
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { viewModel.updateCartQuantity(item.cartItemId, item.quantity - 1.0) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.RemoveCircleOutline, contentDescription = null, tint = Color.Gray)
                                    }

                                    Text(
                                        text = item.quantityFormatted,
                                        modifier = Modifier.padding(horizontal = 4.dp),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    IconButton(
                                        onClick = { viewModel.updateCartQuantity(item.cartItemId, item.quantity + 1.0) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = Color(0xFF1B5E20))
                                    }

                                    IconButton(
                                        onClick = { viewModel.removeCartItem(item.cartItemId) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.Red, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 6.dp))

                // طريقة السداد (نقداً، آجل، شبكة)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("طريقة الدفع:", fontWeight = FontWeight.Bold, fontSize = 12.sp)

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(
                            selected = uiState.paymentMethod == PaymentMethod.CASH,
                            onClick = { viewModel.setPaymentMethod(PaymentMethod.CASH) },
                            label = { Text("نقداً", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = uiState.paymentMethod == PaymentMethod.CREDIT,
                            onClick = { viewModel.setPaymentMethod(PaymentMethod.CREDIT) },
                            label = { Text("آجل (حساب)", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = uiState.paymentMethod == PaymentMethod.MADA,
                            onClick = { viewModel.setPaymentMethod(PaymentMethod.MADA) },
                            label = { Text("شبكة", fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // تفاصيل الإجمالي
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("المجموع قبل الضريبة:", fontSize = 12.sp, color = Color.Gray)
                    Text("%.2f ر.س".format(uiState.cartSummary.taxableAmount), fontSize = 12.sp)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("الضريبة المضافة (15%):", fontSize = 12.sp, color = Color.Gray)
                    Text("%.2f ر.س".format(uiState.cartSummary.taxAmount), fontSize = 12.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("المبلغ الإجمالي الصافي:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        "%.2f ر.س".format(uiState.cartSummary.finalTotal),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = when (uiState.activeOperation) {
                            PosOperation.PURCHASE -> Color(0xFF1976D2)
                            PosOperation.SALE_RETURN, PosOperation.PURCHASE_RETURN -> Color(0xFFD32F2F)
                            else -> Color(0xFF1B5E20)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { viewModel.executeInvoiceTransaction() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when (uiState.activeOperation) {
                            PosOperation.PURCHASE -> Color(0xFF1976D2)
                            PosOperation.SALE_RETURN, PosOperation.PURCHASE_RETURN -> Color(0xFFD32F2F)
                            else -> Color(0xFF1B5E20)
                        }
                    ),
                    enabled = uiState.cartItems.isNotEmpty() && !uiState.isProcessingCheckout
                ) {
                    if (uiState.isProcessingCheckout) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                    } else {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (uiState.activeOperation) {
                                PosOperation.PURCHASE -> "اعتماد وحفظ فاتورة الشراء"
                                PosOperation.SALE_RETURN -> "اعتماد وحفظ مردود البيع"
                                PosOperation.PURCHASE_RETURN -> "اعتماد وحفظ مردود الشراء"
                                else -> "إتمام عملية البيع وطباعة الإيصال"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * قسم السندات المالية (سند القبض وسند الصرف)
 * يتم إخفاء سلة المنتجات بالكامل وعرض نموذج محاسبي لإدخال السند وقيده بالصندوق والحسابات
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PosVoucherSection(
    uiState: PosUiState,
    viewModel: PosViewModel,
    onOpenAddPartyDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isReceipt = uiState.activeOperation == PosOperation.RECEIPT
    val primaryColor = if (isReceipt) Color(0xFF00796B) else Color(0xFFE65100)

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                // ترويسة السند
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = primaryColor.copy(alpha = 0.15f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    if (isReceipt) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                    contentDescription = null,
                                    tint = primaryColor,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isReceipt) "سند قبض نقدية (Receipt Voucher)" else "سند صرف نقدية (Payment / Expense Voucher)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = primaryColor
                            )
                            Text(
                                text = if (isReceipt) "استلام مبالغ وسداد ديون من عملاء الدفتر (يزيد النقدية ويقلل دين العميل)" else "سداد دفعات لمورد أو تسجيل مصروفات عامة ونثريات (يقلل النقدية في الدرج)",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    Surface(
                        color = if (isReceipt) Color(0xFFE0F2F1) else Color(0xFFFFF3E0),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = if (isReceipt) "وارد للصندوق (+)" else "صادر من الصندوق (-)",
                            color = primaryColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // الحقل 1: تحديد الطرف المستهدف (العميل أو المورد)
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isReceipt) "العميل المستلم منه:" else "المورد أو المستلم:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            TextButton(onClick = onOpenAddPartyDialog, contentPadding = PaddingValues(0.dp)) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(if (isReceipt) "عميل جديد" else "مورد جديد", fontSize = 11.sp)
                            }
                        }

                        var partyMenuExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = partyMenuExpanded,
                            onExpandedChange = { partyMenuExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = uiState.voucherParty?.let { "${it.name} (${if (it.currentBalance > 0) "مدين: %.2f".format(it.currentBalance) else "رصيد: %.2f".format(it.currentBalance)} ر.س)" }
                                    ?: if (isReceipt) "اختر العميل المسدد..." else "اختر المورد (أو اتركه لمصروف عام)...",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = partyMenuExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )

                            ExposedDropdownMenu(
                                expanded = partyMenuExpanded,
                                onDismissRequest = { partyMenuExpanded = false }
                            ) {
                                if (!isReceipt) {
                                    DropdownMenuItem(
                                        text = { Text("بدون مورد (مصروف تشغيلي عام ونثريات)") },
                                        onClick = {
                                            viewModel.selectVoucherParty(null)
                                            partyMenuExpanded = false
                                        }
                                    )
                                }

                                val parties = uiState.parties.filter {
                                    if (isReceipt) it.type == PartyType.CUSTOMER || it.type == PartyType.BOTH
                                    else it.type == PartyType.SUPPLIER || it.type == PartyType.BOTH
                                }

                                parties.forEach { party ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(party.name, fontWeight = FontWeight.Bold)
                                                Text(
                                                    if (party.currentBalance > 0) "مدين لنا: %.2f ر.س".format(party.currentBalance)
                                                    else if (party.currentBalance < 0) "دائن له: %.2f ر.س".format(-party.currentBalance)
                                                    else "الرصيد: 0.00 ر.س",
                                                    fontSize = 11.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                        },
                                        onClick = {
                                            viewModel.selectVoucherParty(party)
                                            partyMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // الحقل 2: المبلغ المسدد أو المصروف
                    Column(modifier = Modifier.weight(1f)) {
                        Text("مبلغ السند (المبلغ المدفوع):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = uiState.voucherAmountInput,
                            onValueChange = { viewModel.updateVoucherAmount(it) },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("0.00") },
                            suffix = { Text("ر.س", fontWeight = FontWeight.Bold, color = primaryColor) },
                            leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null, tint = primaryColor) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // إذا كان سند صرف وبدون مورد: عرض تصنيف المصروف
                if (!isReceipt && uiState.voucherParty == null) {
                    Text("تصنيف المصروف:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    val categories = listOf("كهرباء ومياه", "إيجار", "رواتب وعمالة", "نظافة ومستلزمات", "صيانة ومعدات", "بوفية وضيافة", "نثريات ومستلزمات")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(categories) { cat ->
                            FilterChip(
                                selected = uiState.voucherCategory == cat,
                                onClick = { viewModel.updateVoucherCategory(cat) },
                                label = { Text(cat, fontSize = 12.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("المدفوع له (الجهة أو الشخص المستلم):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = uiState.voucherPaidToInput,
                        onValueChange = { viewModel.updateVoucherPaidTo(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("مثال: شركة الكهرباء / عامل النظافة...") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // طريقة الدفع (نقداً، شبكة، تحويل بنكي)
                Text("طريقة السداد / القبض:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = uiState.voucherPaymentMethod == PaymentMethod.CASH,
                        onClick = { viewModel.updateVoucherPaymentMethod(PaymentMethod.CASH) },
                        label = { Text("نقداً من الصندوق") },
                        leadingIcon = { Icon(Icons.Default.Money, contentDescription = null) }
                    )
                    FilterChip(
                        selected = uiState.voucherPaymentMethod == PaymentMethod.MADA,
                        onClick = { viewModel.updateVoucherPaymentMethod(PaymentMethod.MADA) },
                        label = { Text("شبكة مدى") },
                        leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null) }
                    )
                    FilterChip(
                        selected = uiState.voucherPaymentMethod == PaymentMethod.BANK_TRANSFER,
                        onClick = { viewModel.updateVoucherPaymentMethod(PaymentMethod.BANK_TRANSFER) },
                        label = { Text("تحويل بنكي") },
                        leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = null) }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // بيان وملاحظات السند
                Text("بيان السند والملاحظات المحاسبية:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = uiState.voucherNotesInput,
                    onValueChange = { viewModel.updateVoucherNotes(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("اكتب شرحاً مختصراً للعملية لتوثيقها في القيود المحاسبية...") },
                    maxLines = 2
                )
            }

            // زر الحفظ والاعتماد
            Button(
                onClick = { viewModel.executeVoucherTransaction() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                enabled = !uiState.isVoucherSubmitting
            ) {
                if (uiState.isVoucherSubmitting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isReceipt) "تسجيل وحفظ سند القبض وتحديث حساب العميل" else "تسجيل وحفظ سند الصرف وخصمه من الصندوق",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

/**
 * نافذة استعراض الفواتير والسجلات التاريخية
 */
@Composable
private fun PosHistoryDialog(
    uiState: PosUiState,
    onDismiss: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf("ALL") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("سجل الحركات والعمليات المالية", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق")
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // تصفية السجلات
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedFilter == "ALL",
                            onClick = { selectedFilter = "ALL" },
                            label = { Text("الكل", fontSize = 11.sp) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedFilter == "SALE",
                            onClick = { selectedFilter = "SALE" },
                            label = { Text("مبيعات", fontSize = 11.sp) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedFilter == "PURCHASE",
                            onClick = { selectedFilter = "PURCHASE" },
                            label = { Text("مشتريات", fontSize = 11.sp) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedFilter == "RETURNS",
                            onClick = { selectedFilter = "RETURNS" },
                            label = { Text("مردودات", fontSize = 11.sp) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedFilter == "VOUCHERS",
                            onClick = { selectedFilter = "VOUCHERS" },
                            label = { Text("سندات", fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val filteredRecords = uiState.transactionRecords.filter { rec ->
                    when (selectedFilter) {
                        "SALE" -> rec.operation == PosOperation.SALE
                        "PURCHASE" -> rec.operation == PosOperation.PURCHASE
                        "RETURNS" -> rec.operation in listOf(PosOperation.SALE_RETURN, PosOperation.PURCHASE_RETURN)
                        "VOUCHERS" -> rec.operation in listOf(PosOperation.RECEIPT, PosOperation.EXPENSE)
                        else -> true
                    }
                }

                if (filteredRecords.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("لا توجد سجلات مسجلة مطابقة للتصفية", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredRecords) { record ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(record.id, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                color = when (record.operation) {
                                                    PosOperation.SALE -> Color(0xFFE8F5E9)
                                                    PosOperation.PURCHASE -> Color(0xFFE3F2FD)
                                                    PosOperation.SALE_RETURN, PosOperation.PURCHASE_RETURN -> Color(0xFFFFEBEE)
                                                    PosOperation.RECEIPT -> Color(0xFFE0F2F1)
                                                    PosOperation.EXPENSE -> Color(0xFFFFF3E0)
                                                },
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    record.operation.titleArabic,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = when (record.operation) {
                                                        PosOperation.SALE -> Color(0xFF1B5E20)
                                                        PosOperation.PURCHASE -> Color(0xFF1976D2)
                                                        PosOperation.SALE_RETURN, PosOperation.PURCHASE_RETURN -> Color(0xFFD32F2F)
                                                        PosOperation.RECEIPT -> Color(0xFF00796B)
                                                        PosOperation.EXPENSE -> Color(0xFFE65100)
                                                    },
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text(record.partyName, fontSize = 11.sp, color = Color.DarkGray)
                                        Text(record.date, fontSize = 10.sp, color = Color.Gray)
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            "%.2f ر.س".format(record.amount),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = when (record.operation) {
                                                PosOperation.SALE, PosOperation.RECEIPT -> Color(0xFF1B5E20)
                                                PosOperation.PURCHASE -> Color(0xFF1976D2)
                                                else -> Color(0xFFD32F2F)
                                            }
                                        )
                                        Text(record.paymentMethod.labelArabic, fontSize = 10.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}
