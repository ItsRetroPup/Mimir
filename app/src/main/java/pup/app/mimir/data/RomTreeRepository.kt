package pup.app.mimir.data

import android.content.Context
import android.net.Uri
import android.os.StatFs
import android.os.storage.StorageManager
import androidx.documentfile.provider.DocumentFile
import pup.app.mimir.domain.RomEntry
import pup.app.mimir.domain.ParamSfoParser
import pup.app.mimir.domain.VitaApp
import pup.app.mimir.domain.VitaShortcutFormat

class RomTreeRepository(private val context: Context) {
    data class StorageInfo(val totalBytes: Long, val freeBytes: Long)

    fun storageInfo(rootUri: Uri): StorageInfo? = runCatching {
        val storageManager = context.getSystemService(StorageManager::class.java)
        val directory = storageManager?.getStorageVolume(rootUri)?.directory ?: return null
        val stat = StatFs(directory.path)
        StorageInfo(totalBytes = stat.totalBytes, freeBytes = stat.availableBytes)
    }.getOrNull()

    fun loadVitaShortcutCatalog(): List<VitaApp> =
        context.assets.open("vita_shortcuts.tsv").bufferedReader().useLines { lines ->
            lines.mapNotNull { line ->
                val parts = line.split('\t', limit = 2)
                if (parts.size != 2) return@mapNotNull null
                val title = parts[0].trim()
                val appId = parts[1].trim()
                if (title.isEmpty() || appId.isEmpty()) return@mapNotNull null
                VitaApp(
                    titleId = appId,
                    title = title,
                    sourcePath = "shortcut-db",
                )
            }.toList()
        }

    fun scanTree(
        rootUri: Uri,
        scanHiddenFolders: Boolean = false,
        onFileScanned: ((Int) -> Unit)? = null,
        shouldStop: (() -> Boolean)? = null,
    ): List<RomEntry> {
        val root = DocumentFile.fromTreeUri(context, rootUri) ?: return emptyList()
        return buildList {
            walk(
                node = root,
                sourceSegments = emptyList(),
                outputSegments = emptyList(),
                collector = this,
                scanHiddenFolders = scanHiddenFolders,
                onFileScanned = onFileScanned,
                shouldStop = shouldStop,
            )
        }
    }

    /**
     * Scans compatible files in the selected root and system-specific child folders only.
     * This keeps, for example, a Dreamcast scan from picking up PS1 cue sheets elsewhere in a ROM root.
     */
    fun scanChdTree(
        rootUri: Uri,
        folderAliases: Set<String>,
        supportedExtensions: Set<String>,
        onFileScanned: ((Int) -> Unit)? = null,
        shouldStop: (() -> Boolean)? = null,
    ): List<RomEntry> {
        val root = DocumentFile.fromTreeUri(context, rootUri) ?: return emptyList()
        val normalizedAliases = folderAliases.map(String::lowercase).toSet()
        // Existing CHDs are included so the planner can mark their matching source image as an
        // overwrite choice. The planner itself only creates operations for supported source types.
        val scanExtensions = supportedExtensions + "chd"
        return buildList {
            root.listFiles()
                .sortedBy { it.name.orEmpty().lowercase() }
                .forEach { child ->
                    if (shouldStop?.invoke() == true) throw OperationStoppedException("Scan stopped by user.")
                    val childName = child.name ?: return@forEach
                    when {
                        child.isFile && child.extension() in scanExtensions -> {
                            addChdEntry(
                                child = child,
                                sourceSegments = emptyList(),
                                outputSegments = emptyList(),
                                collector = this,
                                onFileScanned = onFileScanned,
                            )
                        }

                        child.isDirectory && childName.lowercase() in normalizedAliases -> {
                            walkChdDirectory(
                                node = child,
                                sourceSegments = listOf(childName),
                                outputSegments = listOf(childName),
                                supportedExtensions = scanExtensions,
                                collector = this,
                                onFileScanned = onFileScanned,
                                shouldStop = shouldStop,
                            )
                        }
                    }
                }
        }
    }

