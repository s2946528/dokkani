package com.example.dokkani.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.dokkani.data.local.entities.EmployeeTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EmployeeTransactionDao {
    @Query("SELECT * FROM employee_transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<EmployeeTransactionEntity>>

    @Query("SELECT * FROM employee_transactions ORDER BY date DESC")
    suspend fun getAllTransactionsSync(): List<EmployeeTransactionEntity>

    @Query("SELECT * FROM employee_transactions WHERE employeeId = :employeeId ORDER BY date DESC")
    fun getTransactionsForEmployee(employeeId: Long): Flow<List<EmployeeTransactionEntity>>

    @Query("SELECT * FROM employee_transactions WHERE employeeId = :employeeId AND periodMonth = :month AND periodYear = :year")
    suspend fun getTransactionsByPeriodSync(employeeId: Long, month: Int, year: Int): List<EmployeeTransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: EmployeeTransactionEntity): Long

    @Delete
    suspend fun deleteTransaction(transaction: EmployeeTransactionEntity)

    @Query("DELETE FROM employee_transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)
}
