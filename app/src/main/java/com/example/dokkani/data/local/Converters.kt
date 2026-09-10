package com.example.dokkani.data.local

import androidx.room.TypeConverter
import com.example.dokkani.data.local.entities.BatchStatus
import com.example.dokkani.data.local.entities.CostValuationMethod
import com.example.dokkani.data.local.entities.InvoiceStatus
import com.example.dokkani.data.local.entities.InvoiceType
import com.example.dokkani.data.local.entities.MovementType
import com.example.dokkani.data.local.entities.PartyType
import com.example.dokkani.data.local.entities.PaymentMethod

/**
 * محولات الأنواع الخاصة بـ Room لتخزين الـ Enums في جداول SQLite كنصوص
 */
class Converters {

    @TypeConverter
    fun fromPartyType(value: PartyType): String = value.name

    @TypeConverter
    fun toPartyType(value: String): PartyType = try {
        PartyType.valueOf(value)
    } catch (e: Exception) {
        PartyType.CUSTOMER
    }

    @TypeConverter
    fun fromInvoiceType(value: InvoiceType): String = value.name

    @TypeConverter
    fun toInvoiceType(value: String): InvoiceType = try {
        InvoiceType.valueOf(value)
    } catch (e: Exception) {
        InvoiceType.SALE
    }

    @TypeConverter
    fun fromPaymentMethod(value: PaymentMethod): String = value.name

    @TypeConverter
    fun toPaymentMethod(value: String): PaymentMethod = try {
        PaymentMethod.valueOf(value)
    } catch (e: Exception) {
        PaymentMethod.CASH
    }

    @TypeConverter
    fun fromInvoiceStatus(value: InvoiceStatus): String = value.name

    @TypeConverter
    fun toInvoiceStatus(value: String): InvoiceStatus = try {
        InvoiceStatus.valueOf(value)
    } catch (e: Exception) {
        InvoiceStatus.COMPLETED
    }

    @TypeConverter
    fun fromCostValuationMethod(value: CostValuationMethod): String = value.name

    @TypeConverter
    fun toCostValuationMethod(value: String): CostValuationMethod = try {
        CostValuationMethod.valueOf(value)
    } catch (e: Exception) {
        CostValuationMethod.WAC
    }

    @TypeConverter
    fun fromMovementType(value: MovementType): String = value.name

    @TypeConverter
    fun toMovementType(value: String): MovementType = try {
        MovementType.valueOf(value)
    } catch (e: Exception) {
        MovementType.PURCHASE_IN
    }

    @TypeConverter
    fun fromBatchStatus(value: BatchStatus): String = value.name

    @TypeConverter
    fun toBatchStatus(value: String): BatchStatus = try {
        BatchStatus.valueOf(value)
    } catch (e: Exception) {
        BatchStatus.DRAFT
    }
}
