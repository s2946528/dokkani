package com.example.dokkani.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.dokkani.data.local.entities.LicenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LicenseDao {

    @Query("SELECT * FROM app_license WHERE id = 1 LIMIT 1")
    fun getLicenseFlow(): Flow<LicenseEntity?>

    @Query("SELECT * FROM app_license WHERE id = 1 LIMIT 1")
    suspend fun getLicenseSync(): LicenseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(license: LicenseEntity)

    @Query("UPDATE app_license SET lastKnownSystemTimestamp = :time WHERE id = 1")
    suspend fun updateLastKnownTime(time: Long)

    @Query("UPDATE app_license SET isTimeTampered = :isTampered, tamperReason = :reason WHERE id = 1")
    suspend fun updateTamperState(isTampered: Boolean, reason: String?)
}
