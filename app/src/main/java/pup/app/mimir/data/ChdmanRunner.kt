package pup.app.mimir.data

import android.content.Context
import android.text.format.Formatter
import androidx.documentfile.provider.DocumentFile
import pup.app.mimir.R
import pup.app.mimir.domain.FileOperation
import pup.app.mimir.domain.RelativePaths
import java.io.File
import java.util.UUID
import kotlin.concurrent.thread

/**
 * Runs the Android-native CHDMan executable packaged at lib/<abi>/libchdman.so.
 *
 * Android application storage is not executable, so the tool must remain in the APK native-library
 * directory. Input files are copied into an app-cache workspace because CHDMan accepts filesystem
 * paths while Mimir operates on Storage Access Framework document URIs. That copy plus the output
 * can be tens of gigabytes for a PS3 disc, so the cache's free space is checked before starting.
 */
class ChdmanRunner(private val context: Context) {
    data class ConversionResult(
        val output: File,
        val originalSourcePaths: List<String>,
    )

    fun convert(
        root: DocumentFile,
        operation: FileOperation.ConvertToChd,
        cancellation: OperationCancellation? = null,
        onProgress: ((Float) -> Unit)? = null,
    ): ConversionResult =
        runCatching {
            val executable = File(context.applicationInfo.nativeLibraryDir, EXECUTABLE_NAME)
            require(executable.isFile && executable.canExecute()) {
                "CHDMan is not bundled for this device's CPU architecture."
            }

            val source = root.findRelative(operation.sourcePath)
                ?: error("Missing source image: ${operation.sourcePath}")
            val inputs = sourceFiles(source)
            requireCacheSpace(source.name.orEmpty(), inputs.values.sumOf { it.length().coerceAtLeast(0L) })
            val workspace = File(context.cacheDir, "chdman/${UUID.randomUUID()}")
            require(workspace.mkdirs()) { "Unable to prepare CHDMan workspace." }
            try {
                inputs.forEach { (fileName, file) -> copyToWorkspace(file, workspace, fileName) }
                val stagedFiles = inputs.keys
                val stagedSource = File(workspace, source.name ?: error("Source image has no name."))
                val output = File(workspace, File(operation.targetPath).name)
                val process = ProcessBuilder(
                    executable.absolutePath,
                    operation.discType.commandName,
                    "-i", stagedSource.name,
                    "-o", output.absolutePath,
                )
                    .directory(workspace)
                    .redirectErrorStream(true)
                    .start()
                onProgress?.invoke(0f)
                var log = ""
                val logReader = thread(start = true, isDaemon = true) {
                    log = readConverterOutput(process.inputStream, onProgress, MAX_LOG_LENGTH)
                }
                while (process.isAlive) {
                    if (cancellation?.shouldInterruptCurrentOperation() == true) {
                        process.destroy()
                        if (process.isAlive) process.destroyForcibly()
                        logReader.join()
                        throw OperationStoppedException("Conversion stopped by user.")
                    }
                    Thread.sleep(PROCESS_POLL_INTERVAL_MS)
                }
                logReader.join()
                require(process.waitFor() == 0 && output.isFile) {
                    "CHDMan failed to convert ${operation.sourcePath}.${if (log.isBlank()) "" else "\n$log"}"
                }
                onProgress?.invoke(1f)

                // Both live in the cache, so a rename moves the output without a second full copy.
                val retainedOutput = File(context.cacheDir, "chdman-output-${UUID.randomUUID()}.chd")
                if (!output.renameTo(retainedOutput)) output.copyTo(retainedOutput, overwrite = true)
                ConversionResult(
                    output = retainedOutput,
                    originalSourcePaths = stagedFiles.map { fileName ->
                        val parentPath = RelativePaths.parentOf(operation.sourcePath)
                        if (parentPath.isBlank()) fileName else "$parentPath/$fileName"
                    },
                )
            } finally {
                workspace.deleteRecursively()
            }
        }.getOrElse { throw it }

    /** The image and, for a .cue or .gdi, every track it references, keyed by file name. */
    private fun sourceFiles(source: DocumentFile): Map<String, DocumentFile> {
        val sourceName = source.name ?: error("Source image has no name.")
        val files = linkedMapOf(sourceName to source)
        val extension = sourceName.substringAfterLast('.', "").lowercase()
        if (extension !in DESCRIPTOR_EXTENSIONS) return files

        val descriptor = context.contentResolver.openInputStream(source.uri)
            ?.bufferedReader()
            ?.use { it.readText() }
            ?: error("Unable to read ${source.name}")
        val parent = source.parentFile ?: error("Unable to access ${source.name}'s folder.")
        referencedFileNames(extension, descriptor).forEach { fileName ->
            files[fileName] = parent.findFile(fileName)
                ?: error("Missing track referenced by ${source.name}: $fileName")
        }
        return files
    }

    /** The staged copy plus an output that, at worst, is as large as the input. */
    private fun requireCacheSpace(sourceName: String, inputBytes: Long) {
        val needed = inputBytes * 2
        val free = context.cacheDir.usableSpace
        require(free >= needed) {
            context.getString(
                R.string.msg_chd_cache_space,
                sourceName,
                Formatter.formatShortFileSize(context, needed),
                Formatter.formatShortFileSize(context, free),
            )
        }
    }

    private fun copyToWorkspace(source: DocumentFile, workspace: File, fileName: String) {
        require(!fileName.contains('/') && !fileName.contains('\\')) {
            "CHDMan only supports disc tracks in the same folder as the descriptor: $fileName"
        }
        context.contentResolver.openInputStream(source.uri).use { input ->
            requireNotNull(input) { "Unable to read ${source.name}" }
            File(workspace, fileName).outputStream().use(input::copyTo)
        }
    }

    private fun referencedFileNames(extension: String, contents: String): Set<String> =
        when (extension) {
            "cue" -> CUE_FILE_PATTERN.findAll(contents).map { it.groupValues[1] }.toSet()
            "gdi" -> contents
                .lineSequence()
                .drop(1)
                .mapNotNull { line ->
                    GDI_TRACK_PATTERN.find(line)?.let { match ->
                        match.groupValues[1].ifBlank { match.groupValues[2] }
                    }
                }
                .filter { it.isNotBlank() }
                .toSet()
            else -> emptySet()
        }

    private companion object {
        const val EXECUTABLE_NAME = "libchdman.so"
        const val MAX_LOG_LENGTH = 4_000
        const val PROCESS_POLL_INTERVAL_MS = 100L
        val DESCRIPTOR_EXTENSIONS = setOf("cue", "gdi")
        val CUE_FILE_PATTERN = Regex("""(?im)^\s*FILE\s+"([^"]+)"""")
        val GDI_TRACK_PATTERN = Regex("""^\s*\d+\s+\d+\s+\d+\s+\d+\s+(?:"([^"]+)"|(\S+))""")
    }
}