    fun loadExistingVitaShortcuts(
        rootUri: Uri,
        format: VitaShortcutFormat,
    ): Map<String, String> {
        val root = DocumentFile.fromTreeUri(context, rootUri) ?: return emptyMap()
        val entries = buildList {
            walk(
                node = root,
                sourceSegments = emptyList(),
                outputSegments = emptyList(),
                collector = this,
            )
        }
        return entries
            .filter { it.fileName.endsWith(".${format.extension}", ignoreCase = true) }
            .mapNotNull { entry ->
                val file = findFile(root, entry.relativePath) ?: return@mapNotNull null
                val appId = runCatching {
                    context.contentResolver.openInputStream(file.uri).use { input ->
                        requireNotNull(input) { "Unable to read ${entry.relativePath}" }
                        input.readBytes().toString(Charsets.UTF_8).trim()
                    }
                }.getOrNull()
                val titleId = appId?.let { contents ->
                    when (format) {
                        VitaShortcutFormat.Psvita -> contents
                        VitaShortcutFormat.Dpt -> contents
                            .lineSequence()
                            .map(String::trim)
                            .dropWhile(String::isBlank)
                            .takeIf { it.firstOrNull() == "[vita_game_id]" }
                            ?.drop(1)
                            ?.firstOrNull { it.isNotBlank() }
                    }
                }
                if (titleId.isNullOrBlank()) null else titleId to entry.relativePath
            }
            .toMap()
    }

    fun scanVitaApps(rootUri: Uri): List<VitaApp> {
        val root = DocumentFile.fromTreeUri(context, rootUri) ?: return emptyList()
        val appFolder = findVitaAppFolder(root) ?: root
        return appFolder.listFiles()
            .filter { it.isDirectory }
            .mapNotNull { appDirectory ->
                val titleId = appDirectory.name ?: return@mapNotNull null
                val title = readVitaTitle(appDirectory) ?: titleId
                VitaApp(
                    titleId = titleId,
                    title = title,
                    sourcePath = "app/$titleId",
                )
            }
            .sortedWith(compareBy(VitaApp::title, VitaApp::titleId))
    }

    fun hasVita3kInstall(rootUri: Uri): Boolean {
        val root = DocumentFile.fromTreeUri(context, rootUri) ?: return false
        return findVitaAppFolder(root) != null
    }

    private fun findVitaAppFolder(root: DocumentFile): DocumentFile? {
        if (root.name?.lowercase() == "app" && root.parentFile?.name?.lowercase() == "ux0") {
            return root
        }

        val pathSegments = listOf("Android", "data", "org.vita3k.emulator", "files", "vita", "ux0", "app")
        return findDirectoryAtPath(root, pathSegments)
    }

    private fun findDirectoryAtPath(root: DocumentFile, segments: List<String>): DocumentFile? {
        var current: DocumentFile? = root
        for (segment in segments) {
            current = current?.findFile(segment) ?: return null
            if (!current.isDirectory) return null
        }
        return current
    }

    private fun readVitaTitle(appDirectory: DocumentFile): String? {
        val sceSys = appDirectory.findFile("sce_sys") ?: return null
        val paramSfo = sceSys.findFile("param.sfo") ?: return null
        return runCatching {
            context.contentResolver.openInputStream(paramSfo.uri).use { input ->
                requireNotNull(input) { "Unable to read ${paramSfo.uri}" }
                val metadata = ParamSfoParser.parse(input.readBytes())
                metadata["TITLE"] as? String
            }
        }.getOrNull()
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    }

