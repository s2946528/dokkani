package com.example.dokkani.ui.screens.users

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dokkani.data.local.DokkaniDatabase
import com.example.dokkani.data.local.entities.UserEntity
import com.example.dokkani.data.local.entities.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class UserManagementViewModel(application: Application) : AndroidViewModel(application) {
    private val userDao = DokkaniDatabase.getDatabase(application, viewModelScope).userDao()

    val allUsers: StateFlow<List<UserEntity>> = userDao.getAllUsers()
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptyList<UserEntity>())

    private val _showAddEditDialog = MutableStateFlow(false)
    val showAddEditDialog: StateFlow<Boolean> = _showAddEditDialog.asStateFlow()

    private val _editingUser = MutableStateFlow<UserEntity?>(null)
    val editingUser: StateFlow<UserEntity?> = _editingUser.asStateFlow()

    // Form states
    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username.asStateFlow()

    private val _fullName = MutableStateFlow("")
    val fullName: StateFlow<String> = _fullName.asStateFlow()

    private val _pinCode = MutableStateFlow("")
    val pinCode: StateFlow<String> = _pinCode.asStateFlow()

    private val _role = MutableStateFlow(UserRole.CASHIER)
    val role: StateFlow<UserRole> = _role.asStateFlow()

    private val _isActive = MutableStateFlow(true)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    fun openAddDialog() {
        _editingUser.value = null
        _username.value = ""
        _fullName.value = ""
        _pinCode.value = ""
        _role.value = UserRole.CASHIER
        _isActive.value = true
        _showAddEditDialog.value = true
    }

    fun openEditDialog(user: UserEntity) {
        _editingUser.value = user
        _username.value = user.username
        _fullName.value = user.fullName
        _pinCode.value = user.pinCode
        _role.value = user.role
        _isActive.value = user.isActive
        _showAddEditDialog.value = true
    }

    fun closeDialog() {
        _showAddEditDialog.value = false
    }

    fun updateUsername(value: String) { _username.value = value }
    fun updateFullName(value: String) { _fullName.value = value }
    fun updatePinCode(value: String) { _pinCode.value = value }
    fun updateRole(value: UserRole) { _role.value = value }
    fun updateIsActive(value: Boolean) { _isActive.value = value }

    fun saveUser() {
        if (_username.value.isBlank() || _fullName.value.isBlank() || _pinCode.value.length != 4) return
        
        viewModelScope.launch {
            val user = UserEntity(
                id = _editingUser.value?.id ?: 0,
                username = _username.value,
                fullName = _fullName.value,
                pinCode = _pinCode.value,
                role = _role.value,
                isActive = _isActive.value
            )
            if (user.id == 0) {
                userDao.insertUser(user)
            } else {
                userDao.updateUser(user)
            }
            closeDialog()
        }
    }

    fun toggleUserStatus(user: UserEntity) {
        viewModelScope.launch {
            userDao.setUserStatus(user.id, !user.isActive)
        }
    }
}
