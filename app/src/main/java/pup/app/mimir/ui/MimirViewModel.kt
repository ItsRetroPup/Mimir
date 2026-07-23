package pup.app.mimir.ui

import android.app.Application
import android.net.Uri
import android.provider.DocumentsContract
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import pup.app.mimir.data.PlanExecutor
import pup.app.mimir.data.RomTreeRepository
import pup.app.mimir.data.OperationCancellation
import pup.app.mimir.data.OperationStoppedException
import pup.app.mimir.data.StopRequest
import pup.app.mimir.domain.FrontendPreset
import pup.app.mimir.domain.ChdDiscType
import pup.app.mimir.domain.ChdPlanner
import pup.app.mimir.domain.ChdSystem
import pup.app.mimir.domain.FileOperation
import pup.app.mimir.domain.OperationPlan
import pup.app.mimir.domain.RomScanner
import pup.app.mimir.domain.RomZipperPlanner
import pup.app.mimir.domain.ToolMode
import pup.app.mimir.domain.VitaAppIdPlanner
import pup.app.mimir.domain.VitaShortcutFormat
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
    val vitaShortcutFormat: VitaShortcutFormat = VitaShortcutFormat.Psvita,
    val vitaQuery: String = "",
    val vitaDatabaseSize: Int = 0,
    val vitaSearchResults: List<pup.app.mimir.domain.VitaApp> = emptyList(),
    val addedVitaShortcuts: Map<String, String> = emptyMap(),
    val selectedMode: ToolMode = ToolMode.MultiDiscOrganizer,
    val selectedPreset: FrontendPreset = FrontendPreset.EsDe,
    val selectedChdSystem: ChdSystem = ChdSystem.Dreamcast,
    val selectedChdDiscType: ChdDiscType = ChdDiscType.Cd,
    val deleteOriginalChdFiles: Boolean = false,
    val scanHiddenFolders: Boolean = false,
    val useDarkMode: Boolean = true,
    val isBusy: Boolean = false,
    val scanProgressLabel: String? = null,
    val operationProgressLabel: String? = null,
    val operationProgress: Float? = null,
    val chdConversionReport: ChdConversionReport? = null,
    val chdCurrentJobProgress: Float? = null,
    val chdCurrentJobLabel: String? = null,
    val storageInfo: RomTreeRepository.StorageInfo? = null,
    val stopRequest: StopRequest = StopRequest.None,
    val previewCount: Int = 0,
    val previewPlan: OperationPlan? = null,
    val selectedChangePaths: Set<String> = emptySet(),
    val message: String? = null,
)

data class ChdConversionReport(
    val completed: Int,
    val total: Int,
    val spaceSavedBytes: Long = 0L,
    val stopped: Boolean = false,
)

class MimirViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RomTreeRepository(application)
    private val executor = PlanExecutor(application)
    private val prefs = application.getSharedPreferences("mimir_prefs", 0)
    private var vitaCatalog: List<pup.app.mimir.domain.VitaApp> = emptyList()
    private var activeCancellation: OperationCancellation? = null

    private val _uiState = MutableStateFlow(
        MimirUiState(
            selectedFolderUri = prefs.getString(KEY_URI, null)?.let(Uri::parse),
            selectedFolderName = prefs.getString(KEY_LABEL, null) ?: "No ROM folder selected",
            vitaOutputUri = prefs.getString(KEY_VITA_OUTPUT_URI, null)?.let(Uri::parse),
            vitaOutputName = prefs.getString(KEY_VITA_OUTPUT_LABEL, null) ?: "No Vita output directory selected",
            vitaShortcutFormat = prefs.getString(KEY_VITA_SHORTCUT_FORMAT, null)
                ?.let { storedFormat -> VitaShortcutFormat.entries.find { it.name == storedFormat } }
                ?: VitaShortcutFormat.Psvita,
            scanHiddenFolders = prefs.getBoolean(KEY_SCAN_HIDDEN_FOLDERS, false),
            deleteOriginalChdFiles = prefs.getBoolean(KEY_DELETE_ORIGINAL_CHD_FILES, false),
            useDarkMode = prefs.getBoolean(KEY_DARK_MODE, true),
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
        _uiState.update {
            it.copy(selectedPreset = preset, previewPlan = null, selectedChangePaths = emptySet(), message = null)
        }
    }

    fun updateChdSystem(system: ChdSystem) {
        _uiState.update {
            it.copy(
                selectedChdSystem = system,
                selectedChdDiscType = when (system) {
                    ChdSystem.PlayStationPortable -> ChdDiscType.Dvd
                    ChdSystem.PlayStation2 -> it.selectedChdDiscType
                    else -> ChdDiscType.Cd
                },
                previewPlan = null,
                selectedChangePaths = emptySet(),
                message = null,
            )
        }
    }

    fun updateChdDiscType(discType: ChdDiscType) {
        _uiState.update {
            if (it.selectedChdSystem != ChdSystem.PlayStation2) it else it.copy(
                selectedChdDiscType = discType,
                previewPlan = null,
                selectedChangePaths = emptySet(),
                message = null,
            )
        }
    }

    fun updateDeleteOriginalChdFiles(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DELETE_ORIGINAL_CHD_FILES, enabled).apply()
        _uiState.update {
            // This choice is made in the final conversion dialog, so keep the current scan and
            // selection intact. applyChanges applies the value to the queued CHD operations.
            it.copy(deleteOriginalChdFiles = enabled, message = null)
        }
    }

    fun stopNow() {
        activeCancellation?.stopNow()
        _uiState.update { it.copy(stopRequest = StopRequest.Now, message = "Stopping…") }
    }

    fun stopAfterCurrentConversion() {
        activeCancellation?.stopAfterCurrent()
        _uiState.update { it.copy(stopRequest = StopRequest.AfterCurrent, message = "Will stop after the current conversion.") }
    }

    fun updateScanHiddenFolders(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SCAN_HIDDEN_FOLDERS, enabled).apply()
        _uiState.update {
            it.copy(
                scanHiddenFolders = enabled,
                previewPlan = null,
                selectedChangePaths = emptySet(),
                message = null,
            )
        }
    }

    fun updateMode(mode: ToolMode) {
        _uiState.update {
            it.copy(
                selectedMode = mode,
                previewPlan = null,
                previewCount = 0,
                selectedChangePaths = emptySet(),
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

    fun updateVitaShortcutFormat(format: VitaShortcutFormat) {
        prefs.edit().putString(KEY_VITA_SHORTCUT_FORMAT, format.name).apply()
        _uiState.update {
            it.copy(vitaShortcutFormat = format, addedVitaShortcuts = emptyMap(), message = null)
        }
        _uiState.value.vitaOutputUri?.let { outputUri ->
            viewModelScope.launch(Dispatchers.IO) {
                refreshExistingVitaShortcuts(outputUri, format)
            }
        }
    }

    fun updateChangeSelection(detailPath: String, selected: Boolean) {
        _uiState.update { state ->
            if (state.previewPlan?.changes?.any { it.detailPath == detailPath } != true) {
                state
            } else {
                state.copy(
                    selectedChangePaths = if (selected) {
                        state.selectedChangePaths + detailPath
                    } else {
                        state.selectedChangePaths - detailPath
                    },
                )
            }
        }
    }

    fun selectAllChanges() {
        _uiState.update { state ->
            val plan = state.previewPlan ?: return@update state
            state.copy(selectedChangePaths = plan.changes
                .filterNot { state.selectedMode == ToolMode.ChdConverter && it.targetAlreadyExists }
                .mapTo(linkedSetOf()) { it.detailPath })
        }
    }

    fun deselectAllChanges() {
        _uiState.update { it.copy(selectedChangePaths = emptySet()) }
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
                format = _uiState.value.vitaShortcutFormat,
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
                selectedChangePaths = emptySet(),
                storageInfo = null,
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
            ToolMode.MultiDiscOrganizer, ToolMode.RomZipper, ToolMode.ChdConverter -> {
                if (romUri == null) {
                    _uiState.update { it.copy(message = "Select a ROM folder first.") }
                    return
                }
            }
            ToolMode.VitaAppIds -> return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val cancellation = OperationCancellation()
            activeCancellation = cancellation
            _uiState.update {
                it.copy(
                    isBusy = true,
                    scanProgressLabel = initialScanLabel(mode),
                    message = null,
                    stopRequest = StopRequest.None,
                )
            }
            runCatching {
                val plan = when (mode) {
                    ToolMode.MultiDiscOrganizer -> {
                        val entries = repository.scanTree(
                            rootUri = requireNotNull(romUri),
                            scanHiddenFolders = _uiState.value.scanHiddenFolders,
                            onFileScanned = { scannedFiles -> updateScanProgress(mode, scannedFiles) },
                            shouldStop = cancellation::shouldInterruptCurrentOperation,
                        )
                        _uiState.update {
                            it.copy(scanProgressLabel = "Checking ${entries.size} ROM files for multi-disc games…")
                        }
                        val scanResult = RomScanner.scan(entries)
                        pup.app.mimir.domain.ChangePlanner.buildPlan(
                            scanResult = scanResult,
                            preset = _uiState.value.selectedPreset,
                        )
                    }

                    ToolMode.RomZipper -> {
                        val entries = repository.scanTree(
                            rootUri = requireNotNull(romUri),
                            onFileScanned = { scannedFiles -> updateScanProgress(mode, scannedFiles) },
                            shouldStop = cancellation::shouldInterruptCurrentOperation,
                        )
                        _uiState.update {
                            it.copy(scanProgressLabel = "Checking ${entries.size} ROM files for zip-compatible formats…")
                        }
                        RomZipperPlanner.buildPlan(entries)
                    }

                    ToolMode.ChdConverter -> {
                        val entries = repository.scanChdTree(
                            rootUri = requireNotNull(romUri),
                            folderAliases = currentState.selectedChdSystem.folderAliases,
                            supportedExtensions = currentState.selectedChdSystem.supportedExtensions,
                            onFileScanned = { scannedFiles -> updateScanProgress(mode, scannedFiles) },
                            shouldStop = cancellation::shouldInterruptCurrentOperation,
                        )
                        _uiState.update {
                            it.copy(scanProgressLabel = "Checking ${entries.size} ${currentState.selectedChdSystem.displayName} images for CHD conversion…")
                        }
                        ChdPlanner.buildPlan(
                            entries = entries,
                            system = currentState.selectedChdSystem,
                            discType = currentState.selectedChdDiscType,
                            deleteOriginalFiles = currentState.deleteOriginalChdFiles,
                        )
                    }

                    ToolMode.VitaAppIds -> error("Vita shortcuts are created directly from search results.")
                }
                val previewCount = plan.changes.size
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        scanProgressLabel = null,
                        previewCount = previewCount,
                        previewPlan = plan,
                        storageInfo = if (mode == ToolMode.ChdConverter) {
                            repository.storageInfo(requireNotNull(romUri))
                        } else it.storageInfo,
                        selectedChangePaths = plan.changes
                            .filterNot { mode == ToolMode.ChdConverter && it.targetAlreadyExists }
                            .mapTo(linkedSetOf()) { it.detailPath },
                        message = if (plan.changes.isEmpty()) {
                            when (mode) {
                                ToolMode.MultiDiscOrganizer -> "No multi-disc games found."
                                ToolMode.RomZipper -> "No zip-compatible ROMs found."
                                ToolMode.ChdConverter -> "No ${currentState.selectedChdSystem.displayName} images found for CHD conversion."
                                ToolMode.VitaAppIds -> "No Vita shortcuts queued."
                            }
                        } else {
                            null
                        },
                        stopRequest = StopRequest.None,
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        scanProgressLabel = null,
                        stopRequest = StopRequest.None,
                        message = if (error is OperationStoppedException) "Scan stopped." else error.message ?: "Scan failed.",
                    )
                }
            }
            if (activeCancellation === cancellation) activeCancellation = null
        }
    }

    fun applyChanges() {
        val current = _uiState.value
        val plan = current.previewPlan
            ?.forSelectedChanges(current.selectedChangePaths)
            ?.withChdDeleteOriginalFiles(current.deleteOriginalChdFiles)
            ?.takeIf { it.operations.isNotEmpty() }
            ?: return
        val uri = when (plan.mode) {
            ToolMode.VitaAppIds -> current.vitaOutputUri
            else -> current.selectedFolderUri
        } ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val cancellation = OperationCancellation()
            activeCancellation = cancellation
            _uiState.update {
                it.copy(
                    isBusy = true,
                    operationProgressLabel = applyProgressLabel(plan.mode, 0, plan.operations.size),
                    operationProgress = 0f,
                    message = null,
                    chdConversionReport = if (plan.mode == ToolMode.ChdConverter) {
                        ChdConversionReport(completed = 0, total = plan.operations.size)
                    } else {
                        it.chdConversionReport
                    },
                    chdCurrentJobProgress = if (plan.mode == ToolMode.ChdConverter) 0f else null,
                    chdCurrentJobLabel = if (plan.mode == ToolMode.ChdConverter) null else it.chdCurrentJobLabel,
                    stopRequest = StopRequest.None,
                )
            }
            executor.apply(
                rootUri = uri,
                plan = plan,
                onProgress = { completed, total ->
                    _uiState.update {
                        it.copy(
                            operationProgressLabel = applyProgressLabel(plan.mode, completed, total),
                            operationProgress = completed.toFloat() / total,
                            chdConversionReport = if (plan.mode == ToolMode.ChdConverter) {
                                ChdConversionReport(
                                    completed = completed,
                                    total = total,
                                    spaceSavedBytes = it.chdConversionReport?.spaceSavedBytes ?: 0L,
                                )
                            } else {
                                it.chdConversionReport
                            },
                            chdCurrentJobProgress = if (plan.mode == ToolMode.ChdConverter) 0f else null,
                        )
                    }
                },
                onCurrentOperationProgress = { progress ->
                    if (plan.mode == ToolMode.ChdConverter) {
                        _uiState.update { it.copy(chdCurrentJobProgress = progress) }
                    }
                },
                onOperationStarted = { operation ->
                    if (operation is FileOperation.ConvertToChd) {
                        _uiState.update {
                            it.copy(
                                chdCurrentJobLabel = operation.sourcePath
                                    .substringAfterLast('/')
                                    .substringBeforeLast('.'),
                            )
                        }
                    }
                },
                cancellation = cancellation,
            )
                .onSuccess { result ->
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            operationProgressLabel = null,
                            operationProgress = null,
                            chdCurrentJobProgress = null,
                            chdCurrentJobLabel = null,
                            chdConversionReport = if (plan.mode == ToolMode.ChdConverter) {
                                ChdConversionReport(
                                    completed = result.completedOperations,
                                    total = plan.operations.size,
                                    spaceSavedBytes = result.spaceSavedBytes,
                                    stopped = result.stopped,
                                )
                            } else {
                                it.chdConversionReport
                            },
                            stopRequest = StopRequest.None,
                            message = if (result.stopped) {
                                "Stopped after ${result.completedOperations} of ${plan.operations.size} conversions."
                            } else {
                                "Changes applied."
                            },
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            operationProgressLabel = null,
                            operationProgress = null,
                            chdCurrentJobProgress = null,
                            chdCurrentJobLabel = null,
                            chdConversionReport = if (plan.mode == ToolMode.ChdConverter) {
                                ChdConversionReport(
                                    completed = it.chdConversionReport?.completed ?: 0,
                                    total = plan.operations.size,
                                    stopped = error is OperationStoppedException,
                                )
                            } else {
                                it.chdConversionReport
                            },
                            stopRequest = StopRequest.None,
                            message = if (error is OperationStoppedException) {
                                "Conversion stopped."
                            } else {
                                error.message ?: "Apply failed."
                            },
                        )
                    }
                }
            if (activeCancellation === cancellation) activeCancellation = null
        }
    }

    companion object {
        private const val KEY_URI = "rom_tree_uri"
        private const val KEY_LABEL = "rom_tree_label"
        private const val KEY_VITA_OUTPUT_URI = "vita_output_uri"
        private const val KEY_VITA_OUTPUT_LABEL = "vita_output_label"
        private const val KEY_VITA_SHORTCUT_FORMAT = "vita_shortcut_format"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_SCAN_HIDDEN_FOLDERS = "scan_hidden_folders"
        private const val KEY_DELETE_ORIGINAL_CHD_FILES = "delete_original_chd_files"
        private const val MAX_VITA_RESULTS = 40
        private const val SCAN_PROGRESS_UPDATE_INTERVAL = 25
    }

    private fun treeLabel(uri: Uri): String =
        DocumentsContract.getTreeDocumentId(uri).substringAfterLast(':')

    private fun OperationPlan.withChdDeleteOriginalFiles(deleteOriginalFiles: Boolean): OperationPlan {
        if (mode != ToolMode.ChdConverter) return this
        fun update(operation: FileOperation): FileOperation = when (operation) {
            is FileOperation.ConvertToChd -> operation.copy(deleteOriginalFiles = deleteOriginalFiles)
            else -> operation
        }
        return copy(
            changes = changes.map { change ->
                change.copy(operations = change.operations.map(::update))
            },
            operations = operations.map(::update),
        )
    }

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

    private fun initialScanLabel(mode: ToolMode): String =
        when (mode) {
            ToolMode.MultiDiscOrganizer -> "Scanning ROM files for multi-disc games…"
            ToolMode.RomZipper -> "Scanning ROM files for zip-compatible formats…"
            ToolMode.ChdConverter -> "Scanning ROM files for CHD-compatible images…"
            ToolMode.VitaAppIds -> "Scanning ROM files…"
        }

    private fun updateScanProgress(mode: ToolMode, scannedFiles: Int) {
        if (scannedFiles % SCAN_PROGRESS_UPDATE_INTERVAL != 0) return
        val activity = when (mode) {
            ToolMode.MultiDiscOrganizer -> "Scanning for multi-disc games"
            ToolMode.RomZipper -> "Scanning for zip-compatible ROMs"
            ToolMode.ChdConverter -> "Scanning for CHD-compatible images"
            ToolMode.VitaAppIds -> "Scanning ROM files"
        }
        _uiState.update { it.copy(scanProgressLabel = "$activity: $scannedFiles files checked") }
    }

    private fun applyProgressLabel(mode: ToolMode, completed: Int, total: Int): String {
        val activity = when (mode) {
            ToolMode.MultiDiscOrganizer -> "Organizing ROMs"
            ToolMode.RomZipper -> "Zipping ROMs"
            ToolMode.ChdConverter -> "Creating CHDs"
            ToolMode.VitaAppIds -> "Creating shortcuts"
        }
        return "$activity: $completed of $total"
    }

    private suspend fun refreshExistingVitaShortcuts(
        uri: Uri,
        format: VitaShortcutFormat = _uiState.value.vitaShortcutFormat,
    ) {
        val existing = runCatching { repository.loadExistingVitaShortcuts(uri, format) }.getOrElse { emptyMap() }
        _uiState.update { it.copy(addedVitaShortcuts = existing) }
    }
}
