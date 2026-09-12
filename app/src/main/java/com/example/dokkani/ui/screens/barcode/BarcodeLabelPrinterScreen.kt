package com.example.dokkani.ui.screens.barcode

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dokkani.data.local.entities.ProductUnitEntity
import com.example.dokkani.data.local.entities.ProductWithUnits
import com.example.dokkani.data.local.entities.SystemSettingsEntity
import com.example.dokkani.domain.barcode.BarcodeGenerator
import com.example.dokkani.domain.hardware.LabelPaperSize
import com.example.dokkani.domain.hardware.LabelPrintResult

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BarcodeLabelPrinterScreen(
    productsWithUnits: List<ProductWithUnits> = emptyList(),
    selectedProductId: Long? = null,
    selectedUnitId: Long? = null,
    labelPaperSize: LabelPaperSize = LabelPaperSize.SIZE_38X25,
    labelCopies: Int = 1,
    showStoreName: Boolean = true,
    showUnitName: Boolean = true,
    showPrice: Boolean = true,
    showBarcodeText: Boolean = true,
    showTaxNote: Boolean = false,
    customBarcode: String = "",
    isGeneratingBarcode: Boolean = false,
    isPrintingLabel: Boolean = false,
    lastPrintResult: LabelPrintResult? = null,
    settings: SystemSettingsEntity? = null,
    onSelectProduct: (Long) -> Unit = {},
    onSelectUnit: (Long) -> Unit = {},
    onPaperSizeChanged: (LabelPaperSize) -> Unit = {},
    onCopiesChanged: (Int) -> Unit = {},
    onToggleOption: (storeName: Boolean?, unitName: Boolean?, price: Boolean?, barcodeText: Boolean?, taxNote: Boolean?) -> Unit = { _, _, _, _, _ -> },
    onBarcodeChanged: (String) -> Unit = {},
    onGenerateUniqueBarcode: () -> Unit = {},
    onPrintLabel: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var showTsplDialog by remember { mutableStateOf(false) }

    val filteredProducts = remember(productsWithUnits, searchQuery) {
        if (searchQuery.isBlank()) productsWithUnits
        else productsWithUnits.filter {
            it.product.name.contains(searchQuery, ignoreCase = true) ||
                    it.product.code.contains(searchQuery, ignoreCase = true) ||
                    it.units.any { u -> u.barcode.contains(searchQuery, ignoreCase = true) || u.unitName.contains(searchQuery, ignoreCase = true) }
        }
    }

    val selectedProduct = productsWithUnits.firstOrNull { it.product.id == selectedProductId }
    val selectedUnit = selectedProduct?.units?.firstOrNull { it.id == selectedUnitId }
        ?: selectedProduct?.units?.firstOrNull()

    val currentBarcode = customBarcode.ifBlank {
        selectedUnit?.barcode?.ifBlank { "290001001001" } ?: "290001001001"
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("barcode_label_printer_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // بطاقة العنوان والتعريف
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0D3B2E)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF198754),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = "Barcode",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "مصمم وطابعة ملصقات الباركود (Barcode Label Printer)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "توليد باركود فريد للوحدات والعبوات المفردة والطباعة المباشرة لـ Bluetooth Printer",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFD1E7DD)
                        )
                    }
                }
            }
        }

        // اختيار الصنف والوحدة
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "1. اختيار الصنف والوحدة المراد طباعة ملصق لها:",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("ابحث باسم الصنف أو الكود أو الباركود...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "بحث") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // قائمة أفقية أو شبكة للأصناف
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        filteredProducts.take(8).forEach { pw ->
                            val isSelected = pw.product.id == selectedProductId
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSelectProduct(pw.product.id) },
                                label = {
                                    Text(
                                        text = pw.product.name,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Inventory2,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }

                    if (selectedProduct != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "الوحدات المتاحة لـ (${selectedProduct.product.name}):",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            selectedProduct.units.forEach { unit ->
                                val isUnitSelected = unit.id == selectedUnit?.id
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isUnitSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(
                                        width = if (isUnitSelected) 2.dp else 1.dp,
                                        color = if (isUnitSelected) MaterialTheme.colorScheme.primary else Color.LightGray
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onSelectUnit(unit.id) }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = unit.unitName,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = "%.2f ر.س".format(unit.sellingPrice),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF198754),
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (unit.barcode.isBlank()) {
                                            Text(
                                                text = "بدون باركود",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.Red,
                                                fontSize = 10.sp
                                            )
                                        } else {
                                            Text(
                                                text = unit.barcode.takeLast(6),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // مولد الباركود الفريد
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFFB0D7C9)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF4FBF7)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFF0F5132),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "مولد باركود فريد للوحدات المفردة (In-Store EAN-13)",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                color = Color(0xFF0F5132)
                            )
                        }

                        // شارة EAN-13 Modulo 10
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFD1E7DD)
                        ) {
                            Text(
                                text = "GS1 In-Store Prefix 290",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF0F5132),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "يولد كود باركود قياسي فريد EAN-13 مع حساب خانة التحقق تلقائياً للوحدات المجزأة من الكراتين (مثل الحبات/العبوات التي لا تحمل باركود مصنعي) ويقوم بتخزينه فورياً في جدول product_units.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF2C4A3E)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = currentBarcode,
                            onValueChange = { onBarcodeChanged(it) },
                            label = { Text("رمز الباركود الحالي للوحدة") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("barcode_input_field"),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = null) },
                            trailingIcon = {
                                if (BarcodeGenerator.isValidEan13(currentBarcode)) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "Valid EAN13",
                                        tint = Color(0xFF198754)
                                    )
                                }
                            }
                        )
                        Spacer(modifier = Modifier.width(10.dp))

                        Button(
                            onClick = { onGenerateUniqueBarcode() },
                            enabled = !isGeneratingBarcode && selectedProduct != null && selectedUnit != null,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F5132)),
                            modifier = Modifier.testTag("generate_barcode_button")
                        ) {
                            if (isGeneratingBarcode) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("توليد فريد")
                            }
                        }
                    }
                }
            }
        }

        // إعدادات مقاس الورق والنسخ ومحتويات الملصق
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "2. إعدادات مقاس ورق الملصق والنسخ:",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // اختيار المقاس
                    Text("مقاس ورق الملصق (طابعة الباركود):", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        LabelPaperSize.values().forEach { size ->
                            val isSelected = size == labelPaperSize
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onPaperSizeChanged(size) }
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "${size.widthMm}×${size.heightMm}",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "مم",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // عدد النسخ
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text("عدد النسخ المطلوب طباعتها:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text("حدد كمية الملصقات للطابعة", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { onCopiesChanged(maxOf(1, labelCopies - 1)) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "تقليل")
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            ) {
                                Text(
                                    text = "$labelCopies",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                )
                            }

                            IconButton(
                                onClick = { onCopiesChanged(minOf(100, labelCopies + 1)) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "زيادة")
                            }
                        }
                    }

                    // أزرار سريعة للنسخ
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        listOf(1, 2, 5, 10, 20).forEach { cp ->
                            OutlinedButton(
                                onClick = { onCopiesChanged(cp) },
                                modifier = Modifier.weight(1f),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp)
                            ) {
                                Text("$cp")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))

                    // خيارات العناصر الظاهرة على الملصق
                    Text("العناصر المضمنة على الملصق:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = showStoreName, onCheckedChange = { onToggleOption(it, null, null, null, null) })
                            Text("اسم المتجر", style = MaterialTheme.typography.bodySmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = showUnitName, onCheckedChange = { onToggleOption(null, it, null, null, null) })
                            Text("اسم الوحدة", style = MaterialTheme.typography.bodySmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = showPrice, onCheckedChange = { onToggleOption(null, null, it, null, null) })
                            Text("السعر", style = MaterialTheme.typography.bodySmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = showBarcodeText, onCheckedChange = { onToggleOption(null, null, null, it, null) })
                            Text("رقم الباركود", style = MaterialTheme.typography.bodySmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = showTaxNote, onCheckedChange = { onToggleOption(null, null, null, null, it) })
                            Text("شامل الضريبة", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        // معاينة حية للملصق (Live Thermal Label Canvas Preview)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
                border = BorderStroke(1.dp, Color(0xFFD6D8DB)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "3. معاينة الملصق المباشرة (Live Preview):",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFE2E3E5)
                        ) {
                            Text(
                                text = "${labelPaperSize.widthMm} × ${labelPaperSize.heightMm} مم",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    // تصميم الملصق الحراري الفعلي بمقاس محاكي
                    ThermalLabelCanvasPreview(
                        storeName = if (showStoreName) settings?.storeName ?: "دكاني" else "",
                        productName = selectedProduct?.product?.name ?: "طماطم بلدي",
                        unitName = if (showUnitName) selectedUnit?.unitName ?: "حبة" else "",
                        price = selectedUnit?.sellingPrice ?: 5.50,
                        barcode = currentBarcode,
                        showBarcodeText = showBarcodeText,
                        showPrice = showPrice,
                        showTaxNote = showTaxNote,
                        paperSize = labelPaperSize
                    )
                }
            }
        }

        // أزرار الطباعة والعمليات
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Button(
                        onClick = { onPrintLabel() },
                        enabled = !isPrintingLabel && selectedProduct != null && selectedUnit != null,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F5132)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("print_label_button")
                    ) {
                        if (isPrintingLabel) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("جارٍ الإرسال لطابعة الباركود...")
                        } else {
                            Icon(Icons.Default.Print, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "طباعة ($labelCopies) ملصق عبر Bluetooth (TSPL)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showTsplDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("أوامر TSPL الخام")
                        }
                    }

                    if (lastPrintResult != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (lastPrintResult.success) Color(0xFFD1E7DD) else Color(0xFFF8D7DA),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (lastPrintResult.success) Icons.Default.CheckCircle else Icons.Default.QrCode,
                                    contentDescription = null,
                                    tint = if (lastPrintResult.success) Color(0xFF0F5132) else Color(0xFF842029),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = lastPrintResult.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (lastPrintResult.success) Color(0xFF0F5132) else Color(0xFF842029)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // نافذة استعراض أوامر TSPL
    if (showTsplDialog) {
        val tsplText = remember(selectedProduct, selectedUnit, currentBarcode, labelPaperSize, labelCopies) {
            val labelData = com.example.dokkani.domain.hardware.BarcodeLabelData(
                storeName = settings?.storeName ?: "دكاني",
                productName = selectedProduct?.product?.name ?: "طماطم بلدي",
                unitName = selectedUnit?.unitName ?: "حبة",
                barcode = currentBarcode,
                price = selectedUnit?.sellingPrice ?: 5.50,
                size = labelPaperSize,
                copies = labelCopies,
                showStoreName = showStoreName,
                showUnitName = showUnitName,
                showPrice = showPrice,
                showBarcodeText = showBarcodeText,
                showTaxNote = showTaxNote
            )
            com.example.dokkani.domain.hardware.LabelPrinterCommands.buildTsplPreviewText(labelData)
        }

        AlertDialog(
            onDismissRequest = { showTsplDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Code, contentDescription = null, tint = Color(0xFF0F5132))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("حزمة أوامر TSPL لطابعات الملصقات")
                }
            },
            text = {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF1E1E1E),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = tsplText,
                        color = Color(0xFF4AF626),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showTsplDialog = false }) {
                    Text("إغلاق")
                }
            }
        )
    }
}

