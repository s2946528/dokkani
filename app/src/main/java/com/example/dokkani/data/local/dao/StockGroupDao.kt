package com.example.dokkani.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.dokkani.data.local.entities.StockGroupAuditEntity
import com.example.dokkani.data.local.entities.StockGroupEntity
import com.example.dokkani.data.local.entities.StockGroupItemEntity
import com.example.dokkani.data.local.entities.StockGroupWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface StockGroupDao {

    @Query("SELECT * FROM stock_groups WHERE isActive = 1 ORDER BY name ASC")
    fun getAllActiveGroups(): Flow<List<StockGroupEntity>>

    @Query("SELECT * FROM stock_groups WHERE isActive = 1 ORDER BY name ASC")
    suspend fun getAllActiveGroupsSync(): List<StockGroupEntity>

    @Transaction
    @Query("SELECT * FROM stock_groups ORDER BY id DESC")
    fun getAllGroupsWithDetails(): Flow<List<StockGroupWithDetails>>

    @Transaction
    @Query("SELECT * FROM stock_groups ORDER BY id DESC")
    suspend fun getAllGroupsWithDetailsSync(): List<StockGroupWithDetails>

    @Transaction
    @Query("SELECT * FROM stock_groups WHERE id = :groupId LIMIT 1")
    suspend fun getGroupWithDetailsById(groupId: Long): StockGroupWithDetails?

    @Query("SELECT * FROM stock_groups WHERE id = :groupId LIMIT 1")
    suspend fun getGroupById(groupId: Long): StockGroupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: StockGroupEntity): Long

    @Update
    suspend fun updateGroup(group: StockGroupEntity)

    @Delete
    suspend fun deleteGroup(group: StockGroupEntity)

    @Query("DELETE FROM stock_groups WHERE id = :groupId")
    suspend fun deleteGroupById(groupId: Long)

    // إدارة عناصر المجموعات
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroupItems(items: List<StockGroupItemEntity>): List<Long>

    @Query("DELETE FROM stock_group_items WHERE groupId = :groupId")
    suspend fun deleteGroupItems(groupId: Long)

    @Query("DELETE FROM stock_group_items WHERE id = :itemId")
    suspend fun deleteGroupItemById(itemId: Long)

    @Update
    suspend fun updateGroupItem(item: StockGroupItemEntity)

    @Query("SELECT * FROM stock_group_items WHERE groupId = :groupId")
    suspend fun getItemsForGroup(groupId: Long): List<StockGroupItemEntity>

    // إدارة الجرد الدوري
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudit(audit: StockGroupAuditEntity): Long

    @Update
    suspend fun updateAudit(audit: StockGroupAuditEntity)

    @Query("DELETE FROM stock_group_audits WHERE id = :auditId")
    suspend fun deleteAuditById(auditId: Long)

    @Query("SELECT * FROM stock_group_audits WHERE groupId = :groupId ORDER BY auditDate DESC")
    fun getAuditsForGroup(groupId: Long): Flow<List<StockGroupAuditEntity>>

    @Query("SELECT * FROM stock_group_audits WHERE groupId = :groupId ORDER BY auditDate DESC")
    suspend fun getAuditsForGroupSync(groupId: Long): List<StockGroupAuditEntity>

    // فحص التحقق من المبيعات لضمان الحذف الآمن والسلامة المحاسبية
    @Query("SELECT COUNT(*) FROM invoice_items WHERE productId IN (SELECT productId FROM stock_group_items WHERE groupId = :groupId AND productId IS NOT NULL)")
    suspend fun getLinkedSalesCountForGroup(groupId: Long): Int
}
