package com.example.dokkani.data.local.entities

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

/**
 * جدول قيود التلف والإهلاك للصنف الفردي (Product Wastage & Loss Entity)
 * يربط بجدول الأصناف (products) لتفادي أخطاء FOREIGN KEY.
 */
@Entity(
    tableName = "product_wastage",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["productId"]),
        Index(value = ["timestamp"])
    ]
)
data class ProductWastageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,                        // معرف قيد التلف (waste_id)
    val productId: Long,                     // معرف الصنف المربوط جدول المنتجات (product_id)
    val quantity: Double,                    // الكمية/الوزن التالف (quantity)
    val unit: String,                        // وحدة الصنف (unit: كيلو، حبة، كرتون...)
    val currency: String = "YER",             // عملة المعاملة (currency)
    val reason: String,                      // سبب التلف (reason: انتهاء صلاحية، سوء تخزين...)
    val totalCost: Double,                   // تكلفة الخسارة المباشرة (total_cost)
    val adminUser: String,                   // معرف أو اسم مدير النظام المسؤول عن الاعتماد (admin_user)
    val timestamp: Long = System.currentTimeMillis(), // تاريخ وساعة القيد (timestamp)
    val notes: String = ""                   // تفاصيل أو ملاحظات إضافية
)

/**
 * علاقة قيد التلف مع بيانات الصنف المرتبط
 */
data class ProductWastageWithProduct(
    @Embedded val wasteRecord: ProductWastageEntity,
    @Relation(
        parentColumn = "productId",
        entityColumn = "id"
    )
    val product: ProductEntity?
)
