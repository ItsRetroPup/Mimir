package pup.app.mimir.data

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import pup.app.mimir.domain.ConverterTool
import pup.app.mimir.domain.FileOperation
import java.io.File
import java.util.UUID
import kotlin.concurrent.thread

/** Runs a bundled ARM64 command-line converter against a staged Storage Access Framework file. */
class NativeConverterRunner(private val context: Context) {
    data class ConversionResult(val output: File)

    fun convert(
        root: DocumentFile,
        operation: FileOperation.ConvertWithTool,
        cancellation: OperationCancellation? = null,
        onProgress: ((Float) -> Unit)? = null,
    ): ConversionResult {
        val executable = File(context.applicationInfo.nativeLibraryDir, operation.tool.executableName)
        require(executable.isFile && executable.canExecute()) {
            "${operation.tool.displayName} is not bundled for this device's CPU architecture."
        }
        val source = findFile(root, operation.sourcePath)
            ?: error("Missing source image: ${operation.sourcePath}")
        val workspace = File(context.cacheDir, "converter/${UUID.randomUUID()}")
        require(workspace.mkdirs()) { "Unable to prepare converter workspace." }
        try {
            val sourceName = source.name ?: error("Source image has no name.")
            val stagedSource = File(workspace, sourceName)
            context.contentResolver.openInputStream(source.uri).use { input ->
                requireNotNull(input) { "Unable to read ${operation.sourcePath}" }
                stagedSource.outputStream().use(input::copyTo)
            }
            val expectedOutput = File(workspace, File(operation.targetPath).name)
            val command = commandFor(operation.tool, executable, stagedSource, workspace, expectedOutput)
            val process = ProcessBuilder(command)
                .directory(workspace)
                .redirectErrorStream(true)
                .start()
            onProgress?.invoke(0f)
            var outputLog = ""
            val logReader = thread(start = true, isDaemon = true) {
                outputLog = process.inputStream.bufferedReader().useLines { lines ->
                    lines.joinToString(separator = "\n") { line ->
                        PERCENT_PATTERN.findAll(line).lastOrNull()?.groupValues?.get(1)?.toFloatOrNull()?.let {
                            onProgress?.invoke((it / 100f).coerceIn(0f, 1f))
                        }
                        line
                    }
                }
            }
            while (process.isAlive) {
                if (cancellation?.shouldInterruptCurrentOperation() == true) {
                    process.destroyForcibly()
                    logReader.join()
                    throw OperationStoppedException("Conversion stopped by user.")
                }
                Thread.sleep(PROCESS_POLL_INTERVAL_MS)
            }
            logReader.join()
            require(process.waitFor() == 0 && expectedOutput.isFile) {
                "${operation.tool.displayName} failed to convert ${operation.sourcePath}.${if (outputLog.isBlank()) "" else "\n$outputLog"}"
            }
            onProgress?.invoke(1f)
            val retainedOutput = File(context.cacheDir, "converter-output-${UUID.randomUUID()}.${operation.tool.outputExtension}")
            expectedOutput.copyTo(retainedOutput, overwrite = true)
            return ConversionResult(retainedOutput)
        } finally {
            workspace.deleteRecursively()
        }
    }

    private fun commandFor(
        tool: ConverterTool,
        executable: File,
        source: File,
        workspace: File,
        output: File,
    ): List<String> = when (tool) {
        ConverterTool.DolphinRvz -> listOf(
            executable.absolutePath, "convert", "-i", source.absolutePath, "-o", output.absolutePath,
            "-f", "rvz", "-b", "131072", "-c", "zstd", "-l", "5",
        )
        ConverterTool.AzaharZcci -> listOf(
            executable.absolutePath, "-c", source.absolutePath, "-o", workspace.absolutePath,
        )
        ConverterTool.Chd -> error("CHD conversions are run by CHDMan.")
    }

    private fun findFile(root: DocumentFile, relativePath: String): DocumentFile? {
        var current = root
        val parts = relativePath.split('/').filter(String::isNotBlank)
        parts.forEachIndexed { index, part ->
            val child = current.findFile(part) ?: return null
            if (index == parts.lastIndex) return child
            current = child
        }
        return null
    }

    private companion object {
        val PERCENT_PATTERN = Regex("""(\d{1,3}(?:\.\d+)?)\s*%""")
        const val PROCESS_POLL_INTERVAL_MS = 100L
    }
}
