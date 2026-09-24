package com.example.dokkani.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.dokkani.data.local.entities.ProductWastageEntity
import com.example.dokkani.data.local.entities.ProductWastageWithProduct
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductWastageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWasteRecord(record: ProductWastageEntity): Long

    @Update
    suspend fun updateWasteRecord(record: ProductWastageEntity)

    @Delete
    suspend fun deleteWasteRecord(record: ProductWastageEntity)

    @Query("DELETE FROM product_wastage WHERE id = :wasteId")
    suspend fun deleteWasteRecordById(wasteId: Long)

    @Query("SELECT * FROM product_wastage WHERE id = :wasteId LIMIT 1")
    suspend fun getWasteRecordById(wasteId: Long): ProductWastageEntity?

    @Query("SELECT * FROM product_wastage ORDER BY timestamp DESC")
    fun getAllWasteRecords(): Flow<List<ProductWastageEntity>>

    @Transaction
    @Query("SELECT * FROM product_wastage ORDER BY timestamp DESC")
    fun getWasteRecordsWithProduct(): Flow<List<ProductWastageWithProduct>>

    @Transaction
    @Query("SELECT * FROM product_wastage ORDER BY timestamp DESC")
    suspend fun getWasteRecordsWithProductSync(): List<ProductWastageWithProduct>

    @Query("SELECT * FROM product_wastage WHERE productId = :productId ORDER BY timestamp DESC")
    fun getWasteRecordsForProduct(productId: Long): Flow<List<ProductWastageEntity>>

    @Query("SELECT * FROM product_wastage WHERE productId = :productId ORDER BY timestamp DESC")
    suspend fun getWasteRecordsForProductSync(productId: Long): List<ProductWastageEntity>

    @Query("SELECT * FROM product_wastage WHERE productId IN (:productIds)")
    suspend fun getWasteRecordsForProductsSync(productIds: List<Long>): List<ProductWastageEntity>

    @Query("SELECT SUM(quantity) FROM product_wastage WHERE productId = :productId")
    suspend fun getTotalWasteQuantityForProduct(productId: Long): Double?

    @Query("SELECT SUM(quantity) FROM product_wastage WHERE productId = :productId AND unit = :unitName")
    suspend fun getTotalWasteQuantityForProductAndUnit(productId: Long, unitName: String): Double?
}
