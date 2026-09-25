package com.example.dokkani.ui.screens.costcenters

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dokkani.data.local.entities.CostCenterEntity
import com.example.dokkani.data.local.entities.UserRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CostCentersManagementScreen(
    viewModel: CostCentersViewModel = viewModel(),
    currentUserRole: UserRole = UserRole.ADMIN,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isAdmin = currentUserRole == UserRole.ADMIN

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("إدارة مراكز التكلفة (Cost Centers)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("ربط الأقسام، المصروفات، والمشتريات والربحية", fontSize = 11.sp, color = Color.LightGray)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF1B4D3E),
                        titleContentColor = Color.White
                    ),
                    actions = {
                        if (isAdmin) {
                            IconButton(onClick = { viewModel.openAddDialog() }) {
                                Icon(Icons.Default.Add, contentDescription = "إضافة مركز تكلفة جديد", tint = Color.White)
                            }
                        }
                    }
                )
            },
            floatingActionButton = {
                if (isAdmin) {
                    FloatingActionButton(
                        onClick = { viewModel.openAddDialog() },
                        containerColor = Color(0xFF1B5E20),
                        contentColor = Color.White
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("مركز تكلفة جديد", fontWeight = FontWeight.Bold)
                        }
                    }
                }
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                if (!isAdmin) {
                    // شاشة حظر عدم الصلاحية (لغير مدير النظام)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.Security,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "تنبيه صلاحيات الوصول!",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "شاشة إدارة وإنشاء مراكز التكلفة مخصصة حصرياً لـ 'مدير النظام (Admin)' لضمان سلامة التوجيه المحاسبي والدفاتر.",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                } else {
                    // القائمة الرئيسية لمراكز التكلفة
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // كارت ملخص مراكز التكلفة
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                                border = BorderStroke(1.dp, Color(0xFF81C784)),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Business, contentDescription = null, tint = Color(0xFF1B5E20))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "هيكلية مراكز التكلفة والأقسام",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Color(0xFF1B5E20)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        "ملاحظة هامة: تبدأ مراكز التكلفة فارغة ويقوم المستخدم بإنشائها يدوياً (مثل: قسم الخضار، قسم المواد الغذائية، قسم المخبوزات...)، باستثناء 'مركز التكلفة العام' الافتراضي المعتمد للعمليات العامة.",
                                        fontSize = 12.sp,
                                        color = Color(0xFF2E7D32),
                                        lineHeight = 18.sp
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        val totalCount = uiState.costCenters.size
                                        val activeCount = uiState.costCenters.count { it.isActive }
                                        val customCount = uiState.costCenters.count { !it.isGeneral }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color.White,
                                            modifier = Modifier.weight(1f).padding(end = 4.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(8.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text("إجمالي المراكز", fontSize = 10.sp, color = Color.Gray)
                                                Text("$totalCount", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1B5E20))
                                            }
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color.White,
                                            modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(8.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text("مراكز مخصصة", fontSize = 10.sp, color = Color.Gray)
                                                Text("$customCount", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1565C0))
                                            }
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color.White,
                                            modifier = Modifier.weight(1f).padding(start = 4.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(8.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text("المراكز النشطة", fontSize = 10.sp, color = Color.Gray)
                                                Text("$activeCount", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF2E7D32))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // عنوان قائمة المراكز
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("قائمة مراكز التكلفة المسجلة:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                TextButton(onClick = { viewModel.openAddDialog() }) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("إضافة مركز", fontSize = 12.sp)
                                }
                            }
                        }

                        // عناصر مراكز التكلفة
                        items(uiState.costCenters, key = { it.centerId }) { center ->
                            CostCenterCardItem(
                                center = center,
                                onEdit = { viewModel.openEditDialog(center) },
                                onDelete = { viewModel.onRequestDelete(center) }
                            )
                        }

                        item { Spacer(modifier = Modifier.height(60.dp)) }
                    }
                }

                // حوار إضافة / تعديل مركز تكلفة
                if (uiState.showAddEditDialog) {
                    AddEditCostCenterDialog(
                        uiState = uiState,
                        onDismiss = { viewModel.dismissAddEditDialog() },
                        onSave = { viewModel.saveCostCenter() },
                        onUpdateInputs = { name, code, desc, active ->
                            viewModel.updateInputs(name, code, desc, active)
                        }
                    )
                }

                // حوار تأكيد الحذف المحمي
                if (uiState.showDeleteConfirmDialog && uiState.pendingDeleteCenter != null) {
                    DeleteCostCenterConfirmDialog(
                        uiState = uiState,
                        onDismiss = { viewModel.dismissDeleteConfirmDialog() },
                        onConfirmDelete = { viewModel.confirmDeleteCostCenter() }
                    )
                }
            }
        }
    }
}

