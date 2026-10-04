package com.example.dokkani.ui.screens.assembly

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dokkani.data.local.entities.AssemblyStatus
import com.example.dokkani.data.local.entities.ItemAssemblyWithComponents
import com.example.dokkani.ui.components.AppSearchBar
import com.example.dokkani.ui.components.NumericOutlinedTextField
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * شاشة تركيبة الأصناف وتجميع المنتجات المركبة (Bill of Materials & Item Assembly Screen)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemAssemblyScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: ItemAssemblyViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0: إنشاء وتجميع، 1: سجل المجمعات السابقة
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale("ar")) }

    // إظهار تنبيهات التغذية الراجعة
    LaunchedEffect(uiState.feedbackMessage) {
        uiState.feedbackMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.dismissFeedback()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .testTag("item_assembly_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. الترويسة العلوية للشاشة (Header)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Widgets,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "تركيب وتجميع الأصناف (Bill of Materials)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "تجميع الخامات والأصناف الفرعية لإنتاج صنف نهائي وتحديث المخزن تلقائياً",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 2. تبويبات التنقل (Tabs Navigation)
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("إنشاء وتجميع صنف", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                icon = { Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("سجل السندات المجمعة (${uiState.assembliesHistory.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                icon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }

        // 3. المحتوى حسب التبويب المختار
        if (selectedTab == 0) {
            AssemblyFormSection(
                uiState = uiState,
                viewModel = viewModel,
                onSwitchToHistory = { selectedTab = 1 }
            )
        } else {
            AssemblyHistorySection(
                uiState = uiState,
                viewModel = viewModel,
                dateFormat = dateFormat,
                onEdit = {
                    viewModel.editAssembly(it)
                    selectedTab = 0
                }
            )
        }
    }

    // نافذة اختيار المكونات والخامات الفرعية
    if (uiState.showComponentPickerModal) {
        ComponentPickerModal(
            products = uiState.products,
            searchQuery = uiState.componentSearchQuery,
            onSearchQueryChange = viewModel::setComponentSearchQuery,
            onSelectProduct = viewModel::addComponent,
            onDismiss = { viewModel.toggleComponentPickerModal(false) }
        )
    }

    // نافذة تأكيد حذف سند التركيب
    if (uiState.showDeleteConfirmDialog && uiState.assemblyToDelete != null) {
        val target = uiState.assemblyToDelete!!
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteDialog,
            title = {
                Text("حذف سند التركيب وعكس المخزون", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Text("هل أنت أصل متأكد من حذف سند التركيب رقم (${target.assembly.assemblyNumber}) الخاص بمنتج [${target.assembly.finishedProductName}]؟\n\nسيتم عكس حركات المخزون واسترجاع الكميات المخصومة من الخامات وإلغاء الكمية المنتجة تلقائياً.")
            },
            confirmButton = {
                Button(
                    onClick = viewModel::confirmDeleteAssembly,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("حذف وعكس المخزون", fontWeight = FontWeight.Bold)
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

/**
 * قسم نموذج إدخال وتشكيل صنف مجمع جديد
 */
@Composable
private fun AssemblyFormSection(
    uiState: ItemAssemblyUiState,
    viewModel: ItemAssemblyViewModel,
    onSwitchToHistory: () -> Unit
) {
    val scrollState = rememberScrollState()
    val producedQty = uiState.producedQuantityInput.toDoubleOrNull() ?: 1.0

    // الحسابات المالية التقديرية
    val totalCostPerUnit = uiState.selectedComponents.sumOf { it.getTotalCost(1.0) }
    val grandTotalAssemblyCost = totalCostPerUnit * producedQty
    val sellingPricePerUnit = uiState.selectedFinishedProduct?.sellPrice ?: 0.0
    val totalSellingRevenue = sellingPricePerUnit * producedQty
    val profitMargin = totalSellingRevenue - grandTotalAssemblyCost
    val profitPercentage = if (totalSellingRevenue > 0) (profitMargin / totalSellingRevenue) * 100 else 0.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. بطاقة اختيار الصنف النهائي المراد إنتاجه (Finished Product Selection Card)
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Category,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "1. الصنف النهائي المراد إنتاجه وتجميعه:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // اختيار الصنف النهائي من القائمة
                FinishedProductSelector(
                    products = uiState.products,
                    selectedProduct = uiState.selectedFinishedProduct,
                    onSelect = viewModel::selectFinishedProduct
                )

                if (uiState.selectedFinishedProduct != null) {
                    val finished = uiState.selectedFinishedProduct
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = "المخزون المتوفر حالياً: %.2f %s".format(Locale.US, finished.currentStock, finished.unitName),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = "سعر البيع الحالي: %.2f %s".format(Locale.US, finished.sellPrice, uiState.baseCurrencySymbol),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // كمية الإنتاج والتجميع المطلوبة
                NumericOutlinedTextField(
                    value = uiState.producedQuantityInput,
                    onValueChange = viewModel::setProducedQuantity,
                    label = { Text("الكمية المراد إنتاجها وتجميعها الآن", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }

        // 2. بطاقة جدول المكونات الفرعية والمواد الخام (Bill of Materials Components Table)
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Extension,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "2. المكونات والخامات الفرعية للتركيب:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Button(
                        onClick = { viewModel.toggleComponentPickerModal(true) },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("add_component_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إضافة خامة / مكون", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (uiState.selectedComponents.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.PlaylistAdd,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "لم يتم إضافة أي خامات أو مكونات بعد.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "اضغط على زر [إضافة خامة / مكون] لاختيار المواد الخام اللازمة للتجميع.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        uiState.selectedComponents.forEach { comp ->
                            ComponentRowItem(
                                compState = comp,
                                producedQty = producedQty,
                                currencySymbol = uiState.baseCurrencySymbol,
                                onUpdateQty = { newQty -> viewModel.updateComponentQuantity(comp.product.id, newQty) },
                                onUpdateCost = { newCost -> viewModel.updateComponentCost(comp.product.id, newCost) },
                                onRemove = { viewModel.removeComponent(comp.product.id) }
                            )
                        }
                    }
                }
            }
        }

        // 3. الخلاصة والملخص المالي والتكاليف الأرباح المقدرة (Financial & Profit Summary Card)
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "3. ملخص التكاليف والهامش الربحي التقديري:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("تكلفة المكونات للقطعة الواحدة:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        text = "%.2f %s".format(Locale.US, totalCostPerUnit, uiState.baseCurrencySymbol),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("إجمالي تكلفة عملية التجميع ($producedQty قطعة):", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        text = "%.2f %s".format(Locale.US, grandTotalAssemblyCost, uiState.baseCurrencySymbol),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("إجمالي إيراد البيع المتوقع:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        text = "%.2f %s".format(Locale.US, totalSellingRevenue, uiState.baseCurrencySymbol),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("ربح التجميع المتوقع (هامش الربح):", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "%.2f %s (%.1f%%)".format(Locale.US, profitMargin, uiState.baseCurrencySymbol, profitPercentage),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (profitMargin >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }

                // حقل ملاحظات وتفاصيل عملية التركيب
                OutlinedTextField(
                    value = uiState.notesInput,
                    onValueChange = viewModel::setNotesInput,
                    label = { Text("ملاحظات وتفاصيل عملية التركيب (اختياري)", fontSize = 11.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }

        // 4. أزرار الإجراءات والاعتماد (Actions Footer)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { viewModel.executeSaveAssembly(isDraft = false) },
                enabled = !uiState.isProcessing,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1.5f)
                    .height(48.dp)
                    .testTag("approve_assembly_button")
            ) {
                if (uiState.isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                } else {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("اعتماد السند وتحديث المخزون", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            OutlinedButton(
                onClick = { viewModel.executeSaveAssembly(isDraft = true) },
                enabled = !uiState.isProcessing,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("حفظ كمسودة", fontSize = 12.sp)
            }

            if (uiState.editingAssemblyId != null) {
                TextButton(
                    onClick = viewModel::resetForm,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("إلغاء", fontSize = 12.sp)
                }
            }
        }
    }
}

/**
 * عنصر صف المادة الخام أو المكون الفرعي داخل النموذج
 */
@Composable
private fun ComponentRowItem(
    compState: AssemblyComponentItemState,
    producedQty: Double,
    currencySymbol: String,
    onUpdateQty: (Double) -> Unit,
    onUpdateCost: (Double) -> Unit,
    onRemove: () -> Unit
) {
    val totalRequired = compState.getTotalQuantityUsed(producedQty)
    val totalCost = compState.getTotalCost(producedQty)
    val hasEnoughStock = compState.product.currentStock >= totalRequired

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (hasEnoughStock) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
        border = BorderStroke(1.dp, if (hasEnoughStock) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = compState.product.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "كود: ${compState.product.code} | المتاح بالمخزن: %.2f ${compState.product.unitName}".format(Locale.US, compState.product.currentStock),
                        fontSize = 10.sp,
                        color = if (hasEnoughStock) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                    )
                }

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "حذف المكون", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // الكمية الفردية للقطعة الواحدة
                NumericOutlinedTextField(
                    value = compState.quantityPerUnit.toString(),
                    onValueChange = { onUpdateQty(it.toDoubleOrNull() ?: 0.0) },
                    label = { Text("الكمية للوحدة", fontSize = 9.sp) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                )

                // التكلفة الفردية للوحدة
                NumericOutlinedTextField(
                    value = compState.unitCostPrice.toString(),
                    onValueChange = { onUpdateCost(it.toDoubleOrNull() ?: 0.0) },
                    label = { Text("سعر التكلفة", fontSize = 9.sp) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                )

                // إجمالي التكلفة للسطر
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text("إجمالي التكلفة:", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "%.2f %s".format(Locale.US, totalCost, currencySymbol),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * مكون اختيارات الصنف النهائي المفصل
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FinishedProductSelector(
    products: List<AssemblyProductItem>,
    selectedProduct: AssemblyProductItem?,
    onSelect: (AssemblyProductItem) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedProduct?.name ?: "اختر المنتج المركب / الصنف النهائي...",
            onValueChange = {},
            readOnly = true,
            label = { Text("الصنف النهائي المركب *", fontSize = 11.sp) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            products.forEach { prod ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(prod.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("كود: ${prod.code} | القسم: ${prod.category}", fontSize = 10.sp, color = Color.Gray)
                        }
                    },
                    onClick = {
                        onSelect(prod)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * نافذة حوار البحث واختيار المواد الخام والمكونات
 */
@Composable
private fun ComponentPickerModal(
    products: List<AssemblyProductItem>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSelectProduct: (AssemblyProductItem) -> Unit,
    onDismiss: () -> Unit
) {
    val filteredProducts = remember(products, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isBlank()) products
        else products.filter {
            it.name.lowercase().contains(q) || it.code.lowercase().contains(q) || it.category.lowercase().contains(q)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("اختيار مادة خام / صنف فرعي للتركيب", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AppSearchBar(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = "ابحث باسم الخامات أو الكود..."
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredProducts) { prod ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectProduct(prod) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(prod.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text("كود: ${prod.code} | التكلفة: %.2f".format(Locale.US, prod.costPrice), fontSize = 10.sp, color = Color.Gray)
                                }
                                Icon(Icons.Default.AddCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("إغلاق") }
        }
    )
}

/**
 * قسم سجل سندات التجميع السابقة
 */
@Composable
private fun AssemblyHistorySection(
    uiState: ItemAssemblyUiState,
    viewModel: ItemAssemblyViewModel,
    dateFormat: SimpleDateFormat,
    onEdit: (ItemAssemblyWithComponents) -> Unit
) {
    if (uiState.assembliesHistory.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.HistoryToggleOff,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("لا توجد أي سندات تركيب أو تجميع مسجلة سابقاً.", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(uiState.assembliesHistory) { itemWithComps ->
                val assembly = itemWithComps.assembly
                var isExpanded by remember { mutableStateOf(false) }

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (assembly.status == AssemblyStatus.APPROVED) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Text(
                                        text = assembly.status.labelArabic,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (assembly.status == AssemblyStatus.APPROVED) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = assembly.assemblyNumber,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Text(
                                text = dateFormat.format(Date(assembly.date)),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "المنتج المجمع: ${assembly.finishedProductName} (كود: ${assembly.finishedProductCode})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("الكمية المنتجة: %.2f".format(Locale.US, assembly.producedQuantity), fontSize = 11.sp)
                            Text("التكلفة الإجمالية: %.2f %s".format(Locale.US, assembly.totalAssemblyCost, uiState.baseCurrencySymbol), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        if (assembly.notes.isNotBlank()) {
                            Text("ملاحظات: ${assembly.notes}", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                        }

                        Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { isExpanded = !isExpanded },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(if (isExpanded) "إخفاء التفاصيل والخامات ▲" else "عرض المكونات والخامات (${itemWithComps.components.size}) ▼", fontSize = 11.sp)
                            }

                            Row {
                                IconButton(onClick = { onEdit(itemWithComps) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                }
                                IconButton(onClick = { viewModel.requestDeleteAssembly(itemWithComps) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف وعكس المخزون", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        AnimatedVisibility(visible = isExpanded) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("المكونات والمواد الخام المستخدمة:", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                itemWithComps.components.forEach { comp ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("• ${comp.componentProductName} (${comp.unitName})", fontSize = 11.sp)
                                        Text("الكمية: %.2f | التكلفة: %.2f".format(Locale.US, comp.totalQuantityUsed, comp.totalCostPrice), fontSize = 10.sp, color = Color.Gray)
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
