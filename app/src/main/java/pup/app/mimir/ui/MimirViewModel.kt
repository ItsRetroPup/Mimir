package pup.app.mimir.ui

import android.app.Application
import android.net.Uri
import android.provider.DocumentsContract
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import pup.app.mimir.data.PlanExecutor
import pup.app.mimir.data.RomTreeRepository
import pup.app.mimir.domain.FrontendPreset
import pup.app.mimir.domain.OperationPlan
import pup.app.mimir.domain.RomScanner
import pup.app.mimir.domain.RomZipperPlanner
import pup.app.mimir.domain.ToolMode
import pup.app.mimir.domain.VitaAppIdPlanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MimirUiState(
    val selectedFolderUri: Uri? = null,
    val selectedFolderName: String = "No ROM folder selected",
    val vitaOutputUri: Uri? = null,
    val vitaOutputName: String = "No Vita output directory selected",
    val vitaQuery: String = "",
    val vitaDatabaseSize: Int = 0,
    val vitaSearchResults: List<pup.app.mimir.domain.VitaApp> = emptyList(),
    val addedVitaShortcuts: Map<String, String> = emptyMap(),
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
    private var vitaCatalog: List<pup.app.mimir.domain.VitaApp> = emptyList()

    private val _uiState = MutableStateFlow(
        MimirUiState(
            selectedFolderUri = prefs.getString(KEY_URI, null)?.let(Uri::parse),
            selectedFolderName = prefs.getString(KEY_LABEL, null) ?: "No ROM folder selected",
            vitaOutputUri = prefs.getString(KEY_VITA_OUTPUT_URI, null)?.let(Uri::parse),
            vitaOutputName = prefs.getString(KEY_VITA_OUTPUT_LABEL, null) ?: "No Vita output directory selected",
            useDarkMode = prefs.getBoolean(KEY_DARK_MODE, false),
        )
    )
    val uiState: StateFlow<MimirUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val catalog = runCatching { repository.loadVitaShortcutCatalog() }.getOrElse { emptyList() }
            vitaCatalog = catalog
            _uiState.update {
                it.copy(
                    vitaDatabaseSize = catalog.size,
                    vitaSearchResults = emptyList(),
                )
            }
            val existingOutputUri = _uiState.value.vitaOutputUri
            if (existingOutputUri != null) {
                refreshExistingVitaShortcuts(existingOutputUri)
            }
        }
    }

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

    fun updateVitaQuery(query: String) {
        _uiState.update {
            it.copy(
                vitaQuery = query,
                vitaSearchResults = filterVitaCatalog(query),
                message = null,
            )
        }
    }

    fun addVitaShortcut(app: pup.app.mimir.domain.VitaApp) {
        val outputUri = _uiState.value.vitaOutputUri ?: run {
            _uiState.update { it.copy(message = "Select a Vita shortcut output folder first.") }
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isBusy = true, message = null) }
            val existingEntries = repository.scanTree(outputUri)
            val plan = VitaAppIdPlanner.buildPlan(
                apps = listOf(app),
                existingEntries = existingEntries,
            )
            if (plan.changes.isEmpty()) {
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        message = plan.conflicts.firstOrNull() ?: "Unable to add shortcut for ${app.title}.",
                    )
                }
                return@launch
            }
            val targetPath = plan.changes.single().targetFiles.single()
            executor.apply(outputUri, plan)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            isBusy = false,
                            addedVitaShortcuts = state.addedVitaShortcuts + (app.titleId to targetPath),
                            message = "Added shortcut: ${app.title}",
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isBusy = false, message = error.message ?: "Add shortcut failed.")
                    }
                }
        }
    }

    fun removeVitaShortcut(app: pup.app.mimir.domain.VitaApp) {
        val outputUri = _uiState.value.vitaOutputUri ?: return
        val targetPath = _uiState.value.addedVitaShortcuts[app.titleId] ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isBusy = true, message = null) }
            executor.deleteOutputFile(outputUri, targetPath)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            isBusy = false,
                            addedVitaShortcuts = state.addedVitaShortcuts - app.titleId,
                            message = "Removed shortcut: ${app.title}",
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isBusy = false, message = error.message ?: "Remove shortcut failed.")
                    }
                }
        }
    }

    fun updateDarkMode(enabled: Boolean) {
        prefs.edit()
            .putBoolean(KEY_DARK_MODE, enabled)
            .apply()
        _uiState.update { it.copy(useDarkMode = enabled) }
    }

    fun onFolderSelected(uri: Uri) {
        val label = treeLabel(uri)
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

    fun onVitaOutputSelected(uri: Uri) {
        val label = treeLabel(uri)
        prefs.edit()
            .putString(KEY_VITA_OUTPUT_URI, uri.toString())
            .putString(KEY_VITA_OUTPUT_LABEL, label)
            .apply()
        _uiState.update {
            it.copy(
                vitaOutputUri = uri,
                vitaOutputName = label,
                addedVitaShortcuts = emptyMap(),
                message = null,
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            refreshExistingVitaShortcuts(uri)
        }
    }

    fun scan() {
        val currentState = _uiState.value
        val mode = currentState.selectedMode
        val romUri = currentState.selectedFolderUri

        when (mode) {
            ToolMode.MultiDiscOrganizer, ToolMode.RomZipper -> {
                if (romUri == null) {
                    _uiState.update { it.copy(message = "Select a ROM folder first.") }
                    return
                }
            }
            ToolMode.VitaAppIds -> return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isBusy = true, message = null) }
            runCatching {
                val plan = when (mode) {
                    ToolMode.MultiDiscOrganizer -> {
                        val entries = repository.scanTree(requireNotNull(romUri))
                        val scanResult = RomScanner.scan(entries)
                        pup.app.mimir.domain.ChangePlanner.buildPlan(
                            scanResult = scanResult,
                            preset = _uiState.value.selectedPreset,
                        )
                    }

                    ToolMode.RomZipper -> {
                        val entries = repository.scanTree(requireNotNull(romUri))
                        RomZipperPlanner.buildPlan(entries)
                    }

                    ToolMode.VitaAppIds -> error("Vita shortcuts are created directly from search results.")
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
                                ToolMode.VitaAppIds -> "No Vita shortcuts queued."
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
        val plan = current.previewPlan ?: return
        val uri = when (plan.mode) {
            ToolMode.VitaAppIds -> current.vitaOutputUri
            else -> current.selectedFolderUri
        } ?: return
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
        val current = _uiState.value
        val uri = when (current.previewPlan?.mode ?: current.selectedMode) {
            ToolMode.VitaAppIds -> current.vitaOutputUri
            else -> current.selectedFolderUri
        } ?: return
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
        private const val KEY_VITA_OUTPUT_URI = "vita_output_uri"
        private const val KEY_VITA_OUTPUT_LABEL = "vita_output_label"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val MAX_VITA_RESULTS = 40
    }

    private fun treeLabel(uri: Uri): String =
        DocumentsContract.getTreeDocumentId(uri).substringAfterLast(':')

    private fun filterVitaCatalog(query: String): List<pup.app.mimir.domain.VitaApp> {
        val normalized = query.trim().lowercase()
        if (normalized.isBlank()) {
            return emptyList()
        }
        val matches = vitaCatalog.filter { app ->
            app.title.lowercase().contains(normalized) || app.titleId.lowercase().contains(normalized)
        }
        return matches.take(MAX_VITA_RESULTS)
    }

    private suspend fun refreshExistingVitaShortcuts(uri: Uri) {
        val existing = runCatching { repository.loadExistingVitaShortcuts(uri) }.getOrElse { emptyMap() }
        _uiState.update { it.copy(addedVitaShortcuts = existing) }
    }
}
