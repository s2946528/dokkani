package com.example.dokkani.data.local

import android.content.Context
import com.example.dokkani.data.local.entities.UserEntity
import com.example.dokkani.data.local.dao.UserDao
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.withTransaction
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.dokkani.data.local.dao.CashShiftDao
import com.example.dokkani.data.local.dao.CurrencyDao
import com.example.dokkani.data.local.dao.ExpenseDao
import com.example.dokkani.data.local.dao.InvoiceDao
import com.example.dokkani.data.local.dao.LicenseDao
import com.example.dokkani.data.local.dao.MixedProduceBatchDao
import com.example.dokkani.data.local.dao.PartyDao
import com.example.dokkani.data.local.dao.PaymentVoucherDao
import com.example.dokkani.data.local.dao.ProductDao
import com.example.dokkani.data.local.dao.StockMovementDao
import com.example.dokkani.data.local.dao.SystemSettingsDao
import com.example.dokkani.data.local.entities.BatchStatus
import com.example.dokkani.data.local.entities.CashShiftEntity
import com.example.dokkani.data.local.entities.CostValuationMethod
import com.example.dokkani.data.local.entities.CurrencyEntity
import com.example.dokkani.data.local.entities.ExpenseEntity
import com.example.dokkani.data.local.entities.InvoiceEntity
import com.example.dokkani.data.local.entities.InvoiceItemEntity
import com.example.dokkani.data.local.entities.InvoiceStatus
import com.example.dokkani.data.local.entities.InvoiceType
import com.example.dokkani.data.local.entities.LicenseEntity
import com.example.dokkani.data.local.entities.MixedProduceBatchEntity
import com.example.dokkani.data.local.entities.MixedProduceYieldItemEntity
import com.example.dokkani.data.local.entities.MovementType
import com.example.dokkani.data.local.entities.PartyEntity
import com.example.dokkani.data.local.entities.PartyType
import com.example.dokkani.data.local.entities.PaymentMethod
import com.example.dokkani.data.local.entities.PaymentVoucherEntity
import com.example.dokkani.data.local.entities.ProductEntity
import com.example.dokkani.data.local.entities.ProductUnitEntity
import com.example.dokkani.data.local.entities.StockMovementEntity
import com.example.dokkani.data.local.entities.SystemSettingsEntity
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
        LicenseEntity::class
    ],
    version = 2,
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
                        // تنفيذ بذر البيانات داخل Transaction
                        populateInitialGroceryData(database)
                    }
                }
            }
        }

        private suspend fun populateInitialGroceryData(db: DokkaniDatabase) {

            // 0. Seed Default Admin
            val userDao = db.userDao()
            userDao.insertUser(
                UserEntity(
                    username = "admin",
                    fullName = "مدير النظام",
                    pinCode = "1234",
                    role = com.example.dokkani.data.local.entities.UserRole.ADMIN,
                    isActive = true
                )
            )

            // 1. إعدادات النظام الافتراضية
            val settingsDao = db.systemSettingsDao()
            settingsDao.insertOrUpdateSettings(
                SystemSettingsEntity(
                    id = 1,
                    storeName = "دكاني - تموينات ومخضار السعادة",
                    costValuationMethod = CostValuationMethod.WAC,
                    defaultCurrencyCode = "SAR",
                    defaultTaxRate = 0.15,
                    enableProduceShrinkageTracking = true,
                    enableNegativeStock = false,
                    invoiceFooterText = "شكراً لزيارتكم دكاني - تسوقكم يسعدنا!"
                )
            )

            // 2. العملات
            val currencyDao = db.currencyDao()
            val sarId = currencyDao.insertCurrency(
                CurrencyEntity(
                    code = "SAR", name = "ريال سعودي", symbol = "ر.س", exchangeRateToBase = 1.0, isBaseCurrency = true, isDefault = true
                )
            )
            currencyDao.insertCurrency(
                CurrencyEntity(
                    code = "USD", name = "دولار أمريكي", symbol = "$", exchangeRateToBase = 3.75, isBaseCurrency = false, isDefault = false
                )
            )
            currencyDao.insertCurrency(
                CurrencyEntity(
                    code = "EGP", name = "جنيه مصري", symbol = "ج.م", exchangeRateToBase = 0.076, isBaseCurrency = false, isDefault = false
                )
            )

            // 3. الأطراف (العملاء والموردين)
            val partyDao = db.partyDao()
            val suppVegetablesId = partyDao.insertParty(
                PartyEntity(
                    name = "مورد خضار سوق العزيزية المركزي", type = PartyType.SUPPLIER, phone = "0501234567", taxNumber = "300123456700003", currentBalance = -1200.0, notes = "توريد يومي فجر كل يوم لسحاحير الخضار المشكل"
                )
            )
            partyDao.insertParty(
                PartyEntity(
                    name = "شركة المراعي للألبان والعصائر", type = PartyType.SUPPLIER, phone = "0559876543", taxNumber = "300987654300003", currentBalance = 0.0, notes = "مندوب التوزيع يمر كل ثلاثاء وخميس"
                )
            )
            val custAbuAhmedId = partyDao.insertParty(
                PartyEntity(
                    name = "أبو أحمد (دفتر الحساب)", type = PartyType.CUSTOMER, phone = "0561122334", currentBalance = 345.50, creditLimit = 1000.0, notes = "عميل الحي يسدد في نهاية الشهر مع الراتب"
                )
            )

            // 4. المنتجات والوحدات
            val productDao = db.productDao()

            val tomatoId = productDao.insertProduct(
                ProductEntity(code = "PROD-TOMATO", name = "طماطم بلدي طازج", englishName = "Fresh Local Tomatoes", category = "خضار وفواكه", isWeighted = true, minStockAlert = 10.0)
            )
            val tomatoUnitKg = productDao.insertUnit(
                ProductUnitEntity(productId = tomatoId, unitName = "كيلو", conversionFactor = 1.0, barcode = "200001000000", costPrice = 3.50, sellingPrice = 5.50, isBaseUnit = true)
            )
            productDao.insertUnit(
                ProductUnitEntity(productId = tomatoId, unitName = "صندوق 10 كجم", conversionFactor = 10.0, barcode = "628200100010", costPrice = 32.0, sellingPrice = 48.0, isBaseUnit = false)
            )

            val cucumberId = productDao.insertProduct(
                ProductEntity(code = "PROD-CUCUMB", name = "خيار محلي صوبة", englishName = "Local Greenhouse Cucumbers", category = "خضار وفواكه", isWeighted = true, minStockAlert = 8.0)
            )
            productDao.insertUnit(
                ProductUnitEntity(productId = cucumberId, unitName = "كيلو", conversionFactor = 1.0, barcode = "200002000000", costPrice = 2.80, sellingPrice = 4.50, isBaseUnit = true)
            )

            val milkId = productDao.insertProduct(
                ProductEntity(code = "PROD-MILK-1L", name = "حليب كامل الدسم 1 لتر", englishName = "Full Cream Milk 1L", category = "ألبان وأجبان", isWeighted = false, minStockAlert = 12.0)
            )
            val milkUnitPiece = productDao.insertUnit(
                ProductUnitEntity(productId = milkId, unitName = "حبة", conversionFactor = 1.0, barcode = "6281007010015", costPrice = 5.20, sellingPrice = 6.50, isBaseUnit = true)
            )

            // 5. الحركات المخزونية
            val stockMovementDao = db.stockMovementDao()
            val now = System.currentTimeMillis()
            val oneDay = 86400000L

            stockMovementDao.insertMovement(
                StockMovementEntity(
                    productId = tomatoId, productUnitId = tomatoUnitKg, movementType = MovementType.PURCHASE_IN, quantityBaseUnit = 20.0, remainingQuantityForFifo = 12.0, unitCostPriceBase = 3.20, timestamp = now - (3 * oneDay), referenceNumber = "PUR-2026-081"
                )
            )

            // 6. الخضار المشكل
            val produceBatchDao = db.mixedProduceBatchDao()
            val sampleBatchId = produceBatchDao.insertBatch(
                MixedProduceBatchEntity(
                    batchNumber = "MIX-2026-001", date = now - (4 * 3600000L), supplierId = suppVegetablesId, sourceDescription = "سحارة خضار مشكل", totalGrossWeightKg = 25.0, totalPurchaseCost = 90.0, additionalExpense = 10.0, wasteWeightKg = 3.0, netSalableWeightKg = 22.0, wastePercentage = 12.0, effectiveCostPerKg = 4.545, targetProfitMarginPercent = 25.0, suggestedSalePricePerKg = 5.68, status = BatchStatus.SORTED
                )
            )

            // 7. الفواتير والسندات والمصروفات والشفتات
            val invoiceDao = db.invoiceDao()
            val sampleSaleId = invoiceDao.insertInvoice(
                InvoiceEntity(
                    invoiceNumber = "INV-2026-0001", type = InvoiceType.SALE, partyId = custAbuAhmedId, date = now - (2 * 3600000L), currencyId = sarId, exchangeRate = 1.0, subtotal = 38.0, taxRate = 0.15, taxAmount = 5.70, total = 43.70, paidAmount = 0.0, remainingAmount = 43.70, paymentMethod = PaymentMethod.CREDIT, status = InvoiceStatus.COMPLETED
                )
            )

            db.paymentVoucherDao().insertVoucher(
                PaymentVoucherEntity(
                    voucherNumber = "RCV-2026-0001", partyId = custAbuAhmedId, amount = 100.0, paymentMethod = PaymentMethod.CASH, date = now - oneDay, receivedBy = "كاشير 1"
                )
            )

            db.expenseDao().insertExpense(
                ExpenseEntity(
                    expenseNumber = "EXP-2026-0001", category = "كهرباء ومياه", amount = 280.0, paymentMethod = PaymentMethod.MADA, date = now - (2 * oneDay), paidTo = "الشركة السعودية للكهرباء", recordedBy = "المدير العام"
                )
            )

            db.cashShiftDao().insertShift(
                CashShiftEntity(
                    shiftNumber = "SHF-2026-0001", cashierName = "كاشير 1", startTime = now - (24 * 3600000L), endTime = now - (12 * 3600000L), openingCash = 200.0, totalCashSales = 540.0, totalCashCollections = 100.0, totalCashExpenses = 63.0, expectedCashInDrawer = 777.0, actualPhysicalCash = 777.0, cashDiscrepancy = 0.0, status = "CLOSED"
                )
            )
        }
    }
}
