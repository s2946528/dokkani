package com.example.dokkani.data.local.entities

import androidx.room.Embedded
import androidx.room.Relation

/**
 * صنف مدمج مع وحداته المتعددة (علاقة 1 إلى متعدد)
 */
data class ProductWithUnits(
    @Embedded val product: ProductEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "productId"
    )
    val units: List<ProductUnitEntity>
)

/**
 * دفعة خضار مشكل مع تفاصيل أصناف الفرز المحسوبة
 */
data class BatchWithYields(
    @Embedded val batch: MixedProduceBatchEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "batchId"
    )
    val yieldItems: List<MixedProduceYieldItemEntity>
)

/**
 * بند فاتورة مع بيانات الصنف والوحدة
 */
data class InvoiceItemWithDetails(
    @Embedded val item: InvoiceItemEntity,
    @Relation(
        parentColumn = "productId",
        entityColumn = "id"
    )
    val product: ProductEntity?,
    @Relation(
        parentColumn = "productUnitId",
        entityColumn = "id"
    )
    val unit: ProductUnitEntity?
)

/**
 * فاتورة مع بنودها والطرف (العميل/المورد) والعملة
 */
data class InvoiceWithDetails(
    @Embedded val invoice: InvoiceEntity,
    @Relation(
        parentColumn = "partyId",
        entityColumn = "id"
    )
    val party: PartyEntity?,
    @Relation(
        parentColumn = "currencyId",
        entityColumn = "id"
    )
    val currency: CurrencyEntity?,
    @Relation(
        parentColumn = "id",
        entityColumn = "invoiceId"
    )
    val items: List<InvoiceItemEntity>
)
