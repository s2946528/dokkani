package com.example.dokkani.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * دالة وأدوات تنسيق مركزية للأرقام والأسعار والكميات (Global Formatter Utility)
 * تضمن التحكم الإجمالي في إظهار أو إخفاء الكسور العشرية عبر كامل التطبيق.
 */
object NumberFormatter {

    /**
     * تنسيق رقم محدد (Double) بناءً على إعداد الكسور العشرية.
     * @param value القيمة الرقمية المراد تنسيقها.
     * @param showDecimals إذا كانت false (الافتراضي): يعرض كعدد صحيح بدون كسور (مثال: 5000)
     *                      إذا كانت true: يعرض بالكسور العشرية بمرتبتين (مثال: 5000.00)
     */
    fun formatNumber(value: Double, showDecimals: Boolean = false): String {
        return if (showDecimals) {
            val symbols = DecimalFormatSymbols(Locale.US)
            val formatter = DecimalFormat("#,##0.00", symbols)
            formatter.format(value)
        } else {
            val symbols = DecimalFormatSymbols(Locale.US)
            val formatter = DecimalFormat("#,##0", symbols)
            formatter.format(Math.round(value).toDouble())
        }
    }

    /**
     * تنسيق سعر أو مبلغ مالي مع إضافة رمز العملة.
     */
    fun formatCurrency(value: Double, showDecimals: Boolean = false, currencySymbol: String = "ر.س"): String {
        val formattedNumber = formatNumber(value, showDecimals)
        return if (currencySymbol.isNotBlank()) "$formattedNumber $currencySymbol" else formattedNumber
    }

    /**
     * تنسيق الكمية بناءً على إعداد الكسور العشرية.
     */
    fun formatQuantity(value: Double, showDecimals: Boolean = false): String {
        return if (showDecimals) {
            val symbols = DecimalFormatSymbols(Locale.US)
            val formatter = DecimalFormat("#,##0.00", symbols)
            formatter.format(value)
        } else {
            if (value % 1.0 == 0.0) {
                val symbols = DecimalFormatSymbols(Locale.US)
                val formatter = DecimalFormat("#,##0", symbols)
                formatter.format(value)
            } else {
                // إذا كانت الكمية تحتوي على جزء عشري للمنتجات بالوزن (مثال: 1.5 كجم)
                val symbols = DecimalFormatSymbols(Locale.US)
                val formatter = DecimalFormat("#,##0.##", symbols)
                formatter.format(value)
            }
        }
    }
}

// Extension Functions لسهولة وتوحيد الاستخدام على مستوى الكود المصدري للتطبيق
fun Double.formatAmount(showDecimals: Boolean = false): String {
    return NumberFormatter.formatNumber(this, showDecimals)
}

fun Double.formatCurrency(showDecimals: Boolean = false, symbol: String = "ر.س"): String {
    return NumberFormatter.formatCurrency(this, showDecimals, symbol)
}

fun Double.formatQuantity(showDecimals: Boolean = false): String {
    return NumberFormatter.formatQuantity(this, showDecimals)
}

fun Float.formatAmount(showDecimals: Boolean = false): String {
    return NumberFormatter.formatNumber(this.toDouble(), showDecimals)
}

fun Int.formatAmount(showDecimals: Boolean = false): String {
    return NumberFormatter.formatNumber(this.toDouble(), showDecimals)
}
