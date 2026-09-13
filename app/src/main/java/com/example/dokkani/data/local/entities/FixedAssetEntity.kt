package com.example.dokkani.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * كيان الأصول الثابتة للبقالة ومتجر التجزئة (Fixed Assets)
 * تسجيل وتتبع الثلاجات، الأرفف، الموازين، أجهزة الكمبيوتر، سيارات التوصيل، والمصاعد
 */
@Entity(
    tableName = "fixed_assets",
    indices = [
        Index(value = ["assetCode"], unique = true),
        Index(value = ["category"]),
        Index(value = ["purchaseDate"])
    ]
)
data class FixedAssetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val assetCode: String,                       // كود الأصل (مثل: AST-001)
    val name: String,                            // اسم الأصل (مثال: ثلاجة ألبان 3 أبواب)
    val category: String,                        // التصنيف: ثلاجات وتبريد، أرفف وتجهيزات، أجهزة وموازين، وسائل نقل، تكييف وإضاءة
    val purchaseCost: Double,                    // تكلفة الشراء / القيمة الافتتاحية
    val currentValue: Double = purchaseCost,     // القيمة الدفترية الحالية
    val purchaseDate: Long = System.currentTimeMillis(), // تاريخ الشراء / الإضافة
    val supplierName: String = "",               // المورد أو المصدر
    val paymentMethod: PaymentMethod = PaymentMethod.CASH, // طريقة الشراء (نقداً، شبكة، تحويل)
    val status: String = "ACTIVE",               // حالة الأصل: ACTIVE (نشط وموجود)، DISPOSED (مستبعد/مباع)
    val notes: String = ""                       // ملاحظات وبيانات الضمان
)
