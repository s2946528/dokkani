package com.example.dokkani.domain.pos

import com.example.dokkani.data.local.entities.PaymentMethod
import java.util.UUID

/**
 * بند في سلة المبيعات لنقطة البيع (POS Cart Item)
 */
data class PosCartItem(
    val cartItemId: String = UUID.randomUUID().toString(),
    val productId: Long,
    val productName: String,
    val productCode: String,
    val unitId: Long,
    val unitName: String,
    val conversionFactor: Double = 1.0,
    val unitPrice: Double,
    val costPrice: Double,
    val quantity: Double = 1.0,
    val isWeighted: Boolean = false,
    val isCustomOpenPrice: Boolean = false,
    val discount: Double = 0.0,
    val scaleBarcodeRaw: String? = null
) {
    val totalPrice: Double
        get() = ((quantity * unitPrice) - discount).coerceAtLeast(0.0)

    val totalCost: Double
        get() = quantity * (costPrice * conversionFactor)

    val grossProfit: Double
        get() = totalPrice - totalCost

    val quantityFormatted: String
        get() = if (isWeighted) {
            "%.3f كجم".format(quantity)
        } else {
            if (quantity % 1.0 == 0.0) "%.0f %s".format(quantity, unitName)
            else "%.2f %s".format(quantity, unitName)
        }
}

/**
 * بطاقة سريعة للأصناف بدون باركود (Quick Tile)
 */
data class QuickTileItem(
    val id: String,
    val titleArabic: String,
    val subtitle: String,
    val price: Double,
    val category: String,
    val iconType: QuickTileIconType,
    val isWeighted: Boolean = false,
    val defaultWeightKg: Double = 1.0,
    val isOpenPrice: Boolean = false,
    val linkedProductCode: String? = null
)

enum class QuickTileIconType {
    BREAD,
    TAMEES,
    PRODUCE,
    TOMATO,
    CUCUMBER,
    HERBS,
    WATER,
    ICE,
    OPEN_PRICE,
    CUSTOM
}

/**
 * ملخص سلة المبيعات اللحظي
 */
data class CartSummary(
    val itemsCount: Int,
    val totalQuantity: Double,
    val subtotal: Double,
    val discount: Double,
    val taxableAmount: Double,
    val taxRatePercent: Double,
    val taxAmount: Double,
    val finalTotal: Double
)

/**
 * نتيجة إتمام الفاتورة
 */
data class PosCheckoutResult(
    val success: Boolean,
    val invoiceId: Long,
    val invoiceNumber: String,
    val total: Double,
    val paymentMethod: PaymentMethod,
    val paidAmount: Double,
    val changeAmount: Double,
    val remainingCreditAmount: Double,
    val customerName: String? = null,
    val customerNewBalance: Double? = null,
    val drawerKickTriggered: Boolean,
    val receiptData: com.example.dokkani.domain.hardware.ReceiptPrintData,
    val message: String
)
