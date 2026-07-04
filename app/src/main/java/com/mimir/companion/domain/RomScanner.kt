package com.mimir.companion.domain

object RomScanner {
    private val patterns = listOf(
        Regex("""(?i)\s*[\(\[]?(disc|disk|cd)\s*([0-9]+)[\)\]]?"""),
        Regex("""(?i)\s*[\(\[]?([0-9]+)\s*(of)\s*([0-9]+)[\)\]]?"""),
    )

    fun scan(entries: List<RomEntry>): ScanResult {
        val grouped = linkedMapOf<Pair<String, String>, MutableList<Pair<RomEntry, DiscMatch>>>()

        entries.forEach { entry ->
            val discMatch = parseDisc(entry.fileName) ?: return@forEach
            val key = entry.relativeParent() to discMatch.title.lowercase()
            grouped.getOrPut(key) { mutableListOf() }.add(entry to discMatch)
        }

        val discSets = grouped.values
            .mapNotNull { bucket ->
                if (bucket.size < 2) {
                    null
                } else {
                    val sorted = bucket.sortedBy { (_, match) -> match.discNumber }
                    DiscGameSet(
                        title = sorted.first().second.title,
                        parentPath = sorted.first().first.relativeParent(),
                        entries = sorted.map { it.first },
                    )
                }
            }
            .sortedBy { it.title.lowercase() }

        return ScanResult(
            totalFiles = entries.size,
            allEntries = entries,
            discSets = discSets,
        )
    }

    fun parseDisc(fileName: String): DiscMatch? {
        val stem = fileName.substringBeforeLast('.')
        for (pattern in patterns) {
            val match = pattern.find(stem) ?: continue
            val discNumber = when (pattern) {
                patterns[0] -> match.groupValues[2].toIntOrNull()
                else -> match.groupValues[1].toIntOrNull()
            } ?: continue
            val cleaned = buildString {
                append(stem.removeRange(match.range).trim())
            }
                .replace(Regex("""[\s._-]+$"""), "")
                .replace(Regex("""\s{2,}"""), " ")
            if (cleaned.isBlank()) {
                return null
            }
            return DiscMatch(title = cleaned, discNumber = discNumber)
        }
        return null
    }

    private fun RomEntry.relativeParent(): String = RelativePaths.parentOf(relativePath)
}
