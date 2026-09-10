package com.example.dokkani.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.dokkani.data.local.entities.BatchWithYields
import com.example.dokkani.data.local.entities.MixedProduceBatchEntity
import com.example.dokkani.data.local.entities.MixedProduceYieldItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MixedProduceBatchDao {

    @Query("SELECT * FROM mixed_produce_batches ORDER BY date DESC")
    fun getAllBatches(): Flow<List<MixedProduceBatchEntity>>

    @Transaction
    @Query("SELECT * FROM mixed_produce_batches ORDER BY date DESC")
    fun getBatchesWithYields(): Flow<List<BatchWithYields>>

    @Transaction
    @Query("SELECT * FROM mixed_produce_batches WHERE id = :batchId LIMIT 1")
    suspend fun getBatchWithYieldsById(batchId: Long): BatchWithYields?

    @Query("SELECT * FROM mixed_produce_yield_items WHERE batchId = :batchId")
    suspend fun getYieldItemsForBatch(batchId: Long): List<MixedProduceYieldItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatch(batch: MixedProduceBatchEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertYieldItems(items: List<MixedProduceYieldItemEntity>): List<Long>

    @Update
    suspend fun updateBatch(batch: MixedProduceBatchEntity)

    @Delete
    suspend fun deleteBatch(batch: MixedProduceBatchEntity)
}
