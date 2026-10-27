package pup.app.mimir.ui

import android.app.Application
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pup.app.mimir.R
import pup.app.mimir.data.EsDeSystem
import pup.app.mimir.data.EsDeSystemsRepository
import pup.app.mimir.data.OperationCancellation
import pup.app.mimir.data.OperationStoppedException
import pup.app.mimir.data.PlanExecutor
import pup.app.mimir.data.RomTreeRepository
import pup.app.mimir.data.StopRequest
import pup.app.mimir.domain.ChdDiscType
import pup.app.mimir.domain.ChdPlanner
import pup.app.mimir.domain.ChdSystem
import pup.app.mimir.domain.ConverterTool
import pup.app.mimir.domain.FileOperation
import pup.app.mimir.domain.FrontendPreset
import pup.app.mimir.domain.NativeConverterPlanner
import pup.app.mimir.domain.OperationPlan
import pup.app.mimir.domain.RomScanner
import pup.app.mimir.domain.RomZipperPlanner
import pup.app.mimir.domain.ToolMode
import pup.app.mimir.domain.VitaAppIdPlanner
import pup.app.mimir.domain.VitaShortcutFormat

data class MimirUiState(
    val selectedFolderUri: Uri? = null,
    val selectedFolderName: String = "",
    val vitaOutputUri: Uri? = null,
    val vitaOutputName: String = "",
    val vitaShortcutFormat: VitaShortcutFormat = VitaShortcutFormat.Psvita,
    val vitaQuery: String = "",
    val vitaDatabaseSize: Int = 0,
    val vitaSearchResults: List<pup.app.mimir.domain.VitaApp> = emptyList(),
    val addedVitaShortcuts: Map<String, String> = emptyMap(),
    val selectedMode: ToolMode = ToolMode.MultiDiscOrganizer,
    val selectedPreset: FrontendPreset = FrontendPreset.EsDe,
    val selectedChdSystem: ChdSystem = ChdSystem.Dreamcast,
    val selectedChdDiscType: ChdDiscType = ChdDiscType.Cd,
    val selectedConverterTool: ConverterTool = ConverterTool.Chd,
    val nszKeysConfigured: Boolean = false,
    val esDeRootUri: Uri? = null,
    val esDeRootName: String = "",
    val esDeSystems: List<EsDeSystem> = emptyList(),
    val deleteOriginalChdFiles: Boolean = false,
    val scanHiddenFolders: Boolean = false,
    val useDarkMode: Boolean = true,
    val isBusy: Boolean = false,
    val scanProgressLabel: String? = null,
    val operationProgressLabel: String? = null,
    val operationProgress: Float? = null,
    val conversionReport: ConversionReport? = null,
    val currentJobProgress: Float? = null,
    val currentJobLabel: String? = null,
    val storageInfo: RomTreeRepository.StorageInfo? = null,
    val stopRequest: StopRequest = StopRequest.None,
    val previewCount: Int = 0,
    val previewPlan: OperationPlan? = null,
    val selectedChangePaths: Set<String> = emptySet(),
    val message: StatusMessage? = null,
)

/** A status line for the current tool; [isError] marks failures. */
data class StatusMessage(val text: String, val isError: Boolean = false)

data class ConversionReport(
    val completed: Int,
    val total: Int,
    val spaceSavedBytes: Long = 0L,
    val stopped: Boolean = false,
)

class MimirViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RomTreeRepository(application)
    private val executor = PlanExecutor(application)
    private val esDeRepository = EsDeSystemsRepository(application)
    private val prefs = application.getSharedPreferences("mimir_prefs", 0)
    private var vitaCatalog: List<pup.app.mimir.domain.VitaApp> = emptyList()
    private var activeCancellation: OperationCancellation? = null
    private var esDeLatestRequested = false
    private val nszKeysFile = File(application.filesDir, NSZ_KEYS_RELATIVE_PATH)

    private val _uiState = MutableStateFlow(
        MimirUiState(
            selectedFolderUri = prefs.getString(KEY_URI, null)?.let(Uri::parse),
            selectedFolderName = prefs.getString(KEY_LABEL, null) ?: application.getString(R.string.no_rom_folder_selected),
            vitaOutputUri = prefs.getString(KEY_VITA_OUTPUT_URI, null)?.let(Uri::parse),
            vitaOutputName = prefs.getString(KEY_VITA_OUTPUT_LABEL, null) ?: application.getString(R.string.no_vita_output_directory_selected),
            vitaShortcutFormat = prefs.getString(KEY_VITA_SHORTCUT_FORMAT, null)
                ?.let { storedFormat -> VitaShortcutFormat.entries.find { it.name == storedFormat } }
                ?: VitaShortcutFormat.Psvita,
            nszKeysConfigured = nszKeysFile.isFile && nszKeysFile.length() > 0L,
            esDeRootUri = prefs.getString(KEY_ESDE_URI, null)?.let(Uri::parse),
            esDeRootName = prefs.getString(KEY_ESDE_LABEL, null) ?: application.getString(R.string.no_esde_folder_selected),
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
            _uiState.value.esDeRootUri?.let { rootUri ->
                val systems = runCatching {
                    val installed = esDeRepository.loadInstalled(rootUri)
                    val romRoot = _uiState.value.selectedFolderUri
                    if (romRoot == null) installed else {
                        val folders = esDeRepository.scanRomFolders(romRoot).map(String::lowercase).toSet()
                        installed.filter { it.name.lowercase() in folders }
                    }
                }.getOrElse { emptyList() }
                _uiState.update { it.copy(esDeSystems = systems) }
            }
        }
    }

    fun onEsDeFolderSelected(uri: Uri) {
        val label = treeLabel(uri)
        esDeLatestRequested = false
        prefs.edit()
            .putString(KEY_ESDE_URI, uri.toString())
            .putString(KEY_ESDE_LABEL, label)
            .apply()
        _uiState.update { it.copy(esDeRootUri = uri, esDeRootName = label, esDeSystems = emptyList(), message = null) }
        viewModelScope.launch(Dispatchers.IO) {
            val systems = runCatching {
                val installed = esDeRepository.loadInstalled(uri)
                val romRoot = _uiState.value.selectedFolderUri
                if (romRoot == null) {
                    installed
                } else {
                    val folders = esDeRepository.scanRomFolders(romRoot).map(String::lowercase).toSet()
                    installed.filter { it.name.lowercase() in folders }
                }
            }.getOrElse { emptyList() }
            _uiState.update { it.copy(esDeSystems = systems) }
        }
    }

    fun onEsDeSystemFolderSelected(name: String, uri: Uri) {
        val path = EsDeSystemsRepository.absolutePathForTreeUri(uri) ?: run {
            _uiState.update { it.copy(message = failure(R.string.msg_rom_path_unresolved)) }
            return
        }
        _uiState.update { state ->
            state.copy(esDeSystems = state.esDeSystems.map { system ->
                if (system.name == name) system.copy(romFolder = path) else system
            })
        }
    }

    fun defaultEsDeFolderUri(name: String): Uri? {
        val rootUri = _uiState.value.selectedFolderUri ?: return null
        val system = _uiState.value.esDeSystems.firstOrNull { it.name == name } ?: return null
        if (!system.isDefault) return null
        return EsDeSystemsRepository.childTreeUri(rootUri, system.romFolder)
    }

    fun refreshEsDeSystems() {
        val romRootUri = _uiState.value.selectedFolderUri
        if (_uiState.value.esDeRootUri == null) {
            _uiState.update { it.copy(message = info(R.string.msg_select_esde_first)) }
            return
        }
        if (romRootUri == null) {
            _uiState.update { it.copy(message = info(R.string.msg_select_rom_root_find)) }
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isBusy = true, message = info(R.string.msg_esde_downloading)) }
            runCatching {
                val catalog = esDeRepository.fetchLatest()
                esDeRepository.systemsForRomFolders(catalog, esDeRepository.scanRomFolders(romRootUri))
            }
                .onSuccess { systems ->
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            esDeSystems = mergeEsDeOverrides(systems, it.esDeSystems),
                            message = StatusMessage(plural(R.plurals.msg_esde_downloaded, systems.size, systems.size)),
                        )
                    }
                    esDeLatestRequested = true
                }
                .onFailure { error ->
                    Log.e(TAG, "Unable to download ES-DE systems", error)
                    _uiState.update { it.copy(isBusy = false, message = failure(error, R.string.msg_esde_download_failed)) }
                }
        }
    }

    fun applyEsDeSystems() {
        val rootUri = _uiState.value.esDeRootUri
        val systems = _uiState.value.esDeSystems
        val romRootUri = _uiState.value.selectedFolderUri
        if (rootUri == null) {
            _uiState.update { it.copy(message = info(R.string.msg_select_esde_first)) }
            return
        }
        if (systems.isEmpty()) {
            _uiState.update { it.copy(message = info(R.string.msg_esde_download_first)) }
            return
        }
        if (romRootUri == null) {
            _uiState.update { it.copy(message = info(R.string.msg_select_rom_root_map)) }
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isBusy = true, message = info(R.string.msg_esde_installing)) }
            runCatching {
                if (esDeLatestRequested) {
                    esDeRepository.installLatest(
                        rootUri = rootUri,
                        systems = systems,
                    )
                } else {
                    esDeRepository.installCurrent(
                    rootUri = rootUri,
                    systems = systems,
                    )
                }
            }
                .onSuccess { count ->
                    esDeLatestRequested = false
                    _uiState.update { it.copy(isBusy = false, message = StatusMessage(plural(R.plurals.msg_esde_installed, count, count))) }
                }
                .onFailure { error ->
                    Log.e(TAG, "Unable to install ES-DE systems", error)
                    _uiState.update { it.copy(isBusy = false, message = failure(error, R.string.msg_esde_install_failed)) }
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

    fun updateConverterTool(tool: ConverterTool) {
        _uiState.update {
            it.copy(
                selectedConverterTool = tool,
                previewPlan = null,
                previewCount = 0,
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
        _uiState.update { it.copy(stopRequest = StopRequest.Now, message = info(R.string.msg_stopping)) }
    }

    fun stopAfterCurrentConversion() {
        activeCancellation?.stopAfterCurrent()
        _uiState.update { it.copy(stopRequest = StopRequest.AfterCurrent, message = info(R.string.msg_stop_after_current)) }
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
                .filterNot { state.selectedMode == ToolMode.Converter && it.targetAlreadyExists }
                .mapTo(linkedSetOf()) { it.detailPath })
        }
    }

    fun deselectAllChanges() {
        _uiState.update { it.copy(selectedChangePaths = emptySet()) }
    }

    fun addVitaShortcut(app: pup.app.mimir.domain.VitaApp) {
        val outputUri = _uiState.value.vitaOutputUri ?: run {
            _uiState.update { it.copy(message = info(R.string.msg_select_vita_output_first)) }
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
                        message = StatusMessage(plan.conflicts.firstOrNull() ?: text(R.string.msg_vita_add_unable, app.title), isError = true),
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
                            message = info(R.string.msg_vita_added, app.title),
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isBusy = false, message = failure(error, R.string.msg_vita_add_failed))
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
                            message = info(R.string.msg_vita_removed, app.title),
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isBusy = false, message = failure(error, R.string.msg_vita_remove_failed))
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

    fun onNszKeysSelected(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isBusy = true, message = null) }
            runCatching {
                val parent = nszKeysFile.parentFile
                    ?: error(text(R.string.msg_keys_directory_failed))
                require(parent.exists() || parent.mkdirs()) { text(R.string.msg_keys_directory_failed) }
                val temporary = File(parent, "prod.keys.importing")
                getApplication<Application>().contentResolver.openInputStream(uri).use { input ->
                    requireNotNull(input) { text(R.string.msg_keys_unreadable) }
                    temporary.outputStream().use { output -> input.copyTo(output) }
                }
                require(temporary.length() > 0L) { text(R.string.msg_keys_empty) }
                if (!temporary.renameTo(nszKeysFile)) {
                    temporary.copyTo(nszKeysFile, overwrite = true)
                    require(temporary.delete()) { text(R.string.msg_keys_finish_failed) }
                }
            }
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            nszKeysConfigured = true,
                            message = info(R.string.msg_keys_imported),
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isBusy = false, message = failure(error, R.string.msg_keys_import_failed))
                    }
                }
        }
    }

    fun scan() {
        val currentState = _uiState.value
        val mode = currentState.selectedMode
        val romUri = currentState.selectedFolderUri

        when (mode) {
            ToolMode.MultiDiscOrganizer, ToolMode.RomZipper, ToolMode.Converter -> {
                if (romUri == null) {
                    _uiState.update { it.copy(message = info(R.string.msg_select_rom_folder_first)) }
                    return
                }
                if (mode == ToolMode.Converter &&
                    currentState.selectedConverterTool == ConverterTool.NszNsp &&
                    !currentState.nszKeysConfigured
                ) {
                    _uiState.update { it.copy(message = info(R.string.msg_import_keys_first)) }
                    return
                }
            }
            ToolMode.VitaAppIds, ToolMode.EsDeSystems -> return
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
                            it.copy(scanProgressLabel = plural(R.plurals.scan_checking_multidisc, entries.size, entries.size))
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
                            it.copy(scanProgressLabel = plural(R.plurals.scan_checking_zip, entries.size, entries.size))
                        }
                        RomZipperPlanner.buildPlan(entries)
                    }

                    ToolMode.Converter -> {
                        when (currentState.selectedConverterTool) {
                            ConverterTool.Chd -> {
                                val entries = repository.scanChdTree(
                                    rootUri = requireNotNull(romUri),
                                    folderAliases = currentState.selectedChdSystem.folderAliases,
                                    supportedExtensions = currentState.selectedChdSystem.supportedExtensions,
                                    onFileScanned = { scannedFiles -> updateScanProgress(mode, scannedFiles) },
                                    shouldStop = cancellation::shouldInterruptCurrentOperation,
                                )
                                _uiState.update {
                                    it.copy(scanProgressLabel = plural(R.plurals.scan_checking_chd, entries.size, entries.size, currentState.selectedChdSystem.displayName))
                                }
                                ChdPlanner.buildPlan(
                                    entries = entries,
                                    system = currentState.selectedChdSystem,
                                    discType = currentState.selectedChdDiscType,
                                    deleteOriginalFiles = currentState.deleteOriginalChdFiles,
                                )
                            }
                            else -> {
                                val tool = currentState.selectedConverterTool
                                val scannedEntries = repository.scanChdTree(
                                    rootUri = requireNotNull(romUri),
                                    folderAliases = tool.folderAliases,
                                    supportedExtensions = tool.sourceExtensions,
                                    outputExtension = tool.outputExtension,
                                    onFileScanned = { scannedFiles -> updateScanProgress(mode, scannedFiles) },
                                    shouldStop = cancellation::shouldInterruptCurrentOperation,
                                )
                                _uiState.update {
                                    it.copy(scanProgressLabel = plural(R.plurals.scan_checking_tool, scannedEntries.size, scannedEntries.size, tool.displayName))
                                }
                                val entries = if (tool == ConverterTool.XboxXiso) {
                                    repository.filterXboxImagesNeedingXiso(
                                        rootUri = requireNotNull(romUri),
                                        entries = scannedEntries,
                                        shouldStop = cancellation::shouldInterruptCurrentOperation,
                                    )
                                } else {
                                    scannedEntries
                                }
                                NativeConverterPlanner.buildPlan(entries, tool)
                            }
                        }
                    }

                    ToolMode.VitaAppIds, ToolMode.EsDeSystems -> error("This tool does not use ROM scanning.")
                }
                val previewCount = plan.changes.size
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        scanProgressLabel = null,
                        previewCount = previewCount,
                        previewPlan = plan,
                        storageInfo = if (mode == ToolMode.Converter) {
                            repository.storageInfo(requireNotNull(romUri))
                        } else it.storageInfo,
                        selectedChangePaths = plan.changes
                            .filterNot { mode == ToolMode.Converter && it.targetAlreadyExists }
                            .mapTo(linkedSetOf()) { it.detailPath },
                        message = if (plan.changes.isEmpty()) {
                            when (mode) {
                                ToolMode.MultiDiscOrganizer -> info(R.string.msg_none_multidisc)
                                ToolMode.RomZipper -> info(R.string.msg_none_zip)
                                ToolMode.Converter -> info(R.string.msg_none_converter, currentState.selectedConverterTool.displayName)
                                ToolMode.VitaAppIds -> info(R.string.msg_none_vita)
                                ToolMode.EsDeSystems -> info(R.string.msg_none_esde)
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
                        message = if (error is OperationStoppedException) info(R.string.msg_scan_stopped) else failure(error, R.string.msg_scan_failed),
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
            ToolMode.EsDeSystems -> null
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
                    conversionReport = if (plan.mode == ToolMode.Converter) {
                        ConversionReport(completed = 0, total = plan.operations.size)
                    } else {
                        it.conversionReport
                    },
                    currentJobProgress = if (plan.mode == ToolMode.Converter) 0f else null,
                    currentJobLabel = if (plan.mode == ToolMode.Converter) null else it.currentJobLabel,
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
                            conversionReport = if (plan.mode == ToolMode.Converter) {
                                ConversionReport(
                                    completed = completed,
                                    total = total,
                                    spaceSavedBytes = it.conversionReport?.spaceSavedBytes ?: 0L,
                                )
                            } else {
                                it.conversionReport
                            },
                            currentJobProgress = if (plan.mode == ToolMode.Converter) 0f else null,
                        )
                    }
                },
                onCurrentOperationProgress = { progress ->
                    if (plan.mode == ToolMode.Converter) {
                        _uiState.update { it.copy(currentJobProgress = progress) }
                    }
                },
                onOperationStarted = { operation ->
                    if (operation is FileOperation.ConvertToChd || operation is FileOperation.ConvertWithTool) {
                        val sourcePath = when (operation) {
                            is FileOperation.ConvertToChd -> operation.sourcePath
                            is FileOperation.ConvertWithTool -> operation.sourcePath
                            else -> error("Unsupported converter operation.")
                        }
                        _uiState.update {
                            it.copy(
                                currentJobLabel = sourcePath
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
                            currentJobProgress = null,
                            currentJobLabel = null,
                            conversionReport = if (plan.mode == ToolMode.Converter) {
                                ConversionReport(
                                    completed = result.completedOperations,
                                    total = plan.operations.size,
                                    spaceSavedBytes = result.spaceSavedBytes,
                                    stopped = result.stopped,
                                )
                            } else {
                                it.conversionReport
                            },
                            stopRequest = StopRequest.None,
                            message = if (result.stopped) {
                                StatusMessage(
                                    plural(R.plurals.msg_stopped_after, plan.operations.size, result.completedOperations, plan.operations.size),
                                )
                            } else {
                                info(R.string.msg_changes_applied)
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
                            currentJobProgress = null,
                            currentJobLabel = null,
                            conversionReport = if (plan.mode == ToolMode.Converter) {
                                ConversionReport(
                                    completed = it.conversionReport?.completed ?: 0,
                                    total = plan.operations.size,
                                    stopped = error is OperationStoppedException,
                                )
                            } else {
                                it.conversionReport
                            },
                            stopRequest = StopRequest.None,
                            message = if (error is OperationStoppedException) {
                                info(R.string.msg_conversion_stopped)
                            } else {
                                failure(error, R.string.msg_apply_failed)
                            },
                        )
                    }
                }
            if (activeCancellation === cancellation) activeCancellation = null
        }
    }

    companion object {
        private const val TAG = "Mimir.ESDE"
        private const val KEY_URI = "rom_tree_uri"
        private const val KEY_LABEL = "rom_tree_label"
        private const val KEY_VITA_OUTPUT_URI = "vita_output_uri"
        private const val KEY_VITA_OUTPUT_LABEL = "vita_output_label"
        private const val KEY_ESDE_URI = "esde_root_uri"
        private const val KEY_ESDE_LABEL = "esde_root_label"
        private const val KEY_VITA_SHORTCUT_FORMAT = "vita_shortcut_format"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_SCAN_HIDDEN_FOLDERS = "scan_hidden_folders"
        private const val KEY_DELETE_ORIGINAL_CHD_FILES = "delete_original_chd_files"
        private const val NSZ_KEYS_RELATIVE_PATH = "nsz/prod.keys"
        private const val MAX_VITA_RESULTS = 40
        private const val SCAN_PROGRESS_UPDATE_INTERVAL = 25
    }

    private fun text(@StringRes id: Int, vararg args: Any): String =
        getApplication<Application>().getString(id, *args)

    private fun plural(@PluralsRes id: Int, count: Int, vararg args: Any): String =
        getApplication<Application>().resources.getQuantityString(id, count, *args)

    private fun info(@StringRes id: Int, vararg args: Any) = StatusMessage(text(id, *args))

    private fun failure(@StringRes id: Int, vararg args: Any) = StatusMessage(text(id, *args), isError = true)

    private fun failure(error: Throwable, @StringRes fallback: Int) =
        StatusMessage(error.message ?: text(fallback), isError = true)

    private fun treeLabel(uri: Uri): String =
        DocumentsContract.getTreeDocumentId(uri).substringAfterLast(':')

    private fun OperationPlan.withChdDeleteOriginalFiles(deleteOriginalFiles: Boolean): OperationPlan {
        if (mode != ToolMode.Converter) return this
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

    private fun mergeEsDeOverrides(
        latest: List<EsDeSystem>,
        existing: List<EsDeSystem>,
    ): List<EsDeSystem> {
        val existingByName = existing.associateBy { it.name }
        return latest.map { system ->
            val prior = existingByName[system.name]
            if (prior != null && !prior.isDefault && prior.romFolder.isNotBlank()) {
                system.copy(romFolder = prior.romFolder)
            } else {
                system
            }
        }
    }

    private fun initialScanLabel(mode: ToolMode): String =
        when (mode) {
            ToolMode.MultiDiscOrganizer -> text(R.string.scan_start_multidisc)
            ToolMode.RomZipper -> text(R.string.scan_start_zip)
            ToolMode.Converter -> text(R.string.scan_start_tool, _uiState.value.selectedConverterTool.displayName)
            ToolMode.VitaAppIds -> text(R.string.scan_start_files)
            ToolMode.EsDeSystems -> ""
        }

    private fun updateScanProgress(mode: ToolMode, scannedFiles: Int) {
        if (scannedFiles % SCAN_PROGRESS_UPDATE_INTERVAL != 0) return
        val activity = when (mode) {
            ToolMode.MultiDiscOrganizer -> text(R.string.scan_activity_multidisc)
            ToolMode.RomZipper -> text(R.string.scan_activity_zip)
            ToolMode.Converter -> text(R.string.scan_activity_tool, _uiState.value.selectedConverterTool.displayName)
            ToolMode.VitaAppIds -> text(R.string.scan_activity_files)
            ToolMode.EsDeSystems -> ""
        }
        _uiState.update {
            it.copy(scanProgressLabel = plural(R.plurals.scan_files_checked, scannedFiles, activity, scannedFiles))
        }
    }

    private fun applyProgressLabel(mode: ToolMode, completed: Int, total: Int): String {
        val activity = when (mode) {
            ToolMode.MultiDiscOrganizer -> text(R.string.apply_activity_organizing)
            ToolMode.RomZipper -> text(R.string.apply_activity_zipping)
            ToolMode.Converter -> text(R.string.apply_activity_converting, _uiState.value.selectedConverterTool.displayName)
            ToolMode.VitaAppIds -> text(R.string.apply_activity_shortcuts)
            ToolMode.EsDeSystems -> text(R.string.apply_activity_esde)
        }
        return text(R.string.apply_progress, activity, completed, total)
    }

    private suspend fun refreshExistingVitaShortcuts(
        uri: Uri,
        format: VitaShortcutFormat = _uiState.value.vitaShortcutFormat,
    ) {
        val existing = runCatching { repository.loadExistingVitaShortcuts(uri, format) }.getOrElse { emptyMap() }
        _uiState.update { it.copy(addedVitaShortcuts = existing) }
    }
}
