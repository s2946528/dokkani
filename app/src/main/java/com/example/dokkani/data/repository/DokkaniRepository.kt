package com.example.dokkani.data.repository

import androidx.room.withTransaction
import com.example.dokkani.data.local.DokkaniDatabase
import com.example.dokkani.data.local.dao.CurrencyDao
import com.example.dokkani.data.local.entities.CurrencyEntity
import kotlinx.coroutines.flow.Flow

/**
 * المستودع الرئيسي لإدارة البيانات في نظام دكاني
 */
class DokkaniRepository(
    private val database: DokkaniDatabase,
    private val currencyDao: CurrencyDao = database.currencyDao()
) {

    // 1. مراقبة العملة الأساسية تفاعلياً لجميع شاشات النظام
    val baseCurrencyFlow: Flow<CurrencyEntity?> = currencyDao.getBaseCurrencyFlow()

    // 2. جلب قائمة كل العملات
    fun getAllCurrencies(): Flow<List<CurrencyEntity>> = currencyDao.getAllCurrencies()

    // 3. التبديل الذري للعملة الأساسية (يزيل راية الأساسي القديمة ويُثبّت العملة الجديدة)
    suspend fun setAsBaseCurrency(selectedCurrency: CurrencyEntity) {
        database.withTransaction {
            // تصفير رايات العملة الأساسية والافتراضية القديمة
            currencyDao.clearBaseAndDefaultFlags()

            // إعداد الكيان الجديد برقم صرف 1.0 وراية الأساسي
            val updatedCurrency = selectedCurrency.copy(
                isBaseCurrency = true,
                isDefault = true,
                exchangeRateToBase = 1.0
            )

            // حفظ التحديث في قاعدة البيانات
            currencyDao.insertCurrency(updatedCurrency)
        }
    }

    // 4. حذف عملة
    suspend fun deleteCurrency(currency: CurrencyEntity) {
        currencyDao.deleteCurrency(currency)
    }

    // 5. إضافة أو تحديث عملة
    suspend fun saveCurrency(currency: CurrencyEntity) {
        currencyDao.insertCurrency(currency)
    }
}
