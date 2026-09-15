package com.example.dokkani.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dokkani.data.local.entities.ProductEntity
import com.example.dokkani.data.local.entities.ProductUnitEntity
import com.example.dokkani.data.local.entities.ProductWithUnits
import com.example.dokkani.data.local.entities.UserRole
import com.example.dokkani.ui.screens.crud.AddEditProductDialog
import com.example.dokkani.ui.screens.crud.AddEditUnitDialog
import com.example.dokkani.ui.screens.crud.ConfirmDeleteDialog
import com.example.dokkani.ui.components.BarcodeTextField

data class FlattenedUnitItem(
    val product: ProductEntity,
    val unit: ProductUnitEntity
)

@Composable
fun ProductsAndUnitsScreen(
    productsWithUnits: List<ProductWithUnits>,
    currentUserRole: UserRole = UserRole.ADMIN,
    onSaveProduct: (ProductEntity, String, Double, Double, String) -> Unit = { _, _, _, _, _ -> },
    onDeleteProduct: (Long) -> Unit = {},
    onSaveUnit: (ProductUnitEntity) -> Unit = {},
    onDeleteUnit: (ProductUnitEntity) -> Unit = {},
    onPrintLabel: ((productId: Long, unitId: Long) -> Unit)? = null,
    currencySymbol: String = "ر.س",
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    var showAddProductDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var deletingProductId by remember { mutableStateOf<Long?>(null) }

    var showSelectProductForNewUnitDialog by remember { mutableStateOf(false) }
    var addingUnitProductId by remember { mutableStateOf<Long?>(null) }
    var editingUnit by remember { mutableStateOf<ProductUnitEntity?>(null) }
    var deletingUnit by remember { mutableStateOf<ProductUnitEntity?>(null) }

    val isAdmin = currentUserRole == UserRole.ADMIN
    var searchQuery by remember { mutableStateOf("") }

    val filteredProducts = remember(productsWithUnits, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) {
            productsWithUnits
        } else {
            productsWithUnits.filter { item ->
                item.product.name.lowercase().contains(q) ||
                        item.product.code.lowercase().contains(q) ||
                        item.units.any { it.barcode.lowercase().contains(q) }
            }
        }
    }

    // تسطيح قائمة الوحدات لتبويب إدارة الوحدات
    val allFlattenedUnits = remember(productsWithUnits) {
        productsWithUnits.flatMap { pwu ->
            pwu.units.map { u -> FlattenedUnitItem(product = pwu.product, unit = u) }
        }
    }

    var unitFilterType by remember { mutableIntStateOf(0) } // 0: الكل, 1: الأساسية, 2: التجزئة والكراتين
    var unitSearchQuery by remember { mutableStateOf("") }

    val filteredUnits = remember(allFlattenedUnits, unitSearchQuery, unitFilterType) {
        val q = unitSearchQuery.trim().lowercase()
        allFlattenedUnits.filter { item ->
            val matchQuery = if (q.isEmpty()) true else {
                item.unit.unitName.lowercase().contains(q) ||
                        item.product.name.lowercase().contains(q) ||
                        item.product.code.lowercase().contains(q) ||
                        item.unit.barcode.lowercase().contains(q)
            }
            val matchType = when (unitFilterType) {
                1 -> item.unit.isBaseUnit
                2 -> !item.unit.isBaseUnit
                else -> true
            }
            matchQuery && matchType
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // شريط التبويب الرئيسي بين الأصناف وإدارة الوحدات
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Inventory, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("دليل الأصناف والمنتجات", fontWeight = FontWeight.Bold)
                    }
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Scale, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إدارة وحدات القياس والعبوات", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.padding(2.dp)
                        ) {
                            Text(
                                text = "${allFlattenedUnits.size}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (selectedTab == 0) {
                // تبويب الأصناف والمنتجات
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Inventory,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "الأصناف والوحدات المتعددة (Products & Units)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "يدعم النظام تعيين وحدات متعددة لكل صنف (حبة، درزن، كرتون، كيلو، صندوق...) مع معامل التحويل إلى الوحدة الأساسية وباركود وسعر بيع وشراء مستقل لكل وحدة.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }

                    item {
                        BarcodeTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = "بحث بالاسم أو مسح الباركود",
                            placeholder = "امسح باركود الصنف للوصول السريع...",
                            onBarcodeScanned = { scannedCode ->
                                searchQuery = scannedCode
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "قائمة الأصناف المسجلة بالمتجر (${filteredProducts.size}):",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )

                            if (isAdmin) {
                                Button(
                                    onClick = { showAddProductDialog = true },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("إضافة صنف جديد")
                                }
                            }
                        }
                    }

                    items(filteredProducts) { item ->
                        val p = item.product
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth().testTag("product_card_${p.id}")
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = CircleShape,
                                            color = if (p.isWeighted) Color(0xFFD1E7DD) else MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = if (p.isWeighted) Icons.Default.Scale else Icons.Default.Category,
                                                    contentDescription = null,
                                                    tint = if (p.isWeighted) Color(0xFF0F5132) else MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = p.name,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${p.code} • ${p.category} • ${if (p.isWeighted) "يباع بالوزن/الميزان" else "بالقطعة/العبوة"}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier.padding(4.dp)
                                        ) {
                                            Text(
                                                text = "${item.units.size} وحدات",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }

                                        if (isAdmin) {
                                            IconButton(
                                                onClick = { editingProduct = p },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "تعديل الصنف",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }

                                            IconButton(
                                                onClick = { deletingProductId = p.id },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "حذف الصنف",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "الوحدات وأسعار الصرف والباركود:",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.outline
                                    )

                                    if (isAdmin) {
                                        TextButton(
                                            onClick = { addingUnitProductId = p.id },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text("+ إضافة وحدة", fontSize = 12.sp)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                for (unit in item.units) {
                                    UnitItemRow(
                                        productId = item.product.id,
                                        unit = unit,
                                        isAdmin = isAdmin,
                                        onEditUnit = { editingUnit = it },
                                        onDeleteUnit = { deletingUnit = it },
                                        onPrintLabel = onPrintLabel,
                                        currencySymbol = currencySymbol
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                            }
                        }
                    }
                }
            } else {
                // تبويب إدارة وحدات القياس والعبوات المستقل
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // بطاقة إحصائيات وحدات القياس
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Layers,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(26.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "إدارة وحدات القياس والعبوات",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    if (isAdmin) {
                                        Button(
                                            onClick = { showSelectProductForNewUnitDialog = true },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("إضافة وحدة جديدة", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // لوحة مؤشرات الأرقام
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    val baseUnitsCount = allFlattenedUnits.count { it.unit.isBaseUnit }
                                    val subUnitsCount = allFlattenedUnits.count { !it.unit.isBaseUnit }

                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surface
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("إجمالي الوحدات", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                            Text("${allFlattenedUnits.size}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }

                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surface
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("وحدات أساسية", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                            Text("$baseUnitsCount", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
                                        }
                                    }

                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surface
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("كراتين وعبوات", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                            Text("$subUnitsCount", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // البحث ومسح الباركود
                    item {
                        BarcodeTextField(
                            value = unitSearchQuery,
                            onValueChange = { unitSearchQuery = it },
                            label = "بحث باسم الوحدة أو الصنف أو مسح باركود الوحدة",
                            placeholder = "اكتب (كرتون، حبة، درزن...) أو امسح الباركود...",
                            onBarcodeScanned = { scannedCode ->
                                unitSearchQuery = scannedCode
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // أزرار تصفية سريعة
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = unitFilterType == 0,
                                onClick = { unitFilterType = 0 },
                                label = { Text("جميع الوحدات (${allFlattenedUnits.size})") }
                            )
                            FilterChip(
                                selected = unitFilterType == 1,
                                onClick = { unitFilterType = 1 },
                                label = { Text("الأساسية فقط") }
                            )
                            FilterChip(
                                selected = unitFilterType == 2,
                                onClick = { unitFilterType = 2 },
                                label = { Text("الكراتين والعبوات") }
                            )
                        }
                    }

                    item {
                        Text(
                            text = "نتائج الوحدات والعبوات المعتمدة (${filteredUnits.size}):",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (filteredUnits.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.LayersClear, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.LightGray)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("لا توجد وحدات قياس تطابق معايير البحث", fontSize = 13.sp, color = Color.Gray)
                                }
                            }
                        }
                    } else {
                        items(filteredUnits) { item ->
                            val unit = item.unit
                            val prod = item.product
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, if (unit.isBaseUnit) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (unit.isBaseUnit) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                                            ) {
                                                Text(
                                                    text = unit.unitName,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (unit.isBaseUnit) Color(0xFF2E7D32) else Color(0xFFE65100),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            if (unit.isBaseUnit) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = MaterialTheme.colorScheme.primary
                                                ) {
                                                    Text(
                                                        text = "وحدة أساسية",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            } else {
                                                Text(
                                                    text = "معامل التحويل: ${unit.conversionFactor}",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.outline,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }

                                        // أزرار التحكم: تعديل، حذف، طباعة
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (onPrintLabel != null) {
                                                IconButton(
                                                    onClick = { onPrintLabel(prod.id, unit.id) },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Print,
                                                        contentDescription = "طباعة ملصق",
                                                        tint = Color(0xFF198754),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }

                                            if (isAdmin) {
                                                IconButton(
                                                    onClick = { editingUnit = unit },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = "تعديل الوحدة",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }

                                                IconButton(
                                                    onClick = { deletingUnit = unit },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "حذف الوحدة",
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // بيانات الصنف التابع له
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.outline)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "الصنف التابع له: ${prod.name} (${prod.category})",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    if (unit.barcode.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color(0xFF1976D2))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "الباركود: ${unit.barcode}",
                                                fontSize = 12.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = Color(0xFF1976D2),
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    Spacer(modifier = Modifier.height(8.dp))

                                    // جدول أسعار الشراء والبيع وهامش الربح
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("سعر التكلفة والشراء:", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                            Text(
                                                "%.2f %s".format(unit.costPrice, currencySymbol),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        Column {
                                            Text("سعر البيع المعتمد:", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                            Text(
                                                "%.2f %s".format(unit.sellingPrice, currencySymbol),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF2E7D32)
                                            )
                                        }

                                        val margin = if (unit.costPrice > 0) ((unit.sellingPrice - unit.costPrice) / unit.costPrice) * 100 else 0.0
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("هامش الربح التقديري:", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                            Text(
                                                "%.1f%%".format(margin),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (margin >= 0) Color(0xFF1976D2) else MaterialTheme.colorScheme.error
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

    // نافذة اختيار الصنف عند الرغبة في إضافة وحدة جديدة من تبويب إدارة الوحدات
    if (showSelectProductForNewUnitDialog) {
        SelectProductForUnitDialog(
            products = productsWithUnits.map { it.product },
            onSelectProduct = { prod ->
                showSelectProductForNewUnitDialog = false
                addingUnitProductId = prod.id
            },
            onDismiss = { showSelectProductForNewUnitDialog = false }
        )
    }

    // Dialogs
    if (showAddProductDialog || editingProduct != null) {
        AddEditProductDialog(
            initialProduct = editingProduct,
            onSaveProduct = { prod, baseName, cost, sell, barcode ->
                onSaveProduct(prod, baseName, cost, sell, barcode)
                showAddProductDialog = false
                editingProduct = null
            },
            onDismiss = {
                showAddProductDialog = false
                editingProduct = null
            }
        )
    }

    if (deletingProductId != null) {
        ConfirmDeleteDialog(
            message = "هل أنت متأكد من رغبتك في حذف هذا الصنف وجميع وحداته التابعة له؟",
            onConfirm = {
                onDeleteProduct(deletingProductId!!)
                deletingProductId = null
            },
            onDismiss = { deletingProductId = null }
        )
    }

    if (addingUnitProductId != null || editingUnit != null) {
        AddEditUnitDialog(
            productId = addingUnitProductId ?: editingUnit?.productId ?: 0L,
            initialUnit = editingUnit,
            onSaveUnit = { u ->
                onSaveUnit(u)
                addingUnitProductId = null
                editingUnit = null
            },
            onDismiss = {
                addingUnitProductId = null
                editingUnit = null
            }
        )
    }

    if (deletingUnit != null) {
        ConfirmDeleteDialog(
            message = "هل أنت متأكد من حذف الوحدة '${deletingUnit?.unitName}'؟",
            onConfirm = {
                onDeleteUnit(deletingUnit!!)
                deletingUnit = null
            },
            onDismiss = { deletingUnit = null }
        )
    }
}

@Composable
private fun SelectProductForUnitDialog(
    products: List<ProductEntity>,
    onSelectProduct: (ProductEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var search by remember { mutableStateOf("") }
    val filtered = remember(products, search) {
        if (search.isBlank()) products else products.filter {
            it.name.contains(search.trim(), ignoreCase = true) || it.code.contains(search.trim(), ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("اختر الصنف المراد إضافة وحدة جديدة له", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    label = { Text("بحث عن الصنف بالاسم أو الكود") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                )
                Spacer(modifier = Modifier.height(10.dp))

                if (filtered.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                        Text("لا يوجد أصناف مطابقة", color = Color.Gray, fontSize = 12.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filtered) { prod ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectProduct(prod) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(prod.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("${prod.code} • ${prod.category}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                    }
                                    Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
private fun UnitItemRow(
    productId: Long,
    unit: ProductUnitEntity,
    isAdmin: Boolean,
    onEditUnit: (ProductUnitEntity) -> Unit,
    onDeleteUnit: (ProductUnitEntity) -> Unit,
    onPrintLabel: ((productId: Long, unitId: Long) -> Unit)? = null,
    currencySymbol: String = "ر.س"
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (unit.isBaseUnit) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = if (unit.isBaseUnit) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = unit.unitName,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (unit.isBaseUnit) FontWeight.Bold else FontWeight.Medium
                )
                if (unit.isBaseUnit) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(0.dp)
                    ) {
                        Text(
                            text = "أساسية",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "(معامل: ${unit.conversionFactor})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.QrCode,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = unit.barcode.ifBlank { "—" },
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "%.2f %s".format(unit.sellingPrice, currencySymbol),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F5132)
                )

                if (onPrintLabel != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = { onPrintLabel(productId, unit.id) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = "طباعة ملصق",
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFF198754)
                        )
                    }
                }

                if (isAdmin) {
                    IconButton(
                        onClick = { onEditUnit(unit) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "تعديل الوحدة",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = { onDeleteUnit(unit) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "حذف الوحدة",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
