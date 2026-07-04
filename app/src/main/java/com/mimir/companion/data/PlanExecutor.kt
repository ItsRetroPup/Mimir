package com.mimir.companion.data

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.documentfile.provider.DocumentFile
import com.mimir.companion.domain.FileOperation
import com.mimir.companion.domain.OperationPlan
import com.mimir.companion.domain.RelativePaths
import java.io.File
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class PlanExecutor(private val context: Context) {
    private val backupManager = BackupManager(context)

    fun apply(rootUri: Uri, plan: OperationPlan): Result<String> = runCatching {
        require(plan.operations.isNotEmpty()) { "No valid changes to apply." }
        val root = DocumentFile.fromTreeUri(context, rootUri)
            ?: error("Unable to access selected ROM folder.")
        val sessionId = UUID.randomUUID().toString()
        val sourcePaths = plan.operations.flatMap { operation ->
            when (operation) {
                is FileOperation.MoveFile -> listOf(operation.sourcePath)
                is FileOperation.ZipFile -> listOf(operation.sourcePath)
                else -> emptyList()
            }
        }.distinct()
        val sourceBackups = linkedMapOf<String, String>()

        sourcePaths.forEach { sourcePath ->
            val sourceFile = findFile(root, sourcePath) ?: error("Missing source file: $sourcePath")
            val backupFile = backupManager.backupFile(sessionId, sourcePath)
            backupFile.parentFile?.mkdirs()
            context.contentResolver.openInputStream(sourceFile.uri).use { input ->
                requireNotNull(input) { "Unable to read $sourcePath" }
                backupFile.outputStream().use { output -> input.copyTo(output) }
            }
            sourceBackups[sourcePath] = backupFile.name
        }

        val createdDirectories = mutableListOf<String>()
        val createdFiles = mutableListOf<String>()

        plan.operations.forEach { operation ->
            when (operation) {
                is FileOperation.CreateDirectory -> {
                    if (findDirectory(root, operation.relativePath) == null) {
                        ensureDirectory(root, operation.relativePath)
                        createdDirectories += operation.relativePath
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
                    createdFiles += operation.targetPath
                }

                is FileOperation.WriteTextFile -> {
                    val target = createFile(root, operation.relativePath)
                    context.contentResolver.openOutputStream(target.uri, "wt").use { output ->
                        requireNotNull(output) { "Unable to write ${operation.relativePath}" }
                        output.write(operation.contents.toByteArray())
                    }
                    createdFiles += operation.relativePath
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
                    createdFiles += operation.targetPath
                }
            }
        }

        backupManager.saveActiveManifest(
            BackupManifest(
                sessionId = sessionId,
                sourceBackups = sourceBackups,
                createdFiles = createdFiles,
                createdDirectories = createdDirectories,
            )
        )
        sessionId
    }

    fun undo(rootUri: Uri): Result<Unit> = runCatching {
        val manifest = backupManager.loadActiveManifest() ?: error("No backup session available.")
        val root = DocumentFile.fromTreeUri(context, rootUri)
            ?: error("Unable to access selected ROM folder.")

        manifest.createdFiles
            .distinct()
            .sortedByDescending { it.length }
            .forEach { deleteFile(root, it) }

        manifest.sourceBackups.forEach { (originalPath, backupName) ->
            val backupFile = File(backupManager.sessionDir(manifest.sessionId), backupName)
            if (!backupFile.exists()) {
                error("Missing backup for $originalPath")
            }
            val restored = createFile(root, originalPath)
            backupFile.inputStream().use { input ->
                context.contentResolver.openOutputStream(restored.uri, "w").use { output ->
                    requireNotNull(output) { "Unable to restore $originalPath" }
                    input.copyTo(output)
                }
            }
        }

        manifest.createdDirectories
            .distinct()
            .sortedByDescending { it.length }
            .forEach { deleteDirectoryIfEmpty(root, it) }

        backupManager.clearActiveManifest()
        backupManager.deleteSession(manifest.sessionId)
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

    private fun deleteFile(root: DocumentFile, relativePath: String) {
        findFile(root, relativePath)?.delete()
    }

    private fun deleteDirectoryIfEmpty(root: DocumentFile, relativePath: String) {
        val directory = findDirectory(root, relativePath) ?: return
        if (directory.listFiles().isEmpty()) {
            directory.delete()
        }
    }

    private fun mimeTypeFor(fileName: String): String {
        val extension = fileName.substringAfterLast('.', "")
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
            ?: "application/octet-stream"
    }
}
