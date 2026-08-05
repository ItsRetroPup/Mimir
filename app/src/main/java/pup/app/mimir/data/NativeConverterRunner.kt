package pup.app.mimir.data

import android.content.Context
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
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
        if (operation.tool == ConverterTool.NszNsp) {
            return convertNsz(root, operation, cancellation, onProgress)
        }
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

    private fun convertNsz(
        root: DocumentFile,
        operation: FileOperation.ConvertWithTool,
        cancellation: OperationCancellation?,
        onProgress: ((Float) -> Unit)?,
    ): ConversionResult {
        val source = findFile(root, operation.sourcePath)
            ?: error("Missing source package: ${operation.sourcePath}")
        val workspace = File(context.cacheDir, "converter/${UUID.randomUUID()}")
        require(workspace.mkdirs()) { "Unable to prepare converter workspace." }
        try {
            val sourceName = source.name ?: error("Source package has no name.")
            val startedAt = System.nanoTime()
            Log.i(TAG, "NSZ conversion started: ${operation.sourcePath} (${source.length()} bytes)")
            val stagedSource = File(workspace, sourceName)
            context.contentResolver.openInputStream(source.uri).use { input ->
                requireNotNull(input) { "Unable to read ${operation.sourcePath}" }
                stagedSource.outputStream().use(input::copyTo)
            }
            val expectedOutput = File(workspace, File(operation.targetPath).name)
            val keysFile = File(context.filesDir, "nsz/prod.keys")
            require(keysFile.isFile && keysFile.length() > 0L) {
                "Import a prod.keys file before converting NSZ packages."
            }
            val callback = NszConversionCallback(cancellation, onProgress)
            if (!Python.isStarted()) {
                Python.start(AndroidPlatform(context))
            }
            try {
                Python.getInstance()
                    .getModule("mimir_nsz")
                    .callAttr(
                        "decompress_nsz",
                        stagedSource.absolutePath,
                        expectedOutput.absolutePath,
                        keysFile.absolutePath,
                        callback,
                    )
            } catch (error: Exception) {
                Log.e(TAG, "NSZ conversion failed: ${operation.sourcePath}", error)
                if (cancellation?.shouldInterruptCurrentOperation() == true) {
                    throw OperationStoppedException("Conversion stopped by user.")
                }
                throw error
            }
            require(expectedOutput.isFile) {
                "NSZ failed to convert ${operation.sourcePath}."
            }
            val retainedOutput = File(context.cacheDir, "converter-output-${UUID.randomUUID()}.nsp")
            expectedOutput.copyTo(retainedOutput, overwrite = true)
            Log.i(
                TAG,
                "NSZ conversion completed: ${operation.sourcePath} -> ${retainedOutput.length()} bytes " +
                    "in ${(System.nanoTime() - startedAt) / 1_000_000_000L}s",
            )
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
        ConverterTool.NszNsp -> error("NSZ conversions are run by embedded Python.")
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
        const val TAG = "Mimir.NSZ"
        val PERCENT_PATTERN = Regex("""(\d{1,3}(?:\.\d+)?)\s*%""")
        const val PROCESS_POLL_INTERVAL_MS = 100L
    }
}

class NszConversionCallback(
    private val cancellation: OperationCancellation?,
    private val progress: ((Float) -> Unit)?,
) {
    fun isCancellationRequested(): Boolean = cancellation?.shouldInterruptCurrentOperation() == true

    fun onProgress(value: Double) {
        progress?.invoke(value.toFloat().coerceIn(0f, 1f))
    }
}
