package com.example.dokkani.data.local.dao

import androidx.room.*
import com.example.dokkani.data.local.entities.InventoryAuditSheetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryAuditSheetDao {
    @Query("SELECT * FROM inventory_audit_sheets ORDER BY date DESC")
    fun getAllAuditSheets(): Flow<List<InventoryAuditSheetEntity>>

    @Query("SELECT * FROM inventory_audit_sheets WHERE id = :id")
    suspend fun getAuditSheetById(id: Long): InventoryAuditSheetEntity?

    @Query("SELECT * FROM inventory_audit_sheets WHERE voucherNumber = :voucherNumber LIMIT 1")
    suspend fun getAuditSheetByVoucherNumber(voucherNumber: String): InventoryAuditSheetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditSheet(sheet: InventoryAuditSheetEntity): Long

    @Update
    suspend fun updateAuditSheet(sheet: InventoryAuditSheetEntity)

    @Delete
    suspend fun deleteAuditSheet(sheet: InventoryAuditSheetEntity)

    @Query("DELETE FROM inventory_audit_sheets WHERE id = :id AND status = 'DRAFT'")
    suspend fun deleteDraftSheetById(id: Long)
}
