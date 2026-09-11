package com.example.dokkani.domain.security

import java.util.Locale

/**
 * نتيجة توليد مفتاح التفعيل عبر أداة المطور
 */
data class KeyGeneratorResult(
    val isSuccess: Boolean,
    val activationCode: String = "",
    val targetFingerprint: String = "",
    val planLabelArabic: String = "",
    val expiryDateFormatted: String = "",
    val messageArabic: String = ""
)

/**
 * مولد أكواد التفعيل لمطور نظام دكاني (Dokkani Key Generator)
 * يستخدمه المطور / صاحب النظام لتوليد كود التفعيل المقابل فور استلام كود الطلب من التاجر
 */
object DokkaniKeyGenerator {

    /**
     * توليد كود التفعيل المعتمد من كود طلب التاجر
     *
     * @param requestCode كود الطلب القادم من هاتف البقالة (مثل: REQ-DK-A8F2-7C3B-9E01-M01-4829-B7E2)
     * @param selectedPlan باقة التفعيل المراد منحها (شهر / 3 شهور / سنة / فتح نهائي)
     * @param customDays عدد أيام مخصص في حال التقسيط الخاص (اختياري)
     */
    fun generateActivationCodeFromRequest(
        requestCode: String,
        selectedPlan: ActivationPlan = ActivationPlan.MONTHLY_1,
        customDays: Int? = null
    ): KeyGeneratorResult {
        val cleanRequest = requestCode.trim().uppercase(Locale.ROOT)

        if (!cleanRequest.startsWith("REQ-")) {
            return KeyGeneratorResult(
                isSuccess = false,
                messageArabic = "كود الطلب غير صحيح! يجب أن يبدأ بـ REQ-"
            )
        }

        // تفكيك كود الطلب
        // مثال: REQ-DK-A8F2-7C3B-9E01-M01-4829-B7E2
        // بعد إزالة "REQ-" يتبقى: DK-A8F2-7C3B-9E01-M01-4829-B7E2
        val body = cleanRequest.removePrefix("REQ-")
        val parts = body.split("-")

        // التنسيق المتوقع لأجزاء البصمة: DK-[SEG1]-[SEG2]-[SEG3]-[PLAN]-[NONCE]-[SIG]
        if (parts.size < 6) {
            return KeyGeneratorResult(
                isSuccess = false,
                messageArabic = "كود الطلب ناقص ولا يحتوي على بصمة جهاز صالحة."
            )
        }

        val fingerprint = if (parts[0] == "DK") {
            "DK-${parts[1]}-${parts[2]}-${parts[3]}"
        } else {
            parts[0]
        }

        // استخراج الـ Nonce (قبل الأخير)
        val nonce = parts[parts.size - 2]
        val fpShort = parts[1] // مثل: A8F2

        // حساب تاريخ الصلاحية
        val now = System.currentTimeMillis()
        val days = customDays ?: selectedPlan.durationDays

        val expiryTimestamp = if (selectedPlan.isLifetime) {
            0L
        } else {
            now + (days.toLong() * 24L * 60L * 60L * 1000L)
        }

        val expiryHex = java.lang.Long.toHexString(expiryTimestamp).uppercase(Locale.ROOT)
        val maxInvHex = Integer.toHexString(selectedPlan.maxInvoices).uppercase(Locale.ROOT)

        // حساب توقيع HMAC-SHA256 لنفس الـ Payload المتوقع
        val payloadToSign = "$fingerprint|${selectedPlan.code}|$expiryHex|$maxInvHex|$nonce"
        val computedFullSig = OfflineLicenseManager.computeHmacSha256(
            payloadToSign,
            OfflineLicenseManager.MASTER_CRYPTO_KEY
        )
        val signatureShort = computedFullSig.take(8).uppercase(Locale.ROOT)

        // تركيب كود التفعيل المعتمد
        // ACT-[PLAN]-[FP_SHORT]-[EXPIRY_HEX]-[MAX_INV_HEX]-[NONCE]-[SIG]
        val activationCode = "ACT-${selectedPlan.code}-$fpShort-$expiryHex-$maxInvHex-$nonce-$signatureShort"

        val expiryFormatted = if (selectedPlan.isLifetime) {
            "مدى الحياة (مفتوح دائم)"
        } else {
            java.text.SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(java.util.Date(expiryTimestamp))
        }

        return KeyGeneratorResult(
            isSuccess = true,
            activationCode = activationCode,
            targetFingerprint = fingerprint,
            planLabelArabic = selectedPlan.labelArabic,
            expiryDateFormatted = expiryFormatted,
            messageArabic = "تم توليد كود التفعيل المعتمد بنجاح!"
        )
    }

