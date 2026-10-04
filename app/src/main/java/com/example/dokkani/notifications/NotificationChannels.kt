package com.example.dokkani.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

/**
 * إدارة وتجهيز قنوات الإشعارات (Notification Channels) لنظام أندرويد 8.0+
 */
object NotificationChannels {
    const val CHANNEL_ALERTS_ID = "dokkani_alerts_channel"
    const val CHANNEL_EDUCATIONAL_ID = "dokkani_educational_channel"
    const val CHANNEL_CLOUD_ID = "dokkani_cloud_fcm_channel"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                if (notificationManager == null) return

                // 1. قناة التنبيهات المهمة والحرجة (نواقص المخزون، إغلاق الشفت، النسخ الاحتياطي)
                val alertsChannel = NotificationChannel(
                    CHANNEL_ALERTS_ID,
                    "التنبيهات المهمة والحرجة",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "تنبيهات فورية لنواقص المخزون وإغلاق الشفت وتذكيرات النسخ الاحتياطي"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 300, 200, 300)
                }

                // 2. قناة النصائح التعليمية والتحفيزية
                val eduChannel = NotificationChannel(
                    CHANNEL_EDUCATIONAL_ID,
                    "الرسائل التعليمية والتحفيزية",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "نصائح محاسبية وإدارية يومية وتذكيرات إشرافية تحفيزية"
                    enableVibration(false)
                }

                // 3. قناة الإشعارات السحابية العامة (FCM)
                val cloudChannel = NotificationChannel(
                    CHANNEL_CLOUD_ID,
                    "الإشعارات السحابية العامة (FCM)",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "رسائل وتحديثات سحابية فورية مرسلة من لوحة التحكم"
                    enableVibration(true)
                }

                notificationManager.createNotificationChannels(
                    listOf(alertsChannel, eduChannel, cloudChannel)
                )
            } catch (e: Exception) {
                android.util.Log.e("NotificationChannels", "Error creating notification channels: ${e.message}", e)
            }
        }
    }
}
