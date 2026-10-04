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
    data class ParentAccount(
        val code: String,
        val name: String,
        val defaultType: FinancialAccountType,
        val rootGroupCode: String = "1",
        val defaultNature: String = "DEBIT",
        val defaultFinalAccount: String = "BALANCE_SHEET"
    )

    val PARENT_ACCOUNTS = listOf(
        // 1. الأصول (Assets) - كود المجموعة: 1 - مدين - الميزانية العمومية
        ParentAccount("101", "101 - النقدية وما في حكمها (الصناديق والدرج)", FinancialAccountType.CASH_DRAWER, "1", "DEBIT", "BALANCE_SHEET"),
        ParentAccount("102", "102 - البنوك والمصارف التجارية", FinancialAccountType.BANK, "1", "DEBIT", "BALANCE_SHEET"),
        ParentAccount("103", "103 - محافظ الدفع والتحصيل الإلكتروني", FinancialAccountType.E_WALLET, "1", "DEBIT", "BALANCE_SHEET"),
        ParentAccount("104", "104 - الأصول المتداولة (العملاء والذمم المدينة)", FinancialAccountType.CHART_ACCOUNT, "1", "DEBIT", "BALANCE_SHEET"),
        ParentAccount("105", "105 - مخزون البضائع المتاحة للبيع", FinancialAccountType.CHART_ACCOUNT, "1", "DEBIT", "BALANCE_SHEET"),
        ParentAccount("106", "106 - الأصول الثابتة وغير الملموسة", FinancialAccountType.CHART_ACCOUNT, "1", "DEBIT", "BALANCE_SHEET"),

        // 2. الخصوم والالتزامات (Liabilities) - كود المجموعة: 2 - دائن - الميزانية العمومية
        ParentAccount("201", "201 - الخصوم المتداولة والدائنون (الموردون)", FinancialAccountType.LIABILITY, "2", "CREDIT", "BALANCE_SHEET"),
        ParentAccount("202", "202 - المستحقات والتزامات الموظفين والرواتب", FinancialAccountType.LIABILITY, "2", "CREDIT", "BALANCE_SHEET"),

        // 3. حقوق الملكية (Equity) - كود المجموعة: 3 - دائن - الميزانية العمومية
        ParentAccount("301", "301 - رأس المال الافتتاحي والحصص", FinancialAccountType.CHART_ACCOUNT, "3", "CREDIT", "BALANCE_SHEET"),
        ParentAccount("302", "302 - الأرباح والخسائر المدورة والمبقاة", FinancialAccountType.CHART_ACCOUNT, "3", "CREDIT", "BALANCE_SHEET"),

        // 4. الإيرادات والمبيعات (Revenues) - كود المجموعة: 4 - دائن - قائمة الدخل (الأرباح والخسائر)
        ParentAccount("401", "401 - إيرادات مبيعات البضائع والخدمات", FinancialAccountType.CHART_ACCOUNT, "4", "CREDIT", "PROFIT_LOSS"),
        ParentAccount("402", "402 - الإيرادات والأرباح المتنوعة والأخرى", FinancialAccountType.CHART_ACCOUNT, "4", "CREDIT", "PROFIT_LOSS"),

        // 5. المصروفات والتكاليف (Expenses) - كود المجموعة: 5 - مدين - قائمة الدخل (الأرباح والخسائر)
        ParentAccount("501", "501 - تكلفة البضاعة المباعة (المشتريات)", FinancialAccountType.EXPENSE, "5", "DEBIT", "PROFIT_LOSS"),
        ParentAccount("502", "502 - المصروفات والنثريات التشغيلية والإدارية", FinancialAccountType.EXPENSE, "5", "DEBIT", "PROFIT_LOSS"),
        ParentAccount("503", "503 - مصروفات الرواتب والأجور والمنافع", FinancialAccountType.EXPENSE, "5", "DEBIT", "PROFIT_LOSS")
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
    val code: String,                      // كود الحساب (مثل: 10201)
    val name: String,                      // اسم الحساب (مثل: مصرف الراجحي، محفظة STC Pay)
    val accountType: FinancialAccountType, // نوع الحساب المالي
    val parentAccountCode: String,         // كود الحساب الرئيسي الأب
    val parentAccountName: String,         // اسم الحساب الرئيسي الأب
    val isMainAccount: Boolean = false,    // هل الحساب رئيسي تجميعي أم فرعي تنفيذي
    val level: Int = 2,                    // المستوى الهرمي في الشجرة (1، 2، 3...)
    val finalAccountMapping: String = "BALANCE_SHEET", // الحساب الختامي: BALANCE_SHEET (ميزانية عمومية) أو PROFIT_LOSS (أرباح وخسائر)
    val debitCreditNature: String = "DEBIT",           // طبيعة الحساب: DEBIT (مدين) أو CREDIT (دائن)
    val accountNumber: String = "",        // رقم الحساب المصرفي / الآيبان IBAN / رقم المحفظة
    val openingBalance: Double = 0.0,      // الرصيد الافتتاحي
    val currentBalance: Double = 0.0,      // الرصيد الجاري الحالي
    val currency: String = "ر.ي",          // العملة
    val isActive: Boolean = true,          // حالة الحساب: نشط أو معطل
    val isDefault: Boolean = false,        // هل هو الحساب الافتراضي للدفع/التحصيل
    val notes: String = "",                // إعدادات وملاحظات إضافية
    val createdAt: Long = System.currentTimeMillis()
)
