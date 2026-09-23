package com.example.dokkani.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.dokkani.data.local.entities.CurrencyExchangeHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CurrencyExchangeHistoryDao {

    @Query("SELECT * FROM currency_exchange_history ORDER BY changeTimestamp DESC")
    fun getAllLogsFlow(): Flow<List<CurrencyExchangeHistoryEntity>>

    @Query("SELECT * FROM currency_exchange_history ORDER BY changeTimestamp DESC")
    suspend fun getAllLogsSync(): List<CurrencyExchangeHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: CurrencyExchangeHistoryEntity): Long
}
