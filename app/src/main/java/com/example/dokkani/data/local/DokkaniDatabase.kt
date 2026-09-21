package com.example.dokkani.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.dokkani.data.local.dao.CashShiftDao
import com.example.dokkani.data.local.dao.CurrencyDao
import com.example.dokkani.data.local.dao.ExpenseDao
import com.example.dokkani.data.local.dao.FinancialAccountDao
import com.example.dokkani.data.local.dao.FixedAssetDao
import com.example.dokkani.data.local.dao.InvoiceDao
import com.example.dokkani.data.local.dao.LeaseholdRightDao
import com.example.dokkani.data.local.dao.LicenseDao
import com.example.dokkani.data.local.dao.MixedProduceBatchDao
import com.example.dokkani.data.local.dao.OwnerTransactionDao
import com.example.dokkani.data.local.dao.PartyDao
import com.example.dokkani.data.local.dao.PaymentVoucherDao
import com.example.dokkani.data.local.dao.ProductDao
import com.example.dokkani.data.local.dao.StockMovementDao
import com.example.dokkani.data.local.dao.SystemSettingsDao
import com.example.dokkani.data.local.dao.UserDao
import com.example.dokkani.data.local.entities.CashShiftEntity
import com.example.dokkani.data.local.entities.CostValuationMethod
import com.example.dokkani.data.local.entities.CurrencyEntity
import com.example.dokkani.data.local.entities.ExpenseEntity
import com.example.dokkani.data.local.entities.FinancialAccountEntity
import com.example.dokkani.data.local.entities.FinancialAccountType
import com.example.dokkani.data.local.entities.FixedAssetEntity
import com.example.dokkani.data.local.entities.InvoiceEntity
import com.example.dokkani.data.local.entities.InvoiceItemEntity
import com.example.dokkani.data.local.entities.LeaseholdRightEntity
import com.example.dokkani.data.local.entities.LicenseEntity
import com.example.dokkani.data.local.entities.MixedProduceBatchEntity
import com.example.dokkani.data.local.entities.MixedProduceYieldItemEntity
import com.example.dokkani.data.local.entities.OwnerTransactionEntity
import com.example.dokkani.data.local.entities.PartyEntity
import com.example.dokkani.data.local.entities.PaymentVoucherEntity
import com.example.dokkani.data.local.entities.ProductEntity
import com.example.dokkani.data.local.entities.ProductUnitEntity
import com.example.dokkani.data.local.entities.StockMovementEntity
import com.example.dokkani.data.local.entities.SystemSettingsEntity
import com.example.dokkani.data.local.entities.UserEntity
import com.example.dokkani.data.local.entities.UserRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * قاعدة البيانات الرئيسية لنظام دكاني (Dokkani Database)
 */
