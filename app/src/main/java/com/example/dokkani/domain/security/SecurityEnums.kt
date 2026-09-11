package com.example.dokkani.domain.security

/**
 * حالات ترخيص نظام دكاني
 */
enum class LicenseStatus(val labelArabic: String) {
    TRIAL("فترة تجريبية مجانية"),
    SUBSCRIPTION("اشتراك نشط (أقساط / شهري)"),
    LIFETIME("مرخص نهائياً مدى الحياة"),
    EXPIRED("منتهي الصلاحية"),
    TAMPERED("تلاعب في ساعة الجهاز (مقفل أمنياً)")
}

/**
 * خطط التفعيل والترخيص المدعومة
 */
enum class ActivationPlan(
    val code: String,
    val labelArabic: String,
    val durationDays: Int,
    val maxInvoices: Int,
    val isLifetime: Boolean
) {
    TRIAL_500("TRL", "تجربة مجانية (500 فاتورة)", 0, 500, false),
    TRIAL_1000("TRK", "تجربة مجانية موسعة (1000 فاتورة)", 0, 1000, false),
    MONTHLY_1("M01", "اشتراك شهري (30 يوماً)", 30, 0, false),
    MONTHLY_3("M03", "اشتراك ربع سنوي (90 يوماً)", 90, 0, false),
    MONTHLY_6("M06", "اشتراك نصف سنوي (180 يوماً)", 180, 0, false),
    YEARLY_1("Y01", "اشتراك سنوي (365 يوماً)", 365, 0, false),
    INSTALLMENT("INS", "قسط شهري معتمد (30 يوماً)", 30, 0, false),
    LIFETIME("LFT", "تفعيل دائم مدى الحياة (غير محدود)", 0, 0, true)
}

/**
 * نتيجة فحص التلاعب بالأمان والوقت
 */
data class TamperCheckResult(
    val isTampered: Boolean,
    val reasonArabic: String = "",
    val systemTime: Long = System.currentTimeMillis(),
    val latestInvoiceTime: Long = 0L,
    val timeDiscrepancyMs: Long = 0L
)

/**
 * تقييم حالة الترخيص اللحظية في النظام
 */
data class LicenseEvaluationResult(
    val status: LicenseStatus,
    val isLocked: Boolean,
    val canCreateInvoice: Boolean,
    val isLifetime: Boolean,
    val expiryTimestamp: Long?,
    val daysRemaining: Int,
    val totalInvoicesIssued: Int,
    val maxAllowedInvoices: Int,
    val invoicesRemaining: Int,
    val isNearExpiryWarning: Boolean, // أقل من أو يساوي 3 أيام
    val warningMessageArabic: String?,
    val deviceFingerprint: String,
    val isTimeTampered: Boolean
)
