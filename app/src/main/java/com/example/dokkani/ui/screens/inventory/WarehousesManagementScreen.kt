package com.example.dokkani.ui.screens.inventory

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dokkani.data.local.entities.FinancialAccountEntity
import com.example.dokkani.data.local.entities.FinancialAccountType
import com.example.dokkani.data.local.entities.UserRole
import com.example.dokkani.data.local.entities.WarehouseEntity
import com.example.dokkani.ui.DokkaniUiState
import com.example.dokkani.ui.DokkaniViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * شاشة دليل وإدارة المخازن (Warehouses Directory Screen - Material 3)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarehousesManagementScreen(
    uiState: DokkaniUiState,
    currentUserRole: UserRole = UserRole.ADMIN,
    onSaveWarehouse: (WarehouseEntity) -> Unit = {},
    onDeleteWarehouse: (WarehouseEntity) -> Unit = {},
    onSetDefaultWarehouse: (Long) -> Unit = {},
    onSetWarehouseActive: (Long, Boolean) -> Unit = { _, _ -> },
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val isAdmin = currentUserRole == UserRole.ADMIN

    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var showPrintPreviewDialog by remember { mutableStateOf(false) }
    var editingWarehouse by remember { mutableStateOf<WarehouseEntity?>(null) }
    var deletingWarehouse by remember { mutableStateOf<WarehouseEntity?>(null) }

    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()) }

    val filteredWarehouses = remember(uiState.warehouses, searchQuery) {
        if (searchQuery.isBlank()) {
            uiState.warehouses
        } else {
            val q = searchQuery.trim().lowercase()
            uiState.warehouses.filter {
                it.name.lowercase().contains(q) ||
                        it.warehouseCode.lowercase().contains(q) ||
                        it.keeperName.lowercase().contains(q) ||
                        it.notes.lowercase().contains(q)
            }
        }
    }

    val defaultWarehouse = remember(uiState.warehouses) {
        uiState.warehouses.find { it.isDefault } ?: uiState.warehouses.firstOrNull()
    }

    val activeCount = remember(uiState.warehouses) {
        uiState.warehouses.count { it.isActive }
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
                    modifier = Modifier.fillMaxWidth().testTag("warehouses_header_card")
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
                                    text = "دليل وإدارة المخازن (Warehouses Directory)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "تعريف الفروع والمخازن، تعيين أمناء المخازن، والربط بحسابات الدليل المحاسبي",
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
                            // إجمالي المخازن
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("إجمالي المخازن", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${uiState.warehouses.size}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }

                            // المخازن النشطة
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFE8F5E9),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("المخازن النشطة", fontSize = 11.sp, color = Color(0xFF1B5E20))
                                    Text("$activeCount", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                }
                            }

                            // المخزن الرئيسي الافتراضي
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                                modifier = Modifier.weight(1.2f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("المخزن الافتراضي", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                    Text(
                                        text = defaultWarehouse?.name ?: "المخزن الرئيسي",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. شريط التحكم والبحث القياسي (إضافة، بحث، تحديث، طباعة، تصدير)
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
                            placeholder = { Text("بحث باسم المخزن أو الكود أو الأمين...", fontSize = 12.sp) },
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
                            modifier = Modifier.weight(1f).testTag("warehouse_search_input")
                        )

                        if (isAdmin) {
                            Button(
                                onClick = { showAddDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                                modifier = Modifier.testTag("add_warehouse_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("إضافة مخزن", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }

                    // أزرار التحكم القياسية (تحديث، طباعة، تصدير)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                searchQuery = ""
                                Toast.makeText(context, "تم تحديث دليل المخازن بنجاح!", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f).testTag("refresh_warehouses_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تحديث", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { showPrintPreviewDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f).testTag("print_warehouses_button")
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("طباعة", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                val exportText = buildString {
                                    appendLine("=== دليل المخازن والفروع - نظام دكاني ===")
                                    appendLine("الكود\tاسم المخزن\tأمين المخزن\tالحالة\tملاحظات")
                                    filteredWarehouses.forEach { wh ->
                                        val status = if (wh.isDefault) "رئيسي" else if (wh.isActive) "نشط" else "معطل"
                                        appendLine("${wh.warehouseCode}\t${wh.name}\t${wh.keeperName.ifBlank { "-" }}\t$status\t${wh.notes.ifBlank { "-" }}")
                                    }
                                }
                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Warehouses Directory", exportText))
                                Toast.makeText(context, "تم تصدير ونقش كشف المخازن إلى الحافظة بنجاح!", Toast.LENGTH_LONG).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f).testTag("export_warehouses_button")
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تصدير", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 3. قائمة المخازن
            if (filteredWarehouses.isEmpty()) {
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
                                text = if (searchQuery.isBlank()) "لا توجد مخازن مسجلة بعد" else "لا توجد نتائج مطابقة لـ '$searchQuery'",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            } else {
                items(filteredWarehouses, key = { it.id }) { warehouse ->
                    WarehouseItemCard(
                        warehouse = warehouse,
                        financialAccounts = uiState.financialAccounts,
                        isAdmin = isAdmin,
                        dateFormat = dateFormat,
                        onEdit = { editingWarehouse = warehouse },
                        onSetDefault = { onSetDefaultWarehouse(warehouse.id) },
                        onToggleActive = { onSetWarehouseActive(warehouse.id, !warehouse.isActive) },
                        onDelete = { deletingWarehouse = warehouse }
                    )
                }
            }
        }
    }

    // نافذة إضافة / تعديل مخزن
    if (showAddDialog || editingWarehouse != null) {
        AddEditWarehouseDialog(
            initialWarehouse = editingWarehouse,
            existingWarehousesCount = uiState.warehouses.size,
            financialAccounts = uiState.financialAccounts,
            onSave = { wh ->
                onSaveWarehouse(wh)
                showAddDialog = false
                editingWarehouse = null
                Toast.makeText(context, "تم حفظ بيانات المخزن بنجاح!", Toast.LENGTH_SHORT).show()
            },
            onDismiss = {
                showAddDialog = false
                editingWarehouse = null
            }
        )
    }

    // نافذة طباعة واستعراض دليل المخازن
    if (showPrintPreviewDialog) {
        WarehouseDirectoryPrintDialog(
            warehouses = filteredWarehouses,
            financialAccounts = uiState.financialAccounts,
            dateFormat = dateFormat,
            onDismiss = { showPrintPreviewDialog = false }
        )
    }

    // نافذة تأكيد حذف مخزن
    if (deletingWarehouse != null) {
        val whToDelete = deletingWarehouse
        if (whToDelete != null) {
            AlertDialog(
                onDismissRequest = { deletingWarehouse = null },
                icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(32.dp)) },
                title = { Text("تأكيد حذف المخزن", fontWeight = FontWeight.Bold) },
                text = { Text("هل أنت متأكد قاطعاً من حذف المخزن '${whToDelete.name}' (${whToDelete.warehouseCode})؟\nلا يمكن تراجع عن هذه العملية.") },
                confirmButton = {
                    Button(
                        onClick = {
                            onDeleteWarehouse(whToDelete)
                            deletingWarehouse = null
                            Toast.makeText(context, "تم حذف المخزن بنجاح", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("حذف المخزن", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { deletingWarehouse = null }) {
                        Text("إلغاء")
                    }
                },
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

/**
 * بطاقة عرض تفاصيل المخزن الواحد
 */
@Composable
private fun WarehouseItemCard(
    warehouse: WarehouseEntity,
    financialAccounts: List<FinancialAccountEntity>,
    isAdmin: Boolean,
    dateFormat: SimpleDateFormat,
    onEdit: () -> Unit,
    onSetDefault: () -> Unit,
    onToggleActive: () -> Unit,
    onDelete: () -> Unit
) {
    val linkedAccount = remember(warehouse.financialAccountId, financialAccounts) {
        financialAccounts.find { it.id == warehouse.financialAccountId }
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.dp,
            if (warehouse.isDefault) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth().testTag("warehouse_item_${warehouse.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // الصف العلوي: الكود، الاسم، والشارات
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = warehouse.warehouseCode,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = warehouse.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (warehouse.isDefault) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary,
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("المخزن الرئيسي", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (warehouse.isActive) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                    ) {
                        Text(
                            text = if (warehouse.isActive) "نشط" else "معطل",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (warehouse.isActive) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // التفاصيل المحاسبية وأمين المخزن
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (warehouse.keeperName.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "أمين المخزن: ${warehouse.keeperName}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountBalance, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (linkedAccount != null) "الحساب المحاسبي المرتبط: ${linkedAccount.code} - ${linkedAccount.name}" else "الحساب المحاسبي: غير مرتبط بحساب محدد (تلقائي مع المخزون)",
                        fontSize = 12.sp,
                        color = if (linkedAccount != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                }

                if (warehouse.notes.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Notes, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ملاحظات: ${warehouse.notes}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (isAdmin) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // أزرار العمليات والتحكم
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (!warehouse.isDefault) {
                            OutlinedButton(
                                onClick = onSetDefault,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Icon(Icons.Default.StarBorder, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("جعل كافتراضي", fontSize = 11.sp)
                            }
                        }

                        OutlinedButton(
                            onClick = onToggleActive,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(
                                if (warehouse.isActive) Icons.Default.Block else Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (warehouse.isActive) "تعطيل" else "تفعيل", fontSize = 11.sp)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }

                        if (!warehouse.isDefault) {
                            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * نافذة إضافة / تعديل بيانات مخزن
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditWarehouseDialog(
    initialWarehouse: WarehouseEntity?,
    existingWarehousesCount: Int,
    financialAccounts: List<FinancialAccountEntity>,
    onSave: (WarehouseEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var warehouseCode by remember {
        mutableStateOf(initialWarehouse?.warehouseCode ?: "WH-00${existingWarehousesCount + 1}")
    }
    var name by remember { mutableStateOf(initialWarehouse?.name ?: "") }
    var keeperName by remember { mutableStateOf(initialWarehouse?.keeperName ?: "") }
    var selectedAccountId by remember { mutableStateOf(initialWarehouse?.financialAccountId) }
    var notes by remember { mutableStateOf(initialWarehouse?.notes ?: "") }
    var isActive by remember { mutableStateOf(initialWarehouse?.isActive ?: true) }
    var isDefault by remember { mutableStateOf(initialWarehouse?.isDefault ?: false) }

    var nameError by remember { mutableStateOf(false) }
    var accountDropdownExpanded by remember { mutableStateOf(false) }

    val linkedAccount = remember(selectedAccountId, financialAccounts) {
        financialAccounts.find { it.id == selectedAccountId }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialWarehouse == null) "إضافة مخزن / فرع جديد" else "تعديل بيانات المخزن",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // رمز المخزن ورقم الكود
                OutlinedTextField(
                    value = warehouseCode,
                    onValueChange = { warehouseCode = it },
                    label = { Text("رمز / كود المخزن") },
                    placeholder = { Text("مثال: WH-001 أو BR-01") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("warehouse_code_input")
                )

                // اسم المخزن
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = false
                    },
                    label = { Text("اسم المخزن / الفرع *") },
                    placeholder = { Text("مثال: المخزن الرئيسي / فرع الزبيري") },
                    isError = nameError,
                    supportingText = if (nameError) { { Text("اسم المخزن مطلوب", color = MaterialTheme.colorScheme.error) } } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("warehouse_name_input")
                )

                // اسم أمين المخزن
                OutlinedTextField(
                    value = keeperName,
                    onValueChange = { keeperName = it },
                    label = { Text("اسم أمين المخزن المسؤول") },
                    placeholder = { Text("مثال: علي عبدالله / المدير العام") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("warehouse_keeper_input")
                )

                // اختيار حساب المخزون المرتبط بالدليل المحاسبي
                ExposedDropdownMenuBox(
                    expanded = accountDropdownExpanded,
                    onExpandedChange = { accountDropdownExpanded = !accountDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = if (linkedAccount != null) "${linkedAccount.code} - ${linkedAccount.name}" else "افتراضي (حساب المخزون الرئيسي بالدليل)",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("حساب المخزن المرتبط بالدليل المحاسبي") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("warehouse_account_select")
                    )

                    ExposedDropdownMenu(
                        expanded = accountDropdownExpanded,
                        onDismissRequest = { accountDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("افتراضي (حساب المخزون الرئيسي بالدليل)") },
                            onClick = {
                                selectedAccountId = null
                                accountDropdownExpanded = false
                            }
                        )

                        financialAccounts
                            .filter { it.accountType == FinancialAccountType.CHART_ACCOUNT || it.accountType == FinancialAccountType.CASH_DRAWER || it.accountType == FinancialAccountType.BANK }
                            .forEach { acc ->
                                DropdownMenuItem(
                                    text = { Text("${acc.code} - ${acc.name}") },
                                    onClick = {
                                        selectedAccountId = acc.id
                                        accountDropdownExpanded = false
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
                    placeholder = { Text("أية ملاحظات إضافية حول المخزن...") },
                    modifier = Modifier.fillMaxWidth().testTag("warehouse_notes_input")
                )

                // خيار إضافي: تعيين كافتراضي
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("جعل هذا المخزن هو الرئيسي الافتراضي", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Switch(
                        checked = isDefault,
                        onCheckedChange = { isDefault = it },
                        modifier = Modifier.testTag("warehouse_default_switch")
                    )
                }

                // خيار النشاط
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("حالة المخزن (نشط / مفعل)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Switch(
                        checked = isActive,
                        onCheckedChange = { isActive = it },
                        modifier = Modifier.testTag("warehouse_active_switch")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        nameError = true
                        return@Button
                    }
                    val targetWh = (initialWarehouse ?: WarehouseEntity(warehouseCode = "", name = "")).copy(
                        warehouseCode = warehouseCode.ifBlank { "WH-001" },
                        name = name.trim(),
                        keeperName = keeperName.trim(),
                        financialAccountId = selectedAccountId,
                        notes = notes.trim(),
                        isActive = isActive,
                        isDefault = isDefault
                    )
                    onSave(targetWh)
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
 * نافذة معاينة طباعة دليل المخازن بالفروع والحسابات المرتبطة
 */
@Composable
private fun WarehouseDirectoryPrintDialog(
    warehouses: List<WarehouseEntity>,
    financialAccounts: List<FinancialAccountEntity>,
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
                Text("معاينة طباعة دليل المخازن", fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
                    text = "دكاني - كشف رسمي بدليل الفروع والمخازن المعتمدة",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "التاريخ: ${dateFormat.format(Date())} | إجمالي المخازن: ${warehouses.size}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(warehouses) { wh ->
                        val linkedAcc = financialAccounts.find { it.id == wh.financialAccountId }
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
                                    Text("${wh.warehouseCode} - ${wh.name}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text(
                                        text = if (wh.isDefault) "رئيسي" else if (wh.isActive) "نشط" else "معطل",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (wh.isDefault) MaterialTheme.colorScheme.primary else if (wh.isActive) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                                    )
                                }
                                if (wh.keeperName.isNotBlank()) {
                                    Text("الأمين: ${wh.keeperName}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(
                                    text = "الحساب: ${linkedAcc?.let { "${it.code} - ${it.name}" } ?: "غير مرتبط"}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                if (wh.notes.isNotBlank()) {
                                    Text("ملاحظات: ${wh.notes}", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
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
                    Toast.makeText(context, "جاري إرسال التقرير لطابعة الفواتير والتقارير...", Toast.LENGTH_SHORT).show()
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
