package com.example.dokkani.ui.screens.users

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dokkani.data.local.entities.PasswordType
import com.example.dokkani.data.local.entities.SystemSettingsEntity
import com.example.dokkani.data.local.entities.UserEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinLoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: () -> Unit
) {
    val activeUsers by viewModel.activeUsers.collectAsState()
    val selectedUser by viewModel.selectedUser.collectAsState()
    val pinCodeInput by viewModel.pinCodeInput.collectAsState()
    val loginError by viewModel.loginError.collectAsState()
    val settingsState by viewModel.systemSettings.collectAsState()
    val lockoutRemainingSeconds by viewModel.lockoutRemainingSeconds.collectAsState()
    var expanded by remember { mutableStateOf(false) }

    val settings = settingsState ?: SystemSettingsEntity()
    val isLockedOut = lockoutRemainingSeconds > 0

    val mustChangePinUser by viewModel.mustChangePinUser.collectAsState()
    var newPinInput by remember { mutableStateOf("") }
    var confirmPinInput by remember { mutableStateOf("") }
    var changePinError by remember { mutableStateOf<String?>(null) }
    var showPasswordText by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 440.dp)
                .fillMaxWidth(0.92f)
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = "تسجيل الدخول",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // User Selection Dropdown
                ExposedDropdownMenuBox(
                    expanded = expanded && !isLockedOut,
                    onExpandedChange = { if (!isLockedOut) expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedUser?.fullName ?: "اختر المستخدم",
                        onValueChange = {},
                        readOnly = true,
                        enabled = !isLockedOut,
                        leadingIcon = { Icon(Icons.Default.AccountCircle, contentDescription = null) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded && !isLockedOut,
                        onDismissRequest = { expanded = false }
                    ) {
                        activeUsers.forEach { user ->
                            DropdownMenuItem(
                                text = { Text("${user.fullName} (${user.role.name})") },
                                onClick = {
                                    viewModel.selectUser(user)
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                // Error / Lockout Banner Display
                if (loginError != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth().testTag("login_error_banner")
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = loginError!!,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )

                            if (isLockedOut) {
                                val minutes = lockoutRemainingSeconds / 60
                                val seconds = lockoutRemainingSeconds % 60
                                val countdownStr = String.format("%02d:%02d", minutes, seconds)
                                Text(
                                    text = "(متبقي $countdownStr لفك الحظر تلقائياً)",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // Password / PIN Input Mode
                if (settings.passwordType == PasswordType.NUMERIC_PIN) {
                    // PIN Dots Display
                    val pinDotsCount = selectedUser?.pinCode?.trim()?.length?.takeIf { it > 0 } ?: settings.pinLength
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        repeat(pinDotsCount) { index ->
                            val isFilled = index < pinCodeInput.length
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 6.dp)
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isFilled) MaterialTheme.colorScheme.primary
                                        else Color.Transparent
                                    )
                                    .border(
                                        width = 2.dp,
                                        color = if (isLockedOut) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary,
                                        shape = CircleShape
                                    )
                            )
                        }
                    }

                    // Numeric PIN Keypad
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        for (row in listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"))) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                for (digit in row) {
                                    PinPadButton(text = digit, enabled = !isLockedOut) {
                                        viewModel.enterPinDigit(digit, onSuccess = onLoginSuccess)
                                    }
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            PinPadButton(text = "C", enabled = !isLockedOut) { viewModel.clearPin() }
                            PinPadButton(text = "0", enabled = !isLockedOut) {
                                viewModel.enterPinDigit("0", onSuccess = onLoginSuccess)
                            }
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(if (isLockedOut) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable(enabled = !isLockedOut) { viewModel.deletePinDigit() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "حذف رقم",
                                    tint = if (isLockedOut) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                } else {
                    // Alphanumeric Password Mode
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = pinCodeInput,
                            onValueChange = { viewModel.updatePasswordInput(it) },
                            enabled = !isLockedOut,
                            label = { Text("كلمة المرور المركبة") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { showPasswordText = !showPasswordText }) {
                                    Icon(
                                        imageVector = if (showPasswordText) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null
                                    )
                                }
                            },
                            visualTransformation = if (showPasswordText) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Guidance Alert Box for Alphanumeric Requirements
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "تنبيه إرشادي: كلمة المرور المسموحة تشمل أرقام، حروف، ورموز بطول من ${settings.minPasswordLength} إلى ${settings.maxPasswordLength} خانة.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(8.dp),
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Login Button for Alphanumeric Mode Only
                        Button(
                            onClick = { viewModel.login(onSuccess = onLoginSuccess) },
                            enabled = !isLockedOut && pinCodeInput.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("login_submit_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text("دخول", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (mustChangePinUser != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissMustChangePinDialog() },
            title = { Text("إجبار تغيير رمز/كلمة المرور") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "مرحباً ${mustChangePinUser?.fullName}، يتوجب عليك تعيين ${if (settings.passwordType == PasswordType.NUMERIC_PIN) "رمز PIN مكون من ${settings.pinLength} أرقام" else "كلمة مرور جديدة بطول من ${settings.minPasswordLength} إلى ${settings.maxPasswordLength} خانة"} قبل متابعة الدخول.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = newPinInput,
                        onValueChange = { newPinInput = it },
                        label = { Text("الرمز/كلمة المرور الجديدة") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = confirmPinInput,
                        onValueChange = { confirmPinInput = it },
                        label = { Text("تأكيد الرمز/كلمة المرور") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (settings.passwordType == PasswordType.NUMERIC_PIN)
                                "الشروط: ${settings.pinLength} أرقام فقط."
                            else
                                "الشروط: أرقام وحروف ورموز مسموحة، الطول من ${settings.minPasswordLength} إلى ${settings.maxPasswordLength} خانة.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    if (changePinError != null) {
                        Text(text = changePinError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPinInput != confirmPinInput) {
                            changePinError = "كلمة المرور وتأكيدها غير متطابقين"
                        } else {
                            changePinError = null
                            viewModel.changePinAndLogin(newPinInput, onLoginSuccess)
                        }
                    }
                ) {
                    Text("حفظ والدخول")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissMustChangePinDialog() }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun PinPadButton(
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(
                if (enabled) MaterialTheme.colorScheme.surfaceVariant
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
    }
}
