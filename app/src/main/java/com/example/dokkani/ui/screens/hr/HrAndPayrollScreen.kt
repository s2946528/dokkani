package com.example.dokkani.ui.screens.hr

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dokkani.data.local.entities.*
import com.example.dokkani.domain.hr.HrPayrollEngine
import com.example.dokkani.ui.DokkaniUiState
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HrAndPayrollScreen(
    uiState: DokkaniUiState,
    currentUserRole: UserRole = UserRole.ADMIN,
    onSelectSubTab: (Int) -> Unit,
    onOpenAddEmployeeDialog: (EmployeeEntity?) -> Unit,
    onDismissAddEmployeeDialog: () -> Unit,
    onEmployeeInputsChanged: (String, String, String, String, String, EmploymentType, String) -> Unit,
    onSaveEmployee: () -> Unit,
    onToggleEmployeeActive: (EmployeeEntity) -> Unit,
    onOpenAttendanceDialog: (EmployeeEntity) -> Unit,
    onDismissAttendanceDialog: () -> Unit,
    onAttendanceInputsChanged: (AttendanceStatus, String, String) -> Unit,
    onSaveAttendance: () -> Unit,
    onOpenHrTransactionDialog: (EmployeeEntity?, EmployeeTransactionType) -> Unit,
    onDismissHrTransactionDialog: () -> Unit,
    onHrTransactionInputsChanged: (EmployeeTransactionType, String, String, PaymentMethod, EmployeeEntity?) -> Unit,
    onSaveHrTransaction: () -> Unit,
    onGeneratePayrollRun: (Int, Int) -> Unit,
    onPayoutPayrollRecord: (PayrollRecordEntity, PaymentMethod) -> Unit,
    onCancelPayrollPayout: (PayrollRecordEntity) -> Unit = {},
    onOpenAdjustSalaryDialog: (EmployeeEntity) -> Unit = {},
    onDismissAdjustSalaryDialog: () -> Unit = {},
    onAdjustSalaryInputsChanged: (String, String) -> Unit = { _, _ -> },
    onSaveSalaryAdjustment: () -> Unit = {},
    onOpenEmployeeDocumentDialog: (EmployeeEntity) -> Unit = {},
    onDismissEmployeeDocumentDialog: () -> Unit = {},
    onUpdateEmployeePhotoPath: (Long, String?, String?, String?) -> Unit = { _, _, _, _ -> },
    onDismissHrErrorMessage: () -> Unit = {}
) {
    val activeEmployees = remember(uiState.employees) { uiState.employees.filter { it.isActive } }
    val dailyWorkersCount = remember(activeEmployees) { activeEmployees.count { it.employmentType == EmploymentType.DAILY_WAGE } }
    val monthlyWorkersCount = remember(activeEmployees) { activeEmployees.count { it.employmentType == EmploymentType.MONTHLY_SALARY } }
    val totalAdvances = remember(uiState.employeeTransactions) {
        uiState.employeeTransactions
            .filter { it.type == EmployeeTransactionType.ADVANCE || it.type == EmployeeTransactionType.DAILY_WAGE_PAYOUT }
            .sumOf { it.amount } - uiState.employeeTransactions
            .filter { it.type == EmployeeTransactionType.SALARY_PAYMENT }
            .sumOf { it.amount }
    }

    val tabs = listOf(
        "👥 الموظفين والعمال",
        "📅 الحضور واليوميات",
        "💸 السلف والخصومات",
        "📑 قيد المرتبات"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(12.dp)
    ) {
        // --- 1. بطاقات الإحصائيات العلوية ---
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.People,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "نظام إدارة العمال والرواتب (HR & Payroll)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${activeEmployees.size} موظف نشط",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("عمال الأجر اليومي", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                        Text("$dailyWorkersCount عامل", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                    Column {
                        Text("موظفو الراتب الشهري", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                        Text("$monthlyWorkersCount موظف", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                    Column {
                        Text("إجمالي السلف المسحوبة", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                        Text("${"%.2f".format(maxOf(0.0, totalAdvances))} ${uiState.currencySymbol}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // --- 2. التبويبات الفرعية ---
        TabRow(
            selectedTabIndex = uiState.hrSubTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = uiState.hrSubTab == index,
                    onClick = { onSelectSubTab(index) },
                    text = { Text(text = title, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // --- 3. محتوى التبويب المختار ---
        Box(modifier = Modifier.weight(1f)) {
            when (uiState.hrSubTab) {
                0 -> EmployeesDirectoryTab(
                    employees = uiState.employees,
                    currencySymbol = uiState.currencySymbol,
                    onOpenAddEmployee = { onOpenAddEmployeeDialog(null) },
                    onEditEmployee = { onOpenAddEmployeeDialog(it) },
                    onToggleActive = onToggleEmployeeActive,
                    onMarkAttendance = onOpenAttendanceDialog,
                    onAddAdvance = { onOpenHrTransactionDialog(it, EmployeeTransactionType.ADVANCE) },
                    onAdjustSalary = onOpenAdjustSalaryDialog,
                    onOpenDocumentDialog = onOpenEmployeeDocumentDialog
                )
                1 -> DailyAttendanceTab(
                    employees = uiState.employees,
                    attendances = uiState.employeeAttendances,
                    currencySymbol = uiState.currencySymbol,
                    onMarkAttendance = onOpenAttendanceDialog
                )
                2 -> AdvancesAndPenaltiesTab(
                    employees = uiState.employees,
                    transactions = uiState.employeeTransactions,
                    currencySymbol = uiState.currencySymbol,
                    onOpenTransactionDialog = onOpenHrTransactionDialog
                )
                3 -> MonthlyPayrollTab(
                    employees = uiState.employees,
                    payrollRecords = uiState.payrollRecords,
                    currentUserRole = currentUserRole,
                    currencySymbol = uiState.currencySymbol,
                    selectedMonth = uiState.selectedPayrollMonth,
                    selectedYear = uiState.selectedPayrollYear,
                    isSubmitting = uiState.isSubmittingHrAction,
                    onGenerateRun = onGeneratePayrollRun,
                    onPayoutRecord = onPayoutPayrollRecord,
                    onCancelPayoutRecord = onCancelPayrollPayout
                )
            }
        }
    }

    // --- Dialogs ---
    if (uiState.showAddEmployeeDialog) {
        AddEditEmployeeDialog(
            editingEmployee = uiState.editingEmployee,
            name = uiState.empNameInput,
            phone = uiState.empPhoneInput,
            address = uiState.empAddressInput,
            nationalId = uiState.empNationalIdInput,
            jobTitle = uiState.empJobTitleInput,
            employmentType = uiState.empEmploymentType,
            payRate = uiState.empBasePayRateInput,
            isSubmitting = uiState.isSubmittingHrAction,
            currencySymbol = uiState.currencySymbol,
            onInputsChanged = onEmployeeInputsChanged,
            onDismiss = onDismissAddEmployeeDialog,
            onSave = onSaveEmployee
        )
    }

    if (uiState.showAttendanceDialog && uiState.selectedEmployeeForAttendance != null) {
        MarkAttendanceDialog(
            employee = uiState.selectedEmployeeForAttendance,
            status = uiState.attendanceStatusInput,
            overtimeHours = uiState.attendanceOvertimeInput,
            notes = uiState.attendanceNotesInput,
            isSubmitting = uiState.isSubmittingHrAction,
            currencySymbol = uiState.currencySymbol,
            onInputsChanged = onAttendanceInputsChanged,
            onDismiss = onDismissAttendanceDialog,
            onSave = onSaveAttendance
        )
    }

    if (uiState.showHrTransactionDialog) {
        HrTransactionDialog(
            employees = uiState.employees,
            selectedEmployee = uiState.selectedEmployeeForTrans,
            type = uiState.hrTransTypeInput,
            amount = uiState.hrTransAmountInput,
            notes = uiState.hrTransNotesInput,
            paymentMethod = uiState.hrTransPaymentMethod,
            isSubmitting = uiState.isSubmittingHrAction,
            currencySymbol = uiState.currencySymbol,
            onInputsChanged = onHrTransactionInputsChanged,
            onDismiss = onDismissHrTransactionDialog,
            onSave = onSaveHrTransaction
        )
    }

    if (uiState.showAdjustSalaryDialog && uiState.selectedEmployeeForSalaryAdjust != null) {
        AdjustSalaryDialog(
            employee = uiState.selectedEmployeeForSalaryAdjust!!,
            newRateInput = uiState.newBasePayRateInput,
            reasonInput = uiState.salaryAdjustReasonInput,
            adjustmentLogs = uiState.salaryAdjustmentLogs.filter { it.employeeId == uiState.selectedEmployeeForSalaryAdjust!!.id },
            isSubmitting = uiState.isSubmittingHrAction,
            currencySymbol = uiState.currencySymbol,
            onInputsChanged = onAdjustSalaryInputsChanged,
            onDismiss = onDismissAdjustSalaryDialog,
            onSave = onSaveSalaryAdjustment
        )
    }

    if (uiState.showEmployeeDocumentDialog && uiState.selectedEmployeeForDocuments != null) {
        EmployeeDocumentDialog(
            employee = uiState.selectedEmployeeForDocuments!!,
            onDismiss = onDismissEmployeeDocumentDialog,
            onUpdatePhotoPath = onUpdateEmployeePhotoPath
        )
    }

    if (uiState.hrActionErrorMessage != null) {
        AlertDialog(
            onDismissRequest = onDismissHrErrorMessage,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تنبيه: السيولة النقدية غير كافية", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFFDC2626))
                }
            },
            text = {
                Text(uiState.hrActionErrorMessage, fontSize = 13.sp, color = Color(0xFF1E293B), lineHeight = 20.sp)
            },
            confirmButton = {
                Button(
                    onClick = onDismissHrErrorMessage,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("حسناً، فهمت", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

// ==================== TABS COMPONENTS ====================

@Composable
private fun EmployeesDirectoryTab(
    employees: List<EmployeeEntity>,
    currencySymbol: String,
    onOpenAddEmployee: () -> Unit,
    onEditEmployee: (EmployeeEntity) -> Unit,
    onToggleActive: (EmployeeEntity) -> Unit,
    onMarkAttendance: (EmployeeEntity) -> Unit,
    onAddAdvance: (EmployeeEntity) -> Unit,
    onAdjustSalary: (EmployeeEntity) -> Unit = {},
    onOpenDocumentDialog: (EmployeeEntity) -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "قائمة العمال والموظفين المسجلين:",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onOpenAddEmployee,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("btn_add_employee")
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("إضافة موظف / عامل جديد", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (employees.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("لا يوجد موظفين مسجلين حالياً. اضغط 'إضافة موظف' للبدء.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(employees, key = { it.id }) { emp ->
                    EmployeeCard(
                        employee = emp,
                        currencySymbol = currencySymbol,
                        onEdit = { onEditEmployee(emp) },
                        onToggleActive = { onToggleActive(emp) },
                        onMarkAttendance = { onMarkAttendance(emp) },
                        onAddAdvance = { onAddAdvance(emp) },
                        onAdjustSalary = { onAdjustSalary(emp) },
                        onOpenDocumentDialog = { onOpenDocumentDialog(emp) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmployeeCard(
    employee: EmployeeEntity,
    currencySymbol: String,
    onEdit: () -> Unit,
    onToggleActive: () -> Unit,
    onMarkAttendance: () -> Unit,
    onAddAdvance: () -> Unit,
    onAdjustSalary: () -> Unit = {},
    onOpenDocumentDialog: () -> Unit = {}
) {
    val profileBitmap = remember(employee.profilePhotoUri) {
        if (!employee.profilePhotoUri.isNullOrEmpty()) {
            val file = File(employee.profilePhotoUri)
            if (file.exists()) {
                try {
                    BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
                } catch (e: Exception) {
                    null
                }
            } else null
        } else null
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = if (employee.isActive) Color(0xFFDCFCE7) else Color(0xFFF1F5F9),
                        modifier = Modifier
                            .size(42.dp)
                            .clickable { onOpenDocumentDialog() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (profileBitmap != null) {
                                Image(
                                    bitmap = profileBitmap,
                                    contentDescription = "صورة الموظف",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = if (employee.isActive) Color(0xFF16A34A) else Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = employee.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (employee.employmentType == EmploymentType.DAILY_WAGE) Color(0xFFFEF3C7) else Color(0xFFDBEAFE)
                            ) {
                                Text(
                                    text = employee.employmentType.labelArabic,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (employee.employmentType == EmploymentType.DAILY_WAGE) Color(0xFFD97706) else Color(0xFF2563EB),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(text = "الوظيفة: ${employee.jobTitle} • هاتف: ${employee.phone.ifBlank { "غير مسجل" }}", fontSize = 11.sp, color = Color(0xFF64748B))
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${"%.2f".format(employee.basePayRate)} $currencySymbol",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF0F5132)
                    )
                    Text(
                        text = if (employee.employmentType == EmploymentType.DAILY_WAGE) "/ يومياً" else "/ شهرياً",
                        fontSize = 10.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedButton(
                        onClick = onMarkAttendance,
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.EventAvailable, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("حضور", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = onAddAdvance,
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.AttachMoney, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("سلفة", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = onAdjustSalary,
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0F5132)),
                        border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("تعديل الراتب", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onOpenDocumentDialog,
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2563EB)),
                        border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("توثيق الهوية", fontSize = 11.sp)
                    }
                }

                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = Color(0xFF0284C7), modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onToggleActive) {
                        Icon(
                            if (employee.isActive) Icons.Default.Block else Icons.Default.CheckCircle,
                            contentDescription = if (employee.isActive) "توقيف" else "تفعيل",
                            tint = if (employee.isActive) Color(0xFFDC2626) else Color(0xFF16A34A),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyAttendanceTab(
    employees: List<EmployeeEntity>,
    attendances: List<EmployeeAttendanceEntity>,
    currencySymbol: String,
    onMarkAttendance: (EmployeeEntity) -> Unit
) {
    val activeEmployees = remember(employees) { employees.filter { it.isActive } }
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()) }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("تسجيل وتتبع الحضور واليوميات للعمال اليومية والموظفين:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(activeEmployees, key = { it.id }) { emp ->
                val empAttendances = attendances.filter { it.employeeId == emp.id }
                val latest = empAttendances.maxByOrNull { it.date }

                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(emp.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text("${emp.employmentType.labelArabic} • ${"%.2f".format(emp.basePayRate)} $currencySymbol", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            if (latest != null) {
                                Text("آخر تسجيل: ${dateFormat.format(Date(latest.date))} (${latest.status.labelArabic})", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                            } else {
                                Text("لم يسجل حضور اليوم", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                            }
                        }

                        Button(
                            onClick = { onMarkAttendance(emp) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تسجيل اليوم", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdvancesAndPenaltiesTab(
    employees: List<EmployeeEntity>,
    transactions: List<EmployeeTransactionEntity>,
    currencySymbol: String,
    onOpenTransactionDialog: (EmployeeEntity?, EmployeeTransactionType) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd hh:mm a", Locale.getDefault()) }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "سجل السلف، المكافآت والجزاءات الإدارية:",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { onOpenTransactionDialog(null, EmployeeTransactionType.ADVANCE) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                modifier = Modifier.testTag("btn_add_hr_transaction")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("تسجيل سلفة / خصم جديد", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (transactions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("لا توجد حركات سلف أو مكافآت مسجلة مسبقاً", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(transactions, key = { it.id }) { trans ->
                    val empName = employees.find { it.id == trans.employeeId }?.name ?: "موظف"

                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(empName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = when(trans.type) {
                                            EmployeeTransactionType.ADVANCE -> Color(0xFFFEF2F2)
                                            EmployeeTransactionType.BONUS -> Color(0xFFF0FDF4)
                                            EmployeeTransactionType.PENALTY -> Color(0xFFFFF7ED)
                                            else -> Color(0xFFF1F5F9)
                                        }
                                    ) {
                                        Text(
                                            text = trans.type.labelArabic,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when(trans.type) {
                                                EmployeeTransactionType.ADVANCE -> Color(0xFFDC2626)
                                                EmployeeTransactionType.BONUS -> Color(0xFF16A34A)
                                                EmployeeTransactionType.PENALTY -> Color(0xFFEA580C)
                                                else -> Color(0xFF334155)
                                            },
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text("${trans.notes} • ${dateFormat.format(Date(trans.date))}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Text(
                                text = "${if (trans.type == EmployeeTransactionType.ADVANCE || trans.type == EmployeeTransactionType.PENALTY) "-" else "+"}${"%.2f".format(trans.amount)} $currencySymbol",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (trans.type == EmployeeTransactionType.BONUS) Color(0xFF16A34A) else Color(0xFFDC2626)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthlyPayrollTab(
    employees: List<EmployeeEntity>,
    payrollRecords: List<PayrollRecordEntity>,
    currentUserRole: UserRole,
    currencySymbol: String,
    selectedMonth: Int,
    selectedYear: Int,
    isSubmitting: Boolean,
    onGenerateRun: (Int, Int) -> Unit,
    onPayoutRecord: (PayrollRecordEntity, PaymentMethod) -> Unit,
    onCancelPayoutRecord: (PayrollRecordEntity) -> Unit
) {
    var recordToCancel by remember { mutableStateOf<PayrollRecordEntity?>(null) }

    val currentMonthRecords = remember(payrollRecords, selectedMonth, selectedYear) {
        payrollRecords.filter { it.periodMonth == selectedMonth && it.periodYear == selectedYear }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "قيد المرتبات الشهري والكشوفات:",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "شهر: $selectedMonth / $selectedYear",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = { onGenerateRun(selectedMonth, selectedYear) },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                modifier = Modifier.testTag("btn_generate_payroll")
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                } else {
                    Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إصدار قيود مرتبات الشهر الحالي", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (currentMonthRecords.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "لم يتم إصدار قيود المرتبات لهذا الشهر بعد. اضغط إصدار قيود مرتبات الشهر.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(currentMonthRecords, key = { it.id }) { record ->
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(record.employeeName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (record.status == PayrollStatus.PAID) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                                    ) {
                                        Text(
                                            text = record.status.labelArabic,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (record.status == PayrollStatus.PAID) Color(0xFF16A34A) else Color(0xFFDC2626),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "الصافي: ${"%.2f".format(record.netPayableSalary)} $currencySymbol",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("الأساسي: ${"%.2f".format(record.grossSalary)}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("سلف مخصومة: -${"%.2f".format(record.totalAdvancesDeducted)}", fontSize = 10.sp, color = Color(0xFFDC2626))
                                Text("جزاءات: -${"%.2f".format(record.totalPenaltiesDeducted)}", fontSize = 10.sp, color = Color(0xFFDC2626))
                                Text("مكافآت: +${"%.2f".format(record.totalBonuses)}", fontSize = 10.sp, color = Color(0xFF16A34A))
                            }

                            if (record.status == PayrollStatus.UNPAID) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { onPayoutRecord(record, PaymentMethod.CASH) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("صرف صافي الراتب نقداً من الدرج", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            } else if (record.status == PayrollStatus.PAID) {
                                Spacer(modifier = Modifier.height(8.dp))
                                if (currentUserRole == UserRole.ADMIN) {
                                    OutlinedButton(
                                        onClick = { recordToCancel = record },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                                        border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().testTag("btn_cancel_payroll_payout")
                                    ) {
                                        Icon(Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("إلغاء الصرف وعكس الحركة للدرج (ADMIN)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFF1F5F9),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "🔒 تم الصرف (إلغاء وتعديل الحركة يتطلب صلاحيات ADMIN)",
                                            fontSize = 11.sp,
                                            color = Color(0xFF64748B),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (recordToCancel != null) {
        val record = recordToCancel!!
        AlertDialog(
            onDismissRequest = { recordToCancel = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تأكيد إلغاء حركة صرف الراتب", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "هل أنت متأكد من إلغاء حركة صرف راتب الموظف (${record.employeeName}) لشهر ${record.periodMonth}/${record.periodYear}؟",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = "• المبلغ المردود للدرج: ${"%.2f".format(record.netPayableSalary)} $currencySymbol\n• سيتم إلغاء سحب النقدية وتخفيض المصروفات من تقرير الشفت (Z-Report) تلقائياً.\n• ستعود حالة الموظف إلى 'مستحق' لإمكانية تعديلها أو إعادة صرفها.",
                        fontSize = 12.sp,
                        color = Color(0xFF475569),
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCancelPayoutRecord(record)
                        recordToCancel = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("نعم، إلغاء الصرف والعكس المحاسبي", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { recordToCancel = null },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("إلغاء", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

// ==================== DIALOGS ====================

@Composable
private fun AddEditEmployeeDialog(
    editingEmployee: EmployeeEntity?,
    name: String,
    phone: String,
    address: String,
    nationalId: String,
    jobTitle: String,
    employmentType: EmploymentType,
    payRate: String,
    isSubmitting: Boolean,
    currencySymbol: String,
    onInputsChanged: (String, String, String, String, String, EmploymentType, String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (editingEmployee == null) "إضافة موظف/عامل جديد" else "تعديل بيانات الموظف", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { onInputsChanged(it, phone, address, nationalId, jobTitle, employmentType, payRate) },
                    label = { Text("اسم الموظف / العامل بالكامل*") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { onInputsChanged(name, it, address, nationalId, jobTitle, employmentType, payRate) },
                        label = { Text("رقم الهاتف") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = jobTitle,
                        onValueChange = { onInputsChanged(name, phone, address, nationalId, it, employmentType, payRate) },
                        label = { Text("المسمى الوظيفي") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = nationalId,
                        onValueChange = { onInputsChanged(name, phone, address, it, jobTitle, employmentType, payRate) },
                        label = { Text("رقم الهوية / الإقامة") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = address,
                        onValueChange = { onInputsChanged(name, phone, it, nationalId, jobTitle, employmentType, payRate) },
                        label = { Text("العنوان / السكن") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("نوع نظام التوظيف والدفع:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF334155))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = employmentType == EmploymentType.MONTHLY_SALARY,
                        onClick = { onInputsChanged(name, phone, address, nationalId, jobTitle, EmploymentType.MONTHLY_SALARY, payRate) },
                        label = { Text("راتب شهري ثابت") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = employmentType == EmploymentType.DAILY_WAGE,
                        onClick = { onInputsChanged(name, phone, address, nationalId, jobTitle, EmploymentType.DAILY_WAGE, payRate) },
                        label = { Text("أجر يومي") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = payRate,
                    onValueChange = { onInputsChanged(name, phone, address, nationalId, jobTitle, employmentType, it) },
                    label = { Text(if (employmentType == EmploymentType.DAILY_WAGE) "قيمة الأجر اليومي ($currencySymbol)*" else "الراتب الأساسي الشهري ($currencySymbol)*") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                enabled = !isSubmitting && name.isNotBlank() && payRate.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F5132))
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                } else {
                    Text("حفظ البيانات", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
private fun MarkAttendanceDialog(
    employee: EmployeeEntity,
    status: AttendanceStatus,
    overtimeHours: String,
    notes: String,
    isSubmitting: Boolean,
    currencySymbol: String,
    onInputsChanged: (AttendanceStatus, String, String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    val dailyCalc = remember(employee, status, overtimeHours) {
        val ot = overtimeHours.toDoubleOrNull() ?: 0.0
        HrPayrollEngine.calculateDailyWageAmount(employee.basePayRate, status, ot)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("تسجيل حضور اليوم: ${employee.name}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("حالة الحضور اليوم:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Column {
                    AttendanceStatus.values().forEach { st ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onInputsChanged(st, overtimeHours, notes) }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(selected = status == st, onClick = { onInputsChanged(st, overtimeHours, notes) })
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(st.labelArabic, fontSize = 13.sp)
                        }
                    }
                }

                OutlinedTextField(
                    value = overtimeHours,
                    onValueChange = { onInputsChanged(status, it, notes) },
                    label = { Text("عدد الساعات الإضافية (إن وجدت)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (employee.employmentType == EmploymentType.DAILY_WAGE) {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)), modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("الأجر المحسوب لهذا اليوم:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF166534))
                            Text("${"%.2f".format(dailyCalc)} $currencySymbol", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF15803D))
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { onInputsChanged(status, overtimeHours, it) },
                    label = { Text("ملاحظات") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F5132))
            ) {
                Text("حفظ الحضور", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
private fun HrTransactionDialog(
    employees: List<EmployeeEntity>,
    selectedEmployee: EmployeeEntity?,
    type: EmployeeTransactionType,
    amount: String,
    notes: String,
    paymentMethod: PaymentMethod,
    isSubmitting: Boolean,
    currencySymbol: String,
    onInputsChanged: (EmployeeTransactionType, String, String, PaymentMethod, EmployeeEntity?) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    var expandedDropdown by remember { mutableStateOf(false) }
    val activeEmployees = remember(employees) { employees.filter { it.isActive } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (selectedEmployee != null) "تسجيل حركة للموظف: ${selectedEmployee.name}" else "تسجيل سلفة / خصم / مكافأة جديدة",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color(0xFF0F172A)
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // 1. القائمة المنسدلة لاختيار الموظف
                Text("الموظف / العامل المستهدف:*", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF334155))
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedEmployee?.name ?: "اختر الموظف...",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("اسم الموظف") },
                        trailingIcon = {
                            IconButton(onClick = { expandedDropdown = !expandedDropdown }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedDropdown = true }
                    )
                    DropdownMenu(
                        expanded = expandedDropdown,
                        onDismissRequest = { expandedDropdown = false },
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        activeEmployees.forEach { emp ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(emp.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("${emp.jobTitle} • ${emp.employmentType.labelArabic}", fontSize = 11.sp, color = Color(0xFF64748B))
                                    }
                                },
                                onClick = {
                                    onInputsChanged(type, amount, notes, paymentMethod, emp)
                                    expandedDropdown = false
                                }
                            )
                        }
                    }
                }

                // 2. نوع المعاملة
                Text("نوع المعاملة المالية:*", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF334155))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = type == EmployeeTransactionType.ADVANCE,
                        onClick = { 
                            onInputsChanged(EmployeeTransactionType.ADVANCE, amount, notes, PaymentMethod.CASH, selectedEmployee) 
                        },
                        label = { Text("سلفة نقدية", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    )
                    FilterChip(
                        selected = type == EmployeeTransactionType.PENALTY,
                        onClick = { 
                            onInputsChanged(EmployeeTransactionType.PENALTY, amount, notes, PaymentMethod.CREDIT, selectedEmployee) 
                        },
                        label = { Text("خصم / جزاء", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.RemoveCircleOutline, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    )
                    FilterChip(
                        selected = type == EmployeeTransactionType.BONUS,
                        onClick = { 
                            onInputsChanged(EmployeeTransactionType.BONUS, amount, notes, paymentMethod, selectedEmployee) 
                        },
                        label = { Text("مكافأة / حافز", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    )
                }

                // 3. المبلغ
                OutlinedTextField(
                    value = amount,
                    onValueChange = { onInputsChanged(type, it, notes, paymentMethod, selectedEmployee) },
                    label = { Text("المبلغ الفعلي ($currencySymbol)*") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // 4. طريقة الصرف والتأثير على الدرج
                if (type == EmployeeTransactionType.ADVANCE || type == EmployeeTransactionType.BONUS) {
                    Text("طريقة الصرف والتأثير المحاسبي:*", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF334155))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = paymentMethod == PaymentMethod.CASH,
                            onClick = { onInputsChanged(type, amount, notes, PaymentMethod.CASH, selectedEmployee) },
                            label = { Text("صرف نقدي من الدرج", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = paymentMethod == PaymentMethod.CREDIT,
                            onClick = { onInputsChanged(type, amount, notes, PaymentMethod.CREDIT, selectedEmployee) },
                            label = { Text("قيد إداري مؤجل", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 5. بطاقة تنبيه وشرح الأثر المحاسبي تلقائياً
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (paymentMethod == PaymentMethod.CASH && type != EmployeeTransactionType.PENALTY) 
                            Color(0xFFEFF6FF) else Color(0xFFFFF7ED)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (paymentMethod == PaymentMethod.CASH && type != EmployeeTransactionType.PENALTY) 
                                Icons.Default.PointOfSale else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (paymentMethod == PaymentMethod.CASH && type != EmployeeTransactionType.PENALTY) 
                                Color(0xFF1D4ED8) else Color(0xFFC2410C),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when {
                                type == EmployeeTransactionType.PENALTY -> 
                                    "📝 خصم إداري: سيتم قيده كبند استقطاع يُخصم تلقائياً عند إعداد كشف الرواتب الشهري القادم دون مساس بنقدية الخزينة الحالية."
                                paymentMethod == PaymentMethod.CASH && type == EmployeeTransactionType.ADVANCE -> 
                                    "⚡ صرف نقدي: سيتم تسليم المبلغ للموظف وقيده كمصروف درج فوراً في تقرير إغلاق الشفت (Z-Report) تحت 'سلف ومسحوبات عمال' وتخفيض النقدية المتوقعة بالصندوق."
                                paymentMethod == PaymentMethod.CASH && type == EmployeeTransactionType.BONUS -> 
                                    "⚡ صرف نقدي: سيتم تسليم المكافأة للموظف كاش وقيدها كمصروفات رواتب نقدية في شفت الخزينة الحالي."
                                else -> 
                                    "📝 قيد إداري مؤجل: سيتم تسجيل المبلغ كحق أو استقطاع مؤجل في ملف الموظف ليدرج في كشف مسير الرواتب دون التأثير على الدرج اليوم."
                            },
                            fontSize = 11.sp,
                            color = if (paymentMethod == PaymentMethod.CASH && type != EmployeeTransactionType.PENALTY) 
                                Color(0xFF1E40AF) else Color(0xFF9A3412)
                        )
                    }
                }

                // 6. ملاحظات وسبب المعاملة
                OutlinedTextField(
                    value = notes,
                    onValueChange = { onInputsChanged(type, amount, it, paymentMethod, selectedEmployee) },
                    label = { Text("السبب والملاحظات (تأخير، عجز، سلفة طارئة...)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                enabled = !isSubmitting && selectedEmployee != null && (amount.toDoubleOrNull() ?: 0.0) > 0.0,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F5132))
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                } else {
                    Text("حفظ المعاملة", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

// ==================== SALARY ADJUSTMENT & CAMERA DOCUMENT DIALOGS ====================

@Composable
private fun AdjustSalaryDialog(
    employee: EmployeeEntity,
    newRateInput: String,
    reasonInput: String,
    adjustmentLogs: List<SalaryAdjustmentLogEntity>,
    isSubmitting: Boolean,
    currencySymbol: String,
    onInputsChanged: (String, String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = Color(0xFF0F5132),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "تعديل الراتب الأساسي: ${employee.name}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF0F5132)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Card for current details
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF1F5F9),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("الراتب الأساسي الحالي:", fontSize = 11.sp, color = Color(0xFF64748B))
                            Text(
                                text = "${"%.2f".format(employee.basePayRate)} $currencySymbol ${if (employee.employmentType == EmploymentType.DAILY_WAGE) "/ يومياً" else "/ شهرياً"}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF0F172A)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFDBEAFE)
                        ) {
                            Text(
                                text = employee.employmentType.labelArabic,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2563EB),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = newRateInput,
                    onValueChange = { onInputsChanged(it, reasonInput) },
                    label = { Text("الراتب الأساسي الجديد *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = reasonInput,
                    onValueChange = { onInputsChanged(newRateInput, it) },
                    label = { Text("سبب التعديل (إلزامي - ترقية، تعديل أجور...) *") },
                    isError = reasonInput.isBlank(),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "* تنبيه: التعديل يسري فوراً على مسير الرواتب المستقبلي فقط، ولا يغير السجلات المالية التاريخية السابقة.",
                    fontSize = 10.sp,
                    color = Color(0xFF64748B),
                    lineHeight = 14.sp
                )

                if (adjustmentLogs.isNotEmpty()) {
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    Text("سجل التعديلات السابقة للراتب:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF334155))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 140.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(adjustmentLogs, key = { it.id }) { log ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${"%.2f".format(log.oldBasePayRate)} ➔ ${"%.2f".format(log.newBasePayRate)} $currencySymbol",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = Color(0xFF0F5132)
                                        )
                                        Text(
                                            text = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(log.adjustedAt)),
                                            fontSize = 10.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                    Text(
                                        text = "السبب: ${log.reason} • بواسطة: ${log.adjustedBy}",
                                        fontSize = 10.sp,
                                        color = Color(0xFF475569)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                enabled = !isSubmitting && (newRateInput.toDoubleOrNull() ?: 0.0) > 0.0 && reasonInput.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F5132))
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                } else {
                    Text("اعتماد تعديل الراتب", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
private fun EmployeeDocumentDialog(
    employee: EmployeeEntity,
    onDismiss: () -> Unit,
    onUpdatePhotoPath: (employeeId: Long, profilePhotoUri: String?, idCardFrontUri: String?, idCardBackUri: String?) -> Unit
) {
    val context = LocalContext.current

    // State to store target image file & uri when launching camera
    var tempCameraFile by remember { mutableStateOf<File?>(null) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    // Dynamic CAMERA Runtime Permission Launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pendingAction?.invoke()
            pendingAction = null
        } else {
            Toast.makeText(
                context,
                "يلزم منح صلاحية الكاميرا لالتقاط وتوثيق صور المستندات",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Helper to run action with permission check
    fun requestCameraPermissionAndRun(action: () -> Unit) {
        val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            action()
        } else {
            pendingAction = action
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Helper to generate temporary URI using FileProvider
    fun createTempImageUri(): Pair<Uri, File>? {
        return try {
            val dir = File(context.cacheDir, "camera_photos")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "temp_doc_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            Pair(uri, file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Launchers for Full Quality TakePicture
    val profileTakeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && tempCameraFile != null && tempCameraFile!!.exists()) {
            val path = saveFileToAppStorage(context, tempCameraFile!!, "profile_${employee.id}")
            if (path != null) {
                onUpdatePhotoPath(employee.id, path, null, null)
            }
        }
    }

    val idFrontTakeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && tempCameraFile != null && tempCameraFile!!.exists()) {
            val path = saveFileToAppStorage(context, tempCameraFile!!, "id_front_${employee.id}")
            if (path != null) {
                onUpdatePhotoPath(employee.id, null, path, null)
            }
        }
    }

    val idBackTakeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && tempCameraFile != null && tempCameraFile!!.exists()) {
            val path = saveFileToAppStorage(context, tempCameraFile!!, "id_back_${employee.id}")
            if (path != null) {
                onUpdatePhotoPath(employee.id, null, null, path)
            }
        }
    }

    // Fallback Preview Launchers
    val profilePreviewLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            val path = saveBitmapToAppStorage(context, bitmap, "profile_${employee.id}")
            if (path != null) onUpdatePhotoPath(employee.id, path, null, null)
        }
    }

    val idFrontPreviewLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            val path = saveBitmapToAppStorage(context, bitmap, "id_front_${employee.id}")
            if (path != null) onUpdatePhotoPath(employee.id, null, path, null)
        }
    }

    val idBackPreviewLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            val path = saveBitmapToAppStorage(context, bitmap, "id_back_${employee.id}")
            if (path != null) onUpdatePhotoPath(employee.id, null, null, path)
        }
    }

    // Gallery Launchers
    val profileGalleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val path = saveUriToAppStorage(context, uri, "profile_${employee.id}")
            if (path != null) onUpdatePhotoPath(employee.id, path, null, null)
        }
    }
    val idFrontGalleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val path = saveUriToAppStorage(context, uri, "id_front_${employee.id}")
            if (path != null) onUpdatePhotoPath(employee.id, null, path, null)
        }
    }
    val idBackGalleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val path = saveUriToAppStorage(context, uri, "id_back_${employee.id}")
            if (path != null) onUpdatePhotoPath(employee.id, null, null, path)
        }
    }

    // Safe camera launch triggers
    fun safeLaunchProfileCamera() {
        requestCameraPermissionAndRun {
            try {
                val pair = createTempImageUri()
                if (pair != null) {
                    tempCameraUri = pair.first
                    tempCameraFile = pair.second
                    profileTakeLauncher.launch(pair.first)
                } else {
                    profilePreviewLauncher.launch(null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                try {
                    profilePreviewLauncher.launch(null)
                } catch (ex: Exception) {
                    Toast.makeText(context, "تعذر فتح الكاميرا: ${ex.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun safeLaunchIdFrontCamera() {
        requestCameraPermissionAndRun {
            try {
                val pair = createTempImageUri()
                if (pair != null) {
                    tempCameraUri = pair.first
                    tempCameraFile = pair.second
                    idFrontTakeLauncher.launch(pair.first)
                } else {
                    idFrontPreviewLauncher.launch(null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                try {
                    idFrontPreviewLauncher.launch(null)
                } catch (ex: Exception) {
                    Toast.makeText(context, "تعذر فتح الكاميرا: ${ex.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun safeLaunchIdBackCamera() {
        requestCameraPermissionAndRun {
            try {
                val pair = createTempImageUri()
                if (pair != null) {
                    tempCameraUri = pair.first
                    tempCameraFile = pair.second
                    idBackTakeLauncher.launch(pair.first)
                } else {
                    idBackPreviewLauncher.launch(null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                try {
                    idBackPreviewLauncher.launch(null)
                } catch (ex: Exception) {
                    Toast.makeText(context, "تعذر فتح الكاميرا: ${ex.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Badge,
                    contentDescription = null,
                    tint = Color(0xFF2563EB),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "توثيق الصور والمستندات: ${employee.name}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF1E293B)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Section 1: Profile Photo
                DocumentSectionCard(
                    title = "1. الصورة الشخصية للموظف",
                    photoUri = employee.profilePhotoUri,
                    onCaptureCamera = { safeLaunchProfileCamera() },
                    onSelectGallery = { profileGalleryLauncher.launch("image/*") }
                )

                // Section 2: ID Front
                DocumentSectionCard(
                    title = "2. صورة الهوية الوطنية (الوجه الأمامي)",
                    photoUri = employee.idCardFrontUri,
                    onCaptureCamera = { safeLaunchIdFrontCamera() },
                    onSelectGallery = { idFrontGalleryLauncher.launch("image/*") }
                )

                // Section 3: ID Back
                DocumentSectionCard(
                    title = "3. صورة الهوية الوطنية (الوجه الخلفي)",
                    photoUri = employee.idCardBackUri,
                    onCaptureCamera = { safeLaunchIdBackCamera() },
                    onSelectGallery = { idBackGalleryLauncher.launch("image/*") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
            ) {
                Text("إغلاق وحفظ", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun DocumentSectionCard(
    title: String,
    photoUri: String?,
    onCaptureCamera: () -> Unit,
    onSelectGallery: () -> Unit
) {
    val bitmap = remember(photoUri) {
        if (!photoUri.isNullOrEmpty()) {
            val file = File(photoUri)
            if (file.exists()) {
                try {
                    BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
                } catch (e: Exception) {
                    null
                }
            } else null
        } else null
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF1E293B))
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFE2E8F0),
                    modifier = Modifier.size(60.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap,
                                contentDescription = title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                Icons.Default.NoPhotography,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Button(
                        onClick = onCaptureCamera,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F5132)),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("التقاط بالكاميرا", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedButton(
                        onClick = onSelectGallery,
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("اختر من المعرض", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

private fun saveBitmapToAppStorage(context: Context, bitmap: Bitmap, prefix: String): String? {
    return try {
        val dir = File(context.filesDir, "employee_photos")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "${prefix}_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun saveUriToAppStorage(context: Context, uri: Uri, prefix: String): String? {
    return try {
        val dir = File(context.filesDir, "employee_photos")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "${prefix}_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun saveFileToAppStorage(context: Context, sourceFile: File, prefix: String): String? {
    return try {
        val dir = File(context.filesDir, "employee_photos")
        if (!dir.exists()) dir.mkdirs()
        val destFile = File(dir, "${prefix}_${System.currentTimeMillis()}.jpg")
        sourceFile.inputStream().use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }
        destFile.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
