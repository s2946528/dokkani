package com.example.dokkani.domain.security

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

/**
 * نتيجة فحص ومعالجة كود التفعيل المستلم من التاجر
 */
data class ActivationVerificationResult(
    val isSuccess: Boolean,
    val messageArabic: String,
    val newStatus: LicenseStatus = LicenseStatus.TRIAL,
    val isLifetime: Boolean = false,
    val expiryTimestamp: Long? = null,
    val maxAllowedInvoices: Int = 500,
    val targetFingerprint: String = "",
    val plan: ActivationPlan = ActivationPlan.TRIAL_500
)

/**
 * المحرك المركزي للترخيص والتفعيل بدون إنترنت (Offline Challenge-Response Engine)
 * يعتمد على خوارزمية HMAC-SHA256 وبصمة العتاد Device Fingerprint
 */
object OfflineLicenseManager {

    // المفتاح المشترك السري لنظام دكاني لحساب HMAC-SHA256 دون الحاجة للاتصال بالإنترنت
    internal const val MASTER_CRYPTO_KEY = "DOKKANI_POS_ERP_SECURE_HMAC_MASTER_KEY_2026_OFFLINE_PROD"

    /**
     * توليد كود الطلب (Challenge / Request Code) من هاتف البقالة
     * يجمع بين: (بصمة الجهاز + كود عشوائي Nonce + نوع الخطة + التوقيع الرقمي)
     * مثال: REQ-DK-A8F2-7C3B-9E01-M01-4829-B7E2
     */
    fun generateChallengeCode(
        deviceFingerprint: String,
        plan: ActivationPlan,
        nonce: Int = Random.nextInt(1000, 9999),
        requestTime: Long = System.currentTimeMillis()
    ): String {
        // تنظيف البصمة من الفواصل للاختصار
        val cleanFp = deviceFingerprint.trim().uppercase()
        val timeHex = java.lang.Long.toHexString(requestTime / 1000L).uppercase()

        val challengePayload = "$cleanFp|${plan.code}|$nonce|$timeHex"
        val signature = computeHmacSha256(challengePayload, MASTER_CRYPTO_KEY).take(4).uppercase()

        return "REQ-$cleanFp-${plan.code}-$nonce-$signature"
    }

