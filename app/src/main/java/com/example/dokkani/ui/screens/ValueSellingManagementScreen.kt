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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dokkani.data.local.entities.UserRole
import com.example.dokkani.data.local.entities.StockGroupAuditEntity
import com.example.dokkani.data.local.entities.StockGroupEntity
import com.example.dokkani.data.local.entities.StockGroupWithDetails
import com.example.dokkani.data.local.entities.CostValuationMethod
import com.example.dokkani.data.local.entities.ShortageSettlementEntity
import com.example.dokkani.data.local.entities.CostCenterEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ValueSellingManagementScreen(
    viewModel: ValueSellingViewModel = viewModel(),
    currentUserRole: UserRole = UserRole.ADMIN,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("إدارة البيع بالقيمة والمجموعات المخزنية", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("التحكم الكامل بالمجموعات والجرد الدوري", fontSize = 11.sp, color = Color.LightGray)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF133E32),
                        titleContentColor = Color.White
                    ),
                    actions = {
                        IconButton(onClick = { viewModel.openAddGroupDialog() }) {
                            Icon(Icons.Default.Add, contentDescription = "إضافة مجموعة جديدة", tint = Color.White)
                        }
                    }
                )
            },
            snackbarHost = {
                uiState.feedbackMessage?.let { msg ->
                    Snackbar(
                        modifier = Modifier.padding(16.dp),
                        containerColor = if (uiState.isErrorFeedback) MaterialTheme.colorScheme.errorContainer else Color(0xFF1B5E20),
                        contentColor = if (uiState.isErrorFeedback) MaterialTheme.colorScheme.onErrorContainer else Color.White,
                        action = {
                            TextButton(onClick = { viewModel.dismissFeedback() }) {
                                Text("حسناً", color = Color.White)
                            }
                        }
                    ) {
                        Text(msg)
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
                // شريط اختيار المجموعة المخزنية النشطة
                Surface(
                    color = Color(0xFF1B4D3E),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("المجموعات المخزنية المسجلة (${uiState.groupsWithDetails.size}):", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Button(
                                onClick = { viewModel.openAddGroupDialog() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("مجموعة جديدة", fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(uiState.groupsWithDetails) { details ->
                                val isSelected = details.group.id == uiState.selectedGroupId
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.selectGroup(details.group.id) },
                                    label = {
                                        Text(
                                            text = "${details.group.name} (${details.items.size} أصناف)",
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 12.sp
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF81C784),
                                        selectedLabelColor = Color(0xFF003300),
                                        containerColor = Color.White.copy(alpha = 0.15f),
                                        labelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }

                // التبويبات الرئيسية الثلاثة
                PrimaryTabRow(
                    selectedTabIndex = uiState.activeTab,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Tab(
                        selected = uiState.activeTab == 0,
                        onClick = { viewModel.setActiveTab(0) },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("المجموعات والأصناف", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                    Tab(
                        selected = uiState.activeTab == 1,
                        onClick = { viewModel.setActiveTab(1) },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.FactCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("الجرد الدوري و COGS", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                    Tab(
                        selected = uiState.activeTab == 2,
                        onClick = { viewModel.setActiveTab(2) },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MonetizationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تسوية العجز والبيع بالقيمة", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    when (uiState.activeTab) {
                        0 -> GroupsAndItemsView(uiState = uiState, viewModel = viewModel)
                        1 -> GroupAuditCogsView(uiState = uiState, viewModel = viewModel, currentUserRole = currentUserRole, dateFormat = dateFormat)
                        2 -> ShortageSettlementView(uiState = uiState, viewModel = viewModel, currentUserRole = currentUserRole, dateFormat = dateFormat)
                    }
                }
            }

            // نافذة اعتماد وتسوية العجز تحويله لمبيعات بالقيمة
            if (uiState.showSettlementConfirmDialog && uiState.selectedShortageForSettlement != null) {
                ShortageSettlementConfirmDialog(
                    shortage = uiState.selectedShortageForSettlement!!,
                    uiState = uiState,
                    onNotesChange = viewModel::updateSettlementNotesInput,
                    onDismiss = viewModel::dismissSettlementConfirmDialog,
                    onConfirm = { viewModel.confirmAndSettleShortage("مدير النظام") }
                )
            }

            // نافذة إضافة قيد عجز يدوي
            if (uiState.showManualShortageDialog) {
                ManualShortageAddDialog(
                    costCenters = uiState.costCenters,
                    onDismiss = viewModel::dismissManualShortageDialog,
                    onSave = { prodName, ccId, qty, cost, price, notes ->
                        viewModel.addManualShortageRecord(prodName, ccId, qty, cost, price, notes)
                    }
                )
            }

            // نافذة إضافة / تعديل مجموعة
            if (uiState.showGroupDialog) {
                GroupEditDialog(
                    uiState = uiState,
                    onDismiss = { viewModel.dismissGroupDialog() },
                    onSave = { name, code, category, method, desc ->
                        viewModel.saveGroup(name, code, category, method, desc)
                    }
                )
            }

            // نافذة حظر وتأكيد الحذف الآمن
            if (uiState.showDeleteValidationDialog && uiState.pendingDeleteGroup != null) {
                AlertDialog(
                    onDismissRequest = { viewModel.dismissDeleteValidationDialog() },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (uiState.isDeleteAllowed) Icons.Default.Warning else Icons.Default.Block,
                                contentDescription = null,
                                tint = if (uiState.isDeleteAllowed) Color(0xFFE65100) else Color.Red
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (uiState.isDeleteAllowed) "تأكيد حذف المجموعة" else "حظر الحذف للسلامة المحاسبية", fontWeight = FontWeight.Bold)
                        }
                    },
                    text = {
                        Text(uiState.deleteValidationMessage, fontSize = 13.sp)
                    },
                    confirmButton = {
                        if (uiState.isDeleteAllowed) {
                            Button(
                                onClick = { viewModel.confirmDeletePendingGroup() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                            ) {
                                Text("حذف الآن")
                            }
                        } else {
                            Button(onClick = { viewModel.dismissDeleteValidationDialog() }) {
                                Text("فهمت ذلك")
                            }
                        }
                    },
                    dismissButton = {
                        if (uiState.isDeleteAllowed) {
                            OutlinedButton(onClick = { viewModel.dismissDeleteValidationDialog() }) {
                                Text("إلغاء")
                            }
                        }
                    }
                )
            }
        }
    }
}

/**
 * التبويب الأول: إدارة المجموعات المخزنية والأصناف
 */
@Composable
private fun GroupsAndItemsView(
    uiState: ValueSellingUiState,
    viewModel: ValueSellingViewModel
) {
    val currentDetails = uiState.selectedGroupDetails

    if (currentDetails == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("لا توجد مجموعات مخزنية مسجلة حالياً")
        }
        return
    }

    var selectedProduct by remember { mutableStateOf<com.example.dokkani.data.local.entities.ProductWithUnits?>(null) }
    var productDropdownExpanded by remember { mutableStateOf(false) }
    var newItemRatio by remember { mutableStateOf("1.0") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // تفاصيل المجموعة المختارة
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(currentDetails.group.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("الكود: ${currentDetails.group.code} | التصنيف: ${currentDetails.group.category}", fontSize = 11.sp, color = Color.Gray)
                        }

                        Row {
                            IconButton(onClick = { viewModel.openEditGroupDialog(currentDetails.group) }) {
                                Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { viewModel.checkAndDeleteGroup(currentDetails.group) }) {
                                Icon(Icons.Default.Delete, contentDescription = "حذف آمن", tint = Color.Red)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("طريقة تقييم التكلفة: ${currentDetails.group.costMethod.labelArabic}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1B5E20))
                        Text("عدد الأصناف: ${currentDetails.items.size}", fontSize = 12.sp)
                    }

                    if (currentDetails.group.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("الوصف: ${currentDetails.group.description}", fontSize = 11.sp, color = Color.DarkGray)
                    }
                }
            }
        }

        // إضافة صنف جديد للمجموعة محصور حصرياً بقائمة الأصناف المسجلة بقاعدة البيانات
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9)),
                border = BorderStroke(1.dp, Color(0xFFA5D6A7)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("اختيار صنف مكون من قاعدة البيانات الفعليّة للمجموعة:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1B5E20))
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { productDropdownExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFF1B5E20))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = selectedProduct?.product?.name ?: "اضغط لاختيار صنف مسجل بالقاعدة...",
                                        fontWeight = if (selectedProduct != null) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp,
                                        color = if (selectedProduct != null) Color(0xFF1B5E20) else Color.Gray
                                    )
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color(0xFF1B5E20))
                                }
                            }

                            DropdownMenu(
                                expanded = productDropdownExpanded,
                                onDismissRequest = { productDropdownExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.9f)
                            ) {
                                if (uiState.productsWithUnits.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("لا توجد أصناف مسجلة حالياً", fontSize = 12.sp) },
                                        onClick = { productDropdownExpanded = false }
                                    )
                                } else {
                                    uiState.productsWithUnits.forEach { pw ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(pw.product.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                    Text("${pw.product.category} | %.2f %s".format(pw.units.firstOrNull()?.sellingPrice ?: 0.0, uiState.currencySymbol), fontSize = 11.sp, color = Color(0xFF2E7D32))
                                                }
                                            },
                                            onClick = {
                                                selectedProduct = pw
                                                productDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newItemRatio,
                                onValueChange = { newItemRatio = it },
                                label = { Text("معامل القيمة النسبية") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )

                            Button(
                                onClick = {
                                    val sel = selectedProduct
                                    if (sel != null) {
                                        val r = newItemRatio.toDoubleOrNull() ?: 1.0
                                        viewModel.addItemToGroup(
                                            groupId = currentDetails.group.id,
                                            productId = sel.product.id,
                                            productName = sel.product.name,
                                            ratio = r
                                        )
                                        selectedProduct = null
                                        newItemRatio = "1.0"
                                    }
                                },
                                enabled = selectedProduct != null,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20))
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("إضافة الصنف")
                            }
                        }
                    }
                }
            }
        }

        // قائمة الأصناف المسجلة في المجموعة
        item {
            Text("الأصناف التابعة لهذه المجموعة (${currentDetails.items.size}):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }

        items(currentDetails.items) { item ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.productName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(
                            text = "معامل القيمة: ${item.defaultRatio} ${if (item.productId != null) "| مرسخ بقاعدة البيانات (ID: ${item.productId})" else ""}",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFE8F5E9)
                        ) {
                            Text("مرتبط ✓", color = Color(0xFF1B5E20), fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                        }

                        IconButton(
                            onClick = { viewModel.deleteGroupItem(item.id) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف الصنف", tint = Color.Red, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * التبويب الثاني: الجرد الدوري لمجموعات الأصناف ومحرك حساب التكلفة (COGS Engine)
 */
@Composable
private fun GroupAuditCogsView(
    uiState: ValueSellingUiState,
    viewModel: ValueSellingViewModel,
    currentUserRole: UserRole,
    dateFormat: SimpleDateFormat
) {
    val currentDetails = uiState.selectedGroupDetails ?: return
    val isAdmin = currentUserRole == UserRole.ADMIN

    var editingAudit by remember { mutableStateOf<StockGroupAuditEntity?>(null) }
    var deletingAuditId by remember { mutableStateOf<Long?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ==========================================
        // الخطوة الأولى: اختيار المجموعة (Group Selector)
        // ==========================================
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Category, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("الخطوة 1: اختيار المجموعة المستهدفة بالجرد الدوري", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = currentDetails.group.name,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(uiState.groupsWithDetails) { gDetails ->
                            val isSelected = gDetails.group.id == uiState.selectedGroupId
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.selectGroup(gDetails.group.id) },
                                label = { Text("${gDetails.group.name} (${gDetails.items.size} أصناف)") },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                } else null
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // الخطوة الثانية: عرض أصناف المجموعة فقط وحقول الجرد والمخزون المباشر والتالف التلقائي
        // ==========================================
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Inventory2, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("الخطوة 2: أصناف المجموعة (${uiState.groupItemsAuditDetails.size}) والمخزون الفعلي", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        // اختيار طريقة التقييم
                        var menuExpanded by remember { mutableStateOf(false) }
                        Box {
                            FilterChip(
                                selected = true,
                                onClick = { menuExpanded = true },
                                label = { Text(uiState.auditCostMethod.labelArabic, fontSize = 11.sp) },
                                leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(12.dp)) }
                            )
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                CostValuationMethod.entries.forEach { method ->
                                    DropdownMenuItem(
                                        text = { Text(method.labelArabic, fontSize = 12.sp) },
                                        onClick = {
                                            viewModel.updateAuditInputs(costMethod = method)
                                            menuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider()

                    if (uiState.groupItemsAuditDetails.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            Text("لا توجد أصناف مرتبطة بهذة المجموعة حالياً. يرجى إضافة أصناف من تبويب 'المجموعات والأصناف'.", color = Color.Gray, fontSize = 12.sp)
                        }
                    } else {
                        // جدول الأصناف التابعة للمجموعة
                        uiState.groupItemsAuditDetails.forEach { itemDetail ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(itemDetail.productName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.secondaryContainer
                                        ) {
                                            Text(
                                                text = "الوحدة: ${itemDetail.unitName} | العملة: ${itemDetail.currencySymbol}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // مخزون الصنف المباشر (أول المدة + المشتريات)
                                        OutlinedTextField(
                                            value = String.format(Locale.US, "%.1f", itemDetail.currentStockQty),
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("مخزون الصنف (أول+مشتريات)") },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp),
                                            singleLine = true
                                        )

                                        // إدخال آخر المدة الفعلي للصنف
                                        OutlinedTextField(
                                            value = itemDetail.endingActualQtyInput,
                                            onValueChange = { viewModel.updateItemEndingQty(itemDetail.itemId, it) },
                                            label = { Text("آخر المدة الفعلي *") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp),
                                            singleLine = true
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // إجمالي المبيعات بالقيمة
                    OutlinedTextField(
                        value = uiState.auditRecordedSalesRevenueInput,
                        onValueChange = { viewModel.updateAuditInputs(recordedSalesRevenue = it) },
                        label = { Text("إجمالي المبيعات بالقيمة المسجلة بالفواتير (${uiState.currencySymbol}) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }
            }
        }

        // ==========================================
        // الخطوة الرابعة: حساب معادلة COGS وتكلفة المباع ومخرجات الجرد
        // ==========================================
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                border = BorderStroke(1.dp, Color(0xFF81C784)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Calculate, contentDescription = null, tint = Color(0xFF1B5E20))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("الخطوة 4: مخرجات محرك التكلفة COGS ونتائج الجرد", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1B5E20))
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("كمية المباع COGS:", fontSize = 11.sp, color = Color.Gray)
                            Text("%.1f كجم/وحدة".format(uiState.cogsCalculatedQty), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        Column {
                            Text("تكلفة المباع (COGS):", fontSize = 11.sp, color = Color.Gray)
                            Text("%.2f %s".format(uiState.cogsCalculatedCost, uiState.currencySymbol), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFC62828))
                        }

                        Column {
                            Text("صافي الربح المحقق:", fontSize = 11.sp, color = Color.Gray)
                            Text("%.2f %s".format(uiState.netProfitCalculated, uiState.currencySymbol), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1B5E20))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.saveAudit() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("حفظ وتسجيل يومية الجرد الدوري", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // ==========================================
        // الخطوة الثالثة: عرض السجلات السابقة والتحكم (مدير النظام فقط)
        // ==========================================
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "الخطوة 3: سجلات الجرد السابقة للمجموعة (${currentDetails.audits.size}):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                if (!isAdmin) {
                    Text("عرض فقط (صلاحية مدير النظام مطلوبة للتحكم)", fontSize = 11.sp, color = Color.Gray)
                }
            }
        }

        if (currentDetails.audits.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("لا توجد سجلات جرد دوري مسجلة لهذه المجموعة حتى الآن", color = Color.Gray)
                }
            }
        } else {
            items(currentDetails.audits) { audit ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("تاريخ الجرد: ${dateFormat.format(Date(audit.auditDate))}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("طريقة التقييم: ${audit.costValuationMethod.labelArabic}", fontSize = 11.sp, color = Color(0xFF1B5E20))
                            }

                            // أيقونات التعديل والحذف (مدير النظام فقط)
                            if (isAdmin) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { editingAudit = audit },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "تعديل الجرد", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    }

                                    IconButton(
                                        onClick = { deletingAuditId = audit.id },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "حذف الجرد", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("COGS: %.2f %s".format(audit.cogsCost, uiState.currencySymbol), fontSize = 12.sp, color = Color.Red, fontWeight = FontWeight.Bold)
                            Text("المبيعات: %.2f %s".format(audit.totalSalesRevenue, uiState.currencySymbol), fontSize = 12.sp)
                            Text("صافي الربح: %.2f %s".format(audit.netProfit, uiState.currencySymbol), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
                        }
                    }
                }
            }
        }
    }

    // نافذة تعديل سجل الجرد
    editingAudit?.let { audit ->
        var editSales by remember { mutableStateOf(audit.totalSalesRevenue.toString()) }
        var editEndingQty by remember { mutableStateOf(audit.endingActualQtyKg.toString()) }
        var editWasteQty by remember { mutableStateOf(audit.wasteQtyKg.toString()) }

        AlertDialog(
            onDismissRequest = { editingAudit = null },
            title = { Text("تعديل سجل الجرد الدوري", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editEndingQty,
                        onValueChange = { editEndingQty = it },
                        label = { Text("آخر المدة الفعلي (كجم)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editWasteQty,
                        onValueChange = { editWasteQty = it },
                        label = { Text("التالف الهالك (كجم)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editSales,
                        onValueChange = { editSales = it },
                        label = { Text("إجمالي المبيعات بالقيمة") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val end = editEndingQty.toDoubleOrNull() ?: audit.endingActualQtyKg
                        val waste = editWasteQty.toDoubleOrNull() ?: audit.wasteQtyKg
                        val rev = editSales.toDoubleOrNull() ?: audit.totalSalesRevenue

                        val totalAvail = audit.beginningQtyKg + audit.newPurchasesQtyKg
                        val cogsQty = (totalAvail - end - waste).coerceAtLeast(0.0)
                        val unitCost = if (totalAvail > 0) (audit.beginningCost + audit.newPurchasesCost) / totalAvail else 0.0
                        val cogsCost = cogsQty * unitCost
                        val netProfit = rev - cogsCost

                        val updated = audit.copy(
                            endingActualQtyKg = end,
                            wasteQtyKg = waste,
                            totalSalesRevenue = rev,
                            cogsQtyKg = cogsQty,
                            cogsCost = cogsCost,
                            netProfit = netProfit
                        )
                        viewModel.updateAuditRecord(updated)
                        editingAudit = null
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("حفظ التعديلات")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { editingAudit = null }, shape = RoundedCornerShape(8.dp)) {
                    Text("إلغاء")
                }
            }
        )
    }

    // نافذة تأكيد حذف سجل الجرد
    deletingAuditId?.let { auditId ->
        AlertDialog(
            onDismissRequest = { deletingAuditId = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("تأكيد حذف سجل الجرد", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
            text = { Text("هل أنت متأكد من حذف هذا السجل للجرد الدوري للمجموعة؟") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAudit(auditId)
                        deletingAuditId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("تأكيد الحذف")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { deletingAuditId = null }, shape = RoundedCornerShape(8.dp)) {
                    Text("إلغاء")
                }
            }
        )
    }
}

/**
 * نافذة حوارية لإضافة / تعديل مجموعة مخزنية
 */
@Composable
private fun GroupEditDialog(
    uiState: ValueSellingUiState,
    onDismiss: () -> Unit,
    onSave: (name: String, code: String, category: String, method: CostValuationMethod, desc: String) -> Unit
) {
    var name by remember { mutableStateOf(uiState.groupNameInput) }
    var code by remember { mutableStateOf(uiState.groupCodeInput) }
    var category by remember { mutableStateOf(uiState.groupCategoryInput) }
    var method by remember { mutableStateOf(uiState.groupCostMethod) }
    var desc by remember { mutableStateOf(uiState.groupDescriptionInput) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (uiState.editingGroupId == null) "إضافة مجموعة مخزنية جديدة" else "تعديل المجموعة المخزنية", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم المجموعة (مثال: خضار مشكل)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("كود المجموعة") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("التصنيف الرئيسي") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text("طريقة تقييم التكلفة:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CostValuationMethod.entries.forEach { m ->
                        FilterChip(
                            selected = method == m,
                            onClick = { method = m },
                            label = { Text(m.labelArabic, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("وصف المجموعة") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, code, category, method, desc) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20))
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

/**
 * واجهة إدارة وتسوية العجز المخزني والبيع بالقيمة (Value-Based Sales & Shortage Settlement UI)
 */
@Composable
fun ShortageSettlementView(
    uiState: ValueSellingUiState,
    viewModel: ValueSellingViewModel,
    currentUserRole: UserRole,
    dateFormat: SimpleDateFormat
) {
    var selectedCcId by remember { mutableStateOf<Long?>(null) }
    var selectedStatus by remember { mutableStateOf<String?>(null) }

    val filteredList = uiState.shortageSettlements.filter { shortage ->
        val matchesCc = (selectedCcId == null || shortage.costCenterId == selectedCcId)
        val matchesStatus = (selectedStatus == null || shortage.status == selectedStatus)
        matchesCc && matchesStatus
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. ترويسة وبانر الأمان والتوجيه المحاسبي
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B4D3E)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFFFFD54F), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("إدارة البيع بالقيمة وتسوية العجز (مدير النظام فقط)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFC62828)
                        ) {
                            Text("ADMIN ONLY", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "• العجز المخزني يُحسب حصرياً من شاشة الجرد الدوري (الدفتري - الفعلي).\n• التلف والهادر معزول تماماً ومسجل كمصروف مستقل سابقاً لمنع ازدواج الحسابات.\n• تسوية العجز تُسجل كـ 'مبيعات بالقيمة مقفلة ومسددة' تدخل الخزينة مباشرة.",
                        color = Color(0xFFE8F5E9),
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // 2. بطاقات المؤشرات المالية والكميات
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("العجز المعلق (غير مقفل)", fontSize = 11.sp, color = Color(0xFFE65100), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "%.1f وحدة".format(uiState.totalPendingShortageQty),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFBF360C)
                        )
                        Text(
                            text = "تكلفته: %.1f %s".format(uiState.totalPendingShortageCost, uiState.currencySymbol),
                            fontSize = 10.sp,
                            color = Color(0xFFD84315)
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8EAF6)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("مبيعات بالقيمة مستحقة", fontSize = 11.sp, color = Color(0xFF1A237E), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "%.2f %s".format(uiState.totalPendingValueSalesRevenue, uiState.currencySymbol),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF283593)
                        )
                        Text(
                            text = "بانتظار تأكيد المدير",
                            fontSize = 10.sp,
                            color = Color(0xFF3F51B5)
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("مبيعات بالقيمة مسددة", fontSize = 11.sp, color = Color(0xFF1B5E20), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "%.2f %s".format(uiState.totalSettledValueSalesRevenue, uiState.currencySymbol),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                        Text(
                            text = "دخلت الخزينة ومقفلة",
                            fontSize = 10.sp,
                            color = Color(0xFF388E3C)
                        )
                    }
                }
            }
        }

        // 3. شريط الإجراءات والفلاتر
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                val ccId = selectedCcId ?: 1L
                                viewModel.calculateAndImportShortageFromAudit(ccId)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("احتساب العجز من الجرد الحالي", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = { viewModel.openManualShortageDialog() },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("قيد عجز يدوي", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // فلتر مراكز التكلفة
                    Text("تصفية حسب مركز التكلفة:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            FilterChip(
                                selected = selectedCcId == null,
                                onClick = {
                                    selectedCcId = null
                                    viewModel.setCostCenterFilter(null)
                                },
                                label = { Text("جميع المراكز", fontSize = 11.sp) }
                            )
                        }
                        items(uiState.costCenters) { cc ->
                            FilterChip(
                                selected = selectedCcId == cc.centerId,
                                onClick = {
                                    selectedCcId = cc.centerId
                                    viewModel.setCostCenterFilter(cc.centerId)
                                },
                                label = { Text(cc.centerName, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // فلتر الحالة
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("حالة التسوية:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        FilterChip(
                            selected = selectedStatus == null,
                            onClick = {
                                selectedStatus = null
                                viewModel.setStatusFilter(null)
                            },
                            label = { Text("الكل", fontSize = 10.sp) }
                        )
                        FilterChip(
                            selected = selectedStatus == ShortageSettlementEntity.STATUS_PENDING,
                            onClick = {
                                selectedStatus = ShortageSettlementEntity.STATUS_PENDING
                                viewModel.setStatusFilter(ShortageSettlementEntity.STATUS_PENDING)
                            },
                            label = { Text("غير مسدد (معلق)", fontSize = 10.sp) }
                        )
                        FilterChip(
                            selected = selectedStatus == ShortageSettlementEntity.STATUS_SETTLED,
                            onClick = {
                                selectedStatus = ShortageSettlementEntity.STATUS_SETTLED
                                viewModel.setStatusFilter(ShortageSettlementEntity.STATUS_SETTLED)
                            },
                            label = { Text("مُقفل ومسدد", fontSize = 10.sp) }
                        )
                    }
                }
            }
        }

        // 4. قائمة قيود العجز المخزني والبيع بالقيمة
        if (filteredList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CheckCircleOutline, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("لا توجد قيود عجز مخزني مسجلة في هذا الفلتر حالياً.", color = Color.Gray, fontSize = 13.sp)
                        Text("اضغط 'احتساب العجز من الجرد الحالي' لرصد أي نقص مخزني من الجرد الدوري.", color = Color.Gray, fontSize = 11.sp)
                    }
                }
            }
        } else {
            items(filteredList) { shortage ->
                val isSettled = shortage.status == ShortageSettlementEntity.STATUS_SETTLED

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isSettled) Color(0xFFA5D6A7) else Color(0xFFFFCC80)
                    ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSettled) Color(0xFFF1F8E9) else Color(0xFFFFF8E1)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(shortage.productName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Business, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(shortage.costCenterName, fontSize = 11.sp, color = Color.DarkGray)
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSettled) Color(0xFF2E7D32) else Color(0xFFE65100)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isSettled) Icons.Default.Lock else Icons.Default.HourglassTop,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isSettled) "مقفلة / مسددة" else "معلق (قيد الانتظار)",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.LightGray.copy(alpha = 0.5f))

                        // تفاصيل الكميات والأسعار المتبقية
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("الدفتري: %.1f | الفعلي: %.1f".format(shortage.bookQuantity, shortage.actualQuantity), fontSize = 11.sp, color = Color.Gray)
                                Text("كمية العجز: %.1f وحدة".format(shortage.shortageQuantity), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD84315))
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text("سعر البيع: %.1f %s".format(shortage.unitSellingPrice, uiState.currencySymbol), fontSize = 11.sp, color = Color.Gray)
                                Text(
                                    text = "إيراد البيع بالقيمة: %.2f %s".format(shortage.totalValueSalesAmount, uiState.currencySymbol),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1B5E20)
                                )
                            }
                        }

                        if (shortage.notes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("ملاحظات: ${shortage.notes}", fontSize = 11.sp, color = Color.DarkGray)
                        }

                        if (isSettled && shortage.settledAt != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "تمت التسوية بتاريخ: ${dateFormat.format(Date(shortage.settledAt))} بواسطة (${shortage.settledBy ?: "المدير"})",
                                fontSize = 10.sp,
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // الأزرار والإجراءات
                        if (!isSettled && currentUserRole == UserRole.ADMIN) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { viewModel.deleteShortageRecord(shortage.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف القيد", tint = Color.Red, modifier = Modifier.size(18.dp))
                                }

                                Button(
                                    onClick = { viewModel.openSettlementConfirmDialog(shortage) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("تأكيد استلام القيمة وإقفال الدفاتر", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * نافذة تأكيد اعتماد وتسوية العجز تحويله إلى مبيعات بالقيمة مقفلة ومسددة (خاصة بمدير النظام فقط)
 */
@Composable
fun ShortageSettlementConfirmDialog(
    shortage: ShortageSettlementEntity,
    uiState: ValueSellingUiState,
    onNotesChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = Color(0xFF1B5E20))
                Spacer(modifier = Modifier.width(8.dp))
                Text("تأكيد تسوية العجز وتسجيل المبيعات بالقيمة", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("الصنف: ${shortage.productName}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("مركز التكلفة: ${shortage.costCenterName}", fontSize = 11.sp)
                        Text("كمية العجز الناقصة: %.1f وحدة".format(shortage.shortageQuantity), fontSize = 12.sp, color = Color(0xFFD84315))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "المبلغ الإجمالي المستحق للدخول للخزينة: %.2f %s".format(shortage.totalValueSalesAmount, uiState.currencySymbol),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B5E20)
                        )
                    }
                }

                Text(
                    text = "تنبيه محاسبي: عند الضغط على 'تأكيد التسوية والاستلام'، سيقوم النظام بـ:\n1. تسجيل فاتورة مبيعات نقدية بقيمة (%.2f %s) لحساب الخزينة.\n2. إقفال دفاتر العجز لهذا الصنف وتثبيت حالة التسوية إلى 'مقفلة ومسددة'.\n3. إبقاء التلف والهادر معزولاً ومسجلاً كمصروف مستقل.".format(
                        shortage.totalValueSalesAmount,
                        uiState.currencySymbol
                    ),
                    fontSize = 11.sp,
                    color = Color.DarkGray,
                    lineHeight = 15.sp
                )

                OutlinedTextField(
                    value = uiState.settlementAdminNotesInput,
                    onValueChange = onNotesChange,
                    label = { Text("ملاحظات اعتماد مدير النظام") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !uiState.isSettlingShortage,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20))
            ) {
                if (uiState.isSettlingShortage) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                } else {
                    Text("تأكيد واستلام القيمة (إقفال مسدد)")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

/**
 * نافذة إضافة قيد عجز يدوي للبيع بالقيمة
 */
@Composable
fun ManualShortageAddDialog(
    costCenters: List<CostCenterEntity>,
    onDismiss: () -> Unit,
    onSave: (productName: String, costCenterId: Long, shortageQty: Double, unitCost: Double, unitSellingPrice: Double, notes: String) -> Unit
) {
    var prodName by remember { mutableStateOf("") }
    var selectedCcId by remember { mutableStateOf(costCenters.firstOrNull()?.centerId ?: 1L) }
    var qtyStr by remember { mutableStateOf("1.0") }
    var costStr by remember { mutableStateOf("10.0") }
    var priceStr by remember { mutableStateOf("15.0") }
    var notesStr by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة قيد عجز مخزني يدوي للبيع بالقيمة", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = prodName,
                    onValueChange = { prodName = it },
                    label = { Text("اسم الصنف (مثال: طماطم بلدي)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text("مركز التكلفة المسند إليه:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(costCenters) { cc ->
                        FilterChip(
                            selected = selectedCcId == cc.centerId,
                            onClick = { selectedCcId = cc.centerId },
                            label = { Text(cc.centerName, fontSize = 11.sp) }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = qtyStr,
                        onValueChange = { qtyStr = it },
                        label = { Text("كمية العجز") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = { priceStr = it },
                        label = { Text("سعر البيع") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = notesStr,
                    onValueChange = { notesStr = it },
                    label = { Text("ملاحظات القيد") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = qtyStr.toDoubleOrNull() ?: 0.0
                    val cost = costStr.toDoubleOrNull() ?: 0.0
                    val price = priceStr.toDoubleOrNull() ?: 0.0
                    onSave(prodName, selectedCcId, qty, cost, price, notesStr)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20))
            ) {
                Text("حفظ القيد")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