/**
 * رسم ملصق باركود واقعي عبر Canvas يحاكي الورق الحراري عالي الدقة
 */
@Composable
fun ThermalLabelCanvasPreview(
    storeName: String,
    productName: String,
    unitName: String,
    price: Double,
    barcode: String,
    showBarcodeText: Boolean,
    showPrice: Boolean,
    showTaxNote: Boolean,
    paperSize: LabelPaperSize,
    modifier: Modifier = Modifier
) {
    val aspectRatio = paperSize.widthMm.toFloat() / paperSize.heightMm.toFloat()

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(1.dp, Color(0xFFCED4DA)),
        modifier = modifier
            .fillMaxWidth(0.85f)
            .aspectRatio(aspectRatio)
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // اسم المتجر
            if (storeName.isNotBlank()) {
                Text(
                    text = storeName,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // اسم الصنف والوحدة
            val title = if (unitName.isNotBlank()) "$productName ($unitName)" else productName
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.Black,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // رسم خطوط الباركود الفعلية
            val bars = remember(barcode) { BarcodeGenerator.generateBarcodeBars(barcode) }

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(38.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val barCount = bars.size
                    if (barCount > 0) {
                        val barWidth = size.width / barCount
                        for (i in 0 until barCount) {
                            if (bars[i]) {
                                drawRect(
                                    color = Color.Black,
                                    topLeft = Offset(x = i * barWidth, y = 0f),
                                    size = Size(width = barWidth.coerceAtLeast(1.5f), height = size.height)
                                )
                            }
                        }
                    }
                }
            }

            // رقم الباركود
            if (showBarcodeText) {
                Text(
                    text = barcode,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    color = Color.DarkGray
                )
            }

            // السعر والضريبة
            if (showPrice) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "%.2f".format(price),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "ر.س",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    if (showTaxNote) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "(شامل الضريبة)",
                            fontSize = 9.sp,
                            color = Color(0xFF495057)
                        )
                    }
                }
            }
        }
    }
}