    /**
     * دالة لتوليد كود تفعيل مباشر بمعلومية بصمة الهاتف ونوع الباقة دون الحاجة لكود طلب
     */
    fun generateDirectActivationCode(
        fingerprint: String,
        plan: ActivationPlan,
        nonce: String = "9999",
        durationDays: Int? = null
    ): KeyGeneratorResult {
        val cleanFp = fingerprint.trim().uppercase(Locale.ROOT)
        val parts = cleanFp.split("-")
        val fpShort = if (parts.size >= 2) parts[1] else cleanFp.take(4)

        val now = System.currentTimeMillis()
        val days = durationDays ?: plan.durationDays
        val expiryTimestamp = if (plan.isLifetime) 0L else now + (days.toLong() * 24L * 60L * 60L * 1000L)

        val expiryHex = java.lang.Long.toHexString(expiryTimestamp).uppercase(Locale.ROOT)
        val maxInvHex = Integer.toHexString(plan.maxInvoices).uppercase(Locale.ROOT)

        val payloadToSign = "$cleanFp|${plan.code}|$expiryHex|$maxInvHex|$nonce"
        val computedFullSig = OfflineLicenseManager.computeHmacSha256(
            payloadToSign,
            OfflineLicenseManager.MASTER_CRYPTO_KEY
        )
        val signatureShort = computedFullSig.take(8).uppercase(Locale.ROOT)

        val activationCode = "ACT-${plan.code}-$fpShort-$expiryHex-$maxInvHex-$nonce-$signatureShort"

        val expiryFormatted = if (plan.isLifetime) {
            "مدى الحياة (دائم)"
        } else {
            java.text.SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(java.util.Date(expiryTimestamp))
        }

        return KeyGeneratorResult(
            isSuccess = true,
            activationCode = activationCode,
            targetFingerprint = cleanFp,
            planLabelArabic = plan.labelArabic,
            expiryDateFormatted = expiryFormatted,
            messageArabic = "تم توليد كود التفعيل المباشر بنجاح!"
        )
    }
}

/**
 * نقطة دخول لتشغيل مولد المفاتيح كسكربت منفصل للمطور (CLI / Standalone)
 */
fun main(args: Array<String>) {
    println("==================================================")
    println("  مولد مفاتيح وتراخيص نظام دكاني (Dokkani KeyGen)  ")
    println("==================================================")

    val sampleRequest = "REQ-DK-A8F2-7C3B-9E01-M01-4829-B7E2"
    println("تجربة توليد كود لكود طلب تجريبي: $sampleRequest")

    val resultMonthly = DokkaniKeyGenerator.generateActivationCodeFromRequest(
        sampleRequest,
        ActivationPlan.MONTHLY_1
    )
    println("\n[1] كود اشتراك شهري (30 يوماً):")
    println("-> ${resultMonthly.activationCode}")
    println("الصلاحية حتى: ${resultMonthly.expiryDateFormatted}")

    val resultLifetime = DokkaniKeyGenerator.generateActivationCodeFromRequest(
        sampleRequest,
        ActivationPlan.LIFETIME
    )
    println("\n[2] كود فتح نهائي دائم مدى الحياة:")
    println("-> ${resultLifetime.activationCode}")
    println("الصلاحية: ${resultLifetime.expiryDateFormatted}")
    println("==================================================")
}
