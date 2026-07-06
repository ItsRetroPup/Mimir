package pup.app.mimir.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import pup.app.mimir.domain.RomEntry
import pup.app.mimir.domain.ParamSfoParser
import pup.app.mimir.domain.VitaApp

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

    fun scanTree(rootUri: Uri): List<RomEntry> {
        val root = DocumentFile.fromTreeUri(context, rootUri) ?: return emptyList()
        return buildList { walk(root, emptyList(), this) }
    }

    fun loadExistingVitaShortcuts(rootUri: Uri): Map<String, String> {
        val root = DocumentFile.fromTreeUri(context, rootUri) ?: return emptyMap()
        val entries = buildList { walk(root, emptyList(), this) }
        return entries
            .filter { it.fileName.endsWith(".psvita", ignoreCase = true) }
            .mapNotNull { entry ->
                val file = findFile(root, entry.relativePath) ?: return@mapNotNull null
                val appId = runCatching {
                    context.contentResolver.openInputStream(file.uri).use { input ->
                        requireNotNull(input) { "Unable to read ${entry.relativePath}" }
                        input.readBytes().toString(Charsets.UTF_8).trim()
                    }
                }.getOrNull()
                if (appId.isNullOrBlank()) null else appId to entry.relativePath
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
        segments: List<String>,
        collector: MutableList<RomEntry>,
    ) {
        node.listFiles()
            .sortedBy { it.name.orEmpty().lowercase() }
            .forEach { child ->
                val childName = child.name ?: return@forEach
                if (child.isDirectory) {
                    walk(child, segments + childName, collector)
                } else if (child.isFile) {
                    collector += RomEntry(
                        relativePath = (segments + childName).joinToString("/"),
                        fileName = childName,
                    )
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
