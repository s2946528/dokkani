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

    // التحديث التراكمي المباشر للمبيعات النقدية
    @Query("UPDATE cash_shifts SET totalCashSales = totalCashSales + :amount, expectedCashInDrawer = expectedCashInDrawer + :amount WHERE id = :id")
    suspend fun addCashSales(id: Long, amount: Double)

    // التحديث التراكمي المباشر للمشتريات النقدية (تخصم من الدرج)
    @Query("UPDATE cash_shifts SET totalCashPurchases = totalCashPurchases + :amount, expectedCashInDrawer = expectedCashInDrawer - :amount WHERE id = :id")
    suspend fun addCashPurchase(id: Long, amount: Double)

    // التحديث التراكمي المباشر للمصروفات (تخصم من الدرج)
    @Query("UPDATE cash_shifts SET totalCashExpenses = totalCashExpenses + :amount, expectedCashInDrawer = expectedCashInDrawer - :amount WHERE id = :id")
    suspend fun addCashExpense(id: Long, amount: Double)

    // التحديث التراكمي المباشر لسندات القبض وتحصيل الديون (تضيف للدرج)
    @Query("UPDATE cash_shifts SET totalCashCollections = totalCashCollections + :amount, expectedCashInDrawer = expectedCashInDrawer + :amount WHERE id = :id")
    suspend fun addCashCollection(id: Long, amount: Double)

    // التحديث التراكمي المباشر لسندات الصرف للموردين (تخصم من الدرج)
    @Query("UPDATE cash_shifts SET totalSupplierPayments = totalSupplierPayments + :amount, expectedCashInDrawer = expectedCashInDrawer - :amount WHERE id = :id")
    suspend fun addSupplierPayment(id: Long, amount: Double)

    // مسحوبات المالك (تخصم من الدرج)
    @Query("UPDATE cash_shifts SET totalOwnerDrawings = totalOwnerDrawings + :amount, expectedCashInDrawer = expectedCashInDrawer - :amount WHERE id = :id")
    suspend fun addOwnerDrawing(id: Long, amount: Double)

    // سلف الموظفين (تخصم من الدرج)
    @Query("UPDATE cash_shifts SET totalStaffAdvances = totalStaffAdvances + :amount, expectedCashInDrawer = expectedCashInDrawer - :amount WHERE id = :id")
    suspend fun addStaffAdvance(id: Long, amount: Double)

    @Query("UPDATE cash_shifts SET totalMadaSales = totalMadaSales + :amount WHERE id = :id")
    suspend fun addMadaSales(id: Long, amount: Double)

    @Query("UPDATE cash_shifts SET totalWalletSales = totalWalletSales + :amount WHERE id = :id")
    suspend fun addWalletSales(id: Long, amount: Double)

    @Query("UPDATE cash_shifts SET totalTransferSales = totalTransferSales + :amount WHERE id = :id")
    suspend fun addTransferSales(id: Long, amount: Double)

    @Query("UPDATE cash_shifts SET totalCreditSales = totalCreditSales + :amount WHERE id, :amount WHERE id = :id")
    suspend fun addCreditSales(id: Long, amount: Double)

    @Query("UPDATE cash_shifts SET settlementStatus = :status, settlementNotes = :notes, status = 'SETTLED' WHERE id = :id")
    suspend fun updateSettlement(id: Long, status: String, notes: String)

    @Query("SELECT * FROM cash_shifts WHERE status = 'OPEN' ORDER BY startTime DESC LIMIT */
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
