package com.example.dokkani.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.dokkani.data.local.entities.FixedAssetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FixedAssetDao {
    @Query("SELECT * FROM fixed_assets ORDER BY purchaseDate DESC")
    fun getAllAssets(): Flow<List<FixedAssetEntity>>

    @Query("SELECT * FROM fixed_assets WHERE id = :id")
    suspend fun getAssetById(id: Long): FixedAssetEntity?

    @Query("SELECT SUM(currentValue) FROM fixed_assets WHERE status = 'ACTIVE'")
    fun getTotalActiveAssetsValue(): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAsset(asset: FixedAssetEntity): Long

    @Update
    suspend fun updateAsset(asset: FixedAssetEntity)

    @Delete
    suspend fun deleteAsset(asset: FixedAssetEntity)

    @Query("DELETE FROM fixed_assets WHERE id = :id")
    suspend fun deleteAssetById(id: Long)
}
