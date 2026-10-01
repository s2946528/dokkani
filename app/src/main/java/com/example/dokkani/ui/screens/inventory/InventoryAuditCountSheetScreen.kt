package com.example.dokkani.ui.screens.inventory

import android.content.Context
import android.content.Intent
import android.print.PrintAttributes
import android.print.PrintManager
import android.speech.RecognizerIntent
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dokkani.data.local.entities.InventoryAuditSheetEntity
import com.example.dokkani.data.local.entities.UserRole
import com.example.dokkani.ui.components.AppSearchBar
import com.example.dokkani.ui.components.CameraBarcodeScannerDialog
import com.example.dokkani.ui.components.NumericOutlinedTextField
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * شاشة الجرد وقائمة المطابقة المخزنية والميدانية (Inventory Stocktaking & Reconciliation Screen)
 * مع هيكل التبويبات الداخلية وضوابط وصلاحيات الحذف والتعديل المتقدمة.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryAuditCountSheetScreen(
    viewModel: InventoryCountSheetViewModel = viewModel(),
    currentUserRole: UserRole = UserRole.ADMIN,
    onNavigateToValueSelling: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()) }

    // مشغل البحث الصوتي (Speech Recognition)
    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val spokenText = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                if (uiState.selectedTabIndex == 0) {
                    viewModel.setSearchQuery(spokenText)
                } else {
                    viewModel.setArchiveSearchQuery(spokenText)
                }
                Toast.makeText(context, "تم التعرف على الصوت: $spokenText", Toast.LENGTH_SHORT).show()
            }
        }
    }

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
                            .padding(top = 10.dp, start = 12.dp, end = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Inventory2,
                                        contentDescription = null,
                                        tint = Color(0xFF81C784),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "شاشة الجرد وقائمة المطابقة",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "العد الميداني، تسوية الأرخص، والتحكم بالصلاحيات والأرشيف",
                                    color = Color(0xFFA5D6A7),
                                    fontSize = 11.sp
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = { viewModel.startNewStocktakingSheet() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("جرد جديد", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // 1. هيكل التبويبات الداخلية (Internal Tabs Layout)
                        TabRow(
                            selectedTabIndex = uiState.selectedTabIndex,
                            containerColor = Color.Transparent,
                            contentColor = Color.White,
                            indicator = { tabPositions ->
                                if (uiState.selectedTabIndex < tabPositions.size) {
                                    TabRowDefaults.SecondaryIndicator(
                                        modifier = Modifier.tabIndicatorOffset(tabPositions[uiState.selectedTabIndex]),
                                        height = 3.dp,
                                        color = Color(0xFF81C784)
                                    )
                                }
                            }
                        ) {
                            Tab(
                                selected = uiState.selectedTabIndex == 0,
                                onClick = { viewModel.selectTab(0) },
                                modifier = Modifier.testTag("tab_active_audit")
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.EditNote,
                                        contentDescription = null,
                                        tint = if (uiState.selectedTabIndex == 0) Color(0xFF81C784) else Color(0xFFB0BEC5),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "الجرد الحالي",
                                        fontWeight = if (uiState.selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp,
                                        color = if (uiState.selectedTabIndex == 0) Color.White else Color(0xFFB0BEC5)
                                    )
                                    if (uiState.isCurrentSheetPosted) {
                                        Surface(
                                            color = Color(0xFF2E7D32),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "معتمد",
                                                fontSize = 9.sp,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Tab(
                                selected = uiState.selectedTabIndex == 1,
                                onClick = { viewModel.selectTab(1) },
                                modifier = Modifier.testTag("tab_archived_sheets")
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = if (uiState.selectedTabIndex == 1) Color(0xFF81C784) else Color(0xFFB0BEC5),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "أرشيف السندات السابقة",
                                        fontWeight = if (uiState.selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp,
                                        color = if (uiState.selectedTabIndex == 1) Color.White else Color(0xFFB0BEC5)
                                    )
                                    Surface(
                                        color = Color(0xFF37474F),
                                        shape = CircleShape
                                    ) {
                                        Text(
                                            text = uiState.archivedSheets.size.toString(),
                                            fontSize = 10.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            snackbarHost = {
                uiState.feedbackMessage?.let { message ->
                    Snackbar(
                        modifier = Modifier.padding(12.dp),
                        containerColor = if (uiState.isErrorFeedback) MaterialTheme.colorScheme.errorContainer else Color(0xFF1B5E20),
                        contentColor = if (uiState.isErrorFeedback) MaterialTheme.colorScheme.onErrorContainer else Color.White,
                        action = {
                            TextButton(onClick = { viewModel.dismissFeedback() }) {
                                Text("إغلاق", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    ) {
                        Text(message, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(if (isDark) MaterialTheme.colorScheme.background else Color(0xFFF8FAFC))
            ) {
                if (uiState.selectedTabIndex == 0) {
                    ActiveAuditTabContent(
                        uiState = uiState,
                        viewModel = viewModel,
                        currentUserRole = currentUserRole,
                        dateFormat = dateFormat,
                        speechRecognizerLauncher = speechRecognizerLauncher
                    )
                } else {
                    ArchivedAuditSheetsTabContent(
                        uiState = uiState,
                        viewModel = viewModel,
                        dateFormat = dateFormat,
                        speechRecognizerLauncher = speechRecognizerLauncher
                    )
                }

                // 2. نافذة تأكيد حذف مسودة الجرد (Delete Confirm Dialog)
                if (uiState.showDeleteConfirmDialog && uiState.sheetToDelete != null) {
                    val sheet = uiState.sheetToDelete!!
                    AlertDialog(
                        onDismissRequest = { viewModel.dismissDeleteConfirmDialog() },
                        icon = {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(36.dp))
                        },
                        title = {
                            Text("تأكيد حذف مسودة الجرد", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("هل أنت متأكد من حذف مسودة الجرد رقم #${sheet.voucherNumber}؟", fontSize = 13.sp)
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "ملاحظة: تقتصر إمكانية الحذف المباشر على المسودات غير المعتمدة فقط. سيتم حذف البيانات المؤقتة كلياً.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = { viewModel.confirmDeleteSheet() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("نعم (حذف المسودة)", fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            OutlinedButton(onClick = { viewModel.dismissDeleteConfirmDialog() }) {
                                Text("إلغاء")
                            }
                        }
                    )
                }

                // نافذة معاينة الطباعة الحية للجرد الفعلي
                if (uiState.showPrintPreviewDialog) {
                    InventoryPrintPreviewDialog(
                        uiState = uiState,
                        dateFormat = dateFormat,
                        onDismiss = { viewModel.dismissPrintPreviewDialog() },
                        onPrintClick = {
                            printInventoryCountSheetWeb(
                                context = context,
                                uiState = uiState,
                                dateFormat = dateFormat
                            )
                        }
                    )
                }

                // نافذة معاينة وطباعة سند مؤرشف من الأرشيف
                if (uiState.previewingArchivedSheet != null) {
                    ArchivedSheetPreviewDialog(
                        sheet = uiState.previewingArchivedSheet!!,
                        currencySymbol = uiState.currencySymbol,
                        dateFormat = dateFormat,
                        onDismiss = { viewModel.setPreviewingArchivedSheet(null) },
                        onPrintClick = {
                            printArchivedSheetWeb(
                                context = context,
                                sheet = uiState.previewingArchivedSheet!!,
                                currencySymbol = uiState.currencySymbol,
                                dateFormat = dateFormat
                            )
                        }
                    )
                }

                // نافذة الكاميرا لماسح الباركود
                if (uiState.showCameraScannerDialog) {
                    CameraBarcodeScannerDialog(
                        onBarcodeScanned = { barcode ->
                            viewModel.setSearchQuery(barcode)
                            viewModel.setShowCameraScannerDialog(false)
                            Toast.makeText(context, "تم مسح الباركود: $barcode", Toast.LENGTH_SHORT).show()
                        },
                        onDismiss = { viewModel.setShowCameraScannerDialog(false) }
                    )
                }

                // نافذة نجاح اعتماد وترحيل الجرد للبيع بالقيمة
                if (uiState.showCommitSuccessDialog) {
                    AuditCommitSuccessDialog(
                        uiState = uiState,
                        onDismiss = { viewModel.dismissCommitSuccessDialog() },
                        onNavigateToValueSelling = {
                            viewModel.dismissCommitSuccessDialog()
                            onNavigateToValueSelling()
                        }
                    )
                }
            }
        }
    }
}

/**
 * تبويب 1: محتوى "الجرد الحالي" للعد الميداني والإدخال والاعتماد
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActiveAuditTabContent(
    uiState: InventoryCountSheetUiState,
    viewModel: InventoryCountSheetViewModel,
    currentUserRole: UserRole,
    dateFormat: SimpleDateFormat,
    speechRecognizerLauncher: androidx.activity.result.ActivityResultLauncher<Intent>
) {
    val context = LocalContext.current

    val totalItemsCount = uiState.auditItems.size
    val matchingItemsCount = uiState.auditItems.count { it.status == AuditItemStatus.MATCHING }
    val shortageItemsCount = uiState.auditItems.count { it.status == AuditItemStatus.SHORTAGE }
    val surplusItemsCount = uiState.auditItems.count { it.status == AuditItemStatus.SURPLUS }

    val totalShortageQty = uiState.auditItems.sumOf { it.shortageQuantity }
    val totalShortageSellingValue = uiState.auditItems.sumOf { it.totalShortageSellingValue }
    val totalSurplusQty = uiState.auditItems.sumOf { it.surplusQuantity }
    val totalSurplusSellingValue = uiState.auditItems.sumOf { it.totalSurplusSellingValue }

    @OptIn(ExperimentalMaterial3Api::class)
    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = { viewModel.refreshInventoryItems() },
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // قائمة التمرير الرئيسية وتشمل الترويسة والمؤشرات والبحث وأصناف الجرد
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. ترويسة سند الجرد والتحكم بحالة المسودة أو الاعتماد
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (uiState.isCurrentSheetPosted) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (uiState.isCurrentSheetPosted) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (uiState.isCurrentSheetPosted) Icons.Default.Lock else Icons.Default.Receipt,
                                        contentDescription = null,
                                        tint = if (uiState.isCurrentSheetPosted) Color(0xFF1B5E20) else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "سند الجرد: ${uiState.voucherNumber}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (uiState.isCurrentSheetPosted) Color(0xFF1B5E20) else MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                // شارة شريط الحالة والضوابط
                                Surface(
                                    color = if (uiState.isCurrentSheetPosted) Color(0xFF2E7D32) else Color(0xFFFFF3E0),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, if (uiState.isCurrentSheetPosted) Color(0xFF1B5E20) else Color(0xFFFFB74D))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (uiState.isCurrentSheetPosted) Icons.Default.Verified else Icons.Default.Edit,
                                            contentDescription = null,
                                            tint = if (uiState.isCurrentSheetPosted) Color.White else Color(0xFFE65100),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = if (uiState.isCurrentSheetPosted) "🔒 معتمد ومقفل محاسبياً" else "📝 مسودة (قابلة للتعديل)",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (uiState.isCurrentSheetPosted) Color.White else Color(0xFFE65100)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "التاريخ: ${dateFormat.format(Date(uiState.voucherDate))}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                var costCenterExpanded by remember { mutableStateOf(false) }
                                val activeCc = uiState.costCenters.firstOrNull { it.centerId == uiState.selectedCostCenterId }

                                ExposedDropdownMenuBox(
                                    expanded = costCenterExpanded && !uiState.isCurrentSheetPosted,
                                    onExpandedChange = { if (!uiState.isCurrentSheetPosted) costCenterExpanded = !costCenterExpanded },
                                    modifier = Modifier.widthIn(max = 200.dp)
                                ) {
                                    OutlinedTextField(
                                        value = activeCc?.centerName ?: "مركز التكلفة العام",
                                        onValueChange = {},
                                        readOnly = true,
                                        enabled = !uiState.isCurrentSheetPosted,
                                        label = { Text("مركز التكلفة", fontSize = 9.sp) },
                                        trailingIcon = { if (!uiState.isCurrentSheetPosted) ExposedDropdownMenuDefaults.TrailingIcon(expanded = costCenterExpanded) },
                                        modifier = Modifier.menuAnchor(),
                                        singleLine = true,
                                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    )

                                    ExposedDropdownMenu(
                                        expanded = costCenterExpanded && !uiState.isCurrentSheetPosted,
                                        onDismissRequest = { costCenterExpanded = false }
                                    ) {
                                        uiState.costCenters.forEach { cc ->
                                            DropdownMenuItem(
                                                text = { Text(cc.centerName, fontSize = 11.sp) },
                                                onClick = {
                                                    viewModel.setSelectedCostCenter(cc.centerId)
                                                    costCenterExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // إذا كان السند معتمداً، يظهر تنبيه القفل وضوابط النزاهة المحاسبية
                            if (uiState.isCurrentSheetPosted) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    color = Color(0xFF1B5E20).copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = Color(0xFF1B5E20),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "ملاحظة: تم اعتماد هذا الجرد نهائياً وتم تحديث قيود المخزون. يمنع التعديل أو الحذف المباشر لضمان النزاهة المحاسبية.",
                                            fontSize = 10.sp,
                                            color = Color(0xFF1B5E20),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. كروت المؤشرات والإحصائيات الأفقية
                item {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            MetricCard(
                                title = "إجمالي الأصناف",
                                value = "$totalItemsCount صنف",
                                subtitle = "نطاق الجرد الحالي",
                                containerColor = Color(0xFFE3F2FD),
                                contentColor = Color(0xFF1565C0),
                                icon = Icons.Default.List
                            )
                        }
                        item {
                            MetricCard(
                                title = "الأصناف المطابقة",
                                value = "$matchingItemsCount صنف",
                                subtitle = "مطابقة بدون فروقات",
                                containerColor = Color(0xFFE8F5E9),
                                contentColor = Color(0xFF2E7D32),
                                icon = Icons.Default.CheckCircle
                            )
                        }
                        item {
                            MetricCard(
                                title = "عجز مخزني ⚠",
                                value = "%.1f قطعة".format(totalShortageQty),
                                subtitle = "قيمة بيع: %.2f %s".format(totalShortageSellingValue, uiState.currencySymbol),
                                containerColor = Color(0xFFFFEBEE),
                                contentColor = Color(0xFFC62828),
                                icon = Icons.Default.Warning
                            )
                        }
                        item {
                            MetricCard(
                                title = "زيادة مخزنية ▲",
                                value = "%.1f قطعة".format(totalSurplusQty),
                                subtitle = "قيمة بيع: %.2f %s".format(totalSurplusSellingValue, uiState.currencySymbol),
                                containerColor = Color(0xFFE0F7FA),
                                contentColor = Color(0xFF00838F),
                                icon = Icons.Default.TrendingUp
                            )
                        }
                    }
                }

                // 3. شريط البحث الذكي الموحد والمتعدد (نصي / صوتي / باركود عبر الكاميرا)
                item {
                    AppSearchBar(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = "ابحث عن صنف بالاسم، سجارة شملان، كوسة، بطاطس، طماطم...",
                        label = "بحث أصناف الجرد والمطابقة",
                        enableVoiceSearch = true,
                        enableBarcodeScanner = true,
                        onBarcodeScanned = { barcode ->
                            viewModel.setSearchQuery(barcode)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("inventory_search_input")
                    )
                }

                // 4. شريط تصفية المجموعات والتصنيفات
                if (uiState.categories.isNotEmpty()) {
                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            item {
                                FilterChip(
                                    selected = uiState.selectedCategoryFilter == null,
                                    onClick = { viewModel.setCategoryFilter(null) },
                                    label = { Text("الكل (${uiState.auditItems.size})", fontSize = 11.sp) },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                            items(uiState.categories) { cat ->
                                val count = uiState.auditItems.count { it.categoryName == cat }
                                FilterChip(
                                    selected = uiState.selectedCategoryFilter == cat,
                                    onClick = { viewModel.setCategoryFilter(cat) },
                                    label = { Text("$cat ($count)", fontSize = 11.sp) },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }
                }

                // 5. قائمة الأصناف والعد الفعلي للمطابقة
                if (uiState.filteredAuditItems.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.SearchOff,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "لا توجد أصناف مطابقة للبحث أو الجرد الميداني",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "جرّب تغيير كلمة البحث أو إلغاء تصفية المجموعات لإظهار الأصناف",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                } else {
                    items(uiState.filteredAuditItems, key = { it.productId }) { item ->
                        InventoryAuditItemCard(
                            item = item,
                            currencySymbol = uiState.currencySymbol,
                            isReadOnly = uiState.isCurrentSheetPosted,
                            onActualQtyChange = { newQtyStr ->
                                viewModel.updateActualEndingQty(item.productId, newQtyStr)
                            }
                        )
                    }
                }
            }

            // الشريط السفلي الثابت للتحكم والاعتماد والطباعة السرية
            Card(
                shape = RoundedCornerShape(0.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // ميزة الطباعة السرية المفاجئة (Blind Count Toggle)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Switch(
                            checked = uiState.isBlindCountPrintingEnabled,
                            onCheckedChange = { viewModel.toggleBlindCountPrinting(it) },
                            modifier = Modifier.scale(0.8f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "طباعة سرية (إخفاء الدفتري)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // حفظ كمسودة
                        if (!uiState.isCurrentSheetPosted) {
                            OutlinedButton(
                                onClick = { viewModel.saveDraftSheet() },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("حفظ كمسودة", fontSize = 11.sp)
                            }
                        }

                        // معاينة الطباعة
                        OutlinedButton(
                            onClick = { viewModel.openPrintPreviewDialog() },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("طباعة الجرد", fontSize = 11.sp)
                        }
                    }
                }

                // زر اعتماد وترحيل الجرد نهائياً
                if (!uiState.isCurrentSheetPosted) {
                    Button(
                        onClick = { viewModel.commitAuditAndTransferShortageToValueSelling(currentUserRole) },
                        enabled = !uiState.isCommittingAudit,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("commit_audit_button")
                    ) {
                        if (uiState.isCommittingAudit) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                        } else {
                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("اعتماد وترحيل الجرد وإصدار قيود التسوية والبيع بالقيمة", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
}

/**
 * تبويب 2: محتوى "أرشيف السندات السابقة" مع ضوابط وصلاحيات الحذف والتعديل
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArchivedAuditSheetsTabContent(
    uiState: InventoryCountSheetUiState,
    viewModel: InventoryCountSheetViewModel,
    dateFormat: SimpleDateFormat,
    speechRecognizerLauncher: androidx.activity.result.ActivityResultLauncher<Intent>
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // شريط البحث في الأرشيف
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedTextField(
                value = uiState.archiveSearchQuery,
                onValueChange = { viewModel.setArchiveSearchQuery(it) },
                label = { Text("بحث في أرشيف الجرد السابقة", fontSize = 11.sp) },
                placeholder = { Text("رقم السند / المخزن / مركز التكلفة...", fontSize = 11.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                trailingIcon = {
                    if (uiState.archiveSearchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setArchiveSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "مسح")
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("archive_search_input"),
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )
        }

        // أزرار تصفية الحالة للأرشيف
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = uiState.archiveStatusFilter == null,
                onClick = { viewModel.setArchiveStatusFilter(null) },
                label = { Text("كل السندات (${uiState.archivedSheets.size})", fontSize = 11.sp) }
            )
            FilterChip(
                selected = uiState.archiveStatusFilter == InventoryAuditSheetEntity.STATUS_DRAFT,
                onClick = { viewModel.setArchiveStatusFilter(InventoryAuditSheetEntity.STATUS_DRAFT) },
                label = {
                    val count = uiState.archivedSheets.count { it.isDraft }
                    Text("المسودات ($count)", fontSize = 11.sp)
                }
            )
            FilterChip(
                selected = uiState.archiveStatusFilter == InventoryAuditSheetEntity.STATUS_POSTED,
                onClick = { viewModel.setArchiveStatusFilter(InventoryAuditSheetEntity.STATUS_POSTED) },
                label = {
                    val count = uiState.archivedSheets.count { it.isPosted }
                    Text("🔒 المعتمدة ($count)", fontSize = 11.sp)
                }
            )
        }

        // قائمة السندات المؤرشفة
        if (uiState.filteredArchivedSheets.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("لا توجد سندات جرد أسبوعية أو شهرية محفوظة بالأرشيف", color = Color.Gray, fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.filteredArchivedSheets, key = { it.id }) { sheet ->
                    ArchivedAuditSheetCard(
                        sheet = sheet,
                        currencySymbol = uiState.currencySymbol,
                        dateFormat = dateFormat,
                        onLoadClick = { viewModel.loadSheetToActiveAudit(sheet) },
                        onPreviewClick = { viewModel.setPreviewingArchivedSheet(sheet) },
                        onDeleteClick = { viewModel.requestDeleteSheet(sheet) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ArchivedAuditSheetCard(
    sheet: InventoryAuditSheetEntity,
    currencySymbol: String,
    dateFormat: SimpleDateFormat,
    onLoadClick: () -> Unit,
    onPreviewClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (sheet.isPosted) MaterialTheme.colorScheme.surface else Color(0xFFFFF8E1)
        ),
        border = BorderStroke(
            1.dp,
            if (sheet.isPosted) Color(0xFF2E7D32).copy(alpha = 0.5f) else Color(0xFFFFB74D)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "سند رقم #${sheet.voucherNumber}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "التاريخ: ${dateFormat.format(Date(sheet.date))} | مركز التكلفة: ${sheet.costCenterName}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // حالة السند
                Surface(
                    color = if (sheet.isPosted) Color(0xFF2E7D32) else Color(0xFFFFF3E0),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (sheet.isPosted) "🔒 معتمد ومقفل" else "📝 مسودة",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (sheet.isPosted) Color.White else Color(0xFFE65100),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            // ملخص الكميات والفروقات
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "الأصناف: ${sheet.itemsCount} (مطابق: ${sheet.matchingCount})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "عجز: ${sheet.shortageCount} (%.2f %s)".format(sheet.totalShortageSellingValue, currencySymbol),
                    fontSize = 11.sp,
                    color = Color(0xFFC62828),
                    fontWeight = FontWeight.Bold
                )
            }

            // خيارات وأزرار التحكم بالصلاحيات
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // معاينة وطباعة
                    OutlinedButton(
                        onClick = onPreviewClick,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("معاينة/طباعة", fontSize = 11.sp)
                    }

                    // تحميل للسند الحالي
                    Button(
                        onClick = onLoadClick,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (sheet.isDraft) "استكمال الجرد" else "استعراض البنود", fontSize = 11.sp)
                    }
                }

                // زر الحذف الضابط (يُفعل فقط للمسودات، ويُقفل للسندات المعتمدة)
                if (sheet.isDraft) {
                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف المسودة", tint = MaterialTheme.colorScheme.error)
                    }
                } else {
                    // أيقونة قفل الحذف للمعتمد
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "مقفل ضد الحذف والتعديل",
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    containerColor: Color,
    contentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = containerColor,
        modifier = Modifier.width(150.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = contentColor)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = contentColor)
            Text(text = subtitle, fontSize = 9.sp, color = contentColor.copy(alpha = 0.8f))
        }
    }
}

@Composable
private fun InventoryAuditItemCard(
    item: InventoryAuditItemState,
    currencySymbol: String,
    isReadOnly: Boolean,
    onActualQtyChange: (String) -> Unit
) {
    val statusColor = when (item.status) {
        AuditItemStatus.MATCHING -> Color(0xFF2E7D32)
        AuditItemStatus.SHORTAGE -> Color(0xFFC62828)
        AuditItemStatus.SURPLUS -> Color(0xFF00838F)
    }

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (item.status != AuditItemStatus.MATCHING) statusColor.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = item.productName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        text = "التصنيف: ${item.categoryName} ${if (item.barcode.isNotBlank()) " | كود: ${item.barcode}" else ""}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = statusColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = item.status.labelArabic,
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // الكمية الدفتري
                Column {
                    Text("الدفتري الصافي:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "%.1f %s".format(item.bookStockQuantity, item.unitName),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (item.isolatedDailyWasteQty > 0) {
                        Text(
                            text = "(تلف معزول: %.1f)".format(item.isolatedDailyWasteQty),
                            fontSize = 9.sp,
                            color = Color(0xFFD84315)
                        )
                    }
                }

                // إدخال الفعلي مع احترام القفل
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("العد الفعلي الميداني:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    NumericOutlinedTextField(
                        value = item.actualEndingQtyInput,
                        onValueChange = onActualQtyChange,
                        readOnly = isReadOnly,
                        modifier = Modifier.width(100.dp)
                    )
                }

                // الفرق الإجمالي
                Column(horizontalAlignment = Alignment.End) {
                    Text("الفارق / التغير:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "%+.1f %s".format(item.varianceQuantity, item.unitName),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                    if (item.shortageQuantity > 0) {
                        Text(
                            text = "قيمة بيع: %.2f %s".format(item.totalShortageSellingValue, currencySymbol),
                            fontSize = 9.sp,
                            color = Color(0xFFC62828),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InventoryPrintPreviewDialog(
    uiState: InventoryCountSheetUiState,
    dateFormat: SimpleDateFormat,
    onDismiss: () -> Unit,
    onPrintClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("معاينة طباعة قائمة الجرد", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("سند الجرد: ${uiState.voucherNumber}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("وضع العد المفاجئ: ${if (uiState.isBlindCountPrintingEnabled) "مُفعل (تم إخفاء الأرصدة الدفترية)" else "غير مفعل (إظهار كافة الكميات)"}", fontSize = 11.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(4.dp)
                ) {
                    Text(
                        text = "سيتم إنشاء كشف جرد مطبوع بتنسيق HTML متوافق مع كافة الطابعات العادية والمحمولة Bluetooth/USB.",
                        fontSize = 12.sp,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onPrintClick) {
                Icon(Icons.Default.Print, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("طباعة الآن")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}

@Composable
private fun ArchivedSheetPreviewDialog(
    sheet: InventoryAuditSheetEntity,
    currencySymbol: String,
    dateFormat: SimpleDateFormat,
    onDismiss: () -> Unit,
    onPrintClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("معاينة سند الجرد المؤرشف #${sheet.voucherNumber}", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("رقم المستند: ${sheet.voucherNumber}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("التاريخ: ${dateFormat.format(Date(sheet.date))}", fontSize = 12.sp)
                Text("المخزن: ${sheet.targetStoreName} | مركز التكلفة: ${sheet.costCenterName}", fontSize = 12.sp)
                Text("الحالة المحاسبية: ${if (sheet.isPosted) "🔒 معتمد ومقفل" else "📝 مسودة"}", fontWeight = FontWeight.Bold, color = if (sheet.isPosted) Color(0xFF2E7D32) else Color(0xFFE65100))
                Text("عدد الأصناف: ${sheet.itemsCount}", fontSize = 12.sp)
                Text("إجمالي قيمة العجز: %.2f %s".format(sheet.totalShortageSellingValue, currencySymbol), fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
            }
        },
        confirmButton = {
            Button(onClick = onPrintClick) {
                Icon(Icons.Default.Print, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("طباعة الكشف")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}

@Composable
private fun AuditCommitSuccessDialog(
    uiState: InventoryCountSheetUiState,
    onDismiss: () -> Unit,
    onNavigateToValueSelling: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("تم اعتماد وترحيل الجرد الدوري بنجاح", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("رقم سند الجرد: ${uiState.voucherNumber}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("تم إصدار قيود تسوية المخزون وإقفال السند محاسبياً ضد التعديل أو الحذف.", fontSize = 12.sp)
                if (uiState.committedShortageRecordsCount > 0) {
                    Surface(
                        color = Color(0xFFE8F5E9),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("تم ترحيل (${uiState.committedShortageRecordsCount}) أصناف بها عجز إلى شاشة البيع بالقيمة.", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
                            Text("إجمالي قيمة إيراد العجز: %.2f %s".format(uiState.committedTotalShortageValue, uiState.currencySymbol), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onNavigateToValueSelling,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
            ) {
                Text("الانتقال للبيع بالقيمة")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}

/**
 * دالة إنشاء HTML وطباعة كشف الجرد الحالي عبر نظام Android PrintManager
 */
private fun printInventoryCountSheetWeb(
    context: Context,
    uiState: InventoryCountSheetUiState,
    dateFormat: SimpleDateFormat
) {
    val htmlBuilder = StringBuilder()
    htmlBuilder.append("<!DOCTYPE html><html><head><meta charset='utf-8'>")
    htmlBuilder.append("<style>")
    htmlBuilder.append("body { font-family: sans-serif; direction: rtl; text-align: right; padding: 15px; }")
    htmlBuilder.append("h2 { text-align: center; margin-bottom: 5px; }")
    htmlBuilder.append(".header-info { margin-bottom: 15px; font-size: 12px; border-bottom: 1px solid #ccc; padding-bottom: 8px; }")
    htmlBuilder.append("table { width: 100%; border-collapse: collapse; font-size: 11px; }")
    htmlBuilder.append("th, td { border: 1px solid #888; padding: 6px; text-align: center; }")
    htmlBuilder.append("th { background-color: #f2f2f2; }")
    htmlBuilder.append("</style></head><body>")

    htmlBuilder.append("<h2>كشف كشوفات الجرد الفعلي والمطابقة</h2>")
    htmlBuilder.append("<div class='header-info'>")
    htmlBuilder.append("<div><b>رقم المستند:</b> ${uiState.voucherNumber}</div>")
    htmlBuilder.append("<div><b>التاريخ:</b> ${dateFormat.format(Date(uiState.voucherDate))}</div>")
    htmlBuilder.append("<div><b>المخزن:</b> ${uiState.targetStoreName}</div>")
    htmlBuilder.append("<div><b>وضع الجرد المفاجئ (Blind Count):</b> ${if (uiState.isBlindCountPrintingEnabled) "مُفعل (تم إخفاء الرصيد الدفتري)" else "غير مفعل"}</div>")
    htmlBuilder.append("</div>")

    htmlBuilder.append("<table>")
    htmlBuilder.append("<thead><tr>")
    htmlBuilder.append("<th>#</th><th>اسم الصنف</th><th>الكود</th>")
    if (!uiState.isBlindCountPrintingEnabled) {
        htmlBuilder.append("<th>الدفتري</th>")
    }
    htmlBuilder.append("<th>العد الفعلي</th>")
    if (!uiState.isBlindCountPrintingEnabled) {
        htmlBuilder.append("<th>الفارق</th><th>الحالة</th>")
    }
    htmlBuilder.append("</tr></thead><tbody>")

    uiState.auditItems.forEachIndexed { index, item ->
        htmlBuilder.append("<tr>")
        htmlBuilder.append("<td>${index + 1}</td>")
        htmlBuilder.append("<td style='text-align:right;'>${item.productName}</td>")
        htmlBuilder.append("<td>${item.barcode}</td>")
        if (!uiState.isBlindCountPrintingEnabled) {
            htmlBuilder.append("<td>${item.bookStockQuantity}</td>")
        }
        htmlBuilder.append("<td>${if (uiState.isBlindCountPrintingEnabled) "_____" else item.actualEndingQtyInput}</td>")
        if (!uiState.isBlindCountPrintingEnabled) {
            htmlBuilder.append("<td>${item.varianceQuantity}</td>")
            htmlBuilder.append("<td>${item.status.labelArabic}</td>")
        }
        htmlBuilder.append("</tr>")
    }

    htmlBuilder.append("</tbody></table></body></html>")

    val webView = WebView(context)
    webView.webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView?, url: String?) {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            val printAdapter = webView.createPrintDocumentAdapter("Inventory_Sheet_${uiState.voucherNumber}")
            printManager?.print("Inventory_Sheet_${uiState.voucherNumber}", printAdapter, PrintAttributes.Builder().build())
        }
    }
    webView.loadDataWithBaseURL(null, htmlBuilder.toString(), "text/html", "UTF-8", null)
}

/**
 * دالة طباعة كشف جرد مؤرشف من الأرشيف
 */
private fun printArchivedSheetWeb(
    context: Context,
    sheet: InventoryAuditSheetEntity,
    currencySymbol: String,
    dateFormat: SimpleDateFormat
) {
    val htmlBuilder = StringBuilder()
    htmlBuilder.append("<!DOCTYPE html><html><head><meta charset='utf-8'>")
    htmlBuilder.append("<style>")
    htmlBuilder.append("body { font-family: sans-serif; direction: rtl; text-align: right; padding: 15px; }")
    htmlBuilder.append("h2 { text-align: center; margin-bottom: 5px; }")
    htmlBuilder.append(".header-info { margin-bottom: 15px; font-size: 12px; border-bottom: 1px solid #ccc; padding-bottom: 8px; }")
    htmlBuilder.append("</style></head><body>")

    htmlBuilder.append("<h2>سند جرد مؤرشف #${sheet.voucherNumber}</h2>")
    htmlBuilder.append("<div class='header-info'>")
    htmlBuilder.append("<div><b>رقم المستند:</b> ${sheet.voucherNumber}</div>")
    htmlBuilder.append("<div><b>التاريخ:</b> ${dateFormat.format(Date(sheet.date))}</div>")
    htmlBuilder.append("<div><b>المخزن:</b> ${sheet.targetStoreName}</div>")
    htmlBuilder.append("<div><b>مركز التكلفة:</b> ${sheet.costCenterName}</div>")
    htmlBuilder.append("<div><b>الحالة:</b> ${if (sheet.isPosted) "🔒 معتمد ومقفل" else "📝 مسودة"}</div>")
    htmlBuilder.append("<div><b>عدد الأصناف:</b> ${sheet.itemsCount}</div>")
    htmlBuilder.append("<div><b>إجمالي قيمة العجز:</b> ${sheet.totalShortageSellingValue} $currencySymbol</div>")
    htmlBuilder.append("</div>")

    htmlBuilder.append("</body></html>")

    val webView = WebView(context)
    webView.webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView?, url: String?) {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            val printAdapter = webView.createPrintDocumentAdapter("Archived_Sheet_${sheet.voucherNumber}")
            printManager?.print("Archived_Sheet_${sheet.voucherNumber}", printAdapter, PrintAttributes.Builder().build())
        }
    }
    webView.loadDataWithBaseURL(null, htmlBuilder.toString(), "text/html", "UTF-8", null)
}
