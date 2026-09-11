package com.example.dokkani.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
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
 * تشمل جميع الجداول والمخطط المحاسبي الكامل
 */
@Database(
    entities = [
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
        com.example.dokkani.data.local.entities.LicenseEntity::class
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

    companion object {
        @Volatile
        private var INSTANCE: DokkaniDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): DokkaniDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DokkaniDatabase::class.java,
                    "dokkani_pos_database"
                )
                    .addCallback(DokkaniDatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    /**
     * بذر البيانات الأولية للمتجر (إعدادات، عملات، أصناف البقالة، حركات الشراء لاختبار WAC/FIFO، وجرد الخضار المشكل)
     */
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

        private suspend fun populateInitialGroceryData(db: DokkaniDatabase) {
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

            // 2. العملات وأسعار الصرف
            val currencyDao = db.currencyDao()
            val sarId = currencyDao.insertCurrency(
                CurrencyEntity(
                    code = "SAR",
                    name = "ريال سعودي",
                    symbol = "ر.س",
                    exchangeRateToBase = 1.0,
                    isBaseCurrency = true,
                    isDefault = true
                )
            )
            currencyDao.insertCurrency(
                CurrencyEntity(
                    code = "USD",
                    name = "دولار أمريكي",
                    symbol = "$",
                    exchangeRateToBase = 3.75,
                    isBaseCurrency = false,
                    isDefault = false
                )
            )
            currencyDao.insertCurrency(
                CurrencyEntity(
                    code = "EGP",
                    name = "جنيه مصري",
                    symbol = "ج.م",
                    exchangeRateToBase = 0.076,
                    isBaseCurrency = false,
                    isDefault = false
                )
            )

            // 3. العملاء والموردين
            val partyDao = db.partyDao()
            val suppVegetablesId = partyDao.insertParty(
                PartyEntity(
                    name = "مورد خضار سوق العزيزية المركزي",
                    type = PartyType.SUPPLIER,
                    phone = "0501234567",
                    taxNumber = "300123456700003",
                    currentBalance = -1200.0, // دائن (نحن ندين له بـ 1200 ريال)
                    notes = "توريد يومي فجر كل يوم لسحاحير الخضار المشكل"
                )
            )
            val suppDairyId = partyDao.insertParty(
                PartyEntity(
                    name = "شركة المراعي للألبان والعصائر",
                    type = PartyType.SUPPLIER,
                    phone = "0559876543",
                    taxNumber = "300987654300003",
                    currentBalance = 0.0,
                    notes = "مندوب التوزيع يمر كل ثلاثاء وخميس"
                )
            )
            val custAbuAhmedId = partyDao.insertParty(
                PartyEntity(
                    name = "أبو أحمد (دفتر الحساب)",
                    type = PartyType.CUSTOMER,
                    phone = "0561122334",
                    currentBalance = 345.50, // مدين (عليه حساب 345.50 ريال)
                    creditLimit = 1000.0,
                    notes = "عميل الحي يسدد في نهاية الشهر مع الراتب"
                )
            )

            // 4. الأصناف والوحدات المتعددة
            val productDao = db.productDao()

            // صنف 1: طماطم بلدي (خضار - ميزان)
            val tomatoId = productDao.insertProduct(
                ProductEntity(
                    code = "PROD-TOMATO",
                    name = "طماطم بلدي طازج",
                    englishName = "Fresh Local Tomatoes",
                    category = "خضار وفواكه",
                    isWeighted = true,
                    minStockAlert = 10.0
                )
            )
            val tomatoUnitKg = productDao.insertUnit(
                ProductUnitEntity(
                    productId = tomatoId,
                    unitName = "كيلو",
                    conversionFactor = 1.0,
                    barcode = "200001000000",
                    costPrice = 3.50,
                    sellingPrice = 5.50,
                    isBaseUnit = true
                )
            )
            productDao.insertUnit(
                ProductUnitEntity(
                    productId = tomatoId,
                    unitName = "صندوق 10 كجم",
                    conversionFactor = 10.0,
                    barcode = "628200100010",
                    costPrice = 32.0,
                    sellingPrice = 48.0,
                    isBaseUnit = false
                )
            )

            // صنف 2: خيار محلي (خضار - ميزان)
            val cucumberId = productDao.insertProduct(
                ProductEntity(
                    code = "PROD-CUCUMB",
                    name = "خيار محلي صوبة",
                    englishName = "Local Greenhouse Cucumbers",
                    category = "خضار وفواكه",
                    isWeighted = true,
                    minStockAlert = 8.0
                )
            )
            val cucumberUnitKg = productDao.insertUnit(
                ProductUnitEntity(
                    productId = cucumberId,
                    unitName = "كيلو",
                    conversionFactor = 1.0,
                    barcode = "200002000000",
                    costPrice = 2.80,
                    sellingPrice = 4.50,
                    isBaseUnit = true
                )
            )
            productDao.insertUnit(
                ProductUnitEntity(
                    productId = cucumberId,
                    unitName = "سحارة 12 كجم",
                    conversionFactor = 12.0,
                    barcode = "628200200012",
                    costPrice = 30.0,
                    sellingPrice = 45.0,
                    isBaseUnit = false
                )
            )

            // صنف 3: حليب كامل الدسم 1 لتر (ألبان - حبة وكرتون)
            val milkId = productDao.insertProduct(
                ProductEntity(
                    code = "PROD-MILK-1L",
                    name = "حليب كامل الدسم 1 لتر",
                    englishName = "Full Cream Milk 1L",
                    category = "ألبان وأجبان",
                    isWeighted = false,
                    minStockAlert = 12.0
                )
            )
            val milkUnitPiece = productDao.insertUnit(
                ProductUnitEntity(
                    productId = milkId,
                    unitName = "حبة",
                    conversionFactor = 1.0,
                    barcode = "6281007010015",
                    costPrice = 5.20,
                    sellingPrice = 6.50,
                    isBaseUnit = true
                )
            )
            productDao.insertUnit(
                ProductUnitEntity(
                    productId = milkId,
                    unitName = "كرتون (12 حبة)",
                    conversionFactor = 12.0,
                    barcode = "6281007010121",
                    costPrice = 58.0,
                    sellingPrice = 72.0,
                    isBaseUnit = false
                )
            )

            // صنف 4: أرز بسمتي هندي 5 كجم (بقوليات وحبوب)
            val riceId = productDao.insertProduct(
                ProductEntity(
                    code = "PROD-RICE-5KG",
                    name = "أرز بسمتي هندي كلاسيك 5 كجم",
                    englishName = "Basmati Rice 5kg",
                    category = "حبوب وبقوليات",
                    isWeighted = false,
                    minStockAlert = 4.0
                )
            )
            productDao.insertUnit(
                ProductUnitEntity(
                    productId = riceId,
                    unitName = "كيس 5 كجم",
                    conversionFactor = 1.0,
                    barcode = "8901234567890",
                    costPrice = 38.0,
                    sellingPrice = 48.0,
                    isBaseUnit = true
                )
            )
            productDao.insertUnit(
                ProductUnitEntity(
                    productId = riceId,
                    unitName = "شدة (4 أكياس)",
                    conversionFactor = 4.0,
                    barcode = "8901234567894",
                    costPrice = 145.0,
                    sellingPrice = 180.0,
                    isBaseUnit = false
                )
            )

            // صنف 5: خبز صامولي طازج (مخبوزات - سريع بدون باركود)
            val breadId = productDao.insertProduct(
                ProductEntity(
                    code = "0005",
                    name = "خبز صامولي (كيس 5 حبات)",
                    englishName = "Fresh Samoli Bread",
                    category = "مخبوزات",
                    isWeighted = false,
                    minStockAlert = 20.0
                )
            )
            productDao.insertUnit(
                ProductUnitEntity(
                    productId = breadId,
                    unitName = "كيس",
                    conversionFactor = 1.0,
                    barcode = "10005",
                    costPrice = 1.00,
                    sellingPrice = 1.50,
                    isBaseUnit = true
                )
            )

            // صنف 6: تميس عادي طازج (مخبوزات - سريع بدون باركود)
            val tameesId = productDao.insertProduct(
                ProductEntity(
                    code = "0006",
                    name = "تميس عادي طازج",
                    englishName = "Fresh Tamees Bread",
                    category = "مخبوزات",
                    isWeighted = false,
                    minStockAlert = 15.0
                )
            )
            productDao.insertUnit(
                ProductUnitEntity(
                    productId = tameesId,
                    unitName = "قرص",
                    conversionFactor = 1.0,
                    barcode = "10006",
                    costPrice = 0.60,
                    sellingPrice = 1.00,
                    isBaseUnit = true
                )
            )

            // صنف 7: حزمة بقدونس / كزبرة طازجة (خضار ورقيات - سريع)
            val herbsId = productDao.insertProduct(
                ProductEntity(
                    code = "0007",
                    name = "حزمة ورقيات (بقدونس/كزبرة)",
                    englishName = "Fresh Herbs Bunch",
                    category = "خضار وفواكه",
                    isWeighted = false,
                    minStockAlert = 10.0
                )
            )
            productDao.insertUnit(
                ProductUnitEntity(
                    productId = herbsId,
                    unitName = "حزمة",
                    conversionFactor = 1.0,
                    barcode = "10007",
                    costPrice = 0.50,
                    sellingPrice = 1.00,
                    isBaseUnit = true
                )
            )

            // صنف 8: ماء صحة 330 مل (مشروبات - سريع)
            val waterId = productDao.insertProduct(
                ProductEntity(
                    code = "0008",
                    name = "ماء صحة ميني 330 مل",
                    englishName = "Bottled Water 330ml",
                    category = "مشروبات",
                    isWeighted = false,
                    minStockAlert = 24.0
                )
            )
            productDao.insertUnit(
                ProductUnitEntity(
                    productId = waterId,
                    unitName = "قارورة",
                    conversionFactor = 1.0,
                    barcode = "10008",
                    costPrice = 0.60,
                    sellingPrice = 1.00,
                    isBaseUnit = true
                )
            )

            // 5. حركات مخزون سابقة بأسعار مختلفة لاختبار WAC و FIFO و Last Purchase Price عملياً
            val stockMovementDao = db.stockMovementDao()
            val now = System.currentTimeMillis()
            val oneDay = 86400000L

            // شحنات الطماطم:
            // دفعة 1 (قبل 3 أيام): 20 كجم بسعر 3.20 ر.س
            stockMovementDao.insertMovement(
                StockMovementEntity(
                    productId = tomatoId,
                    productUnitId = tomatoUnitKg,
                    movementType = MovementType.PURCHASE_IN,
                    quantityBaseUnit = 20.0,
                    remainingQuantityForFifo = 12.0, // تم بيع 8 كجم منها
                    unitCostPriceBase = 3.20,
                    timestamp = now - (3 * oneDay),
                    referenceNumber = "PUR-2026-081",
                    notes = "توريد دفعة أولى من سوق العزيزية"
                )
            )

            // دفعة 2 (قبل يومين): 30 كجم بسعر 4.00 ر.س (ارتفاع في السوق)
            stockMovementDao.insertMovement(
                StockMovementEntity(
                    productId = tomatoId,
                    productUnitId = tomatoUnitKg,
                    movementType = MovementType.PURCHASE_IN,
                    quantityBaseUnit = 30.0,
                    remainingQuantityForFifo = 30.0,
                    unitCostPriceBase = 4.00,
                    timestamp = now - (2 * oneDay),
                    referenceNumber = "PUR-2026-089",
                    notes = "توريد دفعة ثانية بأسعار أعلى"
                )
            )

            // دفعة 3 (أمس): 15 كجم بسعر 3.80 ر.س (آخر سعر شراء)
            stockMovementDao.insertMovement(
                StockMovementEntity(
                    productId = tomatoId,
                    productUnitId = tomatoUnitKg,
                    movementType = MovementType.PURCHASE_IN,
                    quantityBaseUnit = 15.0,
                    remainingQuantityForFifo = 15.0,
                    unitCostPriceBase = 3.80,
                    timestamp = now - oneDay,
                    referenceNumber = "PUR-2026-095",
                    notes = "آخر دفعة شراء واردة للمحل"
                )
            )

            // حركات الحليب:
            stockMovementDao.insertMovement(
                StockMovementEntity(
                    productId = milkId,
                    productUnitId = milkUnitPiece,
                    movementType = MovementType.PURCHASE_IN,
                    quantityBaseUnit = 48.0, // 4 كراتين
                    remainingQuantityForFifo = 36.0,
                    unitCostPriceBase = 4.833, // 58 / 12
                    timestamp = now - (2 * oneDay),
                    referenceNumber = "PUR-MARAI-101",
                    notes = "توريد شركة المراعي"
                )
            )

            // 6. جرد سريع لخضار المشكل (Mixed Produce Batch)
            val produceBatchDao = db.mixedProduceBatchDao()
            val sampleBatchId = produceBatchDao.insertBatch(
                MixedProduceBatchEntity(
                    batchNumber = "MIX-2026-001",
                    date = now - (4 * 3600000L),
                    supplierId = suppVegetablesId,
                    sourceDescription = "سحارة خضار مشكل (طماطم + خيار + فلفل بارد) - حلقة العزيزية",
                    totalGrossWeightKg = 25.0,
                    totalPurchaseCost = 90.0,
                    additionalExpense = 10.0, // عتالة ونقل
                    wasteWeightKg = 3.0,     // 3 كجم تالف/هدر
                    netSalableWeightKg = 22.0,// 22 كجم صافي
                    wastePercentage = 12.0,   // 12%
                    effectiveCostPerKg = 4.545, // 100 / 22
                    targetProfitMarginPercent = 25.0,
                    suggestedSalePricePerKg = 5.68,
                    status = BatchStatus.SORTED,
                    notes = "تم فرز السحارة وتجنيب 3 كجم تالف بسبب رطوبة الصندوق"
                )
            )

            // بنود الفرز للسحارة المشكلة
            produceBatchDao.insertYieldItems(
                listOf(
                    MixedProduceYieldItemEntity(
                        batchId = sampleBatchId,
                        productId = tomatoId,
                        productName = "طماطم بلدي فرز درجة أولى",
                        sortedWeightKg = 12.0,
                        costAllocationRatio = 1.0,
                        calculatedCostPerKg = 4.545,
                        targetSellingPricePerKg = 5.75,
                        expectedRevenue = 69.0
                    ),
                    MixedProduceYieldItemEntity(
                        batchId = sampleBatchId,
                        productId = cucumberId,
                        productName = "خيار بلدي فرز درجة ثانية",
                        sortedWeightKg = 10.0,
                        costAllocationRatio = 1.0,
                        calculatedCostPerKg = 4.545,
                        targetSellingPricePerKg = 5.50,
                        expectedRevenue = 55.0
                    )
                )
            )

            // 7. فاتورة مبيعات نموذجية
            val invoiceDao = db.invoiceDao()
            val sampleSaleId = invoiceDao.insertInvoice(
                InvoiceEntity(
                    invoiceNumber = "INV-2026-0001",
                    type = InvoiceType.SALE,
                    partyId = custAbuAhmedId,
                    date = now - (2 * 3600000L),
                    currencyId = sarId,
                    exchangeRate = 1.0,
                    subtotal = 38.0,
                    discount = 0.0,
                    taxRate = 0.15,
                    taxAmount = 5.70,
                    total = 43.70,
                    paidAmount = 0.0,
                    remainingAmount = 43.70,
                    paymentMethod = PaymentMethod.CREDIT,
                    status = InvoiceStatus.COMPLETED,
                    notes = "مشتريات مسائية على الحساب لأبو أحمد"
                )
            )

            invoiceDao.insertInvoiceItems(
                listOf(
                    InvoiceItemEntity(
                        invoiceId = sampleSaleId,
                        productId = milkId,
                        productUnitId = milkUnitPiece,
                        quantity = 2.0,
                        unitConversionFactor = 1.0,
                        unitCostPrice = 4.833,
                        unitSellingPrice = 6.50,
                        totalPrice = 13.0
                    ),
                    InvoiceItemEntity(
                        invoiceId = sampleSaleId,
                        productId = tomatoId,
                        productUnitId = tomatoUnitKg,
                        quantity = 3.0,
                        unitConversionFactor = 1.0,
                        unitCostPrice = 3.70,
                        unitSellingPrice = 5.50,
                        totalPrice = 16.50
                    )
                )
            )

            // 8. سندات قبض سابقة (دفتر الشكك)
            val voucherDao = db.paymentVoucherDao()
            voucherDao.insertVoucher(
                PaymentVoucherEntity(
                    voucherNumber = "RCV-2026-0001",
                    partyId = custAbuAhmedId,
                    amount = 100.0,
                    paymentMethod = PaymentMethod.CASH,
                    date = now - (1 * oneDay),
                    receivedBy = "كاشير 1",
                    notes = "سداد دفعة نقدية على الحساب من أبو أحمد"
                )
            )

            // 9. مصروفات تشغيلية ونثريات أولية
            val expenseDao = db.expenseDao()
            expenseDao.insertExpense(
                ExpenseEntity(
                    expenseNumber = "EXP-2026-0001",
                    category = "كهرباء ومياه",
                    amount = 280.0,
                    paymentMethod = PaymentMethod.MADA,
                    date = now - (2 * oneDay),
                    paidTo = "الشركة السعودية للكهرباء",
                    notes = "فاتورة استهلاك كهرباء ثلاجات المتجر لشهر مارس",
                    recordedBy = "المدير العام"
                )
            )
            expenseDao.insertExpense(
                ExpenseEntity(
                    expenseNumber = "EXP-2026-0002",
                    category = "نظافة ومستلزمات",
                    amount = 45.0,
                    paymentMethod = PaymentMethod.CASH,
                    date = now - (4 * 3600000L),
                    paidTo = "مؤسسة المستلزمات البلاستيكية",
                    notes = "شراء رولات أكياس بقالة ومطهرات أرضيات",
                    recordedBy = "كاشير 1"
                )
            )
            expenseDao.insertExpense(
                ExpenseEntity(
                    expenseNumber = "EXP-2026-0003",
                    category = "بوفية وضيافة",
                    amount = 18.0,
                    paymentMethod = PaymentMethod.CASH,
                    date = now - (2 * 3600000L),
                    paidTo = "بوفية الحي",
                    notes = "شاي وقهوة ضيافة للكاشير والعملاء",
                    recordedBy = "كاشير 1"
                )
            )

            // 10. شفت صندوق سابق تم إغلاقه ومطابقته
            val cashShiftDao = db.cashShiftDao()
            cashShiftDao.insertShift(
                CashShiftEntity(
                    shiftNumber = "SHF-2026-0001",
                    cashierName = "كاشير 1",
                    startTime = now - (24 * 3600000L),
                    endTime = now - (12 * 3600000L),
                    openingCash = 200.0,
                    totalCashSales = 540.0,
                    totalCashCollections = 100.0,
                    totalCashExpenses = 63.0,
                    expectedCashInDrawer = 777.0,
                    actualPhysicalCash = 777.0,
                    cashDiscrepancy = 0.0,
                    status = "CLOSED",
                    notes = "تم تسليم الشفت الصباحي بنجاح، ومطابقة النقدية تامة بدون عجز أو زيادة."
                )
            )
        }
    }
}
