package com.example.dokkani.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * نوع حركة حقوق الملكية والمالك
 */
enum class OwnerTransactionType(val labelArabic: String) {
    CASH_DRAWING("مسحوبات نقدية للمالك"),
    GOODS_DRAWING("مسحوبات بضاعة بسعر التكلفة"),
    CAPITAL_DEPOSIT("إيداع إضافي لرأس المال")
}

/**
 * كيان حركات حقوق الملكية والمسحوبات الشخصية (Owner Equity & Drawings)
 * يسجل مسحوبات المالك الشخصية (نقداً أو بضاعة بالتكلفة) وإيداعات زيادة رأس المال
 */
@Entity(
    tableName = "owner_transactions",
    indices = [
        Index(value = ["transactionNumber"], unique = true),
        Index(value = ["type"]),
        Index(value = ["date"])
    ]
)
data class OwnerTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val transactionNumber: String,               // رقم الحركة (مثل: EQ-2026-0001)
    val type: OwnerTransactionType,              // نوع الحركة: مسحوبات نقدية / بضاعة / إيداع رأس مال
    val amount: Double,                          // المبلغ الإجمالي أو تكلفة البضاعة المسحوبة
    val productId: Long? = null,                 // معرف المنتج إذا كانت مسحوبات بضاعة
    val quantity: Double = 0.0,                  // الكمية المسحوبة من البضاعة
    val unitCost: Double = 0.0,                  // سعر تكلفة الوحدة عند السحب
    val paymentMethod: PaymentMethod = PaymentMethod.CASH, // نقداً / تحويل بنكي
    val date: Long = System.currentTimeMillis(), // تاريخ ووقت الحركة
    val details: String = "",                    // البيان والملاحظات التفصيلية
    val recordedBy: String = "المدير العام"       // من قام بتسجيل الحركة
)
