package com.example.dokkani.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * جدول سندات القبض والصرف (Payment & Receipt Vouchers Table)
 */
@Entity(
    tableName = "payment_vouchers",
    foreignKeys = [
        ForeignKey(
            entity = PartyEntity::class,
            parentColumns = ["id"],
            childColumns = ["partyId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["voucherNumber"], unique = true),
        Index(value = ["partyId"]),
        Index(value = ["voucherType"]),
        Index(value = ["date"]),
        Index(value = ["cost_center_id"])
    ]
)
data class PaymentVoucherEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val voucherNumber: String,                            // رقم السند
    val partyId: Long? = null,                            // معرف العميل أو المورد
    val amount: Double,                                   // مبلغ السند
    val voucherType: VoucherType,                          // نوع السند (سند قبض RECEIPT أو سند صرف PAYMENT)
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,// طريقة السداد (كاش، شبكة، محفظة...)
    val transactionRef: String = "",                       // رقم المرجع / رقم الحوالة / رقم الإشعار
    val receiptImagePath: String? = null,                 // صورة الإشعار
    val date: Long = System.currentTimeMillis(),         // تاريخ وساعة السند
    val receivedBy: String = "كاشير 1",                    // اسم مستلم/صارف السند
    val notes: String = "",                               // ملاحظات وتفاصيل
    @ColumnInfo(name = "cost_center_id")
    val costCenterId: Long = 1                            // معرف مركز التكلفة
) {
    val isPayment: Boolean
        get() = voucherType == VoucherType.PAYMENT
}
