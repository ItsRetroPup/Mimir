package pup.app.mimir.domain

/** What one tool has done over the app's lifetime. */
data class ToolUsage(
    val games: Int = 0,
    /** Bytes freed by this tool's writes; negative if its outputs grew. */
    val spaceSavedBytes: Long = 0L,
)

/** Lifetime totals of the games Mimir's space-saving tools have compressed and the space they freed. */
data class UsageStats(
    /** When counting began, in epoch milliseconds. */
    val sinceMillis: Long,
    val tools: Map<String, ToolUsage> = emptyMap(),
) {
    val totalGames: Int get() = tools.values.sumOf { it.games }

    /** Net space freed across every tool, never shown below zero. */
    val totalSpaceSavedBytes: Long get() = tools.values.sumOf { it.spaceSavedBytes }.coerceAtLeast(0L)

    fun usage(key: String): ToolUsage = tools[key] ?: ToolUsage()

    fun record(key: String, games: Int, spaceSavedBytes: Long): UsageStats {
        if (games <= 0 && spaceSavedBytes == 0L) return this
        val current = usage(key)
        return copy(
            tools = tools + (key to ToolUsage(current.games + games.coerceAtLeast(0), current.spaceSavedBytes + spaceSavedBytes)),
        )
    }

    companion object {
        /**
         * The stats key for a tool: the converter's name for converter tools, the mode's name for
         * the rest. These names are persisted, so renaming an enum entry resets that tool's count.
         */
        fun keyFor(mode: ToolMode, converter: ConverterTool? = null): String = converter?.name ?: mode.name

        /** The tools Stats tracks: RomZipper and the compressing converters. NSZ to NSP decompresses, so it is left out. */
        fun savesSpace(mode: ToolMode, converter: ConverterTool?): Boolean = when (mode) {
            ToolMode.RomZipper -> true
            ToolMode.Converter -> converter != null && converter != ConverterTool.NszNsp
            else -> false
        }
    }
}
