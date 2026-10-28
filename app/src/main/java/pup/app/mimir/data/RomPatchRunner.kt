package pup.app.mimir.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import pup.app.mimir.domain.PatchException
import pup.app.mimir.domain.PatchFailure
import pup.app.mimir.domain.PatchFormat
import pup.app.mimir.domain.RomPatcher

/**
 * Reads a picked ROM and patch, and writes the patched copy into the ROM's own folder. A file
 * picked with the system picker is readable but its folder is not writable, so the folder must
 * sit inside a tree the user has granted: the ROM folder, or one granted for this ROM.
 */
class RomPatchRunner(private val context: Context) {
    data class PickedFile(val uri: Uri, val name: String, val sizeBytes: Long)

    /** A writable folder: [treeUri] is the granted tree, [relativePath] leads from it to the folder. */
    data class OutputFolder(val treeUri: Uri, val relativePath: String, val label: String)

    fun describe(uri: Uri): PickedFile {
        val resolver = context.contentResolver
        var name: String? = null
        var size = -1L
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                name = cursor.getString(0)
                if (!cursor.isNull(1)) size = cursor.getLong(1)
            }
        }
        return PickedFile(uri, name ?: uri.lastPathSegment?.substringAfterLast('/') ?: "ROM", size)
    }

    /** The patch format from the file's signature, or null when it is not a supported patch. */
    fun detectFormat(uri: Uri): PatchFormat? {
        val header = ByteArray(RomPatcher.SIGNATURE_SIZE)
        val read = context.contentResolver.openInputStream(uri)?.use { input ->
            var total = 0
            while (total < header.size) {
                val count = input.read(header, total, header.size - total)
                if (count < 0) break
                total += count
            }
            total
        } ?: return null
        return RomPatcher.detect(header.copyOf(read))
    }

    /**
     * Finds a granted, writable tree that holds [rom]'s folder. Document IDs from the external
     * storage provider are paths ("primary:ROMs/snes/Game.sfc"), so containment is a prefix check.
     */
    fun outputFolderFor(rom: PickedFile): OutputFolder? {
        val documentId = runCatching { DocumentsContract.getDocumentId(rom.uri) }.getOrNull() ?: return null
        val parentId = if ('/' in documentId) documentId.substringBeforeLast('/') else "${documentId.substringBefore(':')}:"
        return context.contentResolver.persistedUriPermissions
            .filter { it.isWritePermission && it.uri.authority == rom.uri.authority && DocumentsContract.isTreeUri(it.uri) }
            .firstNotNullOfOrNull { permission ->
                val treeId = DocumentsContract.getTreeDocumentId(permission.uri)
                val relative = when {
                    parentId == treeId -> ""
                    treeId.endsWith(":") && parentId.startsWith(treeId) -> parentId.removePrefix(treeId)
                    parentId.startsWith("$treeId/") -> parentId.removePrefix("$treeId/")
                    else -> return@firstNotNullOfOrNull null
                }
                OutputFolder(permission.uri, relative, parentId.substringAfter(':').ifEmpty { parentId })
            }
    }

    /**
     * Accepts a folder the user granted for [rom] when its document IDs are not paths: the folder
     * counts as the ROM's when it directly holds a file of the same name and size.
     */
    fun outputFolderFromGrant(rom: PickedFile, treeUri: Uri): OutputFolder? {
        outputFolderFor(rom)?.let { return it }
        val folder = DocumentFile.fromTreeUri(context, treeUri) ?: return null
        val match = folder.findFile(rom.name) ?: return null
        if (!match.isFile || (rom.sizeBytes >= 0 && match.length() != rom.sizeBytes)) return null
        val label = DocumentsContract.getTreeDocumentId(treeUri).let { it.substringAfter(':').ifEmpty { it } }
        return OutputFolder(treeUri, "", label)
    }

    fun outputExists(folder: OutputFolder, fileName: String): Boolean =
        resolve(folder)?.findFile(fileName) != null

    /** Patches [rom] with [patch] and writes [outputName] into [folder], replacing any file of that name. */
    fun patch(rom: PickedFile, patch: PickedFile, folder: OutputFolder, outputName: String): Long {
        require(!outputName.equals(rom.name, ignoreCase = true)) { "Refusing to overwrite the original ROM." }
        val patchBytes = readAll(patch.uri)
        val romSize = rom.sizeBytes.takeIf { it >= 0 } ?: 0L
        // The ROM, its patched copy, and the patch are all held in memory at once.
        val budget = Runtime.getRuntime().maxMemory() * 3 / 4
        if (romSize * 2 + patchBytes.size > budget) throw PatchException(PatchFailure.TooLarge)
        val output = RomPatcher.apply(readAll(rom.uri), patchBytes)

        val directory = resolve(folder) ?: error("Unable to open ${folder.label}.")
        directory.findFile(outputName)?.let { existing ->
            require(existing.delete()) { "Unable to replace $outputName." }
        }
        val target = directory.createFile("application/octet-stream", outputName)
            ?: error("Unable to create $outputName.")
        try {
            context.contentResolver.openOutputStream(target.uri, "w").use { stream ->
                requireNotNull(stream) { "Unable to write $outputName." }
                stream.write(output)
            }
        } catch (error: Throwable) {
            target.delete()
            throw error
        }
        return output.size.toLong()
    }

    private fun resolve(folder: OutputFolder): DocumentFile? {
        val root = DocumentFile.fromTreeUri(context, folder.treeUri) ?: return null
        return if (folder.relativePath.isEmpty()) root else root.findRelative(folder.relativePath)
    }

    private fun readAll(uri: Uri): ByteArray =
        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("Unable to read $uri.")
}
