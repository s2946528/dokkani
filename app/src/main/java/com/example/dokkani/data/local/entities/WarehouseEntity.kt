package com.example.dokkani.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * كيان دليل المخازن لنظام دكاني المحاسبي (Warehouse Entity)
 */
@Entity(tableName = "warehouses")
data class WarehouseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val warehouseCode: String,
    val name: String,
    val keeperName: String = "",
    val financialAccountId: Long? = null,
    val notes: String = "",
    val isActive: Boolean = true,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
