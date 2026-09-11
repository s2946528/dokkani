package com.example.dokkani.data.local.dao

import androidx.room.*
import com.example.dokkani.data.local.entities.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE isActive = 1")
    fun getAllActiveUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE username = :username AND pinCode = :pin LIMIT 1")
    suspend fun getUserByPin(username: String, pin: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    @Update
    suspend fun updateUser(user: UserEntity)
}