    /**
     * فك وتفكيك كود الاستجابة (Activation Code) والتحقق من التوقيع الرقمي وبصمة الجهاز
     */
    fun verifyAndApplyActivationCode(
        activationCode: String,
        currentDeviceFingerprint: String,
        currentInvoicesCount: Int
    ): ActivationVerificationResult {
        val trimmedCode = activationCode.trim().uppercase()

        if (!trimmedCode.startsWith("ACT-")) {
            return ActivationVerificationResult(
                isSuccess = false,
                messageArabic = "كود التفعيل غير صالح! يجب أن يبدأ الكود بـ ACT-"
            )
        }

        val parts = trimmedCode.split("-")
        // التنسيق المتوقع: ACT-[PLAN]-[FP_SHORT]-[EXPIRY_HEX]-[MAX_INV_HEX]-[NONCE]-[SIG]
        if (parts.size < 7) {
            return ActivationVerificationResult(
                isSuccess = false,
                messageArabic = "تنسيق كود التفعيل غير مكتمل أو به أجزاء مفقودة."
            )
        }

        val planCode = parts[1]
        val fpShort = parts[2]
        val expiryHex = parts[3]
        val maxInvHex = parts[4]
        val nonce = parts[5]
        val receivedSig = parts[6]

        val plan = ActivationPlan.values().firstOrNull { it.code == planCode }
            ?: return ActivationVerificationResult(
                isSuccess = false,
                messageArabic = "نوع باقة الترخيص الواردة في الكود غير معروفة."
            )

        val expiryTimestamp = try {
            java.lang.Long.parseLong(expiryHex, 16)
        } catch (e: Exception) {
            0L
        }

        val maxAllowedInvoices = try {
            Integer.parseInt(maxInvHex, 16)
        } catch (e: Exception) {
            0
        }

        // 1. التحقق من تطابق بصمة الجهاز
        val currentCleanFp = currentDeviceFingerprint.trim().uppercase()
        // الجزء المختصر في الكود يجب أن يطابق أحد مقاطع بصمة الهاتف
        if (!currentCleanFp.contains(fpShort)) {
            return ActivationVerificationResult(
                isSuccess = false,
                messageArabic = "كود التفعيل هذا مربوط بجهاز آخر ومخصص لهاتف مختلف، ولا يتطابق مع بصمة هذا الجهاز ($currentDeviceFingerprint)!"
            )
        }

        // 2. التحقق من التوقيع الرقمي للترخيص عبر HMAC-SHA256
        val expectedPayload = "$currentCleanFp|$planCode|$expiryHex|$maxInvHex|$nonce"
        val computedFullSig = computeHmacSha256(expectedPayload, MASTER_CRYPTO_KEY)
        val expectedShortSig = computedFullSig.take(8).uppercase()

        if (!MessageDigest.isEqual(receivedSig.toByteArray(), expectedShortSig.toByteArray())) {
            return ActivationVerificationResult(
                isSuccess = false,
                messageArabic = "كود التفعيل غير صحيح أو تم التعديل عليه! فشل التحقق من التوقيع الرقمي المعتمد."
            )
        }

        // 3. تحديد حالة الترخيص الجديدة
        val (newStatus, isLifetime, finalExpiry) = if (plan.isLifetime) {
            Triple(LicenseStatus.LIFETIME, true, null)
        } else if (expiryTimestamp > 0L) {
            // فحص إذا كان تاريخ الصلاحية في المستقبل
            val now = System.currentTimeMillis()
            if (expiryTimestamp < now) {
                Triple(LicenseStatus.EXPIRED, false, expiryTimestamp)
            } else {
                Triple(LicenseStatus.SUBSCRIPTION, false, expiryTimestamp)
            }
        } else {
            Triple(LicenseStatus.TRIAL, false, null)
        }

        return ActivationVerificationResult(
            isSuccess = true,
            messageArabic = "تم تفعيل نظام دكاني بنجاح! باقة الترخيص: ${plan.labelArabic}",
            newStatus = newStatus,
            isLifetime = isLifetime,
            expiryTimestamp = finalExpiry,
            maxAllowedInvoices = if (isLifetime) Int.MAX_VALUE else if (maxAllowedInvoices > 0) maxAllowedInvoices else plan.maxInvoices,
            targetFingerprint = currentDeviceFingerprint,
            plan = plan
        )
    }

