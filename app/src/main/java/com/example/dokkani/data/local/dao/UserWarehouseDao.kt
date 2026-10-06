package com.example.dokkani.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.dokkani.data.local.entities.UserWarehouseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserWarehouseDao {
    @Query("SELECT * FROM user_warehouses ORDER BY id DESC")
    fun getAllUserWarehouses(): Flow<List<UserWarehouseEntity>>

    @Query("SELECT * FROM user_warehouses WHERE id = :id LIMIT 1")
    suspend fun getUserWarehouseById(id: Long): UserWarehouseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserWarehouse(userWarehouse: UserWarehouseEntity): Long

    @Update
    suspend fun updateUserWarehouse(userWarehouse: UserWarehouseEntity)

    @Delete
    suspend fun deleteUserWarehouse(userWarehouse: UserWarehouseEntity)

    @Query("DELETE FROM user_warehouses WHERE id = :id")
    suspend fun deleteUserWarehouseById(id: Long)
}
