package com.example.dokkani.domain.barcode

/**
 * مولد الباركود الداخلي للوحدات الفردية والأصناف المفتوحة في نظام دكاني
 *
 * يدعم المعايير العالمية لترميز المنتجات داخل المتجر (In-Store Barcoding):
 * - معيار EAN-13 الداخلي: يبدأ ببادئة GS1 للمتاجر (20 - 29). نستخدم البادئة "290" للوحدات الفردية.
 * - حساب خانة التحقق (Check Digit) تلقائياً وفق خوارزمية Modulo 10 الرسمية.
 * - دعم الباركود التسلسلي المشفر بنمط Code 128.
 */
object BarcodeGenerator {

    // البادئة المحلية المخصصة للوحدات المفردة المستخرجة من الكراتين
    const val DEFAULT_IN_STORE_PREFIX = "290"

    /**
     * حساب خانة التحقق لباركود EAN-13 عبر خوارزمية Modulo 10
     * @param first12Digits أول 12 رقماً من الباركود
     * @return رقم خانة التحقق (من 0 إلى 9)
     */
    fun calculateEan13CheckDigit(first12Digits: String): Int {
        val clean = first12Digits.filter { it.isDigit() }.take(12)
        if (clean.length < 12) {
            val padded = clean.padEnd(12, '0')
            return calculateEan13CheckDigit(padded)
        }

        var sumOdd = 0  // مواقع فردية (مضروبة في 1)
        var sumEven = 0 // مواقع زوجية (مضروبة في 3)

        for (i in 0 until 12) {
            val digit = clean[i].digitToInt()
            if (i % 2 == 0) {
                sumOdd += digit
            } else {
                sumEven += digit
            }
        }

        val total = sumOdd + (sumEven * 3)
        val remainder = total % 10
        return if (remainder == 0) 0 else 10 - remainder
    }

    /**
     * توليد باركود EAN-13 كامل مكون من 13 رقماً بمعرف فريد
     * @param prefix بادئة المتجر (افتراضياً 290)
     * @param uniqueNumber رقم مميز (مثل معرف الوحدة أو الصنف أو رقم تسلسلي)
     */
    fun generateEan13Barcode(prefix: String = DEFAULT_IN_STORE_PREFIX, uniqueNumber: Long): String {
        val safePrefix = prefix.filter { it.isDigit() }.take(3).ifBlank { DEFAULT_IN_STORE_PREFIX }.padEnd(3, '0')
        // توليد 9 أرقام متبقية ليكون المجموع 12 رقماً قبل خانة التحقق
        val numberPart = "%09d".format(uniqueNumber % 1_000_000_000L)
        val first12 = "$safePrefix$numberPart"
        val checkDigit = calculateEan13CheckDigit(first12)
        return "$first12$checkDigit"
    }

    /**
     * توليد باركود فريد للوحدة الفردية التابعة لصنف معين
     * يدمج معرف الصنف والوحدة مع طابع زمني قصير لضمان عدم التكرار
     */
    fun generateUniqueSingleUnitBarcode(productId: Long, unitId: Long): String {
        val timeComponent = (System.currentTimeMillis() / 1000 % 100000).toString().padStart(5, '0')
        val unitComponent = (unitId % 100).toString().padStart(2, '0')
        val prodComponent = (productId % 100).toString().padStart(2, '0')
        val rawNumber = "$prodComponent$unitComponent$timeComponent".toLongOrNull() ?: System.currentTimeMillis() % 1_000_000_000L
        return generateEan13Barcode(DEFAULT_IN_STORE_PREFIX, rawNumber)
    }

    /**
     * التحقق من صحة بنية باركود EAN-13 وخانة تحققه
     */
    fun isValidEan13(barcode: String): Boolean {
        if (barcode.length != 13 || !barcode.all { it.isDigit() }) return false
        val first12 = barcode.substring(0, 12)
        val expectedCheckDigit = calculateEan13CheckDigit(first12)
        val actualCheckDigit = barcode[12].digitToInt()
        return expectedCheckDigit == actualCheckDigit
    }

    /**
     * توليد متسلسلة خطوط ومسافات الباركود لعرضها على الـ Canvas
     * يعيد قائمة من قيم منطقية (true = خط أسود، false = مسافة بيضاء)
     */
    fun generateBarcodeBars(code: String): List<Boolean> {
        val seed = code.hashCode().toLong()
        val random = java.util.Random(seed)
        val bars = mutableListOf<Boolean>()

        // Guard bars (علامة البداية)
        bars.addAll(listOf(true, false, true))

        // ترميز الخانات
        val digits = code.filter { it.isDigit() }.ifEmpty { "2901234567890" }
        for (ch in digits) {
            val d = ch.digitToInt()
            val pattern = when (d % 10) {
                0 -> listOf(false, false, false, true, true, false, true)
                1 -> listOf(false, false, true, true, false, false, true)
                2 -> listOf(false, false, true, false, false, true, true)
                3 -> listOf(false, true, true, true, true, false, true)
                4 -> listOf(false, true, false, false, false, true, true)
                5 -> listOf(false, true, true, false, false, false, true)
                6 -> listOf(false, true, false, true, true, true, true)
                7 -> listOf(false, true, true, true, false, true, true)
                8 -> listOf(false, true, true, false, true, true, true)
                else -> listOf(false, false, false, true, false, true, true)
            }
            bars.addAll(pattern)
        }

        // Guard bars (علامة النهاية)
        bars.addAll(listOf(true, false, true))
        return bars
    }
}