    /**
     * تقييم حالة الترخيص اللحظية (Locks, Warnings, Operations Count)
     */
    fun evaluateLicense(
        status: LicenseStatus,
        isLifetime: Boolean,
        expiryTimestamp: Long?,
        maxAllowedInvoices: Int,
        totalInvoicesIssued: Int,
        deviceFingerprint: String,
        isTimeTampered: Boolean,
        currentTime: Long = System.currentTimeMillis()
    ): LicenseEvaluationResult {
        // إذا كان هناك تلاعب في الوقت -> قفل فوري
        if (isTimeTampered || status == LicenseStatus.TAMPERED) {
            return LicenseEvaluationResult(
                status = LicenseStatus.TAMPERED,
                isLocked = true,
                canCreateInvoice = false,
                isLifetime = isLifetime,
                expiryTimestamp = expiryTimestamp,
                daysRemaining = 0,
                totalInvoicesIssued = totalInvoicesIssued,
                maxAllowedInvoices = maxAllowedInvoices,
                invoicesRemaining = 0,
                isNearExpiryWarning = false,
                warningMessageArabic = "تم قفل التطبيق أمنياً بسبب اكتشاف تلاعب في ساعة الجهاز أو تاريخه! يرجى ضبط الساعة للوقت الحالي الصحيح.",
                deviceFingerprint = deviceFingerprint,
                isTimeTampered = true
            )
        }

        // التفعيل الدائم مدى الحياة
        if (isLifetime || status == LicenseStatus.LIFETIME) {
            return LicenseEvaluationResult(
                status = LicenseStatus.LIFETIME,
                isLocked = false,
                canCreateInvoice = true,
                isLifetime = true,
                expiryTimestamp = null,
                daysRemaining = 9999,
                totalInvoicesIssued = totalInvoicesIssued,
                maxAllowedInvoices = 0,
                invoicesRemaining = 999999,
                isNearExpiryWarning = false,
                warningMessageArabic = null,
                deviceFingerprint = deviceFingerprint,
                isTimeTampered = false
            )
        }

        // وضع الاشتراك الشهري أو التقسيط المحدد بتاريخ انتهاء
        if (status == LicenseStatus.SUBSCRIPTION && expiryTimestamp != null) {
            val diffMs = expiryTimestamp - currentTime
            val daysRemaining = (diffMs / (1000L * 60 * 60 * 24)).toInt().coerceAtLeast(0)

            if (diffMs <= 0) {
                return LicenseEvaluationResult(
                    status = LicenseStatus.EXPIRED,
                    isLocked = true,
                    canCreateInvoice = false,
                    isLifetime = false,
                    expiryTimestamp = expiryTimestamp,
                    daysRemaining = 0,
                    totalInvoicesIssued = totalInvoicesIssued,
                    maxAllowedInvoices = maxAllowedInvoices,
                    invoicesRemaining = 0,
                    isNearExpiryWarning = false,
                    warningMessageArabic = "انتهت صلاحية قسط/اشتراك التطبيق. يرجى تجديد الاشتراك وتفعيل الكود لمتابعة إصدار الفواتير.",
                    deviceFingerprint = deviceFingerprint,
                    isTimeTampered = false
                )
            }

            val isNearExpiry = daysRemaining <= 3
            val warningMsg = if (isNearExpiry) {
                "تنبيه: متبقي $daysRemaining أيام فقط على انتهاء القسط/الاشتراك! يرجى التواصل لتجديد الترخيص."
            } else null

            return LicenseEvaluationResult(
                status = LicenseStatus.SUBSCRIPTION,
                isLocked = false,
                canCreateInvoice = true,
                isLifetime = false,
                expiryTimestamp = expiryTimestamp,
                daysRemaining = daysRemaining,
                totalInvoicesIssued = totalInvoicesIssued,
                maxAllowedInvoices = maxAllowedInvoices,
                invoicesRemaining = 999999,
                isNearExpiryWarning = isNearExpiry,
                warningMessageArabic = warningMsg,
                deviceFingerprint = deviceFingerprint,
                isTimeTampered = false
            )
        }

        // الوضع المجاني (TRIAL) - يعتمد على عدد العمليات
        val limit = if (maxAllowedInvoices > 0) maxAllowedInvoices else 500
        val remaining = (limit - totalInvoicesIssued).coerceAtLeast(0)
        val isLocked = totalInvoicesIssued >= limit

        val warningMsg = when {
            isLocked -> "تم استهلاك كامل العمليات التجريبية ($limit فاتورة). يرجى تفعيل النسخة الكاملة للاستمرار."
            remaining <= 50 -> "تنبيه: متبقي $remaining فاتورة فقط في النسخة التجريبية المجانية."
            else -> null
        }

        return LicenseEvaluationResult(
            status = if (isLocked) LicenseStatus.EXPIRED else LicenseStatus.TRIAL,
            isLocked = isLocked,
            canCreateInvoice = !isLocked,
            isLifetime = false,
            expiryTimestamp = null,
            daysRemaining = 0,
            totalInvoicesIssued = totalInvoicesIssued,
            maxAllowedInvoices = limit,
            invoicesRemaining = remaining,
            isNearExpiryWarning = isLocked || remaining <= 50,
            warningMessageArabic = warningMsg,
            deviceFingerprint = deviceFingerprint,
            isTimeTampered = false
        )
    }

    /**
     * حساب HMAC-SHA256
     */
    internal fun computeHmacSha256(data: String, key: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        val secretKey = SecretKeySpec(key.toByteArray(Charsets.UTF_8), "HmacSHA256")
        mac.init(secretKey)
        val bytes = mac.doFinal(data.toByteArray(Charsets.UTF_8))
        val sb = StringBuilder()
        for (b in bytes) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }
}
