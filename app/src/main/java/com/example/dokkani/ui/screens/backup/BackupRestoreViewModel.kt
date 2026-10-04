package com.example.dokkani.ui.screens.backup

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dokkani.util.BackupMetadata
import com.example.dokkani.util.BackupRestoreHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BackupRestoreUiState(
    val lastBackup: BackupMetadata? = null,
    val isExporting: Boolean = false,
    val isRestoring: Boolean = false,
    val showRestoreConfirmDialog: Boolean = false,
    val pendingRestoreUri: Uri? = null,
    val feedbackMessage: String? = null,
    val isError: Boolean = false,
    val isRestoreSuccessNeedsRestart: Boolean = false
)

class BackupRestoreViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(BackupRestoreUiState())
    val uiState: StateFlow<BackupRestoreUiState> = _uiState.asStateFlow()

    init {
        loadMetadata()
    }

    fun loadMetadata() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val meta = BackupRestoreHelper.getLastBackupMetadata(getApplication())
                _uiState.update { it.copy(lastBackup = meta) }
            } catch (e: Exception) {
                Log.e("BackupRestoreViewModel", "Error loading backup metadata: ${e.message}", e)
            }
        }
    }

    fun exportBackup(uri: Uri) {
        _uiState.update { it.copy(isExporting = true) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val success = BackupRestoreHelper.exportDatabaseToUri(getApplication(), uri)
                val updatedMeta = BackupRestoreHelper.getLastBackupMetadata(getApplication())
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        lastBackup = updatedMeta,
                        feedbackMessage = if (success) "تم إنشاء وتصدير النسخة الاحتياطية بنجاح!" else "فشل تصدير النسخة الاحتياطية",
                        isError = !success
                    )
                }
            } catch (e: Exception) {
                Log.e("BackupRestoreViewModel", "Error exporting database: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        feedbackMessage = "حدث خطأ أثناء تصدير النسخة الاحتياطية: ${e.message}",
                        isError = true
                    )
                }
            }
        }
    }

    fun requestRestoreBackup(uri: Uri) {
        _uiState.update {
            it.copy(
                pendingRestoreUri = uri,
                showRestoreConfirmDialog = true
            )
        }
    }

    fun dismissRestoreConfirmDialog() {
        _uiState.update {
            it.copy(
                pendingRestoreUri = null,
                showRestoreConfirmDialog = false
            )
        }
    }

    fun confirmRestoreBackup() {
        val uri = _uiState.value.pendingRestoreUri ?: return
        _uiState.update { it.copy(isRestoring = true, showRestoreConfirmDialog = false) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val success = BackupRestoreHelper.restoreDatabaseFromUri(getApplication(), uri)
                _uiState.update {
                    it.copy(
                        isRestoring = false,
                        pendingRestoreUri = null,
                        feedbackMessage = if (success) "تمت استعادة نسخة قاعدة البيانات بنجاح! يرجى إعادة فتح التطبيق لتحديث البيانات المستعادة." else "فشل استعادة ملف قاعدة البيانات",
                        isError = !success,
                        isRestoreSuccessNeedsRestart = success
                    )
                }
            } catch (e: Exception) {
                Log.e("BackupRestoreViewModel", "Error restoring database: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        isRestoring = false,
                        pendingRestoreUri = null,
                        feedbackMessage = "حدث خطأ أثناء استعادة النسخة الاحتياطية: ${e.message}",
                        isError = true
                    )
                }
            }
        }
    }

    fun dismissFeedback() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }
}
