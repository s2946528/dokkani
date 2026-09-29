package com.example.dokkani.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.dokkani.data.local.entities.GlobalGroupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GlobalGroupDao {

    @Query("SELECT * FROM GlobalGroups ORDER BY name ASC")
    fun getAllGroups(): Flow<List<GlobalGroupEntity>>

    @Query("SELECT * FROM GlobalGroups WHERE entity_type = :entityType ORDER BY name ASC")
    fun getGroupsByEntityType(entityType: String): Flow<List<GlobalGroupEntity>>

    @Query("SELECT * FROM GlobalGroups WHERE entity_type = :entityType ORDER BY name ASC")
    suspend fun getGroupsByEntityTypeSync(entityType: String): List<GlobalGroupEntity>

    @Query("SELECT * FROM GlobalGroups WHERE id = :id")
    suspend fun getGroupById(id: Long): GlobalGroupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: GlobalGroupEntity): Long

    @Update
    suspend fun updateGroup(group: GlobalGroupEntity)

    @Delete
    suspend fun deleteGroup(group: GlobalGroupEntity)

    @Query("DELETE FROM GlobalGroups WHERE id = :id")
    suspend fun deleteGroupById(id: Long)
}
