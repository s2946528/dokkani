package com.example.dokkani.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LayersClear
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.dokkani.ui.components.BarcodeTextField
import com.example.dokkani.ui.screens.crud.AddEditProductDialog
import com.example.dokkani.ui.screens.crud.AddEditUnitDialog
import com.example.dokkani.ui.screens.crud.ConfirmDeleteDialog

data class FlattenedUnitItem(
    val product: ProductEntity,
    val unit: ProductUnitEntity
)

@Composable
fun ProductsAndUnitsScreen(
    productsWithUnits: List<ProductWithUnits>,
    currentUserRole: UserRole = UserRole.ADMIN,
    onSaveProduct: (ProductEntity, String, Double, Double, String, Boolean) -> Unit = { _, _, _, _, _, _ -> },
    onDeleteProduct: (Long) -> Unit = {},
    onSaveUnit: (ProductUnitEntity) -> Unit = {},
    onDeleteUnit: (ProductUnitEntity) -> Unit = {},
    onRenameCategory: (oldName: String, newName: String) -> Unit = { _, _ -> },
    onDeleteCategory: (categoryName: String, reassignTo: String) -> Unit = { _, _ -> },
    onPrintLabel: ((productId: Long, unitId: Long) -> Unit)? = null,
    currencySymbol: String = "ر.ي",
    modifier: Modifier = Modifier
) {
    // التبويبات المحدثة بالترتيب والأسماء المطلوبة: 0: الأصناف، 1: الوحدات، 2: التصنيفات
    var selectedTab by remember { mutableIntStateOf(0) }

    var showAddProductDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var deletingProductId by remember { mutableStateOf<Long?>(null) }

    var showSelectProductForNewUnitDialog by remember { mutableStateOf(false) }
    var addingUnitProductId by remember { mutableStateOf<Long?>(null) }
    var editingUnit by remember { mutableStateOf<ProductUnitEntity?>(null) }
    var deletingUnit by remember { mutableStateOf<ProductUnitEntity?>(null) }

    // إدارة التصنيفات
    var customCategories by remember {
        mutableStateOf(
            listOf(
                "خضار وفواكه",
                "ألبان وأجبان",
                "مخبوزات",
                "معلبات ومواد غذائية",
                "مشروبات ومياه",
                "حلويات وتسالي",
                "منظفات ومستلزمات منزلية",
                "عناية شخصية",
                "تموينات عامة"
            )
        )
    }

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var editingCategoryName by remember { mutableStateOf<String?>(null) }
    var deletingCategoryName by remember { mutableStateOf<String?>(null) }

    val allCategories = remember(productsWithUnits, customCategories) {
        (customCategories + productsWithUnits.map { it.product.category.trim() })
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

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
                        item.product.category.lowercase().contains(q) ||
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
        // شريط التبويبات بالترتيب والأسماء المطلوبة: الأصناف، الوحدات، التصنيفات
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
                        Text("الأصناف", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(6.dp))
                        BadgeCount(productsWithUnits.size)
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
                        Text("الوحدات", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(6.dp))
                        BadgeCount(allFlattenedUnits.size)
                    }
                }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("التصنيفات", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(6.dp))
                        BadgeCount(allCategories.size)
                    }
                }
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTab) {
                0 -> {
                    // ==========================================
                    // التبويب الأول: الأصناف (عرض وإدارة الأصناف)
                    // ==========================================
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
                                            text = "دليل المنتجات والأصناف المسجلة",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "إدارة كاملة للأصناف ومتابعة أسعار التكلفة والبيع للوحدات المعتمدة وإعادة التعبئة.",
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
                                label = "بحث بالاسم، الكود، أو القسم أو مسح الباركود",
                                placeholder = "امسح باركود الصنف أو ابحث بالاسم...",
                                onBarcodeScanned = { scannedCode -> searchQuery = scannedCode },
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
                                    text = "قائمة الأصناف المسجلة (${filteredProducts.size}):",
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
                }

                1 -> {
                    // ==========================================
                    // التبويب الثاني: الوحدات العامة (إدارة الوحدات العامة بالكامل)
                    // ==========================================
                    val allUnits = remember(productsWithUnits) {
                        productsWithUnits.flatMap { it.units }
                            .distinctBy { if (it.id > 0) it.id else it.unitName.trim().lowercase() }
                    }
                    UnitsManagementScreen(
                        units = allUnits,
                        currentUserRole = currentUserRole,
                        onSaveUnit = onSaveUnit,
                        onDeleteUnit = onDeleteUnit
                    )
                }

                2 -> {
                    // ==========================================
                    // التبويب الثالث: التصنيفات (إدارة تصنيفات المنتجات)
                    // ==========================================
                    var categorySearchQuery by remember { mutableStateOf("") }

                    val filteredCategories = remember(allCategories, categorySearchQuery) {
                        if (categorySearchQuery.isBlank()) allCategories else allCategories.filter {
                            it.contains(categorySearchQuery.trim(), ignoreCase = true)
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // كارت إحصائيات وإضافة تصنيف
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
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
                                                imageVector = Icons.Default.Category,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                                modifier = Modifier.size(28.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = "دليل وإدارة تصنيفات الأصناف",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                                )
                                                Text(
                                                    text = "إضافة وتعديل التصنيفات مع المعالجة الآمنة للأصناف المرتبطة",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                                                )
                                            }
                                        }

                                        if (isAdmin) {
                                            Button(
                                                onClick = { showAddCategoryDialog = true },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("إضافة تصنيف")
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Surface(
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.surface
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("إجمالي التصنيفات", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                                Text("${allCategories.size}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            }
                                        }

                                        val activeProductsWithCategoriesCount = productsWithUnits.count { it.product.category.isNotBlank() }
                                        Surface(
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.surface
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("الأصناف المصنفة", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                                Text("$activeProductsWithCategoriesCount", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // البحث عن تصنيف
                        item {
                            OutlinedTextField(
                                value = categorySearchQuery,
                                onValueChange = { categorySearchQuery = it },
                                label = { Text("بحث باسم التصنيف") },
                                placeholder = { Text("اكتب اسم التصنيف (ألبان، معلبات، خضار...)") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true
                            )
                        }

                        item {
                            Text(
                                text = "قائمة التصنيفات (${filteredCategories.size}):",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (filteredCategories.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("لا توجد تصنيفات مطابقة للبحث", color = Color.Gray)
                                }
                            }
                        } else {
                            items(filteredCategories) { catName ->
                                val attachedProducts = productsWithUnits.filter { it.product.category.trim() == catName.trim() }
                                var isExpanded by remember { mutableStateOf(false) }

                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = MaterialTheme.colorScheme.primaryContainer,
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            Icons.Default.Folder,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(
                                                        text = catName,
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = "مرتبط بـ ${attachedProducts.size} أصناف",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.outline
                                                    )
                                                }
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (attachedProducts.isNotEmpty()) {
                                                    TextButton(
                                                        onClick = { isExpanded = !isExpanded },
                                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                                    ) {
                                                        Text(
                                                            if (isExpanded) "إخفاء الأصناف" else "عرض الأصناف",
                                                            fontSize = 11.sp
                                                        )
                                                        Icon(
                                                            if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }

                                                if (isAdmin) {
                                                    IconButton(
                                                        onClick = { editingCategoryName = catName },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Default.Edit,
                                                            contentDescription = "تعديل التصنيف",
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }

                                                    IconButton(
                                                        onClick = { deletingCategoryName = catName },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Default.Delete,
                                                            contentDescription = "حذف التصنيف",
                                                            tint = MaterialTheme.colorScheme.error,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // عرض الأصناف التابعة عند التوسيع
                                        if (isExpanded && attachedProducts.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(10.dp))
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                            Spacer(modifier = Modifier.height(8.dp))

                                            Text(
                                                "الأصناف التابعة لهذا التصنيف:",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))

                                            attachedProducts.forEach { pwu ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 3.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            Icons.Default.Inventory2,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(14.dp),
                                                            tint = MaterialTheme.colorScheme.outline
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(pwu.product.name, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                                    }
                                                    Text(pwu.product.code, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline, fontFamily = FontFamily.Monospace)
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

    // ==========================================
    // الحوارات والنافذة التفاعلية (Dialogs)
    // ==========================================

    // نافذة اختيار الصنف لإضافة وحدة من تبويب الوحدات
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

    // إضافة أو تعديل صنف
    if (showAddProductDialog || editingProduct != null) {
        AddEditProductDialog(
            initialProduct = editingProduct,
            onSaveProduct = { prod, baseName, cost, sell, barcode, isBaseUnit ->
                onSaveProduct(prod, baseName, cost, sell, barcode, isBaseUnit)
                showAddProductDialog = false
                editingProduct = null
            },
            onDismiss = {
                showAddProductDialog = false
                editingProduct = null
            }
        )
    }

    // حذف صنف
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

    // إضافة أو تعديل وحدة
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

    // حذف وحدة
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

    // إضافة / تعديل تصنيف
    if (showAddCategoryDialog || editingCategoryName != null) {
        AddEditCategoryDialog(
            initialName = editingCategoryName ?: "",
            onSave = { newName ->
                val trimmed = newName.trim()
                if (trimmed.isNotBlank()) {
                    if (editingCategoryName != null) {
                        val old = editingCategoryName!!
                        customCategories = customCategories.map { if (it == old) trimmed else it }.distinct()
                        onRenameCategory(old, trimmed)
                    } else {
                        customCategories = (customCategories + trimmed).distinct()
                    }
                }
                showAddCategoryDialog = false
                editingCategoryName = null
            },
            onDismiss = {
                showAddCategoryDialog = false
                editingCategoryName = null
            }
        )
    }

    // حذف تصنيف وتأمين الأصناف التابعة
    if (deletingCategoryName != null) {
        val catName = deletingCategoryName!!
        val attachedCount = productsWithUnits.count { it.product.category.trim() == catName.trim() }

        ConfirmDeleteCategoryDialog(
            categoryName = catName,
            attachedProductsCount = attachedCount,
            onConfirm = { fallbackCat ->
                customCategories = customCategories.filter { it != catName }
                onDeleteCategory(catName, fallbackCat)
                deletingCategoryName = null
            },
            onDismiss = { deletingCategoryName = null }
        )
    }
}

@Composable
private fun BadgeCount(count: Int) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.padding(2.dp)
    ) {
        Text(
            text = "$count",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
    currencySymbol: String = "ر.ي"
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

@Composable
private fun AddEditCategoryDialog(
    initialName: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initialName.isBlank()) "إضافة تصنيف جديد" else "تعديل اسم التصنيف",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم التصنيف *") },
                    placeholder = { Text("مثال: ألبان وأجبان، منظفات...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name) },
                enabled = name.isNotBlank(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
private fun ConfirmDeleteCategoryDialog(
    categoryName: String,
    attachedProductsCount: Int,
    onConfirm: (fallbackCategory: String) -> Unit,
    onDismiss: () -> Unit
) {
    var fallbackCategory by remember { mutableStateOf("عام") }

    AlertDialog(
        onDismissRequest = onDismiss,
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
                text = "حذف التصنيف '$categoryName'",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
                fontSize = 16.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "هل أنت متأكد من رغبتك في حذف هذا التصنيف؟",
                    style = MaterialTheme.typography.bodyMedium
                )

                if (attachedProductsCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "تنبيه أمان:",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "يوجد حالياً $attachedProductsCount أصناف مرتبطة بهذا التصنيف. لمنع فقدان بياناتها، سيتم إعادة تحويل هذه الأصناف تلقائياً للتصنيف التالي:",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = fallbackCategory,
                                onValueChange = { fallbackCategory = it },
                                label = { Text("التصنيف البديل للأصناف") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(6.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(fallbackCategory) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("تأكيد الحذف والمعالجة")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) {
                Text("إلغاء")
            }
        }
    )
}
