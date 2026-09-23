package com.example.dokkani.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.dokkani.data.local.entities.SalaryAdjustmentLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SalaryAdjustmentLogDao {
    @Query("SELECT * FROM salary_adjustments WHERE employeeId = :employeeId ORDER BY adjustedAt DESC")
    fun getLogsForEmployee(employeeId: Long): Flow<List<SalaryAdjustmentLogEntity>>

    @Query("SELECT * FROM salary_adjustments WHERE employeeId = :employeeId ORDER BY adjustedAt DESC")
    suspend fun getLogsForEmployeeSync(employeeId: Long): List<SalaryAdjustmentLogEntity>

    @Query("SELECT * FROM salary_adjustments ORDER BY adjustedAt DESC")
    fun getAllLogs(): Flow<List<SalaryAdjustmentLogEntity>>

    @Query("SELECT * FROM salary_adjustments ORDER BY adjustedAt DESC")
    suspend fun getAllLogsSync(): List<SalaryAdjustmentLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: SalaryAdjustmentLogEntity): Long
}
