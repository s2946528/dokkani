package com.example.dokkani.ui.screens.users

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dokkani.data.local.DokkaniDatabase
import com.example.dokkani.data.local.SessionManager
import com.example.dokkani.data.local.entities.PasswordType
import com.example.dokkani.data.local.entities.SystemSettingsEntity
import com.example.dokkani.data.local.entities.UserEntity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val db = DokkaniDatabase.getDatabase(application, viewModelScope)
    private val userDao = db.userDao()
    private val auditLogDao = db.auditLogDao()
    private val systemSettingsDao = db.systemSettingsDao()
    private val sessionManager = SessionManager(application)

    init {
        viewModelScope.launch {
            val users = userDao.getAllUsersList()
            if (users.isEmpty()) {
                val adminUser = UserEntity(
                    username = "admin",
                    fullName = "مدير النظام",
                    pinCode = "1234",
                    role = com.example.dokkani.data.local.entities.UserRole.ADMIN,
                    isActive = true,
                    mustChangePin = false
                )
                userDao.insertUser(adminUser)
            }
        }

        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(userDao.getActiveUsers(), sessionManager.lastLoggedInUserId) { users, lastUserId ->
                if (_selectedUser.value == null && users.isNotEmpty()) {
                    val defaultUser = users.find { it.id == lastUserId } ?: users.first()
                    _selectedUser.value = defaultUser
                }
            }.collect {}
        }
    }

    val systemSettings: StateFlow<SystemSettingsEntity?> = systemSettingsDao.getSettings()
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = null)

    val activeUsers: StateFlow<List<UserEntity>> = userDao.getActiveUsers()
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptyList<UserEntity>())

    val currentUserRole = sessionManager.currentUserRole.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = null)
    val isOnboardingCompleted = sessionManager.isOnboardingCompleted.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = false)

    private val _selectedUser = MutableStateFlow<UserEntity?>(null)
    val selectedUser: StateFlow<UserEntity?> = _selectedUser.asStateFlow()

    private val _pinCodeInput = MutableStateFlow("")
    val pinCodeInput: StateFlow<String> = _pinCodeInput.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    private val _mustChangePinUser = MutableStateFlow<UserEntity?>(null)
    val mustChangePinUser: StateFlow<UserEntity?> = _mustChangePinUser.asStateFlow()

    private val _failedAttemptsCount = MutableStateFlow(0)
    val failedAttemptsCount: StateFlow<Int> = _failedAttemptsCount.asStateFlow()

    private val _lockoutRemainingSeconds = MutableStateFlow(0)
    val lockoutRemainingSeconds: StateFlow<Int> = _lockoutRemainingSeconds.asStateFlow()

    private var lockoutJob: Job? = null

    private fun toArabicNumeral(number: Int): String {
        val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
        return number.toString().map { if (it.isDigit()) arabicDigits[it - '0'] else it }.joinToString("")
    }

    private fun startLockoutTimer(durationSeconds: Int = 300) {
        lockoutJob?.cancel()
        _lockoutRemainingSeconds.value = durationSeconds
        _loginError.value = "لقد استنفذت عدد المحاولات المسموح بها يمكنك المحاولة بعد خمس دقائق"
        lockoutJob = viewModelScope.launch {
            while (_lockoutRemainingSeconds.value > 0) {
                delay(1000L)
                _lockoutRemainingSeconds.value -= 1
            }
            // انتهى وقت الحظر -> إعادة التصفير
            _failedAttemptsCount.value = 0
            _loginError.value = null
        }
    }

    fun selectUser(user: UserEntity) {
        _selectedUser.value = user
        _pinCodeInput.value = ""
        if (_lockoutRemainingSeconds.value == 0) {
            _loginError.value = null
        }
    }

    fun selectAdminUser() {
        viewModelScope.launch {
            val users = userDao.getAllUsersList()
            val admin = users.firstOrNull { it.role == com.example.dokkani.data.local.entities.UserRole.ADMIN } ?: users.firstOrNull()
            if (admin != null) {
                _selectedUser.value = admin
                _pinCodeInput.value = ""
                if (_lockoutRemainingSeconds.value == 0) {
                    _loginError.value = "تم تحديد حساب مدير النظام. يرجى إدخال كلمة المرور / رمز PIN"
                }
            }
        }
    }

    fun enterPinDigit(digit: String, onSuccess: () -> Unit = {}) {
        if (_lockoutRemainingSeconds.value > 0) return

        val user = _selectedUser.value
        val settings = systemSettings.value ?: SystemSettingsEntity()
        val targetLength = if (settings.passwordType == PasswordType.NUMERIC_PIN) {
            user?.pinCode?.trim()?.length?.takeIf { it > 0 } ?: settings.pinLength
        } else settings.pinLength

        if (_pinCodeInput.value.length < targetLength) {
            _pinCodeInput.value += digit
            _loginError.value = null

            // التحقق والدخول التلقائي فوراً فور استكمال الأرقام
            if (settings.passwordType == PasswordType.NUMERIC_PIN && _pinCodeInput.value.length == targetLength) {
                login(onSuccess)
            }
        }
    }

    fun updatePasswordInput(input: String) {
        if (_lockoutRemainingSeconds.value > 0) return
        _pinCodeInput.value = input
        _loginError.value = null
    }

    fun deletePinDigit() {
        if (_lockoutRemainingSeconds.value > 0) return
        if (_pinCodeInput.value.isNotEmpty()) {
            _pinCodeInput.value = _pinCodeInput.value.dropLast(1)
        }
    }

    fun clearPin() {
        if (_lockoutRemainingSeconds.value > 0) return
        _pinCodeInput.value = ""
        _loginError.value = null
    }

    fun dismissMustChangePinDialog() {
        _mustChangePinUser.value = null
        _pinCodeInput.value = ""
    }

    fun login(onSuccess: () -> Unit) {
        if (_lockoutRemainingSeconds.value > 0) {
            _loginError.value = "لقد استنفذت عدد المحاولات المسموح بها يمكنك المحاولة بعد خمس دقائق"
            return
        }

        val user = _selectedUser.value
        val pin = _pinCodeInput.value.trim()
        val settings = systemSettings.value ?: SystemSettingsEntity()

        if (user == null) {
            _loginError.value = "الرجاء اختيار المستخدم"
            return
        }
        if (!user.isActive) {
            _loginError.value = "هذا الحساب معطل. يرجى مراجعة مدير النظام لتنشيطه."
            return
        }

        val targetLength = if (settings.passwordType == PasswordType.NUMERIC_PIN) {
            user.pinCode.trim().length.takeIf { it > 0 } ?: settings.pinLength
        } else settings.minPasswordLength

        if (settings.passwordType == PasswordType.NUMERIC_PIN) {
            if (pin.length < targetLength) {
                _loginError.value = "الرجاء إدخال الرمز المكون من $targetLength أرقام"
                return
            }
        } else {
            if (pin.length < settings.minPasswordLength || pin.length > settings.maxPasswordLength) {
                _loginError.value = "كلمة المرور يجب أن تكون بين ${settings.minPasswordLength} و ${settings.maxPasswordLength} أحرف/رموز"
                return
            }
        }

        viewModelScope.launch {
            val dbUser = userDao.getUserById(user.id)
            if (dbUser != null && dbUser.isActive && dbUser.pinCode.trim() == pin) {
                // نجاح تسجيل الدخول -> تصفير المحاولات الفاشلة
                _failedAttemptsCount.value = 0
                _loginError.value = null

                if (dbUser.mustChangePin) {
                    _mustChangePinUser.value = dbUser
                } else {
                    sessionManager.saveSession(dbUser.id, dbUser.role, dbUser.fullName)
                    auditLogDao.insertLog(
                        com.example.dokkani.data.local.entities.AuditLogEntity(
                            userId = dbUser.id,
                            userName = dbUser.fullName,
                            userRole = dbUser.role.name,
                            action = "USER_LOGIN",
                            details = "تسجيل دخول ناجح للمستخدم ${dbUser.fullName} (${dbUser.username})"
                        )
                    )
                    _pinCodeInput.value = ""
                    onSuccess()
                }
            } else {
                // محاولة فاشلة
                _failedAttemptsCount.value += 1
                val attempts = _failedAttemptsCount.value
                val remaining = 5 - attempts

                _pinCodeInput.value = ""

                if (remaining > 0) {
                    val remainingStr = toArabicNumeral(remaining)
                    _loginError.value = "كلمة المرور خاطئة متبقي ($remainingStr من ٥) محاولات ."
                } else {
                    startLockoutTimer(300) // حظر لمدة 5 دقائق
                }
            }
        }
    }

    fun changePinAndLogin(newPin: String, onSuccess: () -> Unit) {
        val targetUser = _mustChangePinUser.value ?: return
        val settings = systemSettings.value ?: SystemSettingsEntity()

        if (settings.passwordType == PasswordType.NUMERIC_PIN) {
            if (newPin.length != settings.pinLength || !newPin.all { it.isDigit() }) {
                _loginError.value = "رمز PIN الجديد يجب أن يتكون من ${settings.pinLength} أرقام"
                return
            }
        } else {
            if (newPin.length < settings.minPasswordLength || newPin.length > settings.maxPasswordLength) {
                _loginError.value = "كلمة المرور الجديدة يجب أن تكون بين ${settings.minPasswordLength} و ${settings.maxPasswordLength} أحرف"
                return
            }
        }

        viewModelScope.launch {
            userDao.updateUserPin(targetUser.id, newPin)
            sessionManager.saveSession(targetUser.id, targetUser.role, targetUser.fullName)
            auditLogDao.insertLog(
                com.example.dokkani.data.local.entities.AuditLogEntity(
                    userId = targetUser.id,
                    userName = targetUser.fullName,
                    userRole = targetUser.role.name,
                    action = "PIN_CHANGED_FIRST_LOGIN",
                    details = "تم تغيير رمز الدخول عند أول تسجيل دخول بنجاح"
                )
            )
            _mustChangePinUser.value = null
            _pinCodeInput.value = ""
            _failedAttemptsCount.value = 0
            onSuccess()
        }
    }

    fun logout() {
        viewModelScope.launch {
            sessionManager.clearSession()
            _selectedUser.value = null
            _pinCodeInput.value = ""
        }
    }
}
