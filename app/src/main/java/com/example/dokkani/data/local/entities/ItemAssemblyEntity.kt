package com.example.dokkani.data.local.entities

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

enum class AssemblyStatus(val labelArabic: String) {
    DRAFT("مسودة"),
    APPROVED("معتمد ومُجمّع"),
    CANCELLED("ملغى")
}

/**
 * كيان العملية الرئيسية لتركيب وتجميع الأصناف (Bill of Materials & Item Assembly Master)
 */
@Entity(
    tableName = "item_assemblies",
    indices = [Index("finishedProductId")]
)
data class ItemAssemblyEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val assemblyNumber: String,                     // رقم سند التجميع (مثال: ASM-2026-0001)
    val finishedProductId: Long,                    // كود الصنف النهائي المركب
    val finishedProductName: String,                // اسم الصنف النهائي (مثل: وجبة عائلية / صندوق تشكيلة)
    val finishedProductCode: String,                // كود الصنف النهائي
    val producedQuantity: Double = 1.0,             // الكمية المنتجة
    val totalAssemblyCost: Double = 0.0,            // إجمالي تكلفة المكونات الداخلة
    val costPerUnit: Double = 0.0,                  // التكلفة الفردية للوحدة المجمعة
    val sellingPricePerUnit: Double = 0.0,          // سعر بيع الوحدة المجمعة
    val status: AssemblyStatus = AssemblyStatus.APPROVED,
    val notes: String = "",
    val date: Long = System.currentTimeMillis(),
    val costCenterId: Long? = null
)

/**
 * كيان تفاصيل مكونات وأصناف خامات التركيب (Assembly Component Detail)
 */
@Entity(
    tableName = "item_assembly_components",
    foreignKeys = [
        ForeignKey(
            entity = ItemAssemblyEntity::class,
            parentColumns = ["id"],
            childColumns = ["assemblyId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("assemblyId"), Index("componentProductId")]
)
data class ItemAssemblyComponentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val assemblyId: Long,                           // معرف العملية الرئيسية
    val componentProductId: Long,                   // كود المادة الخام أو الصنف الفرعي
    val componentProductName: String,               // اسم المادة الخام
    val componentProductCode: String,               // كود المادة الخام
    val unitName: String = "حبة",                    // وحدة القياس
    val quantityPerAssembly: Double = 1.0,          // المعيار المطلوب للقطعة الواحدة
    val totalQuantityUsed: Double = 1.0,            // إجمالي الكمية المستهلكة (الكمية الفردية * الكمية المنتجة)
    val unitCostPrice: Double = 0.0,                // تكلفة شراء الوحدة الفرعية
    val totalCostPrice: Double = 0.0                // إجمالي تكلفة البند في التركيب
)

/**
 * العلاقة الشاملة بين الهيدر الرئيسي ومكوناته
 */
data class ItemAssemblyWithComponents(
    @Embedded val assembly: ItemAssemblyEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "assemblyId"
    )
    val components: List<ItemAssemblyComponentEntity>
)
