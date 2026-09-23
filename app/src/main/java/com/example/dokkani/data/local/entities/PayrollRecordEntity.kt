package com.example.dokkani.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * جدول مسير الرواتب الشهري (Monthly Payroll Records Table)
 */
@Entity(
    tableName = "payroll_records",
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
        Index(value = ["periodYear", "periodMonth"], unique = false)
    ]
)
data class PayrollRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val employeeId: Long,                                 // معرّف الموظف
    val employeeName: String,                             // اسم الموظف وقت إصدار الكشف
    val periodMonth: Int,                                 // شهر الاستحقاق (1-12)
    val periodYear: Int,                                  // سنة الاستحقاق (مثلاً 2026)
    val employmentType: EmploymentType,                   // نوع التوظيف وقت الكشف
    val basePayRate: Double,                              // الأجر الأساسي (الراتب أو اليومية)
    val daysWorked: Int = 0,                              // عدد الأيام المحضورة
    val overtimeHours: Double = 0.0,                      // ساعات الإضافي
    val grossSalary: Double = 0.0,                        // إجمالي الاستحقاق (الراتب الأساسي/مستحق اليوميات + المكافآت)
    val totalBonuses: Double = 0.0,                       // إجمالي الحوافز والمكافآت
    val totalAdvancesDeducted: Double = 0.0,              // إجمالي السلف والمسحوبات الخصمية
    val totalPenaltiesDeducted: Double = 0.0,             // إجمالي الخصومات والجزاءات الإدارية
    val netPayableSalary: Double = 0.0,                   // صافي الراتب المستحق للصرف
    val paidAmount: Double = 0.0,                         // المبلغ المصروف فعلياً
    val status: PayrollStatus = PayrollStatus.UNPAID,     // حالة السداد (غير مسدد / مسدد)
    val paymentDate: Long? = null,                        // تاريخ تاريخ الصرف
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,// طريقة الصرف
    val paymentAccountId: Long? = null,                   // معرّف الحساب المالي (بنك/محفظة/شبكة)
    val transactionRef: String = "",                       // رقم العملية / المرجع / رقم الحوالة
    val notes: String = ""                                // ملاحظات المسير
)
