package com.example.dokkani.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.dokkani.data.local.entities.LeaseholdRightEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LeaseholdRightDao {
    @Query("SELECT * FROM leasehold_rights ORDER BY contractStartDate DESC")
    fun getAllLeaseholdRights(): Flow<List<LeaseholdRightEntity>>

    @Query("SELECT * FROM leasehold_rights WHERE id = :id")
    suspend fun getLeaseholdRightById(id: Long): LeaseholdRightEntity?

    @Query("SELECT SUM(currentBookValue) FROM leasehold_rights WHERE status = 'ACTIVE'")
    fun getTotalActiveBookValue(): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLeaseholdRight(leaseholdRight: LeaseholdRightEntity): Long

    @Update
    suspend fun updateLeaseholdRight(leaseholdRight: LeaseholdRightEntity)

    @Delete
    suspend fun deleteLeaseholdRight(leaseholdRight: LeaseholdRightEntity)

    @Query("DELETE FROM leasehold_rights WHERE id = :id")
    suspend fun deleteLeaseholdRightById(id: Long)
}
