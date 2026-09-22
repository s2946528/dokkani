package com.example.dokkani.domain.credit

import com.example.dokkani.data.local.entities.PartyEntity
import com.example.dokkani.util.formatCurrency
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * أنواع حركات كشف الحساب الزمني
 */
enum class StatementEntryType(val labelArabic: String) {
    SALE_INVOICE("فاتورة مبيعات آجل"),
    PAYMENT_VOUCHER("سند قبض وتسديد"),
    SALE_RETURN("مرتجع مبيعات"),
    PURCHASE_INVOICE("فاتورة مشتريات"),
    SUPPLIER_PAYMENT("سند صرف وتدفيع مورد"),
    PURCHASE_RETURN("مرتجع مشتريات"),
    OPENING_BALANCE("رصيد افتتاحي / سابق")
}

/**
 * بند زمني في كشف الحساب
 */
data class StatementItem(
    val id: String,
    val rawId: Long = 0L,
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
        storeName: String,
        currencySymbol: String = "ر.ي",
        showDecimals: Boolean = false
    ): CustomerStatementSummary {
        val rawItems = mutableListOf<RawMovement>()
        val isSupplier = party.type == com.example.dokkani.data.local.entities.PartyType.SUPPLIER
        val partyInvoices = invoices.filter { it.partyId == party.id }
        val partyVouchers = vouchers.filter { it.partyId == party.id }

        partyInvoices.forEach { inv ->
            val isCreditPayment = inv.paymentMethod == com.example.dokkani.data.local.entities.PaymentMethod.CREDIT || inv.remainingAmount > 0.001
            val effectiveCreditAmount = if (inv.remainingAmount > 0.001) inv.remainingAmount else (if (isCreditPayment) inv.total else 0.0)

            when (inv.type) {
                com.example.dokkani.data.local.entities.InvoiceType.SALE -> {
                    if (effectiveCreditAmount > 0.001) {
                        rawItems.add(
                            RawMovement(
                                rawId = inv.id,
                                date = inv.date,
                                type = StatementEntryType.SALE_INVOICE,
                                refNumber = inv.invoiceNumber,
                                description = if (inv.notes.isNotBlank()) inv.notes else "مشتريات على الحساب",
                                debit = effectiveCreditAmount,
                                credit = 0.0,
                                paymentMethodArabic = inv.paymentMethod.labelArabic
                            )
                        )
                    }
                }
                com.example.dokkani.data.local.entities.InvoiceType.SALE_RETURN -> {
                    val returnAmt = if (effectiveCreditAmount > 0.001) effectiveCreditAmount else inv.total
                    rawItems.add(
                        RawMovement(
                            rawId = inv.id,
                            date = inv.date,
                            type = StatementEntryType.SALE_RETURN,
                            refNumber = inv.invoiceNumber,
                            description = if (inv.notes.isNotBlank()) inv.notes else "مرتجع مبيعات",
                            debit = 0.0,
                            credit = returnAmt,
                            paymentMethodArabic = inv.paymentMethod.labelArabic
                        )
                    )
                }
                com.example.dokkani.data.local.entities.InvoiceType.PURCHASE -> {
                    if (effectiveCreditAmount > 0.001) {
                        rawItems.add(
                            RawMovement(
                                rawId = inv.id,
                                date = inv.date,
                                type = StatementEntryType.PURCHASE_INVOICE,
                                refNumber = inv.invoiceNumber,
                                description = if (inv.notes.isNotBlank()) inv.notes else "فاتورة توريد مشتريات",
                                debit = 0.0,
                                credit = effectiveCreditAmount,
                                paymentMethodArabic = inv.paymentMethod.labelArabic
                            )
                        )
                    }
                }
                com.example.dokkani.data.local.entities.InvoiceType.PURCHASE_RETURN -> {
                    val returnAmt = if (effectiveCreditAmount > 0.001) effectiveCreditAmount else inv.total
                    rawItems.add(
                        RawMovement(
                            rawId = inv.id,
                            date = inv.date,
                            type = StatementEntryType.PURCHASE_RETURN,
                            refNumber = inv.invoiceNumber,
                            description = if (inv.notes.isNotBlank()) inv.notes else "مرتجع مشتريات",
                            debit = returnAmt,
                            credit = 0.0,
                            paymentMethodArabic = inv.paymentMethod.labelArabic
                        )
                    )
                }
            }
        }

        partyVouchers.forEach { vch ->
            if (!isSupplier) {
                rawItems.add(
                    RawMovement(
                        rawId = vch.id,
                        date = vch.date,
                        type = StatementEntryType.PAYMENT_VOUCHER,
                        refNumber = vch.voucherNumber,
                        description = if (vch.notes.isNotBlank()) vch.notes else "سداد دفعة نقدية - سند قبض",
                        debit = 0.0,
                        credit = vch.amount,
                        paymentMethodArabic = vch.paymentMethod.labelArabic
                    )
                )
            } else {
                rawItems.add(
                    RawMovement(
                        rawId = vch.id,
                        date = vch.date,
                        type = StatementEntryType.SUPPLIER_PAYMENT,
                        refNumber = vch.voucherNumber,
                        description = if (vch.notes.isNotBlank()) vch.notes else "سداد دفعة للمورد - سند صرف",
                        debit = vch.amount,
                        credit = 0.0,
                        paymentMethodArabic = vch.paymentMethod.labelArabic
                    )
                )
            }
        }

        // 1. حساب الحركة الصافية من الفواتير والسندات المسجلة
        val recordedNet = if (!isSupplier) {
            rawItems.sumOf { it.debit - it.credit }
        } else {
            rawItems.sumOf { it.credit - it.debit }
        }

        // 2. الفرق بين الرصيد الفعلي المسجل للطرف وصافي الحركات الفعالية
        val openingBalanceAmount = if (!isSupplier) {
            party.currentBalance - recordedNet
        } else {
            party.currentBalance + recordedNet
        }

        // 3. إدراج بند رصيد افتتاحي إذا كان هناك فارق عن الحركات
        if (kotlin.math.abs(openingBalanceAmount) > 0.001) {
            val earliestDate = rawItems.minOfOrNull { it.date } ?: System.currentTimeMillis()
            val openingDate = earliestDate - 1000L

            if (!isSupplier) {
                rawItems.add(
                    RawMovement(
                        rawId = 0L,
                        date = openingDate,
                        type = StatementEntryType.OPENING_BALANCE,
                        refNumber = "OPENING",
                        description = if (party.notes.isNotBlank()) party.notes else "رصيد افتتاحي / سابق مرحل",
                        debit = if (openingBalanceAmount > 0) openingBalanceAmount else 0.0,
                        credit = if (openingBalanceAmount < 0) kotlin.math.abs(openingBalanceAmount) else 0.0,
                        paymentMethodArabic = "رصيد مرحل"
                    )
                )
            } else {
                val isSupplierLiability = openingBalanceAmount < 0
                val absAmount = kotlin.math.abs(openingBalanceAmount)
                rawItems.add(
                    RawMovement(
                        rawId = 0L,
                        date = openingDate,
                        type = StatementEntryType.OPENING_BALANCE,
                        refNumber = "OPENING",
                        description = if (party.notes.isNotBlank()) party.notes else "رصيد افتتاحي / سابق للمورد",
                        debit = if (!isSupplierLiability) absAmount else 0.0,
                        credit = if (isSupplierLiability) absAmount else 0.0,
                        paymentMethodArabic = "رصيد مرحل"
                    )
                )
            }
        }

        // ترتيب الحركات تصاعدياً حسب التاريخ ثم المعرف لاحتساب الرصيد التراكمي بدقة
        val sortedMovements = rawItems.sortedWith(compareBy({ it.date }, { it.rawId }))

        var cumulativeBalance = 0.0
        val timelineItems = mutableListOf<StatementItem>()

        for (m in sortedMovements) {
            if (!isSupplier) {
                cumulativeBalance += (m.debit - m.credit)
            } else {
                cumulativeBalance += (m.credit - m.debit)
            }
            timelineItems.add(
                StatementItem(
                    id = "${m.refNumber}_${m.date}_${m.rawId}",
                    rawId = m.rawId,
                    date = m.date,
                    dateFormatted = dateFormat.format(Date(m.date)),
                    type = m.type,
                    refNumber = m.refNumber,
                    description = m.description,
                    debit = m.debit,
                    credit = m.credit,
                    runningBalance = if (!isSupplier) cumulativeBalance else -cumulativeBalance,
                    paymentMethodArabic = m.paymentMethodArabic
                )
            )
        }

        // عرض السجل الزمني تنازلياً للمستخدم (الأحدث أولاً)
        val finalTimeline = timelineItems.reversed()

        val nonOpeningMovements = rawItems.filter { it.type != StatementEntryType.OPENING_BALANCE }
        val totalPurchases = if (!isSupplier) nonOpeningMovements.sumOf { it.debit } else nonOpeningMovements.sumOf { it.credit }
        val totalPaid = if (!isSupplier) nonOpeningMovements.sumOf { it.credit } else nonOpeningMovements.sumOf { it.debit }
        val currentBal = party.currentBalance
        val isOver = party.creditLimit > 0 && currentBal > party.creditLimit
        val lastDate = sortedMovements.lastOrNull { it.type != StatementEntryType.OPENING_BALANCE }?.date

        val whatsAppText = if (!isSupplier) {
            generateWhatsAppReminderMessage(
                customerName = party.name,
                balance = currentBal,
                storeName = storeName,
                currencySymbol = currencySymbol,
                showDecimals = showDecimals
            )
        } else {
            generateSupplierWhatsAppMessage(
                supplierName = party.name,
                balance = currentBal,
                storeName = storeName,
                currencySymbol = currencySymbol,
                showDecimals = showDecimals
            )
        }

        return CustomerStatementSummary(
            party = party,
            totalInvoicesCount = partyInvoices.size,
            totalPurchasesOnCredit = totalPurchases,
            totalPayments = totalPaid,
            currentBalance = currentBal,
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
        storeName: String,
        currencySymbol: String = "ر.ي",
        showDecimals: Boolean = false
    ): String {
        val cleanStoreName = storeName.ifBlank { "دكاني" }
        val formattedBalance = balance.formatCurrency(showDecimals, currencySymbol)
        return """
            السلام عليكم ورحمة الله وبركاته، الأخ الكريم $customerName..
            
            نود تذكيركم بلطف بأن رصيد حسابكم الحالي في دفتر الديون لدى ($cleanStoreName) هو:
            💰 *$formattedBalance*
            
            شاكرين ومقدرين لكم حسن التعامل والتفضل بالسداد في أقرب فرصة.
            دمتم بخير وعافية 🌹
        """.trimIndent()
    }

    fun generateSupplierWhatsAppMessage(
        supplierName: String,
        balance: Double,
        storeName: String,
        currencySymbol: String = "ر.ي",
        showDecimals: Boolean = false
    ): String {
        val absBal = kotlin.math.abs(balance)
        val cleanStoreName = storeName.ifBlank { "دكاني" }
        val formattedBalance = absBal.formatCurrency(showDecimals, currencySymbol)
        return """
            السلام عليكم ورحمة الله وبركاته، المحترمون في $supplierName..
            
            تحية طيبة وبعد من ($cleanStoreName)،
            نود الإفادة والتواصل بشأن مطابقة وتصفية الحساب القائم بيننا، والرصيد المسجل هو:
            📊 *$formattedBalance*
            
            نرجو التكرم بالتنسيق معنا لتأكيد الكشف وسداد المستحقات.
            شاكرين ومقدرين تعاملكم الراقي 🌸
        """.trimIndent()
    }

    private data class RawMovement(
        val rawId: Long = 0L,
        val date: Long,
        val type: StatementEntryType,
        val refNumber: String,
        val description: String,
        val debit: Double,
        val credit: Double,
        val paymentMethodArabic: String
    )
}
