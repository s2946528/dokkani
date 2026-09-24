package com.example.dokkani.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Embedded
import androidx.room.Relation

/**
 * كيان المجموعات المخزنية للبيع بالقيمة (Stock Groups Entity)
 * يمثل مجموعات الأصناف المشكلة مثل (الخضار المشكل، المكسرات المشكلة، الأجبان)
 */
@Entity(
    tableName = "stock_groups",
    indices = [
        Index(value = ["code"], unique = true),
        Index(value = ["name"])
    ]
)
data class StockGroupEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,                            // اسم المجموعة (مثل: خضار مشكل، مكسرات مشكلة)
    val code: String,                            // كود المجموعة (مثل: GRP-VEG-01)
    val category: String = "خضار وفواكه",        // التصنيف
    val costMethod: CostValuationMethod = CostValuationMethod.WAC, // طريقة تقييم التكلفة (WAC, FIFO, LAST_PRICE)
    val description: String = "",                // وصف وتفاصيل المجموعة
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)

/**
 * جدول الأصناف التابعة للمجموعة المخزنية (Stock Group Items)
 */
@Entity(
    tableName = "stock_group_items",
    foreignKeys = [
        ForeignKey(
            entity = StockGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["groupId"]),
        Index(value = ["productId"])
    ]
)
data class StockGroupItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val groupId: Long,                     // معرّف المجموعة المخزنية
    val productId: Long? = null,           // معرّف الصنف المربوط (إن وجد)
    val productName: String,               // اسم الصنف التابع للمجموعة
    val defaultRatio: Double = 1.0,        // معامل القيمة / الوزن التقديري
    val notes: String = ""
)

/**
 * جدول الجرد الدوري السريع للمجموعة وحساب COGS (Stock Group Audit)
 */
@Entity(
    tableName = "stock_group_audits",
    foreignKeys = [
        ForeignKey(
            entity = StockGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["groupId"]),
        Index(value = ["auditDate"])
    ]
)
data class StockGroupAuditEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val groupId: Long,                            // معرّف المجموعة المخزنية
    val auditDate: Long = System.currentTimeMillis(), // تاريخ الجرد
    val beginningQtyKg: Double,                   // بضاعة أول المدة (كجم)
    val beginningCost: Double,                    // تكلفة بضاعة أول المدة
    val newPurchasesQtyKg: Double,                // المشتريات الجديدة (كجم)
    val newPurchasesCost: Double,                 // تكلفة المشتريات الجديدة
    val endingActualQtyKg: Double,                // بضاعة آخر المدة بالجرد الفعلي (كجم)
    val wasteQtyKg: Double,                       // التالف والشيء الهالك (كجم)
    val cogsQtyKg: Double,                        // كمية البضاعة المباعة مبنية على المعادلة (كجم)
    val cogsCost: Double,                         // تكلفة البضاعة المباعة (COGS)
    val totalSalesRevenue: Double,                // إجمالي المبيعات بالقيمة المسجلة بالفواتير
    val netProfit: Double,                        // صافي الربح = المبيعات - COGS
    val costValuationMethod: CostValuationMethod = CostValuationMethod.WAC, // المحرك المستخدم
    val notes: String = ""
)

/**
 * علاقة المجموعة مع تفاصيل أصنافها وسجل جردها
 */
data class StockGroupWithDetails(
    @Embedded val group: StockGroupEntity,
    @Relation(parentColumn = "id", entityColumn = "groupId")
    val items: List<StockGroupItemEntity> = emptyList(),
    @Relation(parentColumn = "id", entityColumn = "groupId")
    val audits: List<StockGroupAuditEntity> = emptyList()
)
