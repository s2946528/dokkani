package com.example.dokkani.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * كيان ربط وصلاحيات مخازن المستخدمين لنظام دكاني (User Warehouses Entity)
 */
@Entity(tableName = "user_warehouses")
data class UserWarehouseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long? = null,
    val userName: String,
    val warehouseId: Long? = null,
    val warehouseCode: String = "",
    val warehouseName: String,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