@Database(
    entities = [
        UserEntity::class,
        ProductEntity::class,
        ProductUnitEntity::class,
        CurrencyEntity::class,
        PartyEntity::class,
        InvoiceEntity::class,
        InvoiceItemEntity::class,
        StockMovementEntity::class,
        MixedProduceBatchEntity::class,
        MixedProduceYieldItemEntity::class,
        SystemSettingsEntity::class,
        PaymentVoucherEntity::class,
        ExpenseEntity::class,
        CashShiftEntity::class,
        LicenseEntity::class,
        FixedAssetEntity::class,
        OwnerTransactionEntity::class,
        LeaseholdRightEntity::class,
        FinancialAccountEntity::class
    ],
    version = 9,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class DokkaniDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao
    abstract fun currencyDao(): CurrencyDao
    abstract fun partyDao(): PartyDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun stockMovementDao(): StockMovementDao
    abstract fun mixedProduceBatchDao(): MixedProduceBatchDao
    abstract fun systemSettingsDao(): SystemSettingsDao
    abstract fun paymentVoucherDao(): PaymentVoucherDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun cashShiftDao(): CashShiftDao
    abstract fun licenseDao(): LicenseDao
    abstract fun userDao(): UserDao
    abstract fun fixedAssetDao(): FixedAssetDao
    abstract fun ownerTransactionDao(): OwnerTransactionDao
    abstract fun leaseholdRightDao(): LeaseholdRightDao
    abstract fun financialAccountDao(): FinancialAccountDao

    companion object {
        @Volatile
        private var INSTANCE: DokkaniDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): DokkaniDatabase {
            return INSTANCE ?: synchronized(this) {
                var instance: DokkaniDatabase? = null
                instance = Room.databaseBuilder(
                    context.applicationContext,
                    DokkaniDatabase::class.java,
                    "dokkani_pos_database"
                )
                    .addCallback(DokkaniDatabaseCallback(scope) { instance })
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DokkaniDatabaseCallback(
        private val scope: CoroutineScope,
        private val provider: () -> DokkaniDatabase?
    ) : RoomDatabase.Callback() {

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            scope.launch(Dispatchers.IO) {
                provider()?.let { database ->
                    database.withTransaction {
                        // البذر النظيف فقط عند إنشاء قاعدة البيانات
                        populateInitialGroceryData(database)
                    }
                }
            }
        }

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

            // 2. إدخال 3 عملات فقط (الريال اليمني أساسي)
            val currencyDao = db.currencyDao()
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
                    storeName = "دكاني - تموينات ومخضار السعادة",
                    storeAddress = "صنعاء - شارع الزبيري",
                    storePhone = "777000111",
                    taxNumber = "300123456700003",
                    showPreviousBalanceOnInvoice = true,
                    costValuationMethod = CostValuationMethod.WAC,
                    defaultCurrencyCode = "YER",
                    isTaxEnabled = false,
                    defaultTaxRate = 0.0,
                    isPurchaseTaxEnabled = false,
                    purchaseTaxRate = 0.0,
                    enableProduceShrinkageTracking = false,
                    enableNegativeStock = false,
                    invoiceFooterText = "شكراً لزيارتكم دكاني - تسوقكم يسعدنا!"
                )
            )

            // 4. الحسابات المالية والبنوك والمحافظ الافتراضية
            val accountDao = db.financialAccountDao()
            if (accountDao.getAccountsCount() == 0) {
                accountDao.insertAll(
                    listOf(
                        FinancialAccountEntity(
                            code = "10101",
                            name = "صندوق النقدية الرئيسي",
                            accountType = FinancialAccountType.CASH_DRAWER,
                            parentAccountCode = "101",
                            parentAccountName = "101 - النقدية وما في حكمها (الصناديق)",
                            accountNumber = "DRAWER-01",
                            openingBalance = 0.0,
                            currentBalance = 0.0,
                            notes = "صندوق الكاشير والخزينة النقدية الرئيسية"
                        ),
                        FinancialAccountEntity(
                            code = "10201",
                            name = "مصرف الراجحي - الحساب الجاري",
                            accountType = FinancialAccountType.BANK,
                            parentAccountCode = "102",
                            parentAccountName = "102 - البنوك والمصارف التجارية",
                            accountNumber = "SA0380000201608010000000",
                            openingBalance = 0.0,
                            currentBalance = 0.0,
                            notes = "الحساب البنكي الرئيسي للمتجر"
                        ),
                        FinancialAccountEntity(
                            code = "10202",
                            name = "البنك الأهلي السعودي (SNB)",
                            accountType = FinancialAccountType.BANK,
                            parentAccountCode = "102",
                            parentAccountName = "102 - البنوك والمصارف التجارية",
                            accountNumber = "SA1010000012345678901234",
                            openingBalance = 0.0,
                            currentBalance = 0.0,
                            notes = "حساب بنكي فرعي لتحصيل المبيعات"
                        ),
                        FinancialAccountEntity(
                            code = "10301",
                            name = "محفظة STC Pay للأعمال",
                            accountType = FinancialAccountType.E_WALLET,
                            parentAccountCode = "103",
                            parentAccountName = "103 - محافظ الدفع والتحصيل الإلكتروني",
                            accountNumber = "STC-966500000000",
                            openingBalance = 0.0,
                            currentBalance = 0.0,
                            notes = "محفظة تجارية للتحصيل الإلكتروني السريع"
                        ),
                        FinancialAccountEntity(
                            code = "10302",
                            name = "محفظة يورباي Urpay",
                            accountType = FinancialAccountType.E_WALLET,
                            parentAccountCode = "103",
                            parentAccountName = "103 - محافظ الدفع والتحصيل الإلكتروني",
                            accountNumber = "URP-966555000000",
                            openingBalance = 0.0,
                            currentBalance = 0.0,
                            notes = "محفظة دفع رقمية"
                        )
                    )
                )
            }
        }
    }
}
