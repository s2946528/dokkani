package com.example.dokkani.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.dokkani.data.local.entities.CashShiftEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CashShiftDao {

    @Query("SELECT * FROM cash_shifts ORDER BY startTime DESC")
    fun getAllShifts(): Flow<List<CashShiftEntity>>

    @Query("SELECT * FROM cash_shifts ORDER BY startTime DESC")
    suspend fun getAllShiftsSync(): List<CashShiftEntity>

    @Query("UPDATE cash_shifts SET totalCashSales = :newSales, expectedCashInDrawer = openingCash + :newSales + totalCashCollections - totalCashExpenses WHERE id = :id")
    suspend fun updateSales(id: Long, newSales: Double)

    @Query("UPDATE cash_shifts SET totalCashExpenses = :newExpenses, expectedCashInDrawer = openingCash + totalCashSales + totalCashCollections - :newExpenses WHERE id = :id")
    suspend fun updateExpenses(id: Long, newExpenses: Double)

    @Query("UPDATE cash_shifts SET totalCashCollections = :newCollections, expectedCashInDrawer = openingCash + totalCashSales + :newCollections - totalCashExpenses WHERE id = :id")
    suspend fun updateCollections(id: Long, newCollections: Double)

    @Query("SELECT * FROM cash_shifts WHERE status = 'OPEN' ORDER BY startTime DESC LIMIT 1")
    suspend fun getOpenShift(): CashShiftEntity?

    @Query("SELECT * FROM cash_shifts ORDER BY startTime DESC LIMIT 1")
    suspend fun getLastShift(): CashShiftEntity?

    @Query("SELECT * FROM cash_shifts WHERE id = :id LIMIT 1")
    suspend fun getShiftById(id: Long): CashShiftEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShift(shift: CashShiftEntity): Long

    @Update
    suspend fun updateShift(shift: CashShiftEntity)

    @Query("SELECT COUNT(*) FROM cash_shifts")
    suspend fun countShifts(): Int
}
