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

    @Query("UPDATE cash_shifts SET totalCashSales = :newSales, expectedCashInDrawer = openingCash + :newSales + totalCashCollections - totalCashExpenses - totalSupplierPayments - totalCashPurchases - totalOwnerDrawings - totalStaffAdvances WHERE id = :id")
    suspend fun updateSales(id: Long, newSales: Double)

    @Query("UPDATE cash_shifts SET totalCashExpenses = :newExpenses, expectedCashInDrawer = openingCash + totalCashSales + totalCashCollections - :newExpenses - totalSupplierPayments - totalCashPurchases - totalOwnerDrawings - totalStaffAdvances WHERE id = :id")
    suspend fun updateExpenses(id: Long, newExpenses: Double)

    @Query("UPDATE cash_shifts SET totalCashCollections = :newCollections, expectedCashInDrawer = openingCash + totalCashSales + :newCollections - totalCashExpenses - totalSupplierPayments - totalCashPurchases - totalOwnerDrawings - totalStaffAdvances WHERE id = :id")
    suspend fun updateCollections(id: Long, newCollections: Double)

    @Query("UPDATE cash_shifts SET totalSupplierPayments = :newPayments, expectedCashInDrawer = openingCash + totalCashSales + totalCashCollections - totalCashExpenses - :newPayments - totalCashPurchases - totalOwnerDrawings - totalStaffAdvances WHERE id = :id")
    suspend fun updateSupplierPayments(id: Long, newPayments: Double)

    @Query("UPDATE cash_shifts SET totalCashPurchases = :newPurchases, expectedCashInDrawer = openingCash + totalCashSales + totalCashCollections - totalCashExpenses - totalSupplierPayments - :newPurchases - totalOwnerDrawings - totalStaffAdvances WHERE id = :id")
    suspend fun updateCashPurchases(id: Long, newPurchases: Double)

    @Query("UPDATE cash_shifts SET totalOwnerDrawings = :newDrawings, expectedCashInDrawer = openingCash + totalCashSales + totalCashCollections - totalCashExpenses - totalSupplierPayments - totalCashPurchases - :newDrawings - totalStaffAdvances WHERE id = :id")
    suspend fun updateOwnerDrawings(id: Long, newDrawings: Double)

    @Query("UPDATE cash_shifts SET totalStaffAdvances = :newAdvances, expectedCashInDrawer = openingCash + totalCashSales + totalCashCollections - totalCashExpenses - totalSupplierPayments - totalCashPurchases - totalOwnerDrawings - :newAdvances WHERE id = :id")
    suspend fun updateStaffAdvances(id: Long, newAdvances: Double)

    @Query("UPDATE cash_shifts SET totalMadaSales = totalMadaSales + :amount WHERE id = :id")
    suspend fun addMadaSales(id: Long, amount: Double)

    @Query("UPDATE cash_shifts SET totalWalletSales = totalWalletSales + :amount WHERE id = :id")
    suspend fun addWalletSales(id: Long, amount: Double)

    @Query("UPDATE cash_shifts SET totalTransferSales = totalTransferSales + :amount WHERE id = :id")
    suspend fun addTransferSales(id: Long, amount: Double)

    @Query("UPDATE cash_shifts SET totalCreditSales = totalCreditSales + :amount WHERE id = :id")
    suspend fun addCreditSales(id: Long, amount: Double)

    @Query("UPDATE cash_shifts SET settlementStatus = :status, settlementNotes = :notes, status = 'SETTLED' WHERE id = :id")
    suspend fun updateSettlement(id: Long, status: String, notes: String)

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
