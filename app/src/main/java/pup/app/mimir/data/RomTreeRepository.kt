package pup.app.mimir.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import pup.app.mimir.domain.RomEntry
import pup.app.mimir.domain.ParamSfoParser
import pup.app.mimir.domain.VitaApp
import pup.app.mimir.domain.VitaShortcutFormat

class RomTreeRepository(private val context: Context) {
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
            )
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
    ) {
        node.listFiles()
            .sortedBy { it.name.orEmpty().lowercase() }
            .forEach { child ->
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
                    )
                } else if (child.isFile) {
                    collector += RomEntry(
                        relativePath = (outputSegments + childName).joinToString("/"),
                        fileName = childName,
                        sourcePath = (sourceSegments + childName).joinToString("/"),
                    )
                    onFileScanned?.invoke(collector.size)
                }
            }
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
}
