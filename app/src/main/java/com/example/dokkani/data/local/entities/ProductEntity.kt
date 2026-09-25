package com.example.dokkani.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * جدول الأصناف (Products)
 * يمثل السلعة الأساسية في البقالة/المتجر (معلبات، ألبان، خضار وفواكه...)
 */
@Entity(
    tableName = "products",
    indices = [
        Index(value = ["code"], unique = true),
        Index(value = ["category"]),
        Index(value = ["expiryDate"]),
        Index(value = ["cost_center_id"])
    ]
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val code: String,                     // كود الصنف أو SKU
    val name: String,                     // اسم الصنف بالعربية (مثل: طماطم محلي، حليب نادك 1 لتر)
    val englishName: String = "",         // الاسم بالإنجليزية
    val category: String,                 // القسم (خضار وفواكه، ألبان، معلبات، مشروبات...)
    val isWeighted: Boolean = false,      // هل يباع بالوزن/الميزان (خضار/فواكه/لحوم) أم بالقطعة
    val minStockAlert: Double = 5.0,      // حد إعادة الطلب للتنبيه بنواقص الرفوف
    val expiryDate: Long? = null,         // تاريخ انتهاء الصلاحية
    val isActive: Boolean = true,         // حالة نشاط الصنف
    val createdAt: Long = System.currentTimeMillis(),
    val imagePath: String? = null,        // مسار أو URI صورة المنتج
    @ColumnInfo(name = "cost_center_id")
    val costCenterId: Long = 1            // معرف مركز التكلفة (1: مركز التكلفة العام افتراضياً)
)
