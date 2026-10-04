package com.example.dokkani.notifications

import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * خدمة استقبال وتجهيز الرسائل والإشعارات السحابية القادمة من Firebase Cloud Messaging (FCM)
 */
class DokkaniFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "DokkaniFCM"
        const val PREFS_NAME = "dokkani_fcm_prefs"
        const val KEY_FCM_TOKEN = "fcm_device_token"

        fun getStoredToken(context: Context): String? {
            return try {
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .getString(KEY_FCM_TOKEN, null)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get stored token: ${e.message}")
                null
            }
        }
    }

    /**
     * تستدعى عند توليد أو تحديث رمز جهاز الكاشير/المستخدم الفردي (FCM Registration Token)
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "تم توليد رمز FCM جديد للجهاز: $token")

        try {
            // حفظ الرمز في التفضيلات المحلية لتسهيل الربط مع السيرفر أو لوحة الإشعارات
            getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_FCM_TOKEN, token)
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save FCM token: ${e.message}", e)
        }
    }

    /**
     * تستدعى فور استلام رسالة سحابية قادمة من السيرفر (FCM Message)
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        try {
            Log.d(TAG, "تم استقبال رسالة سحابية من: ${remoteMessage.from}")

            // 1. استخراج العنوان والنص الرئيسي للرسالة
            var title = remoteMessage.notification?.title
            var body = remoteMessage.notification?.body

            // 2. إذا كانت الرسالة تحتوي على حمولة بيانات (Data Payload)
            val data = remoteMessage.data
            if (data.isNotEmpty()) {
                if (title.isNullOrBlank()) title = data["title"]
                if (body.isNullOrBlank()) body = data["body"]
            }

            val targetScreen = data["screen"] ?: data["target"] ?: NotificationHelper.SCREEN_MAIN

            // 3. إنشاء وعرض الإشعار في شريط الحالة المنسدل (Notification Drawer)
            if (!body.isNullOrBlank()) {
                NotificationHelper.showCloudNotification(
                    context = applicationContext,
                    title = title ?: "☁️ إشعار سحابي جديد",
                    body = body,
                    targetScreen = targetScreen
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling FCM message: ${e.message}", e)
        }
    }
}
