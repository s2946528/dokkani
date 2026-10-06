package com.example.dokkani.notifications

import android.util.Log
import com.example.dokkani.data.local.DokkaniDatabase
import com.example.dokkani.data.local.entities.NotificationEntity
import com.example.dokkani.data.local.entities.NotificationType
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * خدمة استقبال الإشعارات السحابية (Firebase Cloud Messaging - FCM)
 */
class DokkaniMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("DokkaniFCM", "تم توليد رمز FCM جديد: $token")
        FcmTokenManager.saveToken(applicationContext, token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d("DokkaniFCM", "تم استقبال إشعار سحابي جديد من: ${remoteMessage.from}")

        // استخراج العنوان والنص من الرسالة السحابية
        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "تنبيه نظام دكاني"

        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: "لديك إشعار جديد في نقطة البيع"

        val typeStr = remoteMessage.data["type"] ?: "ALERT"
        val notificationType = try {
            NotificationType.valueOf(typeStr)
        } catch (e: Exception) {
            NotificationType.ALERT
        }

        val channelId = when (notificationType) {
            NotificationType.DEBT_DUE, NotificationType.LOW_STOCK -> NotificationHelper.CHANNEL_DEBTS
            NotificationType.SYNC -> NotificationHelper.CHANNEL_SYNC
            else -> NotificationHelper.CHANNEL_ALERTS
        }

        // 1. عرض الإشعار في شريط التنبيهات علوياً
        NotificationHelper.showNotification(
            context = applicationContext,
            title = title,
            body = body,
            channelId = channelId
        )

        // 2. حفظ الإشعار في قاعدة البيانات المحلية (Room) لعرضه في مركز الإشعارات داخل التطبيق
        serviceScope.launch {
            try {
                val db = DokkaniDatabase.getDatabase(applicationContext, serviceScope)
                db.notificationDao().insertNotification(
                    NotificationEntity(
                        title = title,
                        message = body,
                        type = notificationType,
                        timestamp = System.currentTimeMillis(),
                        isRead = false,
                        payloadData = remoteMessage.data.toString()
                    )
                )
            } catch (e: Exception) {
                Log.e("DokkaniFCM", "خطأ في حفظ الإشعار بقاعدة البيانات: ${e.message}")
            }
        }
    }
}
