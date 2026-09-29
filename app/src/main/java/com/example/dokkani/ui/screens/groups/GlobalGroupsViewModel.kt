package com.example.dokkani.ui.screens.groups

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dokkani.data.local.DokkaniDatabase
import com.example.dokkani.data.local.entities.GlobalGroupEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class GlobalGroupsUiState(
    val groups: List<GlobalGroupEntity> = emptyList(),
    val filteredGroups: List<GlobalGroupEntity> = emptyList(),
    val selectedEntityTypeFilter: String = "ALL", // "ALL", "PRODUCT", "PARTY", "EXPENSE", "FINANCIAL_ACCOUNT", "EMPLOYEE", "FIXED_ASSET", "COST_CENTER", etc.
    val searchQuery: String = "",
    val showAddEditDialog: Boolean = false,
    val editingGroup: GlobalGroupEntity? = null,
    
    // Dialog Inputs
    val nameInput: String = "",
    val codeInput: String = "",
    val entityTypeInput: String = "PRODUCT",
    val isCustomEntityType: Boolean = false,
    val customEntityTypeInput: String = "",
    val parentIdInput: Long? = null,
    val descriptionInput: String = "",
    val colorHexInput: String = "#1976D2",
    
    val showDeleteDialog: Boolean = false,
    val groupToDelete: GlobalGroupEntity? = null,
    val isLoading: Boolean = false,
    val userMessage: String? = null
)

class GlobalGroupsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = DokkaniDatabase.getDatabase(application, viewModelScope)
    private val dao = db.globalGroupDao()

    private val _uiState = MutableStateFlow(GlobalGroupsUiState())
    val uiState: StateFlow<GlobalGroupsUiState> = _uiState.asStateFlow()

    init {
        loadGroups()
    }

    private fun loadGroups() {
        viewModelScope.launch {
            dao.getAllGroups().collectLatest { allGroups ->
                _uiState.value = _uiState.value.copy(groups = allGroups)
                applyFilterAndSearch()
            }
        }
    }

    fun selectEntityTypeFilter(filter: String) {
        _uiState.value = _uiState.value.copy(selectedEntityTypeFilter = filter)
        applyFilterAndSearch()
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        applyFilterAndSearch()
    }

    private fun applyFilterAndSearch() {
        val currentState = _uiState.value
        val filter = currentState.selectedEntityTypeFilter
        val query = currentState.searchQuery.trim().lowercase()

        val filtered = currentState.groups.filter { group ->
            val matchesType = if (filter == "ALL") true else group.entityType.equals(filter, ignoreCase = true)
            val matchesQuery = if (query.isEmpty()) {
                true
            } else {
                group.name.lowercase().contains(query) ||
                        group.code.lowercase().contains(query) ||
                        group.entityType.lowercase().contains(query) ||
                        group.description.lowercase().contains(query)
            }
            matchesType && matchesQuery
        }

        _uiState.value = currentState.copy(filteredGroups = filtered)
    }

    fun openAddDialog(defaultEntityType: String = "PRODUCT") {
        _uiState.value = _uiState.value.copy(
            showAddEditDialog = true,
            editingGroup = null,
            nameInput = "",
            codeInput = "",
            entityTypeInput = if (defaultEntityType == "ALL") "PRODUCT" else defaultEntityType,
            isCustomEntityType = false,
            customEntityTypeInput = "",
            parentIdInput = null,
            descriptionInput = "",
            colorHexInput = "#1976D2"
        )
    }

    fun openEditDialog(group: GlobalGroupEntity) {
        val standardTypes = listOf("PRODUCT", "PARTY", "EXPENSE", "FINANCIAL_ACCOUNT", "EMPLOYEE", "FIXED_ASSET", "COST_CENTER")
        val isCustom = group.entityType !in standardTypes

        _uiState.value = _uiState.value.copy(
            showAddEditDialog = true,
            editingGroup = group,
            nameInput = group.name,
            codeInput = group.code,
            entityTypeInput = if (isCustom) "CUSTOM" else group.entityType,
            isCustomEntityType = isCustom,
            customEntityTypeInput = if (isCustom) group.entityType else "",
            parentIdInput = group.parentId,
            descriptionInput = group.description,
            colorHexInput = group.colorHex
        )
    }

    fun dismissAddEditDialog() {
        _uiState.value = _uiState.value.copy(showAddEditDialog = false)
    }

    fun updateDialogInputs(
        name: String,
        code: String,
        entityType: String,
        isCustom: Boolean,
        customEntityType: String,
        parentId: Long?,
        description: String,
        colorHex: String
    ) {
        _uiState.value = _uiState.value.copy(
            nameInput = name,
            codeInput = code,
            entityTypeInput = entityType,
            isCustomEntityType = isCustom,
            customEntityTypeInput = customEntityType,
            parentIdInput = parentId,
            descriptionInput = description,
            colorHexInput = colorHex
        )
    }

    fun saveGroup() {
        val state = _uiState.value
        val name = state.nameInput.trim()
        if (name.isEmpty()) {
            _uiState.value = state.copy(userMessage = "يرجى إدخال اسم المجموعة")
            return
        }

        val finalEntityType = if (state.isCustomEntityType || state.entityTypeInput == "CUSTOM") {
            state.customEntityTypeInput.trim().uppercase().ifEmpty { "CUSTOM" }
        } else {
            state.entityTypeInput
        }

        viewModelScope.launch {
            try {
                _uiState.value = state.copy(isLoading = true)
                val groupToSave = GlobalGroupEntity(
                    id = state.editingGroup?.id ?: 0,
                    entityType = finalEntityType,
                    name = name,
                    code = state.codeInput.trim(),
                    parentId = state.parentIdInput,
                    description = state.descriptionInput.trim(),
                    colorHex = state.colorHexInput,
                    isActive = state.editingGroup?.isActive ?: true,
                    createdAt = state.editingGroup?.createdAt ?: System.currentTimeMillis()
                )

                if (groupToSave.id == 0L) {
                    dao.insertGroup(groupToSave)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        showAddEditDialog = false,
                        userMessage = "تم إضافة المجموعة بنجاح"
                    )
                } else {
                    dao.updateGroup(groupToSave)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        showAddEditDialog = false,
                        userMessage = "تم تحديث بيانات المجموعة بنجاح"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    userMessage = "خطأ أثناء الحفظ: ${e.localizedMessage}"
                )
            }
        }
    }

    fun requestDeleteGroup(group: GlobalGroupEntity) {
        _uiState.value = _uiState.value.copy(
            showDeleteDialog = true,
            groupToDelete = group
        )
    }

    fun dismissDeleteDialog() {
        _uiState.value = _uiState.value.copy(
            showDeleteDialog = false,
            groupToDelete = null
        )
    }

    fun confirmDeleteGroup() {
        val group = _uiState.value.groupToDelete ?: return
        viewModelScope.launch {
            try {
                dao.deleteGroup(group)
                _uiState.value = _uiState.value.copy(
                    showDeleteDialog = false,
                    groupToDelete = null,
                    userMessage = "تم حذف المجموعة بنجاح"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    userMessage = "خطأ أثناء الحذف: ${e.localizedMessage}"
                )
            }
        }
    }

    fun clearUserMessage() {
        _uiState.value = _uiState.value.copy(userMessage = null)
    }
}
