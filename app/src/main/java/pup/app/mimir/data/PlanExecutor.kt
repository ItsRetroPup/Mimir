package pup.app.mimir.data

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.documentfile.provider.DocumentFile
import pup.app.mimir.domain.FileOperation
import pup.app.mimir.domain.OperationPlan
import pup.app.mimir.domain.RelativePaths
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class PlanExecutor(private val context: Context) {
    fun apply(
        rootUri: Uri,
        plan: OperationPlan,
        onProgress: ((completedOperations: Int, totalOperations: Int) -> Unit)? = null,
    ): Result<Unit> = runCatching {
        require(plan.operations.isNotEmpty()) { "No valid changes to apply." }
        val root = DocumentFile.fromTreeUri(context, rootUri)
            ?: error("Unable to access selected ROM folder.")

        plan.operations.forEachIndexed { index, operation ->
            when (operation) {
                is FileOperation.CreateDirectory -> {
                    if (findDirectory(root, operation.relativePath) == null) {
                        ensureDirectory(root, operation.relativePath)
                    }
                }

                is FileOperation.MoveFile -> {
                    val sourceFile = findFile(root, operation.sourcePath)
                        ?: error("Missing source file: ${operation.sourcePath}")
                    val target = createFile(root, operation.targetPath)
                    context.contentResolver.openInputStream(sourceFile.uri).use { input ->
                        requireNotNull(input) { "Unable to read ${operation.sourcePath}" }
                        context.contentResolver.openOutputStream(target.uri, "w").use { output ->
                            requireNotNull(output) { "Unable to write ${operation.targetPath}" }
                            input.copyTo(output)
                        }
                    }
                    if (!sourceFile.delete()) {
                        error("Failed to remove original file: ${operation.sourcePath}")
                    }
                }

                is FileOperation.WriteTextFile -> {
                    val target = createFile(root, operation.relativePath)
                    context.contentResolver.openOutputStream(target.uri, "wt").use { output ->
                        requireNotNull(output) { "Unable to write ${operation.relativePath}" }
                        output.write(operation.contents.toByteArray())
                    }
                }

                is FileOperation.ZipFile -> {
                    val sourceFile = findFile(root, operation.sourcePath)
                        ?: error("Missing source file: ${operation.sourcePath}")
                    val target = createFile(root, operation.targetPath)
                    context.contentResolver.openInputStream(sourceFile.uri).use { input ->
                        requireNotNull(input) { "Unable to read ${operation.sourcePath}" }
                        context.contentResolver.openOutputStream(target.uri, "w").use { output ->
                            requireNotNull(output) { "Unable to write ${operation.targetPath}" }
                            ZipOutputStream(output).use { zip ->
                                zip.putNextEntry(ZipEntry(operation.archiveEntryName))
                                input.copyTo(zip)
                                zip.closeEntry()
                            }
                        }
                    }
                    if (!sourceFile.delete()) {
                        error("Failed to remove original file: ${operation.sourcePath}")
                    }
                }
            }
            onProgress?.invoke(index + 1, plan.operations.size)
        }
    }

    fun deleteOutputFile(rootUri: Uri, relativePath: String): Result<Unit> = runCatching {
        val root = DocumentFile.fromTreeUri(context, rootUri)
            ?: error("Unable to access selected output folder.")
        val file = findFile(root, relativePath) ?: error("Missing shortcut: $relativePath")
        require(file.delete()) { "Failed to delete shortcut: $relativePath" }
    }

    private fun ensureDirectory(root: DocumentFile, relativePath: String): DocumentFile {
        val parts = relativePath.split('/').filter { it.isNotBlank() }
        var current = root
        parts.forEach { part ->
            current = current.findFile(part) ?: current.createDirectory(part)
            ?: error("Unable to create directory: $relativePath")
        }
        return current
    }

    private fun findDirectory(root: DocumentFile, relativePath: String): DocumentFile? {
        val parts = relativePath.split('/').filter { it.isNotBlank() }
        var current = root
        parts.forEach { part ->
            current = current.findFile(part) ?: return null
        }
        return current.takeIf { it.isDirectory }
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

    private fun createFile(root: DocumentFile, relativePath: String): DocumentFile {
        val parentPath = RelativePaths.parentOf(relativePath)
        val fileName = RelativePaths.nameOf(relativePath)
        val parent = ensureDirectory(root, parentPath)
        parent.findFile(fileName)?.delete()
        return parent.createFile(mimeTypeFor(fileName), fileName)
            ?: error("Unable to create file: $relativePath")
    }

    private fun mimeTypeFor(fileName: String): String {
        val extension = fileName.substringAfterLast('.', "")
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
            ?: "application/octet-stream"
    }
}
