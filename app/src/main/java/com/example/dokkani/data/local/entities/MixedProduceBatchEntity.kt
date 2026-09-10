package com.example.dokkani.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * جدول دفعات وسحاحير الخضار المشكل (Mixed_Produce_Batches)
 * نموذج محاسبي وجرد سريع مخصص للبقالات ومتاجر الخضار:
 * 1- احتساب الوزن القائم والتكلفة الإجمالية (شراء + نقل ومصاريف)
 * 2- عزل وتسجيل الوزن التالف/الهدر (Waste / Spoilage) وحساب نسبته
 * 3- إعادة احتساب التكلفة الفعلية للكيلو الصافي الصالح للبيع (Effective Net Cost)
 * 4- اقتراح سعر البيع بهامش ربحي مستهدف
 */
@Entity(
    tableName = "mixed_produce_batches",
    foreignKeys = [
        ForeignKey(
            entity = PartyEntity::class,
            parentColumns = ["id"],
            childColumns = ["supplierId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["batchNumber"], unique = true),
        Index(value = ["supplierId"]),
        Index(value = ["date"])
    ]
)
data class MixedProduceBatchEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val batchNumber: String,                     // كود الدفعة (مثل: MIX-2026-001)
    val date: Long = System.currentTimeMillis(), // تاريخ الشراء أو الجرد
    val supplierId: Long? = null,                // مورد الخضار أو حلقة الخضار
    val sourceDescription: String,               // وصف السحارة (مثال: سحارة خضار مشكل طماطم وخيار وباذنجان)
    val totalGrossWeightKg: Double,              // إجمالي الوزن القائم عند الاستلام (كجم)
    val totalPurchaseCost: Double,               // إجمالي تكلفة الشراء المقطوعة (مثلاً 100 ريال)
    val additionalExpense: Double = 0.0,         // مصاريف نقل/تحميل/عتالة
    val wasteWeightKg: Double = 0.0,             // وزن التالف/الهدر بعد الفحص والفرز (كجم)
    val netSalableWeightKg: Double,              // الوزن الصافي القابل للبيع = (الوزن القائم - وزن التالف)
    val wastePercentage: Double,                 // نسبة الهدر المئوية = (التالف / القائم) * 100
    val effectiveCostPerKg: Double,              // التكلفة الفعلية المحاسبية للكيلو الصافي = (إجمالي التكاليف / الوزن الصافي)
    val targetProfitMarginPercent: Double = 25.0,// هامش الربح المستهدف (مثلاً 25%)
    val suggestedSalePricePerKg: Double,         // سعر البيع المقترح للكيلو = التكلفة الفعلية * (1 + الهامش)
    val status: BatchStatus = BatchStatus.DRAFT, // حالة الدفعة (مسودة / تم الفرز / تم التوريد للمخزن)
    val notes: String = ""                       // ملاحظات المحاسب أو البقال
)

/**
 * جدول تفاصيل فرز الأصناف الناتجة من السحارة المشكلة (Mixed_Produce_Yield_Items)
 * عندما يقوم البقال بفرز السحارة المشكلة إلى أصناف منفصلة لرفوف العرض:
 * يتيح توزيع التكلفة الإجمالية بناءً على أوزان الأصناف المفروزة ومعامل القيمة النسبية لكل صنف.
 */
@Entity(
    tableName = "mixed_produce_yield_items",
    foreignKeys = [
        ForeignKey(
            entity = MixedProduceBatchEntity::class,
            parentColumns = ["id"],
            childColumns = ["batchId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["batchId"]),
        Index(value = ["productId"])
    ]
)
data class MixedProduceYieldItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val batchId: Long,                    // معرّف الدفعة المشكلة التابعة لها
    val productId: Long,                  // معرّف الصنف المفروز في جدول الأصناف
    val productName: String,              // اسم الصنف (طماطم، خيار، فلفل رومي...)
    val sortedWeightKg: Double,           // الوزن المفروز القابل للبيع (كجم)
    val costAllocationRatio: Double = 1.0,// معامل القيمة النسبية لتوزيع التكلفة (1.0 عادي، 1.2 لصنف ممتاز/أغلى)
    val calculatedCostPerKg: Double,      // التكلفة المخصصة للكيلو بعد توزيع تكلفة السحارة
    val targetSellingPricePerKg: Double,  // سعر البيع للكيلو على الرف
    val expectedRevenue: Double           // العائد المتوقع = الوزن المفروز * سعر البيع
)
