package com.example.dokkani.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * جدول الوحدات المتعددة للأصناف (Product_Units)
 * يتيح بيع الصنف بوحدات متعددة (حبة، كرتون، درزن، كيلو، صندوق)
 * مع معامل التحويل إلى الوحدة الأساسية والباركود الخاص بكل وحدة وأسعار الشراء والبيع.
 */
@Entity(
    tableName = "product_units",
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
        Index(value = ["barcode"])
    ]
)
data class ProductUnitEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productId: Long,                  // معرّف الصنف التابع له
    val unitName: String,                 // اسم الوحدة (حبة، كرتون، درزن، كيلو، سحارة...)
    val conversionFactor: Double,         // معامل التحويل للوحدة الأساسية (مثال: الوحدة الأساسية حبة = 1.0، كرتون 24 = 24.0)
    val barcode: String,                  // باركود الوحدة للماسح الضوئي في نقطة البيع
    val costPrice: Double,                // سعر الشراء القياسي لهذه الوحدة
    val sellingPrice: Double,             // سعر البيع للمستهلك
    val isBaseUnit: Boolean = false       // هل هذه هي الوحدة الأساسية للصنف (Factor = 1.0)
)
