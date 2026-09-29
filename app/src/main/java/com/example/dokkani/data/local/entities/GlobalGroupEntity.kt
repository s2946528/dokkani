package com.example.dokkani.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * جدول المجموعات العام والحيادي لجميع الكيانات في نظام دكاني (Global Groups Table)
 * يدعم إضافة مجموعات وتصنيفات ديناميكية لأي كيان في النظام (أصناف، عملاء، موردين، مصروفات، حسابات، إلخ)
 */
@Entity(tableName = "GlobalGroups")
data class GlobalGroupEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "entity_type")
    val entityType: String, // نوع الكيان (مثل: PRODUCT, PARTY, EXPENSE, CASH_SHIFT, FINANCIAL_ACCOUNT, EMPLOYEE, FIXED_ASSET, COST_CENTER, etc.)

    @ColumnInfo(name = "name")
    val name: String, // اسم المجموعة

    @ColumnInfo(name = "code")
    val code: String = "", // كود / رمز المجموعة

    @ColumnInfo(name = "parent_id")
    val parentId: Long? = null, // المعرف الأب للمجموعات الفرعية الهرمية

    @ColumnInfo(name = "description")
    val description: String = "", // وصف المجموعة

    @ColumnInfo(name = "color_hex")
    val colorHex: String = "#1976D2", // لون المميز للمجموعة

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
