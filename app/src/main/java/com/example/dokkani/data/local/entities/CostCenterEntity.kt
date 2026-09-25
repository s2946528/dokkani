package com.example.dokkani.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * جدول مراكز التكلفة (Cost Centers Table)
 * مراكز التكلفة تبدأ فارغة ويقوم المستخدم بإنشائها يدوياً، باستثناء "مركز التكلفة العام" الافتراضي.
 */
@Entity(
    tableName = "cost_centers",
    indices = [
        Index(value = ["code"], unique = true)
    ]
)
data class CostCenterEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "center_id")
    val centerId: Long = 0,

    @ColumnInfo(name = "center_name")
    val centerName: String,

    @ColumnInfo(name = "code")
    val code: String = "",

    @ColumnInfo(name = "is_general")
    val isGeneral: Boolean = false, // مركز التكلفة العام الثابت (لا يمكن حذفه)

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "description")
    val description: String = "",

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
