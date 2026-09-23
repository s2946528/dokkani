package com.example.dokkani.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.dokkani.data.local.entities.PayrollRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PayrollRecordDao {
    @Query("SELECT * FROM payroll_records ORDER BY periodYear DESC, periodMonth DESC, id DESC")
    fun getAllPayrollRecords(): Flow<List<PayrollRecordEntity>>

    @Query("SELECT * FROM payroll_records WHERE periodMonth = :month AND periodYear = :year")
    suspend fun getPayrollRecordsForPeriodSync(month: Int, year: Int): List<PayrollRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayrollRecord(record: PayrollRecordEntity): Long

    @Update
    suspend fun updatePayrollRecord(record: PayrollRecordEntity)
}
