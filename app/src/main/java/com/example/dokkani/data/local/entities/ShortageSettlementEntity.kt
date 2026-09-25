package com.example.dokkani.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * كيان تسوية العجز المخزني والبيع بالقيمة (Shortage Settlement & Value-Based Sales Entity)
 * يسجل العجز الناتج حصرياً عن اعتماد شاشة الجرد الدوري (الدفتري - الفعلي).
 * يربط بمركز التكلفة ويضمن عزل التلف/الهادر المسجل كمصروف مستقر سابقاً.
 */
@Entity(
    tableName = "shortage_settlements",
    foreignKeys = [
        ForeignKey(
            entity = CostCenterEntity::class,
            parentColumns = ["center_id"],
            childColumns = ["cost_center_id"],
            onDelete = ForeignKey.SET_DEFAULT
        )
    ],
    indices = [
        Index(value = ["cost_center_id"]),
        Index(value = ["auditId"]),
        Index(value = ["status"]),
        Index(value = ["createdAt"])
    ]
)
data class ShortageSettlementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val auditId: Long = 0,                            // معرف عملية الجرد المرتبطة
    val productId: Long? = null,                       // معرف الصنف
    val productName: String,                           // اسم الصنف
    @ColumnInfo(name = "cost_center_id")
    val costCenterId: Long = 1,                        // معرف مركز التكلفة (1: العام افتراضياً)
    val costCenterName: String = "مركز التكلفة العام",   // اسم مركز التكلفة
    val bookQuantity: Double,                          // الكمية الدفتري المتبقية
    val actualQuantity: Double,                        // الكمية الفعلية المتبقية
    val shortageQuantity: Double,                      // كمية العجز (الدفتري - الفعلي)
    val unitCost: Double,                              // تكلفة الشراء للوحدة
    val unitSellingPrice: Double,                      // سعر البيع للوحدة
    val totalShortageCost: Double,                     // إجمالي التكلفة الدفترية للعجز
    val totalValueSalesAmount: Double,                 // إجمالي القيمة البيعية المستحقة (إيراد مبيعات بالقيمة)
    val status: String = STATUS_PENDING,               // حالة التسوية (PENDING / SETTLED)
    val settledAt: Long? = null,                       // تاريخ الإقفال واستلام القيمة
    val settledBy: String? = null,                     // اسم مدير النظام الذي أعد واعتمد التسوية
    val notes: String = "",                            // ملاحظات أو تفاصيل التسوية
    val createdAt: Long = System.currentTimeMillis()   // تاريخ رصد العجز
) {
    companion object {
        const val STATUS_PENDING = "PENDING"           // معلق / غير مسدد
        const val STATUS_SETTLED = "SETTLED"           // مقفلة / مسددة
    }
}
