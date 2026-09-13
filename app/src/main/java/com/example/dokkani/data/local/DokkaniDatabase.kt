package com.example.dokkani.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.dokkani.data.local.converters.DateConverter
import com.example.dokkani.data.local.converters.EnumConverters
import com.example.dokkani.data.local.dao.CurrencyDao
import com.example.dokkani.data.local.dao.ProductDao
import com.example.dokkani.data.local.dao.SystemSettingsDao
import com.example.dokkani.data.local.dao.UserDao
import com.example.dokkani.data.local.entities.CostValuationMethod
import com.example.dokkani.data.local.entities.CurrencyEntity
import com.example.dokkani.data.local.entities.ProductEntity
import com.example.dokkani.data.local.entities.SystemSettingsEntity
import com.example.dokkani.data.local.entities.UserEntity
import com.example.dokkani.data.local.entities.UserRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserEntity::class,
        CurrencyEntity::class,
        SystemSettingsEntity::class,
        ProductEntity::class
        // أضف أي كيانات (Entities) أخرى موجودة في مشروعك هنا
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(DateConverter::class, EnumConverters::class)
abstract class DokkaniDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun currencyDao(): CurrencyDao
    abstract fun systemSettingsDao(): SystemSettingsDao
    abstract fun productDao(): ProductDao
    // أضف واجهات الـ DAOs المتبقية هنا

    companion object {
        @Volatile
        private var INSTANCE: DokkaniDatabase? = null

        fun getInstance(context: Context, scope: CoroutineScope): DokkaniDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DokkaniDatabase::class.java,
                    "dokkani.db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DokkaniDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        // دالة تعبئة البيانات الافتراضية Clean Seed
        private suspend fun populateInitialGroceryData(db: DokkaniDatabase) {

            // 1. حساب مدير النظام الافتراضي
            val userDao = db.userDao()
            userDao.insertUser(
                UserEntity(
                    username = "admin",
                    fullName = "مدير النظام",
                    pinCode = "1234",
                    role = UserRole.ADMIN,
                    isActive = true
                )
            )

            // 2. الـ 3 عملات فقط
            val currencyDao = db.currencyDao()

            // الريال اليمني (العملة الأساسية والافتراضية)
            currencyDao.insertCurrency(
                CurrencyEntity(
                    code = "YER",
                    name = "ريال يمني",
                    symbol = "ر.ي",
                    exchangeRateToBase = 1.0,
                    isBaseCurrency = true,
                    isDefault = true
                )
            )

            // الريال السعودي
            currencyDao.insertCurrency(
                CurrencyEntity(
                    code = "SAR",
                    name = "ريال سعودي",
                    symbol = "ر.س",
                    exchangeRateToBase = 140.0,
                    isBaseCurrency = false,
                    isDefault = false
                )
            )

            // الدولار الأمريكي
            currencyDao.insertCurrency(
                CurrencyEntity(
                    code = "USD",
                    name = "دولار أمريكي",
                    symbol = "$",
                    exchangeRateToBase = 530.0,
                    isBaseCurrency = false,
                    isDefault = false
                )
            )

            // 3. إعدادات النظام الافتراضية
            val settingsDao = db.systemSettingsDao()
            settingsDao.insertOrUpdateSettings(
                SystemSettingsEntity(
                    id = 1,
                    storeName = "متجري",
                    costValuationMethod = CostValuationMethod.WAC,
                    defaultCurrencyCode = "YER",
                    defaultTaxRate = 0.0,
                    enableProduceShrinkageTracking = false,
                    enableNegativeStock = false,
                    invoiceFooterText = "شكراً لزيارتكم!"
                )
            )
        }
    }

    private class DokkaniDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialGroceryData(database)
                }
            }
        }
    }
}
