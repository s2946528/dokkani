package com.example.dokkani.ui.screens.inventory

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dokkani.data.local.entities.UserRole
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryAuditCountSheetScreen(
    viewModel: InventoryCountSheetViewModel = viewModel(),
    currentUserRole: UserRole = UserRole.ADMIN,
    onNavigateToValueSelling: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()) }

    val totalItemsCount = uiState.auditItems.size
    val totalShortageQty = uiState.auditItems.sumOf { it.shortageQuantity }
    val totalShortageValue = uiState.auditItems.sumOf { it.totalShortageSellingValue }
    val totalShortageCost = uiState.auditItems.sumOf { it.totalShortageCostValue }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                Surface(
                    color = Color(0xFF133E32),
                    shadowElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF2E7D32),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.FactCheck,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "إدارة المخازن: شاشة الجرد وقائمة المطابقة",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "الجرد الدوري الميداني، المطابقة الحية، والطباعة السرية المفاجئة",
                                        fontSize = 11.sp,
                                        color = Color(0xFFA5D6A7)
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "الدور: ${currentUserRole.labelArabic}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
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
                                Text("إغلاق", color = Color.White)
                            }
                        }
                    ) {
                        Text(msg)
                    }
                }
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ==========================================
                // أولاً: بطاقات المؤشرات الحية والإحصائيات
                // ==========================================
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("الأصناف المجرودة", fontSize = 11.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("$totalItemsCount صنف", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("مخزن مسجل", fontSize = 10.sp, color = Color(0xFF2E7D32))
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                            border = BorderStroke(1.dp, Color(0xFFFFB74D))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("إجمالي كمية العجز", fontSize = 11.sp, color = Color(0xFFE65100), fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("%.1f وحدة".format(totalShortageQty), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFFD84315))
                                Text("فارق دفتري-فعلي", fontSize = 10.sp, color = Color(0xFFE65100))
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                            border = BorderStroke(1.dp, Color(0xFF81C784))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("إيراد البيع بالقيمة", fontSize = 11.sp, color = Color(0xFF1B5E20), fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("%.2f %s".format(totalShortageValue, uiState.currencySymbol), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1B5E20))
                                Text("يُرحل للبيع بالقيمة", fontSize = 10.sp, color = Color(0xFF2E7D32))
                            }
                        }
                    }
                }

                // ==========================================
                // ثانياً: شريط التحكم والفلترة والطباعة الميدانية
                // ==========================================
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // حقل البحث واختيار مركز التكلفة
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = uiState.searchQuery,
                                    onValueChange = viewModel::setSearchQuery,
                                    placeholder = { Text("ابحث باسم الصنف، الباركود، أو التصنيف...", fontSize = 12.sp) },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp)
                                )

                                // اختيار مركز التكلفة
                                var ccDropdownExpanded by remember { mutableStateOf(false) }
                                val activeCc = uiState.costCenters.find { it.centerId == uiState.selectedCostCenterId }

                                Box {
                                    OutlinedButton(
                                        onClick = { ccDropdownExpanded = true },
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.Business, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(activeCc?.centerName ?: "مركز التكلفة العام", fontSize = 11.sp)
                                    }

                                    DropdownMenu(
                                        expanded = ccDropdownExpanded,
                                        onDismissRequest = { ccDropdownExpanded = false }
                                    ) {
                                        uiState.costCenters.forEach { cc ->
                                            DropdownMenuItem(
                                                text = { Text(cc.centerName, fontSize = 12.sp) },
                                                onClick = {
                                                    viewModel.setSelectedCostCenter(cc.centerId)
                                                    ccDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // تصفية حسب التصنيف
                            if (uiState.categories.isNotEmpty()) {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    item {
                                        FilterChip(
                                            selected = uiState.selectedCategoryFilter == null,
                                            onClick = { viewModel.setCategoryFilter(null) },
                                            label = { Text("جميع التصانيف", fontSize = 11.sp) }
                                        )
                                    }
                                    items(uiState.categories) { cat ->
                                        FilterChip(
                                            selected = uiState.selectedCategoryFilter == cat,
                                            onClick = { viewModel.setCategoryFilter(cat) },
                                            label = { Text(cat, fontSize = 11.sp) }
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                            // ==========================================
                            // ثالثاً: ميزة الطباعة السرية والمفاجئة (Blind Count)
                            // ==========================================
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (uiState.isBlindCountPrintingEnabled) Color(0xFFFFF3E0) else Color(0xFFF5F5F5)
                                ),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (uiState.isBlindCountPrintingEnabled) Color(0xFFFFB74D) else Color.LightGray
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = if (uiState.isBlindCountPrintingEnabled) Icons.Default.VisibilityOff else Icons.Default.Print,
                                                contentDescription = null,
                                                tint = if (uiState.isBlindCountPrintingEnabled) Color(0xFFE65100) else Color(0xFF1B5E20),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Column {
                                                Text(
                                                    text = "ميزة الطباعة السرية والمفاجئة (Blind Count)",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = if (uiState.isBlindCountPrintingEnabled) Color(0xFFBF360C) else Color.Black
                                                )
                                                Text(
                                                    text = "إخفاء الكميات الدفترية لمنع العمال من معرفة الرصيد أثناء الجرد اليدوي",
                                                    fontSize = 10.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                        }

                                        Switch(
                                            checked = uiState.isBlindCountPrintingEnabled,
                                            onCheckedChange = viewModel::toggleBlindCountPrinting,
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color(0xFFE65100),
                                                checkedTrackColor = Color(0xFFFFE0B2)
                                            )
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { viewModel.openPrintPreviewDialog() },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (uiState.isBlindCountPrintingEnabled) Color(0xFFE65100) else Color(0xFF1B5E20)
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f).height(38.dp)
                                        ) {
                                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (uiState.isBlindCountPrintingEnabled) "معاينة وطباعة قائمة الجرد السرية" else "طباعة قائمة الجرد الشاملة",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        // ==========================================
                                        // رابعاً: اعتماد الجرد وترحيله للبيع بالقيمة
                                        // ==========================================
                                        if (currentUserRole == UserRole.ADMIN) {
                                            Button(
                                                onClick = { viewModel.commitAuditAndTransferShortageToValueSelling(currentUserRole) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                                enabled = !uiState.isCommittingAudit,
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1f).height(38.dp)
                                            ) {
                                                if (uiState.isCommittingAudit) {
                                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                                                } else {
                                                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("اعتماد الجرد وترحيل العجز", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // خامساً: جدول حي لعرض الأصناف وإدخال الفعلي والمطابقة
                // ==========================================
                item {
                    Text(
                        text = "قائمة أصناف الجرد والكميات الفعلية (${uiState.filteredAuditItems.size}):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                if (uiState.filteredAuditItems.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("لا توجد أصناف مخزنية مطابقة للبحث أو الفلتر حالياً.", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                } else {
                    itemsIndexed(uiState.filteredAuditItems) { index, item ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            shape = RoundedCornerShape(10.dp),
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
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text("${index + 1}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(item.productName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("التصنيف: ${item.categoryName} | الوحدة: ${item.unitName}", fontSize = 10.sp, color = Color.Gray)
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFE8F5E9)
                                    ) {
                                        Text(
                                            text = "سعر البيع: %.1f %s".format(item.unitSellingPrice, uiState.currencySymbol),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1B5E20),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // المخزون الدفتري المتبقي (يظهر أو يختفي حسب وضع الجرد)
                                    OutlinedTextField(
                                        value = if (uiState.isBlindCountPrintingEnabled) "مخفي 🔒" else String.format(Locale.US, "%.1f", item.bookStockQuantity),
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("الدفتري الصافي") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = Color(0xFFF5F5F5),
                                            unfocusedContainerColor = Color(0xFFF5F5F5)
                                        )
                                    )

                                    // إدخال بضاعة آخر المدة الفعلي
                                    OutlinedTextField(
                                        value = item.actualEndingQtyInput,
                                        onValueChange = { viewModel.updateActualEndingQty(item.productId, it) },
                                        label = { Text("آخر المدة الفعلي *") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1.2f),
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFF2E7D32)
                                        )
                                    )

                                    // الفارق / العجز المخزني وقيمته
                                    val hasShortage = item.shortageQuantity > 0.01
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (hasShortage) Color(0xFFFFF3E0) else Color(0xFFF1F8E9)
                                        ),
                                        border = BorderStroke(
                                            width = 1.dp,
                                            color = if (hasShortage) Color(0xFFFFB74D) else Color(0xFFA5D6A7)
                                        ),
                                        modifier = Modifier.weight(1.2f)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = if (hasShortage) "عجز: -%.1f %s".format(item.shortageQuantity, item.unitName) else "مطابق ✓",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = if (hasShortage) Color(0xFFD84315) else Color(0xFF2E7D32)
                                            )
                                            if (hasShortage) {
                                                Text(
                                                    text = "قيمته: %.1f %s".format(item.totalShortageSellingValue, uiState.currencySymbol),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFBF360C)
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
        }

        // ==========================================
        // نافذة معاينة كشف الجرد والطباعة الميدانية (Blind Print Dialog)
        // ==========================================
        if (uiState.showPrintPreviewDialog) {
            val isBlind = uiState.isBlindCountPrintingEnabled
            val activeCc = uiState.costCenters.find { it.centerId == uiState.selectedCostCenterId }?.centerName ?: "مركز التكلفة العام"

            AlertDialog(
                onDismissRequest = viewModel::dismissPrintPreviewDialog,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isBlind) Icons.Default.VisibilityOff else Icons.Default.Print,
                            contentDescription = null,
                            tint = if (isBlind) Color(0xFFE65100) else Color(0xFF1B5E20)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isBlind) "كشف الجرد الميداني السري (بدون كميات)" else "كشف مطابقة الجرد الدوري الشامل",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 450.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("المحل/الفرع: متجر دكاني - $activeCc", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("تاريخ استخراج الكشف: ${dateFormat.format(Date())}", fontSize = 10.sp, color = Color.Gray)
                                if (isBlind) {
                                    Text("ملاحظة أمان: هذا الكشف سري ومعد للجرد اليدوي الميداني بدون كشف الأرصدة الدفترية للعمال.", fontSize = 10.sp, color = Color(0xFFD84315), fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        HorizontalDivider()

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            item {
                                Surface(
                                    color = Color(0xFFE0E0E0),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(6.dp).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("اسم الصنف", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1.5f))
                                        Text("الوحدة", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(0.8f))
                                        if (!isBlind) {
                                            Text("الدفتري", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(0.8f))
                                        }
                                        Text("الفعلي (مجرود)", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
                                    }
                                }
                            }

                            itemsIndexed(uiState.auditItems) { idx, item ->
                                Surface(
                                    color = if (idx % 2 == 0) Color.White else Color(0xFFFAFAFA),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(6.dp).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(item.productName, fontSize = 11.sp, modifier = Modifier.weight(1.5f))
                                        Text(item.unitName, fontSize = 11.sp, modifier = Modifier.weight(0.8f))
                                        if (!isBlind) {
                                            Text("%.1f".format(item.bookStockQuantity), fontSize = 11.sp, modifier = Modifier.weight(0.8f))
                                        }
                                        Text(
                                            text = if (isBlind) "[   .   ]" else "%.1f".format(item.actualEndingQty),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isBlind) Color.Gray else Color.Black,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("توقيع مسؤول الجرد: ..................", fontSize = 10.sp, color = Color.Gray)
                            Text("توقيع المدير: ..................", fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            executePrintService(
                                context = context,
                                items = uiState.auditItems,
                                isBlind = isBlind,
                                costCenterName = activeCc,
                                currencySymbol = uiState.currencySymbol
                            )
                            viewModel.dismissPrintPreviewDialog()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isBlind) Color(0xFFE65100) else Color(0xFF1B5E20)
                        )
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("طباعة الآن (Print)")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = viewModel::dismissPrintPreviewDialog) {
                        Text("إلغاء")
                    }
                }
            )
        }

        // ==========================================
        // نافذة تأكيد وتوجيه اعتماد الجرد للبيع بالقيمة
        // ==========================================
        if (uiState.showCommitSuccessDialog) {
            AlertDialog(
                onDismissRequest = viewModel::dismissCommitSuccessDialog,
                icon = {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(36.dp))
                },
                title = {
                    Text("تم اعتماد الجرد وترحيل العجز بنجاح!", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "تم تحديث الأرصدة الفعلية في كافة السجلات، وترحيل عدد (${uiState.committedShortageRecordsCount}) قيود عجز بمبلغ إجمالي قدره (%.2f %s) إلى شاشة 'إدارة البيع بالقيمة'.".format(
                                uiState.committedTotalShortageValue,
                                uiState.currencySymbol
                            ),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("• تم استبعاد التلف اليومي المعزول لمنع ازدواج الخسارة.", fontSize = 11.sp, color = Color(0xFF1B5E20))
                                Text("• يمكنك الآن الانتقال لشاشة البيع بالقيمة لتأكيد استلام المبالغ وقفل الدفاتر.", fontSize = 11.sp, color = Color(0xFF1B5E20))
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.dismissCommitSuccessDialog()
                            onNavigateToValueSelling()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20))
                    ) {
                        Text("الانتقال لإدارة البيع بالقيمة")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = viewModel::dismissCommitSuccessDialog) {
                        Text("إغلاق والبقاء هنا")
                    }
                }
            )
        }
    }
}

/**
 * خدمة الطباعة عبر Android PrintManager لتحويل كشف الجرد إلى HTML وقابليته للطباعة
 */
private fun executePrintService(
    context: Context,
    items: List<InventoryAuditItemState>,
    isBlind: Boolean,
    costCenterName: String,
    currencySymbol: String
) {
    try {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val dateStr = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date())

        val htmlContent = StringBuilder()
        htmlContent.append("<html><head><meta charset='UTF-8'><style>")
        htmlContent.append("body { font-family: sans-serif; direction: rtl; text-align: right; padding: 20px; }")
        htmlContent.append("h2 { color: #1B5E20; text-align: center; margin-bottom: 5px; }")
        htmlContent.append(".subtitle { text-align: center; color: #555; font-size: 12px; margin-bottom: 20px; }")
        htmlContent.append("table { width: 100%; border-collapse: collapse; margin-top: 15px; }")
        htmlContent.append("th, td { border: 1px solid #ddd; padding: 8px; text-align: center; font-size: 12px; }")
        htmlContent.append("th { background-color: #1B5E20; color: white; }")
        htmlContent.append(".blind-box { border: 1px dashed #999; height: 25px; width: 60px; margin: auto; }")
        htmlContent.append(".footer { margin-top: 40px; display: flex; justify-content: space-between; font-size: 12px; }")
        htmlContent.append("</style></head><body>")

        val title = if (isBlind) "كشف الجرد الميداني السري (جرد مفاجئ - بدون كميات)" else "كشف مطابقة الجرد الدوري الشامل"
        htmlContent.append("<h2>$title</h2>")
        htmlContent.append("<div class='subtitle'>متجر دكاني - $costCenterName | التاريخ: $dateStr</div>")

        htmlContent.append("<table><thead><tr>")
        htmlContent.append("<th>#</th><th>اسم الصنف</th><th>التصنيف</th><th>الوحدة</th>")
        if (!isBlind) {
            htmlContent.append("<th>المخزون الدفتري</th>")
        }
        htmlContent.append("<th>الكمية الفعلية المجرودة</th>")
        if (!isBlind) {
            htmlContent.append("<th>العجز/الفارق</th><th>سعر البيع</th>")
        }
        htmlContent.append("</tr></thead><tbody>")

        items.forEachIndexed { idx, item ->
            htmlContent.append("<tr>")
            htmlContent.append("<td>${idx + 1}</td>")
            htmlContent.append("<td><b>${item.productName}</b></td>")
            htmlContent.append("<td>${item.categoryName}</td>")
            htmlContent.append("<td>${item.unitName}</td>")
            if (!isBlind) {
                htmlContent.append("<td>${String.format(Locale.US, "%.1f", item.bookStockQuantity)}</td>")
            }
            if (isBlind) {
                htmlContent.append("<td><div class='blind-box'></div></td>")
            } else {
                htmlContent.append("<td><b>${String.format(Locale.US, "%.1f", item.actualEndingQty)}</b></td>")
            }
            if (!isBlind) {
                htmlContent.append("<td>${String.format(Locale.US, "%.1f", item.shortageQuantity)}</td>")
                htmlContent.append("<td>${String.format(Locale.US, "%.1f %s", item.unitSellingPrice, currencySymbol)}</td>")
            }
            htmlContent.append("</tr>")
        }

        htmlContent.append("</tbody></table>")
        htmlContent.append("<div class='footer'><div>توقيع مسؤول الجرد: ..................</div><div>توقيع مدير المخزن: ..................</div></div>")
        htmlContent.append("</body></html>")

        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val printAdapter = webView.createPrintDocumentAdapter("Inventory_Audit_$dateStr")
                printManager.print("Dokkani_Inventory_Sheet", printAdapter, PrintAttributes.Builder().build())
            }
        }
        webView.loadDataWithBaseURL(null, htmlContent.toString(), "text/html", "UTF-8", null)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
