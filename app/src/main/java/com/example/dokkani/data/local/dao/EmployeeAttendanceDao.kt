package com.example.dokkani.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.dokkani.data.local.entities.EmployeeAttendanceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EmployeeAttendanceDao {
    @Query("SELECT * FROM employee_attendance WHERE employeeId = :employeeId ORDER BY date DESC")
    fun getAttendanceForEmployee(employeeId: Long): Flow<List<EmployeeAttendanceEntity>>

    @Query("SELECT * FROM employee_attendance WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getAttendanceByDateRange(startDate: Long, endDate: Long): Flow<List<EmployeeAttendanceEntity>>

    @Query("SELECT * FROM employee_attendance WHERE date >= :startDate AND date <= :endDate")
    suspend fun getAttendanceByDateRangeSync(startDate: Long, endDate: Long): List<EmployeeAttendanceEntity>

    @Query("SELECT * FROM employee_attendance WHERE employeeId = :employeeId AND date >= :startDate AND date <= :endDate")
    suspend fun getEmployeeAttendanceForPeriodSync(employeeId: Long, startDate: Long, endDate: Long): List<EmployeeAttendanceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: EmployeeAttendanceEntity): Long

    @Update
    suspend fun updateAttendance(attendance: EmployeeAttendanceEntity)
}
