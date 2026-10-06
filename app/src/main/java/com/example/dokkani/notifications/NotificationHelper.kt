package com.example.dokkani.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R

/**
 * مدير وقنوات الإشعارات المحلية والسحابية لنظام دكاني
 */
object NotificationHelper {

    const val CHANNEL_ALERTS = "dokkani_alerts_channel"
    const val CHANNEL_SYNC = "dokkani_sync_channel"
    const val CHANNEL_DEBTS = "dokkani_debts_channel"

    const val SCREEN_MAIN = "main"
    const val SCREEN_REPORTS = "reports"
    const val SCREEN_NOTIFICATIONS = "notifications"

    /**
     * إنشاء قنوات الإشعارات للتوافق مع إصدارات أندرويد الحديثة (Android 8.0+)
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // 1. قناة تنبيهات النظام والمبيعات
            val alertsChannel = NotificationChannel(
                CHANNEL_ALERTS,
                "تنبيهات النظام والمبيعات",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات الفواتير، إغلاق الشفت، والتنبيهات التشغيلية الهامة"
                enableVibration(true)
            }

            // 2. قناة المزامنة والنسخ الاحتياطي
            val syncChannel = NotificationChannel(
                CHANNEL_SYNC,
                "المزامنة والنسخ الاحتياطي",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "إشعارات نجاح المزامنة الدورية والنسخ الاحتياطي لقاعدة البيانات"
            }

            // 3. قناة تنبيهات الديون والآجل
            val debtsChannel = NotificationChannel(
                CHANNEL_DEBTS,
                "تنبيهات الديون والآجل والمخزون",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "تنبيهات تجاوز سقف الائتمان واستحقاق ديون العملاء وانخفاض الأصناف"
                enableVibration(true)
            }

            manager.createNotificationChannel(alertsChannel)
            manager.createNotificationChannel(syncChannel)
            manager.createNotificationChannel(debtsChannel)
        }
    }

    /**
     * التحقق من وجود إذن الإشعارات (Android 13+ POST_NOTIFICATIONS)
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * إرسال وعرض إشعار في شريط النظام للأندرويد
     */
    fun showNotification(
        context: Context,
        title: String,
        body: String,
        channelId: String = CHANNEL_ALERTS,
        notificationId: Int = (System.currentTimeMillis() % 10000).toInt()
    ) {
        // التأكد من إنشاء القنوات أولاً
        createNotificationChannels(context)

        if (!hasNotificationPermission(context)) {
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_SCREEN", "notifications")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            with(NotificationManagerCompat.from(context)) {
                notify(notificationId, builder.build())
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun showCloudNotification(
        context: Context,
        title: String,
        body: String,
        targetScreen: String = SCREEN_MAIN
    ) {
        showNotification(context, title, body, CHANNEL_ALERTS)
    }

    fun showEducationalTipNotification(
        context: Context,
        title: String,
        tipContent: String
    ) {
        showNotification(context, title, tipContent, CHANNEL_SYNC)
    }

    fun showLowStockNotification(
        context: Context,
        productName: String,
        currentStock: Double,
        minStock: Double
    ) {
        val title = "⚠️ تنبيه انخفاض المخزون"
        val body = "الصنف $productName شارف على الانتهاء. المتبقي: $currentStock (الحد الأدنى: $minStock)"
        showNotification(context, title, body, CHANNEL_DEBTS)
    }

    fun showShiftCloseNotification(context: Context, shiftCode: String) {
        val title = "⏰ تذكير إغلاق الشفت"
        val body = "يرجى مطابقة الصندوق وإغلاق الشفت الحالي $shiftCode للتحقق من المبيعات والنقدية."
        showNotification(context, title, body, CHANNEL_ALERTS)
    }

    fun showBackupReminderNotification(context: Context) {
        val title = "💾 تذكير النسخ الاحتياطي"
        val body = "حافِظ على سلامة بياناتك! يوصى بإنشاء نسخة احتياطية لقاعدة البيانات الآن."
        showNotification(context, title, body, CHANNEL_SYNC)
    }
}
