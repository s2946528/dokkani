package com.example.dokkani.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * جدول الفواتير (Invoices)
 * يسجل فواتير المبيعات، المشتريات، ومرتجعات البيع والشراء.
 */
@Entity(
    tableName = "invoices",
    foreignKeys = [
        ForeignKey(
            entity = PartyEntity::class,
            parentColumns = ["id"],
            childColumns = ["partyId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = CurrencyEntity::class,
            parentColumns = ["id"],
            childColumns = ["currencyId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["invoiceNumber"], unique = true),
        Index(value = ["type"]),
        Index(value = ["partyId"]),
        Index(value = ["currencyId"]),
        Index(value = ["date"])
    ]
)
data class InvoiceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val invoiceNumber: String,            // رقم الفاتورة التسلسلي (مثل: INV-2026-0001 أو PUR-0001)
    val type: InvoiceType,                // نوع الفاتورة: بيع، شراء، مرتجع بيع، مرتجع شراء
    val partyId: Long? = null,            // معرّف العميل أو المورد (null للزبون النقدي المباشر)
    val date: Long = System.currentTimeMillis(), // تاريخ ووقت الفاتورة
    val currencyId: Long,                 // معرّف العملة المستخدمة في الفاتورة
    val exchangeRate: Double = 1.0,       // سعر الصرف للعملة مقابل الأساس وقت الفاتورة
    val subtotal: Double,                 // الإجمالي قبل الخصم والضريبة
    val discount: Double = 0.0,           // مبلغ الخصم
    val taxRate: Double = 0.15,           // نسبة الضريبة (مثال: 0.15 = 15%)
    val taxAmount: Double = 0.0,          // قيمة الضريبة المضافة
    val total: Double,                    // الإجمالي النهائي الصافي للفاتورة
    val paidAmount: Double,               // المبلغ المدفوع
    val remainingAmount: Double = 0.0,    // المبلغ المتبقي (آجل/دين)
    val paymentMethod: PaymentMethod = PaymentMethod.CASH, // طريقة السداد (نقداً، شبكة، آجل)
    val status: InvoiceStatus = InvoiceStatus.COMPLETED,   // حالة الفاتورة
    val notes: String = ""                // ملاحظات
)

/**
 * جدول بنود الفواتير (Invoice_Items)
 * يسجل تفاصيل الأصناف والكميات والوحدات والأسعار والتكلفة اللحظية لكل بند.
 */
@Entity(
    tableName = "invoice_items",
    foreignKeys = [
        ForeignKey(
            entity = InvoiceEntity::class,
            parentColumns = ["id"],
            childColumns = ["invoiceId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = ProductUnitEntity::class,
            parentColumns = ["id"],
            childColumns = ["productUnitId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["invoiceId"]),
        Index(value = ["productId"]),
        Index(value = ["productUnitId"])
    ]
)
data class InvoiceItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val invoiceId: Long,                  // معرّف الفاتورة التابع لها
    val productId: Long,                  // معرّف الصنف
    val productUnitId: Long,              // معرّف الوحدة المستخدمة (حبة، كرتون، كيلو...)
    val quantity: Double,                 // الكمية المباعة/المشتراة
    val unitConversionFactor: Double,     // معامل تحويل الوحدة وقت العملية
    val unitCostPrice: Double,            // سعر التكلفة للوحدة وقت العملية (لاحتساب الأرباح)
    val unitSellingPrice: Double,         // سعر البيع/الشراء للوحدة
    val discount: Double = 0.0,           // خصم البند
    val taxRate: Double = 0.0,            // ضريبة البند
    val totalPrice: Double                // إجمالي البند = (الكمية * سعر الوحدة) - الخصم
)