    private fun walk(
        node: DocumentFile,
        sourceSegments: List<String>,
        outputSegments: List<String>,
        collector: MutableList<RomEntry>,
        scanHiddenFolders: Boolean = false,
        onFileScanned: ((Int) -> Unit)? = null,
        shouldStop: (() -> Boolean)? = null,
    ) {
        if (shouldStop?.invoke() == true) throw OperationStoppedException("Scan stopped by user.")
        node.listFiles()
            .sortedBy { it.name.orEmpty().lowercase() }
            .forEach { child ->
                if (shouldStop?.invoke() == true) throw OperationStoppedException("Scan stopped by user.")
                val childName = child.name ?: return@forEach
                if (child.isDirectory) {
                    if (childName.endsWith(".m3u", ignoreCase = true)) return@forEach
                    if (childName.startsWith(".") && !scanHiddenFolders) return@forEach
                    walk(
                        node = child,
                        sourceSegments = sourceSegments + childName,
                        outputSegments = if (childName.startsWith(".")) outputSegments else outputSegments + childName,
                        collector = collector,
                        scanHiddenFolders = scanHiddenFolders,
                        onFileScanned = onFileScanned,
                        shouldStop = shouldStop,
                    )
                } else if (child.isFile) {
                    collector += RomEntry(
                        relativePath = (outputSegments + childName).joinToString("/"),
                        fileName = childName,
                        sourcePath = (sourceSegments + childName).joinToString("/"),
                        sizeBytes = child.length(),
                    )
                    onFileScanned?.invoke(collector.size)
                }
            }
    }

    private fun walkChdDirectory(
        node: DocumentFile,
        sourceSegments: List<String>,
        outputSegments: List<String>,
        supportedExtensions: Set<String>,
        collector: MutableList<RomEntry>,
        onFileScanned: ((Int) -> Unit)? = null,
        shouldStop: (() -> Boolean)? = null,
    ) {
        if (shouldStop?.invoke() == true) throw OperationStoppedException("Scan stopped by user.")
        node.listFiles()
            .sortedBy { it.name.orEmpty().lowercase() }
            .forEach { child ->
                if (shouldStop?.invoke() == true) throw OperationStoppedException("Scan stopped by user.")
                val childName = child.name ?: return@forEach
                when {
                    child.isDirectory -> walkChdDirectory(
                        node = child,
                        sourceSegments = sourceSegments + childName,
                        outputSegments = outputSegments + childName,
                        supportedExtensions = supportedExtensions,
                        collector = collector,
                        onFileScanned = onFileScanned,
                        shouldStop = shouldStop,
                    )

                    child.isFile && child.extension() in supportedExtensions -> addChdEntry(
                        child = child,
                        sourceSegments = sourceSegments,
                        outputSegments = outputSegments,
                        collector = collector,
                        onFileScanned = onFileScanned,
                    )
                }
            }
    }

    private fun addChdEntry(
        child: DocumentFile,
        sourceSegments: List<String>,
        outputSegments: List<String>,
        collector: MutableList<RomEntry>,
        onFileScanned: ((Int) -> Unit)?,
    ) {
        val childName = child.name ?: return
        collector += RomEntry(
            relativePath = (outputSegments + childName).joinToString("/"),
            fileName = childName,
            sourcePath = (sourceSegments + childName).joinToString("/"),
            sizeBytes = chdInputSize(child),
        )
        onFileScanned?.invoke(collector.size)
    }

    private fun DocumentFile.extension(): String =
        name.orEmpty().substringAfterLast('.', "").lowercase()

    private fun chdInputSize(source: DocumentFile): Long {
        if (source.extension() != "cue") return source.length()
        val cueContents = context.contentResolver.openInputStream(source.uri)
            ?.bufferedReader()
            ?.use { it.readText() }
            ?: return 0L
        val parent = source.parentFile ?: return 0L
        return CUE_FILE_PATTERN.findAll(cueContents)
            .map { it.groupValues[1] }
            .distinct()
            .sumOf { fileName -> parent.findFile(fileName)?.length() ?: 0L }
    }

    private fun findFile(root: DocumentFile, relativePath: String): DocumentFile? {
        val parts = relativePath.split('/').filter { it.isNotBlank() }
        var current = root
        parts.forEachIndexed { index, part ->
            val child = current.findFile(part) ?: return null
            if (index == parts.lastIndex) {
                return child
            }
            current = child
        }
        return null
    }

    private companion object {
        val CUE_FILE_PATTERN = Regex("""(?im)^\s*FILE\s+\"([^\"]+)\"""")
    }
}
