package com.example.dokkani.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.dokkani.ui.screens.crud.AddEditProductDialog
import com.example.dokkani.ui.screens.crud.AddEditUnitDialog
import com.example.dokkani.ui.screens.crud.ConfirmDeleteDialog

@Composable
fun ProductsAndUnitsScreen(
    productsWithUnits: List<ProductWithUnits>,
    currentUserRole: UserRole = UserRole.ADMIN,
    onSaveProduct: (ProductEntity, String, Double, Double, String) -> Unit = { _, _, _, _, _ -> },
    onDeleteProduct: (Long) -> Unit = {},
    onSaveUnit: (ProductUnitEntity) -> Unit = {},
    onDeleteUnit: (ProductUnitEntity) -> Unit = {},
    onPrintLabel: ((productId: Long, unitId: Long) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showAddProductDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var deletingProductId by remember { mutableStateOf<Long?>(null) }

    var addingUnitProductId by remember { mutableStateOf<Long?>(null) }
    var editingUnit by remember { mutableStateOf<ProductUnitEntity?>(null) }
    var deletingUnit by remember { mutableStateOf<ProductUnitEntity?>(null) }

    val isAdmin = currentUserRole == UserRole.ADMIN

    Box(modifier = modifier.fillMaxSize()) {
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "قائمة الأصناف المسجلة بالمتجر (${productsWithUnits.size}):",
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

            items(productsWithUnits) { item ->
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
                                onPrintLabel = onPrintLabel
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }
        }
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
            message = "هل أنت ألكيد من رغبتك في حذف هذا الصنف وجميع وحداته التابعة له؟",
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
            message = "هل أنت ألكيد من حذف الوحدة '${deletingUnit?.unitName}'؟",
            onConfirm = {
                onDeleteUnit(deletingUnit!!)
                deletingUnit = null
            },
            onDismiss = { deletingUnit = null }
        )
    }
}

@Composable
private fun UnitItemRow(
    productId: Long,
    unit: ProductUnitEntity,
    isAdmin: Boolean,
    onEditUnit: (ProductUnitEntity) -> Unit,
    onDeleteUnit: (ProductUnitEntity) -> Unit,
    onPrintLabel: ((productId: Long, unitId: Long) -> Unit)? = null
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
                    text = "%.2f ر.س".format(unit.sellingPrice),
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
