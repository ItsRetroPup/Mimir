package pup.app.mimir.domain

/**
 * Matches ROM folder names to a system's aliases however the folder is written. Case, spaces,
 * dashes, underscores, dots and a trailing tag such as "(Decrypted)" are ignored, so "Nintendo 3DS",
 * "nintendo_3ds", "Nintendo-3DS" and the No-Intro "Nintendo - Nintendo 3DS (Decrypted)" all match
 * the alias "nintendo 3ds".
 */
object SystemFolders {
    private val trailingTags = Regex("""\s*[(\[][^)\]]*[)\]]\s*$""")
    private val separators = Regex("""[^\p{L}\p{N}]""")

    fun matches(folderName: String, aliases: Set<String>): Boolean {
        val name = normalize(folderName)
        return name.isNotEmpty() && aliases.any { normalize(it) == name }
    }

    internal fun normalize(name: String): String {
        var stripped = name.trim()
        // Drop every trailing tag, so "3DS (Decrypted) [EU]" is read as "3DS".
        while (true) {
            val next = stripped.replace(trailingTags, "")
            if (next == stripped) break
            stripped = next
        }
        return stripped.lowercase().replace(separators, "")
    }
}
