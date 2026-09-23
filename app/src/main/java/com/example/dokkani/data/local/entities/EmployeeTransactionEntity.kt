package com.example.dokkani.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * جدول السلف، المسحوبات، الجزاءات، والمكافآت (Employee Transactions Table)
 */
@Entity(
    tableName = "employee_transactions",
    foreignKeys = [
        ForeignKey(
            entity = EmployeeEntity::class,
            parentColumns = ["id"],
            childColumns = ["employeeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["employeeId"]),
        Index(value = ["date"]),
        Index(value = ["periodYear", "periodMonth"])
    ]
)
data class EmployeeTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val employeeId: Long,                                 // معرّف الموظف
    val type: EmployeeTransactionType,                    // نوع الحركة (سلفة/مكافأة/جزاء/صرف أجر)
    val amount: Double,                                   // المبلغ
    val date: Long = System.currentTimeMillis(),         // تاريخ ووقت العملية
    val periodMonth: Int = 0,                             // الشهر المختص
    val periodYear: Int = 0,                              // السنة المختصة
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,// طريقة الصرف (نقداً من الدرج/حوالة/محفظة)
    val paymentAccountId: Long? = null,                   // معرّف الحساب المالي (بنك/محفظة/شبكة)
    val transactionRef: String = "",                       // رقم العملية / المرجع / رقم الحوالة
    val notes: String = ""                                // بيان الحركة وملاحظاتها
)
