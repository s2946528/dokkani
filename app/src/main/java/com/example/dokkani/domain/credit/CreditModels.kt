package com.example.dokkani.domain.credit

import com.example.dokkani.data.local.entities.PartyEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * أنواع حركات كشف الحساب الزمني
 */
enum class StatementEntryType(val labelArabic: String) {
    SALE_INVOICE("فاتورة مبيعات آجل"),
    PAYMENT_VOUCHER("سند قبض وتسديد"),
    SALE_RETURN("مرتجع مبيعات")
}

/**
 * بند زمني في كشف الحساب
 */
data class StatementItem(
    val id: String,
    val date: Long,
    val dateFormatted: String,
    val type: StatementEntryType,
    val refNumber: String,
    val description: String,
    val debit: Double,          // مدين (مشتريات بالآجل تزيد الدين على العميل)
    val credit: Double,         // دائن (سداد أو تسوية تقلل الدين)
    val runningBalance: Double,  // الرصيد بعد هذه الحركة
    val paymentMethodArabic: String
)

/**
 * ملخص كشف حساب العميل والديون المستحقة
 */
data class CustomerStatementSummary(
    val party: PartyEntity,
    val totalInvoicesCount: Int,
    val totalPurchasesOnCredit: Double,
    val totalPayments: Double,
    val currentBalance: Double,
    val creditLimit: Double,
    val isOverCreditLimit: Boolean,
    val lastActivityDate: Long?,
    val timeline: List<StatementItem>,
    val whatsAppReminderText: String
)

/**
 * محرك توليد كشف الحساب ورسائل تذكير الواتساب
 */
object CreditNotebookEngine {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    fun buildCustomerStatement(
        party: PartyEntity,
        invoices: List<com.example.dokkani.data.local.entities.InvoiceEntity>,
        vouchers: List<com.example.dokkani.data.local.entities.PaymentVoucherEntity>,
        storeName: String
    ): CustomerStatementSummary {
        val rawItems = mutableListOf<RawMovement>()

        // 1. فواتير الآجل
        invoices.filter { it.partyId == party.id }.forEach { inv ->
            // إذا كانت الفاتورة تتضمن جزءاً مؤجلاً أو كلها آجل
            val creditAmount = if (inv.remainingAmount > 0.001) inv.remainingAmount else inv.total
            rawItems.add(
                RawMovement(
                    date = inv.date,
                    type = StatementEntryType.SALE_INVOICE,
                    refNumber = inv.invoiceNumber,
                    description = if (inv.notes.isNotBlank()) inv.notes else "مشتريات على الحساب",
                    debit = creditAmount,
                    credit = 0.0,
                    paymentMethodArabic = inv.paymentMethod.labelArabic
                )
            )
        }

        // 2. سندات القبض والتسديدات
        vouchers.filter { it.partyId == party.id }.forEach { vch ->
            rawItems.add(
                RawMovement(
                    date = vch.date,
                    type = StatementEntryType.PAYMENT_VOUCHER,
                    refNumber = vch.voucherNumber,
                    description = if (vch.notes.isNotBlank()) vch.notes else "سداد دفعة نقدية - سند قبض",
                    debit = 0.0,
                    credit = vch.amount,
                    paymentMethodArabic = vch.paymentMethod.labelArabic
                )
            )
        }

        // ترتيب الحركات تصاعدياً حسب التاريخ لاحتساب الرصيد التراكمي
        val sortedMovements = rawItems.sortedBy { it.date }

        var cumulativeBalance = 0.0
        val timelineItems = mutableListOf<StatementItem>()

        for (m in sortedMovements) {
            cumulativeBalance += (m.debit - m.credit)
            timelineItems.add(
                StatementItem(
                    id = "${m.refNumber}_${m.date}",
                    date = m.date,
                    dateFormatted = dateFormat.format(Date(m.date)),
                    type = m.type,
                    refNumber = m.refNumber,
                    description = m.description,
                    debit = m.debit,
                    credit = m.credit,
                    runningBalance = cumulativeBalance,
                    paymentMethodArabic = m.paymentMethodArabic
                )
            )
        }

        // عرض السجل الزمني تنازلياً للمستخدم (الأحدث أولاً)
        val finalTimeline = timelineItems.reversed()

        val totalPurchases = rawItems.sumOf { it.debit }
        val totalPaid = rawItems.sumOf { it.credit }
        val finalBalance = party.currentBalance
        val isOver = party.creditLimit > 0 && finalBalance > party.creditLimit
        val lastDate = sortedMovements.lastOrNull()?.date

        val whatsAppText = generateWhatsAppReminderMessage(
            customerName = party.name,
            balance = finalBalance,
            storeName = storeName
        )

        return CustomerStatementSummary(
            party = party,
            totalInvoicesCount = invoices.count { it.partyId == party.id },
            totalPurchasesOnCredit = totalPurchases,
            totalPayments = totalPaid,
            currentBalance = finalBalance,
            creditLimit = party.creditLimit,
            isOverCreditLimit = isOver,
            lastActivityDate = lastDate,
            timeline = finalTimeline,
            whatsAppReminderText = whatsAppText
        )
    }

    fun generateWhatsAppReminderMessage(
        customerName: String,
        balance: Double,
        storeName: String
    ): String {
        return """
            السلام عليكم ورحمة الله وبركاته، الأخ الكريم $customerName..
            
            نود تذكيركم بلطف بأن رصيد حسابكم الحالي في دفتر الديون لدى ($storeName) هو:
            💰 *${"%.2f".format(balance)} ريال سعودي*
            
            شاكرين ومقدرين لكم حسن التعامل والتفضل بالسداد في أقرب فرصة.
            دمتم بخير وعافية 🌹
        """.trimIndent()
    }

    private data class RawMovement(
        val date: Long,
        val type: StatementEntryType,
        val refNumber: String,
        val description: String,
        val debit: Double,
        val credit: Double,
        val paymentMethodArabic: String
    )
}
