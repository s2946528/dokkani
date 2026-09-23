package com.example.dokkani.domain.hr

import com.example.dokkani.data.local.entities.AttendanceStatus
import com.example.dokkani.data.local.entities.EmployeeAttendanceEntity
import com.example.dokkani.data.local.entities.EmployeeEntity
import com.example.dokkani.data.local.entities.EmployeeTransactionEntity
import com.example.dokkani.data.local.entities.EmployeeTransactionType
import com.example.dokkani.data.local.entities.EmploymentType
import com.example.dokkani.data.local.entities.PayrollRecordEntity
import com.example.dokkani.data.local.entities.PayrollStatus

data class EmployeeBalanceSummary(
    val totalAdvances: Double = 0.0,
    val totalBonuses: Double = 0.0,
    val totalPenalties: Double = 0.0,
    val netOutstandingAdvance: Double = 0.0
)

object HrPayrollEngine {

    /**
     * احتساب أجر اليوم المكتسب للعامل بأجر يومي بناءً على حالة الحضور والساعات الإضافية
     */
    fun calculateDailyWageAmount(
        dailyRate: Double,
        status: AttendanceStatus,
        overtimeHours: Double = 0.0,
        hourlyRateMultiplier: Double = 1.5
    ): Double {
        val baseAmount = when (status) {
            AttendanceStatus.PRESENT -> dailyRate
            AttendanceStatus.OVERTIME -> dailyRate
            AttendanceStatus.HALF_DAY -> dailyRate / 2.0
            AttendanceStatus.ABSENT, AttendanceStatus.OFF -> 0.0
        }
        val hourlyRate = dailyRate / 8.0
        val overtimeAmount = if (overtimeHours > 0) overtimeHours * hourlyRate * hourlyRateMultiplier else 0.0
        return baseAmount + overtimeAmount
    }

    /**
     * احتساب ملخص الراتب الشهري واشتقاق كشف الاستحقاق (Payroll Record)
     */
    fun calculateMonthlyPayroll(
        employee: EmployeeEntity,
        attendanceList: List<EmployeeAttendanceEntity>,
        transactions: List<EmployeeTransactionEntity>,
        month: Int,
        year: Int
    ): PayrollRecordEntity {
        val daysWorked = attendanceList.count { it.status == AttendanceStatus.PRESENT || it.status == AttendanceStatus.OVERTIME }
        val halfDays = attendanceList.count { it.status == AttendanceStatus.HALF_DAY } * 0.5
        val totalDaysEquivalent = daysWorked + halfDays
        val totalOvertimeHours = attendanceList.sumOf { it.overtimeHours }

        val baseEarned = if (employee.employmentType == EmploymentType.DAILY_WAGE) {
            attendanceList.sumOf { it.dailyWageCalculated }
        } else {
            employee.basePayRate
        }

        val totalBonuses = transactions
            .filter { it.type == EmployeeTransactionType.BONUS }
            .sumOf { it.amount }

        val totalAdvances = transactions
            .filter { it.type == EmployeeTransactionType.ADVANCE || it.type == EmployeeTransactionType.DAILY_WAGE_PAYOUT }
            .sumOf { it.amount }

        val totalPenalties = transactions
            .filter { it.type == EmployeeTransactionType.PENALTY }
            .sumOf { it.amount }

        val grossSalary = baseEarned + totalBonuses
        val totalDeductions = totalAdvances + totalPenalties
        val netPayable = maxOf(0.0, grossSalary - totalDeductions)

        return PayrollRecordEntity(
            employeeId = employee.id,
            employeeName = employee.name,
            periodMonth = month,
            periodYear = year,
            employmentType = employee.employmentType,
            basePayRate = employee.basePayRate,
            daysWorked = daysWorked,
            overtimeHours = totalOvertimeHours,
            grossSalary = grossSalary,
            totalBonuses = totalBonuses,
            totalAdvancesDeducted = totalAdvances,
            totalPenaltiesDeducted = totalPenalties,
            netPayableSalary = netPayable,
            paidAmount = 0.0,
            status = PayrollStatus.UNPAID
        )
    }

    /**
     * احتساب رصيد السلف المتبقية والخصومات للموظف
     */
    fun calculateEmployeeBalanceSummary(
        transactions: List<EmployeeTransactionEntity>
    ): EmployeeBalanceSummary {
        val totalAdvances = transactions
            .filter { it.type == EmployeeTransactionType.ADVANCE || it.type == EmployeeTransactionType.DAILY_WAGE_PAYOUT }
            .sumOf { it.amount }

        val totalBonuses = transactions
            .filter { it.type == EmployeeTransactionType.BONUS }
            .sumOf { it.amount }

        val totalPenalties = transactions
            .filter { it.type == EmployeeTransactionType.PENALTY }
            .sumOf { it.amount }

        val totalPaidSalaries = transactions
            .filter { it.type == EmployeeTransactionType.SALARY_PAYMENT }
            .sumOf { it.amount }

        return EmployeeBalanceSummary(
            totalAdvances = totalAdvances,
            totalBonuses = totalBonuses,
            totalPenalties = totalPenalties,
            netOutstandingAdvance = maxOf(0.0, totalAdvances - totalPaidSalaries)
        )
    }
}
