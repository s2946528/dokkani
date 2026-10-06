package com.example.dokkani.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.dokkani.data.local.entities.WarehouseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WarehouseDao {
    @Query("SELECT * FROM warehouses ORDER BY isDefault DESC, id ASC")
    fun getAllWarehouses(): Flow<List<WarehouseEntity>>

    @Query("SELECT * FROM warehouses ORDER BY isDefault DESC, id ASC")
    fun getAllWarehousesSync(): List<WarehouseEntity>

    @Query("SELECT * FROM warehouses WHERE id = :id LIMIT 1")
    suspend fun getWarehouseById(id: Long): WarehouseEntity?

    @Query("SELECT * FROM warehouses WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultWarehouse(): WarehouseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWarehouse(warehouse: WarehouseEntity): Long

    @Update
    suspend fun updateWarehouse(warehouse: WarehouseEntity)

    @Delete
    suspend fun deleteWarehouse(warehouse: WarehouseEntity)

    @Query("DELETE FROM warehouses WHERE id = :id AND isDefault = 0")
    suspend fun deleteWarehouseById(id: Long)

    @Query("UPDATE warehouses SET isDefault = 0")
    suspend fun clearAllDefaults()

    @Query("UPDATE warehouses SET isDefault = CASE WHEN id = :id THEN 1 ELSE 0 END")
    suspend fun setDefaultWarehouse(id: Long)

    @Query("UPDATE warehouses SET isActive = :isActive WHERE id = :id")
    suspend fun setWarehouseActive(id: Long, isActive: Boolean)
}