@Composable
private fun CostCenterCardItem(
    center: CostCenterEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (center.isGeneral) Color(0xFFF1F8E9) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
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
                        shape = CircleShape,
                        color = if (center.isGeneral) Color(0xFF1B5E20) else MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                if (center.isGeneral) Icons.Default.Store else Icons.Default.Business,
                                contentDescription = null,
                                tint = if (center.isGeneral) Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = center.centerName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            if (center.isGeneral) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = Color(0xFF2E7D32),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        "افتراضي عام",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        if (center.code.isNotBlank()) {
                            Text("الرمز: ${center.code}", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.primary)
                    }

                    if (!center.isGeneral) {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        IconButton(onClick = {}, enabled = false) {
                            Icon(Icons.Default.Lock, contentDescription = "محمي من الحذف", tint = Color.LightGray)
                        }
                    }
                }
            }

            if (center.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = center.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = if (center.isActive) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (center.isActive) Icons.Default.CheckCircle else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (center.isActive) Color(0xFF2E7D32) else Color(0xFFC62828),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (center.isActive) "حالة المركز: مفعل للربط" else "حالة المركز: معطل",
                            fontSize = 10.sp,
                            color = if (center.isActive) Color(0xFF2E7D32) else Color(0xFFC62828),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = "رقم المعرف: CC-${center.centerId}",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun AddEditCostCenterDialog(
    uiState: CostCenterUiState,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onUpdateInputs: (String?, String?, String?, Boolean?) -> Unit
) {
    val isEdit = uiState.editingCenter != null
    val isGeneral = uiState.editingCenter?.isGeneral == true

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEdit) "تعديل مركز التكلفة" else "إضافة مركز تكلفة جديد",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (isGeneral) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                        border = BorderStroke(1.dp, Color(0xFFFFB300))
                    ) {
                        Text(
                            text = "تنبيه: هذا هو 'مركز التكلفة العام' الافتراضي للنظام. يمكنك تعديل الاسم والوصف فقط، ولا يمكن تعطيله أو حذفه.",
                            fontSize = 11.sp,
                            color = Color(0xFF8D6E63),
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = uiState.nameInput,
                    onValueChange = { onUpdateInputs(it, null, null, null) },
                    label = { Text("اسم مركز التكلفة * (مثل: قسم الخضار)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = uiState.codeInput,
                    onValueChange = { onUpdateInputs(null, it, null, null) },
                    label = { Text("كود/رمز المركز (مثال: CC-002)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = uiState.descriptionInput,
                    onValueChange = { onUpdateInputs(null, null, it, null) },
                    label = { Text("الوصف أو الملاحظات") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (!isGeneral) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("تفعيل مركز التكلفة لاستخدامه بالمعاملات", fontSize = 12.sp)
                        Switch(
                            checked = uiState.isActiveInput,
                            onCheckedChange = { onUpdateInputs(null, null, null, it) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20))
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isEdit) "حفظ التعديلات" else "إضافة المركز")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
private fun DeleteCostCenterConfirmDialog(
    uiState: CostCenterUiState,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    val center = uiState.pendingDeleteCenter ?: return

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(32.dp))
        },
        title = {
            Text("تأكيد حذف مركز التكلفة", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("هل أنت أكتأكد من حذف مركز التكلفة '${center.centerName}'؟", fontSize = 13.sp)

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                    border = BorderStroke(1.dp, Color(0xFFFFB74D))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("سياسة الأمان والسلامة المحاسبية:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFFE65100))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "عند حذف هذا المركز، سيتم تحويل كافة الأصناف، الفواتير، والمصروفات المربوطة به إلى 'مركز التكلفة العام' تلقائياً لمنع أي خلل في القوائم المالية.",
                            fontSize = 11.sp,
                            color = Color(0xFFBF360C)
                        )

                        if (uiState.linkedProductsCount > 0 || uiState.linkedExpensesCount > 0 || uiState.linkedInvoicesCount > 0) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "العمليات المتأثرة: ${uiState.linkedProductsCount} منتجات | ${uiState.linkedExpensesCount} مصروفات | ${uiState.linkedInvoicesCount} فواتير.",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD84315)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmDelete,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("نعم، إتمام الحذف والتحويل")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
