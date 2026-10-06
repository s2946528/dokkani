package com.example.dokkani.notifications

import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * مدير الرمز السحابي FCM Token لنظام دكاني
 */
object FcmTokenManager {

    private const val TAG = "FcmTokenManager"
    private const val PREFS_NAME = "dokkani_fcm_prefs"
    private const val KEY_FCM_TOKEN = "fcm_token"

    private val _currentToken = MutableStateFlow<String?>(null)
    val currentToken: StateFlow<String?> = _currentToken.asStateFlow()

    /**
     * حفظ الرمز في التفضيلات المحلية والشاشة
     */
    fun saveToken(context: Context, token: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_FCM_TOKEN, token).apply()
        _currentToken.value = token
        Log.d(TAG, "تم تحديث رمز FCM Token: $token")
    }

    /**
     * استرجاع الرمز المحفوظ محلياً
     */
    fun getSavedToken(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_FCM_TOKEN, null)
        if (saved != null) {
            _currentToken.value = saved
        }
        return saved
    }

    /**
     * جلب الرمز السحابي مع توفير رمز محلي احتياطي عند بيئات التشغيل/المحاكاة الخالية من خدمات Google Play
     */
    suspend fun fetchFcmToken(context: Context): String {
        val saved = getSavedToken(context)

        return try {
            val token = FirebaseMessaging.getInstance().token.await()
            if (!token.isNullOrEmpty()) {
                saveToken(context, token)
                token
            } else {
                saved ?: generateAndSaveFallbackToken(context)
            }
        } catch (e: Exception) {
            Log.w(TAG, "تنبيه: تعذر الاتصال بمركز تسجيل FCM (قد يكون المحاكي بدون Google Play Services): ${e.message}")
            val fallback = saved ?: generateAndSaveFallbackToken(context)
            _currentToken.value = fallback
            fallback
        }
    }

    private fun generateAndSaveFallbackToken(context: Context): String {
        val fallbackToken = "DEV-FCM-TOKEN-" + UUID.randomUUID().toString().take(12).uppercase()
        saveToken(context, fallbackToken)
        return fallbackToken
    }
}
