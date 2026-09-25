package com.example.dokkani.data.local.dao

import androidx.room.*
import com.example.dokkani.data.local.entities.ShortageSettlementEntity
import kotlinx.coroutines.flow.Flow

/**
 * واجهة الاستعلامات لقواعد بيانات تسوية العجز المخزني والبيع بالقيمة
 */
@Dao
interface ShortageSettlementDao {

    @Query("SELECT * FROM shortage_settlements ORDER BY createdAt DESC")
    fun getAllShortages(): Flow<List<ShortageSettlementEntity>>

    @Query("SELECT * FROM shortage_settlements ORDER BY createdAt DESC")
    suspend fun getAllShortagesSync(): List<ShortageSettlementEntity>

    @Query("SELECT * FROM shortage_settlements WHERE status = :status ORDER BY createdAt DESC")
    fun getShortagesByStatus(status: String): Flow<List<ShortageSettlementEntity>>

    @Query("SELECT * FROM shortage_settlements WHERE cost_center_id = :costCenterId ORDER BY createdAt DESC")
    fun getShortagesByCostCenter(costCenterId: Long): Flow<List<ShortageSettlementEntity>>

    @Query("SELECT * FROM shortage_settlements WHERE id = :id LIMIT 1")
    suspend fun getShortageById(id: Long): ShortageSettlementEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShortage(shortage: ShortageSettlementEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShortages(shortages: List<ShortageSettlementEntity>)

    @Update
    suspend fun updateShortage(shortage: ShortageSettlementEntity)

    @Query("UPDATE shortage_settlements SET status = 'SETTLED', settledAt = :settledAt, settledBy = :settledBy, notes = :notes WHERE id = :id")
    suspend fun markAsSettled(id: Long, settledBy: String, notes: String, settledAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM shortage_settlements WHERE id = :id")
    suspend fun deleteShortageById(id: Long)

    @Query("SELECT SUM(totalValueSalesAmount) FROM shortage_settlements WHERE status = 'SETTLED'")
    fun getTotalSettledValueSalesRevenue(): Flow<Double?>

    @Query("SELECT SUM(totalShortageCost) FROM shortage_settlements WHERE status = 'PENDING'")
    fun getTotalPendingShortageCost(): Flow<Double?>
}
