package com.example.dokkani.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.dokkani.data.local.entities.PaymentVoucherEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentVoucherDao {

    @Query("SELECT * FROM payment_vouchers ORDER BY date DESC")
    fun getAllVouchers(): Flow<List<PaymentVoucherEntity>>

    @Query("SELECT * FROM payment_vouchers ORDER BY date DESC")
    suspend fun getAllVouchersSync(): List<PaymentVoucherEntity>

    @Query("SELECT * FROM payment_vouchers WHERE partyId = :partyId ORDER BY date DESC")
    fun getVouchersForParty(partyId: Long): Flow<List<PaymentVoucherEntity>>

    @Query("SELECT * FROM payment_vouchers WHERE partyId = :partyId ORDER BY date DESC")
    suspend fun getVouchersForPartySync(partyId: Long): List<PaymentVoucherEntity>

    @Query("SELECT * FROM payment_vouchers WHERE date >= :startTime AND date <= :endTime ORDER BY date DESC")
    fun getVouchersByDateRange(startTime: Long, endTime: Long): Flow<List<PaymentVoucherEntity>>

    @Query("SELECT * FROM payment_vouchers WHERE date >= :startTime AND date <= :endTime ORDER BY date DESC")
    suspend fun getVouchersByDateRangeSync(startTime: Long, endTime: Long): List<PaymentVoucherEntity>

    @Query("SELECT * FROM payment_vouchers WHERE id = :id LIMIT 1")
    suspend fun getVoucherById(id: Long): PaymentVoucherEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoucher(voucher: PaymentVoucherEntity): Long

    @Update
    suspend fun updateVoucher(voucher: PaymentVoucherEntity)

    @Delete
    suspend fun deleteVoucher(voucher: PaymentVoucherEntity)

    @Query("SELECT COUNT(*) FROM payment_vouchers")
    suspend fun countVouchers(): Int
}
