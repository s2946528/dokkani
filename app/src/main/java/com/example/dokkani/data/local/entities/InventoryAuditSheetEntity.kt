package com.example.dokkani.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * جدول مستندات وسندات الجرد والمطابقة المخزنية والميدانية
 */
@Entity(tableName = "inventory_audit_sheets")
data class InventoryAuditSheetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val voucherNumber: String,
    val date: Long = System.currentTimeMillis(),
    val targetStoreName: String = "المخزن الرئيسي - الفرع 1",
    val costCenterId: Long = 1,
    val costCenterName: String = "مركز التكلفة العام",
    val status: String = STATUS_DRAFT, // "DRAFT" (مسودة) or "POSTED" (معتمد وترحيل)
    val itemsCount: Int = 0,
    val matchingCount: Int = 0,
    val shortageCount: Int = 0,
    val surplusCount: Int = 0,
    val totalShortageSellingValue: Double = 0.0,
    val totalShortageCostValue: Double = 0.0,
    val totalSurplusSellingValue: Double = 0.0,
    val totalSurplusCostValue: Double = 0.0,
    val itemsDataJson: String = "", // بيانات الأصناف بصيغة JSON لسهولة الاسترجاع والطباعة
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val STATUS_DRAFT = "DRAFT"
        const val STATUS_POSTED = "POSTED"
    }

    val isDraft: Boolean get() = status == STATUS_DRAFT
    val isPosted: Boolean get() = status == STATUS_POSTED
}
