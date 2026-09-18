package pup.app.mimir_flutter

import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.runBlocking
import pup.app.mimir.data.EsDeSystem
import pup.app.mimir.data.EsDeSystemsRepository
import pup.app.mimir.data.OperationCancellation
import pup.app.mimir.data.OperationStoppedException
import pup.app.mimir.data.PlanExecutor
import pup.app.mimir.data.RomTreeRepository
import pup.app.mimir.domain.ChdDiscType
import pup.app.mimir.domain.ChdSystem
import pup.app.mimir.domain.ConverterTool
import pup.app.mimir.domain.FileOperation
import pup.app.mimir.domain.FrontendPreset
import pup.app.mimir.domain.OperationPlan
import pup.app.mimir.domain.PlannedChange
import pup.app.mimir.domain.RomEntry
import pup.app.mimir.domain.ToolMode
import pup.app.mimir.domain.VitaShortcutFormat
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/**
 * Thin Android host for the Flutter UI.
 *
 * SAF URIs, persisted permissions, converter workspaces, Chaquopy, and the
 * existing Mimir preference file stay here. Flutter receives opaque handles
 * and versioned operation events only.
 */
class MainActivity : FlutterActivity() {
    private val background = Executors.newCachedThreadPool()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val repository by lazy { RomTreeRepository(this) }
    private val executor by lazy { PlanExecutor(this) }
    private val esDeRepository by lazy { EsDeSystemsRepository(this) }
    private val legacyPreferences by lazy { getSharedPreferences(LEGACY_PREFERENCES, MODE_PRIVATE) }
    private val activeOperations = ConcurrentHashMap<String, OperationCancellation>()
    private val eventSinks = ConcurrentHashMap<String, EventChannel.EventSink>()
    private val queuedEvents = ConcurrentHashMap<String, MutableList<Map<String, Any?>>>()
    private var activeScan: OperationCancellation? = null
    private var pendingPickerResult: MethodChannel.Result? = null
    private var pendingPickerPurpose: String? = null
    private var pendingFileResult: MethodChannel.Result? = null

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, PLATFORM_CHANNEL)
            .setMethodCallHandler(::handleMethodCall)
        EventChannel(flutterEngine.dartExecutor.binaryMessenger, EVENTS_CHANNEL)
            .setStreamHandler(object : EventChannel.StreamHandler {
                override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
                    val sessionId = arguments?.toString() ?: return
                    if (events == null) return
                    eventSinks[sessionId] = events
                    queuedEvents.remove(sessionId)?.forEach { events.success(it) }
                }

                override fun onCancel(arguments: Any?) {
                    val sessionId = arguments?.toString() ?: return
                    eventSinks.remove(sessionId)
                    queuedEvents.remove(sessionId)
                }
            })
    }

    override fun onDestroy() {
        activeOperations.values.forEach(OperationCancellation::stopNow)
        activeScan?.stopNow()
        background.shutdownNow()
        super.onDestroy()
    }

    @Deprecated("Activity result callbacks are retained for the Flutter host bridge.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == DIRECTORY_REQUEST) {
            val result = pendingPickerResult ?: return
            val purpose = pendingPickerPurpose
            pendingPickerResult = null
            pendingPickerPurpose = null
            val uri = data?.data?.takeIf { resultCode == RESULT_OK }
            if (uri != null) {
                persistTreePermission(uri)
                persistSelection(purpose, uri)
            }
            result.success(uri?.toString())
        } else if (requestCode == FILE_REQUEST) {
            val result = pendingFileResult ?: return
            pendingFileResult = null
            val uri = data?.data?.takeIf { resultCode == RESULT_OK }
            result.success(uri?.toString())
        }
    }

    private fun handleMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "pickDirectory" -> pickDirectory(call, result)
            "pickFile" -> pickFile(result)
            "scan" -> scan(call, result)
            "storageInfo" -> storageInfo(call, result)
            "apply" -> apply(call, result)
            "stopOperation" -> stopOperation(call, result)
            "importNszKeys" -> importNszKeys(call, result)
            "hasNszKeys" -> {
                val keys = File(filesDir, "nsz/prod.keys")
                result.success(keys.isFile && keys.length() > 0L)
            }
            "deleteOutputFile" -> deleteOutputFile(call, result)
            "loadExistingVitaShortcuts" -> loadExistingVitaShortcuts(call, result)
            "loadInstalledEsDe" -> loadInstalledEsDe(call, result)
            "fetchEsDeSystems" -> fetchEsDeSystems(call, result)
            "installEsDeSystems" -> installEsDeSystems(call, result)
            "defaultEsDeSystemFolder" -> defaultEsDeSystemFolder(call, result)
            "legacySetting" -> result.success(legacyPreferences.all[call.stringArgument("key")])
            "saveLegacySetting" -> saveLegacySetting(call, result)
            else -> result.notImplemented()
        }
    }

    private fun pickDirectory(call: MethodCall, result: MethodChannel.Result) {
        if (pendingPickerResult != null) {
            result.error("PICKER_BUSY", "A folder picker is already open.", null)
            return
        }
        pendingPickerResult = result
        pendingPickerPurpose = call.stringArgument("purpose")
        val initial = call.stringArgument("initialHandle")
            ?.takeIf(String::isNotBlank)
            ?.let(Uri::parse)
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION,
            )
            if (initial != null) putExtra("android.provider.extra.INITIAL_URI", initial)
        }
        startActivityForResult(intent, DIRECTORY_REQUEST)
    }

    private fun pickFile(result: MethodChannel.Result) {
        if (pendingFileResult != null) {
            result.error("PICKER_BUSY", "A file picker is already open.", null)
            return
        }
        pendingFileResult = result
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        startActivityForResult(intent, FILE_REQUEST)
    }

    private fun scan(call: MethodCall, result: MethodChannel.Result) {
        val root = call.stringArgument("rootHandle")?.let(Uri::parse)
        if (root == null) {
            result.error("INVALID_ROOT", "A ROM folder is required.", null)
            return
        }
        val mode = call.stringArgument("mode")
        val hidden = call.booleanArgument("scanHiddenFolders")
        val cancellation = OperationCancellation()
        activeScan = cancellation
        background.execute {
            try {
                val entries = if (mode == "multiDiscOrganizer" || mode == "romZipper") {
                    repository.scanTree(
                        rootUri = root,
                        scanHiddenFolders = hidden,
                        shouldStop = cancellation::shouldInterruptCurrentOperation,
                    )
                } else {
                    val converter = converterTool(call.stringArgument("converterTool"))
                    val system = chdSystem(call.stringArgument("chdSystem"))
                    repository.scanChdTree(
                        rootUri = root,
                        folderAliases = if (converter == ConverterTool.Chd) system?.folderAliases.orEmpty() else converter?.folderAliases.orEmpty(),
                        supportedExtensions = if (converter == ConverterTool.Chd) system?.supportedExtensions.orEmpty() else converter?.sourceExtensions.orEmpty(),
                        outputExtension = if (converter == ConverterTool.Chd) "chd" else converter?.outputExtension.orEmpty(),
                        shouldStop = cancellation::shouldInterruptCurrentOperation,
                    )
                }
                postSuccess(result, entries.map(::romEntryToMap))
            } catch (error: Exception) {
                postError(result, error)
            } finally {
                if (activeScan === cancellation) activeScan = null
            }
        }
    }

    private fun storageInfo(call: MethodCall, result: MethodChannel.Result) {
        val root = call.stringArgument("rootHandle")?.let(Uri::parse)
        if (root == null) {
            result.success(null)
            return
        }
        val info = repository.storageInfo(root)
        result.success(info?.let { mapOf("totalBytes" to it.totalBytes, "freeBytes" to it.freeBytes) })
    }

    private fun apply(call: MethodCall, result: MethodChannel.Result) {
        val sessionId = call.stringArgument("sessionId") ?: UUID.randomUUID().toString()
        val root = call.stringArgument("rootHandle")?.let(Uri::parse)
        val rawPlan = call.mapArgument("plan")
        if (root == null || rawPlan == null) {
            result.error("INVALID_PLAN", "A root folder and operation plan are required.", null)
            return
        }
        val plan = try {
            planFromMap(rawPlan)
        } catch (error: Exception) {
            result.error("INVALID_PLAN", error.message, null)
            return
        }
        val cancellation = OperationCancellation()
        activeOperations[sessionId] = cancellation
        result.success(sessionId)
        background.execute {
            var completed = 0
            var current: FileOperation? = null
            try {
                val execution = executor.apply(
                    rootUri = root,
                    plan = plan,
                    cancellation = cancellation,
                    onOperationStarted = { operation ->
                        current = operation
                        emitEvent(
                            sessionId,
                            operationEvent(
                                completed = completed,
                                total = plan.operations.size,
                                current = operationLabel(operation),
                                currentProgress = if (isConversion(operation)) 0.0 else null,
                            ),
                        )
                    },
                    onCurrentOperationProgress = { progress ->
                        emitEvent(
                            sessionId,
                            operationEvent(
                                completed = completed,
                                total = plan.operations.size,
                                current = current?.let(::operationLabel),
                                currentProgress = progress.toDouble(),
                            ),
                        )
                    },
                    onProgress = { finished, total ->
                        completed = finished
                        emitEvent(
                            sessionId,
                            operationEvent(
                                completed = finished,
                                total = total,
                                current = current?.let(::operationLabel),
                                currentProgress = if (isConversion(current)) 1.0 else null,
                            ),
                        )
                    },
                )
                execution.fold(
                    onSuccess = { value ->
                        emitEvent(
                            sessionId,
                            operationEvent(
                                completed = value.completedOperations,
                                total = plan.operations.size,
                                spaceSavedBytes = value.spaceSavedBytes,
                                stopped = value.stopped,
                                finished = true,
                            ),
                        )
                    },
                    onFailure = { error ->
                        emitEvent(
                            sessionId,
                            operationEvent(
                                completed = completed,
                                total = plan.operations.size,
                                current = current?.let(::operationLabel),
                                stopped = error is OperationStoppedException,
                                finished = true,
                                error = error.message ?: error.toString(),
                            ),
                        )
                    },
                )
            } catch (error: Exception) {
                emitEvent(
                    sessionId,
                    operationEvent(
                        completed = completed,
                        total = plan.operations.size,
                        current = current?.let(::operationLabel),
                        stopped = error is OperationStoppedException,
                        finished = true,
                        error = error.message ?: error.toString(),
                    ),
                )
            } finally {
                activeOperations.remove(sessionId)
            }
        }
    }

    private fun stopOperation(call: MethodCall, result: MethodChannel.Result) {
        val request = call.stringArgument("mode")
        val sessionId = call.stringArgument("sessionId")
        if (sessionId == null) {
            activeScan?.let { cancellation -> if (request == "now") cancellation.stopNow() else cancellation.stopAfterCurrent() }
        } else {
            activeOperations[sessionId]?.let { cancellation -> if (request == "now") cancellation.stopNow() else cancellation.stopAfterCurrent() }
        }
        result.success(null)
    }

    private fun importNszKeys(call: MethodCall, result: MethodChannel.Result) {
        val source = call.stringArgument("sourceHandle")?.let(Uri::parse)
        if (source == null) {
            result.error("INVALID_KEYS", "A prod.keys file is required.", null)
            return
        }
        background.execute {
            try {
                val directory = File(filesDir, "nsz")
                require(directory.exists() || directory.mkdirs()) { "Unable to prepare Mimir's NSZ key directory." }
                val temporary = File(directory, "prod.keys.importing")
                contentResolver.openInputStream(source).use { input ->
                    requireNotNull(input) { "Unable to read the selected prod.keys file." }
                    temporary.outputStream().use(input::copyTo)
                }
                require(temporary.length() > 0L) { "The selected prod.keys file is empty." }
                if (!temporary.renameTo(File(directory, "prod.keys"))) {
                    temporary.copyTo(File(directory, "prod.keys"), overwrite = true)
                    require(temporary.delete()) { "Unable to finish importing prod.keys." }
                }
                postSuccess(result, null)
            } catch (error: Exception) {
                postError(result, error)
            }
        }
    }

    private fun deleteOutputFile(call: MethodCall, result: MethodChannel.Result) {
        val root = call.stringArgument("rootHandle")?.let(Uri::parse)
        val relativePath = call.stringArgument("relativePath")
        if (root == null || relativePath == null) {
            result.error("INVALID_PATH", "A root folder and relative path are required.", null)
            return
        }
        background.execute {
            executor.deleteOutputFile(root, relativePath).fold(
                onSuccess = { postSuccess(result, null) },
                onFailure = { postError(result, it) },
            )
        }
    }

    private fun loadExistingVitaShortcuts(call: MethodCall, result: MethodChannel.Result) {
        val root = call.stringArgument("rootHandle")?.let(Uri::parse)
        val format = vitaFormat(call.stringArgument("format"))
        if (root == null || format == null) {
            result.error("INVALID_VITA_ROOT", "A Vita output folder and format are required.", null)
            return
        }
        background.execute { postSuccess(result, repository.loadExistingVitaShortcuts(root, format)) }
    }

    private fun loadInstalledEsDe(call: MethodCall, result: MethodChannel.Result) {
        val root = call.stringArgument("rootHandle")?.let(Uri::parse)
        if (root == null) {
            result.success(emptyList<Map<String, Any?>>())
            return
        }
        val romRoot = call.stringArgument("romRootHandle")?.let(Uri::parse)
        background.execute {
            try {
                val installed = runBlocking { esDeRepository.loadInstalled(root) }
                val filtered = if (romRoot == null) installed else {
                    val folders = runBlocking { esDeRepository.scanRomFolders(romRoot) }.map(String::lowercase).toSet()
                    installed.filter { it.name.lowercase() in folders }
                }
                postSuccess(result, filtered.map(::esDeSystemToMap))
            } catch (error: Exception) {
                postError(result, error)
            }
        }
    }

    private fun fetchEsDeSystems(call: MethodCall, result: MethodChannel.Result) {
        val romRoot = call.stringArgument("romRootHandle")?.let(Uri::parse)
        if (romRoot == null) {
            result.error("INVALID_ROM_ROOT", "A ROM root folder is required.", null)
            return
        }
        background.execute {
            try {
                val systems = runBlocking {
                    val catalog = esDeRepository.fetchLatest()
                    esDeRepository.systemsForRomFolders(catalog, esDeRepository.scanRomFolders(romRoot))
                }
                postSuccess(result, systems.map(::esDeSystemToMap))
            } catch (error: Exception) {
                postError(result, error)
            }
        }
    }

    private fun installEsDeSystems(call: MethodCall, result: MethodChannel.Result) {
        val esdeRoot = call.stringArgument("esdeRootHandle")?.let(Uri::parse)
        val systems = call.listArgument("systems")?.mapNotNull { (it as? Map<*, *>)?.let(::esDeSystemFromMap) }
        if (esdeRoot == null || systems == null) {
            result.error("INVALID_ESDE_ROOT", "An ES-DE folder and systems are required.", null)
            return
        }
        val latest = call.booleanArgument("useLatestCatalog")
        background.execute {
            try {
                val count = runBlocking {
                    if (latest) esDeRepository.installLatest(esdeRoot, systems)
                    else esDeRepository.installCurrent(esdeRoot, systems)
                }
                postSuccess(result, count)
            } catch (error: Exception) {
                postError(result, error)
            }
        }
    }

    private fun defaultEsDeSystemFolder(call: MethodCall, result: MethodChannel.Result) {
        val romRoot = call.stringArgument("romRootHandle")?.let(Uri::parse)
        val system = call.mapArgument("system")?.let(::esDeSystemFromMap)
        if (romRoot == null || system == null || !system.isDefault) {
            result.success(null)
            return
        }
        result.success(EsDeSystemsRepository.childTreeUri(romRoot, system.romFolder)?.toString())
    }

    private fun saveLegacySetting(call: MethodCall, result: MethodChannel.Result) {
        val key = call.stringArgument("key")
        val value = call.argument<Any?>("value")
        if (key == null || value == null) {
            result.error("INVALID_SETTING", "A setting key and value are required.", null)
            return
        }
        legacyPreferences.edit().apply {
            when (value) {
                is Boolean -> putBoolean(key, value)
                is Number -> putLong(key, value.toLong())
                else -> putString(key, value.toString())
            }
        }.apply()
        result.success(null)
    }

    private fun persistTreePermission(uri: Uri) {
        runCatching {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }
    }

    private fun persistSelection(purpose: String?, uri: Uri) {
        val key = when (purpose) {
            "romRoot" -> "rom_tree_uri"
            "vitaOutput" -> "vita_output_uri"
            "esdeRoot" -> "esde_root_uri"
            else -> null
        } ?: return
        legacyPreferences.edit().putString(key, uri.toString()).apply()
    }

    private fun emitEvent(sessionId: String, event: Map<String, Any?>) {
        mainHandler.post {
            val sink = eventSinks[sessionId]
            if (sink != null) {
                sink.success(event)
            } else {
                queuedEvents.getOrPut(sessionId) { mutableListOf() }.add(event)
            }
        }
    }

    private fun postSuccess(result: MethodChannel.Result, value: Any?) = mainHandler.post { result.success(value) }

    private fun postError(result: MethodChannel.Result, error: Throwable) = mainHandler.post {
        result.error("MIMIR_ERROR", error.message ?: error.toString(), null)
    }

    private fun planFromMap(raw: Map<*, *>): OperationPlan {
        val version = (raw["version"] as? Number)?.toInt()
        require(version == null || version == WIRE_VERSION) {
            "Unsupported Mimir wire version: $version"
        }
        val changes = raw.list("changes").map { parseChange(it.asMap()) }
        val operations = raw.list("operations").map { parseOperation(it.asMap()) }
        return OperationPlan(
            mode = toolMode(raw.string("mode")) ?: error("Unknown operation mode."),
            preset = raw.string("preset")?.let(::frontendPreset),
            changes = changes,
            operations = operations,
            conflicts = raw.list("conflicts").map { it.toString() },
        )
    }

    private fun parseChange(raw: Map<*, *>): PlannedChange = PlannedChange(
        title = raw.string("title").orEmpty(),
        sourceFiles = raw.list("sourceFiles").map { it.toString() },
        targetFiles = raw.list("targetFiles").map { it.toString() },
        detailLabel = raw.string("detailLabel").orEmpty(),
        detailPath = raw.string("detailPath").orEmpty(),
        operations = raw.list("operations").map { parseOperation(it.asMap()) },
        sourceSizeBytes = raw.long("sourceSizeBytes"),
        targetAlreadyExists = raw.boolean("targetAlreadyExists"),
    )

    private fun parseOperation(raw: Map<*, *>): FileOperation = when (raw.string("type")) {
        "createDirectory" -> FileOperation.CreateDirectory(raw.string("relativePath") ?: error("Missing directory path."))
        "moveFile" -> FileOperation.MoveFile(
            sourcePath = raw.string("sourcePath") ?: error("Missing source path."),
            targetPath = raw.string("targetPath") ?: error("Missing target path."),
        )
        "writeTextFile" -> FileOperation.WriteTextFile(
            relativePath = raw.string("relativePath") ?: error("Missing text path."),
            contents = raw.string("contents").orEmpty(),
        )
        "zipFile" -> FileOperation.ZipFile(
            sourcePath = raw.string("sourcePath") ?: error("Missing source path."),
            targetPath = raw.string("targetPath") ?: error("Missing target path."),
            archiveEntryName = raw.string("archiveEntryName") ?: error("Missing archive entry name."),
        )
        "convertToChd" -> FileOperation.ConvertToChd(
            sourcePath = raw.string("sourcePath") ?: error("Missing source path."),
            targetPath = raw.string("targetPath") ?: error("Missing target path."),
            system = chdSystem(raw.string("system")) ?: error("Missing CHD system."),
            discType = chdDiscType(raw.string("discType")) ?: error("Missing CHD disc type."),
            deleteOriginalFiles = raw.boolean("deleteOriginalFiles"),
        )
        "convertWithTool" -> FileOperation.ConvertWithTool(
            sourcePath = raw.string("sourcePath") ?: error("Missing source path."),
            targetPath = raw.string("targetPath") ?: error("Missing target path."),
            tool = converterTool(raw.string("tool")) ?: error("Missing converter tool."),
        )
        else -> error("Unknown operation type: ${raw.string("type")}")
    }

    private fun romEntryToMap(entry: RomEntry): Map<String, Any?> = mapOf(
        "relativePath" to entry.relativePath,
        "fileName" to entry.fileName,
        "sourcePath" to entry.sourcePath,
        "sizeBytes" to entry.sizeBytes,
    )

    private fun esDeSystemToMap(system: EsDeSystem): Map<String, Any?> = mapOf(
        "name" to system.name,
        "fullName" to system.fullName,
        "defaultPath" to system.defaultPath,
        "romFolder" to system.romFolder,
    )

    private fun esDeSystemFromMap(raw: Map<*, *>): EsDeSystem = EsDeSystem(
        name = raw.string("name").orEmpty(),
        fullName = raw.string("fullName").orEmpty(),
        defaultPath = raw.string("defaultPath").orEmpty(),
        romFolder = raw.string("romFolder").orEmpty(),
    )

    private fun operationEvent(
        completed: Int,
        total: Int,
        current: String? = null,
        currentProgress: Double? = null,
        spaceSavedBytes: Long = 0,
        stopped: Boolean = false,
        finished: Boolean = false,
        error: String? = null,
    ): Map<String, Any?> = mapOf(
        "version" to 1,
        "completed" to completed,
        "total" to total,
        "current" to current,
        "currentProgress" to currentProgress,
        "spaceSavedBytes" to spaceSavedBytes,
        "stopped" to stopped,
        "finished" to finished,
        "error" to error,
    )

    private fun operationLabel(operation: FileOperation): String = when (operation) {
        is FileOperation.CreateDirectory -> operation.relativePath
        is FileOperation.MoveFile -> operation.sourcePath
        is FileOperation.WriteTextFile -> operation.relativePath
        is FileOperation.ZipFile -> operation.sourcePath
        is FileOperation.ConvertToChd -> operation.sourcePath
        is FileOperation.ConvertWithTool -> operation.sourcePath
    }

    private fun isConversion(operation: FileOperation?): Boolean =
        operation is FileOperation.ConvertToChd || operation is FileOperation.ConvertWithTool

    private fun converterTool(value: String?): ConverterTool? = when (value) {
        "chd" -> ConverterTool.Chd
        "dolphinRvz" -> ConverterTool.DolphinRvz
        "azaharZcci" -> ConverterTool.AzaharZcci
        "nszNsp" -> ConverterTool.NszNsp
        else -> null
    }

    private fun chdSystem(value: String?): ChdSystem? = when (value) {
        "dreamcast" -> ChdSystem.Dreamcast
        "playStation1" -> ChdSystem.PlayStation1
        "playStation2" -> ChdSystem.PlayStation2
        "segaCd" -> ChdSystem.SegaCd
        "segaSaturn" -> ChdSystem.SegaSaturn
        "playStationPortable" -> ChdSystem.PlayStationPortable
        else -> null
    }

    private fun chdDiscType(value: String?): ChdDiscType? = when (value) {
        "cd" -> ChdDiscType.Cd
        "dvd" -> ChdDiscType.Dvd
        else -> null
    }

    private fun vitaFormat(value: String?): VitaShortcutFormat? = when (value) {
        "psvita" -> VitaShortcutFormat.Psvita
        "dpt" -> VitaShortcutFormat.Dpt
        else -> null
    }

    private fun toolMode(value: String?): ToolMode? = when (value) {
        "multiDiscOrganizer" -> ToolMode.MultiDiscOrganizer
        "romZipper" -> ToolMode.RomZipper
        "chdConverter" -> ToolMode.ChdConverter
        "vitaAppIds" -> ToolMode.VitaAppIds
        "esDeSystems" -> ToolMode.EsDeSystems
        else -> null
    }

    private fun frontendPreset(value: String?): FrontendPreset? = when (value) {
        "esDe" -> FrontendPreset.EsDe
        "other" -> FrontendPreset.Other
        else -> null
    }

    private fun MethodCall.stringArgument(key: String): String? = argument<Any?>(key)?.toString()

    private fun MethodCall.booleanArgument(key: String): Boolean = argument<Any?>(key) as? Boolean ?: false

    @Suppress("UNCHECKED_CAST")
    private fun MethodCall.mapArgument(key: String): Map<*, *>? = argument<Any?>(key) as? Map<*, *>

    @Suppress("UNCHECKED_CAST")
    private fun MethodCall.listArgument(key: String): List<Any?>? = argument<Any?>(key) as? List<Any?>

    private fun Map<*, *>.string(key: String): String? = this[key]?.toString()

    private fun Map<*, *>.long(key: String): Long = (this[key] as? Number)?.toLong() ?: this[key]?.toString()?.toLongOrNull() ?: 0L

    private fun Map<*, *>.boolean(key: String): Boolean = this[key] as? Boolean ?: false

    @Suppress("UNCHECKED_CAST")
    private fun Map<*, *>.list(key: String): List<Any?> = this[key] as? List<Any?> ?: emptyList()

    @Suppress("UNCHECKED_CAST")
    private fun Any?.asMap(): Map<*, *> = this as? Map<*, *> ?: error("Expected a map.")

    companion object {
        private const val DIRECTORY_REQUEST = 4101
        private const val FILE_REQUEST = 4102
        private const val PLATFORM_CHANNEL = "pup.app.mimir/platform"
        private const val EVENTS_CHANNEL = "pup.app.mimir/platform/events"
        private const val LEGACY_PREFERENCES = "mimir_prefs"
        private const val WIRE_VERSION = 1
    }
}
