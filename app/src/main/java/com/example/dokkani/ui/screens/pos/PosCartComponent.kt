package com.example.dokkani.ui.screens.pos

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dokkani.data.local.entities.ProductUnitEntity
import com.example.dokkani.domain.pos.CartSummary
import com.example.dokkani.domain.pos.PosCartItem

/**
 * مكون سلة المبيعات المتقدم لنقطة البيع (Advanced POS Shopping Cart)
 * يدعم تفاصيل البند والباركود/الرمز، التغيير الديناميكي للوحدات المتعددة،
 * التحكم السلس بالكميات والإدخال المباشر، إضافة الملاحظات، وحفظ/تأكيد الحذف.
 */
@Composable
fun PosCartComponent(
    cartItems: List<PosCartItem>,
    cartSummary: CartSummary,
    currencySymbol: String = "ر.س",
    onQuantityChange: (cartItemId: String, newQty: Double) -> Unit,
    onUnitPriceChange: (cartItemId: String, newPrice: Double) -> Unit = { _, _ -> },
    onUnitChange: (cartItemId: String, newUnit: ProductUnitEntity) -> Unit = { _, _ -> },
    onNoteChange: (cartItemId: String, newNote: String) -> Unit = { _, _ -> },
    onRemoveItem: (cartItemId: String) -> Unit,
    onClearCart: () -> Unit,
    onCheckoutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showClearConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth()
        ) {
            // ترويسة السلة
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = "السلة",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "سلة المبيعات",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${cartItems.size}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                if (cartItems.isNotEmpty()) {
                    if (showClearConfirm) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "تأكيد؟",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                            Button(
                                onClick = {
                                    showClearConfirm = false
                                    onClearCart()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("مسح الكل", fontSize = 11.sp, color = Color.White)
                            }
                            OutlinedButton(
                                onClick = { showClearConfirm = false },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("إلغاء", fontSize = 11.sp)
                            }
                        }
                    } else {
                        IconButton(
                            onClick = { showClearConfirm = true },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("clear_cart_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "مسح السلة",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)

            // قائمة بنود السلة
            if (cartItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "السلة فارغة",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "امسح الباركود أو اختر من التايلات السريعة",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .animateContentSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(cartItems, key = { it.cartItemId }) { item ->
                        PosCartItemRow(
                            item = item,
                            currencySymbol = currencySymbol,
                            onQuantityChange = { newQty -> onQuantityChange(item.cartItemId, newQty) },
                            onUnitPriceChange = { newPrice -> onUnitPriceChange(item.cartItemId, newPrice) },
                            onUnitChange = { newUnit -> onUnitChange(item.cartItemId, newUnit) },
                            onNoteChange = { note -> onNoteChange(item.cartItemId, note) },
                            onRemove = { onRemoveItem(item.cartItemId) }
                        )
                    }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)

            // ملخص الحساب
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "المجموع الفرعي:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "%.2f %s".format(cartSummary.subtotal, currencySymbol),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (cartSummary.discount > 0.0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "الخصم المباشر:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "-%.2f %s".format(cartSummary.discount, currencySymbol),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                if (cartSummary.taxRatePercent > 0.0) {
                    val rateStr = if (cartSummary.taxRatePercent % 1.0 == 0.0) "${cartSummary.taxRatePercent.toInt()}%" else "%.1f%%".format(cartSummary.taxRatePercent)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "ضريبة القيمة المضافة ($rateStr):",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "%.2f %s".format(cartSummary.taxAmount, currencySymbol),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "الإجمالي الصافي:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "%.2f %s".format(cartSummary.finalTotal, currencySymbol),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // زر الدفع الرئيسي
            Button(
                onClick = onCheckoutClick,
                enabled = cartItems.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("checkout_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.PointOfSale,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "دفع وإصدار الفاتورة (%.2f %s)".format(cartSummary.finalTotal, currencySymbol),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * صف بند السلة المطور مع تفاصيل الصنف، قائمة تغيير الوحدات، إدخال الكمية المباشر، وتأكيد الحذف
 */
@Composable
fun PosCartItemRow(
    item: PosCartItem,
    currencySymbol: String,
    onQuantityChange: (Double) -> Unit,
    onUnitPriceChange: (Double) -> Unit = {},
    onUnitChange: (ProductUnitEntity) -> Unit = {},
    onNoteChange: (String) -> Unit = {},
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showQtyDialog by remember { mutableStateOf(false) }
    var showPriceDialog by remember { mutableStateOf(false) }
    var showNoteDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var unitMenuExpanded by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = modifier.fillMaxWidth().animateContentSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            // الصف الأول: الاسم، الباركود/الكود، الأزرار والتأكيد
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.productName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (item.isWeighted) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Scale,
                                contentDescription = "وزن",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    // تفاصيل كود الباركود والوسوم
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        if (item.productCode.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                            ) {
                                Text(
                                    text = item.productCode,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }

                        if (item.scaleBarcodeRaw != null) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer
                            ) {
                                Text(
                                    text = "ميزان 21",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }

                // أزرار الملاحظات والحذف
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showNoteDialog = true },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = if (item.notes.isBlank()) Icons.Default.EditNote else Icons.Default.Notes,
                            contentDescription = "ملاحظات الصنف",
                            tint = if (item.notes.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (!showDeleteConfirm) {
                        IconButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "حذف البند",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // الملاحظات إن وجدت
            if (item.notes.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "ملاحظة: ${item.notes}",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // عرض سعر الشراء الأصلي والكمية المشتراة كمرجع في المردودات
            if (item.originalInvoiceQuantity != null && item.originalInvoiceCostPrice != null) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFE8F5E9),
                    border = BorderStroke(1.dp, Color(0xFF81C784)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "سعر الشراء بالفاتورة الأصلية: %.2f %s".format(item.originalInvoiceCostPrice, currencySymbol),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B5E20)
                        )
                        Text(
                            text = "الكمية المشتراة: %.2f %s".format(item.originalInvoiceQuantity, item.unitName),
                            fontSize = 10.sp,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            }

            // شريط تأكيد الحذف بدلاً من الحذف الفوري المباشر
            if (showDeleteConfirm) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "تأكيد حذف الصنف من السلة؟",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Bold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Button(
                                onClick = onRemove,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("حذف", fontSize = 11.sp, color = Color.White)
                            }
                            OutlinedButton(
                                onClick = { showDeleteConfirm = false },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("إلغاء", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // الصف الثاني: منتقي الوحدات + السعر + أزرار الكمية السريعة + إجمالي السطر
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // منتقي الوحدات المتعددة والسعر الفردي
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Box {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                            modifier = Modifier.clickable {
                                if (item.availableUnits.size > 1) {
                                    unitMenuExpanded = true
                                } else {
                                    showPriceDialog = true
                                }
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "%.2f %s / %s".format(item.unitPrice, currencySymbol, item.unitName),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (item.availableUnits.size > 1) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "اختيار وحدة أخرى",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        // القائمة المنسدلة للوحدات المتعددة
                        DropdownMenu(
                            expanded = unitMenuExpanded,
                            onDismissRequest = { unitMenuExpanded = false }
                        ) {
                            item.availableUnits.forEach { unit ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = unit.unitName,
                                                fontWeight = if (unit.id == item.unitId) FontWeight.Bold else FontWeight.Normal
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = "%.2f %s".format(unit.sellingPrice, currencySymbol),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    },
                                    onClick = {
                                        unitMenuExpanded = false
                                        onUnitChange(unit)
                                    }
                                )
                            }
                        }
                    }

                    // زر تعديل السعر المباشر
                    IconButton(
                        onClick = { showPriceDialog = true },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EditNote,
                            contentDescription = "تعديل سعر الشراء",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // أدوات التحكم بالكمية الإضافية / المباشرة
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // زر إنقاص الكمية (-)
                    FilledTonalButton(
                        onClick = {
                            val step = if (item.isWeighted) 0.250 else 1.0
                            val newQty = (item.quantity - step).coerceAtLeast(0.0)
                            if (newQty <= 0.001) {
                                showDeleteConfirm = true
                            } else {
                                onQuantityChange(newQty)
                            }
                        },
                        modifier = Modifier.size(32.dp),
                        contentPadding = PaddingValues(0.dp),
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "إنقاص",
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // مربع عرض الكمية وإمكانية النقر للإدخال المباشر
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.clickable { showQtyDialog = true }
                    ) {
                        Text(
                            text = item.quantityFormatted,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // زر زيادة الكمية (+)
                    FilledTonalButton(
                        onClick = {
                            val step = if (item.isWeighted) 0.250 else 1.0
                            onQuantityChange(item.quantity + step)
                        },
                        modifier = Modifier.size(32.dp),
                        contentPadding = PaddingValues(0.dp),
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "زيادة",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // إجمالي البند الخطي (Total)
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "الإجمالي",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = "%.2f %s".format(item.totalPrice, currencySymbol),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }

    // نافذة الإدخال الرقمي المباشر للكمية (Direct Numeric Quantity Input Dialog)
    if (showQtyDialog) {
        CartItemQtyDialog(
            currentQty = item.quantity,
            unitName = item.unitName,
            isWeighted = item.isWeighted,
            onConfirm = { newQty ->
                showQtyDialog = false
                onQuantityChange(newQty)
            },
            onDismiss = { showQtyDialog = false }
        )
    }

    // نافذة الإدخال الرقمي المباشر لسعر الشراء/الوحدة
    if (showPriceDialog) {
        CartItemPriceDialog(
            currentPrice = item.unitPrice,
            productName = item.productName,
            unitName = item.unitName,
            currencySymbol = currencySymbol,
            onConfirm = { newPrice ->
                showPriceDialog = false
                onUnitPriceChange(newPrice)
            },
            onDismiss = { showPriceDialog = false }
        )
    }

    // نافذة إدخال الملاحظات والبيانات الفرعية للصنف
    if (showNoteDialog) {
        CartItemNoteDialog(
            initialNote = item.notes,
            productName = item.productName,
            onConfirm = { note ->
                showNoteDialog = false
                onNoteChange(note)
            },
            onDismiss = { showNoteDialog = false }
        )
    }
}

/**
 * نافذة الإدخال الرقمي المباشر لسعر الشراء والوحدة
 */
@Composable
private fun CartItemPriceDialog(
    currentPrice: Double,
    productName: String,
    unitName: String,
    currencySymbol: String,
    onConfirm: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var priceText by remember {
        mutableStateOf(if (currentPrice % 1.0 == 0.0) currentPrice.toInt().toString() else "%.2f".format(currentPrice))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "تعديل سعر الشراء والتكلفة ($unitName)",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = productName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("سعر الشراء والتكلفة ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cart_price_input_field")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = priceText.toDoubleOrNull() ?: currentPrice
                    onConfirm(p.coerceAtLeast(0.0))
                }
            ) {
                Text("حفظ السعر")
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
 * نافذة الإدخال الرقمي المباشر للكميات مع خيارات إضافية سريعة
 */
@Composable
private fun CartItemQtyDialog(
    currentQty: Double,
    unitName: String,
    isWeighted: Boolean,
    onConfirm: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var qtyText by remember {
        mutableStateOf(if (currentQty % 1.0 == 0.0) currentQty.toInt().toString() else "%.3f".format(currentQty))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "تعديل الكمية المباشر ($unitName)",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.matches(Regex("""^\d*\.?\d*$"""))) {
                            qtyText = input
                        }
                    },
                    label = { Text("الكمية المطلوب بيعها") },
                    suffix = { Text(unitName) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("cart_item_qty_input")
                )

                // خيارات إضافية سريعة +1, +5, +10, الخ
                Text("إضافة سريعة:", style = MaterialTheme.typography.labelMedium)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val presets = if (isWeighted) listOf(0.25, 0.5, 1.0, 2.0, 5.0) else listOf(1.0, 2.0, 5.0, 10.0, 24.0)
                    presets.forEach { delta ->
                        SuggestionChip(
                            onClick = {
                                val current = qtyText.toDoubleOrNull() ?: 0.0
                                val updated = current + delta
                                qtyText = if (updated % 1.0 == 0.0) updated.toInt().toString() else "%.3f".format(updated)
                            },
                            label = { Text("+$delta") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = qtyText.toDoubleOrNull() ?: currentQty
                    onConfirm(parsed.coerceAtLeast(0.001))
                }
            ) {
                Text("تأكيد")
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
 * نافذة إدخال الملاحظات/الرقم التسلسلي/الدفعة الفرعية للصنف
 */
@Composable
private fun CartItemNoteDialog(
    initialNote: String,
    productName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var noteText by remember { mutableStateOf(initialNote) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "ملاحظات وتفاصيل الصنف",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = productName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("أدخل ملاحظات البند أو رقم الدفعة") },
                    placeholder = { Text("مثال: تغليف خاص، خصم مخصص، رقم التشغيلة...") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("cart_item_note_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(noteText.trim()) }
            ) {
                Text("حفظ الملاحظة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
