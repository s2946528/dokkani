package com.example.dokkani.ui.screens.users

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dokkani.data.local.DokkaniDatabase
import com.example.dokkani.data.local.SessionManager
import com.example.dokkani.data.local.entities.UserEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val userDao = DokkaniDatabase.getDatabase(application, viewModelScope).userDao()
    private val sessionManager = SessionManager(application)

    val activeUsers: StateFlow<List<UserEntity>> = userDao.getActiveUsers()
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptyList<UserEntity>())

    val currentUserRole = sessionManager.currentUserRole.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = null)

    private val _selectedUser = MutableStateFlow<UserEntity?>(null)
    val selectedUser: StateFlow<UserEntity?> = _selectedUser.asStateFlow()

    private val _pinCodeInput = MutableStateFlow("")
    val pinCodeInput: StateFlow<String> = _pinCodeInput.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    fun selectUser(user: UserEntity) {
        _selectedUser.value = user
        _pinCodeInput.value = ""
        _loginError.value = null
    }

    fun enterPinDigit(digit: String) {
        if (_pinCodeInput.value.length < 4) {
            _pinCodeInput.value += digit
            _loginError.value = null
        }
    }

    fun deletePinDigit() {
        if (_pinCodeInput.value.isNotEmpty()) {
            _pinCodeInput.value = _pinCodeInput.value.dropLast(1)
        }
    }

    fun clearPin() {
        _pinCodeInput.value = ""
        _loginError.value = null
    }

    fun login(onSuccess: () -> Unit) {
        val user = _selectedUser.value
        val pin = _pinCodeInput.value
        if (user == null) {
            _loginError.value = "الرجاء اختيار المستخدم"
            return
        }
        if (pin.length != 4) {
            _loginError.value = "الرجاء إدخال الرمز المكون من 4 أرقام"
            return
        }

        viewModelScope.launch {
            val dbUser = userDao.getUserByPin(pin)
            if (dbUser != null && dbUser.id == user.id) {
                // Success
                sessionManager.saveSession(dbUser.id, dbUser.role, dbUser.fullName)
                _pinCodeInput.value = ""
                onSuccess()
            } else {
                _loginError.value = "رمز الدخول غير صحيح"
                _pinCodeInput.value = ""
            }
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
