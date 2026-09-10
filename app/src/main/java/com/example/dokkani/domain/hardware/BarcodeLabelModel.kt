package com.example.dokkani.domain.hardware

/**
 * مقاسات ورق ملصقات الباركود الشائعة في طابعات الملصقات الحرارية المحمولة والمكتبية
 */
enum class LabelPaperSize(
    val widthMm: Int,
    val heightMm: Int,
    val labelArabic: String,
    val dpi: Int = 203 // الدقة القياسية لطابعات الملصقات الحرارية (8 dots/mm)
) {
    SIZE_38X25(38, 25, "38 × 25 مم (الملصق القياسي للبقالات والرفوف)"),
    SIZE_40X30(40, 30, "40 × 30 مم (ملصق متوسط مع تفاصيل السعر)"),
    SIZE_50X30(50, 30, "50 × 30 مم (ملصق عريض للأصناف والكراتين)"),
    SIZE_58X40(58, 40, "58 × 40 مم (ملصق كبير للمستودعات والسحاحير)");

    val dotsWidth: Int get() = (widthMm * 8) // 203 DPI = ~8 dots per mm
    val dotsHeight: Int get() = (heightMm * 8)
}

/**
 * بيانات ملصق الباركود المراد تصميمه وطباعته
 */
data class BarcodeLabelData(
    val storeName: String,
    val productName: String,
    val unitName: String,
    val barcode: String,
    val price: Double,
    val currencySymbol: String = "ر.س",
    val isPriceInclusiveTax: Boolean = true,
    val taxRatePercent: Double = 15.0,
    val size: LabelPaperSize = LabelPaperSize.SIZE_38X25,
    val copies: Int = 1,
    val showStoreName: Boolean = true,
    val showUnitName: Boolean = true,
    val showPrice: Boolean = true,
    val showBarcodeText: Boolean = true,
    val showTaxNote: Boolean = true,
    val additionalNote: String = ""
)

/**
 * نتيجة معالجة أمر طباعة الملصق
 */
data class LabelPrintResult(
    val success: Boolean,
    val message: String,
    val isSimulated: Boolean,
    val copiesPrinted: Int,
    val tsplCommands: String,
    val rawBytes: ByteArray,
    val timestamp: Long = System.currentTimeMillis()
)
