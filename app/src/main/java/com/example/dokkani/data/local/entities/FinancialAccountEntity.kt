package com.example.dokkani.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * أنواع الحسابات المالية في الدليل المحاسبي
 */
enum class FinancialAccountType(val labelArabic: String) {
    BANK("حساب بنكي"),
    E_WALLET("محفظة إلكترونية"),
    CASH_DRAWER("صندوق / خزينة نقدية"),
    CHART_ACCOUNT("حساب عام بالدليل"),
    LIABILITY("خصوم والتزامات"),
    EXPENSE("مصروفات ونفقات")
}

/**
 * قائمة الحسابات الرئيسية المعيارية في الدليل المحاسبي (Chart of Accounts Categories)
 */
object ChartOfAccountsDefaults {
    data class ParentAccount(val code: String, val name: String, val defaultType: FinancialAccountType)

    val PARENT_ACCOUNTS = listOf(
        ParentAccount("101", "101 - النقدية وما في حكمها (الصناديق)", FinancialAccountType.CASH_DRAWER),
        ParentAccount("102", "102 - البنوك والمصارف التجارية", FinancialAccountType.BANK),
        ParentAccount("103", "103 - محافظ الدفع والتحصيل الإلكتروني", FinancialAccountType.E_WALLET),
        ParentAccount("104", "104 - الأصول المتداولة الأخرى", FinancialAccountType.CHART_ACCOUNT),
        ParentAccount("105", "105 - الأصول الثابتة غير الملموسة (خلو رجل / نقل قدم)", FinancialAccountType.CHART_ACCOUNT),
        ParentAccount("201", "201 - الخصوم المتداولة والدائنون", FinancialAccountType.LIABILITY),
        ParentAccount("501", "501 - المصروفات والنثريات التشغيلية", FinancialAccountType.EXPENSE)
    )
}

/**
 * جدول الحسابات المالية والبنوك والمحافظ والدليل المحاسبي (Financial Accounts)
 */
@Entity(
    tableName = "financial_accounts",
    indices = [
        Index(value = ["code"], unique = true),
        Index(value = ["accountType"]),
        Index(value = ["parentAccountCode"])
    ]
)
data class FinancialAccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val code: String,                      // كود الحساب الفرعي (مثل: 10201)
    val name: String,                      // اسم الحساب (مثل: مصرف الراجحي، محفظة STC Pay)
    val accountType: FinancialAccountType, // نوع الحساب المالي
    val parentAccountCode: String,         // كود الحساب الرئيسي في الدليل
    val parentAccountName: String,         // اسم الحساب الرئيسي في الدليل
    val accountNumber: String = "",        // رقم الحساب المصرفي / الآيبان IBAN / رقم المحفظة
    val openingBalance: Double = 0.0,      // الرصيد الافتتاحي
    val currentBalance: Double = 0.0,      // الرصيد الجاري الحالي
    val currency: String = "ر.ي",          // العملة
    val isActive: Boolean = true,          // حالة الحساب: نشط أو معطل
    val isDefault: Boolean = false,        // هل هو الحساب الافتراضي للدفع/التحصيل
    val notes: String = "",                // إعدادات وملاحظات إضافية
    val createdAt: Long = System.currentTimeMillis()
)
