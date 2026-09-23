package com.example.dokkani.data.local

import androidx.room.TypeConverter
import com.example.dokkani.data.local.entities.BatchStatus
import com.example.dokkani.data.local.entities.CostValuationMethod
import com.example.dokkani.data.local.entities.InvoiceStatus
import com.example.dokkani.data.local.entities.InvoiceType
import com.example.dokkani.data.local.entities.MovementType
import com.example.dokkani.data.local.entities.PartyType
import com.example.dokkani.data.local.entities.PaymentMethod
import com.example.dokkani.data.local.entities.VoucherType

/**
 * محولات الأنواع الخاصة بـ Room لتخزين الـ Enums في جداول SQLite كنصوص
 */
import com.example.dokkani.data.local.entities.UserRole

class Converters {
    @TypeConverter
    fun fromUserRole(value: UserRole): String = value.name

    @TypeConverter
    fun toUserRole(value: String): UserRole = try {
        UserRole.valueOf(value)
    } catch (e: Exception) {
        UserRole.CASHIER
    }


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
    fun fromPasswordType(value: com.example.dokkani.data.local.entities.PasswordType): String = value.name

    @TypeConverter
    fun toPasswordType(value: String): com.example.dokkani.data.local.entities.PasswordType = try {
        com.example.dokkani.data.local.entities.PasswordType.valueOf(value)
    } catch (e: Exception) {
        com.example.dokkani.data.local.entities.PasswordType.NUMERIC_PIN
    }

    @TypeConverter
    fun fromForeignCurrencyPricingMode(value: com.example.dokkani.data.local.entities.ForeignCurrencyPricingMode): String = value.name

    @TypeConverter
    fun toForeignCurrencyPricingMode(value: String): com.example.dokkani.data.local.entities.ForeignCurrencyPricingMode = try {
        com.example.dokkani.data.local.entities.ForeignCurrencyPricingMode.valueOf(value)
    } catch (e: Exception) {
        com.example.dokkani.data.local.entities.ForeignCurrencyPricingMode.SALE_DATE
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

    @TypeConverter
    fun fromLicenseStatus(value: com.example.dokkani.domain.security.LicenseStatus): String = value.name

    @TypeConverter
    fun toLicenseStatus(value: String): com.example.dokkani.domain.security.LicenseStatus = try {
        com.example.dokkani.domain.security.LicenseStatus.valueOf(value)
    } catch (e: Exception) {
        com.example.dokkani.domain.security.LicenseStatus.TRIAL
    }

    @TypeConverter
    fun fromVoucherType(value: VoucherType): String = value.name

    @TypeConverter
    fun toVoucherType(value: String): VoucherType = try {
        VoucherType.valueOf(value)
    } catch (e: Exception) {
        if (value.startsWith("PAY")) VoucherType.PAYMENT else VoucherType.RECEIPT
    }

    @TypeConverter
    fun fromEmploymentType(value: com.example.dokkani.data.local.entities.EmploymentType): String = value.name

    @TypeConverter
    fun toEmploymentType(value: String): com.example.dokkani.data.local.entities.EmploymentType = try {
        com.example.dokkani.data.local.entities.EmploymentType.valueOf(value)
    } catch (e: Exception) {
        com.example.dokkani.data.local.entities.EmploymentType.MONTHLY_SALARY
    }

    @TypeConverter
    fun fromAttendanceStatus(value: com.example.dokkani.data.local.entities.AttendanceStatus): String = value.name

    @TypeConverter
    fun toAttendanceStatus(value: String): com.example.dokkani.data.local.entities.AttendanceStatus = try {
        com.example.dokkani.data.local.entities.AttendanceStatus.valueOf(value)
    } catch (e: Exception) {
        com.example.dokkani.data.local.entities.AttendanceStatus.PRESENT
    }

    @TypeConverter
    fun fromEmployeeTransactionType(value: com.example.dokkani.data.local.entities.EmployeeTransactionType): String = value.name

    @TypeConverter
    fun toEmployeeTransactionType(value: String): com.example.dokkani.data.local.entities.EmployeeTransactionType = try {
        com.example.dokkani.data.local.entities.EmployeeTransactionType.valueOf(value)
    } catch (e: Exception) {
        com.example.dokkani.data.local.entities.EmployeeTransactionType.ADVANCE
    }

    @TypeConverter
    fun fromPayrollStatus(value: com.example.dokkani.data.local.entities.PayrollStatus): String = value.name

    @TypeConverter
    fun toPayrollStatus(value: String): com.example.dokkani.data.local.entities.PayrollStatus = try {
        com.example.dokkani.data.local.entities.PayrollStatus.valueOf(value)
    } catch (e: Exception) {
        com.example.dokkani.data.local.entities.PayrollStatus.UNPAID
    }
}
