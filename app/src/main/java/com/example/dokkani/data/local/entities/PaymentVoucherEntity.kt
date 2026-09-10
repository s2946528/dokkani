package com.example.dokkani.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * جدول سندات القبض والدفع (Payment Vouchers)
 * يسجل عمليات سداد الديون والمقبوضات من عملاء الدفتر (الشكك)
 * أو الدفعات المسددة للموردين
 */
@Entity(
    tableName = "payment_vouchers",
    foreignKeys = [
        ForeignKey(
            entity = PartyEntity::class,
            parentColumns = ["id"],
            childColumns = ["partyId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["voucherNumber"], unique = true),
        Index(value = ["partyId"]),
        Index(value = ["date"])
    ]
)
data class PaymentVoucherEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val voucherNumber: String,                  // رقم السند التسلسلي (مثل: RCV-2026-0001)
    val partyId: Long,                          // العميل أو المورد
    val amount: Double,                         // المبلغ المسدد
    val paymentMethod: PaymentMethod = PaymentMethod.CASH, // طريقة السداد: نقداً، شبكة، تحويل بنكي
    val date: Long = System.currentTimeMillis(),// تاريخ ووقت السداد
    val receivedBy: String = "كاشير 1",         // المستلم / الكاشير
    val notes: String = ""                      // ملاحظات وبيان السند
)
