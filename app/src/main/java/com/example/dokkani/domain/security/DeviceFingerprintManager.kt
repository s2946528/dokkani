package com.example.dokkani.domain.security

import android.content.Context
import android.os.Build
import android.provider.Settings
import java.security.MessageDigest
import java.util.Locale

/**
 * مدير استخراج وتوليد بصمة الجهاز الفريدة (Device Fingerprint / Hardware UUID)
 * لربط الترخيص وقاعدة البيانات بهاتف البقالة ومنع نسخ التطبيق لجهاز آخر
 */
object DeviceFingerprintManager {

    private var cachedFingerprint: String? = null

    /**
     * استخراج بصمة الجهاز بصيغة كود فريد قابل للقراءة (مثل: DK-A8F2-7C3B-9E01)
     */
    fun getDeviceFingerprint(context: Context): String {
        cachedFingerprint?.let { return it }

        val androidId = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "UNKNOWN_ID"
        } catch (e: Exception) {
            "FALLBACK_ID"
        }

        val hardwareComponents = listOf(
            androidId,
            Build.MANUFACTURER.uppercase(Locale.ROOT),
            Build.MODEL.uppercase(Locale.ROOT),
            Build.DEVICE.uppercase(Locale.ROOT),
            Build.BOARD.uppercase(Locale.ROOT),
            Build.HARDWARE.uppercase(Locale.ROOT),
            Build.PRODUCT.uppercase(Locale.ROOT)
        ).joinToString(separator = "::")

        val rawHash = sha256Hex(hardwareComponents)

        // تنسيق البصمة إلى 4 مقاطع مميزة بأحرف وأرقام لاتينية كبيرة
        // مثال: DK-9B4E-2A1C-8D7F
        val seg1 = rawHash.substring(0, 4).uppercase(Locale.ROOT)
        val seg2 = rawHash.substring(4, 8).uppercase(Locale.ROOT)
        val seg3 = rawHash.substring(8, 12).uppercase(Locale.ROOT)

        val formattedFingerprint = "DK-$seg1-$seg2-$seg3"
        cachedFingerprint = formattedFingerprint
        return formattedFingerprint
    }

    /**
     * استخراج تفاصيل العتاد لعرضها في شاشة معلومات النظام
     */
    fun getHardwareDetails(context: Context): Map<String, String> {
        val androidId = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "غير متاح"
        } catch (e: Exception) {
            "غير متاح"
        }

        return mapOf(
            "معرف النظام (Android ID)" to androidId,
            "الشركة المصنعة" to Build.MANUFACTURER,
            "موديل الجهاز" to Build.MODEL,
            "العتاد الداخلي (Hardware)" to Build.HARDWARE,
            "اللوحة الأم (Board)" to Build.BOARD,
            "إصدار أندرويد" to "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
        )
    }

    private fun sha256Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        val sb = StringBuilder()
        for (b in bytes) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }
}
