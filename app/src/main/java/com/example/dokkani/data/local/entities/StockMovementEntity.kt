package com.example.dokkani.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * جدول حركات المخزون وطبقات الشراء (Stock_Movements / Inventory_Lots)
 * العمود الفقري للمحاسبة وحساب تكلفة البضاعة المباعة (COGS)
 * يدعم بدقة:
 * 1- المتوسط المرجح (WAC): تجميع إجمالي التكلفة / إجمالي الكميات المتوفرة.
 * 2- الوارد أولاً صادر أولاً (FIFO): تتبع رصيد كل طبقة شراء (remainingQuantityForFifo) بترتيب زمني تصاعدي.
 * 3- آخر سعر شراء (Last Purchase Price): استرداد أحدث حركة شراء مسجلة للصنف.
 */
@Entity(
    tableName = "stock_movements",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = InvoiceEntity::class,
            parentColumns = ["id"],
            childColumns = ["invoiceId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["productId"]),
        Index(value = ["invoiceId"]),
        Index(value = ["timestamp"]),
        Index(value = ["movementType"])
    ]
)
data class StockMovementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productId: Long,                  // معرّف الصنف
    val productUnitId: Long? = null,      // معرّف الوحدة المستخدمة في الحركة
    val invoiceId: Long? = null,          // الفاتورة المرتبطة إن وجدت
    val movementType: MovementType,       // نوع الحركة: شراء وارد (+)، بيع صادر (-)، تسوية، فرز خضار
    val quantityBaseUnit: Double,         // الكمية بالوحدة الأساسية (موجبة للوارد، سالبة للصادر)
    val remainingQuantityForFifo: Double, // الكمية المتبقية من هذه الطبقة لاستهلاك FIFO
    val unitCostPriceBase: Double,        // تكلفة الوحدة الأساسية لهذه الطبقة بالعملة الرئيسية
    val timestamp: Long = System.currentTimeMillis(), // وقت وتاريخ الحركة
    val referenceNumber: String? = null,  // رقم السند أو الدفعة المرجعية
    val notes: String = ""                // ملاحظات
)
