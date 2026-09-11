package com.example.dokkani.domain.security

import android.os.SystemClock

/**
 * حارس الأمان ومكافحة التلاعب بتاريخ وساعة الجهاز (Anti-Time Tampering Engine)
 * يفحص انقضاء الوقت ويمنع إرجاع تاريخ الهاتف للوراء للالتفاف على فترات الاشتراك والتقسيط
 */
object AntiTamperGuard {

    // سماحية مقبولة لضبط الثواني أو دقائق بسيطة (2 دقيقة)
    private const val CLOCK_SKEW_TOLERANCE_MS = 120_000L

    /**
     * فحص شامل لسلامة وقت الجهاز ومقارنته بآخر المعاملات المسجلة
     *
     * @param currentSystemTime الوقت الحالي للنظام عبر System.currentTimeMillis()
     * @param latestInvoiceTime تاريخ آخر فاتورة مسجلة في قاعدة البيانات
     * @param lastKnownSystemTime آخر وقت نظام تم حفظه مسبقاً في إعدادات الترخيص
     * @param previousElapsedRealtime آخر قراءة مسجلة للوقت المنقضي منذ تشغيل المعالج
     * @param currentElapsedRealtime قراءة الوقت المنقضي الحالية SystemClock.elapsedRealtime()
     */
    fun verifyTimeIntegrity(
        currentSystemTime: Long = System.currentTimeMillis(),
        latestInvoiceTime: Long = 0L,
        lastKnownSystemTime: Long = 0L,
        previousElapsedRealtime: Long = 0L,
        currentElapsedRealtime: Long = SystemClock.elapsedRealtime()
    ): TamperCheckResult {
        // 1. فحص الرجوع للوراء مقارنة بآخر فاتورة مسجلة في قاعدة البيانات
        if (latestInvoiceTime > 0L) {
            val invoiceDiff = latestInvoiceTime - currentSystemTime
            if (invoiceDiff > CLOCK_SKEW_TOLERANCE_MS) {
                return TamperCheckResult(
                    isTampered = true,
                    reasonArabic = "تم اكتشاف إرجاع ساعة الهاتف للوراء! يوجد فواتير مسجلة في النظام بتاريخ أحدث من تاريخ الجهاز الحالي.",
                    systemTime = currentSystemTime,
                    latestInvoiceTime = latestInvoiceTime,
                    timeDiscrepancyMs = invoiceDiff
                )
            }
        }

        // 2. فحص الرجوع للوراء مقارنة بآخر وقت محفوظ للنظام
        if (lastKnownSystemTime > 0L) {
            val knownDiff = lastKnownSystemTime - currentSystemTime
            if (knownDiff > CLOCK_SKEW_TOLERANCE_MS) {
                return TamperCheckResult(
                    isTampered = true,
                    reasonArabic = "تم رصد تقديم أو إرجاع تاريخ الهاتف بصورة غير نظامية مقارنة بآخر تشغيل للتطبيق.",
                    systemTime = currentSystemTime,
                    latestInvoiceTime = latestInvoiceTime,
                    timeDiscrepancyMs = knownDiff
                )
            }
        }

        // 3. فحص التناغم بين الوقت المنقضي لساعة المعالج والوقت الفعلي للجلسة
        if (previousElapsedRealtime > 0L && currentElapsedRealtime > previousElapsedRealtime && lastKnownSystemTime > 0L) {
            val cpuElapsedDelta = currentElapsedRealtime - previousElapsedRealtime
            val wallClockDelta = currentSystemTime - lastKnownSystemTime

            // إذا تقدم وقت المعالج بأكثر من ساعة، لكن ساعة النظام لم تتحرك أو تحركت للوراء
            if (cpuElapsedDelta > 300_000L && wallClockDelta < -CLOCK_SKEW_TOLERANCE_MS) {
                return TamperCheckResult(
                    isTampered = true,
                    reasonArabic = "تم تجميد أو إرجاع وقت الهاتف أثناء عمل التطبيق.",
                    systemTime = currentSystemTime,
                    latestInvoiceTime = latestInvoiceTime,
                    timeDiscrepancyMs = cpuElapsedDelta - wallClockDelta
                )
            }
        }

        // إذا اجتازت جميع الفحوصات
        return TamperCheckResult(
            isTampered = false,
            reasonArabic = "وقت النظام سليم ومنسجم مع سجل الفواتير وقاعدة البيانات.",
            systemTime = currentSystemTime,
            latestInvoiceTime = latestInvoiceTime,
            timeDiscrepancyMs = 0L
        )
    }
}
