package pup.app.mimir.data

import java.io.InputStream

/** Turns a converter's console output into progress that only moves forward for one conversion. */
class ConverterProgress(private val onProgress: ((Float) -> Unit)?) {
    private var reported = -1f

    fun onLine(line: String) {
        val progress = parse(line) ?: return
        if (progress > reported) {
            reported = progress
            onProgress?.invoke(progress)
        }
    }

    companion object {
        // CHDMan prints "Compressing, 37.4% complete... (ratio=48.2%)"; the ratio is not progress.
        private val COMPLETE_PATTERN = Regex("""(\d{1,3}(?:\.\d+)?)\s*%\s*complete""", RegexOption.IGNORE_CASE)
        private val PERCENT_PATTERN = Regex("""(\d{1,3}(?:\.\d+)?)\s*%""")

        /** Returns the line's progress as 0..1, preferring an explicit "% complete" value. */
        fun parse(line: String): Float? =
            (COMPLETE_PATTERN.find(line) ?: PERCENT_PATTERN.find(line))
                ?.groupValues?.get(1)?.toFloatOrNull()
                ?.let { (it / 100f).coerceIn(0f, 1f) }
    }
}

/**
 * Reads a converter's combined output, reporting progress for each `\r`- or `\n`-terminated update,
 * and returns the tail of the log for error messages.
 */
fun readConverterOutput(
    input: InputStream,
    onProgress: ((Float) -> Unit)?,
    maxLogLength: Int = 4_000,
): String {
    val progress = ConverterProgress(onProgress)
    val log = StringBuilder()
    val update = StringBuilder()
    input.bufferedReader().use { reader ->
        while (true) {
            val value = reader.read()
            if (value == -1) break
            val character = value.toChar()
            if (character == '\r' || character == '\n') {
                progress.onLine(update.toString())
                update.clear()
            } else {
                update.append(character)
            }
            log.append(character)
            if (log.length > maxLogLength) log.delete(0, log.length - maxLogLength)
        }
    }
    progress.onLine(update.toString())
    return log.toString()
}
