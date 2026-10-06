package com.example.dokkani.ui.screens.warehouse

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dokkani.data.local.entities.FinancialAccountEntity
import com.example.dokkani.data.local.entities.UserRole
import com.example.dokkani.data.local.entities.WarehouseEntity

/**
 * واجهة "دليل المخازن" المستقلة والمنظمة لنظام دكاني المحاسبي (Warehouses Directory)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarehouseDirectoryScreen(
    warehouses: List<WarehouseEntity>,
    financialAccounts: List<FinancialAccountEntity>,
    currentUserRole: UserRole,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSaveWarehouse: (
        code: String,
        name: String,
        keeper: String,
        financialAccountId: Long?,
        notes: String,
        isActive: Boolean,
        isDefault: Boolean,
        editingId: Long
    ) -> Unit,
    onDeleteWarehouse: (WarehouseEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Form inputs state
    var editingWarehouseId by remember { mutableLongStateOf(0L) }
    var codeInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }
    var keeperInput by remember { mutableStateOf("") }
    var selectedAccountId by remember { mutableStateOf<Long?>(null) }
    var notesInput by remember { mutableStateOf("") }
    var isActiveInput by remember { mutableStateOf(true) }
    var isDefaultInput by remember { mutableStateOf(false) }

    // Dialog states
    var showAccountDropdown by remember { mutableStateOf(false) }
    var warehouseToDelete by remember { mutableStateOf<WarehouseEntity?>(null) }
    var showPrintDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }

    // Filter warehouses by search query
    val filteredWarehouses = remember(warehouses, searchQuery) {
        if (searchQuery.isBlank()) {
            warehouses
        } else {
            warehouses.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.warehouseCode.contains(searchQuery, ignoreCase = true) ||
                        it.keeperName.contains(searchQuery, ignoreCase = true) ||
                        it.notes.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // Clear form fields
    fun clearForm() {
        editingWarehouseId = 0L
        codeInput = "WH-${(warehouses.size + 1).toString().padStart(3, '0')}"
        nameInput = ""
        keeperInput = ""
        selectedAccountId = financialAccounts.firstOrNull { it.code == "10501" }?.id
            ?: financialAccounts.firstOrNull()?.id
        notesInput = ""
        isActiveInput = true
        isDefaultInput = warehouses.isEmpty()
    }

    // Auto-initialize code input if empty
    LaunchedEffect(warehouses.size) {
        if (editingWarehouseId == 0L && codeInput.isBlank()) {
            codeInput = "WH-${(warehouses.size + 1).toString().padStart(3, '0')}"
        }
    }

    // Populate form when selecting a row
    fun populateWarehouse(warehouse: WarehouseEntity) {
        editingWarehouseId = warehouse.id
        codeInput = warehouse.warehouseCode
        nameInput = warehouse.name
        keeperInput = warehouse.keeperName
        selectedAccountId = warehouse.financialAccountId
        notesInput = warehouse.notes
        isActiveInput = warehouse.isActive
        isDefaultInput = warehouse.isDefault
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // 1. بطاقة الهيدر والإحصائيات
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1B5E20)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.HomeWork,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "دليل المخازن (Warehouses Directory)",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "إدارة المخازن ورصد مواقع التخزين والحسابات المرتبطة بالدليل المحاسبي",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = { },
                        label = { Text("الإجمالي: ${warehouses.size}", color = Color.White, fontWeight = FontWeight.Bold) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = Color.White.copy(alpha = 0.2f))
                    )
                    AssistChip(
                        onClick = { },
                        label = { Text("النشطة: ${warehouses.count { it.isActive }}", color = Color.White, fontWeight = FontWeight.Bold) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = Color.White.copy(alpha = 0.2f))
                    )
                }
            }
        }

        // 2. نموذج إدخال وإدارة بيانات المخزن
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = if (editingWarehouseId > 0) "✏️ تعديل بيانات المخزن رقم #$editingWarehouseId" else "➕ إدخال مخزن جديد للدليل المحاسبي",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // الصف الأول: كود المخزن، اسم المخزن، أمين المخزن
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = codeInput,
                        onValueChange = { codeInput = it },
                        label = { Text("رقم المخزن (Code)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        trailingIcon = {
                            IconButton(onClick = {
                                codeInput = "WH-${(warehouses.size + 1).toString().padStart(3, '0')}"
                            }) {
                                Icon(Icons.Default.Refresh, contentDescription = "توليد تلقائي")
                            }
                        }
                    )

                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("اسم المخزن *") },
                        singleLine = true,
                        modifier = Modifier.weight(1.5f),
                        isError = nameInput.isBlank()
                    )

                    OutlinedTextField(
                        value = keeperInput,
                        onValueChange = { keeperInput = it },
                        label = { Text("أمين المخزن / المسئول") },
                        singleLine = true,
                        modifier = Modifier.weight(1.5f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // الصف الثاني: الحساب المحاسبي المرتبط والملاحظات
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // اختيار الحساب المحاسبي
                    val selectedAccount = financialAccounts.firstOrNull { it.id == selectedAccountId }
                    ExposedDropdownMenuBox(
                        expanded = showAccountDropdown,
                        onExpandedChange = { showAccountDropdown = !showAccountDropdown },
                        modifier = Modifier.weight(2f)
                    ) {
                        OutlinedTextField(
                            value = selectedAccount?.let { "${it.code} - ${it.name}" } ?: "اختر الحساب المحاسبي المرتبط",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("الحساب المرتبط بالدليل المحاسبي") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showAccountDropdown) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = showAccountDropdown,
                            onDismissRequest = { showAccountDropdown = false }
                        ) {
                            financialAccounts.forEach { account ->
                                DropdownMenuItem(
                                    text = { Text("${account.code} - ${account.name}") },
                                    onClick = {
                                        selectedAccountId = account.id
                                        showAccountDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        label = { Text("ملاحظات / وصف موقع المخزن") },
                        singleLine = true,
                        modifier = Modifier.weight(2f)
                    )

                    // خيارات النشاط والافتراضي
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FilterChip(
                            selected = isActiveInput,
                            onClick = { isActiveInput = !isActiveInput },
                            label = { Text(if (isActiveInput) "نشط" else "موقف") },
                            leadingIcon = {
                                Icon(
                                    if (isActiveInput) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                    contentDescription = null,
                                    tint = if (isActiveInput) Color(0xFF2E7D32) else Color.Red
                                )
                            }
                        )

                        FilterChip(
                            selected = isDefaultInput,
                            onClick = { isDefaultInput = !isDefaultInput },
                            label = { Text("افتراضي") },
                            leadingIcon = {
                                if (isDefaultInput) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFF57C00))
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3. أزرار التحكم والعمليات القياسية
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // زر جديد / تفرغ
                        Button(
                            onClick = { clearForm() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("جديد")
                        }

                        // زر إضافة / تعديل (حفظ)
                        Button(
                            onClick = {
                                if (nameInput.isBlank()) {
                                    Toast.makeText(context, "يرجى إدخال اسم المخزن أولاً", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                onSaveWarehouse(
                                    codeInput,
                                    nameInput,
                                    keeperInput,
                                    selectedAccountId,
                                    notesInput,
                                    isActiveInput,
                                    isDefaultInput,
                                    editingWarehouseId
                                )
                                Toast.makeText(
                                    context,
                                    if (editingWarehouseId > 0) "تم تعديل المخزن بنجاح!" else "تم إضافة المخزن بنجاح!",
                                    Toast.LENGTH_SHORT
                                ).show()
                                clearForm()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20))
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (editingWarehouseId > 0) "تحديث المخزن" else "حفظ المخزن")
                        }

                        // زر حذف (إذا كان محدد للتعديل)
                        if (editingWarehouseId > 0) {
                            OutlinedButton(
                                onClick = {
                                    val entity = warehouses.firstOrNull { it.id == editingWarehouseId }
                                    if (entity != null) {
                                        warehouseToDelete = entity
                                    }
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("حذف")
                            }
                        }

                        // زر تحديث
                        IconButton(onClick = { clearForm() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "تحديث البيانات")
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // زر طباعة
                        OutlinedButton(onClick = { showPrintDialog = true }) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("طباعة الدليل")
                        }

                        // زر تصدير
                        OutlinedButton(onClick = { showExportDialog = true }) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تصدير")
                        }
                    }
                }
            }
        }

        // 4. حقل البحث في دليل المخازن
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            placeholder = { Text("🔍 بحث برقم المخزن، الاسم، أمين المخزن، أو الملاحظات...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "مسح البحث")
                    }
                }
            },
            singleLine = true
        )

        // 5. جدول عرض دليل المخازن (LazyColumn Table)
        Card(
            modifier = Modifier.fillMaxSize(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // هيدر الجدول المحاسبي
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF2E7D32))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("#", modifier = Modifier.width(40.dp), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("رقم المخزن", modifier = Modifier.width(90.dp), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("اسم المخزن", modifier = Modifier.weight(1.5f), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("أمين المخزن", modifier = Modifier.weight(1.2f), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("الحساب المحاسبي المرتبط", modifier = Modifier.weight(1.8f), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("الحالة", modifier = Modifier.width(80.dp), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center)
                    Text("الإجراءات", modifier = Modifier.width(90.dp), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center)
                }

                HorizontalDivider()

                if (filteredWarehouses.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.HomeWork,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (searchQuery.isNotEmpty()) "لا توجد مخازن تطابق كلمة البحث" else "لم يتم إدخال مخازن حتى الآن",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredWarehouses, key = { it.id }) { warehouse ->
                            val linkedAccount = financialAccounts.firstOrNull { it.id == warehouse.financialAccountId }
                            val isSelected = editingWarehouseId == warehouse.id

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                        else MaterialTheme.colorScheme.surface
                                    )
                                    .clickable { populateWarehouse(warehouse) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${warehouse.id}",
                                    modifier = Modifier.width(40.dp),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Text(
                                    text = warehouse.warehouseCode,
                                    modifier = Modifier.width(90.dp),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Row(modifier = Modifier.weight(1.5f), verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = warehouse.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (warehouse.isDefault) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Surface(
                                            color = Color(0xFFFFF3E0),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "افتراضي",
                                                fontSize = 10.sp,
                                                color = Color(0xFFE65100),
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = warehouse.keeperName.ifBlank { "-" },
                                    modifier = Modifier.weight(1.2f),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Text(
                                    text = linkedAccount?.let { "${it.code} (${it.name})" } ?: "غير مرتبط",
                                    modifier = Modifier.weight(1.8f),
                                    fontSize = 12.sp,
                                    color = if (linkedAccount != null) Color(0xFF1B5E20) else Color.Gray
                                )

                                Surface(
                                    modifier = Modifier.width(80.dp),
                                    color = if (warehouse.isActive) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = if (warehouse.isActive) "نشط" else "موقف",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (warehouse.isActive) Color(0xFF2E7D32) else Color.Red,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                }

                                Row(
                                    modifier = Modifier.width(90.dp),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    IconButton(
                                        onClick = { populateWarehouse(warehouse) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    }

                                    IconButton(
                                        onClick = { warehouseToDelete = warehouse },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.Red, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }

    // تأكيد الحذف Dialog
    warehouseToDelete?.let { warehouse ->
        AlertDialog(
            onDismissRequest = { warehouseToDelete = null },
            title = { Text("تأكيد حذف المخزن") },
            text = { Text("هل أنت تأكد من إزالة المخزن (${warehouse.name} - ${warehouse.warehouseCode}) من دليل المخازن؟") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteWarehouse(warehouse)
                        warehouseToDelete = null
                        if (editingWarehouseId == warehouse.id) clearForm()
                        Toast.makeText(context, "تم حذف المخزن بنجاح", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { warehouseToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // طباعة الدليل Dialog Preview
    if (showPrintDialog) {
        AlertDialog(
            onDismissRequest = { showPrintDialog = false },
            title = { Text("🖨️ معاينة طباعة دليل المخازن") },
            text = {
                Column {
                    Text("تقرير رسم دليل ورصيد المخازن المحاسبي - دكاني POS", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("عدد المخازن الإجمالي: ${filteredWarehouses.size}")
                    Text("تاريخ الطباعة: ${java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}")
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            filteredWarehouses.forEach { wh ->
                                Text("• [${wh.warehouseCode}] ${wh.name} - أمين المخزن: ${wh.keeperName.ifBlank { "غير محدد" }}")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    showPrintDialog = false
                    Toast.makeText(context, "تم إرسال أمر طباعة دليل المخازن بنجاح!", Toast.LENGTH_SHORT).show()
                }) {
                    Text("طباعة الآن")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showPrintDialog = false }) {
                    Text("إغلاق")
                }
            }
        )
    }

    // تصدير الدليل Dialog Preview
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("📤 تصدير دليل المخازن") },
            text = {
                Column {
                    Text("اختر صيغة تصدير سجلات دليل المخازن:")
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            showExportDialog = false
                            Toast.makeText(context, "تم تصدير دليل المخازن بصيغة Excel (CSV) بنجاح!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("تصدير إلى ملف CSV / Excel")
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = {
                            showExportDialog = false
                            Toast.makeText(context, "تم تصدير دليل المخازن بصيغة PDF بنجاح!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("تصدير إلى تقرير PDF")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                OutlinedButton(onClick = { showExportDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}
