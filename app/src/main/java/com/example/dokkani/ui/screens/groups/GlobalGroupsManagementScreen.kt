package com.example.dokkani.ui.screens.groups

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dokkani.data.local.entities.GlobalGroupEntity
import com.example.dokkani.data.local.entities.UserRole

data class EntityTypeModule(
    val code: String,
    val nameArabic: String,
    val iconColor: Color
)

val STANDARD_ENTITY_MODULES = listOf(
    EntityTypeModule("ALL", "كافة الكيانات", Color(0xFF1976D2)),
    EntityTypeModule("PRODUCT", "الأصناف والمنتجات", Color(0xFF2E7D32)),
    EntityTypeModule("PARTY", "العملاء والموردين", Color(0xFF0288D1)),
    EntityTypeModule("EXPENSE", "المصروفات والخزينة", Color(0xFFED6C02)),
    EntityTypeModule("FINANCIAL_ACCOUNT", "الحسابات المالية", Color(0xFF9C27B0)),
    EntityTypeModule("EMPLOYEE", "الموظفين والرواتب", Color(0xFF009688)),
    EntityTypeModule("FIXED_ASSET", "الأصول الثابتة", Color(0xFF795548)),
    EntityTypeModule("COST_CENTER", "مراكز التكلفة", Color(0xFFD32F2F))
)

fun getModuleArabicName(code: String): String {
    val found = STANDARD_ENTITY_MODULES.find { it.code.equals(code, ignoreCase = true) }
    return found?.nameArabic ?: code
}

