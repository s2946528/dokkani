package com.example.dokkani.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.dokkani.domain.security.LicenseStatus

/**
 * جدول الترخيص وحماية النظام (App License & Security State)
 * يحفظ حالة تفعيل نظام دكاني، نوع الباقة، تاريخ الانتهاء، ومعلومات مكافحة التلاعب
 */
@Entity(tableName = "app_license")
data class LicenseEntity(
    @PrimaryKey
    val id: Int = 1,                                       // سجل وحيد لحالة الترخيص
    val status: LicenseStatus = LicenseStatus.TRIAL,       // TRIAL / SUBSCRIPTION / LIFETIME / EXPIRED / TAMPERED
    val isLifetime: Boolean = false,                       // هل التفعيل نهائي مدى الحياة
    val expiryTimestamp: Long? = null,                     // تاريخ انتهاء الاشتراك أو القسط بالمللي ثانية
    val maxAllowedInvoices: Int = 500,                     // الحد الأقصى للفواتير في التجربة (500 أو 1000)
    val activatedAt: Long? = null,                         // تاريخ تطبيق آخر كود تفعيل
    val lastKnownSystemTimestamp: Long = System.currentTimeMillis(), // آخر وقت نظام موثوق
    val lastKnownInvoiceTimestamp: Long = 0L,              // تاريخ آخر فاتورة مسجلة
    val isTimeTampered: Boolean = false,                   // علم رصد تلاعب في ساعة الجهاز
    val tamperReason: String? = null,                      // سبب القفل الأمني إن وجد
    val deviceFingerprint: String = "",                    // بصمة الجهاز المربوط بها الترخيص
    val appliedActivationCode: String? = null,             // آخر كود تفعيل معتمد
    val activePlanCode: String = "TRL"                     // رمز باقة التفعيل الحالية
)
