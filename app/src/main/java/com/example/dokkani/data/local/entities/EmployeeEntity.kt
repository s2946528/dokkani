package com.example.dokkani.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * جدول بيانات الموظفين والعمال (Employees Table)
 */
@Entity(tableName = "employees")
data class EmployeeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,                    // اسم الموظف / العامل الكامل
    val phone: String = "",              // رقم الهاتف
    val address: String = "",            // العنوان / السكن
    val nationalId: String = "",         // رقم الهوية أو الإقامة
    val jobTitle: String = "",           // المسمى الوظيفي (كاشير، عامل رفوف، موزع، إلخ)
    val employmentType: EmploymentType = EmploymentType.MONTHLY_SALARY, // نوع التوظيف (راتب شهري أو أجر يومي)
    val basePayRate: Double,             // قيمة الأجر الأساسي (الراتب الشهري أو قيمة اليومية)
    val hireDate: Long = System.currentTimeMillis(), // تاريخ التوظيف
    val isActive: Boolean = true,        // حالة الموظف (على رأس العمل / متوقف)
    val notes: String = "",              // ملاحظات إضافية
    val profilePhotoUri: String? = null, // مسار الصورة الشخصية للموظف
    val idCardFrontUri: String? = null,  // مسار صورة الهوية (الوجه الأمامي)
    val idCardBackUri: String? = null   // مسار صورة الهوية (الوجه الخلفي)
)