fun parseHexColor(hex: String): Color {
    return try {
        val cleaned = hex.removePrefix("#")
        val colorInt = cleaned.toLong(16)
        if (cleaned.length == 6) {
            Color(colorInt or 0xFF000000)
        } else {
            Color(colorInt)
        }
    } catch (e: Exception) {
        Color(0xFF1976D2)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalGroupsManagementScreen(
    viewModel: GlobalGroupsViewModel = viewModel(),
    currentUserRole: UserRole = UserRole.ADMIN,
    onNavigateBack: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openAddDialog(uiState.selectedEntityTypeFilter) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_global_group_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = "إضافة مجموعة")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إضافة مجموعة", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Header Bar
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "رجوع",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Category,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "نظام المجموعات والتصنيفات الديناميكية",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "تصنيف شامل لكافة كيانات وأقسام النظام (الأصناف، العملاء، المصروفات، الحسابات، إلخ)",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Entity Type Module Filters Row
            Text(
                "اختر نوع الكيان / القسم للتصنيف:",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                items(STANDARD_ENTITY_MODULES) { module ->
                    val isSelected = uiState.selectedEntityTypeFilter.equals(module.code, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectEntityTypeFilter(module.code) },
                        label = {
                            Text(
                                module.nameArabic,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(module.iconColor, shape = CircleShape)
                            )
                        },
                        modifier = Modifier.testTag("filter_chip_${module.code}")
                    )
                }
            }

            // Search Box & Count Summary Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::updateSearchQuery,
                    placeholder = { Text("بحث باسم أو كود المجموعة...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "مسح")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("global_groups_search_input")
                )

                Spacer(modifier = Modifier.width(12.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        "${uiState.filteredGroups.size} مجموعة",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp)
                    )
                }
            }

            // Group List
            if (uiState.filteredGroups.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Folder,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "لا توجد مجموعات معرفة لهذا التصنيف حالياً",
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.outline,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.openAddDialog(uiState.selectedEntityTypeFilter) },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("إضافة أول مجموعة الآن")
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(uiState.filteredGroups, key = { it.id }) { group ->
                        GlobalGroupCardItem(
                            group = group,
                            allGroups = uiState.groups,
                            onEdit = { viewModel.openEditDialog(group) },
                            onDelete = { viewModel.requestDeleteGroup(group) }
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Dialog
    if (uiState.showAddEditDialog) {
        GlobalGroupAddEditDialog(
            uiState = uiState,
            onDismiss = viewModel::dismissAddEditDialog,
            onInputsChanged = viewModel::updateDialogInputs,
            onSave = viewModel::saveGroup
        )
    }

    // Delete Confirmation Dialog
    if (uiState.showDeleteDialog && uiState.groupToDelete != null) {
        val group = uiState.groupToDelete!!
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteDialog,
            title = { Text("تأكيد حذف المجموعة") },
            text = { Text("هل أنت تأكد من رغبتك في حذف المجموعة \"${group.name}\"؟") },
            confirmButton = {
                Button(
                    onClick = viewModel::confirmDeleteGroup,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = viewModel::dismissDeleteDialog) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun GlobalGroupCardItem(
    group: GlobalGroupEntity,
    allGroups: List<GlobalGroupEntity>,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val groupColor = parseHexColor(group.colorHex)
    val parentName = remember(group.parentId, allGroups) {
        if (group.parentId != null) {
            allGroups.find { it.id == group.parentId }?.name
        } else null
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("global_group_item_${group.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color indicator bar
            Box(
                modifier = Modifier
                    .size(width = 8.dp, height = 48.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(groupColor)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = group.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (group.code.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f)
                        ) {
                            Text(
                                text = group.code,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = groupColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = getModuleArabicName(group.entityType),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = groupColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (parentName != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "فرع من: $parentName",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (group.description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = group.description,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(
                onClick = onEdit,
                modifier = Modifier.testTag("edit_group_button_${group.id}")
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "تعديل",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_group_button_${group.id}")
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "حذف",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalGroupAddEditDialog(
    uiState: GlobalGroupsUiState,
    onDismiss: () -> Unit,
    onInputsChanged: (
        name: String,
        code: String,
        entityType: String,
        isCustom: Boolean,
        customEntityType: String,
        parentId: Long?,
        description: String,
        colorHex: String
    ) -> Unit,
    onSave: () -> Unit
) {
    val isEdit = uiState.editingGroup != null

    var name by remember { mutableStateOf(uiState.nameInput) }
    var code by remember { mutableStateOf(uiState.codeInput) }
    var entityType by remember { mutableStateOf(uiState.entityTypeInput) }
    var isCustom by remember { mutableStateOf(uiState.isCustomEntityType) }
    var customEntityType by remember { mutableStateOf(uiState.customEntityTypeInput) }
    var parentId by remember { mutableStateOf(uiState.parentIdInput) }
    var description by remember { mutableStateOf(uiState.descriptionInput) }
    var selectedColorHex by remember { mutableStateOf(uiState.colorHexInput) }

    var moduleMenuExpanded by remember { mutableStateOf(false) }
    var parentMenuExpanded by remember { mutableStateOf(false) }

    val availableParentGroups = remember(entityType, isCustom, customEntityType, uiState.groups, uiState.editingGroup) {
        val targetType = if (isCustom || entityType == "CUSTOM") customEntityType.uppercase() else entityType
        uiState.groups.filter {
            it.entityType.equals(targetType, ignoreCase = true) &&
                    it.id != (uiState.editingGroup?.id ?: -1L)
        }
    }

    val presetColors = listOf(
        "#1976D2", "#2E7D32", "#0288D1", "#ED6C02",
        "#9C27B0", "#009688", "#795548", "#D32F2F",
        "#455A64", "#C2185B", "#3F51B5", "#8E24AA"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (isEdit) "تعديل المجموعة" else "إضافة مجموعة جديدة",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Name Input
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        onInputsChanged(name, code, entityType, isCustom, customEntityType, parentId, description, selectedColorHex)
                    },
                    label = { Text("اسم المجموعة *") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_group_name_input")
                )

                // Code Input
                OutlinedTextField(
                    value = code,
                    onValueChange = {
                        code = it
                        onInputsChanged(name, code, entityType, isCustom, customEntityType, parentId, description, selectedColorHex)
                    },
                    label = { Text("كود / رمز المجموعة (اختياري)") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Module/EntityType Selector
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = if (isCustom || entityType == "CUSTOM") "نوع كيان مخصص ($customEntityType)" else getModuleArabicName(entityType),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("نوع الكيان / القسم المرتبط *") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { moduleMenuExpanded = true }
                    )

                    DropdownMenu(
                        expanded = moduleMenuExpanded,
                        onDismissRequest = { moduleMenuExpanded = false }
                    ) {
                        STANDARD_ENTITY_MODULES.filter { it.code != "ALL" }.forEach { module ->
                            DropdownMenuItem(
                                text = { Text(module.nameArabic) },
                                onClick = {
                                    entityType = module.code
                                    isCustom = false
                                    parentId = null
                                    moduleMenuExpanded = false
                                    onInputsChanged(name, code, entityType, isCustom, customEntityType, parentId, description, selectedColorHex)
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("➕ نوع كيان مخصص جديد...") },
                            onClick = {
                                entityType = "CUSTOM"
                                isCustom = true
                                parentId = null
                                moduleMenuExpanded = false
                                onInputsChanged(name, code, entityType, isCustom, customEntityType, parentId, description, selectedColorHex)
                            }
                        )
                    }
                }

                // Custom Entity Type Input
                AnimatedVisibility(visible = isCustom || entityType == "CUSTOM") {
                    OutlinedTextField(
                        value = customEntityType,
                        onValueChange = {
                            customEntityType = it
                            onInputsChanged(name, code, entityType, isCustom, customEntityType, parentId, description, selectedColorHex)
                        },
                        label = { Text("أدخل رمز نوع الكيان المخصص (مثل: VEHICLES)") },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Parent Group Selector
                if (availableParentGroups.isNotEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        val parentText = availableParentGroups.find { it.id == parentId }?.name ?: "بدون مجموعة أب (مجموعة رئيسية)"
                        OutlinedTextField(
                            value = parentText,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("المجموعة الأب (للمجموعات الهرمية الفرعية)") },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { parentMenuExpanded = true }
                        )

                        DropdownMenu(
                            expanded = parentMenuExpanded,
                            onDismissRequest = { parentMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("بدون مجموعة أب (مجموعة رئيسية)") },
                                onClick = {
                                    parentId = null
                                    parentMenuExpanded = false
                                    onInputsChanged(name, code, entityType, isCustom, customEntityType, parentId, description, selectedColorHex)
                                }
                            )
                            availableParentGroups.forEach { parent ->
                                DropdownMenuItem(
                                    text = { Text(parent.name) },
                                    onClick = {
                                        parentId = parent.id
                                        parentMenuExpanded = false
                                        onInputsChanged(name, code, entityType, isCustom, customEntityType, parentId, description, selectedColorHex)
                                    }
                                )
                            }
                        }
                    }
                }

                // Color Hex Selection
                Column {
                    Text(
                        "اختيار اللون المميز للمجموعة:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(presetColors) { colorHex ->
                            val color = parseHexColor(colorHex)
                            val isSelected = selectedColorHex.equals(colorHex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        selectedColorHex = colorHex
                                        onInputsChanged(name, code, entityType, isCustom, customEntityType, parentId, description, selectedColorHex)
                                    }
                            )
                        }
                    }
                }

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        description = it
                        onInputsChanged(name, code, entityType, isCustom, customEntityType, parentId, description, selectedColorHex)
                    },
                    label = { Text("وصف أو ملاحظات المجموعة (اختياري)") },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                modifier = Modifier.testTag("save_global_group_button")
            ) {
                Text(if (isEdit) "حفظ التعديلات" else "إضافة المجموعة")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
