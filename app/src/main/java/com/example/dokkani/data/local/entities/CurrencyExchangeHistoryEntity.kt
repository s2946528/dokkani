package com.example.dokkani.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * جدول تسجيل وتتبع التغيرات على أسعار صرف العملات (سجل النشاط)
 */
@Entity(tableName = "currency_exchange_history")
data class CurrencyExchangeHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val currencyId: Long = 0L,
    val currencyName: String,
    val currencyCode: String,
    val newExchangeRate: Double,
    val oldExchangeRate: Double = 0.0,
    val changeTimestamp: Long = System.currentTimeMillis()
)
