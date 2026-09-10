package com.example.dokkani.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.dokkani.data.local.entities.CostValuationMethod
import com.example.dokkani.data.local.entities.SystemSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SystemSettingsDao {

    @Query("SELECT * FROM system_settings WHERE id = 1 LIMIT 1")
    fun getSettings(): Flow<SystemSettingsEntity?>

    @Query("SELECT * FROM system_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsSync(): SystemSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSettings(settings: SystemSettingsEntity)

    @Query("UPDATE system_settings SET costValuationMethod = :method, lastUpdated = :timestamp WHERE id = 1")
    suspend fun updateCostValuationMethod(method: CostValuationMethod, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE system_settings SET storeName = :name, defaultTaxRate = :taxRate, lastUpdated = :timestamp WHERE id = 1")
    suspend fun updateStoreInfo(name: String, taxRate: Double, timestamp: Long = System.currentTimeMillis())
}
