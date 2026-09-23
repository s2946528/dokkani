package com.example.dokkani.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * جدول سجل الحضور والانصراف اليومي للعمال (Employee Attendance Table)
 */
@Entity(
    tableName = "employee_attendance",
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
        Index(value = ["date"])
    ]
)
data class EmployeeAttendanceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val employeeId: Long,                         // معرّف الموظف
    val date: Long,                               // تاريخ اليوم (بالميلي ثانية)
    val status: AttendanceStatus = AttendanceStatus.PRESENT, // حالة الحضور (حاضر/غائب/نصف يوم/إضافي)
    val overtimeHours: Double = 0.0,              // عدد الساعات الإضافية
    val dailyWageCalculated: Double = 0.0,        // الأجر المحسوب لليوم
    val isPaidOut: Boolean = false,               // هل تم صرف أجر هذا اليوم نقداً
    val notes: String = ""                        // ملاحظات الحضور
)
