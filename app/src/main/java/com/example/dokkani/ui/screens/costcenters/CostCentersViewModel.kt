package com.example.dokkani.ui.screens.costcenters

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dokkani.data.local.DokkaniDatabase
import com.example.dokkani.data.local.entities.CostCenterEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CostCenterUiState(
    val costCenters: List<CostCenterEntity> = emptyList(),
    val isLoading: Boolean = false,
    val showAddEditDialog: Boolean = false,
    val editingCenter: CostCenterEntity? = null, // null = إضافة، غير null = تعديل
    val nameInput: String = "",
    val codeInput: String = "",
    val descriptionInput: String = "",
    val isActiveInput: Boolean = true,
    
    // إحصائيات وربط الحركات
    val pendingDeleteCenter: CostCenterEntity? = null,
    val showDeleteConfirmDialog: Boolean = false,
    val linkedProductsCount: Int = 0,
    val linkedExpensesCount: Int = 0,
    val linkedInvoicesCount: Int = 0,
    val linkedWastageCount: Int = 0,
    
    val feedbackMessage: String? = null,
    val isErrorFeedback: Boolean = false
)

class CostCentersViewModel(application: Application) : AndroidViewModel(application) {

    private val db = DokkaniDatabase.getDatabase(application, viewModelScope)
    private val dao = db.costCenterDao()

    private val _uiState = MutableStateFlow(CostCenterUiState())
    val uiState: StateFlow<CostCenterUiState> = _uiState.asStateFlow()

    init {
        ensureGeneralCostCenterExists()
        loadCostCenters()
    }

    /**
     * الضمان الفوري لوجود "مركز التكلفة العام" الافتراضي وثباته بالقيمة ID = 1
     */
    private fun ensureGeneralCostCenterExists() {
        viewModelScope.launch(Dispatchers.IO) {
            val general = dao.getGeneralCostCenter()
            if (general == null) {
                dao.insertCostCenter(
                    CostCenterEntity(
                        centerId = 1,
                        code = "CC-GEN",
                        centerName = "مركز التكلفة العام",
                        isGeneral = true,
                        isActive = true,
                        description = "مركز التكلفة العام الافتراضي لجميع الأنشطة والأصناف والمصاريف"
                    )
                )
            }
        }
    }

    private fun loadCostCenters() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            dao.getAllCostCenters().collectLatest { list ->
                _uiState.update {
                    it.copy(
                        costCenters = list,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun openAddDialog() {
        val nextCodeNumber = (_uiState.value.costCenters.size + 1)
        val defaultCode = "CC-%03d".format(nextCodeNumber)
        _uiState.update {
            it.copy(
                showAddEditDialog = true,
                editingCenter = null,
                nameInput = "",
                codeInput = defaultCode,
                descriptionInput = "",
                isActiveInput = true
            )
        }
    }

    fun openEditDialog(center: CostCenterEntity) {
        _uiState.update {
            it.copy(
                showAddEditDialog = true,
                editingCenter = center,
                nameInput = center.centerName,
                codeInput = center.code,
                descriptionInput = center.description,
                isActiveInput = center.isActive
            )
        }
    }

    fun dismissAddEditDialog() {
        _uiState.update { it.copy(showAddEditDialog = false, editingCenter = null) }
    }

    fun updateInputs(
        name: String? = null,
        code: String? = null,
        description: String? = null,
        isActive: Boolean? = null
    ) {
        _uiState.update {
            it.copy(
                nameInput = name ?: it.nameInput,
                codeInput = code ?: it.codeInput,
                descriptionInput = description ?: it.descriptionInput,
                isActiveInput = isActive ?: it.isActiveInput
            )
        }
    }

    fun saveCostCenter() {
        val state = _uiState.value
        val name = state.nameInput.trim()
        val code = state.codeInput.trim()
        val desc = state.descriptionInput.trim()

        if (name.isBlank()) {
            _uiState.update { it.copy(feedbackMessage = "يرجى إدخال اسم مركز التكلفة بشكل صحيح", isErrorFeedback = true) }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val editing = state.editingCenter
            if (editing != null) {
                // تعديل مركز قائم
                val updated = editing.copy(
                    centerName = name,
                    code = code,
                    description = desc,
                    // لا يسمح بتعطيل مركز التكلفة العام
                    isActive = if (editing.isGeneral) true else state.isActiveInput
                )
                dao.updateCostCenter(updated)
                _uiState.update {
                    it.copy(
                        showAddEditDialog = false,
                        editingCenter = null,
                        feedbackMessage = "تم تعديل مركز التكلفة '$name' بنجاح",
                        isErrorFeedback = false
                    )
                }
            } else {
                // إضافة مركز تكلفة جديد
                val newCenter = CostCenterEntity(
                    centerName = name,
                    code = code.ifBlank { "CC-%03d".format(System.currentTimeMillis() % 1000) },
                    isGeneral = false,
                    isActive = state.isActiveInput,
                    description = desc
                )
                dao.insertCostCenter(newCenter)
                _uiState.update {
                    it.copy(
                        showAddEditDialog = false,
                        editingCenter = null,
                        feedbackMessage = "تم إضافة مركز التكلفة الجديد '$name' بنجاح",
                        isErrorFeedback = false
                    )
                }
            }
        }
    }

    fun onRequestDelete(center: CostCenterEntity) {
        if (center.isGeneral) {
            _uiState.update {
                it.copy(
                    feedbackMessage = "لا يمكن حذف 'مركز التكلفة العام' لأنه المركز الافتراضي للنظام",
                    isErrorFeedback = true
                )
            }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val pCount = dao.getProductsCountByCostCenter(center.centerId)
            val eCount = dao.getExpensesCountByCostCenter(center.centerId)
            val iCount = dao.getInvoicesCountByCostCenter(center.centerId)
            val wCount = dao.getWastageCountByCostCenter(center.centerId)

            _uiState.update {
                it.copy(
                    pendingDeleteCenter = center,
                    showDeleteConfirmDialog = true,
                    linkedProductsCount = pCount,
                    linkedExpensesCount = eCount,
                    linkedInvoicesCount = iCount,
                    linkedWastageCount = wCount
                )
            }
        }
    }

    fun dismissDeleteConfirmDialog() {
        _uiState.update {
            it.copy(
                showDeleteConfirmDialog = false,
                pendingDeleteCenter = null
            )
        }
    }

    fun confirmDeleteCostCenter() {
        val center = _uiState.value.pendingDeleteCenter ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val deletedCount = dao.deleteCostCenterById(center.centerId)
            if (deletedCount > 0) {
                _uiState.update {
                    it.copy(
                        showDeleteConfirmDialog = false,
                        pendingDeleteCenter = null,
                        feedbackMessage = "تم حذف مركز التكلفة '${center.centerName}' وتحويل ربط العمليات السابقة إلى المركز العام",
                        isErrorFeedback = false
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        showDeleteConfirmDialog = false,
                        pendingDeleteCenter = null,
                        feedbackMessage = "تعذر حذف مركز التكلفة",
                        isErrorFeedback = true
                    )
                }
            }
        }
    }

    fun dismissFeedback() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }
}
