package com.example.dokkani.notifications

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity

/**
 * مدير إنشاء وإطلاق الإشعارات المحلية والسحابية مع دعم التوجيه (Deep Linking)
 */
object NotificationHelper {

    const val EXTRA_TARGET_SCREEN = "extra_target_screen"

    // ثوابت الشاشات للتوجيه
    const val SCREEN_INVENTORY = "inventory"
    const val SCREEN_SHIFT = "shift"
    const val SCREEN_BACKUP = "backup"
    const val SCREEN_REPORTS = "reports"
    const val SCREEN_MAIN = "main"

    private fun createPendingIntent(context: Context, targetScreen: String, notificationId: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_TARGET_SCREEN, targetScreen)
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getActivity(context, notificationId, intent, flags)
    }

    /**
     * التحقق من منح إذن الإشعارات لأندرويد 13+
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * 1. إشعار تنبيه بنواقص المخزون (Low Stock Alert)
     */
    fun showLowStockNotification(context: Context, productName: String, currentStock: Double, minStock: Double) {
        if (!hasNotificationPermission(context)) return

        NotificationChannels.createNotificationChannels(context)

        val notificationId = (System.currentTimeMillis() % 10000).toInt() + 100
        val pendingIntent = createPendingIntent(context, SCREEN_INVENTORY, notificationId)

        val notification = NotificationCompat.Builder(context, NotificationChannels.CHANNEL_ALERTS_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("⚠️ تنبيه نواقص المخزون: $productName")
            .setContentText("انخفض رصيد $productName إلى (%.2f) وحدات، ووصل لحد التنبيه (%.2f). يرجى طلب التوريد.".format(currentStock, minStock))
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "رصيد المادة/الصنف [$productName] في المخزن حالياً هو (%.2f) وحدات فقط، وقد تجاوز الحد الأدنى للتنبيه البالغ (%.2f).\nاضغط هنا لفتح شاشة إدارة المنتجات والجرد لتوريد الكمية.".format(currentStock, minStock)
            ))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }

    /**
     * 2. إشعار تذكير بإغلاق الشفت / الوردية (Shift End Reminder)
     */
    fun showShiftCloseNotification(context: Context, shiftNumber: String = "") {
        if (!hasNotificationPermission(context)) return

        NotificationChannels.createNotificationChannels(context)

        val notificationId = 201
        val pendingIntent = createPendingIntent(context, SCREEN_SHIFT, notificationId)

        val title = if (shiftNumber.isNotBlank()) "🔔 تذكير بإغلاق الوردية رقم ($shiftNumber)" else "🔔 تذكير بإغلاق وردية الكاشير"
        val body = "شارف يوم العمل على الانتهاء. يرجى مراجعة وتصفية صندوق الكاشير وإغلاق الشفت لضمان مطابقة النقدية."

        val notification = NotificationCompat.Builder(context, NotificationChannels.CHANNEL_ALERTS_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$body\n\nانقر هنا للانتقال المباشر لشاشة إدارة الشفتات والدرج."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }

    /**
     * 3. إشعار تذكير بالنسخ الاحتياطي (Backup Reminder)
     */
    fun showBackupReminderNotification(context: Context) {
        if (!hasNotificationPermission(context)) return

        NotificationChannels.createNotificationChannels(context)

        val notificationId = 301
        val pendingIntent = createPendingIntent(context, SCREEN_BACKUP, notificationId)

        val notification = NotificationCompat.Builder(context, NotificationChannels.CHANNEL_ALERTS_ID)
            .setSmallIcon(android.R.drawable.ic_menu_save)
            .setContentTitle("💾 تذكير هام: النسخ الاحتياطي لقاعدة البيانات")
            .setContentText("لحماية بيانات متجرك وحساباتك المالية، ننصح بإنشاء نسخة احتياطية جديدة الآن.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("لحماية بيانات الفواتير والعملاء والمنتجات من الضياع، يفضل إنشاء نسخة احتياطية وتصديرها بانتظام.\nاضغط هنا للانتقال إلى قسم الإعدادات والنسخ الاحتياطي."))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }

    /**
     * 4. إشعار الرسائل التعليمية والتحفيزية اليومية (Educational / Motivational)
     */
    fun showEducationalTipNotification(context: Context, title: String, tipContent: String) {
        if (!hasNotificationPermission(context)) return

        NotificationChannels.createNotificationChannels(context)

        val notificationId = (System.currentTimeMillis() % 10000).toInt() + 400
        val pendingIntent = createPendingIntent(context, SCREEN_REPORTS, notificationId)

        val notification = NotificationCompat.Builder(context, NotificationChannels.CHANNEL_EDUCATIONAL_ID)
            .setSmallIcon(android.R.drawable.btn_star_big_on)
            .setContentTitle("💡 $title")
            .setContentText(tipContent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(tipContent))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }

    /**
     * 5. إشعار الرسائل السحابية العامة (Firebase Cloud Messaging - FCM)
     */
    fun showCloudNotification(context: Context, title: String, body: String, targetScreen: String = SCREEN_MAIN) {
        if (!hasNotificationPermission(context)) return

        NotificationChannels.createNotificationChannels(context)

        val notificationId = (System.currentTimeMillis() % 10000).toInt() + 500
        val pendingIntent = createPendingIntent(context, targetScreen, notificationId)

        val notification = NotificationCompat.Builder(context, NotificationChannels.CHANNEL_CLOUD_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title.ifBlank { "☁️ إشعار جديد من دكاني السحابي" })
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }
}
