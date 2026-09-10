package com.example.dokkani.domain.barcode

/**
 * نتيجة فك تشفير باركود ميزان الباركود الإلكتروني (Variable Weight / Price Barcode)
 */
data class ScaleBarcodeResult(
    val isValidScaleBarcode: Boolean,
    val rawBarcode: String,
    val prefix: String = "",
    val productCode: String = "",
    val weightKg: Double? = null,
    val embeddedPrice: Double? = null,
    val checksum: String = "",
    val explanationArabic: String = "",
    val mode: ScaleBarcodeMode = ScaleBarcodeMode.WEIGHT_BASED
)

enum class ScaleBarcodeMode(val labelArabic: String) {
    WEIGHT_BASED("موزون بالجرام/الكيلو"),
    PRICE_BASED("مضمن السعر الإجمالي")
}

/**
 * محرك فك تشفير الباركود المتغير الخاص بموازين الباركود الإلكترونية لمتاجر التجزئة والبقالات
 * يدعم بشكل أساسي البادئة 21 (البروتوكول القياسي للبقالات) مع دعم البادئات البديلة 20 و 02.
 *
 * صيغة EAN-13 القياسية للميزان (13 خانة):
 * [PP] [CCCC] [WWWWW] [K]
 * PP: بادئة الميزان (مثل: 21)
 * CCCC أو CCCCC: كود الصنف / SKU (مثل: 0004)
 * WWWWW: الوزن بالجرامات (مثلاً 01750 = 1.750 كجم) أو السعر بالهللات (01500 = 15.00 ر.س)
 * K: خانة التحقق (Checksum)
 */
object ScaleBarcodeParser {

    private val SUPPORTED_PREFIXES = listOf("21", "20", "02")

    /**
     * فحص هل الباركود يبدأ ببادئة ميزان إلكتروني
     */
    fun isScaleBarcode(barcode: String): Boolean {
        val clean = barcode.trim()
        return clean.length in 12..13 && SUPPORTED_PREFIXES.any { clean.startsWith(it) }
    }

    /**
     * فك تشفير الباركود واستخراج كود الصنف والوزن/السعر
     */
    fun parse(barcode: String, assumeWeightBased: Boolean = true): ScaleBarcodeResult {
        val clean = barcode.trim()

        if (!isScaleBarcode(clean)) {
            return ScaleBarcodeResult(
                isValidScaleBarcode = false,
                rawBarcode = clean,
                explanationArabic = "الباركود ليس باركود ميزان متغير (يجب أن يتكون من 12-13 خانة ويبدأ بالبادئة 21 أو 20)"
            )
        }

        val prefix = clean.substring(0, 2)
        val body = clean.substring(2)

        // في المعيار الأكثر شيوعاً:
        // إذا كان الباركود 13 خانة:
        // [0..1]: بادئة 21
        // [2..5] أو [2..6]: كود الصنف (4 أو 5 أرقام)
        // [6..10] أو [7..11]: القيمة (الوزن أو السعر)
        // [12]: Checksum

        return try {
            val productCode: String
            val valueDigits: String
            val checksum = clean.takeLast(1)

            if (clean.length == 13) {
                // النمط الشائع: 2 خانات بادئة + 5 خانات كود صنف + 5 خانات وزن/سعر + 1 خانة تحقق
                // أو 2 خانات بادئة + 4 خانات كود صنف + 6 خانات وزن
                // سنجعلها مرنة: 4 أو 5 خانات لكود الصنف
                // النمط القياسي للبقالات السعودية: 21 + 4 أرقام كود الصنف + 5 أرقام وزن (بالجرام) + 1 أو 2 تحقق
                productCode = clean.substring(2, 6) // خانات 2,3,4,5 (مثال: 0004)
                valueDigits = clean.substring(6, 11) // خانات 6,7,8,9,10 (مثال: 01750)
            } else {
                // 12 خانة
                productCode = clean.substring(2, 6)
                valueDigits = clean.substring(6, 11)
            }

            val numericValue = valueDigits.toDoubleOrNull() ?: 0.0

            if (assumeWeightBased) {
                // القيمة تمثل وزناً بالجرام (01750 جرام = 1.750 كجم)
                val weightKg = numericValue / 1000.0
                ScaleBarcodeResult(
                    isValidScaleBarcode = true,
                    rawBarcode = clean,
                    prefix = prefix,
                    productCode = productCode,
                    weightKg = weightKg,
                    embeddedPrice = null,
                    checksum = checksum,
                    mode = ScaleBarcodeMode.WEIGHT_BASED,
                    explanationArabic = "تم فك تشفير باركود ميزان إلكتروني: بادئة ($prefix) - كود الصنف ($productCode) - الوزن المستخرج: ($weightKg كجم) من القيمة ($valueDigits جم)"
                )
            } else {
                // القيمة تمثل السعر الإجمالي بالهللات/القروش (مثال: 01575 = 15.75 ر.س)
                val price = numericValue / 100.0
                ScaleBarcodeResult(
                    isValidScaleBarcode = true,
                    rawBarcode = clean,
                    prefix = prefix,
                    productCode = productCode,
                    weightKg = null,
                    embeddedPrice = price,
                    checksum = checksum,
                    mode = ScaleBarcodeMode.PRICE_BASED,
                    explanationArabic = "تم فك تشفير باركود ميزان إلكتروني: بادئة ($prefix) - كود الصنف ($productCode) - السعر المضمن: ($price ر.س)"
                )
            }
        } catch (e: Exception) {
            ScaleBarcodeResult(
                isValidScaleBarcode = false,
                rawBarcode = clean,
                explanationArabic = "تعذر استخراج بيانات الميزان: ${e.message}"
            )
        }
    }

    /**
     * محاكي لتوليد باركود ميزان إلكتروني لاختبار المنظومة
     * مثال: صنف كوده "0004" ووزنه 1.750 كجم يولد: 210004017508
     */
    fun generateSimulatedScaleBarcode(productCode: String, weightKg: Double, prefix: String = "21"): String {
        val cleanCode = productCode.filter { it.isDigit() }.padStart(4, '0').takeLast(4)
        val grams = (weightKg * 1000).toInt().coerceIn(1, 99999).toString().padStart(5, '0')
        val partial = "$prefix$cleanCode$grams"
        val checkDigit = calculateEanChecksum(partial)
        return "$partial$checkDigit"
    }

    private fun calculateEanChecksum(twelveDigits: String): Int {
        var sum = 0
        twelveDigits.forEachIndexed { index, c ->
            val digit = c.digitToIntOrNull() ?: 0
            sum += if (index % 2 == 0) digit else digit * 3
        }
        val remainder = sum % 10
        return if (remainder == 0) 0 else 10 - remainder
    }
}
