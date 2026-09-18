package pup.app.mimir.data

import android.content.Context
import androidx.documentfile.provider.DocumentFile
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
 * paths while Mimir operates on Storage Access Framework document URIs.
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

            val source = findFile(root, operation.sourcePath)
                ?: error("Missing source image: ${operation.sourcePath}")
            val workspace = File(context.cacheDir, "chdman/${UUID.randomUUID()}")
            require(workspace.mkdirs()) { "Unable to prepare CHDMan workspace." }
            try {
                val stagedFiles = stageSource(source, workspace)
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
                    log = readProcessOutput(process.inputStream, onProgress)
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

                val retainedOutput = File(context.cacheDir, "chdman-output-${UUID.randomUUID()}.chd")
                output.copyTo(retainedOutput, overwrite = true)
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

    private fun readProcessOutput(
        input: java.io.InputStream,
        onProgress: ((Float) -> Unit)?,
    ): String {
        val log = StringBuilder()
        val update = StringBuilder()
        input.bufferedReader().use { reader ->
            while (true) {
                val value = reader.read()
                if (value == -1) break
                val character = value.toChar()
                if (character == '\r' || character == '\n') {
                    reportProgress(update.toString(), onProgress)
                    update.clear()
                } else {
                    update.append(character)
                }
                log.append(character)
                if (log.length > MAX_LOG_LENGTH) log.delete(0, log.length - MAX_LOG_LENGTH)
            }
        }
        reportProgress(update.toString(), onProgress)
        return log.toString()
    }

    private fun reportProgress(line: String, onProgress: ((Float) -> Unit)?) {
        PERCENT_PATTERN.findAll(line).lastOrNull()?.groupValues?.get(1)?.toFloatOrNull()?.let { percent ->
            onProgress?.invoke((percent / 100f).coerceIn(0f, 1f))
        }
    }

    private fun stageSource(source: DocumentFile, workspace: File): Set<String> {
        val sourceName = source.name ?: error("Source image has no name.")
        val stagedFiles = linkedSetOf(sourceName)
        copyToWorkspace(source, workspace, sourceName)
        val extension = source.name.orEmpty().substringAfterLast('.', "").lowercase()
        if (extension !in DESCRIPTOR_EXTENSIONS) return stagedFiles

        val descriptor = context.contentResolver.openInputStream(source.uri)
            ?.bufferedReader()
            ?.use { it.readText() }
            ?: error("Unable to read ${source.name}")
        val parent = source.parentFile ?: error("Unable to access ${source.name}'s folder.")
        referencedFileNames(extension, descriptor).forEach { fileName ->
            val referenced = parent.findFile(fileName)
                ?: error("Missing track referenced by ${source.name}: $fileName")
            copyToWorkspace(referenced, workspace, fileName)
            stagedFiles += fileName
        }
        return stagedFiles
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

    private fun findFile(root: DocumentFile, relativePath: String): DocumentFile? {
        var current = root
        val parts = relativePath.split('/').filter { it.isNotBlank() }
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
        const val EXECUTABLE_NAME = "libchdman.so"
        const val MAX_LOG_LENGTH = 4_000
        const val PROCESS_POLL_INTERVAL_MS = 100L
        val DESCRIPTOR_EXTENSIONS = setOf("cue", "gdi")
        val CUE_FILE_PATTERN = Regex("""(?im)^\s*FILE\s+"([^"]+)"""")
        val GDI_TRACK_PATTERN = Regex("""^\s*\d+\s+\d+\s+\d+\s+\d+\s+(?:"([^"]+)"|(\S+))""")
        val PERCENT_PATTERN = Regex("""(\d{1,3}(?:\.\d+)?)\s*%""")
    }
}
