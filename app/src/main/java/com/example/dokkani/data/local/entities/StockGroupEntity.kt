package com.example.dokkani.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

/**
 * كيان مجموعات الأصناف المخزنية المتشابهة (Stock Group Entity)
 * تستخدم لمجموعات الخضار/الفواكه والأصناف المباعة بالقيمة أو الوزن.
 */
@Entity(
    tableName = "stock_groups",
    indices = [
        Index(value = ["code"], unique = true),
        Index(value = ["cost_center_id"]),
        Index(value = ["isActive"])
    ]
)
data class StockGroupEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,                                   // اسم المجموعة (مثل: مجموعة البطاطس)
    val code: String = "",                              // رمز/كود المجموعة
    val description: String = "",                      // الوصف
    @ColumnInfo(name = "cost_center_id")
    val costCenterId: Long = 1,                         // مركز التكلفة المرتبط (1: العام افتراضياً)
    val isActive: Boolean = true,                       // حالة المجموعة
    val createdAt: Long = System.currentTimeMillis()    // تاريخ الإنشاء
)

/**
 * الأصناف الفرعية التابعة للمجموعة المخزنية (Stock Group Items)
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
    val groupId: Long,                                  // معرف المجموعة الرئيسية
    val productId: Long? = null,                        // معرف الصنف المرتبط (إن وجد)
    val productName: String,                            // اسم الصنف الفرعي
    val unitCost: Double = 0.0,                         // سعر التكلفة للكيلو/الوحدة
    val unitSellingPrice: Double = 0.0,                 // سعر البيع المقترح
    val notes: String = ""                              // ملاحظات
)

/**
 * سجّلات الجرد الدوري للمجموعة (Stock Group Audit Logs)
 * يحسب معادلات محرك التكلفة وـ COGS وصافي الربح بدقة.
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
    val groupId: Long,                                  // معرف المجموعة
    val auditDate: Long = System.currentTimeMillis(),   // تاريخ وساعة الجرد
    val conductedBy: String = "المدير",                  // اسم الجارد / الكاشير
    val beginningStockQty: Double = 0.0,                // مخزون أول المدة (كمية/وزن)
    val purchasesQty: Double = 0.0,                     // إجمالي المشتريات والوارد
    val actualEndingQty: Double = 0.0,                  // إجمالي الجرد الفعلي (آخر المدة)
    val totalCogsQty: Double = 0.0,                     // كمية المباع (COGS) = (أول + مشتريات) - الفعلي
    val totalValueSalesAmount: Double = 0.0,            // إجمالي المبيعات بالقيمة للمجموعة
    val approxPricePerKg: Double = 0.0,                 // سعر بيع الكيلو التقريبي = المبيعات بالقيمة / كمية المباع
    val totalCogsCost: Double = 0.0,                    // إجمالي تكلفة البضاعة المباعة (مجموع تكاليف الأصناف الخارجة)
    val netProfit: Double = 0.0,                        // صافي الربح الحقيقي = المبيعات بالقيمة - إجمالي التكلفة
    val status: String = "COMPLETED",                   // حالة الجرد
    val notes: String = ""                              // ملاحظات
)

/**
 * تفاصيل المجموعة كاملة مع عناصرها وجروداتها
 */
data class StockGroupWithDetails(
    @Embedded val group: StockGroupEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "groupId"
    )
    val items: List<StockGroupItemEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "groupId"
    )
    val audits: List<StockGroupAuditEntity>
)
