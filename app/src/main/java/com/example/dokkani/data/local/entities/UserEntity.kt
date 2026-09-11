package com.example.dokkani.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class UserRole {
    ADMIN,      // مدير النظام (صلاحيات كاملة)
    CASHIER,    // كاشير (فواتير، شفتات، جلسات بيع)
    INVENTORY   // مسؤول مخزن (إدخال منتجات، حركات مخزونية، خضار مشكل)
}

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val username: String,
    val fullName: String,
    val pinCode: String, // رمز PIN سريع لتغيير الكاشير/المستخدم
    val role: UserRole = UserRole.CASHIER,
    val isActive: Boolean = true
)
