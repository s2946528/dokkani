package com.example.dokkani.data.local.dao

import androidx.room.*
import com.example.dokkani.data.local.entities.AssemblyStatus
import com.example.dokkani.data.local.entities.ItemAssemblyComponentEntity
import com.example.dokkani.data.local.entities.ItemAssemblyEntity
import com.example.dokkani.data.local.entities.ItemAssemblyWithComponents
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemAssemblyDao {

    @Transaction
    @Query("SELECT * FROM item_assemblies ORDER BY date DESC, id DESC")
    fun getAllAssembliesWithComponents(): Flow<List<ItemAssemblyWithComponents>>

    @Transaction
    @Query("SELECT * FROM item_assemblies WHERE id = :id LIMIT 1")
    suspend fun getAssemblyByIdWithComponents(id: Long): ItemAssemblyWithComponents?

    @Query("SELECT COUNT(*) FROM item_assemblies")
    suspend fun getAssemblyCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssembly(assembly: ItemAssemblyEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComponents(components: List<ItemAssemblyComponentEntity>)

    @Update
    suspend fun updateAssembly(assembly: ItemAssemblyEntity)

    @Query("UPDATE item_assemblies SET status = :status WHERE id = :id")
    suspend fun updateAssemblyStatus(id: Long, status: AssemblyStatus)

    @Query("DELETE FROM item_assembly_components WHERE assemblyId = :assemblyId")
    suspend fun deleteComponentsByAssemblyId(assemblyId: Long)

    @Delete
    suspend fun deleteAssembly(assembly: ItemAssemblyEntity)

    @Query("DELETE FROM item_assemblies WHERE id = :id")
    suspend fun deleteAssemblyById(id: Long)
}
