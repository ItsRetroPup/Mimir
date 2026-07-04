package com.mimir.companion.ui

import android.app.Application
import android.net.Uri
import android.provider.DocumentsContract
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mimir.companion.data.PlanExecutor
import com.mimir.companion.data.RomTreeRepository
import com.mimir.companion.domain.FrontendPreset
import com.mimir.companion.domain.OperationPlan
import com.mimir.companion.domain.RomScanner
import com.mimir.companion.domain.RomZipperPlanner
import com.mimir.companion.domain.ToolMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MimirUiState(
    val selectedFolderUri: Uri? = null,
    val selectedFolderName: String = "No ROM folder selected",
    val selectedMode: ToolMode = ToolMode.MultiDiscOrganizer,
    val selectedPreset: FrontendPreset = FrontendPreset.EsDe,
    val useDarkMode: Boolean = false,
    val isBusy: Boolean = false,
    val previewCount: Int = 0,
    val previewPlan: OperationPlan? = null,
    val message: String? = null,
)

class MimirViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RomTreeRepository(application)
    private val executor = PlanExecutor(application)
    private val prefs = application.getSharedPreferences("mimir_prefs", 0)

    private val _uiState = MutableStateFlow(
        MimirUiState(
            selectedFolderUri = prefs.getString(KEY_URI, null)?.let(Uri::parse),
            selectedFolderName = prefs.getString(KEY_LABEL, null) ?: "No ROM folder selected",
            useDarkMode = prefs.getBoolean(KEY_DARK_MODE, false),
        )
    )
    val uiState: StateFlow<MimirUiState> = _uiState.asStateFlow()

    fun updatePreset(preset: FrontendPreset) {
        _uiState.update { it.copy(selectedPreset = preset, previewPlan = null, message = null) }
    }

    fun updateMode(mode: ToolMode) {
        _uiState.update {
            it.copy(
                selectedMode = mode,
                previewPlan = null,
                previewCount = 0,
                message = null,
            )
        }
    }

    fun updateDarkMode(enabled: Boolean) {
        prefs.edit()
            .putBoolean(KEY_DARK_MODE, enabled)
            .apply()
        _uiState.update { it.copy(useDarkMode = enabled) }
    }

    fun onFolderSelected(uri: Uri) {
        val label = DocumentsContract.getTreeDocumentId(uri).substringAfterLast(':')
        prefs.edit()
            .putString(KEY_URI, uri.toString())
            .putString(KEY_LABEL, label)
            .apply()
        _uiState.update {
            it.copy(
                selectedFolderUri = uri,
                selectedFolderName = label,
                previewPlan = null,
                message = null,
            )
        }
    }

    fun scan() {
        val uri = _uiState.value.selectedFolderUri ?: run {
            _uiState.update { it.copy(message = "Select a ROM folder first.") }
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isBusy = true, message = null) }
            runCatching {
                val entries = repository.scanTree(uri)
                val mode = _uiState.value.selectedMode
                val plan = when (mode) {
                    ToolMode.MultiDiscOrganizer -> {
                        val scanResult = RomScanner.scan(entries)
                        com.mimir.companion.domain.ChangePlanner.buildPlan(
                            scanResult = scanResult,
                            preset = _uiState.value.selectedPreset,
                        )
                    }

                    ToolMode.RomZipper -> RomZipperPlanner.buildPlan(entries)
                }
                val previewCount = plan.changes.size
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        previewCount = previewCount,
                        previewPlan = plan,
                        message = if (plan.changes.isEmpty()) {
                            when (mode) {
                                ToolMode.MultiDiscOrganizer -> "No multi-disc games found."
                                ToolMode.RomZipper -> "No zip-compatible ROMs found."
                            }
                        } else {
                            null
                        },
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(isBusy = false, message = error.message ?: "Scan failed.")
                }
            }
        }
    }

    fun applyChanges() {
        val current = _uiState.value
        val uri = current.selectedFolderUri ?: return
        val plan = current.previewPlan ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isBusy = true, message = null) }
            executor.apply(uri, plan)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            message = "Changes applied. Backup session is ready for undo.",
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isBusy = false, message = error.message ?: "Apply failed.")
                    }
                }
        }
    }

    fun undoLastApply() {
        val uri = _uiState.value.selectedFolderUri ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isBusy = true, message = null) }
            executor.undo(uri)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            previewPlan = null,
                            previewCount = 0,
                            message = "Last apply session restored from backup.",
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isBusy = false, message = error.message ?: "Undo failed.")
                    }
                }
        }
    }

    companion object {
        private const val KEY_URI = "rom_tree_uri"
        private const val KEY_LABEL = "rom_tree_label"
        private const val KEY_DARK_MODE = "dark_mode"
    }
}
