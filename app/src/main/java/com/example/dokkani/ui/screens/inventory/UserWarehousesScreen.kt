package com.example.dokkani.ui.screens.inventory

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dokkani.data.local.entities.UserRole
import com.example.dokkani.data.local.entities.UserWarehouseEntity
import com.example.dokkani.data.local.entities.WarehouseEntity
import com.example.dokkani.ui.DokkaniUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * شاشة مخازن المستخدمين وإدارة صلاحيات الربط للمخازن والفروع (User Warehouses Screen)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserWarehousesScreen(
    uiState: DokkaniUiState,
    currentUserRole: UserRole = UserRole.ADMIN,
    onSaveUserWarehouse: (UserWarehouseEntity) -> Unit = {},
    onDeleteUserWarehouse: (UserWarehouseEntity) -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val isAdmin = currentUserRole == UserRole.ADMIN

    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var showPrintPreviewDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<UserWarehouseEntity?>(null) }
    var deletingItem by remember { mutableStateOf<UserWarehouseEntity?>(null) }
    var showToolsMenu by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale.getDefault()) }

    val filteredList = remember(uiState.userWarehouses, searchQuery) {
        if (searchQuery.isBlank()) {
            uiState.userWarehouses
        } else {
            val q = searchQuery.trim().lowercase()
            uiState.userWarehouses.filter {
                it.userName.lowercase().contains(q) ||
                        it.warehouseName.lowercase().contains(q) ||
                        it.warehouseCode.lowercase().contains(q) ||
                        it.notes.lowercase().contains(q)
            }
        }
    }

    val uniqueUsersCount = remember(uiState.userWarehouses) {
        uiState.userWarehouses.map { it.userName }.distinct().size
    }

    val uniqueWarehousesCount = remember(uiState.userWarehouses) {
        uiState.userWarehouses.map { it.warehouseName }.distinct().size
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. ترويسة الشاشة والبطاقة الرئيسية
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primaryContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth().testTag("user_warehouses_header_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Storefront,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "مخازن المستخدمين (User Warehouses)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "ربط وتحديد صلاحيات الوصول للمخازن والفروع الحسابية لكل مستخدم",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        // صف الإحصائيات السريعة
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // إجمالي السجلات
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("سجلات الربط", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${uiState.userWarehouses.size}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }

                            // المستخدمين المعينين
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFE8F5E9),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("المستخدمين المعينين", fontSize = 11.sp, color = Color(0xFF1B5E20))
                                    Text("$uniqueUsersCount", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                }
                            }

                            // المخازن المربوطة
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("المخازن المربوطة", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                    Text("$uniqueWarehousesCount", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                }
                            }
                        }
                    }
                }
            }

            // 2. شريط البحث والتحكم والعمليات القياسية
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("بحث باسم المستخدم أو المخزن...", fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "مسح", modifier = Modifier.size(18.dp))
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("user_warehouse_search_input")
                        )

                        if (isAdmin) {
                            Button(
                                onClick = { showAddDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                                modifier = Modifier.testTag("add_user_warehouse_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("إضافة ربط", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }

                    // أزرار العمليات القياسية (تحديث، أدوات، تصدير، طباعة)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                searchQuery = ""
                                Toast.makeText(context, "تم تحديث سجلات مخازن المستخدمين بنجاح!", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f).testTag("refresh_user_warehouses_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تحديث", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { showToolsMenu = true },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.fillMaxWidth().testTag("tools_user_warehouses_button")
                            ) {
                                Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("أدوات", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            DropdownMenu(
                                expanded = showToolsMenu,
                                onDismissRequest = { showToolsMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("تصفية البحث والتحديد") },
                                    leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = null) },
                                    onClick = {
                                        searchQuery = ""
                                        showToolsMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("نسخ ملخص السجلات للحافظة") },
                                    leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                                    onClick = {
                                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        clipboard.setPrimaryClip(
                                            android.content.ClipData.newPlainText("User Warehouses Summary", "إجمالي السجلات: ${uiState.userWarehouses.size} | المستخدمين: $uniqueUsersCount | المخازن: $uniqueWarehousesCount")
                                        )
                                        Toast.makeText(context, "تم نسخ الملخص بنجاح", Toast.LENGTH_SHORT).show()
                                        showToolsMenu = false
                                    }
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                val exportText = buildString {
                                    appendLine("=== سجلات مخازن المستخدمين والصلاحيات - دكاني ===")
                                    appendLine("اسم المستخدم\tحساب المخزن\tملاحظات\tوقت الإضافة")
                                    filteredList.forEach { item ->
                                        appendLine("${item.userName}\t${item.warehouseCode} - ${item.warehouseName}\t${item.notes.ifBlank { "-" }}\t${dateFormat.format(Date(item.createdAt))}")
                                    }
                                }
                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("User Warehouses Export", exportText))
                                Toast.makeText(context, "تم تصدير ونقش كشف مخازن المستخدمين للحافظة بنجاح!", Toast.LENGTH_LONG).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f).testTag("export_user_warehouses_button")
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تصدير", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { showPrintPreviewDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f).testTag("print_user_warehouses_button")
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("طباعة", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 3. كارتات وقائمة الجدول (LazyColumn / Table view)
            if (filteredList.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storefront,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (searchQuery.isBlank()) "لا توجد سجلات ربط لمخازن المستخدمين بعد" else "لا توجد نتائج مطابقة لـ '$searchQuery'",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            } else {
                items(filteredList, key = { it.id }) { item ->
                    UserWarehouseRowCard(
                        item = item,
                        isAdmin = isAdmin,
                        dateFormat = dateFormat,
                        onEdit = { editingItem = item },
                        onDelete = { deletingItem = item }
                    )
                }
            }
        }
    }

    // نافذة إضافة / تعديل سجل ربط مخزن لمستخدم
    if (showAddDialog || editingItem != null) {
        AddEditUserWarehouseDialog(
            initialItem = editingItem,
            availableWarehouses = uiState.warehouses,
            onSave = { entity ->
                onSaveUserWarehouse(entity)
                showAddDialog = false
                editingItem = null
                Toast.makeText(context, "تم حفظ بيانات ربط المخزن بنجاح!", Toast.LENGTH_SHORT).show()
            },
            onDismiss = {
                showAddDialog = false
                editingItem = null
            }
        )
    }

    // نافذة معاينة طباعة السجلات
    if (showPrintPreviewDialog) {
        UserWarehousesPrintPreviewDialog(
            items = filteredList,
            dateFormat = dateFormat,
            onDismiss = { showPrintPreviewDialog = false }
        )
    }

    // نافذة تأكيد حذف السجل
    if (deletingItem != null) {
        val target = deletingItem
        if (target != null) {
            AlertDialog(
                onDismissRequest = { deletingItem = null },
                icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(32.dp)) },
                title = { Text("تأكيد حذف ربط المخزن", fontWeight = FontWeight.Bold) },
                text = { Text("هل أنت متأكد من حذف صلاحية ربط المستخدم '${target.userName}' مع المخزن '${target.warehouseName}'؟") },
                confirmButton = {
                    Button(
                        onClick = {
                            onDeleteUserWarehouse(target)
                            deletingItem = null
                            Toast.makeText(context, "تم حذف السجل بنجاح", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("حذف السجل", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { deletingItem = null }) {
                        Text("إلغاء")
                    }
                },
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

/**
 * صف أو بطاقة عرض بيانات ربط مخزن لـ مستخدم
 */
@Composable
private fun UserWarehouseRowCard(
    item: UserWarehouseEntity,
    isAdmin: Boolean,
    dateFormat: SimpleDateFormat,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth().testTag("user_warehouse_item_${item.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // اسم المستخدم
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = item.userName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "وقت الإضافة: ${dateFormat.format(Date(item.createdAt))}",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                if (isAdmin) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // تفاصيل حساب المخزن والملاحظات
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Storefront, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "حساب/اسم المخزن: ",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = if (item.warehouseCode.isNotBlank()) "${item.warehouseCode} - ${item.warehouseName}" else item.warehouseName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                if (item.notes.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Notes, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ملاحظات: ${item.notes}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * نافذة إضافة / تعديل بيانات ربط مخزن لمستخدم
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditUserWarehouseDialog(
    initialItem: UserWarehouseEntity?,
    availableWarehouses: List<WarehouseEntity>,
    onSave: (UserWarehouseEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var userName by remember { mutableStateOf(initialItem?.userName ?: "") }
    var selectedWarehouseId by remember { mutableStateOf(initialItem?.warehouseId) }
    var warehouseNameInput by remember { mutableStateOf(initialItem?.warehouseName ?: "") }
    var warehouseCodeInput by remember { mutableStateOf(initialItem?.warehouseCode ?: "") }
    var notes by remember { mutableStateOf(initialItem?.notes ?: "") }

    var userDropdownExpanded by remember { mutableStateOf(false) }
    var warehouseDropdownExpanded by remember { mutableStateOf(false) }

    var userNameError by remember { mutableStateOf(false) }
    var warehouseError by remember { mutableStateOf(false) }

    // قائمة مستخدمين سريعة للاختيار المباشر
    val defaultUserNames = listOf("مدير النظام (admin)", "كاشير الرئيسية (cashier1)", "أمين المخزن (inventory1)", "المشرف العام")

    val selectedWh = remember(selectedWarehouseId, availableWarehouses) {
        availableWarehouses.find { it.id == selectedWarehouseId }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialItem == null) "إضافة ربط مخزن لمستخدم" else "تعديل بيانات ربط المخزن",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // اختيار / كتابة اسم المستخدم
                ExposedDropdownMenuBox(
                    expanded = userDropdownExpanded,
                    onExpandedChange = { userDropdownExpanded = !userDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = userName,
                        onValueChange = {
                            userName = it
                            userNameError = false
                        },
                        label = { Text("اسم المستخدم *") },
                        placeholder = { Text("اختر أو اكتب اسم المستخدم...") },
                        isError = userNameError,
                        supportingText = if (userNameError) { { Text("اسم المستخدم مطلوب", color = MaterialTheme.colorScheme.error) } } else null,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = userDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("user_name_input")
                    )

                    ExposedDropdownMenu(
                        expanded = userDropdownExpanded,
                        onDismissRequest = { userDropdownExpanded = false }
                    ) {
                        defaultUserNames.forEach { name ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    userName = name
                                    userNameError = false
                                    userDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // اختيار حساب المخزن
                ExposedDropdownMenuBox(
                    expanded = warehouseDropdownExpanded,
                    onExpandedChange = { warehouseDropdownExpanded = !warehouseDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = if (selectedWh != null) "${selectedWh.warehouseCode} - ${selectedWh.name}" else warehouseNameInput,
                        onValueChange = {
                            warehouseNameInput = it
                            selectedWarehouseId = null
                            warehouseError = false
                        },
                        readOnly = false,
                        label = { Text("حساب / اسم المخزن المرتبط *") },
                        placeholder = { Text("اختر المخزن من القائمة...") },
                        isError = warehouseError,
                        supportingText = if (warehouseError) { { Text("يرجى اختيار أو تحديد المخزن", color = MaterialTheme.colorScheme.error) } } else null,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = warehouseDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("warehouse_select_input")
                    )

                    ExposedDropdownMenu(
                        expanded = warehouseDropdownExpanded,
                        onDismissRequest = { warehouseDropdownExpanded = false }
                    ) {
                        availableWarehouses.forEach { wh ->
                            DropdownMenuItem(
                                text = { Text("${wh.warehouseCode} - ${wh.name}") },
                                onClick = {
                                    selectedWarehouseId = wh.id
                                    warehouseNameInput = wh.name
                                    warehouseCodeInput = wh.warehouseCode
                                    warehouseError = false
                                    warehouseDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // ملاحظات
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات") },
                    placeholder = { Text("أية ملاحظات حول صلاحية الوصول للمخزن...") },
                    modifier = Modifier.fillMaxWidth().testTag("user_warehouse_notes_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalUserName = userName.trim()
                    val finalWhName = (selectedWh?.name ?: warehouseNameInput).trim()
                    if (finalUserName.isBlank()) {
                        userNameError = true
                        return@Button
                    }
                    if (finalWhName.isBlank()) {
                        warehouseError = true
                        return@Button
                    }

                    val entity = (initialItem ?: UserWarehouseEntity(userName = "", warehouseName = "")).copy(
                        userName = finalUserName,
                        warehouseId = selectedWarehouseId,
                        warehouseCode = selectedWh?.warehouseCode ?: warehouseCodeInput,
                        warehouseName = finalWhName,
                        notes = notes.trim()
                    )
                    onSave(entity)
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("حفظ البيانات", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

/**
 * نافذة معاينة طباعة سجلات مخازن المستخدمين
 */
@Composable
private fun UserWarehousesPrintPreviewDialog(
    items: List<UserWarehouseEntity>,
    dateFormat: SimpleDateFormat,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("معاينة طباعة مخازن المستخدمين", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Icon(Icons.Default.Print, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "دكاني - كشف رسمي بسجلات وصلاحيات مخازن المستخدمين",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "التاريخ: ${dateFormat.format(Date())} | إجمالي السجلات: ${items.size}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items) { record ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("المستخدم: ${record.userName}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text(dateFormat.format(Date(record.createdAt)), fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                }
                                Text("المخزن: ${if (record.warehouseCode.isNotBlank()) "${record.warehouseCode} - ${record.warehouseName}" else record.warehouseName}", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                                if (record.notes.isNotBlank()) {
                                    Text("ملاحظات: ${record.notes}", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    Toast.makeText(context, "جاري إرسال كشف مخازن المستخدمين إلى الطابعة...", Toast.LENGTH_SHORT).show()
                    onDismiss()
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("طباعة الكشف", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}
