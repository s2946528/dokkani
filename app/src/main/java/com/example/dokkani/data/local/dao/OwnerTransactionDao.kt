package com.example.dokkani.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.dokkani.data.local.entities.OwnerTransactionEntity
import com.example.dokkani.data.local.entities.OwnerTransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface OwnerTransactionDao {
    @Query("SELECT * FROM owner_transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<OwnerTransactionEntity>>

    @Query("SELECT * FROM owner_transactions ORDER BY date DESC")
    suspend fun getAllTransactionsSync(): List<OwnerTransactionEntity>

    @Query("SELECT * FROM owner_transactions WHERE type = :type ORDER BY date DESC")
    fun getTransactionsByType(type: OwnerTransactionType): Flow<List<OwnerTransactionEntity>>

    @Query("SELECT SUM(amount) FROM owner_transactions WHERE type IN ('CASH_DRAWING', 'GOODS_DRAWING')")
    fun getTotalDrawings(): Flow<Double?>

    @Query("SELECT SUM(amount) FROM owner_transactions WHERE type = 'CAPITAL_DEPOSIT'")
    fun getTotalCapitalDeposits(): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: OwnerTransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: OwnerTransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: OwnerTransactionEntity)

    @Query("DELETE FROM owner_transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)
}
