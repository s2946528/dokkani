package com.example.dokkani.data.local.entities

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
        Index(value = ["category"])
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
    val isActive: Boolean = true,         // حالة نشاط الصنف
    val createdAt: Long = System.currentTimeMillis()
)
