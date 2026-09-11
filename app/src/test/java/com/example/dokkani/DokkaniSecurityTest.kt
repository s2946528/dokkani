package com.example.dokkani

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.dokkani.domain.security.ActivationPlan
import com.example.dokkani.domain.security.AntiTamperGuard
import com.example.dokkani.domain.security.DeviceFingerprintManager
import com.example.dokkani.domain.security.DokkaniKeyGenerator
import com.example.dokkani.domain.security.LicenseStatus
import com.example.dokkani.domain.security.OfflineLicenseManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DokkaniSecurityTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun testDeviceFingerprintGeneration() {
        val fp = DeviceFingerprintManager.getDeviceFingerprint(context)
        assertNotNull(fp)
        assertTrue("بصمة الجهاز يجب أن تبدأ بـ DK-", fp.startsWith("DK-"))
        assertEquals("بصمة الجهاز يجب أن تتكون من 4 مقاطع مفصولة بشرطة", 4, fp.split("-").size)
    }

    @Test
    fun testChallengeAndActivationCodeFlow() {
        val fp = DeviceFingerprintManager.getDeviceFingerprint(context)
        
        // 1. توليد كود الطلب (Challenge)
        val challenge = OfflineLicenseManager.generateChallengeCode(fp, ActivationPlan.MONTHLY_1)
        assertTrue("كود الطلب يجب أن يبدأ بـ REQ-", challenge.startsWith("REQ-"))

        // 2. توليد كود التفعيل بواسطة المطور
        val keyGenResult = DokkaniKeyGenerator.generateActivationCodeFromRequest(
            requestCode = challenge,
            selectedPlan = ActivationPlan.MONTHLY_1
        )
        assertTrue("توليد المفتاح يجب أن ينجح", keyGenResult.isSuccess)
        val activationCode = keyGenResult.activationCode
        assertTrue("كود التفعيل يجب أن يبدأ بـ ACT-", activationCode.startsWith("ACT-"))

        // 3. فك تشفير وتطبيق كود التفعيل في النظام على نفس الجهاز (Matching Hardware UUID)
        val applyResult = OfflineLicenseManager.verifyAndApplyActivationCode(
            activationCode = activationCode,
            currentDeviceFingerprint = fp,
            currentInvoicesCount = 25
        )
        assertTrue("تطبيق كود التفعيل يجب أن ينجح على نفس الجهاز", applyResult.isSuccess)
        assertEquals(LicenseStatus.SUBSCRIPTION, applyResult.newStatus)
        assertFalse(applyResult.isLifetime)
        assertNotNull(applyResult.expiryTimestamp)

        // 4. اختبار منع تفعيل الكود على جهاز آخر (Hardware Binding Protection)
        val differentFp = "DK-FFFF-EEEE-DDDD"
        val foreignDeviceResult = OfflineLicenseManager.verifyAndApplyActivationCode(
            activationCode = activationCode,
            currentDeviceFingerprint = differentFp,
            currentInvoicesCount = 0
        )
        assertFalse("يجب رفض التفعيل على جهاز ببصمة عتاد مختلفة لمنع القرصنة ونقل التراخيص", foreignDeviceResult.isSuccess)
        assertTrue(foreignDeviceResult.messageArabic.contains("مربوط بجهاز آخر"))
    }

    @Test
    fun testLifetimeLicenseFlow() {
        val fp = DeviceFingerprintManager.getDeviceFingerprint(context)
        val challenge = OfflineLicenseManager.generateChallengeCode(fp, ActivationPlan.LIFETIME)
        
        val keyGenResult = DokkaniKeyGenerator.generateActivationCodeFromRequest(
            requestCode = challenge,
            selectedPlan = ActivationPlan.LIFETIME
        )
        assertTrue(keyGenResult.isSuccess)

        val applyResult = OfflineLicenseManager.verifyAndApplyActivationCode(
            activationCode = keyGenResult.activationCode,
            currentDeviceFingerprint = fp,
            currentInvoicesCount = 100
        )
        assertTrue(applyResult.isSuccess)
        assertEquals(LicenseStatus.LIFETIME, applyResult.newStatus)
        assertTrue(applyResult.isLifetime)
        assertEquals(Int.MAX_VALUE, applyResult.maxAllowedInvoices)
    }

    @Test
    fun testTamperedActivationCodeIsRejected() {
        val fp = DeviceFingerprintManager.getDeviceFingerprint(context)
        val badCode = "ACT-M01-1234-9999-TAMPERED"

        val result = OfflineLicenseManager.verifyAndApplyActivationCode(
            activationCode = badCode,
            currentDeviceFingerprint = fp,
            currentInvoicesCount = 0
        )
        assertFalse("يجب رفض الأكواد المزيفة أو المتلاعب بها رياضياً", result.isSuccess)
    }

    @Test
    fun testAntiTamperGuardDetectsTimeRollback() {
        val now = 1715000000000L // وقت مستقبلي افتراضي (سليم)
        val latestInvoiceTime = 1714900000000L // فاتورة صدرت قبل قليل
        val lastKnownTime = 1714950000000L

        // وقت سليم
        val okCheck = AntiTamperGuard.verifyTimeIntegrity(now, latestInvoiceTime, lastKnownTime)
        assertFalse("الوقت السليم يجب ألا يكتشف تلاعباً", okCheck.isTampered)

        // تلاعب: المستخدم قام بإرجاع ساعة الهاتف لتاريخ يسبق آخر فاتورة مصدرة
        val rolledBackTime = 1714000000000L // وقت يسبق آخر فاتورة بحوالي 10 أيام
        val tamperCheck = AntiTamperGuard.verifyTimeIntegrity(rolledBackTime, latestInvoiceTime, lastKnownTime)
        assertTrue("يجب اكتشاف إرجاع الساعة لتاريخ يسبق الفواتير المسجلة", tamperCheck.isTampered)
        assertNotNull(tamperCheck.reasonArabic)
    }

    @Test
    fun testLicenseEvaluationLockingRules() {
        val fp = "DK-1234-5678-90AB"

        // 1. الوضع التجريبي تجاوز 500 فاتورة -> قفل
        val trialOverLimit = OfflineLicenseManager.evaluateLicense(
            status = LicenseStatus.TRIAL,
            isLifetime = false,
            expiryTimestamp = null,
            maxAllowedInvoices = 500,
            totalInvoicesIssued = 501,
            deviceFingerprint = fp,
            isTimeTampered = false
        )
        assertTrue("تجاوز 500 فاتورة بالنسخة التجريبية يجب أن يقفل النظام", trialOverLimit.isLocked)
        assertFalse("لا يمكن إنشاء فواتير جديدة", trialOverLimit.canCreateInvoice)

        // 2. اشتراك منتهي التاريخ -> قفل
        val expiredSub = OfflineLicenseManager.evaluateLicense(
            status = LicenseStatus.SUBSCRIPTION,
            isLifetime = false,
            expiryTimestamp = System.currentTimeMillis() - 100000L, // انتهى
            maxAllowedInvoices = Int.MAX_VALUE,
            totalInvoicesIssued = 50,
            deviceFingerprint = fp,
            isTimeTampered = false
        )
        assertTrue("الاشتراك منتهي الصلاحية يجب أن يقفل النظام", expiredSub.isLocked)
        assertFalse(expiredSub.canCreateInvoice)

        // 3. تلاعب بالوقت -> قفل فوري
        val tamperedEvaluation = OfflineLicenseManager.evaluateLicense(
            status = LicenseStatus.TAMPERED,
            isLifetime = true,
            expiryTimestamp = null,
            maxAllowedInvoices = Int.MAX_VALUE,
            totalInvoicesIssued = 10,
            deviceFingerprint = fp,
            isTimeTampered = true
        )
        assertTrue("التلاعب بالساعة يجب أن يقفل النظام حتى لو كان مرخصاً", tamperedEvaluation.isLocked)
        assertFalse(tamperedEvaluation.canCreateInvoice)
    }
}
