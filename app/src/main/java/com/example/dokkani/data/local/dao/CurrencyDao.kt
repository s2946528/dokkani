package com.example.dokkani.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.dokkani.data.local.entities.CurrencyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CurrencyDao {

    @Query("SELECT * FROM currencies ORDER BY isBaseCurrency DESC, isDefault DESC, code ASC")
    fun getAllCurrencies(): Flow<List<CurrencyEntity>>

    @Query("SELECT * FROM currencies ORDER BY isBaseCurrency DESC, isDefault DESC, code ASC")
    suspend fun getAllCurrenciesSync(): List<CurrencyEntity>

    // 1. مراقبة التغيرات لحظياً على العملة الأساسية (مهم جداً للواجهات)
    @Query("SELECT * FROM currencies WHERE isBaseCurrency = 1 LIMIT 1")
    fun getBaseCurrencyFlow(): Flow<CurrencyEntity?>

    @Query("SELECT * FROM currencies WHERE isBaseCurrency = 1 LIMIT 1")
    suspend fun getBaseCurrency(): CurrencyEntity?

    @Query("SELECT * FROM currencies WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultCurrency(): CurrencyEntity?

    @Query("SELECT * FROM currencies WHERE id = :id LIMIT 1")
    suspend fun getCurrencyById(id: Long): CurrencyEntity?

    // 2. تصفير راية العملة الأساسية السابقة لضمان عدم وجود أكثر من عملة أساسية
    @Query("UPDATE currencies SET isBaseCurrency = 0")
    suspend fun clearBaseCurrencyFlag()

    @Query("UPDATE currencies SET isDefault = 0")
    suspend fun clearDefaultCurrencyFlag()

    // 3. تعيين عملة كعملة أساسية جديدة بشكل آمن (Transaction)
    @Transaction
    suspend fun setAsBaseCurrency(currencyId: Long) {
        clearBaseCurrencyFlag()
        clearDefaultCurrencyFlag()
        @Query("UPDATE currencies SET isBaseCurrency = 1, isDefault = 1, exchangeRate = 1.0 WHERE id = :currencyId")
        // تنفيذ التحديث داخل الـ Repository أو استخدام الاستعلام المنفصل
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCurrency(currency: CurrencyEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCurrencies(currencies: List<CurrencyEntity>): List<Long>

    @Update
    suspend fun updateCurrency(currency: CurrencyEntity)

    @Delete
    suspend fun deleteCurrency(currency: CurrencyEntity)
}
