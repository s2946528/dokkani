package com.example.dokkani.ui.screens.notifications

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dokkani.notifications.DokkaniFirebaseMessagingService
import com.example.dokkani.notifications.NotificationHelper
import com.example.dokkani.notifications.NotificationWorkScheduler
import com.google.firebase.messaging.FirebaseMessaging

/**
 * بطاقة إدارة الإشعارات المحلية والسحابية (FCM) والتحكم في التنبيهات في إعدادات النظام
 */
@Composable
fun NotificationManagementCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(NotificationHelper.hasNotificationPermission(context)) }
    var fcmToken by remember { mutableStateOf(DokkaniFirebaseMessagingService.getStoredToken(context) ?: "جاري التحميل...") }

    // launcher لطلب إذن الإشعارات لأندرويد 13+
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (isGranted) {
            Toast.makeText(context, "تم تفعيل إذن الإشعارات بنجاح!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "تم رفض الإذن. لن تتمكن من استلام التنبيهات بالخلفية.", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful && task.result != null) {
                fcmToken = task.result
            }
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("notification_management_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. الترويسة الرئيسية
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "نظام الإشعارات والتنبيهات الذكية (FCM & Local)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "إدارة التنبيهات السحابية، تذكيرات الشفت والمخزون، والرسائل التعليمية",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // 2. حالة الإذن وإمكانية طلبه
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (hasPermission) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                border = BorderStroke(1.dp, if (hasPermission) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = if (hasPermission) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (hasPermission) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (hasPermission) "إذن الإشعارات مفعّل في الجهاز" else "إذن الإشعارات غير مفعّل",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (hasPermission) "التطبيق جاهز لاستلام تنبيهات المخزون والسحابة والعمل بالخلفية." else "اضغط على الزر لمنح الإذن واستلام الإشعارات في شريط الحالة.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (!hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("تفعيل الإذن", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 3. عرض رمز الجهاز السحابي (FCM Token)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Cloud, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("رمز استقبال الإشعارات السحابية (FCM Token):", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        TextButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("FCM Token", fcmToken))
                                Toast.makeText(context, "تم نسخ رمز FCM إلى الحافظة بنجاح", Toast.LENGTH_SHORT).show()
                            },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("نسخ الرمز", fontSize = 10.sp)
                        }
                    }
                    Text(
                        text = fcmToken,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 2
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "⚡ إجراءات واختبار التنبيهات السريعة (شريط النظام المنسدل):",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary
            )

            // 4. أزرار إطلاق واختبار الإشعارات فورياً
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            NotificationHelper.showLowStockNotification(
                                context = context,
                                productName = "حليب نادك 1 لتر",
                                currentStock = 2.0,
                                minStock = 5.0
                            )
                            Toast.makeText(context, "تم إرسال إشعار نقص المخزون لشريط النظام!", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تنبيه نقص مخزون", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            NotificationHelper.showShiftCloseNotification(context, "SHIFT-008")
                            Toast.makeText(context, "تم إرسال إشعار تذكير إغلاق الشفت!", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تذكير إغلاق الشفت", fontSize = 11.sp)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            NotificationHelper.showBackupReminderNotification(context)
                            Toast.makeText(context, "تم إرسال تذكير النسخ الاحتياطي!", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تذكير النسخ الاحتياطي", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            NotificationWorkScheduler.triggerImmediateMotivationalWorker(context)
                            Toast.makeText(context, "تم إشغال WorkManager وإطلاق نصيحة تحفيزية!", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("نصيحة تحفيزية (WorkManager)", fontSize = 11.sp)
                    }
                }

                Button(
                    onClick = {
                        NotificationHelper.showCloudNotification(
                            context = context,
                            title = "☁️ رسالة سحابية تجريبية من دكاني",
                            body = "مرحباً بك! يعمل نظام الإشعارات السحابية (FCM) بكفاءة عالية وتم ربطه مع شريط الحالة المنسدل.",
                            targetScreen = NotificationHelper.SCREEN_REPORTS
                        )
                        Toast.makeText(context, "تم اختبار الإشعار السحابي وتوليده!", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CloudDone, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("اختبار إشعار FCM السحابي والشريط المنسدل", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}
