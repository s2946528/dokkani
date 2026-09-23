package com.example.dokkani.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * جدول توثيق وسجل تعديلات الرواتب الأساسية للموظفين (Salary Adjustment History)
 */
@Entity(tableName = "salary_adjustments")
data class SalaryAdjustmentLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val employeeId: Long,
    val employeeName: String,
    val oldBasePayRate: Double,             // الراتب الأساسي قبل التعديل
    val newBasePayRate: Double,             // الراتب الأساسي بعد التعديل
    val reason: String,                     // سبب التعديل (إلزامي: ترقية، هيكل أجور، تخفيض...إلخ)
    val adjustedBy: String,                 // اسم المدير أو المستخدم المسؤول الذي قام بالإجراء
    val adjustedAt: Long = System.currentTimeMillis() // تاريخ ووقت الإجراء بالميلي ثانية
)
