package com.example.dokkani.ui.screens.notifications

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dokkani.data.local.entities.NotificationEntity
import com.example.dokkani.data.local.entities.NotificationType
import com.example.dokkani.ui.DokkaniUiState
import com.example.dokkani.ui.DokkaniViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    viewModel: DokkaniViewModel,
    uiState: DokkaniUiState
) {
    val context = LocalContext.current
    var selectedFilterIndex by remember { mutableStateOf(0) }

    // Launcher لطلب أذونات الإشعارات على أندرويد 13+ (POST_NOTIFICATIONS)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.updateNotificationPermissionState(isGranted)
        if (isGranted) {
            Toast.makeText(context, "تم تفعيل أذونات الإشعارات بنجاح", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "تم رفض إذن الإشعارات", Toast.LENGTH_SHORT).show()
        }
    }

    val filters = listOf("الكل", "التنبيهات", "الديون والآجل", "المخزون", "النسخ والمزامنة", "المهام الخلفية WorkManager")

    val filteredNotifications = remember(uiState.notifications, selectedFilterIndex) {
        when (selectedFilterIndex) {
            1 -> uiState.notifications.filter { it.type == NotificationType.ALERT || it.type == NotificationType.SHIFT_CLOSE }
            2 -> uiState.notifications.filter { it.type == NotificationType.DEBT_DUE }
            3 -> uiState.notifications.filter { it.type == NotificationType.LOW_STOCK }
            4 -> uiState.notifications.filter { it.type == NotificationType.SYNC }
            else -> uiState.notifications
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // 1. بطاقة أذونات الإشعارات والرمز السحابي FCM Token
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = if (uiState.hasNotificationPermission) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                        contentDescription = "حالة الإشعارات",
                        tint = if (uiState.hasNotificationPermission) Color(0xFF2E7D32) else Color(0xFFD32F2F),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (uiState.hasNotificationPermission) "خدمة الإشعارات السحابية مفعلة" else "إذن الإشعارات غير مفعّل",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = if (uiState.hasNotificationPermission) "التطبيق جاهز لاستقبال إشعارات FCM والتنبيهات المباشرة" else "يرجى منح الإذن لاستقبال تنبيهات المبيعات والديون",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!uiState.hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("تفعيل الإذن", fontSize = 12.sp)
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                // عرض رمز FCM Token
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDone,
                        contentDescription = "FCM Token",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "رمز الجهاز السحابي (FCM Token):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = { viewModel.refreshFcmToken() },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "تحديث الرمز", modifier = Modifier.size(18.dp))
                    }
                }

                val tokenText = uiState.fcmToken ?: "جاري جلب رمز FCM من خوادم خادم Firebase..."
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = tokenText,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (uiState.fcmToken != null) {
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("FCM Token", uiState.fcmToken)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "تم نسخ رمز FCM إلى الحافظة", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "نسخ", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2. تبويبات الفلاتر وإدارة WorkManager
        ScrollableTabRow(
            selectedTabIndex = selectedFilterIndex,
            edgePadding = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            filters.forEachIndexed { index, title ->
                Tab(
                    selected = selectedFilterIndex == index,
                    onClick = { selectedFilterIndex = index },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(title, fontSize = 13.sp)
                            if (index == 0 && uiState.unreadNotificationCount > 0) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFD32F2F), CircleShape)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${uiState.unreadNotificationCount}",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // إذا تم اختيار تبويب المهام الخلفية WorkManager
        if (selectedFilterIndex == 5) {
            WorkManagerControlsSection(viewModel = viewModel)
        } else {
            // شريط الإجراءات لمركز الإشعارات
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "سجل الإشعارات (${filteredNotifications.size})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Row {
                    OutlinedButton(
                        onClick = { viewModel.markAllNotificationsAsRead() },
                        modifier = Modifier.height(34.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Default.DoneAll, contentDescription = "قراءة الكل", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تحديد الكل كمقروء", fontSize = 11.sp)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedButton(
                        onClick = { viewModel.clearAllNotifications() },
                        modifier = Modifier.height(34.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F)),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "مسح الكل", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("مسح السجل", fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (filteredNotifications.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "لا يوجد إشعارات",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "لا توجد إشعارات حالياً في هذا القسم",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredNotifications, key = { it.id }) { item ->
                        NotificationItemCard(
                            item = item,
                            onMarkAsRead = { viewModel.markNotificationAsRead(item.id) },
                            onDelete = { viewModel.deleteNotification(item.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkManagerControlsSection(viewModel: DokkaniViewModel) {
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale("ar")) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "WorkManager",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "إدارة وسجل المهام الخلفية (WorkManager)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "تقوم WorkManager بتنفيذ مهام فحص الديون والمخزون والنسخ الاحتياطي بالخلفية بأمان تكتيكي دون التأثير على سلاسة واجهة المستخدم (Main Thread).",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Text(
                text = "تشغيل وتجربة المهام الخلفية فوراً:",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        // 1. مهمة النسخ الاحتياطي اليومي
        item {
            WorkTaskActionCard(
                title = "النسخ الاحتياطي ومراجعة السجلات",
                description = "يقوم بفحص سلامة الجداول وإنشاء إشعار مزامنة دوري بالخلفية.",
                icon = Icons.Default.Backup,
                iconColor = Color(0xFF1976D2),
                onRunNow = {
                    viewModel.triggerWorkBackupNow()
                    Toast.makeText(context, "تم إرسال مهمة النسخ الاحتياطي للعمل بالخلفية عبر WorkManager", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // 2. مهمة فحص الأصناف المنخفضة بالمخزون
        item {
            WorkTaskActionCard(
                title = "فحص الأصناف المنخفضة في المخزون",
                description = "يفحص المنتجات ذات الكمية المنخفضة ويصدر تنبيهاً فورياً.",
                icon = Icons.Default.Inventory,
                iconColor = Color(0xFFE65100),
                onRunNow = {
                    viewModel.triggerWorkStockCheckNow()
                    Toast.makeText(context, "تم إرسال مهمة فحص المخزون بالخلفية", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // 3. مهمة فحص ديون العملاء
        item {
            WorkTaskActionCard(
                title = "متابعة ديون العملاء والآجل",
                description = "يفحص سجلات العملاء المدينين وينبه بالإجمالي المستحق.",
                icon = Icons.Default.Warning,
                iconColor = Color(0xFFD32F2F),
                onRunNow = {
                    viewModel.triggerWorkDebtCheckNow()
                    Toast.makeText(context, "تم إرسال مهمة فحص الديون بالخلفية", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // 4. تجربة إرسال إشعار محلي/سحابي
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "اختبار إرسال إشعار تجريبي مخصص",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                viewModel.sendTestNotification(
                                    title = "فاتورة بيع جديدة 🧾",
                                    body = "تم إصدار فاتورة بيع مباشر بمبلغ 15,000 ر.ي بنجاح",
                                    type = NotificationType.ALERT
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("إشعار مبيعات", fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                viewModel.sendTestNotification(
                                    title = "تنبيه نقص الطماطم 🍅",
                                    body = "الكمية المتبقية من الطماطم في المخزون: 2.5 كجم فقط",
                                    type = NotificationType.LOW_STOCK
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100))
                        ) {
                            Text("إشعار مخزون", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkTaskActionCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    onRunNow: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(iconColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = iconColor, modifier = Modifier.size(24.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onRunNow,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = iconColor)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "تشغيل", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("تشغيل الآن", fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun NotificationItemCard(
    item: NotificationEntity,
    onMarkAsRead: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale("ar")) }
    val formattedTime = remember(item.timestamp) { dateFormat.format(Date(item.timestamp)) }

    val (badgeColor, badgeText, iconVector) = when (item.type) {
        NotificationType.ALERT -> Triple(Color(0xFF1976D2), "تنبيه نظام", Icons.Default.Notifications)
        NotificationType.LOW_STOCK -> Triple(Color(0xFFE65100), "نقص مخزون", Icons.Default.Inventory)
        NotificationType.DEBT_DUE -> Triple(Color(0xFFD32F2F), "تنبيه ديون", Icons.Default.Warning)
        NotificationType.SHIFT_CLOSE -> Triple(Color(0xFF388E3C), "إغلاق شفت", Icons.Default.CheckCircle)
        NotificationType.SYNC -> Triple(Color(0xFF00796B), "مزامنة ونسخ", Icons.Default.Backup)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isRead) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .background(badgeColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(iconVector, contentDescription = null, tint = badgeColor, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(badgeText, color = badgeColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(formattedTime, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                if (!item.isRead) {
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onMarkAsRead, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.DoneAll, contentDescription = "تحديد كمقروء", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    }
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.Gray, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = item.title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = item.message,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
