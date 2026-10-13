package pup.app.mimir

import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : FlutterActivity() {
    private val workers = Executors.newCachedThreadPool()
    private var folderResult: MethodChannel.Result? = null
    @Volatile private var events: EventChannel.EventSink? = null
    @Volatile private var activeProcess: Process? = null
    private val cancelled = AtomicBoolean(false)
    private val converting = AtomicBoolean(false)

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        EventChannel(flutterEngine.dartExecutor.binaryMessenger, "pup.app.mimir/conversion")
            .setStreamHandler(object : EventChannel.StreamHandler {
                override fun onListen(arguments: Any?, sink: EventChannel.EventSink) { events = sink }
                override fun onCancel(arguments: Any?) { events = null }
            })
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, "pup.app.mimir/native")
            .setMethodCallHandler { call, result ->
                if (call.method == "pickFolder") {
                    if (folderResult != null) { result.error("busy", "A folder picker is already open", null); return@setMethodCallHandler }
                    folderResult = result
                    startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                    }, 81)
                } else if (call.method == "cancelConversion") {
                    cancelled.set(true)
                    activeProcess?.destroyForcibly()
                    result.success(null)
                } else {
                    workers.execute {
                        try {
                            val value = handle(call)
                            runOnUiThread { result.success(value) }
                        } catch (error: Exception) {
                            runOnUiThread { result.error("operation_failed", error.message ?: error.toString(), null) }
                        }
                    }
                }
            }
    }

    @Deprecated("Android document-tree result bridge")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != 81) return
        val pending = folderResult ?: return
        folderResult = null
        val uri = data?.data
        if (resultCode != RESULT_OK || uri == null) { pending.success(null); return }
        try {
            contentResolver.takePersistableUriPermission(uri, (data?.flags ?: 0) and
                (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION))
            pending.success(mapOf("reference" to uri.toString(), "label" to (DocumentFile.fromTreeUri(this, uri)?.name ?: "Selected folder")))
        } catch (error: Exception) { pending.error("permission", error.message, null) }
    }

    private fun handle(call: MethodCall): Any? {
        when (call.method) {
            "legacySettings" -> return legacySettings()
            "migrateKeys" -> {
                val old = File(filesDir, "nsz/prod.keys")
                val destination = privateFile(call.argument<String>("destination")!!)
                if (old.isFile && !destination.exists()) { destination.parentFile!!.mkdirs(); old.copyTo(destination) }
                return null
            }
            "converterAvailable" -> {
                val tool = call.argument<String>("tool")!!
                return tool == "nsp" || executable(tool).let { it.isFile && it.canExecute() }
            }
            "convert" -> { convert(call); return null }
        }
        val rootUri = Uri.parse(call.argument<String>("root") ?: error("Missing folder reference"))
        val root = DocumentFile.fromTreeUri(this, rootUri) ?: error("Folder access is unavailable. Choose the folder again.")
        require(root.exists() && root.canRead()) { "Folder access is unavailable. Choose the folder again." }
        val relative = call.argument<String>("path").orEmpty()
        return when (call.method) {
            "list" -> find(root, relative)?.listFiles()?.map { file -> mapOf("name" to file.name.orEmpty(), "size" to file.length(), "directory" to file.isDirectory) }
                ?: error("Unable to list selected folder")
            "exists" -> find(root, relative)?.exists() == true
            "mkdir" -> { directory(root, relative); null }
            "rmdir" -> {
                val folder = find(root, relative)
                if (folder != null && folder.isDirectory && folder.listFiles().isEmpty()) require(folder.delete()) { "Unable to remove empty folder" }
                null
            }
            "read" -> contentResolver.openInputStream(required(root, relative).uri)?.use { it.readBytes() } ?: error("Unable to read $relative")
            "export" -> {
                val destination = privateFile(call.argument<String>("local")!!)
                contentResolver.openInputStream(required(root, relative).uri)?.use { input -> destination.outputStream().use(input::copyTo) }
                    ?: error("Unable to read $relative")
                null
            }
            "commit" -> { commit(root, relative, privateFile(call.argument<String>("local")!!), call.argument<Boolean>("overwrite") == true); null }
            "move" -> { move(root, call.argument<String>("source")!!, relative); null }
            "delete" -> { require(required(root, relative).delete()) { "Unable to delete $relative" }; null }
            "absolutePath" -> absolutePath(rootUri)
            else -> error("Unknown storage operation ${call.method}")
        }
    }

    private fun parts(path: String): List<String> {
        if (path.isEmpty()) return emptyList()
        val values = path.split('/')
        require(values.none { it.isEmpty() || it == "." || it == ".." || it.contains('\\') || it.contains(':') }) { "Invalid relative path" }
        return values
    }
    private fun find(root: DocumentFile, path: String): DocumentFile? {
        var current = root
        for (name in parts(path)) current = current.findFile(name) ?: return null
        return current
    }
    private fun required(root: DocumentFile, path: String) = find(root, path) ?: error("Missing file: $path")
    private fun directory(root: DocumentFile, path: String): DocumentFile {
        var current = root
        for (name in parts(path)) {
            current = current.findFile(name) ?: current.createDirectory(name) ?: error("Unable to create $name")
            require(current.isDirectory) { "$name is not a folder" }
        }
        return current
    }
    private fun parent(root: DocumentFile, path: String): DocumentFile {
        val names = parts(path)
        require(names.isNotEmpty()) { "Missing filename" }
        return required(root, names.dropLast(1).joinToString("/"))
    }
    private fun copy(local: File, target: DocumentFile) {
        contentResolver.openOutputStream(target.uri, "wt")?.use { output -> local.inputStream().use { it.copyTo(output) } }
            ?: error("Unable to write ${target.name}")
        require(target.length() == local.length()) { "Incomplete output copy" }
    }
    private fun commit(root: DocumentFile, path: String, local: File, overwrite: Boolean) {
        val parent = parent(root, path)
        val name = parts(path).last()
        val old = parent.findFile(name)
        require(old == null || (overwrite && old.isFile)) { "Target already exists: $path" }
        val nonce = UUID.randomUUID().toString()
        val temporary = parent.createFile("application/octet-stream", ".mimir-$nonce.tmp") ?: error("Unable to stage output")
        var backedUp = false
        var committed = false
        try {
            copy(local, temporary)
            if (old != null) {
                require(old.renameTo(".mimir-$nonce.backup")) { "This provider cannot safely replace files" }
                backedUp = true
            } else require(parent.findFile(name) == null) { "Target appeared during operation" }
            require(temporary.renameTo(name)) { "This provider cannot commit staged files" }
            require(parent.findFile(name)?.length() == local.length()) { "Output verification failed" }
            committed = true
        } catch (error: Exception) {
            temporary.delete()
            if (backedUp) require(old!!.renameTo(name)) { "Output failed; recover .mimir-$nonce.backup manually" }
            throw error
        } finally { if (!committed) temporary.delete() }
        if (backedUp) require(old!!.delete()) { "Output created; old output backup could not be removed" }
    }
    private fun move(root: DocumentFile, sourcePath: String, targetPath: String) {
        val source = required(root, sourcePath)
        val parent = parent(root, targetPath)
        val name = parts(targetPath).last()
        require(parent.findFile(name) == null) { "Target already exists: $targetPath" }
        val local = File.createTempFile("mimir-move", ".tmp", cacheDir)
        try {
            contentResolver.openInputStream(source.uri)?.use { input -> local.outputStream().use(input::copyTo) } ?: error("Unable to read source")
            commit(root, targetPath, local, false)
            if (!source.delete()) {
                parent.findFile(name)?.delete()
                error("Source could not be removed; move stopped")
            }
        } finally { local.delete() }
    }
    private fun privateFile(path: String): File {
        val file = File(path).canonicalFile
        val allowed = listOf(cacheDir, filesDir).any { file.path.startsWith(it.canonicalPath + File.separator) }
        require(allowed) { "Expected an app-private file" }
        return file
    }
    private fun absolutePath(uri: Uri): String? {
        if (uri.authority != "com.android.externalstorage.documents") return null
        val id = android.provider.DocumentsContract.getTreeDocumentId(uri)
        val volume = id.substringBefore(':')
        val suffix = id.substringAfter(':', "").trim('/')
        val base = if (volume.equals("primary", true)) "/storage/emulated/0" else "/storage/$volume"
        return if (suffix.isEmpty()) base else "$base/$suffix"
    }
    private fun legacySettings(): Map<String, Any> {
        val prefs = getSharedPreferences("mimir_prefs", MODE_PRIVATE)
        val values = mutableMapOf<String, Any>()
        for ((old, key) in listOf("rom_tree" to "romRoot", "vita_output" to "vitaOutput", "esde_root" to "esdeRoot")) {
            val uri = prefs.getString("${old}_uri", null) ?: continue
            values[key] = JSONObject(mapOf("reference" to uri, "label" to (prefs.getString("${old}_label", null) ?: "Saved folder"))).toString()
        }
        values["dark"] = prefs.getBoolean("dark_mode", true)
        values["hidden"] = prefs.getBoolean("scan_hidden_folders", false)
        values["deleteSource"] = prefs.getBoolean("delete_original_chd_files", false)
        values["shortcutFormat"] = if (prefs.getString("vita_shortcut_format", "Psvita") == "Dpt") "dpt" else "psvita"
        return values
    }
    private fun executable(tool: String) = File(applicationInfo.nativeLibraryDir, when (tool) {
        "chd" -> "libchdman.so"; "rvz" -> "libdolphintool.so"; "zcci" -> "libazahar.so"; else -> error("Unknown converter")
    })
    private fun progress(value: Double) { runOnUiThread { events?.success(value.coerceIn(0.0, 1.0)) } }
    private fun runProcess(command: List<String>, working: File) {
        val process = ProcessBuilder(command).directory(working).redirectErrorStream(true).start()
        activeProcess = process
        val log = StringBuilder()
        try {
            process.inputStream.bufferedReader().use { reader ->
                val line = StringBuilder()
                while (true) {
                    val value = reader.read()
                    if (value < 0) break
                    if (value == 10 || value == 13) {
                        Regex("(\\d{1,3}(?:\\.\\d+)?)\\s*%").find(line.toString())?.groupValues?.get(1)?.toDoubleOrNull()?.let { progress(it / 100) }
                        if (log.length < 8192) log.append(line).append('\n')
                        line.clear()
                    } else if (line.length < 8192) line.append(value.toChar())
                }
            }
            require(!cancelled.get()) { "Conversion stopped by user" }
            require(process.waitFor() == 0) { "Converter failed: $log" }
        } finally { activeProcess = null; if (process.isAlive) process.destroyForcibly() }
    }
    private fun convert(call: MethodCall) {
        require(converting.compareAndSet(false, true)) { "A conversion is already running" }
        cancelled.set(false)
        try {
            val tool = call.argument<String>("tool")!!
            val source = privateFile(call.argument<String>("source")!!)
            val output = privateFile(call.argument<String>("output")!!)
            if (tool == "nsp") {
                val keys = privateFile(call.argument<String>("keys")!!)
                if (!Python.isStarted()) Python.start(AndroidPlatform(this))
                Python.getInstance().getModule("mimir_nsz").callAttr("decompress_nsz", source.path, output.path, keys.path,
                    NszCallback(cancelled) { progress(it) })
            } else {
                val executable = executable(tool)
                require(executable.isFile && executable.canExecute()) { "Converter is missing from this build" }
                val arguments = when (tool) {
                    "chd" -> listOf("create${call.argument<String>("discType")}", "-i", source.path, "-o", output.path)
                    "rvz" -> listOf("convert", "-i", source.path, "-o", output.path, "-f", "rvz", "-b", "131072", "-c", "zstd", "-l", "5")
                    "zcci" -> listOf("-c", source.path, "-o", output.parent!!)
                    else -> error("Unknown converter")
                }
                runProcess(listOf(executable.path) + arguments, source.parentFile!!)
                if (tool == "chd") runProcess(listOf(executable.path, "verify", "-i", output.path), source.parentFile!!)
            }
            require(!cancelled.get() && output.isFile && output.length() > 0) { "Conversion stopped or produced no output" }
            progress(1.0)
        } finally { converting.set(false) }
    }
}

class NszCallback(private val cancelled: AtomicBoolean, private val progress: (Double) -> Unit) {
    fun isCancellationRequested(): Boolean = cancelled.get()
    fun onProgress(value: Double) = progress(value)
}
