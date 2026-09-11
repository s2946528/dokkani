package com.example.dokkani.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val username: String,
    val fullName: String,
    val pinCode: String,
    val role: UserRole,
    val isActive: Boolean = true
)
